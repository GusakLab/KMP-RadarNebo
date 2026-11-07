package com.gusak.lab.radarnebo.server.domain.alert

import com.gusak.lab.radarnebo.data.ThreatLevel
import kotlin.time.Instant

/**
 * An alert declared for one [location]: active from [startedAt] until [finishedAt] (null while active).
 * [id] is the record id and stays the same for the alert's whole life.
 */
data class AirAlert(
    val id: Long,
    val location: AlertLocation,
    val type: AlertType = AlertType.AirRaid,
    /** Null when the source does not grade the alert. */
    val level: ThreatLevel? = ThreatLevel.Red,
    val startedAt: Instant,
    val updatedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val notes: String? = null,
    val threats: List<ThreatSignal> = emptyList(),
    /** Passed through from alerts.in.ua (always null so far). */
    val country: String? = null,
) {
    internal val isActive: Boolean get() = finishedAt == null

    internal fun finish(at: Instant): AirAlert = if (isActive) copy(finishedAt = at, updatedAt = at) else this

    /** True when the alert is for the location [uid] or, if [uid] is a region, for any place in it. */
    internal fun concerns(uid: String): Boolean =
        location.uid == uid || (LocationDirectory.isRegionUid(uid) && location.oblastUid?.toString() == uid)

    /** True when the alert was active at some point at or after [since]. */
    internal fun activeSince(since: Instant): Boolean = finishedAt?.let { it >= since } ?: true
}
