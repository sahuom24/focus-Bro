package dev.breakmode.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.breakmode.ui.glass
import kotlin.math.roundToInt

@Composable
fun SlideToAction(text: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var progress by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier.fillMaxWidth().height(58.dp).glass(29.dp).padding(5.dp)
            .semantics(mergeDescendants = true) {
                onClick(label = "Request exit") { onAction(); true }
            },
    ) {
        val travel = with(LocalDensity.current) { (maxWidth - 48.dp).toPx().coerceAtLeast(0f) }
        val drag = rememberDraggableState { delta ->
            if (travel > 0f) progress = (progress + delta / travel).coerceIn(0f, 1f)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text, Modifier.graphicsLayer { alpha = 1f - progress },
                color = colors.onSurface, letterSpacing = 2.sp,
                style = MaterialTheme.typography.labelMedium)
        }
        Box(
            Modifier.offset { IntOffset((progress * travel).roundToInt(), 0) }
                .size(48.dp).glass(24.dp, active = true)
                .background(colors.primary.copy(alpha = .14f))
                .draggable(
                    state = drag, orientation = Orientation.Horizontal, reverseDirection = rtl,
                    onDragStopped = {
                        if (progress >= .82f) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            progress = 0f
                            onAction()
                        } else {
                            animate(progress, 0f, animationSpec = tween(160)) { value, _ -> progress = value }
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = colors.primary)
        }
    }
}
