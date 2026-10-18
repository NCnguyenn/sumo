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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ControllerScreen(state:ControlState,settings:ControllerSettings,connection:String,onEvent:(ControlEvent)->Unit,
    onConnect:()->Unit,onSettings:()->Unit,onUpdates:()->Unit,onLimiterSaved:()->Unit={},receiver:ControlState?=null) {
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
                        input.changes.any{it.changedToDownIgnoreConsumed()})event(ControlEvent.EndUnlock)
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
                                else if(name=="unlock") event(ControlEvent.EndUnlock)
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
    val muted=Color(0xFF99A9BE)
    val teal=Color(0xFF6BE6CC)
    val red=Color(0xFFFF6775)
    val orange=Color(0xFFFFB467)
    val yellow=Color(0xFFF3D46D)
    val blue=Color(0xFF80BFFF)
    val panel=Color(0xFF131C27)
    val driving=state.pwm.left!=0 || state.pwm.right!=0
    @Composable fun Pad(tag:String,label:String,accent:Color,modifier:Modifier=Modifier,
                        detail:String="",symbol:String="",active:Boolean=false) {
        val shape=RoundedCornerShape(14.dp)
        Box(modifier.heightIn(min=48.dp).testTag(tag).onGloballyPositioned{regions[tag]=it.boundsInRoot()}
            .background(if(active)accent.copy(alpha=.30f) else accent.copy(alpha=.10f),shape)
            .border(if(active)2.dp else 1.dp,accent.copy(alpha=if(active).85f else .28f),shape)
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
            } else Column(Modifier.padding(horizontal=6.dp,vertical=3.dp),horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(1.dp)) {
                if(symbol.isNotEmpty())Text(symbol,color=accent,fontSize=if(tag=="boost")27.sp else 21.sp,
                    lineHeight=if(tag=="boost")28.sp else 22.sp,fontWeight=FontWeight.Medium)
                Text(label,color=if(!active && tag in listOf("forward","backward","left","right"))muted else Color(0xFFF1F5FA),
                    fontSize=if(tag=="boost")18.sp else 11.sp,lineHeight=if(tag=="boost")21.sp else 14.sp,
                    fontWeight=FontWeight.SemiBold,letterSpacing=if(tag=="boost")2.sp else .4.sp,textAlign=TextAlign.Center)
                if(detail.isNotEmpty())Text(detail,color=accent,fontSize=10.sp,lineHeight=12.sp,textAlign=TextAlign.Center)
            }
        }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF0B1016)).testTag("controller").then(pointer)
        .safeDrawingPadding().padding(horizontal=14.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.padding(end=6.dp)) {
                Text("SUMO",color=Color(0xFFF1F5FA),fontSize=21.sp,lineHeight=23.sp,fontWeight=FontWeight.Black,letterSpacing=3.sp)
                Text("CONTROLLER",color=muted,fontSize=8.sp,lineHeight=11.sp,letterSpacing=1.2.sp)
            }
            Box(Modifier.size(6.dp).background(if(state.connected)teal else orange,CircleShape))
            Text(connection,Modifier.weight(1f).testTag("connection"),color=if(state.connected)teal else muted,
                fontSize=11.sp,lineHeight=15.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
            Button(onClick=onConnect,modifier=Modifier.testTag("connect"),shape=RoundedCornerShape(10.dp),
                contentPadding=PaddingValues(horizontal=12.dp)){Text(if(state.connected)"Kết nối lại" else "Kết nối",fontSize=12.sp)}
            OutlinedButton(onClick=onSettings,modifier=Modifier.testTag("settings"),shape=RoundedCornerShape(10.dp),
                contentPadding=PaddingValues(horizontal=10.dp)){Text("Cấu hình",fontSize=12.sp,color=Color(0xFFE1E9F2))}
            OutlinedButton(onClick=onUpdates,modifier=Modifier.testTag("updates"),shape=RoundedCornerShape(10.dp),
                contentPadding=PaddingValues(horizontal=10.dp)){Text("Cập nhật",fontSize=12.sp,color=Color(0xFFE1E9F2))}
        }
        Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight().background(panel,RoundedCornerShape(18.dp)).padding(8.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("ĐIỀU HƯỚNG",color=muted,fontSize=10.sp,letterSpacing=1.sp)
                    Text(if(settings.inputMode==InputMode.D_PAD)"D-PAD" else "JOYSTICK",color=teal,fontSize=10.sp)
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                    val side=minOf(maxWidth,maxHeight).coerceAtMost(230.dp)
                    val key=((side-8.dp)/3).coerceAtLeast(48.dp)
                    if(settings.inputMode==InputMode.D_PAD) {
                        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Pad("forward","TIẾN",teal,Modifier.size(key),symbol="↑",active=driving && held.containsValue("forward"))
                            Row(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
                                Pad("left","TRÁI",teal,Modifier.size(key),symbol="←",active=driving && held.containsValue("left"))
                                Box(Modifier.size(key),contentAlignment=Alignment.Center) {
                                    Box(Modifier.size(key*.58f).border(1.dp,teal.copy(alpha=.16f),CircleShape),contentAlignment=Alignment.Center) {
                                        Box(Modifier.size(6.dp).background(teal.copy(alpha=.6f),CircleShape))
                                    }
                                }
                                Pad("right","PHẢI",teal,Modifier.size(key),symbol="→",active=driving && held.containsValue("right"))
                            }
                            Pad("backward","LÙI",teal,Modifier.size(key),symbol="↓",active=driving && held.containsValue("backward"))
                        }
                        DisposableEffect(Unit){onDispose{listOf("forward","backward","left","right").forEach{regions.remove(it)}}}
                    } else {
                        Pad("joystick","JOYSTICK",teal,Modifier.size(side.coerceAtLeast(144.dp)))
                        DisposableEffect(Unit){onDispose{regions.remove("joystick")}}
                    }
                }
            }
            Column(Modifier.width(136.dp).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Column(Modifier.weight(1f).fillMaxWidth().background(panel,RoundedCornerShape(18.dp)).padding(10.dp),
                    verticalArrangement=Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(state.mode.name,color=teal,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp)
                        Text("AUTO khóa",color=muted,fontSize=9.sp)
                    }
                    if(!state.killed)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        for((name,value) in listOf("TRÁI" to state.pwm.left,"PHẢI" to state.pwm.right))Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(name,color=muted,fontSize=9.sp,letterSpacing=.5.sp)
                            Text("$value",color=Color(0xFFF1F5FA),fontSize=24.sp,lineHeight=28.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Monospace)
                        }
                    }
                    Text("PWM ${state.pwm.left} / ${state.pwm.right} · B${if(state.boost)1 else 0} W${if(state.weapon)1 else 0} K${if(state.brake)1 else 0}",
                        Modifier.testTag("output"),color=muted,fontSize=10.sp,lineHeight=13.sp)
                    Text(if(state.turnId>0)"Xoay #${state.turnId}: ${if(state.turnActive)"đã ACK" else "chờ ACK"}" else
                        receiver?.let{"ACK cuối PWM ${it.pwm.left} / ${it.pwm.right} · W${if(it.weapon)1 else 0}"} ?: "Chờ phản hồi receiver",
                        Modifier.testTag("receiver"),color=muted,fontSize=9.sp,lineHeight=12.sp,maxLines=2)
                }
                Pad("kill",if(state.killed)"ĐÃ KHÓA" else "KILL",red,Modifier.fillMaxWidth().height(58.dp),
                    detail=if(state.killed)"Dừng khẩn cấp" else "DỪNG KHẨN CẤP",active=state.killed)
                if(state.killed)Pad("unlock","Giữ mở khóa",orange,Modifier.fillMaxWidth().height(48.dp),detail="1 giây · chờ ACK")
                DisposableEffect(state.killed){if(!state.killed)regions.remove("unlock");onDispose{}}
            }
            Column(Modifier.weight(1.05f).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Pad("boost","BOOST",red,Modifier.fillMaxWidth().weight(1.3f),detail="GIỮ ĐỂ TĂNG TỐC",symbol="ϟ",active=state.boost)
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Pad("weapon","VŨ KHÍ",orange,Modifier.weight(1f).fillMaxHeight(),detail=if(settings.weaponMode==WeaponMode.HOLD)"GIỮ · HOLD" else "BẬT / TẮT",active=state.weapon)
                    Pad("turn","QUAY NHANH",yellow,Modifier.weight(1f).fillMaxHeight(),detail="${settings.turnDurationMs} ms",active=state.turnId>0)
                }
                Pad("brake","BRAKE · GIỮ PHANH",blue,Modifier.fillMaxWidth().height(48.dp),active=state.brake)
            }
        }
        Row(Modifier.height(48.dp).fillMaxWidth().background(panel,RoundedCornerShape(14.dp)).padding(horizontal=12.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Column(Modifier.width(76.dp)) {
                Text("GIỚI HẠN TỐC",color=muted,fontSize=9.sp,letterSpacing=.6.sp)
                Text("${settings.limitPercent}%",color=teal,fontSize=20.sp,lineHeight=23.sp,fontWeight=FontWeight.Bold)
            }
            Slider(settings.limitPercent.toFloat(),{event(ControlEvent.Limiter(it.toInt()))},Modifier.weight(1f).testTag("limiter"),
                valueRange=30f..100f,onValueChangeFinished=onLimiterSaved,
                colors=SliderDefaults.colors(thumbColor=teal,activeTrackColor=teal,inactiveTrackColor=Color(0xFF2A394C)))
            Text("${settings.sendIntervalMs} ms",color=muted,fontSize=11.sp,fontFamily=FontFamily.Monospace)
        }
    }
}
