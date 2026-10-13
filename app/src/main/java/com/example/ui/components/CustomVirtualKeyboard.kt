package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Custom Virtual Keyboard adhering to the monochrome (white, black, gray) design system.
 *
 * Features:
 * 1. Floating Text Bubble: Positioned outside the keyboard above the existing text, with
 *    matching curvature (22.dp). Absolutely empty of extra buttons (no send, no minimize,
 *    no copy) - only the text box. Touching it never minimizes the keyboard.
 * 2. Keyboard Styling: Pure white background, black buttons (0xFF18181B), white characters.
 * 3. Curvature: Top corners have exactly the same 28.dp curvature as the right settings drawer.
 * 4. Closing Options: Dragging down on the keyboard closes it, or tapping on the screen above
 *    the floating bubble closes it.
 * 5. Input Functionality: Fully functional spacebar, backspace key, shift toggle, ?123 mode,
 *    and a single unified Send button on the bottom row.
 * 6. Touch Isolation: Taps behind the keys or in margins are consumed and never dismiss the keyboard.
 */
@Composable
fun CustomVirtualKeyboard(
    visible: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val currentText by rememberUpdatedState(text)
    val currentOnTextChange by rememberUpdatedState(onTextChange)

    var isShifted by remember { mutableStateOf(false) }
    var isNumericMode by remember { mutableStateOf(false) }
    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }

    fun triggerKeyHaptic() {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 260)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 200)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1. Transparent touch-dismiss area above the floating bubble:
            // Allows selecting other parts of the screen to close the keyboard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onClose()
                    }
            )

            // 2. Floating Text Box Bubble (Outside the keyboard, above existing text)
            // Curvature: 22.dp. Absolutely empty (no send, no minimize, no copy buttons).
            // Shows a prominent black bar cursor that fades in and fades out, confirming active typing mode.
            val infiniteTransition = rememberInfiniteTransition(label = "cursor_fade")
            val cursorAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 480),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "cursor_alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(12.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Consumes tap with haptic confirmation - typing mode active, never minimizes keyboard
                        triggerKeyHaptic()
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag("floating_text_bubble"),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Black bar cursor that fades in and fades out indicating active typing mode
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color(0xFF18181B).copy(alpha = cursorAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Type your message here...",
                            color = Color(0xFFA1A1AA),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = text,
                            color = Color(0xFF18181B),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(3.dp))

                        // Black bar cursor that fades in and fades out indicating active typing mode
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color(0xFF18181B).copy(alpha = cursorAlpha))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Keyboard Container
            // Background: White. Same 28.dp curvature as the right drawer window.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(18.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE4E4E7),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Consumes tap - behind the keys is NOT a closing function!
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Drag-down handle pill: dragging down on this handle smoothly closes the keyboard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { verticalDragAccumulator = 0f },
                                onDragCancel = { verticalDragAccumulator = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    if (dragAmount > 0f) {
                                        verticalDragAccumulator += dragAmount
                                        if (verticalDragAccumulator > 22f) {
                                            change.consume()
                                            onClose()
                                            verticalDragAccumulator = 0f
                                        }
                                    }
                                }
                            )
                        }
                        .padding(bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFD4D4D8))
                    )
                }

                // Keyboard Matrix
                val rows = if (!isNumericMode) {
                    listOf(
                        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                        listOf("z", "x", "c", "v", "b", "n", "m")
                    )
                } else {
                    listOf(
                        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                        listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"),
                        listOf("!", "\"", "'", ":", ";", ",", "?")
                    )
                }

                // Row 1
                KeyRow(
                    keys = rows[0],
                    isShifted = isShifted && !isNumericMode,
                    onKeyPress = { char ->
                        triggerKeyHaptic()
                        currentOnTextChange(currentText + char)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2
                KeyRow(
                    keys = rows[1],
                    isShifted = isShifted && !isNumericMode,
                    onKeyPress = { char ->
                        triggerKeyHaptic()
                        currentOnTextChange(currentText + char)
                    },
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Row 3: Shift + Middle Letters + Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shift Key
                    Box(
                        modifier = Modifier
                            .weight(1.35f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isShifted) Color(0xFF3F3F46) else Color(0xFF18181B))
                            .clickable {
                                triggerKeyHaptic()
                                isShifted = !isShifted
                            }
                            .testTag("key_shift"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardCapslock,
                            contentDescription = "Caps Lock",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Middle Letters
                    rows[2].forEach { keyChar ->
                        val displayChar = if (isShifted && !isNumericMode) keyChar.uppercase() else keyChar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF18181B))
                                .clickable {
                                    triggerKeyHaptic()
                                    currentOnTextChange(currentText + displayChar)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayChar,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Backspace Key: Instant delete on press + repeat delete on hold
                    Box(
                        modifier = Modifier
                            .weight(1.35f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF18181B))
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    triggerKeyHaptic()
                                    if (currentText.isNotEmpty()) {
                                        currentOnTextChange(currentText.dropLast(1))
                                    }
                                    val repeatJob = coroutineScope.launch {
                                        delay(350L)
                                        while (true) {
                                            triggerKeyHaptic()
                                            if (currentText.isNotEmpty()) {
                                                currentOnTextChange(currentText.dropLast(1))
                                            }
                                            delay(60L)
                                        }
                                    }
                                    waitForUpOrCancellation()
                                    repeatJob.cancel()
                                }
                            }
                            .testTag("key_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 4: Mode switch, Comma, Spacebar, Period, Single Send
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ?123 / ABC Mode Toggle
                    Box(
                        modifier = Modifier
                            .weight(1.35f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF27272A))
                            .clickable {
                                triggerKeyHaptic()
                                isNumericMode = !isNumericMode
                            }
                            .testTag("key_mode_switch"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isNumericMode) "ABC" else "?123",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Comma Key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF18181B))
                            .clickable {
                                triggerKeyHaptic()
                                currentOnTextChange("$currentText,")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = ",", color = Color.White, fontSize = 16.sp)
                    }

                    // Spacebar Key (Dedicated spacing function)
                    Box(
                        modifier = Modifier
                            .weight(4.2f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF18181B))
                            .clickable {
                                triggerKeyHaptic()
                                currentOnTextChange("$currentText ")
                            }
                            .testTag("key_spacebar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "space",
                            color = Color(0xFFA1A1AA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Period Key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF18181B))
                            .clickable {
                                triggerKeyHaptic()
                                currentOnTextChange("$currentText.")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = ".", color = Color.White, fontSize = 16.sp)
                    }

                    // Single Send Key (The ONLY send button)
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (currentText.isNotBlank()) Color(0xFF18181B) else Color(0xFF3F3F46)
                            )
                            .clickable(
                                enabled = currentText.isNotBlank(),
                                onClick = {
                                    triggerKeyHaptic()
                                    val trimmed = currentText.trim()
                                    if (trimmed.isNotEmpty()) {
                                        onSend(trimmed)
                                    }
                                }
                            )
                            .testTag("key_send"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Send",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<String>,
    isShifted: Boolean,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEach { key ->
            val display = if (isShifted) key.uppercase() else key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF18181B))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onKeyPress(display) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = display,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
