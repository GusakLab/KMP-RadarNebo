package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType

/**
 * How a tracked threat class moves, which sets how its alerts are linked into tracks and how far its
 * position is projected past the last report. Classes without kinematics (ballistic missiles, bombs,
 * aviation, which only come as area alerts) are shown as markers and not tracked from alerts.
 */
data class TrackKinematics(
    /** Faster apparent speeds are artefacts of coarse alert timing. */
    val maxSpeedKmh: Double,
    /** Furthest a new alert can be from a track's last position and still continue it. */
    val maxHopKm: Double,
    /** How long a track that lost its alerts can still be picked up again. */
    val reacquireWindowMs: Long,
    /** Dead reckoning past the newest sample stops after this long, to avoid inventing positions. */
    internal val maxExtrapolationMs: Long,
    /**
     * With no new report for this long the target is presumed gone (passed, shot down or landed), even while
     * the alert it raised stays open: a target still flying keeps raising alerts as it crosses areas.
     */
    internal val staleAfterMs: Long = maxOf(reacquireWindowMs, maxExtrapolationMs),
    /** Slowest believable cruise; alert timing makes groups look slower than they fly. */
    val minSpeedKmh: Double = 0.0,
    /** How fast the course can change, in degrees per hour. */
    internal val maxTurnDegPerHour: Double = 3_600.0,
    /** Process noise of the [TrackFilter] (km²/h³): how much the velocity may wander between reports. */
    internal val processNoise: Double = 2e4,
    /** How long before the target a yellow (preliminary) alert is typically raised for an area on its way. */
    internal val warningLeadMs: Long = 0L,
    /** How long before the target a red alert is typically raised: areas a few minutes ahead go red too. */
    internal val redLeadMs: Long = 0L,
    /** How far ahead the server forecasts the course. */
    internal val forecastHorizonMs: Long = maxExtrapolationMs,
    /** Spacing of the smoothed path's points. */
    internal val pathStepKm: Double = 2.0,
    /** Furthest it flies; a track that would grow longer is another target. */
    val rangeKm: Double = Double.POSITIVE_INFINITY,
    /** Circles and doubles back (decoys), so a reversed course does not mean another target. */
    val loiters: Boolean = false,
) {
    /** How far ahead of the target an alert of [level] is raised; nothing for reports that are not alerts. */
    internal fun leadMs(level: ThreatLevel?): Long = when (level) {
        ThreatLevel.Yellow -> warningLeadMs
        ThreatLevel.Red -> redLeadMs
        null -> 0L
    }

    companion object {
        /**
         * Shahed-type drones cruise at 150-185 km/h; reports come as a group crosses raions, 10-30 min apart.
         * Replaying the alerts.in.ua archive, a 45 min re-acquire window links the most reports while courses stay
         * coherent; at 60 min unrelated groups start being chained into tracks that turn back on themselves.
         */
        val Drones: TrackKinematics = TrackKinematics(
            220.0, 120.0, 45 * 60_000L, 30 * 60_000L, staleAfterMs = 60 * 60_000L,
            minSpeedKmh = 120.0, maxTurnDegPerHour = 360.0, processNoise = 5e3, warningLeadMs = 20 * 60_000L,
            redLeadMs = 4 * 60_000L, rangeKm = 2_500.0,
        )

        /** Gerbera decoys are slower and wander more: they are flown to draw air defence, not to a target. */
        internal val DecoyDrones: TrackKinematics = Drones.copy(
            maxSpeedKmh = 200.0, minSpeedKmh = 100.0, maxTurnDegPerHour = 540.0, processNoise = 4e4, rangeKm = 1_000.0,
            loiters = true,
        )

        /** Jet Shaheds (Geran-3) fly at 350-550 km/h; they come as drone alerts, recognised by the alert text. */
        internal val JetDrones: TrackKinematics = TrackKinematics(
            600.0, 120.0, 15 * 60_000L, 10 * 60_000L, staleAfterMs = 20 * 60_000L,
            minSpeedKmh = 350.0, maxTurnDegPerHour = 540.0, processNoise = 1e5, warningLeadMs = 7 * 60_000L,
            redLeadMs = 2 * 60_000L, pathStepKm = 5.0, rangeKm = 2_500.0,
        )

        /** Cruise missiles fly at 700-950 km/h and cross an oblast in minutes, so reports are further apart. */
        internal val CruiseMissiles: TrackKinematics = TrackKinematics(
            1_000.0, 300.0, 10 * 60_000L, 5 * 60_000L, staleAfterMs = 15 * 60_000L,
            minSpeedKmh = 650.0, maxTurnDegPerHour = 1_080.0, processNoise = 5e5, warningLeadMs = 8 * 60_000L,
            redLeadMs = 2 * 60_000L, pathStepKm = 10.0, rangeKm = 4_000.0,
        )

        /**
         * Guided bombs (KAB with UMPK kits) glide 40-70 km in a few minutes. Alerts are raised for the target,
         * not along the way, so they are never linked from alerts; a source that knows their flight path
         * (the proxy's simulation) publishes their tracks directly.
         */
        internal val GuidedBombs: TrackKinematics = TrackKinematics(900.0, 0.0, 0L, 60_000L, processNoise = 1e6)

        /**
         * Ballistic and aeroballistic missiles (Iskander-M, KN-23, Kinzhal) cover hundreds of km in minutes.
         * Alerts are raised for whole oblasts, so they are never linked from alerts; only a source that sees
         * the launch publishes their tracks.
         */
        internal val BallisticMissiles: TrackKinematics =
            TrackKinematics(12_000.0, 0.0, 0L, 30_000L, processNoise = 1e7, pathStepKm = 20.0)

        internal fun of(type: ThreatType, kind: TargetKind): TrackKinematics? =
            if (type == ThreatType.Drones) ofDrone(kind) else of(type)

        internal fun of(type: ThreatType): TrackKinematics? = when (type) {
            ThreatType.Drones -> Drones
            ThreatType.CruiseMissiles, ThreatType.UnspecifiedMissiles -> CruiseMissiles
            else -> null
        }

        fun of(track: DroneTrack): TrackKinematics = when {
            track.type == ThreatType.Drones -> ofDrone(track.kind ?: TargetClassifier.classify(track).kind)
            track.type == ThreatType.GuidedBombs -> GuidedBombs
            track.type == ThreatType.BallisticMissiles -> BallisticMissiles
            else -> of(track.type) ?: Drones
        }

        /** Drones move by their kind: jets and decoys have their own; every other drone flies like a Shahed. */
        private fun ofDrone(kind: TargetKind): TrackKinematics = when (kind) {
            TargetKind.JetDrone -> JetDrones
            TargetKind.DecoyDrone -> DecoyDrones
            else -> Drones
        }
    }
}
