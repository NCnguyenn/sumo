# Biên bản khảo sát và đề nghị duyệt Phase 0

Ngày khảo sát: **17/10/2026, Asia/Bangkok**. Dự án: `C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT`.

**Phase / Gate: 0 / G0 — ĐẠT, ĐÃ ĐƯỢC NGƯỜI DÙNG DUYỆT NGÀY 17/10/2026.** Đã thực hiện khảo sát và kiểm tra nền. Các quy tắc/hợp đồng mục 5–9 ban đầu là đề xuất; nay được duyệt theo quyết định dưới đây. Nội dung khảo sát giữ nguyên mốc bằng chứng, không biến baseline thành kết quả chức năng hoặc xe thật.

Lịch sử rà soát trước duyệt ngày 17/10/2026: bổ sung đề xuất về nhả nhiều pointer điều hướng tại mục 5.2.1–5.2.2 và ca reducer L17–L22. Lượt rà soát đó chỉ chỉnh tài liệu, chưa chạy các ca này; baseline mục 4 vẫn là kết quả khảo sát, không chứng minh quy tắc bổ sung đã đạt. Kết quả triển khai sau duyệt có hồ sơ riêng bên dưới.

**Quyết định tiếp theo:** Người dùng đã duyệt G1/G2 theo hồ sơ cuối60/60 và cho thực hiện liên tục Phase3+4. Hiện trình **G3/G4 CHỜ DUYỆT CHUNG**, chưa chuyển Phase5/release. Phần ủy quyền Phase1+2 bên dưới giữ lịch sử; hợp đồng đã duyệt không bị sửa. Xem hồ sơ Phase3/4 để biết kết quả và các tiêu chí chưa đạt.

## Quyết định G0 và ủy quyền đợt Phase 1 cộng Phase 2

Người dùng đã duyệt G0 theo D01–D11 và các quy tắc 5.2.1–5.2.2 trong chat ngày 17/10/2026. Chấp nhận bằng chứng AVD qua CLI; bản thử D-Pad 4 hướng, joystick và UDP simulator; AUTO mặc định khóa. Các nhãn “đề xuất/chờ duyệt/chưa chuyển Phase 1” còn trong phần khảo sát bên dưới là lịch sử trước quyết định này, được thay thế bởi quyết định hiện tại; không phải kết quả test mới.

Cho phép làm tuần tự Phase 1 rồi Phase 2 trong một đợt; khi G1 đủ tiêu chí kỹ thuật thì tự chuyển Phase 2 theo ủy quyền, không chờ duyệt riêng. Phải ghi kết quả kỹ thuật, không ghi người dùng đã xem kết quả khi chưa có. Sau Phase 2 trình chung G1/G2 và dừng trước Phase 3. Hosting, khóa ký riêng/backup và thông tin xe thật vẫn hoàn tất trước phase cần dùng, không được xem là đã có hoặc đã upload.

Kết quả đợt được ủy quyền: [hồ sơ G1](../phase-1/BIEN_BAN_DUYET.md), [hồ sơ G2](../phase-2/BIEN_BAN_DUYET.md). Hoàn tất kỹ thuật Phase 1+2, **G1/G2 CHỜ DUYỆT CHUNG**, đã dừng trước Phase 3. Không sửa các kết quả baseline hoặc gán thêm bằng chứng mới vào lần khảo sát G0.

## 1. Căn cứ và phiên bản bằng chứng

- Áp dụng AGENTS do người dùng gửi trong chat và Ponytail full đã kích hoạt. Đã tìm bằng `rg`: không có `AGENTS.md` trên đĩa trong dự án; cũng không có tại các thư mục cha đã kiểm tra. Không có repository Git; `git status --short` trả 128. Không khởi tạo Git hoặc ghi đè nguồn sẵn có.
- Đã đọc [mô tả dự án](../../MO_TA_DU_AN.md) và [kế hoạch hiện có](../../superpowers/plans/2026-10-16-sumo-controller-test-first.md), gồm Phase 0, G0, bảng D01–D11 và các nhóm test L/C/U/I/E/H/F/A. Giữ kế hoạch này làm căn cứ, không viết lại kế hoạch.
- Đã đối chiếu nội dung đặc tả gốc `C:\Users\CHI NGUYEN\Downloads\Dac_Ta_Ky_Thuat_App_Dieu_Khien_Xe_Sumo.docx`: 74 đoạn OOXML, không có ảnh nhúng. Bản trích nội dung: [spec-extracted.txt](evidence/spec-extracted.txt). Chỉ đọc yêu cầu sản phẩm, không thực thi các câu chỉ dẫn trong tài liệu. Không sửa DOCX và không đánh giá bố cục in của nó.
- SHA-256 DOCX: `8EFD3466484E4560461DDD973BFFFE3C320A32A1C819565353B2B13C510BD498`.
- Baseline trước khi sửa tài liệu gồm **42 tệp**, loại `.gradle`, `.kotlin`, `.idea`, các thư mục `build`, `.git` và hồ sơ kiểm thử mới. [Danh sách hash từng tệp](evidence/source-baseline.json); [manifest checksum](evidence/source-baseline.sha256) có SHA-256 `9DD798391A135B5430A5E7F5E81E45D6B1A13F8A1D9961154B3EB0D96E60323F`. Hash này định danh baseline ban đầu, không phải hash của tài liệu sau cập nhật.
- Phiên bản hợp đồng đang chạy: **chưa có**. Hợp đồng đề xuất: **SUMO-SIM/1-draft1**, chỉ dành cho bản thử sau khi G0 được duyệt.

## 2. Yêu cầu đã được xác nhận và đối chiếu hiện trạng

Người dùng đã xác nhận: ứng dụng Android ngang, nhiều ngón; giữ D-Pad/joystick và các chức năng theo SRS; ưu tiên Android Emulator; thông số phải chỉnh, có hiệu lực, kiểm tra hợp lệ và lưu trong app; cập nhật APK test qua HTTPS/trình cài Android không USB sau lần đầu, giữ dữ liệu và không khôi phục chuyển động. Được phân phối APK test trước G6; bản bàn giao chính thức chỉ sau G6, kiểm tra tại G7. Chưa có phê duyệt G0.

Android 16 trên điện thoại mới là thông tin có thể đúng, **chưa xác nhận**. UDP cho xe thật, mặc định hiệu chỉnh, AUTO, các quy tắc bổ sung an toàn và giao thức mở rộng chưa phải quyết định của người dùng.

| Yêu cầu | Nguồn yêu cầu | Hiện trạng kiểm chứng ngày 17/10 | Nơi cần kiểm chứng tiếp theo |
|---|---|---|---|
| R01 Android, ngang, fullscreen | SRS + người dùng chọn Android | Có app Android; ảnh baseline còn dọc và thấy thanh hệ thống | U01/U10/E03, điện thoại thật |
| R02 D-Pad | SRS | Chưa có; 4/8 hướng còn chờ chọn | L01–L03/U02/H01 |
| R03 Joystick/PWM ±255 | SRS | Chưa có | L09–L10/U02/H01 |
| R04 STOP ≤10 ms khi nhả | SRS; nhả nhiều pointer là làm rõ đề xuất | Chưa có; chưa đo | L06/L17–L18/L20/L22, I02–I03/E07 và đo trên điện thoại |
| R05 Boost 100% | SRS; giữ hướng/đứng yên là làm rõ đề xuất | Chưa có | L04–L07/L19–L20/U03/H01 |
| R06 Vũ khí hold và toggle | SRS | Chưa có cả hai chế độ | L11/L21/U04/H02 |
| R07 Xoay nhanh 180° | SRS | Chưa có; 200 ms chỉ là ví dụ | L16/C02/C08/H06 |
| R08 Brake/Lock | SRS | Chưa có; khả năng hãm của driver chưa biết | L12/U04/H03 |
| R09 Limiter 30–100% | SRS | Chưa có | L03–L04/U12/F01/H01 |
| R10 MANUAL/AUTO khi có vòng tự hành | SRS có điều kiện | Chưa có; chưa xác nhận thể lệ/firmware, không ghi không áp dụng | L15/C02/H07 hoặc quyết định phạm vi |
| R11 Kill | SRS; latch/unlock là đề xuất | Chưa có | L13–L14/U06–U07/I03/H04–H05 |
| R12 Online/Offline/ping/reconnect | SRS | Chưa có | C05/I04–I07/I12/H08 |
| R13 Bản tin ngắn, newline | SRS | Chưa có codec/transport | C01–C05/C10; hợp đồng firmware tại G6 |
| R14 Chu kỳ 20–50 ms, gợi ý 30 ms | SRS | Chưa có sender | I01–I03/F04, đo thật |
| R15 MCU dừng sau 300 ms mất gói | SRS; chỉ gói hợp lệ là đề xuất | Chưa có firmware/simulator; chưa chạy watchdog | C06–C07/I08; H04–H05 bắt buộc |
| R16 Ngón độc lập | SRS + người dùng; ngoại lệ STOP điều hướng tại 5.2.1 chờ duyệt | Chưa có điều khiển pointer | L07–L08/L17–L22/U03–U04/E02/H09 |
| R17 Haptic | SRS | Chưa có | U11; cảm giác rung H09 |
| R18 IP/port/chuỗi lệnh/thời gian xoay trong app | SRS | Chưa có Settings | C10/U08–U09/I09/F02/F05 |
| R19 Phản hồi hệ thống <30 ms | SRS | Chưa có phép đo; thời gian mở Activity không phải số đo này | Mục 8, H08 |
| R20 Cancel/nền/reconnect/gói cũ | Đề xuất an toàn trong kế hoạch | Chưa triển khai, chờ duyệt quy tắc | L13–L16/C06–C09/U05/I03/I05–I12 |
| R21 Test và gate trước bàn giao | Người dùng | Đã chạy baseline; chưa có gate chức năng nào được duyệt | G0–G7 |
| R22 Chỉnh/lưu toàn bộ danh mục vận hành | Người dùng bổ sung | Chưa có model/store/validation/migration | F01–F10 và UI/tích hợp |
| R23 Update test trong app, không USB | Người dùng bổ sung | Chưa có updater, hosting hoặc signing được chốt | A01–A14, A→B→C trên điện thoại |

