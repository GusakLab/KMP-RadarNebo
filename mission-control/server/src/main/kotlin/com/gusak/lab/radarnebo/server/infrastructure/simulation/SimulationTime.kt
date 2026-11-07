package com.gusak.lab.radarnebo.server.infrastructure.simulation

internal const val MINUTE_MS = 60_000L
internal const val HOUR_MS = 3_600_000L

/** The epoch-millisecond time [minutes] after this one. */
internal fun Long.plusMinutes(minutes: Double): Long = this + (minutes * MINUTE_MS).toLong()
