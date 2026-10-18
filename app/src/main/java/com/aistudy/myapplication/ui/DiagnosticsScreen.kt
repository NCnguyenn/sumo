package com.aistudy.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudy.myapplication.diagnostics.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun DiagnosticsScreen(snapshot:DiagnosticSnapshot,previous:String?,metadata:String,nowMs:Long,requested:String,
    onClose:()->Unit,onCopy:(Boolean)->Unit,onExport:(Boolean)->Unit) {
    var filter by remember{mutableStateOf<DiagnosticGroup?>(null)}
    var history by remember{mutableStateOf(false)}
    val colors=MaterialTheme.colorScheme
    val shape=RoundedCornerShape(12.dp)
    val summary=snapshot.summary
    val entries=remember(snapshot.entries,filter){snapshot.entries.filter{filter==null || it.group==filter}}
    val scroll=rememberLazyListState()
    val time=remember{SimpleDateFormat("HH:mm:ss.SSS",Locale.ROOT)}
    BackHandler(onBack=onClose)
    LaunchedEffect(entries.lastOrNull()){if(entries.isNotEmpty())scroll.scrollToItem(entries.lastIndex)}
    @Composable fun Metric(title:String,value:String,modifier:Modifier=Modifier) {
        Column(modifier.fillMaxHeight().background(colors.surface,shape).border(1.dp,colors.outline.copy(alpha=.5f),shape).padding(horizontal=8.dp,vertical=5.dp),
            verticalArrangement=Arrangement.Center) {
            Text(title,color=colors.onSurfaceVariant,fontSize=10.sp,lineHeight=12.sp,maxLines=1)
            Text(value,color=colors.onSurface,fontSize=14.sp,lineHeight=17.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact=maxHeight<400.dp
    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().padding(if(compact)6.dp else 12.dp).testTag("diagnostics-screen"),
        verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 6.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick=onClose,modifier=Modifier.height(48.dp).testTag("close-diagnostics"),contentPadding=PaddingValues(horizontal=12.dp)){Text("‹ Trở về")}
            Text("Chẩn đoán",fontSize=23.sp,lineHeight=26.sp,fontWeight=FontWeight.Bold,color=colors.onSurface)
            Text("Ⅱ Điều khiển đã dừng",fontSize=11.sp,lineHeight=14.sp,color=colors.primary,modifier=Modifier.weight(1f))
            OutlinedButton(onClick={history=!history},enabled=previous!=null,modifier=Modifier.testTag("diagnostics-history")) {
                Text(if(history)"Phiên lỗi trước ▾" else if(previous!=null)"Phiên hiện tại ▾" else "Chưa có phiên lỗi trước",fontSize=11.sp,lineHeight=14.sp)
            }
        }
        if(!history)Row(Modifier.fillMaxWidth().height(58.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Metric("APP YÊU CẦU","PWM $requested",Modifier.weight(1.2f))
            Metric("ACK CUỐI (IoT)","PWM ${summary.acknowledged ?: "chưa có"}",Modifier.weight(1.6f))
            Metric("TUỔI ACK",summary.ackAge(nowMs)?.let{"$it ms"} ?: "Chưa có",Modifier.weight(.9f))
            Metric("TX","${summary.tx}",Modifier.weight(.55f))
            Metric("RX","${summary.rx}",Modifier.weight(.55f))
            Metric("BỊ LOẠI","${summary.discarded}",Modifier.weight(.6f))
        }
        Text(if(history)"Nhật ký phiên lỗi trước · thông tin và cấu hình nằm trong bản ghi" else "!  Nguồn lỗi: ${summary.source}${summary.warning?.let{" · $it"} ?: ""}",
            color=Color(0xFFF6AD55),fontSize=11.sp,lineHeight=14.sp,maxLines=2,
            modifier=Modifier.fillMaxWidth().background(Color(0xFFF6AD55).copy(alpha=.09f),shape).padding(horizontal=10.dp,vertical=7.dp))
        if(!history)Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
            FilterChip(selected=filter==null,onClick={filter=null},label={Text("Tất cả",fontSize=11.sp,lineHeight=14.sp)},modifier=Modifier.testTag("diagnostics-all"))
            for(group in DiagnosticGroup.entries)FilterChip(selected=filter==group,onClick={filter=group},
                label={Text(group.label,fontSize=11.sp,lineHeight=14.sp)},modifier=Modifier.testTag("diagnostics-${group.name.lowercase()}"))
            Spacer(Modifier.weight(1f));Text("Tối đa 240 dòng / phiên",color=colors.onSurfaceVariant,fontSize=9.sp,lineHeight=11.sp)
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF08111E),shape).border(1.dp,colors.outline.copy(alpha=.5f),shape).padding(10.dp)) {
            if(history)SelectionContainer {Text(previous.orEmpty(),Modifier.verticalScroll(rememberScrollState()).testTag("diagnostics-log"),
                color=colors.onSurface,fontSize=11.sp,lineHeight=16.sp,fontFamily=FontFamily.Monospace)}
            else if(entries.isEmpty())Text("Chưa có sự kiện trong nhóm này. Kết nối receiver để ghi nhật ký hoạt động.",color=colors.onSurfaceVariant,fontSize=12.sp)
            else LazyColumn(state=scroll,modifier=Modifier.fillMaxSize().testTag("diagnostics-log"),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                items(entries) {entry->
                    val accent=when(entry.group){DiagnosticGroup.APP->colors.primary;DiagnosticGroup.NETWORK->Color(0xFFF6AD55);
                        DiagnosticGroup.PROTOCOL->Color(0xFFFF8790);DiagnosticGroup.IOT->Color(0xFF60A5FA)}
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(time.format(Date(entry.atMs)),Modifier.width(128.dp),color=colors.onSurfaceVariant,fontSize=10.sp,lineHeight=13.sp,maxLines=1,fontFamily=FontFamily.Monospace)
                        Text(entry.group.label,Modifier.width(82.dp),color=accent,fontSize=10.sp,lineHeight=13.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Monospace)
                        Text(entry.text,color=if(entry.warning)Color(0xFFF6AD55) else colors.onSurface,fontSize=11.sp,lineHeight=15.sp,fontFamily=FontFamily.Monospace)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min=58.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text("Chưa đủ dữ liệu quy lỗi cho app, mạng hay IoT.",color=colors.onSurfaceVariant,fontSize=10.sp,lineHeight=12.sp)
                Text("ACK không chứng minh động cơ thực sự quay.",color=colors.onSurfaceVariant,fontSize=10.sp,lineHeight=12.sp)
                Text(if(history)"Thông tin phiên lỗi được giữ nguyên trong nhật ký" else metadata,color=colors.onSurfaceVariant,fontSize=9.sp,lineHeight=11.sp,maxLines=1)
            }
            OutlinedButton(onClick={onCopy(history)},modifier=Modifier.height(48.dp).testTag("copy-diagnostics")){Text("Sao chép",fontSize=11.sp,lineHeight=14.sp)}
            Button(onClick={onExport(history)},modifier=Modifier.height(48.dp).testTag("export-diagnostics")){Text("Xuất nhật ký",fontSize=11.sp,lineHeight=14.sp)}
        }
    }
    }
}
