package com.gusak.lab.radarnebo.server.domain.tracking

import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.tracking.DroneTrack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

internal data class TrackQuery(
    val fromMs: Long,
    val toMs: Long,
    val type: ThreatType? = null,
    val limit: Int = DEFAULT_LIMIT,
) {
    init {
        require(fromMs <= toMs) { "'from' must not be after 'to'" }
        require(limit in 1..MAX_LIMIT) { "'limit' must be between 1 and $MAX_LIMIT" }
        // Bounds the rows one call can read and decode; `in` also rejects a difference that overflowed.
        require(toMs - fromMs in 0..MAX_WINDOW.inWholeMilliseconds) {
            "the 'from'..'to' window must be at most ${MAX_WINDOW.inWholeDays} days"
        }
    }

    fun overlaps(track: DroneTrack): Boolean =
        track.firstSeen <= toMs && track.lastSeen >= fromMs && (type == null || track.type == type)

    companion object {
        const val DEFAULT_LIMIT: Int = 200
        const val MAX_LIMIT: Int = 200
        val MAX_WINDOW: Duration = 7.days
    }
}
