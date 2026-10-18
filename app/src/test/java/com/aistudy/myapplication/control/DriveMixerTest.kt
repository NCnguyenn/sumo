package com.aistudy.myapplication.control

import com.aistudy.myapplication.settings.DriveCalibration
import org.junit.Assert.*
import org.junit.Test

class DriveMixerTest {
    @Test fun L01_L03_F01_limitsAndSymmetricRounding() {
        for ((limit, pwm) in listOf(30 to 77, 60 to 153, 100 to 255)) {
            assertEquals(WheelPwm(pwm, pwm), DriveMixer.mix(0f, 1f, limit, false))
            assertEquals(WheelPwm(-pwm, -pwm), DriveMixer.mix(0f, -1f, limit, false))
        }
    }
    @Test fun L02_pivotsHaveCorrectSigns() {
        assertEquals(WheelPwm(-255, 255), DriveMixer.mix(-1f, 0f, 100, false))
        assertEquals(WheelPwm(255, -255), DriveMixer.mix(1f, 0f, 100, false))
    }
    @Test fun L04_L05_boostPreservesRatioAndNeverCreatesDirection() {
        assertEquals(WheelPwm(255, 255), DriveMixer.mix(0f, 0.2f, 30, true))
        assertEquals(WheelPwm(255, 0), DriveMixer.mix(1f, 1f, 30, true))
        assertEquals(WheelPwm(0, 0), DriveMixer.mix(0f, 0f, 60, true))
        assertEquals(WheelPwm(-255, -255), DriveMixer.mix(0f, -1f, 60, true))
    }
    @Test fun L09_L10_allQuadrantsFiniteClampedAndBounded() {
        for (x in listOf(-10f, -1f, -0.3f, 0f, 0.3f, 1f, 10f)) {
            for (y in listOf(-10f, -1f, 0f, 1f, 10f)) {
                for (boost in listOf(false, true)) {
                    val pwm = DriveMixer.mix(x, y, 100, boost)
                    assertTrue(pwm.left in -255..255 && pwm.right in -255..255)
                }
            }
        }
        assertEquals(WheelPwm(255, 0), DriveMixer.mix(4f, 4f, 100, false))
        assertEquals(WheelPwm(0, 255), DriveMixer.mix(-1f, 1f, 100, false))
        assertEquals(WheelPwm(0, -255), DriveMixer.mix(1f, -1f, 100, false))
    }
    @Test fun L10_nonFiniteInputIsNeutralEvenWithBoost() {
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(WheelPwm(0, 0), DriveMixer.mix(bad, 1f, 60, true))
            assertEquals(WheelPwm(0, 0), DriveMixer.mix(1f, bad, 60, false))
        }
    }
    @Test fun F03_deadZoneIsRadialAndRescaled() {
        val c = DriveCalibration(deadZone = 0.2f)
        assertEquals(WheelPwm(0, 0), DriveMixer.mix(0f, 0.2f, 100, true, c))
        assertEquals(WheelPwm(128, 128), DriveMixer.mix(0f, 0.6f, 100, false, c))
        assertEquals(WheelPwm(0, 0), DriveMixer.mix(0.1f, 0.1f, 100, false, c))
    }
    @Test fun F03_sensitivityGainsTurnAndBoostUseCalibration() {
        assertEquals(WheelPwm(128, 128), DriveMixer.mix(0f, 0.25f, 100, false, DriveCalibration(sensitivity = 2f)))
        assertEquals(WheelPwm(77, 153), DriveMixer.mix(0f, 1f, 60, false, DriveCalibration(leftGain = 0.5f)))
        assertEquals(WheelPwm(128, 255), DriveMixer.mix(0f, 1f, 30, true, DriveCalibration(leftGain = 0.5f)))
        assertEquals(WheelPwm(128, -128), DriveMixer.mix(1f, 0f, 100, false, DriveCalibration(turnPercent = 50)))
    }
    @Test fun invalidLimitsAndPwmAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { DriveMixer.mix(0f, 1f, 29, false) }
        assertThrows(IllegalArgumentException::class.java) { WheelPwm(256, 0) }
        assertThrows(IllegalArgumentException::class.java) { WheelPwm(0, -256) }
    }
}
