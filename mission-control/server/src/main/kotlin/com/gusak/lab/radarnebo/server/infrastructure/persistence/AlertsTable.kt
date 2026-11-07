package com.gusak.lab.radarnebo.server.infrastructure.persistence

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

/** Archived alerts, one row per alert record. [payload] is the alert in the alerts.in.ua format. */
internal object AlertsTable : Table("alerts") {
    /** Which data source the alert came from, so simulated and real ids never mix. */
    val partition: Column<String> = varchar("source", 32)
    val id: Column<Long> = long("id")
    val locationUid: Column<String> = varchar("location_uid", 64)
    val oblastUid: Column<Int?> = integer("oblast_uid").nullable()
    val startedAt: Column<Long> = long("started_at")
    val finishedAt: Column<Long?> = long("finished_at").nullable()
    val payload: Column<String> = text("payload")

    override val primaryKey: PrimaryKey = PrimaryKey(partition, id)

    init {
        index(isUnique = false, partition, locationUid, startedAt)
        index(isUnique = false, partition, oblastUid, startedAt)
        index(isUnique = false, partition, finishedAt)
    }
}
