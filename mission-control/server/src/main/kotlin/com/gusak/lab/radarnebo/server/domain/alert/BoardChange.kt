package com.gusak.lab.radarnebo.server.domain.alert

/** What changed between two boards. */
internal data class BoardChange(
    val board: AlertBoard,
    val opened: List<AirAlert> = emptyList(),
    val updated: List<AirAlert> = emptyList(),
    val finished: List<AirAlert> = emptyList(),
) {
    val changed: Boolean get() = opened.isNotEmpty() || updated.isNotEmpty() || finished.isNotEmpty()
}
