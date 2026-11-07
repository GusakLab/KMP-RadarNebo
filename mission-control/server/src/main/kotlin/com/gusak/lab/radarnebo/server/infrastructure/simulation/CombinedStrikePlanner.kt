package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.geo.GeoMath
import kotlin.random.Random

/**
 * A massive combined strike as Russia flies them, compressed to a few hours. Minutes from the start:
 *
 * - 0-120: waves of 22-36 Shahed groups, mixed with Gerbera decoys and a few jet Shaheds, from launch sites in
 *   Russia and occupied Crimea and Donetsk. They take 2-5 hours to their objectives, doglegging on the way.
 * - 110-170: Tu-95MS bombers reach their launch lines (strategic aviation alert over the whole country until
 *   the missiles reach Ukraine) and launch 10-30 Kh-101 cruise missiles, which enter through the north, east
 *   or south-east and wind to their objectives. Ships in the Black Sea launch Kalibrs through the southern
 *   coast.
 * - 165: a MiG-31K takes off (nationwide alert); it usually launches Kinzhals 15-20 minutes later.
 * - 200-215: Iskander-M and KN-23 ballistic missiles at the objectives within their range.
 *
 * Every flight's fate is decided when it is planned, with real-world interception rates (most Shaheds
 * and cruise missiles are shot down, ballistic missiles much less often).
 */
internal class CombinedStrikePlanner(private val seed: Long) {

    fun plan(slot: Long, startsAt: Long): AttackPlan = Strike(slot, startsAt, Random(seed * 1_000_003L + slot)).plan()

    /**
     * One strike being planned. Its parts are planned one after another, each drawing from [random] in a fixed
     * order, so a plan is a pure function of the seed and the slot.
     */
    private class Strike(private val slot: Long, private val startsAt: Long, private val random: Random) {
        private val factory = FlightFactory(random)
        private val objectives = List(3 + random.nextInt(3)) {
            StrikeGeography.OBJECTIVES.weighted(random) { it.weight }
        }.distinct()

        fun plan(): AttackPlan {
            val drones = droneWaves()
            val kh101 = kh101Salvo()
            val kalibr = kalibrSalvo()
            val kinzhals = kinzhalSalvo()
            val ballistic = ballisticMissiles()
            return AttackPlan(
                flights = drones + kh101.flights + kalibr.flights + kinzhals.flights + ballistic,
                areas = listOf(kh101.area, kalibr.area, kinzhals.area),
            )
        }

        private fun id(kind: String, n: Int): String = "S$slot-$kind${(n + 1).toString().padStart(2, '0')}"

        /** Drone waves: Shahed groups with Gerbera decoys among them, and a few jet Shaheds late in the wave. */
        private fun droneWaves(): List<Flight> {
            val droneGroups = List(22 + random.nextInt(15)) {
                if (random.nextDouble() < 0.22) Munition.Gerbera else Munition.Shahed
            } + List(1 + random.nextInt(3)) { Munition.JetShahed }
            val flights = mutableListOf<Flight>()
            droneGroups.forEachIndexed { i, munition ->
                val objective = if (random.nextDouble() < 0.8) {
                    objectives.random(random)
                } else {
                    StrikeGeography.OBJECTIVES.weighted(random) { it.weight }
                }
                val target = factory.jitter(StrikeGeography.point(objective.settlement), 6.0)
                val site = nearestSites(StrikeGeography.DRONE_SITES, target, maxKm = 900.0, n = 3).random(random)
                val jet = munition == Munition.JetShahed
                flights += factory.flight(
                    id = id("D", i),
                    munition = munition,
                    waypoints = factory.droneRouteFrom(site, target),
                    departAt = startsAt.plusMinutes(
                        if (jet) random.nextDouble(90.0, 130.0) else random.nextDouble(0.0, 120.0),
                    ),
                    origin = site,
                    target = objective.settlement,
                    count = if (jet) 1 + random.nextInt(2) else 1 + random.nextInt(5),
                )
            }
            return flights
        }

        /** Kh-101 from Tu-95MS bombers. */
        private fun kh101Salvo(): Salvo {
            val bomberLine = StrikeGeography.BOMBER_LAUNCH_LINES.random(random)
            val kh101Start = startsAt.plusMinutes(130.0 + random.nextDouble(0.0, 5.0))
            val flights = mutableListOf<Flight>()
            var launchAt = kh101Start
            repeat(10 + random.nextInt(21)) { i ->
                val objective = objectives.random(random)
                val target = factory.jitter(StrikeGeography.point(objective.settlement), 4.0)
                flights += factory.flight(
                    id = id("KH", i),
                    munition = Munition.Kh101,
                    waypoints = factory.cruiseRoute(
                        factory.jitter(bomberLine.point, 40.0),
                        factory.gate(StrikeGeography.CRUISE_GATES, bomberLine.point, target, 10.0),
                        target,
                    ),
                    departAt = launchAt,
                    origin = bomberLine,
                    target = objective.settlement,
                )
                launchAt = launchAt.plusMinutes(random.nextDouble(0.2, 0.9))
            }
            // The whole country stays alerted from the bombers' approach until the missiles cross into Ukraine.
            val kh101Seen = flights.maxOf { it.detectedAt }
            val threat = AreaThreat(
                type = ThreatType.StrategicAviation,
                oblastUids = SimulatedLocations.FREE_REGION_UIDS,
                from = kh101Start - 20 * MINUTE_MS,
                until = maxOf(launchAt, kh101Seen) + 10 * MINUTE_MS,
                message = "Активність стратегічної авіації: Ту-95МС на рубежах пуску крилатих ракет Х-101",
            )
            return Salvo(flights, threat)
        }

