package dev.breakmode.ui

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.breakmode.session.BreakNotifier
import dev.breakmode.session.Interruptions
import dev.breakmode.ui.theme.BreakModeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BreakViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BreakNotifier.ensureChannels(this)
        setContent {
            BreakModeTheme {
                BreakModeApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Closes out a break that expired while we were away, and re-reads special access.
        viewModel.sync()
    }
}

@Composable
fun BreakModeApp(viewModel: BreakViewModel) {
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val appsLoaded by viewModel.appsLoaded.collectAsStateWithLifecycle()
    val picking by viewModel.picking.collectAsStateWithLifecycle()
    val editingBucketId by viewModel.editingBucketId.collectAsStateWithLifecycle()
    val dndAccess by viewModel.dndAccess.collectAsStateWithLifecycle()
    val accessibilityAccess by viewModel.accessibilityAccess.collectAsStateWithLifecycle()
    val emergencyExitAvailable by viewModel.emergencyExitAvailable.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val justFinished by viewModel.justFinishedMinutes.collectAsStateWithLifecycle()

    var showEmergencyExitConfirm by remember { mutableStateOf(false) }
    var showEmergencyExitExhaustedNotice by remember { mutableStateOf(false) }
    var showDndExplain by remember { mutableStateOf(false) }
    var showAccessibilityExplain by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshAccess() }

    val dndSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshAccess() }

    val accessibilitySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshAccess() }

    // Device credential (PIN, pattern, password or biometric) confirmation. The app never sees
    // the credential: the system sheet handles it and only reports success.
    val credentialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.useEmergencyExit()
    }

    val requestEmergencyExit: () -> Unit = {
        if (!emergencyExitAvailable) {
            showEmergencyExitExhaustedNotice = true
        } else {
            val current = active
            val keyguard = context.getSystemService(KeyguardManager::class.java)
            @Suppress("DEPRECATION")
            val intent = if (current != null && current.requireUnlockToEnd && keyguard != null) {
                runCatching {
                    keyguard.createConfirmDeviceCredentialIntent(
                        "Use Emergency Exit?",
                        "Unlock your phone to use today's 1 emergency exit.",
                    )
                }.getOrNull()
            } else {
                null
            }
            if (intent != null) {
                credentialLauncher.launch(intent)
            } else {
                showEmergencyExitConfirm = true
            }
        }
    }

    val current = active
    when {
        current != null -> {
            BackHandler {
                if (emergencyExitAvailable) {
                    requestEmergencyExit()
                } else {
                    showEmergencyExitExhaustedNotice = true
                }
            }
            SessionScreen(
                active = current,
                wallpaper = settings.wallpaper,
                allowedApps = apps.filter { it.packageName in current.allowedPackages },
                onExpired = { viewModel.endBreak(completed = true) },
                onRequestEmergencyExit = requestEmergencyExit,
                emergencyExitAvailable = emergencyExitAvailable,
                onExtend = viewModel::extendBreak,
            )
        }

        picking -> {
            BackHandler { viewModel.closePicker() }
            val currentBucket = settings.buckets.firstOrNull { it.id == editingBucketId }
            val selectedPackages = currentBucket?.appPackages ?: settings.allowedPackages
            PickerScreen(
                apps = apps,
                loaded = appsLoaded,
                selected = selectedPackages,
                onToggle = { pkg ->
                    if (currentBucket != null) {
                        viewModel.toggleAppInBucket(currentBucket.id, pkg)
                    } else {
                        viewModel.toggleApp(pkg)
                    }
                },
                onClear = {
                    if (currentBucket != null) {
                        viewModel.updateBucket(currentBucket.id, currentBucket.name, emptySet())
                    } else {
                        viewModel.clearAllowedApps()
                    }
                },
                onBack = viewModel::closePicker,
                title = if (currentBucket != null) "Edit ${currentBucket.name}" else "Keep available",
                maxSelection = if (currentBucket != null) 5 else null,
            )
        }

        else -> SetupScreen(
            settings = settings,
            history = history,
            dndAccess = dndAccess,
            accessibilityAccess = accessibilityAccess,
            notificationsEnabled = notificationsEnabled,
            allowedLabels = apps
                .filter { it.packageName in settings.effectiveAllowedPackages }
                .map { it.label }
                .sorted(),
            justFinishedMinutes = justFinished,
            onDismissFinished = viewModel::dismissFinished,
            onStart = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (!accessibilityAccess) {
                    showAccessibilityExplain = true
                } else {
                    viewModel.startBreak()
                }
            },
            onMinutes = viewModel::setMinutes,
            onOpenPicker = {
                viewModel.dismissFinished()
                viewModel.openPicker()
            },
            onOpenBucketPicker = { bucketId ->
                viewModel.dismissFinished()
                viewModel.openBucketPicker(bucketId)
            },
            onSelectBucket = viewModel::selectBucket,
            onRenameBucket = { id, name ->
                val b = settings.buckets.firstOrNull { it.id == id }
                if (b != null) viewModel.updateBucket(id, name, b.appPackages)
            },
            onAddSchedule = viewModel::addSchedule,
            onUpdateSchedule = viewModel::updateSchedule,
            onDeleteSchedule = viewModel::deleteSchedule,
            onToggleSchedule = viewModel::toggleSchedule,
            onPauseInterruptions = { enabled ->
                viewModel.setPauseInterruptions(enabled)
                if (enabled && !dndAccess) showDndExplain = true
            },
            onRequireUnlock = viewModel::setRequireUnlock,
            onWallpaper = viewModel::setWallpaper,
            onRequestDndAccess = { showDndExplain = true },
            onRequestAccessibilityAccess = { showAccessibilityExplain = true },
            onOpenNotificationSettings = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                runCatching { context.startActivity(intent) }
            },
        )
    }

    if (showDndExplain) {
        AlertDialog(
            onDismissRequest = { showDndExplain = false },
            title = { Text("Allow Do Not Disturb access?") },
            text = {
                Text(
                    "Break Mode only uses this to turn Do Not Disturb on when it is currently off, " +
                        "and turns it back off when the break ends. Android asks you to grant this " +
                        "in system settings, and you can revoke it there at any time.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDndExplain = false
                        Interruptions.accessSettingsIntent(context)?.let(dndSettingsLauncher::launch)
                    },
                ) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showDndExplain = false }) { Text("Not now") }
            },
        )
    }

    if (showAccessibilityExplain) {
        AlertDialog(
            onDismissRequest = { showAccessibilityExplain = false },
            title = { Text("Enable App Restriction") },
            text = {
                Text(
                    "To restrict all unselected apps during focus mode while keeping notifications " +
                        "and essential settings accessible, Break Mode requires Accessibility permission in system settings.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccessibilityExplain = false
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        accessibilitySettingsLauncher.launch(intent)
                    },
                ) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAccessibilityExplain = false
                        viewModel.startBreak()
                    },
                ) { Text("Start anyway") }
            },
        )
    }

    if (showEmergencyExitConfirm) {
        AlertDialog(
            onDismissRequest = { showEmergencyExitConfirm = false },
            title = { Text("Use Emergency Exit?") },
            text = {
                Text(
                    "You are allowed only 1 emergency exit per day.\n\n" +
                        "Once exhausted, no further breaks can be ended early until midnight.\n\n" +
                        "Do you want to end this focus session?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmergencyExitConfirm = false
                        viewModel.useEmergencyExit()
                    },
                ) { Text("Exit focus") }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyExitConfirm = false }) { Text("Stay focused") }
            },
        )
    }

    if (showEmergencyExitExhaustedNotice) {
        AlertDialog(
            onDismissRequest = { showEmergencyExitExhaustedNotice = false },
            title = { Text("Emergency Exit Unavailable") },
            text = {
                Text(
                    "Your daily emergency exit has already been used today.\n\n" +
                        "This break cannot be ended early and will reset at midnight.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showEmergencyExitExhaustedNotice = false }) { Text("OK") }
            },
        )
    }
}
