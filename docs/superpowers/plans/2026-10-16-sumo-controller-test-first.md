# Kế hoạch phát triển và kiểm thử ứng dụng điều khiển robot Sumo

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking. Thực hiện trực tiếp; chỉ dùng subagent khi người dùng yêu cầu. Việc tạo tài liệu này không phải phê duyệt triển khai hoặc phê duyệt các gate.

**Goal:** Hoàn thiện ứng dụng có thông số chỉnh ngay trên app, kiểm thử bằng JVM/emulator/simulator và cập nhật APK test trực tiếp trên điện thoại không USB; chỉ đóng gói bàn giao dùng thi đấu sau nghiệm thu.  
**Architecture:** Giữ một module Android. Lõi Kotlin quản lý trạng thái và codec độc lập với Compose; một bộ điều phối tuần tự quản lý sự kiện và lịch gửi. Robot mô phỏng nhận dữ liệu độc lập để đối chiếu đầu ra của app.  
**Tech Stack:** Kotlin, Jetpack Compose, Material 3, JUnit 4, Android instrumentation/Compose Test, Android Emulator, Java DatagramSocket nếu chọn UDP; Python 3.13 thư viện chuẩn cho simulator trên máy tính.  
**Ngày lập:** 16/10/2026, Asia/Bangkok.  
**Trạng thái cập nhật18/10/2026:** G0/G1/G2 đã duyệt (G1/G2 theo hồ sơ cuối60/60); G3/G4 CHỜ DUYỆT CHUNG. Người dùng đã chốtG5CÓ ĐIỀU KIỆN cho chức năngAVD; STOP/cadence còn lỗi mở, chặn dùngxe/release. Đợt18/10 tiếp tục kiểm chứng phần mềm độc lập đếnPhase5, không chuyểnPhase6/7 hoặc upload. Không dùng baseline mẫu hoặc ca dự kiến làm bằng chứng; kết quả tại checkpoint/hồ sơphase.

Mô tả dự án và danh sách yêu cầu R01–R23: [MO_TA_DU_AN.md](../../MO_TA_DU_AN.md).

**Điều chỉnh yêu cầu ngày 16/10/2026:** người dùng cho phép vòng lặp sửa mã nguồn → chạy test → build/ký/upload APK test → cập nhật trong app trên điện thoại → test tiếp. Quyền phân phối APK test không phụ thuộc G6; G6 vẫn chặn bản bàn giao dùng thi đấu. Nội dung này thay thế cách hiểu trước đó rằng mọi APK đều phải chờ nghiệm thu cuối.

**Ủy quyền hiện hành18/10/2026:** tiếp tục sửa chữa/kiểm chứng độc lập trong phạm vi đếnPhase5 theo yêu cầu người dùng, giữ quyết địnhG5có điều kiện vàG3/G4CHỜ DUYỆT CHUNG. Không tự chuyểnPhase6/7, dùngxe,upload hoặc release. CLIAVD,D-Pad4/joystick/UDPsimulator,AUTOkhóa và các ngưỡng đã duyệt giữ nguyên. Các ủy quyềnPhase1+2/3+4 bên dưới là lịch sử, không phải quyền mới. Checkpoint tại `docs/testing/TIEN_DO_AI.md`; không viết lại các bước kế hoạch.

## 1. Ràng buộc chung

- Kiểm thử trên Android Emulator của Android Studio là lựa chọn người dùng đã xác nhận.
- Cho phép debug/instrumentation trên emulator và APK test ký ổn định để cài/cập nhật qua Internet trên điện thoại, không cần USB.
- Được build, ký và đưa APK test lên kênh thử để sửa lỗi trước G6 theo checklist mục 18. Không tạo bản release dùng thi đấu trước G6.
- applicationId và khóa ký cho chuỗi cập nhật phải chốt trước bản test đầu tiên trên điện thoại, không đợi tới Phase 7. Mỗi APK được tải lên có versionCode tăng; không dùng khóa debug tự sinh khác nhau theo máy.
- minSdk 36, targetSdk 37, compileSdk 37 theo project hiện tại; chưa tự hạ/nâng phiên bản.
- Giữ AGP 9.3.3, Gradle 9.5.0, Kotlin Compose plugin 2.2.10 và các dependency hiện có nếu chưa có lý do được kiểm chứng để đổi.
- PWM nằm trong `-255…255`; limiter 30–100%; Boost tối đa theo quy tắc hướng lái được duyệt.
- Chu kỳ định kỳ mặc định 30 ms, chỉnh ngay trong app trong khoảng 20–50 ms. Sự kiện STOP/Kill không chờ chu kỳ và không bị thay đổi bởi thông số này.
- App phát STOP trong vòng 10 ms từ mốc bắt đầu đo đã duyệt. Mục tiêu phản hồi hệ thống dưới 30 ms phải được đo đúng phạm vi.
- Firmware tự dừng khi không nhận bản tin hợp lệ trong 300 ms. Dùng “hợp lệ” thay vì bất kỳ byte nhận được là đề xuất an toàn cần duyệt.
- Không dùng HTTP GET/POST cho nút di chuyển.
- Không coi PASS unit test, PASS simulator hoặc PASS emulator là PASS phần cứng thật.
- Không bỏ joystick, một chế độ cơ cấu, Kill hoặc fail-safe để làm demo nhanh. AUTO/8 hướng chỉ được xác định không áp dụng khi có quyết định phạm vi rõ ràng.
- Chức năng mới có logic phải có test lỗi trước, triển khai tối thiểu rồi chạy lại test. Không thêm dependency cho vài dòng có thể giải quyết bằng nền tảng.
- Không thêm cloud cho điều khiển xe, đăng nhập, backend nghiệp vụ, cơ sở dữ liệu máy chủ, CI/CD hoặc đa module. Kênh cập nhật chỉ cần hosting HTTPS cho APK/metadata, không phải backend điều khiển.
- Chỉ lưu cấu hình; không lưu để khôi phục trạng thái đang chạy, pointer đang giữ, Boost hoặc macro qua lần mở app.
- Thông số vận hành ở mục 6.1 của mô tả dự án phải chỉnh/lưu trong app; không buộc build lại để đổi giá trị. Miền an toàn được kiểm tra và hiển thị rõ.
- Cập nhật APK phải giữ cấu hình bằng cài đè/migration; tải hoặc cài lỗi không được gỡ app hay xóa dữ liệu cũ.
- Cập nhật dùng trình cài Android có xác nhận người dùng; không hứa cài im lặng hoặc vá mã nguồn trực tiếp trên điện thoại.
- Các ngưỡng đo mẫu, thứ tự ưu tiên và hành vi bổ sung ghi “đề xuất” phải được duyệt trong G0.

## 2. Cách đọc trạng thái và duyệt phase

| Trạng thái | Ý nghĩa |
|---|---|
| CHƯA BẮT ĐẦU | Chưa thực hiện đầu việc của phase |
| ĐANG LÀM | Đang triển khai/test, chưa đủ bằng chứng |
| CHỜ DUYỆT | Đủ hồ sơ để người dùng kiểm tra, chưa được đi tiếp |
| ĐẠT | Đủ bằng chứng và đã được người có quyền duyệt chấp thuận |
| KHÔNG ĐẠT | Có ca test sai hoặc thiếu điều kiện bắt buộc |
| CHƯA KIỂM CHỨNG | Chưa có môi trường/bằng chứng phù hợp; không được tính là ĐẠT |
| KHÔNG ÁP DỤNG | Có lý do và quyết định thay đổi phạm vi được người dùng duyệt |

Quy tắc chuyển phase:

1. Hoàn tất checklist, chạy đúng nhóm test và tự kiểm tra thay đổi.
2. Lưu log, kết quả đối chiếu và ảnh/video nếu có UI.
3. Gửi bản đang chạy hoặc bằng chứng cụ thể cho người dùng; ghi CHỜ DUYỆT.
4. Chỉ ghi ĐẠT và chuyển phase sau khi có phê duyệt. Không tự đánh dấu thay người dùng.
5. Nếu user đã duyệt rõ trong chat, ghi lại quyết định và thời điểm; không hỏi lại cùng một nội dung.
6. Khi sửa chức năng đã duyệt, chạy lại ca bị ảnh hưởng và ghi việc duyệt lại; không giữ kết quả cũ cho mã mới.
7. Thiếu xe thật không ngăn hoàn thành giai đoạn emulator, nhưng giữ các ca vật lý ở CHƯA KIỂM CHỨNG và chặn G6.

### Bảng gate ban đầu

| Phase | Gate | Đầu ra | Trạng thái hiện tại | Người duyệt |
|---|---|---|---|---|
| 0 | G0 | Phạm vi test, quy tắc an toàn, hợp đồng bản tin | ĐẠT — người dùng duyệt 17/10/2026 | Người dùng; người phụ trách firmware khi đã có |
| 1 | G1 | Lõi điều khiển và test trạng thái | ĐẠT — người dùng duyệt hồ sơ cuối 60/60 | Người dùng dựa trên kết quả test |
| 2 | G2 | Codec và robot mô phỏng | ĐẠT — người dùng duyệt cùng G1 | Người dùng/người kiểm thử |
| 3 | G3 | UI, chỉnh thông số và luồng cập nhật APK test | CHỜ DUYỆT CHUNG G3/G4; phần độc lập đã triển khai, cập nhật thật thiếu hosting/khóa/thiết bị | Người dùng trực tiếp thử thao tác/cập nhật |
| 4 | G4 | Kết nối và điều phối lệnh qua simulator | CHỜ DUYỆT CHUNG G3/G4; chức năng có bằng chứng, timing/I11 còn chưa đạt hoặc chưa kiểm chứng | Người dùng/người kiểm thử |
| 5 | G5 | Hồi quy và nghiệm thu giai đoạn giả lập | CÓ ĐIỀU KIỆN cho chức năngAVD theo quyết định17/10; STOP/cadence còn lỗi mở, chặn dùngxe/release | Người dùng |
| 6 | G6 | Tích hợp xe thật, nghiệm thu, quyền đóng gói | CHƯA BẮT ĐẦU | Người dùng; người phụ trách firmware |
| 7 | G7 | Bản release đã smoke test và bàn giao | CHƯA BẮT ĐẦU | Người dùng nhận bàn giao |

G5 có thể được duyệt với kết luận ĐẠT KIỂM THỬ TRÊN GIẢ LẬP và kết quả test điện thoại đã chạy. APK test có thể được đưa lên và cập nhật lặp từ Phase 3 theo mục 18, không đợi G5/G6 và không được tự ghi các gate đã đạt. Chỉ G6 mới cho phép đóng gói bản dùng thi đấu; G7 xác nhận bản bàn giao vẫn đạt.

Không đặt ngày hoàn thành hoặc số ngày công khi chưa biết hạn thi, phần cứng và người phối hợp. Phạm vi bổ sung cần được ghi thành thay đổi yêu cầu.

## 3. Tệp và ranh giới triển khai dự kiến

Các đường dẫn trong bảng tính từ thư mục gốc dự án. Tệp Phase 1+2 đã có, đối chiếu checksum/hồ sơ từng phase; tệp phase sau vẫn là dự kiến, chưa tồn tại chỉ vì được liệt kê.

