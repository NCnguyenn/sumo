# Mô tả dự án ứng dụng điều khiển robot Sumo

Ngày lập: 16/10/2026, múi giờ Asia/Bangkok.  
Trạng thái cập nhật18/10/2026: **G0/G1/G2 đã được người dùng duyệt; G3/G4 CHỜ DUYỆT CHUNG; G5 CÓ ĐIỀU KIỆN cho chức năng AVD.** UI/Settings/updater và UDP/ControlSession đã triển khai; STOP/cadence vẫn là lỗi mở, chặn dùng xe/release. Chưa chuyểnPhase6/7, chưa kiểm chứng toàn luồng cập nhật HTTPS trên điện thoại. Hồ sơ: [G3](testing/phase-3/BIEN_BAN_DUYET.md), [G4](testing/phase-4/BIEN_BAN_DUYET.md), [G5](testing/phase-5/BIEN_BAN_DUYET.md), [checkpoint](testing/TIEN_DO_AI.md).

Cập nhật yêu cầu ngày 16/10/2026: thông số vận hành phải chỉnh được ngay trong app; cho phép đưa APK test mới lên mạng để cập nhật trực tiếp trên điện thoại và lặp lại việc sửa mã nguồn/test, không cần USB.

## 1. Mục đích và đầu ra

Xây dựng ứng dụng Android hoạt động như tay cầm điều khiển robot Sumo trong trận đấu. Người lái dùng hai ngón cái để vừa điều hướng hai bánh, vừa tăng tốc hoặc kích hoạt cơ cấu phụ. Ứng dụng phải xử lý nhả tay, khóa khẩn cấp và mất liên lạc theo các quy tắc an toàn được thống nhất với firmware.

Đầu ra của giai đoạn hiện tại là mã nguồn chạy kiểm thử được trên Android Emulator, APK test cập nhật được trực tiếp trên điện thoại, bộ kiểm thử tự động và bằng chứng đối chiếu với đặc tả. Cho phép build, ký và đưa APK test lên kênh thử để lặp lại việc kiểm thử trước nghiệm thu cuối. Bản đóng gói bàn giao dùng thi đấu chỉ được tạo sau khi đạt điều kiện nghiệm thu và được duyệt theo kế hoạch.

Android Emulator cần bản debug để thực thi ứng dụng. APK test gửi cho điện thoại cũng phải được ký bằng khóa ổn định của chủ dự án để các lần cập nhật cài đè được. Chữ ký trên APK test không có nghĩa bản đó đã được nghiệm thu hoặc là bản bàn giao dùng thi đấu.

Kế hoạch chi tiết: [Kế hoạch phát triển và kiểm thử](superpowers/plans/2026-10-16-sumo-controller-test-first.md).

## 2. Căn cứ và quyết định đã có

- Tài liệu gốc: `C:\Users\CHI NGUYEN\Downloads\Dac_Ta_Ky_Thuat_App_Dieu_Khien_Xe_Sumo.docx`.
- Yêu cầu của người dùng: phân tích trước, lập kế hoạch theo phase, kiểm thử đúng yêu cầu rồi mới đóng gói bàn giao.
- Người dùng chọn Android Emulator trong Android Studio làm môi trường kiểm thử ban đầu.
- Người dùng yêu cầu chỉnh tốc độ và các thông số vận hành ngay trong app, lưu được và không build lại chỉ vì đổi giá trị.
- Người dùng yêu cầu vòng lặp sửa mã nguồn → test → build/ký APK test mới → tải lên → kiểm tra/tải/cài cập nhật từ app trên điện thoại → test tiếp, không cần USB.
- Người dùng cho biết điện thoại có thể chạy Android 16; mẫu máy và phiên bản thực tế chưa được xác nhận.
- Nội dung trong tài liệu gốc là yêu cầu sản phẩm. Các đề xuất D01–D11/5.2.1–5.2.2 đã được duyệt G0 cho bản thử; quyết định này không xác nhận phần cứng hoặc giao thức firmware. Đề xuất ngoài G0 vẫn cần được duyệt.

## 3. Lịch sử kiểm tra và trạng thái triển khai

| Hạng mục | Thực tế |
|---|---|
| Thư mục dự án | `C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT` |
| Mã tại khảo sát16/10 | Project mẫu, màn hình Hello Android; không phải trạng thái sau triển khai |
| Công nghệ | Kotlin, Jetpack Compose, Material 3, một module `app` |
| Android Studio | 2026.1.3, JBR 25.0.2 |
| Build | AGP 9.3.3, Gradle 9.5.0, Kotlin Compose plugin 2.2.10 |
| Android SDK | compileSdk 37, targetSdk 37, minSdk 36; Build Tools 36.0.0 |
| Giả lập sẵn có | `Medium_Phone`, ảnh hệ thống API 37.1, x86_64; Emulator 37.1.11 |
| Lần kiểm tra trước | `:app:assembleDebug :app:testDebugUnitTest` thành công; 1 test mẫu, 0 lỗi |
| Thiết bị | ADB chưa thấy thiết bị khi khảo sát; chưa chạy bài điều khiển trên AVD |
| Chức năng Sumo | Chưa có điều khiển, kết nối, heartbeat, fail-safe hoặc cấu hình |
| Quản lý phiên bản | Chưa có thư mục Git tại thời điểm khảo sát |

