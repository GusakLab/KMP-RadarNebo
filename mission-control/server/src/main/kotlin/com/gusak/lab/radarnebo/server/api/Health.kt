package com.gusak.lab.radarnebo.server.api

import com.gusak.lab.radarnebo.server.application.SituationPoller
import com.gusak.lab.radarnebo.server.application.SituationService
import kotlinx.serialization.Serializable

/** For operators and load balancers; not part of the client API, so it needs no client key. */
@Serializable
data class Health(
    val status: String,
    val source: String,
    val simulated: Boolean,
    val upstreamBudgetPerMinute: Int?,
    val upstreamRequestsLastMinute: Int,
    val upstreamRequestsTotal: Long,
    val upstreamRateLimitedTotal: Long,
    val upstreamRejectedTotal: Long,
    val upstreamOtherFailuresTotal: Long,
    val upstreamLastSuccessAt: Long?,
    val pollIntervalSeconds: Long,
    val lastAttemptAt: Long?,
    val nextPollAt: Long?,
    val dataVersion: Long?,
    val dataConfirmedAt: Long?,
    val dataAgeSeconds: Long?,
    val alerts: Int,
    val tracks: Int,
    val activeTracks: Int,
    val lastError: String?,
)

/**
 * What `/health` reports at [nowMs]; the server console shows it too. `stale`: the poller is overdue (dead or
 * hung), whatever the last poll did, or polls succeed but the data is still too old. Failing polls with a running
 * poller are `degraded`, however old the data.
 */
fun health(situation: SituationService, poller: SituationPoller, upstreamBudgetPerMinute: Int?, nowMs: Long): Health {
    val board = situation.board
    val status = poller.status
    val source = poller.source
    val intervalMs = source.pollInterval.inWholeMilliseconds
    val ageMs = board?.let { (nowMs - it.confirmedAt.toEpochMilliseconds()).coerceAtLeast(0) }
    val overdue = status.nextPollAt?.let { nowMs > it + 2 * intervalMs } == true
    return Health(
        status = when {
            board == null -> "warming_up"
            overdue || (status.lastError == null && ageMs != null && ageMs > staleAfterMs(intervalMs)) -> "stale"
            status.lastError != null -> "degraded"
            else -> "ok"
        },
        source = source.kind.key,
        simulated = situation.simulated,
        upstreamBudgetPerMinute = upstreamBudgetPerMinute,
        upstreamRequestsLastMinute = source.requestsInLastMinute,
        upstreamRequestsTotal = source.requestsTotal,
        upstreamRateLimitedTotal = source.rateLimitedTotal,
        upstreamRejectedTotal = source.rejectedTotal,
        upstreamOtherFailuresTotal = source.otherFailuresTotal,
        upstreamLastSuccessAt = source.lastSuccessAt,
        pollIntervalSeconds = source.pollInterval.inWholeSeconds,
        lastAttemptAt = status.lastAttemptAt,
        nextPollAt = status.nextPollAt,
        dataVersion = board?.version,
        dataConfirmedAt = board?.confirmedAt?.toEpochMilliseconds(),
        dataAgeSeconds = ageMs?.let { it / 1000 },
        alerts = board?.alerts?.size ?: 0,
        tracks = situation.tracks.size,
        activeTracks = situation.tracks.count { it.active },
        lastError = status.lastError,
    )
}

/**
 * Data older than this while polls succeed is `stale`. DESIGN.md §9 sets the freshness SLO at 60 s; three missed
 * intervals tolerate a slow upstream when the interval is longer than that.
 */
private fun staleAfterMs(pollIntervalMs: Long): Long = maxOf(3 * pollIntervalMs, 60_000)
