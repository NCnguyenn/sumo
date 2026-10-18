package com.aistudy.myapplication.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudy.myapplication.settings.*
import com.aistudy.myapplication.updates.requireHttps

@Composable fun SettingsDialog(current:StoredSettings,onSave:(StoredSettings)->Unit,onClose:()->Unit) {
    val s=current.controller
    val values=remember{mutableStateMapOf("limit" to "${s.limitPercent}","sensitivity" to "${s.calibration.sensitivity}",
        "deadZone" to "${s.calibration.deadZone}","leftGain" to "${s.calibration.leftGain}","rightGain" to "${s.calibration.rightGain}",
        "turnPercent" to "${s.calibration.turnPercent}","duration" to "${s.turnDurationMs}","host" to s.host,"port" to "${s.port}",
        "interval" to "${s.sendIntervalMs}","template" to s.driveTemplate.replace("\n","\\n"),"feed" to current.updateFeed)}
    var input by remember{mutableStateOf(s.inputMode)};var weapon by remember{mutableStateOf(s.weaponMode)}
    var errors by remember{mutableStateOf<Map<String,String>>(emptyMap())}
    var confirmReset by remember{mutableStateOf(false)};var confirmFeed by remember{mutableStateOf<StoredSettings?>(null)}
    var tab by remember{mutableIntStateOf(0)}
    fun save() {
        val problems=mutableMapOf<String,String>()
        fun integer(key:String,range:IntRange):Int {
            val v=values.getValue(key).toIntOrNull()
            if(v==null || v !in range) problems[key]="Nhập số nguyên ${range.first}–${range.last}"
            return v ?: range.first
        }
        fun decimal(key:String,min:Float,max:Float):Float {
            val v=values.getValue(key).toFloatOrNull()
            if(v==null || !v.isFinite() || v !in min..max) problems[key]="Nhập số hữu hạn $min–$max (dấu chấm)"
            return v ?: min
        }
        val candidate=StoredSettings(ControllerSettings(integer("limit",30..100),DriveCalibration(decimal("sensitivity",.25f,2f),
            decimal("deadZone",0f,.30f),decimal("leftGain",.5f,1f),decimal("rightGain",.5f,1f),integer("turnPercent",30..100)),
            integer("duration",1..2000),weapon,input,values.getValue("host"),integer("port",1..65535),integer("interval",20..50),
            values.getValue("template").replace("\\n","\n")),values.getValue("feed"))
        try{ControllerSettings(host=candidate.controller.host).validated()}catch(_:IllegalArgumentException){problems["host"]="IPv4 4 octet hợp lệ; không wildcard/multicast/broadcast"}
        if(candidate.controller.driveTemplate !in listOf(ControllerSettings.S_TEMPLATE,ControllerSettings.D_TEMPLATE)) problems["template"]="Chỉ S:{L},{R},{B},{W},{K}\\n hoặc alias D"
        if(candidate.updateFeed.isNotEmpty())try{requireHttps(candidate.updateFeed)}catch(_:IllegalArgumentException){problems["feed"]="URL HTTPS hợp lệ; để trống nếu chưa có hosting"}
        errors=problems
        if(problems.isNotEmpty())tab=when(problems.keys.first()){ "feed"->2;"limit","sensitivity","deadZone","leftGain","rightGain","turnPercent"->1;else->0 }
        if(problems.isEmpty()) {
            if(candidate.updateFeed!=current.updateFeed)confirmFeed=candidate else onSave(candidate)
        }
    }
    val labels=linkedMapOf("limit" to "Limiter (30–100%)","sensitivity" to "Độ nhạy joystick (0.25–2.0)","deadZone" to "Vùng chết (0–0.30)",
        "leftGain" to "Hệ số bánh trái (0.50–1.00)","rightGain" to "Hệ số bánh phải (0.50–1.00)","turnPercent" to "Mức quay (30–100%)",
        "duration" to "Thời gian quay (1–2000 ms)","host" to "IP receiver bản thử","port" to "UDP port (1–65535)","interval" to "Chu kỳ gửi (20–50 ms)",
        "template" to "Template (\\n là một LF)","feed" to "URL HTTPS metadata cập nhật (chưa có: để trống)")
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(.88f).widthIn(max=820.dp).fillMaxHeight(.96f),
            shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface,border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
            Column(Modifier.padding(horizontal=14.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("Cấu hình",fontSize=22.sp,fontWeight=FontWeight.Bold)
                    Text("Đã đóng phiên\nđiều khiển",Modifier.weight(1f),color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp,lineHeight=13.sp)
                    for((index,title) in listOf("Chung","Hiệu chỉnh","Cập nhật").withIndex()) {
                        val tag=listOf("settings-general","settings-calibration","settings-update")[index]
                        OutlinedButton(onClick={tab=index},modifier=Modifier.height(48.dp).testTag(tag),shape=RoundedCornerShape(9.dp),
                            colors=ButtonDefaults.outlinedButtonColors(containerColor=if(tab==index)MaterialTheme.colorScheme.primary.copy(alpha=.12f) else androidx.compose.ui.graphics.Color.Transparent),
                            contentPadding=PaddingValues(horizontal=12.dp)){Text(title,fontSize=11.sp,color=if(tab==index)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)}
                    }
                }
                HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.35f))
                key(tab) {Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    val rows=when(tab) {
                        0->listOf(listOf("host","port"),listOf("interval","duration"),listOf("template"))
                        1->listOf(listOf("limit","turnPercent"),listOf("sensitivity","deadZone"),listOf("leftGain","rightGain"))
                        else->listOf(listOf("feed"))
                    }
                    for(pair in rows)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            for(key in pair)Column(Modifier.weight(1f)) {
                                OutlinedTextField(values.getValue(key),{values[key]=it},
                                    label={Text(labels.getValue(key),fontSize=11.sp)},
                                    modifier=Modifier.fillMaxWidth().testTag("field-$key"),isError=key in errors,singleLine=true,
                                    textStyle=LocalTextStyle.current.copy(fontSize=14.sp),
                                    shape=RoundedCornerShape(10.dp))
                                errors[key]?.let{Text(it,color=MaterialTheme.colorScheme.error,fontSize=11.sp)}
                            }
                    }
                    if(tab==0)Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick={input=if(input==InputMode.D_PAD)InputMode.JOYSTICK else InputMode.D_PAD},
                            modifier=Modifier.weight(1f).height(48.dp).testTag("input-mode"),shape=RoundedCornerShape(10.dp)) {Text("Điều hướng: ${if(input==InputMode.D_PAD)"D-Pad" else "Joystick"}",fontSize=12.sp)}
                        OutlinedButton(onClick={weapon=if(weapon==WeaponMode.HOLD)WeaponMode.TOGGLE else WeaponMode.HOLD},
                            modifier=Modifier.weight(1f).height(48.dp).testTag("weapon-mode"),shape=RoundedCornerShape(10.dp)) {Text("Vũ khí: ${if(weapon==WeaponMode.HOLD)"Giữ · HOLD" else "Bật / tắt · TOGGLE"}",fontSize=12.sp)}
                    }
                    if(tab!=0)Text(when(tab){1->"Lưu để áp dụng độ nhạy và hiệu chỉnh; không cần build lại.";
                        else->"Chỉ nhận HTTPS. Chưa có hosting: để trống. Nút Cập nhật trên màn chính dùng để kiểm tra và tải APK."},
                        color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp)
                }}
                HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=.35f))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick={confirmReset=true},modifier=Modifier.testTag("reset-settings")){Text("Khôi phục mặc định",fontSize=12.sp)}
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick=onClose,shape=RoundedCornerShape(10.dp)){Text("Hủy")}
                    Button(onClick=::save,modifier=Modifier.testTag("save-settings"),shape=RoundedCornerShape(10.dp)){Text("Lưu toàn bộ")}
                }
            }
        }
    }
    if(confirmReset)AlertDialog(onDismissRequest={confirmReset=false},title={Text("Khôi phục mặc định?")},text={Text("Đổi các thông số về mặc định; giữ URL cập nhật và luôn dừng điều khiển.")},
        confirmButton={TextButton(onClick={onSave(StoredSettings(updateFeed=current.updateFeed))}){Text("Xác nhận")}},dismissButton={TextButton(onClick={confirmReset=false}){Text("Hủy")}})
    confirmFeed?.let{candidate->AlertDialog(onDismissRequest={confirmFeed=null},title={Text("Đổi nguồn cập nhật?")},text={Text(candidate.updateFeed.ifEmpty{"Xóa địa chỉ cập nhật"})},
        confirmButton={TextButton(onClick={onSave(candidate)}){Text("Xác nhận nguồn")}},dismissButton={TextButton(onClick={confirmFeed=null}){Text("Hủy")}})}
}
