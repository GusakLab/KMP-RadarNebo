package com.gusak.lab.radarnebo.server.infrastructure.persistence

import com.gusak.lab.radarnebo.data.Alert
import com.gusak.lab.radarnebo.server.contract.AlertsInUaContract
import com.gusak.lab.radarnebo.server.contract.AlertsInUaContract.toDomain
import com.gusak.lab.radarnebo.server.contract.AlertsInUaContract.toDto
import com.gusak.lab.radarnebo.server.domain.alert.AirAlert
import com.gusak.lab.radarnebo.server.domain.alert.AlertArchive
import com.gusak.lab.radarnebo.server.domain.alert.LocationDirectory
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant

/** [AlertArchive] in [ArchiveDatabase], partitioned by data source ([partitionKey]). */
internal class ExposedAlertArchive(private val db: ArchiveDatabase, private val partitionKey: String) : AlertArchive {

    override suspend fun save(alerts: Collection<AirAlert>) {
        if (alerts.isEmpty()) return
        db.query {
            AlertsTable.batchUpsert(alerts) { alert ->
                this[AlertsTable.partition] = partitionKey
                this[AlertsTable.id] = alert.id
                this[AlertsTable.locationUid] = alert.location.uid
                this[AlertsTable.oblastUid] = alert.location.oblastUid
                this[AlertsTable.startedAt] = alert.startedAt.toEpochMilliseconds()
                this[AlertsTable.finishedAt] = alert.finishedAt?.toEpochMilliseconds()
                this[AlertsTable.payload] = AlertsInUaContract.json.encodeToString(Alert.serializer(), alert.toDto())
            }
        }
    }

    override suspend fun unfinished(): List<AirAlert> = db.query {
        AlertsTable.selectAll()
            .where { (AlertsTable.partition eq partitionKey) and AlertsTable.finishedAt.isNull() }
            .map(::toAlert)
    }

    override suspend fun history(uid: String, since: Instant, limit: Int): List<AirAlert> = db.query {
        val region = uid.takeIf { LocationDirectory.isRegionUid(it) }?.toInt()
        val location = if (region != null) {
            (AlertsTable.locationUid eq uid) or (AlertsTable.oblastUid eq region)
        } else {
            AlertsTable.locationUid eq uid
        }
        AlertsTable.selectAll()
            .where {
                (AlertsTable.partition eq partitionKey) and location and
                    (AlertsTable.finishedAt.isNull() or (AlertsTable.finishedAt greaterEq since.toEpochMilliseconds()))
            }
            .orderBy(AlertsTable.startedAt, SortOrder.DESC)
            .limit(limit)
            .map(::toAlert)
    }

    override suspend fun prune(before: Instant): Int = db.query {
        AlertsTable.deleteWhere {
            (AlertsTable.partition eq partitionKey) and (AlertsTable.finishedAt less before.toEpochMilliseconds())
        }
    }

    private fun toAlert(row: ResultRow): AirAlert {
        val dto = AlertsInUaContract.json.decodeFromString(Alert.serializer(), row[AlertsTable.payload])
        return dto.toDomain(Instant.fromEpochMilliseconds(row[AlertsTable.startedAt]))
    }
}
