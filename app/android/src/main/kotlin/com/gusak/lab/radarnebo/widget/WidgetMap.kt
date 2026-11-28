package com.gusak.lab.radarnebo.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.geo.UkraineGeometry
import com.gusak.lab.radarnebo.tracking.InterpolatedPosition
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Ukraine's oblasts, shaded where an alert is on, with a dot per target, drawn into a bitmap for the widget, which
 * can't host a live map. An equirectangular projection, squeezed to the country's mid latitude, is close enough at
 * this size.
 */
internal object WidgetMap {
    private const val WEST = 22.0
    private const val EAST = 40.4
    private const val SOUTH = 44.2
    private const val NORTH = 52.5
    private val LON_SCALE = cos(Math.toRadians((SOUTH + NORTH) / 2))
    private const val DEFAULT_WIDTH_PX = 720

    private val land = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E2A36.toInt() }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0xFF46607A.toInt()
    }
    private val alert = Paint(Paint.ANTI_ALIAS_FLAG)
    private val target = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * [alertedOblasts] maps an oblast uid to true when the alert covers all of it, false for part of it, as the app's
     * map shades them.
     */
    fun draw(
        targets: List<InterpolatedPosition>,
        alertedOblasts: Map<Int, Boolean>,
        widthPx: Int = DEFAULT_WIDTH_PX,
    ): Bitmap {
        val heightPx = (widthPx * (NORTH - SOUTH) / ((EAST - WEST) * LON_SCALE)).roundToInt()
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        fun x(lon: Double) = ((lon - WEST) / (EAST - WEST) * widthPx).toFloat()
        fun y(lat: Double) = ((NORTH - lat) / (NORTH - SOUTH) * heightPx).toFloat()
        UkraineGeometry.oblasts.forEach { shape ->
            val path = Path()
            val lonLat = shape.lonLat
            path.moveTo(x(lonLat[0].toDouble()), y(lonLat[1].toDouble()))
            for (i in 2 until lonLat.size step 2) path.lineTo(x(lonLat[i].toDouble()), y(lonLat[i + 1].toDouble()))
            path.close()
            canvas.drawPath(path, land)
            alertedOblasts[shape.uid]?.let { whole ->
                alert.color = if (whole) ALERT_WHOLE else ALERT_PART
                canvas.drawPath(path, alert)
            }
            canvas.drawPath(path, border)
        }
        val radius = widthPx / 110f
        targets.forEach { position ->
            target.color = if (position.level == ThreatLevel.Red) RED else AMBER
            target.alpha = GLOW_ALPHA
            canvas.drawCircle(x(position.lon), y(position.lat), radius * 2.4f, target)
            target.alpha = OPAQUE
            canvas.drawCircle(x(position.lon), y(position.lat), radius, target)
        }
        return bitmap
    }

    // The app's Radar palette for oblasts under alert, over the land colour.
    private const val ALERT_WHOLE = 0x66E5484D
    private const val ALERT_PART = 0x40F5A524
    private const val RED = 0xFFFF5252.toInt()
    private const val AMBER = 0xFFFFB020.toInt()
    private const val GLOW_ALPHA = 70
    private const val OPAQUE = 255
}
