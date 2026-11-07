package com.gusak.lab.radarnebo.tracking

import kotlinx.serialization.Serializable

@Serializable
data class DroneTracksResponse(
    /** Server clock, so clients can correct for clock skew before interpolating. */
    val serverTime: Long,
    val pollIntervalMs: Long,
    val lastUpstreamFetch: Long?,
    val tracks: List<DroneTrack>,
    /** True when the server serves simulated data; clients must label it as such. */
    val simulated: Boolean = false,
)
