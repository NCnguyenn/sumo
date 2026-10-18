package com.aistudy.myapplication

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudy.myapplication.settings.*
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidFaultTest {
    @get:Rule val rule=createEmptyComposeRule()
    private lateinit var scenario:ActivityScenario<MainActivity>
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private var original:String?=null
    private var originalKill=false
    @Before fun launchNeutral() {
        val prefs=context.getSharedPreferences("sumo_settings",0);original=prefs.getString("record",null)
        originalKill=context.getSharedPreferences("sumo_safety",0).getBoolean("kill",false)
        prefs.edit().putString("record",SettingsDocument.encode(StoredSettings())).commit()
        context.getSharedPreferences("sumo_safety",0).edit().putBoolean("kill",false).commit()
        if(Build.VERSION.SDK_INT>=37)InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName,"android.permission.ACCESS_LOCAL_NETWORK")
        scenario=ActivityScenario.launch(MainActivity::class.java)
    }
    @After fun restore() {
        scenario.close()
        context.getSharedPreferences("sumo_settings",0).edit().putString("record",original).commit()
        context.getSharedPreferences("sumo_safety",0).edit().putBoolean("kill",originalKill).commit()
    }
    private fun text(tag:String)=rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].first().text
    private fun point(tag:String)=rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center-rule.onNodeWithTag("controller").fetchSemanticsNode().boundsInRoot.topLeft
    @Test fun I05_I07_I08_faultedAndroidSessionNeverRestoresHeldDirection() {
        val name=InstrumentationRegistry.getArguments().getString("scenario","normal")
        val delay=InstrumentationRegistry.getArguments().getString("delay","150")
        rule.onNodeWithTag("connect").performClick()
        rule.waitUntil(4000){text("connection").startsWith("Online")}
        val root=rule.onNodeWithTag("controller");val f=point("forward");val b=point("boost")
        root.performTouchInput{down(0,f);down(1,b)}
        rule.waitUntil(2000){text("output").contains("PWM 255 / 255")}
        val loses=name in listOf("drop-rx","drop-reply","malformed") || (name=="delay" && delay=="350")
        if(loses) {
            rule.waitUntil(5000){text("connection").startsWith("Offline")}
            root.performTouchInput{moveBy(0,androidx.compose.ui.geometry.Offset(1f,0f))}
            rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
        } else {
            val due=android.os.SystemClock.uptimeMillis()+1200
            rule.waitUntil(2500){android.os.SystemClock.uptimeMillis()>=due}
            root.performTouchInput{up(0)}
            rule.waitUntil(3000){text("receiver").contains("PWM 0 / 0")}
            rule.onNodeWithTag("connection").assertTextContains("Online",substring=true)
            root.performTouchInput{up(1)}
            if(name=="duplicate") {
                rule.onNodeWithTag("turn").performClick()
                rule.waitUntil(1500){text("receiver").contains("Xoay #")}
                rule.waitUntil(2000){!text("receiver").contains("Xoay #")}
            }
            return
        }
        root.performTouchInput{up(0);up(1)}
    }
}
