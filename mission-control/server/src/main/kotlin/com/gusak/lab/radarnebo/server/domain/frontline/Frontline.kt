package com.gusak.lab.radarnebo.server.domain.frontline

import com.gusak.lab.radarnebo.geo.GeoPoint

/** The territory Russia occupies: the outline of each area. Its edge inside Ukraine is the frontline. */
internal data class Frontline(
    /** When the areas came from the [FrontlineSource], in epoch milliseconds. */
    val fetchedAtMs: Long,
    val areas: List<List<GeoPoint>>,
)
