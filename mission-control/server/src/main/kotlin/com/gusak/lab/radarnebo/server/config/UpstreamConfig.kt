package com.gusak.lab.radarnebo.server.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * alerts.in.ua limits: soft limit 8-10 requests per minute per IP, hard limit 12 per minute per IP.
 * The server never exceeds [HARD_CEILING_PER_MINUTE], which sits at the bottom of the soft range, so a
 * misconfiguration can't get the token or IP blocked.
 */
data class UpstreamConfig(
    val token: String? = null,
    val baseUrl: String = DEFAULT_BASE_URL,
    val maxPerMinute: Int = DEFAULT_PER_MINUTE,
    val pollInterval: Duration = 15.seconds,
) {
    /** Upstream budget after clamping to the safe ceiling. */
    val effectivePerMinute: Int get() = maxPerMinute.coerceIn(1, HARD_CEILING_PER_MINUTE)

    /** Poll no faster than the budget allows, evenly spaced through the minute. */
    internal val effectivePollInterval: Duration
        get() = maxOf(pollInterval, (60_000.0 / effectivePerMinute).milliseconds)

    /** The generated form, with the token masked: a config may be logged, its token never. */
    override fun toString(): String =
        "UpstreamConfig(token=${token?.let { MASKED }}, baseUrl=$baseUrl, maxPerMinute=$maxPerMinute, " +
            "pollInterval=$pollInterval)"

    internal companion object {
        const val DEFAULT_BASE_URL: String = "https://api.alerts.in.ua"
        const val HARD_CEILING_PER_MINUTE: Int = 8
        const val DEFAULT_PER_MINUTE: Int = 6
        private const val MASKED = "***"
    }
}
