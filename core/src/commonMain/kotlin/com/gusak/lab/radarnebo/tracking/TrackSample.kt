package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.geo.GeoPoint
import kotlinx.serialization.Serializable

/** One observed position of a drone group (or missile). [t] is epoch milliseconds. */
@Serializable
data class TrackSample(
    val t: Long,
    val lat: Double,
    val lon: Double,
    /** Where the threat was reported: settlement, hromada or raion name. */
    val title: String,
    val oblastUid: Int? = null,
    /** Original alert text, which may name the drone type. */
    val message: String? = null,
    /** How precisely the report places the target; null from servers that did not say (treated as a raion). */
    val precision: SamplePrecision? = null,
    /** The alert level of the report: a yellow one names where the target is heading. Null when not an alert. */
    val level: ThreatLevel? = null,
    /** One-sigma position error in km when the reported area's size is known; else [precision] decides. */
    val errorKm: Double? = null,
) {
    val point: GeoPoint get() = GeoPoint(lat, lon)
}

/**
 * The report's position error: its area's, else its precision's, or a raion's for reports from servers that did
 * not say.
 */
internal val TrackSample.sigmaKm: Double get() = errorKm ?: (precision ?: SamplePrecision.Raion).sigmaKm
