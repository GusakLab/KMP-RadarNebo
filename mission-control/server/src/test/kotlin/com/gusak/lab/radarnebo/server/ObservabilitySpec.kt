package com.gusak.lab.radarnebo.server

import arrow.core.Either
import com.gusak.lab.radarnebo.server.application.AlertsDataSource
import com.gusak.lab.radarnebo.server.application.FetchResult
import com.gusak.lab.radarnebo.server.application.SituationPoller
import com.gusak.lab.radarnebo.server.application.SituationService
import com.gusak.lab.radarnebo.server.config.ArchiveConfig
import com.gusak.lab.radarnebo.server.config.ObservabilityConfig
import com.gusak.lab.radarnebo.server.config.ServerConfig
import com.gusak.lab.radarnebo.server.domain.alert.LocationDirectory
import com.gusak.lab.radarnebo.server.domain.tracking.ObservationTracking
import com.gusak.lab.radarnebo.server.infrastructure.observability.Resilience4jUpstreamGuard
import com.gusak.lab.radarnebo.server.infrastructure.observability.prometheusRegistry
import com.gusak.lab.radarnebo.server.infrastructure.persistence.InMemoryAlertArchive
import com.gusak.lab.radarnebo.server.infrastructure.persistence.InMemoryTrackArchive
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class ObservabilitySpec : FunSpec({
    test("Either rejects HIKARI_MAX_POOL=0") {
        val parsed = ObservabilityConfig.fromEnv { name -> if (name == "HIKARI_MAX_POOL") "0" else null }
        val left = parsed.shouldBeInstanceOf<Either.Left<String>>()
        left.value shouldBe "HIKARI_MAX_POOL='0' is not an integer between 1 and 16"
    }

    test("metrics is 200 and does not contain a token passed as a query parameter") {
        val secret = "metrics-query-secret"
        val components = ServerComponents.create(
            ServerConfig(archive = ArchiveConfig(jdbcUrl = "memory")),
            archives = Archives.inMemory(),
        )
        // Registered before the scrape, so the filter is what keeps the secret out of the body.
        components.metrics.counter("probe", "uri", "/metrics?token=$secret", "token", secret).increment()
        try {
            testApplication {
                application { radarServer(components, startPoller = false) }
                val response = client.get("/metrics?token=$secret")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldNotContain secret
            }
        } finally {
            components.close()
        }
    }

    test("after the breaker opens, source.fetch is not called") {
        val source = mockk<AlertsDataSource>()
        every { source.pollInterval } returns 15.seconds
        every { source.history(0L) } returns emptySequence()
        coEvery { source.fetch(null) } returns FetchResult.Failed("down", minWaitMs = 1_000)
        val situation = SituationService(
            tracking = ObservationTracking(),
            alertArchive = InMemoryAlertArchive(),
            trackArchive = InMemoryTrackArchive(),
            locations = LocationDirectory(),
            simulated = false,
        )
        val poller = SituationPoller(
            source,
            situation,
            retention = 1.days,
            clock = { 0L },
            upstream = Resilience4jUpstreamGuard.create(prometheusRegistry()),
        )
        repeat(4) { poller.pollOnce() }
        coVerify(exactly = 4) { source.fetch(null) }
        poller.pollOnce()
        coVerify(exactly = 4) { source.fetch(null) }
    }
})
