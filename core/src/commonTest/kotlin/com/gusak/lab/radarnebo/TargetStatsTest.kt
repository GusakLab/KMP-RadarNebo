package com.gusak.lab.radarnebo

import com.gusak.lab.radarnebo.data.RadarNeboApi
import com.gusak.lab.radarnebo.data.ServerEndpoint
import com.gusak.lab.radarnebo.tracking.TargetStatsResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TargetStatsTest {
    private val endpoint = ServerEndpoint(RadarNeboApi.DEFAULT_SERVER_URL, "secret")

    private fun api(status: HttpStatusCode, body: String) = RadarNeboApi(
        RadarNeboApi.client(
            MockEngine { request ->
                assertEquals(RadarNeboApi.TARGET_STATS_PATH, request.url.encodedPath)
                assertEquals("Bearer secret", request.headers[HttpHeaders.Authorization])
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )

    @Test
    fun statsAreReadFromTheServer() = runTest {
        val body = """{"serverTime":5,"today":3,"yesterday":12,"thisMonth":240,"simulated":false}"""

        val stats = api(HttpStatusCode.OK, body).targetStats(endpoint)

        assertEquals(TargetStatsResponse(serverTime = 5, today = 3, yesterday = 12, thisMonth = 240), stats)
    }

    @Test
    fun aServerWithoutStatsGivesNull() = runTest {
        assertNull(api(HttpStatusCode.NotFound, """{"message":"Not found"}""").targetStats(endpoint))
    }
}
