package com.gusak.lab.radarnebo.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gusak.lab.radarnebo.App
import com.gusak.lab.radarnebo.data.SettingsRepository
import com.gusak.lab.radarnebo.debug.TrafficLog
import com.gusak.lab.radarnebo.ui.RadarTheme
import com.gusak.lab.radarnebo.ui.applyAppIdentity
import com.gusak.lab.radarnebo.ui.debug.RequestInspectorScreen
import com.gusak.lab.radarnebo.di.desktopModule
import com.gusak.lab.radarnebo.di.initKoin
import com.gusak.lab.radarnebo.di.trafficLogModule
import com.gusak.lab.radarnebo.ui.map.LocalPinchZoom
import com.gusak.lab.radarnebo.ui.rememberWindowPinch
import io.ktor.client.plugins.logging.Logger
import org.slf4j.LoggerFactory
import java.io.File

fun main() {
    // Taskbar inside applyAppIdentity starts AWT, and sun.java2d.uiScale is ignored after that.
    applyLinuxDesktopScale()
    val icon = applyAppIdentity("RadarNebo", "radarnebo.png")
    // Compose Desktop jumps a whole step per mouse-wheel notch unless smooth scrolling is enabled;
    // this animates wheel scrolls in lists (Drones, Favorites, Settings). Must be set before any UI.
    System.setProperty("compose.scrolling.smooth.enabled", "true")
    // -Dradarnebo.settings=/path/file.properties overrides the default ~/.radarnebo/settings.properties.
    // LOG_HTTP=true logs every request to the server and its JSON response (see logback.xml).
    val httpLogger = if (System.getenv("LOG_HTTP").toBoolean()) {
        val log = LoggerFactory.getLogger("http")
        object : Logger {
            override fun log(message: String) = log.info(message)
        }
    } else {
        null
    }
    // Only Gradle's development runs set this (see build.gradle.kts), so a packaged app has no request inspector.
    val debugBuild = System.getProperty("radarnebo.debug").toBoolean()
    val koin = initKoin(desktopModule(System.getProperty("radarnebo.settings")?.let(::File), httpLogger)) {
        if (debugBuild) modules(trafficLogModule())
    }.koin
    openWindows(icon, koin.getOrNull(), koin.get())
}

private fun openWindows(
    icon: Painter?,
    traffic: TrafficLog?,
    settings: SettingsRepository,
) {
    application {
        var requestsOpen by remember { mutableStateOf(false) }
        Window(
            onCloseRequest = ::exitApplication,
            title = "RadarNebo",
            icon = icon,
            state = rememberWindowState(width = 1100.dp, height = 820.dp),
        ) {
            CompositionLocalProvider(LocalPinchZoom provides rememberWindowPinch(window.rootPane)) {
                App(onOpenRequests = if (traffic != null) ({ requestsOpen = true }) else null)
            }
        }
        if (requestsOpen && traffic != null) {
            Window(
                onCloseRequest = { requestsOpen = false },
                title = "RadarNebo · Requests",
                icon = icon,
                state = rememberWindowState(width = 1100.dp, height = 760.dp),
            ) {
                val exchanges by traffic.exchanges.collectAsStateWithLifecycle()
                val appSettings by settings.settings.collectAsStateWithLifecycle()
                RadarTheme(appSettings.themeMode, appSettings.colorPalette) {
                    RequestInspectorScreen(exchanges, onClear = traffic::clear)
                }
            }
        }
    }
}
