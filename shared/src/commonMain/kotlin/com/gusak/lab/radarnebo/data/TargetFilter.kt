package com.gusak.lab.radarnebo.data

import com.gusak.lab.radarnebo.tracking.DroneTrack
import com.gusak.lab.radarnebo.tracking.TargetClassifier
import com.gusak.lab.radarnebo.tracking.TargetKind
import com.gusak.lab.radarnebo.tracking.TargetProfile
import com.gusak.lab.radarnebo.tracking.TrackInterpolator

/** Which tracked targets the Targets list and the map show. Alerts and proximity warnings are never filtered. */
data class TargetFilter(
    val hiddenKinds: Set<TargetKind> = emptySet(),
    /** Hides targets whose type is only inferred, not named in the alert. */
    val hideProbable: Boolean = false,
) {
    internal val isActive: Boolean get() = hiddenKinds.isNotEmpty() || hideProbable

    internal fun allows(profile: TargetProfile): Boolean =
        profile.kind !in hiddenKinds && !(hideProbable && profile.source == TargetProfile.Source.Probable)

    /** [now] is server time; the speed at that moment can change a drone's probable type. */
    internal fun allows(track: DroneTrack, now: Long): Boolean = !isActive || allows(profileAt(track, now))

    internal companion object {
        fun profileAt(track: DroneTrack, now: Long): TargetProfile =
            TargetClassifier.classify(track, TrackInterpolator.positionAt(track, now)?.speedKmh)
    }
}
