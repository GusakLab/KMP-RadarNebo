package com.gusak.lab.radarnebo.server.application

/**
 * Whether [SituationPoller] may call the upstream source. [Allowing] is the default, so existing poller
 * tests stay valid. An open circuit returns false and the poller waits its normal interval without fetching.
 */
internal interface UpstreamGuard {
    /** True when a fetch may start. A true result holds one permission until success, failure or [release]. */
    fun allow(): Boolean

    /** Drops a permission taken by [allow] when the call is cancelled before it finishes. */
    fun release() {}

    fun recordSuccess() {}

    fun recordFailure() {}

    companion object {
        val Allowing: UpstreamGuard = object : UpstreamGuard {
            override fun allow(): Boolean = true
        }
    }
}
