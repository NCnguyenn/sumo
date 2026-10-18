package com.aistudy.myapplication.control

import com.aistudy.myapplication.settings.DriveCalibration
import com.aistudy.myapplication.settings.InputMode
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class WheelPwm(val left: Int, val right: Int) {
    init { require(left in -255..255 && right in -255..255) }
}

object DriveMixer {
    fun mix(x: Float, y: Float, limitPercent: Int, boost: Boolean,
            calibration: DriveCalibration = DriveCalibration(), inputMode: InputMode = InputMode.JOYSTICK): WheelPwm {
        require(limitPercent in 30..100)
        calibration.validated()
        if (!x.isFinite() || !y.isFinite()) return WheelPwm(0, 0)
        var cx = x.coerceIn(-1f, 1f).toDouble()
        var cy = y.coerceIn(-1f, 1f).toDouble()
        if (inputMode == InputMode.JOYSTICK) {
            val radius = hypot(cx, cy)
            if (radius <= calibration.deadZone) return WheelPwm(0, 0)
            val magnitude = min(1.0, calibration.sensitivity * (min(radius, 1.0) - calibration.deadZone) / (1 - calibration.deadZone))
            cx = cx / radius * magnitude
            cy = cy / radius * magnitude
        }
        val turn = cx * calibration.turnPercent / 100
        val forward = cy
        val scale = max(1.0, max(abs(forward + turn), abs(forward - turn)))
        val left = (forward + turn) / scale * calibration.leftGain
        val right = (forward - turn) / scale * calibration.rightGain
        val peak = max(abs(left), abs(right))
        if (peak == 0.0) return WheelPwm(0, 0)
        val multiplier = if (boost) 255 / peak else 255 * limitPercent / 100.0
        fun pwm(value: Double): Int {
            val rounded = abs(value * multiplier).roundToInt().coerceIn(0, 255)
            return if (value < 0) -rounded else rounded
        }
        return WheelPwm(pwm(left), pwm(right))
    }
}
