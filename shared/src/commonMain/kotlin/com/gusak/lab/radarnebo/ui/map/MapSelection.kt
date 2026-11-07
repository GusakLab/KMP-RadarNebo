package com.gusak.lab.radarnebo.ui.map

import com.gusak.lab.radarnebo.data.FavoriteLocation
import com.gusak.lab.radarnebo.domain.ThreatMarker
import com.gusak.lab.radarnebo.tracking.DroneTrack
import com.gusak.lab.radarnebo.tracking.InterpolatedPosition

/** What a tap on the map picked. */
sealed interface MapSelection {
    data class Threat(val marker: ThreatMarker) : MapSelection
    data class Favorite(val favorite: FavoriteLocation) : MapSelection
    data class Track(val track: DroneTrack, val position: InterpolatedPosition) : MapSelection
}
