package com.aistudy.myapplication.control

import com.aistudy.myapplication.connection.*
import com.aistudy.myapplication.settings.ControllerSettings
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.*

class ControlRuntimeTest {
    private class Echo:RobotTransport {
        val sent=CopyOnWriteArrayList<String>();val receive=ConcurrentLinkedQueue<RobotDatagram>()
        var pollObserver:(()->Unit)?=null
        val motion=CountDownLatch(1);val unblock=CountDownLatch(1);val stopped=CountDownLatch(1)
        var seenMotion=false
        override fun open(host:String,port:Int){}
        override fun send(bytes:ByteArray){
            val raw=String(bytes);sent.add(raw)
            if(raw.startsWith("H1:"))receive.add(RobotDatagram("H1:1111111111111111,0123456789abcdef,3\n".replace("1111111111111111",raw.substringAfter(':').trim()).toByteArray(),"10.0.2.2",8888))
            else {
                val header=raw.substringBefore('|').substringAfter(':').split(',');val body=raw.substringAfter('|').trim()
                val p=if(body.startsWith("S:"))body.substringAfter(':').split(',') else listOf("0","0","0","0","0")
                if(p[0]!="0" || p[1]!="0"){seenMotion=true;motion.countDown();check(unblock.await(2,TimeUnit.SECONDS))}
                else if(seenMotion)stopped.countDown()
                val reply="A1:${header[0]},${header[1]},OK,${p.joinToString(",")},${header[2]},0,0,0\n"
                receive.add(RobotDatagram(reply.toByteArray(),"10.0.2.2",8888))
            }
        }
        override fun receive():RobotDatagram? {pollObserver?.invoke();return receive.poll()}
        override fun close(){}
    }
    @Test fun I03_busySenderNeverFlushesQueuedMovementAfterRelease() {
        val wire=Echo();val online=CountDownLatch(1)
        val runtime=ControlRuntime(ControllerSettings(),changed={if(it.control.connected)online.countDown()},transport=wire)
        try {
            runtime.connect();assertTrue(online.await(2,TimeUnit.SECONDS))
            runtime.post(ControlEvent.Press(0,ControlButton.FORWARD));assertTrue(wire.motion.await(2,TimeUnit.SECONDS))
            runtime.post(ControlEvent.Press(1,ControlButton.BOOST));runtime.post(ControlEvent.Release(0));wire.unblock.countDown()
            assertTrue(wire.stopped.await(2,TimeUnit.SECONDS))
            assertFalse(wire.sent.any{it.contains("|S:255,255")})
        } finally{wire.unblock.countDown();runtime.close()}
    }
    @Test fun I09_A13_stopCallbackRunsOnlyAfterCloseAckOrWatchdogAndIsOffUiThread() {
        val wire=Echo();val online=CountDownLatch(1);val stopped=CountDownLatch(1);var callbackThread=""
        val runtime=ControlRuntime(ControllerSettings(),changed={if(it.control.connected)online.countDown()},transport=wire)
        try{runtime.connect();assertTrue(online.await(2,TimeUnit.SECONDS));runtime.stop{callbackThread=Thread.currentThread().name;stopped.countDown()}
            assertTrue(stopped.await(2,TimeUnit.SECONDS));assertEquals("sumo-control",callbackThread);assertTrue(wire.sent.any{it.endsWith("|C\n")})
        }finally{runtime.close()}
    }
    @Test fun I02_slowDiagnosticSinkCannotBlockUrgentStop() {
        val wire=Echo();wire.unblock.countDown()
        val online=CountDownLatch(1);val logging=CountDownLatch(1);val releaseLogger=CountDownLatch(1)
        val runtime=ControlRuntime(ControllerSettings(),changed={if(it.control.connected)online.countDown()},
            trace={if(it.contains("event Release(")){logging.countDown();check(releaseLogger.await(2,TimeUnit.SECONDS))}},transport=wire)
        try {
            runtime.connect();assertTrue(online.await(2,TimeUnit.SECONDS))
            runtime.post(ControlEvent.Press(0,ControlButton.FORWARD));assertTrue(wire.motion.await(2,TimeUnit.SECONDS))
            runtime.post(ControlEvent.Release(0));assertTrue(logging.await(1,TimeUnit.SECONDS))
            assertTrue("STOP must reach transport while the diagnostic sink is still blocked",wire.stopped.await(300,TimeUnit.MILLISECONDS))
            releaseLogger.countDown();assertTrue("Trace queue must drain without losing records",runtime.flushTrace())
        } finally {releaseLogger.countDown();runtime.close()}
    }
    @Test fun I01_idlePollingDoesNotRebuildUnchangedSnapshots() {
        val memory=java.lang.management.ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        assertTrue(memory.isThreadAllocatedMemorySupported);memory.isThreadAllocatedMemoryEnabled=true
        val wire=Echo();val online=CountDownLatch(1);val measured=CountDownLatch(1)
        var polls=0;var emptyRun=0;var gaps=0;var lastSent=0;var lastBytes=0L;var allocated=0L
        wire.pollObserver={
            if(online.count==0L) {
                polls++
                @Suppress("DEPRECATION")
                val thread=Thread.currentThread().id
                val bytes=memory.getThreadAllocatedBytes(thread);val sent=wire.sent.size;val empty=wire.receive.isEmpty()
                // Exclude allocations from heartbeat encoding and the fake receiver itself.
                if(polls>200 && empty && emptyRun>=2 && sent==lastSent && gaps<1000) {
                    allocated+=bytes-lastBytes;gaps++
                    if(gaps==1000)measured.countDown()
                }
                emptyRun=if(empty && sent==lastSent)emptyRun+1 else 0
                lastBytes=bytes;lastSent=sent
            }
        }
        val runtime=ControlRuntime(ControllerSettings(),changed={if(it.control.connected)online.countDown()},transport=wire)
        try {
            runtime.connect();assertTrue(online.await(2,TimeUnit.SECONDS))
            val complete=measured.await(30,TimeUnit.SECONDS)
            println("runtime_idle_sampling polls=$polls gaps=$gaps sent=${wire.sent.size} complete=$complete")
            assertTrue("Measure 1000 idle receive polls with heartbeat ACKs still active; polls=$polls gaps=$gaps sent=${wire.sent.size}",complete)
            println("runtime_idle_bytes_per_1000_empty_poll_gaps=$allocated")
            assertTrue("Idle polling must not repeatedly allocate control/UI snapshots; actual=$allocated bytes",allocated<=256_000)
        } finally {runtime.close()}
    }
    @Test fun I02_I09_cachedPollingStillPublishesControlSettingsAndDisconnect() {
        val wire=Echo();wire.unblock.countDown()
        val online=CountDownLatch(1);val moving=CountDownLatch(1);val killed=CountDownLatch(1)
        val limiter=CountDownLatch(1);val offline=CountDownLatch(1)
        val runtime=ControlRuntime(ControllerSettings(),changed={
            if(it.control.connected)online.countDown() else if(online.count==0L)offline.countDown()
            if(it.control.pwm==WheelPwm(153,153))moving.countDown()
            if(it.control.killed)killed.countDown()
            if(it.settings.limitPercent==30)limiter.countDown()
        },transport=wire)
        try {
            runtime.connect();assertTrue(online.await(2,TimeUnit.SECONDS))
            runtime.post(ControlEvent.Press(0,ControlButton.FORWARD));assertTrue(moving.await(2,TimeUnit.SECONDS))
            runtime.post(ControlEvent.Kill);assertTrue(killed.await(2,TimeUnit.SECONDS))
            runtime.post(ControlEvent.Limiter(30));assertTrue(limiter.await(2,TimeUnit.SECONDS))
            runtime.stop();assertTrue(offline.await(2,TimeUnit.SECONDS))
        } finally {runtime.close()}
    }
}