| Tệp | Trách nhiệm | Phase |
|---|---|---|
| `app/src/main/java/com/aistudy/myapplication/MainActivity.kt` | Host Compose, lifecycle, giữ màn hình hoạt động khi điều khiển | 3 |
| `app/src/main/AndroidManifest.xml` | Landscape, khai báo quyền đúng kết nối | 3–4 |
| `app/src/main/java/com/aistudy/myapplication/control/ControlState.kt` | Trạng thái, enum chế độ/nút, giá trị PWM | 1 |
| `app/src/main/java/com/aistudy/myapplication/control/DriveMixer.kt` | Trộn X/Y, D-Pad, limiter và Boost | 1 |
| `app/src/main/java/com/aistudy/myapplication/control/ControlReducer.kt` | Pointer độc lập, thứ tự ưu tiên, hủy/khóa trạng thái | 1 |
| `app/src/main/java/com/aistudy/myapplication/control/ControlSession.kt` | Nối UI/transport, lịch gửi từ cấu hình (mặc định 30 ms), STOP/Kill và watchdog phía app | 4 |
| `app/src/main/java/com/aistudy/myapplication/protocol/CommandCodec.kt` | Encode/decode hợp đồng bản tin đã duyệt | 2 |
| `app/src/main/java/com/aistudy/myapplication/protocol/ReplyTracker.kt` | Xác thực nguồn/nonce/SID/seq/tuổi ACK và ý định, trạng thái Online/ping thuần Kotlin; chưa có socket | 2 |
| `app/src/main/java/com/aistudy/myapplication/connection/RobotTransport.kt` | Ranh giới gửi/nhận/đóng kết nối để test bằng fake | 4 |
| `app/src/main/java/com/aistudy/myapplication/connection/UdpRobotTransport.kt` | Adapter UDP nếu G0 chọn UDP cho bản thử | 4 |
| `app/src/main/java/com/aistudy/myapplication/settings/ControllerSettings.kt` | Model tham số và validation thuần Kotlin để test | 1–3 |
| `app/src/main/java/com/aistudy/myapplication/settings/ControllerSettingsStore.kt` | Lưu/đọc, schema và migration bằng API nền tảng | 3 |
| `app/src/main/java/com/aistudy/myapplication/ui/ControllerScreen.kt` | D-Pad, joystick, các nút và trạng thái | 3 |
| `app/src/main/java/com/aistudy/myapplication/ui/SettingsDialog.kt` | Nhập, báo lỗi, áp dụng cấu hình | 3 |
| `app/src/main/res/values/strings.xml` | Tên, nhãn, mô tả accessibility và lỗi cấu hình | 3 |
| `app/build.gradle.kts` | Chốt identity/signing/version cho test từ sớm; release sau G6 | 0/3/7 |
| `app/src/androidTest/java/com/aistudy/myapplication/ExampleInstrumentedTest.kt` | Assertion package theo identity đã chốt trước bản test đầu tiên | 0/3 |
| `app/src/main/java/com/aistudy/myapplication/updates/UpdateInfo.kt` | Metadata, so phiên bản, kiểm tra miền dữ liệu | 3 |
| `app/src/main/java/com/aistudy/myapplication/updates/AppUpdateManager.kt` | Check, tải/verify APK, điều phối PackageInstaller | 3 |
| `app/src/main/java/com/aistudy/myapplication/updates/InstallResultReceiver.kt` | Kết quả/pending user action của phiên cài | 3 |
| `app/src/main/java/com/aistudy/myapplication/ui/AppUpdateDialog.kt` | Phiên bản, ghi chú, tiến trình, thử lại và xác nhận cập nhật | 3 |
| `app/src/test/java/com/aistudy/myapplication/updates/UpdateInfoTest.kt` | So phiên bản, metadata và điều kiện chấp nhận APK | 3 |
| `app/src/androidTest/java/com/aistudy/myapplication/AppUpdateFlowTest.kt` | Metadata Android, quyền cài và nhánh lỗi trước/sau cài | 3–5 |
| `app/src/test/java/com/aistudy/myapplication/control/DriveMixerTest.kt` | Kiểm tra đầu ra bánh | 1 |
| `app/src/test/java/com/aistudy/myapplication/control/ControlReducerTest.kt` | Đa ngón, ưu tiên, hủy/khóa | 1 |
| `app/src/test/java/com/aistudy/myapplication/protocol/CommandCodecTest.kt` | Bản tin hợp lệ/lỗi và cấu hình lệnh | 2 |
| `app/src/test/java/com/aistudy/myapplication/settings/ControllerSettingsTest.kt` | Mặc định, miền giá trị và validation toàn cấu hình | 1 |
| `app/src/test/java/com/aistudy/myapplication/protocol/ReplyTrackerTest.kt` | Handshake, ACK/timeout/intent và nguồn/phiên/thứ tự hợp lệ | 2 |
| `app/src/test/java/com/aistudy/myapplication/protocol/SimulatorInteropTest.kt` | Datagram Kotlin đối chiếu ACK byte thật với tiến trình receiver Python độc lập | 2 |
| `tools/test_sumo_simulator.py` | Self-test engine/scenario có clock điều khiển được và lỗi CLI | 2 |
| `app/src/test/java/com/aistudy/myapplication/settings/ControllerSettingsTest.kt` | Validation cấu hình | 3 |
| `app/src/test/java/com/aistudy/myapplication/control/ControlSessionTest.kt` | Lịch gửi, sự kiện, mất kết nối và race | 4 |
| `app/src/test/java/com/aistudy/myapplication/connection/UdpRobotTransportTest.kt` | Socket loopback thật cho adapter UDP | 4 |
| `app/src/androidTest/java/com/aistudy/myapplication/ControllerScreenTest.kt` | Inject pointer trên UI thật | 3 |
| `app/src/androidTest/java/com/aistudy/myapplication/SettingsFlowTest.kt` | Nhập/lưu cấu hình và lifecycle | 3 |
| `app/src/androidTest/java/com/aistudy/myapplication/ControllerE2ETest.kt` | UI tới robot mô phỏng | 4–5 |
| `tools/sumo_simulator.py` | Receiver độc lập, phản hồi, watchdog, chế độ gây lỗi, self-test | 2–5 |

Không tạo trước toàn bộ các tệp rỗng. Chỉ tạo khi tác vụ cần dùng; fake và helper nhỏ đặt ngay trong test, không dựng framework test riêng.

Nếu G0 chọn SPP/BLE thay UDP, thay tác vụ adapter UDP bằng adapter đã được duyệt. Giữ bài test hợp đồng và fake transport; việc test simulator qua UDP không được tính là đã nghiệm thu kết nối Bluetooth thật.

## 4. Giao diện giữa các phần

G0 phải chốt hợp đồng trước khi viết phần phụ thuộc vào nó. Các tên sau là thiết kế đề xuất để người thực hiện và test dùng thống nhất:

| Phần | Đầu vào | Đầu ra |
|---|---|---|
| `DriveMixer.mix(x, y, limitPercent, boost, calibration, inputMode)` | X/Y, limiter, Boost và `DriveCalibration`; calibration mặc định trung tính, inputMode mặc định JOYSTICK; D_PAD dùng đầy đủ vector trục | `WheelPwm(left: Int, right: Int)` |
| `ControlReducer.press(pointerId, button)` | ID pointer, `ControlButton` | Cập nhật `state: ControlState` |
| `ControlReducer.moveJoystick(pointerId, x, y)` | ID pointer, X/Y | Chỉ cập nhật từ pointer điều hướng hợp lệ; move của pointer đã bị vô hiệu không phục hồi PWM/B |
| `ControlReducer.release(pointerId)` | Một ID pointer | Nhả Boost/vũ khí giữ độc lập pointer khác; nhả pointer điều hướng hợp lệ yêu cầu PWM/B hiệu lực 0 và vô hiệu điều hướng cũ theo D04 đã duyệt |
| `ControlReducer.cancelAll()` | Hủy toàn bộ thao tác | Trạng thái an toàn |
| `ControlReducer.kill()` / `unlock()` | Khóa / thao tác mở khóa rõ ràng | Kill giữ khóa / trung tính, chưa tự chạy |
| `CommandCodec.encodeDrive(pwm, boost, weapon, brake)` | PWM và ba cờ | String gói S, kết thúc newline |
| `CommandCodec.encodeControl(frame, template)` | ControlFrame/typed Payload và template S/D đã duyệt | ByteArray V1 theo hợp đồng G0 |
| `CommandCodec.decodeReply(bytes)` | Phản hồi raw | `RobotReply` hợp lệ hoặc kết quả lỗi |
| `ReplyTracker.begin()/recordSent()/accept()/tick()` | Nonce, ControlFrame, nguồn reply và thời gian monotonic truyền vào | Reply đã đối soát hoặc null; Online/ping chỉ từ ACK state hợp lệ |
| `RobotTransport.send(payload: ByteArray)` | Bản tin hợp lệ | Hoàn tất gửi hoặc báo lỗi, không báo xe đã áp dụng |
| `RobotTransport.receive()` | Kết nối đang mở | Phản hồi raw hoặc lỗi có thể xử lý |
| `RobotTransport.close()` | Kết nối | Hủy nhận/gửi và giải phóng tài nguyên |
| `ControlSession` | Sự kiện reducer, transport, clock đơn điệu và lịch kiểm soát được | Gói gửi tuần tự, trạng thái kết nối và số đo |
| `ControllerSettings` | Toàn bộ danh mục vận hành mục 6.1 của mô tả dự án | Model hợp lệ, không chứa trạng thái chạy |
| `ControllerSettingsStore.load()/save(settings)` | Dữ liệu bền vững/schema và model hợp lệ | Cấu hình có migration, giữ nguyên giá trị cũ khi lỗi |
| `ControlReducer.applySettings(settings)` | Cấu hình được duyệt | Áp dụng theo quy tắc live limiter hoặc stop-before-apply |
| `AppUpdateManager.checkForUpdate()` | Feed HTTPS và phiên bản đang cài | `UpdateInfo` hợp lệ hoặc trạng thái mới nhất/lỗi |
| `AppUpdateManager.downloadAndVerify(info)` | Metadata đã kiểm tra | APK cache riêng có hash/package/signature/version/OS phù hợp |
| `AppUpdateManager.requestInstall()` | APK đã verify, phiên điều khiển đã dừng, quyền được cấp | Session cài Android, trạng thái chờ xác nhận/kết quả |

`ControlButton` bao gồm FORWARD, BACKWARD, LEFT, RIGHT, BOOST, WEAPON, BRAKE, JOYSTICK. Xoay nhanh, Kill và đổi mode là sự kiện riêng vì không cùng hành vi giữ/nhả. `ControlState` chứa PWM, Boost, cơ cấu, Brake, mode, Kill và trạng thái macro; limiter nằm trong `reducer.settings`. Metadata phiên/thứ tự và `RobotReply` theo SUMO-SIM/1-draft1 đã duyệt, chưa phải xác nhận firmware.

**D04 ĐÃ ĐƯỢC DUYỆT G0:** Sau nhả/mất vùng chạm của bất kỳ pointer điều hướng hợp lệ nào, reducer công bố PWM=(0,0), B hiệu lực=0, vô hiệu các pointer điều hướng cũ và theo dõi đến khi chúng nhấc hết. Với cả D-Pad/joystick, kéo trở lại vùng chạm, move hoặc tick của pointer cũ không được phục hồi hướng; pointer ra ngoài vùng chạm vẫn phải nhấc thực sự. Chỉ down điều hướng mới sau khi nhấc hết mới được nhận; chạm đến sớm rồi giữ/move không tự chuyển thành down mới. Nhấc hết mà chưa down mới vẫn giữ PWM/B=0 ở mọi tick. Tick của ControlSession phải đọc trạng thái hiệu lực này, không dựng lại PWM/B từ pointer cũ (kiểm tra tích hợp ở Phase 4). Nhả riêng Boost giữ hướng hợp lệ; nhả điều hướng giữ pointer vũ khí HOLD/latch TOGGLE. Không cần nhấc Boost/vũ khí để nhận down điều hướng mới. Kill, Brake, reconnect và cancel toàn cục giữ quy tắc trong biên bản. Bảng chuỗi chuẩn: [biên bản mục 5.2.1–5.2.2](../../testing/phase-0/BIEN_BAN_DUYET.md); reducer đã kiểm chứng bằng L17–L22 trong hồ sơ G1.

Lõi trạng thái không gọi Android hoặc socket. Không dùng thời gian hệ thống để kiểm thử logic timeout; đưa clock/lịch vào bộ điều phối. Log thời gian chạy thật dùng đồng hồ monotonic trong cùng tiến trình.

`DriveCalibration` là model thuần Kotlin: sensitivity/deadZone/leftGain/rightGain/turnPercent, mặc định lần lượt 1/0/1/1/100 để giữ các test cơ bản. Tham số `calibration` có giá trị mặc định này để các caller/test bốn tham số không bị thay đổi kết quả. G0 chốt miền hiệu chỉnh; không cho gain làm vượt PWM ±255. `UpdateInfo` có schema ở mục 18.2; không đưa kiểu Android vào unit test validation.

## 5. Phase 0 — Chốt phạm vi test và hợp đồng điều khiển

**Mục tiêu:** Có đủ quyết định để viết app thử và simulator mà không tự bịa đặc tả firmware.  
**Đầu vào:** SRS, mô tả dự án và các yêu cầu trong chat.  
**Tệp:** cập nhật hai tài liệu hiện có; khi thực hiện lưu biên bản vào `docs/testing/phase-0/BIEN_BAN_DUYET.md`.

### Các bước

- [x] 0.1 Đối chiếu R01–R23, đánh dấu yêu cầu gốc, yêu cầu người dùng và đề xuất bổ sung. Bằng chứng ngày 17/10/2026: biên bản mục 2; không phải kết quả test chức năng.
- [x] 0.2 Chốt dùng D-Pad cơ bản và joystick; giữ cả hai chế độ cơ cấu. Ghi riêng quyết định về 8 hướng, nút cua và AUTO. G0 D01/D03 đã duyệt, AUTO khóa mặc định.
- [x] 0.3 Duyệt các đề xuất an toàn ở bảng dưới. G0 D02–D04 và 5.2.1–5.2.2 đã duyệt.
- [x] 0.4 Chốt định dạng byte, cờ, miền giá trị, frame kết thúc, phản hồi, phiên/thứ tự và các lệnh ngoài S. G0 D05 đã duyệt cho bản thử.
- [x] 0.5 Chốt transport bản thử. UDP tới simulator theo G0; không khẳng định xe thật dùng UDP.
- [x] 0.6 Chốt phương pháp đo, ngưỡng, số mẫu và nội dung nào chỉ chứng minh được bằng xe thật. G0 D06 đã duyệt.
- [x] 0.7 Kiểm tra/khởi động `Medium_Phone`, xác nhận ADB trước instrumentation. Bằng chứng CLI Phase 0 đã được người dùng chấp nhận thay mở Device Manager; không ghi đã mở UI đó.
- [x] 0.8 Ghi baseline nguồn và kết quả unit test hiện tại; chọn Git commit hoặc checksum làm mã phiên bản bằng chứng. Đã lưu checksum 42 tệp và XML unit 1/1 ngày 17/10/2026.
- [x] 0.9 Trình người dùng bản phạm vi/hợp đồng cụ thể để duyệt G0. Hồ sơ tại `docs/testing/phase-0/BIEN_BAN_DUYET.md`; người dùng đã duyệt qua chat ngày 17/10/2026.
- [x] 0.10 Chốt danh mục tham số, miền hiệu chỉnh, giá trị mặc định và trường cần firmware ACK theo mục 18.1. G0 D07 đã duyệt.
- [ ] 0.11 Chốt applicationId, chủ quản/backup khóa ký ổn định, URL HTTPS feed/APK và quy tắc versionCode trước APK test đầu tiên. Không tạo URL hoặc tải lên tài khoản khi chưa có thông tin đích.

