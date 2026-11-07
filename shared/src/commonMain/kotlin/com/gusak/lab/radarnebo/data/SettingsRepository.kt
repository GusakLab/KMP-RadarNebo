package com.gusak.lab.radarnebo.data

import com.gusak.lab.radarnebo.tracking.TargetKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App settings, persisted in [KeyValueStorage] and observable as [settings]. */
class SettingsRepository(
    private val storage: KeyValueStorage,
    private val defaults: ServerDefaults = ServerDefaults(),
) {
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** Applies [transform], saves the result and returns it. */
    fun update(transform: (AppSettings) -> AppSettings): AppSettings {
        val new = transform(_settings.value)
        if (new != _settings.value) {
            save(new)
            _settings.value = new
        }
        return new
    }

    private fun load(): AppSettings {
        // Earlier versions talked to alerts.in.ua directly, or to a proxy given as the base URL with its key as the
        // token.
        val legacyProxyUrl = storage.getString(K_LEGACY_BASE_URL)
            ?.takeIf { it.isNotBlank() && LEGACY_UPSTREAM_HOST !in it }
        val legacyKey = if (legacyProxyUrl != null) storage.getString(K_LEGACY_TOKEN) else null
        return AppSettings(
            serverUrl = storage.getString(K_SERVER_URL)?.takeIf { it.isNotBlank() } ?: legacyProxyUrl ?: defaults.url,
            accessKey = storage.getString(K_ACCESS_KEY)?.takeIf { it.isNotBlank() } ?: legacyKey ?: defaults.accessKey,
            mapMode = storage.getEnum<MapMode>(K_MODE) ?: MapMode.Static,
            themeMode = storage.getEnum<ThemeMode>(K_THEME) ?: ThemeMode.System,
            colorPalette = storage.getEnum<ColorPalette>(K_PALETTE) ?: ColorPalette.Radar,
            maxMapMarkers = storage.getString(K_MAX_MARKERS)?.toIntOrNull()
                ?.coerceIn(AppSettings.MAX_MARKERS_RANGE) ?: AppSettings.DEFAULT_MAX_MARKERS,
            proximityRadiusKm = storage.getString(K_RADIUS)?.toIntOrNull()?.coerceIn(AppSettings.RADIUS_RANGE) ?: 50,
            dronesOnly = storage.getString(K_DRONES_ONLY)?.toBooleanStrictOrNull() ?: true,
            pollSeconds = storage.getString(K_POLL)?.toIntOrNull()?.coerceIn(AppSettings.POLL_RANGE) ?: 30,
            targetFilter = TargetFilter(
                hiddenKinds = storage.getString(K_HIDDEN_KINDS).orEmpty().split(',')
                    .mapNotNull { name -> TargetKind.entries.firstOrNull { it.name == name } }.toSet(),
                hideProbable = storage.getString(K_HIDE_PROBABLE)?.toBooleanStrictOrNull() ?: false,
            ),
            targetWindowMinutes = storage.getString(K_TARGET_WINDOW)?.toIntOrNull()
                ?.coerceIn(AppSettings.TARGET_WINDOW_RANGE) ?: AppSettings.DEFAULT_TARGET_WINDOW_MINUTES,
            mapBackground = storage.getEnum<MapBackground>(K_MAP_BACKGROUND) ?: MapBackground.Streets,
        )
    }

    /** The constant of [T] named by the value stored at [key]; null when nothing or an unknown name is stored. */
    private inline fun <reified T : Enum<T>> KeyValueStorage.getEnum(key: String): T? =
        getString(key)?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }

    private fun save(settings: AppSettings) {
        storage.putString(K_SERVER_URL, settings.serverUrl.trim())
        storage.putString(K_ACCESS_KEY, settings.accessKey.trim())
        storage.putString(K_MODE, settings.mapMode.name)
        storage.putString(K_THEME, settings.themeMode.name)
        storage.putString(K_PALETTE, settings.colorPalette.name)
        storage.putString(K_MAX_MARKERS, settings.maxMapMarkers.toString())
        storage.putString(K_RADIUS, settings.proximityRadiusKm.toString())
        storage.putString(K_DRONES_ONLY, settings.dronesOnly.toString())
        storage.putString(K_POLL, settings.pollSeconds.toString())
        storage.putString(K_HIDDEN_KINDS, settings.targetFilter.hiddenKinds.joinToString(",") { it.name })
        storage.putString(K_HIDE_PROBABLE, settings.targetFilter.hideProbable.toString())
        storage.putString(K_TARGET_WINDOW, settings.targetWindowMinutes.toString())
        storage.putString(K_MAP_BACKGROUND, settings.mapBackground.name)
        // An alerts.in.ua token belongs on the server, never on the device.
        storage.putString(K_LEGACY_TOKEN, null)
        storage.putString(K_LEGACY_BASE_URL, null)
    }

    private companion object {
        const val K_SERVER_URL = "server_url"
        const val K_ACCESS_KEY = "server_access_key"
        const val K_LEGACY_TOKEN = "api_token"
        const val K_LEGACY_BASE_URL = "api_base_url"
        const val LEGACY_UPSTREAM_HOST = "alerts.in.ua"
        const val K_MODE = "map_mode"
        const val K_THEME = "theme_mode"
        const val K_PALETTE = "color_palette"
        const val K_MAX_MARKERS = "map_max_markers"
        const val K_RADIUS = "proximity_radius_km"
        const val K_DRONES_ONLY = "drones_only"
        const val K_POLL = "poll_seconds"
        const val K_HIDDEN_KINDS = "targets_hidden_kinds"
        const val K_HIDE_PROBABLE = "targets_hide_probable"
        const val K_TARGET_WINDOW = "targets_window_minutes"
        const val K_MAP_BACKGROUND = "map_background"
    }
}
