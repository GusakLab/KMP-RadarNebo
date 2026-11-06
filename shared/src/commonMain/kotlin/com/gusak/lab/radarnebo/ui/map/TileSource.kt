package com.gusak.lab.radarnebo.ui.map

import com.gusak.lab.radarnebo.data.MapBackground

/**
 * A web map tile set (Web Mercator, 256 px tiles) and the credit its terms require. The street maps are Carto's
 * label-free basemaps, bundled with the app (see [bundled]) so they show offline and leave cities to the map's
 * own labels; satellite imagery comes from Esri's public service, which needs no key. At night (the dark theme)
 * the satellite view shows NASA's Black Marble city lights instead, bundled too.
 */
enum class TileSource(
    internal val maxZoom: Int,
    internal val attribution: String,
    private val template: String,
    /** File extension of bundled tiles. */
    internal val extension: String = "png",
    /** Photographic imagery rather than a drawn map. */
    internal val isImagery: Boolean = false,
) {
    DarkStreets(BUNDLED_MAX_ZOOM, "Map © CARTO, © OpenStreetMap contributors", "bundled:carto-dark"),
    LightStreets(BUNDLED_MAX_ZOOM, "Map © CARTO, © OpenStreetMap contributors", "bundled:carto-light"),
    Satellite(
        18,
        "Imagery © Esri, Maxar, Earthstar Geographics",
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        isImagery = true,
    ),
    // The deepest zoom NASA GIBS serves Black Marble at (NIGHT_MAX_ZOOM in tools/download-map-tiles.py).
    NightSatellite(
        8,
        "Imagery: NASA Black Marble, via NASA GIBS",
        "bundled:nasa-night",
        extension = "jpg",
        isImagery = true,
    ),
    ;

    /** The resource folder (under files/map) holding this set's tiles as z/x/y.[extension], or null if downloaded. */
    internal val bundled: String? get() = template.removePrefix("bundled:").takeIf { it != template }

    internal fun url(z: Int, x: Int, y: Int): String =
        template.replace("{z}", "$z").replace("{x}", "$x").replace("{y}", "$y")

    internal companion object {
        /** The tiles for [background]; street maps and satellite imagery follow the theme. Null shows no map. */
        fun of(background: MapBackground, dark: Boolean): TileSource? = when (background) {
            MapBackground.None -> null
            MapBackground.Streets -> if (dark) DarkStreets else LightStreets
            MapBackground.Satellite -> if (dark) NightSatellite else Satellite
        }
    }
}

/** The deepest zoom tools/download-map-tiles.py stores (MAX_TILE_ZOOM there); closer views upscale it. */
private const val BUNDLED_MAX_ZOOM = 9
