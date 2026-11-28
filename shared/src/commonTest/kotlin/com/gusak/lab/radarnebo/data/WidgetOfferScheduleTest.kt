package com.gusak.lab.radarnebo.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class WidgetOfferScheduleTest {
    private val schedule = WidgetOfferSchedule(InMemoryStorage())
    private var now = 1_700_000_000_000L

    private fun wait(duration: Duration) {
        now += duration.inWholeMilliseconds
    }

    @Test
    fun theFirstLaunchOffersTheWidget() {
        assertTrue(schedule.isDue(now))
    }

    @Test
    fun offersComeAfterAWeekAMonthThreeMonthsAndAYearThenStop() {
        schedule.recordOffer(now)
        for (gap in listOf(7.days, 30.days, 90.days, 365.days)) {
            wait(gap - 1.milliseconds)
            assertFalse(schedule.isDue(now), "not yet due before $gap")
            wait(1.milliseconds)
            assertTrue(schedule.isDue(now), "due after $gap")
            schedule.recordOffer(now)
        }
        wait(3650.days)
        assertFalse(schedule.isDue(now), "no offer after the yearly one")
    }

    @Test
    fun theScheduleSurvivesARestart() {
        val storage = InMemoryStorage()
        WidgetOfferSchedule(storage).recordOffer(now)

        assertFalse(WidgetOfferSchedule(storage).isDue(now + 1.days.inWholeMilliseconds))
    }

    @Test
    fun aStoppedScheduleNeverOffersAndAResetOneOffersAtOnce() {
        schedule.stop()
        assertFalse(schedule.isDue(now))
        schedule.reset()
        assertTrue(schedule.isDue(now))
    }
}
