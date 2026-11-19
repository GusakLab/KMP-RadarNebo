package com.gusak.lab.radarnebo.server.domain.frontline

import com.gusak.lab.radarnebo.geo.GeoPoint

/** Where the server gets the occupied territory from. */
interface FrontlineSource : AutoCloseable {
    /** The outline of every occupied area; null when the source can't be read. */
    suspend fun fetch(): List<List<GeoPoint>>?

    /** Releases what the source holds, e.g. its HTTP client. */
    override fun close() {}
}
