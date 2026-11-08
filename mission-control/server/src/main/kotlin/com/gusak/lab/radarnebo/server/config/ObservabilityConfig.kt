package com.gusak.lab.radarnebo.server.config

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Metrics, tracing and the archive pool. Loaded once at startup, beside [ServerConfig].
 * [fromEnv] is the only reader of the process environment; tests build this with the defaults.
 */
data class ObservabilityConfig(
    /** Pool size for a non-SQLite archive. SQLite forces one connection in the archive itself. */
    val hikariMaxPool: Int = DEFAULT_MAX_POOL,
    /** OTLP/gRPC endpoint. Null disables tracing, so responses gain no trace headers. */
    val otelEndpoint: String? = null,
) {
    init {
        require(hikariMaxPool in POOL_RANGE) { hikariMaxPoolMessage(hikariMaxPool.toString()) }
    }

    companion object {
        const val DEFAULT_MAX_POOL: Int = 10
        val POOL_RANGE: IntRange = 1..16

        /** [fromEnv], throwing [IllegalArgumentException] with the [Either.Left] message. */
        fun fromEnvOrThrow(env: (String) -> String? = System::getenv): ObservabilityConfig = fromEnv(env).fold(
            { message -> throw IllegalArgumentException(message) },
            { it },
        )

        /**
         * Blank or unset `HIKARI_MAX_POOL` keeps [DEFAULT_MAX_POOL]. A value that is not an integer from 1 to 16
         * is [Either.Left]. A blank `OTEL_EXPORTER_OTLP_ENDPOINT` leaves [otelEndpoint] null.
         */
        fun fromEnv(env: (String) -> String? = System::getenv): Either<String, ObservabilityConfig> {
            val rawPool = env(HIKARI_MAX_POOL)?.trim().orEmpty()
            val pool = if (rawPool.isEmpty()) {
                DEFAULT_MAX_POOL
            } else {
                val parsed = rawPool.toIntOrNull()
                if (parsed == null || parsed !in POOL_RANGE) return hikariMaxPoolMessage(rawPool).left()
                parsed
            }
            val endpoint = env(OTEL_ENDPOINT)?.trim()?.takeIf { it.isNotEmpty() }
            return ObservabilityConfig(hikariMaxPool = pool, otelEndpoint = endpoint).right()
        }

        private const val HIKARI_MAX_POOL: String = "HIKARI_MAX_POOL"
        private const val OTEL_ENDPOINT: String = "OTEL_EXPORTER_OTLP_ENDPOINT"

        private fun hikariMaxPoolMessage(raw: String): String =
            "HIKARI_MAX_POOL='$raw' is not an integer between 1 and 16"
    }
}
