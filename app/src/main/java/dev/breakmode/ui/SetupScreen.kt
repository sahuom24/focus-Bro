package dev.breakmode.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.breakmode.session.AppBucket
import dev.breakmode.session.BreakRecord
import dev.breakmode.session.BreakSettings
import dev.breakmode.session.FocusSchedule
import dev.breakmode.session.FocusScheduleManager
import dev.breakmode.session.Wallpaper
import dev.breakmode.ui.theme.wallpaperSwatch
import kotlin.math.roundToInt

enum class AppTab(val title: String) {
    TIMER("Timer"),
    BUCKETS("Buckets"),
    SCHEDULE("Schedule"),
    ANALYSIS("Analysis"),
}

@Composable
fun SetupScreen(
    settings: BreakSettings,
    history: List<BreakRecord>,
    dndAccess: Boolean,
    accessibilityAccess: Boolean,
    notificationsEnabled: Boolean,
    allowedLabels: List<String>,
    justFinishedMinutes: Int?,
    onDismissFinished: () -> Unit,
    onStart: () -> Unit,
    onMinutes: (Int) -> Unit,
    onOpenPicker: () -> Unit,
    onOpenBucketPicker: (String) -> Unit,
    onSelectBucket: (String?) -> Unit,
    onRenameBucket: (String, String) -> Unit,
    onAddSchedule: (FocusSchedule) -> Unit,
    onUpdateSchedule: (FocusSchedule) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onToggleSchedule: (String, Boolean) -> Unit,
    onPauseInterruptions: (Boolean) -> Unit,
    onRequireUnlock: (Boolean) -> Unit,
    onWallpaper: (Wallpaper) -> Unit,
    onRequestDndAccess: () -> Unit,
    onRequestAccessibilityAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(AppTab.TIMER) }
    var renameBucketTarget by remember { mutableStateOf<AppBucket?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showSelectBucketPrompt by remember { mutableStateOf(false) }

    val handleStartClick = {
        if (settings.selectedBucketId == null && settings.buckets.isNotEmpty()) {
            showSelectBucketPrompt = true
        } else {
            onStart()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Main content depending on active tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (selectedTab) {
                    AppTab.TIMER -> MainTimerTab(
                        settings = settings,
                        onMinutes = onMinutes,
                        onSelectBucket = onSelectBucket,
                        onStart = handleStartClick,
                    )

                    AppTab.BUCKETS -> BucketsPageTab(
                        buckets = settings.buckets,
                        selectedBucketId = settings.selectedBucketId,
                        onSelectBucket = onSelectBucket,
                        onOpenBucketPicker = onOpenBucketPicker,
                        onStartRename = { bucket ->
                            renameBucketTarget = bucket
                            renameText = bucket.name
                        },
                    )

                    AppTab.SCHEDULE -> SchedulesPageTab(
                        schedules = settings.schedules,
                        buckets = settings.buckets,
                        onToggle = onToggleSchedule,
                        onDelete = onDeleteSchedule,
                        onAddClick = { showAddScheduleDialog = true },
                    )

                    AppTab.ANALYSIS -> AnalysisPageTab(
                        history = history,
                        settings = settings,
                        dndAccess = dndAccess,
                        accessibilityAccess = accessibilityAccess,
                        notificationsEnabled = notificationsEnabled,
                        onPauseInterruptions = onPauseInterruptions,
                        onRequireUnlock = onRequireUnlock,
                        onWallpaper = onWallpaper,
                        onRequestDndAccess = onRequestDndAccess,
                        onRequestAccessibilityAccess = onRequestAccessibilityAccess,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                    )
                }
            }

            // Bottom Navigation Bar with 4 tabs
            NavigationBar(
                containerColor = colors.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.TIMER,
                    onClick = { selectedTab = AppTab.TIMER },
                    icon = { Icon(AppIcons.Timer, contentDescription = "Timer") },
                    label = { Text("Timer") },
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.BUCKETS,
                    onClick = { selectedTab = AppTab.BUCKETS },
                    icon = { Icon(AppIcons.Buckets, contentDescription = "Buckets") },
                    label = { Text("Buckets") },
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.SCHEDULE,
                    onClick = { selectedTab = AppTab.SCHEDULE },
                    icon = { Icon(AppIcons.Schedule, contentDescription = "Schedule") },
                    label = { Text("Schedule") },
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.ANALYSIS,
                    onClick = { selectedTab = AppTab.ANALYSIS },
                    icon = { Icon(AppIcons.Analysis, contentDescription = "Analysis") },
                    label = { Text("Analysis") },
                )
            }
        }
    }

    // Select Bucket Prompt Dialog (if starting without bucket selected)
    if (showSelectBucketPrompt) {
        AlertDialog(
            onDismissRequest = { showSelectBucketPrompt = false },
            title = { Text("Choose a Focus Bucket") },
            text = {
                Column {
                    Text(
                        text = "Select which bucket of apps you need for this focus period. Phone and Messages are always permanently available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    settings.buckets.forEach { bucket ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (settings.selectedBucketId == bucket.id) {
                                    colors.primary.copy(alpha = 0.2f)
                                } else {
                                    colors.surface
                                },
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onSelectBucket(bucket.id)
                                    showSelectBucketPrompt = false
                                    onStart()
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = bucket.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "${bucket.appPackages.size} / 5 apps configured",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSelectBucketPrompt = false }) { Text("Cancel") }
            },
        )
    }

    // Rename Bucket Dialog
    renameBucketTarget?.let { bucket ->
        AlertDialog(
            onDismissRequest = { renameBucketTarget = null },
            title = { Text("Rename Bucket") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Bucket name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onRenameBucket(bucket.id, renameText.trim())
                        }
                        renameBucketTarget = null
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameBucketTarget = null }) { Text("Cancel") }
            },
        )
    }

    // Add Schedule Dialog
    if (showAddScheduleDialog) {
        AddScheduleDialog(
            buckets = settings.buckets,
            onDismiss = { showAddScheduleDialog = false },
            onConfirm = { schedule ->
                onAddSchedule(schedule)
                showAddScheduleDialog = false
            },
        )
    }
}

