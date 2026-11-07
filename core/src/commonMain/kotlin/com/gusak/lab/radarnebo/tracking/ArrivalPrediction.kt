package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.geo.GeoPoint

/** Where a tracked drone group is predicted to arrive next, and roughly when. */
data class ArrivalPrediction(
    internal val trackId: String,
    val place: Place,
    /** Estimated current position the prediction starts from. */
    internal val from: GeoPoint,
    val distanceKm: Double,
    val speedKmh: Double,
    val etaMinutes: Int,
)
