package com.gusak.lab.radarnebo.tracking

import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.geo.Regions

/**
 * Where an alert says the target is heading: monitoring channels write "курсом на Полтаву", "у напрямку Житомира",
 * "курс на м. Біла Церква". Only places the map knows (cities) count.
 */
internal object HeadingHints {
    private val phrase = Regex(
        """(?:курсом|курс|у напрямку|в напрямку|напрямок)\s+(?:на\s+)?""" + """(?:м\.\s*)?([^,.;()!]{2,40})""",
        RegexOption.IGNORE_CASE,
    )

    fun target(message: String?): GeoPoint? {
        val place = phrase.find(message ?: return null)?.groupValues?.get(1) ?: return null
        return Regions.cityForTitle(place)?.point
    }
}
