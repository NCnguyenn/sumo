package com.aistudy.myapplication

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.aistudy.myapplication.control.*
import com.aistudy.myapplication.settings.*
import com.aistudy.myapplication.ui.ControllerScreen

@RunWith(AndroidJUnit4::class)
class ControllerUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun U01_U10_landscapeSafetyLabelsAndNeutralStart() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,rule.activity.requestedOrientation)
        rule.onNodeWithTag("kill").assertExists()
        rule.onNodeWithTag("settings").assertExists()
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
        rule.onNodeWithTag("connection").assertTextContains("Offline",substring=true)
    }
    private fun harness(settings:ControllerSettings=ControllerSettings()):ControlReducer {
        val r=ControlReducer(settings); r.connectAcknowledged()
        val state=mutableStateOf(r.state)
        rule.activity.runOnUiThread { rule.activity.setContent {
            ControllerScreen(state.value,r.settings,"TEST reducer — không kết nối xe",{r.handle(it);state.value=r.state},{},{r.disconnect();state.value=r.state},{})
        } }
        rule.waitForIdle(); return r
    }
    private fun point(tag:String)=rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center-
        rule.onNodeWithTag("controller").fetchSemanticsNode().boundsInRoot.topLeft
    @Test fun U03_directionAndBoostBothReleaseOrdersNeverResumeOldGesture() {
        harness(); val f=point("forward"); val b=point("boost")
        val root=rule.onNodeWithTag("controller")
        root.performTouchInput { down(0,f);down(1,b) }
        rule.onNodeWithTag("output").assertTextContains("PWM 255 / 255",substring=true)
        rule.onNodeWithTag("forward").assertIsSelected()
        rule.onNodeWithTag("left").assertIsNotSelected()
        root.performTouchInput { up(1) }
        rule.onNodeWithTag("output").assertTextContains("PWM 153 / 153",substring=true)
        root.performTouchInput { down(2,b);up(0);moveBy(2,androidx.compose.ui.geometry.Offset(1f,0f)) }
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
        rule.onNodeWithTag("forward").assertIsNotSelected()
        root.performTouchInput { up(2);down(3,f) }
        rule.onNodeWithTag("output").assertTextContains("PWM 153 / 153",substring=true)
        root.performTouchInput { up(3) }
    }
    @Test fun U04_holdWeaponRemainsAfterDirectionUpAndCancelClearsAll() {
        harness(); val f=point("forward");val w=point("weapon");val root=rule.onNodeWithTag("controller")
        root.performTouchInput {down(0,f);down(1,w);up(0)}
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0 · B0 W1",substring=true)
        root.performTouchInput {cancel()}
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0 · B0 W0",substring=true)
    }
    @Test fun U02_U06_joystickMovesButKillCannotBeClearedByHeldFingers() {
        harness(ControllerSettings(inputMode=InputMode.JOYSTICK));val j=point("joystick");val k=point("kill")
        val root=rule.onNodeWithTag("controller")
        root.performTouchInput {down(0,j);moveBy(0,androidx.compose.ui.geometry.Offset(0f,-40f));down(1,k)}
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
        root.performTouchInput {up(1);moveBy(0,androidx.compose.ui.geometry.Offset(0f,-5f))}
        rule.onNodeWithTag("kill").assertContentDescriptionEquals("KILL · ĐÃ KHÓA")
        root.performTouchInput {up(0)}
    }
    @Test fun U04_U12_toggleBrakeAndLiveLimiterUseSameReducer() {
        harness(ControllerSettings(weaponMode=WeaponMode.TOGGLE))
        val f=point("forward");val w=point("weapon");val brake=point("brake");val root=rule.onNodeWithTag("controller")
        root.performTouchInput{down(0,f);down(1,w);up(1)}
        rule.onNodeWithTag("output").assertTextContains("PWM 153 / 153 · B0 W1",substring=true)
        rule.onNodeWithTag("limiter").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(30f)}
        rule.onNodeWithTag("output").assertTextContains("PWM 77 / 77",substring=true)
        root.performTouchInput{down(2,brake);up(2)}
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0 · B0 W0",substring=true)
        root.performTouchInput{moveBy(0,androidx.compose.ui.geometry.Offset(1f,0f));up(0)}
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
    }
    @Test fun U03_releaseOneDirectionInvalidatesOldAndEarlyPointers() {
        harness();val f=point("forward");val l=point("left");val root=rule.onNodeWithTag("controller")
        root.performTouchInput{down(0,f);down(1,l);up(1);down(2,l)}
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
        root.performTouchInput{up(0);moveBy(2,androidx.compose.ui.geometry.Offset(1f,0f))}
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
        root.performTouchInput{down(3,f)}
        rule.onNodeWithTag("output").assertTextContains("PWM 153 / 153",substring=true)
        root.performTouchInput{up(2);up(3)}
    }
    @Test fun U06_killSurvivesActivityRecreateWithoutRestoringMotion() {
        val prefs=rule.activity.getSharedPreferences("sumo_safety",android.content.Context.MODE_PRIVATE)
        val original=prefs.getBoolean("kill",false)
        try {
            rule.onNodeWithTag("kill").performClick()
            rule.onNodeWithTag("kill").assertContentDescriptionEquals("KILL · ĐÃ KHÓA")
            rule.activityRule.scenario.recreate()
            rule.onNodeWithTag("kill").assertContentDescriptionEquals("KILL · ĐÃ KHÓA")
            rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
        } finally {prefs.edit().putBoolean("kill",original).commit()}
    }
    @Test fun U10_touchTargetsHave48DpMinimumAtCurrentWindowSize() {
        val density=rule.activity.resources.displayMetrics.density
        for(tag in listOf("forward","backward","left","right","boost","weapon","turn","brake","kill")) {
            val box=rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("$tag too narrow: ${box.width/density} dp",box.width/density>=47.9f)
            org.junit.Assert.assertTrue("$tag too short: ${box.height/density} dp",box.height/density>=47.9f)
        }
    }
    @Test fun U10_dPadIsBalancedCrossWithSquareDirectionKeys() {
        fun box(tag:String)=rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val forward=box("forward");val backward=box("backward");val left=box("left");val right=box("right")
        val tolerance=rule.activity.resources.displayMetrics.density
        assertEquals("Forward must be square",forward.width,forward.height,tolerance)
        assertEquals("Left/right must share a row",left.center.y,right.center.y,tolerance)
        assertEquals("Forward/backward must share a column",forward.center.x,backward.center.x,tolerance)
        org.junit.Assert.assertTrue("Backward belongs below the side keys",backward.top>=left.bottom)
        org.junit.Assert.assertTrue("Forward belongs above the side keys",forward.bottom<=left.top)
    }
    @Test fun U06_U07_unlockRequiresFreshTouchAfterEveryOtherFingerIsUp() {
        var now=0L
        val reducer=ControlReducer(nowMs={now}).apply{connectAcknowledged()}
        val state=mutableStateOf(reducer.state)
        rule.activity.runOnUiThread{rule.activity.setContent{
            ControllerScreen(state.value,reducer.settings,"TEST reducer",{reducer.handle(it);state.value=reducer.state},{},{},{})
        }}
        val root=rule.onNodeWithTag("controller")
        val kill=point("kill")
        root.performTouchInput{down(0,kill)}
        val unlock=point("unlock")
        root.performTouchInput{down(1,unlock)}
        rule.runOnIdle{now=1000;org.junit.Assert.assertFalse("Held Kill must block unlock",reducer.unlock())}
        root.performTouchInput{up(0)}
        rule.runOnIdle{now=2000;org.junit.Assert.assertFalse("Early unlock touch must not arm later",reducer.unlock())}
        root.performTouchInput{up(1);down(2,unlock)}
        rule.runOnIdle{now=3000;org.junit.Assert.assertTrue("Fresh lone unlock touch may request ACK",reducer.unlock())}
        root.performTouchInput{up(2)}
        val turn=point("turn")
        root.performTouchInput{down(3,unlock);down(4,turn)}
        rule.runOnIdle{now=4000;org.junit.Assert.assertFalse("An extra finger cancels an unlock hold",reducer.unlock())}
        root.performTouchInput{up(3);up(4)}
    }
}