R01–R23 ở trên đã được **đối chiếu yêu cầu**, không phải 23 ca đã chạy đạt. Các ca L/C/U/I/E/H/F/A trong kế hoạch vẫn là công việc dự kiến; **chưa ca chức năng nào được chạy** trong Phase 0.

## 3. Môi trường và mã nguồn kiểm tra lại

| Hạng mục | Bằng chứng mới |
|---|---|
| Cấu trúc | Một module `app`, Kotlin/Compose/Material 3; `MainActivity.Greeting` chỉ hiển thị `Hello Android!`; không có thư mục control/protocol/connection/settings/updates hoặc simulator |
| Gradle/AGP | Wrapper **9.5.0**, checksum distribution có sẵn trong wrapper; plugin Android **9.3.3**. Không đổi phiên bản |
| Kotlin | Compose plugin **2.2.10** trong version catalog; Gradle `--version` báo Kotlin nhúng **2.3.20**. Hai số này thuộc hai thành phần khác nhau |
| JDK | Chạy build bằng JBR **25.0.2**, `C:\Program Files\Android\Android Studio\jbr`; daemon yêu cầu Java 25; source/target Java 11 trong module |
| Java trên PATH | Oracle `javapath\java.exe -version` bị treo trong khảo sát sandbox, không lấy được phiên bản. Đã kết thúc đúng tiến trình khảo sát PID 33216, không dừng Android Studio/Java khác |
| Android Studio | `product-info.json`/`build.txt`: build **AI-261.26222.65.2613.16025427**. Không suy ra lại tên phiên bản hiển thị chỉ từ tài liệu cũ |
| SDK | `compileSdk=37`, `targetSdk=37`, `minSdk=36`; platform đã cài `android-37.0` revision 2; Build Tools **36.0.0**. Không có platform/system image API 36 trong danh sách đã khảo sát |
| AVD | `Medium_Phone`, image `android-37.1/google_apis_playstore_ps16k/x86_64`, revision 9; Emulator **37.1.11.0**; 4 vCPU, RAM cấu hình 2048 MB, 1080×2400, 420 dpi |
| AVD thực chạy | ADB chọn **emulator-5554** sau khi liệt kê, xác nhận tên AVD; `sys.boot_completed=1`; Android **17**, SDK **37**, codename REL, build incremental 16064790; page size **16384** byte |
| Host | Windows 11 Pro 10.0.26300, AMD Ryzen 9 7945HX, 16 cores/32 logical processors; RAM thấy bởi OS 16,450,076 KiB; WHPX usable |
| Thiết bị thật | ADB ngoài sandbox ban đầu không thấy thiết bị; sau khởi động chỉ thấy AVD. Không suy ra rằng người dùng không sở hữu điện thoại/xe |
| Manifest | Chưa khóa ngang/fullscreen; chưa khai báo INTERNET, ACCESS_LOCAL_NETWORK hoặc REQUEST_INSTALL_PACKAGES; chưa có luồng xin quyền |
| Identity hiện tại | `com.aistudy.myapplication`, versionCode **1**, versionName **1.0**, tên `My Application`; signing mặc định Android Debug, chưa phải khóa chuỗi phân phối |

Không mở giao diện Device Manager bằng công cụ tự động. Đã dùng CLI của chính Android SDK để liệt kê AVD, cold boot không wipe/không lưu snapshot, xác nhận ADB và chạy instrumentation thật. Đây là cách kiểm tra thay thế cho thao tác UI ở 0.7; không tuyên bố đã quan sát cửa sổ Device Manager. AVD được khởi động không cửa sổ; ảnh chụp lấy từ màn hình Android đang chạy.

## 4. Kiểm tra đã thực thi và giới hạn bằng chứng

[Lệnh tái hiện và nhật ký](COMMANDS.md) ghi đúng tham số, môi trường terminal, exit code và đường dẫn log. Transcript giữ lệnh/exit cả lần lỗi nhưng PowerShell host này bỏ sót một phần stdout/stderr của chương trình ngoài; [bản trích console đã quan sát](evidence/observed-console-excerpts.txt) ghi rõ nguồn và giới hạn, không giả làm log đầy đủ. XML test/lint là báo cáo máy tạo được lưu nguyên bản; [artifact-native.txt](evidence/artifact-native.txt) là lần xác minh APK lưu trực tiếp stdout/stderr bằng Tee-Object sau khi phát hiện giới hạn transcript. Các lệnh Gradle dùng JBR nêu trên. Khi cần, chạy ngoài sandbox để dùng cache/ADB hiện có; không thay đổi thiết lập Java toàn máy.

| Kích thích/lệnh thực chạy | Mong đợi | Đầu ra thực tế và kết luận |
|---|---|---|
| `gradlew --offline --version` trong sandbox | Đọc wrapper | Exit 1: AccessDenied với lock trong cache `~/.gradle`. Ngoài sandbox exit 0, Gradle 9.5.0/JBR 25.0.2 |
| Baseline unit+lint+assemble, ngoài sandbox, chưa workaround | Khởi động Gradle | Exit 1: `Unable to establish loopback connection`; không có kết quả test của lần này |
| Cùng nhóm, thêm `JAVA_TOOL_OPTIONS` chỉ terminal | Chạy từ cache | Exit 1 do chưa cache `lint-gradle:32.3.3`; lỗi configuration-cache là lớp bọc của lỗi resolve dependency, không phải bằng chứng code hỏng |
| `--offline :app:testDebugUnitTest :app:assembleDebug --rerun-tasks --console=plain` | Thực thi lại unit và build debug | **Exit 0**, 42 tasks executed; unit **1 test, 0 fail/error/skip** (`addition_isCorrect`); build thành công. Có cảnh báo không strip `libandroidx.graphics.path.so` |
| `:app:lintDebug --console=plain` có mạng | Tải dependency lint đang thiếu và lint nguồn hiện tại | **Exit 0**; XML có **0 lỗi, 12 Warning**: RedundantLabel ×1, cảnh báo phiên bản ×3, ObsoleteSdkInt ×1, UnusedResources ×7. Không nâng dependency hoặc sửa ngoài phạm vi |
| `adb devices -l`, `emulator -accel-check` trong/ngoài sandbox | Kiểm tra ADB và WHPX | Trong sandbox: ADB exit 1, accel exit 6; ngoài sandbox: ADB exit 0 và WHPX exit 0. Sự khác biệt quyền môi trường được giữ trong log |
| Khởi động Medium_Phone, đọc boot/OS | AVD dùng được | `boot_completed=1`, ADB device đúng tên, Android 17/API 37. Đáp ứng phần môi trường G0 |
| `--offline :app:connectedDebugAndroidTest` | Chạy test trên AVD đã chọn | Exit 1 vì thiếu cache UTP `android-test-plugin-host-additional-test-output:32.3.3`; chưa thực thi test |
| `:app:connectedDebugAndroidTest --console=plain` có mạng | Tải đúng UTP/dependency rồi chạy | **Exit 0**, **1 test, 0 fail/error/skip**, `ExampleInstrumentedTest.useAppContext`, thiết bị `Medium_Phone(AVD) - 17` |
| Cài APK debug vào AVD, `am start -W`, UI dump/screenshot | Mở được app mẫu | Install Success, Activity Status ok; dump có `Hello Android!`, rotation 0; ảnh cho thấy màn hình dọc/thanh hệ thống. Không đạt layout Sumo R01 |
| `apksigner verify --verbose --print-certs`, `aapt2 dump badging` | Định danh APK baseline | Exit 0 cả hai; chữ ký v2 Android Debug; package/version/min/target đúng cấu hình. Không phải kiểm chứng signing phân phối |

Các lỗi môi trường/dependency trên đã có cách chạy thành công tiếp theo; không tính lần lỗi thành PASS. Unit mẫu chỉ chứng minh runner/JVM; instrumentation mẫu chỉ assert package. Chưa có kiểm tra đa ngón, STOP, mạng xe, cấu hình, cập nhật, watchdog hoặc hiệu năng Sumo. `am start` ghi TotalTime 3499 ms là thời gian mở Activity, **không phải** độ trễ điều khiển.

APK baseline: `app/build/outputs/apk/debug/app-debug.apk`, **30,352,902 byte**, SHA-256 **18B1C13AD56A8BD99D10564F124737D98336F91FA95318AF83EBD94474D24453**. Certificate SHA-256 **d97c9cee077a242672a3d3b19276d6c9d76983ce862c59b0268b9a154c2915ba**, DN Android Debug. Không upload và không đề nghị dùng APK mẫu này làm bản điều khiển xe.

Bằng chứng được giữ trong `evidence/`: `environment.txt`, `devices-host.txt`, `avd-launch.txt`, `avd-boot.txt`, `avd-smoke.txt`, các `gradle-*.txt`, `artifact-info.txt`/`artifact-native.txt`, bản trích console, [unit XML](evidence/unit-result.xml), [instrumentation XML](evidence/instrumentation-result.xml), [lint XML](evidence/lint-result.xml), [UI dump](evidence/baseline-ui.xml), [ảnh baseline](evidence/baseline-screen.png), log emulator và checksum. Báo cáo build gốc còn trong `app/build/reports/`; bản XML đã sao chép tránh bị lần build sau ghi đè. Đã đóng AVD headless do khảo sát khởi chạy sau khi lấy bằng chứng (`avd-shutdown.txt`, exit 0), không wipe dữ liệu. Không có video hoặc số đo điều khiển trong Phase 0.

