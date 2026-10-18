package com.aistudy.myapplication.control

import org.junit.Assert.*
import org.junit.Test

class ControlTraceTest {
    @Test fun traceHexKeepsUnsignedBytesAndLfWithoutFormatterAllocations() {
        assertEquals("00010a0f107f80ff",byteArrayOf(0,1,10,15,16,127,128.toByte(),255.toByte()).controlHex())
        assertEquals("",byteArrayOf().controlHex())
        val bytes=ByteArray(256){it.toByte()}
        assertArrayEquals(bytes,bytes.controlHex().chunked(2).map{it.toInt(16).toByte()}.toByteArray())
    }
}
