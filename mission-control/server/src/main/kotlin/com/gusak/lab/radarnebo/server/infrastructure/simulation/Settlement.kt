package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.geo.City
import com.gusak.lab.radarnebo.server.domain.alert.AlertLocation

/** A settlement the simulated warning system declares alerts for. */
internal class Settlement(val city: City, val location: AlertLocation)
