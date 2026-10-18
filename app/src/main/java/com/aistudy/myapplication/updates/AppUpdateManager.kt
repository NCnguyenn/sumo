package com.aistudy.myapplication.updates

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

data class UpdateStatus(val message:String="Chưa có địa chỉ phân phối. Nhập URL HTTPS trong Cấu hình.",
    val busy:Boolean=false,val info:UpdateInfo?=null,val progress:Int=0,val ready:Boolean=false,val feed:String?=null)

class AppUpdateManager(private val context:Context,private val changed:(UpdateStatus)->Unit) {
    private val io=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private val cancelled=AtomicBoolean(false)
    private val apk=File(context.cacheDir,"sumo-update.apk")
    @Volatile private var state=UpdateStatus()
    @Volatile private var alive=true
    private fun publish(next:UpdateStatus) {state=next;main.post{if(alive)changed(next)}}
    private fun execute(action:()->Unit) {
        if(state.busy)return
        cancelled.set(false);publish(state.copy(busy=true,ready=false))
        io.execute {try{action()}catch(e:Exception){apk.delete();publish(state.copy(message=e.message ?: "Cập nhật thất bại",busy=false,ready=false))}}
    }
    fun cancel(){cancelled.set(true)}
    fun close(){alive=false;cancel();io.shutdownNow()}
    fun installed():InstalledBuild {
        val p=context.packageManager.getPackageInfo(context.packageName,PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        return InstalledBuild(context.packageName,p.longVersionCode,Build.VERSION.SDK_INT,signers(p.signingInfo?.apkContentsSigners))
    }
    fun versionLabel():String {
        val p=context.packageManager.getPackageInfo(context.packageName,0)
        return "${p.versionName} (${p.longVersionCode})"
    }
    fun check(feed:String) {
        if(feed.isBlank()){publish(UpdateStatus());return}
        execute {
            val out=ByteArrayOutputStream()
            HttpsTransfer().transfer(requireHttps(feed),65536,out,{cancelled.get()})
            val info=parseMetadata(out.toString(Charsets.UTF_8)).validate(installed())
            require(java.net.URI(info.apkUrl).host.equals(java.net.URI(feed).host,ignoreCase=true)){"APK phải cùng host HTTPS đã duyệt với metadata"}
            apk.delete();publish(UpdateStatus("Có bản test mới; xem ghi chú trước khi tải",info=info,feed=feed))
        }
    }
    fun download(feed:String) {
        val checked=state
        val info=checked.info ?: return
        execute {
            require(feed==checked.feed){"Nguồn cập nhật đã đổi; nhấn Kiểm tra để nạp metadata mới"}
            info.validate(installed());apk.delete()
            var last=-1
            apk.outputStream().use {out->HttpsTransfer().transfer(info.apkUrl,info.sizeBytes,out,{cancelled.get()}) { count->
                val percent=(count*100/info.sizeBytes).toInt()
                if(percent!=last){last=percent;publish(UpdateStatus("Đang tải APK test",true,info,percent,feed=checked.feed))}
            }}
            verify(apk,info)
            if(cancelled.get())throw java.io.IOException("Đã hủy tải")
            publish(UpdateStatus("Đã kiểm tra hash, package, phiên bản và chứng thư; Android sẽ kiểm tra chữ ký khi cài",info=info,progress=100,ready=true,feed=checked.feed))
        }
    }
    fun install(feed:String) {
        val checked=state
        val info=checked.info ?: return
        if(!checked.ready)return
        execute {
            require(feed==checked.feed){"Nguồn cập nhật đã đổi; nhấn Kiểm tra để nạp metadata mới"}
            requestInstall(apk,info)
            publish(state.copy(message="Đã gửi phiên cài; chờ xác nhận/kết quả Android",busy=false,ready=false))
        }
    }
    // The caller ends the control session before entering the platform installer.
    fun requestInstall(file:File,info:UpdateInfo):Int {
            check(context.packageManager.canRequestPackageInstalls()){"Cần cho phép cài từ ứng dụng này trong Cài đặt Android"}
            verify(file,info)
            val installer=context.packageManager.packageInstaller
            val params=PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName);setSize(file.length())
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
                setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE)
            }
            val id=installer.createSession(params)
            try {
                installer.openSession(id).use {session->
                    session.openWrite("base.apk",0,file.length()).use {out->file.inputStream().use{it.copyTo(out)};session.fsync(out)}
                    if(cancelled.get())throw java.io.IOException("Đã hủy cài")
                    val intent=Intent(context,InstallResultReceiver::class.java).setAction("${context.packageName}.INSTALL_RESULT")
                    val pending=PendingIntent.getBroadcast(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                    session.commit(pending.intentSender)
                }
                return id
            } catch(e:Exception){installer.abandonSession(id);throw e}
    }
    fun verify(file:File,info:UpdateInfo) {
        val hash=file.inputStream().use {input->val digest=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(16384)
            while(true){val read=input.read(buffer);if(read<0)break;digest.update(buffer,0,read)};digest.digest().hex()}
        val p=requireNotNull(context.packageManager.getPackageArchiveInfo(file.path,PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))){"APK không đọc được"}
        val app=requireNotNull(p.applicationInfo){"APK thiếu manifest"}
        val archive=ArchiveBuild(p.packageName,p.longVersionCode,app.minSdkVersion,verifiedSigners(file))
        info.verifyArchive(installed(),archive,file.length(),hash)
        require(p.versionName==info.versionName){"Tên phiên bản APK khác metadata"}
    }
    fun verifiedSigners(file:File):Set<String> {
        val parsed=requireNotNull(context.packageManager.getPackageArchiveInfo(file.path,PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())))
        return signers(parsed.signingInfo?.apkContentsSigners)
    }
    companion object {
        private fun ByteArray.hex()=joinToString(""){"%02x".format(it.toInt() and 255)}
        private fun signers(values:Array<android.content.pm.Signature>?):Set<String> = values?.map{MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).hex()}?.toSet() ?: emptySet()
        fun parseMetadata(raw:String):UpdateInfo {
            require(raw.toByteArray(Charsets.UTF_8).size<=65536){"Metadata quá dài"}
            val p=JSONObject(raw)
            fun text(key:String)=p.get(key) as? String ?: throw IllegalArgumentException("Metadata $key phải là chuỗi")
            fun number(key:String):Long {
                val v=p.get(key);require(v is Number){"Metadata $key phải là số nguyên"}
                return v.toString().toLongOrNull() ?: throw IllegalArgumentException("Số nguyên metadata không hợp lệ")
            }
            fun integer(key:String):Int {val v=number(key);require(v in 0..Int.MAX_VALUE);return v.toInt()}
            return UpdateInfo(integer("schemaVersion"),text("applicationId"),number("versionCode"),text("versionName"),integer("minSdk"),
                text("apkUrl"),number("sizeBytes"),text("sha256"),text("releaseNotes"),text("channel"))
        }
    }
}