APK debug mẫu đã được tạo trong lần khảo sát môi trường trước yêu cầu lập kế hoạch này. Nó chỉ chứa ứng dụng mẫu, không chứng minh ứng dụng điều khiển đã hoàn thành. Kết quả build và test mẫu không được dùng để duyệt các phase chức năng.

### 3.1. Kiểm tra lại ngày 17/10/2026

Đã kiểm tra nguồn, SDK/JBR/Gradle và khởi động thực tế `Medium_Phone` bằng CLI Android SDK. AVD boot hoàn tất, Android 17/API 37, x86_64, page size 16 KB; chưa có thiết bị thật kết nối ADB. Unit mẫu 1/1 và instrumentation mẫu 1/1 đạt; build debug thành công; lint chạy thành công với 12 cảnh báo, không có lỗi. Đã lưu cả lỗi sandbox/loopback/thiếu cache và các lần chạy thành công sau đó. Không đổi SDK, dependency hoặc mã nguồn ứng dụng.

Tại mốc khảo sát Phase0, giao diện chỉ có Hello Android, dọc và thấy thanh hệ thống. Các test mẫu không chứng minh bất kỳ chức năng điều khiển, cấu hình, cập nhật hoặc fail-safe Sumo nào đã đạt. Chưa chạy đo độ trễ hoặc thử xe thật.

[Biên bản Phase 0](testing/phase-0/BIEN_BAN_DUYET.md) chứa checksum nguồn, bằng chứng mới, đối chiếu R01–R23, D01–D11 và thông tin còn thiếu. [Nhật ký lệnh](testing/phase-0/COMMANDS.md) ghi lệnh/exit code. Kết quả khảo sát này có trước quyết định duyệt G0; quyết định và triển khai tiếp theo được ghi ở mục 3.2. Hosting/khóa ký riêng vẫn chưa có, không suy ra đã tồn tại chỉ vì quy trình được duyệt.

### 3.2. Kết quả đợt Phase 1+2 ngày 17/10/2026

Người dùng duyệt G0 D01–D11 và quy tắc 5.2.1–5.2.2; chấp nhận CLI AVD, D-Pad 4/joystick/UDP simulator, AUTO khóa mặc định. Phase 1 đủ kỹ thuật đã chuyển Phase 2 theo ủy quyền riêng cho đợt này, không coi là người dùng đã xem kết quả G1. Sau đợt này đã trình G1/G2; người dùng tiếp đó duyệt theo hồ sơ cuối và giao thực hiện Phase3+4.

Đã có DriveMixer, ControlReducer/ControlState, cấu hình/validation thuần Kotlin, codec/ReplyTracker và receiver Python độc lập. Kết quả cuối sau rà soát và sửa hai lỗi: **60/60 JUnit** (59 chức năng + 1 mẫu), **31/31 self-test Python**, **25/25 vector byte Kotlin/Python**; lint 0 error/12 warning baseline. Có ca UDP loopback thật Kotlin ↔ tiến trình Python; watchdog/lỗi mạng kiểm tra bằng clock điều khiển được, chưa phải bằng chứng firmware/xe thật. Red/green, XML, trace và ánh xạ L01–L22/C01–C10/F01/F03 lõi ở hồ sơ G1/G2; sáu ca mới bao phủ D-Pad/calibration và ACK đến sau khi ý định điều khiển đã thay đổi.

Trong riêng đợt Phase1+2, MainActivity/Manifest/Gradle được giữ nguyên và UI còn Hello Android. Chỉnh tham số qua model/reducer đã kiểm tra tác động ở lõi; nhập/lưu qua UI, migration, lịch gửi/STOP Android, tích hợp transport và updater thuộc phase sau. Chưa build/phân phối APK mới, chưa đo độ trễ hoặc thử xe thật trong đợt này.

### 3.3. Kết quả đợt Phase3+4 ngày17/10/2026

Đã nối UI ngang/fullscreen, D-Pad/joystick/đa pointer, Settings/store/migration, Kill lưu riêng, updater và UDP/ControlSession một owner tới receiver Python thật. Theo phản hồi người dùng, giao diện được chỉnh thành D-Pad chữ thập, các cụm nút cân đối, chỉ số hai bánh, dialog cấu hình hai cột và theme tối thống nhất; giữ màu SRS và AUTO khóa.

Bằng chứng tổng thể:97/97 JVM,24/24 Android ở lượt Phase4-26; phần UI/Settings/updater sửa sau đó được kiểm tra lại có chọn lọc, gồm8/8 Settings trên cửa sổ nhỏ và4/4 UDP tích hợp sau modal mới.8/8 fault scenario với NIO đạt. PackageInstaller AVD có prompt/Cancel thật, giữ version1/cấu hình; chưa có cập nhật thành công từ hosting. Python31/31 và25/25 vector là hồ sơ G2 đã duyệt, nguồn/fixture không đổi.

**G3/G4 CHỜ DUYỆT CHUNG.**500 STOP: t1max8.9006ms,t2max13.2437ms,1/500 mẫu t2 vượt10ms. Cadence đủ1000+ ở20/30/50 nhưng có mẫu ngoài20–50ms, chưa đạt chuẩnG0. I11 chưa tạo được LAN còn route khi Internet bị tắt. Hosting/khóa phân phối/điện thoại/firmware còn thiếu; không suy kết quả simulator sang xe thật. Chi tiết và ảnh tại [G3](testing/phase-3/BIEN_BAN_DUYET.md), [G4](testing/phase-4/BIEN_BAN_DUYET.md).