Ghi nhận 17/10/2026: bằng chứng CLI/ADB/instrumentation mẫu Phase 0 đã được chấp nhận qua quyết định G0; không dùng test mẫu chứng minh chức năng. Người dùng duyệt D01–D11 cùng quy tắc 5.2.1–5.2.2. Riêng 0.11 còn thiếu khóa ký riêng/chủ quản/backup/URL hosting thực, được ghi rõ và phải hoàn tất trước bản test phân phối đầu; không chặn lõi/codec/simulator. Phương án ký/cập nhật được duyệt không có nghĩa khóa hoặc URL đã tồn tại.

### Các quyết định phải ghi bằng văn bản

| Mã | Nội dung | Đề xuất để duyệt | Tác động nếu chưa xác nhận |
|---|---|---|---|
| D01 | Nền tảng test | Android Emulator trước; APK test cập nhật trực tiếp trên điện thoại không USB | Đã có lựa chọn từ người dùng; ghi lại vào biên bản |
| D02 | Hướng lái và Boost | Không có hướng thì PWM 0 kể cả giữ Boost; giữ dấu/hướng; làm tròn biên độ tuyệt đối rồi gắn dấu; NaN/vô hạn về trung tính, giá trị hữu hạn ngoài miền được clamp | Chặn G0 cho logic |
| D03 | Thứ tự ưu tiên | Kill/lỗi phiên > Brake > điều khiển hợp lệ; hủy macro khi Kill, Brake hoặc mất điều khiển | Chặn G0; phải mô tả riêng tương tác AUTO |
| D04 | Hủy và mở khóa | Nhả/mất vùng một pointer điều hướng hợp lệ yêu cầu PWM/B hiệu lực 0, vô hiệu điều hướng cũ, giữ 0 ở mọi tick/move đến khi nhấc hết rồi down mới; nhả Boost giữ hướng, nhả điều hướng giữ HOLD/TOGGLE; cancel/nền/reconnect/unlock theo biên bản | Đã duyệt G0; L17–L22 có kết quả reducer G1 và tích hợp G3/G4, không suy thành đạt10ms |
| D05 | Giao thức | Ghi chính xác lệnh Kill, unlock, mode, turn duration/ID, phản hồi và cơ chế gói cũ/trùng | Chặn G0 của codec; không dùng định dạng S cũ như thể đã đủ |
| D06 | Cấu hình | Danh mục mục 18.1; port 1–65535; xoay thử 200 ms, miền đề xuất 1–2000 ms; chỉ template đã quy định | Miền hiệu chỉnh phải duyệt; không hardcode thông số vận hành trong code |
| D07 | Bản thử và xe thật | UDP là đề xuất bản thử; loại mạch/transport/firmware xe thật chưa có | Không chặn test mô phỏng sau khi bản thử được duyệt; chặn G6 |
| D08 | Đo thời gian | Các phép đo ở mục 14; ngưỡng mẫu/soak là đề xuất nghiệm thu | Chặn tuyên bố đạt hiệu năng |
| D09 | AUTO và cơ cấu | Chốt phạm vi AUTO và trạng thái cơ cấu khi Kill/timeout; W=0 chỉ là giả định simulator nếu chưa có xe | Hành vi simulator phải rõ; hành vi vật lý chặn G6 |
| D10 | Cập nhật trên điện thoại | Check/tải trong app, PackageInstaller có xác nhận, cài đè giữ cấu hình; APK test được upload trước G6 | Yêu cầu đã được người dùng xác nhận; kỹ thuật/ca test theo mục 18 |
| D11 | Đích phân phối và identity | Feed/APK HTTPS chủ dự án quản lý, applicationId và khóa ký ổn định, versionCode tăng toàn chuỗi | Cần thông tin trước lần upload test đầu tiên; không tự bịa hosting |

Nếu chưa có firmware, hợp đồng mô phỏng được duyệt với nhãn HỢP ĐỒNG BẢN THỬ. Khi có firmware phải đối chiếu lại ở Phase 6; không gọi đây là tương thích phần cứng đã được xác nhận.

### Điều kiện duyệt G0

- [x] Không còn hai cách hiểu khác nhau về STOP, Brake, Kill, Boost khi đứng yên và thao tác đa ngón, gồm đầu ra ngay và các tick/move sau khi nhả một trong nhiều pointer điều hướng (D04, biên bản 5.2.1–5.2.2).
- [x] Có ít nhất bản tin mẫu input/output cho đứng yên, tiến, Boost, cơ cấu, Brake, Kill/unlock, đổi mode, xoay và phản hồi.
- [x] Phạm vi AUTO/8 hướng có quyết định G0; AUTO khóa mặc định, không tự ghi tự hành thật đã được kiểm chứng hoặc không áp dụng.
- [x] Có mốc đo cụ thể cho 10 ms, dưới 30 ms và 300 ms.
- [x] Môi trường AVD khởi động được qua CLI được chấp nhận; sự cố môi trường được ghi riêng.
- [x] Danh mục/miền tham số và quy trình cập nhật/danh tính ký ổn định đã duyệt. Khóa/hosting còn thiếu ghi rõ tại 0.11, phải hoàn thiện trước upload, không chặn unit test độc lập.
- [x] Người dùng đã duyệt phạm vi bản thử, giao thức mô phỏng và các đề xuất liên quan.

**Không đạt:** tiếp tục sửa tài liệu/hợp đồng; không viết codec theo một giả định chưa duyệt.

## 6. Phase 1 — Lõi điều khiển và an toàn bằng unit test

**Mục tiêu:** Chứng minh đầu ra PWM và trạng thái đúng trước khi nối UI/mạng.  
**Tệp:** `ControlState.kt`, `DriveMixer.kt`, `ControlReducer.kt` và hai test tương ứng trong bảng mục 3.  
**Phụ thuộc:** G0 đã duyệt. **Đầu ra dùng cho Phase 2–4:** `WheelPwm`, `ControlState` và reducer theo mục 4.

### Các bước

- [x] 1.1 Viết test tham số cho D-Pad, joystick, limiter, Boost và giá trị biên; chạy để thấy lỗi đúng nguyên nhân.
- [x] 1.2 Viết phép trộn tối thiểu, clamp/normalize theo D02; không đổi ý nghĩa hai bánh để làm test xanh.
- [x] 1.3 Viết test nhiều pointer, thứ tự nhấn/nhả, cancel, Kill/unlock và mode; L17–L22 theo D04 đã duyệt, assert ngay sau sự kiện và các tick/move tiếp theo.
- [x] 1.4 Triển khai reducer giữ trạng thái theo pointer; nhả riêng Boost/vũ khí không làm mất hướng hợp lệ. D04 đã duyệt: nhả/mất vùng một pointer điều hướng dừng PWM/B và vô hiệu điều hướng cũ, chỉ nhận down mới sau khi nhấc hết; không xóa HOLD/TOGGLE vì nhả điều hướng.
- [x] 1.5 Viết test trường hợp tranh chấp: hai hướng đối nhau, Boost không có hướng, Brake khi Boost, Kill khi đang xoay.
- [x] 1.6 Chạy toàn bộ test logic, ghi hồ sơ G1; đủ kỹ thuật chuyển Phase 2 theo ủy quyền và trình duyệt chung sau đó.
- [x] 1.7 Thêm test F01/F03 lõi cho limiter và calibration lấy từ cấu hình; phần UI/store vẫn ở Phase 3.

**Bằng chứng thực hiện:** [biên bản G1](../../testing/phase-1/BIEN_BAN_DUYET.md), lượt 01–06 red/green; G1 tại lượt 06 là 37/37 JUnit (36 chức năng + 1 mẫu). Sau review bổ sung ba ca calibration, G1 có 39 ca chức năng đạt trong lượt 18 toàn module **60/60**, lint 0 error/12 warning. Các số lịch sử của lượt 06/13 giữ nguyên theo thời điểm chạy.

Ví dụ test đầu ra theo đề xuất D02, chỉ viết/chạy sau khi D02/G0 được duyệt; đặt trong `DriveMixerTest.kt`:

~~~kotlin
package com.aistudy.myapplication.control

import org.junit.Assert.assertEquals
import org.junit.Test

class DriveMixerTest {
    @Test
    fun forwardAtSixtyPercentIs153OnBothWheels() {
        assertEquals(
            WheelPwm(153, 153),
            DriveMixer.mix(x = 0f, y = 1f, limitPercent = 60, boost = false)
        )
    }

    @Test
    fun boostAtCenterDoesNotCreateMovement() {
        assertEquals(
            WheelPwm(0, 0),
            DriveMixer.mix(x = 0f, y = 0f, limitPercent = 60, boost = true)
        )
    }
}
~~~

Đây là ví dụ test cụ thể cho hai ca, không thay thế các ca trong bảng:

| Test | Kích thích | Kỳ vọng dự kiến, chỉ áp dụng sau duyệt G0 |
|---|---|---|
| L01 | Tiến/lùi tại limiter 100% | `255,255` / `-255,-255` |
| L02 | Trái/phải tại 100% | Hai bánh ngược dấu, đúng chiều |
| L03 | Tiến/lùi tại limiter 30%, 60%, 100% | Biên độ 77, 153, 255 theo D02; giữ đúng dấu và không vượt mức |
| L04 | Giữ Tiến, giữ/nhả Boost | Tăng tối đa rồi trở lại limiter; hướng không mất |
| L05 | Không có hướng, Boost bật | Hai bánh 0 |
| L06 | Nhả điều hướng khi Boost còn giữ | Hai bánh/B hiệu lực 0 ngay và các tick sau, không tự phục hồi từ pointer cũ; chuỗi đầy đủ ở L20 |
| L07 | Pointer 0 Tiến, pointer 1 Boost; nhả pointer 1 | Tiến còn, Boost tắt |
| L08 | Thứ tự ngón đảo lại hoặc pointer ID không liên tiếp | Kết quả độc lập với thứ tự/ID |
| L09 | Joystick trung tâm, bốn góc và chéo | Không vượt ±255; tâm bằng 0; chiều đúng |
| L10 | X/Y NaN/vô hạn hoặc hữu hạn ngoài [-1,1] | NaN/vô hạn tạo PWM 0; hữu hạn được clamp/normalize, không vượt ±255 |
| L11 | Cơ cấu hold và toggle, chạm lặp | Chuyển trạng thái đúng chế độ; release không đảo toggle lần nữa |
| L12 | Brake khi đang chạy/Boost | Hành vi hãm ưu tiên đúng D03; không chỉ đổi màu nút |
| L13 | Kill khi có nhiều ngón hoặc macro | Trạng thái chuyển động bị khóa, không hồi lại khi thả một ngón |
| L14 | Unlock/reconnect rồi chưa có thao tác mới | Trung tính |
| L15 | Cancel, đổi mode điều khiển, chuyển MANUAL/AUTO | Không giữ đầu vào cũ hoặc cho manual chen vào AUTO |
| L16 | Xoay nhanh rồi Kill/Brake/cancel | Macro bị hủy theo hợp đồng |
| L17 | Tiến + Lùi → nhả Lùi khi Tiến còn giữ → nhấc hết → down Tiến mới | Limiter 60%/calibration mặc định: (153,153) → (0,0) khi hai hướng đối nhau → nhả Lùi vẫn (0,0), B=0 và pointer Tiến cũ bị vô hiệu; mọi tick/move giữ 0, nhấc hết vẫn 0; chỉ down mới cho (153,153) |
| L18 | Tiến + Trái → nhả Trái khi Tiến còn giữ | Trước nhả: (0,153) theo mixer tại 60%; ngay sau nhả và mọi tick/move của Tiến cũ: (0,0), B=0; nhấc hết chưa down vẫn 0, down Tiến mới cho (153,153) |
| L19 | Tiến + Boost → nhả riêng Boost | (255,255), B=1 → (153,153), B=0 ngay và các tick sau; pointer Tiến vẫn hợp lệ, không yêu cầu nhấc/chạm lại; mở rộng assert chuỗi của L04/L07 |
| L20 | Tiến + Boost → nhả Tiến khi Boost còn giữ | Ngay sau nhả và mọi tick/move: (0,0), B hiệu lực=0; Boost không tự sinh hướng. Khi điều hướng cũ đã nhấc, down Tiến mới trong lúc Boost vẫn giữ được (255,255), B=1; không yêu cầu nhấc Boost |
| L21 | Nhả điều hướng khi vũ khí HOLD hoặc TOGGLE đang bật, chạy riêng từng chế độ | Ngay sau nhả và mọi tick: PWM=(0,0), B=0, W=1; HOLD giữ pointer vũ khí, chỉ up vũ khí đưa W=0; TOGGLE giữ latch W=1 dù không còn pointer vũ khí, chỉ down vũ khí lần kế tiếp mới tắt. Nhả điều hướng không thay thế cancel toàn cục/Kill/Brake |
| L22 | D-Pad/joystick mất vùng chạm hợp lệ, kéo trở vào; down điều hướng đến trước khi pointer cũ nhấc hết; đổi thứ tự và ID pointer | PWM/B=0 ngay lúc mất vùng và mọi tick/move; pointer ra ngoài vẫn phải nhấc thực sự. Down đến sớm bị bỏ qua, giữ/move nó sau khi pointer cũ cuối cùng nhấc không phục hồi chạy; phải có down mới sau mốc nhấc hết. Lặp L17–L21 với ID không liên tiếp/thứ tự đảo; W giữ theo chế độ |

