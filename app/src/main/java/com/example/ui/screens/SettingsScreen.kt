package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.VibrationPatterns
import com.example.data.ApiKeyItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.AppSettings
import com.example.data.KeyStatus

enum class SettingsCategory(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    CHAT_DISPLAY(
        title = "Chat & Display",
        description = "Token streaming, font scaling & display options",
        icon = Icons.AutoMirrored.Filled.Chat
    ),
    MODELS_REASONING(
        title = "AI Models & Thinking",
        description = "Primary Gemini tiers, reasoning depth & grammar",
        icon = Icons.Default.Tune
    ),
    GEMINI_TTS_VOICE(
        title = "Gemini 3.8 Flash-Lite TTS & Voice Changer",
        description = "gemini-3.8-flash-lite-tts (no fallback) & 20 unique AI voice personas",
        icon = Icons.Default.GraphicEq
    ),
    VOICE_INPUT(
        title = "Voice & Microphone",
        description = "Sensitivity, silence pause & 10 unique vibration patterns",
        icon = Icons.Default.Mic
    ),
    LONG_TERM_MEMORY(
        title = "Long-Term Memory",
        description = "Memory vault notes, background extraction & RAG",
        icon = Icons.Default.Psychology
    ),
    API_KEYS(
        title = "API Keys & Pool",
        description = "Failover key pool, add keys & bulk configuration",
        icon = Icons.Default.Key
    ),
    WEB_SEARCH(
        title = "Web Search & Tavily",
        description = "Context-aware live search, thoroughness depth & sources",
        icon = Icons.Default.Public
    )
}

/**
 * Categorized Settings Screen strictly styled in combinations of White, Black, and Grays.
 * Clean category cards lead to focused sub-pages so everything is organized and tidy.
 */