### 3.4. Kết quả đợt độc lập Phase5 ngày17/10/2026

Kết quả lịch sử đợt độc lập Phase5 ở hồ sơ08–15 (ngoại lệ do người dùng cho phép khi G3/G4 còn tồn đọng) được lưu tại [biên bản Phase5](testing/phase-5/BIEN_BAN_DUYET.md), [ma trận R01–R23](testing/phase-5/MA_TRAN_R01_R23.md). Soak30phút600chu kỳ/20reconnect đạt; STOP500 max6,6546ms và cadence20/30 đạt, cadence50 vẫn626/1283 mẫu>50ms/max58,4052ms. Đợt08–15 không thay mã sản phẩm/SDK; khi đó chưa đủ điều kiện đề nghị G5 đạt. G3/G4 chưa được duyệt; I11/API36/điện thoại/HTTPS/khóa phân phối và hệ thống thật vẫn có phần chưa kiểm chứng.

Sau đó người dùng duyệt ngoại lệ G5AVD cho cấu hình50ms có jitter+10ms,STOP vẫn≤10ms. Bản mới sửa cache snapshot/giảm cấp phát poll rỗng và bỏ CPU sampler chẩn đoán mặc định;110unit,6Android (4tích hợp+2thu timing) và8fault đạt phần chức năng. Cohort21 cadence50 max54,4796ms đạt cận60 nhưng STOP2/500>10ms,max11,4174ms;cohort Normal20 còn cadence50>60ms. Sau khi nhận kết quả, người dùng chốt **G5 CÓ ĐIỀU KIỆN cho chức năng AVD**, giữ STOP/cadence là lỗi mở, chặn dùng xe và release. Ngày18/10 giữ nguyên nguồn/APK, chạy25/25AndroidUI/Settings/updater+soak/probe; soak33 riêng trênbinarycuối đạt30phút0,668giây/600chu kỳ/20reconnect,audit60.557framekhôngrevival. Trace chẩn đoán không tái hiện vi phạm và không đóngFAILcũ; không tự duyệtG3/G4 hoặc chuyểnPhase6/7. Xem [quyết định và kết quả bổ sung](testing/phase-5/KET_QUA_BO_SUNG_G5.md).

### 3.5. Giao diện compact và Chẩn đoán ngày18/10/2026

Theo mockup v3 người dùng duyệt, đã áp dụng bố cục gọn hơn, Kill/chế độ trên thanh đầu, cảnh báo giữ vị trí cố định, ping/tuổi ACK và App PWM/ACK PWM tách riêng. Có4giao diện: Điều khiển (D-Pad/joystick), Chẩn đoán, modal Cấu hình (3tab) và modal Cập nhật. Mở công cụ dừng/đóng phiên; quay lại không tự phát lại chuyển động.

Chẩn đoán dùng dữ liệu thật, nhóm App/Mạng/Giao thức/IoT, bộ đếm TX/RX/bị loại, lý do ACK/rejection, clipboard, xuất file bằng Android và lưu phiên lỗi trước. Ghi log qua trace worker, đọc summary immutable; UI cập nhật250ms, ring240dòng và giới hạn báo cáo. Giữ Chưa xác định khi thiếu dữ liệu; ACK không chứng minh trạng thái cơ học. Các thông số lái/kết nối vẫn chỉnh và lưu được không build lại.

118unit,33case Android chức năng phân biệt và8kịch bản fault có bằng chứng trên APK mới;33case được đối chiếu từ đợt35và5case kiểm lại36, không cộng trùng. Một batch STOP500/cadence mới đạt cận đã duyệt nhưng không đóng FAIL lịch sử hoặc thay G5 có điều kiện. APK G5 cũ và bằng chứng soak cũ giữ nguyên; chưa thử máy thật/release. [13ảnh thật](screenshots/ui-compact-2026-10-18/README.md) · [Báo cáo/binary/giới hạn](testing/ui-compact-2026-10-18/KET_QUA.md).

### 3.6. Kiểm tra cài thử điện thoại ngày18/10/2026

Đã sửa lỗi cắt chữ/bố cục ở800×360dp/font150% trong Controller/Chẩn đoán/updater và lỗi nhả nút mở khóa sau khi gửiU nhưng trướcACK làm Kill bật lại. Regression phiên red→green;66/66unit liên quan đạt,smokeUDPthật xác nhận nhả trướcACK vẫn mở khóa trung tính. APK debug cục bộ cuối SHA256 `b31ac740afa3515c8cc5a2a344061e170c63a5972ca4beef2058c3ac7c5c736f`,min36/target37,version1.0/code1; kiểm chữ ký/libABIthực và cài đèAVDgiữdata,khôngđổikhóa/minSdk/SDK.

