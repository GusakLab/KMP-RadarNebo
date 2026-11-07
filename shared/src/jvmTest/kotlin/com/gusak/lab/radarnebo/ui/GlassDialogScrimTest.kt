package com.gusak.lab.radarnebo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.gusak.lab.radarnebo.data.ThemeMode
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The scrim must reach every edge of the window, under the status bar, cutouts and the system navigation bar, whatever
 * insets the panel keeps clear; here padding stands in for those insets, as tests have no real ones.
 */
@OptIn(ExperimentalTestApi::class)
class GlassDialogScrimTest {
    private val items = listOf(GlassNavItem("Map", "◎"), GlassNavItem("Favorites", "★"), GlassNavItem("Settings", "⚙"))

    /** The same arrangement as the app's root: the dialog is inset, and the navigation bar floats on top. */
    @Composable
    private fun Host(theme: ThemeMode, open: Boolean, onDismiss: () -> Unit = {}, onSelect: (Int) -> Unit = {}) {
        RadarTheme(theme) {
            val backdrop = rememberGlassBackdrop()
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().glassBackdropSource(backdrop).background(Color.White))
                GlassDialog(open, "Dialog", onDismiss, backdrop, Modifier.padding(start = 44.dp, top = 59.dp, end = 44.dp), bottomInset = 122.dp) {
                    Text("Content")
                }
                GlassNavigationBar(items, 1, onSelect, backdrop, Modifier.align(Alignment.BottomCenter).padding(bottom = 46.dp), dimmed = open)
            }
        }
    }

    private fun PixelMap.brightness(x: Int, y: Int) = this[x, y].let { (it.red + it.green + it.blue) / 3 }

    /**
     * Glass redraws whenever its backdrop source draws and the source bumps a counter each time, so the UI never goes
     * idle: left to run by itself, the clock would keep [androidx.compose.ui.test.ComposeUiTest.waitForIdle] waiting.
     */
    private fun SkikoComposeUiTest.show(content: @Composable () -> Unit) {
        mainClock.autoAdvance = false
        setContent(content)
        settle()
    }

    private fun SkikoComposeUiTest.settle() = mainClock.advanceTimeBy(1000)

    @Test
    fun theScrimCoversEveryEdgeAndCorner() {
        for ((theme, expected) in listOf(ThemeMode.Light to 0.8f, ThemeMode.Dark to 0.65f)) {
            runDesktopComposeUiTest(402, 874) {
                show { Host(theme, open = true) }
                val pixels = captureToImage().toPixelMap()
                val w = pixels.width - 1
                val h = pixels.height - 1
                // Corners, the middle of the status bar and of the system navigation bar, then both sides.
                for ((x, y) in listOf(0 to 0, w to 0, 0 to h, w to h, w / 2 to 0, w / 2 to 20, w / 2 to h, 0 to h / 2, w to h / 2)) {
                    assertEquals(expected, pixels.brightness(x, y), 0.01f, "$theme: pixel ($x, $y) isn't dimmed like the rest")
                }
            }
        }
    }

    @Test
    fun tappingTheScrimOutsideThePanelsInsetsDismisses() = runDesktopComposeUiTest(402, 874) {
        var dismissed = 0
        show { Host(ThemeMode.Dark, open = true, onDismiss = { dismissed++ }) }
        onRoot().performTouchInput { click(Offset(width / 2f, 10f)) }
        settle()
        assertEquals(1, dismissed)
    }

    @Test
    fun theNavigationBarIsDimmedLikeTheScrimButStaysTappable() {
        fun shot(open: Boolean, onSelect: (Int) -> Unit = {}): Pair<PixelMap, Offset> {
            lateinit var result: Pair<PixelMap, Offset>
            runDesktopComposeUiTest(402, 874) {
                show { Host(ThemeMode.Light, open, onSelect = onSelect) }
                val bounds = onNodeWithText("Map", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                result = captureToImage().toPixelMap() to bounds.center
                if (open) onNodeWithText("Settings").performClick()
                settle()
            }
            return result
        }
        var selected = -1
        val (dimmed, point) = shot(open = true, onSelect = { selected = it })
        val (clear, _) = shot(open = false)
        assertEquals(2, selected, "The bar must still switch tabs with a dialog open")
        // The label of the unselected "Map" tab sits on plain glass in the bar; sample just left of the text.
        val x = point.x.toInt() - 30
        val y = point.y.toInt()
        val ratio = dimmed.brightness(x, y) / clear.brightness(x, y)
        assertTrue(abs(ratio - 0.8f) < 0.02f, "The bar should be dimmed like the scrim, but its brightness changed by $ratio")
    }
}
