package com.gusak.lab.radarnebo.ui

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.awt.Taskbar
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO

/**
 * Gives a desktop app its name and icon instead of the JVM's ("MainKt" and the Java cup): the macOS app
 * menu and Dock, the Windows/Linux taskbar. Call first thing in `main()`, before any window opens. Returns the
 * icon for `Window(icon = …)`, or null when [iconResource] is missing. The Gradle builds also pass
 * `-Xdock:name`/`-Xdock:icon`, so macOS shows them from launch rather than once this runs.
 */
fun applyAppIdentity(name: String, iconResource: String): Painter? {
    System.setProperty("apple.awt.application.name", name)
    val bytes = Thread.currentThread().contextClassLoader.getResourceAsStream(iconResource)
        ?.use { it.readBytes() }
        ?: return null
    setTaskbarIcon(bytes)
    return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}

/** Best effort: a desktop without a taskbar icon, or a security manager that forbids it, keeps the JVM's icon. */
private fun setTaskbarIcon(bytes: ByteArray) {
    try {
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().iconImage = ImageIO.read(ByteArrayInputStream(bytes))
        }
    } catch (_: UnsupportedOperationException) {
        // The platform's taskbar has no icon to set.
    } catch (_: SecurityException) {
        // Not allowed to change the taskbar.
    } catch (_: IOException) {
        // The icon resource is no image ImageIO can read.
    }
}
