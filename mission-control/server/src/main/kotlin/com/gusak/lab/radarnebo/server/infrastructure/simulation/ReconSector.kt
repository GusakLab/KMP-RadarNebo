package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.geo.GeoPoint

/** A stretch of the front where reconnaissance drones cross in from [base] to look at [settlements]. */
internal class ReconSector(val base: GeoPoint, val settlements: List<String>)
