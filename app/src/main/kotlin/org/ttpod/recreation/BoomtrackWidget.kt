package org.ttpod.recreation

import androidx.media3.common.util.UnstableApi
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import org.ttpod.recreation.playback.PlaybackService

@UnstableApi
class BoomtrackWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { update(context, manager, it) }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, BoomtrackWidget::class.java)).forEach { update(context, manager, it) }
        } else if (intent.action == ACTION_TOGGLE || intent.action == ACTION_NEXT) {
            val pending = goAsync()
            val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
            future.addListener({
                runCatching { future.get() }.onSuccess { controller ->
                    if (intent.action == ACTION_NEXT) controller.seekToNextMediaItem()
                    else if (controller.isPlaying) controller.pause() else { controller.prepare(); controller.play() }
                    controller.release()
                }
                pending.finish()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    companion object {
        const val ACTION_REFRESH = "org.ttpod.recreation.WIDGET_REFRESH"
        private const val ACTION_TOGGLE = "org.ttpod.recreation.WIDGET_TOGGLE"
        private const val ACTION_NEXT = "org.ttpod.recreation.WIDGET_NEXT"
        private fun broadcast(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(context, code, Intent(context, BoomtrackWidget::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences("classic-ui", Context.MODE_PRIVATE)
            val views = RemoteViews(context.packageName, R.layout.widget_boomtrack)
            views.setTextViewText(R.id.widgetTitle, prefs.getString("widgetTitle", "No song selected"))
            views.setTextViewText(R.id.widgetArtist, prefs.getString("widgetArtist", "boomtrack"))
            views.setTextViewText(R.id.widgetToggle, if (prefs.getBoolean("widgetPlaying", false)) "Ⅱ" else "▶")
            views.setOnClickPendingIntent(R.id.widgetToggle, broadcast(context, ACTION_TOGGLE, 1))
            views.setOnClickPendingIntent(R.id.widgetNext, broadcast(context, ACTION_NEXT, 2))
            val open = PendingIntent.getActivity(context, 3, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widgetInfo, open)
            manager.updateAppWidget(id, views)
        }
    }
}