Kết luận **CÓ ĐIỀU KIỆN cài thử UI/cấu hình/nhật ký trên điện thoại, không nối xe**. Fullunit thực chạy117/118còn1FAILsampling;Androidcuối40+41có34/36ca chức năng phân biệt đạt,2FAILđược giữ,8/8faultmớiđạt. Chỉ1batchtimingtrênbinarytrunggian:STOP6/500>10ms,max25,3868msFAIL;cadenceđạtcậnđãduyệt. Chỉ1lượtthửsoak bịtestchạmsớm trướcACK,khônghoàntất30phút/khôngchạylượt2. API36/điệnthoạiARM/UDPLAN/HTTPS A→B→C/khóaphânphốiổnđịnh/firmwarevẫnchưakiểmchứng. G5cóđiềukiện/G3G4chờduyệt/chặnxe-releasegiữnguyên. [Kết luận,APK/hướng dẫn/evidence](testing/phone-readiness/KET_LUAN.md) · [16ảnhthậtAPKcuối](testing/phone-readiness/screenshots/INDEX.md).

## 4. Người sử dụng và quy trình chính

Người sử dụng chính là người lái robot. Người cấu hình hoặc kiểm thử cần thiết lập kết nối, kiểm tra trạng thái xe, chạy kịch bản và đọc kết quả.

Quy trình sử dụng dự kiến:

1. Mở app theo chiều ngang, toàn màn hình.
2. Nhập cấu hình kết nối và chọn chế độ điều khiển phù hợp.
3. Kết nối, nhận phản hồi từ robot hoặc robot mô phỏng.
4. Chỉnh giới hạn tốc độ từ 30% đến 100%.
5. Giữ hoặc kéo vùng điều hướng bằng ngón trái; phối hợp các nút bên phải bằng ngón khác.
6. Nhả vùng điều hướng để phát STOP; dùng Brake để hãm chủ động hoặc Kill để khóa điều khiển.
7. Khi mất kết nối, quan sát trạng thái Offline; kết nối lại không tự khôi phục thao tác đang giữ trước đó.
8. Chỉ tiếp tục di chuyển khi người dùng thực hiện thao tác mới.

Bước 7–8 là đề xuất an toàn cần duyệt trong Phase 0.

## 5. Phạm vi chức năng theo đặc tả

| Mã | Yêu cầu | Kết quả cần kiểm tra |
|---|---|---|
| R01 | Android, landscape và fullscreen | Bố cục hai ngón cái; vùng trái khoảng 50%, phải 40%, thanh công cụ 10% theo mô tả |
| R02 | D-Pad | Tiến, lùi, xoay trái/phải tại chỗ; lựa chọn 4/8 hướng cần chốt |
| R03 | Joystick | Chuyển X/Y thành PWM trái/phải trong `-255…255` |
| R04 | STOP khi nhả điều hướng | App phát lệnh dừng trong vòng 10 ms |
| R05 | Boost/Dash | Giữ để vượt limiter lên mức tối đa theo hướng đang điều khiển; nhả để bỏ Boost |
| R06 | Cơ cấu xúc/nâng | Có chế độ giữ/nhả và chế độ toggle |
| R07 | Xoay nhanh 180 độ | Nhấn một lần; thời gian xoay cấu hình được và cần hiệu chỉnh trên xe |
| R08 | Brake/Lock | Giữ để hãm chủ động; nhả để bỏ hãm |
| R09 | Speed limiter | Khoảng 30–100%, áp dụng cho di chuyển bình thường |
| R10 | MANUAL/AUTO | Chuyển chế độ nếu có phần thi tự hành; phạm vi AUTO phải được xác nhận |
| R11 | Kill switch | Ngắt điều khiển và dừng động cơ; quy tắc mở khóa cần chốt |
| R12 | Kết nối và trạng thái | Online/Offline, ping, Reconnect dựa trên phản hồi hợp lệ |
| R13 | Bản tin điều khiển | Chuỗi ngắn, kết thúc newline, tương thích hợp đồng firmware |
| R14 | Gửi định kỳ | 20–50 ms; tài liệu khuyến nghị gói tổng hợp mỗi 30 ms |
| R15 | Heartbeat/watchdog | Firmware dừng động cơ nếu không nhận gói tin trong 300 ms |
| R16 | Multi-touch | Nhả riêng Boost/vũ khí không làm mất hướng của ngón khác; nhả pointer điều hướng STOP và vô hiệu điều hướng cũ theo G0, đã kiểm chứng reducer và UI/sender trên simulator; giới hạn hiệu năng xem G4 |
| R17 | Haptic | Rung nhẹ khi bấm tấn công và đổi trạng thái kết nối |
| R18 | Settings | Đổi IP, port, chuỗi lệnh, thời gian xoay mà không build lại |
| R19 | Độ trễ phản hồi | Dưới 30 ms; điểm bắt đầu/kết thúc phép đo phải được duyệt |
| R20 | Xử lý gián đoạn | Đề xuất bổ sung: cancel, chuyển nền, reconnect, gói lỗi/cũ/trùng phải về trạng thái an toàn |
| R21 | Test trước khi bàn giao chính thức | Giả lập là môi trường đầu tiên; cho phép phân phối APK test để sửa lỗi liên tục; bản dùng thi đấu chỉ sau nghiệm thu |
| R22 | Tự chỉnh thông số vận hành | Chỉnh và lưu tốc độ, tham số lái/xoay/joystick, kết nối và tham số cơ cấu được firmware hỗ trợ, không build lại APK |
| R23 | Cập nhật APK test không cần USB | Ngay trong app kiểm tra bản mới, tải và chuyển sang xác nhận cài đặt Android; giữ cấu hình và lặp được nhiều phiên bản |

