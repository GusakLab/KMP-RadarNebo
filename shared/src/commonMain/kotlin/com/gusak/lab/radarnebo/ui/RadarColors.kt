package com.gusak.lab.radarnebo.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Map and overlay colours that Material's colour scheme doesn't cover. Read via [LocalRadarColors]. */
@Immutable
data class RadarColors(
    val isDark: Boolean,
    val mapBackground: Color,
    val grid: Color,
    val oblastIdle: Color,
    val oblastBorder: Color,
    val oblastAlertFull: Color,
    val oblastAlertPartial: Color,
    val danger: Color,
    val warning: Color,
    val favorite: Color,
    /** "All clear" / healthy state. */
    val ok: Color,
    /**
     * [danger], [ok] and [warning] for text: the same hues, darkened (light) or lightened (dark) just enough
     * for WCAG AA contrast on surfaces and on [overlay]. The plain colours are for icons and fills.
     */
    val dangerText: Color,
    val okText: Color,
    val warningText: Color,
    /** Text on a [warning] fill, such as the predicted-route chips. */
    val onWarning: Color,
    /** Translucent chip behind text drawn over the map, and the colours on it. */
    val overlay: Color,
    val onOverlay: Color,
    val onOverlayMuted: Color,
    /** Labels and selection rings drawn straight onto the map. */
    val mapLabel: Color,
    val glyphOutline: Color,
    /** City dots and the city names shown once zoomed in. */
    val cityDot: Color,
    val cityLabel: Color,
    /**
     * Liquid glass: tint over the blurred backdrop, rim highlight and the selected-item lens. The tint is strong
     * enough that text on the glass reads at WCAG AA over the darkest or lightest backdrop.
     */
    val glassTint: Color,
    val glassRimLight: Color,
    val glassRimShade: Color,
    val glassSelection: Color,
) {
    internal companion object {
        val Dark: RadarColors = RadarColors(
            isDark = true,
            mapBackground = Color(0xFF070D14),
            grid = Color(0xFF13202E),
            oblastIdle = Color(0xFF152536),
            oblastBorder = Color(0xFF3B5A78),
            oblastAlertFull = Color(0x66E5484D),
            oblastAlertPartial = Color(0x40F5A524),
            danger = Color(0xFFFF4D4F),
            warning = Color(0xFFFFB020),
            favorite = Color(0xFF3DD9EB),
            ok = Color(0xFF3DEB8A),
            dangerText = Color(0xFFFF7879),
            okText = Color(0xFF3DEB8A),
            warningText = Color(0xFFFFB020),
            onWarning = Color.Black,
            overlay = Color(0xCC000000),
            onOverlay = Color.White,
            onOverlayMuted = Color(0xB3FFFFFF),
            mapLabel = Color.White,
            glyphOutline = Color(0x99000000),
            cityDot = Color(0xB39FB8CF),
            cityLabel = Color(0xCCC8D6E4),
            glassTint = Color(0xB8000000),
            glassRimLight = Color(0x80FFFFFF),
            glassRimShade = Color(0x14FFFFFF),
            glassSelection = Color(0x1AFFFFFF),
        )

        val Light: RadarColors = RadarColors(
            isDark = false,
            mapBackground = Color(0xFFE9EEF3),
            grid = Color(0xFFD7DFE8),
            oblastIdle = Color(0xFFFFFFFF),
            oblastBorder = Color(0xFF9DB2C7),
            oblastAlertFull = Color(0x59E5484D),
            oblastAlertPartial = Color(0x52F5A524),
            danger = Color(0xFFE03B40),
            warning = Color(0xFFBB7000),
            favorite = Color(0xFF0B93A6),
            ok = Color(0xFF1C9553),
            dangerText = Color(0xFFA12A2E),
            okText = Color(0xFF14683A),
            warningText = Color(0xFF834E00),
            onWarning = Color.Black,
            overlay = Color(0xEBFFFFFF),
            onOverlay = Color(0xFF0E1822),
            onOverlayMuted = Color(0xBF0E1822),
            mapLabel = Color(0xFF0E1822),
            glyphOutline = Color(0xB3FFFFFF),
            cityDot = Color(0xFF76879A),
            cityLabel = Color(0xFF3A4D60),
            glassTint = Color(0xCCFFFFFF),
            glassRimLight = Color(0xF2FFFFFF),
            glassRimShade = Color(0x1A0E1822),
            glassSelection = Color(0x66FFFFFF),
        )
    }
}
