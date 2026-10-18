package com.aistudy.myapplication.protocol

import com.aistudy.myapplication.control.*
import com.aistudy.myapplication.settings.ControllerSettings
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CommandCodecTest {
    private val sid="0123456789abcdef"
    private val nonce="1111111111111111"
    private fun bytes(s:String)=s.toByteArray(Charsets.US_ASCII)
    private fun frame(seq:Int=1, pwm:WheelPwm=WheelPwm(0,0), boost:Boolean=false, weapon:Boolean=false,
        brake:Boolean=false, kill:Boolean=false, mode:DriveMode=DriveMode.MANUAL, turnId:Int=0,
        duration:Int=0, payload:Payload=Payload.Drive(pwm,boost,weapon,brake)) =
        ControlFrame(sid,seq,kill,mode,turnId,duration,payload)

    @Test fun C01_legacyCompositeAndSrsSamplesAreExactLfBytes() {
        assertEquals("S:200,200,1,0,0\n",CommandCodec.encodeDrive(WheelPwm(200,200),true,false,false))
        assertEquals("D:-255,255,0,0,0\n",CommandCodec.encodeDrive(WheelPwm(-255,255),false,false,false,ControllerSettings.D_TEMPLATE))
        for (pwm in listOf(WheelPwm(0,0),WheelPwm(255,255),WheelPwm(-255,-255),WheelPwm(-255,255),WheelPwm(255,-255))) {
            assertArrayEquals(bytes("V1:$sid,1,0,0,0,0|S:${pwm.left},${pwm.right},0,0,0\n"),CommandCodec.encodeControl(frame(pwm=pwm)))
        }
    }
    @Test fun C01_C02_exactFrozenKotlinVectorsForIndependentPythonReceiver() {
        val commands=mapOf(
            "hello" to CommandCodec.encodeHello(nonce),"servoHello" to CommandCodec.encodeHello(nonce),
            "confirm" to CommandCodec.encodeControl(frame()),"servoConfirm" to CommandCodec.encodeControl(frame()),
            "forward" to CommandCodec.encodeControl(frame(2,WheelPwm(153,153))),
            "boost" to CommandCodec.encodeControl(frame(3,WheelPwm(255,255),boost=true)),
            "stop" to CommandCodec.encodeControl(frame(4)),"weapon" to CommandCodec.encodeControl(frame(5,weapon=true)),
            "weaponOff" to CommandCodec.encodeControl(frame(6)),"brake" to CommandCodec.encodeControl(frame(7,brake=true)),
            "brakeOff" to CommandCodec.encodeControl(frame(8)),"kill" to CommandCodec.encodeControl(frame(9,kill=true)),
            "unlock" to CommandCodec.encodeControl(frame(10,payload=Payload.Unlock)),
            "autoUnsupported" to CommandCodec.encodeControl(frame(11,mode=DriveMode.AUTO)),
            "manual" to CommandCodec.encodeControl(frame(12)),"turn" to CommandCodec.encodeControl(frame(13,turnId=1,duration=200)),
            "turnRepeat" to CommandCodec.encodeControl(frame(14,turnId=1,duration=200)),
            "turnDone" to CommandCodec.encodeControl(frame(15,turnId=1,duration=200)),
            "turnStop" to CommandCodec.encodeControl(frame(16)),
            "alias" to CommandCodec.encodeControl(frame(17,WheelPwm(153,153)),ControllerSettings.D_TEMPLATE),
            "close" to CommandCodec.encodeControl(frame(18,payload=Payload.Close)),
            "retiredSid" to CommandCodec.encodeControl(frame(19,WheelPwm(153,153))),
            "config" to CommandCodec.encodeControl(frame(2,payload=Payload.Configure(1,0,100))),
            "configRetry" to CommandCodec.encodeControl(frame(3,payload=Payload.Configure(1,0,100))),
            "configConflict" to CommandCodec.encodeControl(frame(4,payload=Payload.Configure(1,0,101)))
        )
        val root=generateSequence(File(".").canonicalFile){it.parentFile}.first{File(it,"docs/testing/phase-2/fixtures.tsv").exists()}
        val rows=File(root,"docs/testing/phase-2/fixtures.tsv").readLines()
        val generated=mutableListOf(rows.first())
        for (row in rows.drop(1)) {
            val columns=row.split('\t'); val actual=commands.getValue(columns[2])
            val hex=actual.joinToString(""){"%02x".format(it.toInt() and 255)}
            assertEquals(columns[2],columns[3],hex)
            if(columns[4]!="-") assertNotNull(CommandCodec.decodeReply(columns[4].chunked(2).map{it.toInt(16).toByte()}.toByteArray()))
            generated.add(columns.toMutableList().also{it[3]=hex}.joinToString("\t"))
        }
        File(root,"docs/testing/phase-2/evidence/kotlin-vectors.tsv").writeText(generated.joinToString("\n",postfix="\n"))
    }
    @Test fun C02_decodeEveryReplyTypeAndPendingState() {
        assertEquals(RobotReply.Offer(nonce,sid,3),CommandCodec.decodeReply(bytes("H1:$nonce,$sid,3\n")))
        assertEquals(RobotReply.Busy(nonce),CommandCodec.decodeReply(bytes("N1:$nonce,BUSY\n")))
        val ack=CommandCodec.decodeReply(bytes("A1:$sid,4,OK,0,0,0,1,0,0,0,0,0\n")) as RobotReply.Ack
        assertTrue(ack.state.weapon); assertEquals(4,ack.seq)
        val q=CommandCodec.decodeReply(bytes("Q1:$sid,7,2,0,100,OK\n")) as RobotReply.ConfigAck
        assertEquals(100,q.activeDeg)
    }
    @Test fun C03_C04_rejectMalformedLfAsciiBoundsAndCounts() {
        val good="A1:$sid,1,OK,0,0,0,0,0,0,0,0,0\n"
        val bad=listOf(good.trimEnd(),good.replace("\n","\\n"),good.replace("\n","\r\n"),good+"x",good+good,
            good.replace(",OK,",",WAT,"),good.replace(",1,OK",",0,OK"),good.replace(",1,OK",",2147483648,OK"),
            good.replace("OK,0,0","OK,256,0"),good.replace("OK,0,0","OK,-256,0"),
            good.replace("OK,0,0","OK,+1,0"),good.replace("OK,0,0","OK,NaN,0"),good.replace("OK,0,0","OK, 1,0"),
            good.replace(sid,"0000000000000000"),good.replace(sid,"0123456789ABCDEF"),
            "A1:$sid,1,OK,0,0,0,0,0,0,0,0,1\n", "A1:$sid,1,OK,0,0,0,0,0,0,0,0\n",
            "H1:$nonce,$sid,0\n","N1:$nonce,OK\n","Q1:$sid,1,0,0,90,OK\n","Q1:$sid,1,1,0,181,OK\n",
            "X".repeat(256)+"\n",good.replace("OK","O\u0000K"))
        for(s in bad) assertThrows(s,IllegalArgumentException::class.java){CommandCodec.decodeReply(bytes(s))}
        assertThrows(IllegalArgumentException::class.java){CommandCodec.decodeReply(byteArrayOf(0xff.toByte(),10))}
    }
    @Test fun C03_outgoingFrameSemanticValidationCannotBeBypassed() {
        for(f in listOf(frame().copy(seq=0),frame().copy(sid="0".repeat(16)),frame(turnId=1),
            frame(duration=200),frame(turnId=1,duration=2001),frame(pwm=WheelPwm(1,1),kill=true),
            frame(pwm=WheelPwm(1,1),brake=true),frame(weapon=true,mode=DriveMode.AUTO),
            frame(turnId=1,duration=200,pwm=WheelPwm(1,1)),frame(boost=true),
            frame(kill=true,payload=Payload.Unlock),frame(payload=Payload.Configure(0,0,90)),
            frame(payload=Payload.Configure(1,-1,90)))) {
            assertThrows(IllegalArgumentException::class.java){CommandCodec.encodeControl(f)}
        }
        assertArrayEquals(bytes("V1:$sid,2147483647,0,0,0,0|S:0,0,0,0,0\n"),CommandCodec.encodeControl(frame(Int.MAX_VALUE)))
    }
    @Test fun C10_templatesAreWhitelistedAndAffectWireBytes() {
        val f=frame(2,WheelPwm(153,153))
        assertTrue(String(CommandCodec.encodeControl(f,ControllerSettings.D_TEMPLATE)).contains("|D:153,153"))
        for(t in listOf("S:{R},{L},{B},{W},{K}\n","S:{L},{R}\\n","S:{L},{R},{B},{W},{K}\nX:1\n"))
            assertThrows(IllegalArgumentException::class.java){CommandCodec.encodeControl(f,t)}
    }
}
