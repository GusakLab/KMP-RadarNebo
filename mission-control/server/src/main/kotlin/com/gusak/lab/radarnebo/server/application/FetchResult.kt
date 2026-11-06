package com.gusak.lab.radarnebo.server.application

/** What one fetch from an [AlertsDataSource] produced. */
sealed interface FetchResult {
    /** New content. [lastModified] is the source's validator for the next conditional fetch. */
    data class Fresh(val report: SituationReport, val lastModified: String? = null) : FetchResult

    /** The content is unchanged since the `lastModified` passed to [AlertsDataSource.fetch]. */
    data object NotModified : FetchResult

    /**
     * Wait at least [minWaitMs] (or the source's [retryAfterMs]) before trying again.
     * [maxWaitMs] caps the exponential backoff, so a transient outage doesn't delay recovery for long.
     */
    data class Failed(
        val message: String,
        val minWaitMs: Long,
        val retryAfterMs: Long? = null,
        val maxWaitMs: Long = 60_000,
    ) : FetchResult
}
