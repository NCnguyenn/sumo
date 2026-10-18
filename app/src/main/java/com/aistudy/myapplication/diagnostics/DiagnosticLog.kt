package com.aistudy.myapplication.diagnostics

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DiagnosticGroup(val label:String) { APP("App"), NETWORK("Mạng"), PROTOCOL("Giao thức"), IOT("IoT") }
data class DiagnosticEntry(val atMs:Long,val group:DiagnosticGroup,val text:String,val warning:Boolean=false)
data class DiagnosticSummary(val tx:Int=0,val rx:Int=0,val discarded:Int=0,val requested:String="0 / 0",
    val acknowledged:String?=null,val lastAckAtMs:Long?=null,val rttMs:Long?=null,val warning:String?=null) {
    fun ackAge(nowMs:Long):Long?=lastAckAtMs?.let{(nowMs-it).coerceAtLeast(0)}
    val source:String get()=when{warning?.contains("quyền")==true->"App · thiếu quyền mạng trên điện thoại"
        warning?.startsWith("Receiver trả")==true->"IoT · receiver trả mã từ chối";else->"Chưa xác định"}
}
data class DiagnosticSnapshot(val summary:DiagnosticSummary=DiagnosticSummary(),val entries:List<DiagnosticEntry> = emptyList())
class DiagnosticLog(private val directory:File,private val capacity:Int=240,
    private val wallOffsetMs:Long=System.currentTimeMillis()-System.nanoTime()/1_000_000L,private val appInfo:String="") {
    init { require(capacity in 1..500) }
    private val entries=ArrayDeque<DiagnosticEntry>()
    @Volatile private var state=DiagnosticSummary()
    private var sessionInfo=appInfo
    private val history=File(directory,"previous-error.txt")
    @Volatile private var previous=runCatching{history.takeIf{it.isFile && it.length()<=MAX_REPORT_BYTES}?.readText()
        ?.takeIf{it.startsWith("SUMO diagnostic\n")}}.getOrNull()
    private var storageFailure:String?=null

    // Called by the existing trace worker, never by the control owner/sender.
    @Synchronized fun record(line:String) {
        val core=line.substringAfter(' ',line)
        val at=line.substringBefore(' ').toLongOrNull()
            ?: field(line,"t2_ns")?.toLongOrNull()?.div(1_000_000L) ?: System.nanoTime()/1_000_000L
        if(core.startsWith("connecting")) {entries.clear();state=DiagnosticSummary();sessionInfo="$appInfo\n$core"}
        var group=DiagnosticGroup.APP
        var text=core
        var warning=false
        when {
            line.startsWith("wire ")-> {
                state=state.copy(tx=state.tx+1)
                val raw=decodeHex(field(line,"hex").orEmpty())
                text="Gửi ${raw.trim()}"
                val body=raw.substringAfter('|',"")
                if(body.startsWith("S:") || body.startsWith("D:")) {
                    val pwm=body.substringAfter(':').split(',')
                    if(pwm.size>=2)state=state.copy(requested="${pwm[0]} / ${pwm[1]}")
                }
            }
            core.startsWith("receive ")-> {
                state=state.copy(rx=state.rx+1);group=DiagnosticGroup.NETWORK
                text="Nhận ${core.substringBefore(" hex=")} · ${decodeHex(field(core,"hex").orEmpty()).trim()}"
            }
            core.startsWith("ack ")-> {
                group=DiagnosticGroup.IOT
                val pwm=field(core,"pwm")?.replace("/"," / ")
                state=state.copy(acknowledged=pwm,lastAckAtMs=at,rttMs=field(core,"rtt")?.toLongOrNull())
                val result=field(core,"result")
                val meaning=when(result){"LOCKED"->"đang khóa";"BAD"->"lệnh không hợp lệ";"UNSUPPORTED"->"không hỗ trợ";"BUSY"->"đang bận";else->"đã xác nhận"}
                text="ACK seq=${field(core,"seq")} · PWM $pwm · $result ($meaning)"
                if(result!="OK"){warning=true;state=state.copy(warning="Receiver trả $result: $meaning")}
            }
            core.startsWith("discard ")-> {
                group=DiagnosticGroup.PROTOCOL;warning=true;state=state.copy(discarded=state.discarded+1)
                text="Loại phản hồi: ${core.substringAfter("reason=")}"
            }
            core.startsWith("offer ")-> {group=DiagnosticGroup.IOT;text="Receiver chấp nhận bắt tay · ${core.substringAfter(' ')}"}
            core.startsWith("config-ack ")-> {group=DiagnosticGroup.IOT;text=core}
            core.startsWith("event Joystick")->return
            core.startsWith("send seq=") || core.startsWith("send hello")->return
        }
        val failed=core.startsWith("closed ") && listOf("Hết hạn","lỗi","Lỗi","quyền","từ chối","chưa xác nhận dừng").any{it in core} ||
            core.startsWith("owner error") || core.startsWith("ack ") && field(core,"result")!="OK"
        if(failed) {
            warning=true;group=when{core.startsWith("ack ")->DiagnosticGroup.IOT;core.startsWith("owner error")->DiagnosticGroup.APP;else->DiagnosticGroup.NETWORK}
            val reason=core.substringAfter("reason=",core).removePrefix("Offline · ")
            if(!core.startsWith("ack "))state=state.copy(warning=if("ACK 300 ms" in reason)"Mất phản hồi quá 300 ms — đã dừng điều khiển" else reason)
        }
        val clean=text.replace(Regex("[\\r\\n\\t\\u0000-\\u001f]")," ").take(600)
        entries.addLast(DiagnosticEntry(at+wallOffsetMs,group,clean,warning))
        while(entries.size>capacity)entries.removeFirst()
        if(failed) {
            previous=report("Phiên lỗi · thời gian nguồn monotonic=${at}ms")
            try {
                directory.mkdirs()
                val temp=File(directory,"previous-error.tmp");temp.writeText(previous!!)
                java.nio.file.Files.move(temp.toPath(),history.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }catch(e:Exception){storageFailure="Chưa lưu phiên lỗi: ${e.javaClass.simpleName}"}
        }
    }
    // UI status reads an immutable snapshot without waiting for trace/file I/O.
    fun summary()=state
    @Synchronized fun snapshot()=DiagnosticSnapshot(state,entries.toList())
    fun previousFailure():String?=previous
    @Synchronized fun report(metadata:String):String {
        val format=SimpleDateFormat("HH:mm:ss.SSS",Locale.ROOT)
        return buildString {
            append("SUMO diagnostic\n");append(sessionInfo.take(1600));append('\n');append(metadata.take(1600));append('\n')
            append("TX=${state.tx} RX=${state.rx} Loại=${state.discarded}\nTX cuối PWM ${state.requested}; ACK cuối PWM ${state.acknowledged ?: "chưa có"}\n")
            append("Nguồn lỗi: ${state.source}; ${state.warning ?: "không có cảnh báo"}\n")
            append("ACK không chứng minh động cơ thực sự quay.\n")
            storageFailure?.let{append(it);append('\n')}
            for(entry in entries)append("${format.format(Date(entry.atMs))}\t${entry.group.label}\t${entry.text}\n")
        }.take(80_000)
    }
    private fun field(line:String,key:String)=line.substringAfter("$key=","").takeIf{it.isNotEmpty()}?.substringBefore(' ')
    private fun decodeHex(value:String):String {
        if(value.length>512 || value.length%2!=0)return "[frame quá dài hoặc hex không hợp lệ]"
        return runCatching{String(value.chunked(2).map{it.toInt(16).toByte()}.toByteArray(),Charsets.US_ASCII)}.getOrElse{"[hex không hợp lệ]"}
    }
    companion object { const val MAX_REPORT_BYTES=256_000L }
}
