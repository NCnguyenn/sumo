package com.aistudy.myapplication.protocol

import com.aistudy.myapplication.control.DriveMode
import com.aistudy.myapplication.control.WheelPwm
import com.aistudy.myapplication.settings.ControllerSettings

class ReplyTracker(private val host:String,private val port:Int) {
    init { ControllerSettings(host=host,port=port).validated() }
    var sid:String? = null
        private set
    var capabilities:Int = 0
        private set
    var online:Boolean = false
        private set
    var lastRttMs:Long? = null
        private set
    var lastRejection:String? = null
        private set
    private fun reject(reason:String):RobotReply? {lastRejection=reason;return null}
    private var nonce:String?=null
    private var startedAt:Long?=null
    private var lastProgress:Long?=null
    private var lastSent=0
    private var lastAck=0
    private var intentFloor=1
    private var lastIntent:ControlFrame?=null
    private data class Sent(val frame:ControlFrame,val at:Long)
    private val pending=linkedMapOf<Int,Sent>()
    private fun clear() {
        sid=null; capabilities=0; online=false; lastRttMs=null
        nonce=null; startedAt=null; lastProgress=null; lastSent=0; lastAck=0
        intentFloor=1; lastIntent=null; pending.clear()
    }
    fun begin(nonce:String,nowMs:Long) {
        CommandCodec.encodeHello(nonce)
        clear(); this.nonce=nonce; startedAt=nowMs
    }
    fun recordSent(frame:ControlFrame,nowMs:Long,template:String=ControllerSettings.S_TEMPLATE):ByteArray {
        tick(nowMs)
        require(sid!=null && frame.sid==sid && startedAt!=null && nowMs>=startedAt!!)
        val bytes=CommandCodec.encodeControl(frame,template)
        if(!online) require(frame==ControlFrame(sid!!,1))
        val existing=pending[frame.seq]
        if(existing!=null) { require(existing.frame==frame); return bytes }
        require(frame.seq>lastSent)
        lastSent=frame.seq
        pending[frame.seq]=Sent(frame,nowMs)
        if(frame.payload !is Payload.Configure) {
            val intent=frame.copy(seq=1)
            if(intent!=lastIntent) { intentFloor=frame.seq; lastIntent=intent }
        }
        while(pending.size>256) pending.remove(pending.keys.first())
        return bytes
    }
    private fun confirms(frame:ControlFrame,reply:RobotReply.Ack):Boolean {
        val s=reply.state
        val stopped=s.pwm==WheelPwm(0,0) && !s.boost && !s.weapon && !s.brake &&
            s.mode==DriveMode.MANUAL && s.turnId==0 && !s.turnActive
        return when(val p=frame.payload) {
            Payload.Unlock -> stopped && !s.killed
            Payload.Close -> stopped
            is Payload.Configure -> false
            is Payload.Drive -> when {
                frame.kill -> stopped && s.killed
                frame.turnId>0 -> !s.killed && !s.brake && (s.turnId==frame.turnId || (s.turnId==0 && stopped))
                frame.mode==DriveMode.AUTO -> s.mode==DriveMode.AUTO && s.pwm==WheelPwm(0,0) &&
                    !s.boost && !s.weapon && !s.brake && !s.killed && s.turnId==0 && !s.turnActive
                s.killed -> p==Payload.Drive() && stopped
                else -> s.pwm==p.pwm && s.boost==p.boost && s.weapon==p.weapon && s.brake==p.brake &&
                    s.mode==DriveMode.MANUAL && s.turnId==0 && !s.turnActive
            }
        }
    }
    fun accept(bytes:ByteArray,fromHost:String,fromPort:Int,nowMs:Long):RobotReply? {
        lastRejection=null
        tick(nowMs)
        val start=startedAt ?: return reject("SESSION: chưa có phiên hoặc phiên đã hết hạn")
        if(fromHost!=host || fromPort!=port || nowMs<start) return reject("PEER: sai IP/port hoặc mốc thời gian")
        val reply=try { CommandCodec.decodeReply(bytes) } catch(_:IllegalArgumentException) { return reject("FRAME: sai định dạng/LF/miền giá trị") }
        if(reply is RobotReply.Offer) {
            if(online || reply.nonce!=nonce || (sid!=null && sid!=reply.sid)) return reject("HANDSHAKE: nonce/SID hoặc trạng thái không khớp")
            sid=reply.sid; capabilities=reply.capabilities
            return reply
        }
        val replySid:String
        val seq:Int
        when(reply) {
            is RobotReply.Ack -> { replySid=reply.sid; seq=reply.seq }
            is RobotReply.ConfigAck -> { replySid=reply.sid; seq=reply.seq }
            else -> return reject("BUSY: receiver đang bận; chưa Online")
        }
        if(replySid!=sid || seq<=lastAck) return reject("REPLAY: SID sai hoặc seq cũ/trùng")
        val sent=pending[seq] ?: return reject("SEQ: chưa gửi hoặc đã ra khỏi cửa sổ")
        val age=nowMs-sent.at
        if(age !in 0..299) return reject("STALE: ACK hết hạn 300 ms hoặc thời gian đảo")
        if(reply is RobotReply.ConfigAck) {
            val config=sent.frame.payload as? Payload.Configure ?: return reject("CONFIG: không có yêu cầu cấu hình tương ứng")
            if(!online || config.id!=reply.configId || config.homeDeg!=reply.homeDeg || config.activeDeg!=reply.activeDeg) return reject("CONFIG: giá trị ACK không khớp yêu cầu")
        } else if(reply is RobotReply.Ack) {
            if(reply.result=="OK" && !confirms(sent.frame,reply)) return reject("STATE: ACK không xác nhận đúng trạng thái đã gửi")
            if(sent.frame.payload is Payload.Configure) {
                if(reply.result=="OK" || !online) return reject("CONFIG: cần phản hồi Q1 trong phiên Online")
            } else {
                if(!online) {
                    val s=reply.state
                    if(seq!=1 || reply.result!="OK" || s.pwm!=WheelPwm(0,0) || s.boost || s.weapon || s.brake ||
                        s.mode!=DriveMode.MANUAL || s.turnId!=0 || s.turnActive) return reject("CONFIRM: chưa có ACK trung tính seq 1")
                }
                online=true; lastProgress=nowMs; lastRttMs=age
            }
        }
        lastAck=seq
        pending.keys.removeAll { it<=seq }
        // Superseded ACKs prove communication progress but cannot confirm current reducer intent.
        if(reply is RobotReply.Ack && seq<intentFloor) return reject("SUPERSEDED: ACK của ý định cũ; không áp dụng lại")
        if(reply is RobotReply.Ack && sent.frame.payload==Payload.Close && reply.result=="OK") clear()
        return reply
    }
    fun tick(nowMs:Long) {
        val start=startedAt ?: return
        val progress=lastProgress
        if((progress==null && nowMs-start>=1000) || (progress!=null && nowMs-progress>=300)) clear()
    }
}
