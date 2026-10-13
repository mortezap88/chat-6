package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioRecorderManager
import com.example.audio.VoicePersonaManager
import com.example.audio.VoicePersonas
import com.example.data.ApiKeyItem
import com.example.data.AppSettings
import com.example.data.ChatMessage
import com.example.data.FailoverEvent
import com.example.data.LinguisticCorrection
import com.example.data.UnifiedLinguisticResult
import com.example.data.SettingsRepository
import com.example.memory.MemoryRepository
import com.example.memory.retrieval.HybridMemoryRetriever
import com.example.network.GeminiPoolService
import com.example.network.TavilyContextDecider
import com.example.network.TavilyPoolService
import android.util.Log
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.MemoryVaultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var audioRecorderManager: AudioRecorderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)
        audioRecorderManager = AudioRecorderManager(this)

        setContent {
            MyApplicationTheme {
                MainAppContainer(
                    settingsRepository = settingsRepository,
                    audioRecorderManager = audioRecorderManager,
                    context = this
                )
            }
        }
    }
}

enum class ScreenState {
    CHAT,
    SETTINGS,
    MEMORY_VAULT
}

/**
 * Single-layer shape unifying the settings drawer sheet and the protruding bookmark notch.
 * Symmetrical fillet curvatures smoothly transition the bookmark tab into the drawer sheet
 * on both the top and bottom sides, creating a continuous, seamless single-layer surface.
 */