Cụm “theo hướng đang điều khiển” trong R05 đã được làm rõ tại G0 để tránh Boost tự gây chuyển động khi không có hướng lái. R20 là yêu cầu bổ sung phục vụ an toàn đã duyệt cho bản thử, không phải trích nguyên văn đặc tả hoặc xác nhận firmware.

## 6. Giao diện và thao tác

| Vùng | Thành phần | Hành vi |
|---|---|---|
| Trái | D-Pad và joystick | Dùng độc lập theo chế độ đang chọn; nhả hoặc hủy điều hướng thì dừng |
| Phải | Boost đỏ, cơ cấu cam, xoay nhanh vàng, Brake xanh dương | Giữ/nhả hoặc nhấn một lần theo loại nút; Boost lớn nhất |
| Công cụ | Online/Offline, ping, Reconnect, limiter, MANUAL/AUTO, Kill | Kill luôn dễ tiếp cận; trạng thái phải nhận biết được cả bằng chữ và màu |
| Hộp cấu hình | Tốc độ, joystick, thời gian xoay, IP/port, mã lệnh và hành vi cơ cấu | Chỉnh/lưu trực tiếp, kiểm tra hợp lệ, không build lại khi chỉ đổi tham số |
| Cập nhật ứng dụng | Phiên bản đang cài, bản mới, ghi chú thay đổi, Kiểm tra/Tải/Cài đặt | Cài đè APK test trực tiếp trên điện thoại qua trình cài Android, không cần USB |

Ví dụ đa điểm chạm bắt buộc: giữ Tiến bằng ngón 0, giữ Boost bằng ngón 1, nhả ngón 1 thì Tiến vẫn còn. Khi nhả ngón 0, PWM di chuyển về 0 kể cả ngón Boost còn giữ.

**ĐÃ DUYỆT G0 — nhả nhiều pointer điều hướng:** Nhả hoặc mất vùng chạm của bất kỳ pointer điều hướng hợp lệ nào lập tức yêu cầu PWM hai bánh 0 và Boost hiệu lực 0; vô hiệu mọi pointer điều hướng cũ còn giữ. Tick/move của chúng không làm xe chạy lại, kể cả sau khi kéo trở vào vùng chạm. Với pointer đã ra ngoài vùng chạm, vẫn phải chờ ngón đó nhấc thực sự. Chỉ nhận down điều hướng mới sau khi toàn bộ pointer điều hướng cũ đã nhấc; việc nhấc hết chỉ cho phép chạm mới, không tự phục hồi chuyển động ở tick sau. Chạm đến trước mốc nhấc hết bị bỏ qua, không tự có hiệu lực chỉ vì tiếp tục giữ/move. L17–L22 đã kiểm chứng reducer ở G1 và nối UI/sender ở Phase 3+4; log pointer/receiver xem hồ sơ G3/G4.

Nhả riêng Boost vẫn giữ hướng hợp lệ của ngón điều hướng khác. Nhả điều hướng không xóa pointer vũ khí HOLD hoặc latch TOGGLE; W giữ theo quy tắc vũ khí đã đề xuất. Không yêu cầu nhấc Boost/vũ khí để đủ điều kiện chạm điều hướng mới; trong khi chờ hướng mới, Boost hiệu lực vẫn 0. Kill, Brake, reconnect và cancel toàn cục giữ quy tắc trong biên bản. Quy tắc bổ sung này làm rõ R04/R16/R20, đã được G0 duyệt và đã triển khai; không suy từ kết quả chức năng rằng STOP luôn đạt10ms. [Biên bản Phase 0 mục 5.2.1–5.2.2](testing/phase-0/BIEN_BAN_DUYET.md) có bảng chuỗi sự kiện/đầu ra ngay và các tick tiếp theo; kết quả reducer L17–L22 đã chạy nằm trong hồ sơ G1, phần tích hợp trong G3/G4. Bảng kích thích trong kế hoạch không tự là kết quả test.

STOP, Brake và Kill là ba hành vi riêng. STOP là yêu cầu tốc độ bằng 0; Brake phụ thuộc khả năng hãm của driver; Kill khóa việc phát sinh chuyển động. Không đồng nhất ba nút thành cùng một thao tác hiển thị.

Cơ chế khóa sau Kill, trạng thái cơ cấu khi dừng và thứ tự ưu tiên giữa các thao tác là thiết kế đề xuất cần duyệt.

### 6.1. Thông số người dùng tự chỉnh

| Nhóm | Giá trị phải chỉnh được | Cách áp dụng |
|---|---|---|
| Tốc độ | Limiter 30–100%, mức tốc độ quay/lái trong miền được duyệt | Limiter trên màn hình lái áp dụng ngay; thay đổi hiệu chỉnh nâng cao chỉ khi đã dừng |
| Joystick | Độ nhạy, vùng chết quanh tâm, hiệu chỉnh cân bằng hai bánh trong phạm vi phần cứng cho phép | Chỉnh và lưu trên app; giới hạn/giá trị mặc định được chốt ở G0 |
| Xoay nhanh | Thời gian xoay; giá trị mặc định thử là 200 ms, không coi là hiệu chỉnh góc thật | Lưu trên app và truyền đúng tham số nếu firmware thực thi macro |
| Cơ cấu | Hold/toggle; các vị trí hoặc mức tác động nếu firmware có hỗ trợ cấu hình | Chỉ hiển thị tham số có hợp đồng với mạch; áp dụng sau phản hồi xác nhận |
| Kết nối | IP, port và chuỗi lệnh theo hợp đồng | Dừng phiên cũ, kiểm tra dữ liệu, kết nối lại an toàn |
| Tần suất gửi | Khoảng gửi 20–50 ms, mặc định 30 ms | Điều chỉnh lịch gửi hiện tại, không tạo thêm sender hoặc thay đổi đường STOP tức thời |
| Cập nhật app | URL HTTPS của kênh cập nhật do chủ dự án quản lý | Cấu hình riêng với IP xe, có xác nhận khi đổi nguồn và luôn kiểm tra APK |

