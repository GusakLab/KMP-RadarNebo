package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.geo.GeoMath
import com.gusak.lab.radarnebo.geo.GeoPoint

/** A route flown at constant speed, starting at [departAt]. Positions between waypoints are linear. */
internal class FlightPath(val waypoints: List<GeoPoint>, val speedKmh: Double, val departAt: Long) {
    init {
        require(waypoints.size >= 2) { "A path needs at least two waypoints" }
        require(speedKmh > 0) { "Speed must be positive: $speedKmh" }
    }

    /** Distance from the first waypoint to each waypoint, km. */
    private val cumulativeKm: DoubleArray = waypoints.zipWithNext { a, b -> GeoMath.distanceKm(a, b) }
        .runningFold(0.0) { acc, d -> acc + d }.toDoubleArray()

    val lengthKm: Double get() = cumulativeKm.last()
    val durationMs: Long = (lengthKm / speedKmh * HOUR_MS).toLong().coerceAtLeast(1)
    val arriveAt: Long get() = departAt + durationMs
    val target: GeoPoint get() = waypoints.last()

    fun timeAtFraction(fraction: Double): Long = departAt + (durationMs * fraction.coerceIn(0.0, 1.0)).toLong()

    fun positionAt(t: Long): GeoPoint = positionAtKm(kmAt(t))

    fun positionAtFraction(fraction: Double): GeoPoint = positionAtKm(lengthKm * fraction.coerceIn(0.0, 1.0))

    /** Points every [stepKm] along the path from [from] to [to] (times), both ends included. */
    fun pointsBetween(from: Long, to: Long, stepKm: Double): List<GeoPoint> {
        val start = kmAt(from)
        val end = kmAt(to)
        if (end <= start) return listOf(positionAtKm(start))
        val steps = ((end - start) / stepKm).toInt().coerceAtLeast(1)
        return (0..steps).map { positionAtKm(start + (end - start) * it / steps) }
    }

    /** Distance flown by [t], km: none before departure, the whole path after arrival. */
    private fun kmAt(t: Long): Double = (t - departAt).coerceIn(0, durationMs) * speedKmh / HOUR_MS

    private fun positionAtKm(km: Double): GeoPoint {
        val i = cumulativeKm.indexOfLast { it <= km }.coerceIn(0, waypoints.lastIndex - 1)
        val legKm = cumulativeKm[i + 1] - cumulativeKm[i]
        val f = if (legKm <= 0.0) 0.0 else ((km - cumulativeKm[i]) / legKm).coerceIn(0.0, 1.0)
        return between(waypoints[i], waypoints[i + 1], f)
    }

    companion object {
        fun between(a: GeoPoint, b: GeoPoint, f: Double): GeoPoint =
            GeoPoint(a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f)
    }
}
