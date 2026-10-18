# Lệnh và bằng chứng khảo sát Phase 0

Thực chạy ngày 17/10/2026, Asia/Bangkok, tại `C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT`, PowerShell. Đây là nhật ký baseline, không phải kết quả các ca chức năng dự kiến trong kế hoạch. Trong [evidence](evidence/), transcript giữ lệnh/exit code nhưng bỏ sót một phần native stdout/stderr; `observed-console-excerpts.txt` là bản trích từ output tool đã quan sát, được ghi nhãn rõ, không phải log console đầy đủ. XML test/lint giữ nguyên báo cáo máy tạo. `artifact-native.txt` lưu trực tiếp lần xác minh APK sau đó qua `2>&1 | Tee-Object`, tránh giới hạn transcript.

## 1. Khảo sát nguồn và phiên bản

```powershell
Get-Location
git status --short
rg --files --hidden -g AGENTS.md -g '!**/.gradle/**' -g '!**/build/**'
rg --files --hidden -g '!**/.gradle/**' -g '!**/.kotlin/**' -g '!**/build/**' -g '!**/.idea/**' -g '!**/.git/**' -g '!docs/testing/**'
```

Git exit 128: không có repository. Tìm AGENTS exit 1: không có kết quả. Đã kiểm tra thêm `C:\AGENTS.md`, `C:\Users\AGENTS.md`, `C:\Users\CHI NGUYEN\AGENTS.md`, `C:\Users\CHI NGUYEN\AndroidStudioProjects\AGENTS.md`: không có. Dùng AGENTS người dùng cung cấp trong chat. Đọc toàn bộ cấu hình Gradle/manifest, Activity, theme, backup rules và hai test mẫu. Baseline 42 tệp trong `source-baseline.json`/`source-baseline.sha256`; mỗi hash tính bằng `Get-FileHash -Algorithm SHA256`, đường dẫn được sắp xếp và chuẩn hóa `/`.

```powershell
java -version
& 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' -version
Get-Content -Raw 'C:\Program Files\Android\Android Studio\product-info.json'
Get-Content -Raw 'C:\Program Files\Android\Android Studio\build.txt'
py -3.13 --version
```

Oracle Java trên PATH treo trong sandbox; chỉ tiến trình `java.exe -version` do khảo sát khởi chạy PID 33216/parent 11828 được kết thúc sau khi đối chiếu path/command bằng `Get-CimInstance Win32_Process`. Không xác nhận phiên bản Oracle. JBR exit 0, 25.0.2; Python exit 0, 3.13.0. Log: `environment.txt`. Đọc thêm SDK `source.properties`, AVD `.ini`/`config.ini`; `devices-host.txt` ghi thông tin OS/CPU/RAM qua CIM ngoài sandbox.

DOCX chỉ đọc bằng `.NET System.IO.Compression.ZipFile`, lấy `word/document.xml`, ghép các `w:t` trong từng `w:p`, lưu `spec-extracted.txt`; 74 đoạn, không có `word/media` nhúng. Không tạo/sửa DOCX.

## 2. Gradle baseline

