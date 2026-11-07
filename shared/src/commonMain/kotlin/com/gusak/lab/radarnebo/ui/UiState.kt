package com.gusak.lab.radarnebo.ui

import com.gusak.lab.radarnebo.data.AppSettings
import com.gusak.lab.radarnebo.data.FavoriteLocation
import com.gusak.lab.radarnebo.data.ServerHealth
import com.gusak.lab.radarnebo.domain.ProximityHit
import com.gusak.lab.radarnebo.domain.ThreatMarker
import com.gusak.lab.radarnebo.tracking.DroneTrack
import kotlin.time.Instant

data class UiState(
    val settings: AppSettings = AppSettings(),
    val favorites: List<FavoriteLocation> = emptyList(),
    /** All threats after the drones-only filter, priority sorted. */
    val threats: List<ThreatMarker> = emptyList(),
    /** Threats actually drawn on the map, capped by [AppSettings.maxMapMarkers]. */
    val mapThreats: List<ThreatMarker> = emptyList(),
    val proximity: List<ProximityHit> = emptyList(),
    /** Oblast UIDs with any active alert, used to shade the map. */
    val alertedOblasts: Map<Int, Boolean> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Instant? = null,
    /** Target position history from the server (shown classes only); the app interpolates between reports. */
    val tracks: List<DroneTrack> = emptyList(),
    /** [tracks] including those the target filter hides. */
    val allTracks: List<DroneTrack> = tracks,
    /** Server clock minus local clock, applied before interpolating track positions. */
    val clockOffsetMs: Long = 0,
    /** The server's data is simulated, not the real situation; always shown as such. */
    val simulated: Boolean = false,
    /** The server's last health report; null while it can't be reached. */
    val serverHealth: ServerHealth? = null,
    /** When the app tries the server again after a failed poll; null while polls succeed. */
    val nextRetryAt: Instant? = null,
)
