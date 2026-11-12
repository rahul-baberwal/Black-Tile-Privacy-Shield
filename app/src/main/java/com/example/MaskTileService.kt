package com.example

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile as QsTile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile provider for toggling the Black Tile privacy overlay.
 * Tapping toggles between running/visible and hidden, or starts the overlay.
 * If overlay permissions are missing, opens MainActivity using the appropriate API level method.
 */
class MaskTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // 1. If overlay permission is missing, open MainActivity
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= 34) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        // 2. If service is not running, start it
        if (!OverlayService.alive) {
            val startIntent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_START
            }
            MaskPrefs.setVisible(this, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
        } else {
            // 3. If service is running, toggle visibility
            val currentVisible = MaskPrefs.isVisible(this)
            val toggleIntent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_VISIBLE
                putExtra(OverlayService.EXTRA_VISIBLE, !currentVisible)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(toggleIntent)
            } else {
                startService(toggleIntent)
            }
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isAlive = OverlayService.alive
        val isVisible = MaskPrefs.isVisible(this)

        if (isAlive && isVisible) {
            tile.state = QsTile.STATE_ACTIVE
            tile.label = getString(R.string.qs_tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.action_show)
            }
        } else if (isAlive && !isVisible) {
            tile.state = QsTile.STATE_INACTIVE
            tile.label = getString(R.string.qs_tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.action_hide)
            }
        } else {
            tile.state = QsTile.STATE_INACTIVE
            tile.label = getString(R.string.qs_tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.status_stopped)
            }
        }
        tile.updateTile()
    }
}
