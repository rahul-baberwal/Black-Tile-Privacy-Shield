package com.rahulbaberwal.blacktile

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Outline
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * About Developer activity displaying developer identity, round avatar (or fallback initials "RB"),
 * profile links (Website, GitHub, GitLab, LinkedIn, Email), app version, and open-source repo.
 * Operates strictly offline with zero network permissions.
 */
class AboutActivity : Activity() {

    private lateinit var btnBack: ImageButton
    private lateinit var imgAvatar: ImageView
    private lateinit var tvAvatarPlaceholder: TextView
    private lateinit var btnWebsite: LinearLayout
    private lateinit var btnGithub: LinearLayout
    private lateinit var btnGitlab: LinearLayout
    private lateinit var btnLinkedin: LinearLayout
    private lateinit var btnEmail: LinearLayout
    private lateinit var btnSourceRepo: LinearLayout
    private lateinit var tvAppVersion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        initViews()
        setupAvatar()
        setupAppVersion()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        imgAvatar = findViewById(R.id.imgAvatar)
        tvAvatarPlaceholder = findViewById(R.id.tvAvatarPlaceholder)
        btnWebsite = findViewById(R.id.btnWebsite)
        btnGithub = findViewById(R.id.btnGithub)
        btnGitlab = findViewById(R.id.btnGitlab)
        btnLinkedin = findViewById(R.id.btnLinkedin)
        btnEmail = findViewById(R.id.btnEmail)
        btnSourceRepo = findViewById(R.id.btnSourceRepo)
        tvAppVersion = findViewById(R.id.tvAppVersion)
    }

    private fun setupAvatar() {
        try {
            imgAvatar.setImageResource(R.drawable.developer)
            imgAvatar.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            imgAvatar.clipToOutline = true
            imgAvatar.visibility = View.VISIBLE
            tvAvatarPlaceholder.visibility = View.GONE
        } catch (e: Exception) {
            // Fallback: show circular placeholder with initials "RB"
            imgAvatar.visibility = View.GONE
            tvAvatarPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun setupAppVersion() {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            val versionName = packageInfo.versionName ?: "1.1"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            tvAppVersion.text = getString(R.string.app_version_format, versionName, versionCode)
        } catch (e: Exception) {
            tvAppVersion.text = getString(R.string.app_version_format, "1.1", 2)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnWebsite.setOnClickListener {
            openBrowser(getString(R.string.url_website))
        }

        btnGithub.setOnClickListener {
            openBrowser(getString(R.string.url_github))
        }

        btnGitlab.setOnClickListener {
            openBrowser(getString(R.string.url_gitlab))
        }

        btnLinkedin.setOnClickListener {
            openBrowser(getString(R.string.url_linkedin))
        }

        btnEmail.setOnClickListener {
            openEmail(getString(R.string.url_email))
        }

        btnSourceRepo.setOnClickListener {
            openBrowser(getString(R.string.url_source_repo))
        }
    }

    private fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.toast_no_app_available, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_no_app_available, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEmail(emailAddress: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$emailAddress")
                putExtra(Intent.EXTRA_SUBJECT, "Black Tile Feedback")
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.toast_no_app_available, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_no_app_available, Toast.LENGTH_SHORT).show()
        }
    }
}

