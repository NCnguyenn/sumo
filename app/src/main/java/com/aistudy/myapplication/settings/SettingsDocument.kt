package com.aistudy.myapplication.settings

import com.aistudy.myapplication.updates.requireHttps
import java.util.Properties
import java.io.StringReader
import java.io.StringWriter

data class StoredSettings(val controller:ControllerSettings=ControllerSettings(),val updateFeed:String="")
object SettingsDocument {
    fun encode(value:StoredSettings):String {
        val s=value.controller.validated(); val c=s.calibration
        if(value.updateFeed.isNotEmpty()) requireHttps(value.updateFeed)
        val fields=mapOf("schema" to "1","limit" to s.limitPercent.toString(),"sensitivity" to c.sensitivity.toString(),
            "deadZone" to c.deadZone.toString(),"leftGain" to c.leftGain.toString(),"rightGain" to c.rightGain.toString(),
            "turnPercent" to c.turnPercent.toString(),"duration" to s.turnDurationMs.toString(),"weapon" to s.weaponMode.name,
            "input" to s.inputMode.name,"host" to s.host,"port" to s.port.toString(),"interval" to s.sendIntervalMs.toString(),
            "template" to s.driveTemplate,"feed" to value.updateFeed)
        val p=Properties(); fields.forEach { (k,v)->p.setProperty(k,v) }
        return StringWriter().also{p.store(it,null)}.toString()
    }
    fun decode(raw:String):StoredSettings {
        try {
            val p=Properties().apply{load(StringReader(raw))}
            val schema=p.getProperty("schema")?.toIntOrNull()
            require(schema==0 || schema==1){"Schema cấu hình không hỗ trợ; giữ nguyên dữ liệu để phục hồi"}
            val defaults=Properties().apply{load(StringReader(encode(StoredSettings())))}
            fun field(name:String):String = p.getProperty(name) ?: if(schema==0) defaults.getProperty(name) else
                throw IllegalArgumentException("Thiếu trường $name; dữ liệu cũ được giữ nguyên")
            fun integer(name:String)=field(name).toIntOrNull() ?: throw IllegalArgumentException("Sai số $name")
            val settings=ControllerSettings(integer("limit"),DriveCalibration(field("sensitivity").toFloat(),field("deadZone").toFloat(),
                field("leftGain").toFloat(),field("rightGain").toFloat(),integer("turnPercent")),integer("duration"),
                WeaponMode.valueOf(field("weapon")),InputMode.valueOf(field("input")),field("host"),integer("port"),integer("interval"),field("template")).validated()
            val value=StoredSettings(settings,field("feed")); encode(value)
            return value
        } catch(e:RuntimeException) { throw IllegalArgumentException(e.message ?: "Cấu hình hỏng; dữ liệu được giữ nguyên",e) }
    }
}
