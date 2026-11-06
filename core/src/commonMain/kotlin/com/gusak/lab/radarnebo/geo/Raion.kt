package com.gusak.lab.radarnebo.geo

/**
 * A raion (district): its alerts.in.ua oblast UID, Ukrainian names (current first, then former ones), centre and
 * radius in km (half the mean of its bounding box's width and height).
 */
internal class Raion(
    val oblastUid: Int,
    val names: List<String>,
    val nameEn: String,
    val lat: Double,
    val lon: Double,
    val radiusKm: Double,
) {
    val point: GeoPoint get() = GeoPoint(lat, lon)
}
