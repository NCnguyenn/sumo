package com.aistudy.myapplication

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import org.junit.Assert.*
import androidx.lifecycle.Lifecycle
import com.aistudy.myapplication.settings.*

@RunWith(AndroidJUnit4::class)
class AndroidSimulatorTest {
    @Test fun diagnosticsWhileHoldingClosesSessionAndCopiesRealReport() {
        online()
        val root=rule.onNodeWithTag("controller");val forward=point("forward");val details=point("diagnostics")
        root.performTouchInput{down(0,forward)};waitText("receiver","PWM 153 / 153")
        root.performTouchInput{down(1,details);up(1);up(0)}
        rule.onNodeWithTag("diagnostics-screen").assertIsDisplayed()
        rule.onNodeWithTag("diagnostics-iot").performClick()
        rule.waitUntil(3000){rule.onAllNodesWithTag("diagnostics-log").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("copy-diagnostics").performClick()
        rule.runOnIdle {
            val clip=rule.activity.getSystemService(android.content.ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString()
            assertTrue(clip.contains("SUMO diagnostic"));assertTrue(clip.contains("endpoint=10.0.2.2:8888"))
            assertTrue(clip.contains("ACK seq="));assertTrue(clip.contains("App yêu cầu PWM 0 / 0"))
        }
        rule.onNodeWithTag("close-diagnostics").performClick()
        waitText("connection","Offline");waitText("output","PWM 0 / 0 · B0 W0")
        online();waitText("receiver","PWM 0 / 0")
    }
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private var original:String?=null
    private var originalKill=false
    @Before fun preserve(){original=rule.activity.getSharedPreferences("sumo_settings",0).getString("record",null);originalKill=rule.activity.getSharedPreferences("sumo_safety",0).getBoolean("kill",false)}
    @After fun restore(){rule.activity.getSharedPreferences("sumo_settings",0).edit().putString("record",original).commit();rule.activity.getSharedPreferences("sumo_safety",0).edit().putBoolean("kill",originalKill).commit()}
    private fun text(tag:String)=rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString(" "){it.text}
    private fun waitText(tag:String,value:String){rule.waitUntil(3000){text(tag).contains(value)}}
    private fun online() {
        if(Build.VERSION.SDK_INT>=37)InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(rule.activity.packageName,"android.permission.ACCESS_LOCAL_NETWORK")
        rule.onNodeWithTag("connect").performClick()
        rule.waitUntil(5000){rule.onNodeWithTag("connection").fetchSemanticsNode().config[SemanticsProperties.Text].first().text.startsWith("Online")}
    }
    private fun point(tag:String)=rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center-rule.onNodeWithTag("controller").fetchSemanticsNode().boundsInRoot.topLeft
    @Test fun I01_I02_U03_androidUdpToIndependentPythonHasRealAckAndSafeRelease() {
        online()
        val root=rule.onNodeWithTag("controller");val f=point("forward");val b=point("boost")
        root.performTouchInput{down(0,f);down(1,b)}
        waitText("output","PWM 255 / 255");waitText("receiver","PWM 255 / 255")
        root.performTouchInput{up(1)}
        waitText("output","PWM 153 / 153");waitText("receiver","PWM 153 / 153")
        root.performTouchInput{down(2,b);up(0)}
        waitText("output","PWM 0 / 0");waitText("receiver","PWM 0 / 0")
        root.performTouchInput{up(2)}
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("save-settings").assertExists()
    }
    @Test fun I02_U04_U06_U07_weaponBrakeKillAndHeldUnlockUseReceiverAck() {
        online();val root=rule.onNodeWithTag("controller");val f=point("forward");val w=point("weapon");val b=point("boost");val brake=point("brake");val kill=point("kill")
        root.performTouchInput{down(0,f);down(1,w);up(0)}
        waitText("receiver","PWM 0 / 0 · W1")
        root.performTouchInput{up(1);down(2,f);down(3,b);down(4,brake)}
        waitText("output","PWM 0 / 0 · B0 W0 K1")
        root.performTouchInput{up(4);moveBy(2,androidx.compose.ui.geometry.Offset(1f,0f))}
        waitText("output","PWM 0 / 0 · B0 W0 K0")
        root.performTouchInput{up(2);up(3);down(5,f);down(6,b);down(7,kill)}
        rule.waitUntil(2000){rule.onNodeWithTag("kill").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].first().contains("ĐÃ KHÓA")}
        val unlock=point("unlock")
        root.performTouchInput{up(7);up(6);up(5);down(8,unlock)}
        rule.waitUntil(3000){!rule.onNodeWithTag("kill").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].first().contains("ĐÃ KHÓA")}
        root.performTouchInput{up(8)}
        waitText("output","PWM 0 / 0")
        assertFalse(rule.activity.getSharedPreferences("sumo_safety",0).getBoolean("kill",true))
    }
    @Test fun I09_F02_F04_F07_settingsStopsOldEndpointAndAppliesAliasJoystickMacro() {
        online();val root=rule.onNodeWithTag("controller");val f=point("forward");val settings=point("settings")
        val version=rule.activity.packageManager.getPackageInfo(rule.activity.packageName,0).longVersionCode
        root.performTouchInput{down(0,f)}
        for((limit,pwm) in listOf(30 to 77,60 to 153,100 to 255,60 to 153)) {
            rule.onNodeWithTag("limiter").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(limit.toFloat())}
            waitText("receiver","PWM $pwm / $pwm")
        }
        root.performTouchInput{down(1,settings);up(1);up(0)}
        rule.waitUntil(3000){rule.onAllNodesWithTag("save-settings").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("input-mode").performScrollTo().performClick()
        rule.onNodeWithTag("weapon-mode").performScrollTo().performClick()
        for((key,value) in listOf("duration" to "350","interval" to "50","port" to "8889","sensitivity" to "1.5","deadZone" to "0.2",
            "leftGain" to "0.7","rightGain" to "0.8","turnPercent" to "40","template" to "D:{L},{R},{B},{W},{K}\\n")) {
            rule.onNodeWithTag(if(key in listOf("sensitivity","deadZone","leftGain","rightGain","turnPercent"))"settings-calibration" else "settings-general").performClick()
            rule.onNodeWithTag("field-$key").performScrollTo().performTextReplacement(value)
        }
        rule.onNodeWithTag("save-settings").performClick()
        rule.waitUntil(3000){rule.onAllNodesWithTag("save-settings").fetchSemanticsNodes().isEmpty()}
        online();waitText("connection","8889")
        val j=point("joystick");val delta=rule.onNodeWithTag("joystick").fetchSemanticsNode().boundsInRoot.height*.35f
        root.performTouchInput{down(2,j);moveBy(2,androidx.compose.ui.geometry.Offset(0f,-delta))}
        waitText("receiver","PWM 100 / 115")
        root.performTouchInput{up(2)};waitText("receiver","PWM 0 / 0")
        rule.onNodeWithTag("turn").assertContentDescriptionEquals("QUAY NHANH\n350 ms").performClick()
        waitText("receiver","Xoay #")
        rule.waitUntil(2000){!text("receiver").contains("Xoay #")}
        val stored=ControllerSettingsStore(rule.activity).load().getOrThrow().controller
        assertEquals(350,stored.turnDurationMs);assertEquals(50,stored.sendIntervalMs);assertEquals(8889,stored.port)
        assertEquals(InputMode.JOYSTICK,stored.inputMode);assertEquals(WeaponMode.TOGGLE,stored.weaponMode)
        assertEquals(version,rule.activity.packageManager.getPackageInfo(rule.activity.packageName,0).longVersionCode)
    }
    @Test fun I08_U05_lifecycleClosesAndResumesOfflineWithoutHeldGestureReplay() {
        online();val root=rule.onNodeWithTag("controller");val f=point("forward")
        root.performTouchInput{down(0,f)};waitText("receiver","PWM 153 / 153")
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        waitText("connection","Offline");waitText("output","PWM 0 / 0")
        root.performTouchInput{moveBy(0,androidx.compose.ui.geometry.Offset(1f,0f));up(0)}
        online();waitText("output","PWM 0 / 0")
    }
}
