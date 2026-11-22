package com.netspeed.app

/**
 * Cumulative device-wide traffic counters and a monotonic timestamp, sampled together.
 * All rates below use decimal kbit/s: 1 kbit = 1,000 bits.
 */
internal data class TrafficSnapshot(
    val rxBytes: Long,
    val txBytes: Long,
    val elapsedMs: Long,
)

internal data class TransferRates(
    val downloadKbitPerSecond: Double,
    val uploadKbitPerSecond: Double,
) {
    val combinedKbitPerSecond: Double
        get() = downloadKbitPerSecond + uploadKbitPerSecond
}

/**
 * Calculate separate download (RX) and upload (TX) rates over the ACTUAL elapsed
 * interval. Never infer an exact 1,000 ms interval from Handler scheduling.
 *
 * Null means that there is no trustworthy pair of samples (first sample,
 * unsupported counters, counter reset, or invalid elapsed time).
 */
internal object TrafficRateCalculator {
    fun calculate(previous: TrafficSnapshot?, current: TrafficSnapshot): TransferRates? {
        if (previous == null ||
            previous.rxBytes < 0 || previous.txBytes < 0 ||
            current.rxBytes < previous.rxBytes || current.txBytes < previous.txBytes ||
            current.elapsedMs <= previous.elapsedMs
        ) {
            return null
        }

        val elapsedMs = (current.elapsedMs - previous.elapsedMs).toDouble()
        return TransferRates(
            downloadKbitPerSecond = (current.rxBytes - previous.rxBytes).toDouble() * 8 / elapsedMs,
            uploadKbitPerSecond = (current.txBytes - previous.txBytes).toDouble() * 8 / elapsedMs,
        )
    }
}
