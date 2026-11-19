package com.rahulbaberwal.blacktile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.service.quicksettings.TileService
import android.view.Gravity
import android.view.WindowManager

/**
 * Foreground service that hosts and controls multiple floating privacy mask tiles.
 * Supports starting, adding tiles, toggling lock/edit modes, visibility,
 * refreshing opacity (both edit and locked opacities), clearing all tiles, and stopping.
 */
class OverlayService : Service(), MaskView.TileInteractionListener {

    companion object {
        const val ACTION_START = "com.rahulbaberwal.blacktile.action.START"
        const val ACTION_ADD = "com.rahulbaberwal.blacktile.action.ADD"
        const val ACTION_LOCK = "com.rahulbaberwal.blacktile.action.LOCK"
        const val ACTION_VISIBLE = "com.rahulbaberwal.blacktile.action.VISIBLE"
        const val ACTION_REFRESH = "com.rahulbaberwal.blacktile.action.REFRESH"
        const val ACTION_CLEAR_ALL = "com.rahulbaberwal.blacktile.action.CLEAR_ALL"
        const val ACTION_STOP = "com.rahulbaberwal.blacktile.action.STOP"
        const val ACTION_STATE_CHANGED = "com.rahulbaberwal.blacktile.action.STATE_CHANGED"

        const val EXTRA_LOCKED = "extra_locked"
        const val EXTRA_VISIBLE = "extra_visible"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "black_tile_overlay_channel"

        @Volatile
        var alive: Boolean = false
            private set

        fun interface StateListener {
            fun onStateChanged()
        }

        private val listeners = java.util.concurrent.CopyOnWriteArraySet<StateListener>()

        fun registerListener(listener: StateListener) {
            listeners.add(listener)
        }

        fun unregisterListener(listener: StateListener) {
            listeners.remove(listener)
        }
    }

