package com.gusak.lab.radarnebo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.gusak.lab.radarnebo.data.ColorPalette
import com.gusak.lab.radarnebo.data.MapBackground
import com.gusak.lab.radarnebo.data.MapMode
import com.gusak.lab.radarnebo.data.ThemeMode
import com.gusak.lab.radarnebo.ui.map.LocalMapTiles
import com.gusak.lab.radarnebo.ui.map.MapSelection
import com.gusak.lab.radarnebo.ui.map.MapTiles
import com.gusak.lab.radarnebo.ui.preview.PreviewData
import io.ktor.client.HttpClient
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Screenshots of every map overlay and dialog, for each map background in each theme, to review text contrast
 * by eye. Satellite tiles are downloaded, so this only runs with `-Dcontrast.screenshots.dir=<dir>`:
 * `./gradlew :shared:jvmTest --tests '*ContrastScreenshotsTest*' -PcontrastScreenshots`.
 */
@OptIn(ExperimentalTestApi::class)
class ContrastScreenshotsTest {
    private val out = System.getProperty("contrast.screenshots.dir")?.let { File(it).apply { mkdirs() } }
    private val palettes = System.getProperty("contrast.palettes")?.split(",")?.map { ColorPalette.valueOf(it) }
        ?: listOf(ColorPalette.Radar)

    private enum class Dialog { None, Favorites, Settings }

    /** One palette, theme and map background, and the name its files start with. */
    private class Look(val palette: ColorPalette, val theme: ThemeMode, val background: MapBackground) {
        val name: String = "${palette.name.lowercase()}-${theme.name.lowercase()}-${background.name.lowercase()}"
    }

    /** Where [Host] puts the map tiles it loads, so the test can wait for them. */
    private class TilesHolder {
        var tiles: MapTiles? = null
    }

    private companion object {
        const val PAGE_HEIGHT = 1720
        const val SETTINGS_PAGES = 3

        /** How long a frame's tile downloads may take before the test gives up on them. */
        const val TILE_TIMEOUT_MS = 60_000L
    }

    @Test
    fun everyMapBackgroundAndTheme() {
        assumeTrue("run with -PcontrastScreenshots", out != null)
        for (palette in palettes) for (theme in listOf(ThemeMode.Light, ThemeMode.Dark)) {
            for (background in MapBackground.entries) shoot(Look(palette, theme, background))
        }
    }

    private fun shoot(look: Look) {
        val shots = mutableListOf<Image>()
        runDesktopComposeUiTest(840, PAGE_HEIGHT) { shootScenes(look, shots) }
        writeSheets(look.name, shots + settingsPages(look))
    }

