package com.gusak.lab.radarnebo.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.gusak.lab.radarnebo.data.WidgetOfferSchedule

/** The launcher's pin dialog for the home screen widget, offered on launch as [WidgetOfferSchedule] says. */
internal class WidgetPromo(private val schedule: WidgetOfferSchedule) {
    /** Offers the widget when the schedule says so, unless one is already there or the launcher can't pin one. */
    fun offerIfDue(context: Context, nowMs: Long) {
        val due = schedule.isDue(nowMs) &&
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported &&
            TargetsWidget.widgetIds(context).isEmpty()
        if (due && TargetsWidget.requestPin(context)) schedule.recordOffer(nowMs)
    }
}
