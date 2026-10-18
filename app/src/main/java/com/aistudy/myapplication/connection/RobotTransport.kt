package com.aistudy.myapplication.connection

data class RobotDatagram(val bytes:ByteArray,val host:String,val port:Int)
interface RobotTransport:AutoCloseable {
    fun open(host:String,port:Int)
    fun send(bytes:ByteArray)
    fun receive():RobotDatagram?
    override fun close()
}
