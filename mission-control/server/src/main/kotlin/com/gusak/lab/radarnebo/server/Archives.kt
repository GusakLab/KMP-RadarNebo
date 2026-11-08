package com.gusak.lab.radarnebo.server

import com.gusak.lab.radarnebo.server.config.ObservabilityConfig
import com.gusak.lab.radarnebo.server.config.ServerConfig
import com.gusak.lab.radarnebo.server.domain.alert.AlertArchive
import com.gusak.lab.radarnebo.server.domain.tracking.TrackArchive
import com.gusak.lab.radarnebo.server.infrastructure.persistence.ArchiveDatabase
import com.gusak.lab.radarnebo.server.infrastructure.persistence.ExposedAlertArchive
import com.gusak.lab.radarnebo.server.infrastructure.persistence.ExposedTrackArchive
import com.gusak.lab.radarnebo.server.infrastructure.persistence.InMemoryAlertArchive
import com.gusak.lab.radarnebo.server.infrastructure.persistence.InMemoryTrackArchive

/** The archive, in [ArchiveDatabase] or, with `ARCHIVE_JDBC_URL=memory`, in memory. */
class Archives internal constructor(
    internal val alerts: AlertArchive,
    internal val tracks: TrackArchive,
    private val database: ArchiveDatabase? = null,
) : AutoCloseable {
    /** Closes the Hikari pool. In-memory archives hold nothing, so this is a no-op. */
    override fun close() {
        database?.close()
    }

    internal companion object {
        fun inMemory(): Archives = Archives(InMemoryAlertArchive(), InMemoryTrackArchive())

        fun create(
            config: ServerConfig,
            hikariMaxPool: Int = ObservabilityConfig.DEFAULT_MAX_POOL,
        ): Archives {
            if (config.archive.inMemory) return inMemory()
            val db = ArchiveDatabase(config.archive.jdbcUrl, hikariMaxPool)
            val partition = config.dataSource.key
            return Archives(ExposedAlertArchive(db, partition), ExposedTrackArchive(db, partition), db)
        }
    }
}
