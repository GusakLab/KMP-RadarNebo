package com.gusak.lab.radarnebo.missioncontrol

import kotlinx.serialization.Serializable

/**
 * The server's `/health` report (mission-control/server's HealthRoutes.kt). Every field has a default, so a server
 * that adds or drops one still parses.
 */
@Serializable
internal data class Health(
    val status: String = "",
    val source: String = "",
    val simulated: Boolean = false,
    val upstreamBudgetPerMinute: Int? = null,
    val upstreamRequestsLastMinute: Int = 0,
    val upstreamRequestsTotal: Long = 0,
    val upstreamRateLimitedTotal: Long = 0,
    val upstreamRejectedTotal: Long = 0,
    val upstreamOtherFailuresTotal: Long = 0,
    val upstreamLastSuccessAt: Long? = null,
    val pollIntervalSeconds: Long = 0,
    val lastAttemptAt: Long? = null,
    val nextPollAt: Long? = null,
    val dataVersion: Long? = null,
    val dataConfirmedAt: Long? = null,
    val dataAgeSeconds: Long? = null,
    val alerts: Int = 0,
    val tracks: Int = 0,
    val activeTracks: Int = 0,
    val lastError: String? = null,
)
