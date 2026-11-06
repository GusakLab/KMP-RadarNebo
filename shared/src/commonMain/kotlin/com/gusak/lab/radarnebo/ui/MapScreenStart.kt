package com.gusak.lab.radarnebo.ui

import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.ui.map.MapCamera
import com.gusak.lab.radarnebo.ui.map.MapSelection

/**
 * How the map screen opens. The app always uses the defaults; previews and README screenshots start
 * it zoomed in, with a target selected, a card showing or the radius control open.
 */
data class MapScreenStart(
    val camera: MapCamera = MapCamera.Overview,
    /** Opens the targets panel on this target's details. */
    val selectedTrackId: String? = null,
    /** Opens the targets panel on this threat, for one no track follows. */
    val selectedThreatId: String? = null,
    val selection: MapSelection? = null,
    /** Shows the "add this place to favorites?" card for this spot. */
    val pendingFavorite: GeoPoint? = null,
    val radiusOpen: Boolean = false,
    val targetsOpen: Boolean = selectedTrackId != null || selectedThreatId != null,
)
