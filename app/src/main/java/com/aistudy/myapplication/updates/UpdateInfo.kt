package com.aistudy.myapplication.updates

import java.net.URI

data class InstalledBuild(val applicationId:String,val versionCode:Long,val sdk:Int,val signers:Set<String>)
data class ArchiveBuild(val applicationId:String,val versionCode:Long,val minSdk:Int,val signers:Set<String>)
data class UpdateInfo(val schemaVersion:Int,val applicationId:String,val versionCode:Long,val versionName:String,
    val minSdk:Int,val apkUrl:String,val sizeBytes:Long,val sha256:String,val releaseNotes:String,val channel:String) {
    fun validate(installed:InstalledBuild):UpdateInfo = apply {
        require(schemaVersion==1 && channel=="test"){"Metadata/schema/kênh test không hỗ trợ"}
        require(applicationId==installed.applicationId){"Sai danh tính ứng dụng"}
        require(versionCode>installed.versionCode && versionCode>0){"Không có bản mới hơn"}
        require(versionName.isNotBlank() && versionName.length<=100 && releaseNotes.length<=10000){"Tên/ghi chú phiên bản không hợp lệ"}
        require(minSdk in 1..installed.sdk){"Bản mới không tương thích Android hiện tại"}
        require(sizeBytes in 1..100L*1024*1024){"APK phải trong giới hạn 100 MiB"}
        require(sha256.matches(Regex("[0-9a-f]{64}"))){"SHA-256 không hợp lệ"}
        requireHttps(apkUrl)
    }
    fun verifyArchive(installed:InstalledBuild,archive:ArchiveBuild,actualSize:Long,actualHash:String) {
        validate(installed)
        require(actualSize==sizeBytes && actualHash==sha256){"APK thiếu dữ liệu hoặc sai SHA-256"}
        require(archive.applicationId==applicationId && archive.versionCode==versionCode && archive.minSdk==minSdk){"Danh tính/phiên bản/minSdk APK khác metadata"}
        require(archive.signers.isNotEmpty() && installed.signers.isNotEmpty() && archive.signers==installed.signers){"Chữ ký APK khác ứng dụng đang cài"}
    }
}
fun requireHttps(url:String):String {
    val uri=try{URI(url)}catch(e:Exception){throw IllegalArgumentException("URL HTTPS không hợp lệ",e)}
    require(uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.rawUserInfo==null && uri.rawFragment==null && uri.port in -1..65535){"Chỉ dùng URL HTTPS, không chứa tài khoản hoặc fragment"}
    return url
}
