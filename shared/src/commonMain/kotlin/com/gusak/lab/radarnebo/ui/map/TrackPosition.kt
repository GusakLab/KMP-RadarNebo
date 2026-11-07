package com.gusak.lab.radarnebo.ui.map

import com.gusak.lab.radarnebo.tracking.DroneTrack
import com.gusak.lab.radarnebo.tracking.InterpolatedPosition

/** A tracked target and where the map shows it at the moment. */
internal data class TrackPosition(val track: DroneTrack, val position: InterpolatedPosition)
