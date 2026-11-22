package com.netspeed.app

import kotlin.math.roundToLong

internal enum class SpeedDirection { DOWNLOAD, UPLOAD, IDLE }

/** One compact reading suitable for a 24dp status-bar notification icon. */
internal data class StatusIconReading(
    val number: String,
    val unit: String,
    val direction: SpeedDirection,
)

internal object StatusIconFormatter {
    /**
     * A direction arrow must describe the number shown beside it.
     * Therefore, show the dominant direction's rate in the icon, not the
     * download + upload total. The existing notification continues to show
     * the combined rate until Phase 3.
     */
    fun fromRates(rates: TransferRates): StatusIconReading {
        val direction = when {
            rates.downloadKbitPerSecond <= 0.0 && rates.uploadKbitPerSecond <= 0.0 ->
                SpeedDirection.IDLE
            rates.downloadKbitPerSecond >= rates.uploadKbitPerSecond ->
                SpeedDirection.DOWNLOAD
            else -> SpeedDirection.UPLOAD
        }
        val kbits = when (direction) {
            SpeedDirection.DOWNLOAD -> rates.downloadKbitPerSecond
            SpeedDirection.UPLOAD -> rates.uploadKbitPerSecond
            SpeedDirection.IDLE -> 0.0
        }

        val roundedKbits = kbits.roundToLong()
        if (roundedKbits < 1_000) {
            return StatusIconReading(roundedKbits.toString(), "k", direction)
        }

        val mbits = kbits / 1_000
        val tenths = (mbits * 10).roundToLong()
        val number = if (tenths < 100) {
            "${tenths / 10}.${tenths % 10}"
        } else {
            mbits.roundToLong().toString()
        }
        return StatusIconReading(number, "M", direction)
    }
}
