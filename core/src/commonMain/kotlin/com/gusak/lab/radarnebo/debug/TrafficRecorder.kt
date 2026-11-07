package com.gusak.lab.radarnebo.debug

/** Receives every exchange. It runs on request threads, so it must be quick and thread-safe. */
fun interface TrafficRecorder {
    fun record(exchange: HttpExchange)
}
