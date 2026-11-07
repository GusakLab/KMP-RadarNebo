package com.gusak.lab.radarnebo.server.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** The SQLite archive of finished alerts and tracks. */
data class ArchiveConfig(
    val jdbcUrl: String = DEFAULT_JDBC_URL,
    val retention: Duration = 45.days,
) {
    init {
        // Pruning keeps only what finished within the retention, so zero or less would empty the archive.
        require(retention.isPositive()) { "ARCHIVE_RETENTION_DAYS must be positive" }
    }

    /** True for ARCHIVE_JDBC_URL=memory: the archive is kept in memory, not in a database. */
    val inMemory: Boolean get() = jdbcUrl == IN_MEMORY

    companion object {
        const val DEFAULT_JDBC_URL: String = "jdbc:sqlite:data/radarnebo-archive.db"
        internal const val IN_MEMORY: String = "memory"
    }
}
