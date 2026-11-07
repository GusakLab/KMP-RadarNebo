package com.gusak.lab.radarnebo.data

enum class ThreatType(val apiValue: String, val label: String) {
    Drones("drones", "Drones (Shahed/UAV)"),
    BallisticMissiles("ballistic_missiles", "Ballistic missiles"),
    CruiseMissiles("cruise_missiles", "Cruise missiles"),
    UnspecifiedMissiles("unspecified_missiles", "Missiles"),
    GuidedBombs("guided_aerial_bombs", "Guided aerial bombs"),
    TacticalAviation("tactic_aircraft_activity", "Tactical aviation"),
    StrategicAviation("strategic_aircraft_activity", "Strategic aviation"),
    Mig31k("mig31k_departure", "MiG-31K departure"),
    AirDefense("air_defense", "Air defense active"),
    Unknown("unknown", "Unknown threat");

    companion object {
        fun from(value: String): ThreatType = entries.firstOrNull { it.apiValue == value } ?: Unknown
    }
}
