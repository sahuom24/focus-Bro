package dev.breakmode.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A tactile slide-to-confirm widget inspired by BlockIt's "SLIDE TO START" and "SLIDE TO EXIT".
 * Dragging the thumb across the track triggers [onAction] when past the completion threshold.
 */
@Composable
fun SlideToAction(
    text: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    thumbIcon: ImageVector = Icons.Default.PlayArrow,
    trackColor: Color = Color(0xFF1E1E22),
    thumbColor: Color = Color(0xFFE6E6E6),
    thumbIconColor: Color = Color(0xFF121212),
    textColor: Color = Color(0xFFA0A0A5),
    completedTrackColor: Color = Color(0xFF8FE3C8),
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val thumbHeight = 56.dp
    val thumbWidth = 64.dp
    val cornerRadius = 18.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(trackColor),
    ) {
        val maxDragPx = with(density) { (maxWidth - thumbWidth).toPx() }
        val offsetX = remember { Animatable(0f) }
        val fraction by remember {
            derivedStateOf {
                if (maxDragPx > 0f) (offsetX.value / maxDragPx).coerceIn(0f, 1f) else 0f
            }
        }

        // Active fill behind thumb
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, 0) }
                .clip(RoundedCornerShape(cornerRadius))
                .background(completedTrackColor.copy(alpha = fraction * 0.25f)),
        )

        // Center label
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                ),
                color = textColor.copy(alpha = (1f - fraction * 1.5f).coerceAtLeast(0.15f)),
            )
        }

        // Draggable thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(thumbWidth, thumbHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(thumbColor)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val newTarget = (offsetX.value + delta).coerceIn(0f, maxDragPx)
                            offsetX.snapTo(newTarget)
                        }
                    },
                    onDragStopped = {
                        if (offsetX.value >= maxDragPx * 0.82f) {
                            // Completed slide!
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                offsetX.animateTo(maxDragPx, spring(stiffness = Spring.StiffnessMedium))
                                onAction()
                                offsetX.snapTo(0f)
                            }
                        } else {
                            // Snap back to start
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = thumbIcon,
                contentDescription = text,
                tint = thumbIconColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
