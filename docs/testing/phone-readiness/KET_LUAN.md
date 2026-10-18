# Kết luận cài thử điện thoại — Sumo Controller

**Ngày kiểm tra: 18/10/2026.** Nhánh `codex/ui-compact-diagnostics`; giữ công việc chưa commit sẵn có. Không upload, tạo release, đổi khóa ký hoặc cài/gỡ điện thoại thật. Không mở lại thiết kế, duyệt G3/G4 hoặc chuyển Phase6/7.

> Bổ sung sau đợt bàn giao18/10: người dùng cho phép commit/merge/push và đóng gói debug gửi nhóm. Unit mới120/120 và10/10Android liên quan đạt sau sửa đồng bộ test; cả hai FAILAndroid bên dưới đã có lượt kiểm lại trong cohort42 trên cùng APKb31…. Không xóa kết quả lịch sử. APK nhóm code2, checksum/cài đè/giới hạn xem [bàn giao nhóm](../group-test-2026-10-18/BAN_GIAO_NHOM.md). STOP/cadence/soak và ranh giới xe/release giữ nguyên.

## 1. Quyết định

| Phạm vi | Kết luận | Căn cứ và điều kiện |
|---|---|---|
| **CÀI THỬ giao diện, cấu hình, nhật ký trên điện thoại** | **CÓ ĐIỀU KIỆN** | Chỉ APK debug cài tạm, **không nối xe**. Máy phải Android16/API36 trở lên, ABI phù hợp, không xung đột chữ ký/phiên bản app đang cài. Có kiểm tra APK thực và smoke AVD; chưa chạy API36/điện thoại ARM, chưa hoàn tất soak30phút, còn FAIL công khai bên dưới. |
| UDP từ điện thoại thật | **CHƯA KIỂM CHỨNG** | Chỉ AVD API37 ↔ receiver Python loopback. `10.0.2.2` chỉ dành cho AVD; simulator hiện không bind LAN. |
| Cập nhật APK trực tiếp trong app | **CHƯA SẴN SÀNG trọn luồng** | Có code kiểm tra/tải/xác minh/PackageInstaller; thiếu hosting/feed HTTPS thật, khóa phân phối ổn định và chuỗi A→B→C tăng versionCode được kiểm trên điện thoại. |
| Điều khiển xe / release | **GIỮ CHẶN** | STOP vẫn FAIL; hai ca Android cuối còn FAIL; soak chưa đạt; firmware/watchdog/động cơ thật chưa kiểm chứng. Giữ G5 có điều kiện cho chức năng AVD, G3/G4 chờ duyệt. |

ACK **không chứng minh động cơ thực sự quay hoặc dừng**. Thiếu bằng chứng quy lỗi app/mạng/IoT/phần cứng phải ghi **Chưa xác định**.

## 2. Đối chiếu nguồn và bằng chứng

Đã đọc đủ các nguồn người dùng chỉ định. Git ban đầu đúng nhánh, có12file tracked sửa và11mục untracked; không reset/clean/checkout hoặc lấy GitHub đè workspace. [Binding ban đầu](evidence/initial-binding.json) xác nhận59checksum nguồn khớp hồ sơ compact, APK app/test cache khớp hash đã công bố.

| Binary / cohort | Cách sử dụng bằng chứng |
|---|---|
| Compact gốc `c8f88c…` | APK giữ nguyên. 118unit,33case Android chức năng phân biệt,8fault là **lịch sử**, không phải test vừa chạy. 33case là hợp cohort35/36, không cộng35+5. |
| G5 cũ `92abb6…` / soak33 | Không nhận là soak của UI compact hoặc APK bàn giao. |
| Trung gian `c377cd…` / cohort37 | Một batch timing và đúng một lần thử soak trong đợt này; giữ mọi mẫu/FAIL. Sau đó sửa cột timestamp Chẩn đoán và lỗi unlock. **Không cùng hash APK cuối.** |
| Trung gian `aa0b56…` / cohort39 | 35/36ca đạt; một FAIL Hủy picker do test giả định Activity không tái tạo. Giữ FAIL và sửa test chờ surface thực, xác nhận Offline/PWM0. |
| **Cuối `b31ac7…`** / cohort40+41 | 12ca hoàn tất40 +24ca còn lại41: **34/36ca chức năng đạt,2FAIL**. Ngoài ra8/8fault mới và smoke thực trên hash này. Không cộng trùng các cohort. |

