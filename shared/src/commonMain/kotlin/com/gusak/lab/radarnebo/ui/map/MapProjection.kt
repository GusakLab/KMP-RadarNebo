package com.gusak.lab.radarnebo.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.gusak.lab.radarnebo.geo.GeoPoint

/**
 * Equirectangular projection centred on Ukraine. World units are degrees, with longitude squashed
 * by cos(48.5°) so shapes keep their proportions at Ukraine's latitude.
 */
object MapProjection {
    private const val K = 0.6626f
    private const val KM_PER_DEGREE = 111.2f

    internal val bounds: Rect = Rect(left = 22.0f * K, top = -52.5f, right = 40.3f * K, bottom = -44.2f)

    internal fun toWorld(p: GeoPoint): Offset = Offset((p.lon * K).toFloat(), (-p.lat).toFloat())
    fun toGeo(world: Offset): GeoPoint = GeoPoint(lat = -world.y.toDouble(), lon = (world.x / K).toDouble())
    internal fun kmToWorld(km: Float): Float = km / KM_PER_DEGREE
}
