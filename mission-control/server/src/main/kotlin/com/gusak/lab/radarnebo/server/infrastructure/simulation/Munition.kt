package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.tracking.TrackOutcome

/**
 * A weapon used in the simulated attacks, with the performance and air-defence figures that drive it.
 * [message] is the warning text as Ukrainian monitoring channels word it; it names the model, so the
 * app's classifier recognises it.
 */
internal enum class Munition(
    val weaponClass: WeaponClass,
    val threat: ThreatType,
    /** Cruise speed; for ballistic missiles the average over the whole flight. */
    val speedKmh: ClosedFloatingPointRange<Double>,
    /** Chance air defence shoots it down or jams it. */
    val interceptChance: Double,
    /** Chance contact is lost without an interception: decoys drop off radar or come down on their own. */
    val lossChance: Double,
    /** Where along its path (as a fraction) an interception happens. */
    val interceptWindow: ClosedFloatingPointRange<Double>,
    /** Largest surveillance position error, km. */
    val positionErrorKm: Double,
    val warning: WarningProfile,
    val message: String,
    /** How a flight that reaches the end of its path ends. */
    val completesAs: TrackOutcome = TrackOutcome.Impact,
) {
    Shahed(
        WeaponClass.Drone, ThreatType.Drones, 150.0..185.0,
        interceptChance = 0.82, lossChance = 0.06, interceptWindow = 0.45..0.98,
        positionErrorKm = 0.4, warning = WarningProfile.DRONE,
        message = "Ударні БпЛА типу «Шахед» (Shahed-136 / Герань-2)",
    ),
    Gerbera(
        WeaponClass.Drone, ThreatType.Drones, 120.0..160.0,
        interceptChance = 0.40, lossChance = 0.45, interceptWindow = 0.3..0.95,
        positionErrorKm = 0.5, warning = WarningProfile.DRONE,
        message = "БпЛА-імітатори типу «Гербера»",
    ),
    JetShahed(
        WeaponClass.Drone, ThreatType.Drones, 420.0..520.0,
        interceptChance = 0.55, lossChance = 0.05, interceptWindow = 0.5..0.97,
        positionErrorKm = 0.6, warning = WarningProfile.JET_DRONE,
        message = "Реактивні ударні БпЛА «Герань-3» (Shahed-238)",
    ),
    Orlan(
        WeaponClass.Drone, ThreatType.Drones, 90.0..130.0,
        interceptChance = 0.30, lossChance = 0.0, interceptWindow = 0.25..0.8,
        positionErrorKm = 0.3, warning = WarningProfile.RECON_DRONE,
        message = "Розвідувальний БпЛА «Орлан-10» на лінії фронту",
        completesAs = TrackOutcome.Lost,
    ),
    Kh101(
        WeaponClass.CruiseMissile, ThreatType.CruiseMissiles, 720.0..800.0,
        interceptChance = 0.86, lossChance = 0.02, interceptWindow = 0.55..0.98,
        positionErrorKm = 0.3, warning = WarningProfile.CRUISE_MISSILE,
        message = "Крилаті ракети Х-101, пуски з Ту-95МС",
    ),
    Kalibr(
        WeaponClass.CruiseMissile, ThreatType.CruiseMissiles, 750.0..850.0,
        interceptChance = 0.80, lossChance = 0.02, interceptWindow = 0.5..0.98,
        positionErrorKm = 0.3, warning = WarningProfile.CRUISE_MISSILE,
        message = "Крилаті ракети «Калібр» з акваторії Чорного моря",
    ),
    IskanderM(
        WeaponClass.BallisticMissile, ThreatType.BallisticMissiles, 4_200.0..5_400.0,
        interceptChance = 0.40, lossChance = 0.0, interceptWindow = 0.9..0.99,
        positionErrorKm = 0.0, warning = WarningProfile.NONE,
        message = "Балістичні ракети «Іскандер-М»",
    ),
    Kn23(
        WeaponClass.BallisticMissile, ThreatType.BallisticMissiles, 4_500.0..5_600.0,
        interceptChance = 0.30, lossChance = 0.0, interceptWindow = 0.9..0.99,
        positionErrorKm = 0.0, warning = WarningProfile.NONE,
        message = "Балістичні ракети KN-23",
    ),
    Kinzhal(
        WeaponClass.BallisticMissile, ThreatType.BallisticMissiles, 4_500.0..5_800.0,
        interceptChance = 0.55, lossChance = 0.0, interceptWindow = 0.92..0.99,
        positionErrorKm = 0.0, warning = WarningProfile.NONE,
        message = "Аеробалістичні ракети Х-47М2 «Кинджал» з МіГ-31К",
    ),
    Kab(
        WeaponClass.GuidedBomb, ThreatType.GuidedBombs, 550.0..750.0,
        interceptChance = 0.0, lossChance = 0.0, interceptWindow = 1.0..1.0,
        positionErrorKm = 0.2, warning = WarningProfile.NONE,
        message = "Пуски КАБ (УМПК) з Су-34",
    ),
}
