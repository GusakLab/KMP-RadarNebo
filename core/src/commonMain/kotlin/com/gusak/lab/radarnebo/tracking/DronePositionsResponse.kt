package com.gusak.lab.radarnebo.tracking

import kotlinx.serialization.Serializable

@Serializable
data class DronePositionsResponse(
    val at: Long,
    val positions: List<InterpolatedPosition>,
)
