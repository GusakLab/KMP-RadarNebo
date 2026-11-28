package com.gusak.lab.radarnebo

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.gusak.lab.radarnebo.data.KeyValueStorage
import com.gusak.lab.radarnebo.data.WidgetOfferSchedule
import com.gusak.lab.radarnebo.widget.TargetsWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.mp.KoinPlatform
import java.util.regex.Pattern

/**
 * Drives the installed app from the outside. The map animates without end, so the Compose test rule would never see
 * the UI go idle; UI Automator reads the accessibility tree instead. Needs no server: every assertion holds offline.
 */
@RunWith(AndroidJUnit4::class)
class AppUiTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val packageName = ApplicationProvider.getApplicationContext<Context>().packageName

    private val storage by lazy { KoinPlatform.getKoin().get<KeyValueStorage>() }

    @Before
    fun launchApp() {
        // The map animates without end, so the screen never goes idle: every query would wait the full default 10 s.
        Configurator.getInstance().waitForIdleTimeout = IDLE_TIMEOUT
        // The launcher's widget pin dialog would cover the screens under test; one test brings it back.
        WidgetOfferSchedule(storage).stop()
        launch()
        need(By.text("Settings"))
    }

    private fun launch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = requireNotNull(context.packageManager.getLaunchIntentForPackage(packageName))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        need(By.pkg(packageName).depth(0))
    }

    @Test
    fun theWidgetOfferIsMadeOnFirstLaunchOnly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // With a widget already on the home screen the offer rightly stays away; there is nothing to test then.
        assumeTrue("a widget is on the home screen", TargetsWidget.widgetIds(context).isEmpty())
        val schedule = WidgetOfferSchedule(storage)
        schedule.reset()
        launch()
        // The launcher's pin dialog is not ours to drive; Back dismisses whatever it looks like.
        device.pressBack()
        assertFalse("The next offer waits a week", schedule.isDue(System.currentTimeMillis()))
        launch()
        need(By.text("Settings"))
    }

    @Test
    fun launchShowsTheMapAndItsControls() {
        need(By.text("Map"))
        need(By.text("Favorites"))
        need(By.text("Settings"))
        need(By.desc("Enter cinematic mode"))
        need(By.desc("Show tracked targets"))
    }

    @Test
    fun favoritesOpensAndCloses() {
        need(By.text("Favorites")).click()
        need(By.text("Favorite places"))
        need(By.desc("Close")).click()
        assertTrue("Favorites should close", device.wait(Until.gone(By.text("Favorite places")), TIMEOUT))
        need(By.desc("Show tracked targets"))
    }

    @Test
    fun tappingTheScrimCloses() {
        need(By.text("Favorites")).click()
        need(By.text("Favorite places"))
        // Just below the status bar, whose own window takes the touches on it; the panel keeps a margin from there.
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val statusBarId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBar = resources.getDimensionPixelSize(statusBarId)
        device.click(device.displayWidth / 2, statusBar + (6 * resources.displayMetrics.density).toInt())
        assertTrue("Favorites should close", device.wait(Until.gone(By.text("Favorite places")), TIMEOUT))
    }

    @Test
    fun theNavigationBarStaysUsableWithADialogOpen() {
        need(By.text("Favorites")).click()
        need(By.text("Favorite places"))
        need(By.text("Settings")).click()
        need(By.text("APPEARANCE"))
        val favoritesGone = device.wait(Until.gone(By.text("Favorite places")), TIMEOUT)
        assertTrue("Favorites should give way to Settings", favoritesGone)
        // Settings has a "Map" button of its own; the bar's tab is the lowest one.
        need(By.text("Map"))
        device.findObjects(By.text("Map")).maxBy { it.visibleBounds.bottom }.click()
        assertTrue("Settings should close", device.wait(Until.gone(By.text("APPEARANCE")), TIMEOUT))
    }

    @Test
    fun aFavoriteCanBeAddedAndRemoved() {
        val place = "UI test ${System.currentTimeMillis()}"
        need(By.text("Favorites")).click()
        need(By.text("Add by coordinates")).click()

        // The dialog's title has the same text as the button behind it, so wait for its fields instead.
        need(By.text("Cancel"))
        val fields = device.findObjects(By.clazz("android.widget.EditText"))
            .sortedBy { it.visibleBounds.top }
            .takeLast(3)
        assertEquals("name, latitude and longitude fields", 3, fields.size)
        fields[0].text = place
        fields[1].text = "50.45"
        fields[2].text = "30.52"
        need(By.text("Add")).click()

        removeFavorite(place)
    }

    @Test
    fun aCityFoundBySearchCanBeAddedAndRemoved() {
        need(By.text("Favorites")).click()
        val search = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), TIMEOUT).first()
        search.text = "Lviv"
        // "+" only marks a search suggestion, so this can't be satisfied by the text just typed into the field.
        need(By.text("+")).click()
        assertTrue("Picking a suggestion should clear the search", device.wait(Until.gone(By.text("+")), TIMEOUT))
        removeFavorite("Lviv")
    }

    @Test
    fun settingsShowsEverySectionAndChangesTheTheme() {
        need(By.text("Settings")).click()
        need(By.text("APPEARANCE"))
        need(By.text("Theme"))

        for (theme in listOf("Dark", "Light", "System")) {
            tap(By.text(theme))
            // A segmented button is a radio button to accessibility services, so its state is "checked".
            val selected = Until.hasObject(By.checked(true).hasDescendant(By.text(theme)))
            assertTrue("$theme should be selected", device.wait(selected, TIMEOUT))
        }

        for (section in listOf("ALERTS & TRACKING", "HOME SCREEN WIDGET", "TARGET TYPES")) {
            scrollTo(By.text(section))
        }
    }

    @Test
    fun requestInspectorOpensFromSettings() {
        need(By.text("Settings")).click()
        scrollTo(By.text("Open"))
        tap(By.text("Open"))
        assertTrue(
            "RequestInspectorActivity should open",
            device.wait(Until.gone(By.text("Request inspector")), TIMEOUT),
        )
        device.pressBack()
        need(By.text("Settings"))
    }

    @Test
    fun theAppSurvivesARotation() {
        device.setOrientationLandscape()
        try {
            need(By.text("Settings"))
            need(By.text("Favorites")).click()
            need(By.text("Favorite places"))
        } finally {
            device.setOrientationNatural()
            device.unfreezeRotation()
        }
    }

    /** Taps the Remove button on the row showing [place], which may not be the only favorite. */
    private fun removeFavorite(place: String) {
        val row = need(By.text(place)).visibleBounds
        val rowSpan = row.top - ROW_SLOP..row.bottom + ROW_SLOP
        val remove = device.findObjects(By.text("Remove")).first { it.visibleBounds.centerY() in rowSpan }
        remove.click()
        assertTrue("$place should be gone", device.wait(Until.gone(By.text(place)), TIMEOUT))
    }

    private fun need(selector: BySelector): UiObject2 {
        val found = device.wait(Until.findObject(selector), TIMEOUT)
        assertNotNull("Not on screen: $selector", found)
        return found
    }

    /** Taps [selector], looking it up again if a scroll still settling replaces the node in between. */
    private fun tap(selector: BySelector) {
        repeat(MAX_TAP_TRIES) {
            try {
                return need(selector).click()
            } catch (_: StaleObjectException) {
                device.waitForIdle(SETTLE)
            }
        }
        need(selector).click()
    }

    /** Scrolls until [selector] is on screen and stays put: an animated scroll can carry it past. */
    private fun scrollTo(selector: BySelector) {
        repeat(MAX_SCROLLS) {
            val before = boundsOf(selector)
            if (before == null) {
                try {
                    scrollDown()
                } catch (_: StaleObjectException) {
                    // A scroll still settling replaced the node; the next pass looks it up again.
                }
            }
            device.waitForIdle(SETTLE)
            if (before != null && before == boundsOf(selector)) return
        }
        need(selector)
    }

    private fun boundsOf(selector: BySelector): Rect? = try {
        device.findObject(selector)?.visibleBounds
    } catch (_: StaleObjectException) {
        null
    }

    /**
     * Swipes the list up from the lowest label on it, never from its centre: a swipe that starts on a slider moves
     * the slider, and the list stays put.
     */
    private fun scrollDown() {
        val list = device.findObject(By.scrollable(true)) ?: return
        val bounds = list.visibleBounds
        val label = list.findObjects(By.text(Pattern.compile(".+")))
            .map { it.visibleBounds }
            .filter { it.centerY() < bounds.bottom - bounds.height() / 10 }
            .maxByOrNull { it.centerY() }
            ?: return
        // Half a screen at a time, slowly, so the list doesn't fling past what we're looking for.
        val to = (label.centerY() - bounds.height() / 2).coerceAtLeast(bounds.top + bounds.height() / 10)
        device.swipe(label.centerX(), label.centerY(), label.centerX(), to, SWIPE_STEPS)
    }

    private companion object {
        const val TIMEOUT = 10_000L
        const val SETTLE = 500L
        const val MAX_SCROLLS = 30
        const val ROW_SLOP = 150
        const val MAX_TAP_TRIES = 3
        const val SWIPE_STEPS = 100
        const val IDLE_TIMEOUT = 500L
    }
}
