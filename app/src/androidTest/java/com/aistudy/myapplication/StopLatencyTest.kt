package com.aistudy.myapplication

import android.os.Build
import android.os.Debug
import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aistudy.myapplication.settings.*
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StopLatencyTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private fun text(tag:String)=rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].first().text
    @Test(timeout=360000) fun I02_stop500RealTouchCallbacksAndReceiverAcknowledgements() {
        val prefs=rule.activity.getSharedPreferences("sumo_settings",0)
        val safety=rule.activity.getSharedPreferences("sumo_safety",0)
        val original=prefs.getString("record",null);val killed=safety.getBoolean("kill",false)
        try {
        prefs.edit().putString("record",SettingsDocument.encode(StoredSettings())).commit()
        safety.edit().putBoolean("kill",false).commit()
        rule.activityRule.scenario.recreate()
        if(Build.VERSION.SDK_INT>=37)InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(rule.activity.packageName,"android.permission.ACCESS_LOCAL_NETWORK")
        rule.onNodeWithTag("connect").performClick()
        rule.waitUntil(4000){text("connection").startsWith("Online")}
        val root=rule.onNodeWithTag("controller")
        val forward=rule.onNodeWithTag("forward").fetchSemanticsNode().boundsInRoot.center-root.fetchSemanticsNode().boundsInRoot.topLeft
        assertTrue(rule.activity.flushControlTrace())
        performanceHeap("BEGIN STOP500")
        Log.i("SumoSession","BEGIN STOP500")
        repeat(500) {
            root.performTouchInput{down(0,forward)}
            rule.waitUntil(2000){text("receiver").contains("PWM 153 / 153")}
            root.performTouchInput{up(0)}
            rule.waitUntil(2000){text("receiver").contains("PWM 0 / 0")}
        }
        assertTrue("All diagnostic records must be retained",rule.activity.flushControlTrace())
        Log.i("SumoSession","END STOP500")
        performanceHeap("END STOP500")
        } finally {prefs.edit().putString("record",original).commit();safety.edit().putBoolean("kill",killed).commit()}
    }
}

internal fun performanceHeap(marker:String){
    Log.i("SumoSession","PERF_HEAP $marker ns=${System.nanoTime()} stats=${Debug.getRuntimeStats()}")
}