**L17–L22 đã được G0 duyệt và đã chạy ở reducer Phase 1.** Bảng trên giữ kích thích/kỳ vọng, kết quả thực và tên test/XML được ánh xạ trong biên bản G1. Kiểm tra bằng sự kiện/đồng hồ kiểm soát được, không sleep. Ở Phase 4 còn phải assert tick sender chỉ đọc PWM/B hiệu lực đã dừng, không phục hồi từ pointer cũ; chưa coi unit test là tích hợp sender đã đạt.

Run:

~~~powershell
./gradlew.bat --offline :app:testDebugUnitTest --tests "com.aistudy.myapplication.control.DriveMixerTest" --tests "com.aistudy.myapplication.control.ControlReducerTest" --console=plain
~~~

**Kỳ vọng:** lệnh thoát 0, các ca mới đạt, không chỉ test phép cộng mẫu.

### Điều kiện duyệt G1

- [x] L01–L22 và F01/F03 lõi đạt kỹ thuật theo G0; có test/XML thực trong hồ sơ G1, không có ca skip.
- [x] Có bằng chứng test bắt lỗi trước khi triển khai phần logic tương ứng.
- [x] Không dùng sleep/wall clock để chứng minh tính đúng đắn của reducer.
- [x] Reducer không sinh PWM khác 0 sau nhả điều hướng, Kill hoặc mất quyền điều khiển cho tới thao tác/ACK mới hợp lệ theo G0; chưa kết luận cho sender/UI.
- [x] Người dùng đã duyệt G1/G2 theo hồ sơ cuối60/60 và giao tiếp tục Phase3+4; không tự gán duyệt cho kết quả mới.

## 7. Phase 2 — Codec và robot mô phỏng độc lập

**Mục tiêu:** Chứng minh app mã hóa đúng và phía nhận hiểu đúng hợp đồng bản thử.  
**Tệp:** `CommandCodec.kt`, `ReplyTracker.kt`, 3 lớp test protocol, `tools/sumo_simulator.py`, `tools/test_sumo_simulator.py`.  
**Đầu vào:** trạng thái Phase 1 và bản tin G0. **Đầu ra:** codec, reply hợp lệ và simulator có log.

### Các bước

- [x] 2.1 Viết test exact bytes cho các bản tin đã duyệt, gồm newline và miền số/cờ.
- [x] 2.2 Triển khai encoder và decoder tối thiểu; giữ bản tin mẫu SRS trong test để kiểm tra tương thích đã chọn.
- [x] 2.3 Viết receiver Python chỉ dùng thư viện chuẩn: socket, time, json, argparse, unittest; không dùng lại encoder app làm oracle.
- [x] 2.4 Thêm trạng thái hai bánh, cơ cấu, phanh, Kill, mode và macro đúng hợp đồng; watchdog có clock kiểm soát được trong self-test.
- [x] 2.5 Thêm phản hồi hợp lệ, log JSONL và các scenario ở mục 13.3 để tái hiện lỗi.
- [x] 2.6 Chạy self-test và cross-check dữ liệu mẫu giữa Kotlin/Python; đề nghị duyệt chung G1/G2.

**Bằng chứng thực hiện:** [biên bản G2](../../testing/phase-2/BIEN_BAN_DUYET.md), red/green/fixture/trace/XML thực. Sau review và hai bản sửa, toàn module **60/60 JUnit** (39 G1 + 20 G2 + 1 mẫu), simulator 31/31 self-test, 25/25 vector Kotlin/Python; có 1 ca DatagramSocket ↔ tiến trình Python thật. Các lỗi mạng/watchdog kiểm tra bằng clock điều khiển được; chưa đo mạng tới xe thật.

Ví dụ test bản tin S gốc trong `CommandCodecTest.kt`:

~~~kotlin
package com.aistudy.myapplication.protocol

import com.aistudy.myapplication.control.WheelPwm
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandCodecTest {
    @Test
    fun compositePacketContainsEveryFlagAndOneNewline() {
        assertEquals(
            "S:200,200,1,0,0\n",
            CommandCodec.encodeDrive(
                pwm = WheelPwm(200, 200),
                boost = true,
                weapon = false,
                brake = false
            )
        )
    }
}
~~~

Nếu G0 chọn hợp đồng mở rộng, giữ test gói S này như test format gốc nếu format còn được hỗ trợ; thêm exact-bytes test cho toàn bộ frame mới. Không dùng test S để chứng minh đã gửi được Kill, mode hoặc metadata.

| Test | Kỳ vọng |
|---|---|
| C01 | Đứng yên, tiến/lùi, xoay, cờ 0/1 được mã hóa đúng từng byte |
| C02 | Kill/unlock, mode, thời gian và ID xoay có bản tin và phản hồi đúng G0 |
| C03 | Thiếu/thừa trường, quá dài, số lỗi, PWM ngoài biên, thiếu terminator bị từ chối theo hợp đồng |
| C04 | Không nhận nhầm chuỗi literal backslash-n thành newline |
| C05 | Reply sai phiên/thứ tự hoặc sai nguồn không làm Online hay cập nhật ping |
| C06 | Bản tin không hợp lệ không làm mới watchdog |
| C07 | Simulator có PWM 0 tại mốc 300 ms không có lệnh hợp lệ, kể cả đang Boost/macro |
| C08 | Macro cùng một ID được gửi lặp chỉ thực hiện một lần |
| C09 | Gói di chuyển cũ đến sau STOP/Kill không làm xe mô phỏng chạy lại |
| C10 | Sửa template hợp lệ cho kết quả mới; template lỗi bị chặn trước khi gửi |

Run:

~~~powershell
./gradlew.bat --offline :app:testDebugUnitTest --tests "com.aistudy.myapplication.protocol.CommandCodecTest" --console=plain
py -3.13 tools/sumo_simulator.py --self-test
~~~

**Kỳ vọng:** cả hai lệnh thoát 0. Self-test simulator phải có kiểm tra parser và watchdog, không chỉ in “OK”.

### Điều kiện duyệt G2

- [x] C01–C10 đạt kỹ thuật; log thể hiện trạng thái áp dụng, không chỉ raw bytes đã nhận.
- [x] Fixture bản tin và mã phiên bản SUMO-SIM/1-draft1 được lưu vào biên bản/checksum.
- [x] Simulator xử lý gói xấu và thời gian giả lập bằng assert.
- [x] Ghi rõ kết quả watchdog là ở simulator, chưa phải firmware.
- [x] Người dùng đã duyệt G1/G2 theo hồ sơ cuối60/60,31 Python,25 vector và cho phép thực hiện Phase 3+4. Không thay số liệu lịch sử.

## 8. Phase 3 — Giao diện, tự chỉnh thông số và cập nhật APK test

**Mục tiêu:** Thao tác và chỉnh thông số được trên emulator; chuẩn bị vòng cập nhật test trên điện thoại không USB, trước khi phụ thuộc xe thật.  
**Tệp:** UI/settings/store, update manager/info/receiver/dialog, build config và các test ở mục 3.  
**Đầu vào:** reducer Phase 1, codec Phase 2, identity/hosting đã chốt. **Đầu ra:** UI đúng sự kiện, tham số có hiệu lực và được lưu, APK test cập nhật được trong app.

### Các bước

- [x] 3.1 Viết test UI đầu tiên cho màn hình landscape, nút có nhãn/semantics và trạng thái trung tính.
- [x] 3.2 Thay Greeting bằng ControllerScreen, bố cục trái/phải/công cụ, màu nút theo SRS, fullscreen và màn hình không ngủ khi điều khiển.
- [x] 3.3 Nối pointer ID thật tới reducer; xử lý up, cancel, rời vùng theo G0, chuyển D-Pad/joystick.
- [x] 3.4 Nối Boost, cả hai chế độ cơ cấu, Brake, xoay, limiter, mode, Kill/unlock. Khi chưa có kết nối chỉ cho xem/test qua fake được gắn rõ nhãn.
- [x] 3.5 Viết F01–F10, rồi nối toàn bộ danh mục chỉnh thông số, validation/store/schema/restore-default và migration. Cấu hình phải tác động thật tới mixer/codec/lịch, không chỉ lưu hoặc hiển thị.
- [x] 3.6 Nối lifecycle: vào nền/mất tương tác theo G0 thì trung tính, hủy macro; không khôi phục thao tác từ saved state.
- [x] 3.7 Thêm haptic theo sự kiện và trạng thái; không rung lặp mỗi heartbeat.
- [ ] 3.8 Chạy instrumentation và thử tay giao diện/cấu hình trên emulator; lưu ảnh/video.
- [ ] 3.9 Viết A01–A14 và triển khai UpdateInfo/AppUpdateManager/AppUpdateDialog theo mục 18: khai báo INTERNET từ Phase 3, REQUEST_INSTALL_PACKAGES/receiver cần thiết, HTTPS metadata, so phiên bản, tải/verify APK và PackageInstaller có xác nhận.
- [ ] 3.10 Cấu hình khóa ký/identity ổn định cho APK test; tạo A/B/C tăng versionCode, upload APK trước metadata và chạy ít nhất hai lần cập nhật từ app, không dùng USB/adb install thay luồng cần nghiệm thu.
- [ ] 3.11 Đối chiếu cấu hình còn nguyên, mở lại ở trạng thái dừng; ghi video/phiên bản và duyệt G3. Nhánh liên quan transport thật của A13 chạy lại ở Phase 4.

| Test | Thao tác và đầu ra |
|---|---|
| U01 | Mở app, kiểm tra ngang/fullscreen, không bị che nút bởi inset/cutout |
| U02 | Tiến/lùi/trái/phải và joystick tác động reducer đúng |
| U03 | Inject pointer 0 Tiến + pointer 1 Boost; nhả từng ngón theo cả hai thứ tự, đối chiếu đầu ra ngay và tick tiếp theo ở L19/L20 sau khi được G0 duyệt |
| U04 | Giữ điều hướng + cơ cấu hold/toggle hoặc Brake; từng ngón độc lập |
| U05 | Hủy gesture, Home, mở Settings, đổi chế độ điều khiển: không giữ chuyển động cũ |
| U06 | Kill trong khi giữ nhiều ngón; thả/nhấn ngón khác không mở khóa |
| U07 | Mở khóa rõ ràng khi đã trung tính; không tự chạy lại |
| U08 | IP rỗng/sai, port 0/65536/chữ, thời gian 0/âm/quá giới hạn, template sai: báo lỗi và không áp dụng |
| U09 | Cấu hình hợp lệ được áp dụng; đóng/mở app giữ cấu hình nhưng không giữ lệnh chạy |
| U10 | Nhãn, vùng chạm, màu và chữ trạng thái dễ phân biệt; nút lớn không chồng chéo |
| U11 | Sự kiện haptic được phát đúng một lần cho loại tương tác đã duyệt; cảm giác rung để Phase 6 |
| U12 | Thanh limiter 30/60/100% và chuyển chế độ cơ cấu có đầu ra đúng |

Test đa điểm chạm phải inject ít nhất hai pointer trong cùng chuỗi gesture trên root Compose. Click liên tiếp bằng một con trỏ hoặc chỉ quan sát Preview không đủ duyệt U03/U04.

Run:

~~~powershell
./gradlew.bat --offline :app:testDebugUnitTest :app:lintDebug --console=plain
./gradlew.bat --offline :app:connectedDebugAndroidTest --console=plain
~~~

Chỉ chạy connected test khi ADB có đúng AVD test hoặc đã chọn thiết bị rõ ràng; không vô tình cài lên điện thoại khác. Tác vụ tạo debug/test APK là bình thường trong phase này, chưa có release.

