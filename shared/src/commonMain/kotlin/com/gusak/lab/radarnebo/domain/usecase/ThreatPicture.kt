package com.gusak.lab.radarnebo.domain.usecase

import com.gusak.lab.radarnebo.data.AppSettings
import com.gusak.lab.radarnebo.domain.ProximityHit
import com.gusak.lab.radarnebo.domain.ThreatMarker
import com.gusak.lab.radarnebo.tracking.DroneTrack

/** What the screens show, derived from the raw alerts and tracks under the current settings. */
data class ThreatPicture(
    /** All threats after the drones-only filter, priority sorted. */
    val threats: List<ThreatMarker>,
    /** Threats actually drawn on the map, capped by [AppSettings.maxMapMarkers]. */
    val mapThreats: List<ThreatMarker>,
    val proximity: List<ProximityHit>,
    /** Oblast UID to whether the alert covers the whole oblast (true) or only part of it (false). */
    val alertedOblasts: Map<Int, Boolean>,
    /**
     * Tracks the settings show: active or seen within the timeline window, only drones while "drones only" is on,
     * and only kinds the target filter allows.
     */
    val tracks: List<DroneTrack>,
    /** [tracks] before the target filter, for the Targets list to count and offer what is hidden. */
    val unfilteredTracks: List<DroneTrack> = tracks,
)
