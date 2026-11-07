package com.gusak.lab.radarnebo.server.api

import com.gusak.lab.radarnebo.tracking.DroneTrack
import kotlinx.serialization.Serializable

/** Tracks that were in the air in a time window, from the archive. */
@Serializable
internal data class TrackHistoryResponse(
    val from: Long,
    val to: Long,
    val tracks: List<DroneTrack>,
    val simulated: Boolean,
)
