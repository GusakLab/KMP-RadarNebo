package com.gusak.lab.radarnebo.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/** The latest [capacity] exchanges, newest last, for an inspector to show. */
@OptIn(ExperimentalAtomicApi::class)
class TrafficLog(private val capacity: Int = 200) : TrafficRecorder {
    private val _exchanges = MutableStateFlow<List<HttpExchange>>(emptyList())
    val exchanges: StateFlow<List<HttpExchange>> = _exchanges.asStateFlow()

    // Ids keep counting across clear(), so an inspector's list keys never repeat.
    private val lastId = AtomicLong(0)

    override fun record(exchange: HttpExchange) {
        val numbered = exchange.copy(id = lastId.incrementAndFetch())
        _exchanges.update { list -> (list + numbered).takeLast(capacity) }
    }

    fun clear() {
        _exchanges.update { emptyList() }
    }
}
