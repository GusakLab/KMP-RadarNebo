package com.gusak.lab.radarnebo.server.infrastructure.upstream

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.geo.City
import com.gusak.lab.radarnebo.geo.Regions
import com.gusak.lab.radarnebo.server.domain.alert.AirAlert
import com.gusak.lab.radarnebo.server.domain.alert.AlertLocation
import com.gusak.lab.radarnebo.server.domain.alert.LocationType
import com.gusak.lab.radarnebo.server.domain.alert.ThreatSignal
import kotlin.time.Instant

/**
 * Reads the Ukrainian Air Force's warning messages ("🏍 Реактивний БпЛА через Бровари на схід Києва",
 * "Київщина: ще один реактивний БпЛА в напрямку Березані", "Реактивний БпЛА курсом на Полтаву") as alerts:
 *
 * - the weapon from its name (БпЛА / шахед → drones, крилаті → cruise, балістика → ballistic, КАБ, ракети);
 * - where it is: "через <місто>" puts it at that city (red). Otherwise "курсом на / в напрямку / на <місто>", or a
 *   settlement in brackets ("в напрямку Вінниччини (Погребище)"), names where it is heading, which is what a yellow
 *   alert means, so it becomes a yellow alert for that city. Failing those, an oblast named as "<…>щина" puts it
 *   somewhere in the oblast (red, oblast-wide).
 *
 * The whole message is kept as the threat's text, so the classifier and the heading hints read it too. Messages
 * naming no weapon or no place the map knows ("Лівобережний масив.") yield nothing.
 */
internal object AirForceMessages {
    fun parse(id: Long, text: String, at: Instant): AirAlert? {
        val type = threatType(text.lowercase()) ?: return null
        val (location, level) = through(text)?.let { it to ThreatLevel.Red }
            ?: heading(text)?.let { it to ThreatLevel.Yellow }
            ?: oblast(text)?.let { oblastLocation(it) to ThreatLevel.Red }
            ?: return null
        return AirAlert(
            id = id,
            location = location,
            level = level,
            startedAt = at,
            threats = listOf(ThreatSignal(type, level, at, text.trim())),
        )
    }

    private fun threatType(lower: String): ThreatType? = when {
        listOf("бпла", "шахед", "дрон", "герань", "гербер").any { it in lower } -> ThreatType.Drones
        "крилат" in lower -> ThreatType.CruiseMissiles
        "балістик" in lower || "балістичн" in lower -> ThreatType.BallisticMissiles
        guidedBombs.containsMatchIn(lower) -> ThreatType.GuidedBombs
        "ракет" in lower -> ThreatType.UnspecifiedMissiles
        else -> null
    }

    /** One or two capitalised words: "Бровари", "Кривий Ріг", "Білу Церкву". */
    private const val PLACE = """([А-ЯІЇЄҐ][\p{L}'’-]+(?:\s+[А-ЯІЇЄҐ][\p{L}'’-]+)?)"""
    /** "КАБ", "КАБи", "КАБів", "КАБами", but not a word that only starts with the letters. */
    private val guidedBombs = Regex("""(?<!\p{L})каб(?:и|ів|ами|ам)?(?!\p{L})""")
    private val throughPhrase = Regex("""через\s+(?:м\.\s*)?$PLACE""")
    private val headingPhrase = Regex("""(?:курсом на|у напрямку|в напрямку|напрямок на|на)\s+(?:м\.\s*)?$PLACE""")
    private val bracketedPlace = Regex("""\(\s*$PLACE\s*\)""")

    /**
     * Villages the Air Force names that [Regions.cities] lacks: the English name, a keyword, the oblast and the
     * stem of the hromada they belong to. An alert for that hromada is placed on the map by its outline, as an
     * alerts.in.ua hromada alert is.
     */
    private val VILLAGES = listOf(
        Village("Velyka Dymerka", "Велика Димерка", 14, "Великодимерська"),
        Village("Pohrebyshche", "Погребищ", 4, "Погребищенська"),
        Village("Orativ", "Оратів", 4, "Оратівська"),
    )

    private class Village(val name: String, val keyword: String, val oblastUid: Int, val hromadaStem: String)

