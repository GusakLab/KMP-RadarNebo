package com.gusak.lab.radarnebo.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusak.lab.radarnebo.debug.HttpExchange
import com.gusak.lab.radarnebo.debug.HttpHeader
import com.gusak.lab.radarnebo.ui.STATUS_TINT
import com.gusak.lab.radarnebo.ui.formatClock
import com.gusak.lab.radarnebo.ui.radarColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * A list of HTTP exchanges with their details: headers and the response body, pretty-printed when it is JSON.
 * Side by side when there is room, otherwise the list opens a detail page. [showDirections] adds filters for
 * incoming and outgoing requests, for the server, which has both.
 */
@Composable
fun TrafficInspector(
    exchanges: List<HttpExchange>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    showDirections: Boolean = false,
    header: (@Composable () -> Unit)? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var direction by rememberSaveable { mutableStateOf<HttpExchange.Direction?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = exchanges.asReversed().filter { e ->
        val matchesQuery = query.isBlank() || query.trim().lowercase() in e.url.lowercase()
        (direction == null || e.direction == direction) && matchesQuery
    }
    val selected = exchanges.firstOrNull { it.id == selectedId }

    BoxWithConstraints(modifier) {
        val wide = maxWidth >= 760.dp
        val list = @Composable { listModifier: Modifier ->
            Column(listModifier) {
                header?.invoke()
                FilterRow(
                    query,
                    onQueryChange = { query = it },
                    onClear = {
                        onClear()
                        selectedId = null
                    },
                )
                if (showDirections) {
                    DirectionChips(exchanges, direction, onDirectionChange = { direction = it })
                }
                ExchangeList(exchanges, shown, selectedId, showDirections, onSelect = { selectedId = it })
            }
        }

        if (wide) {
            SideBySide(list, selected)
        } else if (selected != null) {
            ExchangeDetail(selected, onBack = { selectedId = null })
        } else {
            list(Modifier.fillMaxSize())
        }
    }
}

/** The [list] beside the [selected] exchange's details, or a hint to pick one. */
@Composable
private fun SideBySide(list: @Composable (Modifier) -> Unit, selected: HttpExchange?) {
    Row(Modifier.fillMaxSize()) {
        list(Modifier.weight(0.42f).fillMaxHeight())
        VerticalDivider()
        Box(Modifier.weight(0.58f).fillMaxHeight()) {
            if (selected != null) {
                ExchangeDetail(selected, onBack = null)
            } else {
                Text(
                    "Select a request to see its details.",
                    Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The [shown] exchanges, newest first, or why there are none of [all]. */
@Composable
private fun ExchangeList(
    all: List<HttpExchange>,
    shown: List<HttpExchange>,
    selectedId: Long?,
    showDirections: Boolean,
    onSelect: (Long) -> Unit,
) {
    if (shown.isEmpty()) {
        Text(
            if (all.isEmpty()) "No requests yet." else "No requests match.",
            Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
        items(shown, key = { it.id }) { exchange ->
            ExchangeRow(exchange, selected = exchange.id == selectedId, showDirection = showDirections) {
                onSelect(exchange.id)
            }
        }
    }
}

/** The URL filter, and a button that clears the log. */
@Composable
private fun FilterRow(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            query,
            onValueChange = onQueryChange,
            placeholder = { Text("Filter by URL") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear) { Text("Clear") }
    }
}

/** Shows every exchange, or only incoming or outgoing ones; null [direction] is all of them. */
@Composable
private fun DirectionChips(
    exchanges: List<HttpExchange>,
    direction: HttpExchange.Direction?,
    onDirectionChange: (HttpExchange.Direction?) -> Unit,
) {
    val incoming = exchanges.count { it.direction == HttpExchange.Direction.Incoming }
    val outgoing = exchanges.count { it.direction == HttpExchange.Direction.Outgoing }
    Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            direction == null,
            onClick = { onDirectionChange(null) },
            label = { Text("All (${exchanges.size})") },
        )
        FilterChip(
            direction == HttpExchange.Direction.Incoming,
            onClick = { onDirectionChange(HttpExchange.Direction.Incoming) },
            label = { Text("From clients ($incoming)") },
        )
        FilterChip(
            direction == HttpExchange.Direction.Outgoing,
            onClick = { onDirectionChange(HttpExchange.Direction.Outgoing) },
            label = { Text("Upstream ($outgoing)") },
        )
    }
}

@Composable
private fun ExchangeRow(exchange: HttpExchange, selected: Boolean, showDirection: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            formatClock(exchange.startedAt),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showDirection) {
            val arrow = if (exchange.direction == HttpExchange.Direction.Incoming) "↓" else "↑"
            Text(arrow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        StatusBadge(exchange)
        Column(Modifier.weight(1f)) {
            Text(
                "${exchange.method} ${exchange.path}",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(exchange.host ?: exchange.remoteAddress, "${exchange.durationMs} ms", bodySize(exchange))
                    .joinToString(" · "),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StatusBadge(exchange: HttpExchange) {
    val colors = radarColors
    val status = exchange.status
    val color = when {
        status == null -> colors.dangerText
        status >= 500 -> colors.dangerText
        status >= 400 -> colors.warningText
        status == 304 -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> colors.okText
    }
    Text(
        status?.toString() ?: "ERR",
        Modifier
            .background(color.copy(alpha = STATUS_TINT), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
    )
}

private fun bodySize(exchange: HttpExchange) = exchange.responseBody?.length?.let { chars ->
    if (chars < 1024) "$chars B" else "${(chars / 102.4).toInt() / 10.0} KB"
}

@Composable
private fun ExchangeDetail(exchange: HttpExchange, onBack: (() -> Unit)?) {
    val pretty = remember(exchange.id) { exchange.responseBody?.let(::prettyJson) }
    val body = remember(pretty) { pretty?.lines().orEmpty() }
    val highlight = jsonHighlightColors()
    SelectionContainer {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                if (onBack != null) TextButton(onClick = onBack) { Text("← Requests") }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusBadge(exchange)
                    Text(exchange.method, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Text(
                    exchange.url,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                ExchangeFacts(exchange)
                Section("Request headers")
                Headers(exchange.requestHeaders)
                Section("Response headers")
                Headers(exchange.responseHeaders)
                Section("Response body") {
                    if (pretty != null) CopyButton(pretty)
                }
                if (body.isEmpty()) Text("(none)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            itemsIndexed(body) { _, line ->
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    Text(
                        highlightJsonLine(line, highlight),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/** When, how long, which way and from whom an exchange went, and how it ended. */
@Composable
private fun ExchangeFacts(exchange: HttpExchange) {
    Fact("Time", formatClock(exchange.startedAt))
    Fact("Duration", "${exchange.durationMs} ms")
    Fact(
        "Direction",
        if (exchange.direction == HttpExchange.Direction.Incoming) "Incoming (a client called)" else "Outgoing",
    )
    exchange.remoteAddress?.let { Fact("Client", it) }
    exchange.userAgent?.let { Fact("User agent", it) }
    exchange.contentType?.let { Fact("Content type", it) }
    bodySize(exchange)?.let { Fact("Body", it) }
    exchange.error?.let { Fact("Error", it, radarColors.dangerText) }
}

@Composable
private fun Fact(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.padding(vertical = 1.dp)) {
        Text(label, Modifier.width(110.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, color = color)
    }
}

@Composable
private fun Section(title: String, action: @Composable () -> Unit = {}) {
    Spacer(Modifier.padding(top = 12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        action()
    }
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
}

/** Copies [text] to the clipboard, and says so for a moment. */
@Composable
private fun CopyButton(text: String) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember(text) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    TextButton(onClick = {
        scope.launch {
            clipboard.setClipEntry(plainTextClip(text))
            copied = true
        }
    }) { Text(if (copied) "Copied ✓" else "Copy to clipboard") }
}

@Composable
private fun Headers(headers: List<HttpHeader>) {
    if (headers.isEmpty()) Text("(none)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    headers.forEach { (name, value) ->
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(name) }
                append(": ")
                append(value)
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
private val PrettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

/** [text] re-indented when it is JSON, as it is otherwise. */
internal fun prettyJson(text: String): String = try {
    PrettyJson.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(text))
} catch (_: SerializationException) {
    // Not JSON: shown as it came.
    text
}

private class JsonColors(val key: Color, val string: Color, val literal: Color)

@Composable
private fun jsonHighlightColors() = JsonColors(
    key = MaterialTheme.colorScheme.primary,
    string = radarColors.okText,
    literal = radarColors.warningText,
)

private val jsonToken = Regex(
    "\"(?:\\\\.|[^\"\\\\])*\"(\\s*:)?" + "|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?" + "|\\btrue\\b|\\bfalse\\b|\\bnull\\b",
)

/** Keys, strings and literals in one line of pretty-printed JSON, coloured. */
private fun highlightJsonLine(line: String, colors: JsonColors): AnnotatedString = buildAnnotatedString {
    var at = 0
    jsonToken.findAll(line).forEach { match ->
        append(line.substring(at, match.range.first))
        val color = when {
            match.groupValues[1].isNotEmpty() -> colors.key
            match.value.startsWith("\"") -> colors.string
            else -> colors.literal
        }
        withStyle(SpanStyle(color = color)) { append(match.value) }
        at = match.range.last + 1
    }
    append(line.substring(at))
}

/** The request inspector as a page of its own: the desktop app's window, the Android activity. */
@Composable
fun RequestInspectorScreen(exchanges: List<HttpExchange>, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        TrafficInspector(
            exchanges,
            onClear,
            Modifier.fillMaxSize(),
            header = {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).widthIn(max = 600.dp)) {
                    Text("Requests", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Every request this app made to its server, newest first, with the response.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}
