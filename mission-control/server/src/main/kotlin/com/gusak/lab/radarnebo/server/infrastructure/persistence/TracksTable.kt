package com.gusak.lab.radarnebo.server.infrastructure.persistence

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

/** Archived tracks of finished flights. [payload] is the track as the tracks API serves it. */
internal object TracksTable : Table("tracks") {
    val partition: Column<String> = varchar("source", 32)
    val id: Column<String> = varchar("id", 128)
    val type: Column<String> = varchar("type", 64)
    val firstSeen: Column<Long> = long("first_seen")
    val lastSeen: Column<Long> = long("last_seen")
    val payload: Column<String> = text("payload")

    override val primaryKey: PrimaryKey = PrimaryKey(partition, id)

    init {
        index(isUnique = false, partition, lastSeen)
    }
}
