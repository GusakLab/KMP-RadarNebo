package com.gusak.lab.radarnebo.server.domain.alert

import com.gusak.lab.radarnebo.geo.Regions
import java.util.concurrent.ConcurrentHashMap

/**
 * Every location the server knows: the oblasts, the locations its data source declares in advance, and
 * any location an alert has been seen for. The IoT status endpoints index their strings by it.
 */
internal class LocationDirectory(known: Collection<AlertLocation> = emptyList()) {
    private val byUid = ConcurrentHashMap<String, AlertLocation>()

    init {
        OBLASTS.forEach { byUid[it.uid] = it }
        known.forEach { byUid[it.uid] = it }
    }

    fun find(uid: String): AlertLocation? = byUid[uid]

    fun all(): Collection<AlertLocation> = byUid.values

    /** Remembers the locations of [alerts]; alerts.in.ua publishes no location list of its own. */
    fun learn(alerts: Collection<AirAlert>): Unit = alerts.forEach { byUid.putIfAbsent(it.location.uid, it.location) }

    companion object {
        /** The 25 oblasts plus Kyiv and Sevastopol, as alerts.in.ua numbers them. */
        val OBLASTS: List<AlertLocation> = Regions.oblasts.map { oblast ->
            val isCity = oblast.uid == Regions.KYIV_CITY_UID || oblast.uid == Regions.SEVASTOPOL_UID
            AlertLocation(
                uid = oblast.uid.toString(),
                title = oblast.nameUk,
                titleEn = oblast.nameEn,
                type = if (isCity) LocationType.City else LocationType.Oblast,
                oblastUid = oblast.uid,
                oblastTitle = oblast.nameUk,
            )
        }

        private val REGION_UIDS: Set<String> = OBLASTS.mapTo(HashSet()) { it.uid }

        /** Oblasts, Kyiv and Sevastopol: the UIDs whose history includes the places inside them. */
        fun isRegionUid(uid: String): Boolean = uid in REGION_UIDS
    }
}
