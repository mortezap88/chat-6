package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.data.ApiKeyItem
import com.example.data.AppSettings
import com.example.data.ChatMessage
import com.example.data.CorrectionType
import com.example.data.FailoverEvent
import com.example.data.LinguisticCorrection
import com.example.ui.components.CorrectionDialog
import com.example.ui.components.CustomVirtualKeyboard
import com.example.ui.components.FailoverBanner
import com.example.ui.components.InteractiveCorrectedText

sealed class ChatPage {
    data class UserPage(val message: ChatMessage, val index: Int) : ChatPage()
    data class AiPage(val message: ChatMessage, val index: Int) : ChatPage()
}

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    settings: AppSettings,
    apiKeys: List<ApiKeyItem>,
    activeKeyIndex: Int,
    failoverEvent: FailoverEvent?,
    isRecordingAudio: Boolean,
    isConsolidatingMemory: Boolean = false,
    onConsolidateMemory: () -> Unit = {},
    onStartAudioRecording: () -> Unit,
    onStopAudioRecording: () -> Unit,
    isTtsEnabled: Boolean,
    onToggleTts: () -> Unit = {},
    isWebSearchEnabled: Boolean = settings.tavilySearchEnabled,
    onToggleWebSearch: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClearChat: () -> Unit,
    onDismissFailover: () -> Unit,
    onRetryMessage: (ChatMessage) -> Unit,
    onApplyCorrection: (ChatMessage, LinguisticCorrection) -> Unit,
    speakingMessageId: String? = null
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var typedText by remember { mutableStateOf("") }
    var activeCorrectionModal by remember { mutableStateOf<Pair<ChatMessage, LinguisticCorrection>?>(null) }

    // Build the list of pages: User message page + AI response page for every turn
    val pages = remember(messages.toList()) {
        val list = mutableListOf<ChatPage>()
        messages.forEachIndexed { idx, msg ->
            list.add(ChatPage.UserPage(msg, idx))
            list.add(ChatPage.AiPage(msg, idx))
        }
        list
    }

    val pagerState = rememberPagerState(
        initialPage = (pages.size - 1).coerceAtLeast(0),
        pageCount = { if (pages.isEmpty()) 1 else pages.size }
    )

    LaunchedEffect(pages.size) {
        if (pages.isNotEmpty()) {
            pagerState.animateScrollToPage(pages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0C))
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation & Status Bar (token counter & page counter on left, settings button on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Token Counter Button (replaces plus button, on the left)
                val tokenCount = remember(messages.toList()) {
                    if (messages.isEmpty()) {
                        0
                    } else {
                        var totalChars = 1150
                        for (m in messages) {
                            totalChars += m.userText.length + m.aiResponse.length + 30
                        }
                        (totalChars / 3.8f).toInt()
                    }
                }
                val isPassing20k = tokenCount >= 20000

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .defaultMinSize(minWidth = 38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(
                            if (isPassing20k) Color(0xFFFCA5A5) else Color.White
                        )
                        .pointerInput(isPassing20k, isConsolidatingMemory, messages.size) {
                            detectTapGestures(
                                onTap = {
                                    if (isPassing20k && !isConsolidatingMemory) {
                                        onConsolidateMemory()
                                    }
                                },
                                onLongPress = {
                                    if (!isConsolidatingMemory && messages.isNotEmpty()) {
                                        onConsolidateMemory()
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConsolidatingMemory) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (tokenCount >= 1000) String.format(java.util.Locale.US, "%,d", tokenCount) else "$tokenCount",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("token_counter_button")
                        )
                    }
                }

                // Page Number on the left next to token counter
                if (pages.size > 1) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${pagerState.currentPage + 1}/${pages.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("page_counter_text")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            // Page-Based Message Container
            // Hold down to record (when mic enabled) without moving;
            // Dragging pages through messages without opening keyboard;
            // Single tap toggles virtual keyboard.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPos = down.position
                            val pointerId = down.id
                            var isHeldRecording = false
                            var isDragGesture = false

                            val holdJob = coroutineScope.launch {
                                kotlinx.coroutines.delay(280L)
                                if (!isDragGesture) {
                                    isHeldRecording = true
                                    onStartAudioRecording()
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    break
                                }
                                val distance = (change.position - startPos).getDistance()
                                if (distance > viewConfiguration.touchSlop) {
                                    isDragGesture = true
                                    holdJob.cancel()
                                }
                            }
                            holdJob.cancel()

                            if (isHeldRecording) {
                                onStopAudioRecording()
                            } else if (!isDragGesture) {
                                // Strictly a single tap without dragging: toggle keyboard
                                isKeyboardVisible = !isKeyboardVisible
                            }
                            // If isDragGesture == true: do nothing, user was dragging/swiping to look at other pages!
                        }
                    }
            ) {
                if (messages.isEmpty()) {
                    AnimatedPromptPlaceholder(
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val pageItem = pages.getOrNull(pageIndex)
                        if (pageItem != null) {
                            when (pageItem) {
                                is ChatPage.UserPage -> {
                                    UserMessagePageView(
                                        message = pageItem.message,
                                        settings = settings,
                                        onCorrectionClick = { corr ->
                                            activeCorrectionModal = Pair(pageItem.message, corr)
                                        }
                                    )
                                }
                                is ChatPage.AiPage -> {
                                    AiResponsePageView(
                                        message = pageItem.message,
                                        settings = settings,
                                        onRetry = { onRetryMessage(pageItem.message) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom-left Controls: Speaker (TTS) & Web Search On/Off Toggle Buttons
        if (!isKeyboardVisible) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val isPlayingAudio = speakingMessageId != null

            val ttsBackgroundColor by animateColorAsState(
                targetValue = if (isTtsEnabled) Color.White else Color(0xFF27272A),
                animationSpec = tween(220),
                label = "tts_btn_bg"
            )
            val ttsBorderColor by animateColorAsState(
                targetValue = if (isTtsEnabled) Color(0xFFE4E4E7) else Color(0xFF3F3F46),
                animationSpec = tween(220),
                label = "tts_btn_border"
            )
            val ttsIconTint by animateColorAsState(
                targetValue = when {
                    isPlayingAudio -> Color(0xFF2563EB) // Blue accent during active audio playback
                    isTtsEnabled -> Color(0xFF18181B)   // Dark speaker icon when enabled
                    else -> Color(0xFFA1A1AA)          // Muted subtle grey icon when disabled
                },
                animationSpec = tween(220),
                label = "tts_btn_tint"
            )

            val infiniteTransition = rememberInfiniteTransition(label = "tts_speaking_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isPlayingAudio) 1.1f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(550, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "tts_pulse_scale"
            )

            // Web Search Button (Public / Globe) styling:
            // When ON: Active deep blue container with electric blue border and light blue icon
            // When OFF: Dark container with muted grey icon (no web search)
            val webSearchBgColor by animateColorAsState(
                targetValue = if (isWebSearchEnabled) Color(0xFF1E3A8A) else Color(0xFF27272A),
                animationSpec = tween(220),
                label = "web_search_btn_bg"
            )
            val webSearchBorderColor by animateColorAsState(
                targetValue = if (isWebSearchEnabled) Color(0xFF3B82F6) else Color(0xFF3F3F46),
                animationSpec = tween(220),
                label = "web_search_btn_border"
            )
            val webSearchIconTint by animateColorAsState(
                targetValue = if (isWebSearchEnabled) Color(0xFF60A5FA) else Color(0xFFA1A1AA),
                animationSpec = tween(220),
                label = "web_search_btn_tint"
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Speaker Button (TTS Toggle)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(ttsBackgroundColor)
                        .border(
                            width = 1.dp,
                            color = ttsBorderColor,
                            shape = CircleShape
                        )
                        .clickable { onToggleTts() }
                        .testTag("tts_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isTtsEnabled,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                        label = "tts_icon_anim"
                    ) { active ->
                        Icon(
                            imageVector = if (active) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (active) "Disable Speech (TTS)" else "Enable Speech (TTS)",
                            tint = ttsIconTint,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // 2. Web Search Button (Globe Toggle) next to the speaker button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(webSearchBgColor)
                        .border(
                            width = 1.dp,
                            color = webSearchBorderColor,
                            shape = CircleShape
                        )
                        .clickable {
                            onToggleWebSearch()
                            android.widget.Toast.makeText(
                                context,
                                if (!isWebSearchEnabled) "🌐 Web Search ON (All inputs searched online)" else "🌐 Web Search OFF (No web search)",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        .testTag("web_search_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isWebSearchEnabled,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                        label = "web_search_icon_anim"
                    ) { enabled ->
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = if (enabled) "Web search ON (forced web search for all messages). Tap to turn OFF." else "Web search OFF (internal knowledge only). Tap to turn ON.",
                            tint = webSearchIconTint,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        CustomVirtualKeyboard(
            visible = isKeyboardVisible,
            text = typedText,
            onTextChange = { typedText = it },
            onSend = { sent ->
                onSendMessage(sent)
                typedText = ""
                isKeyboardVisible = false
            },
            onClose = { isKeyboardVisible = false },
            modifier = Modifier
                .fillMaxSize()
                .testTag("custom_virtual_keyboard")
        )
    }

    // Active Correction Dialog modal
    activeCorrectionModal?.let { (msg, corr) ->
        CorrectionDialog(
            correction = corr,
            onApply = { appliedCorr ->
                onApplyCorrection(msg, appliedCorr)
                activeCorrectionModal = null
            },
            onDismiss = { activeCorrectionModal = null }
        )
    }
}

/**
 * Full page view for USER's message with clean, cardless typography and interactive linguistic corrections.
 */
@Composable
internal fun UserMessagePageView(
    message: ChatMessage,
    settings: AppSettings,
    onCorrectionClick: (LinguisticCorrection) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        if (message.isAnalyzingCorrections) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFF59E0B),
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Inspecting...",
                    color = Color(0xFF71717A),
                    fontSize = 11.sp
                )
            }
        }

        InteractiveCorrectedText(
            text = message.userText,
            corrections = message.corrections,
            fontSizeSp = settings.maxFontSizeSp,
            onCorrectionClick = onCorrectionClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Full page view for AI's response with cardless, clean typography and smooth token streaming.
 */
@Composable
internal fun AiResponsePageView(
    message: ChatMessage,
    settings: AppSettings,
    onRetry: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        if (message.error != null) {
            ErrorCard(error = message.error, onRetry = onRetry)
        } else if (message.aiResponse.isEmpty() && message.isStreaming) {
            StreamingIndicator(model = message.modelUsed)
        } else {
            val answerColor = if (message.isWebSearchEnhanced) Color(0xFF60A5FA) else Color(0xFFF4F4F5)
            SmoothStreamingText(
                fullText = message.aiResponse,
                isStreaming = message.isStreaming,
                maxFontSizeSp = settings.maxFontSizeSp,
                textColor = answerColor,
                enableScroll = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ErrorCard(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF261214))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Generation Error",
                color = Color(0xFFFCA5A5),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = Color(0xFFF87171),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3F191D))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StreamingIndicator(model: String) {
    Row(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Streaming response from $model...",
            color = Color(0xFFA1A1AA),
            fontSize = 14.sp
        )
    }
}

/**
 * Smooth token streaming text component.
 * Progressively reveals new incoming text tokens at 60fps with adaptive pacing,
 * so text emergence is buttery smooth without lagging behind network bursts.
 */
@Composable
fun SmoothStreamingText(
    fullText: String,
    isStreaming: Boolean,
    maxFontSizeSp: Int,
    textColor: Color = Color(0xFFF4F4F5),
    enableScroll: Boolean = false,
    modifier: Modifier = Modifier
) {
    var displayedLength by remember { mutableIntStateOf(if (isStreaming) 0 else fullText.length) }

    LaunchedEffect(fullText, isStreaming) {
        if (!isStreaming) {
            displayedLength = fullText.length
        } else {
            while (displayedLength < fullText.length) {
                val backlog = fullText.length - displayedLength
                val step = when {
                    backlog > 80 -> 8
                    backlog > 40 -> 5
                    backlog > 20 -> 3
                    backlog > 8 -> 2
                    else -> 1
                }
                displayedLength = (displayedLength + step).coerceAtMost(fullText.length)
                delay(14L) // buttery smooth ~70fps emergence
            }
        }
    }

    val visibleText = remember(fullText, displayedLength) {
        fullText.take(displayedLength)
    }

    AutoShrinkText(
        text = visibleText,
        maxFontSizeSp = maxFontSizeSp,
        color = textColor,
        enableScroll = enableScroll,
        modifier = modifier
    )
}

/**
 * Auto-shrinks text dynamically to fit within available page height and width.
 * Prevents nesting scrollable containers inside unbounded parents to prevent Compose crashes.
 */
@Composable
fun AutoShrinkText(
    text: String,
    maxFontSizeSp: Int,
    minFontSizeSp: Int = 12,
    color: Color = Color(0xFFF4F4F5),
    enableScroll: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.TopStart) {
        val maxAvailableHeightPx = with(density) { maxHeight.toPx() }
        val maxAvailableWidthPx = with(density) { maxWidth.toPx() }

        var chosenSizeSp = maxFontSizeSp
        if (maxAvailableHeightPx.isFinite() && maxAvailableWidthPx.isFinite() && maxAvailableWidthPx > 0f) {
            for (candidateSp in maxFontSizeSp downTo minFontSizeSp) {
                val measured = textMeasurer.measure(
                    text = text,
                    style = TextStyle(
                        fontSize = candidateSp.sp,
                        lineHeight = (candidateSp * 1.4f).sp
                    ),
                    constraints = Constraints(maxWidth = maxAvailableWidthPx.toInt().coerceAtLeast(1))
                )
                if (measured.size.height <= maxAvailableHeightPx || candidateSp == minFontSizeSp) {
                    chosenSizeSp = candidateSp
                    break
                }
            }
        }

        val scrollState = rememberScrollState()
        val textModifier = if (enableScroll && maxAvailableHeightPx.isFinite()) {
            Modifier.verticalScroll(scrollState)
        } else {
            Modifier
        }

        Text(
            text = text,
            color = color,
            fontSize = chosenSizeSp.sp,
            lineHeight = (chosenSizeSp * 1.45f).sp,
            fontWeight = FontWeight.Normal,
            modifier = textModifier.fillMaxWidth()
        )
    }
}

/**
 * Animated prompt placeholder with a continuous moving gradient across shades of gray.
 * Strictly uses dark gray, medium gray, and light gray without white or black.
 * Smoothly transitions through prompts ("Ask me something", "How do you feel?", "What is up?").
 */
@Composable
fun AnimatedPromptPlaceholder(
    modifier: Modifier = Modifier
) {
    val prompts = remember {
        listOf(
            "Ask me something",
            "How do you feel?",
            "What is up?",
            "What's on your mind?",
            "Tell me about your day"
        )
    }

    var currentPromptIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3800L)
            currentPromptIndex = (currentPromptIndex + 1) % prompts.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "gray_gradient_motion")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_shift"
    )

    // Smooth gradient strictly consisting of gray tones (no black 0x00, no white 0xFF)
    // Dark gray -> Medium gray -> Light gray
    val grayColors = remember {
        listOf(
            Color(0xFF424242), // Dark gray
            Color(0xFF5E5E5E), // Medium-dark gray
            Color(0xFF7E7E7E), // Medium gray
            Color(0xFF9E9E9E), // Medium-light gray
            Color(0xFFB8B8B8), // Light gray
            Color(0xFF9E9E9E), // Medium-light gray
            Color(0xFF7E7E7E), // Medium gray
            Color(0xFF5E5E5E), // Medium-dark gray
            Color(0xFF424242)  // Dark gray
        )
    }

    val brush = Brush.linearGradient(
        colors = grayColors,
        start = Offset(gradientShift, 0f),
        end = Offset(gradientShift + 650f, 250f),
        tileMode = TileMode.Repeated
    )

    Box(
        modifier = modifier
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentPromptIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
            },
            label = "prompt_text_animation"
        ) { index ->
            Text(
                text = prompts[index],
                style = TextStyle(
                    brush = brush,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp,
                    letterSpacing = 0.3.sp
                )
            )
        }
    }
}
