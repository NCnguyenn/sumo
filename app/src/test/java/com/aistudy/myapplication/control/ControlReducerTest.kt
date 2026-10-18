package com.aistudy.myapplication.control

import com.aistudy.myapplication.settings.*
import org.junit.Assert.*
import org.junit.Test

class ControlReducerTest {
    private var now = 0L
    private fun online(settings: ControllerSettings = ControllerSettings(), caps: Int = 3, auto: Boolean = false) =
        ControlReducer(settings) { now }.also { it.connectAcknowledged(caps, auto) }
    private fun stop(r: ControlReducer) {
        assertEquals(WheelPwm(0, 0), r.state.pwm)
        assertFalse(r.state.boost)
    }
    private fun forward(r: ControlReducer, id: Int = 0) = r.press(id, ControlButton.FORWARD)

    @Test fun offlineInputsCannotDriveAndConnectionAckStartsNeutral() {
        val r = ControlReducer()
        forward(r); r.press(1, ControlButton.BOOST); stop(r)
        assertFalse(r.state.connected)
        r.connectAcknowledged(); stop(r)
        r.moveJoystick(0, 0f, 1f); stop(r)
        r.release(0); r.release(1); forward(r, 2)
        assertEquals(WheelPwm(153, 153), r.state.pwm)
    }
    @Test fun L04_L07_L19_releasingOnlyBoostKeepsDirectionAcrossTicks() {
        val r = online(); forward(r); r.press(7, ControlButton.BOOST)
        assertEquals(WheelPwm(255, 255), r.state.pwm); assertTrue(r.state.boost)
        r.release(7)
        repeat(5) { r.tick(); assertEquals(WheelPwm(153, 153), r.state.pwm); assertFalse(r.state.boost) }
    }
    @Test fun L08_pointerIdsAndPressOrderAreIndependent() {
        val r = online(); r.press(99, ControlButton.BOOST); forward(r, 42)
        assertEquals(WheelPwm(255, 255), r.state.pwm)
        r.release(99); assertEquals(WheelPwm(153, 153), r.state.pwm)
        r.release(999); assertEquals(WheelPwm(153, 153), r.state.pwm)
    }
    @Test fun L05_L06_L20_boostCannotRestoreReleasedDirection() {
        val r = online(); r.press(1, ControlButton.BOOST); stop(r)
        forward(r); r.release(0)
        repeat(5) { r.tick(); r.moveJoystick(0, 0f, 1f); stop(r) }
        forward(r, 2)
        assertEquals(WheelPwm(255, 255), r.state.pwm); assertTrue(r.state.boost)
    }
    @Test fun L17_opposedReleaseInvalidatesOldForwardUntilAllUpAndFreshDown() {
        val r = online(); forward(r); r.press(8, ControlButton.BACKWARD); stop(r)
        r.release(8)
        repeat(5) { r.tick(); r.moveJoystick(0, 0f, 1f); forward(r); stop(r) }
        r.release(0); repeat(3) { r.tick(); stop(r) }
        forward(r, 3); assertEquals(WheelPwm(153, 153), r.state.pwm)
    }
    @Test fun L18_adjacentReleaseStopsInsteadOfReturningToForward() {
        val r = online(); forward(r); r.press(1, ControlButton.LEFT)
        assertEquals(WheelPwm(0, 153), r.state.pwm)
        r.release(1); repeat(5) { r.tick(); stop(r) }
        r.release(0); r.tick(); stop(r)
        forward(r, 9); assertEquals(WheelPwm(153, 153), r.state.pwm)
    }
    @Test fun L22_earlyDownNeverBecomesValidByMoveAfterOldFingersLift() {
        val r = online(); forward(r, 42); r.press(7, ControlButton.LEFT); r.release(7)
        forward(r, 99); stop(r)
        r.release(42)
        repeat(4) { r.moveJoystick(99, 0f, 1f); r.tick(); stop(r) }
        forward(r, 100); assertEquals(WheelPwm(153, 153), r.state.pwm)
        r.release(99); assertEquals(WheelPwm(153, 153), r.state.pwm)
    }
    @Test fun L22_leavingDpadRequiresPhysicalUpAndFreshDown() {
        val r = online(); forward(r); r.leaveControl(0)
        repeat(4) { forward(r); r.tick(); stop(r) }
        forward(r, 1); stop(r); r.release(0); r.tick(); stop(r)
        r.release(1); forward(r, 2); assertEquals(WheelPwm(153, 153), r.state.pwm)
    }
    @Test fun L09_L22_joystickReleaseOrLostCaptureCannotReviveByOldMove() {
        val r = online(ControllerSettings(inputMode = InputMode.JOYSTICK))
        r.press(4, ControlButton.JOYSTICK); r.moveJoystick(4, 0f, 1f)
        assertEquals(WheelPwm(153, 153), r.state.pwm)
        r.leaveControl(4); r.moveJoystick(4, 0f, 1f); r.tick(); stop(r)
        r.release(4); r.moveJoystick(4, 0f, 1f); stop(r)
        r.press(5, ControlButton.JOYSTICK); r.moveJoystick(5, 0f, 1f)
        assertEquals(WheelPwm(153, 153), r.state.pwm)
        r.release(5); repeat(3) { r.tick(); stop(r) }
    }
    @Test fun L11_L21_holdSurvivesDirectionReleaseAndEndsOnWeaponUp() {
        val r = online(); r.press(2, ControlButton.WEAPON); forward(r)
        r.release(0)
        repeat(4) { r.tick(); stop(r); assertTrue(r.state.weapon) }
        r.release(2); assertFalse(r.state.weapon)
    }
    @Test fun L11_L21_toggleChangesOnlyOnNewDownNotUpOrRepeatedDown() {
        val r = online(ControllerSettings(weaponMode = WeaponMode.TOGGLE))
        r.press(2, ControlButton.WEAPON); r.press(2, ControlButton.WEAPON); r.release(2)
        assertTrue(r.state.weapon); forward(r); r.release(0)
        repeat(4) { r.tick(); stop(r); assertTrue(r.state.weapon) }
        r.press(3, ControlButton.WEAPON); r.release(3); assertFalse(r.state.weapon)
    }
    @Test fun L12_brakeOverridesBoostWeaponAndRequiresOldFingersUp() {
        val r = online(); forward(r); r.press(1, ControlButton.BOOST); r.press(2, ControlButton.WEAPON)
        r.press(3, ControlButton.BRAKE); stop(r); assertTrue(r.state.brake); assertFalse(r.state.weapon)
        r.release(3); assertFalse(r.state.brake); stop(r)
        forward(r, 4); stop(r); r.release(0); forward(r, 5); stop(r)
        r.release(1); r.release(2); r.release(4); r.release(5); forward(r, 6)
        assertEquals(WheelPwm(153, 153), r.state.pwm)
    }
    @Test fun L13_L14_killNeedsAllFingersUpHoldAndMatchingUnlockAck() {
        val r = online(); forward(r); r.press(1, ControlButton.BOOST); r.kill()
        r.release(0); assertFalse(r.beginUnlock()); stop(r)
        r.release(1); assertTrue(r.beginUnlock())
        now=999; assertFalse(r.unlock()); now=1000; assertTrue(r.unlock())
        assertTrue(r.state.killed); forward(r, 2); stop(r); r.release(2)
        r.acknowledgeUnlock(false); assertTrue(r.state.killed)
        assertTrue(r.beginUnlock()); now=2000; assertTrue(r.unlock())
        r.acknowledgeUnlock(true); assertFalse(r.state.killed); stop(r)
        r.tick(); stop(r); forward(r, 4); assertEquals(WheelPwm(153, 153), r.state.pwm)
    }
    @Test fun L13_killAgainOrDisconnectInvalidatesPendingUnlockAck() {
        val r=online(); r.kill(); r.beginUnlock(); now=1000; assertTrue(r.unlock())
        r.kill(); r.acknowledgeUnlock(true); assertTrue(r.state.killed)
        r.beginUnlock(); now=2000; r.unlock(); r.disconnect(); r.acknowledgeUnlock(true)
        assertTrue(r.state.killed); stop(r)
    }
    @Test fun L13_earlyUnlockReleaseAndExplicitCancellationCannotUnlockLater() {
        val r=online();r.kill();r.handle(ControlEvent.BeginUnlock)
        now=999;r.handle(ControlEvent.EndUnlock);now=1000
        assertFalse(r.unlock());r.acknowledgeUnlock(true);assertTrue(r.state.killed)
        r.handle(ControlEvent.BeginUnlock);now=2000;assertTrue(r.unlock())
        r.handle(ControlEvent.CancelUnlock);r.acknowledgeUnlock(true)
        assertTrue(r.state.killed);assertFalse(r.state.unlockPending);stop(r)
    }
    @Test fun L14_reconnectNeverRestoresOldPointersOrUnlocksKill() {
        val r=online(); forward(r); r.disconnect(); r.connectAcknowledged()
        r.moveJoystick(0,0f,1f); r.tick(); stop(r)
        r.release(0); forward(r,5); assertEquals(WheelPwm(153,153),r.state.pwm)
        r.kill(); r.disconnect(); r.connectAcknowledged(); assertTrue(r.state.killed); stop(r)
    }
    @Test fun L15_cancelClearsToggleAndRequiresNewGesture() {
        val r=online(ControllerSettings(weaponMode=WeaponMode.TOGGLE)); r.press(4,ControlButton.WEAPON)
        forward(r); r.cancelAll(); r.moveJoystick(0,0f,1f); r.tick(); stop(r); assertFalse(r.state.weapon)
        r.release(0); forward(r,6); assertEquals(WheelPwm(153,153),r.state.pwm)
    }
    @Test fun L15_autoIsLockedWithoutCapabilityApprovalOrNeutrality() {
        assertFalse(online().requestMode(DriveMode.AUTO))
        assertFalse(online(caps=7).requestMode(DriveMode.AUTO))
        val r=online(caps=7,auto=true); forward(r); assertFalse(r.requestMode(DriveMode.AUTO))
        r.release(0); assertTrue(r.requestMode(DriveMode.AUTO))
        assertEquals(DriveMode.MANUAL,r.state.mode)
        r.acknowledgeMode(DriveMode.MANUAL,true); assertEquals(DriveMode.MANUAL,r.state.mode)
        r.acknowledgeMode(DriveMode.AUTO,false); assertEquals(DriveMode.MANUAL,r.state.mode)
        assertTrue(r.requestMode(DriveMode.AUTO)); r.acknowledgeMode(DriveMode.AUTO,true)
        assertEquals(DriveMode.AUTO,r.state.mode); forward(r,4); stop(r)
        r.release(4); assertTrue(r.requestMode(DriveMode.MANUAL)); forward(r,5); stop(r)
        r.acknowledgeMode(DriveMode.MANUAL,true); r.release(5); forward(r,6)
        assertEquals(WheelPwm(153,153),r.state.pwm)
    }
    @Test fun L15_brakeExitsAutoAndStaleModeAckDoesNotReenableIt() {
        val r=online(caps=7,auto=true); r.requestMode(DriveMode.AUTO); r.acknowledgeMode(DriveMode.AUTO,true)
        r.press(2,ControlButton.BRAKE); assertEquals(DriveMode.MANUAL,r.state.mode)
        r.acknowledgeMode(DriveMode.AUTO,true); assertEquals(DriveMode.MANUAL,r.state.mode); stop(r)
    }
    @Test fun L16_macroUsesNewIdAndNeedsAckButNeverResumesOldDirection() {
        val r=online(); assertTrue(r.quickTurn()); val id=r.state.turnId
        assertTrue(id>0); assertEquals(200,r.state.turnDurationMs); assertFalse(r.state.turnActive)
        assertFalse(r.quickTurn()); r.acknowledgeTurn(id+1,true); assertFalse(r.state.turnActive)
        r.acknowledgeTurn(id,true); assertTrue(r.state.turnActive)
        r.acknowledgeTurn(id,false); assertEquals(0,r.state.turnId); stop(r)
        r.tick(); stop(r); assertTrue(r.quickTurn()); assertTrue(r.state.turnId>id)
    }
    @Test fun L16_killBrakeCancelDisconnectAndDirectionCancelMacro() {
        for (action in listOf<(ControlReducer)->Unit>({it.kill()}, {it.press(8,ControlButton.BRAKE)},
            {it.cancelAll()}, {it.disconnect()}, {forward(it,6)})) {
            val r=online(); r.quickTurn(); val id=r.state.turnId; action(r)
            assertEquals(0,r.state.turnId); stop(r)
            r.acknowledgeTurn(id,true); r.tick(); assertEquals(0,r.state.turnId); stop(r)
        }
    }
    @Test fun L16_macroRequiresNeutralityAndCapability() {
        val r=online(caps=1); assertFalse(r.quickTurn())
        val driving=online(); forward(driving); assertFalse(driving.quickTurn())
        driving.release(0); driving.press(1,ControlButton.WEAPON); assertFalse(driving.quickTurn())
    }
    @Test fun newSessionResetsTurnIdSpaceAndKeepsConfiguredDuration() {
        val r=online(ControllerSettings(turnDurationMs=350)); r.quickTurn()
        assertEquals(350,r.state.turnDurationMs); r.acknowledgeTurn(r.state.turnId,false)
        r.quickTurn(); assertEquals(2,r.state.turnId)
        r.disconnect(); r.connectAcknowledged(); r.quickTurn(); assertEquals(1,r.state.turnId)
    }
    @Test fun F01_liveLimiterUsesSameReducerAndKeepsGesture() {
        val r=online(); forward(r)
        for ((limit,pwm) in listOf(30 to 77,60 to 153,100 to 255)) {
            r.setLimiter(limit); assertEquals(WheelPwm(pwm,pwm),r.state.pwm)
        }
        assertThrows(IllegalArgumentException::class.java){r.setLimiter(101)}
        assertEquals(WheelPwm(255,255),r.state.pwm)
    }
    @Test fun F03_advancedCalibrationRequiresStoppedSessionAndAffectsOutput() {
        val r=online(); forward(r)
        assertThrows(IllegalStateException::class.java){r.applySettings(r.settings.copy(calibration=DriveCalibration(leftGain=0.5f)))}
        r.disconnect(); r.applySettings(r.settings.copy(calibration=DriveCalibration(leftGain=0.5f)))
        r.connectAcknowledged(); forward(r,5); assertEquals(WheelPwm(77,153),r.state.pwm)
        r.disconnect(); val old=r.settings
        assertThrows(IllegalArgumentException::class.java){r.applySettings(old.copy(calibration=DriveCalibration(sensitivity=Float.NaN)))}
        assertEquals(old,r.settings)
    }
    @Test fun F03_dpadIgnoresJoystickSensitivityAndDeadZone() {
        val r=online(ControllerSettings(calibration=DriveCalibration(sensitivity=0.25f,deadZone=0.3f)))
        forward(r); assertEquals(WheelPwm(153,153),r.state.pwm)
    }
    @Test fun F03_dpadForwardRightUsesFullAxesWithLowTurnCalibration() {
        for (c in listOf(DriveCalibration(turnPercent=30), DriveCalibration(sensitivity=0.25f,deadZone=0.3f,turnPercent=30))) {
            val r=online(ControllerSettings(limitPercent=60,calibration=c))
            forward(r); assertEquals(WheelPwm(153,153),r.state.pwm)
            r.press(1,ControlButton.RIGHT)
            assertEquals(WheelPwm(153,82),r.state.pwm)
        }
    }
    @Test fun F03_dpadForwardLeftUsesFullAxesWithLowTurnCalibration() {
        for (c in listOf(DriveCalibration(turnPercent=30), DriveCalibration(sensitivity=0.25f,deadZone=0.3f,turnPercent=30))) {
            val r=online(ControllerSettings(limitPercent=60,calibration=c))
            forward(r); assertEquals(WheelPwm(153,153),r.state.pwm)
            r.press(1,ControlButton.LEFT)
            assertEquals(WheelPwm(82,153),r.state.pwm)
        }
    }
    @Test fun F03_joystickDiagonalsKeepRadialNormalizationWithLowTurnCalibration() {
        val r=online(ControllerSettings(inputMode=InputMode.JOYSTICK,limitPercent=60,calibration=DriveCalibration(turnPercent=30)))
        r.press(0,ControlButton.JOYSTICK)
        r.moveJoystick(0,1f,1f); assertEquals(WheelPwm(141,76),r.state.pwm)
        r.moveJoystick(0,-1f,1f); assertEquals(WheelPwm(76,141),r.state.pwm)
    }
}
