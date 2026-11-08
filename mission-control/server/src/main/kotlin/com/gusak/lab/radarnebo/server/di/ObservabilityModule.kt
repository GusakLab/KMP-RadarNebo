package com.gusak.lab.radarnebo.server.di

import com.gusak.lab.radarnebo.server.application.UpstreamGuard
import com.gusak.lab.radarnebo.server.infrastructure.observability.Resilience4jUpstreamGuard
import com.gusak.lab.radarnebo.server.infrastructure.observability.prometheusRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.core.module.Module
import org.koin.dsl.module

/** Registry and upstream guard for one [org.koin.core.KoinApplication]. Not a process-wide module. */
internal fun observabilityModule(clock: () -> Long): Module = module {
    single { prometheusRegistry() }
    single {
        val meters = get<PrometheusMeterRegistry>()
        Resilience4jUpstreamGuard.create(meters, clock)
    }
}
