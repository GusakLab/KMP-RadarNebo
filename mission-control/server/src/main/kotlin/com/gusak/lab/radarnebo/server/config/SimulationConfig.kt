package com.gusak.lab.radarnebo.server.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * The combined-strike simulation. A massive drone and missile strike starts every [strikePeriod]; front-line
 * activity (guided bombs, tactical aviation, reconnaissance drones, small drone groups) runs continuously.
 * Everything is seeded from [seed] and the time slot, so restarts see the same attacks.
 */
data class SimulationConfig(
    val seed: Long = DEFAULT_SEED,
    val strikePeriod: Duration = 6.hours,
    /** How often the simulated feed is refreshed. */
    val tick: Duration = 10.seconds,
    /** History replayed on start, so tracks and alerts already have a past on the first request. */
    val warmUp: Duration = 2.hours,
) {
    init {
        require(strikePeriod >= MIN_STRIKE_PERIOD) {
            "A combined strike takes about 5 hours; SIM_STRIKE_PERIOD_MINUTES must be at least " +
                "${MIN_STRIKE_PERIOD.inWholeMinutes}"
        }
        require(tick.isPositive()) { "SIM_TICK_SECONDS must be positive" }
    }

    internal companion object {
        const val DEFAULT_SEED: Long = 2022_02_24L
        val MIN_STRIKE_PERIOD: Duration = 3.hours
    }
}
