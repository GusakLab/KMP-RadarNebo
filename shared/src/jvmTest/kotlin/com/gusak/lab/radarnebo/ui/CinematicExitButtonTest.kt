package com.gusak.lab.radarnebo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.gusak.lab.radarnebo.data.MapMode
import com.gusak.lab.radarnebo.data.ThemeMode
import com.gusak.lab.radarnebo.ui.preview.PreviewData
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The way out of cinematic mode sits over the letterbox's top bar and must stay solid and tappable there.
 * The windows here have no insets, so the button lies entirely within the bar. Renders land in `build/cinematic-exit`.
 */
@OptIn(ExperimentalTestApi::class)
class CinematicExitButtonTest {
    @Test fun phonePortrait() = check("phone-portrait", 402, 874)
    @Test fun phoneLandscape() = check("phone-landscape", 874, 402)
    @Test fun tabletPortrait() = check("tablet-portrait", 834, 1194)
    @Test fun tabletLandscape() = check("tablet-landscape", 1194, 834)

    private fun check(name: String, width: Int, height: Int) {
        for (theme in listOf(ThemeMode.Dark, ThemeMode.Light)) {
            var modeAsked: MapMode? = null
            var primary = Color.Unspecified
            runDesktopComposeUiTest(width, height) {
                // Map markers pulse forever, so the clock only moves when told to.
                mainClock.autoAdvance = false
                setContent {
                    RadarTheme(theme) {
                        primary = MaterialTheme.colorScheme.primary
                        MapScreen(PreviewData.state(theme, mapMode = MapMode.Cinematic), onModeChange = { modeAsked = it }, onAddFavorite = { _, _ -> })
                    }
                }
                // Past the toggle's 450 ms fade.
                mainClock.advanceTimeBy(1000)

                val button = onNodeWithContentDescription("Exit cinematic mode")
                val bounds = button.fetchSemanticsNode().boundsInRoot
                assertTrue(bounds.top < TOP_BAR_HEIGHT, "The button should overlap the top bar: $bounds")

                val image = captureToImage()
                File("build/cinematic-exit").apply { mkdirs() }.resolve("$name-${theme.name.lowercase()}.png")
                    .writeBytes(Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes)

                // In the button's fill, outside the radar icon: any fade or bar over it would shift this pixel.
                val fill = image.toPixelMap()[bounds.center.x.toInt(), bounds.top.toInt() + 4]
                assertTrue(fill.closeTo(primary), "$name/$theme: expected the solid fill $primary, got $fill")

                // A real touch, so it fails if anything above the button takes the pointer.
                button.performTouchInput { click() }
                mainClock.advanceTimeBy(200)
                assertEquals(MapMode.Static, modeAsked, "$name/$theme: the tap should leave cinematic mode")
            }
        }
    }

    private fun Color.closeTo(other: Color) =
        abs(red - other.red) < 0.02f && abs(green - other.green) < 0.02f && abs(blue - other.blue) < 0.02f

    private companion object {
        /** The bar's height in dp at its smallest; the windows here are at density 1. */
        const val TOP_BAR_HEIGHT = 84f
    }
}
