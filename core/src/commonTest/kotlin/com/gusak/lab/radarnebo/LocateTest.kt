package com.gusak.lab.radarnebo

import com.gusak.lab.radarnebo.data.ActiveAlertsResponse
import com.gusak.lab.radarnebo.data.Alert
import com.gusak.lab.radarnebo.data.RadarNeboApi
import com.gusak.lab.radarnebo.domain.ThreatMapper
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.geo.Regions
import com.gusak.lab.radarnebo.geo.UkraineRaions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocateTest {
    @Test
    fun hromadaSnapsToCityButRaionDoesNot() {
        val hromada = Alert(
            id = 1, locationTitle = "м. Харків та Харківська територіальна громада", locationType = "hromada",
            locationUid = "1293", locationOblastUid = 22,
        )
        val raion = hromada.copy(
            id = 2, locationTitle = "Харківський район", locationType = "raion", locationUid = "1290",
        )
        assertEquals(GeoPoint(49.9935, 36.2304), ThreatMapper.locate(hromada))
        val raionPoint = assertNotNull(ThreatMapper.locate(raion))
        assertTrue(raionPoint != GeoPoint(49.9935, 36.2304))
        assertEquals(22, Regions.oblastAt(raionPoint))
    }

    @Test
    fun raionsSitAtAStableSpotInTheirOblast() {
        val bashtanka = Alert(
            id = 1, locationTitle = "Баштанський район", locationType = "raion", locationUid = "96",
            locationOblast = "Миколаївська область",
        )
        val point = assertNotNull(ThreatMapper.locate(bashtanka))
        assertEquals(17, Regions.oblastAt(point))
        assertEquals(point, ThreatMapper.locate(bashtanka.copy(id = 2)), "the same raion lands on the same spot")
    }

    @Test
    fun raionsAreFoundUnderAnyOfTheirNames() {
        // Renamed in 2024 (Новомосковський → Самарівський); alerts may use either name. Apostrophe variants match too.
        assertEquals(Regions.raionForTitle("Самарівський район"), Regions.raionForTitle("Новомосковський район"))
        assertEquals(
            Regions.raionForTitle("Кам’янець-Подільський район"),
            Regions.raionForTitle("Кам'янець-Подільський район"),
        )
        // Every raion centre lies in its own oblast.
        UkraineRaions.all.filter { it.oblastUid != 29 }.forEach { raion ->
            assertEquals(raion.oblastUid, Regions.oblastByPolygon(raion.point) ?: raion.oblastUid, raion.names.first())
        }
    }

    @Test
    fun theOblastComesFromItsNameBecauseAlertsInUaRepeatsTheLocationUid() {
        // As alerts.in.ua sends it: location_oblast_uid is the raion's own UID, a number.
        val response = RadarNeboApi.json.decodeFromString(ActiveAlertsResponse.serializer(), """
            {"alerts":[{"id":1,"location_title":"Нікопольський район","location_type":"raion","alert_type":"air_raid",
              "location_uid":"48","location_oblast":"Дніпропетровська область","location_oblast_uid":48,
              "threats":[{"threat_type":"drones","level":"yellow"}]}]}
        """.trimIndent())
        val alert = response.alerts.single()
        assertEquals(48, alert.locationOblastUid)
        assertEquals(9, ThreatMapper.oblastUid(alert))
        assertEquals(9, Regions.oblastAt(assertNotNull(ThreatMapper.locate(alert))))
        assertEquals(9, ThreatMapper.toMarkers(response.alerts, dronesOnly = false).single().oblastUid)
    }
}
