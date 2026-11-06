package com.gusak.lab.radarnebo.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Threat(
    @SerialName("threat_type") val threatType: String,
    val level: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("source_message") @EncodeDefault(EncodeDefault.Mode.NEVER) val sourceMessage: String? = null,
)