Danh mục này là yêu cầu bổ sung từ người dùng; miền hợp lệ chi tiết được duyệt ở Phase 0. Không để các thông số vận hành này chỉ tồn tại như hằng số trong mã nguồn hoặc cần Android Studio để chỉnh.

Ứng dụng phải cho biết giá trị đang có hiệu lực, lưu sau khi mở lại và có thao tác khôi phục mặc định có xác nhận. Hủy sửa hoặc dữ liệu không hợp lệ giữ nguyên cấu hình cũ. Không khôi phục thao tác chạy/Boost/macro khi nạp cấu hình.

Các ràng buộc nghiệm thu như PWM trong ±255, STOP trong 10 ms và watchdog firmware 300 ms không phải lựa chọn để người dùng tắt cơ chế an toàn. Tham số cần mạch thực thi chỉ được báo đã áp dụng khi có phản hồi phù hợp; thay đổi trên app không tự sửa firmware.

### 6.2. Vòng lặp cập nhật và test trực tiếp trên điện thoại

1. Cài bản test đầu tiên bằng liên kết HTTPS mở trên điện thoại; không cần cáp USB.
2. Người phát triển sửa mã nguồn, chạy các test phù hợp và build APK test với `versionCode` lớn hơn bản trước.
3. Ký bằng cùng khóa của chuỗi cập nhật. Kiểm tra bản APK khởi động được và các ca an toàn đã có; liệt kê lỗi chưa sửa.
4. Tải APK mang tên/đường dẫn theo phiên bản lên nơi lưu trữ; kiểm tra tải đủ và đúng checksum.
5. Sau đó mới cập nhật metadata bản mới trên kênh test. Không yêu cầu người dùng cài lại từ Android Studio.
6. Trên điện thoại vào Cập nhật ứng dụng → Kiểm tra bản mới → xem thay đổi → Tải và cài đặt.
7. App yêu cầu dừng điều khiển trước khi tải/cài, kiểm tra tệp và mở trình cài Android. Người dùng cấp quyền cài từ nguồn này và xác nhận cài khi Android yêu cầu.
8. Mở lại app: thấy phiên bản mới, giữ thông số đã lưu, điều khiển ở trạng thái dừng an toàn.
9. Nếu còn lỗi, ghi lại phiên bản và thao tác tái hiện; sửa mã nguồn, tăng versionCode và lặp lại bước 2–8.

Build/ký vẫn diễn ra ở máy phát triển hoặc môi trường build; điện thoại tải và cài APK đã tạo. Đổi tốc độ/tham số trong app không cần một vòng build/upload; đổi logic hoặc thêm chức năng thì cần APK mới.

Luồng đề xuất dùng HTTPS cho metadata/APK và `PackageInstaller` của Android cho cài đặt có xác nhận. Có nút kiểm tra bản mới ngay trong app; không yêu cầu Google Play, máy chủ điều khiển xe hoặc công cụ vá mã chạy từ xa.

Trước bản test đầu tiên phải chốt `applicationId` và khóa ký dùng ổn định cho chuỗi cập nhật, bao gồm bản bàn giao sau này. Mỗi lần đưa bản mới lên phải tăng `versionCode`, hiển thị `versionName` và ghi chú thay đổi. Không đổi package/khóa ở cuối dự án làm người dùng buộc gỡ app và mất dữ liệu.

Kiểm tra HTTPS, checksum SHA-256, applicationId, chữ ký APK, versionCode và mức Android hỗ trợ trước cài. Khi tải/cài bị hủy hoặc lỗi, giữ app và cấu hình cũ, cho thử lại. Nâng schema cấu hình phải có migration giữ giá trị người dùng, không xóa toàn bộ rồi khôi phục mặc định.

Kênh test có thể phân phối bản còn lỗi không nghiêm trọng để người dùng thử tiếp; không được ghi là đã nghiệm thu hoặc cho vận hành xe khi còn lỗi an toàn. Kênh bàn giao dùng thi đấu vẫn chịu G6/G7.

Khi điện thoại đang nối Wi-Fi xe không có Internet, cần dừng điều khiển và dùng mạng có thể tải bản cập nhật. Chức năng điều khiển xe không phụ thuộc việc máy chủ cập nhật đang truy cập được.

## 7. Kết nối và hợp đồng dữ liệu

Tài liệu cho phép Wi-Fi UDP hoặc WebSocket; Bluetooth Classic SPP hoặc BLE GATT Write Without Response. Không mặc định phải triển khai toàn bộ. Phương án kiểm thử được đề xuất là UDP với robot mô phỏng trên máy tính; giao thức xe thật vẫn cần xác nhận.

Các bản tin gốc:

