package com.example.memory

import android.content.Context
import android.util.Log
import com.example.data.ChatMessage
import com.example.data.ExtractedMemoryItem
import com.example.memory.data.AppDatabase
import com.example.memory.data.MemoryCategory
import com.example.memory.data.MemoryDao
import com.example.memory.data.MemoryEntity
import com.example.memory.retrieval.HybridMemoryRetriever
import com.example.memory.retrieval.ScoredMemory
import com.example.memory.retrieval.VectorMath
import com.example.network.GeminiPoolService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class MemoryRepository(
    context: Context,
    private val poolService: GeminiPoolService? = null
) {
    private val database: AppDatabase = AppDatabase.getInstance(context)
    private val dao: MemoryDao = database.memoryDao()

    val allMemories: Flow<List<MemoryEntity>> = dao.getAllMemoriesFlow()

    /**
     * Retrieves the most relevant memories for the incoming user query
     * using the Hybrid Vector RAG engine.
     */
    suspend fun retrieveRelevantMemories(
        query: String,
        maxMemories: Int = 4
    ): List<MemoryEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val all = dao.getAllMemories()
        if (all.isEmpty()) return@withContext emptyList()

        // Generate query embedding (remote embedding with local fallback)
        val queryEmbedding = computeEmbedding(query)

        val scored = HybridMemoryRetriever.scoreAndRank(
            query = query,
            queryEmbedding = queryEmbedding,
            candidates = all,
            topK = maxMemories
        )

        // Record access count and timestamp for retrieved memories
        for (item in scored) {
            dao.recordAccess(item.memory.id)
        }

        scored.map { it.memory }
    }

    /**
     * Simulates retrieval without updating access statistics.
     * Used by the Memory Vault Query Simulator UI for testing and inspection.
     */
    suspend fun simulateRetrieval(query: String): List<ScoredMemory> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val all = dao.getAllMemories()
        if (all.isEmpty()) return@withContext emptyList()

        val queryEmbedding = computeEmbedding(query)
        HybridMemoryRetriever.scoreAndRank(
            query = query,
            queryEmbedding = queryEmbedding,
            candidates = all,
            topK = 6,
            minScoreThreshold = 0.05f
        )
    }

    /**
     * Manually adds a user-created memory note into the vault.
     */
    suspend fun addMemoryNote(
        content: String,
        category: MemoryCategory = MemoryCategory.GENERAL,
        importance: Int = 3,
        keywords: String = "",
        source: String = "USER_NOTE"
    ): MemoryEntity = withContext(Dispatchers.IO) {
        val cleanContent = content.trim()
        val cleanKeywords = if (keywords.isNotBlank()) {
            keywords.trim()
        } else {
            extractSimpleKeywords(cleanContent)
        }

        val embedding = computeEmbedding(cleanContent)
        val embeddingJson = VectorMath.serializeEmbedding(embedding.toList())

        val entity = MemoryEntity(
            id = UUID.randomUUID().toString(),
            content = cleanContent,
            category = category.name,
            importance = importance.coerceIn(1, 5),
            timestamp = System.currentTimeMillis(),
            lastAccessed = System.currentTimeMillis(),
            accessCount = 0,
            keywords = cleanKeywords,
            source = source,
            embeddingJson = embeddingJson
        )

        dao.insert(entity)
        entity
    }

    suspend fun updateMemory(memory: MemoryEntity) = withContext(Dispatchers.IO) {
        val updatedEmbedding = computeEmbedding(memory.content)
        val withEmbedding = memory.copy(
            embeddingJson = VectorMath.serializeEmbedding(updatedEmbedding.toList())
        )
        dao.update(withEmbedding)
    }

    suspend fun deleteMemory(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        dao.getCount()
    }

    /**
     * Analyzes conversation turn and recent context to automatically extract atomic lasting memories.
     * Deduplicates against existing memories before saving.
     */
    suspend fun autoExtractAndSave(
        userText: String,
        aiResponse: String,
        recentHistory: List<ChatMessage> = emptyList(),
        primaryModel: String
    ) = withContext(Dispatchers.IO) {
        if (poolService == null) return@withContext
        if (userText.trim().length < 3 && recentHistory.isEmpty()) return@withContext

        try {
            val extractedItems = poolService.extractMemoriesFromConversation(
                userText = userText,
                aiResponse = aiResponse,
                recentHistory = recentHistory,
                model = primaryModel
            ).getOrNull() ?: emptyList()

            if (extractedItems.isEmpty()) return@withContext

            val existingMemories = dao.getAllMemories()

            for (item in extractedItems) {
                if (item.content.length < 5) continue

                val itemEmbedding = computeEmbedding(item.content)

                // Check for duplicate / existing equivalent memory
                var isDuplicate = false
                for (existing in existingMemories) {
                    val existingEmb = VectorMath.deserializeEmbedding(existing.embeddingJson)
                    if (existingEmb != null) {
                        val sim = VectorMath.cosineSimilarity(itemEmbedding, existingEmb)
                        if (sim > 0.82f) {
                            // Already known: bump access and update if new one is more descriptive
                            dao.recordAccess(existing.id)
                            isDuplicate = true
                            break
                        }
                    }
                }

                if (!isDuplicate) {
                    val entity = MemoryEntity(
                        id = UUID.randomUUID().toString(),
                        content = item.content,
                        category = MemoryCategory.fromString(item.category).name,
                        importance = item.importance.coerceIn(1, 5),
                        timestamp = System.currentTimeMillis(),
                        lastAccessed = System.currentTimeMillis(),
                        accessCount = 0,
                        keywords = item.keywords.joinToString(", "),
                        source = "AUTO_EXTRACTED",
                        embeddingJson = VectorMath.serializeEmbedding(itemEmbedding.toList())
                    )
                    dao.insert(entity)
                    Log.i("MemoryRepo", "Auto-extracted memory stored: ${entity.content}")
                }
            }
        } catch (e: Exception) {
            Log.w("MemoryRepo", "Memory extraction failed gracefully: ${e.message}")
        }
    }

    /**
     * Consolidates an entire session into lasting long-term memory notes,
     * computing semantic embeddings and saving to the Room database.
     * Returns the count of newly added memories.
     */
    suspend fun consolidateSessionAndSave(
        session: List<ChatMessage>,
        primaryModel: String
    ): Int = withContext(Dispatchers.IO) {
        if (poolService == null || session.isEmpty()) return@withContext 0

        var addedCount = 0
        try {
            val extractedItems = poolService.consolidateSessionIntoMemories(
                session = session,
                model = primaryModel
            ).getOrNull() ?: emptyList()

            if (extractedItems.isEmpty()) return@withContext 0

            val existingMemories = dao.getAllMemories()

            for (item in extractedItems) {
                if (item.content.length < 5) continue
                val itemEmbedding = computeEmbedding(item.content)

                var isDuplicate = false
                for (existing in existingMemories) {
                    val existingEmb = VectorMath.deserializeEmbedding(existing.embeddingJson)
                    if (existingEmb != null) {
                        val sim = VectorMath.cosineSimilarity(itemEmbedding, existingEmb)
                        if (sim > 0.85f) {
                            dao.recordAccess(existing.id)
                            isDuplicate = true
                            break
                        }
                    }
                }

                if (!isDuplicate) {
                    val entity = MemoryEntity(
                        id = UUID.randomUUID().toString(),
                        content = item.content,
                        category = MemoryCategory.fromString(item.category).name,
                        importance = item.importance.coerceIn(1, 5),
                        timestamp = System.currentTimeMillis(),
                        lastAccessed = System.currentTimeMillis(),
                        accessCount = 0,
                        keywords = item.keywords.joinToString(", "),
                        source = "SESSION_CONSOLIDATED",
                        embeddingJson = VectorMath.serializeEmbedding(itemEmbedding.toList())
                    )
                    dao.insert(entity)
                    addedCount++
                    Log.i("MemoryRepo", "Consolidated memory stored: ${entity.content}")
                }
            }
        } catch (e: Exception) {
            Log.w("MemoryRepo", "Consolidation failed gracefully: ${e.message}")
        }
        addedCount
    }

    private suspend fun computeEmbedding(text: String): FloatArray {
        if (poolService != null) {
            val remote = poolService.generateEmbedding(text).getOrNull()
            if (remote != null && remote.isNotEmpty()) {
                return remote.toFloatArray()
            }
        }
        // Deterministic on-device fallback
        return VectorMath.generateLocalEmbedding(text).toFloatArray()
    }

    private fun extractSimpleKeywords(text: String): String {
        return text.lowercase()
            .split(Regex("[\\s,.:;!?'\"()\\[\\]{}]+"))
            .filter { it.length > 3 && it !in commonStopWords }
            .distinct()
            .take(6)
            .joinToString(", ")
    }

    private val commonStopWords = setOf(
        "this", "that", "with", "have", "from", "they", "will", "would", "there", "their", "what",
        "about", "which", "when", "make", "like", "time", "just", "know", "take", "people", "into",
        "year", "your", "good", "some", "could", "them", "other", "than", "then", "now", "look",
        "only", "come", "its", "over", "think", "also", "back", "after", "use", "two", "how", "our", "work"
    )
}
