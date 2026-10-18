package com.aistudy.myapplication.updates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.compose.runtime.mutableStateOf

class InstallResultReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val status=intent.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE)
        val message=when(status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION->"Android yêu cầu xác nhận cài đặt"
            PackageInstaller.STATUS_SUCCESS->"Android báo cài thành công; mở app để kiểm tra phiên bản thực tế"
            PackageInstaller.STATUS_FAILURE_ABORTED->"Đã hủy cài đặt; ứng dụng/cấu hình hiện tại được giữ"
            else->"Android không cài được (mã $status): ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "không có chi tiết"}"
        }
        context.getSharedPreferences("sumo_install_result",Context.MODE_PRIVATE).edit().putString("message",message).apply()
        result.value=message
        if(status==PackageInstaller.STATUS_PENDING_USER_ACTION)pending.value=intent.getParcelableExtra(Intent.EXTRA_INTENT,Intent::class.java)
    }
    companion object {
        val result=mutableStateOf("")
        val pending=mutableStateOf<Intent?>(null)
    }
}
