package com.gusak.lab.radarnebo.debug

import io.ktor.http.HttpHeaders

/**
 * One HTTP exchange, as the request inspectors (the app's debug window, the server console) show it.
 * Credentials are masked before an exchange is created.
 */
data class HttpExchange(
    val startedAt: Long,
    val direction: Direction,
    val method: String,
    val url: String,
    /** Who sent an [Direction.Incoming] request; null for requests this side made. */
    val remoteAddress: String?,
    val status: Int?,
    val durationMs: Long,
    val requestHeaders: List<HttpHeader>,
    val responseHeaders: List<HttpHeader>,
    val contentType: String?,
    /** Text bodies only, cut at [MAX_BODY_CHARS]; null for binary, streamed or empty bodies. */
    val responseBody: String?,
    /** Why the exchange failed before a response arrived. */
    val error: String? = null,
    /** Assigned by [TrafficLog]; increases with each exchange. */
    val id: Long = 0,
) {
    enum class Direction {
        /** A request this side received (a client calling the server). */
        Incoming,

        /** A request this side made (the app calling its server, the server calling alerts.in.ua). */
        Outgoing,
    }

    val userAgent: String? get() = header(requestHeaders, HttpHeaders.UserAgent)

    /** The URL without scheme and host; [url] already is one for incoming requests. */
    val path: String get() = if ("://" in url) "/" + url.substringAfter("://").substringAfter('/', "") else url
    val host: String? get() = if ("://" in url) url.substringAfter("://").substringBefore('/') else null

    companion object {
        internal const val MAX_BODY_CHARS: Int = 2_000_000

        private fun header(headers: List<HttpHeader>, name: String) =
            headers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value
    }
}
