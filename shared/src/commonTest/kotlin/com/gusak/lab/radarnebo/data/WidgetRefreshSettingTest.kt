package com.gusak.lab.radarnebo.data

import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetRefreshSettingTest {
    private val storage = InMemoryStorage()

    @Test
    fun theWidgetRefreshIntervalSurvivesARestart() {
        SettingsRepository(storage).update { it.copy(widgetRefreshMinutes = 15) }

        assertEquals(15, SettingsRepository(storage).settings.value.widgetRefreshMinutes)
    }

    @Test
    fun aStoredIntervalOutsideTheRangeIsClamped() {
        storage.putString("widget_refresh_minutes", "0")
        val minutes = SettingsRepository(storage).settings.value.widgetRefreshMinutes

        assertEquals(AppSettings.WIDGET_REFRESH_RANGE.first, minutes)
    }
}
