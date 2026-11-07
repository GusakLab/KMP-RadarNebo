package com.gusak.lab.radarnebo.server.api

import io.ktor.http.HttpStatusCode

/** Ends a request with [status] and an [ErrorBody]; turned into the response by StatusPages. */
internal open class ApiException(val status: HttpStatusCode, override val message: String) : RuntimeException(message)

internal fun badRequest(message: String): Nothing = throw ApiException(HttpStatusCode.BadRequest, message)

internal fun notFound(message: String): Nothing = throw ApiException(HttpStatusCode.NotFound, message)
