package com.gusak.lab.radarnebo.data

import com.gusak.lab.radarnebo.tracking.DroneTrack
import kotlin.time.Clock

/**
 * Alerts and tracks from the RadarNebo server, the app's only data source. The server tracks targets and
 * decides where its data comes from; the app only reads and, between polls, interpolates positions.
 */
class AlertsRepository(
    private val api: RadarNeboApi,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private var alerts: List<Alert> = emptyList()
    private var tracks: List<DroneTrack> = emptyList()
    private var clockOffsetMs = 0L
    private var simulated = false

    /** Polls [endpoint] once. */
    internal suspend fun refresh(endpoint: ServerEndpoint): AlertsUpdate {
        when (val result = api.activeAlerts(endpoint)) {
            is FetchResult.Fresh -> alerts = result.alerts
            is FetchResult.Failure -> return AlertsUpdate.Failed(result.message, result.backOff)
        }
        // Tracks move even when alerts don't, so they are fetched on every poll. A failed request keeps the
        // previous tracks rather than wiping the map.
        api.tracks(endpoint)?.let { response ->
            tracks = response.tracks
            clockOffsetMs = response.serverTime - clock()
            simulated = response.simulated
        }
        return AlertsUpdate.Updated(AlertsSnapshot(alerts, tracks, clockOffsetMs, simulated, api.health(endpoint)))
    }

    /** Forgets everything fetched, e.g. after switching to another server. */
    internal fun reset() {
        alerts = emptyList()
        tracks = emptyList()
        clockOffsetMs = 0L
        simulated = false
    }
}
