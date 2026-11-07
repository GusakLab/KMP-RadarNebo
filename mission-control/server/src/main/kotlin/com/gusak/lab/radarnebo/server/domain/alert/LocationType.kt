package com.gusak.lab.radarnebo.server.domain.alert

/** Administrative level an alert is declared for, as alerts.in.ua reports it. */
enum class LocationType(internal val apiValue: String) {
    Oblast("oblast"),
    Raion("raion"),
    City("city"),
    Hromada("hromada"),
    Unknown("unknown");

    internal companion object {
        fun from(value: String?): LocationType = entries.firstOrNull { it.apiValue == value } ?: Unknown
    }
}
