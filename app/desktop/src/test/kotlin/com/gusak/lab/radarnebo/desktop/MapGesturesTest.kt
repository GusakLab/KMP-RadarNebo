package com.gusak.lab.radarnebo.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import com.gusak.lab.radarnebo.ui.MacTrackpad
import com.gusak.lab.radarnebo.ui.MapScreen
import com.gusak.lab.radarnebo.ui.RadarTheme
import com.gusak.lab.radarnebo.ui.UiState
import com.gusak.lab.radarnebo.ui.map.LocalPinchZoom
import com.gusak.lab.radarnebo.ui.map.MapCamera
import com.gusak.lab.radarnebo.ui.map.ScrollSmoother
import com.gusak.lab.radarnebo.ui.map.UkraineMap
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.skia.Image
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class MapGesturesTest {

    private fun Image.pixels(): IntArray {
        val bitmap = org.jetbrains.skia.Bitmap.makeFromImage(this)
        return IntArray(width * height) { bitmap.getColor(it % width, it / width) }
    }

    private fun changedFraction(a: IntArray, b: IntArray) = a.indices.count { a[it] != b[it] }.toDouble() / a.size

    @Test
    fun twoFingerScrollPansAndPinchZoomsTheMap() {
        val pinch = MutableSharedFlow<Float>(extraBufferCapacity = 8)
        ImageComposeScene(width = 1000, height = 800, density = Density(1f)) {
            CompositionLocalProvider(LocalPinchZoom provides pinch) {
                RadarTheme { MapScreen(UiState(), onModeChange = {}, onAddFavorite = { _, _ -> }) }
            }
        }.use { scene ->
            var t = 0L
            fun frame() = scene.render(t).also { t += 16_000_000L }
            repeat(3) { frame() }
            val before = frame().pixels()

            // Trackpad two-finger swipe: Compose delivers it as a scroll event.
            scene.sendPointerEvent(PointerEventType.Enter, Offset(500f, 500f))
            scene.sendPointerEvent(PointerEventType.Scroll, Offset(500f, 500f), scrollDelta = Offset(6f, 3f))
            repeat(30) { frame() }
            val panned = frame().pixels()
            assertTrue(changedFraction(before, panned) > 0.05, "scroll should move the map")

            // macOS trackpad pinch, delivered through LocalPinchZoom.
            assertTrue(pinch.tryEmit(1.8f))
            repeat(3) { frame() }
            val zoomed = frame().pixels()
            assertTrue(changedFraction(panned, zoomed) > 0.05, "pinch should zoom the map")
        }
    }

    @Test
    fun aBurstOfPinchEventsWithinOneFrameZoomsByAllOfThem() {
        // The macOS trackpad reports a pinch as many small steps, several per frame.
        val pinch = MutableSharedFlow<Float>(extraBufferCapacity = 64)
        var camera by mutableStateOf(MapCamera.Overview)
        ImageComposeScene(width = 1000, height = 800, density = Density(1f)) {
            CompositionLocalProvider(LocalPinchZoom provides pinch) {
                RadarTheme {
                    UkraineMap(
                        camera = camera, threats = emptyList(), alertedOblasts = emptyMap(), favorites = emptyList(),
                        alertedFavoriteIds = emptySet(), proximityRadiusKm = 50, modifier = Modifier.fillMaxSize(),
                        onCameraChange = { camera = it },
                    )
                }
            }
        }.use { scene ->
            var t = 0L
            fun frame() = scene.render(t).also { t += 16_000_000L }
            repeat(3) { frame() }
            repeat(10) { assertTrue(pinch.tryEmit(1.05f)) }
            repeat(3) { frame() }
            assertEquals(1.05.pow(10).toFloat(), camera.zoom, 1e-3f, "every step counts")
        }
    }

    /** A bare map driven by touch: [touch] sends one event at a time, [camera] is where the map ended up. */
    private class TouchMap(start: MapCamera) {
        var camera by mutableStateOf(start)
        private var timeMs = 0L
        val scene = ImageComposeScene(width = 1000, height = 800, density = Density(1f)) {
            RadarTheme {
                UkraineMap(
                    camera = camera, threats = emptyList(), alertedOblasts = emptyMap(), favorites = emptyList(),
                    alertedFavoriteIds = emptySet(), proximityRadiusKm = 50, modifier = Modifier.fillMaxSize(),
                    onCameraChange = { camera = it },
                )
            }
        }

        /** Sends [type] at [x], [y] after [afterMs], then renders a frame so the gesture detectors run. */
        fun touch(type: PointerEventType, x: Float, y: Float, afterMs: Long = 16) {
            timeMs += afterMs
            scene.sendPointerEvent(type, Offset(x, y), timeMillis = timeMs, type = PointerType.Touch)
            scene.render(timeMs * 1_000_000)
        }

        /** A quick tap, then a second touch at the same spot that stays down. */
        fun doubleTapAndHold(x: Float, y: Float) {
            touch(PointerEventType.Press, x, y)
            touch(PointerEventType.Release, x, y, afterMs = 60)
            touch(PointerEventType.Press, x, y, afterMs = 100)
        }
    }

    private fun touchMap(start: MapCamera, test: TouchMap.() -> Unit) {
        val map = TouchMap(start)
        map.scene.use {
            repeat(3) { map.touch(PointerEventType.Move, 0f, 0f) }
            map.test()
        }
    }

    @Test
    fun doubleTapAndDragDownZoomsInAroundTheTappedSpot() = touchMap(MapCamera.Overview) {
        val tapped = Offset(400f, 300f)
        val spot = camera.toWorld(tapped, Size(1000f, 800f))

        doubleTapAndHold(tapped.x, tapped.y)
        touch(PointerEventType.Move, tapped.x, 330f) // past the touch slop: the drag starts here
        touch(PointerEventType.Move, tapped.x, 450f)
        touch(PointerEventType.Move, tapped.x, 570f)
        touch(PointerEventType.Release, tapped.x, 570f)

        assertEquals(4f, camera.zoom, 1e-3f, "240 px down doubles the zoom twice")
        val after = camera.toWorld(tapped, Size(1000f, 800f))
        assertTrue(abs(after.x - spot.x) < 1e-3f && abs(after.y - spot.y) < 1e-3f, "the tapped spot stays put")
    }

    @Test
    fun doubleTapAndDragUpZoomsOut() = touchMap(MapCamera.Overview.copy(zoom = 4f)) {
        doubleTapAndHold(500f, 400f)
        touch(PointerEventType.Move, 500f, 370f)
        touch(PointerEventType.Move, 500f, 250f)
        touch(PointerEventType.Release, 500f, 250f)

        assertEquals(2f, camera.zoom, 1e-3f)
    }

    @Test
    fun aDoubleTapWithoutDraggingStillReturnsToTheOverview() = touchMap(MapCamera.Overview.copy(zoom = 3f)) {
        doubleTapAndHold(500f, 400f)
        touch(PointerEventType.Release, 500f, 400f, afterMs = 60)

        assertEquals(MapCamera.Overview, camera)
    }

    @Test
    fun aSingleFingerDragStillPansWithoutZooming() = touchMap(MapCamera.Overview.copy(zoom = 2f)) {
        val start = camera

        touch(PointerEventType.Press, 500f, 400f)
        touch(PointerEventType.Move, 500f, 450f)
        touch(PointerEventType.Move, 500f, 550f)
        touch(PointerEventType.Release, 500f, 550f)

        assertEquals(start.zoom, camera.zoom)
        assertTrue(camera.center != start.center, "the drag moved the map")
    }

    private fun smoother(applied: MutableList<Offset>) = ScrollSmoother(scope = null) { pan, _, _ -> applied += pan }

    @Test
    fun scrollIsEasedOverFramesInsteadOfJumping() {
        val applied = mutableListOf<Offset>()
        val s = smoother(applied)
        s.pan(Offset(0f, -120f))
        s.step(1 / 60f)
        assertTrue(
            abs(applied.last().y) in 10f..60f,
            "first frame applies only part of the scroll, got ${applied.last()}",
        )
        var frames = 1
        while (s.step(1 / 60f)) frames++
        assertTrue(frames in 5..40, "glide settles within a fraction of a second, took $frames frames")
        assertEquals(-120f, applied.fold(0f) { acc, o -> acc + o.y }, 1e-3f)
    }

    @Test
    fun scrollDuringGlideAccumulates() {
        val applied = mutableListOf<Offset>()
        val s = smoother(applied)
        s.pan(Offset(0f, -50f))
        s.step(1 / 60f)
        s.pan(Offset(0f, -50f))
        do {
            val gliding = s.step(1 / 60f)
        } while (gliding)
        assertEquals(-100f, applied.fold(0f) { acc, o -> acc + o.y }, 1e-3f)
    }

    @Test
    fun scrollPanMovesCameraByExactlyTheScrolledDistance() {
        val size = Size(1000f, 800f)
        val camera = MapCamera.Overview
        val moved = camera.transformed(Offset(500f, 400f), Offset(-96f, 0f), 1f, size, MapCamera.MAX_ZOOM)
        // Content follows the fingers: a 96 px swipe moves the camera centre 96 px worth of world units.
        assertEquals(96f / camera.scale(size), moved.center.x - camera.center.x, 1e-4f)
        assertEquals(camera.zoom, moved.zoom)
    }

    @Test
    fun pinchZoomKeepsThePointUnderTheCursorFixed() {
        val size = Size(1000f, 800f)
        val camera = MapCamera.Overview
        val cursor = Offset(700f, 300f)
        val anchor = camera.toWorld(cursor, size)
        val zoomed = camera.transformed(cursor, Offset.Zero, 1.5f, size, MapCamera.MAX_ZOOM)
        assertEquals(1.5f, zoomed.zoom, 1e-4f)
        val after = zoomed.toWorld(cursor, size)
        assertTrue(abs(after.x - anchor.x) < 1e-4f && abs(after.y - anchor.y) < 1e-4f)
    }

    @Test
    fun macTrackpadBridgeInstallsOnMacJdk() {
        val onMac = System.getProperty("os.name").lowercase().contains("mac")
        assertEquals(onMac, MacTrackpad.installPinch(JPanel()) { })
    }
}
