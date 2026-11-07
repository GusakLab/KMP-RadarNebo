package com.gusak.lab.radarnebo.server.api

import com.gusak.lab.radarnebo.server.application.SituationService
import com.gusak.lab.radarnebo.server.domain.alert.AlertBoard
import io.ktor.http.HttpStatusCode

/** Thrown until the first report from the data source has been published; answered with a `Retry-After`. */
internal class WarmingUpException :
    ApiException(HttpStatusCode.ServiceUnavailable, "The server is warming up, no data yet")

/** The board, or a 503 while the server has no data yet. */
internal fun AlertBoard?.orWarmingUp(): AlertBoard = this ?: throw WarmingUpException()

/** Answers 503 while the server has no data yet. */
internal fun SituationService.requireData() {
    board.orWarmingUp()
}
