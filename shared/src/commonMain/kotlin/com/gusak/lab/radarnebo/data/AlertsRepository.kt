package com.gusak.lab.radarnebo.data

import com.gusak.lab.radarnebo.tracking.DroneTrack
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * Alerts, tracks and the frontline from the RadarNebo server, the app's only data source. The server tracks targets and
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
    private var frontline: List<FrontlineArea> = emptyList()
    private var nextFrontlineFetchAtMs = Long.MIN_VALUE

    /** Polls [endpoint] once; [withFrontline] also keeps the frontline fresh. */
    internal suspend fun refresh(endpoint: ServerEndpoint, withFrontline: Boolean = false): AlertsUpdate {
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
        if (withFrontline) refreshFrontline(endpoint)
        return AlertsUpdate.Updated(
            AlertsSnapshot(alerts, tracks, clockOffsetMs, simulated, api.health(endpoint), frontline),
        )
    }

    /**
     * The frontline moves over days and is the largest response by far, so it is fetched once a day, not on
     * every poll. A failed request keeps the previous one and is tried again in a few minutes.
     */
    private suspend fun refreshFrontline(endpoint: ServerEndpoint) {
        val now = clock()
        if (now < nextFrontlineFetchAtMs) return
        val response = api.frontline(endpoint)
        if (response != null) frontline = response.areas
        val wait = if (response != null) FRONTLINE_REFRESH else FRONTLINE_RETRY
        nextFrontlineFetchAtMs = now + wait.inWholeMilliseconds
    }

    /** Forgets everything fetched, e.g. after switching to another server. */
    internal fun reset() {
        alerts = emptyList()
        tracks = emptyList()
        clockOffsetMs = 0L
        simulated = false
        frontline = emptyList()
        nextFrontlineFetchAtMs = Long.MIN_VALUE
    }

    private companion object {
        val FRONTLINE_REFRESH = 1.days
        val FRONTLINE_RETRY = 5.minutes
    }
}
