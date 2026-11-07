package com.gusak.lab.radarnebo.ui

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Instant

internal fun formatTime(instant: Instant): String {
    val t = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

/** Local wall-clock time with seconds, e.g. `23:14:05`, for an epoch-millisecond timestamp. */
fun formatClock(epochMs: Long): String {
    val t = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    return listOf(t.hour, t.minute, t.second).joinToString(":") { it.toString().padStart(2, '0') }
}

/** How long before [now] (local time) [instant] was: "just now", "5 min ago", "1 h 20 min ago". */
internal fun formatAgo(instant: Instant, now: Instant): String {
    val minutes = (now - instant).inWholeMinutes
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60} h ${minutes % 60} min ago"
    }
}

/** "10 min", "1 h", "1 h 30 min". */
internal fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

/** "50 m", "2 km", "1.5 km". */
internal fun formatAltitude(meters: Int): String = when {
    meters < 1_000 -> "$meters m"
    meters % 1_000 == 0 -> "${meters / 1_000} km"
    else -> "${meters / 1_000}.${meters % 1_000 / 100} km"
}

internal fun formatCoord(value: Double): String {
    val scaled = (value * 10_000).roundToLong()
    val whole = scaled / 10_000
    val fraction = (kotlin.math.abs(scaled) % 10_000).toString().padStart(4, '0')
    return "$whole.$fraction"
}
