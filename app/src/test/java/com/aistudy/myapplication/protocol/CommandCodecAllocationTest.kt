package com.aistudy.myapplication.protocol

import com.aistudy.myapplication.control.WheelPwm
import java.lang.management.ManagementFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandCodecAllocationTest {
    @Test fun repeatedDriveEncodingDoesNotAllocateParserObjectsForItsOwnOutput() {
        val memory = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        assertTrue("This JVM must support thread allocation accounting", memory.isThreadAllocatedMemorySupported)
        memory.isThreadAllocatedMemoryEnabled = true
        val frame = ControlFrame("0123456789abcdef", 123, payload = Payload.Drive(WheelPwm(153, 153)))
        var checksum = 0
        repeat(5_000) { checksum += CommandCodec.encodeControl(frame).size }
        @Suppress("DEPRECATION") // Keep the allocation regression compatible with Java 11 test runtimes.
        val thread = Thread.currentThread().id
        val before = memory.getThreadAllocatedBytes(thread)
        repeat(1_000) { checksum += CommandCodec.encodeControl(frame).size }
        val perFrame = (memory.getThreadAllocatedBytes(thread) - before) / 1_000
        println("drive_encode_allocated_bytes_per_frame=$perFrame checksum=$checksum")
        assertTrue("A short drive frame must stay within 1024 bytes of temporary allocations; actual=$perFrame", perFrame <= 1024)
    }
}
