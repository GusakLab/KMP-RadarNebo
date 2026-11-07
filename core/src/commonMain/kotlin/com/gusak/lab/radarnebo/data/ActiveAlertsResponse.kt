package com.gusak.lab.radarnebo.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Response of `GET /v1/alerts/active.json` from https://devs.alerts.in.ua. Properties are declared in
 * alerts.in.ua's key order, and those it leaves out when empty are left out here too, so the server's
 * output reads like the original.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ActiveAlertsResponse(
    val alerts: List<Alert> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val meta: AlertsMeta? = null,
)
