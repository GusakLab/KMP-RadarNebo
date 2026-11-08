package com.gusak.lab.radarnebo.server

import com.gusak.lab.radarnebo.debug.TrafficRecorder
import com.gusak.lab.radarnebo.server.application.AlertsDataSource
import com.gusak.lab.radarnebo.server.application.SituationPoller
import com.gusak.lab.radarnebo.server.application.SituationService
import com.gusak.lab.radarnebo.server.application.UpstreamGuard
import com.gusak.lab.radarnebo.server.config.DataSourceKind
import com.gusak.lab.radarnebo.server.config.ObservabilityConfig
import com.gusak.lab.radarnebo.server.config.ServerConfig
import com.gusak.lab.radarnebo.server.config.TrackingMode
import com.gusak.lab.radarnebo.server.di.observabilityModule
import com.gusak.lab.radarnebo.server.domain.alert.LocationDirectory
import com.gusak.lab.radarnebo.server.domain.tracking.ObservationTracking
import com.gusak.lab.radarnebo.server.domain.tracking.TargetTracking
import com.gusak.lab.radarnebo.server.infrastructure.CompositeAlertsSource
import com.gusak.lab.radarnebo.server.infrastructure.observability.ServerTracing
import com.gusak.lab.radarnebo.server.infrastructure.simulation.SimulatedAlertsSource
import com.gusak.lab.radarnebo.server.infrastructure.tracking.AlertDerivedTracking
import com.gusak.lab.radarnebo.server.infrastructure.tracking.FusedTracking
import com.gusak.lab.radarnebo.server.infrastructure.upstream.AlertsInUaSource
import com.gusak.lab.radarnebo.server.infrastructure.upstream.TelegramChannelSource
import com.gusak.lab.radarnebo.server.infrastructure.upstream.UpstreamRateLimiter
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.OpenTelemetry
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.logger.SLF4JLogger
import java.util.concurrent.atomic.AtomicBoolean

/** The composition root: the object graph for one data source. */
class ServerComponents internal constructor(
    internal val config: ServerConfig,
    val situation: SituationService,
    val poller: SituationPoller,
    val clock: () -> Long,
    /** Receives the server's HTTP traffic, both ways; the server console sets it. */
    internal val traffic: TrafficRecorder? = null,
    internal val metrics: PrometheusMeterRegistry,
    private val archives: Archives,
    private val koin: KoinApplication,
    private val tracing: ServerTracing?,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    /** The alerts.in.ua budget `/health` reports; null for the simulation. Follows the source actually in use. */
    val upstreamBudgetPerMinute: Int?
        get() = config.upstream.effectivePerMinute.takeIf { poller.source.kind == DataSourceKind.AlertsInUa }

    /** Null when tracing is off, so responses carry no trace headers. */
    internal val openTelemetry: OpenTelemetry? get() = tracing?.openTelemetry

    /**
     * Releases what the components hold: the alerts.in.ua HTTP client, the archive pool, tracing and the
     * local Koin container. Safe to call more than once.
     */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        try {
            poller.source.close()
        } finally {
            runCatching { archives.close() }
            runCatching { tracing?.close() }
            runCatching { koin.close() }
        }
    }

    companion object {
        /**
         * [upstreamLimiter] guards every alerts.in.ua request; pass the same one to components built one after
         * another (a server restarted by the console) so restarts can't exceed the budget.
         */
        fun create(
            config: ServerConfig,
            clock: () -> Long = System::currentTimeMillis,
            traffic: TrafficRecorder? = null,
            upstreamLimiter: UpstreamRateLimiter = UpstreamRateLimiter(config.upstream.effectivePerMinute),
            observability: ObservabilityConfig = ObservabilityConfig(),
            // Defaults are evaluated in order: the archive opens first, so a failure there can't leak the source's
            // HttpClient.
            archives: Archives = Archives.create(config, observability.hikariMaxPool),
            source: AlertsDataSource = defaultSource(config, clock, traffic, upstreamLimiter),
            /** Further feeds fused into tracking only (see [CompositeAlertsSource]); clients are served [source]. */
            extraSources: List<AlertsDataSource> =
                config.telegramChannels.map { TelegramChannelSource(it, clock = clock) },
        ): ServerComponents {
            // One container per server. Two servers on one machine must not share a registry or a breaker.
            // Never GlobalContext.startKoin.
            val koinApp = koinApplication {
                logger(SLF4JLogger())
                modules(observabilityModule(clock))
            }
            var tracing: ServerTracing? = null
            try {
                val metrics = koinApp.koin.get<PrometheusMeterRegistry>()
                val upstream = koinApp.koin.get<UpstreamGuard>()
                tracing = observability.otelEndpoint?.let(ServerTracing::start)
                val polled = if (extraSources.isEmpty()) source else CompositeAlertsSource(source, extraSources)
                // A source with air surveillance gives real flight paths; otherwise tracks are inferred from alerts.
                val runId = (clock() / 1000).toString(36)
                val situation = SituationService(
                    tracking = tracking(config.tracking, polled, extraSources, runId),
                    alertArchive = archives.alerts,
                    trackArchive = archives.tracks,
                    locations = LocationDirectory(source.locations),
                    simulated = source.kind == DataSourceKind.Simulation,
                )
                val poller = SituationPoller(polled, situation, config.archive.retention, clock, upstream)
                return ServerComponents(
                    config, situation, poller, clock, traffic, metrics, archives, koinApp, tracing,
                )
            } catch (e: Throwable) {
                runCatching { tracing?.close() }
                runCatching { koinApp.close() }
                runCatching { archives.close() }
                throw e
            }
        }

        /** The tracking [mode] asks for; [TrackingMode.Auto] picks it from what the sources report. */
        private fun tracking(
            mode: TrackingMode,
            polled: AlertsDataSource,
            extraSources: List<AlertsDataSource>,
            runId: String,
        ): TargetTracking = when (mode) {
            TrackingMode.Auto -> when {
                extraSources.isNotEmpty() -> FusedTracking(runId)
                polled.providesObservations -> ObservationTracking()
                else -> AlertDerivedTracking(runId)
            }
            TrackingMode.Alerts -> AlertDerivedTracking(runId)
            TrackingMode.Sightings -> ObservationTracking()
            TrackingMode.Fused -> FusedTracking(runId)
        }

        private fun defaultSource(
            config: ServerConfig,
            clock: () -> Long,
            traffic: TrafficRecorder?,
            limiter: UpstreamRateLimiter,
        ): AlertsDataSource = when (config.dataSource) {
            DataSourceKind.Simulation -> SimulatedAlertsSource(config.simulation, clock)
            DataSourceKind.AlertsInUa -> AlertsInUaSource(
                config.upstream,
                AlertsInUaSource.defaultClient(config.logHttp, traffic),
                limiter,
                clock,
            )
        }
    }
}
