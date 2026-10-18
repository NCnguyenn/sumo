# Hồ sơ Phase 3 — giao diện, cấu hình và updater

**Cập nhật sau Phase5,17/10/2026:** người dùng chỉ cho ngoại lệ chạy phần độc lập Phase5, G3/G4 vẫn CHỜ DUYỆT CHUNG. [Hồ sơ Phase5](../phase-5/BIEN_BAN_DUYET.md) có unit108, soak600/20, STOP500 đạt/cadence50FAIL, API37 và installer prompt/Cancel thật. Không có A→B→C HTTPS/điện thoại/khóa phân phối+backup; chưa xác nhận R23 đạt. Nội dung bên dưới giữ lịch sử Phase3.

Ngày 17/10/2026, Asia/Bangkok. Người dùng đã duyệt G1/G2 theo hồ sơ cuối 60/60 và giao thực hiện liên tục Phase 3 rồi Phase 4, không chờ duyệt giữa đợt. **G3/G4 CHỜ DUYỆT CHUNG**, chưa có quyết định nghiệm thu của người dùng và không chuyển Phase 5/release. Nội dung này ghi phần đã kiểm chứng cùng phần còn thiếu, không coi toàn bộ R23 đã đạt.

## Đã triển khai

Thay màn hình mẫu bằng ControllerScreen ngang/fullscreen. Theo phản hồi người dùng, đã chỉnh D-Pad thành chữ thập với phím vuông, joystick có cần hiển thị vị trí thật, các cụm nút cân đối, hiển thị riêng hai bánh và theme tối thống nhất. Giữ màu SRS: Boost đỏ, vũ khí cam, quay vàng, Brake xanh; Kill nổi bật ở giữa. Cấu hình dùng hai cột, chia nhóm và giữ nút Lưu ở chân dialog; updater gọn theo chiều ngang; vùng chạm tối thiểu 48 dp trên hai kích thước AVD đã thử. D-Pad/joystick đưa ID pointer thật vào reducer; quyền sở hữu giữ từ down tới up/cancel. Nhả/mất vùng một hướng vô hiệu nhóm hướng cũ, chạm đến sớm không phục hồi chuyển động; nhả riêng Boost giữ hướng, nhả hướng giữ HOLD/TOGGLE. Cancel, mất focus và ra nền kết thúc phiên; trở lại luôn trung tính, không tự reconnect.

Kill truy cập được bằng chạm/semantics, giữ latch qua tạo lại Activity. Mở khóa giữ 1 giây và chờ ACK; có haptic cho thao tác vũ khí và thay đổi Online/Offline, không rung theo từng heartbeat. AUTO mặc định khóa với lý do firmware/thuật toán chưa được duyệt; không dựng tự hành. Màn hình phân biệt PWM yêu cầu và ACK cuối từ receiver bản thử, không tuyên bố xe thật đã dừng.

Settings có đủ limiter, sensitivity, dead zone, gain hai bánh, mức quay, thời gian macro, HOLD/TOGGLE, D-Pad/joystick, IPv4/port, chu kỳ 20–50 ms, template S/D và URL HTTPS feed. Mặc định/miền giữ G0; toàn bản phải hợp lệ trước lưu, lỗi hiển thị tại trường. Limiter trên màn hình lái áp dụng live; nâng cao dừng/đóng phiên trước áp dụng. URL feed đổi cần xác nhận, mặc định trống. Metadata/ready APK gắn với đúng feed đã kiểm tra; đổi sang nguồn khác vô hiệu nút tải/cài và backend từ chối metadata cũ trước HTTP hoặc PackageInstaller. Tham số cơ khí xe chưa có hợp đồng nên không gửi góc servo; fixture Q/capability/ACK kiểm tra riêng trong Phase 4.

SettingsDocument/ControllerSettingsStore dùng record schema1; schema0 giữ trường cũ và thêm mặc định trường mới, giữ backup khi migration. Schema lạ/hỏng giữ raw và khóa điều khiển, không âm thầm xóa. Không khôi phục pointer/Boost/macro/AUTO/SID khi mở app. Kill latch lưu riêng cho an toàn.

Updater chỉ hoạt động theo thao tác người dùng: HTTPS metadata, giới hạn 64 KiB/100 MiB, cùng host đã duyệt, tối đa 3 redirect HTTPS, timeout 10 s/30 s; kiểm tra schema/channel/package/version/minSdk/size/hash và signer APK với app đang cài. Có kiểm tra APK thực qua API Android, PackageInstaller yêu cầu xác nhận, màn hình quyền cài theo app, pending user action và kết quả/hủy/lỗi. Phiên lái phải ACK đóng hoặc hết deadline watchdog trước mở updater; không tự phát lại chuyển động sau cài. Chưa có địa chỉ phân phối thật, không tạo URL hoạt động hoặc upload.

