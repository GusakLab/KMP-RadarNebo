package com.gusak.lab.radarnebo.ui

import com.gusak.lab.radarnebo.geo.GeoMath
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.geo.Regions
import kotlin.math.roundToInt

/** A long-pressed spot described the way a person would: "12 km NE of Uman · Cherkasy Oblast". */
data class PlaceSummary(val suggestedName: String, val where: String, val inUkraine: Boolean)

fun describePlace(point: GeoPoint): PlaceSummary {
    val (city, km) = Regions.nearestCity(point)
    val oblast = Regions.oblastAt(point)?.let(Regions::oblast)
    val cityPoint = GeoPoint(city.lat.toDouble(), city.lon.toDouble())
    val (name, near) = if (km < NEAR_CITY_KM) {
        city.name to "In ${city.name}"
    } else {
        "Near ${city.name}" to "${km.roundToInt()} km ${compass(GeoMath.bearingDeg(cityPoint, point))} of ${city.name}"
    }
    return PlaceSummary(name, listOfNotNull(near, oblast?.nameEn).joinToString(" · "), inUkraine = oblast != null)
}

/** Within this distance of a city's centre a spot counts as "in" the city. */
private const val NEAR_CITY_KM = 3.0
