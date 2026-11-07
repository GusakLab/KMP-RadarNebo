package com.gusak.lab.radarnebo.server.application

import com.gusak.lab.radarnebo.server.domain.alert.AirAlert
import com.gusak.lab.radarnebo.server.domain.tracking.TargetObservation

/** Everything a data source knew at [observedAt]: the active alerts and, if it has surveillance, target sightings. */
data class SituationReport(
    val observedAt: Long,
    val alerts: List<AirAlert>,
    val observations: List<TargetObservation> = emptyList(),
    /** The active-alerts body exactly as the source sent it, served to clients unchanged; null for the simulation. */
    val rawAlerts: String? = null,
    /**
     * Alerts from further sources ([com.gusak.lab.radarnebo.server.infrastructure.CompositeAlertsSource]): they feed
     * tracking only and are never served as alerts, so `/v1/alerts/active.json` stays the primary source's.
     */
    val trackingAlerts: List<AirAlert> = emptyList(),
)
