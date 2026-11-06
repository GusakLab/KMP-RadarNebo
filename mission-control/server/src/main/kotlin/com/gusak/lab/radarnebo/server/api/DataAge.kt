package com.gusak.lab.radarnebo.server.api

import com.gusak.lab.radarnebo.server.domain.alert.AlertBoard
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header

/** Tells the client how old [board]'s data is: seconds since the data source last confirmed it. */
internal fun ApplicationCall.dataAge(board: AlertBoard, nowMs: Long) {
    val ageSeconds = (nowMs - board.confirmedAt.toEpochMilliseconds()) / 1000
    response.header(DATA_AGE_HEADER, ageSeconds.coerceAtLeast(0).toString())
}

/** Seconds since the data source last confirmed the data. */
internal const val DATA_AGE_HEADER: String = "X-Data-Age-Seconds"
