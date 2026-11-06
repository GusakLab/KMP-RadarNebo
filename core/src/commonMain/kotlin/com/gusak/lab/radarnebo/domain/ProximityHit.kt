package com.gusak.lab.radarnebo.domain

import com.gusak.lab.radarnebo.data.FavoriteLocation

data class ProximityHit(
    val favorite: FavoriteLocation,
    val threat: ThreatMarker,
    val distanceKm: Double,
    /** True when the favorite lies inside an oblast that has an oblast-wide threat. */
    val insideRegion: Boolean,
)
