package com.gusak.lab.radarnebo.server.infrastructure.upstream

import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.server.domain.frontline.FrontlineSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import org.slf4j.LoggerFactory
import kotlin.math.round

/**
 * DeepStateMap (deepstatemap.live), the only component that talks to it. Its latest map is GeoJSON: polygons
 * named by status, and points for units. The occupied polygons are kept, with those of the territory occupied
 * since 2014. The source owns the [client] it is given and closes it.
 */
internal class DeepStateFrontlineSource(
    private val client: HttpClient = defaultClient(),
    private val url: String = DEFAULT_URL,
) : FrontlineSource {
    private val log = LoggerFactory.getLogger(DeepStateFrontlineSource::class.java)

    override suspend fun fetch(): List<List<GeoPoint>>? = try {
        val response = client.get(url)
        if (response.status == HttpStatusCode.OK) {
            occupiedAreas(response.bodyAsText())
        } else {
            log.warn("DeepStateMap returned {}", response.status.value)
            null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.warn("DeepStateMap request failed: {}", e.message ?: e::class.simpleName)
        null
    }

    override fun close() {
        client.close()
    }

    companion object {
        const val DEFAULT_URL: String = "https://deepstatemap.live/api/history/last"

        /**
         * What a polygon's name ends with when it is occupied. Names hold the Ukrainian and English label and this
         * key, e.g. `Окуповано /// Occupied /// geoJSON.status.occupied`; only the key is stable.
         */
        private val OCCUPIED_KEYS = listOf(
            "geoJSON.status.occupied",
            "geoJSON.territories.ordlo",
            "geoJSON.territories.crimea",
            "geoJSON.territories.tuzla",
        )

        /** Three decimals of a degree, about 100 m: under a pixel at the app's closest zoom. */
        private const val PRECISION = 1_000.0

        fun defaultClient(): HttpClient = HttpClient(CIO) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
            }
        }

        /**
         * The outer ring of every occupied polygon in [body], DeepStateMap's `{"map": <FeatureCollection>}`, rounded
         * to [PRECISION]. Throws when [body] isn't that shape or holds no occupied polygon.
         */
        private fun occupiedAreas(body: String): List<List<GeoPoint>> {
            val features = Json.parseToJsonElement(body).field("map").field("features") as? JsonArray
            val areas = features.orEmpty()
                .filter { feature -> feature.name().let { name -> OCCUPIED_KEYS.any { it in name } } }
                .mapNotNull { it.field("geometry").outerRing() }
            require(areas.isNotEmpty()) { "no occupied areas in the map" }
            return areas
        }

        private fun JsonElement?.field(name: String): JsonElement? = (this as? JsonObject)?.get(name)

        private fun JsonElement.name(): String =
            (field("properties").field("name") as? JsonPrimitive)?.content.orEmpty()

        /** The outer ring of a GeoJSON Polygon; null for any other geometry or a ring that isn't an area. */
        private fun JsonElement?.outerRing(): List<GeoPoint>? {
            if ((field("type") as? JsonPrimitive)?.content != "Polygon") return null
            val ring = ((field("coordinates") as? JsonArray)?.firstOrNull() as? JsonArray).orEmpty()
                .mapNotNull { it.point() }
                // Rounding makes neighbours equal; one of each run is enough.
                .fold(emptyList<GeoPoint>()) { kept, point -> if (kept.lastOrNull() == point) kept else kept + point }
            return ring.takeIf { it.size >= 3 }
        }

        /** A GeoJSON position, `[lon, lat]` or `[lon, lat, altitude]`. */
        private fun JsonElement.point(): GeoPoint? {
            val position = (this as? JsonArray)?.map { (it as? JsonPrimitive)?.doubleOrNull } ?: return null
            val lon = position.getOrNull(0) ?: return null
            val lat = position.getOrNull(1) ?: return null
            return GeoPoint(lat = round(lat * PRECISION) / PRECISION, lon = round(lon * PRECISION) / PRECISION)
        }
    }
}