| Chức năng | Bản tin |
|---|---|
| Dừng | `M:0,0\n` |
| Tiến/lùi | `M:200,200\n` / `M:-200,-200\n` |
| Xoay tại chỗ | `M:-180,180\n` / `M:180,-180\n` |
| Boost | `B:1\n` / `B:0\n` |
| Cơ cấu | `W:1\n` / `W:0\n` |
| Phanh | `K:1\n` / `K:0\n` |
| Xoay nhanh | `T:180\n` |
| Đồng bộ | `S:L,R,B,W,K\n` |

`\n` ở bảng là ký tự xuống dòng kết thúc bản tin. PWM mẫu 200/180, IP `192.168.4.1`, port `8888` và thời gian xoay 200 ms là ví dụ.

Gói `S` là phương án chính được khuyến nghị trong tài liệu, nhưng chưa mang đầy đủ Kill, MANUAL/AUTO, thời gian xoay, mã thao tác một lần hoặc số thứ tự. Cũng chưa có định dạng phản hồi/ping.

Trước khi viết codec và tích hợp, Phase 0 phải xác định chính xác:

- Bản tin dữ liệu chính và các lệnh điều khiển bổ sung, bao gồm phản hồi của chúng.
- Cách giữ hoặc mở rộng định dạng gốc; mọi mở rộng phải được ghi rõ là hợp đồng mới.
- Cách loại gói cũ/trùng và phân biệt phiên kết nối; không giả định UDP đảm bảo thứ tự/giao nhận.
- Cách thực hiện macro một lần khi trạng thái được gửi lặp mỗi 30 ms.
- Timeout Online/Offline và điều kiện chuyển sang trạng thái an toàn.
- Kiểm tra số trường, miền giá trị, newline, độ dài và nguồn phản hồi; dữ liệu lỗi không được duy trì chuyển động hoặc làm mới watchdog.
- Các lệnh cấu hình được phép; không biến ô nhập chuỗi thành bộ thực thi mã tùy ý.

Robot mô phỏng kiểm tra hợp đồng này. Nó không phải firmware thật và không chứng minh driver hoặc động cơ đã thực hiện đúng.

## 8. Phân công app, simulator và firmware

| Thành phần | Trách nhiệm |
|---|---|
| App Android | Đọc thao tác, điều phối trạng thái, gửi lệnh, hiển thị phản hồi, lưu cấu hình, xử lý lifecycle và quyền |
| Robot mô phỏng | Nhận và phân tích bản tin độc lập, phản hồi, ghi trạng thái áp dụng, mô phỏng watchdog và lỗi mạng |
| Firmware xe thật | Thực thi motor/cơ cấu, phanh đúng driver, watchdog độc lập, macro/tự hành, xác nhận trạng thái |
| Người kiểm thử | Đối chiếu ca test, thu bằng chứng, ghi giới hạn môi trường và đề nghị duyệt |
| Người dùng/chủ dự án | Duyệt phạm vi, hành vi, đầu ra từng phase và quyền đóng gói bàn giao |

Thuật toán tự hành, lựa chọn linh kiện và chế tạo cơ khí chưa được đặc tả. Chỉ chuyển chế độ AUTO trên app không đồng nghĩa đã hoàn thành chức năng tự hành.

## 9. Chiến lược kiểm thử

1. Unit test JVM: phép trộn bánh, limiter, Boost, reducer đa ngón, Kill, codec và lịch gửi bằng đồng hồ kiểm soát được.
2. Android Emulator: giao diện thật, inject nhiều pointer bằng instrumentation, lifecycle, cấu hình và quyền mạng.
3. Kết nối simulator: kiểm tra dữ liệu thực sự ra khỏi app, phản hồi, mất gói, gói trùng/cũ và timeout.
4. Điện thoại và xe thật: xác minh phiên bản Android mục tiêu, kết nối thực tế, độ trễ, phanh, cơ cấu, xoay và watchdog của firmware.
5. Chạy lại bài hồi quy trên bản release sau khi được duyệt đóng gói.
6. Kiểm tra cấu hình không cần build và ít nhất hai lần cập nhật liên tiếp A → B → C từ app; giữ dữ liệu, kiểm tra lỗi tải/cài và xác nhận không cần USB.

Giả lập xác nhận hành vi phần mềm trong môi trường giả lập. Kết quả mô phỏng watchdog không chứng minh MCU thật tự dừng; thời gian ACK khứ hồi không phải độ trễ một chiều từ chạm tới động cơ. Rung trên emulator cần quan sát lời gọi/sự kiện; cảm giác rung phải kiểm tra trên điện thoại.

Ba mức kết luận:

| Mức | Ý nghĩa |
|---|---|
| Đạt logic | Unit test đạt, chưa chứng minh UI/mạng trên Android |
| Đạt kiểm thử trên giả lập | UI và kịch bản simulator đạt; yêu cầu vật lý vẫn chưa kiểm chứng |
| Đạt nghiệm thu hệ thống | Yêu cầu bắt buộc được đối chiếu với bằng chứng đúng môi trường; đủ điều kiện đề nghị đóng gói |

## 10. Kiến trúc dự kiến

Giữ project Android một module. Lõi trạng thái và codec viết bằng Kotlin có thể test trên JVM. Compose đọc trạng thái và phát sự kiện; một bộ điều phối duy nhất quản lý gửi định kỳ và xử lý STOP/Kill để tránh hai nguồn phát lệnh mâu thuẫn.

