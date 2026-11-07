package com.gusak.lab.radarnebo.server.api

import kotlinx.serialization.Serializable

/** Error body, as alerts.in.ua sends it: `{"message": "..."}`. */
@Serializable
internal data class ErrorBody(val message: String)
