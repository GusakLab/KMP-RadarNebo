package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.domain.ThreatMarker

/**
 * alerts.in.ua reports the threat class (drones, cruise missiles, guided bombs...), not the model.
 * The model comes from the alert text when it names one (as Ukrainian alert messages often do),
 * otherwise it is the most likely model for the threat class and observed speed, labelled as such.
 */
object TargetClassifier {
    internal val Shahed: TargetProfile = TargetProfile(
        kind = TargetKind.AttackDrone,
        model = "Shahed-136 / Geran-2",
        make = "HESA (Iran); licence-built as Geran-2 at Alabuga, Russia",
        role = "One-way attack loitering munition, ~50 kg warhead",
        cruiseSpeedKmh = 150..185,
    )

    internal val Gerbera: TargetProfile = TargetProfile(
        kind = TargetKind.DecoyDrone,
        model = "Gerbera",
        make = "Russia",
        role = "Decoy to saturate air defence; sometimes reconnaissance or a small warhead",
        cruiseSpeedKmh = 120..160,
    )

    internal val JetShahed: TargetProfile = TargetProfile(
        kind = TargetKind.JetDrone,
        model = "Shahed-238 / Geran-3",
        make = "HESA (Iran); built in Russia as Geran-3",
        role = "Jet-powered one-way attack drone",
        cruiseSpeedKmh = 350..550,
    )

    internal val ReconDrone: TargetProfile = TargetProfile(
        kind = TargetKind.ReconDrone,
        model = "Orlan-10 / ZALA / Supercam",
        make = "Russia",
        role = "Reconnaissance and artillery spotting near the front",
        cruiseSpeedKmh = 90..150,
    )

    internal val CruiseMissile: TargetProfile = TargetProfile(
        kind = TargetKind.CruiseMissile,
        model = "Kh-101 / Kh-555 / 3M14 Kalibr",
        make = "Russia; air-launched from Tu-95MS, Kalibr from ships and submarines",
        role = "Subsonic cruise missile flying low along a winding route",
        cruiseSpeedKmh = 700..950,
    )

    internal val BallisticMissile: TargetProfile = TargetProfile(
        kind = TargetKind.BallisticMissile,
        model = "9M723 Iskander-M / KN-23",
        make = "Russia; KN-23 supplied by North Korea",
        role = "Short-range ballistic missile, minutes from launch to impact",
        cruiseSpeedKmh = 5_000..7_500,
    )

    internal val Kinzhal: TargetProfile = TargetProfile(
        kind = TargetKind.AeroballisticMissile,
        model = "Kh-47M2 Kinzhal",
        make = "Russia",
        role = "Air-launched aeroballistic missile carried by the MiG-31K",
        cruiseSpeedKmh = 4_000..12_000,
    )

    internal val GuidedBomb: TargetProfile = TargetProfile(
        kind = TargetKind.GuidedBomb,
        model = "KAB / FAB with UMPK kit",
        make = "Russia",
        role = "Glide bomb released by aircraft 40–70 km from its target",
        cruiseSpeedKmh = 500..900,
    )

    internal val TacticalAircraft: TargetProfile = TargetProfile(
        kind = TargetKind.TacticalAircraft,
        model = "Su-34 / Su-35",
        make = "Russia",
        role = "Fighter-bombers launching guided bombs and missiles near the front",
        cruiseSpeedKmh = 800..1_400,
    )

    internal val StrategicBomber: TargetProfile = TargetProfile(
        kind = TargetKind.StrategicBomber,
        model = "Tu-95MS / Tu-22M3",
        make = "Russia",
        role = "Long-range bombers that launch Kh-101 and Kh-22 cruise missiles",
        cruiseSpeedKmh = 700..900,
    )

    internal val MiG31K: TargetProfile = TargetProfile(
        kind = TargetKind.MiG31K,
        model = "MiG-31K",
        make = "Russia",
        role = "Interceptor modified to carry the Kinzhal; its take-off triggers a nationwide alert",
        cruiseSpeedKmh = 1_500..3_000,
    )

    internal val AirDefense: TargetProfile = TargetProfile(
        kind = TargetKind.AirDefense,
        model = "Air defense active",
        make = "Ukrainian Air Force",
        role = "Air defense is engaging targets nearby; stay in shelter",
        cruiseSpeedKmh = null,
    )

