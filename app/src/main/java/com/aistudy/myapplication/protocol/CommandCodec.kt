package com.aistudy.myapplication.protocol

import com.aistudy.myapplication.control.*
import com.aistudy.myapplication.settings.ControllerSettings

sealed interface Payload {
    data class Drive(val pwm:WheelPwm=WheelPwm(0,0),val boost:Boolean=false,val weapon:Boolean=false,val brake:Boolean=false):Payload
    data object Unlock:Payload
    data object Close:Payload
    data class Configure(val id:Int,val homeDeg:Int,val activeDeg:Int):Payload
}
data class ControlFrame(val sid:String,val seq:Int,val kill:Boolean=false,val mode:DriveMode=DriveMode.MANUAL,
    val turnId:Int=0,val durationMs:Int=0,val payload:Payload=Payload.Drive())
sealed interface RobotReply {
    data class Offer(val nonce:String,val sid:String,val capabilities:Int):RobotReply
    data class Busy(val nonce:String):RobotReply
    data class Ack(val sid:String,val seq:Int,val result:String,val state:ControlState):RobotReply
    data class ConfigAck(val sid:String,val seq:Int,val configId:Int,val homeDeg:Int,val activeDeg:Int,val result:String):RobotReply
}
object CommandCodec {
    private val results = setOf("OK", "LOCKED", "BAD", "UNSUPPORTED", "BUSY")
    private fun id(value:String):String {
        require(value.length == 16)
        var nonZero = false
        for (character in value) {
            require(character in '0'..'9' || character in 'a'..'f')
            if (character != '0') nonZero = true
        }
        require(nonZero)
        return value
    }
    private fun number(value:String, min:Int=0, max:Int=Int.MAX_VALUE):Int {
        val start = if (min < 0 && value.startsWith('-')) 1 else 0
        require(start < value.length)
        for (index in start until value.length) require(value[index] in '0'..'9')
        return requireNotNull(value.toIntOrNull()).also { require(it in min..max) }
    }
    private fun flag(value:String):Boolean { require(value == "0" || value == "1"); return value == "1" }
    private fun Boolean.bit() = if (this) 1 else 0
    private fun text(bytes:ByteArray):String {
        require(bytes.size in 1..256 && bytes.last() == 10.toByte())
        for (index in 0 until bytes.lastIndex) require((bytes[index].toInt() and 255) in 32..126)
        return String(bytes, 0, bytes.lastIndex, Charsets.US_ASCII)
    }
    private fun wire(value:String):ByteArray {
        require(value.length in 1..256 && value.last() == '\n')
        for (index in 0 until value.lastIndex) require(value[index] in ' '..'~')
        return value.toByteArray(Charsets.US_ASCII)
    }
    fun encodeHello(nonce:String):ByteArray = wire("H1:${id(nonce)}\n")
    private fun driveBody(pwm:WheelPwm,boost:Boolean,weapon:Boolean,brake:Boolean,template:String):String {
        require(template == ControllerSettings.S_TEMPLATE || template == ControllerSettings.D_TEMPLATE)
        val verb = if (template == ControllerSettings.D_TEMPLATE) "D" else "S"
        return "$verb:${pwm.left},${pwm.right},${boost.bit()},${weapon.bit()},${brake.bit()}"
    }
    fun encodeDrive(pwm:WheelPwm,boost:Boolean,weapon:Boolean,brake:Boolean,template:String=ControllerSettings.S_TEMPLATE):String =
        driveBody(pwm,boost,weapon,brake,template) + "\n"
    fun encodeControl(frame:ControlFrame,template:String=ControllerSettings.S_TEMPLATE):ByteArray {
        id(frame.sid)
        require(frame.seq > 0 && frame.turnId >= 0)
        require((frame.turnId == 0 && frame.durationMs == 0) || (frame.turnId > 0 && frame.durationMs in 1..2000))
        val neutralMetadata = !frame.kill && frame.mode == DriveMode.MANUAL && frame.turnId == 0
        val body = when (val p = frame.payload) {
            is Payload.Drive -> {
                val zero = p.pwm == WheelPwm(0, 0)
                if (frame.kill) require(frame.mode == DriveMode.MANUAL && frame.turnId == 0 && zero && !p.boost && !p.weapon && !p.brake)
                if (p.brake) require(neutralMetadata && zero && !p.boost && !p.weapon)
                if (frame.mode == DriveMode.AUTO || frame.turnId > 0) require(!frame.kill && zero && !p.boost && !p.weapon && !p.brake)
                if (frame.mode == DriveMode.AUTO) require(frame.turnId == 0)
                if (frame.turnId > 0) require(frame.mode == DriveMode.MANUAL)
                require(!p.boost || !zero)
                driveBody(p.pwm,p.boost,p.weapon,p.brake,template)
            }
            Payload.Unlock -> { require(neutralMetadata); "U" }
            Payload.Close -> { require(neutralMetadata); "C" }
            is Payload.Configure -> {
                require(neutralMetadata && p.id > 0 && p.homeDeg in 0..180 && p.activeDeg in 0..180)
                "Q:${p.id},${p.homeDeg},${p.activeDeg}"
            }
        }
        return wire("V1:${frame.sid},${frame.seq},${frame.kill.bit()},${if(frame.mode==DriveMode.AUTO) 1 else 0},${frame.turnId},${frame.durationMs}|$body\n")
    }
    fun decodeReply(bytes:ByteArray):RobotReply {
        val raw=text(bytes).split(':',limit=2)
        require(raw.size==2)
        val p=raw[1].split(',')
        return when(raw[0]) {
            "H1" -> {
                require(p.size==3)
                val caps=number(p[2],1)
                require(caps and 1 != 0)
                RobotReply.Offer(id(p[0]),id(p[1]),caps)
            }
            "N1" -> { require(p.size==2 && p[1]=="BUSY"); RobotReply.Busy(id(p[0])) }
            "Q1" -> {
                require(p.size==6 && p[5] in results)
                RobotReply.ConfigAck(id(p[0]),number(p[1],1),number(p[2],1),number(p[3],0,180),number(p[4],0,180),p[5])
            }
            "A1" -> {
                require(p.size==12 && p[2] in results)
                val pwm=WheelPwm(number(p[3],-255,255),number(p[4],-255,255))
                val b=flag(p[5]); val w=flag(p[6]); val k=flag(p[7]); val x=flag(p[8])
                val mode=if(flag(p[9])) DriveMode.AUTO else DriveMode.MANUAL
                val turn=number(p[10]); val active=flag(p[11]); val zero=pwm==WheelPwm(0,0)
                require(!b || !zero)
                if(x) require(zero && !b && !w && !k && mode==DriveMode.MANUAL && turn==0 && !active)
                if(k) require(zero && !b && !w && !x && mode==DriveMode.MANUAL && turn==0 && !active)
                if(active) require(turn>0 && pwm==WheelPwm(-255,255) && !b && !w && !k && !x && mode==DriveMode.MANUAL)
                if(turn>0 && !active) require(zero && !b && !w && !k && !x && mode==DriveMode.MANUAL)
                RobotReply.Ack(id(p[0]),number(p[1],1),p[2],ControlState(pwm=pwm,boost=b,weapon=w,brake=k,killed=x,mode=mode,turnId=turn,turnActive=active))
            }
            else -> throw IllegalArgumentException("Unsupported SUMO-SIM/1 reply")
        }
    }
}
