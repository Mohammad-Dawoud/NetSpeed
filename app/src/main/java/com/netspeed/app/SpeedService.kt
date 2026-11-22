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
import android.util.Log
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
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var previousSample: TrafficSnapshot? = null
    private var lastReading: Reading? = null
    private var lastIconReading: StatusIconReading? = null

    private val sampleSpeed = object : Runnable {
        override fun run() {
            val current = readSnapshot()
            val rates = TrafficRateCalculator.calculate(previousSample, current)
            previousSample = current

            // Phase 2: the notification still reports combined traffic, but the
            // status icon shows the faster direction (RX or TX) and its arrow.
            val reading = rates?.let { Reading.fromCombinedKbit(it.combinedKbitPerSecond) }
                ?: Reading(null, R.string.speed_unavailable, "")
            val iconReading = rates?.let(StatusIconFormatter::fromRates)

            // Update when the icon direction changes, even if combined speed
            // rounds to the same displayed number.
            if (reading != lastReading || iconReading != lastIconReading) {
                val text = reading.number?.let { getString(reading.textResource, it) }
                    ?: getString(reading.textResource)
                val icon = iconReading?.let(::drawIcon)
                notifications.notify(NOTIFICATION_ID, notification(text, icon))
                lastReading = reading
                lastIconReading = iconReading
            }

            // Use real elapsed time in the calculation even if this callback was delayed.
            handler.postDelayed(this, SAMPLE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
        Log.i(TAG, "Speed service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.i(TAG, "Speed service stopped explicitly")
            stopSelf()
            return START_NOT_STICKY
        }
        if (previousSample == null) {
            if (intent == null) Log.i(TAG, "Speed service restarted by Android")
            startForeground(NOTIFICATION_ID, notification(getString(R.string.speed_measuring)))
            previousSample = readSnapshot()
            handler.postDelayed(sampleSpeed, SAMPLE_INTERVAL_MS)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(sampleSpeed)
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(TAG, "Speed service destroyed")
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

    private fun drawIcon(reading: StatusIconReading): Icon {
        val scale = resources.displayMetrics.density
        val size = (24 * scale).roundToInt().coerceAtLeast(24)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(size / 24f, size / 24f)

        // Bold, larger first line: 17.5dp instead of 13dp. Fit long
        // numbers to the available width without clipping.
        iconPaint.textSize = 17.5f
        val width = iconPaint.measureText(reading.number)
        if (width > 22f) iconPaint.textSize *= 22f / width
        canvas.drawText(reading.number, 12f, 15.5f, iconPaint)

        // Draw the arrow as strokes rather than a text glyph; arrow fonts
        // differ across manufacturers and may become illegible when scaled.
        when (reading.direction) {
            SpeedDirection.DOWNLOAD -> {
                canvas.drawLine(6.5f, 16.5f, 6.5f, 22f, arrowPaint)
                canvas.drawLine(3.8f, 19.3f, 6.5f, 22f, arrowPaint)
                canvas.drawLine(9.2f, 19.3f, 6.5f, 22f, arrowPaint)
            }
            SpeedDirection.UPLOAD -> {
                canvas.drawLine(6.5f, 22f, 6.5f, 16.5f, arrowPaint)
                canvas.drawLine(3.8f, 19.2f, 6.5f, 16.5f, arrowPaint)
                canvas.drawLine(9.2f, 19.2f, 6.5f, 16.5f, arrowPaint)
            }
            SpeedDirection.IDLE -> Unit
        }

        iconPaint.textSize = 11f
        val unitX = if (reading.direction == SpeedDirection.IDLE) 12f else 17.5f
        canvas.drawText(reading.unit, unitX, 23f, iconPaint)
        return Icon.createWithBitmap(bitmap)
    }

    private fun readSnapshot() = TrafficSnapshot(
        rxBytes = TrafficStats.getTotalRxBytes(),
        txBytes = TrafficStats.getTotalTxBytes(),
        elapsedMs = SystemClock.elapsedRealtime(),
    )

    private data class Reading(val number: String?, val textResource: Int, val iconUnit: String) {
        companion object {
            fun fromCombinedKbit(kilobitsPerSecond: Double): Reading {
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
        }
    }

    companion object {
        const val ACTION_STOP = "com.netspeed.app.STOP"
        private const val TAG = "NetSpeed"
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
