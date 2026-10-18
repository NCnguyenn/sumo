# Áp dụng giao diện compact và Chẩn đoán

> Thực hiện trong phiên hiện tại theo workflow executing-plans/TDD. Người dùng đã duyệt bộ mockup v3 và yêu cầu triển khai, chụp giao diện thật; không mở lại vòng duyệt thiết kế.

**Đầu ra:** bố cục Compose theo `docs/mockups/2026-10-18/v3-compact-diagnostics`, màn Chẩn đoán đọc dữ liệu thật và bộ ảnh từ APK debug mới trên AVD.

**Ràng buộc:** giữ reducer/mixer, đa pointer, STOP/Kill/Brake, AUTO khóa và validation/cài đặt APK. Không đổi Gradle/dependency/protocol, không tạo release/upload APK. G5 có điều kiện và lỗi hiệu năng cũ vẫn mở. Không coi ACK là chuyển động cơ học. Không dùng dữ liệu minh họa trong giao diện chạy thật.

- [x] Nhật ký: test trước cho giới hạn dung lượng, thống kê TX/RX, loại phản hồi, ACK cũ, lưu phiên lỗi trước và xuất báo cáo. Dùng callback trace hiện có ngoài luồng sender; ghi/lưu không chặn control thread. File `diagnostics/DiagnosticLog.kt`, test `diagnostics/DiagnosticLogTest.kt`; bổ sung lý do loại bỏ trong `ReplyTracker` và trace `ControlSession`, giữ nguyên điều kiện chấp nhận.
- [x] Giao diện: bổ sung Android test dải cảnh báo giữ vị trí nút và mở/đóng Chẩn đoán an toàn. Sửa `ControllerScreen`, `SettingsDialog`, `AppUpdateDialog`, theme; thêm `DiagnosticsScreen`; nối `MainActivity`, clipboard và xuất bằng Android CreateDocument. Các trường cấu hình chia tab nhưng vẫn lưu toàn bộ nguyên tử; cập nhật test gọi trường chuyển tab.
- [x] Kiểm chứng: chạy unit module, lint/build debug và Android UI/Settings/UDP/regression phù hợp trên AVD; kiểm tra phiên lỗi/thống kê/export/bounded log. Chỉ đo hiệu năng thêm một batch trên binary mới nếu harness hiện có cho phép, giữ mọi FAIL và không tuyên bố đã sửa lỗi cũ.
- [x] Chụp D-Pad/joystick, cảnh báo ACK thật, Chẩn đoán hiện tại/phiên lỗi trước, ba tab cấu hình và cập nhật. Lưu PNG/XML/manifest, lệnh/exit code/XML test trong `docs/testing/ui-compact-2026-10-18`, gallery trong `docs/screenshots/ui-compact-2026-10-18`; cập nhật checkpoint và báo giới hạn binary mới.

Kiểm thử: `gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`; Android chạy bằng runner hiện có với danh sách class cụ thể, không chạy lại soak 30 phút chỉ để chụp UI. Lỗi mới phải sửa trong phạm vi trước bàn giao.
