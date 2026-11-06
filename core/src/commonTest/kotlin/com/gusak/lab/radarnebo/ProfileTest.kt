package com.gusak.lab.radarnebo

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.tracking.DroneTrack
import com.gusak.lab.radarnebo.tracking.TargetClassifier
import com.gusak.lab.radarnebo.tracking.TargetProfile
import com.gusak.lab.radarnebo.tracking.TrackMetrics
import com.gusak.lab.radarnebo.tracking.TrackSample
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileTest {
    @Test
    fun modelComesFromAlertTextOrIsMarkedProbable() {
        fun drone(text: String) = TargetClassifier.classify(ThreatType.Drones, listOf(text))
        assertEquals("Gerbera", drone(SampleMessages.GERBERA).model)
        assertEquals("Shahed-136 / Geran-2", drone("Ударні БпЛА типу Шахед").model)
        assertEquals("Shahed-238 / Geran-3", drone("Реактивний шахед на Київ").model)
        val unnamed = drone("Загроза ударних БпЛА")
        assertEquals(TargetProfile.Source.Probable, unnamed.source)
        assertEquals("Shahed-136 / Geran-2", unnamed.model)
    }

    @Test
    fun distanceFlownSumsReportedLegs() {
        val track = DroneTrack(
            id = "t", level = ThreatLevel.Red, oblastUid = null, active = false, firstSeen = 0, lastSeen = 0,
            markerIds = emptyList(),
            samples = listOf(
                TrackSample(0, 50.0, 30.0, "A"),
                TrackSample(1, 50.0, 31.0, "B"),
                TrackSample(2, 51.0, 31.0, "C"),
            ),
        )
        // ~71.5 km east at 50°N, then ~111 km north.
        assertEquals(71.5 + 111.2, TrackMetrics.distanceFlownKm(track), 1.5)
    }
}
