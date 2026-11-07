package com.gusak.lab.radarnebo.data

import kotlinx.serialization.Serializable

/** The server's `/health` report: whether it is getting data, and from where. Unknown fields are ignored. */
@Serializable
data class ServerHealth(
    /**
     * `ok`, `degraded` (the data source is failing; the last data is still served), `stale` (the data is too old)
     * or `warming_up`.
     */
    val status: String,
    /** `AlertsInUa` or `Simulation`. */
    val source: String = "",
    val simulated: Boolean = false,
    val upstreamBudgetPerMinute: Int? = null,
    val upstreamRequestsLastMinute: Int = 0,
    val pollIntervalSeconds: Long = 0,
    val lastAttemptAt: Long? = null,
    val dataConfirmedAt: Long? = null,
    val lastError: String? = null,
) {
    val isOk: Boolean get() = status == "ok"
}
