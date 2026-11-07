package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.geo.GeoMath
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Turns a track's reports into what clients draw: the current [TrackEstimate], the smoothed past [PathPoint]s,
 * the [ForecastPoint]s ahead and the settlements on the way. Runs on the server; clients only draw the result.
 *
 * The reports go through a [TrackFilter] forward, then a Rauch-Tung-Striebel pass backwards, so every past
 * position also benefits from the reports after it. Between smoothed states the path is a cubic Hermite curve
 * whose tangents are the smoothed velocities, capped so the curve never loops or overshoots.
 */
object TrajectoryEstimator {
    /** Most path points served per track. */
    private const val MAX_PATH_POINTS = 100
    private const val FORECAST_STEP_MS = 5 * 60_000L

    fun enrich(track: DroneTrack, nowMs: Long, places: List<Place> = TrackPredictor.cityPlaces): DroneTrack {
        val samples = track.samples
        if (samples.isEmpty()) return track
        val kind = TargetClassifier.classify(track.copy(kind = null)).kind
        val kinematics = TrackKinematics.of(track.copy(kind = kind))
        val steps = filter(samples, kinematics)
        val last = steps.last().filter
        val estimate = TrackEstimate(
            t = last.t,
            lat = last.position.lat,
            lon = last.position.lon,
            headingDeg = last.headingDeg,
            speedKmh = last.speedKmh,
            sigmaKm = last.sigmaKm,
        )
        // Speed decides between a jet and a piston Shahed when the text does not say.
        val refinedKind = TargetClassifier.classify(track.copy(kind = null), estimate.speedKmh).kind
        val path = path(steps, kinematics, last)
        val moving = track.active && estimate.speedKmh != null && estimate.headingDeg != null
        val forecast = if (moving) forecast(last, kinematics, nowMs) else emptyList()
        val arrivals = if (moving) {
            val elapsed = min((nowMs - estimate.t).coerceAtLeast(0), kinematics.maxExtrapolationMs)
            val ahead = estimate.speedKmh * elapsed / TrackFilter.HOUR_MS
            val from = GeoMath.destination(estimate.point, estimate.headingDeg, ahead)
            TrackPredictor.routeFrom(track.id, from, estimate.headingDeg, estimate.speedKmh, places).map {
                val at = nowMs + it.etaMinutes * 60_000L
                TrackArrival(it.place.name, it.place.point.lat, it.place.point.lon, it.distanceKm, at)
            }
        } else {
            emptyList()
        }
        return track.copy(
            kind = refinedKind,
            estimate = estimate,
            path = path,
            forecast = forecast,
            arrivals = arrivals,
        )
    }

    /** A filter step: the state predicted to a report's time, and after fusing it. */
    private class Step(val predictedX: DoubleArray, val predictedP: DoubleArray, val filter: TrackFilter)

    private fun filter(samples: List<TrackSample>, kinematics: TrackKinematics): List<Step> {
        val filter = TrackFilter(kinematics, samples.first().point)
        return samples.map { sample ->
            val predicted = filter.copy().apply { predict(sample.t) }
            filter.fuse(sample)
            Step(predicted.state, predicted.covariance, filter.copy())
        }
    }

    /** Smoothed states `[x, y, vx, vy]` at every report, oldest first. */
    private fun smooth(steps: List<Step>, kinematics: TrackKinematics): List<DoubleArray> {
        val smoothed = MutableList(steps.size) { steps[it].filter.state }
        for (k in steps.lastIndex - 1 downTo 0) {
            val dt = (steps[k + 1].filter.t - steps[k].filter.t) / TrackFilter.HOUR_MS
            val f = TrackFilter.transition(dt)
            val inverse = TrackFilter.invert(steps[k + 1].predictedP) ?: continue
            val c = TrackFilter.mul(TrackFilter.mul(steps[k].filter.covariance, TrackFilter.transpose(f)), inverse)
            val diff = DoubleArray(4) { smoothed[k + 1][it] - steps[k + 1].predictedX[it] }
            val xs = steps[k].filter.state
            for (r in 0..3) xs[r] += (0..3).sumOf { c[r * 4 + it] * diff[it] }
            holdSpeed(xs, kinematics)
            smoothed[k] = xs
        }
        return smoothed
    }

