package com.aistudy.myapplication

import android.os.Build
import android.os.SystemClock
import android.system.Os
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudy.myapplication.settings.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.File

class SoakTest {
    @get:Rule val rule = createEmptyComposeRule()
    private fun text(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].first().text
    private fun waitText(tag: String, value: String) = rule.waitUntil(5000) { text(tag).contains(value) }
    private fun point(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center -
        rule.onNodeWithTag("controller").fetchSemanticsNode().boundsInRoot.topLeft
    private fun resources(): Map<String, Any> {
        val threads = Thread.getAllStackTraces().keys.filter { it.isAlive }
        val sockets = File("/proc/self/fd").listFiles().orEmpty().mapNotNull {
            runCatching { Os.readlink(it.path) }.getOrNull()?.takeIf { link -> link.startsWith("socket:") }
        }
        return mapOf("worker" to threads.count { it.name == "sumo-control" }, "trace" to threads.count { it.name == "sumo-trace" },
            "sockets" to sockets.size, "socket_fds" to sockets, "threads" to threads.size,
            "heap_bytes" to (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()))
    }

    @Test fun E06_evidenceDirectoryAndSocketProbeAreUsable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (Build.VERSION.SDK_INT >= 37) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, "android.permission.ACCESS_LOCAL_NETWORK")
        val directory = File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")))
        assertTrue(directory.isDirectory)
        val file = File(directory, "probe.txt"); file.writeText("Phase5 app UID output")
        assertEquals("Phase5 app UID output", file.readText())
        val before = resources()["sockets"] as Int
        java.net.DatagramSocket().use { assertEquals(before + 1, resources()["sockets"]) }
        assertEquals(before, resources()["sockets"])
    }

    @Test(timeout = 2100000) fun E05_thirtyMinutes600PressReleaseAnd20Reconnects() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("sumo_settings", 0); val safety = context.getSharedPreferences("sumo_safety", 0)
        val original = prefs.getString("record", null); val killed = safety.getBoolean("kill", false)
        var cycles = 0; var reconnects = 0; var kills = 0; var complete = false
        val start = SystemClock.elapsedRealtime()
        val directory = File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")))
        check(directory.isDirectory || directory.mkdirs())
        val output = File(directory, "soak.jsonl")
        fun record(event: String, extra: Map<String, Any> = emptyMap()) {
            val row = JSONObject(mapOf("event" to event, "elapsed_ms" to (SystemClock.elapsedRealtime() - start),
                "cycles" to cycles, "reconnects" to reconnects, "kills" to kills) + extra)
            output.appendText("$row\n"); Log.i("SumoPhase5", row.toString())
        }
        try {
            prefs.edit().putString("record", SettingsDocument.encode(StoredSettings())).commit()
            safety.edit().putBoolean("kill", false).commit()
            if (Build.VERSION.SDK_INT >= 37) InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, "android.permission.ACCESS_LOCAL_NETWORK")
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                rule.onNodeWithTag("connect").performClick(); waitText("connection", "Online")
                rule.waitUntil(5000) { resources()["trace"] == 1 }
                val baseline = resources(); record("begin", baseline)
                assertEquals(1, baseline["worker"]); assertTrue((baseline["sockets"] as Int) >= 1)
                val root = rule.onNodeWithTag("controller")
                val f = point("forward"); val boost = point("boost"); val weapon = point("weapon"); val kill = point("kill")
                val connect = point("connect")
                val due = SystemClock.elapsedRealtime() + 1800000L
                for (cycle in 1..600) {
                    val withBoost = cycle % 5 == 0; val withWeapon = cycle % 7 == 0
                    root.performTouchInput { down(0, f); if (withBoost) down(1, boost); if (withWeapon) down(2, weapon) }
                    waitText("receiver", if (withBoost) "PWM 255 / 255" else "PWM 153 / 153")
                    if (cycle % 30 == 0) {
                        root.performTouchInput { down(9, connect); up(9) }
                        waitText("receiver", "PWM 0 / 0"); waitText("connection", "Online")
                        root.performTouchInput { moveBy(0, Offset(1f, 0f)) }
                        waitText("output", "PWM 0 / 0 · B0 W0")
                        reconnects++
                    }
                    if (cycle % 60 == 15) {
                        root.performTouchInput { down(3, kill) }; waitText("receiver", "PWM 0 / 0 · W0")
                        root.performTouchInput { up(3); moveBy(0, Offset(-1f, 0f)) }
                        rule.onNodeWithTag("kill").assertContentDescriptionEquals("KILL · ĐÃ KHÓA")
                        kills++
                    }
                    root.performTouchInput { up(0); if (withBoost) up(1); if (withWeapon) up(2) }
                    waitText("receiver", "PWM 0 / 0 · W0")
                    if (cycle % 60 == 15) {
                        val unlock = point("unlock")
                        root.performTouchInput { down(4, unlock) }
                        rule.waitUntil(5000) { !rule.onNodeWithTag("kill").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].first().contains("ĐÃ KHÓA") }
                        root.performTouchInput { up(4) }; waitText("output", "PWM 0 / 0 · B0 W0")
                    }
                    cycles = cycle
                    if (cycle % 30 == 0) {
                        val current = resources(); record("checkpoint", current)
                        assertEquals(1, current["worker"]); assertEquals(1, current["trace"])
                        assertTrue("Socket count grew: $baseline -> $current", (current["sockets"] as Int) <= baseline["sockets"] as Int)
                    }
                    val next = due - (600 - cycle) * 3000L
                    while (SystemClock.elapsedRealtime() < next) SystemClock.sleep(minOf(200L, next - SystemClock.elapsedRealtime()).coerceAtLeast(1L))
                }
                assertTrue(SystemClock.elapsedRealtime() >= due)
                waitText("output", "PWM 0 / 0 · B0 W0")
                var activity: MainActivity? = null; scenario.onActivity { activity = it }
                assertTrue(requireNotNull(activity).flushControlTrace())
                record("neutral_end", resources())
            }
            rule.waitUntil(5000) { resources()["worker"] == 0 && resources()["trace"] == 0 }
            assertTrue(cycles >= 500); assertTrue(reconnects >= 20)
            complete = true
        } finally {
            record("finish", resources() + mapOf("complete" to complete))
            prefs.edit().putString("record", original).commit(); safety.edit().putBoolean("kill", killed).commit()
        }
    }
}
