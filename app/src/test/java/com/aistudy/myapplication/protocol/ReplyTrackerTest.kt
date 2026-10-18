package com.aistudy.myapplication.protocol

import com.aistudy.myapplication.control.WheelPwm
import com.aistudy.myapplication.control.DriveMode
import org.junit.Assert.*
import org.junit.Test

class ReplyTrackerTest {
    @Test fun C05_validatedWireBytesAreReturnedForSendingAndIdenticalRetry() {
        val t=confirmed();val frame=ControlFrame(sid,2,payload=Payload.Drive(WheelPwm(153,153)))
        val expected="V1:$sid,2,0,0,0,0|S:153,153,0,0,0\n".toByteArray()
        val first:Any=t.recordSent(frame,10)
        assertTrue("Do not discard already validated bytes and encode the same frame again",first is ByteArray)
        assertArrayEquals(expected,first as ByteArray)
        val retry:Any=t.recordSent(frame,11);assertArrayEquals(expected,retry as ByteArray)
        assertNotNull(receive(t,ack(2,"153,153,0,0,0,0,0,0,0"),12))
        assertNull(receive(t,ack(2,"153,153,0,0,0,0,0,0,0"),13))
    }
    @Test fun C05_invalidFrameStillCannotAdvanceSequenceOrProduceAnAck() {
        val t=confirmed()
        val invalid=listOf(ControlFrame(sid,0),ControlFrame(sid,2,kill=true,payload=Payload.Drive(WheelPwm(153,153))),
            ControlFrame(sid,2,turnId=1,durationMs=0),ControlFrame(sid,2,payload=Payload.Drive(boost=true)),
            ControlFrame(sid,2,payload=Payload.Drive(weapon=true,brake=true)))
        for(frame in invalid)assertThrows(IllegalArgumentException::class.java){t.recordSent(frame,10)}
        assertNull(receive(t,ack(2),11))
        t.recordSent(ControlFrame(sid,2),12);assertNotNull(receive(t,ack(2),13))
        assertThrows(IllegalArgumentException::class.java){t.recordSent(ControlFrame(sid,2),14)}
    }
    @Test fun C05_prunedWindowStillRejectsEvictedFutureWrongPeerAndReplayedAcks() {
        val t=confirmed()
        for(seq in 2..402)t.recordSent(ControlFrame(sid,seq),0)
        assertNull(receive(t,ack(146),0));assertNull(receive(t,ack(403),0))
        assertNotNull(receive(t,ack(147),0));assertNull(receive(t,ack(147),0))
        assertNull(receive(t,ack(402),0,from="127.0.0.2"))
        assertNotNull(receive(t,ack(402),0));assertNull(receive(t,ack(400),0))
    }
    private val sid="0123456789abcdef"
    private val nonce="1111111111111111"
    private val host="127.0.0.1"
    private fun tracker()=ReplyTracker(host,8888).also { it.begin(nonce,0) }
    private fun receive(t:ReplyTracker,s:String,at:Long=1,from:String=host,port:Int=8888)=t.accept(s.toByteArray(),from,port,at)
    private fun offer(t:ReplyTracker)=receive(t,"H1:$nonce,$sid,3\n",0)
    private fun ack(seq:Int,fields:String="0,0,0,0,0,0,0,0,0")="A1:$sid,$seq,OK,$fields\n"
    private fun confirmed():ReplyTracker = tracker().also { t -> offer(t); t.recordSent(ControlFrame(sid,1),0); receive(t,ack(1),1) }

