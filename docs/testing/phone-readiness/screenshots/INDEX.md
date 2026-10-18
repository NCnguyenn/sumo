# Ảnh kiểm tra cài thử điện thoại

Ảnh PNG/XML lấy trực tiếp từ AVD API37; chưa phải ảnh điện thoại thật. [Manifest/checksum](../screenshots-manifest.json). Bộ cuối từ ảnh39đến54khớp APK `b31ac740afa3515c8cc5a2a344061e170c63a5972ca4beef2058c3ac7c5c736f`; ảnh01–37là lỗi trước sửa/binarytrung gian,giữ riêng để đối chiếu.

| Ảnh cuối | Nội dung |
|---|---|
| [39](39-final-b31-unlock-after-release-delayed-ack.png) | Đã nhả sau1.050ms,trướcACKunlocktrễ200ms; sauACKappvẫnPWM0,khôngKill. [Trace](../evidence/unlock-live-summary.json). |
| [40](40-b31-dpad-online.png) | Điều khiểnD-Pad,receiverPython thật;khôngphảixe. |
| [41](41-b31-kill-locked.png) | Kill/giữmởkhóa. |
| [42](42-b31-settings-general.png) | Cấu hìnhChung. |
| [43](43-b31-settings-calibration.png) | Cấu hìnhHiệu chỉnh. |
| [44](44-b31-settings-update.png) | Cấu hìnhnguồncậpnhật. |
| [45](45-b31-updater-historical-install-error.png) | Updaterchưahosting; mã6làfixturelịchsử,khôngxóa. |
| [46](46-b31-joystick-online.png) | JoystickOnlinevớisimulator. |
| [47](47-b31-real-stale-ack.png) | MấtACKthật:AppPWM0/0,ACKcũ71/72,khôngtuyênbốđộngcơđãdừng. |
| [48](48-b31-diagnostics-real-error.png) | Chẩn đoánphiênthậtcóTX/RX/loạivànguồnlỗiChưaxácđịnh. |
| [49](49-b31-diagnostic-history.png) | Lịchsửphiênlỗithậtđãlưu. |
| [50](50-b31-small-font150-controller.png) | 800×360dp,font150%;D-Pad≥48dp,BOOSTlớnnhất,cảnhbáogiữchỗ. |
| [51](51-b31-small-font150-diagnostics.png) | Metric/nhậtký/timestampđọctạifont150%. Hàngtrêncùngđangcuộnlàclippingviewportbìnhthường,cộttimestampkhôngxuốngdòng. |
| [52](52-b31-small-font150-updater.png) | Updaterfont150%vùngstatusvẫncóthểđọc/cuộn. |
| [53](53-b31-android-export-picker.png) | AndroidCreateDocumentthật. Filecuối695byte,SHA9bca5c…;filephiênlỗidàihơnởevidence/exported-diagnostic.txtthuộcbinaryc377… |
| [54](54-b31-final-offline-restored.png) | KếtthúcOffline,PWM0/0,B0W0K0,AUTOkhóa;khôiphụccấuhình/display. |

Ảnh nhỏfont150%:1600×720pixel; ảnhappmặcđịnh:2400×1080pixel;hộpxuấtAndroid:1080×2400pixel. Không ghép hoặc sửa PNGgốc. ChỉdùngảnhđểchứngminhUIhiểnthị,khônglàbằngchứnglatency/hardware/soak.
