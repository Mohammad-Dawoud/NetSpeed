package com.netspeed.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSpeedFormatterTest {
    @Test fun matchingReferenceStyleForSubKilobitTraffic() {
        assertEquals(
            "Down: 624 bit/s / Up: 0 bit/s",
            NotificationSpeedFormatter.format(TransferRates(0.624, 0.0)),
        )
    }

    @Test fun downloadAndUploadHaveIndependentValuesAndUnits() {
        assertEquals(
            "Down: 47 kbit/s / Up: 1.5 Mbit/s",
            NotificationSpeedFormatter.format(TransferRates(47.0, 1_500.0)),
        )
    }

    @Test fun highSpeedUsesMegabitsAndSmallSpeedRetainsBits() {
        assertEquals(
            "Down: 144 Mbit/s / Up: 132 bit/s",
            NotificationSpeedFormatter.format(TransferRates(144_000.0, 0.132)),
        )
    }

    @Test fun valuesChangeEvenWhenTotalRemainsConstant() {
        val initial = NotificationSpeedFormatter.format(TransferRates(100.0, 50.0))
        val changed = NotificationSpeedFormatter.format(TransferRates(90.0, 60.0))
        assertEquals("Down: 100 kbit/s / Up: 50 kbit/s", initial)
        assertEquals("Down: 90 kbit/s / Up: 60 kbit/s", changed)
    }

    @Test fun zeroAndFractionalKilobitsAreReadable() {
        assertEquals("0 bit/s", NotificationSpeedFormatter.formatRate(0.0))
        assertEquals("500 bit/s", NotificationSpeedFormatter.formatRate(0.5))
        assertEquals("1.2 kbit/s", NotificationSpeedFormatter.formatRate(1.24))
        assertEquals("1.3 Mbit/s", NotificationSpeedFormatter.formatRate(1_250.0))
    }

    @Test fun unsupportedValuesAreNotPresentedAsRealTraffic() {
        assertEquals("unavailable", NotificationSpeedFormatter.formatRate(Double.NaN))
        assertEquals("unavailable", NotificationSpeedFormatter.formatRate(-1.0))
    }
}