    /** Every place a message can name, by its keywords. */
    private val PLACES: List<Pair<List<String>, AlertLocation>> =
        Regions.cities.map { it.keywords to cityLocation(it) } +
            VILLAGES.map { listOf(it.keyword) to villageLocation(it) }

    private fun through(text: String): AlertLocation? =
        throughPhrase.findAll(text).firstNotNullOfOrNull { place(it.groupValues[1]) }

    private fun heading(text: String): AlertLocation? =
        (headingPhrase.findAll(text) + bracketedPlace.findAll(text)).firstNotNullOfOrNull { place(it.groupValues[1]) }

    /**
     * The city or village a place name in any case refers to: "Полтаву", "Дніпра", "Києва", "Білу Церкву". Keywords
     * are stems ("Полтав"), but Ukrainian also alternates vowels inside the stem (Київ → Києва, Харків → Харкова),
     * so those vowels are compared loosely and each word's ending is dropped.
     */
    private fun place(name: String): AlertLocation? {
        // "Чернігівщину" holds the stem of Чернігів, but names the oblast.
        if (oblast(name) != null) return null
        PLACES.firstOrNull { (keywords, _) -> keywords.any { name.contains(it, ignoreCase = true) } }
            ?.let { return it.second }
        val stems = name.split(Regex("""\s+""")).map { loose(it.dropLast(1)) }
        if (stems.sumOf { it.length } < 4) return null
        return PLACES.firstOrNull { (keywords, _) ->
            keywords.any { keyword ->
                val words = loose(keyword).split(' ')
                words.size == stems.size && words.zip(stems).all { (word, stem) -> word.startsWith(stem) }
            }
        }?.second
    }

    private fun loose(word: String) = word.lowercase().replace(Regex("[іїоеєи]"), "*")

    /** Oblasts as the messages name them, by stem: "Сумщина", "на Сумщині", "Харківщину". */
    private val OBLAST_STEMS = listOf(
        "Хмельниччин" to 3, "Вінниччин" to 4, "Рівненщин" to 5, "Волин" to 8, "Дніпропетровщин" to 9, "Дніпровщин" to 9,
        "Житомирщин" to 10, "Закарпатт" to 11, "Франківщин" to 13, "Прикарпатт" to 13, "Київщин" to 14,
        "Кіровоградщин" to 15, "Кропивниччин" to 15, "Луганщин" to 16, "Миколаївщин" to 17, "Одещин" to 18,
        "Полтавщин" to 19, "Сумщин" to 20, "Тернопільщин" to 21, "Харківщин" to 22, "Херсонщин" to 23,
        "Черкащин" to 24, "Чернігівщин" to 25, "Буковин" to 26, "Чернівеччин" to 26, "Львівщин" to 27,
        "Донеччин" to 28, "Крим" to 29,
    )

    private fun oblast(text: String): Int? =
        OBLAST_STEMS.firstOrNull { (stem, _) -> text.contains(stem, ignoreCase = true) }?.second

    private fun cityLocation(city: City) = AlertLocation(
        uid = "tg-${city.name}",
        // A keyword of the city, so it is found on the map from the title.
        title = city.keywords.first(),
        titleEn = city.name,
        type = LocationType.City,
        oblastUid = city.oblastUid,
        oblastTitle = Regions.oblast(city.oblastUid)?.nameUk.orEmpty(),
    )

    private fun villageLocation(village: Village) = AlertLocation(
        uid = "tg-${village.name}",
        // As alerts.in.ua titles a hromada, so the map finds its outline from the title.
        title = "${village.hromadaStem} територіальна громада",
        titleEn = village.name,
        type = LocationType.Hromada,
        oblastUid = village.oblastUid,
        oblastTitle = Regions.oblast(village.oblastUid)?.nameUk.orEmpty(),
    )

    private fun oblastLocation(uid: Int) = AlertLocation(
        uid = uid.toString(),
        title = Regions.oblast(uid)?.nameUk.orEmpty(),
        titleEn = Regions.oblast(uid)?.nameEn,
        type = LocationType.Oblast,
        oblastUid = uid,
        oblastTitle = Regions.oblast(uid)?.nameUk.orEmpty(),
    )
}
