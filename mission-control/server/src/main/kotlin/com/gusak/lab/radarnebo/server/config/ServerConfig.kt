package com.gusak.lab.radarnebo.server.config

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Everything the server is configured with; built from the environment by [fromEnv]. */
data class ServerConfig(
    val port: Int = 8080,
    val upstream: UpstreamConfig = UpstreamConfig(),
    val simulation: SimulationConfig = SimulationConfig(),
    val archive: ArchiveConfig = ArchiveConfig(),
    val clients: ClientApiConfig = ClientApiConfig(),
    /** Explicit choice; by default alerts.in.ua when a token is set, the simulation otherwise. */
    val dataSourceOverride: DataSourceKind? = null,
    /** Logs every alerts.in.ua request and response, body included, with the token masked. */
    val logHttp: Boolean = false,
    val tracking: TrackingMode = TrackingMode.Auto,
    /** Public Telegram warning channels read as extra sources for tracking (`TELEGRAM_CHANNELS`, e.g. `kpszsu`). */
    val telegramChannels: List<String> = emptyList(),
) {
    init {
        // 0 asks for any free port, as the tests do.
        require(port in 0..65_535) { "PORT must be between 0 and 65535" }
    }

    val dataSource: DataSourceKind
        get() = dataSourceOverride
            ?: if (upstream.token.isNullOrBlank()) DataSourceKind.Simulation else DataSourceKind.AlertsInUa

    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): ServerConfig {
            val reader = EnvReader(env)
            val defaults = ServerConfig()
            // Unset or blank means the data-class default; a set value that doesn't parse is reported below.
            val config = ServerConfig(
                port = reader.int("PORT") ?: defaults.port,
                upstream = reader.upstream(defaults.upstream),
                simulation = reader.simulation(defaults.simulation),
                archive = reader.archive(defaults.archive),
                clients = reader.clients(defaults.clients),
                dataSourceOverride = reader.dataSourceOverride(),
                logHttp = reader.bool("LOG_HTTP"),
                tracking = reader.tracking() ?: defaults.tracking,
                telegramChannels = reader.telegramChannels(),
            )
            require(reader.errors.isEmpty()) { "Invalid configuration: ${reader.errors.joinToString("; ")}" }
            return config
        }
    }
}

/** Reads the variables of [env] for [ServerConfig.fromEnv], collecting every value that doesn't parse in [errors]. */
private class EnvReader(private val env: Map<String, String>) {
    private val problems = mutableListOf<String>()

    val errors: List<String> get() = problems

    fun upstream(defaults: UpstreamConfig): UpstreamConfig = UpstreamConfig(
        token = text("ALERTS_API_TOKEN"),
        baseUrl = text("UPSTREAM_BASE_URL") ?: defaults.baseUrl,
        maxPerMinute = int("UPSTREAM_MAX_PER_MINUTE") ?: defaults.maxPerMinute,
        pollInterval = int("POLL_INTERVAL_SECONDS")?.seconds ?: defaults.pollInterval,
    )

    fun simulation(defaults: SimulationConfig): SimulationConfig = SimulationConfig(
        seed = parse("SIM_SEED", "an integer", String::toLongOrNull) ?: defaults.seed,
        strikePeriod = int("SIM_STRIKE_PERIOD_MINUTES")?.minutes ?: defaults.strikePeriod,
        tick = int("SIM_TICK_SECONDS")?.seconds ?: defaults.tick,
        warmUp = int("SIM_WARM_UP_MINUTES")?.minutes ?: defaults.warmUp,
    )

    fun archive(defaults: ArchiveConfig): ArchiveConfig = ArchiveConfig(
        jdbcUrl = text("ARCHIVE_JDBC_URL") ?: defaults.jdbcUrl,
        retention = int("ARCHIVE_RETENTION_DAYS")?.days ?: defaults.retention,
    )

    fun clients(defaults: ClientApiConfig): ClientApiConfig = ClientApiConfig(
        keys = (text("CLIENT_KEYS") ?: text("PROXY_CLIENT_KEYS")).orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet(),
        requestsPerMinute = int("CLIENT_REQUESTS_PER_MINUTE") ?: defaults.requestsPerMinute,
        historyRequestsPerMinute = int("HISTORY_REQUESTS_PER_MINUTE") ?: defaults.historyRequestsPerMinute,
        archiveRequestsPerMinute = int("ARCHIVE_REQUESTS_PER_MINUTE") ?: defaults.archiveRequestsPerMinute,
        trustProxyHeaders = bool("TRUST_PROXY_HEADERS"),
    )

    fun dataSourceOverride(): DataSourceKind? =
        parse("DATA_SOURCE", "simulation, alerts_in_ua, alerts.in.ua or upstream") {
            when (it.lowercase()) {
                "simulation" -> DataSourceKind.Simulation
                "alerts_in_ua", "alerts.in.ua", "upstream" -> DataSourceKind.AlertsInUa
                else -> null
            }
        }

    fun tracking(): TrackingMode? = parse("TRACKING", "auto, alerts, sightings or fused") { value ->
        TrackingMode.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }

    fun telegramChannels(): List<String> = text("TELEGRAM_CHANNELS").orEmpty()
        .split(',')
        .map { it.trim().removePrefix("@") }
        .filter { it.isNotEmpty() }

    fun int(name: String): Int? = parse(name, "an integer", String::toIntOrNull)

    fun bool(name: String): Boolean = parse(name, "true or false") { it.lowercase().toBooleanStrictOrNull() } ?: false

    private fun text(name: String): String? = env[name]?.trim()?.takeIf { it.isNotEmpty() }

    private fun <T : Any> parse(name: String, kind: String, parse: (String) -> T?): T? =
        text(name)?.let { value -> parse(value).also { if (it == null) problems += "$name='$value' is not $kind" } }
}
