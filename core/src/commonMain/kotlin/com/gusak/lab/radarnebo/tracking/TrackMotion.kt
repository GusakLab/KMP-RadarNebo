package com.gusak.lab.radarnebo.tracking

/** How a target moves: heading in degrees clockwise from north, and speed in km/h. */
data class TrackMotion(val headingDeg: Double, val speedKmh: Double)
