package com.gusak.lab.radarnebo.desktop

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/** [gnomeScalingFactor] of 0 is GNOME's automatic setting, not a scale of 0. */
internal data class LinuxScaleReading(
    val osName: String,
    val uiScaleProperty: String? = null,
    val j2dUiScale: String? = null,
    val uiScaleEnabled: String? = null,
    val toolkitName: String? = null,
    val waylandDisplay: String? = null,
    val sessionType: String? = null,
    val gdkScale: Double? = null,
    val gdkDpiScale: Double? = null,
    val xftDpi: Double? = null,
    val mutterScale: Double? = null,
    val qtScaleFactor: Double? = null,
    val gnomeScalingFactor: Int? = null,
    val xrandrCurrentWidthPx: Int? = null,
    val monitorModeWidthPx: Int? = null,
)

internal fun linuxDesktopScale(reading: LinuxScaleReading): Double? {
    if (leavesScaleUnset(reading)) return null
    val scale = gtkScale(reading) ?: windowScale(reading) ?: return null
    return scale.takeIf { it > 1.0 && it <= MAX_UI_SCALE }
}

internal fun applyLinuxDesktopScale() {
    val scale = linuxDesktopScale(currentLinuxScaleReading()) ?: return
    System.setProperty("sun.java2d.uiScale", scale.toString())
}

internal fun xftDpiOf(xrdbQuery: String): Double? =
    XFT_DPI.find(xrdbQuery)?.groupValues?.get(1)?.toDoubleOrNull()

/** The framebuffer width from `current <width> x`, not a listed monitor mode. */
internal fun xrandrCurrentWidthPx(xrandr: String): Int? =
    XRANDR_CURRENT_WIDTH.find(xrandr)?.groupValues?.get(1)?.toIntOrNull()

internal fun mutterDisplayScale(displayConfig: String): Double? {
    val monitors = LOGICAL_MONITOR.findAll(displayConfig).toList()
    val monitor = monitors.firstOrNull { it.groupValues[4] == "true" } ?: monitors.firstOrNull()
    return monitor?.groupValues?.get(3)?.toDoubleOrNull()
}

/** Panel width of the mode Mutter marks current. XWayland's xrandr current mode is the scaled buffer. */
internal fun mutterCurrentModeWidthPx(displayConfig: String): Int? =
    CURRENT_MODE_WIDTH.find(displayConfig)?.groupValues?.get(2)?.toIntOrNull()

internal fun gnomeScalingFactorOf(gsettings: String): Int? =
    GNOME_SCALING_FACTOR.find(gsettings)?.groupValues?.get(1)?.toIntOrNull()

private fun leavesScaleUnset(reading: LinuxScaleReading): Boolean =
    !reading.osName.startsWith("Linux") ||
        reading.uiScaleProperty != null ||
        reading.j2dUiScale != null ||
        reading.uiScaleEnabled.equals("false", ignoreCase = true)

private fun gtkScale(reading: LinuxScaleReading): Double? {
    val gdkScale = reading.gdkScale ?: return null
    return gdkScale * (reading.gdkDpiScale ?: 1.0)
}

private fun windowScale(reading: LinuxScaleReading): Double? =
    when (desktopWindowSystem(reading)) {
        DesktopWindowSystem.NativeWayland -> nativeWaylandScale(reading)
        DesktopWindowSystem.XWayland -> xWaylandScale(reading)
        DesktopWindowSystem.X11 -> x11Scale(reading)
    }

private fun desktopWindowSystem(reading: LinuxScaleReading): DesktopWindowSystem = when {
    reading.toolkitName == WL_TOOLKIT -> DesktopWindowSystem.NativeWayland
    !reading.waylandDisplay.isNullOrBlank() || reading.sessionType == WAYLAND_SESSION ->
        DesktopWindowSystem.XWayland
    else -> DesktopWindowSystem.X11
}

private fun nativeWaylandScale(reading: LinuxScaleReading): Double? =
    reading.mutterScale
        ?: reading.qtScaleFactor
        ?: reading.gnomeScalingFactor?.takeIf { it > 0 }?.toDouble()

private fun xWaylandScale(reading: LinuxScaleReading): Double? {
    reading.xftDpi?.let { return it / XFT_DPI_PER_SCALE }
    val mutterScale = reading.mutterScale ?: return null
    val currentWidthPx = reading.xrandrCurrentWidthPx ?: return null
    val modeWidthPx = reading.monitorModeWidthPx ?: return null
    if (currentWidthPx <= 0 || modeWidthPx <= 0) return null
    return currentWidthPx.toDouble() * mutterScale / modeWidthPx.toDouble()
}

