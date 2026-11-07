package com.gusak.lab.radarnebo.server.domain.tracking

import com.gusak.lab.radarnebo.server.domain.alert.AirAlert

/**
 * Turns what the data source reports into tracks. Sources differ in what they report, so each comes with
 * its own strategy: alerts alone (alerts.in.ua) or surveillance sightings (the simulation).
 */
internal interface TargetTracking {
    /** Feeds in the source's latest report, made at [nowMs]. */
    fun update(alerts: List<AirAlert>, observations: List<TargetObservation>, nowMs: Long): TrackingUpdate
}