**Thực thi17/10/2026:** 3.1–3.7 đã triển khai và có test red/green trong hồ sơG3/G4. 3.8 đã có instrumentation/ảnh AVD; người dùng trực tiếp thử còn chờ. 3.9 phần logic/platform đã làm, A10/A14 chưa có hosting/khóa/điện thoại. 3.10–3.11 chưa đạt điều kiện cập nhật thật, không dùng adb install thay A→B→C. UI chỉnh theo phản hồi: D-Pad chữ thập, trạng thái bánh, nhóm nút màu SRS và theme tối thống nhất. Không viết lại thiết kế hoặc thêm tính năng.

### Điều kiện duyệt G3

- [ ] U01–U12 đạt theo phạm vi G0; không dùng kết quả unit test thay cho bài pointer thật.
- [x] UI đọc được ở landscape trên AVD hiện có và một kích thước cửa sổ nhỏ hơn phù hợp.
- [ ] Người dùng đã thử D-Pad, joystick, Boost, cơ cấu, Brake, Kill và Settings.
- [x] Có ảnh/video thao tác, log và báo cáo instrumentation.
- [x] Không ghi đã kết nối xe thật khi vẫn dùng fake.
- [ ] F01–F10 đạt trên phần mềm/simulator tương ứng; tham số firmware chưa có mạch được ghi rõ chưa kiểm chứng vật lý.
- [ ] A01–A14 được kiểm tra trong phạm vi phase; A10 có ít nhất A → B → C bằng luồng trong app, giữ cấu hình. Nhánh A13 qua kết nối thật chỉ duyệt tiếp ở G4/G5.
- [ ] Kênh test có hướng dẫn upload và chữ ký/versionCode đúng; không cần USB để người dùng nhận phiên bản tiếp theo.

## 9. Phase 4 — Kết nối simulator và điều phối gửi lệnh

**Mục tiêu:** Đi từ thao tác Android tới dữ liệu bên nhận, có phản hồi và fail-safe.  
**Tệp:** `ControlSession.kt`, `RobotTransport.kt`, adapter được G0 chọn, manifest và các test session/transport/E2E.  
**Đầu vào:** UI Phase 3, codec/simulator Phase 2. **Đầu ra:** hệ thống thử tích hợp có log hai phía.

### Các bước

- [x] 4.1 Viết test session bằng clock/lịch kiểm soát được: tick mặc định 30 ms và giá trị cấu hình 20/50 ms, thao tác tức thời, mất phản hồi, đóng kết nối.
- [x] 4.2 Viết loopback socket test trước adapter; triển khai gửi/nhận ở ngoài UI thread và đóng socket đúng vòng đời.
- [x] 4.3 Cho một chủ sở hữu quản lý thứ tự phát lệnh; event STOP/Kill ưu tiên và không để gói chuyển động đã xếp hàng vượt qua trạng thái mới.
- [x] 4.4 Nối Online/Offline, nonce/seq và ping từ phản hồi hợp lệ; send thành công không được tự chuyển Online.
- [x] 4.5 Kiểm tra INTERNET đã khai báo từ Phase 3 và xử lý quyền mạng nội bộ phù hợp OS/target; test cấp, từ chối và thu hồi trên AVD API 37.
- [x] 4.6 Chạy emulator nối simulator qua địa chỉ host đã xác nhận; đối chiếu raw frame và trạng thái áp dụng.
- [ ] 4.7 Chạy mất gói, reply lỗi/trễ, reconnect, app nền, Kill và macro trùng; lưu bằng chứng, duyệt G4.
- [ ] 4.8 Chạy lại F02/F04/F07/F09 và A13 qua session/transport: áp dụng cấu hình vào frame/thời gian thực sự, cập nhật app chỉ khi điều khiển đã dừng; không để mạng tải APK tạo một vòng gửi khác.

| Test | Kích thích | Kỳ vọng |
|---|---|---|
| I01 | Giữ hướng | Gói định kỳ đầy đủ trạng thái, lịch danh nghĩa 30 ms |
| I02 | Nhấn/nhả/STOP/Kill | Yêu cầu phát ngay, không đợi tick |
| I03 | STOP/Kill xuất hiện khi sender đang bận | Không có chuyển động cũ áp dụng sau lệnh mới |
| I04 | Không có reply hợp lệ | Không hiển thị Online hoặc ping giả |
| I05 | Dừng simulator, chặn/không phản hồi | App về Offline/trung tính; thao tác giữ không tự chạy lại khi kết nối hồi |
| I06 | Restart simulator hoặc app | Phiên cũ không được tiếp tục điều khiển |
| I07 | Gói trễ/cũ/trùng, reply sai nguồn/phiên | Bị loại theo hợp đồng, macro không lặp |
| I08 | Mất mạng trong khi chạy/Boost/macro | App phản ứng an toàn; watchdog simulator dừng theo mốc hợp đồng |
| I09 | Sửa Settings khi đang giữ điều hướng | Dừng phiên cũ, áp dụng an toàn, không gửi tới hai endpoint cùng lúc |
| I10 | Từ chối/thu hồi quyền LAN | Báo trạng thái dễ hiểu, không crash hoặc tiếp tục báo Online |
| I11 | Hệ thống không có Internet nhưng endpoint nội bộ hợp lệ | Không phụ thuộc dịch vụ cloud để hoạt động |
| I12 | Lỗi I/O/đóng socket khi nhận đang chờ | Không treo UI, không tạo nhiều vòng gửi sau Reconnect |

Run:

~~~powershell
./gradlew.bat --offline :app:testDebugUnitTest :app:lintDebug --console=plain
py -3.13 tools/sumo_simulator.py --host 127.0.0.1 --port 8888 --scenario normal --log docs/testing/phase-4/simulator-normal.jsonl
~~~

Giữ simulator ở terminal riêng rồi chạy `:app:connectedDebugAndroidTest` ở terminal dự án. Simulator tự tạo thư mục log nếu chưa có và thoát lỗi rõ ràng khi port đang bị dùng; không tự kết thúc tiến trình khác.

**Thực thi17/10/2026:** 4.1–4.6 đã có bằng chứng; 4.7 các kịch bản đã chạy nhưng phần duyệt G4 vẫn chờ; 4.8 đã kiểm tra cấu hình/macro/endpoint/gate, F09 chỉ fixture receiver theo G0. Hồ sơG4 giữ cả log lỗi/sửa và kết quả đo không đạt ngưỡng. Không tự nới10ms hoặc20–50ms và không chuyển Phase5.

### Điều kiện duyệt G4

- [ ] I01–I12 đạt hoặc có phạm vi không áp dụng được G0 xác nhận.
- [x] Có dữ liệu từ bên nhận để chứng minh không mất hướng/cờ khi nhiều ngón.
- [x] Không có gói chuyển động được áp dụng sau STOP/Kill trong ca race và gói cũ.
- [x] Online/ping có phản hồi hợp lệ, không dựa trên ICMP hoặc số giả định.
- [ ] Không crash, treo hoặc nhân đôi lịch gửi khi thay cấu hình/Reconnect.
- [ ] Người dùng duyệt chạy tích hợp app–simulator.
- [ ] Đổi thời gian xoay/chu kỳ gửi/endpoint từ app có hiệu lực bên nhận mà không build; cập nhật APK không tiếp tục phiên lái.

## 10. Phase 5 — Hồi quy và nghiệm thu giai đoạn giả lập

**Thực thi17/10/2026 theo ngoại lệ người dùng:** được làm phần độc lập dù G3/G4 còn tồn đọng, không phải duyệt hai gate đó. Kết quả thực tế tại [biên bản Phase5](../../testing/phase-5/BIEN_BAN_DUYET.md) và [ma trận R](../../testing/phase-5/MA_TRAN_R01_R23.md): soak600/20 đạt, STOP500 và cadence20/30 đạt, cadence50 còn626/1283 mẫu>50ms. I11/API36/điện thoại/HTTPS/khóa ký và một số phần vật lý chưa kiểm chứng; chưa đề nghị G5 đạt. Bảng ca và ngưỡng dưới đây giữ nguyên; không dùng kế hoạch làm bằng chứng test.

**Điều chỉnh G5 AVD được người dùng duyệt sau lượt08:** giữ lịch danh nghĩa20/30/50ms;20/30ms chấm20–50ms, riêng50ms chấm20–60ms (jitter tối đa+10ms),≥1000interval/mức, STOP500 vẫn≤10ms. Dùng một cohort mới sau duyệt. Điện thoại/HTTPS/API36/I11/xe thật được giữ CHƯA KIỂM CHỨNG ngoài phạm vi G5 AVD hiện tại; không ghi R23 hoàn thành hoặc tự chuyển Phase6/7/release. Các số FAIL lượt08 nêu trên là lịch sử, không chấm lại theo cận mới. [Quyết định và cách chấm](../../testing/phase-5/DIEU_CHINH_G5_AVD.md) quy định ngoại lệ này; không thay yêu cầu hệ thống G6.

**Bổ sung sau sửa và quyết định17/10, kiểm chứng tiếp18/10/2026:** cache snapshot/CPU profiler mặc định đã sửa;110/110unit,6/6Android trênAPK cuối (gồm2thu timing),8/8fault. Cohort21 cadence đạt cận đã duyệt nhưng STOP2/500>10ms,max11,4174ms;Normal20 còncadence50>60ms. Người dùng chốt **G5CÓ ĐIỀU KIỆN cho chức năngAVD**, giữSTOP/cadence làlỗi mở,chặnxe/release. Ngày18/10không đổi nguồn/APK:25/25AndroidUI/Settings/updater+soak/probe ở33;soak30phút0,668giây/600chu kỳ/20reconnect,audit60.557framekhôngrevival. Soak08giữbinarycũ;tracechẩnđoán30–32khôngđóngFAIL20/21. KhôngtựduyệtG3/G4/chuyểnPhase6/7 hoặcđánhdấuđủG5toànphần. [Báocáobổsung/quyếtđịnh](../../testing/phase-5/KET_QUA_BO_SUNG_G5.md)giữmọibằngchứngFAILvàgiớihạn;khôngdùngkếhoạchthaybằngchứng.

**Mục tiêu:** Nghiệm thu giai đoạn giả lập và vòng cập nhật APK test; chưa bàn giao bản dùng thi đấu.  
**Tệp:** các test hiện có; bằng chứng lưu `docs/testing/phase-5/`; cập nhật ma trận R01–R23.  
**Đầu vào:** G1–G4 đã duyệt. **Đầu ra:** báo cáo ca test, giới hạn và quyết định G5.

### Các bước

- [ ] 5.1 Chạy unit test, lint và toàn bộ instrumentation trên cùng phiên bản mã.
- [ ] 5.2 Chạy từng scenario simulator ở mục 13.3; đối chiếu L/C/U/I và các ca E dưới đây.
- [ ] 5.3 Đo thời gian nội bộ app theo mục 14; ghi số mẫu, phân vị và max. Không suy ra độ trễ vật lý từ RTT.
- [ ] 5.4 Đề xuất chạy liên tục 30 phút, tối thiểu 500 chu kỳ nhấn/nhả và 20 lần reconnect; các mức này phải đã duyệt trong G0.
- [ ] 5.5 Sửa lỗi theo ca tái hiện; chạy lại ca liên quan và hồi quy phần bị tác động.
- [ ] 5.6 Lập báo cáo rõ PASS/FAIL/CHƯA KIỂM CHỨNG cho từng R; đưa cho người dùng thử và duyệt G5.
- [ ] 5.7 Chạy lại F01–F10/A01–A14 trên bản test mới nhất; người dùng nhận ít nhất hai cập nhật bằng Internet trên điện thoại, test phiên bản mới và ghi một vòng lỗi → sửa → cập nhật.

| Test | Kịch bản | Điều kiện đạt |
|---|---|---|
| E01 | Kết nối, limiter, tiến/lùi, joystick, Boost, cơ cấu, Brake, xoay, STOP, Kill | UI, gói lệnh và trạng thái áp dụng bên nhận khớp hợp đồng |
| E02 | Hai ngón hoạt động, ngón thứ ba bấm Kill | Kill có hiệu lực; không rơi sự kiện nhả |
| E03 | Ứng dụng ra nền, đóng/mở, restart AVD, đổi kích thước | Không khôi phục chuyển động đang chạy |
| E04 | Các lỗi simulator xảy ra trong lúc giữ nút | Không có chuyển động tự phục hồi |
| E05 | Kịch bản soak đã duyệt | Không crash/ANR, không tăng số sender/socket, không giữ pointer sau kết thúc |
| E06 | Bản app đang chạy và log simulator cùng hợp đồng/source version | Kết quả tái hiện được từ hướng dẫn |
| E07 | Đo release-to-send trên emulator | Đối chiếu ≤10 ms đúng mốc đo; mẫu vượt ngưỡng ghi lỗi, không bỏ mẫu |
| E08 | API 37 cấp/từ chối LAN và API 36 khi có môi trường | Ghi môi trường từng ca; API 37 PASS không đại diện API 36 |

### Điều kiện duyệt G5

