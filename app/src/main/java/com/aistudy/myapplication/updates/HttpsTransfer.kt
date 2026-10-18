package com.aistudy.myapplication.updates

import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStream
import java.io.IOException

class HttpsTransfer(private val open:(URL)->HttpURLConnection={it.openConnection() as HttpURLConnection}) {
    fun transfer(url:String,limit:Long,output:OutputStream,cancelled:()->Boolean={false},progress:(Long)->Unit={}):Long {
        require(limit>0)
        var next=requireHttps(url)
        val trustedHost=URL(next).host
        repeat(4) { hop->
            val connection=open(URL(next))
            try {
                connection.instanceFollowRedirects=false;connection.connectTimeout=10000;connection.readTimeout=30000
                if(cancelled()) throw IOException("Đã hủy tải")
                val code=connection.responseCode
                if(code in listOf(301,302,303,307,308)) {
                    if(hop==3) throw IOException("Quá nhiều redirect")
                    val location=connection.getHeaderField("Location") ?: throw IOException("Redirect thiếu địa chỉ")
                    next=requireHttps(URL(URL(next),location).toString())
                    require(URL(next).host.equals(trustedHost,ignoreCase=true)){"Redirect sang host chưa được duyệt"}
                } else {
                    if(code!=200) throw IOException("Máy chủ trả HTTP $code")
                    var count=0L
                    connection.inputStream.use { input->
                        val buffer=ByteArray(16384)
                        while(true) {
                            if(cancelled()) throw IOException("Đã hủy tải")
                            val read=input.read(buffer)
                            if(read<0) break
                            count+=read
                            if(count>limit) throw IOException("Tệp vượt kích thước cho phép")
                            output.write(buffer,0,read);progress(count)
                        }
                    }
                    if(cancelled()) throw IOException("Đã hủy tải")
                    return count
                }
            } finally {connection.disconnect()}
        }
        throw IOException("Không tải được dữ liệu")
    }
}
