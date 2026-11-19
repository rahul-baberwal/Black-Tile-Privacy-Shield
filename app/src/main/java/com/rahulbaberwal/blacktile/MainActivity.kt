package com.rahulbaberwal.blacktile

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/**
 * Main dashboard activity for the Black Tile privacy display application.
 * Controls service start/stop, adding tiles, quick lock/show toggles,
 * granting overlay/notification permissions, and navigating to Settings and About screens.
 * Automatically synchronizes with OverlayService, Quick Settings, and Widgets in real time.
 */
class MainActivity : Activity(), OverlayService.Companion.StateListener {

    private lateinit var btnTopSettings: ImageButton
    private lateinit var btnTopAbout: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var btnGrantPermission: Button
    private lateinit var btnStartStop: Button
    private lateinit var btnAddTile: Button
    private lateinit var switchLock: Switch
    private lateinit var switchVisible: Switch
    private lateinit var rowNavSettings: LinearLayout
    private lateinit var rowNavAbout: LinearLayout

    private var isBinding = false

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnUiThread { refreshUi() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        checkNotificationPermission()
    }

    override fun onStart() {
        super.onStart()
        OverlayService.registerListener(this)
        val filter = IntentFilter(OverlayService.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onStop() {
        super.onStop()
        OverlayService.unregisterListener(this)
        try {
            unregisterReceiver(stateReceiver)
        } catch (e: Exception) {
            // Ignored if already unregistered
        }
    }

    override fun onStateChanged() {
        runOnUiThread { refreshUi() }
    }

    private fun initViews() {
        btnTopSettings = findViewById(R.id.btnTopSettings)
        btnTopAbout = findViewById(R.id.btnTopAbout)
        tvStatus = findViewById(R.id.tvStatus)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnAddTile = findViewById(R.id.btnAddTile)
        switchLock = findViewById(R.id.switchLock)
        switchVisible = findViewById(R.id.switchVisible)
        rowNavSettings = findViewById(R.id.rowNavSettings)
        rowNavAbout = findViewById(R.id.rowNavAbout)
    }

    private fun setupListeners() {
        btnTopSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnTopAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        rowNavSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        rowNavAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

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
                refreshUi(optimisticAlive = false)
            } else {
                MaskPrefs.setVisible(this, true)
                sendServiceAction(OverlayService.ACTION_START)
                refreshUi(optimisticAlive = true)
            }
        }

        btnAddTile.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                btnGrantPermission.performClick()
                return@setOnClickListener
            }

            MaskPrefs.setVisible(this, true)
            sendServiceAction(OverlayService.ACTION_ADD)
            // Immediately reflect active status with optimistic update so button and label never lag
            refreshUi(optimisticAlive = true, optimisticAdded = true)
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
    }

    private fun refreshUi(optimisticAlive: Boolean? = null, optimisticAdded: Boolean = false) {
        isBinding = true
        try {
            val hasOverlayPermission = Settings.canDrawOverlays(this)
            val isAlive = optimisticAlive ?: OverlayService.alive

            // 1. Permission button visibility
            btnGrantPermission.visibility = if (hasOverlayPermission) View.GONE else View.VISIBLE

            // 2. Status text
            val tiles = MaskPrefs.loadTiles(this)
            var count = tiles.size
            if (optimisticAdded && count == 0) {
                count = 1
            }
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
            switchVisible.isChecked = if (optimisticAlive == true) true else MaskPrefs.isVisible(this)
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
