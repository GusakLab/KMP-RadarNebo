package com.gusak.lab.radarnebo.server.infrastructure.simulation

import com.gusak.lab.radarnebo.geo.GeoMath
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.geo.Regions

/** Where the attacks come from and what they aim at; coordinates are approximate. */
internal object StrikeGeography {
    private fun site(name: String, originUk: String, lat: Double, lon: Double): LaunchSite =
        LaunchSite(name, originUk, GeoPoint(lat, lon))

    /** Shahed launch sites in Russia and occupied Crimea and Donetsk. */
    val DRONE_SITES: List<LaunchSite> = listOf(
        site("Kursk", "Курської області", 51.75, 36.30),
        site("Oryol", "Орловської області", 52.97, 36.08),
        site("Navlya", "Брянської області", 52.83, 34.50),
        site("Shatalovo", "Смоленської області", 54.33, 32.47),
        site("Millerovo", "Ростовської області", 48.92, 40.40),
        site("Primorsko-Akhtarsk", "Краснодарського краю", 46.05, 38.17),
        site("Cape Chauda", "тимчасово окупованого Криму", 45.00, 35.84),
        site("Hvardiiske", "тимчасово окупованого Криму", 45.11, 33.97),
        site("Donetsk", "тимчасово окупованої Донеччини", 48.07, 37.74),
    )

    /** Tu-95MS launch lines for Kh-101 cruise missiles. */
    val BOMBER_LAUNCH_LINES: List<LaunchSite> = listOf(
        site("Volgodonsk", "Волгодонська (Ту-95МС)", 47.60, 42.10),
        site("Caspian Sea", "акваторії Каспійського моря (Ту-95МС)", 46.10, 47.60),
        site("Engels", "Саратовської області (Ту-95МС)", 51.30, 45.40),
    )

    /** Black Sea Fleet ships and submarines launching Kalibr cruise missiles. */
    val KALIBR_CARRIERS: List<LaunchSite> = listOf(
        site("Novorossiysk", "акваторії Чорного моря (Новоросійськ)", 44.45, 37.55),
        site("Black Sea west of Crimea", "акваторії Чорного моря", 44.40, 32.60),
    )

    /** Iskander-M and KN-23 launch areas; ballistic missiles reach about 500 km (KN-23: 700 km). */
    val BALLISTIC_SITES: List<LaunchSite> = listOf(
        site("Kursk", "Курської області", 51.70, 36.20),
        site("Belgorod", "Бєлгородської області", 50.60, 36.60),
        site("Bryansk", "Брянської області", 52.95, 34.00),
        site("Voronezh", "Воронезької області", 51.60, 39.20),
        site("Rostov", "Ростовської області", 47.25, 39.00),
        site("Dzhankoi", "тимчасово окупованого Криму", 45.70, 34.40),
    )

    /** MiG-31K launch areas for the Kinzhal. */
    val KINZHAL_LAUNCH_AREAS: List<LaunchSite> = listOf(
        site("Lipetsk", "Липецької області (МіГ-31К)", 52.60, 39.60),
        site("Black Sea", "акваторії Чорного моря (МіГ-31К)", 44.30, 35.20),
    )

    /** Settlements on the border, coast or front over which drone groups cross in. */
    val DRONE_GATES: List<String> = listOf(
        "Novhorod-Siverskyi", "Hlukhiv", "Shostka", "Sumy", "Trostianets", "Okhtyrka", "Zolochiv", "Chuhuiv",
        "Kupiansk", "Izium", "Kramatorsk", "Zaporizhzhia", "Nikopol", "Kherson", "Mykolaiv", "Pivdenne", "Izmail",
    )

    /** Settlements on the border, coast or front through which cruise missiles enter. */
    val CRUISE_GATES: List<String> = listOf(
        "Novhorod-Siverskyi", "Shostka", "Hlukhiv", "Okhtyrka", "Kupiansk", "Izium", "Zaporizhzhia", "Nikopol",
    )
    val KALIBR_GATES: List<String> =
        listOf("Pivdenne", "Bilhorod-Dnistrovskyi", "Chornomorsk", "Mykolaiv", "Izmail", "Odesa")

    /** What massive strikes aim at: energy, defence industry, airfields and the capital. */
    val OBJECTIVES: List<Objective> = listOf(
        Objective("Kyiv", 6),
        Objective("Lviv", 2),
        Objective("Ivano-Frankivsk", 1),
        Objective("Starokostiantyniv", 2),
        Objective("Khmelnytskyi", 1),
        Objective("Ladyzhyn", 1),
        Objective("Vinnytsia", 2),
        Objective("Zhytomyr", 1),
        Objective("Bila Tserkva", 1),
        Objective("Rivne", 1),
        Objective("Lutsk", 1),
        Objective("Ternopil", 1),
        Objective("Odesa", 3),
        Objective("Izmail", 1),
        Objective("Mykolaiv", 1),
        Objective("Kryvyi Rih", 2),
        Objective("Cherkasy", 1),
        Objective("Kropyvnytskyi", 1),
        Objective("Kremenchuk", 2),
        Objective("Poltava", 1),
        Objective("Dnipro", 3),
        Objective("Pavlohrad", 1),
        Objective("Kharkiv", 3),
        Objective("Zaporizhzhia", 2),
        Objective("Chernihiv", 1),
        Objective("Sumy", 1),
    )

