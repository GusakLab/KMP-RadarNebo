package com.gusak.lab.radarnebo.data

sealed interface AlertsUpdate {
    data class Updated(val snapshot: AlertsSnapshot) : AlertsUpdate
    /** [backOff] as in [FetchResult.Failure]. */
    data class Failed(val message: String, val backOff: Boolean = false) : AlertsUpdate
}
