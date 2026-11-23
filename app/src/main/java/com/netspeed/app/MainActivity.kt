package com.netspeed.app

import android.app.Activity
import android.app.AlertDialog
import android.Manifest
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
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
    private lateinit var powerButton: View
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
        drawerOverlay = requireViewById(R.id.drawer_overlay)

        powerButton.setOnClickListener { toggleMonitoring() }
        requireViewById<View>(R.id.menu_button).setOnClickListener { openDrawer() }
        requireViewById<View>(R.id.drawer_scrim).setOnClickListener { closeDrawer() }
        requireViewById<View>(R.id.drawer_close).setOnClickListener { closeDrawer() }

        requireViewById<View>(R.id.nav_repository).setOnClickListener { openUrl(REPOSITORY_URL) }
        requireViewById<View>(R.id.nav_report_issue).setOnClickListener { openUrl(ISSUES_URL) }
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
            // Deliver Stop to the existing foreground service.
            startService(intent.setAction(SpeedService.ACTION_STOP))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Android 13+: the status-bar indicator requires a visible
            // notification. Ask only when the user taps the power button.
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST
            )
        } else {
            startMonitoring()
        }
        refreshScreen()
    }

    private fun startMonitoring() {
        requestedStart = true
        startForegroundService(Intent(this, SpeedService::class.java))
    }

    @Deprecated("Legacy Activity permission result callback; no AndroidX dependency required")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                startMonitoring()
            } else {
                showInfo(
                    getString(R.string.notification_permission_title),
                    getString(R.string.notification_permission_body)
                )
            }
            refreshScreen()
        }
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
        // The button is icon-only, so announce the correct action to TalkBack.
        powerButton.contentDescription =
            getString(if (showRunning) R.string.stop_monitoring else R.string.start_monitoring)
        powerButton.stateDescription =
            getString(if (showRunning) R.string.monitoring_on else R.string.monitoring_off)
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
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: android.content.ActivityNotFoundException) {
            showInfo(getString(R.string.github_repository), url)
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersion(): String =
        packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 100
        private const val REPOSITORY_URL = "https://github.com/Mohammad-Dawoud/NetSpeed"
        private const val ISSUES_URL = "https://github.com/Mohammad-Dawoud/NetSpeed/issues"
    }
}
