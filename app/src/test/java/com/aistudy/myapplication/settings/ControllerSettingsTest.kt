package com.aistudy.myapplication.settings

import org.junit.Assert.*
import org.junit.Test

class ControllerSettingsTest {
    @Test fun defaultsAndApprovedBoundaryValuesAreValid() {
        assertEquals(ControllerSettings(), ControllerSettings().validated())
        for (interval in listOf(20, 30, 50)) for (duration in listOf(1, 200, 2000)) {
            ControllerSettings(sendIntervalMs = interval, turnDurationMs = duration, port = 65535).validated()
        }
        DriveCalibration(0.25f, 0.30f, 0.5f, 0.5f, 30).validated()
        DriveCalibration(2f, 0f, 1f, 1f, 100).validated()
    }
    @Test fun F03_invalidCalibrationCannotBeApplied() {
        val invalid = listOf(
            DriveCalibration(sensitivity = Float.NaN), DriveCalibration(sensitivity = Float.POSITIVE_INFINITY),
            DriveCalibration(sensitivity = 0.24f), DriveCalibration(sensitivity = 2.01f),
            DriveCalibration(deadZone = -0.1f), DriveCalibration(deadZone = 0.31f),
            DriveCalibration(leftGain = 0.49f), DriveCalibration(rightGain = 1.01f),
            DriveCalibration(turnPercent = 29), DriveCalibration(turnPercent = 101)
        )
        for (c in invalid) assertThrows(IllegalArgumentException::class.java) { c.validated() }
    }
    @Test fun invalidSettingsAreRejectedAsAWhole() {
        val s = ControllerSettings()
        for (bad in listOf(s.copy(limitPercent = 29), s.copy(limitPercent = 101), s.copy(port = 0),
            s.copy(port = 65536), s.copy(turnDurationMs = 0), s.copy(turnDurationMs = 2001),
            s.copy(sendIntervalMs = 19), s.copy(sendIntervalMs = 51), s.copy(host = ""),
            s.copy(host = "0.0.0.0"), s.copy(host = "000.000.000.000"), s.copy(host = "224.0.0.1"), s.copy(host = "255.255.255.255"),
            s.copy(host = "1.2.3.256"), s.copy(host = "example.org"), s.copy(host = "1.2.3"),
            s.copy(driveTemplate = "S:{L},{R}\\n"), s.copy(driveTemplate = "evil\n"))) {
            assertThrows(IllegalArgumentException::class.java) { bad.validated() }
        }
        s.copy(driveTemplate = ControllerSettings.D_TEMPLATE).validated()
    }
}
