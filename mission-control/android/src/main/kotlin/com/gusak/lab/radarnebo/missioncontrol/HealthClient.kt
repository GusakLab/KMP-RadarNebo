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
import kotlinx.serialization.json.Json

/** Reads `/health` from a RadarNebo server; it needs no client key. */
internal class HealthClient {
    private val http = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 10_000
        }
        defaultRequest {
            headers.append(HttpHeaders.UserAgent, "MissionControl-Nebo/${BuildConfig.VERSION_NAME} (Android)")
        }
    }

    suspend fun fetch(serverUrl: String): Health = http.get(serverUrl.trimEnd('/') + "/health").body()

    fun close(): Unit = http.close()
}
