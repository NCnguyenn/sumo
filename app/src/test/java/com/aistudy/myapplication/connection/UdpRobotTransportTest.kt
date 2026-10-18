package com.aistudy.myapplication.connection

import java.net.*
import java.util.concurrent.*
import org.junit.Assert.*
import org.junit.Test

class UdpRobotTransportTest {
    @Test fun I12_sendBufferKeepsExactLengthAndRejectsOversizeWithoutTruncating() {
        DatagramSocket(0,InetAddress.getByName("127.0.0.1")).use { server->
            server.soTimeout=1000
            val transport=UdpRobotTransport()
            try {
                transport.open("127.0.0.1",server.localPort)
                for(size in listOf(256,1,73)) {
                    val data=ByteArray(size){it.toByte()}
                    transport.send(data)
                    val packet=DatagramPacket(ByteArray(300),300);server.receive(packet)
                    assertArrayEquals(data,packet.data.copyOf(packet.length))
                }
                assertThrows(java.io.IOException::class.java){transport.send(ByteArray(257))}
            } finally {transport.close()}
        }
    }
    @Test fun I11_I12_realLoopbackPreservesDatagramAndFreshPortOnReopen() {
        DatagramSocket(0,InetAddress.getByName("127.0.0.1")).use{server->
            server.soTimeout=1000
            val transport=UdpRobotTransport()
            try {
                transport.open("127.0.0.1",server.localPort)
                val raw="H1:1111111111111111\n".toByteArray();transport.send(raw)
                val packet=DatagramPacket(ByteArray(1024),1024);server.receive(packet)
                assertArrayEquals(raw,packet.data.copyOf(packet.length));val first=packet.port
                server.send(DatagramPacket(raw,raw.size,packet.address,packet.port))
                val reply=generateSequence{transport.receive()}.first()
                assertArrayEquals(raw,reply.bytes);assertEquals(server.localPort,reply.port)
                assertNull(transport.receive())
                transport.close();transport.open("127.0.0.1",server.localPort);transport.send(raw);server.receive(packet)
                assertNotEquals(first,packet.port)
            } finally{transport.close()}
        }
    }
    @Test fun I12_closeDoesNotWaitForReceiverAndSendAfterCloseFails() {
        val transport=UdpRobotTransport();transport.open("127.0.0.1",8888)
        transport.close();transport.close()
        assertThrows(java.io.IOException::class.java){transport.send(byteArrayOf(1))}
    }
    @Test fun C03_I12_reusedReceiveBufferPreservesNextLongerPacketAndRejectsOversize() {
        DatagramSocket(0,InetAddress.getByName("127.0.0.1")).use{server->
            server.soTimeout=1000;val transport=UdpRobotTransport()
            try {
                transport.open("127.0.0.1",server.localPort);transport.send(byteArrayOf(1))
                val peer=DatagramPacket(ByteArray(10),10);server.receive(peer)
                for(size in listOf(1,256,300)) {
                    val data=ByteArray(size){120};data[size-1]=10
                    server.send(DatagramPacket(data,data.size,peer.address,peer.port))
                    val received=requireNotNull(transport.receive()).bytes
                    if(size<=256)assertArrayEquals(data,received)
                    else assertThrows(IllegalArgumentException::class.java){com.aistudy.myapplication.protocol.CommandCodec.decodeReply(received)}
                }
            }finally{transport.close()}
        }
    }
}
