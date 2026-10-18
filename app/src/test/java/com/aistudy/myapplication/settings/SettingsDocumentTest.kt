package com.aistudy.myapplication.settings

import org.junit.Assert.*
import org.junit.Test

class SettingsDocumentTest {
    @Test fun F06_roundTripEverySettingWithoutRuntimeState() {
        val expected=StoredSettings(ControllerSettings(limitPercent=30, calibration=DriveCalibration(1.5f,.2f,.7f,.8f,40),
            turnDurationMs=350, weaponMode=WeaponMode.TOGGLE,inputMode=InputMode.JOYSTICK,host="127.0.0.1",port=9999,
            sendIntervalMs=50,driveTemplate=ControllerSettings.D_TEMPLATE),"https://updates.example.invalid/test.json")
        assertEquals(expected,SettingsDocument.decode(SettingsDocument.encode(expected)))
        assertFalse(SettingsDocument.encode(expected).contains("pointer"))
    }
    @Test fun F10_legacyAddsDefaultsAndBadSchemaDoesNotBecomeDefaults() {
        val migrated=SettingsDocument.decode("schema=0\nlimit=45\nhost=10.0.2.2\nport=8888\n")
        assertEquals(45,migrated.controller.limitPercent); assertEquals(30,migrated.controller.sendIntervalMs)
        assertThrows(IllegalArgumentException::class.java){SettingsDocument.decode("schema=99\n")}
        assertThrows(IllegalArgumentException::class.java){SettingsDocument.decode("schema=1\nlimit=bad\n")}
    }
    @Test fun F05_invalidRecordCannotBeEncodedOrPartiallyApplied() {
        assertThrows(IllegalArgumentException::class.java){SettingsDocument.encode(StoredSettings(ControllerSettings(port=0)))}
        assertThrows(IllegalArgumentException::class.java){SettingsDocument.encode(StoredSettings(updateFeed="http://example.invalid"))}
    }
}
