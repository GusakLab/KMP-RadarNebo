package com.gusak.lab.radarnebo

import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.geo.Regions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettlementsTest {
    /** Catches coordinate typos: each settlement must fall inside its oblast's (simplified) polygon. */
    @Test
    fun everySettlementLiesInItsOblast() {
        // Right on a border or river, where the 0.02° simplified outline is off by a few km.
        val knownBorderTowns = setOf("Kherson", "Zhovti Vody")
        val cityRegions = setOf(Regions.KYIV_CITY_UID, Regions.SEVASTOPOL_UID)
        val wrong = Regions.cities
            .filter { it.oblastUid !in cityRegions && it.name !in knownBorderTowns }
            .filter { Regions.oblastByPolygon(it.point) != it.oblastUid }
            .map { "${it.name}: expected ${it.oblastUid}, found ${Regions.oblastByPolygon(it.point)}" }
        assertTrue(wrong.isEmpty(), wrong.joinToString("\n"))
    }

    @Test
    fun borderTownsResolveToTheirOwnOblast() {
        assertEquals(23, Regions.oblastAt(GeoPoint(46.6354, 32.6169)), "Kherson")
        assertEquals(9, Regions.oblastAt(GeoPoint(48.3456, 33.5023)), "Zhovti Vody")
    }

    @Test
    fun sameNameInAnotherOblastIsNotConfused() {
        // "Золочів" exists in Kharkiv and Lviv oblasts.
        assertEquals("Zolochiv", Regions.cityForTitle("Золочівська громада", 22)?.name)
        assertNull(Regions.cityForTitle("Золочівська громада", 27))
    }
}