class BookmarkDrawerShape(
    private val protrusionPx: Float,
    private val tabTopPx: Float,
    private val tabHeightPx: Float,
    private val filletRadiusPx: Float,
    private val sheetCornerRadiusPx: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val sheetLeft = protrusionPx
            val tabBottom = tabTopPx + tabHeightPx
            val tabOuterRadius = tabHeightPx / 2f

            // Start at top-right of container
            moveTo(size.width, 0f)

            // Top edge of main sheet from right to sheet's left edge
            lineTo(sheetLeft, 0f)

            // Top vertical edge of main sheet down to top fillet start
            val topFilletStart = (tabTopPx - filletRadiusPx).coerceAtLeast(0f)
            lineTo(sheetLeft, topFilletStart)

            // TOP FILLET CURVATURE:
            // Smoothly transitions from the vertical left edge of the main sheet outward to the top edge of the tab.
            quadraticTo(
                sheetLeft, tabTopPx,
                sheetLeft - filletRadiusPx, tabTopPx
            )

            // Top edge of bookmark tab extending left to the outer rounded cap
            lineTo(tabOuterRadius, tabTopPx)

            // Outer semicircular rounded cap of the bookmark tab
            arcTo(
                rect = Rect(
                    left = 0f,
                    top = tabTopPx,
                    right = tabHeightPx,
                    bottom = tabBottom
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )

            // Bottom edge of bookmark tab extending right to bottom fillet start
            lineTo(sheetLeft - filletRadiusPx, tabBottom)

            // BOTTOM FILLET CURVATURE:
            // Symmetrical curvature connecting the bottom edge of the tab back into the vertical left edge of the main sheet.
            quadraticTo(
                sheetLeft, tabBottom,
                sheetLeft, tabBottom + filletRadiusPx
            )

            // Left edge of the main sheet down to bottom corner
            lineTo(sheetLeft, size.height - sheetCornerRadiusPx)

            // Bottom-left rounded corner of the main sheet
            quadraticTo(
                sheetLeft, size.height,
                sheetLeft + sheetCornerRadiusPx, size.height
            )

            // Bottom edge to bottom-right
            lineTo(size.width, size.height)

            // Close back to top-right
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun MainAppContainer(
    settingsRepository: SettingsRepository,
    audioRecorderManager: AudioRecorderManager,
    context: android.content.Context
) {
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(ScreenState.CHAT) }
    var settings by remember { mutableStateOf(settingsRepository.loadSettings()) }
    val apiKeys = remember { mutableStateListOf<ApiKeyItem>().apply { addAll(settingsRepository.loadApiKeys()) } }
    val tavilyKeys = remember { mutableStateListOf<ApiKeyItem>().apply { addAll(settingsRepository.loadTavilyApiKeys()) } }
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            addAll(settingsRepository.loadChatMessages())
        }
    }

    var failoverEvent by remember { mutableStateOf<FailoverEvent?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var isConsolidatingMemory by remember { mutableStateOf(false) }
    var activeKeyIndex by remember { mutableStateOf(0) }

    val preTranscribedChunks = remember { Collections.synchronizedList(mutableListOf<String>()) }

    val poolService = remember {
        GeminiPoolService(
            apiKeys = apiKeys.toList(),
            onKeyStatusChanged = { updatedList ->
                coroutineScope.launch(Dispatchers.Main) {
                    apiKeys.clear()
                    apiKeys.addAll(updatedList)
                    settingsRepository.saveApiKeys(updatedList)
                }
            },
            onFailover = { event ->
                coroutineScope.launch(Dispatchers.Main) {
                    failoverEvent = event
                }
            }
        )
    }

    val tavilyPoolService = remember {
        TavilyPoolService(
            tavilyKeys = tavilyKeys.toList(),
            onKeyStatusChanged = { updatedList ->
                coroutineScope.launch(Dispatchers.Main) {
                    tavilyKeys.clear()
                    tavilyKeys.addAll(updatedList)
                    settingsRepository.saveTavilyApiKeys(updatedList)
                }
            },
            onFailover = { event ->
                coroutineScope.launch(Dispatchers.Main) {
                    failoverEvent = event
                }
            }
        )
    }

    val memoryRepository = remember {
        MemoryRepository(
            context = context,
            poolService = poolService
        )
    }
    val allMemories by memoryRepository.allMemories.collectAsStateWithLifecycle(initialValue = emptyList())

    val voicePersonaManager = remember {
        VoicePersonaManager(
            context = context,
            poolService = poolService
        )
    }
    val currentlySpeakingPersonaId by voicePersonaManager.currentlySpeakingPersonaId.collectAsStateWithLifecycle()
    val isLoadingPersonaId by voicePersonaManager.isLoadingPersonaId.collectAsStateWithLifecycle()
    val currentlySpeakingMessageId by voicePersonaManager.currentlySpeakingMessageId.collectAsStateWithLifecycle()

    // Process a user message with unified 3-part streaming (Main Answer, Polished Text, Corrections)
    fun submitMessage(prompt: String, isAudio: Boolean = false, audioBase64: String? = null) {
        val messageId = UUID.randomUUID().toString()
        val initialUserText = if (prompt.isNotBlank() && prompt != "silence") prompt else if (isAudio) "🎤 Spoken Voice Input" else prompt
        val newMessage = ChatMessage(
            id = messageId,
            userText = initialUserText,
            isAudioInput = isAudio,
            aiResponse = "",
            isStreaming = true,
            modelUsed = settings.primaryModel,
            isAnalyzingCorrections = true
        )
        messages.add(newMessage)

        coroutineScope.launch {
            poolService.updateKeys(apiKeys.toList())
            activeKeyIndex = poolService.getActiveKeyIndex()

            var effectivePrompt = prompt
            // If audio was passed without prior transcription, transcribe it immediately
            if ((effectivePrompt.isBlank() || effectivePrompt == "silence") && !audioBase64.isNullOrEmpty()) {
                val transcribeRes = poolService.transcribeAudioChunk(
                    audioBase64 = audioBase64,
                    audioMimeType = "audio/wav",
                    preferredModel = "gemini-flash-lite-latest"
                )
                if (transcribeRes.isSuccess) {
                    val transcribed = transcribeRes.getOrNull()?.trim().orEmpty()
                    if (transcribed.isNotBlank()) {
                        effectivePrompt = transcribed
                        coroutineScope.launch(Dispatchers.Main) {
                            val idx = messages.indexOfFirst { it.id == messageId }
                            if (idx >= 0) {
                                messages[idx] = messages[idx].copy(userText = transcribed)
                            }
                        }
                    }
                }
            }

            // Web search execution logic:
            // Controlled by settings.tavilySearchEnabled (the toggle button next to the speaker button)
            // When ON: EVERYTHING said or typed is processed with web search, NO EXCEPTION.
            // When OFF: NO web search is conducted at all; answer purely from model knowledge regardless of internet.
            var webSearchContext: String? = null
            var didEnhanceWithWeb = false
            var useGoogleSearch = false

            val shouldSearch = settings.tavilySearchEnabled && effectivePrompt.isNotBlank() && effectivePrompt != "silence"

            if (shouldSearch) {
                var foundSearchResults = false
                val recentTopic = messages.lastOrNull { it.id != messageId && it.aiResponse.isNotBlank() }?.let { "${it.userText} ${it.aiResponse.take(250)}" }
                val cleanSearchQuery = TavilyContextDecider.extractEffectiveSearchQuery(effectivePrompt, recentTopic)
                val searchQuery = if (cleanSearchQuery.isNotBlank()) cleanSearchQuery else effectivePrompt

                tavilyPoolService.updateKeys(tavilyKeys.toList())
                val searchResult = try {
                    tavilyPoolService.search(
                        query = searchQuery,
                        thoroughness = settings.tavilyThoroughness,
                        maxResults = settings.tavilyMaxResults
                    )
                } catch (e: Exception) {
                    Log.w("MainActivity", "Web search exception: ${e.message}")
                    null
                }

                if (searchResult != null && searchResult.results.isNotEmpty()) {
                    foundSearchResults = true
                    didEnhanceWithWeb = true
                    webSearchContext = buildString {
                        appendLine("=== LIVE WEB SEARCH CONTEXT (Tabulae / Tavily) ===")
                        appendLine("Search Query: ${searchResult.query}")
                        if (!searchResult.answer.isNullOrBlank()) {
                            appendLine("Synthesized Web Summary: ${searchResult.answer}")
                        }
                        appendLine("Web Sources (${searchResult.results.size}):")
                        searchResult.results.forEachIndexed { i, src ->
                            appendLine("${i + 1}. [${src.title}] (${src.url})")
                            appendLine("   Snippet: ${src.content.take(600)}")
                        }
                        appendLine("=== END LIVE WEB SEARCH CONTEXT ===")
                        appendLine("CRITICAL INSTRUCTION: Live web search data has been retrieved above. Incorporate these fresh facts directly and accurately into your answer. Specifically name the exact titles, releases, dates, headlines, and platforms. DO NOT say 'Let me search' or 'I will search'. Deliver the direct factual answer immediately.")
                    }
                }

                if (!foundSearchResults) {
                    useGoogleSearch = true
                }
            }

            // Retrieve relevant long-term memories if enabled
            val recalledMemories = if (settings.longTermMemoryEnabled && effectivePrompt.isNotBlank()) {
                try {
                    memoryRepository.retrieveRelevantMemories(effectivePrompt, settings.maxRetrievedMemories)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            val memoryContext = if (recalledMemories.isNotEmpty()) {
                HybridMemoryRetriever.formatMemoriesForContext(recalledMemories)
            } else {
                null
            }

            // Pass previous conversation history (only past userText and past aiResponse, NEVER corrections)
            val history = messages.filter { it.id != messageId && it.aiResponse.isNotBlank() }

            val result = poolService.executeChatRequest(
                userText = effectivePrompt,
                audioBase64 = audioBase64,
                audioMimeType = "audio/wav",
                settings = settings,
                conversationHistory = history,
                retrievedMemoriesContext = memoryContext,
                webSearchContext = webSearchContext,
                enableGoogleSearch = if (settings.tavilySearchEnabled) useGoogleSearch else false,
                enableTavilyTool = settings.tavilySearchEnabled,
                tavilySearchTool = if (settings.tavilySearchEnabled) { query ->
                    tavilyPoolService.updateKeys(tavilyKeys.toList())
                    tavilyPoolService.search(
                        query = query,
                        thoroughness = settings.tavilyThoroughness,
                        maxResults = settings.tavilyMaxResults
                    )
                } else null,
                onMainAnswerReceived = { mainAnswer ->
                    coroutineScope.launch(Dispatchers.Main) {
                        val idx = messages.indexOfFirst { it.id == messageId }
                        if (idx >= 0) {
                            messages[idx] = messages[idx].copy(
                                aiResponse = mainAnswer,
                                isStreaming = true,
                                isWebSearchEnhanced = didEnhanceWithWeb
                            )
                        }
                    }
                },
                onPolishedReceived = { polished ->
                    coroutineScope.launch(Dispatchers.Main) {
                        val idx = messages.indexOfFirst { it.id == messageId }
                        if (idx >= 0) {
                            messages[idx] = messages[idx].copy(polishedText = polished)
                        }
                    }
                },
                onCorrectionsReceived = { corrections ->
                    coroutineScope.launch(Dispatchers.Main) {
                        val idx = messages.indexOfFirst { it.id == messageId }
                        if (idx >= 0) {
                            messages[idx] = messages[idx].copy(
                                corrections = corrections,
                                isAnalyzingCorrections = false
                            )
                        }
                    }
                },
                onGroundingDetected = {
                    didEnhanceWithWeb = true
                    coroutineScope.launch(Dispatchers.Main) {
                        val idx = messages.indexOfFirst { it.id == messageId }
                        if (idx >= 0) {
                            messages[idx] = messages[idx].copy(isWebSearchEnhanced = true)
                        }
                    }
                }
            )

            coroutineScope.launch(Dispatchers.Main) {
                activeKeyIndex = poolService.getActiveKeyIndex()
                val finalIdx = messages.indexOfFirst { it.id == messageId }
                if (finalIdx >= 0) {
                    if (result.isSuccess) {
                        val finalData = result.getOrNull()
                        val current = messages[finalIdx]
                        messages[finalIdx] = current.copy(
                            aiResponse = finalData?.mainAnswer?.ifBlank { current.aiResponse } ?: current.aiResponse,
                            userText = current.userText, // Strictly keep original user input with mistakes intact for grammar inspector
                            polishedText = finalData?.polishedText ?: current.polishedText,
                            corrections = if (finalData != null && finalData.corrections.isNotEmpty()) finalData.corrections else current.corrections,
                            isStreaming = false,
                            isAnalyzingCorrections = false,
                            isWebSearchEnhanced = didEnhanceWithWeb,
                            error = null
                        )
                        settingsRepository.saveChatMessages(messages.toList())

                        // Automatic TTS voice synthesis if enabled using dedicated gemini-3.8-flash-lite-tts model
                        if (settings.ttsAutoSpeak && finalData != null && finalData.mainAnswer.isNotBlank()) {
                            val persona = VoicePersonas.findById(settings.selectedVoicePersona)
                            val modelToUse = if (settings.ttsModel.isNotBlank()) settings.ttsModel else "gemini-3.8-flash-lite-tts"
                            voicePersonaManager.speakText(finalData.mainAnswer, persona, modelToUse, coroutineScope, messageId)
                        }

                        // Automatic background long-term memory extraction with multi-turn conversation context
                        if (settings.autoMemoryExtraction && finalData != null && prompt.isNotBlank()) {
                            val recentTurns = messages.takeLast(6).toList()
                            coroutineScope.launch(Dispatchers.IO) {
                                memoryRepository.autoExtractAndSave(
                                    userText = prompt,
                                    aiResponse = finalData.mainAnswer,
                                    recentHistory = recentTurns,
                                    primaryModel = settings.primaryModel
                                )
                            }
                        }
                    } else {
                        messages[finalIdx] = messages[finalIdx].copy(
                            isStreaming = false,
                            isAnalyzingCorrections = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to generate response."
                        )
                        settingsRepository.saveChatMessages(messages.toList())
                    }
                }
            }
        }
    }

    fun startVoiceRecording() {
        // 1. Disable slice-and-send during speech
        audioRecorderManager.onChunkAvailable = null

        val started = audioRecorderManager.startRecording(
            silencePauseSeconds = settings.silencePauseDuration,
            sensitivity = settings.micSensitivity,
            vibrationPattern = settings.vibrationPattern
        )
        if (started) {
            isRecordingAudio = true
        }
    }

    fun stopVoiceRecordingAndSend() {
        if (!isRecordingAudio) return
        val recordingResult = audioRecorderManager.stopRecording()
        isRecordingAudio = false

        if (recordingResult == null || recordingResult.durationMs < 300L) return

        coroutineScope.launch {
            // 2. Exactly ONE call to gemini-flash-lite-latest
            val transcribeRes = poolService.transcribeAudioChunk(
                audioBase64 = recordingResult.fullAudioBase64,
                audioMimeType = "audio/wav",
                preferredModel = "gemini-flash-lite-latest"
            )

            val resolvedTranscript = transcribeRes.getOrNull()?.trim().orEmpty()

            coroutineScope.launch(Dispatchers.Main) {
                if (resolvedTranscript.isNotBlank()) {
                    submitMessage(prompt = resolvedTranscript, isAudio = true)
                } else {
                    submitMessage(prompt = "silence", isAudio = true)
                }
            }
        }
    }

    fun applyCorrection(targetMessage: ChatMessage, correction: LinguisticCorrection) {
        val idx = messages.indexOfFirst { it.id == targetMessage.id }
        if (idx >= 0) {
            val current = messages[idx]
            val updatedText = current.userText.replaceFirst(correction.original, correction.suggested)
            val updatedCorrections = current.corrections.filter { it.id != correction.id }
            messages[idx] = current.copy(
                userText = updatedText,
                corrections = updatedCorrections
            )
            settingsRepository.saveChatMessages(messages.toList())
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecording()
        }
    }

    fun handleToggleVoiceRecording() {
        if (isRecordingAudio) {
            stopVoiceRecordingAndSend()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val drawerFraction = remember { Animatable(0f) }

    BackHandler(enabled = currentScreen == ScreenState.MEMORY_VAULT) {
        currentScreen = ScreenState.CHAT
    }

    BackHandler(enabled = currentScreen == ScreenState.CHAT && drawerFraction.value > 0.05f) {
        audioRecorderManager.stopVibration()
        coroutineScope.launch {
            drawerFraction.animateTo(0f, tween(260))
        }
    }

    fun handleConsolidateMemory() {
        if (messages.isEmpty() || isConsolidatingMemory) return
        isConsolidatingMemory = true
        coroutineScope.launch {
            try {
                val sessionList = messages.toList()
                memoryRepository.consolidateSessionAndSave(
                    session = sessionList,
                    primaryModel = settings.primaryModel
                )
                messages.clear()
                settingsRepository.saveChatMessages(emptyList())
            } finally {
                isConsolidatingMemory = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF090A0C)
    ) {
        if (currentScreen == ScreenState.MEMORY_VAULT) {
            MemoryVaultScreen(
                memories = allMemories,
                onAddMemory = { content, category, importance, keywords ->
                    coroutineScope.launch {
                        memoryRepository.addMemoryNote(
                            content = content,
                            category = category,
                            importance = importance,
                            keywords = keywords
                        )
                    }
                },
                onUpdateMemory = { memory ->
                    coroutineScope.launch {
                        memoryRepository.updateMemory(memory)
                    }
                },
                onDeleteMemory = { id ->
                    coroutineScope.launch {
                        memoryRepository.deleteMemory(id)
                    }
                },
                onClearAllMemories = {
                    coroutineScope.launch {
                        memoryRepository.clearAll()
                    }
                },
                onSimulateRetrieval = { query ->
                    memoryRepository.simulateRetrieval(query)
                },
                onBack = { currentScreen = ScreenState.CHAT }
            )
        } else {
            // Main Chat with Sliding Drawer from Right to Left
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidthDp = maxWidth
                val screenWidthPx = constraints.maxWidth.toFloat()

                // Base Layer: Chat Screen
                ChatScreen(
                    messages = messages,
                    settings = settings,
                    apiKeys = apiKeys,
                    activeKeyIndex = activeKeyIndex,
                    failoverEvent = failoverEvent,
                    isRecordingAudio = isRecordingAudio,
                    isConsolidatingMemory = isConsolidatingMemory,
                    onConsolidateMemory = { handleConsolidateMemory() },
                    onStartAudioRecording = { handleToggleVoiceRecording() },
                    onStopAudioRecording = { stopVoiceRecordingAndSend() },
                    isTtsEnabled = settings.ttsAutoSpeak,
                    onToggleTts = {
                        val nextState = !settings.ttsAutoSpeak
                        val updated = settings.copy(
                            ttsAutoSpeak = nextState,
                            dedicatedTtsModelActive = nextState
                        )
                        settings = updated
                        settingsRepository.saveSettings(updated)
                        // If toggling off, or if speech is currently playing, stop audio immediately at this exact moment
                        if (!nextState || currentlySpeakingMessageId != null || currentlySpeakingPersonaId != null) {
                            voicePersonaManager.stop()
                        }
                    },
                    isWebSearchEnabled = settings.tavilySearchEnabled,
                    onToggleWebSearch = {
                        val nextState = !settings.tavilySearchEnabled
                        val updated = settings.copy(tavilySearchEnabled = nextState)
                        settings = updated
                        settingsRepository.saveSettings(updated)
                    },
                    onSendMessage = { text -> submitMessage(text) },
                    onOpenSettings = {
                        coroutineScope.launch {
                            drawerFraction.animateTo(1f, tween(320))
                        }
                    },
                    onClearChat = {
                        messages.clear()
                        settingsRepository.saveChatMessages(emptyList())
                    },
                    onDismissFailover = { failoverEvent = null },
                    onRetryMessage = { chatMsg ->
                        submitMessage(chatMsg.userText, isAudio = chatMsg.isAudioInput)
                    },
                    onApplyCorrection = { msg, corr ->
                        applyCorrection(msg, corr)
                    },
                    speakingMessageId = currentlySpeakingMessageId
                )

                // Unified Settings Drawer & Integrated Bookmark Notch (Single Layer)
                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val notchHeight = 38.dp
                val notchProtrusion = 38.dp
                val filletRadius = 12.dp
                val sheetCornerRadius = 28.dp

                val density = LocalDensity.current
                val protrusionPx = with(density) { notchProtrusion.toPx() }
                val tabTopPx = with(density) { (statusBarTop + 8.dp).toPx() }
                val tabHeightPx = with(density) { notchHeight.toPx() }
                val filletRadiusPx = with(density) { filletRadius.toPx() }
                val sheetCornerRadiusPx = with(density) { sheetCornerRadius.toPx() }

                val bookmarkDrawerShape = remember(protrusionPx, tabTopPx, tabHeightPx, filletRadiusPx, sheetCornerRadiusPx) {
                    BookmarkDrawerShape(
                        protrusionPx = protrusionPx,
                        tabTopPx = tabTopPx,
                        tabHeightPx = tabHeightPx,
                        filletRadiusPx = filletRadiusPx,
                        sheetCornerRadiusPx = sheetCornerRadiusPx
                    )
                }

                val dragTravelPx = screenWidthPx.coerceAtLeast(1f)
                val containerOffsetX = (-protrusionPx + (dragTravelPx * (1f - drawerFraction.value))).roundToInt()

                // Semi-transparent scrim behind drawer
                if (drawerFraction.value > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = (drawerFraction.value * 0.45f).coerceIn(0f, 0.6f)))
                            .clickable {
                                audioRecorderManager.stopVibration()
                                voicePersonaManager.stop()
                                coroutineScope.launch {
                                    drawerFraction.animateTo(0f, tween(260))
                                }
                            }
                    )
                }

                // Full-height right edge drag area on base screen (strictly drag, no click)
                if (drawerFraction.value < 0.99f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(44.dp)
                            .pointerInput(dragTravelPx) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        val delta = -dragAmount / dragTravelPx
                                        coroutineScope.launch {
                                            drawerFraction.snapTo((drawerFraction.value + delta).coerceIn(0f, 1f))
                                        }
                                    },
                                    onDragEnd = {
                                        coroutineScope.launch {
                                            if (drawerFraction.value > 0.20f) {
                                                drawerFraction.animateTo(1f, tween(280))
                                            } else {
                                                drawerFraction.animateTo(0f, tween(240))
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            if (drawerFraction.value > 0.20f) {
                                                drawerFraction.animateTo(1f, tween(280))
                                            } else {
                                                drawerFraction.animateTo(0f, tween(240))
                                            }
                                        }
                                    }
                                )
                            }
                    )
                }

                // Single-layer Drawer & Bookmark Tab Container
                // Unifies the Settings Drawer sheet and the bookmark notch into ONE physical layer
                // with smooth symmetrical fillet curvature transitions on both the top and bottom sides.
                // Using requiredWidth forces Compose to ignore the screen constraint so the main sheet extends completely to the right edge with zero gap.
                Box(
                    modifier = Modifier
                        .requiredWidth(screenWidthDp + notchProtrusion)
                        .fillMaxHeight()
                        .offset { IntOffset(containerOffsetX, 0) }
                        .shadow(
                            elevation = 16.dp,
                            shape = bookmarkDrawerShape
                        )
                        .clip(bookmarkDrawerShape)
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE4E4E7),
                            shape = bookmarkDrawerShape
                        )
                        .pointerInput(dragTravelPx) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = -dragAmount / dragTravelPx
                                    coroutineScope.launch {
                                        drawerFraction.snapTo((drawerFraction.value + delta).coerceIn(0f, 1f))
                                    }
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (drawerFraction.value > 0.35f) {
                                            drawerFraction.animateTo(1f, tween(280))
                                        } else {
                                            drawerFraction.animateTo(0f, tween(240))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        if (drawerFraction.value > 0.35f) {
                                            drawerFraction.animateTo(1f, tween(280))
                                        } else {
                                            drawerFraction.animateTo(0f, tween(240))
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    // 1. Settings Screen Content inside the main sheet area
                    Box(
                        modifier = Modifier
                            .width(screenWidthDp)
                            .fillMaxHeight()
                            .offset(x = notchProtrusion)
                    ) {
                        SettingsScreen(
                            currentSettings = settings,
                            apiKeys = apiKeys,
                            tavilyApiKeys = tavilyKeys,
                            memoryCount = allMemories.size,
                            onOpenMemoryVault = { currentScreen = ScreenState.MEMORY_VAULT },
                            onSaveSettings = { updated ->
                                settings = updated
                                settingsRepository.saveSettings(updated)
                            },
                            onAddKey = { newKey ->
                                val newItem = ApiKeyItem(
                                    key = newKey,
                                    label = "Key #${apiKeys.size + 1}"
                                )
                                apiKeys.add(newItem)
                                settingsRepository.saveApiKeys(apiKeys.toList())
                                poolService.updateKeys(apiKeys.toList())
                            },
                            onBulkAddKeys = { keysList ->
                                val currentSize = apiKeys.size
                                val newItems = keysList.mapIndexed { idx, k ->
                                    ApiKeyItem(
                                        key = k,
                                        label = "Key #${currentSize + idx + 1}"
                                    )
                                }
                                apiKeys.addAll(newItems)
                                settingsRepository.saveApiKeys(apiKeys.toList())
                                poolService.updateKeys(apiKeys.toList())
                            },
                            onDeleteKey = { keyId ->
                                apiKeys.removeAll { it.id == keyId }
                                settingsRepository.saveApiKeys(apiKeys.toList())
                                poolService.updateKeys(apiKeys.toList())
                            },
                            onAddTavilyKey = { newKey ->
                                val newItem = ApiKeyItem(
                                    key = newKey,
                                    label = "Tavily #${tavilyKeys.size + 1}"
                                )
                                tavilyKeys.add(newItem)
                                settingsRepository.saveTavilyApiKeys(tavilyKeys.toList())
                                tavilyPoolService.updateKeys(tavilyKeys.toList())
                            },
                            onBulkAddTavilyKeys = { keysList ->
                                val currentSize = tavilyKeys.size
                                val newItems = keysList.mapIndexed { idx, k ->
                                    ApiKeyItem(
                                        key = k,
                                        label = "Tavily #${currentSize + idx + 1}"
                                    )
                                }
                                tavilyKeys.addAll(newItems)
                                settingsRepository.saveTavilyApiKeys(tavilyKeys.toList())
                                tavilyPoolService.updateKeys(tavilyKeys.toList())
                            },
                            onDeleteTavilyKey = { keyId ->
                                tavilyKeys.removeAll { it.id == keyId }
                                settingsRepository.saveTavilyApiKeys(tavilyKeys.toList())
                                tavilyPoolService.updateKeys(tavilyKeys.toList())
                            },
                            onResetDefaults = {
                                val def = AppSettings()
                                settings = def
                                settingsRepository.saveSettings(def)
                            },
                            onPreviewVibration = { patternId ->
                                audioRecorderManager.previewPattern(patternId)
                            },
                            onStopPreviewVibration = {
                                audioRecorderManager.stopVibration()
                            },
                            onPreviewVoicePersona = { personaId ->
                                val persona = VoicePersonas.findById(personaId)
                                val modelToUse = if (settings.ttsModel.isNotBlank()) settings.ttsModel else "gemini-3.8-flash-lite-tts"
                                voicePersonaManager.previewPersona(persona, modelToUse, coroutineScope)
                            },
                            onStopPreviewVoicePersona = {
                                voicePersonaManager.stop()
                            },
                            currentlySpeakingPersonaId = currentlySpeakingPersonaId,
                            isLoadingPersonaId = isLoadingPersonaId,
                            onClose = {
                                audioRecorderManager.stopVibration()
                                voicePersonaManager.stop()
                                coroutineScope.launch {
                                    drawerFraction.animateTo(0f, tween(280))
                                }
                            }
                        )
                    }

                    // 2. Integrated Bookmark Tab Gear Button
                    // Located seamlessly inside the bookmark notch area on the left edge
                    Box(
                        modifier = Modifier
                            .padding(top = statusBarTop + 8.dp)
                            .size(width = notchProtrusion, height = notchHeight)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                coroutineScope.launch {
                                    if (drawerFraction.value < 0.5f) {
                                        drawerFraction.animateTo(1f, tween(320))
                                    } else {
                                        drawerFraction.animateTo(0f, tween(260))
                                    }
                                }
                            }
                            .testTag("settings_gear_notch"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Notch",
                            tint = Color(0xFF18181B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
