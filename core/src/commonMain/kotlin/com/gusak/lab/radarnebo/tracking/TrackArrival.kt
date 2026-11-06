package com.gusak.lab.radarnebo.tracking

import kotlinx.serialization.Serializable

/** A settlement on the forecast course and when the target would reach it. */
@Serializable
data class TrackArrival(val place: String, val lat: Double, val lon: Double, val distanceKm: Double, val at: Long)
