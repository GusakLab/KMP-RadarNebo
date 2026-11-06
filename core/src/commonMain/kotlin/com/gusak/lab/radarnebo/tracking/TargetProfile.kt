package com.gusak.lab.radarnebo.tracking

/** Type information shown for a target. */
data class TargetProfile(
    val kind: TargetKind,
    val model: String,
    val make: String,
    val role: String,
    /** Typical speed, km/h; null for things that don't fly (air defence) or aren't known. */
    val cruiseSpeedKmh: IntRange?,
    val source: Source = Source.Reported,
) {
    enum class Source {
        /** The alert text named the type. */
        Reported,

        /** alerts.in.ua does not identify models; inferred from the threat class and flight profile. */
        Probable,
    }
}