Tách một ranh giới nhỏ giữa điều phối và kết nối để dùng fake trong unit test và kết nối thật trong test tích hợp. Tận dụng Compose, JUnit và Android SDK hiện có; chưa có nhu cầu thêm backend cloud, cơ sở dữ liệu máy chủ, tài khoản, DI framework hoặc thư viện joystick.

Lưu cấu hình nhỏ bằng tính năng nền tảng Android, có phiên bản schema và migration. Thêm màn hình cập nhật, tải metadata/APK qua HTTPS và tích hợp PackageInstaller bằng API nền tảng; ưu tiên thư viện đã có. Nơi phân phối chỉ cần lưu tệp APK và metadata, chưa có nhu cầu backend riêng. Công cụ simulator trên máy tính phục vụ kiểm thử, không được đóng gói kèm bản release Android.

Đây là kiến trúc đề xuất cho Phase 0 duyệt, chưa mô tả chức năng đang tồn tại trong mã nguồn.

## 11. Phạm vi chưa được chốt

- Mạch điều khiển, driver, cơ cấu phụ, firmware và kết nối xe thật.
- Có vòng AUTO hay không; hành vi khi mất điều khiển trong AUTO.
- Có yêu cầu 8 hướng/nút cua mở rộng hay chỉ D-Pad cơ bản.
- Cơ chế nhả an toàn của cơ cấu, ưu tiên Brake, cách thoát Kill và kết thúc macro.
- Định dạng mở rộng giao thức và bản tin trả về.
- Cách đo và điều kiện đánh giá các mốc 10 ms, 30 ms, 300 ms.
- Điện thoại thi đấu, phiên bản Android chính xác, hạn cần chạy thử và thể lệ giải.
- applicationId, chủ sở hữu/backup khóa ký và URL HTTPS kênh test phải chốt trước bản test đầu tiên trên điện thoại; tên hiển thị cuối cùng có thể hoàn thiện sau.
- Danh mục/miền tham số hiệu chỉnh và lệnh cấu hình firmware có phản hồi xác nhận.

Các mục này có đầu việc và điều kiện giải quyết trong Phase 0 hoặc Phase 6 của kế hoạch. Không tự bỏ yêu cầu bắt buộc chỉ vì thiếu phần cứng; khi chưa đủ bằng chứng thì ghi CHƯA KIỂM CHỨNG.

Thông tin hosting hoặc firmware chưa có phải được thu thập ở phase cần dùng; không tự tạo URL đang hoạt động, tự đăng ký tài khoản hoặc khẳng định đã có kênh cập nhật.

## 12. Điều kiện đóng gói bàn giao

- Các phase bắt buộc đã có kết quả test và được duyệt.
- Không còn lỗi gây chạy ngoài ý muốn, mất STOP, mất Kill hoặc sai watchdog.
- Mọi yêu cầu đã có bằng chứng hoặc được xác định rõ là không thuộc phạm vi bằng một thay đổi được người dùng duyệt.
- Đã phân biệt kết quả trên giả lập và xe thật; không ghi đạt toàn bộ khi các mốc vật lý chưa được kiểm tra.
- Người dùng duyệt Gate G6 cho phép tạo bản release.
- Bản release được smoke test và ghi nhận checksum, version, cấu hình cùng hạn chế đã biết trước khi bàn giao.
- Đã kiểm chứng giữ cấu hình khi nâng cấp và vòng lặp cập nhật không USB; bản bàn giao tiếp nối applicationId/khóa ký đã chốt.

Nếu chỉ hoàn thành test trên emulator, trạng thái bàn giao giai đoạn này là ĐẠT KIỂM THỬ TRÊN GIẢ LẬP; Gate đóng gói bản dùng thi đấu vẫn chưa được duyệt.

Điều kiện này áp dụng cho bản bàn giao dùng thi đấu. Không chặn việc tạo, ký và tải APK test lên để tiếp tục sửa lỗi trước G6 theo yêu cầu cập nhật mới.

## 13. Tài liệu tham khảo đã đối chiếu

Các trang Android dưới đây đã được đọc ngày 16/10/2026; dùng để thiết kế bài kiểm tra nền tảng, không thay thế đặc tả sản phẩm.

- [Android Studio — Test your app](https://developer.android.com/studio/test): unit test, instrumentation và công cụ kiểm thử.
- [Android Emulator networking](https://developer.android.com/studio/run/emulator-networking): các khả năng mô phỏng mạng.
- [Local network permission](https://developer.android.com/privacy-and-security/local-network-permission?hl=en): quyền truy cập mạng nội bộ, gồm yêu cầu cho target SDK 37 trên Android 17.
- [How app updates work](https://developer.android.com/google/play/app-updates): applicationId, chữ ký và versionCode khi cập nhật.
- [Sign your app](https://developer.android.com/studio/publish/app-signing): quản lý chữ ký cho chuỗi APK.
- [Alternative distribution](https://developer.android.com/distribute/marketing-tools/alternative-distribution): phân phối APK qua nơi lưu trữ của người phát triển.
- [PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller): phiên cài và yêu cầu người dùng xác nhận.
- [PackageManager.canRequestPackageInstalls](https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls()): kiểm tra quyền yêu cầu cài ứng dụng.

