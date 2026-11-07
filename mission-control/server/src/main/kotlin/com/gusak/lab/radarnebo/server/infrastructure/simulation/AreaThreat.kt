package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType

/**
 * A threat over whole regions for a while: aviation activity, a MiG-31K in the air, missile carriers at
 * sea. [oblastUids] are alerted from [from] until [until].
 */
internal class AreaThreat(
    val type: ThreatType,
    val oblastUids: Collection<Int>,
    val from: Long,
    val until: Long,
    val message: String,
    val level: ThreatLevel = ThreatLevel.Red,
) {
    fun isActive(t: Long): Boolean = t in from until until
}
