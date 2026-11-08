package com.gusak.lab.radarnebo.server.infrastructure.observability

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.DelegatingSpanData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter

/**
 * OTLP tracing for one server. Not the global OpenTelemetry. Built only when the endpoint is non-blank,
 * so tests (which leave it unset) add no trace headers to responses.
 */
internal class ServerTracing private constructor(
    private val sdk: OpenTelemetrySdk,
) : AutoCloseable {
    val openTelemetry: OpenTelemetry get() = sdk

    override fun close() {
        sdk.close()
    }

    companion object {
        fun start(endpoint: String): ServerTracing {
            val exporter = RedactingSpanExporter(OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build())
            val provider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .build()
            val sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(provider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build()
            return ServerTracing(sdk)
        }
    }
}

/** Installed first, and only when [openTelemetry] is non-null. Captures no Authorization header. */
internal fun Application.installServerTracing(openTelemetry: OpenTelemetry) {
    install(KtorServerTelemetry) {
        setOpenTelemetry(openTelemetry)
        capturedRequestHeaders(emptyList())
        capturedResponseHeaders(emptyList())
    }
}

/**
 * The Ktor instrumentation records `url.query` and cannot be told not to. Drop it, and any authorization
 * attribute, before the span leaves the process.
 */
private class RedactingSpanExporter(
    private val delegate: SpanExporter,
) : SpanExporter {
    override fun export(spans: MutableCollection<SpanData>): CompletableResultCode =
        delegate.export(spans.map { RedactedSpan(it) })

    override fun flush(): CompletableResultCode = delegate.flush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()
}

private class RedactedSpan(delegate: SpanData) : DelegatingSpanData(delegate) {
    private val redacted: Attributes = redact(delegate.attributes)

    override fun getAttributes(): Attributes = redacted
}

private fun redact(attributes: Attributes): Attributes {
    val fullKey = AttributeKey.stringKey("url.full")
    val builder = attributes.toBuilder().removeIf { key ->
        val name = key.key.lowercase()
        name == "url.query" || name == "authorization" || name.endsWith(".authorization")
    }
    val full = attributes.get(fullKey)
    if (full != null && '?' in full) builder.put(fullKey, full.substringBefore('?'))
    return builder.build()
}
