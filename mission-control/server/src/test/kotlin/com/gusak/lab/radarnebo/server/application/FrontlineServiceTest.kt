package com.gusak.lab.radarnebo.server.application

import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.server.domain.frontline.FrontlineSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** A source that counts its reads and reports whatever the test sets; null stands for a failed read. */
private class CountingSource(var areas: List<List<GeoPoint>>?) : FrontlineSource {
    var reads = 0

    override suspend fun fetch(): List<List<GeoPoint>>? {
        reads++
        return areas
    }
}

class FrontlineServiceTest {
    private var now = 1_000_000L
    private val area = listOf(GeoPoint(48.0, 37.5), GeoPoint(48.1, 37.6), GeoPoint(48.0, 37.7))
    private val source = CountingSource(listOf(area))
    private val service = FrontlineService(source, clock = { now }, refreshEvery = 1.hours, retryAfter = 5.minutes)

    @Test
    fun theSourceIsReadOncePerRefreshPeriod() = runTest {
        service.current()
        now += 59.minutes.inWholeMilliseconds
        service.current()

        assertEquals(1, source.reads)

        now += 1.minutes.inWholeMilliseconds
        service.current()

        assertEquals(2, source.reads)
    }

    @Test
    fun aFailedReadKeepsTheLastFrontlineAndIsRetriedSooner() = runTest {
        val first = assertNotNull(service.current())
        source.areas = null
        now += 1.hours.inWholeMilliseconds

        assertEquals(first, service.current())

        now += 5.minutes.inWholeMilliseconds
        service.current()

        assertEquals(3, source.reads)
    }

    @Test
    fun thereIsNoFrontlineUntilTheSourceHasBeenRead() = runTest {
        source.areas = null

        assertNull(service.current())
    }
}
