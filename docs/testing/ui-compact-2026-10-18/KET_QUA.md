# Kết quả chỉnh giao diện compact và Chẩn đoán

Người dùng duyệt mockup v3 và yêu cầu triển khai, gửi ảnh app thật. Thực hiện trên nhánh cục bộ `codex/ui-compact-diagnostics`. Đây là đợt bổ sung giao diện, không tự chuyển Phase6/7 hoặc duyệt G3/G4/G5 toàn bộ.

## Đã thay đổi

- `ControllerScreen`: D-Pad/joystick gọn hơn, bỏ cột công cụ giữa, Kill/chế độ trên thanh đầu, dải cảnh báo cố định, ping/tuổi ACK, phân biệt App PWM/ACK PWM. Giữ nguyên thuật toán xử lý pointer và reducer/mixer.
- `SettingsDialog`: modal hai cột với ba tab Chung/Hiệu chỉnh/Cập nhật; giữ bản nháp khi đổi tab và validation/lưu toàn bộ/Hủy/reset/xác nhận nguồn. `AppUpdateDialog` và theme dùng bố cục/màu mới.
- `DiagnosticLog`/`DiagnosticsScreen`: dữ liệu thật từ trace, nhóm App/Mạng/Giao thức/IoT, TX/RX/bị loại, mã/lý do ACK, bộ lọc, clipboard, xuất bằng CreateDocument và lưu phiên lỗi trước. Ring240dòng, mỗi dòng≤600ký tự; báo cáo≤80.000ký tự (≤240.000byte UTF-8); chỉ tải lịch sử≤256.000byte. Không có lệnh shell nhập tay hoặc telemetry phần cứng giả.
- `ReplyTracker` chỉ bổ sung lý do loại bỏ; điều kiện chấp nhận SID/seq/ACK/replay giữ nguyên. `ControlSession` bổ sung trace metadata/báo ACK/lý do loại; file I/O chạy ngoài owner/sender. Main đọc summary immutable không chờ khóa ghi file; UI cập nhật theo đợt250ms.
- Phân biệt yêu cầu app hiện tại với TX thành công cuối cùng khi gửi STOP gặp lỗi. Metadata phiên được ghi trước khi open/permission có thể thất bại; lịch sử giữ thông tin phiên gốc. Nội dung chờ xuất được lưu/khôi phục khi Activity tái tạo, không lặng lẽ để file rỗng.

## Kiểm tra và cách đếm

| Kiểm tra | Kết quả và bằng chứng |
|---|---|
| Unit toàn module | **118/118**,0fail/error/skip; gồm117ca dự án và1ca mẫu. `12-final-build.txt`, `unit-xml/` |
| Lint | **0lỗi,17cảnh báo**; `lint-results-debug.xml`, `14-recheck-build.txt` |
| Android chức năng | **33case phân biệt đã đạt trên cùng APK app**; xem cách ghép bằng chứng dưới đây |
| Lỗi mạng | **8/8**: mất gói, mất phản hồi, trễ150/350ms, trễ phản hồi, trùng, đảo thứ tự, malformed; `15-final-faults.txt`, `faults/android-fault-summary.json` |
| Xuất nhật ký | Hộp Android thật lưu file20.059byte; `exported-diagnostic.txt`, `export-verification.json`; ca Hủy và Activity tái tạo cũng đạt |
| Ảnh | 13PNG/XML;12ảnh app ngang+1hộp Android dọc. [Gallery](../../screenshots/ui-compact-2026-10-18/README.md) |

Đợt34 (`../phase-5/evidence/34-ui-compact-initial/`) đạt29/29 trên binary trung gian; không cộng vào33. Đợt35 (`../phase-5/evidence/35-ui-compact-final/`) có35ca:33chức năng+2thu timing; kết quả34đạt/1fail/0skip. Ca mới `diagnosticsWhileHoldingClosesSessionAndCopiesRealReport` đọc nhật ký trước đợt cập nhật250ms nên không thấy node log. Chỉ sửa test để đợi dữ liệu; không sửa app, không nới tiêu chí an toàn. Đợt36 chạy lại5ca `AndroidSimulatorTest`, **5/5đạt**. Đối chiếu theo classname+method cho **33ca chức năng phân biệt đạt**; không nói toàn bộ35đã chạy lại đạt. Hai ca thu timing không được cộng vào33.

