package com.netspeed.app

import kotlin.math.roundToLong

/**
 * Formats the individually measured traffic directions for the notification.
 * TrafficStats counts bytes; TrafficRateCalculator converts to decimal kbit/s.
 * Display very low rates in bit/s so a value such as 624 bit/s does not
 * misleadingly appear as 1 kbit/s or 0 kbit/s.
 */
internal object NotificationSpeedFormatter {
    fun format(rates: TransferRates): String =
        "Down: ${formatRate(rates.downloadKbitPerSecond)} / Up: ${formatRate(rates.uploadKbitPerSecond)}"

    internal fun formatRate(kbitsPerSecond: Double): String {
        if (!kbitsPerSecond.isFinite() || kbitsPerSecond < 0) return "unavailable"
        if (kbitsPerSecond < 1.0) {
            return "${(kbitsPerSecond * 1_000).roundToLong()} bit/s"
        }
        if (kbitsPerSecond < 1_000.0) {
            return "${scaledNumber(kbitsPerSecond)} kbit/s"
        }
        return "${scaledNumber(kbitsPerSecond / 1_000)} Mbit/s"
    }

    private fun scaledNumber(value: Double): String {
        val tenths = (value * 10).roundToLong()
        return if (tenths < 100) {
            "${tenths / 10}.${tenths % 10}"
        } else {
            value.roundToLong().toString()
        }
    }
}
