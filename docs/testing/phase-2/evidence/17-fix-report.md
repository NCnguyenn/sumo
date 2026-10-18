# Sửa hai lỗi từ rà soát độc lập Phase 1+2

Ngày 17/10/2026. Tổng hợp từ nguồn hiện tại, log lệnh và XML đã lưu; không thay thế các kết quả lịch sử. Phạm vi vẫn là Phase 1+2, G1/G2 chờ người dùng duyệt chung.

## Lỗi D-Pad với mức quay thấp

Nguyên nhân: đường D-Pad dùng chung bước chuẩn hóa bán kính dành cho joystick. Với limiter 60%, turnPercent 30%, Tiến + Phải thành `(141,76)` thay vì `(153,82)`; Tiến + Trái thành `(76,141)` thay vì `(82,153)`.

Đã thêm tham số `inputMode` vào `DriveMixer.mix`, mặc định JOYSTICK để giữ caller cũ. D-Pad bỏ bước sensitivity/dead zone/chuẩn hóa bán kính, giữ vector trục đầy đủ; hai chế độ tiếp tục dùng chung phép trộn bánh, gain, limiter/Boost và làm tròn. Reducer truyền D_PAD tại đúng caller.

Ba test mới trong `ControlReducerTest`: `F03_dpadForwardRightUsesFullAxesWithLowTurnCalibration`, `F03_dpadForwardLeftUsesFullAxesWithLowTurnCalibration`, `F03_joystickDiagonalsKeepRadialNormalizationWithLowTurnCalibration`. Hai ca D-Pad kiểm tra cả cấu hình sensitivity/deadZone mặc định và khác mặc định; ca joystick giữ đầu ra radial `(141,76)/(76,141)`.

## Lỗi ACK khi PWM đổi nhanh hơn RTT

Nguyên nhân: `ReplyTracker` loại ACK của ý định cũ trước khi ghi nhận tiến triển liên lạc. Nếu gửi mỗi 30 ms, RTT 50 ms và PWM đổi mỗi lần gửi, phiên có phản hồi hợp lệ vẫn có thể hết hạn ở 301 ms.

Đã giữ validation nguồn/SID/seq/tuổi và đối chiếu state với frame thực đã gửi. ACK hợp lệ tăng thứ tự được ghi nhận vào tiến triển/RTT và tiêu thụ seq trước khi lọc `intentFloor`. ACK thuộc ý định cũ vẫn trả `null`, không chuyển state cũ đến reducer; vì vậy không xác nhận sớm STOP/Kill/unlock/mode/macro hoặc đóng phiên do ACK Close đã bị thay thế. Config ACK vẫn không duy trì watchdog liên lạc điều khiển.

Ba test mới trong `ReplyTrackerTest`: `C05_changingDriveAcksKeepSessionAliveWithoutConfirmingSupersededState`, `C05_supersededMismatchedAckCannotMaintainProgress`, `C05_supersededSafetyAcksOnlyAdvanceCommunication`. Luồng PWM đổi kéo dài đến t=450 ms với RTT 50 ms; phiên vẫn Online qua 301 ms và chỉ hết hạn 300 ms sau ACK cuối, tại t=750 ms. Reply sai state/peer/seq chưa gửi không tạo tiến triển. Bảng tình huống riêng bao phủ ACK cũ của di chuyển, STOP, Kill, unlock, AUTO, macro và Close.

## Bằng chứng red/green

Các lệnh chạy ở gốc workspace, dùng JBR25 và workaround Unix socket chỉ trong process như hồ sơ G0. Lệnh đầy đủ và exit code nằm đầu/cuối từng log; native output được lưu trực tiếp, không dựng lại từ tóm tắt.

| Lượt | Lệnh Gradle sau `--offline :app:testDebugUnitTest` | Kết quả |
|---|---|---|
| [17a](17a-p1-red.txt) | `--tests com.aistudy.myapplication.control.DriveMixerTest --tests com.aistudy.myapplication.control.ControlReducerTest --console=plain --rerun-tasks` | Exit 1; 36 test, 2 fail đúng PWM D-Pad, 0 error/skip; [XML](17a-p1-red-xml/) |
| [17b](17b-p1-green.txt) | Cùng bộ lọc 17a | Exit 0; 36/36 đạt sau sửa; [XML](17b-p1-green-xml/) |
| [17c](17c-p2-red.txt) | `--tests com.aistudy.myapplication.protocol.ReplyTrackerTest --tests com.aistudy.myapplication.protocol.CommandCodecTest --tests com.aistudy.myapplication.protocol.SimulatorInteropTest --console=plain --rerun-tasks` | Exit 1; 20 test, 2 fail đúng RTT/progress, 0 error/skip; [XML](17c-p2-red-xml/) |
| [17d](17d-p2-green.txt) | Cùng bộ lọc 17c | Exit 0; 20/20 đạt, gồm UDP Kotlin/Python; [XML](17d-p2-green-xml/) |
| [17e](17e-module-green.txt) | Thêm `:app:lintDebug --console=plain --rerun-tasks`, không bộ lọc | Exit 0; toàn module 60/60 đạt; [XML](17e-module-green-xml/), [lint](17e-lint.xml) 0 error/12 warning baseline |
| [18](18-final-gradle.txt) | Thêm `:app:lintDebug --console=plain --rerun-tasks`, không bộ lọc | Root chạy xác minh cuối: exit 0; 60/60 đạt; [XML](18-final-xml/), [lint](18-lint.xml) 0 error/12 warning baseline |

Chỉ năm tệp nguồn/test thay đổi so với snapshot trước review: DriveMixer.kt, ControlReducer.kt, ReplyTracker.kt, ControlReducerTest.kt và ReplyTrackerTest.kt. [Diff](18-review-diff.txt) thể hiện đúng phạm vi này. Không đổi Python/codec/fixture/Gradle hoặc thêm dependency. Các phép kiểm tra simulator 31/31 và crosscheck 25/25 của lượt 16 vẫn áp dụng cho nguồn Python/codec/fixture không đổi; test UDP liên tiến trình chạy lại trong suite 18.

Đây là kiểm tra lõi/giao thức/simulator. Chưa kiểm chứng UI Android, lịch gửi thực, STOP 10 ms, firmware, cập nhật APK hay xe thật; không tạo release hoặc chuyển Phase 3.
