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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Top Bar: "UNPLUGGED" on left, "Life" emergency exit button on top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "UNPLUGGED",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    letterSpacing = 4.sp,
                    color = Mint,
                )
                Text(
                    text = "until $endsAtLabel",
                    style = TextStyle(fontSize = 12.sp),
                    color = Paper.copy(alpha = 0.55f),
                )
            }

            // Top Corner: Life (Emergency Exit)
            Button(
                onClick = onRequestEmergencyExit,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (emergencyExitAvailable) Color(0xFF1E1E1E) else Color(0xFF121212),
                    contentColor = if (emergencyExitAvailable) Paper else Paper.copy(alpha = 0.35f),
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Life",
                    tint = if (emergencyExitAvailable) Color(0xFFFF5252) else Color(0xFF555555),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (emergencyExitAvailable) "Life (1)" else "Life (0)",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
            }
        }

        // Center: Timer countdown
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(154.dp)) {
                    val stroke = 7.dp.toPx()
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
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light,
                            fontFeatureSettings = "tnum",
                        ),
                        color = Paper,
                    )
                    Text(
                        text = "remaining",
                        style = TextStyle(fontSize = 11.sp),
                        color = Paper.copy(alpha = 0.45f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = { onExtend(10) },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("+10 min", color = Paper.copy(alpha = 0.7f), fontSize = 13.sp)
            }

            if (active.requireUnlockToEnd) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Phone lock required for Life exit.",
                    style = TextStyle(fontSize = 11.sp),
                    color = Paper.copy(alpha = 0.45f),
                )
            }
        }

        // Bottom section: Apps in a pill + Left corner Phone + Right corner Messages
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Apps - in a pill
            if (bucketApps.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF161616),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        bucketApps.forEach { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { openApp(context, app.packageName) }
                                    .padding(vertical = 2.dp, horizontal = 4.dp),
                            ) {
                                AppIcon(packageName = app.packageName, size = 42.dp)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = app.label,
                                    style = TextStyle(fontSize = 10.sp),
                                    color = Paper.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Nothing but this screen until the break ends.",
                    style = TextStyle(fontSize = 12.sp),
                    color = Paper.copy(alpha = 0.45f),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            // Left bottom corner - phone, Right bottom corner - messages
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left bottom corner: Phone
                Surface(
                    onClick = { DefaultCommunicationApps.openPhone(context) },
                    shape = CircleShape,
                    color = Color(0xFF162B1D),
                    modifier = Modifier.size(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Phone",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // Subtitle in the center between phone and messages
                if (!emergencyExitAvailable) {
                    Text(
                        text = "Life resets at midnight",
                        style = TextStyle(fontSize = 10.sp),
                        color = Paper.copy(alpha = 0.35f),
                    )
                }

                // Right bottom corner: Messages
                Surface(
                    onClick = { DefaultCommunicationApps.openMessages(context) },
                    shape = CircleShape,
                    color = Color(0xFF142436),
                    modifier = Modifier.size(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Messages,
                            contentDescription = "Messages",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(24.dp),
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
