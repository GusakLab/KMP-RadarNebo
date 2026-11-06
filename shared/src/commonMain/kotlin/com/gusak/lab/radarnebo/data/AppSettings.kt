package com.gusak.lab.radarnebo.data

data class AppSettings(
    /** The RadarNebo server the app gets everything from. */
    val serverUrl: String = RadarNeboApi.DEFAULT_SERVER_URL,
    /** Only needed when the server requires client keys. */
    val accessKey: String = "",
    val mapMode: MapMode = MapMode.Static,
    val themeMode: ThemeMode = ThemeMode.System,
    val colorPalette: ColorPalette = ColorPalette.Radar,
    val maxMapMarkers: Int = DEFAULT_MAX_MARKERS,
    val proximityRadiusKm: Int = 50,
    val dronesOnly: Boolean = true,
    val pollSeconds: Int = 30,
    val targetFilter: TargetFilter = TargetFilter(),
    /** Targets last seen longer ago than this are hidden; active ones always show. */
    val targetWindowMinutes: Int = DEFAULT_TARGET_WINDOW_MINUTES,
    val mapBackground: MapBackground = MapBackground.Streets,
) {
    internal val endpoint: ServerEndpoint get() = ServerEndpoint(serverUrl.trim(), accessKey.trim())

    internal companion object {
        const val DEFAULT_MAX_MARKERS: Int = 60
        val MAX_MARKERS_RANGE: IntRange = 5..300
        val RADIUS_RANGE: IntRange = 5..200
        val POLL_RANGE: IntRange = 10..300
        const val DEFAULT_TARGET_WINDOW_MINUTES: Int = 60
        val TARGET_WINDOW_RANGE: IntRange = 10..25 * 60
    }
}
