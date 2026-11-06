package com.gusak.lab.radarnebo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gusak.lab.radarnebo.data.AlertsSnapshot
import com.gusak.lab.radarnebo.data.AlertsUpdate
import com.gusak.lab.radarnebo.data.AppSettings
import com.gusak.lab.radarnebo.data.FavoritesRepository
import com.gusak.lab.radarnebo.data.MapMode
import com.gusak.lab.radarnebo.data.SettingsRepository
import com.gusak.lab.radarnebo.domain.usecase.AddFavoriteUseCase
import com.gusak.lab.radarnebo.domain.usecase.BuildThreatPictureUseCase
import com.gusak.lab.radarnebo.domain.usecase.RefreshAlertsUseCase
import com.gusak.lab.radarnebo.domain.usecase.RemoveFavoriteUseCase
import com.gusak.lab.radarnebo.domain.usecase.UpdateSettingsUseCase
import com.gusak.lab.radarnebo.geo.GeoPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Screen state for the whole app. Settings and favorites come from their repositories; alerts are
 * polled through [RefreshAlertsUseCase] and turned into what the screens show by [BuildThreatPictureUseCase].
 */
class AppViewModel(
    settingsRepository: SettingsRepository,
    favoritesRepository: FavoritesRepository,
    private val refreshAlerts: RefreshAlertsUseCase,
    private val buildThreatPicture: BuildThreatPictureUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ViewModel() {

    private val _state = MutableStateFlow(
        UiState(settings = settingsRepository.settings.value, favorites = favoritesRepository.favorites.value)
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Latest successful poll; null until the first one and after switching data source. */
    private var snapshot: AlertsSnapshot? = null
    private var pollJob: Job? = null

    init {
        // The threat picture depends on settings (filters, radius) and favorites, so rebuild it when they change.
        combine(settingsRepository.settings, favoritesRepository.favorites, ::Pair)
            .onEach { (settings, favorites) ->
                _state.update { it.copy(settings = settings, favorites = favorites) }
                recompute()
            }
            .launchIn(viewModelScope)
        startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(fetchOnce())
            }
        }
    }

    /** Tries the server right away instead of waiting for the reconnect timer. */
    fun reconnect() {
        startPolling()
    }

    /**
     * Returns how long to wait before the next poll: the poll interval, or after a failure the reconnect delay,
     * so a server started after the app is picked up within seconds. Rate-limit and auth errors back off instead.
     */
    private suspend fun fetchOnce(): Long {
        _state.update { it.copy(loading = true) }
        return when (val update = refreshAlerts()) {
            is AlertsUpdate.Updated -> {
                snapshot = update.snapshot
                _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        lastUpdated = currentInstant(),
                        clockOffsetMs = update.snapshot.clockOffsetMs,
                        simulated = update.snapshot.simulated,
                        serverHealth = update.snapshot.health,
                        nextRetryAt = null,
                    )
                }
                recompute()
                _state.value.settings.pollSeconds * 1000L
            }
            is AlertsUpdate.Failed -> {
                val waitMs = if (update.backOff) _state.value.settings.pollSeconds * 1000L * 4 else RECONNECT_MS
                _state.update {
                    it.copy(
                        loading = false,
                        error = update.message,
                        serverHealth = null,
                        nextRetryAt = currentInstant() + waitMs.milliseconds,
                    )
                }
                waitMs
            }
        }
    }

    private fun currentInstant(): Instant = Instant.fromEpochMilliseconds(clock())

    private fun recompute() {
        _state.update { s ->
            val now = clock() + s.clockOffsetMs
            val picture = buildThreatPicture(
                snapshot?.alerts.orEmpty(),
                snapshot?.tracks.orEmpty(),
                s.settings,
                s.favorites,
                now,
            )
            s.copy(
                threats = picture.threats,
                mapThreats = picture.mapThreats,
                proximity = picture.proximity,
                alertedOblasts = picture.alertedOblasts,
                tracks = picture.tracks,
                allTracks = picture.unfilteredTracks,
            )
        }
    }

    internal fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val update = updateSettingsUseCase(transform)
        if (update.sourceChanged) {
            snapshot = null
            _state.update {
                it.copy(
                    error = null,
                    tracks = emptyList(),
                    allTracks = emptyList(),
                    clockOffsetMs = 0,
                    simulated = false,
                    serverHealth = null,
                    lastUpdated = null,
                    nextRetryAt = null,
                )
            }
            startPolling()
        }
    }

    internal fun setMapMode(mode: MapMode) {
        updateSettings { it.copy(mapMode = mode) }
    }

    internal fun addFavorite(name: String, point: GeoPoint) {
        addFavoriteUseCase(name, point)
    }

    internal fun removeFavorite(id: String) {
        removeFavoriteUseCase(id)
    }
}

/** How soon the app tries an unreachable server again. */
private const val RECONNECT_MS = 5_000L
