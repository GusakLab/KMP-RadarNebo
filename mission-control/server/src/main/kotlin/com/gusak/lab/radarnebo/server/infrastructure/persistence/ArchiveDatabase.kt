package com.gusak.lab.radarnebo.server.infrastructure.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.sql.Connection

/**
 * The archive database, SQLite by default (`jdbc:sqlite:data/radarnebo-archive.db`). The schema is created
 * on first use. Queries run on [Dispatchers.IO], so request and poller coroutines never block on JDBC.
 */
internal class ArchiveDatabase(jdbcUrl: String) {
    private val database: Database

    init {
        val isSqlite = jdbcUrl.startsWith(SQLITE_PREFIX)
        sqliteFile(jdbcUrl)?.absoluteFile?.parentFile?.mkdirs()
        database = Database.connect(
            url = jdbcUrl,
            setupConnection = { connection ->
                if (isSqlite) connection.createStatement().use { it.execute("PRAGMA journal_mode=WAL") }
            },
            // SQLite supports only serializable transactions.
            databaseConfig = DatabaseConfig { defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE },
        )
        transaction(database) { SchemaUtils.create(AlertsTable, TracksTable) }
    }

    suspend fun <T> query(block: JdbcTransaction.() -> T): T =
        withContext(Dispatchers.IO) { transaction(database) { block() } }

    private companion object {
        const val SQLITE_PREFIX = "jdbc:sqlite:"

        /** The database file of a SQLite URL; null for other databases and in-memory (`:memory:`) URLs. */
        fun sqliteFile(jdbcUrl: String): File? {
            if (!jdbcUrl.startsWith(SQLITE_PREFIX)) return null
            val path = jdbcUrl.removePrefix(SQLITE_PREFIX).substringBefore('?')
            return path.takeIf { it.isNotBlank() && !it.startsWith(":") }?.let(::File)
        }
    }
}
