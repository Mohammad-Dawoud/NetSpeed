package com.netspeed.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.net.TrafficStats
import android.os.Handler
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import kotlin.math.roundToInt

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
    private var lastNotificationText: String? = null
    private var lastIconReading: StatusIconReading? = null

    private val localBinder = LocalBinder()
    inner class LocalBinder : Binder() {
        val service: SpeedService get() = this@SpeedService
    }

    var isMonitoring: Boolean = false
        private set
    var currentRates: TransferRates? = null
        private set

    private val sampleSpeed = object : Runnable {
        override fun run() {
            val current = readSnapshot()
            val rates = TrafficRateCalculator.calculate(previousSample, current)
            previousSample = current
            currentRates = rates

            // Phase 3: show RX and TX separately in the notification;
            // retain the Phase 2 dominant-direction status-bar icon.
            val notificationText = rates?.let(NotificationSpeedFormatter::format)
                ?: getString(R.string.speed_unavailable)
            val iconReading = rates?.let(StatusIconFormatter::fromRates)

            // The individual rates may change without changing the combined
            // speed or the rounded status icon, so compare notification text too.
            if (notificationText != lastNotificationText || iconReading != lastIconReading) {
                val icon = iconReading?.let(::drawIcon)
                notifications.notify(NOTIFICATION_ID, notification(notificationText, icon))
                lastNotificationText = notificationText
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
            handler.removeCallbacks(sampleSpeed)
            isMonitoring = false
            previousSample = null
            currentRates = null
            lastNotificationText = null
            lastIconReading = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!isMonitoring) {
            if (intent == null) Log.i(TAG, "Speed service restarted by Android")
            startForeground(NOTIFICATION_ID, notification(getString(R.string.speed_measuring)))
            isMonitoring = true
            currentRates = null
            previousSample = readSnapshot()
            handler.removeCallbacks(sampleSpeed)
            handler.postDelayed(sampleSpeed, SAMPLE_INTERVAL_MS)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = localBinder

    override fun onDestroy() {
        isMonitoring = false
        currentRates = null
        previousSample = null
        handler.removeCallbacks(sampleSpeed)
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(TAG, "Speed service destroyed")
        super.onDestroy()
    }

    private fun notification(text: String, icon: Icon? = null): Notification {
        // Use contrasting teal in light/dark themes. SystemUI can override
        // styled notification text on some Android manufacturer versions.
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val accentColor = if (isDark) Color.rgb(94, 224, 213) else Color.rgb(0, 112, 112)
        val coloredText = SpannableString(text).apply {
            setSpan(ForegroundColorSpan(accentColor), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(coloredText)
            .setColor(accentColor)
            .setStyle(
                Notification.BigTextStyle()
                    .setBigContentTitle(getString(R.string.app_name))
                    .bigText(coloredText)
            )
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

        // Maximize the number within a 24dp status icon while reserving the
        // lower row (arrow and unit). The arrow's highest painted point is
        // about y=15.1dp, including half its stroke; keep a 1dp clear gap.
        // The bounds/width are measured for every reading, so long values
        // shrink rather than touching the icon edges or lower-row symbols.
        val numberTop = 0.55f
        val maxNumberHeight = 13.5f
        val maxNumberWidth = 22.8f
        iconPaint.textSize = 24f
        val numberBounds = android.graphics.Rect()
        iconPaint.getTextBounds(reading.number, 0, reading.number.length, numberBounds)
        val textWidth = iconPaint.measureText(reading.number).coerceAtLeast(1f)
        val glyphHeight = numberBounds.height().coerceAtLeast(1).toFloat()
        iconPaint.textSize *= minOf(1f, maxNumberWidth / textWidth, maxNumberHeight / glyphHeight)
        iconPaint.getTextBounds(reading.number, 0, reading.number.length, numberBounds)
        canvas.drawText(reading.number, 12f, numberTop - numberBounds.top, iconPaint)

        // A slightly longer arrow (6.8dp shaft vs 5.5dp before), drawn
        // alongside the unit. The second row starts below the number.
        when (reading.direction) {
            SpeedDirection.DOWNLOAD -> {
                canvas.drawLine(6.2f, 16f, 6.2f, 22.8f, arrowPaint)
                canvas.drawLine(3.5f, 20f, 6.2f, 22.8f, arrowPaint)
                canvas.drawLine(8.9f, 20f, 6.2f, 22.8f, arrowPaint)
            }
            SpeedDirection.UPLOAD -> {
                canvas.drawLine(6.2f, 22.8f, 6.2f, 16f, arrowPaint)
                canvas.drawLine(3.5f, 18.8f, 6.2f, 16f, arrowPaint)
                canvas.drawLine(8.9f, 18.8f, 6.2f, 16f, arrowPaint)
            }
            SpeedDirection.IDLE -> Unit
        }

        iconPaint.textSize = 9.6f
        val unitX = if (reading.direction == SpeedDirection.IDLE) 12f else 17.5f
        canvas.drawText(reading.unit, unitX, 23f, iconPaint)
        return Icon.createWithBitmap(bitmap)
    }

    private fun readSnapshot() = TrafficSnapshot(
        rxBytes = TrafficStats.getTotalRxBytes(),
        txBytes = TrafficStats.getTotalTxBytes(),
        elapsedMs = SystemClock.elapsedRealtime(),
    )

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
