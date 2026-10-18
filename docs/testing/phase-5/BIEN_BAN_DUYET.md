# Phase5 — hồi quy và nghiệm thu giai đoạn giả lập

17/10/2026, Asia/Bangkok. **G5 ĐÃ CHỐT CÓ ĐIỀU KIỆN cho chức năng AVD theo quyết định người dùng; giữ STOP/cadence là lỗi mở, chặn dùng xe và release. G3/G4 vẫn CHỜ DUYỆT CHUNG.** Không tự chuyển Phase6/7.

Người dùng cho phép ngoại lệ tiếp tục Phase5 dù G3/G4 còn tồn đọng; không coi là duyệt kết quả trước đó. Không Phase6/7, release hoặc upload công khai. Không sửa scheduler/tracking theo phỏng đoán, không nới ngưỡng G0.

## Kết quả bổ sung mới nhất — sau quyết định phạm vi G5 AVD

Người dùng đã duyệt riêng cadence50ms có jitter+10ms trên AVD, giữ STOP≤10ms; xem [quyết định](DIEU_CHINH_G5_AVD.md). Đã sửa cache snapshot trong Runtime/Session và bỏ CPU sampler chẩn đoán mặc định ở MainActivity. **110/110 unit,lint0lỗi/17warning;6/6 Android trên APK cuối (4UDP+2thu timing);8/8fault trên APK cuối.**28/28UI/Settings/UDP trước khi bỏ CPU sampler được ghi riêng, không cộng trùng.

Cohort21: STOP500 max trước send9,8024ms/sau send11,4174ms,2mẫu sau send>10ms — **FAIL**. Cadence20/30/50 đủ1245/1339/1282interval,max30,9317/38,7934/54,4796ms; đạt cận20–50/20–50/20–60 trong thí nghiệm QEMU power throttling tắt tạm. Đã khôi phục mọi ưu tiên/power policy. Các cohort Normal17/20 vẫn có cadence50>60ms; không tuyên bố toàn bộ môi trường đạt ổn định hoặc chọn cohort21 để xóa lỗi khác.

Chi tiết mã, red/green, mọi cohort16/17/18/20/21 và giới hạn nằm ở [kết quả bổ sung](KET_QUA_BO_SUNG_G5.md). [APK debug](apk/sumo-phase5-avd-debug-v1-g5-check.apk), SHA256 `92abb62d00eb6f841eb8b9eb158d4b0be4b98bd39c6e05bd39b69de79613e5e3`. Soak08 giữ lịch sử; ngày18/10 đã chạy và audit soak33 riêng trên đúng APK cuối, không thay số liệu08.

**Quyết định người dùng sau khi nhận báo cáo:** “Chốt G5 CÓ ĐIỀU KIỆN cho chức năng AVD; giữ lỗi hiệu năng mở, chặn dùng xe và release” (chat17/10/2026). Không đổi kết luận FAIL của mẫu STOP hoặc cadence Normal20, không suy thành người dùng đã trực tiếp lái thử hoặc G6/G7 được duyệt. Chỉ chốt phần chức năng AVD; điều kiện hiệu năng/thiết bị thật và quyền đóng gói còn mở.

**Tiếp tục18/10/2026 đã hoàn tất phần kiểm chứng độc lập:** nguồn63file/app/test APK giữ nguyên và khớp cohort21. Trace scheduler/Binder/ART không ủng hộ Binder mỗi poll; STOP500 chẩn đoán max4,1192ms và cadence50 n1285,max52,4731ms không đóng FAIL20/21 vì có tracing/điều kiện khác. Không sửa mã phỏng đoán. **25/25Android,0fail/skip trên APK cuối** ở `evidence/33-final-functional-soak/`: 23caUI/Settings/updater+1soak+1probe. Soak30phút0,668giây,600chu kỳ,20reconnect,10Kill/mở khóa; audit60.557frame/21SID không hồi sinh/đảo seq,20checkpointworker/trace1/1,FDsocket14→10sau đóng. XML/raw/CSV/`soak-summary.json`/`final-binding.json` giữ riêng. Đã xem ảnh/XML mở lại Offline,PWM0; tắt trace/receiver/AVD phiên này. Unit/lint/UDP/8fault tái dùng bằng chứng khớp nguồn/binary, không cộng test trùng. Chi tiết ở báo cáo bổ sung và hồ sơ30–33.

