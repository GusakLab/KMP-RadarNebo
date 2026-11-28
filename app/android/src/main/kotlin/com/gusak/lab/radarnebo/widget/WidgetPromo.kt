package com.gusak.lab.radarnebo.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.gusak.lab.radarnebo.data.KeyValueStorage

/** The launch offer of the home screen widget: shown until the user adds the widget or ignores the offer, once. */
internal class WidgetPromo(private val storage: KeyValueStorage) {
    /** Due unless answered, a widget is already there, or the launcher can't pin one. */
    fun isDue(context: Context): Boolean =
        storage.getString(KEY_ANSWERED) == null &&
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported &&
            TargetsWidget.widgetIds(context).isEmpty()

    fun answer() {
        storage.putString(KEY_ANSWERED, true.toString())
    }

    companion object {
        const val KEY_ANSWERED: String = "widget_promo_answered"
    }
}
