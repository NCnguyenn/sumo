# Đợt34 — sửa trực tiếp codec và kiểm tra cuối

17/10/2026. Người dùng yêu cầu tự sửa các vấn đề đã thấy, không gửi tiếp prompt bàn giao. Phạm vi vẫn Phase3–4; chưa Phase5/release. **STOP đạt trong cohort này; cadence50 CHƯA ĐẠT, G3/G4 CHỜ DUYỆT CHUNG.**

## Thay đổi

`CommandCodec.kt` bỏ Regex tạo theo từng SID/số, bỏ bản sao danh sách byte/String khi kiểm tra ASCII/LF, dùng chung phần thân lệnh lái thay việc encode rồi cắt LF. Kiểm tra ký tự trực tiếp vẫn giữ SID16 hex thường/khác0, miền số, ASCII, LF và giới hạn256 byte. API, định dạng wire, validation ngữ nghĩa, tracker/ACK/replay và thuật toán lịch gửi không đổi. Không đổi cấu hình50ms hay cận20–50ms.

`CommandCodecAllocationTest.kt` là test mới duy nhất. Test trên JBR25, warm-up5000 và đo1000 lần encode cùng một drive frame: trước sửa **1896 byte/frame**, assertion budget1024 thất bại; sau sửa **232 byte/frame**, đạt. Đây là cấp phát JVM, không phải phép đo ART hoặc bảo đảm latency Android. `01-codec-allocation-red.txt/xml`, `02-related-green.txt` và `02-related-xml/` giữ bằng chứng.

**54/54 JVM liên quan đạt:** codec6, allocation1, tracker16, Kotlin/Python interop1, session18, runtime3, scheduling5, UDP4. APK debug và instrumentation build thành công. Không chạy lại toàn97/24/8. `05-source-scope.json` đối chiếu82 file của đợt33: chỉ codec thay đổi, không mất file; thêm allocation test. Gradle/SDK/dependency giữ nguyên.

## Một batch trên AVD

Script `03-run-acceptance.ps1` cài đè bằng `adb install -r` trên đúng emulator-5554, chạy STOP500 và cadence50 một lần qua UDP simulator thật; instrumentation báo **2/2 đạt thu mẫu**,162,357s. Kết quả chấm ngưỡng tách riêng bên dưới. Test lưu/khôi phục cấu hình và Kill. Simulator/logcat do script tạo đã dọn đúng PID; không uninstall app sau đo.

| Chỉ số | Kết quả34 |
|---|---|
| STOP500, t1−t0 max /t2−t0 max |6,5814 /6,9630ms;0/500 vượt10ms — đạt cohort|
| Cadence50 |1282 interval;641>50ms,0<20ms — **CHƯA ĐẠT**|
| Cadence50 min /p50 /p95 /p99 /max |23,1701 /49,9979 /52,5062 /56,0456 /151,6225ms|

`04-stop-check.txt` exit0; `04-cadence50-check.txt` exit1. CSV/JSON và log thô giữ đủ mọi mẫu, không bỏ outlier.20/30ms không đo lại, bằng chứng đạt gần nhất vẫn là32, chưa chứng minh trên build34.

`05-worst-interval.json` ghi cặp send422→423: deadline vẫn tăng50ms, wake trễ3,3236→104,1400ms, xử lý wake→t1 là0,6324→1,4385ms. Phân rã:151,6225=50+(104,1400−3,3236)+(1,4385−0,6324). Log hiện tại không xác định nguồn gây trễ worker; không quy hết cho AVD/Windows/GC. Allocation JVM giảm không đồng nghĩa cadence đạt: số vi phạm và max đều xấu hơn33. Không chạy thêm cohort để chọn kết quả đẹp.

## Bàn giao và giới hạn

APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA256 `76d693f87432834da52fc676dfdbf2d8b914a546cb592e126a335a1c17ea151b`, debug/versionCode1; đang cài trên AVD. Đây là bản thử nội bộ, không phải bản phát hành/cuộc thi.

Chỉ có emulator-5554 kết nối; bốn biến ký phân phối `SUMO_KEYSTORE/SUMO_KEY_ALIAS/SUMO_STORE_PASSWORD/SUMO_KEY_PASSWORD` chưa cấu hình. Hosting/URL thật và điện thoại chưa có: chưa xác minh cập nhật HTTPS A→B→C, API36, firmware/xe thật hoặc I11 trên LAN còn route khi mất Internet. Cadence50 vẫn chặn nghiệm thu. Hoàn tất sửa codec và đợt kiểm tra này; không tuyên bố toàn ứng dụng hoàn thành.