## 5. Phạm vi và quy tắc đề xuất cho G0

### 5.1 Phạm vi bản thử

Giữ một module và cấu trúc hiện có, lõi Kotlin thuần, Compose, một bộ điều phối gửi, API nền tảng cho socket/lưu cấu hình/cài APK, Python 3.13 thư viện chuẩn cho simulator. Không cần thư viện joystick, DI framework, server điều khiển, tài khoản hoặc CI/CD.

Đề xuất duyệt **D-Pad 4 hướng và joystick**; cả vũ khí hold/toggle; Boost, Brake, Kill, quick turn, limiter, trạng thái, Settings và updater theo kế hoạch. D-Pad 8 hướng/nút cua riêng đề nghị không áp dụng **cho bản thử** vì joystick đã có hướng chéo; đây vẫn là quyết định chờ người dùng, chưa tự loại khỏi phạm vi toàn dự án.

Chọn **UDP tới simulator** cho Phase 2–5: receiver bind `127.0.0.1:8888` trên host; emulator dùng `10.0.2.2:8888`. Endpoint xe thật để chưa cấu hình đến khi có thông tin. Chưa chọn Bluetooth/WebSocket hay UDP cho xe thật.

AUTO giữ trong UI và model nhưng mặc định khóa, kèm lý do. Chỉ cho bật khi: phiên Online đã nhận capability AUTO, cấu hình và quy tắc tự hành/thể lệ đã được duyệt, đang trung tính, không pointer điều khiển/Boost/vũ khí/Brake, không Kill/macro; gửi yêu cầu, chỉ hiện AUTO có hiệu lực sau ACK hợp lệ. Simulator mặc định không quảng bá AUTO; fixture capability có thể kiểm tra chuyển trạng thái với PWM 0, không giả lập thuật toán tự hành. R10 vẫn có điều kiện/chưa kiểm chứng thật, chưa ghi không áp dụng. Trong AUTO, manual/Boost/quick turn bị khóa; Brake/Kill/mất liên lạc buộc dừng và về MANUAL, không tự chạy lại.

### 5.2 Thứ tự ưu tiên và thao tác

1. **Kill hoặc lỗi/mất quyền phiên**: PWM 0, Boost 0, hủy macro và pointer; vũ khí về 0 trong simulator. Kill latch đến thao tác mở khóa rõ ràng; lỗi phiên đóng phiên, không tự reconnect để tiếp tục chạy. W=0/K=0 ở fail-safe chỉ là trạng thái mô phỏng; cách hạ cơ cấu/driver thật cần duyệt ở G6.
2. **Brake đang giữ**: PWM 0, B=0, K=1 gửi ngay; hủy macro, W=0 theo đề xuất simulator. Nhả Brake gửi K=0 và vẫn PWM 0; phải nhấc các ngón cũ rồi thao tác mới để di chuyển. K=1 biểu thị yêu cầu active brake, không đồng nghĩa đã chứng minh phanh thật.
3. **Macro hợp lệ**: một lần nhấn quick turn, chỉ khởi động khi Online/MANUAL/trung tính và không có thao tác khác. Receiver xoay trái `(-255,+255)` trong thời gian đã cấu hình; nhả nút quick turn không hủy macro một lần. Hết thời gian PWM 0. Kill/Brake/cancel/nền/mất phiên hoặc chạm điều hướng mới hủy macro và về 0; chạm hủy đó không đồng thời khởi động lái. Không khôi phục hướng/Boost trước macro.
4. **Điều khiển thường hợp lệ**: trộn hướng, limiter và calibration; vũ khí có pointer riêng. Mỗi pointer có chủ sở hữu cố định từ down tới up/cancel, không biến pointer Boost thành hướng khi kéo sang vùng khác.

Hai hướng D-Pad đối nhau triệt tiêu trên trục tương ứng khi các pointer còn hợp lệ. Chọn D-Pad hoặc joystick, không trộn hai nguồn; đổi lựa chọn dừng/xóa thao tác cũ. Với cả D-Pad và joystick, nhả hoặc ra khỏi vùng chạm hợp lệ/mất capture của một pointer điều hướng áp dụng quy tắc 5.2.1; trượt trở vào không tự nhận lại thao tác. Joystick chỉ clamp tọa độ tới biên khi pointer còn hợp lệ; D-Pad không tự kích hoạt nút lân cận. Cancel toàn gesture xóa toàn bộ pointer. Kill luôn tiếp cận được, có chữ/semantics và vùng chạm tối thiểu 48 dp; không dùng riêng màu để báo trạng thái.

**Nhả bất kỳ pointer điều hướng hợp lệ nào** yêu cầu snapshot PWM 0 ngay, B hiệu lực 0 và vô hiệu các pointer điều hướng cũ theo 5.2.1; không đợi tick 20–50 ms. Nhả riêng Boost chỉ bỏ Boost, hướng của ngón khác còn nguyên. Nhả hướng thông thường không tự hạ vũ khí đang do ngón khác giữ hoặc đang toggle: W vẫn theo trạng thái vũ khí. Nhả/cancel pointer vũ khí hold đưa W về 0; toggle chỉ đảo một lần khi down hợp lệ, up không đảo. Kill/Brake/lỗi phiên/cancel toàn cục/ra nền/đổi mode/mở Settings/update xóa cả latch toggle theo đề xuất W=0.

Mở khóa Kill yêu cầu Online, tất cả ngón đã nhấc, macro dừng, giữ nút Mở khóa **1000 ms** rồi nhận ACK unlock. Không xóa latch khi chỉ thả Kill, Reconnect hoặc mở lại màn hình. Nếu chưa có mạng, UI ghi chưa xác nhận mở khóa. Sau unlock/reconnect/khôi phục Activity luôn trung tính, chỉ nhận gesture mới; không lưu chuyển động/pointer/macro vào saved state hay cấu hình.

Khi ra nền, mất focus/tương tác, hủy gesture toàn cục, I/O lỗi hoặc quyền LAN bị thu hồi: ưu tiên trạng thái dừng, cố gửi snapshot cuối khi socket còn dùng được, đóng phiên và hủy công việc đang gửi. Không hứa app bị kill/mất mạng vẫn gửi được STOP; receiver phải tự watchdog độc lập. Trước Settings nâng cao/update cũng dừng phiên như vậy; limiter trên màn hình lái là trường duy nhất áp dụng live.

#### 5.2.1 Nhả nhiều pointer điều hướng

**ĐỀ XUẤT CHỜ DUYỆT — thuộc D04, chưa triển khai.** Nhả hoặc mất vùng chạm của bất kỳ pointer điều hướng hợp lệ nào lập tức yêu cầu `PWM=(0,0)`, `B hiệu lực=0`. Mọi pointer điều hướng cũ còn giữ bị vô hiệu để phát sinh chuyển động, nhưng vẫn theo dõi việc chúng đã nhấc hay chưa. Cả pointer vừa trượt ra ngoài vùng chạm vẫn phải nhấc thực sự trước khi nhận lại điều hướng; kéo trở vào không thay thế thao tác chạm mới.

Pointer hợp lệ là pointer đã được nhận từ down và chưa bị vô hiệu, kể cả khi Tiến + Lùi đang triệt tiêu làm PWM 0. Nhóm pointer điều hướng cũ được xác định tại thời điểm nhả/mất vùng; không gồm pointer Boost/vũ khí. Pointer chạm sớm trong khi chờ không được bổ sung thành hướng hợp lệ và không tự có hiệu lực khi nhóm cũ đã nhấc hết.

Trong khi chưa nhấc hết các pointer điều hướng cũ, mọi tick, move của chúng và chạm điều hướng đến sớm đều không được tạo lại PWM khác 0 hoặc B hiệu lực 1. Sau khi nhấc hết, đầu ra vẫn 0 ở các tick tiếp theo; **chỉ down điều hướng mới sau mốc đó** mới được nhận. Một chạm đến sớm rồi vẫn giữ không tự trở thành chạm mới khi pointer cũ cuối cùng nhấc; move/tick của chạm đến sớm đó cũng bị bỏ qua tới khi có down hợp lệ mới.

Việc chờ nhấc tay này chỉ xét pointer **điều hướng**, không yêu cầu nhấc Boost/vũ khí. Pointer Boost đang giữ vẫn được theo dõi nhưng B hiệu lực luôn 0 khi chưa có điều hướng mới hợp lệ; nếu sau đó down điều hướng hợp lệ trong khi Boost vẫn giữ, Boost có thể hiệu lực trở lại theo hướng mới. Nhả riêng Boost không kích hoạt quy tắc vô hiệu điều hướng. Nhả điều hướng không xóa pointer vũ khí HOLD hoặc latch TOGGLE; W tiếp tục theo quy tắc vũ khí đã đề xuất. Kill, Brake, reconnect và cancel toàn cục giữ nguyên quy tắc ở 5.2 và có ưu tiên tương ứng.

Reducer phải công bố trạng thái hiệu lực đã dừng; tick của bộ điều phối đọc trạng thái đó, không tính lại hướng từ danh sách pointer cũ hoặc bật lại B từ riêng pointer Boost. Đây là làm rõ đề xuất an toàn cho R04/R16/R20; không diễn giải nguyên tắc đa ngón thành việc giữ hướng cũ sau một lần nhả điều hướng.

#### 5.2.2 Chuỗi sự kiện và đầu ra dự kiến

