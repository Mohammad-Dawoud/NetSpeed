package com.netspeed.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrafficRateCalculatorTest {
    @Test fun separatesDownloadAndUploadInKbitPerSecond() {
        val previous = TrafficSnapshot(1_000, 2_000, 5_000)
        val current = TrafficSnapshot(2_000, 2_250, 6_000)
        val rates = requireNotNull(TrafficRateCalculator.calculate(previous, current))

        assertEquals(8.0, rates.downloadKbitPerSecond, 0.000001)
        assertEquals(2.0, rates.uploadKbitPerSecond, 0.000001)
        assertEquals(10.0, rates.combinedKbitPerSecond, 0.000001)
    }

    @Test fun usesActualElapsedTimeNotAnAssumedSecond() {
        val rates = requireNotNull(TrafficRateCalculator.calculate(
            TrafficSnapshot(0, 0, 1_000),
            TrafficSnapshot(2_000, 1_000, 3_000),
        ))
        assertEquals(8.0, rates.downloadKbitPerSecond, 0.000001)
        assertEquals(4.0, rates.uploadKbitPerSecond, 0.000001)
    }

    @Test fun reportsZeroWhenCountersAreUnchanged() {
        val rates = requireNotNull(TrafficRateCalculator.calculate(
            TrafficSnapshot(100, 200, 1_000),
            TrafficSnapshot(100, 200, 2_000),
        ))
        assertEquals(0.0, rates.combinedKbitPerSecond, 0.0)
    }

    @Test fun rejectsFirstSampleAndUnsupportedCounters() {
        assertNull(TrafficRateCalculator.calculate(null, TrafficSnapshot(100, 200, 2_000)))
        assertNull(TrafficRateCalculator.calculate(
            TrafficSnapshot(-1, 100, 1_000),
            TrafficSnapshot(100, 200, 2_000),
        ))
        assertNull(TrafficRateCalculator.calculate(
            TrafficSnapshot(100, 100, 1_000),
            TrafficSnapshot(200, -1, 2_000),
        ))
    }

    @Test fun rejectsCounterResetAndInvalidTimeDelta() {
        val previous = TrafficSnapshot(100, 200, 1_000)
        assertNull(TrafficRateCalculator.calculate(previous, TrafficSnapshot(99, 300, 2_000)))
        assertNull(TrafficRateCalculator.calculate(previous, TrafficSnapshot(300, 199, 2_000)))
        assertNull(TrafficRateCalculator.calculate(previous, TrafficSnapshot(200, 300, 1_000)))
        assertNull(TrafficRateCalculator.calculate(previous, TrafficSnapshot(200, 300, 999)))
    }
}