Mỗi lệnh build dùng thiết lập process này, không đổi cấu hình toàn máy:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
```

| Lệnh thực chạy | Ngữ cảnh | Exit | Log |
|---|---|---|---|
| `./gradlew.bat --offline --version --console=plain` | Sandbox | 1, cache lock AccessDenied | `gradle-version.txt` |
| `./gradlew.bat --offline --version --console=plain` | Ngoài sandbox | 0, Gradle 9.5.0/JBR 25.0.2 | `gradle-baseline.txt` |
| `./gradlew.bat --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --console=plain` | Ngoài sandbox, chưa workaround temp | 1, loopback connection | `gradle-baseline.txt` |

Sau lỗi loopback, thêm **chỉ vào terminal gọi lệnh**:

```powershell
$env:JAVA_TOOL_OPTIONS = '"-Djdk.net.unixdomain.tmpdir=C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT\.gradle"'
```

| Lệnh thực chạy tiếp, ngoài sandbox với hai biến trên | Exit/kết quả | Log |
|---|---|---|
| `./gradlew.bat --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --console=plain --stacktrace` | 1, thiếu cache `lint-gradle:32.3.3`; chưa có kết quả test lần này | `gradle-baseline-short-temp.txt` |
| `./gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --rerun-tasks --console=plain` | 0; 42 tasks executed, 9 s; unit 1/1, 0 fail/error/skip, assemble debug thành công | `gradle-unit-assemble.txt`, `unit-result.xml` |
| `./gradlew.bat :app:lintDebug --console=plain` | 0, 30 s; 0 error/12 warning | `gradle-lint-online.txt`, `lint-result.xml` |
| `./gradlew.bat --offline :app:connectedDebugAndroidTest --console=plain` | 1, UTP chưa cache, chưa chạy test | `gradle-instrumentation-offline.txt` |
| `./gradlew.bat :app:connectedDebugAndroidTest --console=plain` | 0, 31 s; 1/1 trên Medium_Phone, 0 fail/error/skip | `gradle-instrumentation-online.txt`, `instrumentation-result.xml` |

Hai lần không `--offline` dùng repositories có sẵn trong project để lấy đúng dependency; không đổi plugin/dependency version. Trước instrumentation, ADB chỉ có một AVD, đã xác nhận tên và boot. `ANDROID_SERIAL` được đặt từ serial vừa quan sát, không giả định trước:

```powershell
$env:ANDROID_SERIAL = (Get-Content -Raw 'docs\testing\phase-0\evidence\selected-avd.txt').Trim()
```

Không chạy assembleRelease/lintRelease. Không dùng test mẫu để ghi đạt L/C/U/I/E/H/F/A. Không chạy lại cả suite chỉ vì chỉnh hồ sơ; nguồn ứng dụng không thay đổi.

## 3. AVD và smoke app mẫu

```powershell
$adbPath = 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$emulatorPath = 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\emulator\emulator.exe'
& $emulatorPath -version
& $emulatorPath -list-avds
& $emulatorPath -accel-check
& $adbPath version
& $adbPath devices -l
```

Emulator/version/list exit 0; Medium_Phone tồn tại. Sandbox accel exit 6 và ADB exit 1; ngoài sandbox WHPX/ADB exit 0, danh sách lúc đầu trống. Không dừng ADB/thiết bị khác.

Lệnh khởi chạy thực tế ngoài sandbox, redirect stdout/stderr tới evidence, `WindowStyle Hidden`:

```powershell
$phaseEvidence = Join-Path (Get-Location) 'docs\testing\phase-0\evidence'
Start-Process -FilePath $emulatorPath -ArgumentList @('-avd','Medium_Phone','-no-window','-no-audio','-no-snapshot-load','-no-snapshot-save','-no-boot-anim') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $phaseEvidence 'emulator-stdout.txt') -RedirectStandardError (Join-Path $phaseEvidence 'emulator-stderr.txt')
```

Launcher PID 1948, bắt đầu 08:08:41 +07:00. Không wipe, không sửa AVD config, không lưu snapshot. Đã xác nhận serial `emulator-5554` từ danh sách ADB rồi đọc:

```powershell
$phaseSerial = (Get-Content -Raw 'docs\testing\phase-0\evidence\selected-avd.txt').Trim()
& $adbPath -s $phaseSerial emu avd name
& $adbPath -s $phaseSerial shell getprop sys.boot_completed
foreach ($p in @('ro.build.version.release','ro.build.version.sdk','ro.build.version.codename','ro.build.version.incremental','ro.product.model','ro.product.cpu.abi','ro.kernel.qemu')) {
    & $adbPath -s $phaseSerial shell getprop $p
}
& $adbPath -s $phaseSerial shell wm size
& $adbPath -s $phaseSerial shell wm density
& $adbPath -s $phaseSerial shell getconf PAGE_SIZE
```

Medium_Phone/boot=1/Android17/API37/REL/x86_64/1080×2400/420dpi/16384byte; log `avd-boot.txt`. Không mở Device Manager UI; bằng chứng CLI + instrumentation là phương án thay thế được đề nghị ở G0.

Smoke thực chạy trên AVD sau instrumentation, không phải test updater trên điện thoại:

```powershell
& $adbPath -s $phaseSerial install -r 'app\build\outputs\apk\debug\app-debug.apk'
& $adbPath -s $phaseSerial shell am start -W -n 'com.aistudy.myapplication/.MainActivity'
& $adbPath -s $phaseSerial shell uiautomator dump '/data/local/tmp/phase-0-baseline.xml'
& $adbPath -s $phaseSerial pull '/data/local/tmp/phase-0-baseline.xml' 'docs\testing\phase-0\evidence\baseline-ui.xml'
& $adbPath -s $phaseSerial shell screencap -p '/data/local/tmp/phase-0-baseline.png'
& $adbPath -s $phaseSerial pull '/data/local/tmp/phase-0-baseline.png' 'docs\testing\phase-0\evidence\baseline-screen.png'
& $adbPath -s $phaseSerial shell dumpsys activity activities | Select-String 'ResumedActivity|topResumedActivity'
```

Install/launch/dump/pull/screencap exit 0. Đã mở ảnh bằng `view_image`: Hello Android, dọc, thanh hệ thống còn hiện. Log `avd-smoke.txt`; không có chức năng Sumo để thao tác đa ngón. Đóng AVD do khảo sát khởi chạy sau khi lấy đủ bằng chứng; lệnh và kết quả trong `avd-shutdown.txt`.

## 4. Artifact và tài liệu bên ngoài

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
& 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\build-tools\36.0.0\apksigner.bat' verify --verbose --print-certs 'app\build\outputs\apk\debug\app-debug.apk'
& 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\build-tools\36.0.0\aapt2.exe' dump badging 'app\build\outputs\apk\debug\app-debug.apk'
Get-FileHash -LiteralPath 'app\build\outputs\apk\debug\app-debug.apk' -Algorithm SHA256
Get-FileHash -LiteralPath 'C:\Users\CHI NGUYEN\Downloads\Dac_Ta_Ky_Thuat_App_Dieu_Khien_Xe_Sumo.docx' -Algorithm SHA256
```

