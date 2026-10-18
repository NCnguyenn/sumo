package com.aistudy.myapplication

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudy.myapplication.updates.*
import java.io.File
import java.security.MessageDigest
import org.junit.*
import android.os.ParcelFileDescriptor
import android.view.accessibility.AccessibilityNodeInfo

class InstallerPlatformTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun A07_A09_preparedAvdFixtureUsesRealPackageInstallerUserAction() {
        val candidate=InstrumentationRegistry.getArguments().getString("candidate")
        Assume.assumeNotNull("Manual AVD fixture APK path is required",candidate)
        rule.onNodeWithTag("updates").performClick()
        rule.onNodeWithTag("update-status").assertExists()
        val file=File(requireNotNull(candidate))
        val manager=AppUpdateManager(rule.activity){}
        var session:Int?=null
        try {
            rule.activity.packageManager.packageInstaller.mySessions.forEach{rule.activity.packageManager.packageInstaller.abandonSession(it.sessionId)}
            InstallResultReceiver.result.value=""
            val digest=MessageDigest.getInstance("SHA-256")
            file.inputStream().use{input->val bytes=ByteArray(16384);while(true){val n=input.read(bytes);if(n<0)break;digest.update(bytes,0,n)}}
            val info=UpdateInfo(1,rule.activity.packageName,2,"1.0",36,"https://fixture.invalid/avd-b.apk",file.length(),
                digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)},"AVD fixture only; not a hosted update","test")
            session=manager.requestInstall(file,info)
            android.util.Log.i("SumoInstallTest","REQUESTED_SESSION $session")
            rule.waitUntil(5000){InstallResultReceiver.result.value.contains("xác nhận")}
            val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
            rule.waitUntil(10000){automation.rootInActiveWindow?.packageName?.toString()?.contains("packageinstaller")==true}
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("screencap -p /sdcard/sumo-native-installer-prompt.png")).use{it.readBytes()}
            val window=requireNotNull(automation.rootInActiveWindow)
            val cancel=window.findAccessibilityNodeInfosByText("Cancel").firstOrNull{it.isClickable}
            Assert.assertNotNull("Real Android installer must offer Cancel",cancel)
            Assert.assertTrue(requireNotNull(cancel).performAction(AccessibilityNodeInfo.ACTION_CLICK))
            rule.waitUntil(5000){InstallResultReceiver.result.value.contains("hủy",ignoreCase=true)}
            Assert.assertEquals(1L,rule.activity.packageManager.getPackageInfo(rule.activity.packageName,0).longVersionCode)
            rule.onNodeWithTag("output").assertTextContains("PWM 0 / 0",substring=true)
        }finally{
            session?.let{id->if(rule.activity.packageManager.packageInstaller.mySessions.any{it.sessionId==id})rule.activity.packageManager.packageInstaller.abandonSession(id)}
            manager.close()
        }
    }
}
