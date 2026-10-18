package com.aistudy.myapplication.protocol

import com.aistudy.myapplication.control.DriveMode
import com.aistudy.myapplication.control.WheelPwm
import com.aistudy.myapplication.settings.ControllerSettings
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class SimulatorInteropTest {
    @Test(timeout=20000) fun C01_C02_C05_C08_C09_kotlinDatagramsToIndependentPythonProcess() {
        val root=generateSequence(File(".").canonicalFile){it.parentFile}.first{File(it,"tools/sumo_simulator.py").exists()}
        val log=File(root,"docs/testing/phase-2/evidence/udp-kotlin-python.jsonl")
        val process=ProcessBuilder("py","-3.13",File(root,"tools/sumo_simulator.py").path,"--host","127.0.0.1",
            "--port","0","--log",log.path).redirectErrorStream(true).start()
        val reader=Executors.newSingleThreadExecutor()
        try {
            val ready=reader.submit(Callable{process.inputStream.bufferedReader().readLine()}).get(5,TimeUnit.SECONDS)
            assertTrue("Expected READY <port>, got $ready",ready?.startsWith("READY ")==true)
            val port=ready.substringAfter("READY ").toInt()
            val loopback=InetAddress.getByName("127.0.0.1")
            DatagramSocket(0,loopback).use { socket ->
                socket.soTimeout=2000
                fun send(data:ByteArray) { socket.send(DatagramPacket(data,data.size,loopback,port)) }
                fun receive():DatagramPacket {
                    val packet=DatagramPacket(ByteArray(1024),1024)
                    socket.receive(packet); return packet
                }
                fun exchange(data:ByteArray):ByteArray { send(data); val p=receive(); assertEquals(port,p.port); return p.data.copyOf(p.length) }
                val nonce="1111111111111111"
                val offerBytes=exchange(CommandCodec.encodeHello(nonce))
                val offer=CommandCodec.decodeReply(offerBytes) as RobotReply.Offer
                assertEquals(nonce,offer.nonce); assertEquals(3,offer.capabilities)
                val sid=offer.sid
                fun frame(seq:Int,payload:Payload=Payload.Drive(),x:Boolean=false,mode:DriveMode=DriveMode.MANUAL,tid:Int=0,duration:Int=0)=
                    ControlFrame(sid,seq,x,mode,tid,duration,payload)
                fun expected(seq:Int,fields:String="0,0,0,0,0,0,0,0,0",status:String="OK")="A1:$sid,$seq,$status,$fields\n".toByteArray()
                fun expect(f:ControlFrame,fields:String="0,0,0,0,0,0,0,0,0",status:String="OK",template:String=ControllerSettings.S_TEMPLATE):ByteArray {
                    val reply=exchange(CommandCodec.encodeControl(f,template)); assertArrayEquals(expected(f.seq,fields,status),reply); return reply
                }
                val confirm=expect(frame(1))
                val tracker=ReplyTracker("127.0.0.1",port); tracker.begin(nonce,0)
                tracker.accept(offerBytes,"127.0.0.1",port,0); tracker.recordSent(frame(1),0)
                DatagramSocket(0,loopback).use { attacker ->
                    attacker.send(DatagramPacket(confirm,confirm.size,loopback,socket.localPort))
                    val forged=receive()
                    assertNull(tracker.accept(forged.data.copyOf(forged.length),requireNotNull(forged.address.hostAddress),forged.port,1))
                    assertFalse(tracker.online)
                }
                assertNotNull(tracker.accept(confirm,"127.0.0.1",port,2)); assertTrue(tracker.online)
                expect(frame(2,Payload.Drive(WheelPwm(153,153))),"153,153,0,0,0,0,0,0,0")
                expect(frame(3,Payload.Drive(WheelPwm(255,255),boost=true)),"255,255,1,0,0,0,0,0,0")
                expect(frame(4)); expect(frame(5,Payload.Drive(weapon=true)),"0,0,0,1,0,0,0,0,0")
                expect(frame(6)); expect(frame(7,Payload.Drive(brake=true)),"0,0,0,0,1,0,0,0,0")
                expect(frame(8)); expect(frame(9,x=true),"0,0,0,0,0,1,0,0,0")
                expect(frame(10,Payload.Unlock)); expect(frame(11,mode=DriveMode.AUTO),status="UNSUPPORTED")
                expect(frame(12,tid=1,duration=200),"-255,255,0,0,0,0,0,1,1")
                expect(frame(13,tid=1,duration=200),"-255,255,0,0,0,0,0,1,1")
                expect(frame(14)); expect(frame(15,tid=1,duration=200))
                expect(frame(16,Payload.Drive(WheelPwm(153,153))),"153,153,0,0,0,0,0,0,0",template=ControllerSettings.D_TEMPLATE)
                expect(frame(17))
                assertArrayEquals("Q1:$sid,18,1,0,100,UNSUPPORTED\n".toByteArray(),exchange(CommandCodec.encodeControl(frame(18,Payload.Configure(1,0,100)))))
                expect(frame(19,Payload.Close))
                socket.soTimeout=150
                send(CommandCodec.encodeControl(frame(20,Payload.Drive(WheelPwm(153,153)))))
                assertThrows(SocketTimeoutException::class.java){receive()}
                socket.soTimeout=2000
                val next=CommandCodec.decodeReply(exchange(CommandCodec.encodeHello(nonce))) as RobotReply.Offer
                assertNotEquals(sid,next.sid)
                exchange(CommandCodec.encodeControl(ControlFrame(next.sid,1)))
                send(CommandCodec.encodeControl(frame(21,Payload.Drive(WheelPwm(153,153)))))
                socket.soTimeout=150
                assertThrows(SocketTimeoutException::class.java){receive()}
            }
        } finally {
            process.destroy()
            if(!process.waitFor(2,TimeUnit.SECONDS)) process.destroyForcibly()
            reader.shutdownNow()
        }
        val records=log.readText()
        assertTrue(records.contains("\"applied\""))
        assertEquals(1,Regex("\"event\"\\s*:\\s*\"macro-start\"").findAll(records).count())
    }
}
