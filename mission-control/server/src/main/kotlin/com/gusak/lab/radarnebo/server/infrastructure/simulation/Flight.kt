package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.tracking.TrackOutcome

/**
 * One airborne target: a drone group, a missile or a bomb. Its fate is decided when it is planned: it
 * ends at [endAt], shot down, lost or on its target ([outcome]). Air surveillance sees it from [detectedAt].
 */
internal class Flight(
    val id: String,
    val munition: Munition,
    val path: FlightPath,
    val endAt: Long,
    val outcome: TrackOutcome,
    val detectedAt: Long,
    /** Drones in the group; 1 for missiles and bombs. */
    val count: Int = 1,
    /** Where it was launched from, as the warnings name it ("з Курської області"). */
    private val origin: String,
    /** The settlement or region it is aimed at. */
    val targetName: String,
    val targetOblastUid: Int,
    val level: ThreatLevel = ThreatLevel.Red,
) {
    val departAt: Long get() = path.departAt
    val threat: ThreatType get() = munition.threat

    val message: String = buildString {
        append(munition.message)
        if (count > 1) append(", група $count од.")
        if (origin.isNotEmpty()) append(", ").append(fromPreposition(origin)).append(' ').append(origin)
    }

    /** "з Курської області", but "зі Смоленської області": Ukrainian uses "зі" before з, с, ш, щ, ж and a consonant. */
    private fun fromPreposition(word: String): String {
        val first = word.firstOrNull()?.lowercaseChar()
        val second = word.getOrNull(1)?.lowercaseChar()
        return if (first != null && first in "зсшщж" && second != null && second !in "аеєиіїоуюя") "зі" else "з"
    }

    /** In the air and seen by air surveillance at [t]. */
    fun isTracked(t: Long): Boolean = t in detectedAt until endAt

    fun positionAt(t: Long): GeoPoint = path.positionAt(minOf(t, endAt))

    val endPoint: GeoPoint get() = path.positionAt(endAt)
}
