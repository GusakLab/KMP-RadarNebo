package com.gusak.lab.radarnebo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlertsMeta(
    /** When the data last changed, as `2026/10/26 08:38:08 +0000`. */
    @SerialName("last_updated_at") val lastUpdatedAt: String,
    val type: String = "full",
)
