package com.gusak.lab.radarnebo.server.domain.alert

enum class AlertType(internal val apiValue: String) {
    AirRaid("air_raid"),
    ArtilleryShelling("artillery_shelling"),
    UrbanFights("urban_fights"),
    Chemical("chemical"),
    Nuclear("nuclear");

    internal companion object {
        /**
         * An alert type alerts.in.ua does not document is read as an air raid, so it still counts for the IoT
         * statuses.
         */
        fun from(value: String?): AlertType = entries.firstOrNull { it.apiValue == value } ?: AirRaid
    }
}
