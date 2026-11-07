package com.gusak.lab.radarnebo.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.gusak.lab.radarnebo.geo.GeoPoint
import kotlin.math.min

data class MapCamera(val center: Offset, val zoom: Float) {

    fun scale(size: Size): Float =
        min(size.width / MapProjection.bounds.width, size.height / MapProjection.bounds.height) * 0.94f * zoom

    internal fun toScreen(world: Offset, size: Size): Offset {
        val s = scale(size)
        return Offset(size.width / 2 + (world.x - center.x) * s, size.height / 2 + (world.y - center.y) * s)
    }

    fun toWorld(screen: Offset, size: Size): Offset {
        val s = scale(size)
        return Offset(center.x + (screen.x - size.width / 2) / s, center.y + (screen.y - size.height / 2) / s)
    }

    /** Applies a pinch/pan gesture keeping the point under [centroid] fixed. */
    fun transformed(centroid: Offset, pan: Offset, zoomChange: Float, size: Size, maxZoom: Float): MapCamera {
        val anchor = toWorld(centroid, size)
        val newZoom = (zoom * zoomChange).coerceIn(MIN_ZOOM, maxZoom)
        val zoomed = copy(zoom = newZoom)
        val s = zoomed.scale(size)
        val newCenter = Offset(
            anchor.x - (centroid.x - size.width / 2) / s - pan.x / s,
            anchor.y - (centroid.y - size.height / 2) / s - pan.y / s,
        )
        val b = MapProjection.bounds
        val clamped = Offset(newCenter.x.coerceIn(b.left, b.right), newCenter.y.coerceIn(b.top, b.bottom))
        return zoomed.copy(center = clamped)
    }

    companion object {
        internal const val MIN_ZOOM: Float = 0.8f
        const val MAX_ZOOM: Float = 16f
        val Overview: MapCamera = MapCamera(MapProjection.bounds.center, 1f)

        /**
         * Camera that fits [points] plus [marginKm] around them inside [visible], the part of the
         * [viewport] not covered by other UI. It is centred on [focus] when given (the points then fit
         * around it), otherwise on the middle of the points.
         */
        internal fun framing(
            points: List<GeoPoint>,
            viewport: Size,
            visible: Rect,
            marginKm: Float = 40f,
            focus: GeoPoint? = null,
        ): MapCamera {
            if (points.isEmpty() || visible.isEmpty) return Overview
            val world = points.map(MapProjection::toWorld)
            val margin = MapProjection.kmToWorld(marginKm)
            val minX = world.minOf { it.x } - margin
            val maxX = world.maxOf { it.x } + margin
            val minY = world.minOf { it.y } - margin
            val maxY = world.maxOf { it.y } + margin
            val center = focus?.let(MapProjection::toWorld) ?: Offset((minX + maxX) / 2, (minY + maxY) / 2)
            // Half the extent on each side of the centre, so every point stays in view around it.
            val halfWidth = maxOf(center.x - minX, maxX - center.x)
            val halfHeight = maxOf(center.y - minY, maxY - center.y)
            val base = Overview.scale(viewport)
            val zoom = min(visible.width / (2 * halfWidth * base), visible.height / (2 * halfHeight * base))
                .coerceIn(1f, 10f)
            return centeredOn(center, zoom, viewport, visible)
        }

        /** Camera at [zoom] that puts the world point [focus] in the middle of [visible], a part of [viewport]. */
        internal fun centeredOn(focus: Offset, zoom: Float, viewport: Size, visible: Rect): MapCamera {
            val s = Overview.scale(viewport) * zoom
            return MapCamera(
                Offset(
                    focus.x - (visible.center.x - viewport.width / 2) / s,
                    focus.y - (visible.center.y - viewport.height / 2) / s,
                ),
                zoom,
            )
        }
    }
}
