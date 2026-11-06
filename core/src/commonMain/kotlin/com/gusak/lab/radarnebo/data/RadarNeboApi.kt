package com.gusak.lab.radarnebo.data

import com.gusak.lab.radarnebo.debug.TrafficRecorder
import com.gusak.lab.radarnebo.debug.httpTrafficCapture
import com.gusak.lab.radarnebo.tracking.DroneTracksResponse
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Client for the RadarNebo server (`:server`), the app's only data source. The server decides where its
 * data comes from (alerts.in.ua or its simulation), tracks targets and archives history; the app only reads.
 *
 * Alerts are served in the alerts.in.ua format at the same path, as alerts.in.ua sent them; every poll gets
 * the whole list.
 */
class RadarNeboApi(
    private val client: HttpClient = defaultClient(),
) {
    suspend fun activeAlerts(endpoint: ServerEndpoint): FetchResult = try {
        val response = client.get(endpoint.resolve(ACTIVE_ALERTS_PATH)) { authorize(endpoint) }
        when (response.status) {
            HttpStatusCode.OK -> FetchResult.Fresh(response.body<ActiveAlertsResponse>().alerts)
            HttpStatusCode.Unauthorized ->
                FetchResult.Failure("The server needs a valid access key (401)", backOff = true)
            HttpStatusCode.TooManyRequests ->
                FetchResult.Failure("Too many requests, slowing down (429)", backOff = true)
            HttpStatusCode.ServiceUnavailable -> FetchResult.Failure("The server is starting up, no data yet (503)")
            else -> FetchResult.Failure("Unexpected response ${response.status.value}")
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        FetchResult.Failure(e.message ?: "Network error")
    }

    /** Tracked targets with their position history; null on any failure. */
    suspend fun tracks(endpoint: ServerEndpoint): DroneTracksResponse? = try {
        val response = client.get(endpoint.resolve(TRACKS_PATH)) { authorize(endpoint) }
        if (response.status == HttpStatusCode.OK) response.body<DroneTracksResponse>() else null
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** The server's health; null when it can't be reached or doesn't answer. Needs no access key. */
    suspend fun health(endpoint: ServerEndpoint): ServerHealth? = try {
        val response = client.get(endpoint.resolve(HEALTH_PATH))
        if (response.status == HttpStatusCode.OK) response.body<ServerHealth>() else null
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun ServerEndpoint.resolve(path: String) = "${url.trimEnd('/')}$path"

    private fun HttpRequestBuilder.authorize(endpoint: ServerEndpoint) {
        if (endpoint.accessKey.isNotBlank()) header(HttpHeaders.Authorization, "Bearer ${endpoint.accessKey}")
    }

    companion object {
        /** The deployed RadarNebo server. */
        const val DEFAULT_SERVER_URL: String = "https://radar-nebo.worxbend.com"

        const val ACTIVE_ALERTS_PATH: String = "/v1/alerts/active.json"
        const val TRACKS_PATH: String = "/v1/drones/tracks.json"
        const val POSITIONS_PATH: String = "/v1/drones/positions.json"
        const val TRACK_ARCHIVE_PATH: String = "/v1/drones/archive.json"
        const val HEALTH_PATH: String = "/health"

        val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

        /**
         * With [httpLogger], every request and response is logged with headers and body; [traffic] receives them for
         * the request inspector. The access key is masked in both.
         */
        fun defaultClient(httpLogger: Logger? = null, traffic: TrafficRecorder? = null): HttpClient =
            HttpClient { configure(httpLogger, traffic) }

        /** A client on [engine], configured like the default one; tests pass a mock engine. */
        fun client(engine: HttpClientEngine, traffic: TrafficRecorder? = null): HttpClient =
            HttpClient(engine) { configure(httpLogger = null, traffic = traffic) }

        private fun HttpClientConfig<*>.configure(httpLogger: Logger?, traffic: TrafficRecorder?) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }
            if (httpLogger != null) logHttp(httpLogger)
            if (traffic != null) install(httpTrafficCapture(traffic))
        }

        /** Logs whole exchanges, bodies included, to [logger], with the Authorization header masked. */
        private fun HttpClientConfig<*>.logHttp(logger: Logger) {
            install(Logging) {
                this.logger = logger
                level = LogLevel.ALL
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
        }
    }
}