## Tệp thay đổi chính

- UI: `MainActivity.kt`, `ui/ControllerScreen.kt`, `SettingsDialog.kt`, `AppUpdateDialog.kt`, `ui/theme/Theme.kt`.
- Cấu hình: `settings/SettingsDocument.kt`, `ControllerSettingsStore.kt`; model/limiter/calibration G1 được tái sử dụng.
- Cập nhật: `updates/UpdateInfo.kt`, `HttpsTransfer.kt`, `AppUpdateManager.kt`, `InstallResultReceiver.kt`.
- Cấu hình app: `AndroidManifest.xml`, `res/values/strings.xml`, `app/build.gradle.kts`, `.gitignore`. Signing/distribution có kiểm tra riêng; không đổi SDK/dependency/wrapper.
- Test: JVM SettingsDocument/UpdateInfo/HttpsTransfer; Android ControllerUi/SettingsUpdate/ApkSignature/InstallerPlatform. Script capture trong evidence và công cụ kiểm tra Phase4; danh mục đầy đủ tại manifest nguồnG4.

Các đường mã Kotlin ở trên cùng gốc `app/src/main/java/com/aistudy/myapplication/`; test nằm dưới `app/src/test` và `app/src/androidTest`. Biên bản/kế hoạch/mô tả/checkpoint đã đồng bộ trạng thái và giữ nguyên kết quả lịch sử.

## Lệnh, test đỏ và kết quả đã quan sát

Gradle dùng JBR25/Gradle9.5/AGP9.3.3 như baseline, không thêm dependency hoặc đổi SDK. Lệnh chạy tại gốc workspace với `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`, `JAVA_TOOL_OPTIONS='"-Djdk.net.unixdomain.tmpdir=C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT\.gradle"'`. Native stdout/stderr thu bằng Tee-Object, giữ exit và XML theo lượt. Dòng COMMAND trong từng log là lệnh đầy đủ, gồm filter.

