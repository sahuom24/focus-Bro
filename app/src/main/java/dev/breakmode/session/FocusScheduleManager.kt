package dev.breakmode.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Manages background alarms for automated focus mode schedules.
 */
object FocusScheduleManager {

    private const val REQUEST_CODE = 42
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"

    fun updateNextAlarm(context: Context, schedules: List<FocusSchedule>) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val enabledSchedules = schedules.filter { it.enabled }
        if (enabledSchedules.isEmpty()) {
            cancel(context)
            return
        }

        // Find the next earliest schedule to trigger
        var earliestMillis: Long? = null
        var earliestScheduleId: String? = null

        for (schedule in enabledSchedules) {
            val trigger = nextTriggerMillis(schedule) ?: continue
            if (earliestMillis == null || trigger < earliestMillis) {
                earliestMillis = trigger
                earliestScheduleId = schedule.id
            }
        }

        if (earliestMillis != null && earliestScheduleId != null) {
            val intent = Intent(context, FocusScheduleReceiver::class.java).apply {
                putExtra(EXTRA_SCHEDULE_ID, earliestScheduleId)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            runCatching {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, earliestMillis, pending)
            }
        } else {
            cancel(context)
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, FocusScheduleReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pending != null) {
            runCatching { manager.cancel(pending) }
        }
    }

    fun nextTriggerMillis(schedule: FocusSchedule): Long? {
        if (!schedule.enabled || schedule.daysOfWeek.isEmpty()) return null
        val now = Calendar.getInstance()
        for (dayOffset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, schedule.startHour)
                set(Calendar.MINUTE, schedule.startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek in schedule.daysOfWeek && candidate.timeInMillis > now.timeInMillis) {
                return candidate.timeInMillis
            }
        }
        return null
    }
}

/**
 * Receiver that triggers when a scheduled focus mode starts.
 */
class FocusScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra(FocusScheduleManager.EXTRA_SCHEDULE_ID) ?: return
        val repository = BreakRepository.get(context)
        val schedules = repository.settings.value.schedules
        val schedule = schedules.firstOrNull { it.id == scheduleId && it.enabled }

        if (schedule != null) {
            repository.startScheduled(schedule)
        }

        // Re-arm for the next scheduled occurrence
        FocusScheduleManager.updateNextAlarm(context, schedules)
    }
}
