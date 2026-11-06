package com.gusak.lab.radarnebo.server.infrastructure.persistence

import com.gusak.lab.radarnebo.server.contract.AlertsInUaContract
import com.gusak.lab.radarnebo.server.domain.tracking.TrackArchive
import com.gusak.lab.radarnebo.server.domain.tracking.TrackQuery
import com.gusak.lab.radarnebo.tracking.DroneTrack
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/** [TrackArchive] in [ArchiveDatabase], partitioned by data source ([partitionKey]). */
internal class ExposedTrackArchive(private val db: ArchiveDatabase, private val partitionKey: String) : TrackArchive {

    override suspend fun save(tracks: Collection<DroneTrack>) {
        if (tracks.isEmpty()) return
        db.query {
            TracksTable.batchUpsert(tracks) { track ->
                this[TracksTable.partition] = partitionKey
                this[TracksTable.id] = track.id
                this[TracksTable.type] = track.type.apiValue
                this[TracksTable.firstSeen] = track.firstSeen
                this[TracksTable.lastSeen] = track.lastSeen
                this[TracksTable.payload] = AlertsInUaContract.json.encodeToString(DroneTrack.serializer(), track)
            }
        }
    }

    override suspend fun history(query: TrackQuery): List<DroneTrack> = db.query {
        TracksTable.selectAll()
            .where {
                val window = (TracksTable.partition eq partitionKey) and
                    (TracksTable.firstSeen lessEq query.toMs) and (TracksTable.lastSeen greaterEq query.fromMs)
                query.type?.let { window and (TracksTable.type eq it.apiValue) } ?: window
            }
            .orderBy(TracksTable.lastSeen, SortOrder.DESC)
            .limit(query.limit)
            .map { AlertsInUaContract.json.decodeFromString(DroneTrack.serializer(), it[TracksTable.payload]) }
    }

    override suspend fun prune(beforeMs: Long): Int = db.query {
        TracksTable.deleteWhere { (TracksTable.partition eq partitionKey) and (TracksTable.lastSeen less beforeMs) }
    }
}
