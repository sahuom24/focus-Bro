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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import dev.breakmode.ui.components.DayOfWeekSelector
import dev.breakmode.ui.components.RoutinesBlockItView
import dev.breakmode.ui.components.SlideToAction
import dev.breakmode.ui.components.SuitcasesRow
import kotlin.math.roundToInt

enum class AppTab(val title: String) {
    TIMER("Timer"),
    BUCKETS("Bags"),
    SCHEDULE("Routines"),
    ANALYSIS("Analysis"),
}

@Composable
fun SetupScreen(
    settings: BreakSettings,
    history: List<BreakRecord>,
    dndAccess: Boolean,
    accessibilityAccess: Boolean,
    overlayAccess: Boolean = false,
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
    onRequestOverlayAccess: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(AppTab.TIMER) }
    var renameBucketTarget by remember { mutableStateOf<AppBucket?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showSelectBucketPrompt by remember { mutableStateOf(false) }
    var showCustomMinutesDialog by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf(settings.minutes.toString()) }

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
                        onOpenCustom = {
                            customMinutesInput = settings.minutes.toString()
                            showCustomMinutesDialog = true
                        },
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

                    AppTab.SCHEDULE -> RoutinesBlockItView(
                        schedules = settings.schedules,
                        buckets = settings.buckets,
                        onUpdateSchedule = onUpdateSchedule,
                        onToggleSchedule = onToggleSchedule,
                    )

                    AppTab.ANALYSIS -> AnalysisPageTab(
                        history = history,
                        settings = settings,
                        dndAccess = dndAccess,
                        accessibilityAccess = accessibilityAccess,
                        overlayAccess = overlayAccess,
                        notificationsEnabled = notificationsEnabled,
                        onPauseInterruptions = onPauseInterruptions,
                        onRequireUnlock = onRequireUnlock,
                        onRequestDndAccess = onRequestDndAccess,
                        onRequestAccessibilityAccess = onRequestAccessibilityAccess,
                        onRequestOverlayAccess = onRequestOverlayAccess,
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
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colors.primary.copy(alpha = 0.2f),
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                    ),
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.BUCKETS,
                    onClick = { selectedTab = AppTab.BUCKETS },
                    icon = { Icon(AppIcons.Buckets, contentDescription = "Bags") },
                    label = { Text("Bags") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colors.primary.copy(alpha = 0.2f),
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                    ),
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.SCHEDULE,
                    onClick = { selectedTab = AppTab.SCHEDULE },
                    icon = { Icon(AppIcons.Schedule, contentDescription = "Routines") },
                    label = { Text("Routines") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colors.primary.copy(alpha = 0.2f),
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                    ),
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.ANALYSIS,
                    onClick = { selectedTab = AppTab.ANALYSIS },
                    icon = { Icon(AppIcons.Analysis, contentDescription = "Analysis") },
                    label = { Text("Analysis") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colors.primary.copy(alpha = 0.2f),
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                    ),
                )
            }
        }
    }

    // Select Bucket Prompt Dialog (if starting without bucket selected)
    if (showSelectBucketPrompt) {
        AlertDialog(
            onDismissRequest = { showSelectBucketPrompt = false },
            title = { Text("Choose a Focus Bag") },
            text = {
                Column {
                    Text(
                        text = "Select which bag of apps you need for this focus period. Phone and Messages are permanently available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    settings.buckets.forEach { bucket ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectBucket(bucket.id)
                                    showSelectBucketPrompt = false
                                    onStart()
                                },
                        ) {
                            Text(
                                text = "${bucket.name} (${bucket.appPackages.size} apps)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(14.dp),
                            )
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
            title = { Text("Rename Bag") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Bag name") },
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

    // Custom Minutes Dialog (Allows 1 to 720 minutes / 12h)
    if (showCustomMinutesDialog) {
        AlertDialog(
            onDismissRequest = { showCustomMinutesDialog = false },
            title = { Text("Custom Focus Duration") },
            text = {
                Column {
                    Text(
                        text = "Enter desired duration in minutes (up to 720 min / 12 hours):",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customMinutesInput,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                customMinutesInput = it
                            }
                        },
                        label = { Text("Minutes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    val parsed = customMinutesInput.toIntOrNull() ?: 0
                    if (parsed > 0) {
                        Text(
                            text = "Equals: ${formatMinutes(parsed)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.primary,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = customMinutesInput.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            onMinutes(parsed.coerceIn(5, 720))
                        }
                        showCustomMinutesDialog = false
                    },
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMinutesDialog = false }) { Text("Cancel") }
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
    onOpenCustom: () -> Unit,
    onStart: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val activeBucket = settings.buckets.firstOrNull { it.id == settings.selectedBucketId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "FocusBro",
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

        Spacer(Modifier.height(6.dp))

        // Center: Big Digital Clock + Slider + Presets
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
                    fontSize = 72.sp,
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

            Spacer(Modifier.height(16.dp))

            // Continuous Slider (5 min to 720 min / 12h)
            Slider(
                value = settings.minutes.toFloat(),
                onValueChange = { onMinutes(((it / 5f).roundToInt() * 5).coerceIn(5, 720)) },
                valueRange = 5f..720f,
                steps = 142,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            // Preset Chips (up to 12h + Custom)
            val presets = listOf(15, 30, 45, 60, 120, 180, 360, 720)
            val isCustom = settings.minutes !in presets

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { minsVal ->
                    FilterChip(
                        selected = settings.minutes == minsVal,
                        onClick = { onMinutes(minsVal) },
                        label = { Text(formatMinutes(minsVal)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.onPrimary,
                        ),
                    )
                }
                // Custom Chip
                FilterChip(
                    selected = isCustom,
                    onClick = onOpenCustom,
                    label = {
                        Text(if (isCustom) "Custom (${formatMinutes(settings.minutes)})" else "Custom...")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = colors.onPrimary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Bag Quick Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SELECT ACTIVE BAG",
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
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary.copy(alpha = 0.2f),
                            selectedLabelColor = colors.primary,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // SLIDE TO START (Deliberate tactile start slider)
        SlideToAction(
            text = "SLIDE TO START",
            onAction = onStart,
            completedTrackColor = colors.primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// -------------------------------------------------------------- TAB 2: BAGS (BUCKETS)
@Composable
private fun BucketsPageTab(
    buckets: List<AppBucket>,
    selectedBucketId: String?,
    onSelectBucket: (String?) -> Unit,
    onOpenBucketPicker: (String) -> Unit,
    onStartRename: (AppBucket) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val activeBucket = buckets.firstOrNull { it.id == selectedBucketId } ?: buckets.firstOrNull()

    val getAppLabel: (String) -> String = remember(context) {
        { pkg ->
            runCatching {
                val info = context.packageManager.getApplicationInfo(pkg, 0)
                context.packageManager.getApplicationLabel(info).toString()
            }.getOrDefault(pkg)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        Text(
            text = "Focus Bags",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Pack essential apps for each mode (up to 6 apps per bag). Phone and Messages are permanently available.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        // Graphic Suitcases Row (BlockIt inspired)
        SuitcasesRow(
            buckets = buckets,
            selectedBucketId = activeBucket?.id,
            onSelectBucket = { onSelectBucket(it) },
            activeColor = colors.primary,
        )

        Spacer(Modifier.height(16.dp))

        // Active Bag Card & App Slots
        activeBucket?.let { bucket ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    // Header with Rename
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bucket.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
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
                                text = "${bucket.appPackages.size} of ${AppBucket.MAX_APPS_PER_BUCKET} packed apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary,
                            )
                        }

                        Button(
                            onClick = { onOpenBucketPicker(bucket.id) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 14.dp,
                                vertical = 6.dp,
                            ),
                        ) {
                            Text("Edit Apps")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 6 App Slots Grid (2 rows of 3)
                    val appList = bucket.appPackages.toList()
                    val totalSlots = AppBucket.MAX_APPS_PER_BUCKET

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (row in 0 until (totalSlots + 2) / 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                for (col in 0 until 3) {
                                    val index = row * 3 + col
                                    if (index < totalSlots) {
                                        val pkg = appList.getOrNull(index)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(84.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (pkg != null) Color(0xFF19191E) else Color(0xFF141417),
                                                )
                                                .clickable { onOpenBucketPicker(bucket.id) }
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (pkg != null) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                ) {
                                                    AppIcon(packageName = pkg, size = 38.dp)
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        text = getAppLabel(pkg),
                                                        style = TextStyle(fontSize = 10.sp),
                                                        color = colors.onBackground,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center,
                                                    )
                                                }
                                            } else {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add app",
                                                        tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = "Add slot",
                                                        style = TextStyle(fontSize = 9.sp),
                                                        color = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Life / Parachute Info Card (100% Free)
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF241515),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Life",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "1 Daily Emergency Life",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Allows an emergency exit once a day, resetting at midnight. 100% free with no in-app purchases or subscriptions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------ TAB 3: SCHEDULES (ROUTINES)
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
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Automatic Routines",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Lock down automatically at scheduled times.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(Modifier.height(16.dp))

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
                        text = "No routines created yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tap '+ Add' to set up automatic focus blocks for bedtime, study, or deep work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            schedules.forEach { schedule ->
                val bucketName = buckets.firstOrNull { it.id == schedule.bucketId }?.name ?: "All apps"
                val startStr = String.format("%02d:%02d", schedule.startHour, schedule.startMinute)
                val endTotalMins = (schedule.startHour * 60 + schedule.startMinute + schedule.durationMinutes) % (24 * 60)
                val endStr = String.format("%02d:%02d", endTotalMins / 60, endTotalMins % 60)

                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                    ) {
                        // Title row with switch & delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = schedule.name.uppercase(),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                    ),
                                    color = if (schedule.enabled) colors.primary else colors.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "$startStr → $endStr",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.primary.copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            text = formatMinutes(schedule.durationMinutes),
                                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                            color = colors.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = "Bag: $bucketName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }

                            IconButton(onClick = { onDelete(schedule.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Switch(
                                checked = schedule.enabled,
                                onCheckedChange = { onToggle(schedule.id, it) },
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Day of week chips row (BlockIt inspired)
                        DayOfWeekSelector(
                            selectedDays = schedule.daysOfWeek,
                            onToggleDay = {},
                            activeColor = if (schedule.enabled) colors.primary else Color(0xFF44444A),
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
    overlayAccess: Boolean,
    notificationsEnabled: Boolean,
    onPauseInterruptions: (Boolean) -> Unit,
    onRequireUnlock: (Boolean) -> Unit,
    onRequestDndAccess: () -> Unit,
    onRequestAccessibilityAccess: () -> Unit,
    onRequestOverlayAccess: () -> Unit,
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
                    title = "Lockdown mode (Display over apps)",
                    subtitle = when {
                        overlayAccess -> "Keeps FocusBro locked as your home screen during active sessions."
                        else -> "Permission needed to prevent exiting to launcher. Tap to allow."
                    },
                    subtitleIsWarning = !overlayAccess,
                    checked = overlayAccess,
                    onCheckedChange = { onRequestOverlayAccess() },
                    onClick = onRequestOverlayAccess,
                )
                SettingRow(
                    title = "Require unlock to end",
                    subtitle = "Phone lock required for Life exit.",
                    checked = settings.requireUnlockToEnd,
                    onCheckedChange = onRequireUnlock,
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
        title = { Text("Add Focus Routine") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Routine name") },
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

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Total focus duration: ${formatMinutes(duration)}",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(16.dp))

                // Days selection
                Text(text = "Repeat on days", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                DayOfWeekSelector(
                    selectedDays = selectedDays,
                    onToggleDay = { day ->
                        selectedDays = if (day in selectedDays) {
                            if (selectedDays.size > 1) selectedDays - day else selectedDays
                        } else {
                            selectedDays + day
                        }
                    },
                )

                Spacer(Modifier.height(16.dp))

                // Bag selection
                Text(text = "Allowed Bag", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    buckets.forEach { b ->
                        FilterChip(
                            selected = selectedBucketId == b.id,
                            onClick = { selectedBucketId = b.id },
                            label = { Text(b.name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        FocusSchedule(
                            name = name.ifBlank { "Routine" },
                            startHour = startHour,
                            startMinute = startMinute,
                            durationMinutes = duration,
                            daysOfWeek = selectedDays,
                            bucketId = selectedBucketId,
                            enabled = true,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ------------------------------------------------------------- SHARED HELPERS
@Composable
private fun StatsCard(history: List<BreakRecord>) {
    val colors = MaterialTheme.colorScheme
    val lastSeven = remember(history) { lastSevenDays(history) }
    val todayMin = remember(history) { todayMinutes(history) }
    val maxDay = (lastSeven.maxOrNull() ?: 0).coerceAtLeast(60)

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = formatMinutes(todayMin),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                    )
                }
                Text(
                    text = "${history.size} total sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(18.dp))

            // 7-day Bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                val dayLabels = listOf("6d ago", "5d", "4d", "3d", "2d", "Yest", "Today")
                lastSeven.forEachIndexed { i, mins ->
                    val fraction = (mins.toFloat() / maxDay).coerceIn(0.06f, 1f)
                    val isToday = i == 6
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (mins > 0) {
                            Text(
                                text = "${mins}m",
                                style = TextStyle(fontSize = 9.sp),
                                color = if (isToday) colors.primary else colors.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (isToday) colors.primary else colors.surfaceVariant,
                                ),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dayLabels[i],
                            style = TextStyle(fontSize = 9.sp),
                            color = if (isToday) colors.onBackground else colors.onSurfaceVariant,
                        )
                    }
                }
            }
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
