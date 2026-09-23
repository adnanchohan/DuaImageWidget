package com.watchfulai.mywidgets.tasbeeh

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.watchfulai.mywidgets.R
import com.watchfulai.mywidgets.ui.tasbeeh.TasbeehConfigurationActivity
import java.text.NumberFormat
import java.util.concurrent.Executors

class TasbeehWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        enqueue { ids.forEach { update(context, it) } }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        enqueue { update(context, id) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        enqueue { ids.forEach { TasbeehRepository(context).remove(it) } }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        enqueue {
            val repository = TasbeehRepository(context)
            oldWidgetIds.zip(newWidgetIds).forEach { (old, new) ->
                repository.get(old)?.let { config -> repository.update(new) { config } }
                if (old != new) repository.remove(old)
                update(context, new)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_INCREMENT -> {
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                enqueue {
                    if (isActive(context, id) && TasbeehRepository(context).get(id) != null) {
                        TasbeehRepository(context).update(id) { it!!.increment() }
                        update(context, id)
                    }
                }
            }
            Intent.ACTION_LOCALE_CHANGED, Intent.ACTION_BOOT_COMPLETED -> enqueue {
                AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, TasbeehWidgetProvider::class.java),
                ).forEach { update(context, it) }
            }
        }
    }

    private fun enqueue(work: () -> Unit) {
        val pending = goAsync()
        executor.execute {
            try { work() }
            catch (error: Exception) { android.util.Log.e("TasbeehWidget", "Widget update failed", error) }
            finally { pending.finish() }
        }
    }

    companion object {
        private const val ACTION_INCREMENT = "com.watchfulai.mywidgets.tasbeeh.INCREMENT"
        private val executor = Executors.newSingleThreadExecutor()

        fun isActive(context: Context, id: Int): Boolean =
            AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider ==
                ComponentName(context, TasbeehWidgetProvider::class.java)

        fun update(context: Context, id: Int) {
            if (!isActive(context, id)) return
            val config = TasbeehRepository(context).get(id)
            val views = RemoteViews(context.packageName, R.layout.tasbeeh_widget)
            val edit = PendingIntent.getActivity(context, id,
                Intent(context, TasbeehConfigurationActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.tasbeeh_details, edit)
            if (config == null) {
                views.setTextViewText(R.id.tasbeeh_dhikr, context.getString(R.string.tasbeeh_setup))
                views.setViewVisibility(R.id.tasbeeh_meaning, View.GONE)
                views.setOnClickPendingIntent(R.id.tasbeeh_counter, edit)
            } else {
                views.setTextViewText(R.id.tasbeeh_dhikr, config.dhikr)
                views.setTextViewText(R.id.tasbeeh_meaning, config.meaning)
                views.setViewVisibility(R.id.tasbeeh_meaning, if (config.showMeaning && config.meaning.isNotBlank()) View.VISIBLE else View.GONE)
                val format = NumberFormat.getIntegerInstance()
                val count = "${format.format(config.count)} / ${format.format(config.target)}"
                views.setTextViewText(R.id.tasbeeh_count, count)
                views.setTextViewTextSize(R.id.tasbeeh_count, android.util.TypedValue.COMPLEX_UNIT_SP,
                    if (count.length > 9) 11f else 17f)
                views.setTextViewText(R.id.tasbeeh_tap, context.getString(
                    if (config.count >= config.target) {
                        if (config.restartAtTarget) R.string.tasbeeh_next_round else R.string.tasbeeh_complete
                    } else R.string.showcase_tasbeeh_tap))
                views.setContentDescription(R.id.tasbeeh_counter, context.getString(R.string.tasbeeh_counter_description, count))
                val increment = PendingIntent.getBroadcast(context, id,
                    Intent(context, TasbeehWidgetProvider::class.java).setAction(ACTION_INCREMENT)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.tasbeeh_counter, increment)
            }
            AppWidgetManager.getInstance(context).updateAppWidget(id, views)
        }
    }
}
