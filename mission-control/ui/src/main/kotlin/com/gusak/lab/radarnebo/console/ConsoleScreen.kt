package com.gusak.lab.radarnebo.console

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.qos.logback.classic.Level
import com.gusak.lab.radarnebo.server.api.Health
import com.gusak.lab.radarnebo.server.config.DataSourceKind
import com.gusak.lab.radarnebo.ui.debug.TrafficInspector
import com.gusak.lab.radarnebo.ui.formatClock
import com.gusak.lab.radarnebo.ui.radarColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URI
import java.net.URISyntaxException

private val log = LoggerFactory.getLogger("Console")

private const val REFRESH_MS = 1_000L

private enum class ConsoleTab(val label: String) { Requests("Requests"), Clients("Clients"), Logs("Logs") }

/** The console's window content; [clock] times how long the server has been up. */
@Composable
internal fun ConsoleScreen(
    server: ConsoleServer,
    logs: LogCapture,
    clock: () -> Long = System::currentTimeMillis,
) {
    val state by server.state.collectAsStateWithLifecycle()
    // The server's own report, refreshed every second while it runs; not polled while it doesn't.
    val health by produceHealth(server, state as? ServerState.Running)
    var clientView by rememberSaveable { mutableStateOf(ClientView.Hidden) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            TopBar(server, state, clientView, onClientViewChange = { clientView = it })
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // The console stays composed under a fullscreen client, so its tabs and filters are kept.
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Overview(server, state, health, clock)
                        ConsoleTabs(server, logs)
                    }
                    if (clientView == ClientView.Embedded) {
                        VerticalDivider()
                        Spacer(Modifier.width(EmbeddedClientWidth))
                    }
                }
                // One call site for both visible views, so switching between them keeps the client as it is.
                if (clientView != ClientView.Hidden) {
                    val size = if (clientView == ClientView.Fullscreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxHeight().width(EmbeddedClientWidth)
                    }
                    ClientPane(Modifier.align(Alignment.TopEnd).then(size))
                }
            }
        }
    }
}

@Composable
private fun produceHealth(server: ConsoleServer, running: ServerState.Running?): State<Health?> =
    produceState<Health?>(null, running) {
        // The state outlives a change of key, so the last report is cleared by hand once the server stops.
        if (running == null) {
            value = null
            return@produceState
        }
        while (true) {
            value = server.health()
            delay(REFRESH_MS)
        }
    }

/** The Requests, Clients and Logs tabs, filling the rest of the console's column. */
@Composable
private fun ColumnScope.ConsoleTabs(server: ConsoleServer, logs: LogCapture) {
    var tab by rememberSaveable { mutableStateOf(ConsoleTab.Requests) }
    PrimaryTabRow(selectedTabIndex = tab.ordinal) {
        ConsoleTab.entries.forEach { entry ->
            Tab(selected = tab == entry, onClick = { tab = entry }, text = { Text(entry.label) })
        }
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
        when (tab) {
            ConsoleTab.Requests -> RequestsTab(server)
            ConsoleTab.Clients -> ClientsTab(server)
            ConsoleTab.Logs -> LogsTab(logs)
        }
    }
}

@Composable
private fun RequestsTab(server: ConsoleServer) {
    val exchanges by server.traffic.exchanges.collectAsStateWithLifecycle()
    TrafficInspector(
        exchanges,
        onClear = server.traffic::clear,
        modifier = Modifier.fillMaxSize(),
        showDirections = true,
    )
}

/** Stopped, or failed to start: either way the server can be started. */
private val ServerState.isStopped: Boolean
    get() = this is ServerState.Stopped || this is ServerState.Failed

@Composable
private fun TopBar(
    server: ConsoleServer,
    state: ServerState,
    clientView: ClientView,
    onClientViewChange: (ClientView) -> Unit,
) {
    var port by rememberSaveable { mutableStateOf(server.defaultPort.toString()) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ServerStatus(state, Modifier.weight(1f))
        DataSourceToggle(server, enabled = state is ServerState.Running || state.isStopped)
        ClientViewToggle(clientView, onClientViewChange)
        OutlinedTextField(
            port,
            onValueChange = { value -> port = value.filter(Char::isDigit).take(5) },
            label = { Text("Port") },
            singleLine = true,
            enabled = state.isStopped,
            modifier = Modifier.width(110.dp),
        )
        StartStopButton(server, state, port = port.toIntOrNull()?.takeIf { it in 1..65_535 })
    }
    HorizontalDivider()
}

