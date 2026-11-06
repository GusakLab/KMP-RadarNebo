package com.gusak.lab.radarnebo.geo

/**
 * A hromada (community): its alerts.in.ua oblast UID, the stem of its name ("Червоногригорівська"), centre and
 * radius in km.
 */
internal class Hromada(val oblastUid: Int, val stem: String, val lat: Double, val lon: Double, val radiusKm: Double) {
    val point: GeoPoint get() = GeoPoint(lat, lon)
}
