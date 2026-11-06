package com.gusak.lab.radarnebo.geo

/** An oblast's outline as [UkraineGeometry] holds it: [lonLat] packs lon,lat pairs. */
class OblastShape(
    val uid: Int,
    internal val centerLat: Double,
    internal val centerLon: Double,
    val lonLat: FloatArray,
)
