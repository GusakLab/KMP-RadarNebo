package com.gusak.lab.radarnebo.console

import com.gusak.lab.radarnebo.server.config.ArchiveConfig
import com.gusak.lab.radarnebo.server.config.ObservabilityConfig
import com.gusak.lab.radarnebo.server.config.ServerConfig
import java.io.File

/** [ServerConfig] plus [ObservabilityConfig], from the same merged environment. */
internal data class ConsoleStartup(
    val config: ServerConfig,
    val observability: ObservabilityConfig,
)

/**
 * The server's settings as `:mission-control:server:run` gets them: the environment, then `.env` in the server's
 * directory (the `radarnebo.server.dir` system property the build passes), with the archive kept under that directory.
 * Variables set in the environment win over the file. A rejected [ObservabilityConfig] throws
 * [IllegalArgumentException] with the parse message, the same way an invalid [ServerConfig] does.
 */
internal fun consoleStartup(
    serverDir: File? = System.getProperty("radarnebo.server.dir")?.let(::File),
    environment: Map<String, String> = System.getenv(),
): ConsoleStartup {
    val fromFile = serverDir?.resolve(".env")?.takeIf { it.isFile }?.let(::readEnvFile).orEmpty()
    val env = fromFile + environment
    val observability = ObservabilityConfig.fromEnvOrThrow(env::get)
    return ConsoleStartup(adjustArchive(serverDir, env, ServerConfig.fromEnv(env)), observability)
}

/** The server's settings. See [consoleStartup]. */
internal fun consoleConfig(
    serverDir: File? = System.getProperty("radarnebo.server.dir")?.let(::File),
    environment: Map<String, String> = System.getenv(),
): ServerConfig = consoleStartup(serverDir, environment).config

private fun adjustArchive(serverDir: File?, env: Map<String, String>, config: ServerConfig): ServerConfig {
    if (serverDir == null || "ARCHIVE_JDBC_URL" in env) return config
    val archive = serverDir.resolve(ArchiveConfig.DEFAULT_JDBC_URL.removePrefix("jdbc:sqlite:")).absolutePath
    return config.copy(archive = config.archive.copy(jdbcUrl = "jdbc:sqlite:$archive"))
}

/** `KEY=value` lines; blank values, comments and lines without `=` are skipped, surrounding quotes removed. */
internal fun readEnvFile(file: File): Map<String, String> = file.readLines()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") && "=" in it }
    .map { it.substringBefore("=").trim() to it.substringAfter("=").trim().removeSurrounding("\"") }
    .filter { (_, value) -> value.isNotEmpty() }
    .toMap()
