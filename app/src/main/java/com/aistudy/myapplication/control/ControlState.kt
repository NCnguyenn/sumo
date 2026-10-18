package com.aistudy.myapplication.control

enum class ControlButton { FORWARD, BACKWARD, LEFT, RIGHT, BOOST, WEAPON, BRAKE, JOYSTICK }
enum class DriveMode { MANUAL, AUTO }

data class ControlState(
    val pwm: WheelPwm = WheelPwm(0, 0), val boost: Boolean = false,
    val weapon: Boolean = false, val brake: Boolean = false, val killed: Boolean = false,
    val connected: Boolean = false, val mode: DriveMode = DriveMode.MANUAL,
    val pendingMode: DriveMode? = null, val turnId: Int = 0, val turnDurationMs: Int = 0,
    val turnActive: Boolean = false, val unlockPending: Boolean = false
)
