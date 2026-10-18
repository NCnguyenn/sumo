package com.aistudy.myapplication

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import com.aistudy.myapplication.control.*
import com.aistudy.myapplication.settings.ControllerSettings
import com.aistudy.myapplication.diagnostics.DiagnosticSummary
import com.aistudy.myapplication.ui.ControllerScreen
import com.aistudy.myapplication.ui.theme.MyApplicationTheme

class CompactDiagnosticsTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun warningKeepsDriveGeometryAndBoostRemainsLargestAction() {
        val warning=mutableStateOf(DiagnosticSummary())
        val reducer=ControlReducer().apply{connectAcknowledged()}
        rule.activity.runOnUiThread{rule.activity.setContent{MyApplicationTheme{
            ControllerScreen(reducer.state,ControllerSettings(),"Online",{},{},{},{},diagnostics=warning.value)
        }}}
        fun box(tag:String)=rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val before=listOf("forward","left","right","backward","boost","weapon","turn","brake","kill").associateWith(::box)
        rule.runOnIdle{warning.value=warning.value.copy(warning="Mất phản hồi quá 300 ms — đã dừng điều khiển")}
        for((tag,position) in before)assertEquals(tag,position,box(tag))
        val boost=box("boost")
        for(tag in listOf("weapon","turn","brake")){val other=box(tag);assertTrue(boost.width*boost.height>other.width*other.height)}
    }
    @Test fun smallLandscapeWithLargeFontKeepsControlsAndTextInsideTheirBounds() {
        rule.activity.runOnUiThread{rule.activity.setContent{MyApplicationTheme{
            CompositionLocalProvider(LocalDensity provides Density(2f,1.5f)) {
                Box(Modifier.requiredSize(800.dp,360.dp)) {
                    ControllerScreen(ControlState(),ControllerSettings(),"Offline",{},{},{},{})
                }
            }
        }}}
        val screen=rule.onNodeWithTag("controller").fetchSemanticsNode().boundsInRoot
        for(tag in listOf("forward","backward","left","right","boost","weapon","turn","brake","kill","limiter","output","receiver","ack-age")) {
            val box=rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue("$tag outside controller: $box / $screen",box.left>=screen.left && box.right<=screen.right && box.top>=screen.top && box.bottom<=screen.bottom)
            if(tag in listOf("forward","backward","left","right","boost","weapon","turn","brake","kill")) {
                assertTrue("$tag must remain at least 48dp",box.width>=96f && box.height>=96f)
            }
        }
        rule.onAllNodes(hasAnyAncestor(hasTestTag("controller")) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true)
            .fetchSemanticsNodes().flatMap { node->
                val layouts=mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
                layouts.flatMap { layout-> (0 until layout.lineCount).filter { line->
                    layout.getLineRight(line)-layout.getLineLeft(line)>layout.size.width+1 || layout.getLineBottom(line)>layout.size.height+1 || layout.isLineEllipsized(line)
                }.map { "${layout.layoutInput.text.text}: line=$it width=${layout.getLineRight(it)-layout.getLineLeft(it)} bottom=${layout.getLineBottom(it)} / ${layout.size}" } }
            }.let { assertTrue("Clipped text: $it",it.isEmpty()) }
    }
    @Test fun smallLandscapeDiagnosticsKeepsValuesAndLogVisibleWithLargeFont() {
        rule.activity.runOnUiThread{rule.activity.setContent{MyApplicationTheme{
            CompositionLocalProvider(LocalDensity provides Density(2f,1.5f)) {
                Box(Modifier.requiredSize(800.dp,360.dp)) {
                    com.aistudy.myapplication.ui.DiagnosticsScreen(
                        com.aistudy.myapplication.diagnostics.DiagnosticSnapshot(DiagnosticSummary(),listOf(
                            com.aistudy.myapplication.diagnostics.DiagnosticEntry(0,com.aistudy.myapplication.diagnostics.DiagnosticGroup.APP,"Kiểm tra bố cục"))),
                        null,"App 1.0 (1) · API 37",0,"0 / 0",{},{},{})
                }
            }
        }}}
        val log=rule.onNodeWithTag("diagnostics-log").fetchSemanticsNode().boundsInRoot
        assertTrue("Log must retain room for a readable row",log.height>=96f)
        for(value in listOf("PWM 0 / 0","PWM chưa có","Chưa có","0")) {
            rule.onAllNodesWithText(value,useUnmergedTree=true).assertCountEquals(if(value=="0")3 else 1)
            rule.onAllNodesWithText(value,useUnmergedTree=true).fetchSemanticsNodes().forEach { node->
                val layouts=mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
                layouts.forEach { layout->assertTrue("Hidden diagnostic value: $value",layout.getLineBottom(0)<=layout.size.height+1) }
            }
        }
        rule.onNodeWithText(java.text.SimpleDateFormat("HH:mm:ss.SSS",java.util.Locale.ROOT).format(java.util.Date(0)),useUnmergedTree=true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult){action->
                val layouts=mutableListOf<TextLayoutResult>();action(layouts)
                assertEquals("Timestamp must not wrap",1,layouts.first().lineCount)
                assertTrue("Timestamp must fit",layouts.first().getLineRight(0)<=layouts.first().size.width+1)
            }
    }
    @Test fun largeFontUpdaterKeepsStatusReadableAndHistoryReachable() {
        val history="Lịch sử kiểm thử: Android báo lỗi cài đặt (mã 6)"
        rule.activity.runOnUiThread{rule.activity.setContent{MyApplicationTheme{
            CompositionLocalProvider(LocalDensity provides Density(2f,1.5f)) {
                com.aistudy.myapplication.ui.AppUpdateDialog("1.0 (1)","",com.aistudy.myapplication.updates.UpdateStatus(),history,{},{},{},{},{})
            }
        }}}
        val status=rule.onNodeWithTag("update-status")
        status.performScrollTo().assertIsDisplayed()
        val layouts=mutableListOf<TextLayoutResult>()
        status.performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
        val viewport=rule.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
        assertTrue("Updater must show at least one status line",viewport.height>=layouts.first().getLineBottom(0))
        rule.onNodeWithText(history).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Đóng").assertIsDisplayed()
    }
    @Test fun exportUsesAndroidPickerAndCancellationKeepsControlStopped() {
        rule.onNodeWithTag("diagnostics").performClick()
        rule.waitUntil(3000){rule.onAllNodesWithTag("diagnostics-screen").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("export-diagnostics").performClick()
        val automation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        rule.waitUntil(5000){automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui")==true}
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        if(automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui")==true)
            automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        rule.waitUntil(5000){automation.rootInActiveWindow?.packageName?.toString()==rule.activity.packageName}
        rule.waitUntil(5000){rule.onAllNodesWithTag("controller").fetchSemanticsNodes().isNotEmpty() || rule.onAllNodesWithTag("close-diagnostics").fetchSemanticsNodes().isNotEmpty()}
        if(rule.onAllNodesWithTag("close-diagnostics").fetchSemanticsNodes().isNotEmpty())rule.onNodeWithTag("close-diagnostics").performClick()
        rule.onNodeWithTag("connection").assertTextContains("Offline",substring=true)
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
    }
    @Test fun pendingExportSurvivesActivityRecreation() {
        val field=MainActivity::class.java.getDeclaredField("pendingExport").apply{isAccessible=true}
        val report="SUMO diagnostic\nApp test · PWM 0 / 0\n".repeat(500)
        rule.runOnIdle{field.set(rule.activity,report)}
        rule.activityRule.scenario.recreate()
        rule.runOnIdle{assertEquals(report,field.get(rule.activity))}
    }
    @Test fun diagnosticsEntryReturnsOfflineAndKeepsControlsInPlace() {
        val before=rule.onNodeWithTag("forward").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("diagnostics").assertIsDisplayed().performClick()
        rule.onNodeWithTag("diagnostics-screen").assertIsDisplayed()
        rule.onNodeWithText("ACK không chứng minh động cơ thực sự quay.").assertExists()
        rule.onNodeWithTag("copy-diagnostics").assertIsDisplayed()
        rule.onNodeWithTag("export-diagnostics").assertIsDisplayed()
        rule.onNodeWithTag("close-diagnostics").performClick()
        rule.onNodeWithTag("connection").assertTextContains("Offline",substring=true)
        rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
        assertEquals(before,rule.onNodeWithTag("forward").fetchSemanticsNode().boundsInRoot)
    }
    @Test fun settingsTabsKeepDraftUntilExplicitSaveOrCancel() {
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("field-host").assertIsDisplayed()
        rule.onNodeWithTag("field-template").performScrollTo().performTextReplacement("D:{L},{R},{B},{W},{K}\\n")
        rule.onNodeWithTag("settings-calibration").performClick()
        rule.onNodeWithTag("field-sensitivity").assertExists()
        rule.onNodeWithTag("settings-update").performClick()
        rule.onNodeWithTag("field-feed").assertIsDisplayed()
        rule.onNodeWithTag("settings-general").performClick()
        rule.onNodeWithTag("field-template").assertTextContains("D:{L},{R},{B},{W},{K}\\n")
        rule.onNodeWithText("Hủy").performClick()
        rule.onNodeWithTag("connection").assertTextContains("Offline",substring=true)
    }
}
