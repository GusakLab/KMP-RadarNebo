package com.gusak.lab.radarnebo.server.infrastructure.persistence

import com.gusak.lab.radarnebo.server.domain.tracking.TrackArchive
import com.gusak.lab.radarnebo.server.domain.tracking.TrackQuery
import com.gusak.lab.radarnebo.tracking.DroneTrack
import java.util.concurrent.ConcurrentHashMap

/** [TrackArchive] in memory, for tests and for running without a database. */
internal class InMemoryTrackArchive : TrackArchive {
    private val tracks = ConcurrentHashMap<String, DroneTrack>()

    override suspend fun save(tracks: Collection<DroneTrack>) {
        tracks.forEach { this.tracks[it.id] = it }
    }

    override suspend fun history(query: TrackQuery): List<DroneTrack> = tracks.values
        .filter(query::overlaps)
        .sortedByDescending { it.lastSeen }
        .take(query.limit)

    override suspend fun prune(beforeMs: Long): Int {
        val old = tracks.values.filter { it.lastSeen < beforeMs }
        old.forEach { tracks.remove(it.id) }
        return old.size
    }
}
