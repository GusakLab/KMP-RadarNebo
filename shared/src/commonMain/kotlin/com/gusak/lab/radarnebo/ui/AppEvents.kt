package com.gusak.lab.radarnebo.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Where the host wants the UI to go. Desktop notification clicks use this.
 * [Map] shows the map without a new selection. [Threat] selects that threat.
 */
sealed interface AppRoute {
    data object Favorites : AppRoute

    data object Map : AppRoute

    data class Threat(val threatId: String) : AppRoute
}

/** One request to open a screen. [token] changes when [route] is the same, so the effect runs again. */
data class AppRouteRequest(
    val token: Int,
    val route: AppRoute,
)

/**
 * Selects a threat on a map that is already showing.
 * [MapScreenStart] only applies when the map is first composed, so a later click uses this.
 */
data class MapFocus(
    val token: Int,
    val threatId: String,
)

/**
 * Host navigation into the app, and a callback for each new [UiState].
 * [com.gusak.lab.radarnebo.App] ignores this when the platform passes null.
 * [open] must run on the UI thread.
 */
class AppEvents {
    var request by mutableStateOf<AppRouteRequest?>(null)
        private set

    /** Called with every new picture. The desktop sender turns new threats into notifications. */
    var onState: (UiState) -> Unit = {}

    /** Called after the UI has switched to the requested screen. */
    var onApplied: () -> Unit = {}

    fun open(route: AppRoute) {
        request = AppRouteRequest(token = (request?.token ?: 0) + 1, route = route)
    }
}
