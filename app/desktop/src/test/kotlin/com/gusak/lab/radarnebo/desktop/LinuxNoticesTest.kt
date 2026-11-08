package com.gusak.lab.radarnebo.desktop

import com.gusak.lab.radarnebo.data.FavoriteLocation
import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.data.ThreatType
import com.gusak.lab.radarnebo.domain.ProximityHit
import com.gusak.lab.radarnebo.domain.ThreatMarker
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.ui.AppEvents
import com.gusak.lab.radarnebo.ui.AppRoute
import com.gusak.lab.radarnebo.ui.UiState
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/** Which new threats become Linux notifications, and which click opens which screen. */
class LinuxNoticesTest {
    @Test
    fun anEmptyLoadingPictureDoesNotBecomeTheBaseline() {
        val planner = LinuxNoticePlanner()

        planner.take(UiState())

        assertEquals(emptyList(), planner.take(picture(threats = listOf(threat("a")))))
    }

    @Test
    fun theFirstSuccessfulPictureSendsNothing() {
        val notices = LinuxNoticePlanner().take(picture(threats = listOf(threat("a"))))

        assertEquals(emptyList(), notices)
    }

    @Test
    fun theSamePictureAgainSendsNothing() {
        val planner = LinuxNoticePlanner()
        planner.take(picture(threats = listOf(threat("a"))))

        assertEquals(emptyList(), planner.take(picture(threats = listOf(threat("a")))))
    }

    @Test
    fun aGapInUpdatesDoesNotRepeatOrSwallow() {
        val planner = LinuxNoticePlanner()
        planner.take(picture(threats = listOf(threat("a"))))
        planner.take(picture(threats = listOf(threat("a")), ready = false))

        val notices = planner.take(
            picture(threats = listOf(threat("a"), threat("b", oblastName = "Kharkiv"))),
        )

        assertEquals(listOf(AppRoute.Threat("b")), notices.map { it.route })
    }

    @Test
    fun aNewProximityHitNamesThePlaceAndOpensFavorites() {
        val planner = LinuxNoticePlanner()
        planner.take(picture())
        val hit = ProximityHit(
            favorite("home", "Home"),
            threat("t", level = ThreatLevel.Yellow),
            distanceKm = 12.4,
            insideRegion = false,
        )

        val notices = planner.take(picture(proximity = listOf(hit), threats = listOf(hit.threat)))

        assertEquals(
            listOf(
                LinuxNotice(
                    summary = "Home",
                    body = "Shahed. 12 km from Home",
                    urgency = NoticeUrgency.Normal,
                    route = AppRoute.Favorites,
                ),
            ),
            notices,
        )
    }

    @Test
    fun anOblastWideHitSaysSo() {
        val planner = LinuxNoticePlanner()
        planner.take(picture())
        val marker = threat("t", locationType = "oblast")
        val hit = ProximityHit(favorite("home", "Home"), marker, 40.0, insideRegion = true)

        val notices = planner.take(picture(proximity = listOf(hit), threats = listOf(marker)))

        assertEquals("Shahed. Oblast-wide in Kyiv", notices.single().body)
    }

    @Test
    fun aHitUnderOneKilometerSaysSo() {
        val planner = LinuxNoticePlanner()
        planner.take(picture())
        val hit = ProximityHit(
            favorite("home", "Home"),
            threat("t"),
            distanceKm = 0.4,
            insideRegion = false,
        )

        val notices = planner.take(picture(proximity = listOf(hit), threats = listOf(hit.threat)))

        assertEquals("Shahed. Under 1 km from Home", notices.single().body)
    }

    @Test
    fun aNewThreatOpensThatThreat() {
        val planner = LinuxNoticePlanner()
        planner.take(picture())

        val notices = planner.take(picture(threats = listOf(threat("t", message = "Incoming"))))

        assertEquals(
            listOf(
                LinuxNotice(
                    summary = "Kyiv",
                    body = "Shahed. Incoming",
                    urgency = NoticeUrgency.Critical,
                    route = AppRoute.Threat("t"),
                ),
            ),
            notices,
        )
    }

