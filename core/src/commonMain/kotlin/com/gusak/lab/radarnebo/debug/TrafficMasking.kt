package com.gusak.lab.radarnebo.debug

import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

object TrafficMasking {
    internal const val MASK: String = "***"
    private val tokenParameter = Regex("([?&]token=)[^&]*")

    /** alerts.in.ua-style clients may pass their key as `?token=`. */
    fun url(url: String): String = url.replace(tokenParameter, "$1$MASK")

    fun headers(headers: Headers): List<HttpHeader> = headers.entries().flatMap { (name, values) ->
        values.map { HttpHeader(name, if (name.equals(HttpHeaders.Authorization, ignoreCase = true)) MASK else it) }
    }

    fun isText(type: ContentType?): Boolean {
        if (type == null) return false
        return type.match(ContentType.Application.Json) || type.contentType == "text" ||
            type.contentSubtype.endsWith("+json")
    }

    fun capped(body: String): String =
        if (body.length > HttpExchange.MAX_BODY_CHARS) body.take(HttpExchange.MAX_BODY_CHARS) + "\n… (cut)" else body
}
