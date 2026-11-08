package com.gusak.lab.radarnebo.desktop

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.domain.ProximityHit
import com.gusak.lab.radarnebo.domain.ThreatMarker
import com.gusak.lab.radarnebo.ui.AppRoute
import com.gusak.lab.radarnebo.ui.UiState
import kotlin.math.roundToInt

/** One Linux notification. [route] is what a click on it opens. */
internal data class LinuxNotice(
    val summary: String,
    val body: String,
    val urgency: NoticeUrgency,
    val route: AppRoute,
)

internal enum class NoticeUrgency {
    Normal,
    Critical,
}

/**
 * Notices for threats that appeared after the first successful poll.
 * The first picture is the baseline: opening the app must not announce every current alert.
 * A poll that has not succeeded yet (`lastUpdated == null`) does not move the baseline.
 */
internal class LinuxNoticePlanner {
    private var primed = false
    private val seen = mutableSetOf<String>()

    fun take(state: UiState): List<LinuxNotice> {
        if (state.lastUpdated == null) return emptyList()
        val current = currentKeys(state)
        if (!primed) {
            primed = true
            seen.addAll(current)
            return emptyList()
        }
        val notices = noticesFor(state)
        seen.clear()
        seen.addAll(current)
        return notices
    }

    private fun noticesFor(state: UiState): List<LinuxNotice> {
        val freshHits = state.proximity.filter { hitKey(it) !in seen }
        val covered = state.proximity.map { it.threat.id }.toSet()
        val freshThreats = state.threats.filter { threatKey(it) !in seen && it.id !in covered }
        return cap(freshHits.map(::hitNotice), ::hitsOverflow) +
            cap(freshThreats.map(::threatNotice), ::threatsOverflow)
    }
}

private const val NOTICE_LIMIT = 3
private const val NOTICE_TEXT_LIMIT = 240

private fun cap(
    notices: List<LinuxNotice>,
    overflow: (extra: Int, urgency: NoticeUrgency) -> LinuxNotice,
): List<LinuxNotice> {
    if (notices.size <= NOTICE_LIMIT) return notices
    val rest = notices.drop(NOTICE_LIMIT)
    val urgency = if (rest.any { it.urgency == NoticeUrgency.Critical }) {
        NoticeUrgency.Critical
    } else {
        NoticeUrgency.Normal
    }
    return notices.take(NOTICE_LIMIT) + overflow(rest.size, urgency)
}

private fun hitsOverflow(extra: Int, urgency: NoticeUrgency): LinuxNotice {
    val text = if (extra == 1) {
        "1 more place has a nearby threat"
    } else {
        "$extra more places have a nearby threat"
    }
    return LinuxNotice("RadarNebo", text, urgency, AppRoute.Favorites)
}

private fun threatsOverflow(extra: Int, urgency: NoticeUrgency): LinuxNotice {
    val text = if (extra == 1) "1 more threat" else "$extra more threats"
    return LinuxNotice("RadarNebo", text, urgency, AppRoute.Map)
}

private fun hitNotice(hit: ProximityHit): LinuxNotice {
    val place = hit.favorite.name.ifBlank { "a saved place" }
    val where = when {
        hit.insideRegion -> "Oblast-wide in ${hit.threat.oblastName.ifBlank { "this oblast" }}"
        hit.distanceKm < 1.0 -> "Under 1 km from $place"
        else -> "${hit.distanceKm.roundToInt()} km from $place"
    }
    return LinuxNotice(
        summary = oneLine(hit.favorite.name).ifBlank { "RadarNebo" },
        body = oneLine("${hit.threat.title}. $where"),
        urgency = urgencyOf(hit.threat),
        route = AppRoute.Favorites,
    )
}

private fun threatNotice(marker: ThreatMarker): LinuxNotice {
    val detail = marker.message?.takeIf { it.isNotBlank() }
    val text = if (detail == null) marker.title else "${marker.title}. $detail"
    return LinuxNotice(
        summary = oneLine(marker.oblastName).ifBlank { "RadarNebo" },
        body = oneLine(text),
        urgency = urgencyOf(marker),
        route = AppRoute.Threat(marker.id),
    )
}

private fun urgencyOf(marker: ThreatMarker): NoticeUrgency = if (marker.level == ThreatLevel.Red) {
    NoticeUrgency.Critical
} else {
    NoticeUrgency.Normal
}

private fun currentKeys(state: UiState): Set<String> = buildSet {
    state.proximity.forEach {
        add(hitKey(it))
        add(threatKey(it.threat))
    }
    state.threats.forEach { add(threatKey(it)) }
}

private fun hitKey(hit: ProximityHit): String = "hit:${hit.favorite.id}:${hit.threat.id}"

private fun threatKey(marker: ThreatMarker): String = "threat:${marker.id}"

private val whitespace = Regex("\\s+")

private fun oneLine(text: String): String {
    val flat = whitespace.replace(text, " ").trim()
    if (flat.length <= NOTICE_TEXT_LIMIT) return flat
    return flat.take(NOTICE_TEXT_LIMIT - 1).trimEnd() + "…"
}

/** argv for notify-send. `--` keeps a threat title that starts with `-` from becoming a flag. */
internal fun linuxNotifyCommand(notice: LinuxNotice, iconPath: String?): List<String> = buildList {
    add("notify-send")
    add("--app-name=RadarNebo")
    add("--urgency=${if (notice.urgency == NoticeUrgency.Critical) "critical" else "normal"}")
    add("--action=default=Show")
    if (iconPath != null) add("--icon=$iconPath")
    add("--")
    add(notice.summary)
    add(notice.body)
}
