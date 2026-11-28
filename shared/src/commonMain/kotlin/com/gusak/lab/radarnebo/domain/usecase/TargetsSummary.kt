package com.gusak.lab.radarnebo.domain.usecase

import com.gusak.lab.radarnebo.data.Alert
import com.gusak.lab.radarnebo.tracking.DroneTracksResponse
import com.gusak.lab.radarnebo.tracking.InterpolatedPosition
import com.gusak.lab.radarnebo.tracking.TargetStatsResponse
import com.gusak.lab.radarnebo.tracking.TrackInterpolator

/**
 * What the home screen widget shows: where the targets in the air are, which oblasts are under alert, and how many
 * targets there were lately.
 */
data class TargetsSummary(
    /** One position per target in the air, at [atMs]. */
    val targets: List<InterpolatedPosition>,
    /** Oblasts under an alert, by uid: true when it covers the whole oblast, false for part of it. */
    val alertedOblasts: Map<Int, Boolean>,
    /** Null when the server has no stats (an older server) or did not answer for them. */
    val stats: TargetStatsResponse?,
    /** Server time the positions are for. */
    val atMs: Long,
) {
    internal companion object {
        fun of(response: DroneTracksResponse, alerts: List<Alert>, stats: TargetStatsResponse?): TargetsSummary {
            val at = response.serverTime
            val targets = response.tracks
                .filter { TrackInterpolator.isActive(it, at) }
                .mapNotNull { TrackInterpolator.positionAt(it, at) }
            return TargetsSummary(targets, BuildThreatPictureUseCase.alertedOblasts(alerts), stats, at)
        }
    }
}