        /** Kalibr from the Black Sea. */
        private fun kalibrSalvo(): Salvo {
            val carrier = StrikeGeography.KALIBR_CARRIERS.random(random)
            val kalibrStart = startsAt.plusMinutes(135.0 + random.nextDouble(0.0, 10.0))
            val flights = mutableListOf<Flight>()
            var launchAt = kalibrStart
            repeat(3 + random.nextInt(8)) { i ->
                val objective = objectives.random(random)
                val target = factory.jitter(StrikeGeography.point(objective.settlement), 4.0)
                flights += factory.flight(
                    id = id("KL", i),
                    munition = Munition.Kalibr,
                    waypoints = factory.cruiseRoute(
                        factory.jitter(carrier.point, 25.0),
                        factory.gate(StrikeGeography.KALIBR_GATES, carrier.point, target, 10.0),
                        target,
                    ),
                    departAt = launchAt,
                    origin = carrier,
                    target = objective.settlement,
                )
                launchAt = launchAt.plusMinutes(random.nextDouble(0.3, 1.2))
            }
            val threat = AreaThreat(
                type = ThreatType.UnspecifiedMissiles,
                oblastUids = SimulatedLocations.BLACK_SEA_COAST_UIDS,
                from = kalibrStart - 25 * MINUTE_MS,
                until = launchAt + 3 * MINUTE_MS,
                message = "Загроза ракетного удару: у Чорному морі носії крилатих ракет «Калібр»",
                level = ThreatLevel.Yellow,
            )
            return Salvo(flights, threat)
        }

        /** MiG-31K take-off; most of the time it launches Kinzhals, sometimes it is only a threat. */
        private fun kinzhalSalvo(): Salvo {
            val takeOff = startsAt.plusMinutes(165.0 + random.nextDouble(0.0, 5.0))
            val kinzhals = if (random.nextDouble() < 0.7) 1 + random.nextInt(4) else 0
            val kinzhalLaunch = takeOff.plusMinutes(random.nextDouble(15.0, 22.0))
            val flights = mutableListOf<Flight>()
            repeat(kinzhals) { i ->
                val targetName = KINZHAL_TARGETS.random(random)
                val target = factory.jitter(StrikeGeography.point(targetName), 3.0)
                val area = nearestSites(StrikeGeography.KINZHAL_LAUNCH_AREAS, target, maxKm = Double.MAX_VALUE, n = 1)
                    .single()
                flights += factory.flight(
                    id = id("KZ", i),
                    munition = Munition.Kinzhal,
                    waypoints = listOf(factory.jitter(area.point, 30.0), target),
                    departAt = kinzhalLaunch.plusMinutes(i * random.nextDouble(0.1, 0.5)),
                    origin = area,
                    target = targetName,
                )
            }
            val kinzhalsDown = flights.maxOfOrNull { it.endAt } ?: takeOff.plusMinutes(40.0)
            val threat = AreaThreat(
                type = ThreatType.Mig31k,
                oblastUids = SimulatedLocations.FREE_REGION_UIDS,
                from = takeOff,
                until = kinzhalsDown + 2 * MINUTE_MS,
                message = "Зліт МіГ-31К — загроза застосування ракет «Кинджал» по всій території України",
            )
            return Salvo(flights, threat)
        }

        /** Ballistic missiles at the objectives in range, or at the cities near the front. */
        private fun ballisticMissiles(): List<Flight> {
            val flights = mutableListOf<Flight>()
            repeat(3 + random.nextInt(7)) { i ->
                val munition = if (random.nextDouble() < 0.2) Munition.Kn23 else Munition.IskanderM
                val reachKm = if (munition == Munition.Kn23) 700.0 else 500.0
                val candidates = (objectives.map { it.settlement } + BALLISTIC_FALLBACK_TARGETS).filter { name ->
                    StrikeGeography.BALLISTIC_SITES.any {
                        GeoMath.distanceKm(it.point, StrikeGeography.point(name)) <= reachKm
                    }
                }
                val targetName = candidates.random(random)
                val target = factory.jitter(StrikeGeography.point(targetName), 3.0)
                val site = nearestSites(StrikeGeography.BALLISTIC_SITES, target, reachKm, n = 2).random(random)
                flights += factory.flight(
                    id = id("BM", i),
                    munition = munition,
                    waypoints = listOf(factory.jitter(site.point, 20.0), target),
                    departAt = startsAt.plusMinutes(200.0 + random.nextDouble(0.0, 15.0)),
                    origin = site,
                    target = targetName,
                )
            }
            return flights
        }
    }

    /** Missiles launched together, and the regional threat they raise. */
    private class Salvo(val flights: List<Flight>, val area: AreaThreat)

    private companion object {
        val BALLISTIC_FALLBACK_TARGETS = listOf("Kharkiv", "Dnipro", "Odesa", "Zaporizhzhia", "Sumy", "Poltava", "Kyiv")
        val KINZHAL_TARGETS = listOf("Kyiv", "Kyiv", "Kyiv", "Starokostiantyniv", "Lviv", "Ivano-Frankivsk", "Odesa")
    }
}
