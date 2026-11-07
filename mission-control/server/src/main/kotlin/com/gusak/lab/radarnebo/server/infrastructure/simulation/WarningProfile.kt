package com.gusak.lab.radarnebo.server.infrastructure.simulation

/**
 * How Ukraine's air raid warning system alerts settlements for one kind of target. Settlements within
 * [redRadiusKm] of where the target is, or will be within [redLeadMs], get a red alert; settlements within
 * [corridorKm] of where it will be within [yellowLeadMs] get a yellow (preliminary) one.
 */
internal class WarningProfile(
    val redRadiusKm: Double,
    val redLeadMs: Long,
    val yellowLeadMs: Long,
    val corridorKm: Double,
) {
    companion object {
        val DRONE = WarningProfile(
            redRadiusKm = 22.0,
            redLeadMs = 8 * MINUTE_MS,
            yellowLeadMs = 30 * MINUTE_MS,
            corridorKm = 15.0,
        )
        val JET_DRONE = WarningProfile(
            redRadiusKm = 25.0,
            redLeadMs = 4 * MINUTE_MS,
            yellowLeadMs = 10 * MINUTE_MS,
            corridorKm = 20.0,
        )
        val RECON_DRONE = WarningProfile(
            redRadiusKm = 18.0,
            redLeadMs = 4 * MINUTE_MS,
            yellowLeadMs = 0,
            corridorKm = 0.0,
        )
        val CRUISE_MISSILE = WarningProfile(
            redRadiusKm = 30.0,
            redLeadMs = 4 * MINUTE_MS,
            yellowLeadMs = 12 * MINUTE_MS,
            corridorKm = 25.0,
        )

        /** Ballistic missiles and guided bombs are alerted by region or target, not along the way. */
        val NONE = WarningProfile(redRadiusKm = 0.0, redLeadMs = 0, yellowLeadMs = 0, corridorKm = 0.0)
    }
}
