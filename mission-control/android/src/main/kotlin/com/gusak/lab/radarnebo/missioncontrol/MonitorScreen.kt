package com.gusak.lab.radarnebo.missioncontrol

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** [ms] as a time of day in the app's language, which can differ from the system's (per-app language). */
@Composable
private fun clock(ms: Long?): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(locale).withZone(ZoneId.systemDefault()) }
    return ms?.let { format.format(Instant.ofEpochMilli(it)) } ?: "–"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(viewModel: MonitorViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    var editingServer by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    GlyphButton("↻", stringResource(R.string.action_refresh), viewModel::refresh)
                    GlyphButton("⚙", stringResource(R.string.action_server)) { editingServer = true }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { StatusCard(state, serverUrl, onRetry = viewModel::refresh) }
            state.health?.let { health ->
                item { SourceCard(health) }
                item { SituationCard(health) }
                item { UpstreamCard(health) }
                item { PollingCard(health) }
                health.lastError?.let { error ->
                    item { InfoCard(stringResource(R.string.card_error)) { Text(error, color = StatusColors.danger, fontSize = 14.sp) } }
                }
            }
            item {
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, serverUrl.toUri())) },
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text(stringResource(R.string.action_open_status_page)) }
            }
        }
    }

    if (editingServer) {
        ServerDialog(
            current = serverUrl,
            onSave = { url ->
                viewModel.setServerUrl(url)
                editingServer = false
            },
            onDismiss = { editingServer = false },
        )
    }
}

@Composable
private fun GlyphButton(glyph: String, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
        Text(glyph, fontSize = 22.sp)
    }
}

@Composable
private fun StatusCard(state: MonitorState, serverUrl: String, onRetry: () -> Unit) {
    val health = state.health
    val (color, headline) = when {
        state.error != null -> StatusColors.danger to stringResource(R.string.status_unreachable)
        health == null -> MaterialTheme.colorScheme.onSurfaceVariant to stringResource(R.string.status_connecting)
        health.status == "ok" -> StatusColors.ok to stringResource(R.string.status_ok)
        health.status == "warming_up" -> StatusColors.warning to stringResource(R.string.status_warming_up)
        health.status == "stale" -> StatusColors.danger to stringResource(R.string.status_stale)
        else -> StatusColors.warning to stringResource(R.string.status_degraded)
    }
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(headline, Modifier.weight(1f), color = color, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        Spacer(Modifier.size(6.dp))
        Text(serverUrl, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        when {
            state.error != null -> {
                Text(state.error.ifBlank { stringResource(R.string.unknown_error) }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.fetchedAt?.let { Text(stringResource(R.string.showing_last_known, clock(it)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
            state.fetchedAt != null -> Text(
                stringResource(R.string.updated_at, clock(state.fetchedAt), REFRESH_SECONDS),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceCard(health: Health) {
    InfoCard(stringResource(R.string.card_source)) {
        val source = when {
            health.simulated -> stringResource(R.string.source_simulation)
            health.source == "AlertsInUa" -> "alerts.in.ua"
            else -> health.source
        }
        Fact(stringResource(R.string.source_label), source)
        if (health.simulated) Fact("", stringResource(R.string.source_simulated_warning), StatusColors.warning)
        Fact(stringResource(R.string.data_version), health.dataVersion?.toString() ?: "–")
    }
}

@Composable
private fun SituationCard(health: Health) {
    InfoCard(stringResource(R.string.card_situation)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            BigNumber(stringResource(R.string.situation_alerts), health.alerts.toString(), Modifier.weight(1f))
            BigNumber(stringResource(R.string.situation_active_tracks), health.activeTracks.toString(), Modifier.weight(1f))
        }
        Fact(stringResource(R.string.situation_tracks), stringResource(R.string.situation_tracks_value, health.activeTracks, health.tracks))
        Fact(stringResource(R.string.situation_data_from), clock(health.dataConfirmedAt))
        Fact(
            stringResource(R.string.situation_data_age),
            health.dataAgeSeconds?.let { stringResource(R.string.seconds_ago, it.toInt()) } ?: "–",
        )
    }
}

@Composable
private fun UpstreamCard(health: Health) {
    InfoCard(stringResource(R.string.card_upstream)) {
        health.upstreamBudgetPerMinute?.let { budget ->
            Fact(stringResource(R.string.upstream_last_minute), stringResource(R.string.upstream_last_minute_value, health.upstreamRequestsLastMinute, budget))
        }
        Fact(stringResource(R.string.upstream_total), health.upstreamRequestsTotal.toString())
        Fact(stringResource(R.string.upstream_rate_limited), health.upstreamRateLimitedTotal.toString(), warnIfPositive(health.upstreamRateLimitedTotal))
        Fact(stringResource(R.string.upstream_rejected), health.upstreamRejectedTotal.toString(), warnIfPositive(health.upstreamRejectedTotal))
        Fact(stringResource(R.string.upstream_other_failures), health.upstreamOtherFailuresTotal.toString(), warnIfPositive(health.upstreamOtherFailuresTotal))
        Fact(stringResource(R.string.upstream_last_success), clock(health.upstreamLastSuccessAt))
    }
}

@Composable
private fun PollingCard(health: Health) {
    InfoCard(stringResource(R.string.card_polling)) {
        Fact(stringResource(R.string.poll_interval), stringResource(R.string.poll_interval_value, health.pollIntervalSeconds.toInt()))
        Fact(stringResource(R.string.poll_last), clock(health.lastAttemptAt))
        Fact(stringResource(R.string.poll_next), clock(health.nextPollAt))
    }
}

@Composable
private fun warnIfPositive(count: Long) = if (count > 0) StatusColors.warning else MaterialTheme.colorScheme.onSurface

@Composable
private fun InfoCard(title: String? = null, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().widthIn(max = 640.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(2.dp))
            }
            content()
        }
    }
}

@Composable
private fun Fact(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row {
        Text(label, Modifier.weight(0.45f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(0.55f), fontSize = 14.sp, color = color)
    }
}

@Composable
private fun BigNumber(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ServerDialog(current: String, onSave: (String?) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(current) }
    val url = text.trim()
    val valid = (url.startsWith("http://") || url.startsWith("https://")) && url.substringAfter("://").isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.server_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.server_dialog_text), fontSize = 14.sp)
                OutlinedTextField(
                    text,
                    onValueChange = { text = it },
                    singleLine = true,
                    isError = !valid,
                    supportingText = if (valid) null else { { Text(stringResource(R.string.server_dialog_invalid)) } },
                )
                TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.common_use_default)) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(url.trimEnd('/')) }, enabled = valid) { Text(stringResource(R.string.common_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
