package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.geo.GeoMath
import com.gusak.lab.radarnebo.geo.GeoPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Constant-velocity Kalman filter in a local east/north plane (km, km/h) around [ref]. State `[x, y, vx, vy]`.
 * After every update the velocity is held to the target's speed band and turn rate ([TrackKinematics]), so the
 * estimate stays physically possible however coarse the reports are.
 */
internal class TrackFilter private constructor(
    private val kinematics: TrackKinematics,
    private val ref: GeoPoint,
    private val x: DoubleArray,
    private val p: DoubleArray,
    var t: Long,
    var updates: Int,
) {
    constructor(kinematics: TrackKinematics, ref: GeoPoint) :
        this(kinematics, ref, DoubleArray(4), DoubleArray(16), 0L, 0)

    /** Where the filter expects the target, give or take [sigmaKm]. */
    data class ExpectedPosition(val point: GeoPoint, val sigmaKm: Double)

    /** A point in the filter's local east/north plane, in km from [ref]. */
    private data class LocalPoint(val x: Double, val y: Double)

    fun copy(): TrackFilter = TrackFilter(kinematics, ref, x.copyOf(), p.copyOf(), t, updates)

    private val lonKm = cos(ref.lat * PI / 180.0) * KM_PER_DEGREE

    val position: GeoPoint get() = toGeo(x[0], x[1])
    val sigmaKm: Double get() = sqrt(max(0.0, (p[0] + p[5]) / 2))
    /** Known once two reports have been fused. */
    val speedKmh: Double? get() = if (updates >= 2) sqrt(x[2] * x[2] + x[3] * x[3]) else null
    val headingDeg: Double? get() = if (updates >= 2) heading(x[2], x[3]) else null
    val state: DoubleArray get() = x.copyOf()
    val covariance: DoubleArray get() = p.copyOf()

    /** Moves the state to [toT]; earlier times leave it where it is. */
    fun predict(toT: Long) {
        if (updates == 0) {
            t = toT
            return
        }
        val dt = (toT - t).coerceAtLeast(0) / HOUR_MS
        t = max(t, toT)
        if (dt == 0.0) return
        val f = transition(dt)
        x.indices.map { i -> (0..3).sumOf { f[i * 4 + it] * x[it] } }.forEachIndexed { i, v -> x[i] = v }
        val fp = mul(f, p)
        val fpft = mul(fp, transpose(f))
        val q = kinematics.processNoise
        val dt2 = dt * dt / 2
        val dt3 = dt * dt * dt / 3
        for (i in 0..1) {
            fpft[i * 4 + i] += q * dt3
            fpft[i * 4 + i + 2] += q * dt2
            fpft[(i + 2) * 4 + i] += q * dt2
            fpft[(i + 2) * 4 + i + 2] += q * dt
        }
        fpft.copyInto(p)
    }

    /** Where the target is expected at [at], with the one-sigma error; the filter itself is unchanged. */
    fun predictedAt(at: Long): ExpectedPosition =
        copy().apply { predict(at) }.let { ExpectedPosition(it.position, it.sigmaKm) }

    /**
     * Fuses a report at [at] of the target being at [point] give or take [sigmaKm]. Alerts are raised for areas
     * the target is about to reach, so an alert's area is moved back along the course by [leadMs] of flight
     * (see [TrackKinematics.leadMs]) once the course is known.
     */
    fun update(at: Long, point: GeoPoint, sigmaKm: Double, leadMs: Long = 0L) {
        val reported = toLocal(point)
        if (updates == 0) {
            start(at, reported, sigmaKm)
            return
        }
        val previousHeading = headingDeg
        val dtH = (at - t).coerceAtLeast(0) / HOUR_MS
        predict(at)
        if (!correct(leadCorrected(reported, leadMs), sigmaKm)) return
        updates++
        constrain(previousHeading, dtH)
    }

    /** The first report: the target is at [reported] give or take [sigmaKm], its velocity still unknown. */
    private fun start(at: Long, reported: LocalPoint, sigmaKm: Double) {
        x[0] = reported.x; x[1] = reported.y; x[2] = 0.0; x[3] = 0.0
        p.fill(0.0)
        p[0] = sigmaKm * sigmaKm
        p[5] = sigmaKm * sigmaKm
        val v = kinematics.maxSpeedKmh / 2
        p[10] = v * v
        p[15] = v * v
        t = at
        updates = 1
    }

    /** [reported] moved back along the course by [leadMs] of flight, once there is a course to move it along. */
    private fun leadCorrected(reported: LocalPoint, leadMs: Long): LocalPoint {
        var zx = reported.x
        var zy = reported.y
        val speed = speedKmh
        if (leadMs > 0 && speed != null && speed > 0) {
            val lead = speed * leadMs / HOUR_MS
            zx -= x[2] / speed * lead
            zy -= x[3] / speed * lead
        }
        return LocalPoint(zx, zy)
    }

    /** The Kalman correction by the position measurement [z]; false, and no change, when it cannot be fused. */
    private fun correct(z: LocalPoint, sigmaKm: Double): Boolean {
        val r = sigmaKm * sigmaKm
        val s00 = p[0] + r
        val s01 = p[1]
        val s10 = p[4]
        val s11 = p[5] + r
        val det = s00 * s11 - s01 * s10
        if (det <= 0.0) return false
        val i00 = s11 / det
        val i01 = -s01 / det
        val i10 = -s10 / det
        val i11 = s00 / det
        // K = P Hᵀ S⁻¹: the first two columns of P times S⁻¹.
        val k = DoubleArray(8)
        for (row in 0..3) {
            val a = p[row * 4]
            val b = p[row * 4 + 1]
            k[row * 2] = a * i00 + b * i10
            k[row * 2 + 1] = a * i01 + b * i11
        }
        val ix = z.x - x[0]
        val iy = z.y - x[1]
        for (row in 0..3) x[row] += k[row * 2] * ix + k[row * 2 + 1] * iy
        val newP = DoubleArray(16)
        for (row in 0..3) for (col in 0..3) {
            newP[row * 4 + col] = p[row * 4 + col] - k[row * 2] * p[col] - k[row * 2 + 1] * p[4 + col]
        }
        newP.copyInto(p)
        return true
    }

    /** Fuses [sample]: its position (with its lead time) and, when its text names where it is heading, that course. */
    fun fuse(sample: TrackSample, sigmaKm: Double = sample.sigmaKm) {
        update(sample.t, sample.point, sigmaKm, kinematics.leadMs(sample.level))
        HeadingHints.target(sample.message)?.let { steer(it) }
    }

    /**
     * Turns the course [weight] of the way towards [target], keeping the speed: a weak heading measurement, since
     * "курсом на Київ" says where the group is heading, not that it flies straight there.
     */
    fun steer(target: GeoPoint, weight: Double = HINT_WEIGHT) {
        val heading = headingDeg ?: return
        val speed = speedKmh ?: return
        val towards = GeoMath.bearingDeg(position, target)
        val h = (heading + weight * angleDiff(heading, towards) + 360.0) % 360.0 * PI / 180.0
        x[2] = speed * sin(h)
        x[3] = speed * cos(h)
    }

    /** Holds the velocity to the speed band and, once a heading is known, to the kind's turn rate. */
    private fun constrain(previousHeading: Double?, dtH: Double) {
        val speed = sqrt(x[2] * x[2] + x[3] * x[3])
        var heading = heading(x[2], x[3])
        if (previousHeading != null && speed > 0) {
            val maxTurn = min(180.0, kinematics.maxTurnDegPerHour * dtH)
            val turn = angleDiff(previousHeading, heading)
            if (abs(turn) > maxTurn) heading = (previousHeading + (if (turn > 0) maxTurn else -maxTurn) + 360.0) % 360.0
        }
        val clamped = speed.coerceIn(kinematics.minSpeedKmh, kinematics.maxSpeedKmh)
        if (speed == 0.0 && previousHeading == null) return
        val h = heading * PI / 180.0
        x[2] = clamped * sin(h)
        x[3] = clamped * cos(h)
    }

    private fun toLocal(point: GeoPoint) =
        LocalPoint((point.lon - ref.lon) * lonKm, (point.lat - ref.lat) * KM_PER_DEGREE)

    fun toGeo(px: Double, py: Double): GeoPoint = GeoPoint(ref.lat + py / KM_PER_DEGREE, ref.lon + px / lonKm)

    companion object {
        const val HOUR_MS: Double = 3_600_000.0
        private const val KM_PER_DEGREE = 111.2
        /** How far one "курсом на" turns the course towards the named place. */
        const val HINT_WEIGHT: Double = 0.3

        fun heading(vx: Double, vy: Double): Double = (atan2(vx, vy) * 180.0 / PI + 360.0) % 360.0

        /** Signed turn from [from] to [to], in -180..180 degrees. */
        fun angleDiff(from: Double, to: Double): Double = ((to - from + 540.0) % 360.0) - 180.0

        fun transition(dt: Double): DoubleArray = DoubleArray(16).also {
            for (i in 0..3) it[i * 4 + i] = 1.0
            it[2] = dt
            it[7] = dt
        }

        fun mul(a: DoubleArray, b: DoubleArray): DoubleArray = DoubleArray(16).also { out ->
            for (r in 0..3) for (c in 0..3) out[r * 4 + c] = (0..3).sumOf { a[r * 4 + it] * b[it * 4 + c] }
        }

        fun transpose(a: DoubleArray): DoubleArray = DoubleArray(16).also { out ->
            for (r in 0..3) for (c in 0..3) out[c * 4 + r] = a[r * 4 + c]
        }

        /** Gauss-Jordan inverse of a 4×4 matrix; null when singular. */
        fun invert(a: DoubleArray): DoubleArray? {
            val m = a.copyOf()
            val inv = DoubleArray(16).also { for (i in 0..3) it[i * 4 + i] = 1.0 }
            for (col in 0..3) {
                val pivot = (col..3).maxBy { abs(m[it * 4 + col]) }
                if (abs(m[pivot * 4 + col]) < 1e-12) return null
                for (c in 0..3) {
                    m[col * 4 + c] = m[pivot * 4 + c].also { m[pivot * 4 + c] = m[col * 4 + c] }
                    inv[col * 4 + c] = inv[pivot * 4 + c].also { inv[pivot * 4 + c] = inv[col * 4 + c] }
                }
                val d = m[col * 4 + col]
                for (c in 0..3) {
                    m[col * 4 + c] /= d
                    inv[col * 4 + c] /= d
                }
                for (r in 0..3) if (r != col) {
                    val f = m[r * 4 + col]
                    for (c in 0..3) {
                        m[r * 4 + c] -= f * m[col * 4 + c]
                        inv[r * 4 + c] -= f * inv[col * 4 + c]
                    }
                }
            }
            return inv
        }
    }
}
