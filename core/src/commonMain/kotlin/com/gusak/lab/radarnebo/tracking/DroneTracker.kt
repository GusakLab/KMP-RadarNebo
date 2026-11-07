package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.domain.ThreatMarker
import com.gusak.lab.radarnebo.geo.GeoMath
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.geo.Regions
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Builds [DroneTrack]s from successive snapshots of threats that fly routes: drones and cruise
 * missiles (see [TrackKinematics.of]).
 *
 * Alerts only say which area is under threat, so movement is inferred. Every track runs a [TrackFilter]; a new
 * point report (city, hromada, raion) may continue a track of the same type and kind (a Gerbera decoy group never
 * continues a Shahed track) that is still open or ended within the type's re-acquire window, when it lies within
 * the type's hop distance, is reachable at its top speed (with slack for coarse alert timing) and falls inside
 * the gate around the filter's predicted position. Of those, the cheapest pairing wins (distance in units of the
 * combined error, plus a penalty for turning), assigned over all of an update's reports at once; otherwise the
 * report starts a track. A track takes at most one report per update, and reports appearing together always start
 * separate tracks.
 *
 * A report that only names an oblast (or a place that cannot be located inside it) never becomes a point of its
 * own: it keeps alive the track expected in or near that oblast, moving it to the oblast border when the filter
 * had it outside; with no such track it waits, for the re-acquire window, for a track to start in or next to
 * the oblast, which then counts as seen since the oblast report.
 *
 * A track is active while one of its alerts is open and its newest report (the threat's `started_at`, not
 * the poll that still lists it) is within the kind's [TrackKinematics.staleAfterMs]; a stale track is kept
 * while its alerts stay open, so they don't restart it, but is not continued by new alerts.
 * [maxHopKm] and [reacquireWindowMs] tune drones; missiles use [TrackKinematics.CruiseMissiles].
 */
