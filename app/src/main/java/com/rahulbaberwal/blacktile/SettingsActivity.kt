package com.rahulbaberwal.blacktile

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

/**
 * Settings activity for configuring overlay appearance, style textures, tile opacity,
 * clearing all configured tiles, resetting to defaults, and accessing the privacy policy.
 * All changes persist to SharedPreferences (MaskPrefs) and propagate immediately to the
 * running OverlayService if active.
 */
class SettingsActivity : Activity() {

    private lateinit var btnBack: ImageButton

    // Style rows and checkmarks
    private lateinit var rowStylePureBlack: LinearLayout
    private lateinit var checkStylePureBlack: ImageView
    private lateinit var rowStyleMatteObsidian: LinearLayout
    private lateinit var checkStyleMatteObsidian: ImageView
    private lateinit var rowStyleDarkNavy: LinearLayout
    private lateinit var checkStyleDarkNavy: ImageView
    private lateinit var rowStyleDarkSlate: LinearLayout
    private lateinit var checkStyleDarkSlate: ImageView
    private lateinit var rowStyleCarbonWeave: LinearLayout
    private lateinit var checkStyleCarbonWeave: ImageView
    private lateinit var rowStyleDotMatrix: LinearLayout
    private lateinit var checkStyleDotMatrix: ImageView

    // Opacity
    private lateinit var tvEditOpacityValue: TextView
    private lateinit var sbEditOpacity: SeekBar
    private lateinit var tvLockedOpacityValue: TextView
    private lateinit var sbLockedOpacity: SeekBar

    // Actions
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

        // Styles
        rowStylePureBlack = findViewById(R.id.rowStylePureBlack)
        checkStylePureBlack = findViewById(R.id.checkStylePureBlack)
        rowStyleMatteObsidian = findViewById(R.id.rowStyleMatteObsidian)
        checkStyleMatteObsidian = findViewById(R.id.checkStyleMatteObsidian)
        rowStyleDarkNavy = findViewById(R.id.rowStyleDarkNavy)
        checkStyleDarkNavy = findViewById(R.id.checkStyleDarkNavy)
        rowStyleDarkSlate = findViewById(R.id.rowStyleDarkSlate)
        checkStyleDarkSlate = findViewById(R.id.checkStyleDarkSlate)
        rowStyleCarbonWeave = findViewById(R.id.rowStyleCarbonWeave)
        checkStyleCarbonWeave = findViewById(R.id.checkStyleCarbonWeave)
        rowStyleDotMatrix = findViewById(R.id.rowStyleDotMatrix)
        checkStyleDotMatrix = findViewById(R.id.checkStyleDotMatrix)

        // Opacity
        tvEditOpacityValue = findViewById(R.id.tvEditOpacityValue)
        sbEditOpacity = findViewById(R.id.sbEditOpacity)
        tvLockedOpacityValue = findViewById(R.id.tvLockedOpacityValue)
        sbLockedOpacity = findViewById(R.id.sbLockedOpacity)

        // Actions
        btnRemoveAllTiles = findViewById(R.id.btnRemoveAllTiles)
        btnResetDefaults = findViewById(R.id.btnResetDefaults)
        rowPrivacyPolicy = findViewById(R.id.rowPrivacyPolicy)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        // Style option listeners
        rowStylePureBlack.setOnClickListener {
            selectOverlayStyle(MaskPrefs.STYLE_PURE_BLACK)
        }
        rowStyleMatteObsidian.setOnClickListener {
            selectOverlayStyle(MaskPrefs.STYLE_MATTE_OBSIDIAN)
        }
        rowStyleDarkNavy.setOnClickListener {
            selectOverlayStyle(MaskPrefs.STYLE_DARK_NAVY)
        }
        rowStyleDarkSlate.setOnClickListener {
            selectOverlayStyle(MaskPrefs.STYLE_DARK_SLATE)
        }
        rowStyleCarbonWeave.setOnClickListener {
            selectOverlayStyle(MaskPrefs.STYLE_CARBON_WEAVE)
        }
        rowStyleDotMatrix.setOnClickListener {
            selectOverlayStyle(MaskPrefs.STYLE_DOT_MATRIX)
        }

        // Opacity sliders
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

    private fun selectOverlayStyle(style: String) {
        MaskPrefs.setOverlayStyle(this, style)
        updateStyleCheckmarks(style)
        notifyServiceRefresh()
    }

    private fun updateStyleCheckmarks(currentStyle: String) {
        checkStylePureBlack.visibility = if (currentStyle == MaskPrefs.STYLE_PURE_BLACK) View.VISIBLE else View.GONE
        checkStyleMatteObsidian.visibility = if (currentStyle == MaskPrefs.STYLE_MATTE_OBSIDIAN) View.VISIBLE else View.GONE
        checkStyleDarkNavy.visibility = if (currentStyle == MaskPrefs.STYLE_DARK_NAVY) View.VISIBLE else View.GONE
        checkStyleDarkSlate.visibility = if (currentStyle == MaskPrefs.STYLE_DARK_SLATE) View.VISIBLE else View.GONE
        checkStyleCarbonWeave.visibility = if (currentStyle == MaskPrefs.STYLE_CARBON_WEAVE) View.VISIBLE else View.GONE
        checkStyleDotMatrix.visibility = if (currentStyle == MaskPrefs.STYLE_DOT_MATRIX) View.VISIBLE else View.GONE
    }

    private fun loadPreferences() {
        isBinding = true
        try {
            // Load and display current style selection
            val currentStyle = MaskPrefs.getOverlayStyle(this)
            updateStyleCheckmarks(currentStyle)

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
