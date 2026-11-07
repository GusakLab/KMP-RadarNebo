package com.gusak.lab.radarnebo.server.api

import com.gusak.lab.radarnebo.data.ActiveAlertsResponse
import com.gusak.lab.radarnebo.data.RadarNeboApi
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.geo.Regions
import com.gusak.lab.radarnebo.server.Archives
import com.gusak.lab.radarnebo.server.ServerComponents
import com.gusak.lab.radarnebo.server.config.ClientApiConfig
import com.gusak.lab.radarnebo.server.config.DataSourceKind
import com.gusak.lab.radarnebo.server.config.ServerConfig
import com.gusak.lab.radarnebo.server.config.SimulationConfig
import com.gusak.lab.radarnebo.server.infrastructure.simulation.SimulatedAlertsSource
import com.gusak.lab.radarnebo.server.domain.tracking.TrackQuery
import com.gusak.lab.radarnebo.server.radarServer
import com.gusak.lab.radarnebo.tracking.DroneTracksResponse
import com.gusak.lab.radarnebo.tracking.TrackOutcome
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.KSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The server on its simulation, as the app sees it: the same API as with alerts.in.ua behind it. */
class SimulatedServerTest {
    /** What clients saw of the feed: every threat type alerted, every type tracked, the most targets at once. */
    private data class FeedSeen(
        val threatTypes: Set<ThreatType>,
        val trackTypes: Set<ThreatType>,
        val mostActiveTracks: Int,
    )

    @Test
    fun theSimulationIsServedThroughTheSameApiWithTracksAndArchive() = testApplication {
        val simulation = SimulationConfig()
        val period = simulation.strikePeriod.inWholeMilliseconds
        // Start an hour before a strike, after the warm-up replay of the two hours before that.
        var now = 1_800_000_000_000L / period * period + period - 3_600_000L
        val config = ServerConfig(
            clients = ClientApiConfig(requestsPerMinute = 100_000, historyRequestsPerMinute = 100_000),
            simulation = simulation,
        )
        val source = SimulatedAlertsSource(simulation, clock = { now })
        val server = ServerComponents.create(config, clock = { now }, source = source, archives = Archives.inMemory())
        application { radarServer(server, startPoller = false) }

        // Six hours of the feed, polled every minute, as clients see it.
        val feed = watchFeed(server, minutes = 6 * 60) { now += 60_000L }
        assertEquals(ThreatType.entries.toSet() - ThreatType.Unknown, feed.threatTypes)
        assertEquals(
            setOf(ThreatType.Drones, ThreatType.CruiseMissiles, ThreatType.BallisticMissiles, ThreatType.GuidedBombs),
            feed.trackTypes,
        )
        assertTrue(
            feed.mostActiveTracks >= 20,
            "at the height of the strike ${feed.mostActiveTracks} targets are tracked",
        )

        assertFinishedFlightsAndAlertsAreArchived()
        assertTheServerSaysItIsSimulated()
    }

    /** Polls once a minute for [minutes], reading alerts and tracks as clients do; [nextMinute] moves the clock. */
    private suspend fun ApplicationTestBuilder.watchFeed(
        server: ServerComponents,
        minutes: Int,
        nextMinute: () -> Unit,
    ): FeedSeen {
        val threatTypes = mutableSetOf<ThreatType>()
        val trackTypes = mutableSetOf<ThreatType>()
        var mostTracks = 0
        repeat(minutes) {
            server.poller.pollOnce()
            val alerts = read(ActiveAlertsResponse.serializer(), RadarNeboApi.ACTIVE_ALERTS_PATH)
            alerts.alerts.flatMap { it.threats }.mapTo(threatTypes) { ThreatType.from(it.threatType) }
            val tracks = read(DroneTracksResponse.serializer(), RadarNeboApi.TRACKS_PATH)
            assertTrue(tracks.simulated, "clients are told the data is simulated")
            tracks.tracks.mapTo(trackTypes) { it.type }
            mostTracks = maxOf(mostTracks, tracks.tracks.count { it.active })
            nextMinute()
        }
        return FeedSeen(threatTypes, trackTypes, mostTracks)
    }

    private suspend fun ApplicationTestBuilder.assertFinishedFlightsAndAlertsAreArchived() {
        // Finished flights are in the archive, with how they ended.
        val archive = read(
            TrackHistoryResponse.serializer(),
            "${RadarNeboApi.TRACK_ARCHIVE_PATH}?limit=${TrackQuery.MAX_LIMIT}",
        )
        assertTrue(archive.simulated)
        val outcomes = archive.tracks.filter { !it.active }.mapNotNull { it.outcome }.toSet()
        assertEquals(TrackOutcome.entries.toSet(), outcomes)
        // And so are finished alerts: Kyiv city is alerted during every strike.
        val kyiv = read(
            ActiveAlertsResponse.serializer(),
            "/v1/regions/${Regions.KYIV_CITY_UID}/alerts/day_ago.json",
        )
        assertTrue(kyiv.alerts.any { it.finishedAt != null }, "Kyiv's finished alerts are archived")
    }

    private suspend fun ApplicationTestBuilder.assertTheServerSaysItIsSimulated() {
        val health = read(Health.serializer(), "/health")
        assertTrue(health.simulated)
        assertEquals(0, health.upstreamRequestsTotal)
        assertEquals(DataSourceKind.Simulation.key, health.source)
        assertEquals(null, health.upstreamBudgetPerMinute, "the simulation has no upstream budget")

        val page = client.get("/").bodyAsText()
        assertTrue(
            "RadarNebo server" in page && "Simulation (not the real situation)" in page,
            "a status page for browsers",
        )
    }

    private suspend fun <T> ApplicationTestBuilder.read(serializer: KSerializer<T>, path: String): T =
        RadarNeboApi.json.decodeFromString(serializer, client.get(path).bodyAsText())
}