[Binding nguồn cuối](evidence/final-source-binding.json) lưu checksum từng file và lineage. Không đổi package, version, minSdk, targetSdk, SDK, dependency hoặc khóa ký. Tài liệu lịch sử không được dùng làm quyền upload/release mới.

## 3. Lỗi có căn cứ đã sửa

| Lỗi | Thay đổi nhỏ | Bằng chứng |
|---|---|---|
| Controller cắt ACK, Brake, limiter/PWM; D-Pad thiếu chiều cao ở màn nhỏ/font lớn | Chốt lineHeight, dành chỗ cho ACK/footer; giảm padding/biểu tượng phụ khi chiều cao<400dp. Không thu vùng chạm dưới48dp, không dịch nút khi cảnh báo xuất hiện. | Ảnh trước03/07; regression800×360dp/font150% kiểm bounds/glyph/48dp, cảnh báo cố định và BOOST lớn nhất; ảnh cuối50. |
| Chẩn đoán giấu giá trị metric, bóp vùng log; timestamp xuống dòng | Giảm lineHeight/padding dư; cột thời gian128dp/maxLines1. | Regression giá trị đọc được, log≥48dp, timestamp một dòng; ảnh trước14 và cuối51. |
| Updater bóp vùng trạng thái ở font lớn | Chốt lineHeight phần cố định, giữ status/history cuộn được và nút Đóng tới được. Không xóa lịch sử. | Regression đọc ít nhất một dòng status, cuộn tới history; ảnh trước13 và cuối52. |
| Nhấc ngón sau khi gửi unlock `U`, trước ACK, làm app gửi Kill lại | `EndUnlock` từng xóa pending; frameX1mới làm ACK `U` thành superseded. Nay EndUnlock chỉ kết thúc hold; CancelUnlock riêng cho ngón khác/ra ngoài. Kill/nền/lỗi phiên vẫn hủy. Chưa có ACK vẫn khóa/PWM0. | [Red trước sửa](evidence/unlock-red.xml),66/66unit liên quan sau sửa. **Smoke UDP thật:** U441,249s → nhấc441,302s → ACK441,463s; mở khóa nhưng PWM0/0, cần chạm lái mới. [Trace](evidence/unlock-live-summary.json). |
| Harness nhầm “chưa Online”; test Hủy picker giả định màn cũ còn nguyên | Wait Online dùng startsWith thay contains. Hủy picker chờ controller hoặc Chẩn đoán rồi kiểm Offline/PWM0. Không nới timeout hoặc sửa status app. | Giữ soakFAIL/cohort39FAIL; reconnect38đạt1/1, Hủy picker đạt trong12case40. Không chạy soak thứ2. |

Nguồn sản phẩm đổi: `ui/ControllerScreen.kt`, `ui/DiagnosticsScreen.kt`, `ui/AppUpdateDialog.kt`, `control/ControlEvent.kt`, `control/ControlReducer.kt`, `control/ControlSession.kt`. Regression nằm trong CompactDiagnosticsTest, ControlReducerTest, ControlSessionTest; sửa wait trong SoakTest/Phase5RegressionTest. Không tạo harness trùng chức năng.

## 4. Test thực chạy và lỗi còn mở

### Kết quả

