package com.gusak.lab.radarnebo.server.domain.tracking

import com.gusak.lab.radarnebo.tracking.DroneTrack

/** Tracks of finished flights. */
internal interface TrackArchive {
    /** Inserts or replaces [tracks] by id. */
    suspend fun save(tracks: Collection<DroneTrack>)

    /** Tracks that were in the air at some point in [query]'s window, newest first. */
    suspend fun history(query: TrackQuery): List<DroneTrack>

    /** Deletes tracks last seen before [beforeMs]; returns how many. */
    suspend fun prune(beforeMs: Long): Int
}