@Composable
fun SettingsScreen(
    currentSettings: AppSettings,
    apiKeys: List<ApiKeyItem>,
    tavilyApiKeys: List<ApiKeyItem> = emptyList(),
    memoryCount: Int = 0,
    onOpenMemoryVault: () -> Unit = {},
    onSaveSettings: (AppSettings) -> Unit,
    onAddKey: (String) -> Unit,
    onBulkAddKeys: (List<String>) -> Unit,
    onDeleteKey: (String) -> Unit,
    onAddTavilyKey: (String) -> Unit = {},
    onBulkAddTavilyKeys: (List<String>) -> Unit = {},
    onDeleteTavilyKey: (String) -> Unit = {},
    onResetDefaults: () -> Unit,
    onPreviewVibration: ((String) -> Unit)? = null,
    onStopPreviewVibration: (() -> Unit)? = null,
    onPreviewVoicePersona: ((String) -> Unit)? = null,
    onStopPreviewVoicePersona: (() -> Unit)? = null,
    currentlySpeakingPersonaId: String? = null,
    isLoadingPersonaId: String? = null,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    var primaryModel by remember { mutableStateOf(currentSettings.primaryModel) }
    var nonThinkingMode by remember { mutableStateOf(currentSettings.nonThinkingMode) }
    var tokenStreaming by remember { mutableStateOf(currentSettings.tokenStreaming) }
    var transcriptionModel by remember { mutableStateOf(currentSettings.transcriptionModel) }
    var contextualSmoothing by remember { mutableStateOf(currentSettings.contextualSmoothing) }
    var grammarInspector by remember { mutableStateOf(currentSettings.grammarInspector) }
    var micSensitivity by remember { mutableStateOf(currentSettings.micSensitivity) }
    var silencePauseDuration by remember { mutableFloatStateOf(currentSettings.silencePauseDuration) }
    var minVoiceHoldDurationSeconds by remember { mutableFloatStateOf(currentSettings.minVoiceHoldDurationSeconds) }
    var selectedVibrationPattern by remember { mutableStateOf(currentSettings.vibrationPattern) }
    var previewingPatternId by remember { mutableStateOf<String?>(null) }
    var maxFontSizeSp by remember { mutableIntStateOf(currentSettings.maxFontSizeSp) }
    var longTermMemoryEnabled by remember { mutableStateOf(currentSettings.longTermMemoryEnabled) }
    var autoMemoryExtraction by remember { mutableStateOf(currentSettings.autoMemoryExtraction) }
    var maxRetrievedMemories by remember { mutableIntStateOf(currentSettings.maxRetrievedMemories) }
    var ttsModel by remember { mutableStateOf(currentSettings.ttsModel) }
    var selectedVoicePersona by remember { mutableStateOf(currentSettings.selectedVoicePersona) }
    var ttsAutoSpeak by remember { mutableStateOf(currentSettings.ttsAutoSpeak) }
    var dedicatedTtsModelActive by remember { mutableStateOf(currentSettings.dedicatedTtsModelActive) }
    var tavilySearchEnabled by remember { mutableStateOf(currentSettings.tavilySearchEnabled) }
    var tavilyThoroughness by remember { mutableStateOf(currentSettings.tavilyThoroughness) }
    var tavilyMaxResults by remember { mutableIntStateOf(currentSettings.tavilyMaxResults) }

    LaunchedEffect(currentSettings) {
        primaryModel = currentSettings.primaryModel
        nonThinkingMode = currentSettings.nonThinkingMode
        tokenStreaming = currentSettings.tokenStreaming
        transcriptionModel = currentSettings.transcriptionModel
        contextualSmoothing = currentSettings.contextualSmoothing
        grammarInspector = currentSettings.grammarInspector
        micSensitivity = currentSettings.micSensitivity
        silencePauseDuration = currentSettings.silencePauseDuration
        selectedVibrationPattern = currentSettings.vibrationPattern
        maxFontSizeSp = currentSettings.maxFontSizeSp
        longTermMemoryEnabled = currentSettings.longTermMemoryEnabled
        autoMemoryExtraction = currentSettings.autoMemoryExtraction
        maxRetrievedMemories = currentSettings.maxRetrievedMemories
        minVoiceHoldDurationSeconds = currentSettings.minVoiceHoldDurationSeconds
        ttsModel = currentSettings.ttsModel
        selectedVoicePersona = currentSettings.selectedVoicePersona
        ttsAutoSpeak = currentSettings.ttsAutoSpeak
        dedicatedTtsModelActive = currentSettings.dedicatedTtsModelActive
        tavilySearchEnabled = currentSettings.tavilySearchEnabled
        tavilyThoroughness = currentSettings.tavilyThoroughness
        tavilyMaxResults = currentSettings.tavilyMaxResults
    }

    var newKeyInput by remember { mutableStateOf("") }
    var newTavilyKeyInput by remember { mutableStateOf("") }
    var showBulkDialog by remember { mutableStateOf(false) }
    var bulkKeysInput by remember { mutableStateOf("") }
    var apiKeyTab by remember { mutableIntStateOf(0) } // 0 = AI Studio APIs, 1 = Tavily APIs

    fun syncAndSave() {
        onSaveSettings(
            currentSettings.copy(
                primaryModel = primaryModel,
                nonThinkingMode = nonThinkingMode,
                tokenStreaming = tokenStreaming,
                transcriptionModel = transcriptionModel,
                contextualSmoothing = contextualSmoothing,
                grammarInspector = grammarInspector,
                micSensitivity = micSensitivity,
                silencePauseDuration = silencePauseDuration,
                vibrationPattern = selectedVibrationPattern,
                maxFontSizeSp = maxFontSizeSp,
                longTermMemoryEnabled = longTermMemoryEnabled,
                autoMemoryExtraction = autoMemoryExtraction,
                maxRetrievedMemories = maxRetrievedMemories,
                minVoiceHoldDurationSeconds = minVoiceHoldDurationSeconds,
                ttsModel = ttsModel,
                selectedVoicePersona = selectedVoicePersona,
                ttsAutoSpeak = ttsAutoSpeak,
                dedicatedTtsModelActive = dedicatedTtsModelActive,
                tavilySearchEnabled = tavilySearchEnabled,
                tavilyThoroughness = tavilyThoroughness,
                tavilyMaxResults = tavilyMaxResults
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCategory != null) {
                IconButton(
                    onClick = { selectedCategory = null },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF18181B)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedCategory?.title ?: "Settings",
                    color = Color(0xFF18181B),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (selectedCategory != null) "Customize selected options" else "Select a category to customize",
                    color = Color(0xFF71717A),
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("close_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF18181B)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE4E4E7))
        )

        // Main Animated Content: Hub vs Category View
        AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "CategoryTransition",
            modifier = Modifier.weight(1f)
        ) { category ->
            if (category == null) {
                // CATEGORIES HUB
                val hubScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(hubScroll)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "PREFERENCES & OPTIONS",
                        color = Color(0xFF71717A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // 5 Categories Cards
                    SettingsCategory.values().forEach { cat ->
                        CategoryHubCard(
                            category = cat,
                            onClick = { selectedCategory = cat }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reset Defaults Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFD4D4D8), RoundedCornerShape(12.dp))
                            .clickable {
                                onResetDefaults()
                                primaryModel = "gemini-flash-latest"
                                nonThinkingMode = true
                                tokenStreaming = true
                                transcriptionModel = "gemini-flash-lite-latest"
                                contextualSmoothing = true
                                grammarInspector = true
                                micSensitivity = "Low"
                                silencePauseDuration = 2.0f
                                maxFontSizeSp = 24
                                longTermMemoryEnabled = true
                                autoMemoryExtraction = true
                                maxRetrievedMemories = 4
                                syncAndSave()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = Color(0xFF71717A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reset All Settings to Defaults",
                                color = Color(0xFF3F3F46),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // CATEGORY DETAIL SUB-PAGE
                val detailScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(detailScroll)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (category) {
                        SettingsCategory.CHAT_DISPLAY -> {
                            MonochromeToggle(
                                title = "Token Streaming",
                                description = "Stream response tokens in real-time as they generate for fast visual emergence.",
                                checked = tokenStreaming,
                                onCheckedChange = {
                                    tokenStreaming = it
                                    syncAndSave()
                                },
                                testTag = "toggle_token_streaming"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF4F4F5))
                                    .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Maximum Font Scale",
                                        color = Color(0xFF18181B),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${maxFontSizeSp}sp",
                                        color = Color(0xFF18181B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Auto-shrinks dynamic typography smoothly to keep complete thoughts in view.",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                                )
                                Slider(
                                    value = maxFontSizeSp.toFloat(),
                                    onValueChange = {
                                        maxFontSizeSp = it.toInt()
                                        syncAndSave()
                                    },
                                    valueRange = 18f..48f,
                                    steps = 14,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF18181B),
                                        activeTrackColor = Color(0xFF18181B),
                                        inactiveTrackColor = Color(0xFFD4D4D8)
                                    )
                                )
                            }
                        }

                        SettingsCategory.MODELS_REASONING -> {
                            Text(
                                text = "Primary Chat Model & Reasoning Tier",
                                color = Color(0xFF18181B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select model family and thinking budget. None provides minimal latency; Low enables deliberate reasoning.",
                                color = Color(0xFF71717A),
                                fontSize = 12.sp
                            )

                            val modelChoices = listOf(
                                Triple("Gemini Flash (Fast)", "gemini-flash-latest", true),
                                Triple("Gemini Flash (Thinking)", "gemini-flash-latest", false),
                                Triple("Gemini Pro (Smart)", "gemini-pro-latest", false),
                                Triple("Gemini Flash-Lite", "gemini-flash-lite-latest", true)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                modelChoices.chunked(2).forEach { rowChoices ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowChoices.forEach { (label, modelKey, isNonThinking) ->
                                            val isSelected = primaryModel == modelKey && nonThinkingMode == isNonThinking
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) Color(0xFF18181B) else Color(0xFFE4E4E7),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        primaryModel = modelKey
                                                        nonThinkingMode = isNonThinking
                                                        syncAndSave()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.White else Color(0xFF3F3F46),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Dedicated Audio Engine informative badge
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF4F4F5))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "Dedicated Audio Engine",
                                    color = Color(0xFF18181B),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Always using: gemini-flash-lite-latest\nFast, cost-efficient speech-to-text with generous quota limits.",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            MonochromeToggle(
                                title = "Contextual Smoothing",
                                description = "Joins speech transcription chunks and smooths punctuation without altering your original words or grammar mistakes.",
                                checked = contextualSmoothing,
                                onCheckedChange = {
                                    contextualSmoothing = it
                                    syncAndSave()
                                },
                                testTag = "toggle_contextual_smoothing"
                            )

                            MonochromeToggle(
                                title = "Grammar & Word Choice Inspector",
                                description = "Inspects inputs for grammar suggestions with interactive tap-to-fix modal.",
                                checked = grammarInspector,
                                onCheckedChange = {
                                    grammarInspector = it
                                    syncAndSave()
                                },
                                testTag = "toggle_grammar_inspector"
                            )
                        }

                        SettingsCategory.GEMINI_TTS_VOICE -> {
                            MonochromeToggle(
                                title = "Gemini 3.8 Flash-Lite TTS",
                                description = "Authentic voice synthesis & transcription engine. Activating this enables speak aloud across all incoming responses using your chosen persona.",
                                checked = dedicatedTtsModelActive,
                                onCheckedChange = { active ->
                                    dedicatedTtsModelActive = active
                                    ttsAutoSpeak = active
                                    syncAndSave()
                                },
                                testTag = "toggle_dedicated_tts"
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Voice Changer • 20 Distinct Personas",
                                color = Color(0xFF18181B),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose how she or he speaks and responds. Tap 'Try' next to each option to feel the voice, and tap the card to select your voice persona.",
                                color = Color(0xFF71717A),
                                fontSize = 12.sp
                            )

                            // 20 Personas List
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                com.example.audio.VoicePersonas.ALL_PERSONAS.forEach { persona ->
                                    val isSelected = selectedVoicePersona == persona.id
                                    val isPreviewingThis = currentlySpeakingPersonaId == persona.id

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color(0xFF18181B) else Color(0xFFE4E4E7),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                selectedVoicePersona = persona.id
                                                syncAndSave()
                                            }
                                            .padding(14.dp)
                                            .testTag("persona_card_${persona.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Radio status indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else Color.Transparent)
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (isSelected) Color.White else Color(0xFF71717A),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color(0xFF18181B),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${persona.name} • ${persona.title}",
                                                    color = if (isSelected) Color.White else Color(0xFF18181B),
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 2.dp)
                                                        .clip(RoundedCornerShape(5.dp))
                                                        .background(if (isSelected) Color(0xFF27272A) else Color(0xFFE4E4E7))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = persona.style,
                                                        color = if (isSelected) Color(0xFFF4F4F5) else Color(0xFF3F3F46),
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            val isLoadingThis = isLoadingPersonaId == persona.id

                                            // "Try" Button (synthesizes/plays real Gemini AI model audio)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isPreviewingThis && !isLoadingThis) {
                                                             if (isSelected) Color.White else Color(0xFF18181B)
                                                        } else {
                                                             if (isSelected) Color(0xFF27272A) else Color.White
                                                        }
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) Color(0xFF52525B) else Color(0xFFD4D4D8),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        if (isPreviewingThis) {
                                                            onStopPreviewVoicePersona?.invoke()
                                                        } else {
                                                            onPreviewVoicePersona?.invoke(persona.id)
                                                        }
                                                    }
                                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                                                    .testTag("try_persona_${persona.id}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (isLoadingThis) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(12.dp),
                                                            color = if (isSelected) Color.White else Color(0xFF18181B),
                                                            strokeWidth = 1.5.dp
                                                        )
                                                        Text(
                                                            text = "Synthesizing…",
                                                            color = if (isSelected) Color.White else Color(0xFF18181B),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = if (isPreviewingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                            contentDescription = if (isPreviewingThis) "Stop" else "Try",
                                                            tint = if (isPreviewingThis) {
                                                                if (isSelected) Color(0xFF18181B) else Color.White
                                                            } else {
                                                                if (isSelected) Color.White else Color(0xFF18181B)
                                                            },
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = if (isPreviewingThis) "Stop" else "Try",
                                                            color = if (isPreviewingThis) {
                                                                if (isSelected) Color(0xFF18181B) else Color.White
                                                            } else {
                                                                if (isSelected) Color.White else Color(0xFF18181B)
                                                            },
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = persona.explanation,
                                            color = if (isSelected) Color(0xFFD4D4D8) else Color(0xFF52525B),
                                            fontSize = 12.sp,
                                            lineHeight = 16.5.sp,
                                            modifier = Modifier.padding(start = 30.dp)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "\"${persona.sampleText}\"",
                                            color = if (isSelected) Color(0xFFA1A1AA) else Color(0xFF71717A),
                                            fontSize = 11.5.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.padding(start = 30.dp)
                                        )
                                    }
                                }
                            }
                        }

                        SettingsCategory.VOICE_INPUT -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF4F4F5))
                                    .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Microphone Sensitivity",
                                        color = Color(0xFF18181B),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = micSensitivity,
                                        color = Color(0xFF18181B),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Threshold for separating speech from background silence.",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("High", "Normal", "Low").forEach { level ->
                                        val isSelected = micSensitivity == level
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFF18181B) else Color.White)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF18181B) else Color(0xFFD4D4D8),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    micSensitivity = level
                                                    syncAndSave()
                                                }
                                                .padding(vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = level,
                                                color = if (isSelected) Color.White else Color(0xFF3F3F46),
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF4F4F5))
                                    .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Silence Pause Duration",
                                        color = Color(0xFF18181B),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1fs", silencePauseDuration),
                                        color = Color(0xFF18181B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Seconds of continuous quiet before chunking live audio for background transcription.",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                                )
                                Slider(
                                    value = silencePauseDuration,
                                    onValueChange = {
                                        silencePauseDuration = it
                                        syncAndSave()
                                    },
                                    valueRange = 0.8f..4.0f,
                                    steps = 15,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF18181B),
                                        activeTrackColor = Color(0xFF18181B),
                                        inactiveTrackColor = Color(0xFFD4D4D8)
                                    )
                                )
                            }

                            // Minimum Voice Hold Duration (Unintended hold filter: 1s to 5s)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF4F4F5))
                                    .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Minimum Voice Hold Duration",
                                        color = Color(0xFF18181B),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${minVoiceHoldDurationSeconds.toInt()}s",
                                        color = Color(0xFF18181B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Holds shorter than this threshold (1-5s) are treated as unintended touches and will be rejected/ignored without creating a voice message.",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(1f, 2f, 3f, 4f, 5f).forEach { sec ->
                                        val isSelected = (minVoiceHoldDurationSeconds.toInt() == sec.toInt())
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFF18181B) else Color.White)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF18181B) else Color(0xFFD4D4D8),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    minVoiceHoldDurationSeconds = sec
                                                    syncAndSave()
                                                }
                                                .padding(vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${sec.toInt()}s",
                                                color = if (isSelected) Color.White else Color(0xFF3F3F46),
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            // 10 Unique Vibration Patterns with Personas and Try Buttons
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color(0xFF18181B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Recording Vibration Patterns",
                                        color = Color(0xFF18181B),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "10 unique and complex haptic personas. Tap 'Try' to feel any pattern, and tap a card to make it your active vibration.",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )
                            }

                            VibrationPatterns.ALL.forEach { pattern ->
                                val isSelected = selectedVibrationPattern == pattern.id
                                val isPreviewingThis = previewingPatternId == pattern.id

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) Color(0xFF18181B) else Color(0xFFF4F4F5))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF18181B) else Color(0xFFE4E4E7),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            selectedVibrationPattern = pattern.id
                                            syncAndSave()
                                        }
                                        .padding(14.dp)
                                        .testTag("vibration_card_${pattern.id}")
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Header Row: Selector icon + Name + Persona Badge + Try Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Radio status indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else Color.Transparent)
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (isSelected) Color.White else Color(0xFF71717A),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color(0xFF18181B),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = pattern.name,
                                                        color = if (isSelected) Color.White else Color(0xFF18181B),
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                // Persona badge
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 2.dp)
                                                        .clip(RoundedCornerShape(5.dp))
                                                        .background(if (isSelected) Color(0xFF27272A) else Color(0xFFE4E4E7))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = pattern.persona,
                                                        color = if (isSelected) Color(0xFFF4F4F5) else Color(0xFF3F3F46),
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // "Try" Button
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isPreviewingThis) {
                                                            if (isSelected) Color.White else Color(0xFF18181B)
                                                        } else {
                                                            if (isSelected) Color(0xFF27272A) else Color.White
                                                        }
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) Color(0xFF52525B) else Color(0xFFD4D4D8),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        if (isPreviewingThis) {
                                                            onStopPreviewVibration?.invoke()
                                                            previewingPatternId = null
                                                        } else {
                                                            previewingPatternId = pattern.id
                                                            onPreviewVibration?.invoke(pattern.id)
                                                            coroutineScope.launch {
                                                                delay(2600L)
                                                                if (previewingPatternId == pattern.id) {
                                                                    previewingPatternId = null
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                                                    .testTag("try_vibration_${pattern.id}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPreviewingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                        contentDescription = if (isPreviewingThis) "Stop" else "Try",
                                                        tint = if (isPreviewingThis) {
                                                            if (isSelected) Color(0xFF18181B) else Color.White
                                                        } else {
                                                            if (isSelected) Color.White else Color(0xFF18181B)
                                                        },
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = if (isPreviewingThis) "Stop" else "Try",
                                                        color = if (isPreviewingThis) {
                                                            if (isSelected) Color(0xFF18181B) else Color.White
                                                        } else {
                                                            if (isSelected) Color.White else Color(0xFF18181B)
                                                        },
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Explanation
                                        Text(
                                            text = pattern.explanation,
                                            color = if (isSelected) Color(0xFFD4D4D8) else Color(0xFF52525B),
                                            fontSize = 12.sp,
                                            lineHeight = 16.5.sp,
                                            modifier = Modifier.padding(start = 30.dp)
                                        )
                                    }
                                }
                            }
                        }

                        SettingsCategory.LONG_TERM_MEMORY -> {
                            // Vault entry card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF18181B))
                                    .clickable { onOpenMemoryVault() }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Open Memory Vault",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "View, search and edit $memoryCount long-term notes",
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            MonochromeToggle(
                                title = "Enable Long-Term Memory",
                                description = "Injects retrieved high-relevance memories into context window for deep personalization.",
                                checked = longTermMemoryEnabled,
                                onCheckedChange = {
                                    longTermMemoryEnabled = it
                                    syncAndSave()
                                },
                                testTag = "toggle_long_term_memory"
                            )

                            MonochromeToggle(
                                title = "Auto-Extract from Chats",
                                description = "Extracts and consolidates user facts, projects, and preferences into atomic notes.",
                                checked = autoMemoryExtraction,
                                onCheckedChange = {
                                    autoMemoryExtraction = it
                                    syncAndSave()
                                },
                                testTag = "toggle_auto_memory_extraction"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF4F4F5))
                                    .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Max Memories per Query",
                                        color = Color(0xFF18181B),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$maxRetrievedMemories notes",
                                        color = Color(0xFF18181B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Maximum top atomic notes injected into prompt via Hybrid Vector RAG.",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                                )
                                Slider(
                                    value = maxRetrievedMemories.toFloat(),
                                    onValueChange = {
                                        maxRetrievedMemories = it.toInt()
                                        syncAndSave()
                                    },
                                    valueRange = 1f..8f,
                                    steps = 6,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF18181B),
                                        activeTrackColor = Color(0xFF18181B),
                                        inactiveTrackColor = Color(0xFFD4D4D8)
                                    )
                                )
                            }
                        }

                        SettingsCategory.API_KEYS -> {
                            Text(
                                text = "API Keys & Pooling",
                                color = Color(0xFF18181B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pool automatically fails over across keys if rate limits or quota errors occur.",
                                color = Color(0xFF71717A),
                                fontSize = 12.sp
                            )

                            // Segmented Tabs: AI Studio APIs vs Tavily APIs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF4F4F5))
                                    .padding(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (apiKeyTab == 0) Color(0xFF18181B) else Color.Transparent)
                                        .clickable { apiKeyTab = 0 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "AI Studio APIs (${apiKeys.size})",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (apiKeyTab == 0) Color.White else Color(0xFF71717A)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (apiKeyTab == 1) Color(0xFF18181B) else Color.Transparent)
                                        .clickable { apiKeyTab = 1 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tavily APIs (${tavilyApiKeys.size})",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (apiKeyTab == 1) Color.White else Color(0xFF71717A)
                                    )
                                }
                            }

                            if (apiKeyTab == 0) {
                                // AI Studio Pool
                                apiKeys.forEachIndexed { idx, item ->
                                    MonochromeKeyCard(
                                        item = item,
                                        index = idx + 1,
                                        onDelete = { onDeleteKey(item.id) }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newKeyInput,
                                        onValueChange = { newKeyInput = it },
                                        placeholder = {
                                            Text("Paste Gemini API Key (AIza...)", fontSize = 13.sp, color = Color(0xFF71717A))
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFF18181B),
                                            unfocusedTextColor = Color(0xFF18181B),
                                            focusedContainerColor = Color(0xFFF4F4F5),
                                            unfocusedContainerColor = Color(0xFFF4F4F5),
                                            focusedBorderColor = Color(0xFF18181B),
                                            unfocusedBorderColor = Color(0xFFD4D4D8)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_api_key")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (newKeyInput.isNotBlank()) {
                                                onAddKey(newKeyInput.trim())
                                                newKeyInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18181B)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("add_key_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                // Tavily Pool
                                tavilyApiKeys.forEachIndexed { idx, item ->
                                    MonochromeKeyCard(
                                        item = item,
                                        index = idx + 1,
                                        onDelete = { onDeleteTavilyKey(item.id) }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newTavilyKeyInput,
                                        onValueChange = { newTavilyKeyInput = it },
                                        placeholder = {
                                            Text("Paste Tavily API Key (tvly-...)", fontSize = 13.sp, color = Color(0xFF71717A))
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFF18181B),
                                            unfocusedTextColor = Color(0xFF18181B),
                                            focusedContainerColor = Color(0xFFF4F4F5),
                                            unfocusedContainerColor = Color(0xFFF4F4F5),
                                            focusedBorderColor = Color(0xFF18181B),
                                            unfocusedBorderColor = Color(0xFFD4D4D8)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_tavily_key")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (newTavilyKeyInput.isNotBlank()) {
                                                onAddTavilyKey(newTavilyKeyInput.trim())
                                                newTavilyKeyInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18181B)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("add_tavily_key_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }

                            // Bulk Add Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFD4D4D8), RoundedCornerShape(10.dp))
                                    .clickable { showBulkDialog = true }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Subject,
                                        contentDescription = null,
                                        tint = Color(0xFF3F3F46),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (apiKeyTab == 0) "Bulk add AI Studio keys (one per line)" else "Bulk add Tavily keys (one per line)",
                                        color = Color(0xFF3F3F46),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        SettingsCategory.WEB_SEARCH -> {
                            Text(
                                text = "Context-Aware Web Search (Tavily)",
                                color = Color(0xFF18181B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically queries live search when conversation gets technical or asks about current events & news. Personal matters and daily casual talk remain private without searching.",
                                color = Color(0xFF71717A),
                                fontSize = 12.sp
                            )

                            MonochromeToggle(
                                title = "Manual Web Search",
                                description = "When toggled ON (Globe button on chat screen), messages will query live internet sources via Tavily.",
                                checked = tavilySearchEnabled,
                                onCheckedChange = {
                                    tavilySearchEnabled = it
                                    syncAndSave()
                                },
                                testTag = "toggle_tavily_search"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF4F4F5))
                                    .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Search Thoroughness Level",
                                    color = Color(0xFF18181B),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Higher thoroughness gathers more web pages and rich snippets for maximum factual depth, but uses more tokens and takes more time to process.",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                )

                                val levels = listOf(
                                    Triple("Fast", "Quick Glance", "1-2 sources • lowest latency & token cost"),
                                    Triple("Balanced", "Balanced", "3-5 sources • optimal speed & depth"),
                                    Triple("Comprehensive", "Comprehensive", "6-8 sources • deep web research")
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    levels.forEach { (key, label, desc) ->
                                        val isSelected = tavilyThoroughness == key
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) Color(0xFF18181B) else Color.White)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF18181B) else Color(0xFFE4E4E7),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    tavilyThoroughness = key
                                                    tavilyMaxResults = when (key) {
                                                        "Fast" -> 2
                                                        "Comprehensive" -> 7
                                                        else -> 5
                                                    }
                                                    syncAndSave()
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color.White else Color(0xFF18181B),
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = desc,
                                                    color = if (isSelected) Color(0xFFA1A1AA) else Color(0xFF71717A),
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Max Web Sources Slider
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF4F4F5))
                                    .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Max Web Sources",
                                        color = Color(0xFF18181B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$tavilyMaxResults sources",
                                        color = Color(0xFF18181B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Maximum top search results injected into context window.",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                                )
                                Slider(
                                    value = tavilyMaxResults.toFloat(),
                                    onValueChange = {
                                        tavilyMaxResults = it.toInt()
                                        syncAndSave()
                                    },
                                    valueRange = 1f..8f,
                                    steps = 6,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF18181B),
                                        activeTrackColor = Color(0xFF18181B),
                                        inactiveTrackColor = Color(0xFFD4D4D8)
                                    )
                                )
                            }

                            // Medium Blue Visual Indicator Preview
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141416))
                                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3B82F6))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Medium Blue Visual Indicator",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "When an answer has been influenced by Tavily live search, the entire response appears in this Medium Blue color so you know it reflects fresh internet data.",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bulk Add Keys Dialog
    if (showBulkDialog) {
        val isTavily = apiKeyTab == 1
        AlertDialog(
            onDismissRequest = { showBulkDialog = false },
            title = {
                Text(
                    text = if (isTavily) "Bulk Add Tavily Keys" else "Bulk Add AI Studio Keys",
                    color = Color(0xFF18181B),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isTavily)
                            "Paste one or more Tavily API keys below, each on its own line:"
                        else
                            "Paste one or more Gemini API keys below, each on its own line:",
                        color = Color(0xFF71717A),
                        fontSize = 12.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bulkKeysInput,
                        onValueChange = { bulkKeysInput = it },
                        placeholder = {
                            Text(
                                text = if (isTavily) "tvly-prod-1...\ntvly-backup-2..." else "AIzaSyD...\nAIzaSyB...\nAIzaSyC...",
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF18181B),
                            unfocusedTextColor = Color(0xFF18181B),
                            focusedContainerColor = Color(0xFFF4F4F5),
                            unfocusedContainerColor = Color(0xFFF4F4F5),
                            focusedBorderColor = Color(0xFF18181B),
                            unfocusedBorderColor = Color(0xFFD4D4D8)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = bulkKeysInput
                            .lines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        if (parsed.isNotEmpty()) {
                            if (isTavily) {
                                onBulkAddTavilyKeys(parsed)
                            } else {
                                onBulkAddKeys(parsed)
                            }
                        }
                        bulkKeysInput = ""
                        showBulkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF18181B))
                ) {
                    Text("Add Keys", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDialog = false }) {
                    Text("Cancel", color = Color(0xFF71717A))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun CategoryHubCard(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF4F4F5))
            .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE4E4E7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = Color(0xFF18181B),
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    color = Color(0xFF18181B),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = category.description,
                    color = Color(0xFF71717A),
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF71717A),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun MonochromeToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF4F4F5))
            .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFF18181B),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color(0xFF71717A),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF18181B),
                uncheckedThumbColor = Color(0xFF71717A),
                uncheckedTrackColor = Color(0xFFE4E4E7),
                uncheckedBorderColor = Color(0xFFD4D4D8)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun MonochromeKeyCard(
    item: ApiKeyItem,
    index: Int,
    onDelete: () -> Unit
) {
    val isFailed = item.status == KeyStatus.FAILED
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFailed) Color(0xFFFEF2F2) else Color(0xFFF4F4F5))
            .border(
                1.dp,
                if (isFailed) Color(0xFFEF4444) else Color(0xFFE4E4E7),
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isFailed) Color(0xFFEF4444) else Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.label,
                        color = Color(0xFF18181B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.maskedKey,
                        color = Color(0xFF71717A),
                        fontSize = 11.5.sp
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Error inspection details visible right on the card:
            if (isFailed && !item.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Status: FAILED (${item.failCount}x) • ${item.errorMessage}",
                    color = Color(0xFFDC2626),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
