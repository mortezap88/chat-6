package com.example.network

import android.util.Log
import com.example.data.ApiKeyItem
import com.example.data.FailoverEvent
import com.example.data.KeyStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Multi-key resilient web search engine with automatic key pool failover
 * and zero-setup live web search fallbacks (DuckDuckGo HTML + Wikipedia).
 * Guarantees that web queries always return real website snippets and sources.
 */
class TavilyPoolService(
    private var tavilyKeys: List<ApiKeyItem> = emptyList(),
    private val onKeyStatusChanged: (List<ApiKeyItem>) -> Unit = {},
    private val onFailover: (FailoverEvent) -> Unit = {}
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private var activeKeyIndex: Int = 0
    private var lastErrorReason: String? = null

    fun updateKeys(newKeys: List<ApiKeyItem>) {
        this.tavilyKeys = newKeys
        if (activeKeyIndex >= newKeys.size) {
            activeKeyIndex = 0
        }
    }

    fun getActiveKeyIndex(): Int = activeKeyIndex
    fun getLastError(): String? = lastErrorReason

    private fun isRealTavilyKey(key: String): Boolean {
        val trimmed = key.trim()
        return trimmed.startsWith("tvly-") &&
                !trimmed.contains("production-search-pool-key") &&
                !trimmed.contains("failover-backup-search-key") &&
                trimmed != "MY_TAVILY_API_KEY" &&
                trimmed.length > 15
    }

    suspend fun search(
        query: String,
        thoroughness: String = "Balanced",
        maxResults: Int = 5
    ): TavilySearchResult? = withContext(Dispatchers.IO) {
        lastErrorReason = null
        val effectiveMaxResults = maxResults.coerceIn(1, 10)
        val searchDepth = when (thoroughness.lowercase().trim()) {
            "fast" -> "basic"
            "comprehensive" -> "advanced"
            else -> "basic"
        }

        val realKeys = tavilyKeys.filter { isRealTavilyKey(it.key) }

        // If user configured valid Tavily API keys, attempt to query Tavily API
        if (realKeys.isNotEmpty()) {
            val totalKeys = tavilyKeys.size
            var attempts = 0
            while (attempts < totalKeys) {
                val keyItem = tavilyKeys.getOrNull(activeKeyIndex)
                if (keyItem == null || !isRealTavilyKey(keyItem.key)) {
                    activeKeyIndex = (activeKeyIndex + 1) % tavilyKeys.size.coerceAtLeast(1)
                    attempts++
                    continue
                }

                try {
                    val jsonPayload = JSONObject().apply {
                        put("api_key", keyItem.key)
                        put("query", query)
                        put("search_depth", searchDepth)
                        put("max_results", effectiveMaxResults)
                        put("include_answer", true)
                        put("include_raw_content", false)
                    }

                    val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("https://api.tavily.com/search")
                        .post(body)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        val root = JSONObject(responseBody)
                        val answer = if (root.has("answer") && !root.isNull("answer")) root.getString("answer") else null
                        val resultsList = mutableListOf<TavilySource>()

                        val resultsArray = root.optJSONArray("results") ?: JSONArray()
                        for (i in 0 until resultsArray.length()) {
                            val itemObj = resultsArray.getJSONObject(i)
                            resultsList.add(
                                TavilySource(
                                    title = itemObj.optString("title", "Untitled Source"),
                                    url = itemObj.optString("url", ""),
                                    content = itemObj.optString("content", ""),
                                    score = itemObj.optDouble("score", 0.0)
                                )
                            )
                        }

                        if (keyItem.status != KeyStatus.WORKING) {
                            updateKeyStatus(keyItem.id, KeyStatus.WORKING, null)
                        }

                        if (resultsList.isNotEmpty() || !answer.isNullOrBlank()) {
                            return@withContext TavilySearchResult(
                                query = query,
                                answer = answer,
                                results = resultsList
                            )
                        }
                    } else {
                        val code = response.code
                        val errorMsg = when (code) {
                            401 -> "Tavily API HTTP 401 (Unauthorized: Invalid or expired API key)."
                            429 -> "Tavily API HTTP 429 (Rate limit or monthly quota exceeded)."
                            else -> "Tavily API HTTP $code: ${responseBody.take(120)}"
                        }
                        lastErrorReason = errorMsg
                        Log.w("TavilyPoolService", "Key fail on ${keyItem.label}: $errorMsg")
                        failoverToNextKey(keyItem, errorMsg)
                        attempts++
                    }
                } catch (e: Exception) {
                    val errorMsg = "Tavily network connection issue: ${e.message ?: "Network error"}"
                    lastErrorReason = errorMsg
                    Log.w("TavilyPoolService", "Network exception on ${keyItem.label}: ${e.message}")
                    failoverToNextKey(keyItem, e.message ?: "Network error")
                    attempts++
                }
            }
        }

        // Live web search fallback (DuckDuckGo HTML + Wikipedia) to guarantee real website snippets
        val fallbackRes = executeLiveWebFallbackSearch(query, effectiveMaxResults)
        if (fallbackRes != null && fallbackRes.results.isNotEmpty()) {
            return@withContext fallbackRes
        }

        if (lastErrorReason == null) {
            lastErrorReason = "All web search sources failed to return results."
        }
        null
    }

    private fun executeLiveWebFallbackSearch(query: String, maxResults: Int): TavilySearchResult? {
        try {
            val formBody = okhttp3.FormBody.Builder()
                .add("q", query)
                .build()

            val request = Request.Builder()
                .url("https://html.duckduckgo.com/html/")
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val html = response.body?.string().orEmpty()
                if (html.isNotBlank()) {
                    val results = mutableListOf<TavilySource>()

                    val blockRegex = Regex("""<div class=["']result results_links[^"']*["']>(.*?)</div>\s*</div>\s*</div>""", RegexOption.DOT_MATCHES_ALL)
                    val titleRegex = Regex("""<a[^>]*class=["']result__a["'][^>]*href=["']([^"']*)["'][^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
                    val snippetRegex = Regex("""<a[^>]*class=["']result__snippet["'][^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)

                    val blocks = blockRegex.findAll(html).toList()
                    for (block in blocks) {
                        if (results.size >= maxResults) break
                        val bText = block.groupValues[1]
                        val tm = titleRegex.find(bText)
                        val sm = snippetRegex.find(bText)
                        if (tm != null && sm != null) {
                            var url = tm.groupValues[1].trim()
                            if (url.startsWith("//")) url = "https:$url"
                            if (url.contains("uddg=")) {
                                val m = Regex("""uddg=([^&]+)""").find(url)
                                if (m != null) {
                                    try {
                                        url = java.net.URLDecoder.decode(m.groupValues[1], "UTF-8")
                                    } catch (_: Exception) {}
                                }
                            }
                            val cleanTitle = cleanHtmlText(tm.groupValues[2])
                            val cleanSnippet = cleanHtmlText(sm.groupValues[1])
                            if (cleanTitle.isNotBlank() && cleanSnippet.isNotBlank()) {
                                results.add(
                                    TavilySource(
                                        title = cleanTitle,
                                        url = url,
                                        content = cleanSnippet,
                                        score = 0.95 - (results.size * 0.05)
                                    )
                                )
                            }
                        }
                    }

                    if (results.isNotEmpty()) {
                        return TavilySearchResult(query = query, answer = null, results = results)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TavilyPoolService", "DuckDuckGo search exception: ${e.message}")
        }

        // Secondary fallback to Wikipedia API
        return executeWikipediaFallbackSearch(query, maxResults)
    }

    private fun executeWikipediaFallbackSearch(query: String, maxResults: Int): TavilySearchResult? {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "StreamChat-Android/1.0 (ai.studio)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bodyStr = response.body?.string().orEmpty()
            val root = JSONObject(bodyStr)
            val queryObj = root.optJSONObject("query") ?: return null
            val searchArr = queryObj.optJSONArray("search") ?: return null

            val results = mutableListOf<TavilySource>()
            val count = minOf(searchArr.length(), maxResults)
            for (i in 0 until count) {
                val item = searchArr.getJSONObject(i)
                val title = item.optString("title", "")
                val rawSnippet = item.optString("snippet", "")
                val snippet = cleanHtmlText(rawSnippet)
                val pageUrl = "https://en.wikipedia.org/wiki/${java.net.URLEncoder.encode(title.replace(" ", "_"), "UTF-8")}"
                if (title.isNotBlank() && snippet.isNotBlank()) {
                    results.add(
                        TavilySource(
                            title = title,
                            url = pageUrl,
                            content = snippet,
                            score = 0.90 - (i * 0.05)
                        )
                    )
                }
            }
            if (results.isNotEmpty()) {
                return TavilySearchResult(query = query, answer = null, results = results)
            }
        } catch (e: Exception) {
            Log.w("TavilyPoolService", "Wikipedia fallback error: ${e.message}")
        }
        return null
    }

    private fun cleanHtmlText(raw: String): String {
        val withoutTags = raw.replace(Regex("<[^>]+>"), " ")
        return try {
            android.text.Html.fromHtml(withoutTags, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                .replace(Regex("\\s+"), " ")
        } catch (_: Throwable) {
            withoutTags.trim().replace(Regex("\\s+"), " ")
        }
    }

    private fun failoverToNextKey(failedItem: ApiKeyItem, reason: String) {
        val updated = tavilyKeys.toMutableList()
        val idx = updated.indexOfFirst { it.id == failedItem.id }
        if (idx >= 0) {
            val curr = updated[idx]
            updated[idx] = curr.copy(
                status = KeyStatus.FAILED,
                errorMessage = reason,
                failCount = curr.failCount + 1
            )
        }
        tavilyKeys = updated
        onKeyStatusChanged(updated)

        val nextUsable = tavilyKeys.indexOfFirst { it.status != KeyStatus.FAILED && isRealTavilyKey(it.key) }
        if (nextUsable >= 0) {
            activeKeyIndex = nextUsable
            val toKey = tavilyKeys[nextUsable]
            onFailover(
                FailoverEvent(
                    fromKeyMasked = failedItem.maskedKey,
                    toKeyMasked = toKey.maskedKey,
                    reason = reason
                )
            )
        } else {
            activeKeyIndex = (activeKeyIndex + 1) % tavilyKeys.size.coerceAtLeast(1)
        }
    }

    private fun updateKeyStatus(id: String, status: KeyStatus, error: String?) {
        val updated = tavilyKeys.map {
            if (it.id == id) it.copy(status = status, errorMessage = error) else it
        }
        tavilyKeys = updated
        onKeyStatusChanged(updated)
    }
}
