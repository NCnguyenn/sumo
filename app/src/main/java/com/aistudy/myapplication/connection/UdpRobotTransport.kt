package com.aistudy.myapplication.connection

import com.aistudy.myapplication.settings.ControllerSettings
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class UdpRobotTransport:RobotTransport {
    @Volatile private var channel:DatagramChannel?=null
    private var endpoint:InetSocketAddress?=null
    private val receiveBuffer=ByteBuffer.allocateDirect(257)
    private val sendBuffer=ByteBuffer.allocateDirect(256)
    override fun open(host:String,port:Int){
        ControllerSettings(host=host,port=port).validated()
        close()
        val opened=DatagramChannel.open()
        try{opened.configureBlocking(false);opened.bind(InetSocketAddress(0));endpoint=InetSocketAddress(InetAddress.getByName(host),port);channel=opened}
        catch(e:Exception){opened.close();throw e}
    }
    override fun send(bytes:ByteArray){
        val current=channel ?: throw IOException("Socket đã đóng")
        val remote=endpoint ?: throw IOException("Chưa có endpoint")
        if(bytes.size>sendBuffer.capacity())throw IOException("SUMO frame vượt 256 byte; không cắt bản tin")
        sendBuffer.clear();sendBuffer.put(bytes);sendBuffer.flip()
        if(current.send(sendBuffer,remote)!=bytes.size)throw IOException("UDP chưa sẵn sàng gửi; không xếp lại lệnh cũ")
    }
    override fun receive():RobotDatagram? {
        val current=channel ?: return null
        receiveBuffer.clear()
        val peer=current.receive(receiveBuffer) as? InetSocketAddress ?: return null
        val bytes=ByteArray(receiveBuffer.position())
        receiveBuffer.flip();receiveBuffer.get(bytes)
        return RobotDatagram(bytes,requireNotNull(peer.address.hostAddress),peer.port)
    }
    override fun close(){channel?.close();channel=null;endpoint=null}
}
