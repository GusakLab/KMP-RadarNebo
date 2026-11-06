package com.gusak.lab.radarnebo.debug

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

/** Records each request a client makes, with its response or the error that stopped it. [clock] is epoch ms. */
fun httpTrafficCapture(
    recorder: TrafficRecorder,
    clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
): ClientPlugin<Unit> = createClientPlugin("HttpTrafficCapture") {
    on(Send) { request ->
        val started = clock()
        val url = TrafficMasking.url(request.url.buildString())
        val method = request.method.value
        val requestHeaders = TrafficMasking.headers(request.headers.build())
        fun record(
            status: Int?,
            headers: List<Pair<String, String>>,
            type: ContentType?,
            body: String?,
            error: String?,
        ) = recorder.record(
            HttpExchange(
                startedAt = started,
                direction = HttpExchange.Direction.Outgoing,
                method = method,
                url = url,
                remoteAddress = null,
                status = status,
                durationMs = clock() - started,
                requestHeaders = requestHeaders,
                responseHeaders = headers,
                contentType = type?.toString(),
                responseBody = body,
                error = error,
            )
        )
        val call = try {
            proceed(request)
        } catch (e: CancellationException) {
            // A cancelled exchange is shown too; the cancellation itself carries on.
            record(null, emptyList(), null, null, e.message ?: e::class.simpleName)
            throw e
        } catch (e: Exception) {
            record(null, emptyList(), null, null, e.message ?: e::class.simpleName)
            throw e
        }
        val response = call.response
        val type = response.contentType()
        // Ktor keeps the body in memory, so the caller can still read it afterwards.
        val body = if (TrafficMasking.isText(type)) textBody(response) else null
        record(response.status.value, TrafficMasking.headers(response.headers), type, body?.ifEmpty { null }, null)
        call
    }
}

/** The response's text, cut to size; null when it cannot be read, which only leaves it out of the record. */
private suspend fun textBody(response: HttpResponse): String? = try {
    TrafficMasking.capped(response.bodyAsText())
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    // The body failed to arrive, or is not valid in its charset.
    null
} catch (e: IllegalArgumentException) {
    // The response names a charset this platform does not know.
    null
} catch (e: IllegalStateException) {
    // The body was streamed to the caller and cannot be read again.
    null
}
