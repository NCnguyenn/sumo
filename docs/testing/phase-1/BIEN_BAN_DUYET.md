# Kết quả kỹ thuật Phase 1 và quyền chuyển tiếp

**Quyết định hiện hành:** Người dùng đã duyệt G1/G2 theo hồ sơ cuối 60/60 và cho phép tiếp tục Phase 3+4 trong một đợt. Các dòng “chờ duyệt” bên dưới giữ lịch sử trước quyết định đó, không thay số liệu hoặc gán thêm kết quả mới vào lượt kiểm tra cũ. G3/G4 sẽ trình riêng; không tự chuyển Phase 5.

Ngày 17/10/2026, Asia/Bangkok. G0 đã được người dùng duyệt theo chat, gồm D01–D11/5.2.1–5.2.2. G1 được kiểm tra kỹ thuật trong đợt Phase 1+2; người dùng ủy quyền tự chuyển Phase 2 khi đủ tiêu chí, **chưa xác nhận đã xem kết quả G1**. Trình duyệt chung sau Phase 2, không vào Phase 3.

## Phạm vi và kết quả

Đã tạo lõi Kotlin thuần trong `control/` và `settings/`: DriveMixer, WheelPwm, ControlState, ControlReducer, ControllerSettings/DriveCalibration. Không Android/socket trong lõi; clock monotonic có thể truyền vào để kiểm tra hold unlock. Macro do receiver thực thi timer; lõi giữ ID/duration đến ACK hoàn tất/hủy, không giả báo motor đã xoay. Các phương thức ACK là sự kiện sau xác thực; kiểm tra raw reply/SID/seq thuộc Phase 2, nối transport thực thuộc Phase 4.

G1 tại lượt 06: `06-g1-final.txt`, exit 0, **37 unit tests, 0 failure/error/skip**: mixer 8, reducer 25, settings 3 và test mẫu 1. **36 test chức năng** ở thời điểm đó, không dùng test mẫu để kết luận L/F. Lint exit 0, **0 error, 12 warning có từ baseline**. XML giữ nguyên tại `evidence/06-final-xml/`, lint tại `evidence/06-lint.xml`. Sau rà soát độc lập, phần G1 tăng lên **39 test chức năng** (mixer 8, reducer 28, settings 3), tất cả đạt trong lượt 18 toàn module. Kết quả phần mềm, chưa đo STOP 10 ms trên Android hoặc hành vi xe thật.

## Red trước green

| Lượt | Lệnh Gradle (đều `--offline :app:testDebugUnitTest --console=plain`) | Kết quả và bằng chứng |
|---|---|---|
| 01 | Thêm `--tests *.DriveMixerTest --tests *.ControllerSettingsTest --rerun-tasks` | Exit 1; 11/11 fail vì stub TODO chưa triển khai; compile thành công, lỗi JUnit chứ không lỗi cú pháp. `01-mixer-settings-red.txt`, `01-red-xml/` |
| 02 | Cùng hai bộ lọc, không rerun | Exit 0; 11/11 pass, `02-mixer-settings-green.txt`, `02-green-xml/` |
| 03 | Toàn module | Exit 1; 36 chạy, 25 fail: reducer 24 stub chưa triển khai; settings 1 bắt wildcard `000.000.000.000`; 11 pass. `03-reducer-red.txt`, `03-red-xml/` |
| 04 | Toàn module, thêm `:app:lintDebug` | Exit 0; 36/36 pass, lint 0 error/12 warning; `04-core-green.txt`, `04-green-xml/` |
| 05 | `--tests *.ControlReducerTest.newSessionResetsTurnIdSpaceAndKeepsConfiguredDuration` | Exit 1; 1/1 fail đúng expected ID1 vs actual ID3 khi phiên mới; `05-turn-session-red.txt`, `05-red-xml/` |
| 06 | Toàn module, thêm `:app:lintDebug` | Exit 0; 37/37 pass sau reset miền ID mỗi phiên; `06-g1-final.txt`, `06-final-xml/` |

Môi trường: JBR 25.0.2, Gradle 9.5.0/AGP 9.3.3, không đổi build config/dependency. Các lệnh dùng `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` và `JAVA_TOOL_OPTIONS='"-Djdk.net.unixdomain.tmpdir=C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT\.gradle"'` chỉ trong process. Native output được Tee-Object trực tiếp; mỗi log có command và exit code. Chỉ lượt 01 có `--rerun-tasks`. Không sleep để kiểm tra logic.

## Ánh xạ bằng chứng

Tên hàm trong XML là định danh từng test chạy thật, không phải bảng kế hoạch tự tính pass.

