package com.gusak.lab.radarnebo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gusak.lab.radarnebo.data.KeyValueStorage
import com.gusak.lab.radarnebo.data.SettingsRepository
import com.gusak.lab.radarnebo.data.WidgetOfferSchedule
import com.gusak.lab.radarnebo.widget.TargetsWidget
import com.gusak.lab.radarnebo.widget.WidgetPromo
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settings: SettingsRepository by inject()
    private val storage: KeyValueStorage by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WidgetPromo(WidgetOfferSchedule(storage)).offerIfDue(this, System.currentTimeMillis())
        setContent {
            App(
                // Keep status/navigation bar icons legible when the in-app theme differs from the system one.
                onThemeApplied = { _, dark ->
                    val style = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                },
                onOpenRequests = if (BuildConfig.DEBUG) {
                    { startActivity(Intent(this, RequestInspectorActivity::class.java)) }
                } else {
                    null
                },
                onAddWidget = ::addWidget,
            )
            val appSettings by settings.settings.collectAsStateWithLifecycle()
            // Launching the app or changing the interval refreshes the widget at once and restarts its alarm.
            LaunchedEffect(appSettings.widgetRefreshMinutes) { TargetsWidget.refreshNow(this@MainActivity) }
        }
    }

    private fun addWidget() {
        if (!TargetsWidget.requestPin(this)) {
            Toast.makeText(this, R.string.widget_pin_unsupported, Toast.LENGTH_LONG).show()
        }
    }
}
