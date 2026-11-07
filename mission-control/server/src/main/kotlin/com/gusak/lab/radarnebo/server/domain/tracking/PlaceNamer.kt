package com.gusak.lab.radarnebo.server.domain.tracking

import com.gusak.lab.radarnebo.geo.GeoMath
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.geo.Regions
import kotlin.math.roundToInt

/** Describes a position for people: "Konotop", or "12 km NE of Konotop". */
internal fun interface PlaceNamer {
    fun describe(point: GeoPoint): String

    companion object {
        private val COMPASS_POINTS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

        /** Relative to the nearest settlement with city status. */
        val NearestCity: PlaceNamer = PlaceNamer { point ->
            val (city, km) = Regions.nearestCity(point)
            if (km < 3.0) {
                city.name
            } else {
                val bearing = GeoMath.bearingDeg(GeoPoint(city.lat.toDouble(), city.lon.toDouble()), point)
                "${km.roundToInt()} km ${COMPASS_POINTS[((bearing + 22.5) / 45).toInt() % 8]} of ${city.name}"
            }
        }
    }
}
