package com.gusak.lab.radarnebo.server.domain

import com.gusak.lab.radarnebo.server.Fixtures
import com.gusak.lab.radarnebo.server.Fixtures.alert
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class AirAlertTest {
    private val t0 = Fixtures.T0

    @Test
    fun aRegionCoversThePlacesInItAndAnyOtherLocationOnlyItself() {
        val brovary = alert(1, Fixtures.BROVARY)
        assertTrue(brovary.concerns(Fixtures.BROVARY.uid))
        assertTrue(brovary.concerns("14"), "Brovary lies in Kyiv oblast")
        assertFalse(brovary.concerns("22"), "not in Kharkiv oblast")

        val kyivOblast = alert(2, Fixtures.region(14))
        assertTrue(kyivOblast.concerns("14"))
        assertFalse(
            kyivOblast.concerns(Fixtures.BROVARY.uid),
            "a hromada's history does not include its oblast's alerts",
        )
        assertFalse(alert(3, Fixtures.KHARKIV).concerns("1100"))
    }

    @Test
    fun anAlertIsActiveSinceAnyTimeUntilItFinishes() {
        val active = alert(1, Fixtures.BROVARY)
        assertTrue(active.activeSince(t0 + 1.days), "still active")
        val finished = active.finish(t0 + 1.hours)
        assertTrue(finished.activeSince(t0 + 1.hours), "finished at the very moment")
        assertFalse(finished.activeSince(t0 + 2.hours))
    }
}
