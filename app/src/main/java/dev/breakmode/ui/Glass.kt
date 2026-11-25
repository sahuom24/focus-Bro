package dev.breakmode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        listOf(Color(0xFF2A3A50), Color(0xFF151C28), Color.Transparent),
        center = Offset(size.width * .93f, size.height * .12f),
        radius = size.width * 1.15f,
    )
    onDrawBehind {
        drawRect(Color(0xFF0A0C10))
        drawRect(glow)
    }
}

internal fun Modifier.glass(radius: Dp = 24.dp, active: Boolean = false): Modifier {
    val shape = RoundedCornerShape(radius)
    val body = Brush.linearGradient(listOf(
        if (active) Color(0x88597496) else Color(0x55526277),
        Color(0xA31A222D),
        Color(0x99303D4E),
    ))
    val edge = Brush.linearGradient(listOf(
        Color(0x99DFECFF), Color(0x22566679), Color(0x16566679), Color(0x668EA5BE),
    ))
    return clip(shape).background(body).border(1.dp, edge, shape)
}

@Composable
internal fun BucketEmblem(bucketId: String?, modifier: Modifier) {
    val asset = when (bucketId) {
        "study" -> R.drawable.emblem_book
        "travel" -> R.drawable.emblem_compass
        else -> R.drawable.emblem_moon
    }
    Image(painterResource(asset), contentDescription = null, modifier = modifier)
}
