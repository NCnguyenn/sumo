package com.aistudy.myapplication.control

import com.aistudy.myapplication.connection.*
import com.aistudy.myapplication.settings.ControllerSettings
import org.junit.Assert.*
import org.junit.Test

class ControlSchedulingTest {
    private var now=0L
    private val sid="0123456789abcdef"
    private val incoming=ArrayDeque<RobotDatagram>()
    private val sent=mutableListOf<Long>()
    private val sequences=mutableListOf<Int>()
    private var sendCost=0L
    private var extraNs=0L
    private val wire=object:RobotTransport {
        override fun open(host:String,port:Int){}
        override fun send(bytes:ByteArray){
            sent.add(now);now+=sendCost
            val raw=String(bytes)
            if(raw.startsWith("V1:"))sequences.add(raw.substringAfter(',').substringBefore(',').toInt())
        }
        override fun receive()=incoming.removeFirstOrNull()
        override fun close(){}
    }
    private fun online(interval:Int):ControlSession {
        val session=ControlSession(ControlReducer(ControllerSettings(sendIntervalMs=interval),{now}),wire,{now},{"1111111111111111"},
            nanoClock={now*1_000_000L+extraNs})
        fun reply(raw:String){incoming.add(RobotDatagram(raw.toByteArray(),"10.0.2.2",8888))}
        session.connect();reply("H1:1111111111111111,$sid,3\n");session.pump()
        reply("A1:$sid,1,OK,0,0,0,0,0,0,0,0,0\n");session.pump()
        assertEquals(SessionPhase.ONLINE,session.snapshot.phase);sent.clear();sequences.clear()
        return session
    }
    @Test fun I01_F04_lateWakeKeepsOriginalDeadlineInsteadOfAccumulatingDrift() {
        val s=online(30)
        now=33;s.pump();assertEquals(listOf(33L),sent)
        now=60;s.pump();assertEquals("Next slot stays at 60, not 63 ms",listOf(33L,60L),sent)
        now=90;s.pump();assertEquals(listOf(33L,60L,90L),sent)
    }
    @Test fun I01_F04_sendCrossingSlotsSkipsThemWithoutCatchupOnNextPump() {
        val s=online(30)
        sendCost=35;now=30;s.pump();assertEquals(65L,now)
        sendCost=0;s.pump();assertEquals("No immediate overdue send at 65 ms",listOf(30L),sent)
        now=89;s.pump();assertEquals(1,sent.size)
        now=90;s.pump();assertEquals(listOf(30L,90L),sent)
    }
    @Test fun I01_F04_late20msSlotDoesNotGenerateSub20msCatchupInterval() {
        val s=online(20)
        now=23;s.pump();now=40;s.pump()
        assertEquals("The 40 ms deadline must not send only 17 ms after the last send",listOf(23L),sent)
        now=43;s.pump();assertEquals(listOf(23L,43L),sent)
    }
    @Test fun I01_workerWaitUsesRemainingMonotonicNanoseconds() {
        val s=online(30)
        now=29;extraNs=750_000
        assertEquals(250_000L,s.waitNanos())
        s.pump();assertTrue(sent.isEmpty())
        extraNs=1_000_000;s.pump();assertEquals(listOf(29L),sent)
        assertEquals(2_000_000L,s.waitNanos())
    }
    @Test fun I02_I03_urgentStopKillBrakeBypassPeriodicFloorAndKeepSequence() {
        val s=online(50)
        s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump()
        now=1;s.event(ControlEvent.Release(0))
        now=2;s.event(ControlEvent.Press(1,ControlButton.BRAKE))
        now=3;s.event(ControlEvent.Kill)
        assertEquals(listOf(0L,1L,2L,3L),sent)
        assertEquals(listOf(2,3,4,5),sequences)
        s.event(ControlEvent.Press(2,ControlButton.FORWARD));s.pump()
        now=50;s.pump();assertEquals(WheelPwm(0,0),s.snapshot.control.pwm)
        assertTrue(s.snapshot.control.killed)
        assertTrue(sequences.zipWithNext().all{(a,b)->b==a+1})
    }
}
