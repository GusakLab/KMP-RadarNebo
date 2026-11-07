package com.gusak.lab.radarnebo.server.domain

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.server.Fixtures
import com.gusak.lab.radarnebo.server.Fixtures.alert
import com.gusak.lab.radarnebo.server.domain.alert.AirRaidStatus
import com.gusak.lab.radarnebo.server.domain.alert.AirRaidStatusMap
import com.gusak.lab.radarnebo.server.domain.alert.IotVersion
import com.gusak.lab.radarnebo.server.domain.alert.LocationDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AirRaidStatusTest {
    private val directory = LocationDirectory(listOf(Fixtures.BROVARY, Fixtures.KHARKIV))

    @Test
    fun oblastStringsFollowTheDocumentedOrderAndLevels() {
        val alerts = listOf(
            // Crimea (first in the string) under a whole-region red alert, Kharkiv oblast only partly.
            alert(1, Fixtures.region(29), threat = null),
            alert(2, Fixtures.KHARKIV),
            // Kyiv city with a yellow (preliminary) threat.
            alert(3, Fixtures.region(31), level = ThreatLevel.Yellow),
        )
        val status = AirRaidStatusMap(alerts, directory)
        val v1 = status.byOblast(IotVersion.V1)
        val v2 = status.byOblast(IotVersion.V2)
        assertEquals(27, v1.length)
        val order = AirRaidStatusMap.OBLAST_ORDER
        assertEquals('A', v1[order.indexOf(29)])
        assertEquals('R', v2[order.indexOf(29)])
        assertEquals('P', v1[order.indexOf(22)])
        assertEquals('A', v1[order.indexOf(31)], "v1 does not grade alerts")
        assertEquals('Y', v2[order.indexOf(31)])
        assertEquals(24, v2.count { it == 'N' })
    }

    @Test
    fun aLocationIsAlertedByItsOwnAlertOrByOneOverItsWholeOblast() {
        val alerts = listOf(alert(1, Fixtures.region(14)), alert(2, Fixtures.KHARKIV, level = ThreatLevel.Yellow))
        val status = AirRaidStatusMap(alerts, directory)
        assertEquals(
            AirRaidStatus.Active,
            status.location(Fixtures.BROVARY.uid),
            "Brovary lies in Kyiv oblast, alerted as a whole",
        )
        assertEquals(AirRaidStatus.Preliminary, status.location(Fixtures.KHARKIV.uid))
        assertEquals(AirRaidStatus.Partial, status.location("22"))
        assertEquals(AirRaidStatus.None, status.location("31"))
        assertEquals(AirRaidStatus.None, status.location("999999"), "unknown locations have no alert")

        val byUid = status.byUid(IotVersion.V2)
        assertEquals(1294, byUid.length, "up to the largest known UID")
        assertEquals('R', byUid[1100])
        assertEquals('Y', byUid[1293])
        assertEquals('P', byUid[22])
        assertEquals(' ', byUid[500], "no location has UID 500")
        assertTrue(LocationDirectory.OBLASTS.all { byUid[it.uid.toInt()] != ' ' }, "every region has its place")
    }
}
