package dev.breakmode.session

import java.util.UUID

/** Background used while a break is running. Purely cosmetic, stored locally. */
enum class Wallpaper(val title: String) {
    AMOLED("Amoled"),
    DUSK("Dusk"),
    FOREST("Forest"),
    NIGHT("Night"),
}

/**
 * An app bucket holding up to 5 allowed apps for a specific focus mode.
 */
data class AppBucket(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val appPackages: Set<String> = emptySet(),
) {
    companion object {
        const val MAX_APPS_PER_BUCKET = 6
        const val MAX_BUCKETS = 3
    }
}

/**
 * An automated focus schedule.
 */
data class FocusSchedule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startHour: Int = 22, // 0..23
    val startMinute: Int = 0, // 0..59
    val durationMinutes: Int = 60, // minutes, including overnight routines
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // Calendar.SUNDAY(1) to Calendar.SATURDAY(7)
    val bucketId: String? = null,
    val enabled: Boolean = true,
) {
    companion object {
        const val MAX_ROUTINES = 3
        fun defaultRoutines(): List<FocusSchedule> = listOf(
            FocusSchedule(
                id = "routine_1",
                name = "Bedtime",
                startHour = 23,
                startMinute = 0,
                durationMinutes = 420,
                daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
                bucketId = "default",
                enabled = false,
            ),
            FocusSchedule(
                id = "routine_2",
                name = "Deep Work",
                startHour = 9,
                startMinute = 0,
                durationMinutes = 180,
                daysOfWeek = setOf(2, 3, 4, 5, 6),
                bucketId = "study",
                enabled = false,
            ),
            FocusSchedule(
                id = "routine_3",
                name = "Travel / Gym",
                startHour = 18,
                startMinute = 0,
                durationMinutes = 90,
                daysOfWeek = setOf(2, 3, 4, 5, 6),
                bucketId = "travel",
                enabled = false,
            ),
        )
    }
}

/** Everything the user configures before starting a break. Persisted in a private prefs file. */
data class BreakSettings(
    val minutes: Int = 30,
    /** Packages kept available during a break. Empty means "nothing but the break screen". */
    val allowedPackages: Set<String> = emptySet(),
    val pauseInterruptions: Boolean = true,
    val requireUnlockToEnd: Boolean = false,
    val wallpaper: Wallpaper = Wallpaper.AMOLED,
    val buckets: List<AppBucket> = defaultBuckets(),
    val selectedBucketId: String? = "default",
    val schedules: List<FocusSchedule> = FocusSchedule.defaultRoutines(),
) {
    companion object {
        const val MAX_DURATION_MINUTES = 16 * 60
        fun defaultBuckets(): List<AppBucket> = listOf(
            AppBucket(id = "default", name = "Default", appPackages = emptySet()),
            AppBucket(id = "study", name = "Study", appPackages = emptySet()),
            AppBucket(id = "travel", name = "Travel", appPackages = emptySet()),
        )
    }

    /** Resolves the effective allowed packages (from active bucket or custom set). */
    val effectiveAllowedPackages: Set<String>
        get() {
            val bucket = buckets.firstOrNull { it.id == selectedBucketId }
            return bucket?.appPackages ?: allowedPackages
        }
}

/**
 * A break that is currently running.
 *
 * The remaining time is always derived from [endsAtMillis] instead of being decremented by a
 * ticker, so the countdown stays correct after screen-off, process death or a device reboot.
 */
data class ActiveBreak(
    val startedAtMillis: Long,
    val endsAtMillis: Long,
    val allowedPackages: Set<String>,
    val requireUnlockToEnd: Boolean,
    /** True when this app actually changed the interruption filter and must undo it later. */
    val interruptionsPaused: Boolean,
) {
    val totalMillis: Long get() = (endsAtMillis - startedAtMillis).coerceAtLeast(1L)

    fun remainingMillis(now: Long = System.currentTimeMillis()): Long =
        (endsAtMillis - now).coerceAtLeast(0L)

    fun isOver(now: Long = System.currentTimeMillis()): Boolean = now >= endsAtMillis

    fun fractionElapsed(now: Long = System.currentTimeMillis()): Float =
        ((now - startedAtMillis).toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
}

/** One finished break, kept for the on-device summary. */
data class BreakRecord(
    val endedAtMillis: Long,
    val minutes: Int,
    val completed: Boolean,
)
