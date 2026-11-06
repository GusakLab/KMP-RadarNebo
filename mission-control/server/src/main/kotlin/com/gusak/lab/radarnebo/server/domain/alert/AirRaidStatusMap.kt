package com.gusak.lab.radarnebo.server.domain.alert

import com.gusak.lab.radarnebo.data.ThreatLevel

/** Air raid status of every location under the alerts on [alerts]. */
internal class AirRaidStatusMap(alerts: Collection<AirAlert>, private val locations: LocationDirectory) {
    private val airRaids = alerts.filter { it.isActive && it.type == AlertType.AirRaid }
    private val byLocation = airRaids.groupBy { it.location.uid }
    private val oblastsWithAlerts = airRaids.mapNotNullTo(HashSet()) { it.location.oblastUid }

    /**
     * Status of an oblast (or Kyiv / Sevastopol): whole when the region itself is alerted, partial when part of
     * it is.
     */
    fun oblast(uid: Int): AirRaidStatus =
        wholeStatus(uid.toString()) ?: if (uid in oblastsWithAlerts) AirRaidStatus.Partial else AirRaidStatus.None

    /** Status of any location: its own alert, or the alert over its whole oblast. */
    fun location(uid: String): AirRaidStatus {
        val location = locations.find(uid) ?: return AirRaidStatus.None
        val oblastUid = location.oblastUid
        if (location.isRegion && oblastUid != null) return oblast(oblastUid)
        return wholeStatus(uid) ?: oblastUid?.let { wholeStatus(it.toString()) } ?: AirRaidStatus.None
    }

    /** The 27 oblast statuses in the order the `by_oblast` endpoints use. */
    fun byOblast(version: IotVersion): String = OBLAST_ORDER.map { oblast(it).code(version) }.joinToString("")

    /** One character per location UID, at the position of the UID; a space where no location has that UID. */
    fun byUid(version: IotVersion): String {
        val uids = locations.all().mapNotNull { it.uid.toIntOrNull() }.toSet()
        val max = uids.maxOrNull() ?: return ""
        return (0..max).map { uid -> if (uid in uids) location(uid.toString()).code(version) else ' ' }.joinToString("")
    }

    private fun wholeStatus(uid: String): AirRaidStatus? = byLocation[uid]
        ?.let { alerts ->
            if (alerts.any { it.level != ThreatLevel.Yellow }) AirRaidStatus.Active else AirRaidStatus.Preliminary
        }

    companion object {
        /**
         * Order of the `active_air_raid_alerts_by_oblast` string: Crimea, Volyn, Vinnytsia, Dnipropetrovsk,
         * Donetsk, Zhytomyr, Zakarpattia, Zaporizhzhia, Ivano-Frankivsk, Kyiv city, Kyiv oblast, Kirovohrad,
         * Luhansk, Lviv, Mykolaiv, Odesa, Poltava, Rivne, Sevastopol, Sumy, Ternopil, Kharkiv, Kherson,
         * Khmelnytskyi, Cherkasy, Chernivtsi, Chernihiv.
         */
        val OBLAST_ORDER: List<Int> = listOf(
            29, 8, 4, 9, 28, 10, 11, 12, 13, 31, 14, 15, 16, 27, 17, 18, 19, 5, 30, 20, 21, 22, 23, 3, 24, 26, 25,
        )
    }
}
