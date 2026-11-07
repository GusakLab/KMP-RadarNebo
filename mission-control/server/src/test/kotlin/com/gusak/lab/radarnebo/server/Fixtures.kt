package com.gusak.lab.radarnebo.server

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.server.domain.alert.AirAlert
import com.gusak.lab.radarnebo.server.domain.alert.AlertLocation
import com.gusak.lab.radarnebo.server.domain.alert.LocationDirectory
import com.gusak.lab.radarnebo.server.domain.alert.LocationType
import com.gusak.lab.radarnebo.server.domain.alert.ThreatSignal
import kotlin.time.Instant

/** Alerts for tests, at real alerts.in.ua locations. */
internal object Fixtures {
    val T0: Instant = Instant.parse("2026-10-19T10:00:00Z")

    fun region(uid: Int): AlertLocation = LocationDirectory.OBLASTS.single { it.uid == uid.toString() }

    /** Brovary hromada, Kyiv oblast. */
    val BROVARY = AlertLocation(
        "1100",
        "Броварська територіальна громада",
        "Brovary",
        LocationType.Hromada,
        14,
        "Київська область",
    )

    /** Kharkiv city hromada. */
    val KHARKIV = AlertLocation(
        "1293",
        "м. Харків та Харківська територіальна громада",
        "Kharkiv",
        LocationType.Hromada,
        22,
        "Харківська область",
    )

    fun alert(
        id: Long,
        location: AlertLocation,
        startedAt: Instant = T0,
        level: ThreatLevel = ThreatLevel.Red,
        threat: ThreatType? = ThreatType.Drones,
        message: String? = "Ударні БпЛА типу «Шахед»",
    ) = AirAlert(
        id = id,
        location = location,
        level = level,
        startedAt = startedAt,
        threats = listOfNotNull(threat?.let { ThreatSignal(it, level, startedAt, message) }),
    )
}
