package dev.breakmode.blocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telecom.TelecomManager
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import dev.breakmode.apps.DefaultCommunicationApps
import dev.breakmode.session.BreakRepository
import dev.breakmode.ui.MainActivity

/**
 * Enforces focus mode by restricting non-essential apps while ensuring essential
 * Android system controls (notifications, DND, quick panel, sound, volume, incoming calls)
 * remain fully accessible.
 */
class FocusAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastToastTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 50
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString().orEmpty()

        checkAndEnforce(packageName, className)
    }

    private fun checkAndEnforce(packageName: String, className: String) {
        val repository = BreakRepository.get(applicationContext)
        val active = repository.active.value ?: return

        if (active.isOver()) {
            repository.syncWithClock()
            return
        }

        // Always allow our own app
        if (packageName == applicationContext.packageName) return

        // Always allow the apps explicitly chosen by the user for this break
        if (packageName in active.allowedPackages) return

        // Always allow essential Android system UI (notifications, quick panel, DND, volume, lock screen)
        if (packageName in SYSTEM_PACKAGES) return

        // Always allow active input methods (keyboards)
        if (isInputMethod(packageName)) return

        // Always allow phone calls / dialer for safety
        if (isDialerOrInCall(packageName)) return

        // Always allow default messaging app (SMS)
        if (isDefaultMessagesApp(packageName)) return

        // Do not allow exiting to home screen: lock user in the focus screen
        if (isDefaultLauncher(packageName)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            return
        }

        // For system settings: allow essential controls (DND, Sound, Notifications, Volume, Panels)
        // but block screens that can be used to kill or tamper with the app
        if (packageName == "com.android.settings") {
            if (isAllowedSettingsScreen(className)) {
                return
            }
        }

        // The app is not allowed: restrict it immediately
        blockApp(packageName)
    }

    private fun blockApp(packageName: String) {
        // Return to home first so the restricted app is dismissed
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Bring the focus mode session screen to the foreground
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_BLOCKED_PACKAGE, packageName)
        }
        startActivity(intent)

        val now = System.currentTimeMillis()
        if (now - lastToastTime > TOAST_INTERVAL_MS) {
            lastToastTime = now
            val label = getAppLabel(packageName)
            mainHandler.post {
                Toast.makeText(
                    applicationContext,
                    "$label is restricted during focus mode",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun isDefaultLauncher(packageName: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolve = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolve?.activityInfo?.packageName == packageName
        }.getOrDefault(false)
    }

    private fun isInputMethod(packageName: String): Boolean {
        return runCatching {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
            imm.inputMethodList.any { it.packageName == packageName }
        }.getOrDefault(false)
    }

    private fun isDialerOrInCall(packageName: String): Boolean {
        if (packageName == DefaultCommunicationApps.getPhonePackage(applicationContext)) return true
        return runCatching {
            val telecom = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecom?.defaultDialerPackage == packageName
        }.getOrDefault(false)
    }

    private fun isDefaultMessagesApp(packageName: String): Boolean {
        val defaultSms = DefaultCommunicationApps.getMessagesPackage(applicationContext)
        if (!defaultSms.isNullOrBlank() && packageName == defaultSms) return true
        return packageName == "com.google.android.apps.messaging" ||
            packageName == "com.android.mms" ||
            packageName == "com.samsung.android.messaging"
    }

    private fun isAllowedSettingsScreen(className: String): Boolean {
        val lower = className.lowercase()
        // Strictly prevent tampering / force-stopping
        if (lower.contains("installedappdetails") ||
            lower.contains("manageapplications") ||
            lower.contains("appinfo") ||
            lower.contains("specialaccess")
        ) {
            return false
        }
        // Essential Android settings (DND, sound, notifications, panels, quick settings)
        return lower.contains("zenmode") ||
            lower.contains("sound") ||
            lower.contains("notification") ||
            lower.contains("volume") ||
            lower.contains("panel") ||
            lower.contains("display") ||
            lower.contains("bluetooth") ||
            lower.contains("wifi") ||
            lower.contains("network")
    }

    private fun getAppLabel(packageName: String): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault("This app")
    }

    override fun onInterrupt() {
        // AccessibilityService required method
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "dev.breakmode.extra.BLOCKED_PACKAGE"
        private const val TOAST_INTERVAL_MS = 2000L

        val SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.systemui",
            "com.samsung.android.app.cocktailbarservice",
            "com.miui.systemui",
            "com.huawei.systemui",
            "com.oppo.systemui",
            "com.oneplus.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.phone",
            "com.android.server.telecom",
            "com.google.android.dialer",
            "com.samsung.android.incallui",
        )

        fun isServiceEnabled(context: Context): Boolean {
            return runCatching {
                val expectedComponentName =
                    ComponentName(context, FocusAccessibilityService::class.java).flattenToString()
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                ) ?: return false
                val splitter = TextUtils.SimpleStringSplitter(':')
                splitter.setString(enabledServices)
                while (splitter.hasNext()) {
                    val service = splitter.next()
                    if (service.equals(expectedComponentName, ignoreCase = true) ||
                        service.contains(context.packageName, ignoreCase = true)
                    ) {
                        return true
                    }
                }
                false
            }.getOrDefault(false)
        }
    }
}
