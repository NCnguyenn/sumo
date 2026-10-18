package com.aistudy.myapplication.settings

import android.content.Context

class ControllerSettingsStore(context:Context) {
    private val prefs=context.getSharedPreferences("sumo_settings",Context.MODE_PRIVATE)
    fun load():Result<StoredSettings> = runCatching {
        val raw=prefs.getString("record",null) ?: return@runCatching StoredSettings()
        val value=SettingsDocument.decode(raw)
        if(raw.lineSequence().any{it=="schema=0"}) {
            check(prefs.edit().putString("migration_backup",raw).putString("record",SettingsDocument.encode(value)).commit()){"Không lưu được migration"}
        }
        value
    }
    fun save(value:StoredSettings) {
        val record=SettingsDocument.encode(value)
        check(prefs.edit().putString("record",record).commit()){"Không lưu được cấu hình; bản cũ vẫn được giữ"}
    }
}
