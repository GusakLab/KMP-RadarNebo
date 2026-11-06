package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.geo.GeoPoint
import kotlinx.serialization.Serializable

/** A point of a smoothed past trajectory. */
@Serializable
data class PathPoint(val t: Long, val lat: Double, val lon: Double) {
    val point: GeoPoint get() = GeoPoint(lat, lon)
}