- [ ] Các ca logic, format, UI và tích hợp áp dụng đã đạt; không có lỗi an toàn đang mở.
- [ ] Các ca không chạy phải ghi lý do; không biến skip thành PASS.
- [ ] Có báo cáo số đo thật từ emulator, nêu cấu hình host/AVD và hạn chế.
- [ ] R07 góc thật, R08 phanh thật, R15 MCU, R17 cảm giác rung và R19 hệ thống được ghi CHƯA KIỂM CHỨNG nếu chưa thử thật.
- [ ] Người dùng trực tiếp duyệt app test và kết luận ĐẠT KIỂM THỬ TRÊN GIẢ LẬP.
- [ ] Không tạo release sau G5; chuyển Phase 6 khi có đủ phần cứng.
- [ ] R22/R23 đạt theo phạm vi test: cấu hình không cần build và vòng update app có bằng chứng giữ dữ liệu, không cần USB. Chưa đủ điện thoại/hosting thì giữ CHƯA KIỂM CHỨNG cho phần đó, không tự ghi PASS.

Ở phạm vi G5 AVD đã được duyệt bổ sung, các mục điện thoại/HTTPS/API36/I11/xe thật được hoãn có ghi nhận, không dùng làm bằng chứng đã đạt yêu cầu hệ thống hoặc cập nhật thật. Người dùng duyệt phạm vi/tiêu chí không đồng nghĩa đã trực tiếp xem và duyệt kết quả cohort mới.

Đầu ra dự kiến của giai đoạn là bản thử emulator và APK test cập nhật được trên điện thoại. Quyết định G5 có điều kiện hiện tại chỉ chốt chức năng AVD; cập nhật HTTPS trên điện thoại vẫn CHƯA KIỂM CHỨNG, chưa có quyền upload hoặc release từ quyết định này. Quy trình lặp sửa mã nguồn → test → upload APK test tại mục18 chỉ thực hiện khi có đủ điều kiện và quyền tương ứng. Tiếp tục nghiệm thu phần cứng khi có đủ điều kiện và người dùng cho phép.

## 11. Phase 6 — Tích hợp hệ thống thật và duyệt đóng gói

**Mục tiêu:** Xác nhận các yêu cầu không thể chứng minh bằng emulator và quyết định release.  
**Điều kiện đầu vào:** người dùng muốn tiếp tục, có điện thoại Android mục tiêu, xe, firmware và người phối hợp. Thiếu một điều kiện thì phase CHƯA KIỂM CHỨNG.  
**Tệp:** chỉnh adapter/codec theo hợp đồng phần cứng nếu cần; test hồi quy; `docs/testing/phase-6/` và mô tả dự án.

### Các bước

- [ ] 6.1 Xác nhận điện thoại/Android, MCU, driver, cơ cấu, phương thức kết nối và quy tắc AUTO từ thể lệ.
- [ ] 6.2 Đối chiếu hợp đồng thử với firmware; mọi khác biệt phải được ghi, duyệt và thêm regression test.
- [ ] 6.3 Chạy trước với bánh được kê khỏi mặt đất, kiểm tra dấu và ngưỡng PWM, trạng thái cơ cấu an toàn và phanh driver.
- [ ] 6.4 Chạy watchdog firmware thực: dừng gửi trong lúc chạy, Boost và macro; dữ liệu sai không được làm mới timeout.
- [ ] 6.5 Hiệu chỉnh xoay 180 độ, kiểm tra hủy macro và giới hạn cơ khí; không dùng 200 ms như kết quả hiệu chỉnh mặc định.
- [ ] 6.6 Đo dưới 30 ms và STOP ≤10 ms theo phương pháp đã duyệt; test kết nối không Internet, nhiễu/gián đoạn có kiểm soát.
- [ ] 6.7 Chạy lại đa ngón trên màn hình thật, rung thật và bài lái liên tục; xác nhận API 36 nếu đó là máy thi đấu.
- [ ] 6.8 Hoàn tất ma trận yêu cầu và lỗi; trình bằng chứng cho người dùng duyệt G6, bao gồm quyền đóng gói.

| Test | Kích thích | Kết quả bắt buộc |
|---|---|---|
| H01 | Tiến/lùi/xoay, limiter, Boost và nhả tay | Đúng chiều, mức tốc độ và trạng thái dừng trên xe |
| H02 | Cơ cấu hold/toggle và Kill/timeout | Đúng hành vi và trạng thái cơ khí đã duyệt |
| H03 | Giữ/nhả Brake | Tạo hãm thật đúng driver, không chỉ đặt PWM 0 |
| H04 | Mất gói/mất sóng/đóng app/tắt điện thoại | Firmware độc lập dừng theo 300 ms đã đo |
| H05 | Gói cũ/trùng hoặc bản tin sai | Không khôi phục chuyển động/khởi động lại macro hoặc kéo dài watchdog |
| H06 | Xoay nhanh | Góc và sai số được người dùng duyệt, thời gian có thể chỉnh từ app |
| H07 | MANUAL/AUTO khi áp dụng | Firmware chuyển mode thật; Kill và mất điều khiển đúng quy tắc |
| H08 | Số đo thời gian và trạng thái motor/driver | Có bằng chứng đúng mốc, không lấy RTT thay số đo một chiều |
| H09 | Nhiều ngón và rung trên điện thoại mục tiêu | Thao tác độc lập, không bị hệ thống che nút hoặc ngắt ngoài dự kiến |

### Điều kiện duyệt G6

- [ ] R01–R23 được đối chiếu đầy đủ; mọi điều kiện không áp dụng có quyết định phạm vi được duyệt.
- [ ] Không còn ca bắt buộc CHƯA KIỂM CHỨNG; đặc biệt không bỏ qua STOP, Kill và watchdog.
- [ ] Không có lỗi gây chạy ngoài ý muốn, sai cơ cấu hoặc lỗi an toàn đang mở.
- [ ] Hợp đồng firmware, thông số hiệu chỉnh, thiết bị và source version đã được ghi.
- [ ] Người dùng duyệt kết quả nghiệm thu hệ thống.
- [ ] Người dùng duyệt rõ cho phép tạo bản release bàn giao.

**Không đạt G6:** giữ bản test debug để sửa và kiểm chứng; không chạy Phase 7. Không tuyên bố emulator đã thay thế bước này.

## 12. Phase 7 — Đóng gói sau khi đạt và kiểm tra bản bàn giao

**Mục tiêu:** Tạo đúng bản release từ mã đã được G6 duyệt, rồi kiểm tra bản đó.  
**Tệp:** build config, strings/tài nguyên nhận diện cần thiết và `docs/testing/phase-7/`.  
**Đầu vào bắt buộc:** G6 đã duyệt nghiệm thu và quyền đóng gói.

### Các bước

- [ ] 7.1 Hoàn thiện tên hiển thị và tăng versionCode; giữ applicationId/khóa ký đã chốt cho chuỗi test để cài đè giữ dữ liệu. Không đổi identity ở cuối dự án hoặc ghi mật khẩu/keystore vào Git.
- [ ] 7.2 Giữ nguyên mã chức năng đã được duyệt; loại debug-only test hook/simulator khỏi bản bàn giao.
- [ ] 7.3 Chạy unit test, lint và hồi quy liên quan sau mọi đổi cấu hình release.
- [ ] 7.4 Tạo bản release ký đúng cấu hình được giao; không tự phát hành lên kho ứng dụng.
- [ ] 7.5 Cài đè từ bản test lên chính APK bàn giao qua luồng cập nhật; smoke test cấu hình/migration, phiên bản, kết nối, lái, STOP, Boost, Brake, Kill, reconnect và timeout.
- [ ] 7.6 Ghi checksum SHA-256, version, source/hợp đồng firmware, thiết bị, số đo và hướng dẫn cài/sử dụng.
- [ ] 7.7 Người dùng duyệt G7 và nhận bản đóng gói.

Các lệnh sau chỉ được dùng sau G6:

~~~powershell
./gradlew.bat --offline :app:testDebugUnitTest :app:lintRelease :app:assembleRelease --console=plain
& 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\build-tools\36.0.0\apksigner.bat' verify --verbose --print-certs 'app/build/outputs/apk/release/app-release.apk'
Get-ChildItem -LiteralPath 'app/build/outputs/apk/release' -Filter '*.apk' | Get-FileHash -Algorithm SHA256
~~~

`assembleRelease` thành công không tự chứng minh APK đã được ký bằng khóa bàn giao hoặc chạy đúng. Xác minh chữ ký và smoke test trên chính artifact sẽ giao. Nếu đổi minify/optimization, khóa ký hoặc code sau G6, kiểm tra lại phần chịu ảnh hưởng trước G7.

### Điều kiện duyệt G7

- [ ] Artifact ký hợp lệ, checksum và source version khớp hồ sơ.
- [ ] Chính bản release đạt smoke test và các ca an toàn bắt buộc.
- [ ] Không còn cấu hình simulator/test-only hoặc thông tin nhạy cảm trong artifact.
- [ ] Có hướng dẫn cài, kết nối, Kill, xử lý Offline và giới hạn đã biết.
- [ ] Người dùng đã nhận, cài và duyệt bản bàn giao.
- [ ] Người dùng cài đè qua Internet không USB; thông số cũ được giữ, trạng thái chạy không được khôi phục.

## 13. Cấu hình môi trường và các lệnh kiểm chứng

### 13.1 PowerShell và Gradle

Các lệnh trong kế hoạch là lệnh sẽ chạy khi triển khai, không phải kết quả đã thực thi khi viết tài liệu.

~~~powershell
Set-Location -LiteralPath 'C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
./gradlew.bat --offline :app:testDebugUnitTest :app:lintDebug --console=plain
~~~

Trong phiên sandbox đã khảo sát, JDK gặp lỗi `Unable to establish loopback connection` ở đường dẫn tạm dài. Nếu tái diễn, chỉ áp dụng cho terminal kiểm thử:

~~~powershell
$env:JAVA_TOOL_OPTIONS = '"-Djdk.net.unixdomain.tmpdir=C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT\.gradle"'
~~~

Giữ thư mục `.gradle` của dự án tồn tại. Không sửa thiết lập Java toàn máy để xử lý riêng lỗi này. Nếu offline thiếu dependency, ghi lỗi thật và tải đúng dependency khi được môi trường cho phép; không hạ phiên bản hoặc bỏ test để làm xanh.

### 13.2 Android Emulator

~~~powershell
& 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\emulator\emulator.exe' -list-avds
& 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices -l
~~~

Khởi động `Medium_Phone` từ Device Manager rồi đợi boot xong. AVD sẵn có dùng API 37.1; nếu máy thi đấu thực sự API 36 thì kiểm thử thêm API 36 ở thời điểm có môi trường, không tự coi kết quả API 37 là đã bao phủ.

Chạy UI test:

~~~powershell
./gradlew.bat --offline :app:connectedDebugAndroidTest --console=plain
~~~

Kỳ vọng exit code 0, báo cáo có đúng lớp test chức năng và không có ca fail bị che bởi skip. Xem:

- Unit: `app/build/test-results/testDebugUnitTest/`, `app/build/reports/tests/testDebugUnitTest/`.
- Instrumentation: `app/build/outputs/androidTest-results/connected/`, `app/build/reports/androidTests/connected/`.
- Lint: `app/build/reports/`, chọn báo cáo của variant vừa chạy.

Không gắn cố định serial `emulator-5554` trong lệnh nếu chưa kiểm tra thiết bị thực tế.

### 13.3 Simulator trên máy tính

Chọn Python đã được kiểm tra có sẵn: `py -3.13`. Chạy self-test trước khi dùng simulator làm bằng chứng.

Giao diện CLI đã triển khai và self-test ở Phase 2; lệnh chạy server dưới đây dành cho tích hợp Phase 4 sau khi được phép:

~~~powershell
py -3.13 tools/sumo_simulator.py --self-test
py -3.13 tools/sumo_simulator.py --host 127.0.0.1 --port 8888 --scenario normal --log docs/testing/phase-4/simulator-normal.jsonl
~~~

Trong app trên emulator, cấu hình IP `10.0.2.2`, port `8888` để tới host loopback của máy phát triển. `127.0.0.1` bên trong emulator là chính emulator, không phải receiver trên máy tính. Đây là cấu hình test, không ghi đè endpoint xe thật vào bản bàn giao.

Scenario phải có mục đích rõ và có thể chạy lại bằng cùng tham số:

| Giá trị `--scenario` | Hành vi receiver mô phỏng |
|---|---|
| `normal` | Áp dụng bản tin hợp lệ, phản hồi đúng hợp đồng |
| `drop-rx` | Cố ý không áp dụng và không làm mới watchdog từ các gói điều khiển nhận được |
| `drop-reply` | Nhận lệnh bình thường nhưng không phản hồi để thử Offline của app |
| `delay` | Trì hoãn có kiểm soát bản tin/phản hồi; ghi rõ loại và thời gian trong log |
| `duplicate` | Tái xử lý cùng bản tin/ID để kiểm tra deduplicate macro |
| `reorder` | Giữ một gói chuyển động cũ và đưa tới sau STOP/Kill |
| `malformed` | Trả phản hồi sai cấu trúc/phiên để kiểm tra app không báo Online sai |

