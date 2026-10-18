package com.aistudy.myapplication

import android.os.Debug
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudy.myapplication.connection.*
import com.aistudy.myapplication.control.*
import com.aistudy.myapplication.protocol.*
import com.aistudy.myapplication.settings.ControllerSettings
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/** Diagnostic only: instrumentation/alloc counting perturb execution; never an acceptance cohort. */
@Suppress("DEPRECATION")
class TrackingProfileTest {
    @Test(timeout=150000) fun P01_profileSessionAndTrackerWithAndWithoutPruning() {
        val label=InstrumentationRegistry.getArguments().getString("trackingProfileLabel") ?: "before"
        require(label in setOf("before","after"))
        val directory=File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")))
        check(directory.isDirectory || directory.mkdirs())
        val task=FutureTask(Callable {
            val sid="0123456789abcdef";val nonce="1111111111111111"
            val n=400;val samples=Array(3){Array(n){LongArray(8)}}
            val names=listOf("session-prepare","tracker-ack","tracker-window")
            val stamps=Array(8){LongArray(4)};var stampCount=0;var collecting=false;var sample=0
            fun stamp():Long {
                val cpu=Debug.threadCpuTimeNanos();val bytes=Debug.getThreadAllocSize().toLong()
                val objects=Debug.getThreadAllocCount().toLong();val wall=System.nanoTime()
                if(collecting){check(stampCount<stamps.size);stamps[stampCount++].apply{this[0]=wall;this[1]=cpu;this[2]=bytes;this[3]=objects}}
                return wall
            }
            fun finish(row:LongArray,start:LongArray) {
                val wall=System.nanoTime();val cpu=Debug.threadCpuTimeNanos()
                val bytes=Debug.getThreadAllocSize().toLong();val objects=Debug.getThreadAllocCount().toLong()
                row[0]=start[0];row[1]=wall;row[2]=cpu-start[1];row[3]=bytes-start[2];row[4]=objects-start[3]
                row[5]=start[1];row[6]=start[2];row[7]=bytes
            }
            val incoming=ArrayDeque<RobotDatagram>()
            fun packet(raw:String)=RobotDatagram(raw.toByteArray(),"127.0.0.1",8888)
            val wire=object:RobotTransport {
                override fun open(host:String,port:Int){}
                override fun close(){}
                override fun receive()=incoming.removeFirstOrNull()
                override fun send(bytes:ByteArray){
                    if(collecting){check(stampCount==2);finish(samples[0][sample],stamps[0])}
                    val raw=String(bytes)
                    if(raw.startsWith("H1:"))incoming.add(packet("H1:$nonce,$sid,3\n"))
                    else {
                        val seq=raw.substringAfter(',').substringBefore(',')
                        val pwm=raw.substringAfter("|S:").substringBefore(',')
                        incoming.add(packet("A1:$sid,$seq,OK,$pwm,$pwm,0,0,0,0,0,0,0\n"))
                    }
                }
            }
            val session=ControlSession(ControlReducer(ControllerSettings(host="127.0.0.1"),{0L}),wire,{0L},{nonce},nanoClock=::stamp)
            session.connect();session.pump();session.pump();check(session.snapshot.control.connected)
            session.event(ControlEvent.Press(0,ControlButton.FORWARD));session.pump()
            fun tracker()=ReplyTracker("127.0.0.1",8888).also {
                it.begin(nonce,0);it.accept("H1:$nonce,$sid,3\n".toByteArray(),"127.0.0.1",8888,0)
                it.recordSent(ControlFrame(sid,1),0)
                check(it.accept("A1:$sid,1,OK,0,0,0,0,0,0,0,0,0\n".toByteArray(),"127.0.0.1",8888,0)!=null)
            }
            val normal=tracker();val window=tracker()
            Debug.startAllocCounting()
            val probeBefore=Debug.getThreadAllocSize();val probe=ByteArray(65536){it.toByte()};val probeAfter=Debug.getThreadAllocSize()
            val allocationSupported=probeBefore>=0 && probeAfter-probeBefore>=probe.size
            val trace=File(directory,"tracking-$label.trace")
            Debug.startMethodTracing(trace.absolutePath,64*1024*1024,Debug.TRACE_COUNT_ALLOCS)
            try {
                for(i in 0 until n) {
                    sample=i;stampCount=0
                    session.event(ControlEvent.Limiter(60));collecting=true
                    try{session.pump()}finally{collecting=false}
                    check(session.snapshot.control.connected)
                }
                for((group,t) in listOf(1 to normal,2 to window))for(i in 0 until n) {
                    val frame=ControlFrame(sid,i+2)
                    collecting=false;stampCount=0
                    val start=stamps[0]
                    start[0]=System.nanoTime();start[1]=Debug.threadCpuTimeNanos()
                    start[2]=Debug.getThreadAllocSize().toLong();start[3]=Debug.getThreadAllocCount().toLong()
                    t.recordSent(frame,0);finish(samples[group][i],start)
                    if(group==1)check(t.accept("A1:$sid,${i+2},OK,0,0,0,0,0,0,0,0,0\n".toByteArray(),"127.0.0.1",8888,0)!=null)
                }
            }finally{Debug.stopMethodTracing();Debug.stopAllocCounting();session.close()}
            File(directory,"tracking-$label.csv").bufferedWriter().use { out->
                out.appendLine("group,sample,start_ns,end_ns,wall_ns,cpu_ns,allocated_bytes,allocated_objects,start_cpu_ns,start_allocated_bytes,end_allocated_bytes")
                for(g in samples.indices)for(i in 0 until n){val r=samples[g][i]
                    out.appendLine("${names[g]},${i+1},${r[0]},${r[1]},${r[1]-r[0]},${r[2]},${r[3]},${r[4]},${r[5]},${r[6]},${r[7]}")}
            }
            File(directory,"tracking-$label.json").writeText(JSONObject(mapOf("label" to label,"samples_each" to n,
                "allocation_supported" to allocationSupported,"probe_before_bytes" to probeBefore,"probe_after_bytes" to probeAfter,
                "trace_bytes" to trace.length(),"thread" to Thread.currentThread().name,"clock" to "protocol fixed0; method trace dual; wall System.nanoTime",
                "purpose" to "diagnostic, no acceptance; includes tracing/probe overhead; every sample retained")).toString(2))
            assertTrue(trace.isFile);assertEquals(400,sample+1)
        })
        Thread(task,"sumo-tracking-profile").apply{isDaemon=true;priority=Thread.MAX_PRIORITY;start()}
        task.get(140,TimeUnit.SECONDS)
    }
}
