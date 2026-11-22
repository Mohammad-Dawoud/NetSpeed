package com.netspeed.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSpeedFormatterTest {
    @Test fun matchingReferenceStyleForSubKilobitTraffic() {
        assertEquals(
            "Down: 624 b/s — Up: 0 b/s",
            NotificationSpeedFormatter.format(TransferRates(0.624, 0.0)),
        )
    }

    @Test fun downloadAndUploadHaveIndependentValuesAndUnits() {
        assertEquals(
            "Down: 47 Kb/s — Up: 1.5 Mb/s",
            NotificationSpeedFormatter.format(TransferRates(47.0, 1_500.0)),
        )
    }

    @Test fun highSpeedUsesMegabitsAndSmallSpeedRetainsBits() {
        assertEquals(
            "Down: 144 Mb/s — Up: 132 b/s",
            NotificationSpeedFormatter.format(TransferRates(144_000.0, 0.132)),
        )
    }

    @Test fun valuesChangeEvenWhenTotalRemainsConstant() {
        val initial = NotificationSpeedFormatter.format(TransferRates(100.0, 50.0))
        val changed = NotificationSpeedFormatter.format(TransferRates(90.0, 60.0))
        assertEquals("Down: 100 Kb/s — Up: 50 Kb/s", initial)
        assertEquals("Down: 90 Kb/s — Up: 60 Kb/s", changed)
    }

    @Test fun zeroAndFractionalKilobitsAreReadable() {
        assertEquals("0 b/s", NotificationSpeedFormatter.formatRate(0.0))
        assertEquals("500 b/s", NotificationSpeedFormatter.formatRate(0.5))
        assertEquals("1.2 Kb/s", NotificationSpeedFormatter.formatRate(1.24))
        assertEquals("1.3 Mb/s", NotificationSpeedFormatter.formatRate(1_250.0))
    }

    @Test fun wholeNumbersDoNotShowUnnecessaryDecimals() {
        assertEquals(
            "Down: 6 Mb/s — Up: 0 b/s",
            NotificationSpeedFormatter.format(TransferRates(6_000.0, 0.0)),
        )
        assertEquals("8 Kb/s", NotificationSpeedFormatter.formatRate(8.0))
    }

    @Test fun unsupportedValuesAreNotPresentedAsRealTraffic() {
        assertEquals("unavailable", NotificationSpeedFormatter.formatRate(Double.NaN))
        assertEquals("unavailable", NotificationSpeedFormatter.formatRate(-1.0))
    }
}