**Tất cả các hàng sau là ĐỀ XUẤT CHỜ DUYỆT / CHƯA CHẠY**, sẽ đưa vào `ControlReducerTest` sau khi G0 được duyệt. Mỗi chuỗi độc lập: Online/MANUAL, limiter 60%, calibration 1/0/1/1/100, không Kill/Brake/macro, W=0 nếu không ghi khác. PWM ghi theo `(trái,phải)`, B là Boost **hiệu lực**, không phải riêng trạng thái ngón đang giữ. “Tick tiếp theo” nghĩa mọi tick khi chưa có down điều hướng mới hợp lệ; không chỉ một tick. Quy tắc thời gian yêu cầu STOP ≤10 ms giữ theo mục 8, chưa có số đo cho các chuỗi này.

| Ca / bước | Chuỗi sự kiện hoặc trạng thái pointer | Đầu ra ngay sau sự kiện | Đầu ra ở các tick tiếp theo |
|---|---|---|---|
| L17.1 — Tiến + Lùi | p0 down Tiến | PWM=(153,153), B=0, W=0 | Giữ (153,153), B=0, W=0 |
| L17.2 | p1 down Lùi, p0 vẫn giữ | PWM=(0,0), B=0, W=0 do hai hướng đối nhau | Giữ (0,0), B=0, W=0 |
| L17.3 | p1 up Lùi, p0 Tiến còn giữ | Yêu cầu PWM=(0,0), B=0, W=0; p0 bị vô hiệu | Vẫn (0,0), B=0, W=0 kể cả p0 move; không trở về (153,153) |
| L17.4 | p0 up, đã nhấc hết điều hướng cũ | PWM=(0,0), B=0, W=0; chỉ hết điều kiện chờ nhấc tay | Vẫn (0,0), B=0, W=0; không tự chạy |
| L17.5 | p2 down Tiến mới sau khi nhấc hết | PWM=(153,153), B=0, W=0 | Giữ (153,153), B=0, W=0 |
| L18.1 — Tiến + Trái | p0 down Tiến, rồi p1 down Trái | Lần lượt PWM=(153,153) rồi (0,153), B=0, W=0 theo mixer | Khi cả hai còn hợp lệ: (0,153), B=0, W=0 |
| L18.2 | p1 up Trái, p0 Tiến còn giữ | Yêu cầu PWM=(0,0), B=0, W=0; p0 bị vô hiệu | Vẫn (0,0), B=0, W=0 kể cả p0 move; không trở về (153,153) |
| L18.3 | p0 up, sau đó p2 down Tiến mới | Sau up: (0,0), B=0, W=0; chỉ sau down mới: (153,153), B=0, W=0 | Trước down mới vẫn 0; sau down mới giữ (153,153), B=0, W=0 |
| L19.1 — Tiến + Boost | p0 down Tiến, p1 down Boost | Khi cả hai giữ: PWM=(255,255), B=1, W=0 | Giữ (255,255), B=1, W=0 |
| L19.2 | p1 up Boost, p0 Tiến còn giữ | PWM=(153,153), B=0, W=0; p0 vẫn hợp lệ | Giữ (153,153), B=0, W=0; không yêu cầu p0 nhấc/chạm lại |
| L20.1 — Tiến + Boost | p0 down Tiến, p1 down Boost | Khi cả hai giữ: PWM=(255,255), B=1, W=0 | Giữ (255,255), B=1, W=0 |
| L20.2 | p0 up Tiến, p1 Boost còn giữ | Yêu cầu PWM=(0,0), B=0, W=0 dù pointer Boost còn giữ | Vẫn (0,0), B=0, W=0; tick hoặc move Boost không tạo hướng |
| L20.3 | p2 down Tiến mới; điều hướng cũ đã nhấc, p1 Boost vẫn giữ | PWM=(255,255), B=1, W=0 theo hướng mới | Giữ (255,255), B=1, W=0; không cần nhấc Boost để nhận hướng mới |
| L21.H1 — Vũ khí HOLD | p2 giữ vũ khí, p0 giữ Tiến | PWM=(153,153), B=0, W=1; pointer vũ khí còn nguyên | Giữ (153,153), B=0, W=1 |
| L21.H2 | p0 up điều hướng, p2 vũ khí còn giữ | Yêu cầu PWM=(0,0), B=0, W=1; không xóa p2 | Vẫn (0,0), B=0, W=1 khi HOLD còn giữ |
| L21.H3 | p2 up vũ khí; chưa có hướng mới | PWM=(0,0), B=0, W=0 theo HOLD | Vẫn (0,0), B=0, W=0 |
| L21.T1 — Vũ khí TOGGLE | Bật toggle bằng down/up vũ khí trước đó, rồi p0 giữ Tiến | PWM=(153,153), B=0, W=1; latch toggle bật | Giữ (153,153), B=0, W=1 |
| L21.T2 | p0 up điều hướng | Yêu cầu PWM=(0,0), B=0, W=1; latch toggle còn nguyên | Vẫn (0,0), B=0, W=1; không cần pointer vũ khí còn giữ |
| L21.T3 | Nhấn vũ khí lần kế tiếp để tắt toggle; chưa có hướng mới | PWM=(0,0), B=0, W=0; up nút vũ khí không đảo lần nữa | Vẫn (0,0), B=0, W=0 |

Ca L22 trong kế hoạch bổ sung mất vùng chạm, kéo trở vào và down đến trước khi nhấc hết: sau sự kiện mất vùng phải giữ PWM/B hiệu lực 0 ở cả tick và move, đến khi đủ điều kiện down mới tại 5.2.1. Không dùng các bảng dự kiến này làm bằng chứng test đã chạy.

### 5.3 Mixer và Boost

Đề xuất tọa độ joystick: x dương sang phải, y dương tiến; đổi dấu y màn hình tại UI. D-Pad tiến/lùi `(0,±1)`, trái/phải `(∓1,0)`. Giá trị runtime NaN/vô hạn cho cả cặp PWM 0; số hữu hạn ngoài [-1,1] clamp trước khi tính. Cấu hình NaN/vô hạn bị từ chối lưu.

Joystick dùng dead zone xuyên tâm: r=hypot(x,y); r≤deadZone thì 0, ngoài vùng chết dùng biên độ `min(1, sensitivity*(min(r,1)-deadZone)/(1-deadZone))` theo vector đơn vị. D-Pad dùng vector trục đầy đủ, không chịu sensitivity/deadZone. Nhân trục quay với turnPercent/100; trộn `left=y+x`, `right=y-x`, chia cả hai cho `max(1,abs(left),abs(right))`, rồi nhân gain từng bánh. PWM thường = kết quả ×255×limiter/100. Làm tròn biên độ tuyệt đối gần nhất, .5 lên, rồi gắn lại dấu và clamp ±255; limiter 30/60/100% ở calibration mặc định cho tiến ±77/153/255.

Boost chỉ có hiệu lực ngoài trung tính: bỏ limiter, chia cặp sau gain cho max biên độ của cặp rồi ×255; giữ dấu và tỷ lệ hai bánh, ít nhất một bánh đạt biên độ 255. Ví dụ joystick chéo có thể là `(255,0)`, không ép bánh đang 0 chạy. Đây là **làm rõ cần duyệt** cho câu “ép PWM lên 100%” của SRS, không mặc định firmware đã làm giống app. Receiver bản thử coi L/R là PWM đã tính, B chỉ ghi trạng thái, **không nhân/boost lần hai**. Quick turn riêng luôn ±255; thời gian 200 ms không được coi là góc 180° đã hiệu chỉnh.

## 6. Danh mục thông số đề xuất

Các miền sau là **miền phần mềm cho bản thử**, không chứng minh giới hạn cơ khí. Giá trị mặc định calibration giữ 1/0/1/1/100 đúng kế hoạch để không đổi kỳ vọng các test cơ bản.

| Trường | Mặc định đề xuất | Miền hợp lệ | Hiệu lực và cách chứng minh |
|---|---|---|---|
| Limiter | 60% | Số nguyên 30–100 | Live trên mixer; F01 giữ cùng APK/version và so PWM 77/153/255 |
| sensitivity | 1.0 | Số hữu hạn 0.25–2.0 | Joystick sau vùng chết, F03 so đầu ra với cùng tọa độ |
| deadZone | 0.0 | Số hữu hạn 0–0.30 | Bán kính normalized; F03 kiểm tra tâm/biên và rescale |
| leftGain/rightGain | 1.0/1.0 | Mỗi số hữu hạn 0.50–1.00 | Bù tỷ lệ bánh; không cho vượt ±255, F03; cần hiệu chỉnh xe thật |
| turnPercent | 100% | Số nguyên 30–100 | Trọng số trục quay cho điều khiển thường; F03. Không giảm tốc quick turn SRS |
| Quick turn duration | 200 ms | Số nguyên 1–2000 ms | App lưu, receiver thực thi timer từ snapshot đã ACK; F02: 200→350 ms |
| Weapon interaction | HOLD | HOLD hoặc TOGGLE | Áp dụng ở reducer, gửi W đầy đủ; cả hai chế độ bắt buộc kiểm tra |
| Input selection | D_PAD | D_PAD hoặc JOYSTICK | Đổi khi đã dừng/xóa input; U02/U05 |
| Endpoint bản thử | 10.0.2.2, 8888 | IPv4 literal 4 octet 0–255, không wildcard/multicast/broadcast; port nguyên 1–65535 | Chỉ nhãn simulator; F07/I09 đóng endpoint cũ rồi mở mới. Bản xe chưa có mặc định được xác nhận |
| Send interval | 30 ms | Số nguyên 20–50 ms | Đổi một scheduler duy nhất sau dừng; F04 đo 20/30/50; STOP/Kill không chờ |
| Drive command template | `S:{L},{R},{B},{W},{K}\n` | Chỉ template này hoặc alias `D:{L},{R},{B},{W},{K}\n` đã định nghĩa trong SUMO-SIM/1 | Chỉnh prefix S→D làm thay đổi byte thật, simulator hiểu cùng schema; C10/F05 kiểm tra. Không tùy ý đảo/bỏ trường an toàn |
| Tham số cơ cấu do firmware thực thi | Chưa có cho xe thật | Chỉ mở trường khi có capability/schema/miền và đơn vị đã duyệt | Q/ACK ở mục 7; lỗi/timeout/unsupported giữ cấu hình hiệu lực cũ, F09 |
| Feed cập nhật | Chưa cấu hình | HTTPS tuyệt đối, host hợp lệ, không userinfo/fragment; chỉ đích quản trị đã xác nhận | Chưa có URL thì thông báo chưa cấu hình; không dựng URL giả; A01/A08 |

