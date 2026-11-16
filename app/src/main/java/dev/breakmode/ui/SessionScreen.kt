package dev.breakmode.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.breakmode.apps.DefaultCommunicationApps
import dev.breakmode.apps.LaunchableApp
import dev.breakmode.session.ActiveBreak
import dev.breakmode.session.Wallpaper
import dev.breakmode.ui.components.SlideToAction
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private val Mint = Color(0xFF8FE3C8)
private val Paper = Color(0xFFFFFFFF)
private val AmoledBlack = Color(0xFF000000)

@Composable
fun SessionScreen(
    active: ActiveBreak,
    wallpaper: Wallpaper,
    allowedApps: List<LaunchableApp>,
    onExpired: () -> Unit,
    onRequestEmergencyExit: () -> Unit,
    emergencyExitAvailable: Boolean,
    onExtend: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var extendsUsed by remember { mutableIntStateOf(0) }
    val maxExtends = 4

    LaunchedEffect(active.endsAtMillis) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= active.endsAtMillis) {
                onExpired()
                break
            }
            delay(500)
        }
    }

    val remaining = active.remainingMillis(now)
    val elapsedFraction = active.fractionElapsed(now)
    val secondsLeft = ((remaining / 1000) % 60).coerceAtLeast(0)
    val endsAtLabel = remember(active.endsAtMillis) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(active.endsAtMillis))
    }

    // Filter bucket apps (excluding phone/messages since they have dedicated bottom corners)
    val phonePkg = remember(context) { DefaultCommunicationApps.getPhonePackage(context) }
    val msgPkg = remember(context) { DefaultCommunicationApps.getMessagesPackage(context) }
    val bucketApps = remember(allowedApps, phonePkg, msgPkg) {
        allowedApps.filter { it.packageName != phonePkg && it.packageName != msgPkg }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        // Top Bar: "FOCUSING" on left, "Life: 1/0" on top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "FOCUSING",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                    ),
                    color = Mint,
                )
                Text(
                    text = "Ends at $endsAtLabel",
                    style = TextStyle(fontSize = 11.sp),
                    color = Paper.copy(alpha = 0.55f),
                )
            }

            // Life (Emergency Exit status)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (emergencyExitAvailable) Color(0xFF1C1C22) else Color(0xFF141417),
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Life",
                        tint = if (emergencyExitAvailable) Color(0xFFFF5252) else Color(0xFF666666),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (emergencyExitAvailable) "Life 1" else "Life 0",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = if (emergencyExitAvailable) Paper else Paper.copy(alpha = 0.4f),
                    )
                }
            }
        }

        // Center: Circular Progress + Big Digital Countdown + Seconds Card
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(190.dp)) {
                    val stroke = 8.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = Paper.copy(alpha = 0.08f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = Mint,
                        startAngle = -90f,
                        sweepAngle = 360f * (1f - elapsedFraction),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatCountdown(remaining),
                        style = TextStyle(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = Paper,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E1E24),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = String.format("%02d SEC", secondsLeft),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                            ),
                            color = Mint,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // +15 Min Extension Row (with BlockIt inspired extend dots)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF18181D),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = extendsUsed < maxExtends) {
                        if (extendsUsed < maxExtends) {
                            extendsUsed++
                            onExtend(15)
                        }
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "+15 min",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = if (extendsUsed < maxExtends) Paper else Paper.copy(alpha = 0.35f),
                    )

                    // Extension usage indicators (4 dots)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 0 until maxExtends) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i < extendsUsed) Color(0xFF44444C) else Mint,
                                    ),
                            )
                        }
                    }
                }
            }
        }

        // Bottom Section: Allowed Apps Dock + Slide to Exit + Phone/Messages Corners
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Allowed Apps - Minimalist Text Pills (No Icons)
            if (bucketApps.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    bucketApps.forEach { app ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF16161A),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { openApp(context, app.packageName) },
                        ) {
                            Text(
                                text = app.label,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = Paper,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }

            // Monochrome / Black+White SLIDE TO EXIT Slider (Colourless)
            SlideToAction(
                text = "SLIDE TO EXIT",
                thumbIcon = Icons.Default.Close,
                onAction = onRequestEmergencyExit,
                trackColor = Color(0xFF141416),
                thumbColor = Color(0xFFE2E2E2),
                thumbIconColor = Color(0xFF000000),
                completedTrackColor = Color(0xFF28282E),
                textColor = Color(0xFF888890),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )

            // Phone (Left) and Messages (Right) dedicated bottom dock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Phone
                Surface(
                    onClick = { DefaultCommunicationApps.openPhone(context) },
                    shape = CircleShape,
                    color = Color(0xFF162B1D),
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Phone",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // Status text
                Text(
                    text = if (emergencyExitAvailable) "1 Life Available" else "Life resets at midnight",
                    style = TextStyle(fontSize = 10.sp),
                    color = Paper.copy(alpha = 0.4f),
                )

                // Messages
                Surface(
                    onClick = { DefaultCommunicationApps.openMessages(context) },
                    shape = CircleShape,
                    color = Color(0xFF142436),
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Messages,
                            contentDescription = "Messages",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun openApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    runCatching { context.startActivity(intent) }
}