**Lỗi mở:** chưa tái hiện vi phạm kèm trace đầy đủ để xác định nguyên nhân chung và sửa STOP/cadence. Giữ FAIL20/21; bước hiệu năng tiếp theo cần mẫu lỗi đồng thời scheduler/ARTtrace không overwrite trên điều kiện mục tiêu có ghi nhận, hoặc lỗi deterministic. Điện thoại/HTTPS/API36/I11/hosting/khóa phân phối+backup/firmware/xe thật vẫn CHƯA KIỂM CHỨNG. G5có điều kiện, G3/G4chờ duyệt; không Phase6/7,dùngxe,upload,release.

**Các mục tiếp theo giữ hồ sơ08–15 trước sửa như lịch sử. Trạng thái APK mới dùng phần bổ sung ở trên và báo cáo liên kết, không dùng số liệu08 để thay kết quả mới.**

## Phạm vi và artifact

Không sửa mã sản phẩm, Gradle, SDK, dependency, applicationId hoặc khóa ký. Thêm `app/src/androidTest/java/com/aistudy/myapplication/Phase5RegressionTest.kt` (5 phương thức) và `SoakTest.kt` (probe+soak); sửa công cụ `tools/check_android_permissions.py` để chọn đúng nhãn Kết nối/Kết nối lại và lưu bằng chứng riêng. Các script điều phối/audit nằm trong `evidence/`, không phải framework điều khiển mới.

Baseline `evidence/00-source-before.json`; môi trường `00-environment.txt`. AVD Medium_Phone/emulator-5554, Android17/API37/x86_64,1080×2400/density420, CLI headless/software. Receiver Python chuẩn, SUMO-SIM/1-draft1/caps3, chỉ loopback8888/8889, AUTO khóa. Unit dùng JBR25/Gradle9.5/AGP9.3.3 hiện có. Không có ảnh API36 hoặc điện thoại đã xác nhận.

APK: [sumo-phase5-avd-debug-v1.apk](apk/sumo-phase5-avd-debug-v1.apk), package `com.aistudy.myapplication`,versionCode1/versionName1.0,min36/target37, ký Android Debug đang dùng cho AVD. SHA256:

```text
72861bd59626aff5975f688b42cac07a2ad1f3b1cc193f92f4485f6dd7be5833
```

`08-apk-identity.txt`, `08-apk-signature.txt`/exit0, `08-apk-sha256.json` và `08-measured-binary-binding.json` xác nhận app/test APK cài trên AVD khớp binary và56file app/test giữ nguyên manifest đầu lượt08. Chưa phải chuỗi phân phối điện thoại; bốn biến ký phân phối chưa cấu hình, backup chưa được xác nhận. [Hướng dẫn mở thử/tái hiện](HUONG_DAN_THU_AVD.md).

## Kiểm tra thực tế

Đường dẫn rút gọn dưới đây thuộc `evidence/`; từng log có lệnh và exit code. Không cộng lượt chạy lại thành số phương thức độc lập.