`final-summary.json` lưu danh sách33case, cohort của từng case, hash nguồn/APK/testAPK, thống kê và đối chiếu nguồn sản phẩm giữa35/36/hiện tại. Chênh source35→36 chỉ là dòng chờ trong test AndroidSimulatorTest; APK app không đổi. Kiểm tra red/green giữ ở `05-red-unit-assertions.txt`, `09-red-diagnostic-edges.txt`, `11-red-export-recreate.txt` cùng XML red; các log01–03 là lỗi môi trường/thiếu lớp trước triển khai, không thay bằng chứng assertion thất bại.

## Timing trên APK mới

Chỉ chạy một batch thu timing35, không đổi ưu tiên/power policy và không chọn bỏ mẫu. Cận STOP≤10ms; cadence20/30 trong20–50ms, riêng50 trên AVD trong20–60ms theo ngoại lệ đã duyệt.

| Chỉ số | Mẫu | Max | Kết quả lượt này |
|---|---:|---:|---|
| STOP t1−t0 |500|3,2669ms|Đạt|
| STOP t2−t0 |500|3,8244ms|0/500vượt10ms|
| Cadence20 |1289|26,9885ms|Đạt|
| Cadence30 |1337|34,3807ms|Đạt|
| Cadence50 |1282|53,4177ms|0vượt60ms;634mẫu vẫn>50ms|

Checker `tools/check_android_timing.py` và `tools/check_android_cadence.py --g5-avd-50-jitter` đều exit0. CSV/JSON và log `16-cadence-check.txt`, `17-stop-check.txt` giữ đủ mẫu. Đây là một cohort tốt trên binary mới, không phải chứng minh đã sửa nguyên nhân hoặc ổn định trên mọi điều kiện. FAIL cũ STOP2/500/max11,4174ms và cadence50 Normal20 33/1281/max76,8416ms vẫn giữ trong hồ sơ. Không dùng p99 thay max hoặc thay ngưỡng.

## APK và chụp giao diện

APK debug mới: `apk/sumo-ui-compact-debug.apk`, versionName1.0/versionCode1, min36/target37, SHA256 **c8f88cbcb3dd5fb2abdcd1bb7eb0608b2d2c1b27b9bd3ffa7b7d9aafbfcd9f55**. SHA của bản cài AVD khớp file. APK G5 cũ SHA92abb6… được giữ nguyên; không ghi đè hoặc coi bằng chứng soak cũ là soak của bản UI mới. Chưa tạo bản phân phối ký ổn định, upload hoặc release.

Ảnh chụp với simulator loopback và fault `drop-reply`; có ACK cuối153/153 cũ trong khi app yêu cầu0/0. Đã xác nhận toàn bộ13PNG và các cue XML tương ứng; đã mở xem ảnh. Bộ thông số gốc/kill/lịch sử cài đặt được khôi phục đúng hash3file SharedPreferences. `18-capture-journey.txt` giữ lỗi bước kiểm tra cuối do công cụ đọc XML cũ sau khởi động lại; đã xác minh native screen và dùng tên XML mới ở `19-export-and-restore.txt`/`restored-neutral.xml`, không sửa app để né lỗi công cụ. Các preview trước chỉnh màu cuối ở `previews/`, không thuộc gallery bản cuối.

## Giới hạn còn lại

`final-environment.json` xác nhận không còn receiver UDP8888/8889; đã đóng AVD do phiên này mở sau khi xác nhận app Offline/PWM0/0 và khôi phục cấu hình. Nguồn, liên kết tài liệu và checksum ảnh/APK đã được kiểm tra; không chạy lại soak hoặc lặp batch timing.

**G5 vẫn CÓ ĐIỀU KIỆN cho chức năng AVD; G3/G4 CHỜ DUYỆT CHUNG.** Không duyệt lại thay người dùng hoặc nói đã đạt toàn dự án. Chưa kiểm tra máy thật, API36, font scale/kích thước màn hình khác, soak30phút trên binary UI mới, HTTPS A→B→C, I11, khóa phân phối/backup, firmware/xe thật. ACK không chứng minh động cơ quay/dừng. Giữ chặn dùng xe và release; chưa đẩy các thay đổi UI/ảnh mới lên GitHub.
