package com.gusak.lab.radarnebo.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlin.time.Duration

/**
 * The alarm that refreshes the widget. It doesn't wake the phone: an alarm due while the screen is off goes off when
 * it next turns on, which is when the widget can be seen again.
 */
internal object WidgetSchedule {
    fun start(context: Context, interval: Duration, nowElapsedMs: Long = SystemClock.elapsedRealtime()) {
        val intervalMs = interval.inWholeMilliseconds
        context.alarms().setRepeating(
            AlarmManager.ELAPSED_REALTIME,
            nowElapsedMs + intervalMs,
            intervalMs,
            refreshIntent(context),
        )
    }

    fun stop(context: Context) {
        context.alarms().cancel(refreshIntent(context))
    }

    private fun Context.alarms(): AlarmManager = getSystemService(AlarmManager::class.java)

    private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        TargetsWidget.refreshIntent(context),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