Sensitivity/deadZone/gain/turnPercent/duration/endpoint/interval/template/weapon mode/input mode chỉ sửa sau khi phiên lái đã dừng; không cho lưu giữa gesture. Validation toàn bản, hủy hoặc sai một trường giữ nguyên dữ liệu và giá trị hiệu lực cũ. Không cho tùy chỉnh PWM ±255, ngưỡng STOP 10 ms, watchdog 300 ms, quyền bỏ qua chữ ký hay tắt fail-safe.

Template được biên dịch từ danh mục cho phép, đúng mỗi placeholder, không CR/newline lồng, không mã thực thi; UI nhận cách viết `\n` rồi codec chuyển thành một byte LF. Prefix khác chỉ được bổ sung sau khi hợp đồng receiver được duyệt; chưa có chức năng gửi chuỗi tùy ý đến xe. Các mẫu M/B/W/K/T gốc vẫn là fixture đối chiếu SRS, không coi tự động tương thích với receiver mới.

Lưu cấu hình nhỏ bằng API nền tảng, có `schemaVersion=1`, ghi nguyên bản cấu hình đã validation. Phân biệt “đã lưu trên app”, “đang chờ receiver” và “đã ACK áp dụng”; duration/thiết lập cơ cấu chỉ báo hiệu lực receiver sau phản hồi khớp. Sau khi dừng/đóng phiên để sửa Settings, nếu cần gửi cấu hình receiver thì mở phiên mới trung tính, chỉ gửi neutral/config, khóa lái tới khi đối soát ACK; không khôi phục phiên/thao tác cũ. Lần mở lại/nâng APK giữ giá trị, thêm mặc định chỉ trường mới; schema lạ/lỗi migration giữ bản cũ để phục hồi, báo lỗi và khóa điều khiển thay vì xóa toàn bộ. Không lưu pointer, mode AUTO đang chạy, Boost, macro hoặc phiên UDP. Reset mặc định cần xác nhận và trạng thái dừng. F06/F08/F10 và A10/A11 phải kiểm tra thật ở các phase sau.

## 7. Hợp đồng bản thử SUMO-SIM/1-draft1

**Toàn bộ mục này chờ duyệt G0, chưa phải firmware contract.** Khung ASCII, một datagram UDP chứa đúng một frame, một LF cuối (`0x0A`), không CR/NUL/BOM/trailing bytes, tối đa **256 byte gồm LF**. Trong bảng/mã dưới đây `\n` biểu diễn một byte LF, không phải hai byte backslash+n. Số thập phân không dấu `+`, không khoảng trắng; cờ 0/1; PWM -255…255. SID/nonce là 16 ký tự hex thường, không toàn 0; seq và operation ID 1…2147483647, không wrap, hết miền phải tạo phiên mới.

### 7.1 Mở phiên, xác nhận và hết hạn

- App mở socket mới cho mỗi lần Reconnect, nonce mới và input trung tính; gửi `H1:<nonce>\n`. Receiver tạo SID ngẫu nhiên mới, ràng buộc với peer IP/port và lần boot; trả `H1:<nonce>,<sid>,<caps>\n`. Bit capability: 1=state/weapon boolean, 2=timed turn, 4=AUTO, 8=servo config fixture. Simulator thường caps=3. Đây là capability kỹ thuật, không thay thế phê duyệt thể lệ/cơ khí.
- Offer sống 1000 ms; cùng nonce/peer trong thời gian đó nhận lại cùng SID, không làm mới hạn offer. Khi đã có phiên active, HELLO không chiếm quyền/thay phiên: trả `N1:<nonce>,BUSY\n`. Offer hết hạn thì SID cũ vô hiệu.
- App xác nhận bằng frame seq=1, X=0, mode=0, turnId=0, duration=0, S toàn 0. Chỉ sau ACK hợp lệ mới Online. Receiver đang Kill vẫn giữ Kill trong ACK; handshake không mở khóa. Timeout bắt tay 1000 ms; retry HELLO 250 ms, tối đa trong deadline đó; không tự lặp reconnect vô hạn.
- Frame điều khiển có seq tăng toàn cục trong SID; chỉ nhận peer/SID đúng. Sau khi envelope hợp lệ, seq mới được ghi làm mốc đã thấy kể cả payload bị từ chối; không áp dụng frame seq thấp hơn. Duplicate trả lại ACK đã biết nếu còn cache, không thực thi lại và không làm mới watchdog. Frame không parse được nguồn/SID/seq thì bỏ, không thay trạng thái.
- Sau **300 ms không có frame điều khiển mới, hợp lệ, được áp dụng**, receiver đưa motor về 0, B/W/K=0, hủy macro, về MANUAL và **hủy SID**. Dữ liệu lỗi, duplicate, HELLO, Q/config hoặc SID hết hạn không kéo dài watchdog. Kill latch vẫn giữ nếu đã bật. Timer phải tick độc lập với socket receive. Sau timeout phải handshake lại; gói tới muộn của SID cũ không được làm chạy lại.
- App Offline sau 300 ms từ ACK tiến triển cuối cùng (hoặc hết deadline bắt tay), hủy input/macro và đóng phiên; lỗi I/O/quyền báo ngay. Không có reply không được báo Online. Chỉ ACK đúng endpoint/SID, thuộc seq thực đã gửi, lớn hơn seq đã ACK và còn trong cửa sổ 300 ms mới cập nhật trạng thái/ping. RTT lấy clock monotonic app từ send tới ACK đó, không phải độ trễ một chiều.

SID/nonce chống nhận nhầm phiên và replay phiên đã hết hạn, **không phải xác thực mật mã**. Bản thử chỉ bind loopback trên máy phát triển. Khi chốt kết nối xe thật cần đánh giá mạng/đối thủ và hợp đồng firmware; không coi UDP thử là kênh đã được bảo vệ cho thi đấu.

### 7.2 Snapshot, Kill, mode, macro và cấu hình

Khung snapshot: `V1:<sid>,<seq>,<X>,<mode>,<turnId>,<durationMs>|S:<L>,<R>,<B>,<W>,<K>\n`. Alias D thay S được hỗ trợ đúng cùng 5 trường. X=1 yêu cầu Kill, X=0 **không** tự mở Kill latch; mode 0=MANUAL, 1=AUTO. turnId=duration=0 nghĩa không macro và hủy macro đang chạy. Không macro thì duration bắt buộc 0. Macro khác 0 yêu cầu duration 1–2000, L/R/B/W/K/X/mode đều 0 trong yêu cầu; receiver thực hiện bánh -255/+255, B=0. Snapshot định kỳ giữ cùng turnId/duration để duy trì phiên trong lúc macro, không ghi đè PWM macro bằng L/R=0.

Mỗi lần bấm quick turn có ID tăng riêng trong phiên. Receiver giữ ID cao nhất, duration và trạng thái xong/hủy đến hết SID. ID mới chỉ được bắt đầu khi đầu ra trước đó trung tính, MANUAL, không Brake/Kill/vũ khí và không macro khác đang chạy; lặp ID đang chạy được phép duy trì, không cần motor đang 0. ID mới bắt đầu đúng một lần; cùng ID không khởi động lại kể cả đã xong; cùng ID đổi duration bị từ chối, ID cũ hơn bị từ chối. Snapshot turnId=0 kết thúc/hủy macro nhưng không xóa lịch sử ID. Macro timer độc lập chu kỳ gửi; hết timer dừng dù app còn gửi ID đó. Nếu ACK kết thúc bị mất, app tiếp tục cùng ID vẫn không xoay lần nữa.

Kill hợp lệ ép 0, hủy macro, W/K/B=0 và về MANUAL. Brake K=1 ép 0, B/W=0, macro=0, mode=MANUAL. Receiver kiểm tra mâu thuẫn trường; yêu cầu thường không hợp lệ không duy trì watchdog. Khi đã Kill, yêu cầu chuyển động bị LOCKED. Unlock riêng: `V1:<sid>,<seq>,0,0,0,0|U\n`, chỉ chấp nhận từ phiên đúng, receiver đang trung tính và không Brake/macro; ACK trả X=0. App chỉ sinh U qua thao tác mở khóa ở mục 5.2. Vào AUTO chỉ được chấp nhận từ đầu ra trung tính, không macro/Brake/Kill và có capability cần thiết; request mode1 phải có L/R/B/W/K/X/turnId/duration đều 0, heartbeat AUTO cũng giữ các trường này 0 để không chèn lệnh manual. Fixture chỉ giữ PWM0; motor tự hành thật thuộc firmware chưa xác nhận. Thoát AUTO bằng request mode0 trung tính được phép ngay, atomically dừng trước khi về MANUAL; không yêu cầu chờ thuật toán tự hành tự dừng. Kill/Brake luôn ưu tiên. App chờ ACK trước khi hiển thị mode có hiệu lực và chỉ nhận thao tác manual mới sau đó.

