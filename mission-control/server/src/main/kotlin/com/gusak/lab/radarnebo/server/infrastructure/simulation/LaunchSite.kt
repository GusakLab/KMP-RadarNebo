package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.geo.GeoMath
import com.gusak.lab.radarnebo.geo.GeoPoint

/** A place attacks are launched from; [originUk] is how warnings name it ("Курської області"). */
internal class LaunchSite(private val name: String, val originUk: String, val point: GeoPoint)

/** The [n] sites nearest [target] within [maxKm], or the nearest one if none is that close. */
internal fun nearestSites(sites: List<LaunchSite>, target: GeoPoint, maxKm: Double, n: Int): List<LaunchSite> {
    val sorted = sites.sortedBy { GeoMath.distanceKm(it.point, target) }
    return sorted.filter { GeoMath.distanceKm(it.point, target) <= maxKm }.take(n).ifEmpty { sorted.take(1) }
}