    @Test
    fun aThreatAlreadyInANewHitIsNotSentAgain() {
        val planner = LinuxNoticePlanner()
        planner.take(picture())
        val marker = threat("t")
        val hit = ProximityHit(
            favorite("home", "Home"),
            marker,
            distanceKm = 4.0,
            insideRegion = false,
        )

        val notices = planner.take(picture(proximity = listOf(hit), threats = listOf(marker)))

        assertEquals(listOf(AppRoute.Favorites), notices.map { it.route })
    }

    @Test
    fun aThreatThatLeavesAndReturnsNotifiesAgain() {
        val planner = LinuxNoticePlanner()
        val marker = threat("t")
        planner.take(picture(threats = listOf(marker)))
        planner.take(picture())

        val notices = planner.take(picture(threats = listOf(marker)))

        assertEquals(listOf(AppRoute.Threat("t")), notices.map { it.route })
    }

    @Test
    fun moreThanThreeThreatsAddsOneSummary() {
        val planner = LinuxNoticePlanner()
        planner.take(picture())
        val threats = listOf(
            threat("1", level = ThreatLevel.Yellow, oblastName = "One"),
            threat("2", level = ThreatLevel.Yellow, oblastName = "Two"),
            threat("3", level = ThreatLevel.Yellow, oblastName = "Three"),
            threat("4", level = ThreatLevel.Red, oblastName = "Four"),
        )

        val notices = planner.take(picture(threats = threats))

        assertEquals(AppRoute.Threat("1"), notices[0].route)
        assertEquals(AppRoute.Threat("3"), notices[2].route)
        assertEquals(
            LinuxNotice("RadarNebo", "1 more threat", NoticeUrgency.Critical, AppRoute.Map),
            notices[3],
        )
    }

    @Test
    fun moreThanThreeHitsAddsOneSummary() {
        val planner = LinuxNoticePlanner()
        planner.take(picture())
        val hits = listOf(
            hit("1", ThreatLevel.Yellow),
            hit("2", ThreatLevel.Yellow),
            hit("3", ThreatLevel.Yellow),
            hit("4", ThreatLevel.Red),
        )

        val notices = planner.take(picture(proximity = hits, threats = hits.map { it.threat }))

        assertEquals(AppRoute.Favorites, notices[0].route)
        assertEquals(
            LinuxNotice(
                "RadarNebo",
                "1 more place has a nearby threat",
                NoticeUrgency.Critical,
                AppRoute.Favorites,
            ),
            notices[3],
        )
    }

    @Test
    fun aNoticeBodyIsOneShortLine() {
        val planner = LinuxNoticePlanner()
        planner.take(picture())
        val marker = threat("t", message = "line\n\nnext " + "x".repeat(400))

        val body = planner.take(picture(threats = listOf(marker))).single().body

        assertFalse(body.contains('\n'))
        assertTrue(body.endsWith("…"))
        assertTrue(body.length <= 240)
        assertTrue(body.contains("line next"))
    }

    @Test
    fun aCommandCarriesTheIconUrgencyAndShowAction() {
        val notice = LinuxNotice("Kyiv", "Shahed. Incoming", NoticeUrgency.Critical, AppRoute.Threat("t"))

        assertEquals(
            listOf(
                "notify-send",
                "--app-name=RadarNebo",
                "--urgency=critical",
                "--action=default=Show",
                "--icon=/tmp/radarnebo.png",
                "--",
                "Kyiv",
                "Shahed. Incoming",
            ),
            linuxNotifyCommand(notice, "/tmp/radarnebo.png"),
        )
    }

    @Test
    fun aCommandWithoutAnIconOmitsIt() {
        val notice = LinuxNotice("Kyiv", "Shahed", NoticeUrgency.Normal, AppRoute.Map)

        assertEquals(
            listOf(
                "notify-send",
                "--app-name=RadarNebo",
                "--urgency=normal",
                "--action=default=Show",
                "--",
                "Kyiv",
                "Shahed",
            ),
            linuxNotifyCommand(notice, null),
        )
    }

