package com.gusak.lab.radarnebo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gusak.lab.radarnebo.data.SettingsRepository
import com.gusak.lab.radarnebo.debug.TrafficLog
import com.gusak.lab.radarnebo.ui.RadarTheme
import com.gusak.lab.radarnebo.ui.debug.RequestInspectorScreen
import org.koin.android.ext.android.inject

/** The request inspector: every request the app sent to its server, with the response. */
class RequestInspectorActivity : ComponentActivity() {
    private val traffic: TrafficLog by inject()
    private val settings: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val exchanges by traffic.exchanges.collectAsStateWithLifecycle()
            val appSettings by settings.settings.collectAsStateWithLifecycle()
            RadarTheme(appSettings.themeMode, appSettings.colorPalette) {
                RequestInspectorScreen(
                    exchanges,
                    onClear = traffic::clear,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                )
            }
        }
    }
}
