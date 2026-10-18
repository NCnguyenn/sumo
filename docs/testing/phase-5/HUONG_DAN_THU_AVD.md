# Mở và tái hiện bản thử Phase5 trên AVD

Workspace: `C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT`.

APK mới sau sửa: `apk/sumo-phase5-avd-debug-v1-g5-check.apk`, SHA256 `92abb62d00eb6f841eb8b9eb158d4b0be4b98bd39c6e05bd39b69de79613e5e3`. Identity `com.aistudy.myapplication`,versionCode1/versionName1.0,minSdk36/target37. Bản08 `sumo-phase5-avd-debug-v1.apk` và bằng chứng08-apk-* vẫn giữ lịch sử. Xem [kết quả bổ sung](KET_QUA_BO_SUNG_G5.md): người dùng đã chốtG5có điều kiện cho chức năng AVD,STOP/cadence còn lỗi mở chặn dùng xe/release; đây là debug dùng AVD, chưa phải chuỗi APK phân phối điện thoại.

Kiểm chứng18/10 trên cùng APK/SHA: `evidence/33-final-functional-soak/`25/25Android (23UI/Settings/updater+soak+probe), soak30phút0,668giây/600chu kỳ/20reconnect, audit60.557frame khôngrevival. Hướng dẫn mở thử bên dưới không đổi. Chẩn đoán scheduler/ART30–32 không thay FAILSTOP/cadence20/21; không cần tạo/cài APKkhác vì nguồn/binarygiữ nguyên.

## Mở thử

1. Android Studio → Device Manager → Medium_Phone (ảnh Android17/API37). Kiểm tra `adb devices -l`; chọn đúng serial AVD. Không mở đồng thời hai bản cùng AVD hoặc chọn điện thoại khác. Lượt hồ sơ này dùng `emulator-5554`, CLI headless/software,1080×2400/density420.
2. Từ PowerShell tại workspace, cài đè giữ dữ liệu rồi mở app:

```powershell
$adb = 'C:/Users/CHI NGUYEN/AppData/Local/Android/Sdk/platform-tools/adb.exe'
& $adb -s emulator-5554 install -r 'docs/testing/phase-5/apk/sumo-phase5-avd-debug-v1-g5-check.apk'
& $adb -s emulator-5554 shell am start -W -n 'com.aistudy.myapplication/.MainActivity'
```

3. Terminal riêng tại workspace chạy receiver loopback. Không dùng wildcard/public bind:

```powershell
py -3.13 -X utf8 tools/sumo_simulator.py --host 127.0.0.1 --port 8888 --scenario normal --log .gradle/manual-sumo-avd.jsonl
```

4. Trong Cấu hình app, đặt IP10.0.2.2/port8888, chu kỳ30ms, limiter60%, D-Pad hoặc joystick. Lưu, nhấn Kết nối và quyết định cấp quyền LAN trên màn hình Android. Online chỉ xuất hiện khi có ACK; AUTO vẫn khóa.
5. Thử Tiến/Boost, nhả Boost rồi nhả hướng; thử HOLD/TOGGLE, Brake, Kill và giữ mở khóa1giây. Sau STOP/cancel/reconnect, ngón cũ không được làm app chạy lại. Mở Cấu hình/Cập nhật phải dừng phiên. Vũ khí HOLD/TOGGLE giữ đúng quy tắc riêng khi chỉ nhả điều hướng.

Không dùng `pm clear`, uninstall hoặc xóa dữ liệu để làm bài test đạt. Dừng receiver bằng Ctrl+C khi xong. Nếu bản cũ có cấu hình khác, ghi lại rồi chỉnh trực tiếp trong app.

## Tái hiện kiểm thử tự động

