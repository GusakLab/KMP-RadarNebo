package com.gusak.lab.radarnebo.server.domain.tracking

import com.gusak.lab.radarnebo.tracking.DroneTrack

internal data class TrackingUpdate(
    /** Every track worth serving: active ones and recently ended ones. */
    val tracks: List<DroneTrack>,
    /** Tracks that ended in this update, to be archived. */
    val ended: List<DroneTrack>,
)