    private fun SkikoComposeUiTest.shootScenes(look: Look, shots: MutableList<Image>) {
        var scene by mutableStateOf(Scene())
        val holder = TilesHolder()
        // Map markers pulse forever, so the clock only moves when told to.
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Host(look, scene, holder)
            }
        }
        fun save(shot: String) {
            settle(holder)
            val image = Image.makeFromBitmap(captureToImage().asSkiaBitmap())
            File(out, "${look.name}-$shot.png").writeBytes(png(image))
            shots += image
        }
        save("overview")
        scene = Scene(start = MapScreenStart(camera = PreviewData.camera(PreviewData.city("Kyiv"), 5f)), quiet = true)
        save("cities-kyiv")
        scene = Scene(start = MapScreenStart(camera = PreviewData.camera(PreviewData.city("Lviv"), 4f)), quiet = true)
        save("cities-lviv")
        scene = Scene(start = MapScreenStart(selectedTrackId = PreviewData.Targets.SHAHED))
        save("target-selected")
        scene = Scene(start = MapScreenStart(targetsOpen = true))
        save("targets-list")
        scene = Scene(start = MapScreenStart(radiusOpen = true))
        save("radius")
        scene = Scene(start = MapScreenStart(pendingFavorite = PreviewData.city("Poltava")))
        save("add-favorite")
        scene = Scene(start = MapScreenStart(selection = MapSelection.Favorite(threatenedFavorite(look.theme))))
        save("favorite-card")
        scene = Scene()
        settle(holder)
        // The status strip opens the server card.
        onNodeWithText("drone threats", substring = true).performClick()
        save("server-card")
        scene = Scene(mapMode = MapMode.Cinematic)
        save("cinematic")
        scene = Scene(dialog = Dialog.Favorites)
        save("favorites")
        onNodeWithText("Add by coordinates").performClick()
        save("coordinates")
        scene = Scene(dialog = Dialog.Settings)
        save("settings")
    }

    /** A favorite with a threat near it in the preview scenario, or the first one if none has. */
    private fun threatenedFavorite(theme: ThemeMode) = PreviewData.state(theme).let { s ->
        s.favorites.firstOrNull { f -> s.proximity.any { it.favorite.id == f.id } } ?: s.favorites.first()
    }

    /**
     * The whole Settings dialog, rendered in a window tall enough to need no scrolling (the screen never goes
     * idle, so the test can't scroll it), cut into phone-sized pages.
     */
    private fun settingsPages(look: Look): List<Image> {
        var tall: Image? = null
        runDesktopComposeUiTest(840, PAGE_HEIGHT * SETTINGS_PAGES) {
            val holder = TilesHolder()
            mainClock.autoAdvance = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    Host(look, Scene(dialog = Dialog.Settings), holder)
                }
            }
            settle(holder)
            tall = Image.makeFromBitmap(captureToImage().asSkiaBitmap())
        }
        val whole = assertNotNull(tall)
        return (0 until SETTINGS_PAGES).map { page ->
            val surface = Surface.makeRasterN32Premul(840, PAGE_HEIGHT)
            surface.canvas.drawImageRect(
                whole,
                Rect.makeXYWH(0f, (page * PAGE_HEIGHT).toFloat(), 840f, PAGE_HEIGHT.toFloat()),
                Rect.makeWH(840f, PAGE_HEIGHT.toFloat()),
            )
            surface.makeImageSnapshot().also {
                File(out, "${look.name}-settings-page${page + 1}.png").writeBytes(png(it))
            }
        }
    }

    /** The shots four to a page at half size, to review a combination at a glance. */
    private fun writeSheets(name: String, shots: List<Image>) {
        shots.chunked(4).forEachIndexed { page, four ->
            val w = four.first().width / 2
            val h = four.first().height / 2
            val surface = Surface.makeRasterN32Premul(w * 2, h * 2)
            four.forEachIndexed { i, image ->
                surface.canvas.drawImageRect(
                    image,
                    Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                    Rect.makeXYWH((i % 2) * w.toFloat(), (i / 2) * h.toFloat(), w.toFloat(), h.toFloat()),
                    SamplingMode.LINEAR,
                    null,
                    strict = true,
                )
            }
            File(out, "sheet-$name-${page + 1}.png").writeBytes(png(surface.makeImageSnapshot()))
        }
    }

    private fun png(image: Image): ByteArray = assertNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes

    private data class Scene(
        val start: MapScreenStart = MapScreenStart(),
        val quiet: Boolean = false,
        val mapMode: MapMode = MapMode.Static,
        val dialog: Dialog = Dialog.None,
    )

    @Composable
    private fun Host(look: Look, scene: Scene, holder: TilesHolder) {
        val scope = rememberCoroutineScope()
        // Loads on the composition's own dispatcher, so tiles land between frames like in the app.
        val tiles = remember { MapTiles(HttpClient(), scope = scope).also { holder.tiles = it } }
        val base = if (scene.quiet) {
            PreviewData.quiet(look.theme, favorites = emptyList())
        } else {
            PreviewData.state(look.theme)
        }
        val settings = base.settings.copy(
            mapBackground = look.background,
            colorPalette = look.palette,
            mapMode = scene.mapMode,
        )
        val state = base.copy(settings = settings)
        RadarTheme(look.theme, look.palette) {
            CompositionLocalProvider(LocalMapTiles provides tiles) {
                key(scene) { SceneContent(state, scene) }
            }
        }
    }

    @Composable
    private fun SceneContent(state: UiState, scene: Scene) {
        val backdrop = rememberGlassBackdrop()
        Box(Modifier.fillMaxSize()) {
            Surface(
                Modifier.fillMaxSize().glassBackdropSource(backdrop),
                color = MaterialTheme.colorScheme.background,
            ) {
                MapScreen(
                    state,
                    onModeChange = {},
                    onAddFavorite = { _, _ -> },
                    start = scene.start,
                    bottomInset = 88.dp,
                    clock = PreviewData::nowMs,
                )
            }
            GlassDialog(
                visible = scene.dialog == Dialog.Favorites,
                title = "Favorite places",
                onDismiss = {},
                backdrop = backdrop,
                subtitle = "Threats within 50 km are flagged.",
                bottomInset = 88.dp,
            ) {
                FavoritesScreen(state, onAdd = { _, _ -> }, onRemove = {})
            }
            GlassDialog(scene.dialog == Dialog.Settings, "Settings", {}, backdrop, bottomInset = 88.dp) {
                SettingsScreen(state.settings, onChange = {})
            }
            GlassNavigationBar(
                items = listOf(
                    GlassNavItem("Map", "◎"),
                    GlassNavItem("Favorites", "★", badge = 2),
                    GlassNavItem("Settings", "⚙"),
                ),
                selectedIndex = scene.dialog.ordinal,
                onSelect = {},
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            )
        }
    }

    /**
     * Runs frames for a while, waiting after each for the tile downloads it started: they run on real threads, so
     * the test waits for them to land instead of for a fixed time.
     */
    private fun SkikoComposeUiTest.settle(holder: TilesHolder) {
        repeat(40) {
            mainClock.advanceTimeBy(50)
            waitUntil("map tiles loaded", TILE_TIMEOUT_MS) { holder.tiles?.isLoading != true }
        }
    }
}