    @Test
    fun aMissingNotifySendStopsFurtherNotices() {
        val commands = mutableListOf<List<String>>()
        val events = AppEvents()
        LinuxNotifications(
            events,
            start = { command ->
                commands += command
                throw IOException("missing")
            },
            iconPath = "/tmp/radarnebo.png",
        )
        events.onState(picture())
        events.onState(picture(threats = listOf(threat("t", title = "Shahed", oblastName = "Kyiv"))))
        events.onState(picture(threats = listOf(threat("t"), threat("u"))))

        assertEquals(
            listOf(
                linuxNotifyCommand(
                    LinuxNotice("Kyiv", "Shahed", NoticeUrgency.Critical, AppRoute.Threat("t")),
                    "/tmp/radarnebo.png",
                ),
            ),
            commands,
        )
    }

    @Test
    fun aDefaultActionOpensItsRoute() {
        val opened = mutableListOf<AppRoute>()
        val notes = LinuxNotifications(
            AppEvents(),
            start = { error("unused") },
            iconPath = null,
            deliver = { opened += it },
        )

        notes.consume(ScriptedProcess("default\n"), AppRoute.Threat("t"))

        assertEquals(AppRoute.Threat("t"), opened.single())
    }

    @Test
    fun closingANoticeWithoutShowDoesNotOpenARoute() {
        val opened = mutableListOf<AppRoute>()
        val notes = LinuxNotifications(
            AppEvents(),
            start = { error("unused") },
            iconPath = null,
            deliver = { opened += it },
        )

        notes.consume(ScriptedProcess(""), AppRoute.Map)

        assertTrue(opened.isEmpty())
    }

    @Test
    fun openingARouteBumpsTheTokenSoARepeatFiresAgain() {
        val events = AppEvents()

        events.open(AppRoute.Favorites)
        val first = events.request
        events.open(AppRoute.Favorites)

        assertEquals(1, first?.token)
        assertEquals(AppRoute.Favorites, first?.route)
        assertEquals(2, events.request?.token)
    }

    @Test
    fun theNotificationIconIsCopiedFromTheAppIcon() {
        val dir = Files.createTempDirectory("radarnebo-icon").toFile()
        try {
            val path = copyNotificationIcon("radarnebo.png", File(dir, "notification-icon.png"))

            assertNotNull(path)
            assertTrue(File(path).length() > 0)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aMissingIconLeavesNoPath() {
        val dir = Files.createTempDirectory("radarnebo-icon").toFile()
        try {
            assertNull(copyNotificationIcon("no-such-icon.png", File(dir, "notification-icon.png")))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aBlockedIconDirectoryLeavesNoPath() {
        val dir = Files.createTempDirectory("radarnebo-icon").toFile()
        val blocker = File(dir, "not-a-directory")
        blocker.writeText("x")
        try {
            assertNull(copyNotificationIcon("radarnebo.png", File(blocker, "icon.png")))
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun picture(
        proximity: List<ProximityHit> = emptyList(),
        threats: List<ThreatMarker> = emptyList(),
        ready: Boolean = true,
    ) = UiState(
        proximity = proximity,
        threats = threats,
        lastUpdated = if (ready) READY else null,
    )

    private fun threat(
        id: String,
        level: ThreatLevel = ThreatLevel.Red,
        title: String = "Shahed",
        oblastName: String = "Kyiv",
        message: String? = null,
        locationType: String = "raion",
    ) = ThreatMarker(
        id = id,
        type = ThreatType.Drones,
        level = level,
        title = title,
        oblastUid = 14,
        oblastName = oblastName,
        locationType = locationType,
        point = GeoPoint(50.4, 30.5),
        startedAt = null,
        message = message,
    )

    private fun favorite(id: String, name: String) = FavoriteLocation(id, name, 50.4, 30.5)

    private fun hit(id: String, level: ThreatLevel) = ProximityHit(
        favorite(id, "Place $id"),
        threat(id, level = level),
        distanceKm = 5.0,
        insideRegion = false,
    )

    private class ScriptedProcess(output: String) : Process() {
        private val stdout = ByteArrayInputStream(output.toByteArray(Charsets.UTF_8))

        override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()

        override fun getInputStream(): InputStream = stdout

        override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))

        override fun waitFor(): Int = 0

        override fun exitValue(): Int = 0

        override fun destroy() = Unit
    }

    private companion object {
        val READY: Instant = Instant.fromEpochMilliseconds(1_700_000_000_000)
    }
}
