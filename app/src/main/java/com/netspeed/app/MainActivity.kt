package com.netspeed.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Minimal single-purpose home screen. The binder exposes the SAME sampled
 * values used by the foreground notification; this Activity does not collect
 * its own TrafficStats samples.
 */
class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var speedService: SpeedService? = null
    private var serviceBound = false
    private var requestedStart = false

    private lateinit var downloadValue: TextView
    private lateinit var uploadValue: TextView
    private lateinit var powerButton: LinearLayout
    private lateinit var powerLabel: TextView
    private lateinit var drawerOverlay: View

    private val refresh = object : Runnable {
        override fun run() {
            refreshScreen()
            handler.postDelayed(this, 500L)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            speedService = (binder as? SpeedService.LocalBinder)?.service
            refreshScreen()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            speedService = null
            refreshScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SpeedService.createChannel(this)

        downloadValue = requireViewById(R.id.download_value)
        uploadValue = requireViewById(R.id.upload_value)
        powerButton = requireViewById(R.id.power_button)
        powerLabel = requireViewById(R.id.power_label)
        drawerOverlay = requireViewById(R.id.drawer_overlay)

        powerButton.setOnClickListener { toggleMonitoring() }
        requireViewById<View>(R.id.menu_button).setOnClickListener { openDrawer() }
        requireViewById<View>(R.id.drawer_scrim).setOnClickListener { closeDrawer() }
        requireViewById<View>(R.id.drawer_close).setOnClickListener { closeDrawer() }

        requireViewById<View>(R.id.nav_about).setOnClickListener {
            showInfo(getString(R.string.about_title), getString(R.string.about_body))
        }
        requireViewById<View>(R.id.nav_links).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.useful_links)
                .setItems(arrayOf("GitHub repository", "Releases", "Report an issue")) { _, which ->
                    val urls = arrayOf(
                        REPOSITORY_URL,
                        "$REPOSITORY_URL/releases",
                        "$REPOSITORY_URL/issues"
                    )
                    openUrl(urls[which])
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        requireViewById<View>(R.id.nav_version).setOnClickListener {
            showInfo(getString(R.string.version_label), getString(R.string.version_value, appVersion()))
        }
        requireViewById<View>(R.id.nav_repository).setOnClickListener { openUrl(REPOSITORY_URL) }
        requireViewById<View>(R.id.nav_developer).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.developer_info)
                .setMessage(R.string.developer_description)
                .setPositiveButton(R.string.developer_profile) { _, _ ->
                    openUrl(DEVELOPER_URL)
                }
                .setNegativeButton(android.R.string.close, null)
                .show()
        }
        requireViewById<TextView>(R.id.drawer_version).text =
            getString(R.string.version_value, appVersion())

        refreshScreen()
    }

    override fun onStart() {
        super.onStart()
        serviceBound = bindService(
            Intent(this, SpeedService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        handler.post(refresh)
    }

    override fun onStop() {
        handler.removeCallbacks(refresh)
        if (serviceBound) {
            unbindService(connection)
            serviceBound = false
        }
        speedService = null
        super.onStop()
    }

    private fun toggleMonitoring() {
        val intent = Intent(this, SpeedService::class.java)
        if (speedService?.isMonitoring == true || requestedStart) {
            requestedStart = false
            // Ensure a stop request reaches even an already-running foreground
            // service. Never call startForegroundService for a STOP action.
            startService(intent.setAction(SpeedService.ACTION_STOP))
        } else {
            requestedStart = true
            startForegroundService(intent)
        }
        refreshScreen()
    }

    private fun refreshScreen() {
        val running = speedService?.isMonitoring == true
        if (running) requestedStart = false
        val rates = if (running) speedService?.currentRates else null
        downloadValue.text = rates?.let {
            NotificationSpeedFormatter.formatRate(it.downloadKbitPerSecond)
        } ?: if (running) "—" else getString(R.string.zero_speed)
        uploadValue.text = rates?.let {
            NotificationSpeedFormatter.formatRate(it.uploadKbitPerSecond)
        } ?: if (running) "—" else getString(R.string.zero_speed)

        val showRunning = running || requestedStart
        powerButton.setBackgroundResource(
            if (showRunning) R.drawable.bg_button_active else R.drawable.bg_button_inactive
        )
        powerLabel.setText(if (showRunning) R.string.stop_monitoring else R.string.start_monitoring)
        powerButton.contentDescription = powerLabel.text
    }

    private fun openDrawer() {
        drawerOverlay.visibility = View.VISIBLE
    }

    private fun closeDrawer() {
        drawerOverlay.visibility = View.GONE
    }

    @Deprecated("Navigation handled by Activity on Android 12")
    override fun onBackPressed() {
        if (drawerOverlay.visibility == View.VISIBLE) closeDrawer()
        else super.onBackPressed()
    }

    private fun showInfo(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            showInfo(getString(R.string.useful_links), url)
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersion(): String =
        packageManager.getPackageInfo(packageName, 0).versionName ?: "0.1.0"

    companion object {
        private const val REPOSITORY_URL = "https://github.com/Mohammad-Dawoud/NetSpeed"
        private const val DEVELOPER_URL = "https://github.com/Mohammad-Dawoud"
    }
}
