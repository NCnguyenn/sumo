package com.aistudy.myapplication

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudy.myapplication.settings.*
import com.aistudy.myapplication.ui.SettingsDialog
import com.aistudy.myapplication.updates.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUpdateTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun A05_changedFeedRejectsPreviouslyPreparedInstallerMetadata() {
        var result:UpdateStatus?=null
        val manager=AppUpdateManager(rule.activity){result=it}
        val before=rule.activity.packageManager.packageInstaller.mySessions.map{it.sessionId}
        try {
            val info=UpdateInfo(1,rule.activity.packageName,2,"test2",36,"https://source-a.invalid/2.apk",1,"a".repeat(64),"fixture","test")
            AppUpdateManager::class.java.getDeclaredField("state").apply{isAccessible=true}.set(manager,
                UpdateStatus(info=info,ready=true,feed="https://source-a.invalid/metadata.json"))
            manager.install("https://source-b.invalid/metadata.json")
            rule.waitUntil(3000){result?.busy==false}
            assertTrue("Must reject the source change before touching an APK or Android installer: ${result?.message}",
                result?.message?.contains("nguồn",ignoreCase=true)==true)
            assertEquals(before,rule.activity.packageManager.packageInstaller.mySessions.map{it.sessionId})
            result=null
            AppUpdateManager::class.java.getDeclaredField("state").apply{isAccessible=true}.set(manager,
                UpdateStatus(info=info,ready=true,feed="https://source-a.invalid/metadata.json"))
            manager.download("https://source-b.invalid/metadata.json")
            rule.waitUntil(3000){result?.busy==false}
            assertTrue("Source mismatch must fail before any HTTPS request",result?.message?.contains("nguồn",ignoreCase=true)==true)
        }finally{manager.close()}
    }
    @Test fun U10_settingsShowsConnectionFieldsSideBySideAndVisibleSave() {
        rule.onNodeWithTag("settings").performClick()
        val host=rule.onNodeWithTag("field-host").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val port=rule.onNodeWithTag("field-port").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(host.center.y,port.center.y,rule.activity.resources.displayMetrics.density)
        assertTrue(host.right<=port.left)
        rule.onNodeWithTag("save-settings").assertIsDisplayed()
    }
    @Test fun F01_F08_liveLimiterPersists30_60_100AcrossActivityRecreation() {
        val prefs=rule.activity.getSharedPreferences("sumo_settings",Context.MODE_PRIVATE)
        val original=prefs.getString("record",null)
        try {
            for(limit in listOf(30,60,100)) {
                rule.onNodeWithTag("limiter").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress){it(limit.toFloat())}
                rule.waitUntil(2000){ControllerSettingsStore(rule.activity).load().getOrThrow().controller.limitPercent==limit}
                rule.activityRule.scenario.recreate()
                rule.waitUntil(2000){rule.onNodeWithTag("limiter").fetchSemanticsNode().config[
                    androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo].current==limit.toFloat()}
            }
        }finally{prefs.edit().putString("record",original).commit()}
    }
    @Test fun F06_F10_storeMigrationAndCorruptionKeepOriginalRecord() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val prefs=context.getSharedPreferences("sumo_settings",Context.MODE_PRIVATE)
        val original=prefs.getString("record",null);val backup=prefs.getString("migration_backup",null)
        try {
            val value=StoredSettings(ControllerSettings(limitPercent=45,turnDurationMs=350,sendIntervalMs=50,weaponMode=WeaponMode.TOGGLE))
            ControllerSettingsStore(context).save(value)
            assertEquals(value,ControllerSettingsStore(context).load().getOrThrow())
            prefs.edit().putString("record","schema=0\nlimit=40\nhost=10.0.2.2\nport=8888\n").commit()
            assertEquals(40,ControllerSettingsStore(context).load().getOrThrow().controller.limitPercent)
            assertNotNull(prefs.getString("migration_backup",null))
            prefs.edit().putString("record","schema=99\nsecret-user-setting=preserve").commit()
            assertTrue(ControllerSettingsStore(context).load().isFailure)
            assertEquals("schema=99\nsecret-user-setting=preserve",prefs.getString("record",null))
        } finally {prefs.edit().putString("record",original).putString("migration_backup",backup).commit()}
    }
    @Test fun U08_F05_invalidFieldCannotApplyPartialSettings() {
        var applied:StoredSettings?=null
        rule.activity.runOnUiThread{rule.activity.setContent{SettingsDialog(StoredSettings(),{applied=it},{})}}
        rule.onNodeWithTag("field-port").performScrollTo().performTextReplacement("65536")
        rule.onNodeWithTag("save-settings").performClick()
        rule.onNodeWithText("Nhập số nguyên 1–65535").assertExists()
        assertNull(applied)
        rule.onNodeWithTag("field-port").performScrollTo().performTextReplacement("8889")
        rule.onNodeWithTag("field-duration").performScrollTo().performTextReplacement("350")
        rule.onNodeWithTag("save-settings").performClick()
        rule.runOnIdle {assertEquals(8889,applied?.controller?.port);assertEquals(350,applied?.controller?.turnDurationMs)}
    }
    @Test fun A01_A02_A08_jsonTypesAndActualInstalledIdentityAreChecked() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val manager=AppUpdateManager(context){}
        try {
            val installed=manager.installed();assertEquals(context.packageName,installed.applicationId);assertTrue(installed.signers.isNotEmpty())
            val json="""{"schemaVersion":1,"applicationId":"${context.packageName}","versionCode":2,"versionName":"test2","minSdk":36,"apkUrl":"https://fixture.invalid/2.apk","sizeBytes":100,"sha256":"${"a".repeat(64)}","releaseNotes":"test","channel":"test"}"""
            assertEquals(2L,AppUpdateManager.parseMetadata(json).validate(installed).versionCode)
            assertThrows(IllegalArgumentException::class.java){AppUpdateManager.parseMetadata(json.replace("\"versionCode\":2","\"versionCode\":\"2\""))}
            assertThrows(Exception::class.java){AppUpdateManager.parseMetadata("{broken")}
            val file=java.io.File(requireNotNull(context.applicationInfo.sourceDir))
            val digest=java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use{input->val bytes=ByteArray(16384);while(true){val n=input.read(bytes);if(n<0)break;digest.update(bytes,0,n)}}
            val hash=digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)}
            val info=AppUpdateManager.parseMetadata(json).copy(sizeBytes=file.length(),sha256=hash)
            assertThrows(IllegalArgumentException::class.java){manager.verify(file,info)}
        } finally {manager.close()}
    }
    @Test fun A09_androidAbortAndFailureNeverClaimSuccessfulUpdate() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        InstallResultReceiver().onReceive(context,Intent().putExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE_ABORTED))
        assertTrue(InstallResultReceiver.result.value.contains("hủy"))
        InstallResultReceiver().onReceive(context,Intent().putExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE_STORAGE))
        assertTrue(InstallResultReceiver.result.value.contains("không cài được"))
        InstallResultReceiver.result.value=""
    }
    @Test fun A09_failureResultSurvivesProcessStateLossAndActivityRecreate() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val prefs=context.getSharedPreferences("sumo_install_result",0)
        val original=prefs.getString("message",null)
        try {
            InstallResultReceiver().onReceive(context,Intent().putExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE_STORAGE))
            InstallResultReceiver.result.value=""
            rule.activityRule.scenario.recreate()
            rule.onNodeWithTag("updates").performClick()
            rule.onNodeWithText("Android không cài được",substring=true).assertExists()
        } finally {prefs.edit().putString("message",original).commit();InstallResultReceiver.result.value=""}
    }
}
