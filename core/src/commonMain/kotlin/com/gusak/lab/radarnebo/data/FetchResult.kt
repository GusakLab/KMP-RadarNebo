package com.gusak.lab.radarnebo.data

sealed interface FetchResult {
    data class Fresh(val alerts: List<Alert>) : FetchResult

    /**
     * [backOff]: the server asked the app to slow down (rate limit) or refused it (auth), so retrying soon
     * won't help.
     */
    data class Failure(val message: String, val backOff: Boolean = false) : FetchResult
}