| Mã yêu cầu test | Test thực trong XML / điểm đã kiểm tra |
|---|---|
| L01–L03, F01 | `DriveMixerTest.L01_L03_F01_limitsAndSymmetricRounding`, `L02_pivotsHaveCorrectSigns`; ±77/153/255, dấu bánh |
| L04–L08 | Mixer `L04_L05_boostPreservesRatioAndNeverCreatesDirection`; reducer `L04_L07_L19_*`, `L08_*`, `L05_L06_L20_*`; Boost độc lập/hướng giữ/dừng |
| L09–L10 | Mixer `L09_L10_*`, `L10_nonFinite*`; reducer `L09_L22_*`; tâm, chéo, clamp/NaN/Infinity, PWM không vượt ±255 |
| L11/L21 | Reducer `L11_L21_hold*`, `L11_L21_toggle*`; giữ W sau nhả hướng, HOLD up/toggle down đúng chế độ |
| L12 | `L12_brakeOverridesBoostWeaponAndRequiresOldFingersUp`; K ưu tiên, hủy W/B, không chạy lại sau nhả |
| L13–L14 | `L13_L14_*`, `L13_killAgain*`, `L14_reconnect*`; Kill latch, ngón cũ, giữ 1000 ms, ACK lỗi/trễ, reconnect trung tính |
| L15 | `L15_cancel*`, `L15_auto*`, `L15_brake*`; cancel/mode, AUTO capability + phê duyệt + trung tính + ACK, Brake ưu tiên |
| L16 | `L16_macroUses*`, `L16_killBrake*`, `L16_macroRequires*`; ID một lần, ACK phù hợp, hủy bởi Kill/Brake/cancel/disconnect/hướng mới |
| L17–L18 | `L17_opposed*`, `L18_adjacent*`; 0 sau nhả, tick/move không phục hồi, chờ nhấc hết/down mới |
| L19–L20 | `L04_L07_L19_*`, `L05_L06_L20_*`; nhả Boost giữ 153; nhả hướng B=0 đến down hướng mới |
| L22 | `L22_earlyDown*`, `L22_leavingDpad*`, `L09_L22_*`; mất vùng/joystick, ID không liên tiếp, chạm sớm, move/tick sau nhấc cũ vẫn 0 |
| F01 lõi | `F01_liveLimiterUsesSameReducerAndKeepsGesture`; một reducer, thay 30/60/100 không build lại khi chạy test |
| F03 lõi | Mixer `F03_deadZone*`, `F03_sensitivity*`; reducer `F03_advanced*`, `F03_dpad*`; settings bounds/NaN, invalid giữ cấu hình cũ |
| Hồi quy miền ID | `newSessionResetsTurnIdSpaceAndKeepsConfiguredDuration`; duration350/ID theo SID mới |

## Rà soát G1 và chuyển tiếp

- L01–L22/F01/F03 lõi có assert đầu ra và test chạy thật, không dùng UI/fake preview làm bằng chứng. Không có ca skip.
- Nhả điều hướng vô hiệu đúng nhóm cũ; chạm sớm giữ/move không tự có hiệu lực; pointer vũ khí không bị xóa bởi nhả hướng. Kill/Brake/cancel/reconnect có đường an toàn riêng.
- Giữ validation và kiểu thuần Kotlin, không thêm dependency hoặc lớp Android vào lõi. Clock giả dùng cho unlock; macro timer độc lập sẽ được kiểm chứng bên simulator.
- Đã rà soát nguồn và sửa lỗi ID không reset khi mở phiên mới bằng red/green riêng. Không có Git, dùng checksum nguồn/evidence để định danh, không tạo commit hoặc worktree.

**Kết luận kỹ thuật:** Đủ điều kiện chuyển Phase 2 theo ủy quyền riêng của người dùng cho đợt Phase 1+2. Không phải người dùng đã duyệt kết quả test G1. **Quyết định nghiệm thu chung G1/G2:** chờ người dùng xem hồ sơ sau Phase 2. Không cấp quyền Phase 3 hoặc release.

Rà soát độc lập sau Phase 2 phát hiện D-Pad chéo dùng nhầm chuẩn hóa bán kính joystick khi turnPercent=30. Đã bổ sung hai ca D-Pad exact PWM `(153,82)/(82,153)` và một ca giữ cách tính riêng của joystick. Lượt 17a: 36 test mixer/reducer, 2 fail đúng nguyên nhân; 17b: 36/36 đạt sau sửa. [Báo cáo sửa và red/green](../phase-2/evidence/17-fix-report.md) lưu nguồn, log và XML, không thay thế lịch sử lượt 06.

[Rà soát lại độc lập](../phase-2/evidence/18-independent-rereview.md) đã đóng phát hiện D-Pad và ACK, không có vấn đề mới trong phạm vi các thay đổi được xem xét. Đây là đánh giá kỹ thuật, không thay phê duyệt người dùng.

Hồi quy cuối đợt: [18-final-gradle.txt](../phase-2/evidence/18-final-gradle.txt), [XML](../phase-2/evidence/18-final-xml/) xác nhận toàn module **60/60 pass**, gồm 39 ca chức năng G1, 20 ca chức năng G2 và 1 mẫu; lint 0 error/12 warning trùng baseline. Lượt 13 lịch sử 54/54 và số 37 của lượt G1 vẫn giữ theo thời điểm chạy. [Hồ sơ G2](../phase-2/BIEN_BAN_DUYET.md) ghi kết quả codec/simulator và giới hạn. **Hiện dừng trước Phase 3, G1/G2 CHỜ DUYỆT CHUNG**.
