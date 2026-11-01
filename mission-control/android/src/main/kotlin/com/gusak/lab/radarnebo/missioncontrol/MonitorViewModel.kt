package com.gusak.lab.radarnebo.missioncontrol

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull

const val REFRESH_SECONDS = 5

/**
 * What the monitor knows about the server: its last report and when it came, and whether the latest attempt failed.
 * A failed attempt keeps the last report, so a short outage doesn't blank the screen.
 */
data class MonitorState(
    val health: Health? = null,
    val fetchedAt: Long? = null,
    val error: String? = null,
    val loading: Boolean = true,
)

class MonitorViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("monitor", Context.MODE_PRIVATE)
    private val client = HealthClient()
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)

    private val _serverUrl = MutableStateFlow(prefs.getString(KEY_SERVER_URL, null) ?: BuildConfig.DEFAULT_SERVER_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    /** Polls while someone watches (the screen, while started) and for a few seconds after, so rotation doesn't restart it. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<MonitorState> = _serverUrl
        .flatMapLatest(::poll)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonitorState())

    fun refresh() {
        refreshRequests.trySend(Unit)
    }

    /** Watches [url] from now on; null goes back to the build's default server. */
    fun setServerUrl(url: String?) {
        prefs.edit { if (url == null) remove(KEY_SERVER_URL) else putString(KEY_SERVER_URL, url) }
        _serverUrl.value = url ?: BuildConfig.DEFAULT_SERVER_URL
    }

    private fun poll(url: String) = channelFlow {
        var current = MonitorState()
        send(current)
        while (true) {
            current = try {
                MonitorState(health = client.fetch(url), fetchedAt = System.currentTimeMillis(), loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                current.copy(error = e.message ?: e::class.simpleName.orEmpty(), loading = false)
            }
            send(current)
            // A refresh asked for while waiting cuts the wait short.
            if (withTimeoutOrNull(REFRESH_SECONDS * 1_000L) { refreshRequests.receive() } != null) {
                current = current.copy(loading = true)
                send(current)
            }
        }
    }

    override fun onCleared() = client.close()

    private companion object {
        const val KEY_SERVER_URL = "server_url"
    }
}
