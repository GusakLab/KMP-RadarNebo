package com.gusak.lab.radarnebo.missioncontrol

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The server's `/health` report (mission-control/server's HealthRoutes.kt). Every field has a default, so a server
 * that adds or drops one still parses.
 */
@Serializable
data class Health(
    val status: String = "",
    val source: String = "",
    val simulated: Boolean = false,
    val upstreamBudgetPerMinute: Int? = null,
    val upstreamRequestsLastMinute: Int = 0,
    val upstreamRequestsTotal: Long = 0,
    val upstreamRateLimitedTotal: Long = 0,
    val upstreamRejectedTotal: Long = 0,
    val upstreamOtherFailuresTotal: Long = 0,
    val upstreamLastSuccessAt: Long? = null,
    val pollIntervalSeconds: Long = 0,
    val lastAttemptAt: Long? = null,
    val nextPollAt: Long? = null,
    val dataVersion: Long? = null,
    val dataConfirmedAt: Long? = null,
    val dataAgeSeconds: Long? = null,
    val alerts: Int = 0,
    val tracks: Int = 0,
    val activeTracks: Int = 0,
    val lastError: String? = null,
)

/** Reads `/health` from a RadarNebo server; it needs no client key. */
class HealthClient {
    private val http = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 10_000
        }
        defaultRequest { headers.append(HttpHeaders.UserAgent, "MissionControl-Nebo/${BuildConfig.VERSION_NAME} (Android)") }
    }

    suspend fun fetch(serverUrl: String): Health = http.get(serverUrl.trimEnd('/') + "/health").body()

    fun close() = http.close()
}
