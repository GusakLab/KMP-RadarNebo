package com.gusak.lab.radarnebo.server.config

/** How the server builds tracks from what its source reports (`TRACKING`). */
enum class TrackingMode {
    /** Sightings when the source has surveillance, alerts otherwise. */
    Auto,

    /** From alerts alone, even when the source also sights targets. */
    Alerts,

    /** From sightings alone. */
    Sightings,

    /** Alerts and sightings fused into one picture. */
    Fused,
}
