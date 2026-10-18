package com.aistudy.myapplication

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudy.myapplication.settings.*
import org.junit.*
import org.junit.Assert.assertTrue

class CadenceTest {
    @get:Rule val rule=createEmptyComposeRule()
    @Test(timeout=210000) fun I01_F04_measure20_30_50msWithoutRebuilding() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val targets=InstrumentationRegistry.getArguments().getString("cadenceTargets") ?: "20,30,50"
        require(targets in setOf("50","20,30,50"))
        val prefs=context.getSharedPreferences("sumo_settings",0)
        val safety=context.getSharedPreferences("sumo_safety",0)
        val original=prefs.getString("record",null);val killed=safety.getBoolean("kill",false)
        fun text(tag:String)=rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].first().text
        try {
            if(Build.VERSION.SDK_INT>=37)InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName,"android.permission.ACCESS_LOCAL_NETWORK")
            for((interval,holdMs) in listOf(20 to 28000L,30 to 40000L,50 to 64000L)) {
                if(targets=="50" && interval!=50)continue
                prefs.edit().putString("record",SettingsDocument.encode(StoredSettings(ControllerSettings(sendIntervalMs=interval)))).commit()
                safety.edit().putBoolean("kill",false).commit()
                ActivityScenario.launch(MainActivity::class.java).use { scenario->
                    rule.onNodeWithTag("connect").performClick()
                    rule.waitUntil(5000){text("connection").startsWith("Online")}
                    val root=rule.onNodeWithTag("controller")
                    val forward=rule.onNodeWithTag("forward").fetchSemanticsNode().boundsInRoot.center-root.fetchSemanticsNode().boundsInRoot.topLeft
                    var activity:MainActivity?=null
                    scenario.onActivity{activity=it}
                    assertTrue(requireNotNull(activity).flushControlTrace())
                    performanceHeap("BEGIN CADENCE$interval")
                    Log.i("SumoSession","BEGIN CADENCE$interval")
                    root.performTouchInput{down(0,forward)}
                    rule.waitUntil(2000){text("receiver").contains("PWM 153 / 153")}
                    val due=SystemClock.uptimeMillis()+holdMs
                    rule.waitUntil(holdMs+2000){SystemClock.uptimeMillis()>=due}
                    rule.onNodeWithTag("connection").assertTextContains("Online",substring=true)
                    root.performTouchInput{up(0)}
                    rule.waitUntil(2000){text("receiver").contains("PWM 0 / 0")}
                    assertTrue("All diagnostic records must be retained",requireNotNull(activity).flushControlTrace())
                    Log.i("SumoSession","END CADENCE$interval")
                    performanceHeap("END CADENCE$interval")
                }
            }
        } finally {
            prefs.edit().putString("record",original).commit()
            safety.edit().putBoolean("kill",killed).commit()
        }
    }
}