Mã nguồn/app/test và receiver phải khớp manifest SHA của lượt nghiệm thu trong biên bản. Không suy ra version chỉ từ versionName1.0. Mỗi lượt dùng thư mục bằng chứng mới, không ghi đè cohort cũ.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
$env:JAVA_TOOL_OPTIONS = '"-Djdk.net.unixdomain.tmpdir=C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT\.gradle"'
./gradlew.bat --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
py -3.13 -X utf8 tools/sumo_simulator.py --self-test
```

`evidence/run_android.py` quản lý hai receiver8888/8889 và logcat của chính lượt chạy, cài đè `-r`, không uninstall. Chỉ chạy khi các cổng rảnh; không kết thúc tiến trình khác. Script yêu cầu fixture v2 đã được kiểm tra ở Phase3 (`.gradle/phase3-fixture-b.apk`) cho **prompt/Cancel** PackageInstaller, không cài v2 hoặc tải từ hosting. Tham số `candidate` trong lệnh được ghi nguyên trong `commands.jsonl`.

Trước ca installer dương tính, cấp “Install unknown apps → Allow from this source” cho app bằng Cài đặt Android trên AVD. Runner kiểm preflight sau cài debug và dừng sớm nếu chưa cấp; không tự vượt quyền. Fixture được adb push rồi copy bằng UID app, kiểm SHA khớp; không truyền APK qua stdin Windows (đã tái hiện bị cắt ở Ctrl-Z). Lượt08 giữ36/37 do thiếu quyền; ca bù14-complete1/1 sau cấp quyền và đủ byte, không ghi lại thành một lượt37/37 xanh. Prompt/Cancel đã xem ảnh thật, không phải cập nhật HTTPS.

Lệnh đầy đủ của bộ cuối nằm trong `evidence/08-final-android.txt`. Bộ này gồm toàn bộ lớp instrumentation, soak30phút/600chu kỳ/20reconnect, STOP500 và cadence20/30/50. Không chạy riêng StopLatency/Cadence thêm lần nữa để chọn cohort. `TrackingProfileTest` là chẩn đoán có tracing/allocation counting, tách khỏi các mốc BEGIN/END nghiệm thu.

```powershell
py -3.13 -X utf8 tools/check_android_timing.py --results docs/testing/phase-5/evidence/08-final-android --output-prefix docs/testing/phase-5/evidence/09-stop
py -3.13 -X utf8 tools/check_android_cadence.py --results docs/testing/phase-5/evidence/08-final-android --output-prefix docs/testing/phase-5/evidence/09-cadence
```

Định nghĩa STOP giữ G0: t0 callback app nhận release/cancel; t1 ngay trước send; t2 ngay sau send, cùng System.nanoTime.500 release, max(t1−t0) và max(t2−t0)≤10ms. Đây không phải mốc touch vật lý hoặc motor dừng.

Sau quyết định G5 AVD, cờ `--g5-avd-50-jitter` của checker cho phép riêng cấu hình50ms có interval20–60ms;20/30 vẫn20–50ms. Không bật cờ khi tái hiện G0 hoặc chấm lại hồ sơ08 thành PASS. Cohort21 ởthư mục21-g5-power-cohort có power policy QEMU được ghi riêng và đã khôi phục; không suy ra điều kiện mặc định sẽ đạt cùng số đo. Chưa có quyền nớiSTOP10ms; không đo lặp để chọn cohort đẹp.

Cadence dùng t1 các cặp gửi định kỳ liền nhau trong cùng SID khi giữ hướng; loại cặp bị sự kiện tức thời chen giữa theo định nghĩa đã duyệt. Mỗi cấu hình20/30/50 cần≥1000interval, mọi interval20–50ms; báo min/p50/p95/p99/max và số vi phạm. Không lọc outlier, thay50ms hoặc dùng p99 làm gate. Exit0 của instrumentation xác nhận thu mẫu/test, checker riêng quyết định ngưỡng.

Fault scenarios dùng `tools/run_android_faults.py --serial emulator-5554 --evidence-dir <thư mục mới trong phase-5/evidence>`. Lifecycle và I11 dùng `evidence/run_lifecycle.py`/`run_no_internet.py`, sau khi bộ instrumentation đã kết thúc. I11 chỉ đặt chain firewall riêng trong guest AVD và gỡ đúng chain đó; cần AVD cho adb root, không thay firewall máy phát triển. Không chạy song song các bài điều khiển UI.

## Phần ngoài AVD

API36 chưa có ảnh máy thử; chưa có điện thoại, khóa ký phân phối kèm backup hoặc kênh HTTPS. A→B→C qua màn hình cập nhật trên điện thoại vẫn cần các điều kiện này. Cài bằng adb hoặc prompt/Cancel không thay cho bằng chứng cập nhật HTTPS. Không upload APK debug này, không tạo khóa tạm để phân phối, không suy kết quả simulator thành firmware/motor/xe thật.