@Composable
private fun ServerStatus(state: ServerState, modifier: Modifier) {
    val (color, label) = when (state) {
        is ServerState.Running -> radarColors.ok to "Running on port ${state.port}"
        ServerState.Starting -> radarColors.warning to "Starting…"
        ServerState.Stopping -> radarColors.warning to "Stopping…"
        ServerState.Stopped -> MaterialTheme.colorScheme.onSurfaceVariant to "Stopped"
        is ServerState.Failed -> radarColors.danger to "Failed: ${state.message}"
    }
    Column(modifier) {
        Text("RadarNebo server", style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = color,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Starts the server on [port] (null while the field holds no valid port) or stops it. */
@Composable
private fun StartStopButton(server: ConsoleServer, state: ServerState, port: Int?) {
    val scope = rememberCoroutineScope()
    if (state.isStopped) {
        Button(
            onClick = { port?.let { scope.launch { server.start(it) } } },
            enabled = port != null,
        ) { Text("Start server") }
    } else {
        Button(
            onClick = { scope.launch { server.stop() } },
            enabled = state is ServerState.Running,
            colors = ButtonDefaults.buttonColors(containerColor = radarColors.danger, contentColor = Color.White),
        ) { Text("Stop server") }
    }
}

/** Switches the server between alerts.in.ua and the simulation; a running server restarts to switch. */
@Composable
private fun DataSourceToggle(server: ConsoleServer, enabled: Boolean) {
    val scope = rememberCoroutineScope()
    val current by server.dataSource.collectAsStateWithLifecycle()
    val options = DataSourceKind.entries
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, kind ->
                SegmentedButton(
                    selected = current == kind,
                    onClick = { scope.launch { server.switchDataSource(kind) } },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    enabled = enabled && (kind != DataSourceKind.AlertsInUa || server.alertsInUaAvailable),
                ) { Text(kind.label) }
            }
        }
        if (!server.alertsInUaAvailable) {
            Text(
                "alerts.in.ua needs ALERTS_API_TOKEN",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Overview(server: ConsoleServer, state: ServerState, health: Health?, clock: () -> Long) {
    val now by produceState(clock()) {
        while (true) {
            value = clock()
            delay(REFRESH_MS)
        }
    }
    FlowRow(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NetworkCard(state as? ServerState.Running, now)
        DataSourceCard(server)
        UpstreamCard(health)
        SituationCard(health)
    }
}

@Composable
private fun NetworkCard(running: ServerState.Running?, now: Long) {
    InfoCard("Network") {
        if (running == null) {
            Fact("Listening", "–")
        } else {
            Fact("Listening", "0.0.0.0:${running.port} (all interfaces)")
            Link("http://localhost:${running.port}")
            // Looked up once per run rather than on every one-second recomposition.
            val lan = remember(running.port) { lanAddresses() }
            lan.forEach { Link("http://$it:${running.port}") }
            Fact("Up for", duration(now - running.since))
        }
    }
}

@Composable
private fun DataSourceCard(server: ConsoleServer) {
    val dataSource by server.dataSource.collectAsStateWithLifecycle()
    InfoCard("Data source") {
        Fact("Source", dataSource.label)
        if (dataSource == DataSourceKind.AlertsInUa) Fact("Upstream", server.upstreamUrl)
        Fact(
            "Client keys",
            if (server.clientKeys == 0) "none (open to everyone)" else "${server.clientKeys} configured",
        )
        Fact("Archive", server.archive)
    }
}

@Composable
private fun UpstreamCard(health: Health?) {
    InfoCard("Upstream") {
        if (health == null) {
            Fact("Polling", "–")
        } else {
            health.upstreamBudgetPerMinute?.let { budget ->
                Fact(
                    "Requests",
                    "${health.upstreamRequestsLastMinute} of $budget in the last minute · " +
                        "${health.upstreamRequestsTotal} total",
                )
            }
            Fact("Polled", "every ${health.pollIntervalSeconds} s")
            Fact("Last poll", health.lastAttemptAt?.let(::formatClock) ?: "–")
            Fact("Next poll", health.nextPollAt?.let(::formatClock) ?: "–")
            health.lastError?.let { Fact("Error", it, radarColors.danger) }
        }
    }
}

@Composable
private fun SituationCard(health: Health?) {
    InfoCard("Situation") {
        if (health == null) {
            Fact("Status", "–")
        } else {
            Fact("Status", health.status)
            Fact("Alerts", health.alerts.toString())
            Fact("Tracks", "${health.activeTracks} active of ${health.tracks}")
            Fact("Data from", health.dataConfirmedAt?.let(::formatClock) ?: "–")
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.widthIn(min = 260.dp, max = 420.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.size(4.dp))
            content()
        }
    }
}

@Composable
private fun Fact(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row {
        Text(label, Modifier.width(90.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, color = color)
    }
}

@Composable
private fun Link(url: String) {
    Text(
        url,
        Modifier.padding(start = 90.dp).clickable { browse(url) },
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary,
    )
}

private fun browse(url: String) {
    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        log.warn("Couldn't open {}: this desktop has no browser to open it with", url)
        return
    }
    try {
        Desktop.getDesktop().browse(URI(url))
    } catch (e: IOException) {
        log.warn("Couldn't open {}", url, e)
    } catch (e: SecurityException) {
        log.warn("Couldn't open {}", url, e)
    } catch (e: IllegalArgumentException) {
        log.warn("Couldn't open {}", url, e)
    } catch (e: URISyntaxException) {
        log.warn("Couldn't open {}", url, e)
    }
}

private val DataSourceKind.label: String
    get() = when (this) {
        DataSourceKind.AlertsInUa -> "alerts.in.ua"
        DataSourceKind.Simulation -> "Simulation"
    }

/** Addresses other devices can use to reach this machine, IPv4 only. */
private fun lanAddresses(): List<String> = try {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback && !it.isVirtual }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .map { it.hostAddress }
} catch (_: SocketException) {
    emptyList()
}

private fun duration(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 60 -> "$s s"
        s < 3600 -> "${s / 60} min ${s % 60} s"
        else -> "${s / 3600} h ${s / 60 % 60} min"
    }
}

/** A fixed-width column of the clients table; the user agent follows them and takes the rest of the row. */
private class ClientColumn(val title: String, val width: Dp, val value: (ClientInfo) -> String)

private val clientColumns = listOf(
    ClientColumn("Address", 160.dp) { it.address },
    ClientColumn("Requests", 80.dp) { it.requests.toString() },
    ClientColumn("Errors", 70.dp) { it.errors.toString() },
    ClientColumn("Last seen", 90.dp) { formatClock(it.lastSeen) },
    ClientColumn("Last request", 300.dp) { it.lastRequest },
    ClientColumn("Status", 60.dp) { it.lastStatus?.toString() ?: "–" },
    ClientColumn("Key", 50.dp) { if (it.usedKey) "yes" else "no" },
)

@Composable
private fun ClientsTab(server: ConsoleServer) {
    val clients by server.clients.collectAsStateWithLifecycle()
    val rows = clients.values.sortedByDescending { it.lastSeen }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${rows.size} clients since the console started",
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = server::clearClients) { Text("Clear") }
        }
        ClientsHeader()
        HorizontalDivider(Modifier.padding(top = 6.dp))
        if (rows.isEmpty()) {
            Text(
                "No client has called the server yet.",
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SelectionContainer {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(rows, key = { it.address }) { client -> ClientRow(client) }
            }
        }
    }
}