    internal val Unknown: TargetProfile = TargetProfile(
        kind = TargetKind.Unknown,
        model = "Unknown threat",
        make = "–",
        role = "Threat type not reported",
        cruiseSpeedKmh = null,
    )

    /** Every profile, one per [TargetKind], in the order of [TargetKind]. */
    val all: List<TargetProfile> = listOf(
        Shahed, JetShahed, Gerbera, ReconDrone, CruiseMissile, BallisticMissile, Kinzhal,
        GuidedBomb, TacticalAircraft, StrategicBomber, MiG31K, AirDefense, Unknown,
    )

    internal fun profile(kind: TargetKind): TargetProfile = all.first { it.kind == kind }

    /** Words that name a model in an alert text, and the model they name. */
    private data class NamedModel(val words: List<String>, val profile: TargetProfile)

    // Most specific first: "реактивний шахед" must not be classified as a plain Shahed.
    private val droneKeywords = listOf(
        NamedModel(listOf("реактивн", "shahed-238", "герань-3", "geran-3"), JetShahed),
        NamedModel(listOf("гербер", "gerbera", "імітатор"), Gerbera),
        NamedModel(listOf("розвідувальн", "орлан", "orlan", "zala", "зала", "supercam", "суперкам"), ReconDrone),
        NamedModel(listOf("шахед", "shahed", "герань", "geran"), Shahed),
    )
    private val missileKeywords = listOf(
        NamedModel(listOf("кинджал", "kinzhal", "х-47", "kh-47"), Kinzhal),
        NamedModel(listOf("іскандер", "iskander", "kn-23", "балістичн", "ballistic"), BallisticMissile),
        NamedModel(
            listOf("х-101", "kh-101", "х-555", "kh-555", "калібр", "kalibr", "крилат", "cruise"),
            CruiseMissile,
        ),
    )

    /** The server's [DroneTrack.kind] wins over the text; the model still comes from the text when it agrees. */
    fun classify(track: DroneTrack, speedKmh: Double? = null): TargetProfile {
        val fromText = classify(track.type, track.samples.mapNotNull { it.message }, speedKmh)
        val kind = track.kind ?: return fromText
        return if (fromText.kind == kind) fromText else profile(kind).copy(source = TargetProfile.Source.Probable)
    }

    fun classify(marker: ThreatMarker): TargetProfile = classify(marker.type, listOfNotNull(marker.message))

    fun classify(type: ThreatType, messages: List<String>, speedKmh: Double? = null): TargetProfile {
        val text = messages.joinToString(" ").lowercase()
        fun named(keywords: List<NamedModel>): TargetProfile? =
            keywords.firstOrNull { (words, _) -> words.any { it in text } }?.profile
        fun probable(profile: TargetProfile): TargetProfile = profile.copy(source = TargetProfile.Source.Probable)
        return when (type) {
            // Unnamed long-range drone alerts are overwhelmingly Shahed-class; a very fast group suggests a jet.
            ThreatType.Drones -> named(droneKeywords)
                ?: probable(if (speedKmh != null && speedKmh > 300) JetShahed else Shahed)
            ThreatType.CruiseMissiles -> named(missileKeywords) ?: probable(CruiseMissile)
            ThreatType.BallisticMissiles -> named(missileKeywords) ?: probable(BallisticMissile)
            ThreatType.UnspecifiedMissiles -> named(missileKeywords)
                ?: probable(CruiseMissile.copy(model = "Missile, type not reported"))
            ThreatType.GuidedBombs -> GuidedBomb.reportedIf(text, "каб", "kab", "умпк", "umpk")
            ThreatType.TacticalAviation -> TacticalAircraft.reportedIf(text, "су-34", "su-34", "су-35", "su-35")
            ThreatType.StrategicAviation ->
                StrategicBomber.reportedIf(text, "ту-95", "tu-95", "ту-22", "tu-22", "ту-160", "tu-160")
            ThreatType.Mig31k -> MiG31K
            ThreatType.AirDefense -> AirDefense
            ThreatType.Unknown -> Unknown
        }
    }

    /** The threat class fixes the kind; the model only counts as reported when the text names it. */
    private fun TargetProfile.reportedIf(text: String, vararg names: String) =
        if (names.any { it in text }) this else copy(source = TargetProfile.Source.Probable)
}
