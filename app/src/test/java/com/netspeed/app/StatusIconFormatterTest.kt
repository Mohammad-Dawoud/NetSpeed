package com.netspeed.app

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusIconFormatterTest {
    @Test fun iconShowsDownloadRateNotCombinedWhenDownloadDominates() {
        assertEquals(
            StatusIconReading("800", "k", SpeedDirection.DOWNLOAD),
            StatusIconFormatter.fromRates(TransferRates(800.0, 200.0)),
        )
    }

    @Test fun iconShowsUploadRateAndUpArrowWhenUploadDominates() {
        assertEquals(
            StatusIconReading("900", "k", SpeedDirection.UPLOAD),
            StatusIconFormatter.fromRates(TransferRates(20.0, 900.0)),
        )
    }

    @Test fun idleShowsZeroWithoutMisleadingArrow() {
        assertEquals(
            StatusIconReading("0", "k", SpeedDirection.IDLE),
            StatusIconFormatter.fromRates(TransferRates(0.0, 0.0)),
        )
    }

    @Test fun exactTieFavorsDownloadToKeepChoiceDeterministic() {
        assertEquals(
            StatusIconReading("500", "k", SpeedDirection.DOWNLOAD),
            StatusIconFormatter.fromRates(TransferRates(500.0, 500.0)),
        )
    }

    @Test fun highSpeedUsesMbitsWithCompactNumber() {
        assertEquals(
            StatusIconReading("1.3", "M", SpeedDirection.DOWNLOAD),
            StatusIconFormatter.fromRates(TransferRates(1_250.0, 100.0)),
        )
        assertEquals(
            StatusIconReading("12", "M", SpeedDirection.UPLOAD),
            StatusIconFormatter.fromRates(TransferRates(20.0, 12_000.0)),
        )
    }

    @Test fun verySlowTrafficStillHasItsDirection() {
        assertEquals(
            StatusIconReading("0", "k", SpeedDirection.DOWNLOAD),
            StatusIconFormatter.fromRates(TransferRates(0.2, 0.0)),
        )
    }
}
