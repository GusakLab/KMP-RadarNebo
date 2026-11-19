package com.gusak.lab.radarnebo.server.application

import com.gusak.lab.radarnebo.server.domain.frontline.Frontline
import com.gusak.lab.radarnebo.server.domain.frontline.FrontlineSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * The frontline, read from [source] when a client asks and kept for [refreshEvery]: the frontline moves over
 * days, so the source sees at most one request per period however many clients ask. A failed read is tried
 * again after [retryAfter], and until it succeeds clients keep getting the last frontline read.
 */
internal class FrontlineService(
    private val source: FrontlineSource,
    private val clock: () -> Long = System::currentTimeMillis,
    private val refreshEvery: Duration = 1.days,
    private val retryAfter: Duration = 5.minutes,
) {
    // Held while reading the source, so clients asking at the same moment cause one request, not one each.
    private val mutex = Mutex()
    private var frontline: Frontline? = null
    private var nextFetchAtMs = Long.MIN_VALUE

    /** The latest frontline; null until the source has been read once. */
    suspend fun current(): Frontline? = mutex.withLock {
        val now = clock()
        if (now >= nextFetchAtMs) {
            val areas = source.fetch()
            if (areas != null) frontline = Frontline(now, areas)
            nextFetchAtMs = now + (if (areas != null) refreshEvery else retryAfter).inWholeMilliseconds
        }
        frontline
    }
}
