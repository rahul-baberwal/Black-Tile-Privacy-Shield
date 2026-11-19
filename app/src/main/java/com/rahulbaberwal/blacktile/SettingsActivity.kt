package com.rahulbaberwal.blacktile

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

/**
 * Settings activity for configuring tile opacity (both editing and locked modes),
 * clearing all configured tiles, resetting to defaults, and accessing the privacy policy.
 * All changes persist to SharedPreferences (MaskPrefs) and propagate immediately to the
 * running OverlayService if active.
 */
class SettingsActivity : Activity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvEditOpacityValue: TextView
    private lateinit var sbEditOpacity: SeekBar
    private lateinit var tvLockedOpacityValue: TextView
    private lateinit var sbLockedOpacity: SeekBar
    private lateinit var btnRemoveAllTiles: Button
    private lateinit var btnResetDefaults: Button
    private lateinit var rowPrivacyPolicy: LinearLayout

    private var isBinding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvEditOpacityValue = findViewById(R.id.tvEditOpacityValue)
        sbEditOpacity = findViewById(R.id.sbEditOpacity)
        tvLockedOpacityValue = findViewById(R.id.tvLockedOpacityValue)
        sbLockedOpacity = findViewById(R.id.sbLockedOpacity)
        btnRemoveAllTiles = findViewById(R.id.btnRemoveAllTiles)
        btnResetDefaults = findViewById(R.id.btnResetDefaults)
        rowPrivacyPolicy = findViewById(R.id.rowPrivacyPolicy)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        sbEditOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacity = progress + MaskPrefs.MIN_OPACITY
                tvEditOpacityValue.text = "$opacity%"
                if (fromUser && !isBinding) {
                    MaskPrefs.setOpacity(this@SettingsActivity, opacity)
                    sbLockedOpacity.progress = progress
                    tvLockedOpacityValue.text = "$opacity%"
                    notifyServiceRefresh()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbLockedOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacity = progress + MaskPrefs.MIN_OPACITY
                tvLockedOpacityValue.text = "$opacity%"
                if (fromUser && !isBinding) {
                    MaskPrefs.setOpacity(this@SettingsActivity, opacity)
                    sbEditOpacity.progress = progress
                    tvEditOpacityValue.text = "$opacity%"
                    notifyServiceRefresh()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnRemoveAllTiles.setOnClickListener {
            showRemoveAllTilesDialog()
        }

        btnResetDefaults.setOnClickListener {
            showResetDefaultsDialog()
        }

        rowPrivacyPolicy.setOnClickListener {
            openUrl(getString(R.string.privacy_policy_url))
        }
    }

    private fun loadPreferences() {
        isBinding = true
        try {
            val editOpacity = MaskPrefs.getOpacity(this)
            sbEditOpacity.progress = (editOpacity - MaskPrefs.MIN_OPACITY).coerceIn(0, 80)
            tvEditOpacityValue.text = "$editOpacity%"

            val lockedOpacity = MaskPrefs.getLockedOpacity(this)
            sbLockedOpacity.progress = (lockedOpacity - MaskPrefs.MIN_OPACITY).coerceIn(0, 80)
            tvLockedOpacityValue.text = "$lockedOpacity%"
        } finally {
            isBinding = false
        }
    }

    private fun showRemoveAllTilesDialog() {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(R.string.dialog_remove_all_title)
            .setMessage(R.string.dialog_remove_all_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                MaskPrefs.clearAllTiles(this)
                if (OverlayService.alive) {
                    sendServiceAction(OverlayService.ACTION_CLEAR_ALL)
                }
                Toast.makeText(this, R.string.toast_tiles_removed, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showResetDefaultsDialog() {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(R.string.dialog_reset_title)
            .setMessage(R.string.dialog_reset_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                MaskPrefs.resetToDefaults(this)
                loadPreferences()
                if (OverlayService.alive) {
                    val intent = Intent(this, OverlayService::class.java).apply {
                        action = OverlayService.ACTION_LOCK
                        putExtra(OverlayService.EXTRA_LOCKED, false)
                    }
                    startServiceIntent(intent)
                    sendServiceAction(OverlayService.ACTION_REFRESH)
                }
                Toast.makeText(this, R.string.toast_defaults_reset, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun notifyServiceRefresh() {
        if (OverlayService.alive) {
            sendServiceAction(OverlayService.ACTION_REFRESH)
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

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.toast_no_app_available, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_no_app_available, Toast.LENGTH_SHORT).show()
        }
    }
}