| Kiểm tra | Kết quả | Bằng chứng |
|---|---|---|
|Toàn bộ unit/lint sau sửa harness|108/108,0fail/error/skip; lint0lỗi/17warning; exit0|`08-final-jvm-lint.txt`, `08-final-unit-xml/`, `08-final-lint.*`, `08-final-jvm-summary.json`|
|Build debug/androidTest|exit0; không release|`01-build-added-tests.txt`, `03-final-jvm-lint-build.txt`, `07-reconnect-harness-check.txt`|
|Python self-test|31/31,exit0|`04-python.xml`, `04-python-trace.jsonl`, `04-final-python-and-android.txt`|
|Kotlin/Python exact bytes|25/25vector,exit0|`08-tools-selfcheck-and-vectors.txt`, `03-kotlin-vectors.tsv`, `08-kotlin-python-vectors.jsonl`|
|Toàn bộ instrumentation cùng APK|37 phương thức đã chạy,36đạt/1thiếu quyền fixture,0skip; wrapper exit1|`08-final-android/instrumentation.txt/xml`, `summary.json`, `08-final-android.txt`;35phút26,225s theo runner Android|
|Bù đúng ca installer|1/1 sau cấp quyền UI và kiểm hash fixture; Cancel thật,version1/cấu hình không đổi; exit0|`14-installer-complete.txt`, `14-installer-complete/{instrumentation.xml,summary.json,native-installer-prompt.png,commands.json}`|
|8 lỗi mạng xác định|8/8,exit0; cùng một phương thức AndroidFaultTest ở8cấu hình|`13-faults.txt`, `13-faults/`, `13-fault-scenarios.xml`|
|LAN API37|Deny/grant/revoke qua hệ thống, receiver dừng và mở lại trung tính; exit0|`11-permissions-label-fix.txt` và thư mục cùng tên,XML/ảnh/JSON/receiver|
|Macro/lifecycle/reboot|Các nhánh đã thực thi trước assert resize của harness: updater hủy macro2000ms, drop-rx20/watchdog hủy macro, force-stop khi chạy, reboot thật giữ record|`10-lifecycle-keyboard-fix/` raw/ảnh/XML/commands; log wrapper exit1 vì đòi Offline sau resize quá mức E03|
|Resize bổ sung đúng E03|STOP trước injected-up, không replay; khôi phục kích thước vẫn PWM/B0, cùng SID; exit0|`10-resize-check.txt`, `10-resize/{summary.json,receiver.jsonl,resized.png,commands.json}`|
|I11 LAN còn route khi mất Internet|CHƯA KIỂM CHỨNG; fixture dừng trước đặt rule do AVD production không root|`12-no-internet-check.txt`, `12-no-internet/commands.json`: `adbd cannot run as root in production builds`; không có rule được tạo|

108 unit có1test mẫu.37 Android gồm1mẫu,1diagnostic tracking,1probe,1soak, các ca chức năng và2ca thu timing. Không gọi36+1 là một lượt37/37 xanh. Lượt03 unit trước sửa harness được giữ riêng; không cộng thành216. Profile tracing/allocation P01 được lưu riêng và không dùng thay dữ liệu nghiệm thu. Các test interop cũ tái tạo tệp không gắn phiên bản ở Phase2; đã chép kết quả lượt này sang `08-final-udp-kotlin-python.jsonl`/`08-final-kotlin-vectors.tsv`. Bằng chứng Phase2 có số lượt và toàn bộ32–34 vẫn được giữ.

## Soak30 phút

`09-soak-check.txt` exit0, `09-soak-summary.json`, `09-soak-resources.csv` và `08-final-android/test-output/` giữ dữ liệu. Thời gian từ begin tới finish **1.801.232ms (30phút1,232s)**;600chu kỳ nhấn/nhả,20Reconnect,10Kill/mở khóa. Boost mỗi5chu kỳ, HOLD mỗi7chu kỳ; reconnect khi ngón cũ còn giữ, move cũ phải trung tính. HOLD/TOGGLE, joystick, macro, Brake và lifecycle được kiểm tra thêm trong các ca riêng.

