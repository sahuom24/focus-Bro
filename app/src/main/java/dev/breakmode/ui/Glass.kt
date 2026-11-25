package dev.breakmode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.breakmode.R

// Draw once per size. Layered studio light keeps the material available on Android 8+.
internal fun Modifier.studioBackdrop() = drawWithCache {
    val glow = Brush.radialGradient(
        listOf(Color(0xFF194133), Color(0xFF0D1E18), Color.Transparent),
        center = Offset(size.width * .93f, size.height * .12f),
        radius = size.width * 1.15f,
    )
    onDrawBehind {
        drawRect(Color(0xFF070C0A))
        drawRect(glow)
    }
}

internal fun Modifier.glass(radius: Dp = 24.dp, active: Boolean = false): Modifier {
    val shape = RoundedCornerShape(radius)
    val body = Brush.linearGradient(listOf(
        if (active) Color(0x88457860) else Color(0x553C5148),
        Color(0xA312201B),
        Color(0x99304337),
    ))
    val edge = Brush.linearGradient(listOf(
        Color(0x99D6F5E1), Color(0x224D675C), Color(0x164D675C), Color(0x6684AA94),
    ))
    return clip(shape).background(body).border(1.dp, edge, shape)
}

@Composable
internal fun GlassArt(index: Int, modifier: Modifier) {
    val asset = when (index % 3) {
        0 -> R.drawable.glass_3
        1 -> R.drawable.glass_4
        else -> R.drawable.glass_2
    }
    Image(painterResource(asset), contentDescription = null, modifier = modifier)
}
