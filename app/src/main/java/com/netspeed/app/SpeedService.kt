package com.netspeed.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.net.TrafficStats
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class SpeedService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val notifications by lazy { requireNotNull(getSystemService(NotificationManager::class.java)) }
    private val openControls by lazy {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    }

    private var previousSample: TrafficSample? = null
    private var lastReading: Reading? = null

    private val sampleSpeed = object : Runnable {
        override fun run() {
            val current = TrafficSample.read()
            val reading = current.readingSince(previousSample)
            previousSample = current

            if (reading != lastReading) {
                val text = reading.number?.let { getString(reading.textResource, it) }
                    ?: getString(reading.textResource)
                val icon = reading.number?.let { drawIcon(it, reading.iconUnit) }
                notifications.notify(NOTIFICATION_ID, notification(text, icon))
                lastReading = reading
            }

            // Handler scheduling uses uptime and lets the device sleep between samples.
            handler.postDelayed(this, SAMPLE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (previousSample == null) {
            startForeground(NOTIFICATION_ID, notification(getString(R.string.speed_measuring)))
            previousSample = TrafficSample.read()
            handler.postDelayed(sampleSpeed, SAMPLE_INTERVAL_MS)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(sampleSpeed)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun notification(text: String, icon: Icon? = null): Notification {
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openControls)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)

        if (icon == null) {
            builder.setSmallIcon(R.drawable.ic_speed)
        } else {
            builder.setSmallIcon(icon)
        }
        return builder.build()
    }

    private fun drawIcon(number: String, unit: String): Icon {
        val scale = resources.displayMetrics.density
        val size = (24 * scale).roundToInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)

        iconPaint.textSize = 13f
        val width = iconPaint.measureText(number)
        if (width > 23f) iconPaint.textSize *= 23f / width
        canvas.drawText(number, 12f, 12f, iconPaint)
        iconPaint.textSize = 10f
        canvas.drawText(unit, 12f, 21f, iconPaint)

        return Icon.createWithBitmap(bitmap)
    }

    private data class TrafficSample(val rxBytes: Long, val txBytes: Long, val elapsedMs: Long) {
        fun readingSince(previous: TrafficSample?): Reading {
            if (previous == null || previous.rxBytes < 0 || previous.txBytes < 0 ||
                rxBytes < previous.rxBytes || txBytes < previous.txBytes ||
                elapsedMs <= previous.elapsedMs
            ) {
                return Reading(null, R.string.speed_unavailable, "")
            }

            val transferredBytes = (rxBytes - previous.rxBytes).toDouble() +
                (txBytes - previous.txBytes).toDouble()
            // One kbit/s is one bit/ms.
            val kilobitsPerSecond = transferredBytes * 8 / (elapsedMs - previous.elapsedMs)
            val roundedKbits = kilobitsPerSecond.roundToLong()
            if (roundedKbits < 1_000) {
                return Reading(roundedKbits.toString(), R.string.speed_kbit, "k")
            }

            val megabitsPerSecond = kilobitsPerSecond / 1_000
            val tenths = (megabitsPerSecond * 10).roundToLong()
            val number = if (tenths < 100) {
                "${tenths / 10}.${tenths % 10}"
            } else {
                megabitsPerSecond.roundToLong().toString()
            }
            return Reading(number, R.string.speed_mbit, "M")
        }

        companion object {
            fun read(): TrafficSample = TrafficSample(
                TrafficStats.getTotalRxBytes(),
                TrafficStats.getTotalTxBytes(),
                SystemClock.elapsedRealtime(),
            )
        }
    }

    private data class Reading(val number: String?, val textResource: Int, val iconUnit: String)

    companion object {
        const val ACTION_STOP = "com.netspeed.app.STOP"
        private const val CHANNEL_ID = "transfer_speed"
        private const val NOTIFICATION_ID = 1
        private const val SAMPLE_INTERVAL_MS = 1_000L

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.speed_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = requireNotNull(context.getSystemService(NotificationManager::class.java))
            manager.createNotificationChannel(channel)
        }
    }
}