@Composable
private fun ClientsHeader() {
    Row(Modifier.padding(horizontal = 16.dp)) {
        clientColumns.forEach { column ->
            Text(column.title, Modifier.width(column.width), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Text("User agent", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun ClientRow(client: ClientInfo) {
    Row(Modifier.padding(vertical = 4.dp)) {
        clientColumns.forEach { column ->
            Text(
                column.value(client),
                Modifier.width(column.width),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(client.userAgent ?: "–", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LogsTab(logs: LogCapture) {
    val lines by logs.lines.collectAsStateWithLifecycle()
    var minLevel by rememberSaveable { mutableStateOf(Level.INFO.levelStr) }
    var follow by rememberSaveable { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(lines, minLevel, query) {
        val min = Level.toLevel(minLevel)
        val q = query.lowercase()
        lines.filter { line ->
            line.level.isGreaterOrEqual(min) &&
                (q.isBlank() || q in line.message.lowercase() || q in line.logger.lowercase())
        }
    }
    val listState = rememberLazyListState()
    // Keyed on the newest line, not the count: once the buffer is full the count stays the same as lines arrive.
    LaunchedEffect(shown.lastOrNull()?.id, follow) {
        if (follow && shown.isNotEmpty()) listState.scrollToItem(shown.lastIndex)
    }
    Column(Modifier.fillMaxSize()) {
        LogsToolbar(
            minLevel = minLevel,
            onMinLevelChange = { minLevel = it },
            query = query,
            onQueryChange = { query = it },
            follow = follow,
            onFollowChange = { follow = it },
            onClear = logs::clear,
        )
        HorizontalDivider()
        SelectionContainer {
            LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                items(shown, key = { it.id }) { line -> LogLineRow(line) }
            }
        }
    }
}

@Composable
private fun LogsToolbar(
    minLevel: String,
    onMinLevelChange: (String) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    follow: Boolean,
    onFollowChange: (Boolean) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(Level.INFO, Level.WARN, Level.ERROR).forEach { level ->
            FilterChip(
                selected = minLevel == level.levelStr,
                onClick = { onMinLevelChange(level.levelStr) },
                label = { Text("${level.levelStr}+") },
            )
        }
        OutlinedTextField(
            query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search logs") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        FilterChip(selected = follow, onClick = { onFollowChange(!follow) }, label = { Text("Follow") })
        TextButton(onClick = onClear) { Text("Clear") }
    }
}

@Composable
private fun LogLineRow(line: LogLine) {
    val color = when (line.level) {
        Level.ERROR -> radarColors.danger
        Level.WARN -> radarColors.warning
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(Modifier.padding(vertical = 1.dp)) {
        Text(
            formatClock(line.time),
            Modifier.width(72.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            line.level.levelStr,
            Modifier.width(52.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        Text(
            line.logger,
            Modifier.width(150.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(line.message, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = color)
    }
}
