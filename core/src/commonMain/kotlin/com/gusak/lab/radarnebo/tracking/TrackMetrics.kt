package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.geo.GeoMath

object TrackMetrics {
    /** Distance flown along the reported positions, plus the estimated way from the last report to [current]. */
    fun distanceFlownKm(track: DroneTrack, current: InterpolatedPosition? = null): Double {
        val reported = track.samples.zipWithNext { a, b -> GeoMath.distanceKm(a.point, b.point) }.sum()
        val sinceLast = current
            ?.takeIf { it.mode == PositionMode.Extrapolated }
            ?.let { GeoMath.distanceKm(track.samples.last().point, it.point) }
            ?: 0.0
        return reported + sinceLast
    }
}
