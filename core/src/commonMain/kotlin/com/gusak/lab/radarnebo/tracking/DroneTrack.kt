package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import kotlinx.serialization.Serializable

/**
 * Position history of a drone group or cruise missile, reconstructed from consecutive area alerts:
 * when a threat appears close to where another of the same type was just reported, it is treated as
 * the same target moving.
 */
@Serializable
data class DroneTrack(
    val id: String,
    /** Threat class being tracked; older proxies only tracked drones and omit it. */
    val type: ThreatType = ThreatType.Drones,
    val level: ThreatLevel,
    val oblastUid: Int?,
    /**
     * Still in flight: an alert for it is open and its newest report is no older than the type's
     * [TrackKinematics.staleAfterMs]. alerts.in.ua leaves alerts open for hours (frontline ones for weeks),
     * long after the target that raised them has passed, so an open alert alone does not make a target active.
     */
    val active: Boolean,
    val firstSeen: Long,
    /** Time of the newest report of this target (alert or position), not of the last poll that still listed it. */
    val lastSeen: Long,
    /** Alert threat ids (`alertId:threatType`) currently attached to this track. */
    val markerIds: List<String>,
    val samples: List<TrackSample>,
    /** How the flight ended, when the server knows; null while in flight or when only alerts are known. */
    val outcome: TrackOutcome? = null,
    /** What the server takes the target for; null from older servers. */
    val kind: TargetKind? = null,
    /**
     * The server's fused position and motion at the newest report (see [TrajectoryEstimator]); null from older
     * servers.
     */
    val estimate: TrackEstimate? = null,
    /** Smoothed past trajectory, oldest first, ending at [estimate]. Empty from older servers: draw [samples]. */
    val path: List<PathPoint> = emptyList(),
    /** Expected course from the time the server published the track; empty once it ended or before it moved. */
    val forecast: List<ForecastPoint> = emptyList(),
    /** Settlements on the forecast course, nearest first. */
    val arrivals: List<TrackArrival> = emptyList(),
)
