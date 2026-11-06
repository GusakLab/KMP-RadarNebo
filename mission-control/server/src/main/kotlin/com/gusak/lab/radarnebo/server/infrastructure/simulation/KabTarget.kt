package com.gusak.lab.radarnebo.server.infrastructure.simulation

/**
 * A front-line town under guided-bomb attack. Bombs are released [rangeKm] away at a bearing in
 * [bearingDeg] (clockwise from north, seen from the town), which puts the release point over [launchArea].
 */
internal class KabTarget(
    val settlement: String,
    val bearingDeg: IntRange,
    val rangeKm: IntRange,
    val launchArea: LaunchSite,
    val weight: Int = 1,
)
