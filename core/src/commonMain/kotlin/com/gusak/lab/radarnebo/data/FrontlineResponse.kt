package com.gusak.lab.radarnebo.data

import kotlinx.serialization.Serializable

/**
 * The territory Russia occupies, as `/v1/frontline.json` serves it. The frontline is the edge of [areas] inside
 * Ukraine; areas may touch or overlap, so draw their union.
 */
@Serializable
data class FrontlineResponse(
    /** When the server got [areas] from its source, in epoch milliseconds. */
    val fetchedAt: Long,
    val areas: List<FrontlineArea> = emptyList(),
)

/** One occupied area: its outline as lon,lat pairs packed in [lonLat], like `OblastShape`. */
@Serializable
data class FrontlineArea(val lonLat: List<Double>)