Với delay/duplicate/reorder, G0 và self-test phải quy định thứ tự cụ thể và giá trị thời gian; không dùng random không có seed để làm bằng chứng. Simulator log JSONL ít nhất có thời điểm monotonic, phiên/thứ tự, payload, quyết định accept/reject, trạng thái áp dụng, sự kiện watchdog/macro và lý do.

Tham số CLI thực: mặc định caps=3/AUTO khóa, chỉ bind IPv4 loopback; `--port 0` cho test tự chọn port. `--fault-after 2` cho 2 datagram V1 đầu đi bình thường; `--delay-ms 150`/`350`, `--delay-target rx`/`reply`; reorder phát lại gói cũ sau STOP100 ms. Malformed xoay vòng thiếu trường/sai SID/future seq; sai peer được test riêng bằng tracker và UDP socket thật. Cách chạy có XML/trace và kết quả từng scenario nằm trong biên bản G2, không phải bằng chứng Phase 4 đã chạy.

## 14. Phương pháp đo và điều kiện đạt hiệu năng

Các cỡ mẫu dưới đây là đề xuất nghiệm thu, không phải số liệu hoặc yêu cầu nguyên văn trong SRS. G0 phải duyệt trước khi dùng làm gate.

| Phép đo | Cách đo | Điều kiện đề xuất |
|---|---|---|
| Nhả tay đến phát STOP | Cùng clock monotonic trong app: bắt đầu callback nhận release/cancel tới ngay trước/sau lời gọi gửi socket; ghi rõ hai mốc | Tối thiểu 500 lần nhả; max không vượt 10 ms; báo riêng thời gian callback-to-send và giới hạn chưa đo touch vật lý |
| Lịch heartbeat | Log từng tick định kỳ và lần gửi thật; sự kiện tức thời được phân loại riêng | Danh nghĩa 30 ms; đánh giá khoảng 20–50 ms cho luồng định kỳ khi giữ, không áp cận 20 ms lên event tức thời |
| Ping | Nonce/seq request–reply, cùng clock bên app | Hiển thị RTT đúng; mất reply không giữ ping cũ như trạng thái Online |
| Phản hồi dưới 30 ms | Đồng hồ chung hoặc thiết bị đo chung từ sự kiện được chọn tới lúc firmware áp dụng motor/driver | Tối thiểu 1.000 mẫu và điều kiện tải/mạng được duyệt; báo p50/p95/p99/max, số vượt ngưỡng; yêu cầu “dưới 30” không được thay bằng chỉ p95 dưới 30 |
| Watchdog 300 ms | Simulator dùng clock giả cho test biên; firmware dùng log/logic analyzer có mốc last-valid-frame và motor-off cùng clock | Không có lệnh hợp lệ 300 ms thì ra lệnh dừng; độ phân giải và chu kỳ vòng lặp phải được ghi để đánh giá đúng ngưỡng |
| Xoay 180 độ | Đo góc thực trên xe qua nhiều lần, các mức pin/tải phù hợp | Người dùng duyệt sai số góc và thông số hiệu chỉnh; 200 ms không tự tương đương 180 độ |

Không trừ timestamp trên hai máy không đồng bộ để suy ra một chiều. ACK cho biết nhận/áp dụng theo hợp đồng, không tự chứng minh bánh đã dừng cơ học.

Mỗi báo cáo số đo ghi: source version, hợp đồng, app variant, OS/AVD, host, kết nối, cỡ mẫu, các mốc đo, độ phân giải, phân vị/max và mẫu lỗi. Không xóa mẫu chậm để đổi kết luận; nếu phép đo không đủ độ chính xác thì ghi CHƯA KIỂM CHỨNG.

## 15. Ma trận yêu cầu và nơi duyệt

| Yêu cầu | Test chính | Gate chức năng | Bằng chứng bổ sung trước release |
|---|---|---|---|
| R01 | U01, U10, E03 | G3/G5 | Layout trên điện thoại mục tiêu |
| R02 | L01–L03, U02, E01 | G1/G3/G5 | H01 |
| R03 | L09–L10, U02, E01 | G1/G3/G5 | H01 |
| R04 | L06/L17–L18/L20/L22, I02–I03, E07 | G1/G4/G5 | Số đo app thật, không nhầm với thời gian dừng cơ học |
| R05 | L04–L07/L19–L20, U03, E01 | G1/G3/G5 | H01 |
| R06 | L11/L21, U04, E01 | G1/G3/G5 | H02 |
| R07 | L16, C02/C08, E01 | G1/G2/G5 | H06, góc thật và hiệu chỉnh |
| R08 | L12, U04, E01 | G1/G3/G5 | H03 |
| R09 | L03–L04, U12 | G1/G3/G5 | H01 |
| R10 | L15, C02, E01 | G1/G2/G5 nếu áp dụng | H07 hoặc quyết định không áp dụng đã duyệt |
| R11 | L13–L14, U06–U07, I03 | G1/G3/G4/G5 | H04–H05 |
| R12 | C05, I04–I07/I12 | G2/G4/G5 | Kết nối thật, H08 |
| R13 | C01–C05/C10, I07 | G2/G4/G5 | Hợp đồng firmware thật |
| R14 | I01–I03, mục 14 | G4/G5 | Đối chiếu điện thoại và firmware |
| R15 | C06–C07, I08, E04 | G2/G4/G5 cho mô phỏng | H04–H05 bắt buộc |
| R16 | L07–L08/L17–L22, U03–U04, E02 | G1/G3/G5 | H09; ngoại lệ STOP điều hướng phải được G0 duyệt |
| R17 | U11 | G3/G5 cho lời gọi | H09 cho rung vật lý |
| R18 | C10, U08–U09/U12, I09 | G2/G3/G4/G5 | Áp dụng cấu hình với firmware thật |
| R19 | E07 và mục 14 | G5 cho số đo giới hạn emulator | H08 để duyệt G6 |
| R20 | L13–L22, C06–C09, U05, I03/I05–I12, E03–E04 | G1–G5 | H04–H05/H07 |
| R21 | Hồ sơ gate và danh sách artifact | G0–G7 | G6 trước release, G7 sau smoke test |
| R22 | F01–F10, L03/L09, C10, U08–U09/U12, I09 | G1/G3/G4/G5 | Tham số phần cứng có hiệu lực và ACK thật ở G6 |
| R23 | A01–A14 | G3/G4/G5 | A → B → C trên điện thoại không USB; nâng lên bản bàn giao giữ cấu hình tại G7 |

Không đặt tỷ lệ phần trăm hoàn thành chung khi vẫn còn yêu cầu an toàn chưa kiểm chứng. Báo cáo theo mã R và theo mức môi trường.

## 16. Hồ sơ duyệt từng phase

Khi phase thực sự được thực hiện, lưu hồ sơ dưới `docs/testing/phase-N/`. Tại thời điểm lập kế hoạch chỉ tạo hai file Markdown người dùng yêu cầu; các tệp bằng chứng dưới đây được tạo sau.

Mẫu nội dung bắt buộc cho `BIEN_BAN_DUYET.md`:

~~~text
Phase / Gate:
Phiên bản nguồn hoặc checksum:
Phiên bản hợp đồng app–receiver:
Thiết bị / AVD / OS / app variant:
Danh sách yêu cầu R được kiểm tra:
Lệnh đã chạy và exit code:
Ca đã chạy / đạt / lỗi / chưa kiểm chứng / không áp dụng:
Bảng input → đầu ra mong đợi → đầu ra thực tế:
Đường dẫn log, ảnh, video, báo cáo test và số đo:
Lỗi còn mở, mức ảnh hưởng, ca tái hiện:
Kết luận người thực hiện:
Quyết định người duyệt:
Tên/vai trò người duyệt và thời điểm:
Phạm vi được phép chuyển tiếp:
~~~

Không điền sẵn “ĐẠT” hoặc người duyệt. Những ô này để người thực hiện ghi từ bằng chứng và quyết định thực tế, không phải thiếu nội dung kế hoạch.

Lỗi cần chặn gate ngay:

- Chuyển động sau nhả điều hướng hoặc Kill không theo hợp đồng.
- Nhả Boost/vũ khí làm mất hướng ngoài hợp đồng; hoặc nhả/mất vùng một pointer điều hướng vẫn để pointer điều hướng cũ sinh chuyển động ở tick/move sau, trái D04 được duyệt.
- Gói cũ làm chạy lại; macro khởi động lại do bản tin lặp.
- Watchdog bị kéo dài bởi dữ liệu lỗi hoặc không ngắt đúng điều kiện.
- UI báo Online khi không có phản hồi hợp lệ.
- Test bị bỏ qua nhưng vẫn được báo đạt, hoặc số đo sai môi trường.
- Bản release dùng thi đấu được tạo trước G6; APK test đúng checklist mục 18 không thuộc lỗi này.
- Trường chỉnh thông số không tác động đầu ra, reset cấu hình khi nâng cấp hoặc phải gỡ app để cập nhật do identity/khóa bị đổi.

Khi gặp lỗi, thêm ca tái hiện, sửa nguyên nhân chung, chạy lại nhóm liên quan và cập nhật biên bản. Không đổi ngưỡng hoặc giảm phạm vi để làm kết quả xanh nếu chưa có thay đổi yêu cầu được duyệt.

## 17. Tài liệu kỹ thuật đã đối chiếu

Đã đọc ngày 16/10/2026:

- [Test your app](https://developer.android.com/studio/test).
- [Android Emulator networking](https://developer.android.com/studio/run/emulator-networking).
- [Network address space](https://developer.android.com/studio/run/emulator-networking-address): địa chỉ host `10.0.2.2` và loopback emulator.
- [Local network permission](https://developer.android.com/privacy-and-security/local-network-permission?hl=en): kiểm tra quyền LAN trên Android 17 khi target SDK 37.
- [How app updates work](https://developer.android.com/google/play/app-updates): điều kiện cập nhật cùng applicationId/chữ ký và versionCode.
- [Sign your app](https://developer.android.com/studio/publish/app-signing): chữ ký dùng ổn định cho chuỗi APK.
- [Alternative distribution](https://developer.android.com/distribute/marketing-tools/alternative-distribution): APK do chủ dự án phân phối qua HTTPS.
- [PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller): cài đặt bằng session, xử lý pending user action và kết quả.
- [PackageManager.canRequestPackageInstalls](https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls()): quyền yêu cầu cài từ nguồn app.

Tài liệu Android hỗ trợ cách triển khai/test nền tảng. SRS, quyết định người dùng và các hợp đồng được duyệt là căn cứ nghiệm thu sản phẩm.

## 18. Tự chỉnh thông số và cập nhật APK test không USB

Phần này là yêu cầu bổ sung đã được người dùng nêu, không phải tùy chọn bỏ qua. Nó mở rộng Phase 1/3/4/5 và điều kiện nâng cấp ở Phase 7; không thay đổi điều kiện an toàn của G6.

### 18.1. Cấu hình vận hành phải có hiệu lực thực tế

Dùng danh mục mục 6.1 của mô tả dự án làm đầu vào cho Settings. G0 chốt miền cho sensitivity, dead zone, cân bằng bánh và mức quay; không chọn ngầm một giới hạn cơ khí chưa biết. Các miền đã có căn cứ gồm limiter 30–100%, PWM ±255, khoảng gửi 20–50 ms; thời gian xoay đề xuất 1–2000 ms cần duyệt như D06.

Luồng áp dụng:

1. Đọc cấu hình đã lưu, kiểm tra schema và bổ sung giá trị mặc định chỉ cho trường mới.
2. Người dùng sửa; hiện đơn vị, miền hợp lệ, giá trị hiện có và lỗi ngay tại trường.
3. Limiter trên màn hình lái áp dụng trực tiếp như SRS. Cấu hình nâng cao cần dừng điều khiển trước khi áp dụng.
4. Kiểm tra toàn bộ bản cấu hình trước khi lưu; hủy/sai dữ liệu giữ bản cũ.
5. Nối giá trị mới vào mixer, codec, timer hoặc bản tin cấu hình firmware tương ứng. Không chỉ đổi nhãn/UI.
6. Trường cần firmware áp dụng chỉ báo thành công sau ACK hợp lệ; thiếu hỗ trợ thì ghi rõ và giữ trạng thái trước.
7. Khi mở lại hoặc cài bản mới, nạp các giá trị đã lưu nhưng luôn bắt đầu ở trung tính.
8. Khôi phục mặc định có xác nhận, giữ ràng buộc an toàn và không đổi applicationId/khóa ký/version.

Khóa ký, danh tính ứng dụng và cơ chế fail-safe không phải thông số để tùy ý tắt. Đổi URL cập nhật là cấu hình quản trị có xác nhận/kiểm tra HTTPS, tách riêng với endpoint xe; APK vẫn phải khớp chữ ký và identity đang cài.

| Test | Kích thích | Điều kiện đạt |
|---|---|---|
| F01 | Chỉnh limiter 30/60/100% trên cùng bản cài | PWM cơ bản tương ứng thay đổi; versionCode và APK không đổi, không build lại |
| F02 | Đổi thời gian xoay 200 → 350 ms khi đang dừng | Simulator/firmware dùng giá trị mới đúng hợp đồng; không còn timer cố định 200 ms |
| F03 | Chỉnh sensitivity/dead zone/cân bằng bánh | Dữ liệu đi vào DriveMixer và đầu ra thay đổi theo miền G0; tâm không trôi, Boost không sinh hướng, PWM không vượt ±255 |
| F04 | Đổi chu kỳ gửi 20/30/50 ms | Một lịch gửi duy nhất dùng cấu hình mới; STOP/Kill vẫn đi đường tức thời |
| F05 | Nhập giá trị ngoài miền, NaN/vô hạn, port lỗi hoặc template sai | Báo lỗi, không lưu/áp dụng một phần và không xóa cấu hình trước |
| F06 | Lưu cấu hình, đóng/mở app và khởi động lại thiết bị | Giữ giá trị đã chỉnh; không giữ pointer/Boost/macro/chuyển động |
| F07 | Chỉnh nâng cao hoặc đổi endpoint khi đang giữ nút | Đòi dừng theo G0; không gửi cấu hình giữa một thao tác đang chạy, không sinh hai phiên kết nối |
| F08 | Hủy sửa hoặc khôi phục mặc định | Hủy giữ nguyên dữ liệu; reset chỉ thực hiện sau xác nhận và vẫn ở trạng thái dừng |
| F09 | Đổi tham số cơ cấu hoặc macro cần MCU, ACK lỗi/timeout/không hỗ trợ | Không báo đã áp dụng trên xe; hiện lý do và giữ cấu hình hiệu lực cũ |
| F10 | Schema cấu hình được nâng từ A sang B/C | Migration giữ các giá trị người dùng, thêm mặc định cho trường mới; lỗi migration không âm thầm xóa toàn bộ |

F01/F03 thuộc lõi ở Phase 1 và UI ở Phase 3. F02/F04/F07/F09 được kiểm tra bằng fake trước, chạy lại qua simulator ở Phase 4 và phần cứng tại G6 khi áp dụng. F10 kết hợp A10/A11 để nghiệm thu nâng cấp thật.

### 18.2. Cơ chế cập nhật trong app

Phạm vi tối thiểu cần đủ:

- Màn hình có phiên bản đang cài, nút Kiểm tra bản mới, phiên bản mới, ghi chú thay đổi, tiến trình tải, Cài đặt, Hủy/Thử lại.
- Chỉ kiểm tra/tải theo thao tác người dùng trong giai đoạn này; không dựng dịch vụ nền tự cập nhật giữa trận đấu.
- Đọc metadata qua HTTPS, timeout hữu hạn, không chặn UI. Có lỗi thì giữ app hiện tại, không làm điều khiển phụ thuộc máy chủ cập nhật.
- Tải APK vào cache riêng, kiểm tra hoàn tất và verify trước khi mở phiên cài.
- Đọc metadata APK thực tế để đối chiếu với thông tin công bố, không chỉ tin tên tệp hoặc chuỗi versionName.
- Dùng PackageInstaller của Android; manifest có REQUEST_INSTALL_PACKAGES theo luồng cần thiết, kiểm tra canRequestPackageInstalls và dẫn tới màn hình cấp quyền đúng app khi cần.
- Xử lý STATUS_PENDING_USER_ACTION và kết quả thành công/thất bại/hủy; Android quyết định có cho cài hay không. Không tự cấp quyền hoặc bỏ qua chặn của hệ điều hành.
- Trước khi tải/cài phải kết thúc thao tác điều khiển, hủy macro, gửi trạng thái dừng theo hợp đồng và không tự nối lại để tiếp tục chạy.
- Sau khi cài, người dùng mở lại app, thấy version thực tế của package đã cài, cấu hình được giữ và điều khiển trung tính.

Metadata của bản thử dùng một tài liệu JSON phiên bản hóa trên kênh HTTPS, với các trường bắt buộc:

| Trường | Miền/ý nghĩa |
|---|---|
| schemaVersion | Phiên bản schema metadata được app hỗ trợ; phiên bản không hiểu thì từ chối |
| applicationId | Phải bằng package đang cài của chuỗi cập nhật |
| versionCode | Số nguyên dương, lớn hơn bản đang cài theo chính sách dự án |
| versionName | Chuỗi hiển thị cho người thử, không dùng thay versionCode để so sánh |
| minSdk | Mức Android tối thiểu; phải khớp APK thực tế và tương thích thiết bị |
| apkUrl | URL HTTPS tải APK trực tiếp từ đích được cấu hình; kiểm tra cả redirect |
| sizeBytes | Kích thước tệp để kiểm tra tải đầy đủ và giới hạn tải |
| sha256 | Hash SHA-256 tính từ đúng APK được upload |
| releaseNotes | Nội dung thay đổi/lỗi đã sửa để người dùng biết cần thử gì |
| channel | Giai đoạn hiện tại là test; không gắn nhãn đã nghiệm thu hệ thống |

Kiểm tra chữ ký của APK với chữ ký tin cậy của package đang cài. Không nhận khóa mới chỉ vì metadata cung cấp một fingerprint khác. SHA-256 kiểm tra tính toàn vẹn tệp, không thay thế kiểm tra chữ ký.

Một URL HTTPS cho metadata và nơi chứa APK là đủ cho bản đầu; chưa cần tài khoản trong app, Play Store, hệ thống OTA riêng hoặc CI/CD. URL/nơi lưu trữ thực tế cần người dùng cung cấp hoặc chọn trước upload đầu tiên; tài liệu này chưa tạo server.

### 18.3. Khóa ký, version và dữ liệu qua nhiều lần cập nhật

- Chốt applicationId trước bản test đầu tiên trên điện thoại; bản bàn giao sau này dùng cùng identity để cài đè.
- Chủ dự án giữ/backup một khóa ký ổn định cho chuỗi APK này. Không dùng debug keystore tự tạo khác nhau theo máy hoặc đổi khóa sau mỗi bản.
- Không đưa keystore, mật khẩu hoặc token upload vào source, APK, metadata hay log chia sẻ.
- Mỗi bản test/final được đưa lên có versionCode lớn hơn mọi bản trước của cùng chuỗi. Một lần sửa mã nguồn sau bản B cần bản C với số mới, không ghi đè B bằng nội dung khác dưới cùng versionCode.
- Dùng đường dẫn APK theo phiên bản, không thay nội dung tệp đã công bố. Có thể giữ bản cũ để đối chiếu nhưng app không đề nghị cài hạ phiên bản.
- Cài cập nhật không gỡ app trước. SettingsStore giữ dữ liệu và migration được test; không gọi clear dữ liệu để né lỗi schema.
- Nếu bản mới còn lỗi, sửa rồi đưa bản cao hơn lên. Không xây cơ chế rollback/hạ version trong phạm vi này.

### 18.4. Quy trình đưa bản test lên và nhận trên điện thoại

Checklist cho mỗi vòng lặp, áp dụng trước G6:

- [ ] T1 Ghi phiên bản/lỗi tái hiện từ bản đang cài; sửa mã nguồn và thêm/chạy ca regression liên quan.
- [ ] T2 Chạy unit test, lint và smoke phù hợp; bản test khởi động được, không còn lỗi an toàn đã biết trong phạm vi đem thử. Lỗi chức năng còn lại phải được liệt kê, không giả vờ tất cả test đã đạt.
- [ ] T3 Tăng versionCode/versionName, build APK test và ký bằng khóa đã chốt; không chạy assembleRelease để tạo bản dùng thi đấu.
- [ ] T4 Kiểm tra identity, chữ ký, version và SHA-256 của chính APK; đối chiếu với metadata.
- [ ] T5 Upload APK vào đường dẫn phiên bản và kiểm tra tải lại đúng byte/hash; sau đó mới cập nhật metadata kênh test. Không để app được báo bản mới trong lúc APK chưa có.
- [ ] T6 Điện thoại mở app → Cập nhật ứng dụng → Kiểm tra → Tải → Xác nhận cài Android. Lần đầu nhận app bằng liên kết HTTPS; các lần sau nhận từ màn hình cập nhật.
- [ ] T7 Mở lại bản mới, kiểm tra phiên bản/cấu hình/trung tính và thử lại ca lỗi. Không dùng cáp USB hoặc lệnh adb install để thay cho bước cần nghiệm thu này.
- [ ] T8 Ghi kết quả theo đúng version; nếu vẫn lỗi thì lặp T1–T7 với versionCode mới.

Quyền triển khai luồng và upload test đã được người dùng yêu cầu trong chat. Việc thực hiện thật vẫn cần có APK và đích upload cụ thể; chưa có thì hoàn thiện phần độc lập, ghi phần còn thiếu, không tự tạo tài khoản hoặc tuyên bố đã upload.

Các lệnh kiểm tra artifact dự kiến khi thực hiện, từ thư mục dự án và với signing test đã cấu hình:

~~~powershell
./gradlew.bat --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
& 'C:\Users\CHI NGUYEN\AppData\Local\Android\Sdk\build-tools\36.0.0\apksigner.bat' verify --verbose --print-certs 'app/build/outputs/apk/debug/app-debug.apk'
Get-FileHash -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk' -Algorithm SHA256
~~~

Kiểm tra fingerprint bằng khóa đã chốt, không chỉ nhìn “verify success”. File debug mẫu từ lần khảo sát không phải APK test đã được cấu hình signing/update.

Tải/cài cập nhật cần mạng truy cập được kênh HTTPS. Khi đang nối Wi-Fi xe không Internet, dừng phiên lái trước rồi dùng mạng có thể tải; không tự đổi mạng và bỏ lại xe đang nhận lệnh. App vẫn lái offline khi không kiểm tra cập nhật.

### 18.5. Ca kiểm thử cập nhật

| Test | Kích thích | Điều kiện đạt |
|---|---|---|
| A01 | Mở màn hình update và kiểm tra feed mới hợp lệ | Hiện version đang cài/bản mới/changelog đúng; không cần USB |
| A02 | Feed cùng/thấp versionCode, schema không hỗ trợ hoặc sai applicationId | Không đề nghị cài bản sai/cũ; giải thích rõ và giữ app hiện tại |
| A03 | Tải APK mới, hủy giữa chừng rồi thử lại | Có trạng thái/tiến trình, không dùng tệp thiếu, có thể tải lại |
| A04 | Tệp thiếu, hỏng hoặc hash không khớp | Từ chối trước phiên cài; không xóa app/cấu hình đang dùng |
| A05 | APK sai package, sai chữ ký hoặc khác version công bố | Bị chặn, không đề nghị gỡ app để cài qua kiểm tra |
| A06 | minSdk/khả năng thiết bị không phù hợp | Báo không tương thích trước cài; phiên bản cũ còn dùng được |
| A07 | Chưa cấp quyền yêu cầu cài, từ chối rồi cấp lại | Có hướng dẫn đúng màn hình hệ thống, xử lý quay lại; không crash hoặc tự cấp quyền |
| A08 | Offline, HTTP lỗi, timeout, TLS lỗi, JSON lỗi hoặc redirect không hợp lệ | Báo lỗi/thử lại, không ảnh hưởng dữ liệu cũ hoặc giả báo đã cập nhật |
| A09 | Android hủy/chặn cài, thiếu dung lượng hoặc trả lỗi | Hiện kết quả thật; app hiện tại/cấu hình không bị chủ động xóa |
| A10 | A → B → C; C sửa lỗi đã ghi ở B | Ít nhất hai cập nhật từ app qua Internet, không USB; package/key ổn định, version tăng, cấu hình giữ nguyên |
| A11 | B/C thêm trường cấu hình hoặc migration thất bại | Giữ các thông số cũ; thêm mặc định đúng trường mới; không khôi phục chuyển động hoặc xóa dữ liệu âm thầm |
| A12 | Upload APK trước metadata, feed/cache trỏ bản chưa tải được | Quy trình công bố đúng thứ tự; lỗi tệp không có xử lý được, không ghi đã cài trước xác nhận hệ thống |
| A13 | Yêu cầu update khi đang giữ hướng/Boost/macro hoặc đang có phiên kết nối | Đòi dừng, hủy thao tác và kết thúc phiên trước update; không có gói chạy cũ sau khi mở lại |
| A14 | Tiến trình app bị kết thúc khi cài và người dùng mở lại | Đọc version thực tế vừa cài, còn cấu hình và updater cho vòng tiếp theo, luôn bắt đầu trung tính |

Instrumentation chạy trong chính app A có thể bị kết thúc khi cài B. Không dùng việc test runner bị ngắt như bằng chứng update thành công, cũng không giả định nó sẽ tiếp tục assert. A10/A14 dùng video thử tay hoặc công cụ điều phối bên ngoài app: ghi bản A, thao tác update thật, mở B/C rồi kiểm tra version/cấu hình sau cài.

G3 duyệt UI/settings/update độc lập với xe thật; G4 chạy lại nhánh kết nối của F/A; G5 có bằng chứng vòng sửa lỗi và cập nhật trên điện thoại; G6/G7 duyệt tác động phần cứng và nâng từ test lên bản bàn giao. Nếu chưa có điện thoại/hosting, phần tương ứng là CHƯA KIỂM CHỨNG; không cản việc xây/test logic độc lập, nhưng không được ghi hoàn thành R23.