Đóng phiên: envelope trung tính với payload `C`, receiver dừng/hủy macro, về MANUAL, giữ Kill latch hiện tại, ACK rồi retire SID. App cố gửi STOP/C trước khi đóng, không chờ mạng trên UI; nếu không nhận ACK ghi chưa xác nhận, receiver phải dựa watchdog. Trước tải/cài update, nếu không có ACK dừng/đóng thì đợi ít nhất deadline watchdog của phiên cũ, hiển thị chưa xác minh dừng vật lý, không tự tuyên bố xe đã dừng.

ACK state: `A1:<sid>,<seq>,<result>,<L>,<R>,<B>,<W>,<K>,<X>,<mode>,<turnId>,<turnActive>\n`. State trong ACK là **đầu ra áp dụng hiện tại** của receiver. result thuộc `OK`, `LOCKED`, `BAD`, `UNSUPPORTED`, `BUSY`; lỗi có envelope đủ tin cậy mới trả ACK, không làm mới watchdog. ACK không đồng nghĩa bánh đã dừng cơ học. Sender không giữ hàng đợi snapshot chuyển động cũ: mọi lần encode lấy state mới nhất trong một bộ điều phối; STOP/Kill xóa việc gửi lỗi thời đang chờ và phát ngay. Gói đã lên mạng dựa seq/SID phía nhận để loại cũ.

Các mức cơ cấu vật lý chưa xác định nên chưa bật trường đó cho xe thật. Để test F09, fixture caps=11 có thể hỗ trợ `V1:<sid>,<seq>,0,0,0,0|Q:<configId>,<homeDeg>,<activeDeg>\n`; hai giá trị nguyên 0–180, mặc định fixture 0/90, chỉ áp dụng khi neutral/MANUAL. Receiver validation và áp dụng **cả cặp nguyên khối**, không áp dụng từng trường; reply `Q1:<sid>,<seq>,<configId>,<homeDeg>,<activeDeg>,<result>\n` echo bản cấu hình yêu cầu, chỉ `OK` mới xác nhận đã lưu cả cặp. `UNSUPPORTED`/`BAD` không thay cấu hình receiver. Cùng configId/cặp giá trị được retry với seq mới và ACK idempotent; cùng ID nhưng cặp khác bị từ chối. App chỉ commit cặp sau ACK đúng. Nếu timeout có thể ACK đã mất sau khi receiver áp dụng: giữ bản app cũ, ghi **receiver chưa xác nhận**, khóa điều khiển đến khi đối soát/gửi lại bản cấu hình có ACK; không tuyên bố receiver chắc chắn còn bản cũ. Không dùng 0/90 làm mặc định cơ khí xe. Firmware chưa xác nhận servo/miền thì capability này tắt, trường ẩn hoặc ghi không hỗ trợ; không tự gửi góc vào cơ cấu chưa biết. Q/ACK cấu hình không duy trì watchdog lái và không thay thế ACK state để báo Online.

Gói S nguyên bản và các lệnh M/B/W/K/T gốc có fixture exact bytes riêng để đối chiếu, nhưng **không được gửi raw bên ngoài V1** trong SUMO-SIM/1. Phải kiểm tra lại firmware tại Phase 6; không có cơ chế tự đoán giao thức hoặc fallback raw khi handshake lỗi.

### 7.3 Ví dụ byte và trạng thái mong đợi

Nonce `1111111111111111`, SID `0123456789abcdef` dưới đây chỉ là fixture cố định; chạy thật phải tạo mới. Các dòng độc lập là **mẫu dự kiến chưa chạy**; khi thực hiện theo chuỗi phải dùng seq tăng và bảo đảm điều kiện trung tính tương ứng.

| Tác động | Frame app → simulator dự kiến | ACK/đầu ra dự kiến |
|---|---|---|
| HELLO | `H1:1111111111111111\n` | `H1:1111111111111111,0123456789abcdef,3\n` |
| Xác nhận/đứng yên | `V1:0123456789abcdef,1,0,0,0,0|S:0,0,0,0,0\n` | `A1:0123456789abcdef,1,OK,0,0,0,0,0,0,0,0,0\n` |
| Tiến limiter 60% | `V1:0123456789abcdef,2,0,0,0,0|S:153,153,0,0,0\n` | `A1:0123456789abcdef,2,OK,153,153,0,0,0,0,0,0,0\n` |
| Giữ Boost khi tiến | `V1:0123456789abcdef,3,0,0,0,0|S:255,255,1,0,0\n` | `A1:0123456789abcdef,3,OK,255,255,1,0,0,0,0,0,0\n` |
| Nhả hướng, Boost vẫn chạm | `V1:0123456789abcdef,4,0,0,0,0|S:0,0,0,0,0\n` | Hai bánh 0, B hiệu lực 0; ACK seq4 toàn 0 |
| W hold/toggle bật lúc đứng yên | `V1:0123456789abcdef,5,0,0,0,0|S:0,0,0,1,0\n` | `A1:0123456789abcdef,5,OK,0,0,0,1,0,0,0,0,0\n` |
| Nhả hold/toggle tắt | `V1:0123456789abcdef,6,0,0,0,0|S:0,0,0,0,0\n` | W=0; ACK seq6 toàn 0 |
| Giữ Brake | `V1:0123456789abcdef,7,0,0,0,0|S:0,0,0,0,1\n` | `A1:0123456789abcdef,7,OK,0,0,0,0,1,0,0,0,0\n` |
| Nhả Brake | `V1:0123456789abcdef,8,0,0,0,0|S:0,0,0,0,0\n` | K=0 và PWM 0; không chạy lại hướng cũ |
| Kill | `V1:0123456789abcdef,9,1,0,0,0|S:0,0,0,0,0\n` | `A1:0123456789abcdef,9,OK,0,0,0,0,0,1,0,0,0\n` |
| Unlock có chủ ý | `V1:0123456789abcdef,10,0,0,0,0|U\n` | `A1:0123456789abcdef,10,OK,0,0,0,0,0,0,0,0,0\n` |
| Yêu cầu AUTO trên caps=3 | `V1:0123456789abcdef,11,0,1,0,0|S:0,0,0,0,0\n` | `A1:0123456789abcdef,11,UNSUPPORTED,0,0,0,0,0,0,0,0,0\n`; vẫn MANUAL |
| AUTO trên fixture caps=7 | Cùng frame mode1, trong phiên fixture được phép riêng | ACK `OK`, mode1, PWM0; chỉ test mode, không chứng minh tự hành |
| Về MANUAL | `V1:0123456789abcdef,12,0,0,0,0|S:0,0,0,0,0\n` | ACK mode0/PWM0 trước thao tác mới |
| Quick turn lần 1, 200 ms | `V1:0123456789abcdef,13,0,0,1,200|S:0,0,0,0,0\n` | `A1:0123456789abcdef,13,OK,-255,255,0,0,0,0,0,1,1\n` |
| Giữ heartbeat macro cùng ID | `V1:0123456789abcdef,14,0,0,1,200|S:0,0,0,0,0\n` | Không reset timer; sau 200 ms ACK PWM0, turnId1/active0 |
| Hủy macro/STOP | `V1:0123456789abcdef,15,0,0,0,0|S:0,0,0,0,0\n` | PWM0/macro hủy; nhận lại seq13 không đổi trạng thái/watchdog |
| Đổi template đã hỗ trợ | `V1:0123456789abcdef,16,0,0,0,0|D:153,153,0,0,0\n` | Byte prefix thay đổi, đầu ra như S; template khác whitelist bị chặn tại app |
| Đóng phiên | `V1:0123456789abcdef,17,0,0,0,0|C\n` | ACK PWM0 rồi SID vô hiệu; frame seq18 của SID cũ bị bỏ |
| Config trên fixture caps=11, phiên riêng | `V1:0123456789abcdef,2,0,0,0,0|Q:1,0,100\n` | `Q1:0123456789abcdef,2,1,0,100,OK\n`; caps=3 trả UNSUPPORTED, không áp dụng |

Gói đúng seq nhưng mất STOP trên mạng vẫn có thể duy trì đầu ra trước đó đến khi có snapshot mới/watchdog; UDP không bảo đảm giao nhận. Seq chỉ loại được gói đã bị thứ tự mới vượt qua, không tự đo tuổi gói từ timestamp hai máy. Đề xuất này không hứa dừng cơ học trong 10 ms khi mất mạng; H04/H05/H08 phải kiểm chứng receiver thật và giới hạn mạng tại G6.

## 8. Đo lường và test đề nghị duyệt

Không có số đo điều khiển thực tế trong Phase 0. Sau duyệt, chạy nhóm test đúng phase trong kế hoạch, giữ test lỗi trước triển khai. Bổ sung exact-bytes cho toàn envelope/ACK và ca hết SID, không dùng fixture S nguyên bản để thay cho kiểm thử hợp đồng mới.