class DroneTracker(
    private val maxHopKm: Double = TrackKinematics.Drones.maxHopKm,
    private val reacquireWindowMs: Long = TrackKinematics.Drones.reacquireWindowMs,
    private val retentionMs: Long = 3 * 60 * 60_000L,
    private val maxSamples: Int = 64,
    /** Prefix of track ids; a server that keeps tracks across restarts makes it unique per run. */
    private val idPrefix: String = "trk-",
) {
    private class MutableTrack(
        val id: String,
        val type: ThreatType,
        val kind: TargetKind,
        var level: ThreatLevel,
        var oblastUid: Int?,
        var firstSeen: Long,
        /** Newest report time; see [DroneTrack.lastSeen]. */
        var lastSeen: Long,
        firstSample: TrackSample,
    ) {
        private val openMarkerIds = mutableSetOf<String>()
        private val reports = ArrayDeque(listOf(firstSample))

        /** Alert threat ids attached to this track, in the order they were attached. */
        val markerIds: Set<String> get() = openMarkerIds

        /** Reported positions, oldest first. */
        val samples: List<TrackSample> get() = reports
        val kinematics = TrackKinematics.of(type, kind) ?: TrackKinematics.Drones
        val filter = TrackFilter(kinematics, samples.first().point).apply { samples.forEach { fuse(it) } }
        /** Distance flown along the reports, for the range limit. */
        var flownKm = 0.0

        fun isActive(nowMs: Long): Boolean = markerIds.isNotEmpty() && nowMs - lastSeen <= kinematics.staleAfterMs

        fun attachMarker(id: String) {
            openMarkerIds += id
        }

        /** Keeps only the alerts still open: [openIds]. */
        fun retainMarkers(openIds: Set<String>) {
            openMarkerIds.retainAll(openIds)
        }

        /** Adds [sample] as the newest report, keeping at most [maxSamples]. */
        fun appendSample(sample: TrackSample, maxSamples: Int) {
            reports.addLast(sample)
            while (reports.size > maxSamples) reports.removeFirst()
        }
    }

    /** A new report, located or not. */
    private class Report(val marker: ThreatMarker, val kind: TargetKind, val t: Long) {
        val sigmaKm = marker.errorKm ?: (marker.precision ?: SamplePrecision.Raion).sigmaKm
    }

    /** Reports left after merging twins, and every merged twin with the report it was merged into. */
    private class MergedReports(val located: List<Report>, val twins: Map<Report, Report>)

    /** A track [report] may continue, and the cost of pairing the two. */
    private class Pairing(val report: Report, val track: MutableTrack, val cost: Double)

    /** A track an oblast-only report may be about, where its filter expects it, and how far outside the oblast. */
    private data class RegionalCandidate(val track: MutableTrack, val predicted: GeoPoint, val outsideKm: Double)

    private val tracks = LinkedHashMap<String, MutableTrack>()
    private val markerToTrack = HashMap<String, String>()
    /** Oblast-only reports no track explained yet, by marker id. */
    private val pendingRegions = LinkedHashMap<String, Report>()
    /** Oblast-only reports already handled, so a still-open one is not handled again. */
    private val seenRegionMarkers = HashSet<String>()
    private var openMarkerIds: Set<String> = emptySet()
    private var sequence = 0L
    private var lastUpdateMs = 0L

    fun update(markers: List<ThreatMarker>, nowMs: Long) {
        lastUpdateMs = nowMs
        val tracked = markers.filter { TrackKinematics.of(it.type) != null }
        detachCleared(tracked.mapTo(HashSet()) { it.id }, nowMs)
        val (regional, reports) = newReports(tracked, nowMs).partition { isRegional(it.marker) }
        val merged = mergeTwins(reports)

        // Tracks that took a report in this update, and tracks born in it: simultaneous reports are separate groups.
        val absorbed = HashSet<String>()
        val assigned = assign(merged.located, nowMs, absorbed)
        val trackOf = applyReports(merged.located, assigned, nowMs, absorbed)
        merged.twins.forEach { (report, twin) -> trackOf[twin]?.attach(report.marker, report.t) }
        regional.sortedBy { it.t }.forEach { explainRegional(it, nowMs, absorbed) }

        // Stale tracks whose alerts are still open stay, so those alerts don't start new tracks every update.
        tracks.values.removeAll { it.markerIds.isEmpty() && nowMs - it.lastSeen > retentionMs }
    }

    /** Tracks as of [nowMs], by default the latest [update]. */
    fun snapshot(nowMs: Long = lastUpdateMs): List<DroneTrack> = tracks.values.map {
        DroneTrack(
            id = it.id,
            type = it.type,
            level = it.level,
            oblastUid = it.oblastUid,
            active = it.isActive(nowMs),
            firstSeen = it.firstSeen,
            lastSeen = it.lastSeen,
            markerIds = it.markerIds.toList(),
            samples = it.samples.toList(),
            kind = it.kind,
        )
    }

    private fun detachCleared(currentIds: Set<String>, nowMs: Long) {
        // Threats that cleared detach from their tracks first, so the track can "move" to a neighbour.
        tracks.values.forEach { it.retainMarkers(currentIds) }
        markerToTrack.keys.retainAll(currentIds)
        seenRegionMarkers.retainAll(currentIds)
        openMarkerIds = currentIds
        // A pending report outlives its alert: the target may have flown on into a neighbouring oblast.
        pendingRegions.values.removeAll { nowMs - it.t > reacquireMs(it.marker.type) }
    }

    /** [tracked] markers that no track holds yet, as reports; a marker a track holds only refreshes that track. */
    private fun newReports(tracked: List<ThreatMarker>, nowMs: Long): List<Report> = tracked
        .filter { marker ->
            val existing = markerToTrack[marker.id]?.let(tracks::get) ?: return@filter true
            // Still listed says nothing new; only a newer report time from alerts.in.ua does.
            marker.startedAt?.let { existing.lastSeen = maxOf(existing.lastSeen, observedAt(marker, nowMs)) }
            existing.attachMarker(marker.id)
            false
        }
        .map { Report(it, TargetClassifier.classify(it).kind, observedAt(it, nowMs)) }

    private fun mergeTwins(reports: List<Report>): MergedReports {
        // The same target reported twice (by two sources, or for a hromada and the raion around it) is one report:
        // the most precise one is kept, the others only attach their alerts to its track.
        val twins = HashMap<Report, Report>()
        val located = mutableListOf<Report>()
        reports.sortedBy { it.sigmaKm }.forEach { report ->
            val twin = located.firstOrNull { kept -> areTwins(kept, report) }
            if (twin != null) twins[report] = twin else located += report
        }
        return MergedReports(located, twins)
    }

    private fun areTwins(kept: Report, report: Report): Boolean =
        kept.marker.type == report.marker.type && kept.kind == report.kind &&
            abs(kept.t - report.t) <= MERGE_WINDOW_MS &&
            GeoMath.distanceKm(kept.marker.point, report.marker.point) <= maxOf(kept.sigmaKm, report.sigmaKm)

    /** The cheapest pairings of [located] reports with tracks, each report and each track at most once. */
    private fun assign(located: List<Report>, nowMs: Long, absorbed: MutableSet<String>): Map<Report, MutableTrack> {
        val assigned = HashMap<Report, MutableTrack>()
        located
            .flatMap { report -> candidates(report, nowMs) }
            .sortedBy { it.cost }
            .forEach { pairing ->
                if (pairing.report !in assigned && pairing.track.id !in absorbed) {
                    assigned[pairing.report] = pairing.track
                    absorbed += pairing.track.id
                }
            }
        return assigned
    }

    /** Adds every located report to its track, or starts one; returns the track each report went to. */
    private fun applyReports(
        located: List<Report>,
        assigned: Map<Report, MutableTrack>,
        nowMs: Long,
        absorbed: MutableSet<String>,
    ): Map<Report, MutableTrack> {
        val trackOf = HashMap<Report, MutableTrack>()
        located.sortedBy { it.t }.forEach { report ->
            val track = assigned[report] ?: reacquireInland(report, nowMs, absorbed)
            when {
                track == null -> startTrack(report, nowMs).also { absorbed += it.id; trackOf[report] = it }
                // Re-alerted in the same area: the group is still here, which says nothing about movement.
                GeoMath.distanceKm(track.samples.last().point, report.marker.point) < SAME_PLACE_KM ->
                    track.attach(report.marker, report.t)
                else -> {
                    track.add(sample(report.marker, report.t, track), report.sigmaKm)
                    track.attach(report.marker, report.t)
                }
            }
            track?.let { trackOf[report] = it }
        }
        return trackOf
    }

    /** Oblast-wide threats, and places that could only be put somewhere in their oblast. */
    private fun isRegional(marker: ThreatMarker) = marker.isRegionWide || marker.precision == SamplePrecision.Oblast

    private fun hopKm(type: ThreatType) = if (type == ThreatType.Drones) maxHopKm else trackedKinematics(type).maxHopKm

    private fun reacquireMs(type: ThreatType) =
        if (type == ThreatType.Drones) reacquireWindowMs else trackedKinematics(type).reacquireWindowMs

    /** Missile alerts are raised far ahead of the missile, so their timing says little about its speed. */
    private fun maxSpeedKmh(type: ThreatType, kind: TargetKind) =
        if (type != ThreatType.Drones) Double.POSITIVE_INFINITY else trackedKinematics(type, kind).maxSpeedKmh

    /** Only markers of a type with kinematics are tracked, so every report and track has them. */
    private fun trackedKinematics(type: ThreatType, kind: TargetKind? = null): TrackKinematics =
        checkNotNull(if (kind == null) TrackKinematics.of(type) else TrackKinematics.of(type, kind)) {
            "$type threats are not tracked"
        }

    private fun openTracks(report: Report, nowMs: Long) = tracks.values.asSequence()
        .filter { it.type == report.marker.type && it.kind == report.kind }
        .filter { it.isActive(nowMs) || nowMs - it.lastSeen <= reacquireMs(it.type) }

    /** Tracks [report] may continue, with the cost of pairing them; see the class description. */
    private fun candidates(report: Report, nowMs: Long): List<Pairing> {
        val point = report.marker.point
        val maxSpeed = maxSpeedKmh(report.marker.type, report.kind)
        return openTracks(report, nowMs).mapNotNull { track ->
            val last = track.samples.last()
            val km = GeoMath.distanceKm(last.point, point)
            if (km > hopKm(track.type) || km > reachableKm(maxSpeed, report.t - last.t)) return@mapNotNull null
            if (track.flownKm + km > track.kinematics.rangeKm) return@mapNotNull null
            val (predicted, sigma) = track.filter.predictedAt(report.t)
            val combined = sqrt(sigma * sigma + report.sigmaKm * report.sigmaKm)
            // Gated where the filter would fuse it: an alert's area moved back along the course by its warning lead.
            val gated = track.leadCorrected(point, report.marker.level, report.marker.precision)
            val miss = GeoMath.distanceKm(predicted, gated)
            // An alert-built track runs up to a warning lead ahead of its target, so a report may lie that far behind.
            val leadKm = if (last.level != null) {
                (track.filter.speedKmh ?: 0.0) * track.kinematics.warningLeadMs / 3_600_000.0
            } else {
                0.0
            }
            // Missiles are gated by hop distance only, for the same reason they have no speed limit here.
            if (maxSpeed.isFinite() && miss > GATE_SIGMAS * combined + leadKm) return@mapNotNull null
            val heading = track.filter.headingDeg
            val turn = if (heading != null && km >= SAME_PLACE_KM) {
                abs(TrackFilter.angleDiff(heading, GeoMath.bearingDeg(last.point, point)))
            } else {
                0.0
            }
            if (track.reverses(point, report.sigmaKm)) return@mapNotNull null
            Pairing(report, track, miss / combined + turn / TURN_COST_DEG)
        }.toList()
    }

    /**
     * Drones are launched from Russia, Belarus, occupied land and the sea, so a group first reported in an inland
     * oblast is far more likely one that was lost on its way there than a new one. Before such a report starts a
     * track, it takes the nearest track lost within twice the re-acquire window that could have reached it.
     */
    private fun reacquireInland(report: Report, nowMs: Long, absorbed: MutableSet<String>): MutableTrack? {
        if (report.marker.type != ThreatType.Drones || report.marker.oblastUid !in Regions.INLAND_OBLAST_UIDS) {
            return null
        }
        val point = report.marker.point
        return tracks.values
            .filter { it.type == report.marker.type && it.kind == report.kind && it.id !in absorbed }
            .filter { nowMs - it.lastSeen <= 2 * reacquireMs(it.type) }
            .map { it to GeoMath.distanceKm(it.samples.last().point, point) }
            .filter { (track, km) ->
                !track.reverses(point, report.sigmaKm) &&
                    km <= hopKm(track.type) * INLAND_HOP_FACTOR &&
                    km <= reachableKm(track.kinematics.maxSpeedKmh, report.t - track.samples.last().t) &&
                    track.flownKm + km <= track.kinematics.rangeKm
            }
            .minByOrNull { (_, km) -> km }
            ?.let { (track, _) -> track }
            ?.also { absorbed += it.id }
    }

    /** Oblast-only [report]: keeps the track expected there alive, or waits for one to start there. */
    private fun explainRegional(report: Report, nowMs: Long, absorbed: MutableSet<String>) {
        val marker = report.marker
        seenRegionMarkers += marker.id
        val uid = marker.oblastUid ?: return
        val maxSpeed = maxSpeedKmh(marker.type, report.kind)
        val best = openTracks(report, nowMs)
            .mapNotNull { track ->
                val reach = reachableKm(maxSpeed, report.t - track.samples.last().t)
                val fromLast = Regions.distanceToRegionKm(uid, track.samples.last().point) ?: return@mapNotNull null
                if (fromLast > reach) return@mapNotNull null
                val predicted = track.filter.predictedAt(report.t).point
                val outsideKm = Regions.distanceToRegionKm(uid, predicted) ?: return@mapNotNull null
                RegionalCandidate(track, predicted, outsideKm)
            }
            .minByOrNull { it.outsideKm }
        if (best == null) {
            pendingRegions[marker.id] = report
            return
        }
        val (track, predicted, outsideKm) = best
        val entry = Regions.nearestPointInRegion(uid, predicted) ?: predicted
        if (outsideKm > 0 && track.id !in absorbed && !track.reverses(entry)) {
            // The filter had it short of (or past) the oblast: the target is at least at its border.
            val atBorder = TrackSample(
                t = clampTime(report.t, track),
                lat = entry.lat,
                lon = entry.lon,
                title = marker.oblastName,
                oblastUid = uid,
                message = marker.message,
                precision = SamplePrecision.Oblast,
                level = marker.level,
            )
            track.add(atBorder, SamplePrecision.Oblast.sigmaKm / 2)
            absorbed += track.id
        }
        track.attach(marker, report.t)
    }

    /** An oblast-only report waiting for [report]'s track: same type and kind, earlier, in or next to the oblast. */
    private fun pendingFor(report: Report): Report? = pendingRegions.values
        .filter { it.marker.type == report.marker.type && it.kind == report.kind }
        .filter { it.t <= report.t && report.t - it.t <= reacquireMs(it.marker.type) }
        .firstOrNull { pending ->
            val uid = pending.marker.oblastUid ?: return@firstOrNull false
            uid == report.marker.oblastUid ||
                (Regions.distanceToRegionKm(uid, report.marker.point) ?: Double.MAX_VALUE) <= NEIGHBOUR_KM
        }

    /**
     * A Shahed does not double back between two reports, so a report behind it is another group: a course from
     * the last report to [point] reversing the last reported leg, or the filter's course, by more than
     * [MAX_REVERSAL_DEG]. Decoys loiter and may. A report far more precise than the track's estimate (a sighting
     * behind an alert-built track, whose alerts run ahead of the target) corrects the track rather than reverses it.
     */
    private fun MutableTrack.reverses(point: GeoPoint, reportSigmaKm: Double = Double.MAX_VALUE): Boolean {
        if (kinematics.loiters || reportSigmaKm <= filter.sigmaKm * CORRECTION_RATIO) return false
        val last = samples.last()
        if (GeoMath.distanceKm(last.point, point) < SAME_PLACE_KM) return false
        val next = GeoMath.bearingDeg(last.point, point)
        val leg = samples.getOrNull(samples.lastIndex - 1)
            ?.takeIf { GeoMath.distanceKm(it.point, last.point) >= SAME_PLACE_KM }
            ?.let { GeoMath.bearingDeg(it.point, last.point) }
        return listOfNotNull(leg, filter.headingDeg).any { abs(TrackFilter.angleDiff(it, next)) > MAX_REVERSAL_DEG }
    }

    /**
     * Where the target was when an alert of [level] was raised for [point]: see [TrackFilter.update]. Fixes are
     * where they are.
     */
    private fun MutableTrack.leadCorrected(point: GeoPoint, level: ThreatLevel, precision: SamplePrecision?): GeoPoint {
        val heading = filter.headingDeg ?: return point
        val speed = filter.speedKmh ?: return point
        if (precision == SamplePrecision.Exact) return point
        return GeoMath.destination(point, (heading + 180.0) % 360.0, speed * kinematics.leadMs(level) / 3_600_000.0)
    }

    private fun MutableTrack.add(sample: TrackSample, sigmaKm: Double) {
        flownKm += GeoMath.distanceKm(samples.last().point, sample.point)
        appendSample(sample, maxSamples)
        oblastUid = sample.oblastUid ?: oblastUid
        filter.fuse(sample, sigmaKm)
    }

    /** [reportedAt] is when alerts.in.ua reported [marker]. */
    private fun MutableTrack.attach(marker: ThreatMarker, reportedAt: Long) {
        level = marker.level
        lastSeen = maxOf(lastSeen, reportedAt)
        attachMarker(marker.id)
        markerToTrack[marker.id] = id
    }

    private fun startTrack(report: Report, nowMs: Long): MutableTrack {
        val marker = report.marker
        val id = "$idPrefix${++sequence}"
        val pending = pendingFor(report)
        return MutableTrack(
            id = id,
            type = marker.type,
            kind = report.kind,
            level = marker.level,
            oblastUid = marker.oblastUid,
            firstSeen = pending?.t?.coerceAtMost(nowMs) ?: nowMs,
            lastSeen = report.t,
            firstSample = sample(marker, report.t, null),
        ).also {
            tracks[id] = it
            it.attach(marker, report.t)
            if (pending != null) {
                pendingRegions.remove(pending.marker.id)
                if (pending.marker.id in openMarkerIds) it.attach(pending.marker, pending.t)
            }
        }
    }

    /** Reports can come out of order; a track's samples stay in time order and no later than now. */
    private fun clampTime(t: Long, track: MutableTrack): Long {
        val last = track.samples.last()
        return t.coerceIn(last.t + 1, maxOf(lastUpdateMs, last.t + 1))
    }

    private fun sample(marker: ThreatMarker, t: Long, track: MutableTrack?) = TrackSample(
        t = if (track == null) t else clampTime(t, track),
        lat = marker.point.lat,
        lon = marker.point.lon,
        title = marker.title,
        oblastUid = marker.oblastUid,
        message = marker.message,
        precision = marker.precision,
        level = marker.level,
        errorKm = marker.errorKm,
    )

    private fun observedAt(marker: ThreatMarker, nowMs: Long) =
        (marker.startedAt?.toEpochMilliseconds() ?: nowMs).coerceAtMost(nowMs)

    private companion object {
        const val SAME_PLACE_KM = 1.0
        /** Alerts can come early or late; a hop is plausible up to this much over top speed, plus [TIMING_SLACK_KM]. */
        const val SPEED_TOLERANCE = 1.5
        const val TIMING_SLACK_KM = 25.0
        /** A report further from the predicted position than this many combined sigmas is another target. */
        const val GATE_SIGMAS = 3.0
        /** A turn this sharp costs as much as missing the prediction by one sigma. */
        const val TURN_COST_DEG = 90.0
        /** A track starting this close to an oblast is taken as the target an oblast-only report there announced. */
        const val NEIGHBOUR_KM = 50.0
        /** Reports of one target from two sources (or two nested areas) come within this of each other. */
        const val MERGE_WINDOW_MS = 2 * 60_000L
        /** A course change sharper than this between two reports is a reversal. */
        const val MAX_REVERSAL_DEG = 150.0
        /** A report this much more precise than the track's estimate is a correction, never a reversal. */
        const val CORRECTION_RATIO = 0.25
        /** Re-acquiring a lost group inland may bridge a longer gap than continuing a track. */
        const val INLAND_HOP_FACTOR = 1.5

        fun reachableKm(maxSpeedKmh: Double, elapsedMs: Long): Double =
            maxSpeedKmh * SPEED_TOLERANCE * elapsedMs.coerceAtLeast(0) / 3_600_000.0 + TIMING_SLACK_KM
    }
}
