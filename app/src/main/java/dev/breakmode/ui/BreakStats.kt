package dev.breakmode.ui

import dev.breakmode.session.BreakRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Minutes of break per day for the last seven days, oldest first. */
fun lastSevenDays(records: List<BreakRecord>, zone: ZoneId = ZoneId.systemDefault()): List<Int> {
    val today = LocalDate.now(zone)
    val perDay = records.groupBy { Instant.ofEpochMilli(it.endedAtMillis).atZone(zone).toLocalDate() }
        .mapValues { (_, day) -> day.sumOf { it.minutes } }
    return (6 downTo 0).map { offset -> perDay[today.minusDays(offset.toLong())] ?: 0 }
}

fun todayMinutes(records: List<BreakRecord>, zone: ZoneId = ZoneId.systemDefault()): Int {
    val today = LocalDate.now(zone)
    return records
        .filter { Instant.ofEpochMilli(it.endedAtMillis).atZone(zone).toLocalDate() == today }
        .sumOf { it.minutes }
}

/** Countdown text: "29:59", or "1:05:00" for breaks longer than an hour. */
fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

/** Compact duration label such as "45 min" or "1 h 30 min". */
fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
