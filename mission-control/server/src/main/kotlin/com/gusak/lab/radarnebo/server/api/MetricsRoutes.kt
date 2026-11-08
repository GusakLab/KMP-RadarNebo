package com.gusak.lab.radarnebo.server.api

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/** Prometheus text. Same rate limit as `/health`, and no client key. */
internal fun Route.metricsRoute(registry: PrometheusMeterRegistry) {
    get("/metrics") {
        call.respondText(registry.scrape(), PROMETHEUS_TEXT)
    }
}

private val PROMETHEUS_TEXT: ContentType = ContentType.parse("text/plain; version=0.0.4; charset=utf-8")
