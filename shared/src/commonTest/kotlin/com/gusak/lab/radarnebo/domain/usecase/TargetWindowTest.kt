package com.gusak.lab.radarnebo.domain.usecase

import com.gusak.lab.radarnebo.data.Alert
import com.gusak.lab.radarnebo.data.AppSettings
import com.gusak.lab.radarnebo.data.FavoriteLocation
import com.gusak.lab.radarnebo.data.Threat
import com.gusak.lab.radarnebo.ui.formatAgo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The targets timeline also hides alert threats that started before it, as frontline alerts stay open for weeks. */
class TargetWindowTest {
    private val now = Instant.parse("2026-10-26T00:00:00Z")

    /** An alert in Marhanets, as alerts.in.ua lists one: a missile threat that started [ago] before [now]. */
    private fun shelling(id: Long, ago: Duration) = Alert(
        id = id,
        locationTitle = "Марганець",
        locationType = "city",
        startedAt = (now - ago).toString(),
        alertType = "artillery_shelling",
        locationOblast = "Дніпропетровська область",
        locationTitleEn = "Marhanets city",
        threats = listOf(Threat("unspecified_missiles", "red", (now - ago).toString())),
    )

    @Test
    fun threatsOlderThanTheWindowAreHiddenButStillWarn() {
        val picture = picture(shelling(1, 10.minutes), shelling(2, 49.days))
        assertEquals(listOf("1:unspecified_missiles"), picture.threats.map { it.id })
        assertEquals(picture.threats, picture.mapThreats)

        val stale = picture(shelling(2, 49.days))
        assertEquals(emptyList(), stale.threats)
        assertEquals(
            listOf("2:unspecified_missiles"),
            stale.proximity.map { it.threat.id },
            "an open alert near a favorite warns however old it is",
        )
    }

    private fun picture(vararg alerts: Alert) = BuildThreatPictureUseCase()(
        alerts.toList(),
        tracks = emptyList(),
        settings = AppSettings(targetWindowMinutes = 60),
        favorites = listOf(FavoriteLocation("m", "Marhanets", 47.648, 34.627)),
        now = now.toEpochMilliseconds(),
    )

    @Test
    fun agesOverADayAreInDays() {
        assertEquals("5 h 3 min ago", formatAgo(now - 5.hours - 3.minutes, now))
        assertEquals("49 d ago", formatAgo(now - 49.days - 2.hours, now))
    }
}
