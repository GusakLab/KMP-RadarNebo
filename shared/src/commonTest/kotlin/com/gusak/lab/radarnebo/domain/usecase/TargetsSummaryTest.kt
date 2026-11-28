package com.gusak.lab.radarnebo.domain.usecase

import com.gusak.lab.radarnebo.data.Alert
import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.tracking.DroneTrack
import com.gusak.lab.radarnebo.tracking.DroneTracksResponse
import com.gusak.lab.radarnebo.tracking.TargetStatsResponse
import com.gusak.lab.radarnebo.tracking.TrackSample
import kotlin.test.Test
import kotlin.test.assertEquals

class TargetsSummaryTest {
    private val now = 10_000_000L

    private fun track(id: String, active: Boolean) = DroneTrack(
        id = id,
        level = ThreatLevel.Red,
        oblastUid = 14,
        active = active,
        firstSeen = now - 60_000,
        lastSeen = now - 60_000,
        markerIds = emptyList(),
        samples = listOf(TrackSample(now - 60_000, 50.4, 30.5, "Kyiv")),
    )

    @Test
    fun onlyTargetsInTheAirAreShown() {
        val response = DroneTracksResponse(
            serverTime = now,
            pollIntervalMs = 15_000,
            lastUpstreamFetch = now,
            tracks = listOf(track("flying", active = true), track("ended", active = false)),
        )
        val stats = TargetStatsResponse(serverTime = now, today = 5, yesterday = 7, thisMonth = 90)

        val summary = TargetsSummary.of(response, alerts = emptyList(), stats)

        assertEquals(listOf("flying"), summary.targets.map { it.trackId })
        assertEquals(stats, summary.stats)
        assertEquals(now, summary.atMs)
    }

    @Test
    fun oblastsUnderAlertAreMarkedWholeOrInPart() {
        val response = DroneTracksResponse(
            serverTime = now,
            pollIntervalMs = 15_000,
            lastUpstreamFetch = now,
            tracks = emptyList(),
        )
        val alerts = listOf(
            Alert(1, locationType = "oblast", locationOblast = "Харківська область"),
            Alert(2, locationType = "raion", locationOblast = "Київська область"),
        )

        val summary = TargetsSummary.of(response, alerts, stats = null)

        assertEquals(mapOf(22 to true, 14 to false), summary.alertedOblasts)
    }
}
