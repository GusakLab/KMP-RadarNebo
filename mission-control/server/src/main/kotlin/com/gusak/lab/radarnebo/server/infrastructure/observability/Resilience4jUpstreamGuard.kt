package com.gusak.lab.radarnebo.server.infrastructure.observability

import com.gusak.lab.radarnebo.server.application.UpstreamGuard
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

/**
 * Resilience4j circuit breaker for the poller. There is no retry here: the poller already backs off, and a
 * second retry would break the alerts.in.ua budget of at most 8 requests a minute.
 */
internal class Resilience4jUpstreamGuard private constructor(
    private val breaker: CircuitBreaker,
) : UpstreamGuard {
    override fun allow(): Boolean = breaker.tryAcquirePermission()

    override fun release() {
        breaker.releasePermission()
    }

    override fun recordSuccess() {
        breaker.onSuccess(0, TimeUnit.NANOSECONDS)
    }

    override fun recordFailure() {
        breaker.onError(0, TimeUnit.NANOSECONDS, UpstreamFailure)
    }

    companion object {
        /** Four failures in a row open the breaker. The next poll skips the fetch until the open window ends. */
        fun create(
            meters: PrometheusMeterRegistry,
            clock: () -> Long = System::currentTimeMillis,
        ): UpstreamGuard {
            val config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(WINDOW)
                .minimumNumberOfCalls(WINDOW)
                .failureRateThreshold(FAILURE_RATE_PERCENT)
                .waitDurationInOpenState(OPEN_FOR)
                .permittedNumberOfCallsInHalfOpenState(1)
                // The poller's clock, so a test that advances time also ends the open window.
                .clock(epochMillisClock(clock))
                .build()
            val breakers = CircuitBreakerRegistry.of(config)
            TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(breakers).bindTo(meters)
            return Resilience4jUpstreamGuard(breakers.circuitBreaker(BREAKER_NAME))
        }

        private const val BREAKER_NAME: String = "upstream"
        private const val WINDOW: Int = 4
        private const val FAILURE_RATE_PERCENT: Float = 50f
        private val OPEN_FOR: Duration = Duration.ofSeconds(30)
    }
}

private object UpstreamFailure : Exception("upstream fetch failed")

private fun epochMillisClock(clock: () -> Long): Clock = object : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = Instant.ofEpochMilli(clock())
}
