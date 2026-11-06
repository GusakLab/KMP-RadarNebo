package com.gusak.lab.radarnebo.server.domain

import com.gusak.lab.radarnebo.server.domain.tracking.TrackQuery
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TrackQueryTest {
    @Test
    fun aQueryNeedsAnOrderedWindowAndABoundedLimit() {
        assertFailsWith<IllegalArgumentException> { TrackQuery(fromMs = 2, toMs = 1) }
        assertFailsWith<IllegalArgumentException> { TrackQuery(fromMs = 0, toMs = 1, limit = 0) }
        assertFailsWith<IllegalArgumentException> { TrackQuery(fromMs = 0, toMs = 1, limit = TrackQuery.MAX_LIMIT + 1) }
        TrackQuery(fromMs = 1, toMs = 1, limit = TrackQuery.MAX_LIMIT)
    }

    @Test
    fun aQueryCoversAtMostAWeek() {
        val week = TrackQuery.MAX_WINDOW.inWholeMilliseconds
        TrackQuery(fromMs = 0, toMs = week)
        assertFailsWith<IllegalArgumentException> { TrackQuery(fromMs = 0, toMs = week + 1) }
        assertFailsWith<IllegalArgumentException>("an overflowing window") {
            TrackQuery(fromMs = Long.MIN_VALUE, toMs = Long.MAX_VALUE)
        }
    }
}
