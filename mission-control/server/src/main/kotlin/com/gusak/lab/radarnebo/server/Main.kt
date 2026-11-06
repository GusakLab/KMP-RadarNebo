package com.gusak.lab.radarnebo.server

import com.gusak.lab.radarnebo.server.api.radarApi
import com.gusak.lab.radarnebo.server.application.SituationPoller
import com.gusak.lab.radarnebo.server.config.ServerConfig
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

fun main() {
    val config = ServerConfig.fromEnv()
    val server = embeddedServer(CIO, port = config.port) { radarServer(ServerComponents.create(config)) }
    SingleInstance.acquire {
        server.stop(gracePeriodMillis = 500, timeoutMillis = 3_000)
        exitProcess(0)
    }
    server.start(wait = true)
}

/**
 * Wires the server. [startPoller] is false in tests that drive [SituationPoller.pollOnce] themselves.
 * Stopping the engine closes [components].
 */
fun Application.radarServer(components: ServerComponents, startPoller: Boolean = true) {
    val config = components.config
    radarApi(
        situation = components.situation,
        poller = components.poller,
        clients = config.clients,
        upstreamBudgetPerMinute = components.upstreamBudgetPerMinute,
        clock = components.clock,
        traffic = components.traffic,
    )
    monitor.subscribe(ApplicationStopped) { components.close() }
    if (startPoller) launch { components.poller.run() }
}
