package com.gusak.lab.radarnebo.geo

/** The city closest to a point, and how far from it the point is; see [Regions.nearestCity]. */
data class NearestCity(val city: MapCity, val distanceKm: Double)
