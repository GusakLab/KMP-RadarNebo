package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.geo.GeoMath
import com.gusak.lab.radarnebo.geo.GeoPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min

object TrackInterpolator {
    /** Drone speed cap; see [TrackKinematics.Drones]. */
    internal const val MAX_SPEED_KMH: Double = 220.0
    /** Drone dead-reckoning limit; see [TrackKinematics.Drones]. */
    internal const val MAX_EXTRAPOLATION_MS: Long = 30 * 60_000L

    /** Tracks worth drawing at [t]: active ones and those that ended less than their extrapolation limit ago. */
    fun isVisible(track: DroneTrack, t: Long): Boolean =
        isActive(track, t) || t - track.lastSeen <= TrackKinematics.of(track).maxExtrapolationMs

    /** [DroneTrack.active], rechecked at [t]: a track the server called active goes stale between polls too. */
    fun isActive(track: DroneTrack, t: Long): Boolean =
        track.active && t - track.lastSeen <= TrackKinematics.of(track).staleAfterMs

    /**
     * Linear interpolation between the two samples surrounding [t]. Past the newest sample an active
     * track keeps moving along its [trend] at its (capped) speed, for at most the type's
     * [TrackKinematics.maxExtrapolationMs], so targets keep moving between reports.
     */
    fun positionAt(track: DroneTrack, t: Long): InterpolatedPosition? {
        val kinematics = TrackKinematics.of(track)
        track.estimate?.let { return estimated(track, it, t, kinematics) }
        val s = track.samples
        if (s.isEmpty()) return null
        if (s.size == 1 || t <= s.first().t) return held(track, s.first(), segmentMotion(s))

        val i = s.indexOfLast { it.t <= t }
        if (i < s.lastIndex) {
            val a = s[i]
            val b = s[i + 1]
            val f = (t - a.t).toDouble() / (b.t - a.t).coerceAtLeast(1)
            val (heading, speed) = motion(a, b)
            return InterpolatedPosition(
                trackId = track.id,
                lat = a.lat + (b.lat - a.lat) * f,
                lon = a.lon + (b.lon - a.lon) * f,
                headingDeg = heading,
                speedKmh = min(speed, kinematics.maxSpeedKmh),
                mode = PositionMode.Interpolated,
                level = track.level,
            )
        }

        val b = s.last()
        val (heading, rawSpeed) = trend(s) ?: motion(s[s.lastIndex - 1], b)
        if (!track.active || rawSpeed < MIN_MOVING_KMH) return held(track, b, TrackMotion(heading, rawSpeed))
        val speed = min(rawSpeed, kinematics.maxSpeedKmh)
        val elapsedMs = min(t - b.t, kinematics.maxExtrapolationMs)
        val ahead = GeoMath.destination(b.point, heading, speed * elapsedMs / 3_600_000.0)
        return InterpolatedPosition(
            trackId = track.id,
            lat = ahead.lat,
            lon = ahead.lon,
            headingDeg = heading,
            speedKmh = speed,
            mode = PositionMode.Extrapolated,
            level = track.level,
        )
    }

