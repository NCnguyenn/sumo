package com.aistudy.myapplication.settings

enum class WeaponMode { HOLD, TOGGLE }
enum class InputMode { D_PAD, JOYSTICK }

data class DriveCalibration(
    val sensitivity: Float = 1f, val deadZone: Float = 0f,
    val leftGain: Float = 1f, val rightGain: Float = 1f, val turnPercent: Int = 100
) {
    fun validated(): DriveCalibration = apply {
        require(sensitivity.isFinite() && sensitivity in 0.25f..2f)
        require(deadZone.isFinite() && deadZone in 0f..0.30f)
        require(leftGain.isFinite() && leftGain in 0.5f..1f)
        require(rightGain.isFinite() && rightGain in 0.5f..1f)
        require(turnPercent in 30..100)
    }
}

data class ControllerSettings(
    val limitPercent: Int = 60, val calibration: DriveCalibration = DriveCalibration(),
    val turnDurationMs: Int = 200, val weaponMode: WeaponMode = WeaponMode.HOLD,
    val inputMode: InputMode = InputMode.D_PAD, val host: String = "10.0.2.2",
    val port: Int = 8888, val sendIntervalMs: Int = 30, val driveTemplate: String = S_TEMPLATE
) {
    fun validated(): ControllerSettings = apply {
        require(limitPercent in 30..100)
        calibration.validated()
        require(turnDurationMs in 1..2000 && port in 1..65535 && sendIntervalMs in 20..50)
        val octets = host.split('.')
        require(octets.size == 4 && octets.all { it.matches(Regex("[0-9]{1,3}")) && it.toInt() in 0..255 })
        require(octets.any { it.toInt() != 0 } && octets.any { it.toInt() != 255 })
        require(octets.first().toInt() !in 224..239)
        require(driveTemplate == S_TEMPLATE || driveTemplate == D_TEMPLATE)
    }
    companion object {
        const val S_TEMPLATE = "S:{L},{R},{B},{W},{K}\n"
        const val D_TEMPLATE = "D:{L},{R},{B},{W},{K}\n"
    }
}
