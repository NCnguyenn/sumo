package com.aistudy.myapplication.ui

import android.view.MotionEvent
import androidx.compose.runtime.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.aistudy.myapplication.control.*
import com.aistudy.myapplication.settings.*
import com.aistudy.myapplication.diagnostics.DiagnosticSummary

@OptIn(ExperimentalComposeUiApi::class,ExperimentalMaterial3Api::class)
@Composable
fun ControllerScreen(state:ControlState,settings:ControllerSettings,connection:String,onEvent:(ControlEvent)->Unit,
    onConnect:()->Unit,onSettings:()->Unit,onUpdates:()->Unit,onLimiterSaved:()->Unit={},receiver:ControlState?=null,
    diagnostics:DiagnosticSummary=DiagnosticSummary(),nowMs:Long=System.nanoTime()/1_000_000L,onDiagnostics:()->Unit={}) {
    val event by rememberUpdatedState(onEvent)
    val regions=remember{mutableMapOf<String,Rect>()}
    val held=remember{mutableStateMapOf<Long,String>()}
    var stick by remember{mutableStateOf(Offset.Zero)}
    val buttons=mapOf("forward" to ControlButton.FORWARD,"backward" to ControlButton.BACKWARD,
        "left" to ControlButton.LEFT,"right" to ControlButton.RIGHT,"boost" to ControlButton.BOOST,
        "weapon" to ControlButton.WEAPON,"brake" to ControlButton.BRAKE,"joystick" to ControlButton.JOYSTICK)
    val pointer=Modifier.motionEventSpy { if(it.actionMasked==MotionEvent.ACTION_CANCEL){held.clear();stick=Offset.Zero;event(ControlEvent.Cancel)} }
        .pointerInput(settings.inputMode) {
            try { awaitPointerEventScope {
                while(true) {
                    val input=awaitPointerEvent(PointerEventPass.Initial)
                    if(held.containsValue("unlock") && input.changes.count{it.pressed}>1 &&
                        input.changes.any{it.changedToDownIgnoreConsumed()})event(ControlEvent.CancelUnlock)
                    for(change in input.changes) {
                        val id=change.id.value
                        if(change.changedToDownIgnoreConsumed()) {
                            val name=regions.entries.firstOrNull{it.value.contains(change.position)}?.key ?: continue
                            held[id]=name
                            when(name) {
                                "kill"->event(ControlEvent.Kill)
                                "unlock"->if(input.changes.count{it.pressed}==1)event(ControlEvent.BeginUnlock)
                                "turn"->event(ControlEvent.Turn)
                                else->buttons[name]?.let{event(ControlEvent.Press(id.toInt(),it))}
                            }
                        }
                        val name=held[id] ?: continue
                        if(change.changedToUpIgnoreConsumed()) {
                            held.remove(id)
                            if(name=="joystick")stick=Offset.Zero
                            if(name=="unlock") event(ControlEvent.EndUnlock) else event(ControlEvent.Release(id.toInt()))
                        } else if(change.pressed) {
                            val box=regions[name]
                            if(box!=null && !box.contains(change.position)) {
                                if(name=="joystick")stick=Offset.Zero
                                if(name in listOf("forward","backward","left","right","joystick")) event(ControlEvent.Leave(id.toInt()))
                                else if(name=="unlock") event(ControlEvent.CancelUnlock)
                                else event(ControlEvent.Release(id.toInt()))
                            } else if(name=="joystick" && box!=null) {
                                val radius=minOf(box.width,box.height)/2
                                stick=Offset((change.position.x-box.center.x)/radius,(change.position.y-box.center.y)/radius)
                                event(ControlEvent.Joystick(id.toInt(),stick.x,-stick.y))
                            }
                        }
                        change.consume()
                    }
                }
            } } finally {held.clear();stick=Offset.Zero;event(ControlEvent.Cancel)}
        }
    val muted=Color(0xFFA8B5C8)
    val teal=Color(0xFF2DD4BF)
    val red=Color(0xFFF04452)
    val orange=Color(0xFFF6AD55)
    val yellow=Color(0xFFF6D365)
    val blue=Color(0xFF60A5FA)
    val panel=Color(0xFF111E30)
    val outline=Color(0xFF2B3B53)
    val ackAge=diagnostics.ackAge(nowMs)
    val stale=!state.connected || ackAge==null || ackAge>=300
    val driving=state.pwm.left!=0 || state.pwm.right!=0
    @Composable fun Pad(tag:String,label:String,accent:Color,modifier:Modifier=Modifier,
                        detail:String="",symbol:String="",active:Boolean=false,compact:Boolean=false) {
        val shape=RoundedCornerShape(14.dp)
        Box(modifier.heightIn(min=48.dp).testTag(tag).onGloballyPositioned{regions[tag]=it.boundsInRoot()}
            .background(Brush.linearGradient(listOf(accent.copy(alpha=when{tag=="kill"->.88f;active->.44f;tag=="boost"->.42f;else->.23f}),
                accent.copy(alpha=when(tag){"kill"->.68f;"boost"->.18f;else->.08f}))),shape)
            .border(if(active)2.dp else 1.5.dp,accent.copy(alpha=if(active)1f else .75f),shape)
            .semantics {contentDescription=when(tag){
                "kill"->if(state.killed)"KILL · ĐÃ KHÓA" else "KILL · KHÓA XE"
                "turn"->"$label\n$detail"
                "weapon"->"$label\n${settings.weaponMode}"
                else->label
            };role=Role.Button;selected=active;onClick {
                when(tag){"kill"->event(ControlEvent.Kill);"turn"->event(ControlEvent.Turn)
                    else->buttons[tag]?.let{event(ControlEvent.Press(-100,it));event(ControlEvent.Release(-100))}}
                true
            }},contentAlignment=Alignment.Center) {
            if(tag=="joystick") {
                Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                    val radius=size.minDimension/2
                    drawCircle(teal.copy(alpha=.10f),radius)
                    drawCircle(teal.copy(alpha=.32f),radius,style=Stroke(1.dp.toPx()))
                    drawCircle(teal.copy(alpha=.12f),radius*.62f,style=Stroke(1.dp.toPx()))
                    drawLine(teal.copy(alpha=.15f),Offset(center.x-radius,center.y),Offset(center.x+radius,center.y),1.dp.toPx())
                    drawLine(teal.copy(alpha=.15f),Offset(center.x,center.y-radius),Offset(center.x,center.y+radius),1.dp.toPx())
                    val thumb=center+Offset(stick.x.coerceIn(-1f,1f),stick.y.coerceIn(-1f,1f))*radius*.58f
                    drawCircle(teal.copy(alpha=.13f),radius*.35f,thumb)
                    drawCircle(teal,radius*.24f,thumb)
                    drawCircle(Color(0xFFAEFFF0),radius*.24f,thumb,style=Stroke(1.dp.toPx()))
                }
            } else if(tag in listOf("boost","brake","weapon","turn"))Row(Modifier.padding(horizontal=12.dp,vertical=if(compact)1.dp else 4.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                if(!compact)Text(when(tag){"boost"->"ϟ";"brake"->"⊖";"weapon"->"✴";else->"⟳"},color=accent,fontSize=if(tag=="boost")32.sp else 24.sp,lineHeight=if(tag=="boost")34.sp else 26.sp)
                Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text(label,color=Color(0xFFF4F7FB),fontSize=if(tag=="boost")21.sp else 13.sp,
                        lineHeight=if(tag=="boost")22.sp else 15.sp,fontWeight=FontWeight.Bold,maxLines=1)
                    if(detail.isNotEmpty())Text(detail,color=accent,fontSize=10.sp,lineHeight=12.sp)
                }
            } else Column(Modifier.padding(horizontal=6.dp,vertical=if(compact)1.dp else 3.dp),horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(1.dp)) {
                if(symbol.isNotEmpty())Text(symbol,color=if(state.connected)Color(0xFFF4F7FB) else accent,fontSize=if(compact)12.sp else 24.sp,
                    lineHeight=if(compact)14.sp else 24.sp,fontWeight=FontWeight.Bold)
                Text(label,color=if(!state.connected && tag in listOf("forward","backward","left","right"))muted else Color(0xFFF4F7FB),
                    fontSize=if(compact)9.sp else 11.sp,lineHeight=if(compact)12.sp else 14.sp,
                    fontWeight=FontWeight.SemiBold,letterSpacing=.4.sp,textAlign=TextAlign.Center)
                if(detail.isNotEmpty())Text(detail,color=accent,fontSize=10.sp,lineHeight=12.sp,textAlign=TextAlign.Center)
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compactHeight=maxHeight<400.dp
    Column(Modifier.fillMaxSize().background(Color(0xFF0B1220)).testTag("controller").then(pointer)
        .safeDrawingPadding().padding(horizontal=12.dp,vertical=if(compactHeight)4.dp else 8.dp),verticalArrangement=Arrangement.spacedBy(if(compactHeight)4.dp else 6.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp).background(panel,RoundedCornerShape(12.dp)).border(1.dp,outline,RoundedCornerShape(12.dp))
            .padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(if(compactHeight)6.dp else 10.dp)) {
            Text("SUMO",color=Color(0xFFF4F7FB),fontSize=if(compactHeight)18.sp else 23.sp,lineHeight=25.sp,fontWeight=FontWeight.Black,letterSpacing=1.sp)
            Box(Modifier.size(7.dp).background(if(state.connected)teal else red,CircleShape))
            Text(if(compactHeight){if(state.connected)"Online" else "Offline"} else if(state.connected)"Online · ${settings.host}:${settings.port}" else connection,
                Modifier.weight(1f).testTag("connection"),color=if(state.connected)teal else muted,
                fontSize=10.sp,lineHeight=13.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
            Column(Modifier.width(114.dp)) {
                Text("Ping ${if(state.connected)diagnostics.rttMs?.let{"$it ms"} ?: "—" else "—"}",color=muted,fontSize=10.sp,lineHeight=12.sp)
                Text("ACK ${ackAge?.let{"$it ms trước"} ?: "chưa có"}",Modifier.testTag("ack-age"),color=if(stale)orange else muted,fontSize=10.sp,lineHeight=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            Row(Modifier.border(1.dp,outline,RoundedCornerShape(8.dp)),verticalAlignment=Alignment.CenterVertically) {
                Text(state.mode.name,Modifier.background(teal.copy(alpha=.12f),RoundedCornerShape(8.dp)).padding(9.dp),color=teal,fontSize=10.sp,lineHeight=12.sp,fontWeight=FontWeight.Bold)
                Text("AUTO khóa",Modifier.padding(horizontal=8.dp).semantics{disabled()},color=muted,fontSize=9.sp,lineHeight=11.sp)
            }
            OutlinedButton(onClick=onSettings,modifier=Modifier.height(48.dp).testTag("settings"),shape=RoundedCornerShape(10.dp),
                contentPadding=PaddingValues(horizontal=10.dp)){Text("Cấu hình",fontSize=11.sp,lineHeight=14.sp,color=Color(0xFFF4F7FB))}
            OutlinedButton(onClick=onUpdates,modifier=Modifier.height(48.dp).testTag("updates"),shape=RoundedCornerShape(10.dp),
                contentPadding=PaddingValues(horizontal=10.dp)){Text("↓ Cập nhật",fontSize=11.sp,lineHeight=14.sp,color=Color(0xFFF4F7FB))}
            Pad("kill",if(state.killed)"ĐÃ KHÓA" else "■  KILL",red,Modifier.width(96.dp).height(48.dp),active=state.killed)
        }
        Row(Modifier.fillMaxWidth().height(48.dp).background(if(diagnostics.warning!=null)orange.copy(alpha=.08f) else panel,RoundedCornerShape(12.dp))
            .border(1.dp,if(diagnostics.warning!=null)orange.copy(alpha=.5f) else outline,RoundedCornerShape(12.dp)).padding(horizontal=10.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(if(diagnostics.warning!=null)"!" else ">_",color=if(diagnostics.warning!=null)orange else teal,fontSize=18.sp,lineHeight=20.sp,fontFamily=FontFamily.Monospace)
            Text(diagnostics.warning ?: when{state.killed->"Đã khóa điều khiển · cần giữ mở khóa và nhận ACK";state.connected->"Không có cảnh báo";else->"Chưa điều khiển · kết nối receiver để bắt đầu"},
                Modifier.weight(1f).testTag("status-warning"),color=if(diagnostics.warning!=null)orange else muted,fontSize=11.sp,lineHeight=14.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
            if(state.killed)Pad("unlock","Giữ mở khóa",orange,Modifier.width(132.dp).height(48.dp),detail="1 giây · chờ ACK")
            OutlinedButton(onClick=onConnect,modifier=Modifier.height(48.dp).testTag("connect"),shape=RoundedCornerShape(9.dp),
                contentPadding=PaddingValues(horizontal=12.dp)){Text(if(state.connected)"↻ Kết nối lại" else "↻ Kết nối",fontSize=11.sp,lineHeight=14.sp)}
            TextButton(onClick=onDiagnostics,modifier=Modifier.height(48.dp).testTag("diagnostics"),contentPadding=PaddingValues(horizontal=8.dp)) {
                Text("Xem chi tiết  ›",color=muted,fontSize=11.sp,lineHeight=14.sp)
            }
            DisposableEffect(state.killed){if(!state.killed)regions.remove("unlock");onDispose{}}
        }
        Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1.08f).fillMaxHeight().background(panel,RoundedCornerShape(16.dp)).border(1.dp,outline,RoundedCornerShape(16.dp)).padding(if(compactHeight)6.dp else 10.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(if(compactHeight)"Nhả tay để dừng" else "ĐIỀU HƯỚNG",color=Color(0xFFF4F7FB),fontSize=12.sp,lineHeight=14.sp,fontWeight=FontWeight.Bold,letterSpacing=.7.sp)
                    Text(if(settings.inputMode==InputMode.D_PAD)"D-PAD" else "JOYSTICK",color=teal,fontSize=10.sp,lineHeight=12.sp)
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                    val side=minOf(maxWidth,maxHeight).coerceAtMost(180.dp)
                    val key=((side-8.dp)/3).coerceAtLeast(48.dp)
                    if(settings.inputMode==InputMode.D_PAD) {
                        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Pad("forward","TIẾN",teal,Modifier.size(key),symbol="↑",active=driving && held.containsValue("forward"),compact=compactHeight)
                            Row(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
                                Pad("left","TRÁI",teal,Modifier.size(key),symbol="←",active=driving && held.containsValue("left"),compact=compactHeight)
                                Box(Modifier.size(key),contentAlignment=Alignment.Center) {
                                    Box(Modifier.size(key*.68f).background(Color(0xFF1A304A),CircleShape).border(1.dp,outline,CircleShape),contentAlignment=Alignment.Center) {
                                        Box(Modifier.size(6.dp).background(teal.copy(alpha=.65f),CircleShape))
                                    }
                                }
                                Pad("right","PHẢI",teal,Modifier.size(key),symbol="→",active=driving && held.containsValue("right"),compact=compactHeight)
                            }
                            Pad("backward","LÙI",teal,Modifier.size(key),symbol="↓",active=driving && held.containsValue("backward"),compact=compactHeight)
                        }
                        DisposableEffect(Unit){onDispose{listOf("forward","backward","left","right").forEach{regions.remove(it)}}}
                    } else {
                        Pad("joystick","JOYSTICK",teal,Modifier.size(side.coerceAtLeast(144.dp)))
                        DisposableEffect(Unit){onDispose{regions.remove("joystick")}}
                    }
                }
                if(!compactHeight)Text("Nhả tay để dừng",color=muted,fontSize=9.sp,lineHeight=11.sp)
            }
            Column(Modifier.weight(1f).fillMaxHeight().background(panel,RoundedCornerShape(16.dp)).border(1.dp,outline,RoundedCornerShape(16.dp)).padding(if(compactHeight)6.dp else 10.dp),
                verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Pad("boost","BOOST",red,Modifier.fillMaxWidth().weight(1.2f),detail="GIỮ ĐỂ TĂNG TỐC",active=state.boost,compact=compactHeight)
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Pad("weapon","VŨ KHÍ",orange,Modifier.weight(1f).fillMaxHeight(),detail=if(settings.weaponMode==WeaponMode.HOLD)"GIỮ · HOLD" else "BẬT / TẮT",active=state.weapon,compact=compactHeight)
                    Pad("turn","QUAY NHANH",yellow,Modifier.weight(1f).fillMaxHeight(),detail="${settings.turnDurationMs} ms",active=state.turnId>0,compact=compactHeight)
                }
                Pad("brake","BRAKE",blue,Modifier.fillMaxWidth().height(56.dp),detail="GIỮ PHANH",active=state.brake,compact=compactHeight)
            }
        }
        Row(Modifier.heightIn(min=52.dp).fillMaxWidth().background(panel,RoundedCornerShape(12.dp)).border(1.dp,outline,RoundedCornerShape(12.dp)).padding(horizontal=12.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Column(Modifier.width(96.dp)) {
                Text("GIỚI HẠN TỐC",color=muted,fontSize=9.sp,lineHeight=11.sp,maxLines=1)
                Text("${settings.limitPercent}%",color=teal,fontSize=21.sp,lineHeight=23.sp,fontWeight=FontWeight.Bold)
            }
            Text("30%",color=muted,fontSize=9.sp,lineHeight=11.sp)
            Slider(settings.limitPercent.toFloat(),{event(ControlEvent.Limiter(it.toInt()))},Modifier.weight(1f).testTag("limiter"),
                valueRange=30f..100f,onValueChangeFinished=onLimiterSaved,
                thumb={Box(Modifier.size(20.dp).background(Color.White,CircleShape).border(1.dp,teal,CircleShape))},
                track={SliderDefaults.Track(it,thumbTrackGapSize=0.dp,drawStopIndicator=null)},
                colors=SliderDefaults.colors(thumbColor=teal,activeTrackColor=teal,inactiveTrackColor=outline))
            Text("100%",color=muted,fontSize=9.sp,lineHeight=11.sp)
            Box(Modifier.width(1.dp).height(32.dp).background(outline))
            Column(Modifier.width(160.dp)) {
                Text("APP YÊU CẦU (TRÁI / PHẢI)",color=muted,fontSize=8.sp,lineHeight=10.sp,maxLines=1)
                Text("PWM ${state.pwm.left} / ${state.pwm.right} · B${if(state.boost)1 else 0} W${if(state.weapon)1 else 0} K${if(state.brake)1 else 0}",
                    Modifier.testTag("output"),color=Color(0xFFF4F7FB),fontSize=10.sp,lineHeight=13.sp,maxLines=2,fontFamily=FontFamily.Monospace)
            }
            Box(Modifier.width(1.dp).height(32.dp).background(outline))
            Column(Modifier.width(190.dp)) {
                Text(when{ackAge==null->"CHƯA CÓ ACK RECEIVER";stale->"ACK CŨ · CHƯA XÁC NHẬN";else->"ACK PWM (TRÁI / PHẢI)"},color=if(stale)orange else muted,fontSize=8.sp,lineHeight=10.sp,maxLines=1)
                Text(if(state.turnId>0)"Xoay #${state.turnId}: ${if(state.turnActive)"đã ACK" else "chờ ACK"}" else
                    receiver?.let{"ACK cuối PWM ${it.pwm.left} / ${it.pwm.right} · W${if(it.weapon)1 else 0}"}
                        ?: diagnostics.acknowledged?.let{"ACK cũ PWM $it"} ?: "Chưa có ACK receiver",
                    Modifier.testTag("receiver"),color=if(stale)muted else Color(0xFFF4F7FB),fontSize=10.sp,lineHeight=13.sp,maxLines=2)
            }
            Text("${settings.sendIntervalMs} ms",color=muted,fontSize=10.sp,lineHeight=12.sp,fontFamily=FontFamily.Monospace)
        }
    }
    }
}
