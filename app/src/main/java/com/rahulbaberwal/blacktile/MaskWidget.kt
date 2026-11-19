package com.rahulbaberwal.blacktile

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

/**
 * Home-screen app widget providing 3 quick action buttons:
 * 1. Start / Hide / Show
 * 2. + Tile
 * 3. Lock / Edit
 */
class MaskWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_mask)

        val isAlive = OverlayService.alive
        val isVisible = MaskPrefs.isVisible(context)
        val isLocked = MaskPrefs.isLocked(context)

        // 1. Toggle Button (Start / Hide / Show)
        val toggleText = when {
            !isAlive -> context.getString(R.string.btn_start)
            isVisible -> context.getString(R.string.action_hide)
            else -> context.getString(R.string.action_show)
        }
        views.setTextViewText(R.id.btnWidgetToggle, toggleText)

        val toggleIntent = Intent(context, OverlayService::class.java).apply {
            if (!isAlive) {
                action = OverlayService.ACTION_START
            } else {
                action = OverlayService.ACTION_VISIBLE
                putExtra(OverlayService.EXTRA_VISIBLE, !isVisible)
            }
        }
        val togglePendingIntent = PendingIntent.getForegroundService(
            context,
            101,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.btnWidgetToggle, togglePendingIntent)

        // 2. Add Tile Button
        val addIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_ADD
        }
        val addPendingIntent = PendingIntent.getForegroundService(
            context,
            102,
            addIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.btnWidgetAdd, addPendingIntent)

        // 3. Lock / Edit Button
        val lockText = if (isLocked) {
            context.getString(R.string.action_edit)
        } else {
            context.getString(R.string.action_lock)
        }
        views.setTextViewText(R.id.btnWidgetLock, lockText)

        val lockIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_LOCK
            putExtra(OverlayService.EXTRA_LOCKED, !isLocked)
        }
        val lockPendingIntent = PendingIntent.getForegroundService(
            context,
            103,
            lockIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.btnWidgetLock, lockPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        fun updateWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, MaskWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, MaskWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
