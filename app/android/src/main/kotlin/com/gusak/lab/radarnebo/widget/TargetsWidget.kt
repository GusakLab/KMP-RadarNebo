package com.gusak.lab.radarnebo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.SizeF
import android.widget.RemoteViews
import com.gusak.lab.radarnebo.MainActivity
import com.gusak.lab.radarnebo.R
import com.gusak.lab.radarnebo.data.SettingsRepository
import com.gusak.lab.radarnebo.domain.usecase.LoadTargetsSummaryUseCase
import com.gusak.lab.radarnebo.domain.usecase.TargetsSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The home screen widget: the targets in the air on a map of Ukraine, and how many there were today, yesterday and
 * this month. It fetches from the server itself, so it stays fresh while the app is closed.
 */
class TargetsWidget : AppWidgetProvider(), KoinComponent {
    private val loadSummary: LoadTargetsSummaryUseCase by inject()
    private val settings: SettingsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refresh(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context)
    }

    override fun onDisabled(context: Context) {
        WidgetSchedule.stop(context)
    }

    /** Fetches and shows fresh data, and starts the next interval. A failed fetch leaves the widget as it was. */
    private fun refresh(context: Context) {
        if (widgetIds(context).isEmpty()) {
            WidgetSchedule.stop(context)
            return
        }
        // Every refresh restarts the alarm, which a reboot or an app update clears.
        WidgetSchedule.start(context, settings.settings.value.widgetRefreshMinutes.minutes)
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                // A receiver must finish within ten seconds.
                withTimeoutOrNull(FETCH_TIMEOUT) { loadSummary() }?.let { show(context, it) }
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }

    /** Shows [summary] in both layouts; the launcher picks one for each widget's size, and again on a resize. */
    private fun show(context: Context, summary: TargetsSummary) {
        val map = WidgetMap.draw(summary.targets, summary.alertedOblasts)
        val sized = mapOf(
            COMPACT_SIZE to filled(context, R.layout.widget_targets_compact, summary, map),
            FULL_SIZE to filled(context, R.layout.widget_targets, summary, map),
        )
        AppWidgetManager.getInstance(context).updateAppWidget(widgetIds(context), RemoteViews(sized))
    }

    /** [layout] with [summary]'s data; both layouts use the same view ids. */
    private fun filled(context: Context, layout: Int, summary: TargetsSummary, map: Bitmap): RemoteViews {
        val views = RemoteViews(context.packageName, layout)
        views.setImageViewBitmap(R.id.widget_map, map)
        views.setTextViewText(R.id.widget_active, summary.targets.size.toString())
        val noValue = context.getString(R.string.widget_no_value)
        views.setTextViewText(R.id.widget_today, summary.stats?.today?.toString() ?: noValue)
        views.setTextViewText(R.id.widget_yesterday, summary.stats?.yesterday?.toString() ?: noValue)
        views.setTextViewText(R.id.widget_month, summary.stats?.thisMonth?.toString() ?: noValue)
        // The chronometer counts up from now by itself, so the age stays right between refreshes.
        views.setChronometer(
            R.id.widget_updated,
            SystemClock.elapsedRealtime(),
            context.getString(R.string.widget_updated),
            true,
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)
        return views
    }

    companion object {
        private const val ACTION_REFRESH = "com.gusak.lab.radarnebo.widget.REFRESH"
        private val FETCH_TIMEOUT = 9.seconds

        // In dp. The full layout needs room for its header and row of stats under the map; smaller, the stats move
        // over the map's top left corner.
        private val COMPACT_SIZE = SizeF(110f, 80f)
        private val FULL_SIZE = SizeF(250f, 180f)

        internal fun refreshIntent(context: Context): Intent =
            Intent(context, TargetsWidget::class.java).setAction(ACTION_REFRESH)

        /** Refreshes every widget now; does nothing when none is on the home screen. */
        fun refreshNow(context: Context) {
            context.sendBroadcast(refreshIntent(context))
        }

        /**
         * Asks the launcher to pin the widget; false when it can't, and the user has to add it from the widget
         * picker.
         */
        fun requestPin(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            return manager.requestPinAppWidget(ComponentName(context, TargetsWidget::class.java), null, null)
        }

        internal fun widgetIds(context: Context): IntArray =
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TargetsWidget::class.java))
    }
}