20checkpoint ổn định ghi worker/trace1/1; tổng FD socket14 không tăng, sau đóng Activity còn10 và worker/trace0/0. FD này bao gồm Unix socket, không phải chỉ UDP; đây là sampling tài nguyên, không phải profiler liên tục. Audit đủ **61.097 frame gửi/áp dụng,21SID**,57.925 frame định kỳ trung tính sau STOP;0 chuyển động thiếu thao tác điều hướng mới, không đảo seq áp dụng. Audit âm tính có self-check cho STOP/Kill/reversed-seq; `08-analyzer-selfcheck.txt` là chẩn đoán riêng.

Logcat main/system/crash không có crash/ANR app trong lượt08. Exit-info kết thúc PID12546 là USER REQUESTED/FORCE STOP “finished inst”, không phải crash hoặc ANR. Bằng chứng không khẳng định phần cứng đã dừng. Haptic weapon đúng1lời gọi cho một lần giữ; cảm giác rung chưa kiểm chứng.

## Một cohort timing cuối

Chỉ lượt08 bắt đầu các mốc BEGIN/END STOP500 và CADENCE20/30/50. Các lượt harness04/06 dừng trước timing. Raw logcat/receiver, CSV/JSON được giữ nguyên; không bỏ outlier, giảm cấu hình50 hoặc thay gate bằng p99.

STOP giữ mốc G0: t0 callback `MainActivity.event` nhận release; t1 trước send, t2 sau send; cùng System.nanoTime. Không đo touch vật lý/firmware/motor.

| STOP,ms |n|p50|p95|p99|max|>10ms|
|---|---:|---:|---:|---:|---:|---:|
|t1−t0|500|0,7876|2,4389|3,5960|5,7437|0|
|t2−t0|500|1,5068|3,7589|4,9205|6,6546|0|

`09-stop-check.txt` exit0. Cadence xét các cặp gửi định kỳ liền nhau cùng SID, không chen event, đúng định nghĩa đã duyệt;≥1000interval/cấu hình và mọi mẫu20–50ms.

|Cấu hình,ms|n|min|p50|p95|p99|max|<20|>50|Kết luận|
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
|20|1261|20,6617|22,0889|23,8799|25,1034|35,2186|0|0|PASS|
|30|1337|21,0575|29,9858|31,5739|32,4521|38,9095|0|0|PASS|
|50|1283|36,2196|49,9588|51,6869|52,4027|58,4052|0|626|FAIL|

`09-cadence-check.txt` exit1 đúng do626mẫu vượt50ms. Test instrumentation thu mẫu thành công không làm số đo đạt ngưỡng. So với34: STOPmax6,9630→6,6546; cadence50 641/1282,max151,6225→626/1283,max58,4052. Không đổi mã sản phẩm nên khác biệt giữa cohort không chứng minh tối ưu hoặc bảo đảm thời gian thực; không quy tất cả cho AVD/GC/tracking. Không đo thêm để chọn kết quả đẹp.

## Đối chiếu E01–E08

|Ca|Kết quả phạm vi giả lập|Bằng chứng|
|---|---|---|
|E01|PASS D-Pad4/joystick/limiter/Boost/HOLD/TOGGLE/Brake/Kill/macro/STOP|08 Phase5Regression/AndroidSimulator/ControllerUi và receiver hai phía|
|E02|PASS hai pointer+ngón thứ ba Kill; reconnect/move cũ không phục hồi|08 Phase5Regression E01/E02; Soak600/20|
|E03|PASS phần không khôi phục chuyển động; cấu hình qua reboot, force-stop, resize|10-lifecycle-keyboard-fix/10-resize. Resize/Leave được phép Online+trung tính; nền/reboot phải Offline|
|E04|PASS8scenario, thêm macro active mất gói/watchdog|13-faults/ và10 macro-drop-rx.jsonl; SDK API37|
|E05|PASS soak theo cỡ mẫu G0|09-soak-summary; sampling/giới hạn nêu trên|
|E06|PASS đồng nhất app/test binary, nguồn và hợp đồng|00baseline,08source/binary-binding,APK/SHA, P31/vector25, hướng dẫn|
|E07|PASS cohort500 callback-to-send≤10ms|09-stop CSV/JSON; không suy vật lý|
|E08|PASS API37; API36 CHƯA KIỂM CHỨNG|11-permissions-label-fix; chưa có image/điện thoại API36|

