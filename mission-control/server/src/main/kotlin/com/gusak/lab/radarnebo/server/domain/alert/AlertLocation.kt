package com.gusak.lab.radarnebo.server.domain.alert

/**
 * A place alerts are declared for, identified by its alerts.in.ua UID. Oblasts, Kyiv city and Sevastopol
 * have UIDs 3-31; raions, hromadas and cities have larger ones.
 */
data class AlertLocation(
    val uid: String,
    val title: String,
    val titleEn: String?,
    val type: LocationType,
    val oblastUid: Int?,
    val oblastTitle: String,
    val raionTitle: String? = null,
    /**
     * `location_oblast_uid` as the source wrote it, served back unchanged. alerts.in.ua puts the location's own
     * UID there, not the oblast's; [oblastUid] is the real oblast.
     */
    val sourceOblastUid: Int? = null,
) {
    /** True for an oblast, or a city that is a region of its own (Kyiv, Sevastopol). */
    internal val isRegion: Boolean get() = oblastUid != null && uid == oblastUid.toString()
}
