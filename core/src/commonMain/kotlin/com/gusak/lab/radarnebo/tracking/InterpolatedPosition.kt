package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.geo.GeoPoint
import kotlinx.serialization.Serializable

@Serializable
data class InterpolatedPosition(
    val trackId: String,
    val lat: Double,
    val lon: Double,
    /** Degrees clockwise from north, when the track has moved at least once. */
    val headingDeg: Double?,
    val speedKmh: Double?,
    val mode: PositionMode,
    val level: ThreatLevel,
) {
    val point: GeoPoint get() = GeoPoint(lat, lon)
}
