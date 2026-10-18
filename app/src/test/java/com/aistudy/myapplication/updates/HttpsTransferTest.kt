package com.aistudy.myapplication.updates

import java.net.HttpURLConnection
import java.net.URL
import java.io.*
import org.junit.Assert.*
import org.junit.Test

class HttpsTransferTest {
    private class Response(val body:ByteArray,private val code:Int=200,private val redirect:String?=null):HttpURLConnection(URL("https://fixture.invalid")) {
        var closed=false
        override fun connect(){}
        override fun disconnect(){closed=true}
        override fun usingProxy()=false
        override fun getResponseCode()=code
        override fun getHeaderField(name:String)=if(name=="Location")redirect else null
        override fun getInputStream():InputStream=ByteArrayInputStream(body)
    }
    @Test fun A03_successStreamsExactBytesAndReportsProgress() {
        val response=Response(byteArrayOf(1,2,3));val out=ByteArrayOutputStream();val progress=mutableListOf<Long>()
        assertEquals(3L,HttpsTransfer{response}.transfer("https://fixture.invalid",3,out,progress={progress.add(it)}))
        assertArrayEquals(byteArrayOf(1,2,3),out.toByteArray());assertEquals(3L,progress.last());assertTrue(response.closed)
    }
    @Test fun A03_A04_cancelOversizeAndHttpErrorDoNotSucceed() {
        for((response,limit,cancel) in listOf(Triple(Response(byteArrayOf(1)),10L,true),Triple(Response(byteArrayOf(1,2)),1L,false),Triple(Response(byteArrayOf(),404),10L,false))) {
            assertThrows(IOException::class.java){HttpsTransfer{response}.transfer("https://fixture.invalid",limit,ByteArrayOutputStream(),{cancel})}
            assertTrue(response.closed)
        }
    }
    @Test fun A08_redirectCannotDowngradeHttpsOrLoopWithoutLimit() {
        val targets=mutableListOf<String>()
        assertThrows(IllegalArgumentException::class.java){HttpsTransfer{targets.add(it.toString());Response(byteArrayOf(),302,"http://fixture.invalid/apk")}
            .transfer("https://fixture.invalid",10,ByteArrayOutputStream())}
        assertEquals(1,targets.size)
        assertThrows(IOException::class.java){HttpsTransfer{Response(byteArrayOf(),302,"/again")}.transfer("https://fixture.invalid",10,ByteArrayOutputStream())}
    }
    @Test fun A08_G0RedirectHostCountAndTimeoutsAreEnforced() {
        assertThrows(IllegalArgumentException::class.java){HttpsTransfer{Response(byteArrayOf(),302,"https://other.invalid/file")}
            .transfer("https://fixture.invalid",10,ByteArrayOutputStream())}
        var calls=0
        assertThrows(IOException::class.java){HttpsTransfer{calls++;Response(byteArrayOf(),302,"/again")}.transfer("https://fixture.invalid",10,ByteArrayOutputStream())}
        assertEquals(4,calls)
        val response=Response(byteArrayOf(1))
        HttpsTransfer{response}.transfer("https://fixture.invalid",10,ByteArrayOutputStream())
        assertEquals(10000,response.connectTimeout);assertEquals(30000,response.readTimeout)
    }
}
