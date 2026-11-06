package com.gusak.lab.radarnebo.server.domain.alert

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import kotlin.time.Instant

/** One threat an alert warns about. An alert can carry several, e.g. drones and cruise missiles at once. */
data class ThreatSignal(
    val type: ThreatType,
    val level: ThreatLevel,
    val startedAt: Instant,
    /** The warning's original wording, which often names the weapon. */
    val sourceMessage: String?,
)
