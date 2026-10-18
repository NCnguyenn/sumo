package com.aistudy.myapplication.diagnostics

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class DiagnosticLogTest {
    @Test fun rejectedAckKeepsReceiverCauseAndLastTxIsNotPresentedAsCurrentIntent() {
        val log=log()
        log.record("5 ack seq=2 result=BAD pwm=153/153 rtt=1")
        assertEquals("IoT · receiver trả mã từ chối",log.summary().source)
        assertTrue(log.report("test").contains("TX cuối PWM"))
        assertTrue(log.previousFailure().orEmpty().contains("Receiver trả BAD"))
    }
    @Test fun failureIncludesOriginalAppEndpointAndHasHardUtf8ByteLimit() {
        val dir=Files.createTempDirectory("sumo-limit").toFile()
        val log=DiagnosticLog(dir,500,wallOffsetMs=0,appInfo="App test-build · API 37")
        log.record("0 connecting endpoint=127.0.0.1:8888 interval=30ms")
        repeat(520){log.record("$it event "+"ệ".repeat(600))}
        log.record("600 closed confirmed=false requested=0/0 reason=Offline · Hết hạn ACK 300 ms")
        val report=log.previousFailure()!!
        assertTrue(report.contains("App test-build · API 37"));assertTrue(report.contains("endpoint=127.0.0.1:8888"))
        assertTrue(report.toByteArray(Charsets.UTF_8).size<=DiagnosticLog.MAX_REPORT_BYTES)
    }
    private fun log(capacity:Int=12)=DiagnosticLog(Files.createTempDirectory("sumo-diagnostics").toFile(),capacity,wallOffsetMs=0)
    private fun wire(at:Long,body:String)="wire t2_ns=${at*1_000_000} hex="+body.toByteArray().joinToString(""){"%02x".format(it)}

    @Test fun countersDistinguishRequestedAcceptedAndDiscardedReplies() {
        val log=log()
        log.record("0 connecting")
        log.record(wire(10,"V1:0123456789abcdef,2,0,0,0,0|S:153,153,0,0,0\n"))
        log.record("12 receive host=127.0.0.1:8888 hex=6261640a")
        log.record("12 discard reason=FRAME: frame không hợp lệ")
        log.record("20 receive host=127.0.0.1:8888 hex=41310a")
        log.record("20 ack seq=2 result=OK pwm=153/153 rtt=10")
        val summary=log.summary()
        assertEquals(1,summary.tx);assertEquals(2,summary.rx);assertEquals(1,summary.discarded)
        assertEquals("153 / 153",summary.requested);assertEquals("153 / 153",summary.acknowledged)
        assertEquals(20L,summary.lastAckAtMs);assertEquals(10L,summary.rttMs)
        assertEquals(380L,summary.ackAge(400))
        assertTrue(log.snapshot().entries.any{it.group==DiagnosticGroup.PROTOCOL && "FRAME" in it.text})
    }
    @Test fun ringAndExportStayBoundedAndDoNotInventHardwareFaults() {
        val log=log(6)
        repeat(50){log.record("$it event "+"x".repeat(2000))}
        assertEquals(6,log.snapshot().entries.size)
        assertTrue(log.snapshot().entries.all{it.text.length<=600})
        val report=log.report("App test · endpoint test")
        assertTrue(report.length<7000);assertTrue(report.contains("ACK không chứng minh động cơ"))
        assertFalse(report.contains("nguồn yếu"));assertFalse(report.contains("driver hỏng"))
    }
    @Test fun failureSurvivesRestartAndCleanSessionDoesNotOverwriteIt() {
        val dir=Files.createTempDirectory("sumo-failure").toFile()
        val first=DiagnosticLog(dir,12,wallOffsetMs=0)
        first.record("0 connecting");first.record("310 closed confirmed=false reason=Offline · Hết hạn ACK 300 ms")
        assertTrue(first.summary().warning.orEmpty().contains("300 ms"))
        val failure=first.previousFailure()
        assertTrue(failure.orEmpty().contains("Chưa xác định"))
        first.record("400 connecting");first.record("410 closed confirmed=true reason=Offline · đã ACK đóng phiên")
        assertEquals(failure,DiagnosticLog(dir,12,wallOffsetMs=0).previousFailure())
        assertNull(first.summary().lastAckAtMs);assertNull(first.summary().acknowledged)
    }
    @Test fun corruptOrOversizedHistoryIsNotLoaded() {
        val dir=Files.createTempDirectory("sumo-history").toFile()
        java.io.File(dir,"previous-error.txt").writeText("x".repeat(300_000))
        assertNull(DiagnosticLog(dir).previousFailure())
    }
}
