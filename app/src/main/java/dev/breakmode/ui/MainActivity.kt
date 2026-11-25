package dev.breakmode.ui

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.breakmode.session.BreakNotifier
import dev.breakmode.session.Interruptions
import dev.breakmode.ui.theme.BreakModeTheme

private const val PRIVACY_POLICY_URL = "https://github.com/sahuom24/focus-Bro/blob/4871245ccb6148846c1a714c80045acc41485932/PRIVACY_POLICY.md"

class MainActivity : ComponentActivity() {

    private val viewModel: BreakViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
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
    var pendingForceEnd by remember { mutableStateOf(false) }
    var showDndExplain by remember { mutableStateOf(false) }
    var showAccessibilityExplain by remember { mutableStateOf(false) }
    var accessibilityDeclined by remember { mutableStateOf(false) }

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
        if (result.resultCode == Activity.RESULT_OK) {
            if (pendingForceEnd) {
                pendingForceEnd = false
                viewModel.forceEndEarly()
            } else {
                viewModel.useEmergencyExit()
            }
        } else {
            pendingForceEnd = false
        }
    }

    val requestEmergencyExit: () -> Unit = {
        val current = active
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val isForceEnd = !emergencyExitAvailable
        @Suppress("DEPRECATION")
        val intent = if (current != null && current.requireUnlockToEnd && keyguard != null) {
            runCatching {
                keyguard.createConfirmDeviceCredentialIntent(
                    if (isForceEnd) "End Session Early?" else "Use Emergency Exit?",
                    if (isForceEnd) {
                        "Unlock your phone to end this focus session early."
                    } else {
                        "Unlock your phone to use today's 1 emergency exit."
                    },
                )
            }.getOrNull()
        } else {
            null
        }
        if (intent != null) {
            pendingForceEnd = isForceEnd
            credentialLauncher.launch(intent)
        } else if (isForceEnd) {
            showEmergencyExitExhaustedNotice = true
        } else {
            showEmergencyExitConfirm = true
        }
    }

    val current = active
    when {
        current != null -> {
            BackHandler {
                // Back button does nothing during focus mode.
                // Emergency exit is only accessible via the dedicated "Life" button on screen.
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
                maxSelection = if (currentBucket != null) dev.breakmode.session.AppBucket.MAX_APPS_PER_BUCKET else null,
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
                if (!accessibilityAccess && !accessibilityDeclined) {
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
            onRequestAccessibilityAccess = {
                if (accessibilityAccess) {
                    runCatching {
                        accessibilitySettingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                } else {
                    accessibilityDeclined = false
                    showAccessibilityExplain = true
                }
            },
            onOpenAccessibilityDisclosure = {
                accessibilityDeclined = false
                showAccessibilityExplain = true
            },
            onOpenPrivacyPolicy = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                }
            },
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
                    "FocusBro only uses this to turn Do Not Disturb on when it is currently off, " +
                        "and turns it back off when the session ends. Android asks you to grant this " +
                        "in system settings, and you can revoke it there at any time.",
                )
            },
            confirmButton = {
                Button(
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
            onDismissRequest = { /* Require explicit user button choice per Accessibility API policy */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "AccessibilityService API Disclosure",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "FocusBro uses Android's AccessibilityService API to detect distracting apps and keep you focused during active sessions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Text(
                        text = "Data Accessed & Purpose",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "• Checks the foreground app package and window class during focus to identify apps and allow system security prompts.\n" +
                            "• Detects when an unselected app opens and brings back your FocusBro screen.\n" +
                            "• Essential communication tools (Phone & SMS), System Settings, and uninstall dialogs always remain accessible.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Privacy & Security Guarantees",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "• Does NOT collect, store, or share any personal or sensitive data.\n" +
                            "• Does NOT read screen text, personal messages, passwords, or keystrokes.\n" +
                            "• Operates 100% offline (zero internet permission).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Revocable anytime in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)),
                                    )
                                }
                            },
                        ) {
                            Text("Privacy Policy")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityExplain = false
                        runCatching {
                            accessibilitySettingsLauncher.launch(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                            )
                        }
                    },
                ) {
                    Text("Agree & Continue")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAccessibilityExplain = false
                        accessibilityDeclined = true
                    },
                ) {
                    Text("Decline")
                }
            },
        )
    }

    if (showEmergencyExitConfirm) {
        AlertDialog(
            onDismissRequest = { showEmergencyExitConfirm = false },
            title = { Text("Use Emergency Exit?") },
            text = {
                Text(
                    "You are allowed 1 free emergency exit per day without breaking your streak.\n\n" +
                        "Do you want to end this focus session?",
                )
            },
            confirmButton = {
                Button(onClick = { showEmergencyExitConfirm = false }) { Text("Stay focused") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEmergencyExitConfirm = false
                        viewModel.useEmergencyExit()
                    },
                ) { Text("End session") }
            },
        )
    }

    if (showEmergencyExitExhaustedNotice) {
        AlertDialog(
            onDismissRequest = { showEmergencyExitExhaustedNotice = false },
            title = { Text("Daily Life Already Used") },
            text = {
                Text(
                    "You have already used today's free emergency exit. Ending this session early will reset your focus streak.\n\n" +
                        "Do you still want to end early?",
                )
            },
            confirmButton = {
                Button(onClick = { showEmergencyExitExhaustedNotice = false }) { Text("Stay focused") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEmergencyExitExhaustedNotice = false
                        viewModel.forceEndEarly()
                    },
                ) { Text("End anyway") }
            },
        )
    }
}
