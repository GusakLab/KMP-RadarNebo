package com.gusak.lab.radarnebo.tracking

import kotlinx.serialization.Serializable

/**
 * How precisely a report places its target: alerts name an area, never a point, so a report's position is the
 * area's centre with an error of about [sigmaKm].
 */
@Serializable
enum class SamplePrecision(internal val sigmaKm: Double) {
    /** A position fix (surveillance, the simulation). */
    Exact(1.0),
    City(7.0),
    Hromada(12.0),
    Raion(25.0),
    /** Somewhere in the oblast; a point is only an entry estimate. */
    Oblast(80.0),
}
