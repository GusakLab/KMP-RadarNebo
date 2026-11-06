package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.geo.GeoPoint
import kotlinx.serialization.Serializable

/** The server's estimate of where a target was at [t], and how it was moving. */
@Serializable
data class TrackEstimate(
    val t: Long,
    val lat: Double,
    val lon: Double,
    /** Degrees clockwise from north; null until the track has moved. */
    val headingDeg: Double?,
    val speedKmh: Double?,
    /** One-sigma position error. */
    val sigmaKm: Double,
) {
    val point: GeoPoint get() = GeoPoint(lat, lon)
}
