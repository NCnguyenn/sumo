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
        Surface(Modifier.fillMaxWidth(.88f).padding(vertical=12.dp).heightIn(max=340.dp),
            shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Cập nhật APK test",Modifier.weight(1f),fontSize=20.sp,fontWeight=FontWeight.Bold)
                    Text("Đang cài $version",color=MaterialTheme.colorScheme.primary,fontSize=12.sp)
                }
                Text("Điều khiển đã dừng. Ứng dụng không tự phát lại lệnh chạy.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp)
                HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.35f))
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(when {
                        feed.isBlank()->"Chưa có địa chỉ cập nhật. Nhập URL HTTPS trong Cấu hình."
                        state.info!=null && !sourceMatches->"Nguồn cập nhật đã đổi. Nhấn Kiểm tra để nạp metadata mới."
                        state==UpdateStatus()->"Nhấn Kiểm tra để tìm bản test mới."
                        else->state.message
                    },
                        Modifier.testTag("update-status"),fontSize=14.sp)
                    if(feed.isNotBlank())Text("Nguồn: $feed",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp)
                    if(installResult.isNotEmpty())Text(installResult,fontSize=12.sp)
                    state.info?.takeIf{sourceMatches}?.let{Text("Bản mới ${it.versionName} (${it.versionCode})",fontWeight=FontWeight.SemiBold)
                        Text(it.releaseNotes,fontSize=13.sp)}
                    if(state.busy)LinearProgressIndicator(progress={state.progress/100f},modifier=Modifier.fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick=onCheck,enabled=!state.busy && feed.isNotBlank(),shape=RoundedCornerShape(10.dp)){Text("Kiểm tra",fontSize=12.sp)}
                    OutlinedButton(onClick=onDownload,enabled=sourceMatches && state.info!=null && !state.busy,shape=RoundedCornerShape(10.dp)){Text("Tải APK",fontSize=12.sp)}
                    Button(onClick=onInstall,enabled=sourceMatches && state.ready && !state.busy,shape=RoundedCornerShape(10.dp)){Text("Cài qua Android",fontSize=12.sp)}
                    Spacer(Modifier.weight(1f))
                    if(state.busy)TextButton(onClick=onCancel){Text("Hủy",fontSize=12.sp)}
                    TextButton(onClick={onCancel();onClose()}){Text("Đóng",fontSize=12.sp)}
                }
            }
        }
    }
}
