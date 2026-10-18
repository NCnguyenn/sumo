package com.aistudy.myapplication.control

import com.aistudy.myapplication.settings.ControllerSettings
import com.aistudy.myapplication.settings.InputMode
import com.aistudy.myapplication.settings.WeaponMode

class ControlReducer(initialSettings: ControllerSettings = ControllerSettings(),
                     private val nowMs: () -> Long = { System.nanoTime() / 1_000_000 }) {
    var settings = initialSettings.validated()
        private set
    private val pressed = mutableMapOf<Int, ControlButton>()
    private val blocked = mutableSetOf<Int>()
    private val awaitRelease = mutableSetOf<Int>()
    private val joystick = mutableMapOf<Int, Pair<Float, Float>>()
    private var connected = false
    private var killed = false
    private var caps = 0
    private var autoApproved = false
    private var mode = DriveMode.MANUAL
    private var pendingMode: DriveMode? = null
    private var toggledWeapon = false
    private var unlockStart: Long? = null
    private var unlockPending = false
    private var nextTurnId = 0
    private var turnId = 0
    private var turnDuration = 0
    private var turnActive = false
    private fun ControlButton.direction() = this in setOf(ControlButton.FORWARD, ControlButton.BACKWARD,
        ControlButton.LEFT, ControlButton.RIGHT, ControlButton.JOYSTICK)
    internal fun isDirectionPointerValid(id:Int)=pressed[id]?.direction()==true && id !in blocked

    val state: ControlState get() {
        val valid = pressed.filterKeys { it !in blocked }
        val brake = connected && !killed && ControlButton.BRAKE in valid.values
        val manual = connected && !killed && !brake && mode == DriveMode.MANUAL && pendingMode == null && !unlockPending && turnId == 0
        val weapon = manual && (toggledWeapon || (settings.weaponMode == WeaponMode.HOLD && ControlButton.WEAPON in valid.values))
        val boostHeld = ControlButton.BOOST in valid.values
        val pwm = if (manual && awaitRelease.isEmpty()) {
            if (settings.inputMode == InputMode.JOYSTICK) {
                val position = valid.entries.firstOrNull { it.value == ControlButton.JOYSTICK }?.let { joystick[it.key] }
                DriveMixer.mix(position?.first ?: 0f, position?.second ?: 0f, settings.limitPercent, boostHeld, settings.calibration)
            } else {
                fun axis(positive: ControlButton, negative: ControlButton) =
                    (if (positive in valid.values) 1f else 0f) - (if (negative in valid.values) 1f else 0f)
                DriveMixer.mix(axis(ControlButton.RIGHT, ControlButton.LEFT), axis(ControlButton.FORWARD, ControlButton.BACKWARD),
                    settings.limitPercent, boostHeld, settings.calibration, InputMode.D_PAD)
            }
        } else WheelPwm(0, 0)
        return ControlState(pwm, boostHeld && pwm != WheelPwm(0, 0), weapon, brake, killed, connected, mode,
            pendingMode, turnId, turnDuration, turnActive, unlockPending)
    }

    fun connectAcknowledged(capabilities: Int = 3, autoApproved: Boolean = false, receiverKilled: Boolean = false) {
        require(capabilities and 1 != 0)
        cancelAll()
        caps = capabilities
        this.autoApproved = autoApproved
        killed = killed || receiverKilled
        nextTurnId = 0
        connected = true
    }
    fun disconnect() { cancelAll(); connected = false }

    fun press(pointerId: Int, button: ControlButton) {
        if (pointerId in pressed) return
        pressed[pointerId] = button
        unlockStart = null
        unlockPending = false
        if (!connected || killed) { blocked.add(pointerId); return }
        if (button == ControlButton.BRAKE) {
            invalidateExceptBrakes()
            return
        }
        if (state.brake || mode != DriveMode.MANUAL || pendingMode != null) { blocked.add(pointerId); return }
        if (turnId != 0) {
            if (button.direction()) { endTurn(); awaitRelease.add(pointerId) }
            blocked.add(pointerId)
            return
        }
        if (button.direction()) {
            val inputMatches = (button == ControlButton.JOYSTICK) == (settings.inputMode == InputMode.JOYSTICK)
            val otherJoystick = pressed.any { (id, value) -> id != pointerId && id !in blocked && value == ControlButton.JOYSTICK }
            if (!inputMatches || awaitRelease.isNotEmpty() || (button == ControlButton.JOYSTICK && otherJoystick)) {
                blocked.add(pointerId)
                return
            }
            if (button == ControlButton.JOYSTICK) joystick[pointerId] = 0f to 0f
        }
        if (button == ControlButton.WEAPON && settings.weaponMode == WeaponMode.TOGGLE) toggledWeapon = !toggledWeapon
    }

    fun moveJoystick(pointerId: Int, x: Float, y: Float) {
        if (pressed[pointerId] == ControlButton.JOYSTICK && pointerId !in blocked) joystick[pointerId] = x to y
    }
    private fun invalidateDirections() {
        for ((id, button) in pressed) if (button.direction() && id !in blocked) {
            blocked.add(id)
            awaitRelease.add(id)
        }
        endTurn()
    }
    fun release(pointerId: Int) {
        val button = pressed.remove(pointerId) ?: return
        val wasValid = !blocked.remove(pointerId)
        awaitRelease.remove(pointerId)
        joystick.remove(pointerId)
        if (wasValid && button.direction()) invalidateDirections()
        if (wasValid && button == ControlButton.BRAKE) invalidateExceptBrakes()
    }
    fun leaveControl(pointerId: Int) {
        if (pressed[pointerId]?.direction() == true && pointerId !in blocked) invalidateDirections()
    }
    fun tick(): ControlState = state

    private fun endTurn() { turnId = 0; turnDuration = 0; turnActive = false }
    private fun invalidateExceptBrakes() {
        for ((id, button) in pressed) if (button != ControlButton.BRAKE) { blocked.add(id); awaitRelease.add(id) }
        toggledWeapon = false
        endTurn()
        pendingMode = null
        mode = DriveMode.MANUAL
        unlockPending = false
        unlockStart = null
    }
    fun cancelAll() {
        pressed.clear(); blocked.clear(); awaitRelease.clear(); joystick.clear()
        toggledWeapon = false
        endTurn()
        mode = DriveMode.MANUAL
        pendingMode = null
        unlockPending = false
        unlockStart = null
    }
    fun kill() {
        blocked.addAll(pressed.keys)
        awaitRelease.addAll(pressed.keys)
        toggledWeapon = false
        endTurn()
        killed = true
        mode = DriveMode.MANUAL
        pendingMode = null
        unlockStart = null
        unlockPending = false
    }
    fun beginUnlock(): Boolean {
        if (!connected || !killed || pressed.isNotEmpty() || unlockPending) return false
        unlockStart = nowMs()
        return true
    }
    fun unlock(): Boolean {
        val start = unlockStart ?: return false
        if (!connected || !killed || pressed.isNotEmpty() || unlockPending || nowMs() - start < 1000) return false
        unlockPending = true
        return true
    }
    fun endUnlockHold() { unlockStart = null }
    fun acknowledgeUnlock(accepted: Boolean) {
        if (!unlockPending || !connected || pressed.isNotEmpty()) return
        unlockPending = false
        unlockStart = null
        if (accepted) { killed = false; cancelAll() }
    }
    fun requestMode(mode: DriveMode): Boolean {
        if (!connected || killed || pressed.isNotEmpty() || pendingMode != null || turnId != 0 || state.weapon || this.mode == mode) return false
        if (mode == DriveMode.AUTO && (caps and 4 == 0 || !autoApproved)) return false
        pendingMode = mode
        return true
    }
    fun acknowledgeMode(mode: DriveMode, accepted: Boolean) {
        if (!connected || killed || pendingMode != mode) return
        pendingMode = null
        if (accepted) this.mode = mode
    }
    fun quickTurn(): Boolean {
        if (!connected || killed || mode != DriveMode.MANUAL || pendingMode != null || pressed.isNotEmpty() ||
            state.weapon || turnId != 0 || caps and 2 == 0 || nextTurnId == Int.MAX_VALUE) return false
        turnId = ++nextTurnId
        turnDuration = settings.turnDurationMs
        turnActive = false
        return true
    }
    fun acknowledgeTurn(id: Int, active: Boolean, accepted: Boolean = true) {
        if (id != turnId || turnId == 0) return
        // The receiver owns the timer; retransmitting the same ID must not restart it.
        if (!accepted || !active) endTurn() else turnActive = true
    }
    fun setLimiter(percent: Int) { settings = settings.copy(limitPercent = percent).validated() }
    fun applySettings(value: ControllerSettings) {
        check(!connected && pressed.isEmpty() && turnId == 0)
        settings = value.validated()
    }
}
