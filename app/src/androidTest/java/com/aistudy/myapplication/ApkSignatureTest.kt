package com.aistudy.myapplication

import androidx.test.platform.app.InstrumentationRegistry
import com.aistudy.myapplication.updates.AppUpdateManager
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class ApkSignatureTest {
    @Test fun A05_modifiedDexWithOriginalCertificateMustFailCryptographicVerification() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val original=File(requireNotNull(context.applicationInfo.sourceDir))
        val modified=File(context.cacheDir,"tampered-test.apk")
        val manager=AppUpdateManager(context){}
        try {
            assertEquals(manager.installed().signers,manager.verifiedSigners(original))
            val bytes=original.readBytes()
            val name="classes.dex";val offset=bytes.toString(Charsets.ISO_8859_1).indexOf(name)
            assertTrue(offset>=30)
            val header=ByteBuffer.wrap(bytes,offset-30,30).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(0x04034b50,header.int)
            val extra=ByteBuffer.wrap(bytes,offset-2,2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 65535
            val data=offset+name.length+extra
            bytes[data+20]=(bytes[data+20].toInt() xor 1).toByte();modified.writeBytes(bytes)
            assertThrows(IllegalArgumentException::class.java){manager.verifiedSigners(modified)}
        } finally{modified.delete();manager.close()}
    }
}
