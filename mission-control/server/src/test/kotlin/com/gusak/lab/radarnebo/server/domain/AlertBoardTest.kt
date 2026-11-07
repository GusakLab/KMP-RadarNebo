package com.gusak.lab.radarnebo.server.domain

import com.gusak.lab.radarnebo.data.ThreatLevel
import com.gusak.lab.radarnebo.server.Fixtures
import com.gusak.lab.radarnebo.server.Fixtures.alert
import com.gusak.lab.radarnebo.server.domain.alert.AlertBoard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.minutes

class AlertBoardTest {
    private val t0 = Fixtures.T0

    @Test
    fun versionChangesOnlyWithTheContentAndGoneAlertsFinish() {
        val brovary = alert(1, Fixtures.BROVARY)
        val kharkiv = alert(2, Fixtures.KHARKIV)
        val first = AlertBoard.empty(t0).next(listOf(kharkiv, brovary), t0)
        assertEquals(1, first.board.version)
        assertEquals(listOf(kharkiv, brovary), first.board.alerts, "in the source's order")
        assertEquals(listOf(kharkiv, brovary), first.opened)

        val same = first.board.next(listOf(kharkiv, brovary), t0 + 1.minutes)
        assertFalse(same.changed)
        assertEquals(1, same.board.version)
        assertEquals(t0 + 1.minutes, same.board.confirmedAt)
        assertEquals(t0, same.board.changedAt)

        val updated = kharkiv.copy(level = ThreatLevel.Yellow)
        val next = same.board.next(listOf(updated), t0 + 2.minutes)
        assertEquals(2, next.board.version)
        assertEquals(listOf(updated), next.updated)
        assertEquals(listOf(brovary.finish(t0 + 2.minutes)), next.finished)
        assertEquals(t0 + 2.minutes, next.finished.single().finishedAt)
    }
}
