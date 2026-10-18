# Checkpoint — G5 chốt có điều kiện cho chức năng AVD

18/10/2026, Asia/Bangkok. Workspace `C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT`.

## Quyết định và phạm vi

- Người dùng chọn trong chat: **“Chốt G5 CÓ ĐIỀU KIỆN cho chức năng AVD; giữ lỗi hiệu năng mở, chặn dùng xe và release”** sau khi nhận kết quả mới. Không suy thành người dùng đã trực tiếp thử trên điện thoại.
- G0/G1/G2 đã duyệt; G3/G4 vẫn CHỜ DUYỆT CHUNG. Không tự chuyển Phase6/7, dùng xe, upload hoặc release.
- Ngoại lệ đã duyệt trước đo: cấu hình50ms trên AVD chấm20–60ms;20/30ms vẫn20–50ms;STOP vẫn≤10ms. Không đổi thêm ngưỡng hoặc bỏ mẫu FAIL.
- Đợt tiếp tục18/10 đã hoàn tất kiểm chứng độc lập trên cùng APK cuối; không đổi mã/test/tool/SDK/dependency. Không chạy lặp timing chọn lượt đẹp; giữ nguyên quyết định gate và mọi FAIL.

## Mã và bằng chứng cuối

- `ControlSession.revision` và cache snapshot ở `ControlRuntime` giảm cấp phát poll rỗng2ms; giữ lịch/ưu tiên/ACK/seq/replay. `MainActivity` bỏ CPU sampler chẩn đoán mặc định; mốc monotonic và trace gói vẫn giữ.
- `ControlRuntimeTest` thêm test allocation và publish trạng thái; red488.160→green136.376byte/1000gap, lượt cuối136.088byte. JVM, không suy thành latency ART.
- `tools/check_android_cadence.py` chỉ bật cận60ms với `--g5-avd-50-jitter`; mặc địnhG0 giữ nguyên. `tools/test_android_cadence.py`6/6; red01/green02 và hồi quy05 giữ FAIL626mẫu của log08.
- **110/110 unit**: Gradle `:app:testDebugUnitTest`, exit0 ở `phase-5/evidence/16-g5-avd-approval/23-final-unit-check.txt` và `23-final-unit-xml/`. Lint0lỗi/17cảnh báo và app/test APK build hoàn tất ở log20; toàn lệnh20 từng exit1 vì timeout test allocation, được sửa và kiểm lại ở23.
- **6/6 Android trên APK cuối**, gồm4UDP/tích hợp+2thu timing: `phase-5/evidence/20-g5-profiler-off-cohort/`. Test thu mẫu đạt không đồng nghĩa ngưỡng timing đạt.
- **8/8 fault trên APK cuối**: `phase-5/evidence/22-g5-final-faults/`, wrapper `16-g5-avd-approval/27-final-faults.txt` exit0.
- 28/28UI/Settings/UDP ở `19-g5-related-functional/` là build cache trước khi bỏ CPU sampler; không cộng trùng với6 hoặc nói đã chạy đủ37ca trên APK cuối.
- Soak08/Python31/31/vector25/25 giữ lịch sử. **Soak33 trên APK cuối đã chạy/audit riêng:** 30 phút 0,668 giây, 600 chu kỳ, 20 reconnect, 10 Kill/mở khóa; 60.557 frame/21 SID, không revival/đảo seq; 20 checkpoint worker/trace 1/1, FD socket 14→10 sau đóng. Bằng chứng: `33-final-functional-soak/soak-summary.json`.
- **25/25 Android, 0 fail/skip trên APK cuối** ở33: 23 UI/Settings/updater + soak + probe. Không cộng với6 hoặc28 cũ thành tổng ca độc lập. `final-binding.json` xác nhận63 file nguồn khớp cohort21, app/test hash không đổi; unit/lint/UDP/8fault tái dùng bằng chứng đã khớp.

## Lỗi mở và môi trường

- Cohort21 STOP500: max trước send9,8024ms, sau send11,4174ms; **2/500 sau send>10ms, FAIL**. Cadence20/30/50 đủ1245/1339/1282interval, max30,9317/38,7934/54,4796ms, đạt cận đã duyệt trong thí nghiệm power policy.
- Cohort Normal20 trên cùng APK cuối còn33/1281cadence50>60ms,max76,8416ms;STOP3/500>10ms,max14,0058ms. Không coi cohort21 là bảo đảm ổn định ở mọi điều kiện.
- Mọi cohort16/17/18/20/21 và CSV/raw/JSON giữ trong hồ sơ. Không quy hết lỗi cho AVD/Windows hoặc tuyên bố tối ưu đã sửa latency.
- Thí nghiệm cũ đã khôi phục QEMU Normal/power0/0. Phiên18/10 không đổi ưu tiên/power policy; đã tắt trace/receiver/AVD do phiên mở. `33-final-functional-soak/final-environment.json` xác nhận không còn QEMU/UDP8888/8889/thiết bị ADB; ảnh/XML trước đóng chứng minh app Offline, PWM0, AUTO khóa.
- Điện thoại/HTTPS A→B→C/API36/I11/hosting/khóa phân phối+backup/firmware và xe thật CHƯA KIỂM CHỨNG. Trước dùng xe hoặc release phải giải quyết lỗi hiệu năng, kiểm chứng các điều kiện mục tiêu và có quyền tiếp tục.

## Đầu ra để đọc tiếp

