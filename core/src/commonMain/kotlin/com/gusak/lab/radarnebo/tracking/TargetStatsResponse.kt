package com.gusak.lab.radarnebo.tracking

import kotlinx.serialization.Serializable

/**
 * How many targets were in the air, live or archived, in calendar periods of Kyiv time. A target in the air across
 * midnight counts in both days.
 */
@Serializable
data class TargetStatsResponse(
    val serverTime: Long,
    val today: Int,
    val yesterday: Int,
    val thisMonth: Int,
    /** True when the server serves simulated data; clients must label it as such. */
    val simulated: Boolean = false,
)