| Lượt tại `evidence/` | Lệnh/kiểm tra | Kết quả thật |
|---|---|---|
| 01 → 03 | `:app:testDebugUnitTest --tests *.SettingsDocumentTest --tests *.UpdateInfoTest` | Red 7/7 tại stub; green 7/7, exit0 ở phần JVM03 |
| 02 → 03 → 05 | `:app:connectedDebugAndroidTest`, filter ControllerUiTest | 02 red orientation expected6/actual-1; 03 red 4/4 màn hình/pointer chưa có. 05: 3/4 pass, 1 lỗi assertion test đọc Text trong khi node có ContentDescription; sửa assertion đúng semantics, không coi đây là lỗi Kill |
| 04 → 05 | JVM HttpsTransferTest | 3/3 red stub → 3/3 green; đọc/tải/hủy/giới hạn/HTTP lỗi/redirect |
| 06 | Toàn module JVM/lint + instrumentation | Exit0, 70 JVM và 11 instrumentation ở thời điểm này; chưa có ca bổ sung sau đó |
| 07 → 09 | Updater limits G0 | 5 chạy/2 fail: APK >100 MiB và redirect/timeout chưa đúng G0; sau sửa 09 xanh |
| 08 → 09 | Kill qua Activity recreate | 1 fail: latch mất khi tạo lại màn hình; sau lưu latch riêng, 09 xanh |
| 09 | JVM/lint + toàn instrumentation lúc đó | **71/71 JVM, 12/12 instrumentation**, exit0; lint0 error/18 warning, gồm12 baseline và cảnh báo orientation/UseKtx |
| 10 → 11 | AVD nhỏ900×1600, density420 | Red nút Phải chỉ13,33dp; sửa kích thước D-Pad theo không gian, **13/13 instrumentation** xanh; không hạ chuẩn48dp |
| 12 | `help -PsumoDistribution=true -PsumoVersionCode=2` | Expected exit1 do thiếu SUMO_KEYSTORE; xác nhận không fallback khóa debug |
| 13 probe | ApkSignatureTest trên API37 | **1/1 pass**, không phải red: APK nguyên gốc đọc được signer; sửa byte DEX nhưng giữ chứng thư bị API package từ chối. Giữ log probe exit0, không bịa một lượt thất bại hoặc thêm thư viện không cần |
| 14 → 26 (Phase4) | Geometry D-Pad | Red phím Tiến231×589px; sau sửa chữ thập/vuông, kiểm tra hình học đạt trong hồi quy24/24 Android |
| 15 | Lượt UI bị ngắt | Chỉ13/18 ca chạy,12 pass; adb install nhầm lúc test chạy làm process kết thúc. Giữ log/giới hạn, không dùng làm kết luận xanh |
| 16 → 26 (Phase4) | Accessibility quay | Còn17/18 ca đạt; thiếu duration trong contentDescription. Khôi phục nhãn đầy đủ, xanh trong hồi quy26 |
| 17 → 18 | Kill còn giữ + unlock | Red thực sự cho phép bắt đầu unlock dù Kill còn giữ. Chặn đến khi chỉ còn pointer unlock mới; thêm ngón hủy hold. **19/19 UI/Settings/UDP đạt** sau sửa |
| 19 → 20 → 21 | PackageInstaller AVD thật | Build fixture debugv2 rồi khôi phục v1, không phân phối.20 xác nhận callback pending thật; ảnh sau test20 là launcher, không là prompt.21 chụp prompt khi còn sống và bấm Cancel thật: **1/1 đạt**, version vẫn1, record cấu hình byte-identical, nhận STATUS_FAILURE_ABORTED. Không bấm Update, không gọi đây là cài thành công |
| 22 → 23 | Cửa sổ nhỏ / feedback |22 **15/15 Android đạt** ở900×1600/density420;23 **10/10 ControllerUi đạt**, lint0 lỗi/17 cảnh báo. Selected theo pointer hợp lệ, tránh suy nhầm hướng từ gain hai bánh khác nhau |
| 24 → 25 | Ảnh / limiter lưu thật | Chụp5 màn hình thật;25 **6/6 SettingsUpdate đạt**, gồm live limiter30/60/100 và tạo lại Activity nạp đúng từng giá trị. Không sửa Main vì đã có optimistic update |
| 26 → 27 | Dialog ngang | Red IP field không hiện ở form hẹp; đổi Settings hai cột/nhóm/scroll và footer Lưu, **7/7 SettingsUpdate đạt**, validation/store giữ nguyên. Updater gọn theo chiều ngang |
| 28 → 29 → 30 | Binding nguồn cập nhật |28 không có thiết bị, không phải red chức năng.29 red do metadata cũ đi tới kiểm tra quyền cài trước kiểm tra nguồn. Thêm snapshot atomic info/feed và guard download/install,30 **8/8 SettingsUpdate đạt trên cửa sổ nhỏ**, lint0 lỗi/17 cảnh báo. Ca dùng state fixture, không tải HTTPS từ URL giả |

Các lượt 03/05/06 giữ nguyên trạng thái ở thời điểm chạy. Hồi quy sau tích hợp ở [Phase 4](../phase-4/BIEN_BAN_DUYET.md) thay thế kết luận cho mã hiện tại; không cộng tất cả lượt thành tổng test khác nhau. Lỗi đọc chữ ký/phiên bản/hủy installer được phân biệt với kết quả cài Android thật.

## Ánh xạ U/F/A