// ---------------------------------------------------------------- TAB 1: TIMER
@Composable
private fun MainTimerTab(
    settings: BreakSettings,
    onMinutes: (Int) -> Unit,
    onSelectBucket: (String?) -> Unit,
    onStart: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val activeBucket = settings.buckets.firstOrNull { it.id == settings.selectedBucketId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top app title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Break Mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )
                Text(
                    text = "Phone & Messages always available",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (activeBucket != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primary.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Text(
                        text = activeBucket.name,
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Center: Big Timer Display + Big Slider
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val hours = settings.minutes / 60
            val mins = settings.minutes % 60
            val timeDisplay = if (hours > 0) {
                String.format("%02d:%02d", hours, mins)
            } else {
                String.format("00:%02d", mins)
            }

            Text(
                text = timeDisplay,
                style = TextStyle(
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.primary,
            )
            Text(
                text = formatMinutes(settings.minutes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceVariant,
            )

            Spacer(Modifier.height(18.dp))

            // Big continuous slider (5 min to 480 min)
            Slider(
                value = settings.minutes.toFloat(),
                onValueChange = { onMinutes(((it / 5f).roundToInt() * 5).coerceIn(5, 480)) },
                valueRange = 5f..480f,
                steps = 94,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // Quick preset chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(15, 30, 45, 60, 120, 240, 480).forEach { minsVal ->
                    FilterChip(
                        selected = settings.minutes == minsVal,
                        onClick = { onMinutes(minsVal) },
                        label = { Text(formatMinutes(minsVal)) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Bucket Selection section
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SELECT BUCKET",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = activeBucket?.name ?: "None selected",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = if (activeBucket != null) colors.primary else colors.error,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                settings.buckets.forEach { bucket ->
                    val isSelected = bucket.id == settings.selectedBucketId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectBucket(bucket.id) },
                        label = { Text(bucket.name) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Big "Start Focus Mode" button
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = "Start ${formatMinutes(settings.minutes)} focus",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// -------------------------------------------------------------- TAB 2: BUCKETS
@Composable
private fun BucketsPageTab(
    buckets: List<AppBucket>,
    selectedBucketId: String?,
    onSelectBucket: (String?) -> Unit,
    onOpenBucketPicker: (String) -> Unit,
    onStartRename: (AppBucket) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = "App Buckets",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Configure up to 5 apps for each mode. Phone and Messages are permanently available in all modes.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        buckets.forEach { bucket ->
            val isSelected = bucket.id == selectedBucketId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surface,
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bucket.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                IconButton(
                                    onClick = { onStartRename(bucket) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                            Text(
                                text = "${bucket.appPackages.size} of 5 essential apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }

                        if (!isSelected) {
                            TextButton(onClick = { onSelectBucket(bucket.id) }) {
                                Text("Select")
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.primary,
                            ) {
                                Text(
                                    text = "Active",
                                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                    color = colors.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // App slots display
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        bucket.appPackages.forEach { pkg ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceVariant.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                AppIcon(packageName = pkg, size = 34.dp)
                            }
                        }
                        val emptySlots = (5 - bucket.appPackages.size).coerceAtLeast(0)
                        repeat(emptySlots) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceVariant.copy(alpha = 0.25f))
                                    .clickable { onOpenBucketPicker(bucket.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add app",
                                    tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { onOpenBucketPicker(bucket.id) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Edit apps (${bucket.appPackages.size}/5)")
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ TAB 3: SCHEDULES
@Composable
private fun SchedulesPageTab(
    schedules: List<FocusSchedule>,
    buckets: List<AppBucket>,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Automated Schedules",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Activate focus mode automatically at recurring times.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))

        val context = LocalContext.current
        val canExact = remember(context) { FocusScheduleManager.canScheduleExact(context) }
        if (!canExact) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.errorContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    },
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Exact Alarm Permission Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onErrorContainer,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Tap to grant permission in Settings so schedules turn on automatically at the exact minute.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onErrorContainer.copy(alpha = 0.85f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        if (schedules.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No schedules created yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Add' to set up automatic focus times for bedtime, study, or deep work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            schedules.forEach { schedule ->
                val bucketName = buckets.firstOrNull { it.id == schedule.bucketId }?.name ?: "All apps"
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = schedule.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            val startStr = String.format("%02d:%02d", schedule.startHour, schedule.startMinute)
                            val endTotalMins = (schedule.startHour * 60 + schedule.startMinute + schedule.durationMinutes) % (24 * 60)
                            val endStr = String.format("%02d:%02d", endTotalMins / 60, endTotalMins % 60)
                            Text(
                                text = "$startStr → $endStr (${formatMinutes(schedule.durationMinutes)}) · $bucketName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.primary,
                            )
                            val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                            val activeDaysStr = schedule.daysOfWeek
                                .sorted()
                                .mapNotNull { if (it in 1..7) dayLabels[it - 1] else null }
                                .joinToString(" ")
                            Text(
                                text = "Days: $activeDaysStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(schedule.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete schedule",
                                tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Switch(
                            checked = schedule.enabled,
                            onCheckedChange = { onToggle(schedule.id, it) },
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------- TAB 4: ANALYSIS
@Composable
private fun AnalysisPageTab(
    history: List<BreakRecord>,
    settings: BreakSettings,
    dndAccess: Boolean,
    accessibilityAccess: Boolean,
    notificationsEnabled: Boolean,
    onPauseInterruptions: (Boolean) -> Unit,
    onRequireUnlock: (Boolean) -> Unit,
    onWallpaper: (Wallpaper) -> Unit,
    onRequestDndAccess: () -> Unit,
    onRequestAccessibilityAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = "Screen Time Analysis",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Your on-device focus history and device controls.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        if (history.isNotEmpty()) {
            StatsCard(history)
            Spacer(Modifier.height(20.dp))
        }

        // Settings section
        Text(
            text = "FOCUS PROTECTION",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.4.sp,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                SettingRow(
                    title = "Silence interruptions",
                    subtitle = when {
                        !settings.pauseInterruptions -> "Notifications stay on unless you turn them off yourself."
                        dndAccess -> "Do Not Disturb is used only if currently off, and restored afterwards."
                        else -> "Do Not Disturb access not granted. Tap to allow."
                    },
                    subtitleIsWarning = settings.pauseInterruptions && !dndAccess,
                    checked = settings.pauseInterruptions,
                    onCheckedChange = onPauseInterruptions,
                    onClick = if (!dndAccess) onRequestDndAccess else null,
                )
                SettingRow(
                    title = "App restriction",
                    subtitle = when {
                        accessibilityAccess -> "Restricts unselected apps while keeping essential settings open."
                        else -> "Accessibility permission needed to restrict apps. Tap to allow."
                    },
                    subtitleIsWarning = !accessibilityAccess,
                    checked = accessibilityAccess,
                    onCheckedChange = { if (!accessibilityAccess) onRequestAccessibilityAccess() },
                    onClick = if (!accessibilityAccess) onRequestAccessibilityAccess else null,
                )
                SettingRow(
                    title = "Require unlock to end",
                    subtitle = "Phone lock required for Life exit.",
                    checked = settings.requireUnlockToEnd,
                    onCheckedChange = onRequireUnlock,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "BREAK BACKGROUND",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.4.sp,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Wallpaper.entries.forEach { wallpaper ->
                WallpaperSwatch(
                    wallpaper = wallpaper,
                    selected = settings.wallpaper == wallpaper,
                    onClick = { onWallpaper(wallpaper) },
                )
            }
        }
    }
}

// ------------------------------------------------------------- SHARED DIALOGS
@Composable
private fun AddScheduleDialog(
    buckets: List<AppBucket>,
    onDismiss: () -> Unit,
    onConfirm: (FocusSchedule) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("Bedtime") }
    var startHour by remember { mutableIntStateOf(22) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(6) }
    var endMinute by remember { mutableIntStateOf(0) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) } // Everyday
    var selectedBucketId by remember { mutableStateOf(buckets.firstOrNull()?.id ?: "default") }

    val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")

    val calculateDuration = { sH: Int, sM: Int, eH: Int, eM: Int ->
        val startTotal = sH * 60 + sM
        val endTotal = eH * 60 + eM
        val diff = endTotal - startTotal
        if (diff > 0) diff else (24 * 60 + diff)
    }

    val duration = calculateDuration(startHour, startMinute, endHour, endMinute)

    val openStartTimePicker = {
        TimePickerDialog(
            context,
            { _, h, m ->
                startHour = h
                startMinute = m
            },
            startHour,
            startMinute,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    val openEndTimePicker = {
        TimePickerDialog(
            context,
            { _, h, m ->
                endHour = h
                endMinute = m
            },
            endHour,
            endMinute,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Focus Schedule") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Schedule name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Time Window (Tap to change)",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Start time card
                    Card(
                        onClick = openStartTimePicker,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "START TIME",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = String.format("%02d:%02d", startHour, startMinute),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Tap to set",
                                style = TextStyle(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // End time card
                    Card(
                        onClick = openEndTimePicker,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "END TIME",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = String.format("%02d:%02d", endHour, endMinute),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Tap to set",
                                style = TextStyle(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Total Focus Duration: ${formatMinutes(duration)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(14.dp))
                Text(text = "Repeat on", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    (1..7).forEach { day ->
                        val isSelected = day in selectedDays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays - day else selectedDays
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(dayNames[day - 1]) },
                        )
                    }
                }

                if (buckets.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(text = "App Bucket", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        buckets.forEach { bucket ->
                            FilterChip(
                                selected = selectedBucketId == bucket.id,
                                onClick = { selectedBucketId = bucket.id },
                                label = { Text(bucket.name) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            FocusSchedule(
                                name = name.trim(),
                                startHour = startHour,
                                startMinute = startMinute,
                                durationMinutes = duration,
                                daysOfWeek = selectedDays,
                                bucketId = selectedBucketId,
                                enabled = true,
                            ),
                        )
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun StatsCard(history: List<BreakRecord>) {
    val colors = MaterialTheme.colorScheme
    val week = lastSevenDays(history)
    val today = todayMinutes(history)
    val peak = week.maxOrNull()?.coerceAtLeast(1) ?: 1

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = if (today == 0) "No break yet" else formatMinutes(today),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    week.forEachIndexed { index, minutes ->
                        val height = if (minutes == 0) {
                            6.dp
                        } else {
                            (10 + 38f * (minutes.toFloat() / peak.toFloat())).dp
                        }
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(height)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (index == week.lastIndex) {
                                        colors.primary
                                    } else {
                                        colors.primary.copy(alpha = if (minutes == 0) 0.18f else 0.45f)
                                    },
                                ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Last 7 days · ${formatMinutes(week.sum())}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitleIsWarning: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (subtitleIsWarning) colors.secondary else colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun WallpaperSwatch(wallpaper: Wallpaper, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(wallpaperSwatch(wallpaper))
                .clickable(onClick = onClick),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = wallpaper.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.onBackground else colors.onSurfaceVariant,
        )
    }
}