Verify/dump exit 0; dữ liệu và fingerprint công khai trong `artifact-info.txt`. Chưa tạo/đọc khóa ký riêng, mật khẩu hoặc token; chưa upload. Không tạo bản release. Bản APK mẫu dùng cert Android Debug, chưa thuộc chuỗi phân phối được chốt.

Đã kiểm tra lại cùng APK, cùng tham số verify/dump, không build lại, và lưu native output trực tiếp bằng pipeline sau (exit đều 0, fingerprint/package/version không đổi):

```powershell
$phaseNativeLog = 'docs\testing\phase-0\evidence\artifact-native.txt'
& 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\build-tools\36.0.0\apksigner.bat' verify --verbose --print-certs 'app\build\outputs\apk\debug\app-debug.apk' 2>&1 | Tee-Object -FilePath $phaseNativeLog -Append | Out-Null
& 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\build-tools\36.0.0\aapt2.exe' dump badging 'app\build\outputs\apk\debug\app-debug.apk' 2>&1 | Tee-Object -FilePath $phaseNativeLog -Append | Out-Null
```

Đã thử tuyến đọc web của agent-reach:

```powershell
curl.exe --fail --location --max-time 25 --silent --show-error 'https://r.jina.ai/https://developer.android.com/privacy-and-security/local-network-permission'
```

Exit 1, không resolve được r.jina.ai trong sandbox; chuyển sang công cụ web tích hợp, chỉ mở nguồn Android Developers ngày 17/10/2026: [LAN permission](https://developer.android.com/privacy-and-security/local-network-permission), [app updates](https://developer.android.com/google/play/app-updates), [PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller). Các trang này làm căn cứ đề xuất nền tảng, không phải kết quả chạy quyền/cập nhật của app.

Kiểm tra cuối hồ sơ: đối chiếu hash baseline với nguồn hiện có, chỉ cho phép hai tài liệu mô tả/kế hoạch thay đổi; đọc XML test/lint và kiểm tra liên kết nội bộ. Kết quả trong `verification.txt`; đây là QA hồ sơ, không phải ca nghiệm thu sản phẩm.
