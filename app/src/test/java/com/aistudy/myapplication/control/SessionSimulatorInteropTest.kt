package com.aistudy.myapplication.control

import com.aistudy.myapplication.connection.UdpRobotTransport
import com.aistudy.myapplication.settings.ControllerSettings
import java.io.File
import java.util.UUID
import java.util.concurrent.*
import org.junit.Assert.*
import org.junit.Test

class SessionSimulatorInteropTest {
    private val root=generateSequence(File(".").canonicalFile){it.parentFile}.first{File(it,"tools/sumo_simulator.py").exists()}
    private class Server(val process:Process,val port:Int,val log:File):AutoCloseable {
        override fun close(){process.destroy();if(!process.waitFor(2,TimeUnit.SECONDS))process.destroyForcibly()}
    }
    private fun server(name:String,delay:Int=150,target:String="rx",port:Int=0,tag:String=name):Server {
        val log=File(root,"docs/testing/phase-4/evidence/runtime-$tag-$delay-$target.jsonl")
        val process=ProcessBuilder("py","-3.13",File(root,"tools/sumo_simulator.py").path,"--host","127.0.0.1","--port","$port",
            "--scenario",name,"--delay-ms","$delay","--delay-target",target,"--log",log.path).redirectErrorStream(true).start()
        val reader=Executors.newSingleThreadExecutor()
        try {
            val ready=reader.submit(Callable{process.inputStream.bufferedReader().readLine()}).get(5,TimeUnit.SECONDS)
            check(ready?.startsWith("READY ")==true){"Python server failed: $ready"}
            return Server(process,ready.substringAfter(' ').toInt(),log)
        } catch(e:Exception){process.destroyForcibly();throw e} finally{reader.shutdownNow()}
    }
    private fun session(port:Int):ControlSession=ControlSession(ControlReducer(ControllerSettings(host="127.0.0.1",port=port,
        turnDurationMs=350,driveTemplate=ControllerSettings.D_TEMPLATE)),UdpRobotTransport(),{System.nanoTime()/1_000_000},
        {UUID.randomUUID().toString().replace("-","").take(16)})
    private fun await(s:ControlSession,description:String,timeout:Long=2500,predicate:()->Boolean) {
        val end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeout)
        while(!predicate() && System.nanoTime()<end){s.pump();Thread.yield()}
        assertTrue("$description: ${s.snapshot}",predicate())
    }
    @Test(timeout=45000) fun I01_I07_I08_pythonNormalAndDeterministicNetworkFaults() {
        val cases=listOf(Triple("normal",150,"rx"),Triple("drop-rx",150,"rx"),Triple("drop-reply",150,"rx"),
            Triple("delay",150,"rx"),Triple("delay",350,"rx"),Triple("delay",150,"reply"),
            Triple("duplicate",150,"rx"),Triple("reorder",150,"rx"),Triple("malformed",150,"rx"))
        for((scenario,delay,target) in cases)server(scenario,delay,target).use{server->
            val s=session(server.port)
            try {
                s.connect();await(s,"$scenario handshake"){s.snapshot.control.connected}
                s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump()
                await(s,"$scenario first motion ACK"){s.snapshot.applied?.pwm==WheelPwm(153,153)}
                s.event(ControlEvent.Press(1,ControlButton.BOOST));s.pump()
                val loses=scenario in listOf("drop-rx","drop-reply","malformed") || (scenario=="delay" && delay==350)
                if(loses){
                    await(s,"$scenario Offline"){s.snapshot.phase==SessionPhase.OFFLINE}
                    assertEquals(WheelPwm(0,0),s.snapshot.control.pwm);assertNull(s.snapshot.rttMs)
                    val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(1)
                    while(System.nanoTime()<until && !server.log.readText().contains(if(scenario=="drop-rx" || delay==350)"watchdog" else "closed"))Thread.yield()
                    val log=server.log.readText()
                    assertTrue("$scenario must stop at receiver",log.contains(if(scenario=="drop-rx" || delay==350)"watchdog" else "closed"))
                    assertFalse(s.snapshot.control.connected)
                }else{
                    await(s,"$scenario Boost ACK"){s.snapshot.applied?.pwm==WheelPwm(255,255)}
                    s.event(ControlEvent.Release(0));s.pump()
                    await(s,"$scenario STOP ACK"){s.snapshot.applied?.pwm==WheelPwm(0,0)}
                    s.event(ControlEvent.Release(1));s.pump()
                    if(scenario=="duplicate"){
                        s.event(ControlEvent.Turn);s.pump()
                        await(s,"duplicate macro start"){s.snapshot.control.turnActive}
                        await(s,"duplicate macro finish"){s.snapshot.control.turnId==0}
                        assertEquals(1,Regex("\"event\"\\s*:\\s*\"macro-start\"").findAll(server.log.readText()).count())
                        assertTrue(server.log.readText().contains("macro-complete"))
                    }
                    if(scenario=="reorder"){
                        val end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(150)
                        while(System.nanoTime()<end)s.pump()
                        assertEquals(WheelPwm(0,0),s.snapshot.control.pwm)
                        assertTrue(server.log.readText().contains("duplicate-or-old-seq"))
                    }
                    s.stop();await(s,"$scenario close gate"){s.safeToProceed()}
                }
                assertTrue(server.log.readText().contains("\"applied\""))
            } finally {s.close()}
        }
    }
    @Test(timeout=12000) fun I06_receiverRestartRequiresExplicitFreshHandshakeWithoutMotionReplay() {
        val first=server("normal",tag="restart-first")
        val s=session(first.port)
        var second:Server?=null
        try {
            s.connect();await(s,"initial Online"){s.snapshot.control.connected}
            s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump();await(s,"first motion"){s.snapshot.applied?.pwm==WheelPwm(153,153)}
            first.close();second=server("normal",port=first.port,tag="restart-second")
            await(s,"restart Offline"){s.snapshot.phase==SessionPhase.OFFLINE}
            assertEquals(WheelPwm(0,0),s.snapshot.control.pwm)
            await(s,"old deadline retired"){s.safeToProceed()}
            s.connect();await(s,"explicit reconnect Online"){s.snapshot.control.connected}
            assertEquals(WheelPwm(0,0),s.snapshot.control.pwm);assertEquals(WheelPwm(0,0),s.snapshot.applied?.pwm)
            assertTrue(second.log.readText().contains("wrong-peer-or-retired-sid"))
            s.stop();await(s,"final close"){s.safeToProceed()}
        } finally{s.close();second?.close();first.close()}
    }
}
