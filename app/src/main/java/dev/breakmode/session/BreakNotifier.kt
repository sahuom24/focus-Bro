package dev.breakmode.session

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.breakmode.R
import dev.breakmode.ui.MainActivity
import java.text.DateFormat
import java.util.Date

/**
 * Two small notifications and nothing else: the running break (silent, ongoing, with a live
 * countdown) and a one-off "break complete" note. No notification content is ever personalised
 * with app names or usage data.
 */
object BreakNotifier {

    private const val CHANNEL_ONGOING = "break_in_progress"
    private const val CHANNEL_DONE = "break_complete"
    private const val ID_ONGOING = 1001
    private const val ID_DONE = 1002

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                "Break in progress",
                // Low importance: the ongoing notice is a status line, never a ping.
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows the time left in your current break."
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DONE,
                "Break complete",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "One short notice when a break finishes."
                setShowBadge(false)
            },
        )
    }

    fun showOngoing(context: Context, active: ActiveBreak) {
        ensureChannels(context)
        val endsAt = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(active.endsAtMillis))
        val notification = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_break)
            .setContentTitle("Break in progress")
            .setContentText("Ends at $endsAt")
            .setContentIntent(openApp(context))
            .setWhen(active.endsAtMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        post(context, ID_ONGOING, notification)
    }

    fun clearOngoing(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(ID_ONGOING) }
    }

    fun showComplete(context: Context, minutes: Int) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_break)
            .setContentTitle("Break complete")
            .setContentText("$minutes ${if (minutes == 1) "minute" else "minutes"} unplugged.")
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        post(context, ID_DONE, notification)
    }

    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        // POST_NOTIFICATIONS is a runtime permission on Android 13+. If the user declined it the
        // break still runs; only the status line is missing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