    private lateinit var windowManager: WindowManager
    private val tiles = mutableListOf<Tile>()
    private val activeViews = mutableMapOf<String, Pair<MaskView, WindowManager.LayoutParams>>()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Stop itself if overlay permission is missing
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Must call startForeground immediately
        startForegroundNotification()
        alive = true

        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> handleStart()
            ACTION_ADD -> handleAddTile()
            ACTION_LOCK -> {
                val hasExtra = intent?.hasExtra(EXTRA_LOCKED) == true
                val targetLocked = if (hasExtra) intent?.getBooleanExtra(EXTRA_LOCKED, false) ?: false else !MaskPrefs.isLocked(this)
                handleLock(targetLocked)
            }
            ACTION_VISIBLE -> {
                val hasExtra = intent?.hasExtra(EXTRA_VISIBLE) == true
                val targetVisible = if (hasExtra) intent?.getBooleanExtra(EXTRA_VISIBLE, true) ?: true else !MaskPrefs.isVisible(this)
                handleVisible(targetVisible)
            }
            ACTION_REFRESH -> handleRefresh()
            ACTION_CLEAR_ALL -> handleClearAll()
            ACTION_STOP -> {
                handleStop()
                return START_NOT_STICKY
            }
        }

        updateNotification()
        notifyStateChanged()
        return START_STICKY
    }

    private fun handleStart() {
        tiles.clear()
        tiles.addAll(MaskPrefs.loadTiles(this))
        if (tiles.isEmpty()) {
            val dm = resources.displayMetrics
            val defaultTile = MaskPrefs.createDefaultTile(this, dm.widthPixels, dm.heightPixels)
            tiles.add(defaultTile)
            MaskPrefs.saveTiles(this, tiles)
        }
        renderAllTiles()
    }

    private fun handleAddTile() {
        if (tiles.isEmpty()) {
            tiles.addAll(MaskPrefs.loadTiles(this))
        }
        // If service was started directly with ACTION_ADD, render existing tiles first
        if (activeViews.isEmpty() && MaskPrefs.isVisible(this)) {
            for (tile in tiles) {
                attachTileWindow(tile)
            }
        }

        val dm = resources.displayMetrics
        val newTile = MaskPrefs.createDefaultTile(this, dm.widthPixels, dm.heightPixels)

        // Offset slightly if other tiles exist so they are immediately visible
        if (tiles.isNotEmpty()) {
            val density = dm.density
            val offset = ((tiles.size % 5) * 24 * density).toInt()
            newTile.x = (newTile.x + offset).coerceAtMost((dm.widthPixels - newTile.w).coerceAtLeast(0))
            newTile.y = (newTile.y + offset).coerceAtMost((dm.heightPixels - newTile.h).coerceAtLeast(0))
        }

        tiles.add(newTile)
        MaskPrefs.saveTiles(this, tiles)

        if (MaskPrefs.isVisible(this)) {
            attachTileWindow(newTile)
        }
    }

    private fun handleLock(locked: Boolean) {
        MaskPrefs.setLocked(this, locked)
        val visible = MaskPrefs.isVisible(this)
        val editOpacity = MaskPrefs.getOpacity(this)
        val lockedOpacity = MaskPrefs.getLockedOpacity(this)

        for ((_, pair) in activeViews) {
            val (maskView, params) = pair
            maskView.isLocked = locked
            maskView.opacity = editOpacity
            maskView.lockedOpacity = lockedOpacity
            maskView.invalidate()

            params.flags = computeWindowFlags(locked, visible)
            try {
                windowManager.updateViewLayout(maskView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleVisible(visible: Boolean) {
        MaskPrefs.setVisible(this, visible)
        if (visible) {
            renderAllTiles()
        } else {
            removeAllTileViews()
        }
    }

    private fun handleRefresh() {
        val editOpacity = MaskPrefs.getOpacity(this)
        val lockedOpacity = MaskPrefs.getLockedOpacity(this)
        val locked = MaskPrefs.isLocked(this)
        val visible = MaskPrefs.isVisible(this)

        // Sync tiles list with disk
        tiles.clear()
        tiles.addAll(MaskPrefs.loadTiles(this))

        for ((_, pair) in activeViews) {
            val (maskView, params) = pair
            maskView.opacity = editOpacity
            maskView.lockedOpacity = lockedOpacity
            maskView.isLocked = locked
            maskView.invalidate()

            params.flags = computeWindowFlags(locked, visible)
            try {
                windowManager.updateViewLayout(maskView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleClearAll() {
        removeAllTileViews()
        tiles.clear()
        MaskPrefs.clearAllTiles(this)
    }

    private fun handleStop() {
        removeAllTileViews()
        alive = false
        notifyStateChanged()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun renderAllTiles() {
        removeAllTileViews()
        if (!MaskPrefs.isVisible(this)) return

        if (tiles.isEmpty()) {
            tiles.addAll(MaskPrefs.loadTiles(this))
        }
        for (tile in tiles) {
            attachTileWindow(tile)
        }
    }

    private fun computeWindowFlags(locked: Boolean, visible: Boolean): Int {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        if (locked || !visible) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        return flags
    }

    private fun attachTileWindow(tile: Tile) {
        if (activeViews.containsKey(tile.id)) return

        val isLocked = MaskPrefs.isLocked(this)
        val isVisible = MaskPrefs.isVisible(this)
        val editOpacity = MaskPrefs.getOpacity(this)
        val lockedOpacity = MaskPrefs.getLockedOpacity(this)

        val maskView = MaskView(
            context = this,
            tile = tile,
            opacity = editOpacity,
            lockedOpacity = lockedOpacity,
            isLocked = isLocked,
            listener = this
        )

        val flags = computeWindowFlags(isLocked, isVisible)
        val params = WindowManager.LayoutParams(
            tile.w,
            tile.h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = tile.x
            y = tile.y
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager.addView(maskView, params)
            activeViews[tile.id] = Pair(maskView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeAllTileViews() {
        for ((_, pair) in activeViews) {
            try {
                windowManager.removeView(pair.first)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        activeViews.clear()
    }

    // --- TileInteractionListener callbacks ---

    override fun onMoved(tile: Tile, newX: Int, newY: Int) {
        activeViews[tile.id]?.let { (view, params) ->
            params.x = newX
            params.y = newY
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResized(tile: Tile, newW: Int, newH: Int) {
        activeViews[tile.id]?.let { (view, params) ->
            params.width = newW
            params.height = newH
            try {
                windowManager.updateViewLayout(view, params)
                view.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDelete(tile: Tile) {
        activeViews[tile.id]?.let { (view, _) ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        activeViews.remove(tile.id)
        tiles.removeAll { it.id == tile.id }
        MaskPrefs.saveTiles(this, tiles)
        updateNotification()
        notifyStateChanged()
    }

    override fun onEditFinished(tile: Tile) {
        MaskPrefs.saveTiles(this, tiles)
    }

    // --- Notification & Lifecycle ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val locked = MaskPrefs.isLocked(this)
        val visible = MaskPrefs.isVisible(this)
        val count = tiles.size
        val countText = "$count ${if (count == 1) getString(R.string.tile_singular) else getString(R.string.tile_plural)}"
        val contentText = "${if (locked) getString(R.string.state_locked) else getString(R.string.state_editable)} • ${if (visible) getString(R.string.state_visible) else getString(R.string.state_hidden)} ($countText)"

        // Activity Intent
        val activityIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action 1: Lock/Edit
        val lockIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_LOCK
            putExtra(EXTRA_LOCKED, !locked)
        }
        val lockPendingIntent = PendingIntent.getForegroundService(
            this, 1, lockIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val lockActionTitle = if (locked) getString(R.string.action_edit) else getString(R.string.action_lock)

        // Action 2: Hide/Show
        val visibleIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_VISIBLE
            putExtra(EXTRA_VISIBLE, !visible)
        }
        val visiblePendingIntent = PendingIntent.getForegroundService(
            this, 2, visibleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val visibleActionTitle = if (visible) getString(R.string.action_hide) else getString(R.string.action_show)

        // Action 3: Stop
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getForegroundService(
            this, 3, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, lockActionTitle, lockPendingIntent).build())
            .addAction(Notification.Action.Builder(null, visibleActionTitle, visiblePendingIntent).build())
            .addAction(Notification.Action.Builder(null, getString(R.string.action_stop), stopPendingIntent).build())
            .build()
    }

    private fun startForegroundNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        if (!alive) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun notifyStateChanged() {
        try {
            TileService.requestListeningState(this, ComponentName(this, MaskTileService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        MaskWidget.updateWidgets(this)

        for (listener in listeners) {
            try {
                listener.onStateChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            val intent = Intent(ACTION_STATE_CHANGED).apply {
                setPackage(packageName)
                putExtra("alive", alive)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeAllTileViews()
        alive = false
        notifyStateChanged()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
