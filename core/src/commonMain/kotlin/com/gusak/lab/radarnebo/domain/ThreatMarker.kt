package com.gusak.lab.radarnebo.domain

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.tracking.SamplePrecision
import kotlin.time.Instant

/** A single threat placed on the map. One API alert can carry several threats. */
data class ThreatMarker(
    val id: String,
    val type: ThreatType,
    val level: ThreatLevel,
    val title: String,
    val oblastUid: Int?,
    val oblastName: String,
    val locationType: String,
    val point: GeoPoint,
    val startedAt: Instant?,
    val message: String?,
    /** How precisely [point] places the threat; null when not known (taken as a raion). */
    val precision: SamplePrecision? = null,
    /** How far from [point] the threat may be, in km, when the area's size is known; else [precision] decides. */
    val errorKm: Double? = null,
) {
    /** Oblast-wide threats cover the whole region rather than a specific spot. */
    val isRegionWide: Boolean get() = locationType == "oblast"
}
