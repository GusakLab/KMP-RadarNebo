package com.gusak.lab.radarnebo.server.config

/** What the server asks of its own clients (the app). */
data class ClientApiConfig(
    /** When non-empty, clients must send `Authorization: Bearer <key>` or `?token=<key>`, like alerts.in.ua. */
    val keys: Set<String> = emptySet(),
    val requestsPerMinute: Int = 120,
    /** alerts.in.ua allows 2 history requests per minute; the server keeps the same contract. */
    val historyRequestsPerMinute: Int = 2,
    /** Track archive reads are the heaviest calls (up to a week of tracks each), so they get their own budget. */
    val archiveRequestsPerMinute: Int = 10,
    /**
     * Behind a reverse proxy (Caddy in the deployment), every call comes from the proxy, so the client's IP is
     * taken from the last `X-Forwarded-For` entry, the one the proxy added. Only set it when the server can't be
     * reached around the proxy: otherwise a client could pick its own IP and dodge the rate limits.
     */
    val trustProxyHeaders: Boolean = false,
) {
    init {
        require(requestsPerMinute > 0) { "CLIENT_REQUESTS_PER_MINUTE must be positive" }
        require(historyRequestsPerMinute > 0) { "HISTORY_REQUESTS_PER_MINUTE must be positive" }
        require(archiveRequestsPerMinute > 0) { "ARCHIVE_REQUESTS_PER_MINUTE must be positive" }
    }

    /** The generated form, with only the number of keys: a config may be logged, its keys never. */
    override fun toString(): String =
        "ClientApiConfig(keys=<${keys.size} masked>, requestsPerMinute=$requestsPerMinute, " +
            "historyRequestsPerMinute=$historyRequestsPerMinute, archiveRequestsPerMinute=$archiveRequestsPerMinute, " +
            "trustProxyHeaders=$trustProxyHeaders)"
}
