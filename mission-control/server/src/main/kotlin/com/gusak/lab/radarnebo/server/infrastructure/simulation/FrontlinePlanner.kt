package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.geo.GeoMath
import kotlin.random.Random

/**
 * Front-line activity that never stops, planned per [slotMs] slot: guided bombs from Su-34s on front-line
 * and border towns (with tactical aviation alerts in their oblasts while the bombers work), Orlan
 * reconnaissance drones over the towns behind the front, small Shahed groups at cities near the border,
 * and the occasional ballistic missile.
 */
internal class FrontlinePlanner(private val seed: Long, val slotMs: Long = 10 * MINUTE_MS) {

    fun plan(slot: Long): AttackPlan = Slot(slot, slotMs, Random(seed * 7_368_787L + slot)).plan()

    /**
     * One slot being planned. Its parts are planned one after another, each drawing from [random] in a fixed
     * order, so a plan is a pure function of the seed and the slot.
     */
    private class Slot(private val slot: Long, slotMs: Long, private val random: Random) {
        private val factory = FlightFactory(random)
        private val startsAt = slot * slotMs
        private val slotMinutes = slotMs / MINUTE_MS.toDouble()
        private var n = 0

        fun plan(): AttackPlan {
            val bombs = guidedBombs()
            val areas = tacticalAviation(bombs)
            val flights = bombs + listOfNotNull(reconDrone(), droneGroup(), ballisticMissile())
            return AttackPlan(flights, areas)
        }

        private fun id(kind: String): String = "F$slot-$kind${++n}"

        private fun guidedBombs(): List<Flight> = bombReleases().map { (target, releaseAt) -> kab(target, releaseAt) }

        /** Guided bombs: often a salvo on one town, plus single releases elsewhere. */
        private fun bombReleases(): List<BombRelease> {
            val bombs = mutableListOf<BombRelease>()
            if (random.nextDouble() < 0.6) {
                val target = StrikeGeography.KAB_TARGETS.weighted(random) { it.weight }
                var releaseAt = startsAt.plusMinutes(random.nextDouble(0.0, slotMinutes * 0.8))
                repeat(2 + random.nextInt(3)) {
                    bombs += BombRelease(target, releaseAt)
                    releaseAt += random.nextLong(8_000, 30_000)
                }
            }
            repeat(random.nextInt(3)) {
                bombs += BombRelease(
                    StrikeGeography.KAB_TARGETS.weighted(random) { it.weight },
                    startsAt.plusMinutes(random.nextDouble(0.0, slotMinutes)),
                )
            }
            return bombs
        }

        private fun kab(target: KabTarget, releaseAt: Long): Flight {
            val town = StrikeGeography.point(target.settlement)
            val release = GeoMath.destination(
                town,
                random.nextInt(target.bearingDeg.first, target.bearingDeg.last + 1).toDouble(),
                random.nextInt(target.rangeKm.first, target.rangeKm.last + 1).toDouble(),
            )
            return factory.flight(
                id = id("KAB"),
                munition = Munition.Kab,
                waypoints = listOf(release, factory.jitter(town, 2.5)),
                departAt = releaseAt,
                origin = target.launchArea,
                target = target.settlement,
            )
        }

        /** Tactical aviation alerts in the oblasts the bombers work over, while they do. */
        private fun tacticalAviation(bombs: List<Flight>): List<AreaThreat> =
            bombs.groupBy { it.targetOblastUid }.map { (oblast, oblastBombs) ->
                AreaThreat(
                    type = ThreatType.TacticalAviation,
                    oblastUids = listOf(oblast),
                    from = oblastBombs.minOf { it.departAt } - 5 * MINUTE_MS,
                    until = oblastBombs.maxOf { it.departAt } + 2 * MINUTE_MS,
                    message = "Активність тактичної авіації (Су-34/Су-35), пуски КАБ",
                )
            }

        /** A reconnaissance drone over the towns behind a stretch of the front, and back. */
        private fun reconDrone(): Flight? {
            if (random.nextDouble() >= 0.3) return null
            val sector = StrikeGeography.RECON_SECTORS.random(random)
            val stops = sector.settlements.shuffled(random)
                .take(1 + random.nextInt(2))
                .map { factory.jitter(StrikeGeography.point(it), 6.0) }
            return factory.flight(
                id = id("RC"),
                munition = Munition.Orlan,
                waypoints = listOf(sector.base) + stops + factory.jitter(sector.base, 10.0),
                departAt = startsAt.plusMinutes(random.nextDouble(0.0, slotMinutes)),
                origin = null,
                target = sector.settlements.first(),
                level = ThreatLevel.Yellow,
            )
        }

        /** A small Shahed group at a city near the border or the front. */
        private fun droneGroup(): Flight? {
            if (random.nextDouble() >= 0.25) return null
            val targetName = StrikeGeography.FRONTLINE_DRONE_TARGETS.random(random)
            val target = factory.jitter(StrikeGeography.point(targetName), 5.0)
            val site = nearestSites(StrikeGeography.DRONE_SITES, target, maxKm = 600.0, n = 2).random(random)
            return factory.flight(
                id = id("D"),
                munition = if (random.nextDouble() < 0.2) Munition.Gerbera else Munition.Shahed,
                waypoints = factory.droneRouteFrom(site, target),
                departAt = startsAt.plusMinutes(random.nextDouble(0.0, slotMinutes)),
                origin = site,
                target = targetName,
                count = 1 + random.nextInt(3),
            )
        }

        /** Now and then a ballistic missile at a city near the border. */
        private fun ballisticMissile(): Flight? {
            if (random.nextDouble() >= 0.05) return null
            val targetName = BALLISTIC_TARGETS.random(random)
            val target = factory.jitter(StrikeGeography.point(targetName), 3.0)
            val site = nearestSites(StrikeGeography.BALLISTIC_SITES, target, maxKm = 500.0, n = 1).single()
            return factory.flight(
                id = id("BM"),
                munition = Munition.IskanderM,
                waypoints = listOf(factory.jitter(site.point, 20.0), target),
                departAt = startsAt.plusMinutes(random.nextDouble(0.0, slotMinutes)),
                origin = site,
                target = targetName,
            )
        }
    }

    /** A guided bomb aimed at [target], released at [releaseAt]. */
    private data class BombRelease(val target: KabTarget, val releaseAt: Long)

    private companion object {
        val BALLISTIC_TARGETS = listOf(
            "Kharkiv", "Sumy", "Dnipro", "Odesa", "Zaporizhzhia", "Kramatorsk", "Mykolaiv", "Poltava",
        )
    }
}
