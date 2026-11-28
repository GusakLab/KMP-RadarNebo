package com.gusak.lab.radarnebo.domain.usecase

import com.gusak.lab.radarnebo.data.Alert
import com.gusak.lab.radarnebo.data.AppSettings
import com.gusak.lab.radarnebo.data.FavoriteLocation
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.domain.ThreatMapper
import com.gusak.lab.radarnebo.tracking.DroneTrack
import com.gusak.lab.radarnebo.tracking.TrackInterpolator
import kotlin.time.Clock

/**
 * Turns alerts and tracks into markers, warnings for favorites and oblast shading. Pure, no state; [clock] only
 * supplies the time when a caller gives none.
 */
class BuildThreatPictureUseCase(
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    operator fun invoke(
        alerts: List<Alert>,
        tracks: List<DroneTrack>,
        settings: AppSettings,
        favorites: List<FavoriteLocation>,
        /** Server time, for the speeds that tell drone types apart. */
        now: Long = clock(),
    ): ThreatPicture {
        val since = now - settings.targetWindowMinutes * 60_000L
        val allThreats = ThreatMapper.toMarkers(alerts, settings.dronesOnly)
        // alerts.in.ua keeps frontline alerts open for weeks; the timeline hides those, but they still warn.
        val threats = allThreats.filter { (it.startedAt?.toEpochMilliseconds() ?: now) >= since }
        val shownTracks = tracks
            // The server's verdict is as of its last poll; a target can go stale before the next one.
            .map { if (it.active && !TrackInterpolator.isActive(it, now)) it.copy(active = false) else it }
            .filter { (it.active || it.lastSeen >= since) && (!settings.dronesOnly || it.type == ThreatType.Drones) }
        return ThreatPicture(
            threats = threats,
            mapThreats = ThreatMapper.capForMap(threats, settings.maxMapMarkers),
            proximity = ThreatMapper.proximity(favorites, allThreats, settings.proximityRadiusKm),
            alertedOblasts = alertedOblasts(alerts),
            tracks = shownTracks.filter { settings.targetFilter.allows(it, now) },
            unfilteredTracks = shownTracks,
        )
    }

    internal companion object {
        /** Oblasts under an alert, by uid: true when the alert covers the whole oblast, false for part of it. */
        fun alertedOblasts(alerts: List<Alert>): Map<Int, Boolean> {
            val result = mutableMapOf<Int, Boolean>()
            alerts.filter { it.alertType == "air_raid" || it.threats.isNotEmpty() }.forEach { alert ->
                val uid = ThreatMapper.oblastUid(alert) ?: return@forEach
                result[uid] = (result[uid] ?: false) || alert.locationType == "oblast"
            }
            return result
        }
    }
}