- [Biên bản G5](phase-5/BIEN_BAN_DUYET.md), [báo cáo bổ sung](phase-5/KET_QUA_BO_SUNG_G5.md), [quyết định phạm vi](phase-5/DIEU_CHINH_G5_AVD.md), [ma trận R01–R23](phase-5/MA_TRAN_R01_R23.md).
- [APK debug AVD cuối](phase-5/apk/sumo-phase5-avd-debug-v1-g5-check.apk),versionCode1/min36/target37, SHA256 `92abb62d00eb6f841eb8b9eb158d4b0be4b98bd39c6e05bd39b69de79613e5e3`. Bản08 giữ riêng, không phải bản phân phối điện thoại.
- `phase-5/evidence/16-g5-avd-approval/25-final-build-binding.json` ràng buộc nguồn/test/binary; `28-final-avd-handoff.txt` xác nhận APK thực cài cùng hash. Hồ sơ29 ghi quyết định và kiểm tra tài liệu/checksum cuối, không chạy thêm test.

## Tiếp nhận và xác minh độc lập

- Đã tiếp nhận dự án, nạp đủ 5 tài liệu theo thứ tự; bảo toàn toàn bộ mã nguồn, artifact APK cuối (SHA256 `92abb6...`) và quyết định G5 có điều kiện.
- Lệnh tiếp nhận `:app:testDebugUnitTest` exit0 nhưng task **UP-TO-DATE**: tái dùng kết quả110/110 cũ, không phải110 test thực thi mới. Python simulator31/31 và cadence checker6/6 đã thực thi đạt.
- 18/10: trace scheduler/Binder/ART có sẵn và chạy được qua `atrace`; đính chính nhận định trước rằng thiếu công cụ kernel. Phân rã5STOP lỗi cohort20/21 ở `phase-5/evidence/30-owner-scheduler-diagnostic/prior-outlier-partition.json`; chưa xác định nguyên nhân chung, không sửa phỏng đoán.
- Chẩn đoán trên cùng APK: STOP500 max4,1192ms (`31-owner-traced-stop/`); cadence50 n1285,max52,4731ms (`32-owner-traced-cadence50/`). Không thay FAIL cũ bằng lượt có tracing. Giả thuyết Binder mỗi poll không được ủng hộ; trace STOP có ring overwrite, cadence không overwrite. Raw nén không mất sự kiện.
- Đã chạy `python -X utf8 docs/testing/phase-5/evidence/run_android.py --output docs/testing/phase-5/evidence/33-final-functional-soak --classes Phase5RegressionTest,ControllerUiTest,SettingsUpdateTest,SoakTest`: exit0, 25/25, 1856,589 giây; audit dùng lại `analyze_soak.audit/self_check`, mọi frame/receiver/tài nguyên đạt. Không build/release/upload; APK bàn giao giữ hash ở trên.
- File đổi: checkpoint, biên bản G5, báo cáo bổ sung, ma trận R, hướng dẫn AVD, mô tả dự án; kế hoạch chỉ đồng bộ metadata/gate/kết quả, không viết lại các bước. Bằng chứng mới30–33; không đụng nguồn app/test/tool hoặc artifact cũ.
- **Chưa giải quyết STOP/cadence:** có công cụ trace nhưng hai chẩn đoán không tái hiện vi phạm; chưa đủ căn cứ sửa app. Bước tiếp: khi có giả thuyết mới, bắt mẫu lỗi đồng thời scheduler/ART trace không overwrite và mốc từng đoạn trên điều kiện mục tiêu có ghi nhận, hoặc lỗi deterministic. Thiết bị/HTTPS/API36/I11/hosting/khóa/firmware vẫn thiếu như trên; không tự chuyển phase.

## GitHub và ảnh giao diện — yêu cầu bổ sung 18/10

- Người dùng chỉ định `https://github.com/NCnguyenn/sumo.git` và cho phép đẩy dự án lên repo này, kèm ảnh toàn bộ giao diện. Đây là quyền công bố mã nguồn/tài liệu/ảnh; chưa phải quyền phát hành APK hoặc release. Quyết định G5 và lỗi hiệu năng giữ nguyên.
- Khởi tạo Git nhánh `main`, remote `origin` trỏ đúng repo trên. README gốc dẫn tới [bộ ảnh giao diện](../screenshots/README.md).
- Chụp14ảnh thật,2400×1080, trên AVD API37 với APK cuối SHA256 `92abb62d00eb6f841eb8b9eb158d4b0be4b98bd39c6e05bd39b69de79613e5e3`: D-Pad/joystick Offline/Online, Kill/mở khóa, TOGGLE, toàn bộ phần Cấu hình, xác nhận reset/nguồn cập nhật, lỗi đầu vào và hộp Cập nhật.
- `docs/screenshots/2026-10-18/capture-manifest.json` lưu hash ảnh, APK và trạng thái khôi phục. URL example.com chỉ mở xác nhận rồi Hủy, không lưu/kiểm tra từ mạng; thông báo cài đặt cũ trong ảnh cập nhật được ghi chú rõ.
- Đã sao lưu và khôi phục khớp3file SharedPreferences của AVD; kết thúc Offline,PWM0/0,B0/W0/K0. Đóng receiver loopback và AVD do phiên chụp mở. Không sửa mã app/test/Gradle, không rebuild hoặc chạy lại bộ test.
- `.gitignore` loại khóa ký, cấu hình máy, build/APK và log/trace thô lớn; toàn bộ dữ liệu gốc vẫn giữ ở máy. `.gitattributes` giữ nguyên byte phục vụ đối chiếu checksum. Không đưa APK lên GitHub trong đợt này.
