package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/**
 * Main dashboard activity for the Black Tile privacy display application.
 * Controls service start/stop, adding tiles, toggling lock/visibility,
 * adjusting opacity, and granting system overlay and notification permissions.
 */
class MainActivity : Activity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnGrantPermission: Button
    private lateinit var btnStartStop: Button
    private lateinit var btnAddTile: Button
    private lateinit var switchLock: Switch
    private lateinit var switchVisible: Switch
    private lateinit var tvOpacityValue: TextView
    private lateinit var sbOpacity: SeekBar

    private var isBinding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        checkNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnAddTile = findViewById(R.id.btnAddTile)
        switchLock = findViewById(R.id.switchLock)
        switchVisible = findViewById(R.id.switchVisible)
        tvOpacityValue = findViewById(R.id.tvOpacityValue)
        sbOpacity = findViewById(R.id.sbOpacity)
    }

    private fun setupListeners() {
        btnGrantPermission.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        btnStartStop.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                btnGrantPermission.performClick()
                return@setOnClickListener
            }

            if (OverlayService.alive) {
                sendServiceAction(OverlayService.ACTION_STOP)
            } else {
                MaskPrefs.setVisible(this, true)
                sendServiceAction(OverlayService.ACTION_START)
            }
            refreshUi()
        }

        btnAddTile.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                btnGrantPermission.performClick()
                return@setOnClickListener
            }

            if (!OverlayService.alive) {
                MaskPrefs.setVisible(this, true)
                sendServiceAction(OverlayService.ACTION_START)
            } else {
                sendServiceAction(OverlayService.ACTION_ADD)
            }
            refreshUi()
        }

        switchLock.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            MaskPrefs.setLocked(this, isChecked)
            if (OverlayService.alive) {
                val intent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_LOCK
                    putExtra(OverlayService.EXTRA_LOCKED, isChecked)
                }
                startServiceIntent(intent)
            }
        }

        switchVisible.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            MaskPrefs.setVisible(this, isChecked)
            if (OverlayService.alive) {
                val intent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_VISIBLE
                    putExtra(OverlayService.EXTRA_VISIBLE, isChecked)
                }
                startServiceIntent(intent)
            }
            refreshUi()
        }

        sbOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacity = progress + 20
                tvOpacityValue.text = "$opacity%"
                if (fromUser && !isBinding) {
                    MaskPrefs.setOpacity(this@MainActivity, opacity)
                    if (OverlayService.alive) {
                        sendServiceAction(OverlayService.ACTION_REFRESH)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun refreshUi() {
        isBinding = true
        try {
            val hasOverlayPermission = Settings.canDrawOverlays(this)
            val isAlive = OverlayService.alive

            // 1. Permission button visibility
            btnGrantPermission.visibility = if (hasOverlayPermission) View.GONE else View.VISIBLE

            // 2. Status text
            val tiles = MaskPrefs.loadTiles(this)
            val count = tiles.size
            val countLabel = if (count == 1) getString(R.string.tile_singular) else getString(R.string.tile_plural)

            if (!hasOverlayPermission) {
                tvStatus.text = getString(R.string.status_permission_needed)
                tvStatus.setTextColor(getColor(R.color.accent_red))
            } else if (isAlive) {
                tvStatus.text = getString(R.string.status_active, count, countLabel)
                tvStatus.setTextColor(getColor(R.color.accent_green))
            } else {
                tvStatus.text = getString(R.string.status_stopped)
                tvStatus.setTextColor(getColor(R.color.text_secondary))
            }

            // 3. Start/Stop button label
            btnStartStop.text = if (isAlive) getString(R.string.btn_stop) else getString(R.string.btn_start)

            // 4. Switches
            switchLock.isChecked = MaskPrefs.isLocked(this)
            switchVisible.isChecked = MaskPrefs.isVisible(this)

            // 5. Opacity slider
            val opacity = MaskPrefs.getOpacity(this)
            sbOpacity.progress = (opacity - 20).coerceIn(0, 80)
            tvOpacityValue.text = "$opacity%"
        } finally {
            isBinding = false
        }
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, OverlayService::class.java).apply {
            this.action = action
        }
        startServiceIntent(intent)
    }

    private fun startServiceIntent(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 201)
            }
        }
    }
}
