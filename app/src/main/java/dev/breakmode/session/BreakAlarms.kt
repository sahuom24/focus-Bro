package dev.breakmode.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Schedules the end-of-break bookkeeping so a break that runs out while the app is closed still
 * gets closed out (interruptions restored, completion notice shown).
 *
 * This is an inexact, permission-free alarm: the in-app countdown is the source of truth, the alarm
 * is only a safety net, so it is fine for it to fire a little late in Doze.
 */
object BreakAlarms {

    private const val REQUEST_CODE = 41

    fun schedule(context: Context, endsAtMillis: Long) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtMillis, pending(context))
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { manager.cancel(pending(context)) }
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, BreakAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Closes out an expired break. It never *starts* anything: [BreakRepository.syncWithClock] only
 * finishes a break whose end time has already passed.
 */
class BreakAlarmReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BreakRepository.get(context).syncWithClock()
    }
}
