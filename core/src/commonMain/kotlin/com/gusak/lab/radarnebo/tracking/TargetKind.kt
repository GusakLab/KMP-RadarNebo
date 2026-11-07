package com.gusak.lab.radarnebo.tracking

/**
 * What a target is. Decides its icon and which [TargetProfile]s apply.
 *
 * A class of target. [typicalAltitudeM] is how high it usually flies, in metres: neither alerts.in.ua nor the
 * simulation reports altitude, so the map draws targets at this height and the details say it is typical.
 * Null for what doesn't fly or isn't known.
 */
enum class TargetKind(val label: String, val typicalAltitudeM: Int?) {
    /** Shaheds have moved from 100-500 m up to 1.5-3 km, above small-arms and mobile-gun range. */
    AttackDrone("Attack drone", 2_000),
    JetDrone("Jet attack drone", 3_000),
    DecoyDrone("Decoy drone", 1_500),
    ReconDrone("Reconnaissance drone", 2_000),
    /** Kh-101 and Kalibr hug the terrain at 30-100 m. */
    CruiseMissile("Cruise missile", 50),
    /** Mid-course; the apogee is far higher. */
    BallisticMissile("Ballistic missile", 40_000),
    AeroballisticMissile("Aeroballistic missile", 20_000),
    /** Released at 8-12 km, gliding down. */
    GuidedBomb("Guided aerial bomb", 5_000),
    TacticalAircraft("Tactical aviation", 8_000),
    StrategicBomber("Strategic aviation", 10_000),
    MiG31K("MiG-31K", 15_000),
    AirDefense("Air defense", null),
    Unknown("Unknown threat", null),
}
