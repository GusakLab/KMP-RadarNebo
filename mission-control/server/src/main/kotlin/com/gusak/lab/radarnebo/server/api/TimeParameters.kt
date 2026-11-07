package com.gusak.lab.radarnebo.server.api

import kotlin.time.Instant

/** Epoch milliseconds or ISO-8601. */
internal fun parseTime(value: String): Long? = value.toLongOrNull() ?: Instant.parseOrNull(value)?.toEpochMilliseconds()