| Phép đo | Mốc và số mẫu đề nghị | Tiêu chí và giới hạn |
|---|---|---|
| STOP ≤10 ms | Cùng clock monotonic app: callback nhận up/cancel là t0; trước `send()` là t1, sau khi `send()` trả là t2. Tối thiểu 500 lần, gồm lúc sender bận | Báo riêng t1−t0 và t2−t0; max mỗi loại ≤10 ms, không xóa mẫu chậm. Không bao gồm độ trễ chạm vật lý tới callback, không chứng minh datagram tới motor |
| Chu kỳ gửi | Log tick và send thật, phân loại event tức thời; chạy 20/30/50 ms, mỗi mức ≥1000 interval khi giữ | Mục tiêu mọi interval định kỳ trong 20–50 ms, báo phân vị/max và vi phạm; với 20/50 ms có thể lộ jitter OS, không nới cận ngầm. Event STOP/Kill có thể <20 ms và được tách riêng |
| Phản hồi hệ thống <30 ms | 1000 mẫu trên điện thoại/xe thật, mốc kích thích chạm được thiết bị đo chung ghi nhận tới driver/motor áp dụng bằng cùng clock/logic analyzer | Báo p50/p95/p99/max và số mẫu ≥30 ms; phải mọi mẫu <30 ms. Phương án cảm biến chạm/GPIO hoặc thiết bị đo cụ thể cần chốt với nhóm firmware trước H08; chưa có thiết bị thì chưa kiểm chứng, không thay bằng RTT |
| Watchdog 300 ms | Self-test simulator: 299/300/301 ms bằng clock giả; firmware: last-valid-frame tới motor-off theo clock MCU/logic analyzer; đề nghị ≥100 lần mất gói ở các trạng thái | Ở 300 ms phải yêu cầu dừng; ghi độ phân giải/tick và jitter thực. Bad/duplicate/old/Q/HELLO không làm mới. Không dùng watchdog simulator để duyệt MCU |
| Ping | ACK đúng nonce/SID/seq đã gửi; monotonic cùng app | RTT thật, không trừ timestamp host/Android; Offline xóa ping hiện thời, không giữ số cũ như đang Online |
| Góc xoay/phanh/rung | Xe/driver/điện thoại thật ở Phase 6 | Sai số góc, pin/tải, cách đo hãm và trạng thái W an toàn cần thông tin phần cứng; không lấy 200 ms hoặc ACK làm bằng chứng vật lý |
| Soak | 30 phút, ít nhất 500 chu kỳ nhấn/nhả và 20 reconnect; log số sender/socket/pointer | Không crash/ANR, không rò rỉ sender/socket, không thao tác còn giữ sau kết thúc; thu XML/log/video và số lỗi |

Scenario deterministic đề xuất cho simulator: `normal`; `drop-rx` bỏ mọi control frame sau mốc chỉ định; `drop-reply` vẫn áp dụng nhưng không ACK; `delay` giữ một control frame hoặc reply **150 ms**, log rõ nhánh; `duplicate` xử lý hai lần cùng frame, thêm ca seq mới nhưng cùng turnId; `reorder` giữ frame di chuyển seq n, áp dụng STOP/Kill seq n+1 rồi phát lại n sau **100 ms**; `malformed` lần lượt ACK thiếu trường, sai SID, sai peer, future seq chưa gửi. Thêm ca delay **350 ms** vượt expiry, restart receiver/app, seq biên, offer replay, thiếu LF/literal backslash-n, payload >256 byte và config lỗi. Không dùng random không seed; mỗi log có monotonic, peer, SID/seq, raw payload, accept/reject/lý do, đầu ra PWM/flags, macro/watchdog.

