package com.gusak.lab.radarnebo.server.api

import com.gusak.lab.radarnebo.data.FrontlineArea
import com.gusak.lab.radarnebo.data.FrontlineResponse
import com.gusak.lab.radarnebo.data.RadarNeboApi
import com.gusak.lab.radarnebo.server.application.FrontlineService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** The occupied territory, whose edge is the frontline. Real data, also when the alerts are simulated. */
internal fun Route.frontlineRoutes(frontline: FrontlineService) {
    /** Every occupied area's outline; 503 until the server has read its source once. */
    get(RadarNeboApi.FRONTLINE_PATH) {
        val current = frontline.current()
            ?: throw ApiException(HttpStatusCode.ServiceUnavailable, "No frontline data yet")
        val areas = current.areas.map { ring -> FrontlineArea(ring.flatMap { listOf(it.lon, it.lat) }) }
        call.respond(FrontlineResponse(current.fetchedAtMs, areas))
    }
}