| Kiểm tra | Kết quả | Bằng chứng / giới hạn |
|---|---|---|
| Build app/test APK và lint cuối | Exit0; **0lỗi/17cảnh báo** | [Log](evidence/unlock-green-build.txt), [lint XML](evidence/lint-results-final.xml). Không che cảnh báo. |
| Unit toàn module trong đợt | **117/118đạt,1FAIL** | [Log](evidence/build-ui-fix-quoted.txt), [XML](evidence/unit-xml-first/). Ca idle-poll hết await30s trước khi thu đủ1.000gap. |
| Unit sau sửa unlock | **66/66đạt** | Reducer29,session21,ReplyTracker16; [summary](evidence/unlock-green-summary.json)/[XML](evidence/unit-xml-unlock-green/). Chồng lặp với lượt118, không cộng thành184case. |
| Android trung gian37 | **39/40đạt** | 36chức năng+2thu timing+probe đạt;soakFAIL. Thu mẫu đạt không đồng nghĩa latency đạt. |
| Android cuối40+41 | **34/36ca chức năng phân biệt đạt,2FAIL** | 40bị AVDthoát sau12ca đạt,runnerexit255;41tiếp24ca còn lại,22đạt/2FAIL. [Ma trận testcase](evidence/final-android-case-summary.json). |
| Lỗi mạng trên APK cuối | **8/8đạt** | Mấtgói, mấtreply, trễRX150/350ms, trễreply150ms, trùng, đảo thứ tự, malformed. [Summary/raw](evidence/final-faults/android-fault-summary.json). |
| Smoke trực tiếp APK cuối | Đạt các thao tác đã quan sát | Unlock nhả trước ACK,4UI,joystick/Kill,3tab,mấtACK,history,Androidexport,càiđè giữdata. Không thay soak/timing/caAndroidFAIL bằng smoke. |

### FAIL chưa đóng

1. **Unit idle-poll:** chưa thu đủ1.000gap trong30s. Đây là sampling test trên Windows/JVM; [INFERENCE] lịch wakeup/heartbeat có thể làm ít mẫu, nhưng chưa có trace xác nhận căn nguyên. Không suy thành allocation leak/latencyART, không tăng timeout hoặc chạy lại để báo đạt. Full unit trên APK cuối sau hai regression mới chưa chạy lại;66test liên quan là kết quả mới có thật.
2. **Android I01_I02_U03:** assertion AppPWM255 thấy0 dù log có gửi/ACK255nhiều frame. Phần đóng phiên được ghi sau khi test mở `InstrumentationActivityInvoker$EmptyActivity`, MainActivity PAUSED rồi stop-gate; **không phải bằng chứng app tự đóng phiên**. [INFERENCE] có thể là teardown/mấtfocus sau assertion; nguyên nhân giá trị0 tại assertion chưa xác định. Giữ FAIL, không sửa phỏng đoán hoặc lặp lấy PASS. [Context](evidence/I01_I02_U03_androidUdpToIndependentPythonHasRealAckAndSafeRelease-context.txt)/[lifecycle](evidence/unexpected-close-lifecycle-context.txt).
3. **Android E03_A13:** phần mở Cập nhật khi đang giữ đã đóng an toàn; phần macro200ms có ACKquay→0 nhưng test không quan sát được nhãn `Xoay #`, rồi timeout. [INFERENCE] lịch UI/host có thể bỏ lỡ trạng thái ngắn; chưa đủ căn cứ quy nguyên nhân duy nhất. Giữ FAIL. [Context](evidence/E03_A13_updateEntryWhileHoldingAndDuringMacroClosesSession-context.txt).
4. **Môi trường:** AVDthoát code0 giữa cohort40, lý do chưa xác định. Khởi động lại không wipe/không snapshot rollback, tiếp đúng24ca chưa hoàn tất. [Gián đoạn](evidence/avd-interruption.json). Không gọi đó là crash app hoặc giấu hồ sơ.

### Timing — một batch mới, giữ toàn bộ mẫu

Đo trên binaryc377…do UIđãđổi; không thay power/priority policy. Giữ STOP≤10ms; cadence20/30trong20–50ms,riêng50trênAVDtrong20–60ms.