    /** Cities near the border and front that small daytime drone groups go for. */
    val FRONTLINE_DRONE_TARGETS: List<String> = listOf(
        "Kharkiv", "Sumy", "Odesa", "Zaporizhzhia", "Dnipro", "Mykolaiv", "Kherson", "Chernihiv", "Poltava",
        "Kryvyi Rih", "Izmail", "Pavlohrad", "Kramatorsk", "Sloviansk", "Shostka", "Konotop", "Nikopol",
    )

    private val BELGOROD = site("Belgorod", "Бєлгородської області", 50.60, 36.60)
    private val KURSK = site("Kursk", "Курської області", 51.70, 35.90)
    private val OCCUPIED_LUHANSK = site("occupied Luhansk Oblast", "тимчасово окупованої Луганщини", 49.30, 38.60)
    private val OCCUPIED_DONETSK = site("occupied Donetsk Oblast", "тимчасово окупованої Донеччини", 48.40, 38.30)
    private val OCCUPIED_ZAPORIZHZHIA =
        site("occupied Zaporizhzhia Oblast", "тимчасово окупованої Запорізької області", 47.30, 35.40)
    private val OCCUPIED_KHERSON =
        site("occupied left bank, Kherson Oblast", "окупованого лівобережжя Херсонщини", 46.40, 33.10)

    /** Front-line and border towns under guided bomb attack, from the directions the bombers fly. */
    val KAB_TARGETS: List<KabTarget> = listOf(
        KabTarget("Kharkiv", 0..45, 55..70, BELGOROD, weight = 4),
        KabTarget("Sumy", 40..90, 48..60, KURSK, weight = 3),
        KabTarget("Okhtyrka", 50..70, 55..65, BELGOROD),
        KabTarget("Trostianets", 60..90, 45..55, BELGOROD),
        KabTarget("Zolochiv", -30..30, 30..50, BELGOROD),
        KabTarget("Bohodukhiv", 0..30, 45..60, BELGOROD),
        KabTarget("Chuhuiv", 0..25, 65..80, BELGOROD),
        KabTarget("Kupiansk", 60..120, 40..60, OCCUPIED_LUHANSK, weight = 2),
        KabTarget("Izium", 80..130, 50..65, OCCUPIED_LUHANSK),
        KabTarget("Sloviansk", 90..140, 50..65, OCCUPIED_DONETSK),
        KabTarget("Kramatorsk", 100..160, 45..65, OCCUPIED_DONETSK, weight = 2),
        KabTarget("Zaporizhzhia", 150..200, 40..60, OCCUPIED_ZAPORIZHZHIA, weight = 3),
        KabTarget("Nikopol", 150..210, 25..45, OCCUPIED_ZAPORIZHZHIA),
        KabTarget("Marhanets", 160..220, 25..40, OCCUPIED_ZAPORIZHZHIA),
        KabTarget("Kherson", 110..200, 25..45, OCCUPIED_KHERSON, weight = 2),
    )

    /** Where reconnaissance drones work: from the occupied side over the towns behind the front. */
    val RECON_SECTORS: List<ReconSector> = listOf(
        ReconSector(GeoPoint(49.75, 38.25), listOf("Kupiansk", "Izium", "Balakliia", "Chuhuiv")),
        ReconSector(GeoPoint(48.60, 38.15), listOf("Kramatorsk", "Sloviansk", "Izium")),
        ReconSector(GeoPoint(47.40, 35.75), listOf("Zaporizhzhia")),
        ReconSector(GeoPoint(47.50, 34.60), listOf("Nikopol", "Marhanets")),
        ReconSector(GeoPoint(46.55, 32.90), listOf("Kherson")),
        ReconSector(GeoPoint(51.15, 35.40), listOf("Sumy", "Trostianets", "Lebedyn")),
    )

    /** Radar reach beyond the border, for low-flying targets. */
    private const val COVERAGE_KM = 40.0

    /** Ukrainian radars cover the country up to the front line, but not Crimea. */
    private val UNCOVERED_REGIONS = setOf(SimulatedLocations.CRIMEA_UID, Regions.SEVASTOPOL_UID)

    /** Whether air surveillance sees a low-flying target at [point]: over the mainland, or near a border settlement. */
    fun inCoverage(point: GeoPoint): Boolean {
        Regions.oblastByPolygon(point)?.let { return it !in UNCOVERED_REGIONS }
        return SimulatedLocations.SETTLEMENTS.any { GeoMath.distanceKm(point, it.city.point) <= COVERAGE_KM }
    }

    fun point(settlement: String): GeoPoint = SimulatedLocations.settlement(settlement).city.point
}