    @Test fun C05_offerAndSendAreNotOnlineUntilNeutralConfirmationAck() {
        val t=tracker(); offer(t); assertFalse(t.online); assertEquals(sid,t.sid)
        t.recordSent(ControlFrame(sid,1),0); assertFalse(t.online)
        assertNotNull(receive(t,ack(1),5)); assertTrue(t.online); assertEquals(5L,t.lastRttMs)
        assertNull(receive(t,ack(1),6)); assertEquals(5L,t.lastRttMs)
    }
    @Test fun C05_badPeerNonceSidUnsentSeqAndMalformedNeverMakeOnline() {
        val t=tracker()
        assertNull(receive(t,"H1:$nonce,$sid,3\n",from="127.0.0.2"))
        assertNull(receive(t,"H1:2222222222222222,$sid,3\n"))
        offer(t); t.recordSent(ControlFrame(sid,1),0)
        for((s,ip,port) in listOf(Triple(ack(1),host,8889),Triple(ack(1),"127.0.0.2",8888),
            Triple(ack(1).replace(sid,"2222222222222222"),host,8888),Triple(ack(2),host,8888),Triple("bad\n",host,8888))) {
            assertNull(receive(t,s,1,ip,port)); assertFalse(t.online); assertNull(t.lastRttMs)
        }
    }
    @Test fun C05_movingConfirmationAndWrongInitialRequestCannotBeAccepted() {
        val t=tracker(); offer(t)
        assertThrows(IllegalArgumentException::class.java){t.recordSent(ControlFrame(sid,1,payload=Payload.Drive(WheelPwm(153,153))),0)}
        t.recordSent(ControlFrame(sid,1),0)
        assertNull(receive(t,ack(1,"153,153,0,0,0,0,0,0,0")))
        assertFalse(t.online)
    }
    @Test fun C05_timeoutRetiresSidAndLateRepliesCannotReconnect() {
        val t=confirmed(); t.recordSent(ControlFrame(sid,2),20)
        t.tick(300); assertTrue(t.online)
        t.tick(301); assertFalse(t.online); assertNull(t.sid); assertNull(t.lastRttMs)
        assertNull(receive(t,ack(2),302)); assertFalse(t.online)
    }
    @Test fun C05_handshakeDeadlineDoesNotExtendFromDuplicateOffers() {
        val t=tracker(); offer(t); receive(t,"H1:$nonce,$sid,3\n",999)
        t.tick(1000); assertNull(t.sid); assertFalse(t.online)
    }
    @Test fun C05_oldIntentAckAfterStopCannotConfirmCurrentState() {
        val t=confirmed()
        t.recordSent(ControlFrame(sid,2,payload=Payload.Drive(WheelPwm(153,153))),10)
        t.recordSent(ControlFrame(sid,3),11)
        assertNull(receive(t,ack(2,"153,153,0,0,0,0,0,0,0"),12))
        assertNotNull(receive(t,ack(3),13)); assertEquals(2L,t.lastRttMs)
        assertNull(receive(t,ack(2),14))
    }
    @Test fun C05_unchangedHeartbeatAckCanProgressWhileNewerTickIsInFlight() {
        val t=confirmed(); t.recordSent(ControlFrame(sid,2),10); t.recordSent(ControlFrame(sid,3),40)
        assertNotNull(receive(t,ack(2),50)); assertEquals(40L,t.lastRttMs)
    }
    @Test fun C05_changingDriveAcksKeepSessionAliveWithoutConfirmingSupersededState() {
        val t=confirmed()
        for (seq in 2..15) {
            val sentAt=10L+(seq-2)*30
            val pwm=(seq-1)*10
            t.recordSent(ControlFrame(sid,seq,payload=Payload.Drive(WheelPwm(pwm,pwm))),sentAt)
            if (seq>2) {
                val previousPwm=pwm-10
                assertNull(receive(t,ack(seq-1,"$previousPwm,$previousPwm,0,0,0,0,0,0,0"),sentAt+20))
                assertEquals("seq ${seq-1} must record 50 ms RTT",50L,t.lastRttMs)
                assertTrue(t.online); assertEquals(sid,t.sid)
                if (sentAt==280L) { t.tick(301); assertTrue(t.online) }
            }
        }
        assertNotNull(receive(t,ack(15,"140,140,0,0,0,0,0,0,0"),450))
        assertEquals(50L,t.lastRttMs)
        assertNull(receive(t,ack(15,"140,140,0,0,0,0,0,0,0"),600))
        t.tick(749); assertTrue(t.online)
        t.tick(750); assertFalse(t.online); assertNull(t.sid)
    }
    @Test fun C05_supersededMismatchedAckCannotMaintainProgress() {
        val t=confirmed()
        t.recordSent(ControlFrame(sid,2,payload=Payload.Drive(WheelPwm(10,10))),10)
        t.recordSent(ControlFrame(sid,3,payload=Payload.Drive(WheelPwm(20,20))),40)
        assertNull(receive(t,ack(2,"20,20,0,0,0,0,0,0,0"),60))
        assertNull(receive(t,ack(2,"10,10,0,0,0,0,0,0,0"),61,from="127.0.0.2"))
        assertNull(receive(t,ack(4,"10,10,0,0,0,0,0,0,0"),62))
        assertEquals(1L,t.lastRttMs)
        t.tick(301); assertFalse(t.online); assertNull(t.sid)
    }
    @Test fun C05_supersededSafetyAcksOnlyAdvanceCommunication() {
        val neutral="0,0,0,0,0,0,0,0,0"
        val killed="0,0,0,0,0,1,0,0,0"
        val cases=listOf(
            Triple(ControlFrame(sid,2,payload=Payload.Drive(WheelPwm(153,153))),"153,153,0,0,0,0,0,0,0",ControlFrame(sid,3)),
            Triple(ControlFrame(sid,2),neutral,ControlFrame(sid,3,kill=true)),
            Triple(ControlFrame(sid,2,kill=true),killed,ControlFrame(sid,3,payload=Payload.Unlock)),
            Triple(ControlFrame(sid,2,payload=Payload.Unlock),neutral,ControlFrame(sid,3,kill=true)),
            Triple(ControlFrame(sid,2,mode=DriveMode.AUTO),"0,0,0,0,0,0,1,0,0",ControlFrame(sid,3)),
            Triple(ControlFrame(sid,2,turnId=1,durationMs=200),"-255,255,0,0,0,0,0,1,1",ControlFrame(sid,3)),
            Triple(ControlFrame(sid,2,payload=Payload.Close),neutral,ControlFrame(sid,3))
        )
        for ((old,fields,current) in cases) {
            val t=confirmed(); t.recordSent(old,10); t.recordSent(current,40)
            assertNull("Obsolete $old must not reach reducer",receive(t,ack(2,fields),60))
            assertEquals(50L,t.lastRttMs); assertTrue(t.online); assertEquals(sid,t.sid)
            val currentFields=if(current.kill) killed else neutral
            assertNotNull(receive(t,ack(3,currentFields),61))
            assertEquals(21L,t.lastRttMs)
            assertNull(receive(t,ack(2,fields),62))
        }
    }
    @Test fun C05_configAckIsMatchedButDoesNotMaintainControlWatchdogOrPing() {
        val t=confirmed(); t.recordSent(ControlFrame(sid,2,payload=Payload.Configure(1,0,100)),100)
        assertNull(receive(t,"Q1:$sid,2,2,0,100,OK\n",101))
        assertNotNull(receive(t,"Q1:$sid,2,1,0,100,OK\n",102))
        assertEquals(1L,t.lastRttMs); t.tick(301); assertFalse(t.online)
    }
    @Test fun C05_closeAckAndNewBeginResetConnectionIdentity() {
        val t=confirmed(); t.recordSent(ControlFrame(sid,2,payload=Payload.Close),10)
        assertNotNull(receive(t,ack(2),11)); assertFalse(t.online); assertNull(t.sid)
        t.begin("2222222222222222",20)
        assertNull(receive(t,"H1:$nonce,$sid,3\n",21)); assertFalse(t.online)
    }
    @Test fun C05_okMustConfirmRequestedKillUnlockModeAndWheelState() {
        val t=confirmed()
        t.recordSent(ControlFrame(sid,2,kill=true),10)
        assertNull(receive(t,ack(2),11))
        assertNotNull(receive(t,ack(2,"0,0,0,0,0,1,0,0,0"),12))
        t.recordSent(ControlFrame(sid,3,payload=Payload.Unlock),13)
        assertNull(receive(t,ack(3,"0,0,0,0,0,1,0,0,0"),14))
        assertNotNull(receive(t,ack(3),15))
        t.recordSent(ControlFrame(sid,4,mode=com.aistudy.myapplication.control.DriveMode.AUTO),16)
        assertNull(receive(t,ack(4),17))
        assertNotNull(receive(t,ack(4,"0,0,0,0,0,0,1,0,0"),18))
        t.recordSent(ControlFrame(sid,5),19); assertNotNull(receive(t,ack(5),20))
        t.recordSent(ControlFrame(sid,6,payload=Payload.Drive(WheelPwm(153,153))),21)
        assertNull(receive(t,ack(6,"200,200,0,0,0,0,0,0,0"),22))
        assertNotNull(receive(t,ack(6,"153,153,0,0,0,0,0,0,0"),23))
    }
}
