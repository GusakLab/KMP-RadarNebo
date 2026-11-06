package com.gusak.lab.radarnebo.server.application

/** What the poller last did, for the health check. */
internal data class PollerStatus(
    val lastAttemptAt: Long? = null,
    val nextPollAt: Long? = null,
    val lastError: String? = null,
)
