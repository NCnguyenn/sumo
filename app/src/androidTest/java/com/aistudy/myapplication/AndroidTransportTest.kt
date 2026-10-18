package com.aistudy.myapplication

import android.os.Build
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudy.myapplication.connection.UdpRobotTransport
import org.junit.Assert.*
import org.junit.Test

class AndroidTransportTest {
    @Test fun I02_I12_emptyReceivePollDoesNotBlockUrgentSendOwner() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        if(Build.VERSION.SDK_INT>=37)InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName,"android.permission.ACCESS_LOCAL_NETWORK")
        val transport=UdpRobotTransport()
        try {
            transport.open("10.0.2.2",8888)
            val start=System.nanoTime()
            repeat(1000){assertNull(transport.receive())}
            val elapsed=(System.nanoTime()-start)/1e6
            Log.i("SumoTransportTest","1000 empty polls: $elapsed ms")
            assertTrue("Receive polling must be nonblocking, took $elapsed ms",elapsed<500)
        } finally{transport.close()}
    }
}
