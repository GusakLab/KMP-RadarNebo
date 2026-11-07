package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.geo.GeoPoint

/** A settlement or user favorite that a drone may be heading towards. */
data class Place(val name: String, val point: GeoPoint, val isFavorite: Boolean = false)