private fun x11Scale(reading: LinuxScaleReading): Double? =
    reading.xftDpi?.div(XFT_DPI_PER_SCALE)
        ?: reading.gnomeScalingFactor?.takeIf { it > 0 }?.toDouble()
        ?: reading.qtScaleFactor

private fun currentLinuxScaleReading(): LinuxScaleReading {
    val reading = LinuxScaleReading(
        osName = System.getProperty("os.name") ?: "",
        uiScaleProperty = System.getProperty("sun.java2d.uiScale"),
        j2dUiScale = System.getenv("J2D_UISCALE"),
        uiScaleEnabled = System.getProperty("sun.java2d.uiScale.enabled"),
        toolkitName = System.getProperty("awt.toolkit.name"),
        waylandDisplay = System.getenv("WAYLAND_DISPLAY"),
        sessionType = System.getenv("XDG_SESSION_TYPE"),
        gdkScale = envDouble("GDK_SCALE"),
        gdkDpiScale = envDouble("GDK_DPI_SCALE"),
        qtScaleFactor = envDouble("QT_SCALE_FACTOR"),
    )
    if (leavesScaleUnset(reading) || System.getenv("GDK_SCALE") != null) return reading
    return readDisplay(reading)
}

private fun readDisplay(reading: LinuxScaleReading): LinuxScaleReading {
    val displayConfig = readCommandOutput(MUTTER_STATE_COMMAND)
    return reading.copy(
        xftDpi = readCommandOutput(listOf("xrdb", "-query"))?.let(::xftDpiOf),
        mutterScale = displayConfig?.let(::mutterDisplayScale),
        monitorModeWidthPx = displayConfig?.let(::mutterCurrentModeWidthPx),
        gnomeScalingFactor = readCommandOutput(GNOME_SCALING_COMMAND)?.let(::gnomeScalingFactorOf),
        xrandrCurrentWidthPx = readCommandOutput(listOf("xrandr"))?.let(::xrandrCurrentWidthPx),
    )
}

private fun envDouble(name: String): Double? = System.getenv(name)?.trim()?.toDoubleOrNull()

private fun readCommandOutput(command: List<String>): String? {
    val process = try {
        ProcessBuilder(command)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (_: IOException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    try {
        if (!process.waitFor(COMMAND_WAIT.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
            process.destroy()
            process.destroyForcibly()
            return null
        }
        if (process.exitValue() != 0) return null
        return process.inputStream.bufferedReader().readText()
    } catch (_: IOException) {
        return null
    } catch (_: SecurityException) {
        return null
    } catch (_: InterruptedException) {
        process.destroyForcibly()
        Thread.currentThread().interrupt()
        return null
    } finally {
        runCatching { process.inputStream.close() }
        runCatching { process.outputStream.close() }
    }
}

private enum class DesktopWindowSystem {
    NativeWayland,
    XWayland,
    X11,
}

private const val WL_TOOLKIT: String = "WLToolkit"
private const val WAYLAND_SESSION: String = "wayland"
private const val XFT_DPI_PER_SCALE: Double = 96.0
private const val MAX_UI_SCALE: Double = 4.0
private val COMMAND_WAIT = 2.seconds

private val MUTTER_STATE_COMMAND: List<String> = listOf(
    "gdbus",
    "call",
    "--session",
    "--dest",
    "org.gnome.Mutter.DisplayConfig",
    "--object-path",
    "/org/gnome/Mutter/DisplayConfig",
    "--method",
    "org.gnome.Mutter.DisplayConfig.GetCurrentState",
)

private val GNOME_SCALING_COMMAND: List<String> = listOf(
    "gsettings",
    "get",
    "org.gnome.desktop.interface",
    "scaling-factor",
)

private val XFT_DPI: Regex = Regex("""(?m)^Xft\.dpi:\s*(\d+(?:\.\d+)?)""")
private val XRANDR_CURRENT_WIDTH: Regex = Regex("""current\s+(\d+)\s+x""")
private val LOGICAL_MONITOR: Regex = Regex(
    """\(\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(\d+(?:\.\d+)?)\s*,\s*uint32\s+\d+\s*,\s*(true|false)""",
)
private val CURRENT_MODE_WIDTH: Regex = Regex(
    """\('(\d+)x\d+@[^']*',\s*(\d+),\s*\d+,[^)]*?is-current':\s*<true>""",
)
private val GNOME_SCALING_FACTOR: Regex = Regex("""(?:uint32\s+)?(\d+)""")