    private fun holdSpeed(state: DoubleArray, kinematics: TrackKinematics) {
        val speed = sqrt(state[2] * state[2] + state[3] * state[3])
        if (speed == 0.0) return
        val clamped = speed.coerceIn(kinematics.minSpeedKmh, kinematics.maxSpeedKmh)
        state[2] *= clamped / speed
        state[3] *= clamped / speed
    }

    private fun path(steps: List<Step>, kinematics: TrackKinematics, frame: TrackFilter): List<PathPoint> {
        val states = smooth(steps, kinematics)
        val times = steps.map { it.filter.t }
        fun point(t: Long, px: Double, py: Double): PathPoint =
            frame.toGeo(px, py).let { PathPoint(t, it.lat, it.lon) }
        if (states.size == 1) return listOf(point(times[0], states[0][0], states[0][1]))
        val chords = states.zipWithNext { a, b -> sqrt((b[0] - a[0]) * (b[0] - a[0]) + (b[1] - a[1]) * (b[1] - a[1])) }
        val stepKm = max(kinematics.pathStepKm, chords.sum() / MAX_PATH_POINTS)
        val out = mutableListOf(point(times[0], states[0][0], states[0][1]))
        for (i in chords.indices) {
            val a = states[i]
            val b = states[i + 1]
            val chord = chords[i]
            val dt = (times[i + 1] - times[i]) / TrackFilter.HOUR_MS
            val pieces = max(1, (chord / stepKm).toInt())
            // Tangents no longer than the chord keep the curve from looping between close reports.
            fun tangent(s: DoubleArray, c: Int): Double {
                val length = sqrt(s[2] * s[2] + s[3] * s[3]) * dt
                return if (length <= chord || length == 0.0) s[c] * dt else s[c] * dt * chord / length
            }
            val t0x = tangent(a, 2); val t0y = tangent(a, 3)
            val t1x = tangent(b, 2); val t1y = tangent(b, 3)
            for (j in 1..pieces) {
                val s = j.toDouble() / pieces
                val h00 = 2 * s * s * s - 3 * s * s + 1
                val h10 = s * s * s - 2 * s * s + s
                val h01 = -2 * s * s * s + 3 * s * s
                val h11 = s * s * s - s * s
                out += point(
                    times[i] + ((times[i + 1] - times[i]) * s).toLong(),
                    h00 * a[0] + h10 * t0x + h01 * b[0] + h11 * t1x,
                    h00 * a[1] + h10 * t0y + h01 * b[1] + h11 * t1y,
                )
            }
        }
        return out
    }

    /**
     * The course ahead from [nowMs] over the kind's horizon, with the error growing with the velocity's. Empty
     * for a target whose course is not known yet.
     */
    private fun forecast(filter: TrackFilter, kinematics: TrackKinematics, nowMs: Long): List<ForecastPoint> {
        val heading = filter.headingDeg ?: return emptyList()
        val speed = filter.speedKmh ?: return emptyList()
        val p = filter.covariance
        val sigmaV = sqrt(max(0.0, (p[10] + p[15]) / 2))
        val start = max(nowMs, filter.t)
        val end = start + kinematics.forecastHorizonMs
        return generateSequence(start) { it + FORECAST_STEP_MS }.takeWhile { it <= end }.map { t ->
            val dt = (t - filter.t) / TrackFilter.HOUR_MS
            val point = GeoMath.destination(filter.position, heading, speed * dt)
            ForecastPoint(t, point.lat, point.lon, sqrt(filter.sigmaKm * filter.sigmaKm + sigmaV * sigmaV * dt * dt))
        }.toList()
    }
}
