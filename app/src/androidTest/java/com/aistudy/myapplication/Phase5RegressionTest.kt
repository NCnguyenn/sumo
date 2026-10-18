package com.aistudy.myapplication

import android.os.Build
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudy.myapplication.settings.*
import org.junit.*
import org.junit.Assert.*

class Phase5RegressionTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var scenario: ActivityScenario<MainActivity>
    private var original: String? = null
    private var originalKill = false

    @Before fun preserve() {
        original = context.getSharedPreferences("sumo_settings", 0).getString("record", null)
        originalKill = context.getSharedPreferences("sumo_safety", 0).getBoolean("kill", false)
        if (Build.VERSION.SDK_INT >= 37) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, "android.permission.ACCESS_LOCAL_NETWORK")
    }
    @After fun restore() {
        if (::scenario.isInitialized) scenario.close()
        context.getSharedPreferences("sumo_settings", 0).edit().putString("record", original).commit()
        context.getSharedPreferences("sumo_safety", 0).edit().putBoolean("kill", originalKill).commit()
    }
    private fun launch(settings: ControllerSettings = ControllerSettings()) {
        context.getSharedPreferences("sumo_settings", 0).edit()
            .putString("record", SettingsDocument.encode(StoredSettings(settings))).commit()
        context.getSharedPreferences("sumo_safety", 0).edit().putBoolean("kill", false).commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntil(5000) { Thread.getAllStackTraces().keys.count { it.isAlive && it.name == "sumo-control" } == 1 }
    }
    private fun text(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString(" ") { it.text }
    private fun waitText(tag: String, value: String) = rule.waitUntil(5000) { text(tag).contains(value) }
    private fun point(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center -
        rule.onNodeWithTag("controller").fetchSemanticsNode().boundsInRoot.topLeft
    private fun online() {
        rule.onNodeWithTag("connect").performClick()
        rule.waitUntil(5000) { text("connection").startsWith("Online") }
    }

    @Test fun E01_E02_dPadFourDirectionsToggleAndThirdFingerKillReachReceiver() {
        launch(ControllerSettings(weaponMode = WeaponMode.TOGGLE)); online()
        val root = rule.onNodeWithTag("controller")
        for ((tag, pwm) in listOf("forward" to "153 / 153", "backward" to "-153 / -153",
            "left" to "-153 / 153", "right" to "153 / -153")) {
            val p = point(tag)
            root.performTouchInput { down(0, p) }; waitText("receiver", "PWM $pwm")
            root.performTouchInput { up(0) }; waitText("receiver", "PWM 0 / 0")
        }
        val f = point("forward"); val w = point("weapon"); val boost = point("boost"); val kill = point("kill")
        Log.i("SumoPhase5", "BEGIN HAPTIC_WEAPON_ONCE")
        root.performTouchInput { down(0, f); down(1, w) }; waitText("receiver", "PWM 153 / 153 · W1")
        root.performTouchInput { up(1); up(0) }; waitText("receiver", "PWM 0 / 0 · W1")
        Log.i("SumoPhase5", "END HAPTIC_WEAPON_ONCE")
        root.performTouchInput { down(2, f); down(3, boost) }; waitText("receiver", "PWM 255 / 255 · W1")
        root.performTouchInput { down(4, kill) }; waitText("receiver", "PWM 0 / 0 · W0")
        root.performTouchInput { up(4); moveBy(2, Offset(1f, 0f)) }
        rule.onNodeWithTag("kill").assertContentDescriptionEquals("KILL · ĐÃ KHÓA")
        waitText("output", "PWM 0 / 0 · B0 W0")
        root.performTouchInput { up(2); up(3) }
        val unlock = point("unlock")
        root.performTouchInput { down(5, unlock) }
        rule.waitUntil(5000) { !rule.onNodeWithTag("kill").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].first().contains("ĐÃ KHÓA") }
        root.performTouchInput { up(5) }
        waitText("receiver", "PWM 0 / 0 · W0")
    }

    @Test fun E01_joystickDeadZoneBoostAndMacroBrakeUseReceiver() {
        launch(ControllerSettings(inputMode = InputMode.JOYSTICK, turnDurationMs = 350,
            calibration = DriveCalibration(deadZone = .1f))); online()
        val root = rule.onNodeWithTag("controller"); val j = point("joystick"); val b = point("boost")
        val radius = rule.onNodeWithTag("joystick").fetchSemanticsNode().boundsInRoot.height / 2
        root.performTouchInput { down(0, j); down(1, b); moveBy(0, Offset(0f, -radius * .05f)) }
        waitText("output", "PWM 0 / 0 · B0"); waitText("receiver", "PWM 0 / 0")
        root.performTouchInput { moveTo(0, j + Offset(0f, -radius * .8f)) }
        waitText("receiver", "PWM 255 / 255")
        root.performTouchInput { up(1) }
        waitText("receiver", "PWM 119 / 119")
        root.performTouchInput { up(0) }; waitText("receiver", "PWM 0 / 0")
        rule.onNodeWithTag("turn").performClick(); waitText("receiver", "Xoay #")
        val brake = point("brake")
        root.performTouchInput { down(2, brake) }; waitText("output", "PWM 0 / 0 · B0 W0 K1")
        root.performTouchInput { up(2) }; waitText("receiver", "PWM 0 / 0")
        rule.onNodeWithText("AUTO khóa").assertExists()
    }

    @Test fun E03_A13_updateEntryWhileHoldingAndDuringMacroClosesSession() {
        launch(); online()
        val root = rule.onNodeWithTag("controller"); val f = point("forward"); val b = point("boost")
        val update = point("updates")
        root.performTouchInput { down(0, f); down(1, b) }; waitText("receiver", "PWM 255 / 255")
        root.performTouchInput { down(2, update); up(2); up(0); up(1) }
        rule.onNodeWithTag("update-status").assertExists()
        waitText("connection", "Offline"); waitText("output", "PWM 0 / 0 · B0 W0")
        rule.onNodeWithText("Đóng").performClick()
        online(); rule.onNodeWithTag("turn").performClick(); waitText("receiver", "Xoay #")
        rule.onNodeWithTag("updates").performClick()
        rule.onNodeWithTag("update-status").assertExists()
        waitText("connection", "Offline"); waitText("output", "PWM 0 / 0")
        rule.onNodeWithText("Đóng").performClick()
        scenario.recreate(); waitText("connection", "Offline"); waitText("output", "PWM 0 / 0")
    }

    @Test fun E02_reconnectWithHeldPointersNeedsFreshDirection() {
        launch(); online()
        val root = rule.onNodeWithTag("controller")
        val f = point("forward"); val b = point("boost"); val connect = point("connect")
        repeat(2) {
            root.performTouchInput { down(0, f); down(1, b) }; waitText("receiver", "PWM 255 / 255")
            root.performTouchInput { down(9, connect); up(9) }
            waitText("receiver", "PWM 0 / 0"); waitText("connection", "Online")
            root.performTouchInput { moveBy(0, Offset(1f, 0f)) }
            waitText("output", "PWM 0 / 0 · B0 W0")
            root.performTouchInput { up(0); up(1) }
        }
        root.performTouchInput { down(2, f) }; waitText("receiver", "PWM 153 / 153")
        root.performTouchInput { up(2) }; waitText("receiver", "PWM 0 / 0")
    }

    @Test fun F08_cancelAndResetConfirmationPreserveOrReplaceWholeRecord() {
        val custom = ControllerSettings(limitPercent = 45, port = 8889, turnDurationMs = 350)
        launch(custom)
        val prefs = context.getSharedPreferences("sumo_settings", 0)
        val before = prefs.getString("record", null)
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("field-port").performTextReplacement("9999")
        rule.onNodeWithText("Hủy").performClick()
        assertEquals(before, prefs.getString("record", null))
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("reset-settings").performClick()
        assertEquals(before, prefs.getString("record", null))
        rule.onAllNodesWithText("Hủy").onLast().performClick()
        assertEquals(before, prefs.getString("record", null))
        rule.onNodeWithTag("reset-settings").performClick()
        rule.onNodeWithText("Xác nhận").performClick()
        rule.waitUntil(5000) { ControllerSettingsStore(context).load().getOrThrow() == StoredSettings() }
        waitText("connection", "Offline"); waitText("output", "PWM 0 / 0")
    }
}
