package com.gusak.lab.radarnebo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.gusak.lab.radarnebo.ui.map.MapTiles
import com.gusak.lab.radarnebo.ui.map.TileFrame
import com.gusak.lab.radarnebo.ui.map.TileSource
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.mp.KoinPlatform
import java.io.File
import kotlin.math.abs

/**
 * Captures the five store screenshots into the app's external files, for tools/capture-play-screenshots.sh to pull.
 * Skipped unless run with `-e capture true`, so it stays out of the normal UI tests.
 *
 * Tiles load one by one in the background and the UI says nothing about it, so no fixed delay is safe: a large screen on
 * a fresh theme took long enough that the first store captures showed a mostly bare map. Each shot instead waits until
 * the map's own frames report every tile of the view drawn at its proper zoom, then checks the image it got.
 */
@RunWith(AndroidJUnit4::class)
class PlayScreenshots {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val tiles by lazy { KoinPlatform.getKoin().get<MapTiles>() }
    private val out by lazy { File(requireNotNull(context.getExternalFilesDir(null)), "play-captures").apply { mkdirs() } }

    @Before
    fun launchApp() {
        assumeTrue("run with -e capture true", InstrumentationRegistry.getArguments().getString("capture") == "true")
        out.listFiles()?.forEach(File::delete)
        setNightMode(true)
        val intent = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        need(By.text("Settings"))
    }

    @Test
    fun capture() {
        val dark = TileSource.DarkStreets
        shot(1, dark)

        need(By.text("Favorites")).click()
        need(By.text("Favorite places"))
        // A fresh install has none, and "Kyiv" is then a quick-add button.
        if (!device.hasObject(By.text("Remove"))) need(By.text("Kyiv")).click()
        need(By.text("Remove"))
        shot(2, dark)
        need(By.desc("Close")).click()
        device.wait(Until.gone(By.text("Favorite places")), TIMEOUT)

        need(By.desc("Enter cinematic mode")).click()
        need(By.desc("Exit cinematic mode"))
        shot(3, dark)
        need(By.desc("Exit cinematic mode")).click()
        need(By.desc("Enter cinematic mode"))

        setNightMode(false)
        shot(4, TileSource.LightStreets)

        setNightMode(true)
        // The theme change recreates the activity, which would leave a button found too early stale.
        awaitMap(dark)
        need(By.text("Settings")).click()
        need(By.text("APPEARANCE"))
        shot(5, dark)
    }

    private fun setNightMode(night: Boolean) {
        device.executeShellCommand("cmd uimode night ${if (night) "yes" else "no"}")
    }

    /** Takes shot [number] once the map is complete, again if the map moved on to new tiles during the capture. */
    private fun shot(number: Int, source: TileSource) {
        val file = File(out, "$number.png")
        repeat(MAX_SHOT_TRIES) {
            val waited = awaitMap(source)
            assertTrue("Screenshot $number failed", device.takeScreenshot(file))
            val frame = tiles.lastFrame
            if (frame.isComplete(source)) {
                Log.i(TAG, "Shot $number after ${waited} ms: $frame")
                assertMapCovers(file, number, source)
                return
            }
            Log.i(TAG, "Shot $number retaken, the map changed: $frame")
        }
        fail("Shot $number: the map kept changing, last ${tiles.lastFrame}")
    }

    /** Waits for [source]'s frames to report no tile outstanding, without a break for [SETTLE]; returns the time taken. */
    private fun awaitMap(source: TileSource): Long {
        val start = SystemClock.elapsedRealtime()
        var completeSince = 0L
        while (SystemClock.elapsedRealtime() - start < MAP_TIMEOUT) {
            val now = SystemClock.elapsedRealtime()
            if (tiles.lastFrame.isComplete(source)) {
                if (completeSince == 0L) completeSince = now
                if (now - completeSince >= SETTLE) return completeSince - start
            } else {
                completeSince = 0L
            }
            SystemClock.sleep(POLL)
        }
        fail("The map didn't finish loading in $MAP_TIMEOUT ms, last ${tiles.lastFrame}")
        return 0
    }

    private fun TileFrame?.isComplete(source: TileSource) =
        this != null && this.source == source && drawn > 0 && missing == 0 && absent == 0

    /**
     * An independent check on the image itself: wherever a tile is missing the theme's bare map background shows, and
     * tiles never use that exact colour. The bad store captures were more than half this, the good ones under 1%.
     */
    private fun assertMapCovers(file: File, number: Int, source: TileSource) {
        val bare = if (source == TileSource.LightStreets) BARE_LIGHT else BARE_DARK
        val image: Bitmap = requireNotNull(BitmapFactory.decodeFile(file.path)) { "Can't read $file" }
        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        val share = pixels.count { pixel ->
            abs((pixel shr 16 and 0xFF) - (bare shr 16 and 0xFF)) <= 1 &&
                abs((pixel shr 8 and 0xFF) - (bare shr 8 and 0xFF)) <= 1 &&
                abs((pixel and 0xFF) - (bare and 0xFF)) <= 1
        }.toFloat() / pixels.size
        Log.i(TAG, "Shot $number: ${image.width}x${image.height}, bare map ${share * 100}%")
        assertTrue("Shot $number shows ${share * 100}% bare map background", share < MAX_BARE)
    }

    private fun need(selector: BySelector): UiObject2 {
        val found = device.wait(Until.findObject(selector), TIMEOUT)
        assertNotNull("Not on screen: $selector", found)
        return found
    }

    private companion object {
        const val TAG = "PlayScreenshots"
        const val TIMEOUT = 15_000L
        const val MAP_TIMEOUT = 60_000L
        const val POLL = 50L

        // Long enough for a dialog's entrance and the last tile to reach the screen.
        const val SETTLE = 1_500L
        const val MAX_SHOT_TRIES = 3
        const val MAX_BARE = 0.05f

        // colors.mapBackground of the Radar palette (Theme.kt).
        const val BARE_DARK = 0x070D14
        const val BARE_LIGHT = 0xE9EEF3
    }
}
