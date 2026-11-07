package com.gusak.lab.radarnebo.debug

/** One header of an [HttpExchange], as the request inspectors show it. Credentials are masked already. */
data class HttpHeader(val name: String, val value: String)
