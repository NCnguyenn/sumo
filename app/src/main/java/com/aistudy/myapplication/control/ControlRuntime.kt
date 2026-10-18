package com.aistudy.myapplication.control

import com.aistudy.myapplication.settings.ControllerSettings
import com.aistudy.myapplication.connection.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.UUID

class ControlRuntime(settings:ControllerSettings,initialKilled:Boolean=false,permission:()->Boolean={true},
    changed:(SessionSnapshot)->Unit,private val trace:(String)->Unit={},transport:RobotTransport=UdpRobotTransport(),
    private val cpuTimeNs:()->Long={0L}):AutoCloseable {
    private val actions=LinkedBlockingQueue<()->Unit>()
    private val closed=AtomicBoolean(false)
    private val gates=mutableListOf<()->Unit>()
    private var callbackNs:Long?=null
    private var cause="periodic"
    private var enqueuedNs=0L
    private var dequeuedNs=0L
    private var eventCpuNs=0L
    private var workStartNs=0L
    private var workCpuNs=0L
    private var waitStartNs=0L
    private var wakeNs=0L
    private var requestedWaitNs=0L
    private var previousWorkNs=0L
    private var previousTraceNs=0L
    private var traceSubmitNs=0L
    private val lostTrace=AtomicLong()
    private val traceExecutor=ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,ArrayBlockingQueue<Runnable>(8192),
        {task->Thread(task,"sumo-trace").apply{isDaemon=true}})
    private fun traceLater(value:()->String){
        val start=System.nanoTime()
        try{traceExecutor.execute{try{trace(value())}catch(_:Exception){lostTrace.incrementAndGet()}}}
        catch(_:RejectedExecutionException){lostTrace.incrementAndGet()}
        traceSubmitNs+=System.nanoTime()-start
    }
    private val measured=object:RobotTransport by transport {
        override fun send(bytes:ByteArray) {
            val beforeCpu=cpuTimeNs();val before=System.nanoTime()
            transport.send(bytes)
            val after=System.nanoTime();val afterCpu=cpuTimeNs()
            val callback=callbackNs ?: -1L;val reason=cause
            val queued=enqueuedNs;val dequeued=dequeuedNs
            val work=workStartNs;val cpu=if(callback>=0)eventCpuNs else workCpuNs
            val preparing=session.prepareStartNs;val prepared=session.prepareEndNs;val due=session.periodicDeadlineNs
            val waitStart=waitStartNs;val wake=wakeNs;val requested=requestedWaitNs
            val previousWork=previousWorkNs;val previousTrace=previousTraceNs;val pending=traceExecutor.queue.size
            traceLater{"wire t0_ns=$callback t1_ns=$before t2_ns=$after cause=$reason hex=${bytes.controlHex()}"+
                " enqueued_ns=$queued dequeued_ns=$dequeued work_ns=$work prepare_start_ns=$preparing prepare_end_ns=$prepared"+
                " pre_send_cpu_ns=${beforeCpu-cpu} send_cpu_ns=${afterCpu-beforeCpu} deadline_ns=$due"+
                " wait_start_ns=$waitStart wake_ns=$wake requested_wait_ns=$requested prev_work_ns=$previousWork"+
                " prev_trace_submit_ns=$previousTrace trace_pending=$pending"}
            callbackNs=null;cause="periodic"
        }
    }
    private val session:ControlSession=ControlSession(ControlReducer(settings).apply{if(initialKilled)kill()},measured,{System.nanoTime()/1_000_000},
        {generateSequence{UUID.randomUUID().toString().replace("-","").take(16)}.first{it!="0".repeat(16)}},permission,
        {line->traceLater{line}},{System.nanoTime()})
    private val worker=Thread({
        var previous:SessionSnapshot?=null
        var cachedSnapshot:SessionSnapshot?=null
        var snapshotRevision=-1L
        var publishedAt=0L
        val batch=ArrayList<()->Unit>()
        try {
            while(!closed.get()) {
                val idle=session.offline && session.safeToProceed() && gates.isEmpty()
                requestedWaitNs=if(idle)-1L else session.waitNanos()
                waitStartNs=System.nanoTime()
                val first=if(idle)actions.take() else actions.poll(requestedWaitNs,TimeUnit.NANOSECONDS)
                wakeNs=System.nanoTime();workStartNs=wakeNs;workCpuNs=cpuTimeNs();traceSubmitNs=0L
                first?.invoke()
                actions.drainTo(batch);batch.forEach{it()};batch.clear()
                session.pump()
                // Empty socket polls must not rebuild identical control/UI snapshots.
                if(snapshotRevision!=session.revision){cachedSnapshot=session.snapshot;snapshotRevision=session.revision}
                val state=checkNotNull(cachedSnapshot)
                val old=previous
                val immediate=old==null || state.control!=old.control || state.settings!=old.settings || state.phase!=old.phase ||
                    state.applied!=old.applied || state.configuration!=old.configuration || state.configurationStatus!=old.configurationStatus ||
                    (state.phase!=SessionPhase.ONLINE && state.message!=old.message)
                val now=System.nanoTime()
                if(immediate || (state!=old && now-publishedAt>=100_000_000L)){previous=state;publishedAt=now;changed(state)}
                if(session.safeToProceed() && gates.isNotEmpty()){val ready=gates.toList();gates.clear();ready.forEach{it()}}
                previousWorkNs=System.nanoTime()-workStartNs;previousTraceNs=traceSubmitNs
            }
        } catch(_:InterruptedException) {
            // Activity destruction wakes the owner; it sends its final STOP/C before closing.
        } catch(e:Exception){traceLater{"owner error ${e.message}"}}
        finally{try{session.stop()}finally{transport.close();traceExecutor.shutdown()}}
    },"sumo-control").apply{isDaemon=true;priority=Thread.MAX_PRIORITY;start()}
    fun connect(){enqueue{callbackNs=null;cause="connect";session.connect()}}
    fun post(event:ControlEvent,callbackAtNs:Long=System.nanoTime()){
        val queuedAt=System.nanoTime()
        enqueue{
            dequeuedNs=System.nanoTime();eventCpuNs=cpuTimeNs();enqueuedNs=queuedAt
            callbackNs=callbackNs?.let{minOf(it,callbackAtNs)} ?: callbackAtNs
            cause=event.toString();session.event(event)
        }
    }
    fun stop(then:()->Unit={}){
        val at=System.nanoTime()
        enqueue{dequeuedNs=System.nanoTime();eventCpuNs=cpuTimeNs();enqueuedNs=at;callbackNs=at;cause="stop-gate";session.stop();gates.add(then)}
    }
    fun applySettings(settings:ControllerSettings,then:()->Unit={}){enqueue{session.applySettings(settings);then()}}
    private fun enqueue(action:()->Unit){if(!closed.get())actions.offer(action)}
    internal fun flushTrace():Boolean=try{
        traceExecutor.submit{}.get(5,TimeUnit.SECONDS);lostTrace.get()==0L
    }catch(_:Exception){false}
    override fun close(){if(closed.compareAndSet(false,true))worker.interrupt()}
}
