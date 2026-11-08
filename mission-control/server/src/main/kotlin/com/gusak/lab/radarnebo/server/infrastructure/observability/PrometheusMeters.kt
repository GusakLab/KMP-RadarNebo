package com.gusak.lab.radarnebo.server.infrastructure.observability

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/** Prometheus registry shared by request timers and the upstream circuit breaker. */
internal fun prometheusRegistry(): PrometheusMeterRegistry {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    // Before any meter, including the breaker. A filter added later makes Micrometer warn.
    registry.config().meterFilter(RedactingMeterFilter)
    return registry
}

internal fun Application.installPrometheusMetrics(registry: PrometheusMeterRegistry) {
    install(MicrometerMetrics) {
        this.registry = registry
        // The breaker is already registered. The plugin's own filter would warn and is not required.
        registerDistributionStatisticConfig = false
    }
}

/**
 * Timers must not keep a query string: a scrape of `/metrics?token=secret` would otherwise publish the secret.
 * Tags named token, authorization or key are dropped outright.
 */
private object RedactingMeterFilter : MeterFilter {
    override fun map(id: Meter.Id): Meter.Id {
        val tags = id.tags.mapNotNull { tag ->
            if (tag.key.lowercase() in DENIED_KEYS) return@mapNotNull null
            val query = tag.value.indexOf('?')
            if (query < 0) tag else Tag.of(tag.key, tag.value.substring(0, query))
        }
        return id.replaceTags(tags)
    }

    private val DENIED_KEYS: Set<String> = setOf("token", "authorization", "key")
}
