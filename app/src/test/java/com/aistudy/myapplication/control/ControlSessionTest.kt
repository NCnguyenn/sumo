package com.aistudy.myapplication.control

import com.aistudy.myapplication.connection.*
import com.aistudy.myapplication.settings.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ControlSessionTest {
    private var now=0L
    private var allowed=true
    private val sid="0123456789abcdef"
    private class Fake:RobotTransport {
        val sent=mutableListOf<String>();val incoming=ArrayDeque<RobotDatagram>();var opens=0;var closes=0;var fail=false
        override fun open(host:String,port:Int){opens++}
        override fun send(bytes:ByteArray){if(fail)throw IOException("fixture send failure");sent.add(String(bytes,Charsets.US_ASCII))}
        override fun receive()=incoming.removeFirstOrNull()
        override fun close(){closes++}
    }
    private val fake=Fake()
    private fun session(settings:ControllerSettings=ControllerSettings())=ControlSession(ControlReducer(settings,{now}),fake,{now},{"1111111111111111"},{allowed})
    private fun reply(raw:String,host:String="10.0.2.2",port:Int=8888){fake.incoming.add(RobotDatagram(raw.toByteArray(),host,port))}
    private fun ack(seq:Int,values:String="0,0,0,0,0,0,0,0,0",status:String="OK")=reply("A1:$sid,$seq,$status,$values\n")
    private fun online(s:ControlSession,caps:Int=3) {
        s.connect();assertFalse(s.snapshot.control.connected)
        assertEquals("H1:1111111111111111\n",fake.sent.last())
        reply("H1:1111111111111111,$sid,$caps\n");s.pump()
        assertEquals("V1:$sid,1,0,0,0,0|S:0,0,0,0,0\n",fake.sent.last())
        ack(1);s.pump();assertTrue(s.snapshot.control.connected)
    }
    @Test fun I01_F04_periodUsesConfigured20_30_50WithoutCatchupBurst() {
        for(interval in listOf(20,30,50)) {
            fake.sent.clear();now=0
            val s=session(ControllerSettings(sendIntervalMs=interval));online(s)
            val count=fake.sent.size
            now=interval-1L;s.pump();assertEquals(count,fake.sent.size)
            now=interval.toLong();s.pump();assertEquals(count+1,fake.sent.size)
            now=interval*3L;s.pump();assertEquals(count+2,fake.sent.size)
            s.stop();now+=300;s.pump()
        }
    }
    @Test fun I02_I03_latestStateOnlyAndImmediateStopKillWithoutTick() {
        val s=session();online(s)
        s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.event(ControlEvent.Press(1,ControlButton.BOOST));s.pump()
        assertTrue(fake.sent.last().endsWith("|S:255,255,1,0,0\n"))
        now=1;s.event(ControlEvent.Release(0));s.pump();assertTrue(fake.sent.last().endsWith("|S:0,0,0,0,0\n"))
        s.event(ControlEvent.Press(2,ControlButton.FORWARD));s.event(ControlEvent.Kill);s.pump()
        assertTrue(fake.sent.last().contains(",1,0,0,0|S:0,0,0,0,0"))
    }
    @Test fun I04_handshakeNeedsAckAndDeadlineCannotExtendForever() {
        val s=session();s.connect();now=249;s.pump();assertEquals(1,fake.sent.size)
        now=250;s.pump();assertEquals(2,fake.sent.size);assertFalse(s.snapshot.control.connected)
        now=1000;s.pump();assertEquals(SessionPhase.OFFLINE,s.snapshot.phase);assertNull(s.snapshot.rttMs)
    }
    @Test fun I05_I07_timeoutBadReplyAndLateOldSidCannotReviveInput() {
        val s=session();online(s);s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump()
        now=299;ack(2,"153,153,0,0,0,0,0,0,0");fake.incoming.clear();reply("A1:$sid,2,OK,153,153,0,0,0,0,0,0,0\n",port=9999);s.pump()
        now=300;s.pump();assertFalse(s.snapshot.control.connected);assertEquals(WheelPwm(0,0),s.snapshot.control.pwm)
        ack(2,"153,153,0,0,0,0,0,0,0");now=350;s.pump();assertFalse(s.snapshot.control.connected)
    }
    @Test fun I02_L17_L22_oldAndEarlyDirectionMovesStayZeroAtPeriodicTicks() {
        val s=session();online(s)
        s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.event(ControlEvent.Press(1,ControlButton.LEFT));s.pump()
        s.event(ControlEvent.Release(1));s.event(ControlEvent.Press(2,ControlButton.LEFT));s.pump()
        now=30;s.pump();assertTrue(fake.sent.last().endsWith("|S:0,0,0,0,0\n"))
        s.event(ControlEvent.Release(0));now=60;s.pump();assertTrue(fake.sent.last().endsWith("|S:0,0,0,0,0\n"))
        s.event(ControlEvent.Press(3,ControlButton.FORWARD));s.pump();assertTrue(fake.sent.last().endsWith("|S:153,153,0,0,0\n"))
    }
    @Test fun I09_A13_stopClosesSessionAndWaitsDeadlineBeforeSettingsOrUpdate() {
        val s=session();online(s);s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump()
        now=10;s.stop();assertEquals(SessionPhase.CLOSING,s.snapshot.phase);assertEquals(WheelPwm(0,0),s.snapshot.control.pwm)
        assertTrue(fake.sent.takeLast(2)[0].endsWith("|S:0,0,0,0,0\n"));assertTrue(fake.sent.last().endsWith("|C\n"))
        assertFalse(s.safeToProceed());now=309;s.pump();assertFalse(s.safeToProceed());now=310;s.pump();assertTrue(s.safeToProceed())
        s.applySettings(ControllerSettings(port=8889));assertEquals(8889,s.snapshot.settings.port)
    }
    @Test fun I09_closeAckAllowsEarlierGateButNeverMeansPhysicalBrake() {
        val s=session();online(s);s.stop();ack(3);s.pump();assertTrue(s.safeToProceed());assertFalse(s.snapshot.control.connected)
        assertTrue(s.snapshot.message.contains("ACK"))
    }
    @Test fun I06_I12_reconnectRetiresOldSocketBeforeOneNewNonceSession() {
        val s=session();online(s);s.connect();assertEquals(1,fake.opens)
        ack(3);s.pump();assertEquals(2,fake.opens);assertFalse(s.snapshot.control.connected)
        assertEquals("H1:1111111111111111\n",fake.sent.last())
    }
    @Test fun I10_I12_permissionAndIoErrorsAreImmediateSafeOffline() {
        val s=session();allowed=false;s.connect();assertEquals(0,fake.opens);assertFalse(s.snapshot.control.connected)
        allowed=true;online(s);allowed=false;s.pump();assertFalse(s.snapshot.control.connected);assertTrue(s.snapshot.message.contains("quyền"))
        allowed=true;now=1000;val t=session();online(t);fake.fail=true;t.event(ControlEvent.Press(2,ControlButton.FORWARD));t.pump()
        assertFalse(t.snapshot.control.connected);assertNull(t.snapshot.rttMs);assertEquals(WheelPwm(0,0),t.snapshot.control.pwm)
    }
    @Test fun F02_I07_macroDurationAndIdStayUntilFreshCompletionAck() {
        val s=session(ControllerSettings(turnDurationMs=350));online(s);s.event(ControlEvent.Turn);s.pump()
        assertTrue(fake.sent.last().contains(",0,0,1,350|S:0,0,0,0,0"))
        ack(2,"-255,255,0,0,0,0,0,1,1");s.pump();assertTrue(s.snapshot.control.turnActive)
        now=30;s.pump();assertTrue(fake.sent.last().contains(",1,350|"))
        ack(3,"0,0,0,0,0,0,0,1,0");s.pump();assertEquals(0,s.snapshot.control.turnId)
    }
    @Test fun U07_unlockRequiresHoldAndMatchingAckNotOnlyLocalTimer() {
        val s=session();online(s);s.event(ControlEvent.Kill);s.pump();ack(2,"0,0,0,0,0,1,0,0,0");s.pump()
        s.event(ControlEvent.BeginUnlock)
        for(i in 1..33){now=i*30L;s.pump();ack(i+2,"0,0,0,0,0,1,0,0,0");s.pump()}
        now=1000;s.pump();assertTrue(fake.sent.last().endsWith("|U\n"));assertTrue(s.snapshot.control.killed)
        val seq=fake.sent.last().substringAfter(',').substringBefore(',').toInt()
        ack(seq);s.pump();assertFalse(s.snapshot.control.killed);assertEquals(WheelPwm(0,0),s.snapshot.control.pwm)
    }
    @Test fun F09_configOnlyCommitsMatchedAckAndUnsupportedKeepsOld() {
        val s=session();online(s,11);assertTrue(s.configure(1,0,100));s.pump();assertNull(s.snapshot.configuration)
        reply("Q1:$sid,2,2,0,100,OK\n");s.pump();assertNull(s.snapshot.configuration)
        reply("Q1:$sid,2,1,0,100,OK\n");s.pump();assertEquals(0 to 100,s.snapshot.configuration)
        assertTrue(s.configure(2,10,90));s.pump();reply("Q1:$sid,3,2,10,90,BAD\n");s.pump();assertEquals(0 to 100,s.snapshot.configuration)
        val t=session();now=0;fake.incoming.clear();online(t,3);assertFalse(t.configure(1,0,100));assertNull(t.snapshot.configuration)
    }
    @Test fun C10_F05_aliasStillUsesRequiredSNeutralHandshakeThenDForDriving() {
        val s=session(ControllerSettings(driveTemplate=ControllerSettings.D_TEMPLATE));online(s)
        s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump()
        assertTrue(fake.sent.last().endsWith("|D:153,153,0,0,0\n"))
    }
    @Test fun I05_I08_ackTimeoutAttemptsFinalStopCloseBeforeRetiringSocket() {
        val s=session();online(s);s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump()
        now=300;s.pump()
        assertTrue(fake.sent.takeLast(2)[0].endsWith("|S:0,0,0,0,0\n"))
        assertTrue(fake.sent.last().endsWith("|C\n"));assertFalse(s.snapshot.control.connected)
    }
    @Test fun F09_lostConfigAckLocksDrivingUntilSamePairIsReconciled() {
        val s=session();online(s,11);assertTrue(s.configure(1,0,100))
        for(i in 1..10) {
            now=i*30L;s.pump()
            val seq=fake.sent.last().substringAfter(',').substringBefore(',').toInt()
            ack(seq);s.pump()
        }
        assertTrue(s.snapshot.configurationStatus.contains("chưa xác nhận"));assertNull(s.snapshot.configuration)
        s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump();assertEquals(WheelPwm(0,0),s.snapshot.control.pwm)
        assertTrue(s.configure(1,0,100))
        val seq=fake.sent.last().substringAfter(',').substringBefore(',').toInt()
        reply("Q1:$sid,$seq,1,0,100,OK\n");s.pump();assertEquals(0 to 100,s.snapshot.configuration)
        s.event(ControlEvent.Press(1,ControlButton.FORWARD));s.pump();assertEquals(WheelPwm(153,153),s.snapshot.control.pwm)
    }
    @Test fun I02_releaseThenFreshDownInSameBatchStillSendsStopBeforeNewMotion() {
        val s=session();online(s);s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump()
        val at=fake.sent.size
        s.event(ControlEvent.Release(0));s.event(ControlEvent.Press(1,ControlButton.BACKWARD));s.pump()
        assertTrue(fake.sent.drop(at).first().endsWith("|S:0,0,0,0,0\n"))
        assertTrue(fake.sent.last().endsWith("|S:-153,-153,0,0,0\n"))
    }
    @Test fun I02_brakeTapInSameBatchCannotBeCoalescedAway() {
        val s=session();online(s);s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump()
        val at=fake.sent.size
        s.event(ControlEvent.Press(1,ControlButton.BRAKE));s.event(ControlEvent.Release(1));s.pump()
        assertTrue(fake.sent.drop(at).first().endsWith("|S:0,0,0,0,1\n"))
        assertTrue(fake.sent.last().endsWith("|S:0,0,0,0,0\n"))
    }
    @Test fun I07_rejectedDriveAckStopsAndClosesInsteadOfKeepingOldIntent() {
        val s=session();online(s);s.event(ControlEvent.Press(0,ControlButton.FORWARD));s.pump()
        ack(2,status="BAD");s.pump()
        assertNotEquals(SessionPhase.ONLINE,s.snapshot.phase);assertEquals(WheelPwm(0,0),s.snapshot.control.pwm)
        assertTrue(fake.sent.last().endsWith("|C\n"))
    }
}
