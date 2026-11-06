package com.gusak.lab.radarnebo.server.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class HistoryPeriodTest {

    @Test
    fun aMonthAgoIsACalendarMonthClampedToTheEndOfTheMonth() {
        val now = Instant.parse("2026-03-31T12:00:00Z")
        assertEquals(Instant.parse("2026-02-28T12:00:00Z"), HistoryPeriod.MonthAgo.since(now))
        assertEquals(now - 7.days, HistoryPeriod.WeekAgo.since(now))
        assertEquals(now - 1.days, HistoryPeriod.DayAgo.since(now))
    }
}