## F01–F10

|Ca|Kết quả/bằng chứng thực tế|Phần chưa đủ|
|---|---|---|
|F01|PASS AndroidSimulator limiter30/60/100→77/153/255, cùng version; SettingsUpdate recreate|Bánh thực|
|F02|PASS UI duration350→frame/macro receiver, unit idempotency; fixture2000 cho hủy macro|Góc180°/calibration xe|
|F03|PASS mixer/validation/joystick và UI gain.7/.8/sensitivity1.5/deadzone.2→PWM100/115; new test deadzone.1→119|Hiệu chỉnh động cơ thực|
|F04|Áp dụng20/30/50 một owner PASS; chuẩn cadence tổng thể FAIL ở50|Giữ ngưỡng G0|
|F05|PASS unit toàn miền/NaN/Infinity/IP/template và Android invalid-port/không save một phần; lỗi IME fixture cũng bị chặn|Không có lỗi app mới cần sửa|
|F06|PASS store/recreate/force-stop/reboot thật, port8889/duration2000 giữ nguyên, startup trung tính|Không lấy reboot thay A14 sau update|
|F07|PASS mở Settings khi giữ, dừng endpoint cũ rồi8889/aliasD/advanced; không rebuild|Firmware thật|
|F08|PASS hủy giữ record, reset chỉ sau xác nhận; 08 Phase5Regression F08|Không thay identity/key/version|
|F09|PASS engine fixture ACK lỗi/timeout/unsupported/atomic trong unit/Python; UI cơ khí khóa theo G0|CHƯA KIỂM CHỨNG MCU/cơ cấu|
|F10|PASS schema0→1/default/backup/corruption giữ bản gốc; Android Store/JVM|Migration qua APK A→B→C chưa có điều kiện|

## A01–A14

PASS logic/platform được tách khỏi toàn luồng HTTPS; không dùng fixture.invalid, adb install hoặc prompt/Cancel thay hosting/update thật.

|Ca|Đã kiểm chứng độc lập|Trạng thái phần còn thiếu|
|---|---|---|
|A01|Phiên bản package thực, parser/types/metadata và UI updater|CHƯA KIỂM CHỨNG feed/changelog tải từ host thật|
|A02|PASS logic schema/package/version cũ không được nhận; JVM+Android|Kênh thật chưa có|
|A03|PASS stream/progress/cancel/oversize/đóng connection ở JVM|CHƯA KIỂM CHỨNG tải/hủy/thử lại HTTPS trong app|
|A04|PASS hash/size/archive guard; fixture ZIP bị cắt bị app từ chối thật|Không có tải hỏng từ kênh thật|
|A05|PASS identity/signers/version; Android tamper signature và đổi feed chặn trước request/install|Không đề nghị uninstall để vượt guard|
|A06|PASS metadata/installed SDK/archive SDK validation; min36 thực|Chạy trên API36 CHƯA KIỂM CHỨNG|
|A07|PASS guard khi thiếu quyền ở08, grant/restore qua màn hình Android và native prompt ở14-complete|Chưa phải cài cập nhật thành công|
|A08|PASS JVM HTTP/cancel/redirect host/HTTPS/count/timeouts và Android JSON lỗi|CHƯA KIỂM CHỨNG TLS/timeout từ host thật|
|A09|PASS prompt/Cancel thật, version1/settings byte-identical; callback storage-failure tổng hợp ở08|Không ghi thiếu dung lượng thật đã thử|
|A10|CHƯA KIỂM CHỨNG|Thiếu điện thoại, HTTPS/đích upload, khóa ổn định+backup; chưa A→B→C|
|A11|PASS migration/corruption fixture F10|CHƯA KIỂM CHỨNG nâng APK/migration lỗi ở B/C|
|A12|Có quy trình upload APK trước metadata; JVM HTTP404 xử lý lỗi|CHƯA KIỂM CHỨNG công bố/cache thật|
|A13|PASS UDP thật: đang giữ hướng+Boost và macro active2000ms→STOP/C/ACK trước updater, không auto reconnect|Tải/cài qua hosting thật chưa chạy|
|A14|Ordinary reopen/reboot trung tính đã thử|CHƯA KIỂM CHỨNG mở lại B/C sau trình cài; không dùng force-stop thay update|

