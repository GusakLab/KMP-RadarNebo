package com.gusak.lab.radarnebo.server.infrastructure.upstream

import com.gusak.lab.radarnebo.geo.GeoPoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepStateFrontlineSourceTest {
    private fun feature(name: String, geometry: String) =
        """{"type":"Feature","properties":{"name":"$name"},"geometry":$geometry}"""

    private fun polygon(vararg lonLat: String) =
        """{"type":"Polygon","coordinates":[[${lonLat.joinToString(",") { "[$it]" }}]]}"""

    private fun map(vararg features: String) =
        """{"id":1,"datetime":"now","map":{"type":"FeatureCollection","features":[${features.joinToString(",")}]}}"""

    private fun source(body: String) = DeepStateFrontlineSource(HttpClient(MockEngine { respond(body) }))

    @Test
    fun onlyOccupiedPolygonsAreKept() = runTest {
        val occupied = polygon("37.5,48", "37.6,48.1", "37.7,48")
        val crimea = polygon("34,45", "34.1,45.1", "34.2,45")
        val elsewhere = polygon("30,50", "30.1,50.1", "30.2,50")
        val body = map(
            feature("Окуповано /// Occupied /// geoJSON.status.occupied\\n", occupied),
            feature("Звільнено /// Liberated /// geoJSON.status.dismissed", elsewhere),
            feature("Придністров'я/// Transnistria /// geoJSON.territories.transnistria", elsewhere),
            feature("Окупований Крим /// Occupied Crimea /// geoJSON.territories.crimea", crimea),
            feature("Unit /// geoJSON.status.occupied", """{"type":"Point","coordinates":[37.5,48.0]}"""),
        )

        val areas = source(body).fetch()

        assertEquals(
            listOf(
                listOf(GeoPoint(48.0, 37.5), GeoPoint(48.1, 37.6), GeoPoint(48.0, 37.7)),
                listOf(GeoPoint(45.0, 34.0), GeoPoint(45.1, 34.1), GeoPoint(45.0, 34.2)),
            ),
            areas,
        )
    }

    @Test
    fun pointsAreRoundedAndRunsOfEqualPointsCollapse() = runTest {
        val ring = polygon("37.50004,48.00001,0", "37.50001,48.00004,0", "37.6,48.1,0", "37.7,48,0")

        val areas = source(map(feature("geoJSON.status.occupied", ring))).fetch()

        assertEquals(listOf(listOf(GeoPoint(48.0, 37.5), GeoPoint(48.1, 37.6), GeoPoint(48.0, 37.7))), areas)
    }

    @Test
    fun aMapWithoutOccupiedPolygonsIsAFailedRead() = runTest {
        assertNull(source(map()).fetch())
        assertNull(source("<html>blocked</html>").fetch())
    }

    @Test
    fun anErrorStatusIsAFailedRead() = runTest {
        val failing = DeepStateFrontlineSource(HttpClient(MockEngine { respondError(HttpStatusCode.Forbidden) }))

        assertNull(failing.fetch())
    }
}
