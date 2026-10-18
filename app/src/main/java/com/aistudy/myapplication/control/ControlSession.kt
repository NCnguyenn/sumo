package com.aistudy.myapplication.control

import com.aistudy.myapplication.connection.RobotTransport
import com.aistudy.myapplication.settings.ControllerSettings
import com.aistudy.myapplication.protocol.*

enum class SessionPhase { OFFLINE, CONNECTING, ONLINE, CLOSING }
data class SessionSnapshot(val control:ControlState,val settings:ControllerSettings,val phase:SessionPhase,
    val message:String,val rttMs:Long?=null,val applied:ControlState?=null,val capabilities:Int=0,
    val configuration:Pair<Int,Int>?=null,val configurationStatus:String="Chưa xác nhận cấu hình receiver")
class ControlSession(val reducer:ControlReducer,private val transport:RobotTransport,private val clock:()->Long,
    private val nonce:()->String,private val permission:()->Boolean={true},private val trace:(String)->Unit={},
    private val nanoClock:()->Long={clock()*1_000_000L}):AutoCloseable {
    private var phase=SessionPhase.OFFLINE
    private var tracker=ReplyTracker(reducer.settings.host,reducer.settings.port)
    private var message="Offline · chưa kết nối receiver bản thử"
    private var hello=""
    private var retryAt=0L
    private var nextTickNs=0L
    private var lastSendCompletedNs:Long?=null
    internal var prepareStartNs=0L
        private set
    internal var prepareEndNs=0L
        private set
    internal var periodicDeadlineNs=0L
        private set
    internal val offline get()=phase==SessionPhase.OFFLINE
    private fun periodicDueNs()=maxOf(nextTickNs,lastSendCompletedNs?.plus(20_000_000L) ?: nextTickNs)
    internal fun waitNanos():Long=if(phase==SessionPhase.ONLINE) {
        if(dirty)0L else (periodicDueNs()-nanoClock()).coerceIn(0L,2_000_000L)
    } else 2_000_000L
    private var safeAfter=0L
    private var seq=0
    private var sessionSid:String?=null
    private var dirty=false
    private var reconnect=false
    private var applied:ControlState?=null
    private val sent=linkedMapOf<Int,ControlFrame>()
    private var configuration:Pair<Int,Int>?=null
    private var configStatus="Chưa xác nhận cấu hình receiver"
    private var pendingConfig:Payload.Configure?=null
    private var configAt=0L
    private var uncertainConfig=false
    internal var revision=0L
        private set
    val snapshot:SessionSnapshot get()=SessionSnapshot(reducer.state,reducer.settings,phase,
        if(phase==SessionPhase.ONLINE)"Online · bản thử ${reducer.settings.host}:${reducer.settings.port} · ${tracker.lastRttMs ?: 0} ms" else message,
        if(phase==SessionPhase.ONLINE)tracker.lastRttMs else null,applied,tracker.capabilities,configuration,configStatus)
    private fun log(value:String){trace("${clock()} $value")}
    fun connect(){
        revision++
        if(phase!=SessionPhase.OFFLINE || !safeToProceed()){stop();reconnect=true;return}
        try {
            val s=reducer.settings
            log("connecting endpoint=${s.host}:${s.port} interval=${s.sendIntervalMs}ms limit=${s.limitPercent}% input=${s.inputMode} weapon=${s.weaponMode} turn=${s.turnDurationMs}ms calibration=${s.calibration}")
            if(!permission()){message="Offline · cần cấp quyền mạng nội bộ";log("closed confirmed=false requested=0/0 reason=$message");return}
            reducer.disconnect();tracker=ReplyTracker(reducer.settings.host,reducer.settings.port)
            hello=nonce();tracker.begin(hello,clock());connectDeadline=clock()+1000;seq=0;sessionSid=null;sent.clear();dirty=false;applied=null
            transport.open(reducer.settings.host,reducer.settings.port)
            phase=SessionPhase.CONNECTING;message="Đang bắt tay · chưa Online"
            sendHello()
        } catch(e:Exception){fail("Kết nối lỗi: ${e.message}")}
    }
    private fun sendHello(){
        prepareStartNs=nanoClock();val data=CommandCodec.encodeHello(hello);prepareEndNs=nanoClock()
        transport.send(data);log("send hello hex=${data.hex()}");retryAt=clock()+250
    }
    fun event(value:ControlEvent){
        revision++
        log("event $value")
        if(value==ControlEvent.Cancel){stop();return}
        if((pendingConfig!=null || uncertainConfig) && value !is ControlEvent.Release && value !is ControlEvent.Limiter &&
            value!=ControlEvent.Kill && value!=ControlEvent.EndUnlock && value!=ControlEvent.CancelUnlock)return
        val urgent=reducer.handle(value);dirty=true
        if(urgent && phase==SessionPhase.ONLINE) {
            try{
                if(!permission()){fail("Mất quyền mạng nội bộ");return}
                send(frame());dirty=false
            }catch(e:Exception){fail("Lỗi gửi khẩn cấp: ${e.message}")}
        }
    }
    private fun frame(payload:Payload?=null):ControlFrame {
        check(seq<Int.MAX_VALUE){"Hết miền seq; cần mở phiên mới"}
        val state=reducer.state
        val body=payload ?: if(state.unlockPending)Payload.Unlock else Payload.Drive(state.pwm,state.boost,state.weapon,state.brake)
        return if(body !is Payload.Drive)ControlFrame(requireNotNull(tracker.sid),++seq,payload=body)
        else ControlFrame(requireNotNull(tracker.sid),++seq,state.killed,state.pendingMode ?: state.mode,state.turnId,state.turnDurationMs,body)
    }
    private fun send(frame:ControlFrame) {
        prepareStartNs=nanoClock()
        val bytes=tracker.recordSent(frame,clock(),if(phase==SessionPhase.CONNECTING)ControllerSettings.S_TEMPLATE else reducer.settings.driveTemplate)
        prepareEndNs=nanoClock()
        sent[frame.seq]=frame
        while(sent.size>256)sent.remove(sent.keys.first())
        transport.send(bytes);safeAfter=clock()+300
        lastSendCompletedNs=nanoClock()
        if(phase==SessionPhase.ONLINE) {
            val finished=nanoClock();val period=reducer.settings.sendIntervalMs*1_000_000L
            // Keep the original monotonic slots; skip missed slots instead of sending a burst.
            if(finished>=nextTickNs)nextTickNs+=((finished-nextTickNs)/period+1)*period
        }
        log("send seq=${frame.seq}")
    }
    private fun confirm(){send(ControlFrame(requireNotNull(tracker.sid),1));seq=1;retryAt=clock()+250}
    fun pump(){
        try {
            val now=clock()
            if(phase!=SessionPhase.OFFLINE && !permission()){fail("Mất quyền mạng nội bộ");return}
            if(phase==SessionPhase.CLOSING && now>=safeAfter){finish(false,"Offline · chưa xác nhận dừng vật lý; đã đợi watchdog")}
            if(phase==SessionPhase.OFFLINE){if(reconnect && safeToProceed()){reconnect=false;connect()};return}
            tracker.tick(now)
            if(phase!=SessionPhase.CLOSING && tracker.sid==null && phase==SessionPhase.ONLINE){fail("Hết hạn ACK 300 ms");return}
            if(phase==SessionPhase.CONNECTING) {
                if(now>=connectDeadline){fail("Hết hạn bắt tay 1000 ms");return}
                if(now>=retryAt){
                    if(tracker.sid==null){
                        if(now>=connectDeadline){fail("Hết hạn bắt tay 1000 ms");return}
                        sendHello()
                    } else confirm()
                }
            }
            if(phase==SessionPhase.ONLINE) {
                if(pendingConfig!=null && now-configAt>=300){pendingConfig=null;uncertainConfig=true;configStatus="Receiver chưa xác nhận; khóa lái đến khi đối soát";reducer.cancelAll();dirty=true;revision++}
                if(reducer.unlock()){dirty=true;revision++}
                periodicDeadlineNs=periodicDueNs()
                if(dirty || nanoClock()>=periodicDeadlineNs){send(frame());dirty=false}
            }
            receiving@for(index in 0 until 16) {
                val packet=transport.receive() ?: break
                revision++
                log("receive host=${packet.host}:${packet.port} hex=${packet.bytes.hex()}")
                val reply=tracker.accept(packet.bytes,packet.host,packet.port,clock())
                if(reply==null){log("discard reason=${tracker.lastRejection}");continue}
                when(reply) {
                    is RobotReply.Ack->log("ack seq=${reply.seq} result=${reply.result} pwm=${reply.state.pwm.left}/${reply.state.pwm.right} rtt=${tracker.lastRttMs}")
                    is RobotReply.Offer->log("offer sid=${reply.sid} caps=${reply.capabilities}")
                    is RobotReply.ConfigAck->log("config-ack seq=${reply.seq} result=${reply.result}")
                    else->Unit
                }
                when(reply) {
                    is RobotReply.Offer->if(phase==SessionPhase.CONNECTING){sessionSid=reply.sid;confirm()}
                    is RobotReply.ConfigAck->{
                        val requested=pendingConfig
                        if(requested!=null && requested.id==reply.configId && requested.homeDeg==reply.homeDeg && requested.activeDeg==reply.activeDeg) {
                            if(reply.result=="OK"){configuration=reply.homeDeg to reply.activeDeg;uncertainConfig=false;configStatus="Receiver đã ACK cả cặp (fixture)"}
                            else configStatus="Receiver từ chối ${reply.result}; giữ cấu hình hiệu lực cũ"
                            pendingConfig=null
                        }
                    }
                    is RobotReply.Ack->{
                        val request=sent[reply.seq] ?: continue@receiving
                        if(phase==SessionPhase.CLOSING && request.payload==Payload.Close && reply.result=="OK"){finish(true,"Offline · đã ACK đóng phiên; chưa chứng minh dừng cơ học");continue@receiving}
                        if(phase==SessionPhase.CONNECTING && tracker.online){
                            reducer.connectAcknowledged(tracker.capabilities,receiverKilled=reply.state.killed)
                            phase=SessionPhase.ONLINE;nextTickNs=nanoClock()+reducer.settings.sendIntervalMs*1_000_000L;dirty=reducer.state.killed && !reply.state.killed
                        }
                        if(phase==SessionPhase.ONLINE) {
                            applied=reply.state
                            if(reply.state.killed && !reducer.state.killed)reducer.kill()
                            if(reply.result!="OK" && request.payload is Payload.Drive && request.mode==DriveMode.MANUAL && request.turnId==0) {
                                stop();message="Đang đóng phiên: receiver từ chối ${reply.result}";continue@receiving
                            }
                            if(request.payload==Payload.Unlock){reducer.acknowledgeUnlock(reply.result=="OK");dirty=true}
                            reducer.acknowledgeMode(request.mode,reply.result=="OK")
                            if(request.turnId>0){
                                val before=reducer.state.turnId
                                reducer.acknowledgeTurn(request.turnId,reply.state.turnActive,reply.result=="OK")
                                if(before!=reducer.state.turnId)dirty=true
                            }
                        }
                    }
                    else->Unit
                }
            }
            if(phase==SessionPhase.OFFLINE && reconnect && safeToProceed()){reconnect=false;connect()}
        } catch(e:Exception){fail("Lỗi I/O/phiên: ${e.message}")}
    }
    private var connectDeadline=0L
    private fun finish(confirmed:Boolean,reason:String){
        revision++
        transport.close();reducer.disconnect();tracker=ReplyTracker(reducer.settings.host,reducer.settings.port)
        phase=SessionPhase.OFFLINE;message=reason;applied=null;dirty=false;sent.clear();sessionSid=null
        if(confirmed)safeAfter=clock()
        log("closed confirmed=$confirmed requested=${reducer.state.pwm.left}/${reducer.state.pwm.right} reason=$reason")
    }
    private fun fail(reason:String){
        val sid=sessionSid
        if(sid!=null && permission() && seq<Int.MAX_VALUE-1) {
            reducer.cancelAll()
            for(payload in listOf<Payload>(Payload.Drive(),Payload.Close)) {
                try {
                    val bytes=CommandCodec.encodeControl(ControlFrame(sid,++seq,
                        kill=payload is Payload.Drive && reducer.state.killed,payload=payload),reducer.settings.driveTemplate)
                    transport.send(bytes);safeAfter=clock()+300;log("final-stop-attempt hex=${bytes.hex()}")
                } catch(_:Exception){break}
            }
        }
        finish(false,"Offline · $reason")
    }
    fun stop(){
        revision++
        reconnect=false
        if(pendingConfig!=null){uncertainConfig=true;pendingConfig=null;configStatus="Receiver chưa xác nhận; khóa lái đến khi đối soát"}
        reducer.cancelAll()
        if(phase==SessionPhase.CLOSING)return
        if(phase==SessionPhase.ONLINE) {
            try{send(frame());send(frame(Payload.Close));phase=SessionPhase.CLOSING;message="Đang đóng phiên; chờ ACK/watchdog";reducer.disconnect()}
            catch(e:Exception){fail("Lỗi đóng phiên: ${e.message}")}
        } else finish(false,"Offline · phiên đã đóng")
    }
    fun safeToProceed():Boolean=phase==SessionPhase.OFFLINE && clock()>=safeAfter
    override fun close(){try{stop()}finally{finish(false,"Offline · đã đóng transport")}}
    fun applySettings(value:ControllerSettings){check(safeToProceed());reducer.applySettings(value);tracker=ReplyTracker(value.host,value.port);revision++}
    fun configure(id:Int,home:Int,active:Int):Boolean {
        revision++
        if(phase!=SessionPhase.ONLINE || tracker.capabilities and 8==0){configStatus="Receiver không hỗ trợ cấu hình cơ cấu";return false}
        val value=Payload.Configure(id,home,active)
        CommandCodec.encodeControl(ControlFrame(requireNotNull(tracker.sid),seq+1,payload=value))
        val s=reducer.state
        if(s.pwm!=WheelPwm(0,0) || s.weapon || s.boost || s.brake || s.killed || s.mode!=DriveMode.MANUAL || s.turnId!=0 || pendingConfig!=null)return false
        reducer.cancelAll();pendingConfig=value;configAt=clock();configStatus="Chờ receiver ACK; chưa thay cấu hình hiệu lực"
        try{send(frame(value))}catch(e:Exception){uncertainConfig=true;pendingConfig=null;fail("Lỗi cấu hình: ${e.message}");return false}
        return true
    }
    private fun ByteArray.hex()=controlHex()
}