    /**
     * From the server's [TrackEstimate]: along the smoothed [DroneTrack.path] before it, along its course after it
     * (for at most the type's [TrackKinematics.maxExtrapolationMs]). The server already applied the target's
     * limits, so nothing is refitted here.
     */
    private fun estimated(
        track: DroneTrack,
        e: TrackEstimate,
        t: Long,
        kinematics: TrackKinematics,
    ): InterpolatedPosition {
        fun at(point: GeoPoint, heading: Double?, speed: Double?, mode: PositionMode): InterpolatedPosition =
            InterpolatedPosition(track.id, point.lat, point.lon, heading, speed, mode, track.level)
        val path = track.path
        if (t < e.t && path.size >= 2) {
            if (t <= path.first().t) return at(path.first().point, e.headingDeg, null, PositionMode.Held)
            val i = path.indexOfLast { it.t <= t }.coerceAtMost(path.lastIndex - 1)
            val a = path[i]
            val b = path[i + 1]
            val f = ((t - a.t).toDouble() / (b.t - a.t).coerceAtLeast(1)).coerceIn(0.0, 1.0)
            val point = GeoPoint(a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f)
            val speed = GeoMath.distanceKm(a.point, b.point) / ((b.t - a.t).coerceAtLeast(1) / 3_600_000.0)
            val heading = GeoMath.bearingDeg(a.point, b.point)
            return at(point, heading, min(speed, kinematics.maxSpeedKmh), PositionMode.Interpolated)
        }
        val heading = e.headingDeg
        val speed = e.speedKmh
        if (!track.active || heading == null || speed == null || speed < MIN_MOVING_KMH || t <= e.t) {
            return at(e.point, heading, if (track.active) speed else null, PositionMode.Held)
        }
        val elapsedMs = min(t - e.t, kinematics.maxExtrapolationMs)
        val ahead = GeoMath.destination(e.point, heading, speed * elapsedMs / 3_600_000.0)
        return at(ahead, heading, speed, PositionMode.Extrapolated)
    }

    private fun held(track: DroneTrack, sample: TrackSample, motion: TrackMotion?) = InterpolatedPosition(
        trackId = track.id,
        lat = sample.lat,
        lon = sample.lon,
        headingDeg = motion?.headingDeg,
        speedKmh = null,
        mode = PositionMode.Held,
        level = track.level,
    )

    private fun segmentMotion(s: List<TrackSample>) = if (s.size >= 2) motion(s[s.lastIndex - 1], s.last()) else null

    /** Slower than this the fitted motion is noise: the group is holding where it was last reported. */
    private const val MIN_MOVING_KMH = 1.0
    /** Reports [trend] fits, at most, and how far back from the newest one they may go. */
    private const val TREND_SAMPLES = 4
    private const val TREND_WINDOW_MS = 90 * 60_000L
    private const val KM_PER_DEGREE = 111.2

    /**
     * Heading (degrees clockwise from north) and speed (km/h) fitted by least squares through the latest few
     * reports. Positions are raion and settlement centres, so a single hop's direction is coarse; the fit over
     * several averages that out and follows the group's course rather than its last zig. Null below two reports.
     */
    internal fun trend(samples: List<TrackSample>): TrackMotion? {
        val last = samples.lastOrNull() ?: return null
        val recent = samples.takeLast(TREND_SAMPLES).filter { last.t - it.t <= TREND_WINDOW_MS }
        if (recent.size < 2) return null
        val lonKm = cos(last.lat * PI / 180.0) * KM_PER_DEGREE
        val ts = recent.map { (it.t - last.t) / 3_600_000.0 }
        val xs = recent.map { (it.lon - last.lon) * lonKm }
        val ys = recent.map { (it.lat - last.lat) * KM_PER_DEGREE }
        val tMean = ts.average()
        val variance = ts.sumOf { (it - tMean) * (it - tMean) }
        if (variance <= 0.0) return null
        fun slope(vs: List<Double>): Double {
            val mean = vs.average()
            return ts.indices.sumOf { (ts[it] - tMean) * (vs[it] - mean) } / variance
        }
        val vx = slope(xs)
        val vy = slope(ys)
        return TrackMotion((atan2(vx, vy) * 180.0 / PI + 360.0) % 360.0, kotlin.math.sqrt(vx * vx + vy * vy))
    }

    /** Heading in degrees clockwise from north and average speed in km/h between two samples. */
    fun motion(a: TrackSample, b: TrackSample): TrackMotion {
        val dx = (b.lon - a.lon) * cos(a.lat * PI / 180.0)
        val dy = b.lat - a.lat
        val heading = (atan2(dx, dy) * 180.0 / PI + 360.0) % 360.0
        val hours = (b.t - a.t).coerceAtLeast(1) / 3_600_000.0
        return TrackMotion(heading, GeoMath.distanceKm(a.point, b.point) / hours)
    }
}