## Lỗi, sửa chữa và dữ liệu không bị che

Không có sửa logic sản phẩm. Các lỗi harness đều giữ nguyên red/log/XML và chỉ chạy lại bài liên quan:

- 04 output external do shell tạo không ghi được từ UID app; sửa app-private/run-as tar, probe05đạt.06 performClick pointer0 khi đang giữ pointer0; dùng pointer9, probe07và soak20Reconnect đạt. Cả04/06 chủ động dừng trước timing; “Process crashed” trong transcript là force-stop điều phối, không phải crash app tự phát.
- 10 tọa độ Lưu lấy khi IME chưa ổn định bấm vào ký tự `l`; port8889l bị app chặn, record cũ giữ. Ẩn IME trước đọc bounds.10-keyboard-fix chạy đủ macro/force-stop/reboot nhưng assert Offline sau resize quá mức E03;10-resize xác minh đúng PWM/B0/no replay (first-moving→zero186ms trước injected-up5s, không phải mốc STOP≤10ms).
- 11 permission tool còn tìm nhãn cũ “Kết nối lại” khi Offline hiện “Kết nối”; XML thật tái hiện0match, selector hai nhãn có1match;11-label-fix chạy hệ thống đạt.
- 08 native installer thiếu quyền;14 lần đầu selector kỳ vọng Switch nhưng API37 là View checkable.14-checkable gặp fixture1773byte: đúng vị trí Ctrl-Z đầu tiên trong host APK30762914byte. Đường stdin Windows bị cắt; app từ chối ZIP thật. Sửa adb push+copy trong guest, đối chiếu SHA `009a8da2d1c3dc67c22ccb19ddc612fc5617bd6aad4b6280aa7b19d128c8ca49`,14-complete1/1 đạt; giữ cả bản cắt và JSON chẩn đoán. Runner có preflight quyền và kiểm hash để tránh lặp lỗi chuẩn bị.

## Gate và điều kiện còn thiếu

[Ma trận R01–R23](MA_TRAN_R01_R23.md) nêu PASS/FAIL/CHƯA KIỂM CHỨNG theo môi trường. **Chưa đề nghị G5 đạt**, chưa ghi người dùng đã trực tiếp thử/duyệt. G3/G4 không được tự xác nhận từ ngoại lệ Phase5.

Tồn đọng: cadence50 FAIL; I11 thiếu môi trường LAN còn route khi WAN bị chặn (ảnh hiện tại không root); API36/điện thoại chưa có; hosting URL/đích upload và khóa ký phân phối ổn định+backup chưa được xác nhận. R07góc, R08hãm, R15MCU, R17cảm giác rung, R19độ trễ hệ thống và tương thích firmware vẫn CHƯA KIỂM CHỨNG. Không có lỗi an toàn mới được tái hiện trong phạm vi app/simulator đã chạy; điều đó không chứng minh xe thật an toàn.

Đề xuất giữ ngưỡng G0 và trạng thái FAIL; cần môi trường/thiết bị phù hợp để có bằng chứng độc lập, thay đổi tiêu chí phải có quyết định mới trước lượt tương lai. Không triển khai Phase6/7, không release hoặc tự đăng APK. Bản debug/hướng dẫn/bằng chứng được giao để kiểm tra ngay.