| Phép đo | Mẫu | Max | Kết quả |
|---|---:|---:|---|
| STOP t1−t0 |500|25,1894ms|**FAIL:5mẫu>10ms**|
| STOP t2−t0 |500|25,3868ms|**FAIL:6mẫu>10ms**|
| Cadence20 |1279|36,6047ms|Đạt cận lượt này|
| Cadence30 |1337|44,6013ms|Đạt cận lượt này|
| Cadence50 |1281|59,8723ms|Đạt cậnAVD;646mẫu vẫn>50ms|

[STOP CSV](evidence/stop.csv)/[JSON](evidence/stop.json)/[checkerexit1](evidence/stop-check.txt) · [Cadence CSV](evidence/cadence.csv)/[JSON](evidence/cadence.json)/[checkerexit0](evidence/cadence-check.txt) · [Raw37](../phase-5/evidence/37-phone-readiness-soak/).

Không dùngp99thaymax. FAIL lịch sử STOP2/500/max11,4174ms và cadenceNormal20 33/1281/max76,8416ms vẫn mở; lượt35đẹp không bảo đảm ổn định. Sau sửa unlock không có giả thuyết sửa latency nên không lặp timing chọn kết quả. **APK cuối chưa có timing cùng hash; không đóng lỗi.**

### Ổn định — chưa có soak30phút đạt

Dùng runner/SoakTest có sẵn, chỉ **một lượt thử**. Dừng sau5,948giây từbegin,0chu kỳ/0checkpoint/0reconnect do test chạm trướcACK khi nhầm “chưa Online”. Audit165frame đã gửi/được receiver áp dụng đềuPWM0, không revival. Sửa wait nhưng **không chạy soak thứ2** theo giới hạn. [Summary](evidence/soak-summary.json).

Begin→finish: worker1→0,trace1→0,socketFD14→11,heap10.822.736→8.721.216byte. Đây chỉ là mẫu ngắn, **không đủ kết luận tăng bộ nhớ/worker/socket trong30phút**. Logcohort37không có crash/ANRapp; [exit-info cuối](evidence/final-activity-exit-info.txt) không thay kết quả soak. APKcuối khác hash, vẫn chưa có soak dài cùngbinary.

Nhật ký sản phẩm có ring240dòng, mỗi dòng≤600ký tự, report≤80.000ký tự, chỉ nạp history≤256.000byte; I/Oởtraceworker,summaryđọcimmutable. Sáu testDiagnosticLogđạt trong lượtunit117/118; nguồn phần này không đổi. Không dùng rawlogcatcủatest để nói nhật ký trongappvôhạn.

## 5. Giao diện, thao tác và dữ liệu

Đã thao tác trực tiếp D-Pad/joystick,Kill/giữ mở khóa,cảnh báo,Chẩn đoán,3tabCấu hình và Cập nhật. Kiểm800×360dp/font150%,960×480dp/font130% và mặc định≈914×411dp. Regression xác nhận vùng điều khiển≥48dp,cảnh báo không dịch nút lái,BOOST lớn nhất nhóm tấn công. Chưa bao phủ mọi máy/notch/font200%;font/IME/haptic cần kiểm lại trên máy đích.

Đổi tab giữ bản nháp; Lưu/Hủy/reset có xác nhận/validation/migration/limiterquaActivityrecreate có code và ca thực thi. Lưu thông số không cần build lại; manual đã lưu joystick/TOGGLE, mở lại vẫn giữ. Đa pointer,nhả hướng/Boosthai thứ tự,pointercũ/chạm sớm,HOLD/TOGGLE,Brake,Kill,macro,reconnect/lifecycle,toolentry đều có ca. **Không khẳng định tất cả đạt** vì hai FAILAndroidcuối nêu trên.

Khi mấtACK thật: Appyêucầu0/0,TXcuối0/0,ACKcũ71/72vàtuổităng; nguồn lỗi **Chưa xác định**. Đã bấm đủ filterApp/Mạng/Giao thức/IoT/Tấtcả,clipboard,CreateDocument,lưuhistory và mở lại process. Pendingexport/Activityrecreate có regression.

