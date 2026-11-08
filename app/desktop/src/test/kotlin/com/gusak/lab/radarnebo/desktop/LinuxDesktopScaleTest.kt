package com.gusak.lab.radarnebo.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinuxDesktopScaleTest {
    @Test
    fun aGdkScaleOf2Is2() {
        val reading = LinuxScaleReading(
            osName = "Linux",
            gdkScale = 2.0,
            xftDpi = 192.0,
            mutterScale = 1.25,
            waylandDisplay = "wayland-0",
        )

        val scale = linuxDesktopScale(reading)

        assertEquals(2.0, scale)
    }

    @Test
    fun aGdkScaleOf2TimesDpiScaleOf0625Is125() {
        val reading = LinuxScaleReading(
            osName = "Linux",
            gdkScale = 2.0,
            gdkDpiScale = 0.625,
            xftDpi = 192.0,
            mutterScale = 1.25,
        )

        val scale = linuxDesktopScale(reading)

        assertEquals(1.25, scale)
    }

    @Test
    fun xWaylandWithXftDpi192AndMutterScale125Is2() {
        val reading = LinuxScaleReading(
            osName = "Linux",
            waylandDisplay = "wayland-0",
            xftDpi = 192.0,
            mutterScale = 1.25,
        )

        val scale = linuxDesktopScale(reading)

        assertEquals(2.0, scale)
    }

    @Test
    fun nativeWaylandWithXftDpi192AndMutterScale125Is125() {
        val reading = LinuxScaleReading(
            osName = "Linux",
            toolkitName = "WLToolkit",
            waylandDisplay = "wayland-0",
            xftDpi = 192.0,
            mutterScale = 1.25,
            qtScaleFactor = 2.0,
        )

        val scale = linuxDesktopScale(reading)

        assertEquals(1.25, scale)
    }

    @Test
    fun x11WithXftDpi144Is15() {
        val reading = LinuxScaleReading(osName = "Linux", sessionType = "x11", xftDpi = 144.0)

        val scale = linuxDesktopScale(reading)

        assertEquals(1.5, scale)
    }

    @Test
    fun x11WithNoDpiAndAScalingFactorOf2Is2() {
        val reading = LinuxScaleReading(
            osName = "Linux",
            sessionType = "x11",
            gnomeScalingFactor = 2,
            qtScaleFactor = 1.5,
        )

        val scale = linuxDesktopScale(reading)

        assertEquals(2.0, scale)
    }

    @Test
    fun anXftDpiOf96LeavesTheScaleUnset() {
        val reading = LinuxScaleReading(
            osName = "Linux",
            sessionType = "x11",
            xftDpi = 96.0,
            gnomeScalingFactor = 2,
        )

        val scale = linuxDesktopScale(reading)

        assertNull(scale)
    }

    @Test
    fun aScaleAbove4LeavesTheScaleUnset() {
        val reading = LinuxScaleReading(osName = "Linux", gdkScale = 5.0)

        val scale = linuxDesktopScale(reading)

        assertNull(scale)
    }

    @Test
    fun aScaleOf4IsApplied() {
        val reading = LinuxScaleReading(osName = "Linux", gdkScale = 4.0)

        val scale = linuxDesktopScale(reading)

        assertEquals(4.0, scale)
    }

    @Test
    fun anExistingUiScaleLeavesTheScaleUnset() {
        val reading = LinuxScaleReading(osName = "Linux", uiScaleProperty = "2", gdkScale = 2.0)

        val scale = linuxDesktopScale(reading)

        assertNull(scale)
    }

    @Test
    fun anExistingJ2dUiScaleLeavesTheScaleUnset() {
        val reading = LinuxScaleReading(osName = "Linux", j2dUiScale = "2", gdkScale = 2.0)

        val scale = linuxDesktopScale(reading)

        assertNull(scale)
    }

    @Test
    fun uiScaleEnabledFalseLeavesTheScaleUnset() {
        val reading = LinuxScaleReading(osName = "Linux", uiScaleEnabled = "false", gdkScale = 2.0)

        val scale = linuxDesktopScale(reading)

        assertNull(scale)
    }

    @Test
    fun macOsLeavesTheScaleUnset() {
        val reading = LinuxScaleReading(osName = "Mac OS X", gdkScale = 2.0)

        val scale = linuxDesktopScale(reading)

        assertNull(scale)
    }

    @Test
    fun xWaylandWithoutDpiUsesFramebufferTimesMutterOverMode() {
        val reading = LinuxScaleReading(
            osName = "Linux",
            sessionType = "wayland",
            mutterScale = 1.25,
            xrandrCurrentWidthPx = 6144,
            monitorModeWidthPx = 3840,
        )

        val scale = linuxDesktopScale(reading)

        assertEquals(2.0, scale)
    }

    @Test
    fun xWaylandWithoutDpiOrGeometryLeavesTheScaleUnset() {
        val reading = LinuxScaleReading(
            osName = "Linux",
            waylandDisplay = "wayland-0",
            gnomeScalingFactor = 2,
            qtScaleFactor = 2.0,
        )

        val scale = linuxDesktopScale(reading)

        assertNull(scale)
    }

    @Test
    fun x11WithAScalingFactorOf0UsesQtScaleFactor() {
        val reading = LinuxScaleReading(
            osName = "Linux",
            sessionType = "x11",
            gnomeScalingFactor = 0,
            qtScaleFactor = 1.5,
        )

        val scale = linuxDesktopScale(reading)

        assertEquals(1.5, scale)
    }

    @Test
    fun nativeWaylandWithoutMutterUsesQtScaleBeforeGnome() {
        val reading = LinuxScaleReading(
            osName = "Linux",
            toolkitName = "WLToolkit",
            qtScaleFactor = 1.5,
            gnomeScalingFactor = 2,
        )

        val scale = linuxDesktopScale(reading)

        assertEquals(1.5, scale)
    }

    @Test
    fun xrdbQueryYieldsXftDpi() {
        val xrdb = """
            Xft.antialias:	1
            Xft.dpi:	192
            Xft.hinting:	1
        """.trimIndent()

        val dpi = xftDpiOf(xrdb)

        assertEquals(192.0, dpi)
    }

    @Test
    fun xrandrCurrentWidthIsTheFramebuffer() {
        val xrandr = """
            Screen 0: minimum 16 x 16, current 6144 x 3456, maximum 32767 x 32767
               3840x2160     59.98*+
               6144x3456     59.97
        """.trimIndent()

        val widthPx = xrandrCurrentWidthPx(xrandr)

        assertEquals(6144, widthPx)
    }

    @Test
    fun mutterDisplayConfigYieldsThePrimaryScaleAndCurrentMode() {
        val displayConfig = """
            [('3200x1800@59.956', 3200, 1800, 59.95, 1.0, [1.0], {}),
             ('3840x2160@59.997', 3840, 2160, 59.996, 1.25, [1.0, 1.25], {'is-current': <true>})]
            [(100, 0, 2.0, uint32 0, false, [('DP-2', 'A', 'B', 'C')], @a{sv} {}),
             (0, 0, 1.25, uint32 0, true, [('DP-1', 'DEL', 'DELL', 'X')], @a{sv} {})]
        """.trimIndent()

        val scale = mutterDisplayScale(displayConfig)
        val modeWidthPx = mutterCurrentModeWidthPx(displayConfig)

        assertEquals(1.25, scale)
        assertEquals(3840, modeWidthPx)
    }

    @Test
    fun gsettingsUint32IsTheScalingFactor() {
        val unset = gnomeScalingFactorOf("uint32 0")
        val doubled = gnomeScalingFactorOf("uint32 2")

        assertEquals(0, unset)
        assertEquals(2, doubled)
    }
}
