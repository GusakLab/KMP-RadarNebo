package com.gusak.lab.radarnebo.data

import com.gusak.lab.radarnebo.tracking.DroneTrack

/** Active alerts and the server's tracks, as of the latest successful poll. */
data class AlertsSnapshot(
    val alerts: List<Alert>,
    /** Tracks of every threat class; the UI filters them by settings. */
    val tracks: List<DroneTrack>,
    /** Server clock minus local clock, applied before interpolating track positions. */
    val clockOffsetMs: Long,
    /** The server says its data is simulated; the UI must label it. */
    val simulated: Boolean,
    /** The server's own report on its data source; null if it didn't answer. */
    val health: ServerHealth? = null,
)
