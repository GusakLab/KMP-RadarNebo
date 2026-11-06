package com.gusak.lab.radarnebo.tracking

import kotlinx.serialization.Serializable

/** How a tracked flight ended. */
@Serializable
enum class TrackOutcome {
    /** Shot down or jammed by air defence. */
    Intercepted,

    /** Reached its target. */
    Impact,

    /** Contact lost: no more reports, fate unknown. */
    Lost,
}
