package com.gusak.lab.radarnebo.desktop

import com.gusak.lab.radarnebo.ui.AppEvents
import com.gusak.lab.radarnebo.ui.AppRoute
import com.gusak.lab.radarnebo.ui.UiState
import org.slf4j.LoggerFactory
import java.awt.EventQueue
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

/**
 * Sends [LinuxNoticePlanner] output with notify-send. No dbus library: one command covers it.
 * A click prints `default` because that action id is the notification body. The route is applied
 * on the AWT thread, which is the Compose UI thread.
 */
internal class LinuxNotifications(
    val events: AppEvents,
    private val start: (List<String>) -> Process,
    private val iconPath: String?,
    private val deliver: (AppRoute) -> Unit = { route -> EventQueue.invokeLater { events.open(route) } },
) : AutoCloseable {
    private val log = LoggerFactory.getLogger(LinuxNotifications::class.java)
    private val planner = LinuxNoticePlanner()
    private val running = mutableListOf<Process>()
    private var disabled = false

    init {
        events.onState = ::onState
    }

    private fun onState(state: UiState) {
        if (disabled) return
        planner.take(state).forEach(::publish)
    }

    private fun publish(notice: LinuxNotice) {
        if (disabled) return
        val process = try {
            start(linuxNotifyCommand(notice, iconPath))
        } catch (failure: IOException) {
            disabled = true
            log.warn("notify-send is not available; Linux notifications stay off", failure)
            return
        }
        synchronized(running) { running.add(process) }
        thread(isDaemon = true, name = "linux-notice") {
            readAction(process, notice.route)
        }
    }

    internal fun consume(process: Process, route: AppRoute) {
        val action = process.inputStream.bufferedReader().readLine()?.trim()
        if (action == "default") deliver(route)
        process.waitFor()
    }

    private fun readAction(process: Process, route: AppRoute) {
        try {
            consume(process, route)
        } catch (failure: IOException) {
            // close() destroys the process and breaks this read. A normal dismiss returns null instead.
            log.debug("Linux notification closed before it was read", failure)
        } catch (failure: InterruptedException) {
            Thread.currentThread().interrupt()
            log.debug("Linux notification reader was interrupted", failure)
        } finally {
            synchronized(running) { running.remove(process) }
        }
    }

    override fun close() {
        val processes = synchronized(running) { running.toList().also { running.clear() } }
        processes.forEach { process ->
            runCatching { process.destroy() }
        }
    }
}

internal fun startNotifySend(command: List<String>): Process = ProcessBuilder(command)
    .redirectError(ProcessBuilder.Redirect.DISCARD)
    .start()

/** Copies the app icon where notify-send can read it. Null means the notice has no icon. */
internal fun linuxNotificationIcon(): String? {
    val home = System.getProperty("user.home") ?: return null
    return copyNotificationIcon("radarnebo.png", File(home, ".radarnebo/notification-icon.png"))
}

/** The written path, or null when the icon cannot be read or written. The notice then has no icon. */
internal fun copyNotificationIcon(resource: String, destination: File): String? {
    val stream = LinuxNotifications::class.java.classLoader.getResourceAsStream(resource) ?: return null
    return try {
        destination.parentFile?.mkdirs()
        stream.use { input -> destination.outputStream().use { input.copyTo(it) } }
        destination.absolutePath
    } catch (_: IOException) {
        null
    }
}
