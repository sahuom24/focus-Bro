package dev.breakmode.session

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Optional Do Not Disturb support.
 *
 * Rules followed here:
 *  - Only ever act after the user has granted notification-policy access (special access, not a
 *    runtime permission).
 *  - Only turn filtering on when Do Not Disturb is currently **off**, so a mode the user set
 *    themselves is never overwritten.
 *  - Undo ourselves when the break ends.
 *
 * Note for targets >= Android 15 (API 35): an app can no longer flip the global DND state.
 * `setInterruptionFilter` becomes an app-attributed implicit Zen rule that the system combines with
 * other rules (most restrictive wins). That is another reason this app never assumes it owns DND
 * and keeps its own change strictly reversible.
 */
object Interruptions {

    /** True when the user has granted "Do Not Disturb access" to this app. */
    fun hasAccess(context: Context): Boolean = runCatching {
        notificationManager(context).isNotificationPolicyAccessGranted
    }.getOrDefault(false)

    /** Settings screen where the user grants that access, or null if no such screen exists. */
    fun accessSettingsIntent(context: Context): Intent? = runCatching {
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).takeIf {
            it.resolveActivity(context.packageManager) != null
        }
    }.getOrNull()

    /**
     * Turns interruptions off for the duration of a break.
     *
     * @return true only when this call actually changed the filter, i.e. when [resume] must run.
     */
    fun pause(context: Context): Boolean {
        if (!hasAccess(context)) return false
        return try {
            val manager = notificationManager(context)
            if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                // The user already silenced the device (or another app did). Do not touch it.
                false
            } else {
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                true
            }
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    /** Restores normal notification filtering. Safe to call more than once. */
    fun resume(context: Context) {
        if (!hasAccess(context)) return
        try {
            notificationManager(context)
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        } catch (_: SecurityException) {
            // Access was revoked mid-break; nothing to restore and nothing to report.
        } catch (_: IllegalArgumentException) {
            // Same as above.
        }
    }

    fun notificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)
}
