package dev.breakmode.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.breakmode.ui.MainActivity
import java.util.Calendar

/**
 * Manages background alarms for automated focus mode schedules.
 */
object FocusScheduleManager {

    private const val REQUEST_CODE = 42
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"

    fun canScheduleExact(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(AlarmManager::class.java)
            manager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (manager.canScheduleExactAlarms()) {
                        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, earliestMillis, pending)
                    } else {
                        // setAlarmClock is guaranteed to fire at exact time without SCHEDULE_EXACT_ALARM permission
                        val showIntent = PendingIntent.getActivity(
                            context,
                            REQUEST_CODE + 1,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                        manager.setAlarmClock(AlarmManager.AlarmClockInfo(earliestMillis, showIntent), pending)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, earliestMillis, pending)
                } else {
                    manager.setExact(AlarmManager.RTC_WAKEUP, earliestMillis, pending)
                }
            }.onFailure {
                // Last-resort fallback for constrained OEMs
                runCatching {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, earliestMillis, pending)
                }
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

    /**
     * Checks if current clock time falls inside an active scheduled focus window.
     */
    fun isCurrentlyInSchedule(schedule: FocusSchedule, nowCal: Calendar = Calendar.getInstance()): Boolean {
        return getScheduledEndMillis(schedule, nowCal) != null
    }

    /**
     * Returns the scheduled end timestamp in millis if currently inside the focus window, or null otherwise.
     */
    fun getScheduledEndMillis(schedule: FocusSchedule, nowCal: Calendar = Calendar.getInstance()): Long? {
        if (!schedule.enabled || schedule.daysOfWeek.isEmpty()) return null
        val nowMillis = nowCal.timeInMillis
        for (dayOffset in listOf(-1, 0)) {
            val start = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, schedule.startHour)
                set(Calendar.MINUTE, schedule.startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayOfWeek = start.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek in schedule.daysOfWeek) {
                val endMillis = start.timeInMillis + schedule.durationMinutes * 60_000L
                if (nowMillis in start.timeInMillis until endMillis) {
                    return endMillis
                }
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

            // Bring the focus mode session screen directly to the foreground
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            runCatching { context.startActivity(activityIntent) }
        }

        // Re-arm for the next scheduled occurrence
        FocusScheduleManager.updateNextAlarm(context, schedules)
    }
}

/**
 * Restores alarms when the device boots or the app package is updated.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val repository = BreakRepository.get(context)
            repository.syncWithClock()
            FocusScheduleManager.updateNextAlarm(context, repository.settings.value.schedules)
        }
    }
}
