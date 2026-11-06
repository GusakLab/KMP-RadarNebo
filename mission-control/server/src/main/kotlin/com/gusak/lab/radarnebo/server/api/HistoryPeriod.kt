package com.gusak.lab.radarnebo.server.api

import java.time.ZoneOffset
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal enum class HistoryPeriod(val apiValue: String) {
    MonthAgo("month_ago"),
    WeekAgo("week_ago"),
    DayAgo("day_ago");

    fun since(now: Instant): Instant = when (this) {
        MonthAgo -> now.toJavaInstant().atZone(ZoneOffset.UTC).minusMonths(1).toInstant().toKotlinInstant()
        WeekAgo -> now - 7.days
        DayAgo -> now - 1.days
    }

    companion object {
        fun from(value: String): HistoryPeriod? = entries.firstOrNull { it.apiValue == value }
    }
}
