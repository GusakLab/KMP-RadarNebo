package com.gusak.lab.radarnebo.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * When to offer the home screen widget: on the first launch, then a week, a month, three months and a year after
 * the offer before, and never after that. Times are epoch milliseconds.
 */
class WidgetOfferSchedule(private val storage: KeyValueStorage) {
    fun isDue(nowMs: Long): Boolean {
        val offers = storage.getString(KEY_OFFERS)?.toIntOrNull() ?: 0
        if (offers == 0) return true
        val gap = GAPS.getOrNull(offers - 1) ?: return false
        val last = storage.getString(KEY_LAST_OFFER)?.toLongOrNull() ?: return true
        return nowMs - last >= gap.inWholeMilliseconds
    }

    fun recordOffer(nowMs: Long) {
        val offers = storage.getString(KEY_OFFERS)?.toIntOrNull() ?: 0
        storage.putString(KEY_OFFERS, (offers + 1).toString())
        storage.putString(KEY_LAST_OFFER, nowMs.toString())
    }

    /** Never offers again; tests and store screenshots use it to keep the launcher's dialog away. */
    fun stop() {
        storage.putString(KEY_OFFERS, (GAPS.size + 1).toString())
    }

    /** Back to a fresh install: the next launch offers the widget. */
    fun reset() {
        storage.putString(KEY_OFFERS, null)
        storage.putString(KEY_LAST_OFFER, null)
    }

    companion object {
        /** The wait after each offer before the next one. */
        val GAPS: List<Duration> = listOf(7.days, 30.days, 90.days, 365.days)

        private const val KEY_OFFERS = "widget_offers"
        private const val KEY_LAST_OFFER = "widget_last_offer_ms"
    }
}
