package com.gusak.lab.radarnebo.server.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class TimeParametersTest {

    @Test
    fun timesAreEpochMillisecondsOrIso8601() {
        assertEquals(1_792_404_000_000L, parseTime("1792404000000"))
        assertEquals(Instant.parse("2026-10-19T10:00:00Z").toEpochMilliseconds(), parseTime("2026-10-19T10:00:00Z"))
        assertNull(parseTime("yesterday"))
    }
}
