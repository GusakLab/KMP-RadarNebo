package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.geo.GeoPoint
import kotlinx.serialization.Serializable

/** Where the target is expected at [t] if it keeps its course, give or take [sigmaKm]. */
@Serializable
data class ForecastPoint(val t: Long, val lat: Double, val lon: Double, val sigmaKm: Double) {
    val point: GeoPoint get() = GeoPoint(lat, lon)
}