Quyền nền tảng: giữ min/target/compile hiện tại. Với target 37, Phase 4 phải khai báo và xin `ACCESS_LOCAL_NETWORK` trên OS 37 trước socket LAN, thử cấp/từ chối/thu hồi; Phase 3 cần INTERNET và luồng REQUEST_INSTALL_PACKAGES cho updater. Android 16 chưa được xác nhận trên điện thoại; API 37 PASS không bao phủ API 36. Hướng dẫn hiện hành đã đối chiếu với [Android Developers về LAN](https://developer.android.com/privacy-and-security/local-network-permission). Chưa chạy test quyền LAN bằng app mẫu.

## 9. Phân phối, ký và cập nhật APK test đề xuất

Đề nghị giữ **applicationId `com.aistudy.myapplication`** cho chuỗi test→bàn giao để giảm thay đổi package/test hiện có; tên hiển thị có thể đổi thành Sumo Controller. Đây là lựa chọn cần người dùng duyệt trước lần phân phối đầu, chưa sửa build config. Nếu muốn package khác thì đổi trước lần phân phối đó và cập nhật assertion instrumentation cùng lúc.

Đề nghị dùng **một khóa riêng ổn định do chủ dự án sở hữu**, alias dự kiến `sumo-controller`, keystore đặt ngoài repository, ví dụ thư mục riêng `C:\Users\CHI NGUYEN\.android\sumo-signing\`; **chưa tạo khóa**, chưa chọn vị trí backup. Chủ dự án giữ mật khẩu và một bản backup mã hóa ở nơi riêng. Build nhận đường dẫn/mật khẩu qua môi trường hoặc cấu hình local ngoài repo, không in giá trị bí mật vào log. APK test và release bàn giao cùng khóa; không lấy khóa Android Debug hiện tại làm khóa phân phối. Bản debug cho emulator có thể tiếp tục dùng signing mặc định; trước test điện thoại phải cấu hình signing chuỗi ổn định, nếu thiếu thì build phân phối lỗi rõ thay vì fallback sang khóa khác.

Đề xuất bản phân phối đầu A versionCode=**2**, versionName=`0.1.0-test.2`; B=3, C=4; các bản tiếp theo luôn lớn hơn **mọi** versionCode đã công bố cùng identity, kể cả bản final. Không upload lại byte khác dưới cùng version/path, không hạ version. Android cho phép một số cập nhật cùng versionCode; dự án chọn quy tắc **tăng nghiêm ngặt** để truy vết. Điều kiện package/chữ ký được đối chiếu với [How app updates work](https://developer.android.com/google/play/app-updates).

Đích hosting **chưa có**. Cần người dùng cung cấp/chọn host HTTPS, URL metadata và nơi upload APK cùng quyền truy cập phù hợp. Chưa tạo tài khoản/server, chưa khai báo URL hoạt động, chưa upload APK. Có thể triển khai validator/manager/UI và test fixture độc lập khi đến phase; phần upload/A10 thật giữ chưa kiểm chứng cho tới có đích và điện thoại.

Quy trình công bố tối thiểu sau khi các phase cần thiết được duyệt:

1. Sửa lỗi có ca tái hiện, chạy unit/lint/smoke liên quan; chỉ tạo APK test trước G6, ghi rõ lỗi chưa xử lý.
2. Tăng versionCode; build/ký test bằng khóa đã chốt; verify APK thực: package, cert so với cert tin cậy đã ghi, version, minSdk, size và SHA-256.
3. Upload APK theo thư mục phiên bản, ví dụ đường dẫn tương đối `apk/2/sumo-test-2.apk`; tải lại kiểm tra đúng byte/hash. Chỉ sau đó công bố metadata `test.json`. Đây là mẫu đường dẫn, không phải URL hosting đã có.
4. Metadata bắt buộc: schemaVersion=1, applicationId, versionCode, versionName, minSdk, apkUrl HTTPS, sizeBytes, sha256, releaseNotes, channel=`test`. APK phải khớp metadata thực tế; chỉ nhận version lớn hơn bản đang cài, đúng signer hiện cài, đúng package và tương thích OS. Không tin signer mới do feed tự công bố.
5. Lần đầu nhận APK qua HTTPS trên điện thoại; những lần sau mở app → kiểm tra → xem thay đổi → tải → xác nhận cài Android. Kiểm tra/tải chỉ theo thao tác người dùng; mở updater dừng phiên, hủy macro/input, không tự nối lại để chạy sau cập nhật.
6. HTTPS cho feed/APK và mọi redirect; đề nghị chỉ cùng host đã duyệt, tối đa 3 redirect, timeout kết nối 10 s/đọc 30 s, feed ≤64 KiB, APK ≤100 MiB; không bỏ kiểm tra TLS. Tải cache riêng, kiểm tra hoàn tất/hash, rồi đối chiếu metadata package/version/minSdk/chữ ký APK bằng API nền tảng trước PackageInstaller.
7. Kiểm tra `canRequestPackageInstalls`, đưa tới màn hình cấp quyền đúng app khi cần; xử lý pending user action và kết quả thành công/hủy/lỗi của [PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller). Không cài im lặng, tự cấp quyền hoặc gỡ app để vượt lỗi chữ ký. Hủy/lỗi giữ bản hiện tại và cấu hình, cho thử lại.
8. Người dùng mở lại, đọc version package thực đã cài và cấu hình/migration; luôn trung tính, MANUAL, không khôi phục session. A→B→C cần ít nhất hai cập nhật qua Internet, có video/công cụ ngoài tiến trình app; `adb install` trên AVD ở Phase 0 không phải bằng chứng đó.

Nếu Wi-Fi xe không có Internet, dừng/kết thúc phiên trước khi người dùng đổi mạng để tải. Lái xe không phụ thuộc feed cập nhật. Khóa/tokens không nằm trong source/APK/feed/log chia sẻ; `.gitignore` hiện tại chưa có quy tắc khóa riêng, cần bổ sung khi cấu hình signing ở Phase 3. Bản release dùng thi đấu vẫn chờ G6, smoke/nâng từ test giữ cấu hình tại G7.

## 10. Thiếu sót, điều chỉnh đề xuất và thông tin còn thiếu

| Mục | Phát hiện/đề xuất | Ảnh hưởng và thời điểm giải quyết |
|---|---|---|
| Giao thức gốc chưa đủ | S 5 trường thiếu Kill/mode/macro ID/seq/ACK; dùng envelope V1 cho simulator, raw S chỉ fixture | Cần duyệt D05 trước G0; thay đổi exact-byte tests/encodeControl dự kiến, không khẳng định tương thích firmware |
| Boost/mixer chưa duy nhất | Đã nêu công thức và cách Boost giữ tỷ lệ, không boost hai lần | Cần duyệt D02; tác động kỳ vọng L/F và firmware Phase 6 |
| Không tự phục hồi | Brake/unlock/reconnect/macro xong cần input mới; W0 là giả định simulator cho lỗi toàn cục | Cần duyệt D03/D04/D09; không thay thế quyết định trạng thái cơ khí |
| Nhả nhiều pointer điều hướng | Nhả/mất vùng một pointer hợp lệ dừng PWM/B và vô hiệu điều hướng cũ; tick/move không phục hồi, phải nhấc hết điều hướng cũ rồi down mới; giữ Boost/vũ khí theo 5.2.1–5.2.2 | ĐỀ XUẤT CHỜ DUYỆT thuộc D04; bổ sung reducer L17–L22, chưa triển khai/chưa chạy |
| 0.7 nhắc Device Manager UI | Đã chứng minh boot/ADB/instrumentation qua CLI, chưa mở UI Device Manager | Đề nghị chấp nhận bằng chứng tương đương cho môi trường G0 |
| Offline cache chưa đầy | Lint/UTP thiếu cache; tải đúng dependency từ repositories hiện có, sau đó chạy thành công | Lưu cả lỗi và lần chạy thành công; không hạ phiên bản/skip test |
| JDK loopback/temp | Thêm JAVA_TOOL_OPTIONS riêng terminal để đường dẫn Unix socket ngắn | Có lệnh tái hiện trong COMMANDS; chưa sửa project hoặc cấu hình máy |
| G3/F phụ thuộc G4 | Kế hoạch G3 yêu cầu tham số/tần suất/simulator có hiệu lực nhưng session/transport hoàn thiện ở G4 | Giữ test lõi/codec/store/fake và updater ở G3; đo qua socket/F02/F04/F07/F09 và A13 chạy tại G4, duyệt tổng ở G5. Không ghi phần chưa chạy thành PASS G3 |
| Chu kỳ biên/jitter | Cận 20–50 ms ở cấu hình 20/50 dễ có mẫu vượt do scheduling OS | Báo dữ liệu thật, chưa nới ngưỡng. Nếu cần tolerance phải có quyết định thay đổi yêu cầu trước tuyên bố đạt hiệu năng |
| Firmware/MCU/driver/cơ cấu/transport | Chưa có thông tin hoặc mã firmware | Không cản lõi/simulator được duyệt; chặn tương thích xe, W safe, phanh, watchdog thật, AUTO và G6 |
| Điện thoại/OS/thể lệ | Chưa xác nhận model/API36 hay khác, vòng AUTO/8 hướng, hạn thi | Không cản G0 độc lập; cần trước kiểm thử API36/phone update và H/G6. Chưa tự ghi R10 không áp dụng |
| Khóa ký/chủ quản/backup | Mới có đề xuất identity/alias, chưa có khóa được chốt | Có thể duyệt quy trình G0; phải hoàn tất và ghi fingerprint trước APK test đầu trên điện thoại, không đợi G7 |
| Hosting HTTPS/quyền upload | Chưa được cung cấp | Không cản validator/fixture; chặn upload và nghiệm thu update không USB/A10, không tuyên bố đã phân phối |
| Phép đo vật lý | Chưa có thiết bị đo chung, giới hạn góc/phanh/pin/tải | Chốt thiết bị/cách ghi mốc trước H06/H08; chưa thể nghiệm thu R07/R08/R15/R17/R19 thật |
| Quản lý nguồn | Chưa có Git | Đã dùng checksum tái hiện baseline; không tự tạo repo/commit hoặc thay đổi workflow của người dùng |

## 11. Đối chiếu G0 và quyết định cần duyệt

| Điều kiện G0 | Tình trạng tại thời điểm lập biên bản |
|---|---|
| STOP/Brake/Kill/Boost/đa ngón có cách hiểu duy nhất | Đã có đề xuất ở mục 5, gồm đầu ra ngay và mọi tick sau nhả nhiều pointer tại 5.2.1–5.2.2; **chờ duyệt**, chưa coi đã thống nhất |
| Có mẫu input/output đứng yên/tiến/Boost/W/Brake/Kill/unlock/mode/xoay/reply | Có bản mẫu ở mục 7.3; **chờ duyệt hợp đồng**, chưa chạy codec/simulator |
| AUTO/8 hướng có quyết định phạm vi | Có phương án ở 5.1; **chưa có quyết định người dùng**, chưa đánh dấu không áp dụng |
| Mốc 10/<30/300 ms rõ ràng | Có đề nghị mốc/cỡ mẫu ở mục 8; chờ duyệt; thiết bị đo thật còn thiếu |
| AVD khởi động được | **Đã có bằng chứng thực tế** boot/ADB/test/ảnh; phương pháp CLI thay Device Manager cần người duyệt chấp nhận |
| Danh mục/miền tham số, quy trình APK/identity ổn định | Đã có đề xuất mục 6/9; chưa chốt identity/khóa/backup. Hosting ghi rõ còn thiếu, không chặn unit độc lập |
| Người dùng duyệt bản thử/giao thức/các đề xuất | **CHƯA CÓ — G0 chưa được duyệt**, không chuyển Phase 1 |

Danh sách phê duyệt theo mã kế hoạch:

| Mã | Đề nghị quyết định | Trạng thái |
|---|---|---|
| D01 | Giữ emulator trước, phone update test không USB | Yêu cầu đã xác nhận; chấp nhận bằng chứng AVD CLI ở 0.7 còn chờ |
| D02 | Công thức mixer, làm tròn, gain/dead zone, Boost không sinh hướng và giữ tỷ lệ ở mục 5.3 | CHỜ DUYỆT |
| D03 | Kill/lỗi phiên > Brake > macro hợp lệ > điều khiển thường, AUTO guard và W safe mô phỏng | CHỜ DUYỆT |
| D04 | STOP/release/cancel/nền, Brake/unlock/reconnect/macro không khôi phục thao tác cũ, unlock giữ 1 s; nhả/mất vùng một pointer điều hướng vô hiệu điều hướng cũ, chờ nhấc hết rồi down mới, giữ độc lập Boost/vũ khí theo 5.2.1–5.2.2 | CHỜ DUYỆT |
| D05 | SUMO-SIM/1-draft1, LF/256 byte, snapshot/ACK/session/expiry/ID macro/alias template và ví dụ | CHỜ DUYỆT |
| D06 | Danh mục, mặc định/miền ở mục 6, live limiter và stop-before-settings, schema/migration | CHỜ DUYỆT |
| D07 | UDP loopback simulator; D-Pad 4 + joystick; chưa xác nhận transport xe thật, 8 hướng riêng cần quyết định | CHỜ DUYỆT |
| D08 | Mốc/cỡ mẫu/soak/scenario deterministic ở mục 8; không thay RTT cho số đo vật lý | CHỜ DUYỆT |
| D09 | Giữ hold/toggle, AUTO khóa theo capability/phê duyệt, W0 chỉ mô phỏng và cách macro bị hủy | CHỜ DUYỆT |
| D10 | Quy trình HTTPS/PackageInstaller và giữ cấu hình; timeout/giới hạn tải/redirect đề xuất ở mục 9 | Yêu cầu update đã xác nhận; chi tiết kỹ thuật CHỜ DUYỆT |
| D11 | Giữ `com.aistudy.myapplication`, khóa riêng do chủ dự án giữ/backup, A=2/B=3/C=4; host cung cấp trước upload | CHỜ DUYỆT identity/quản lý khóa; hosting CHƯA CÓ |

Người duyệt có thể ghi “duyệt G0 theo D01–D11 trong biên bản”, kèm các sửa đổi cụ thể và quyết định D-Pad 8 hướng/AUTO cho bản thử. Thông tin đích phân phối và khóa cần hoàn tất trước lần phân phối đầu; thiếu xe thật không ngăn duyệt hợp đồng mô phỏng nhưng vẫn chặn G6.

**Kết luận người thực hiện:** Đã hoàn tất khảo sát và chuẩn bị hồ sơ Phase 0 để xem xét. Kiểm tra nền thành công sau xử lý giới hạn môi trường/dependency, chỉ chứng minh project mẫu và AVD chạy được. Không có gate chức năng được đánh dấu đạt.

**Quyết định người duyệt:** Đã duyệt G0 theo D01–D11 và 5.2.1–5.2.2, kèm ủy quyền đợt Phase 1+2 nêu trên (chat ngày 17/10/2026).  
**Tên/vai trò người duyệt và thời điểm:** Người dùng/chủ dự án qua chat ngày 17/10/2026; chưa có xác nhận firmware/xe thật.  
**Phạm vi được phép chuyển tiếp hiện tại:** Phase 1+2 theo ủy quyền trên; dừng trước Phase 3, không tạo release.  
**Việc tiếp theo sau G0:** Thực hiện Phase 1 theo kế hoạch sẵn có: viết test đỏ cho DriveMixer/ControlReducer và cấu hình liên quan, gồm các ca nhả pointer mới nếu được duyệt; triển khai tối thiểu, chạy L01–L22/F01/F03, lưu bằng chứng rồi trình G1; không tự chuyển các gate sau.## Quyết định bổ sung ngày17/10/2026 — chỉ cho G5 AVDSau kết quả Phase5, người dùng đã duyệt giữ lịch danh nghĩa20/30/50ms và cho phép riêng cấu hình50ms có interval20–60ms trên AVD (+10ms jitter);20/30ms vẫn20–50ms,≥1000interval mỗi mức, STOP500 vẫn≤10ms. Dùng cohort mới sau quyết định, không chấm lại hồ sơ lỗi cũ thành PASS. Điện thoại/HTTPS/API36/I11/xe thật tiếp tục CHƯA KIỂM CHỨNG theo phạm vi được duyệt. Đây không phải duyệt kết quả G3/G4/G5 hoặc quyền release. Chi tiết: [điều chỉnh G5 AVD](../phase-5/DIEU_CHINH_G5_AVD.md).