| Nhóm | Bằng chứng và giới hạn |
|---|---|
| U01/U10 | ControllerUiTest orientation/nhãn/neutral/min48dp, ảnh UI chuẩn/nhỏ. Kiểm tra bằng mắt bố cục, không suy từ số node. Chưa thử tablet/API36 |
| U02/U03 | TouchInput trên **root Compose**, ít nhất2pointer trong cùng gesture; cả hai thứ tự nhả Boost/hướng, hướng cũ/chạm sớm, joystick/Kill. Không thay bằng click liên tiếp một con trỏ |
| U04/U12 | HOLD/TOGGLE/Brake/limiter qua UI + reducer thật có nhãn TEST trong harness; HOLD/Brake qua UDP thật và đổi mode vũ khí qua Settings ở AndroidSimulatorTest. Limiter30/60/100 → receiver77/153/255 trên cùng APK, không đổi version |
| U05/U06/U07 | Recreate giữ Kill; lifecycle về Offline/neutral; UI thật giữ mở khóa1s chờ ACK. Thử đa ngón trong AndroidSimulatorTest; không giả ACK hoặc mở khóa chỉ vì hết timer |
| U08/U09/F05/F06/F08/F10 | Invalid port/giá trị/record/schema, báo lỗi và không áp dụng một phần; round-trip mọi trường, migration0→1/backup/corruption, instance mới đọc lại. Hủy editor không save; reset có xác nhận. Reboot/nâng APK thật phải có bằng chứng riêng, không lấy unit migration thay thế |
| U11 | Log haptic tương ứng press vũ khí và đổi kết nối; không phát theo mỗi ACK. Cảm giác rung điện thoại thật chưa kiểm chứng |
| F01/F02/F03/F04/F07 | AndroidSimulatorTest đổi limiter, gain/deadZone/sensitivity, macro350ms/interval50/endpoint8889/templateD từ UI; receiver nhận PWM100/115 khi joystick70% với calibration mới. Không rebuild giữa các giá trị. Mốc jitter20/50ms/xe thật không suy từ việc lưu được giá trị |
| F09 | Cơ cấu xe thật chưa xác nhận nên trường cơ khí không bật. Engine fixture Q atomic/idempotent, ACK lỗi/timeout/unsupported và khóa lái khi chưa đối soát: test Phase4; không gọi là hiệu chỉnh cơ khí đã đạt |
| A01/A02/A04/A05/A06 | Metadata/version/identity/SDK/hash/signers bằng JVM và parser/archive Android thực; nguồn metadata phải khớp feed hiện tại trước tải/cài (state fixture30); APK tamper probe API37. URL trống báo chưa có hosting, không giả có bản mới |
| A03/A08 | HttpsTransfer dùng stream kiểm soát được cho hủy/quá kích thước/HTTP lỗi/redirect; lỗi JSON/parser kiểm tra trên Android. Chưa có tải HTTPS từ host phân phối thật hoặc thử TLS lỗi qua kênh thật |
| A07/A09 | Mã platform adapter/receiver có xử lý permission/pending user action/kết quả; callback storage-failure trong test là **nhánh tổng hợp**. Lượt21 có prompt và Cancel PackageInstaller thật trên AVD, giữ cấu hình/version1; không phải cập nhật thành công và không thay thế A10 |
| A10/A11/A12/A14 | Hosting/khóa phân phối/điện thoại thiếu. Chưa nghiệm thu A→B→C qua Internet hoặc migration giữa các APK phân phối; hướng dẫn và phần schema độc lập đã chuẩn bị |
| A13 | Mở Settings/updater trong khi điều khiển kết thúc phiên trước tiếp tục; clock-test gate ACK/deadline và Android UI thật. Không có sender thứ hai trong updater |

## Ký, artifact và phần còn thiếu

Giữ identity `com.aistudy.myapplication`; build phân phối có cờ riêng, nhận keystore ngoài repo/secret qua môi trường và yêu cầu versionCode tăng. `.gitignore` chặn keystore/signing.properties. Chưa tạo khóa riêng hoặc chốt backup; APK debug dùng AVD không phải bản đã chốt ký cho điện thoại. Quy trình cụ thể tại [PHAN_PHOI_APK_TEST.md](PHAN_PHOI_APK_TEST.md), không cần một framework/server cập nhật mới.

**Chờ quyết định G3/G4 của người dùng.** Các điều kiện chưa có gồm người dùng trực tiếp thử, kênh HTTPS/khóa/điện thoại và các ca cập nhật thật; mốc10ms và cadence ở Phase4 giữ kết quả đo thực, không gán PASS từ kết quả chức năng. Không phát hành release hoặc chuyển Phase5.

## Kết luận để duyệt chung

Mã độc lập của Phase3 đã triển khai, có test trước/sau cho logic mới quan trọng và đã chỉnh giao diện theo phản hồi. Chỉ xác nhận từng bằng chứng nêu trên. Lượt31 chụp và kiểm tra bằng mắt5 ảnh cuối sau modal mới (giữ ảnh cũ24 riêng), exit0. Phase4-30 kiểm tra lại4/4 Android tích hợp sau các thay đổi UI/updater; đã cleanup hai simulator đúngPID. Ảnh mới tại `evidence/ui-final-landscape.png`, `ui-final-small.png`, `ui-final-settings.png`, `ui-final-joystick.png`, `ui-final-updater.png`; log/XML nằm theo số lượt. Bản APK hiện tại là debug cục bộ/versionCode1, chưa phải APK phân phối với khóa đã chốt. Danh mục nguồn/artifact cuối nằm trong hồ sơG4.

G3 chưa đạt đủ điều kiện nghiệm thu: chưa có A→B→C qua HTTPS trên điện thoại, khóa/hosting chưa cung cấp và người dùng chưa trực tiếp duyệt UI. Số đo STOP/cadence không đạt được ghi tại G4, không che bằng test chức năng. Không tự chuyển Phase5 hoặc tạo release.
