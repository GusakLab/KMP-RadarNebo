package com.gusak.lab.radarnebo.missioncontrol

/**
 * What the monitor knows about the server: its last report and when it came, and whether the latest attempt failed.
 * A failed attempt keeps the last report, so a short outage doesn't blank the screen.
 */
internal data class MonitorState(
    val health: Health? = null,
    val fetchedAt: Long? = null,
    val error: String? = null,
    val loading: Boolean = true,
)