- File phiên lỗi trênc377…:19.397byte/18.271ký tự,SHA256`832d1e52e916d20c1a0c7665f1f6c73551b9e7e8e6a22d80e70332a997db790f`; [file](evidence/exported-diagnostic.txt)/[verification](evidence/export-verification.json).
- File hiện tại trên **APKcuối**:695byte,SHA256`9bca5c8566203e01b55f432fec151fb2b57f7b197b068c71794a3decce7ceb19`; [file](evidence/exported-diagnostic-final.txt)/[verification](evidence/export-final-verification.json). Đây là phiên ngắn sau mở lại, không giả là toàn bộ history.
- Thông báo updater “Android không cài được(mã6)” là fixture kiểm thử đã lưu, **không phải lỗi cài mới** trong lần chụp. Không xóa history để giao diện trông đạt.

**Ảnh thật cùnghashcuối:** [chỉ mục](screenshots/INDEX.md) · [manifest/checksum](screenshots-manifest.json). 16ảnh39–54gồmD-Pad,joystick,Kill,3tab,updater,mấtACK,Chẩn đoán/history,màn nhỏfont150%,Androidpicker,trạng thái cuối. Ảnh01–37là trước sửa/trung gian, giữ riêng, không gán nhầm binary.

## 6. APK và điều kiện cài

[APK test cục bộ](apk/sumo-phone-ui-test-debug.apk) · [SHA256SUMS.txt](apk/SHA256SUMS.txt).

```text
b31ac740afa3515c8cc5a2a344061e170c63a5972ca4beef2058c3ac7c5c736f
```

Kiểm aapt/apksigner/ZIP/zipalign trên tệp thực: [auditcuối](evidence/apk-final-unlock-audit.json)/[bindingbảncàiAVD](evidence/installed-final-binding.json).

| Thuộc tính | Quan sát |
|---|---|
| Package / nhãn | `com.aistudy.myapplication` / `Sumo Controller · Test` |
| Phiên bản | versionName`1.0`,versionCode`1` |
| Android | minSdk`36`,target/compile`37` |
| Loại / kích thước | Debuggable,31.107.957byte |
| Chữ ký | Verifyv2đạt;AndroidDebug,RSA2048 |
| PubliccertSHA256 | `d97c9cee077a242672a3d3b19276d6c9d76983ce862c59b0268b9a154c2915ba` |
| NativeABI | `arm64-v8a`,`armeabi-v7a`,`x86`,`x86_64`;lib`androidx.graphics.path`;zipalign16Kđạt |

CópayloadARM là quan sát từAPK, **không suy từAVDx86vàkhông chứng minh chạy ARM/API36**. SDKhiện chỉ cóplatform37/image37.1x86_64,không cóAPI36runtime;không cài/nângdependency hoặc sửa minSdk để báo đạt. [Inventory](evidence/apk-audit.json).

- **Cài mới:** AVDđã cài thành công; điện thoại phải kiểm API/ABI và app cùngpackage đang cài.
- **Cài đè:** `adb install -r` cùngAPKcuối/signature/version1trênAVDgiữ nguyênbyte3SharedPreferencesvàhistory([evidence](evidence/final-avd-overwrite.json));khônguninstall/cleardata. Đây **không** là bảnBversiontăng/HTTPStrênđiệnthoại. Chưa biết model/ABI/version/chữkýmáythật nên chưa cam kết cài đè giữdata.
- **Khóa ký:** đây là **debug cài tạm**, không phải APKtestkýổnđịnh lâu dài. Không đổi khóa, đọc/ghi privatekey hoặc mật khẩu vào repo/log. Nếu đổi sang khóa phân phối khác, Android/updater có thể từ chối cài đè; không gỡ app để né vì có thể mấtdata. Chủ dự án cần chốt khóa/backup/migration trước chuỗi cập nhật lâu dài.

## 7. UDP điện thoại và cập nhật trong app

### UDP

