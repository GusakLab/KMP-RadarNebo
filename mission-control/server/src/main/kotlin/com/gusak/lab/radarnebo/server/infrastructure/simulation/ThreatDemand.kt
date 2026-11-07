package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType

/**
 * A threat the warning system wants declared for a location. [targetId] is set when the target is right
 * there.
 */
internal data class ThreatDemand(
    val type: ThreatType,
    val level: ThreatLevel,
    val message: String,
    val targetId: String? = null,
)
