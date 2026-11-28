package com.gusak.lab.radarnebo.domain.usecase

import com.gusak.lab.radarnebo.data.FetchResult
import com.gusak.lab.radarnebo.data.RadarNeboApi
import com.gusak.lab.radarnebo.data.SettingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Fetches a [TargetsSummary] from the server, apart from the app's own polling, for the home screen widget. */
class LoadTargetsSummaryUseCase(
    private val api: RadarNeboApi,
    private val settings: SettingsRepository,
) {
    /**
     * Null when the tracks or the alerts can't be fetched, so the widget keeps what it shows rather than an empty map;
     * missing stats alone still give a summary.
     */
    suspend operator fun invoke(): TargetsSummary? = coroutineScope {
        val endpoint = settings.settings.value.endpoint
        val stats = async { api.targetStats(endpoint) }
        val alerts = async { api.activeAlerts(endpoint) }
        val tracks = api.tracks(endpoint)
        val fresh = alerts.await() as? FetchResult.Fresh
        if (tracks == null || fresh == null) null else TargetsSummary.of(tracks, fresh.alerts, stats.await())
    }
}
