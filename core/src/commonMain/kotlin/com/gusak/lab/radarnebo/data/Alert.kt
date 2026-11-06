package com.gusak.lab.radarnebo.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Alert(
    val id: Long,
    @SerialName("location_title") val locationTitle: String = "",
    @SerialName("location_type") val locationType: String = "unknown",
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("alert_type") val alertType: String = "air_raid",
    @SerialName("location_oblast") val locationOblast: String = "",
    @SerialName("location_uid") val locationUid: String = "",
    val notes: String? = null,
    val country: String? = null,
    @SerialName("alert_level") val alertLevel: String? = null,
    @SerialName("location_title_en") val locationTitleEn: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val threats: List<Threat> = emptyList(),
    @SerialName("location_raion") @EncodeDefault(EncodeDefault.Mode.NEVER) val locationRaion: String? = null,
    /**
     * A number in alerts.in.ua; older RadarNebo servers sent it as a string, which still decodes. Despite the
     * name, alerts.in.ua puts the raion's or hromada's own UID here;
     * [com.gusak.lab.radarnebo.domain.ThreatMapper.oblastUid] finds the oblast.
     */
    @SerialName("location_oblast_uid")
    @Serializable(with = LenientIntSerializer::class)
    val locationOblastUid: Int? = null,
)
