package com.gusak.lab.radarnebo.server.application

import com.gusak.lab.radarnebo.server.config.DataSourceKind
import com.gusak.lab.radarnebo.server.domain.alert.AlertLocation
import kotlin.time.Duration

/**
 * Where the server's alerts come from: alerts.in.ua or the simulation. Only the server knows; clients get
 * the same API either way.
 */
interface AlertsDataSource : AutoCloseable {
    val kind: DataSourceKind

    /** For the startup log line. */
    val description: String

    /** How often to fetch. */
    val pollInterval: Duration

    /**
     * True when the source reports [SituationReport.observations], so tracks come from sightings rather than
     * alerts.
     */
    val providesObservations: Boolean

    /** Locations the source declares alerts for, when it knows them in advance. */
    val locations: Collection<AlertLocation> get() = emptyList()

    /** Requests sent upstream in the last minute and in total; zero for sources that make none. */
    val requestsInLastMinute: Int get() = 0
    val requestsTotal: Long get() = 0

    /**
     * Upstream outcomes since start, the early warning for a block (DESIGN.md §11); zero or null for sources that
     * make no requests.
     */
    val rateLimitedTotal: Long get() = 0
    val rejectedTotal: Long get() = 0
    val otherFailuresTotal: Long get() = 0
    val lastSuccessAt: Long? get() = null

    suspend fun fetch(lastModified: String?): FetchResult

    /**
     * Reports leading up to [nowMs], oldest first, replayed before the first fetch so tracks and alerts
     * have a past from the first request. Empty for sources without history.
     */
    fun history(nowMs: Long): Sequence<SituationReport> = emptySequence()

    /** Releases what the source holds, e.g. its HTTP client. Sources that hold nothing need not override it. */
    override fun close() {}
}
