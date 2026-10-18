package com.aistudy.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudy.myapplication.updates.UpdateStatus

@Composable fun AppUpdateDialog(version:String,feed:String,state:UpdateStatus,installResult:String,
    onCheck:()->Unit,onDownload:()->Unit,onInstall:()->Unit,onCancel:()->Unit,onClose:()->Unit) {
    val sourceMatches=state.feed==feed
    Dialog(onDismissRequest={onCancel();onClose()},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(.72f).widthIn(max=660.dp).padding(vertical=12.dp).heightIn(max=350.dp),
            shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface,border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("↓ Cập nhật ứng dụng",Modifier.weight(1f),fontSize=21.sp,lineHeight=25.sp,fontWeight=FontWeight.Bold)
                }
                Text("Ⅱ Đã đóng phiên điều khiển · không tự phát lại lệnh chạy",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp,lineHeight=12.sp)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)){Text("ĐANG CÀI",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp,lineHeight=11.sp);Text(version,fontSize=19.sp,lineHeight=23.sp,fontWeight=FontWeight.SemiBold)}
                    Text("→",color=MaterialTheme.colorScheme.primary,fontSize=24.sp,lineHeight=26.sp)
                    Column(Modifier.weight(1f)){Text("BẢN THỬ MỚI",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp,lineHeight=11.sp)
                        Text(state.info?.takeIf{sourceMatches}?.let{"${it.versionName} (${it.versionCode})"} ?: "Chưa kiểm tra",fontSize=16.sp,lineHeight=20.sp,fontWeight=FontWeight.SemiBold)}
                }
                HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.35f))
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(when {
                        feed.isBlank()->"Chưa có địa chỉ cập nhật. Nhập URL HTTPS trong Cấu hình."
                        state.info!=null && !sourceMatches->"Nguồn cập nhật đã đổi. Nhấn Kiểm tra để nạp metadata mới."
                        state==UpdateStatus()->"Nhấn Kiểm tra để tìm bản test mới."
                        else->state.message
                    },
                        Modifier.testTag("update-status"),fontSize=14.sp,lineHeight=18.sp)
                    if(feed.isNotBlank())Text("Nguồn: $feed",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp,lineHeight=14.sp)
                    if(installResult.isNotEmpty())Text(installResult,fontSize=12.sp,lineHeight=16.sp)
                    state.info?.takeIf{sourceMatches}?.let{Text("Bản mới ${it.versionName} (${it.versionCode})",fontWeight=FontWeight.SemiBold)
                        Text(it.releaseNotes,fontSize=13.sp,lineHeight=17.sp)}
                    if(state.busy){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Đang xử lý…",fontSize=11.sp,lineHeight=14.sp);Text("${state.progress}%",color=MaterialTheme.colorScheme.primary,fontSize=13.sp,lineHeight=17.sp)}
                        LinearProgressIndicator(progress={state.progress/100f},modifier=Modifier.fillMaxWidth())}
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick=onCheck,enabled=!state.busy && feed.isNotBlank(),shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=10.dp)){Text("Kiểm tra",fontSize=11.sp,lineHeight=14.sp)}
                    OutlinedButton(onClick=onDownload,enabled=sourceMatches && state.info!=null && !state.busy,shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=10.dp)){Text("Tải APK",fontSize=11.sp,lineHeight=14.sp)}
                    Button(onClick=onInstall,enabled=sourceMatches && state.ready && !state.busy,shape=RoundedCornerShape(10.dp),contentPadding=PaddingValues(horizontal=10.dp)){Text("Cài qua Android",fontSize=11.sp,lineHeight=14.sp)}
                    Spacer(Modifier.weight(1f))
                    if(state.busy)TextButton(onClick=onCancel){Text("Hủy",fontSize=12.sp,lineHeight=15.sp)}
                    TextButton(onClick={onCancel();onClose()}){Text("Đóng",fontSize=12.sp,lineHeight=15.sp)}
                }
                Text("Android sẽ yêu cầu xác nhận trước khi cài đặt.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp,lineHeight=12.sp)
            }
        }
    }
}
