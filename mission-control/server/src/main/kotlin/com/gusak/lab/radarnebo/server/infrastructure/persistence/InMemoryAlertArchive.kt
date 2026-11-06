package com.gusak.lab.radarnebo.server.infrastructure.persistence

import com.gusak.lab.radarnebo.server.domain.alert.AirAlert
import com.gusak.lab.radarnebo.server.domain.alert.AlertArchive
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Instant

/** [AlertArchive] in memory, for tests and for running without a database. */
internal class InMemoryAlertArchive : AlertArchive {
    private val alerts = ConcurrentHashMap<Long, AirAlert>()

    override suspend fun save(alerts: Collection<AirAlert>) {
        alerts.forEach { this.alerts[it.id] = it }
    }

    override suspend fun unfinished(): List<AirAlert> = alerts.values.filter { it.isActive }

    override suspend fun history(uid: String, since: Instant, limit: Int): List<AirAlert> = alerts.values
        .filter { it.concerns(uid) && it.activeSince(since) }
        .sortedByDescending { it.startedAt }
        .take(limit)

    override suspend fun prune(before: Instant): Int {
        val old = alerts.values.filter { alert -> alert.finishedAt?.let { it < before } == true }
        old.forEach { alerts.remove(it.id) }
        return old.size
    }
}
