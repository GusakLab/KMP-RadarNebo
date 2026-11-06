package com.gusak.lab.radarnebo

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.tracking.DroneTrack
import com.gusak.lab.radarnebo.tracking.Place
import com.gusak.lab.radarnebo.tracking.TrackPredictor
import com.gusak.lab.radarnebo.tracking.TrackSample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PredictionTest {
    private val minute = 60_000L

    /** A group reported east of Kyiv 10 minutes apart, flying due west at ~150 km/h. */
    private fun westbound(active: Boolean = true) = DroneTrack(
        id = "t", level = ThreatLevel.Red, oblastUid = 14, active = active, firstSeen = 0, lastSeen = 10 * minute,
        markerIds = listOf("m"),
        samples = listOf(
            TrackSample(0, 50.45, 31.40, "A", 14),
            TrackSample(10 * minute, 50.45, 31.05, "B", 14),
        ),
    )

    @Test
    fun predictsNextCityAheadWithEta() {
        val p = assertNotNull(TrackPredictor.predict(westbound(), 10 * minute))
        // Brovary lies on the way to Kyiv, so it is the next settlement.
        assertEquals("Brovary", p.place.name)
        assertTrue(p.distanceKm in 15.0..25.0, "distance ${p.distanceKm}")
        // ~25 km in 10 min = ~150 km/h, so ~19 km takes ~8 min.
        assertTrue(p.etaMinutes in 6..10, "eta ${p.etaMinutes}")
    }

    @Test
    fun ignoresPlacesBehindTooFarOrOffTheCorridor() {
        val behind = Place("Behind", GeoPoint(50.45, 31.50))
        val far = Place("Far", GeoPoint(50.45, 30.0))
        val aside = Place("Aside", GeoPoint(50.75, 30.85))
        assertNull(TrackPredictor.predict(westbound(), 10 * minute, listOf(behind, far, aside)))
    }

    @Test
    fun favoritesCanBeTheTarget() {
        val home = Place("Home", GeoPoint(50.46, 30.85), isFavorite = true)
        val p = assertNotNull(TrackPredictor.predict(westbound(), 10 * minute, TrackPredictor.cityPlaces + home))
        assertEquals("Home", p.place.name, "the first place along the path wins")
    }

    @Test
    fun placeOnThePathBeatsAnEarlierOneOffToTheSide() {
        // Boryspil comes first along the path but ~11 km aside; Brovary is almost on the line.
        val boryspil = Place("Boryspil", GeoPoint(50.3527, 30.9550))
        val brovary = Place("Brovary", GeoPoint(50.5110, 30.7909))
        val onTheLine = TrackPredictor.predict(westbound(), 10 * minute, listOf(boryspil, brovary))
        assertEquals("Brovary", onTheLine?.place?.name)
        // With nothing on the line, the wider corridor still finds the town the drone passes.
        assertEquals("Boryspil", TrackPredictor.predict(westbound(), 10 * minute, listOf(boryspil))?.place?.name)
    }

    @Test
    fun noPredictionForClearedOrSinglePointTracks() {
        assertNull(TrackPredictor.predict(westbound(active = false), 10 * minute))
        val single = westbound().let { it.copy(samples = it.samples.take(1)) }
        assertNull(TrackPredictor.predict(single, 10 * minute))
    }

    @Test
    fun predictedRouteListsSettlementsAheadInOrder() {
        val route = TrackPredictor.route(westbound(), 10 * minute)
        assertTrue(route.size >= 2, "route: ${route.map { it.place.name }}")
        assertEquals("Brovary", route.first().place.name)
        assertTrue("Kyiv" in route.map { it.place.name })
        assertTrue(route.zipWithNext().all { (x, y) -> y.etaMinutes >= x.etaMinutes }, "ETAs increase along the route")
    }
}