Đã kiểm AVDAPI37↔PythonSUMO-SIM/1-draft1qua`10.0.2.2:8888/8889`, không phải firmware/xe. Simulator chỉ choIPv4loopback; điện thoại không dùng`10.0.2.2`để tớiPC. LAN/route/firewall/quyềnmáythật và I11(LANcònroutekhimấtInternet) chưa kiểm chứng. Không tự mở bind0.0.0.0/firewall ngoài phạm vi.

### Updater

Đã đọc UpdateInfo,HttpsTransfer,AppUpdateManager,InstallResultReceiver và test tương ứng:

- HTTPSmetadata≤64KiB,URLkhônguserinfo/fragment;redirectHTTPS/cùnghost/tốiđa3lần;APK≤100MiB,cancel/timeoutcó xử lý.
- Schema1/channeltest,đúngpackage,versionCode>đangcài,minSdk≤máy;kiểmbytes/SHA256/manifest/versionName/minSdk/package/cert. Android kiểm chữ ký khi cài.
- PackageInstaller`USER_ACTION_REQUIRED`,quyềnInstallunknownapps/xácnhận hệ thống; Hủy/lỗi không tuyên bố thành công. Mở updater đóng phiên control, quay lại không tự chạy.
- History là thông báo cài lần cuối lưu qua processrecreate, không phải sổ kiểm toán đủversion/hash/session. Prompt/Cancelnativecũ chỉ là localfixturev2, chưa càiB/chưaHTTPS; không nhận là test mới.

**Còn thiếu:** hosting/feedHTTPS thật và quyền sở hữu; B/CversionCode>1và tăng liên tiếp/cùngsigner; khóa ổn định+backup doownerquảnlý;điệnthoại chỉ định; A→B→C giữ cấu hình/history/dừng an toàn. APKversion1hiện tại không được updater coi là “bản mới” cho appversion1. Chưa uploadAPK/metadata/tạo release hoặc cài/gỡ máy thật.

## 8. Hướng dẫn cài thử ngắn

1. Xác nhận Android16/API36trở lên,ABIthuộcAPK;đối chiếuSHA256. Nếu đã có package cùng tên, kiểmversionCode/publiccert trước cài đè. Khácsignature/bản đang cài cao hơn: **dừng**, không gỡapp/cleardata.
2. Khi được chỉ định, chuyển APKcục bộ theo kênh tin cậy, mở bằng trình quản lý tệp. Cho phép “Cài ứng dụng không rõ nguồn gốc” chỉ cho nguồn cài đã chọn, xác nhận Android rồi tắt quyền nếu không cần nữa. Chưa thực hiện trên điện thoại thật.
3. Mở ngang, kiểmOffline/PWM0/0/AUTOkhóa. Chỉ thử UI/3tab/lưu-Hủy-reset/validation/nhật ký,màn nhỏ/font lớn/bàn phím. **Không nối xe**, không tự nghiệm thu động cơ.
4. Nếu lỗi, ghi model/API/ABI/version/hash và thao tác. Chưa dùng`10.0.2.2`để testUDPđiện thoại; updater chưa là kênh sẵn sàng. UDPPC cần kênhLAN/đúng giao thức được duyệt riêng.

Cài thử là bước kiểm chứng tiếp theo, không thay nghiệm thu dùng xe hoặc phát hành. Các FAIL và giới hạn là điều kiện công khai của bản test, không phải lý do xóa dữ liệu hoặc nới tiêu chí.

Kết thúc kiểm tra: ảnh54chứng minh Offline/PWM0/B0W0K0/AUTOkhóa; ba SharedPreferences và size/density/font đã khôi phục, giữ history lỗi mới. Đã đóng receiver và gửi `adb emu kill` cho AVD do phiên này mở; kiểm tra sau đó không còn thiết bị ADB/QEMU/UDP8888–8889. [Môi trường cuối](evidence/final-environment-confirmed.json). Wrapper dịch vụ báo exit127sau khi emu kill được Android xác nhận; không suy thành crashapp.
