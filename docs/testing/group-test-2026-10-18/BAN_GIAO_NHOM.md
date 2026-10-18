# Bản cài thử cho nhóm — 18/10/2026

## Phạm vi đã cho phép

Người dùng yêu cầu commit, merge vào main local, push `NCnguyenn/sumo` và đóng gói APK mới để gửi nhóm. Bản này cho phép **cài thử giao diện, cấu hình, nhật ký**, theo kết luận có điều kiện của đợt phone-readiness. **Không kết nối xe**; G5 vẫn có điều kiện, G3/G4 vẫn chờ duyệt. Không tạo bản production hoặc GitHub Release.

## Thay đổi đã kiểm tra trước merge

- UI điều khiển gọn hơn, dải trạng thái cố định, Chẩn đoán riêng có nhật ký giới hạn, sao chép/xuất và lưu phiên lỗi trước; ba tab Cấu hình và màn Cập nhật.
- Bảo toàn sửa unlock: nhấc ngón sau gửi U vẫn chờ ACK; chạm khác/ra ngoài/Kill/lỗi phiên hủy theo quy tắc. ACK không chứng minh động cơ đã quay hoặc dừng.
- Hai ca Android FAIL cũ được sửa ở test: chờ output và ACK sau xử lý bất đồng bộ; ca mở updater trong macro dùng fixture1000ms để không phụ thuộc việc bắt nhãn200ms. App mặc định vẫn200ms; không nới timeout test hoặc ngưỡng hiệu năng.
- Rà soát mã độc lập trước merge không tìm thấy lỗi an toàn/bảo mật/mất dữ liệu/accessibility mới chặn phạm vi cài thử offline.

## Bằng chứng mới

| Kiểm tra | Kết quả | Giới hạn |
|---|---|---|
| Unit toàn module | **120/120**,0fail/0skip | XML và tổng hợp trong `evidence/unit-xml/`, `evidence/unit-lint-summary.json`. Không cộng với các lượt cũ. |
| Build app/test + lint | Exit0;0lỗi/17cảnh báo | `evidence/01-build-check.txt`, `evidence/lint-results.xml`. |
| AndroidSimulatorTest + Phase5RegressionTest | **10/10**,0fail/0skip | [Cohort42](../phase-5/evidence/42-group-test-regression/summary.json), thời gian70,758s. Có cả hai ca FAIL cũ. |
| Allocation idle-poll |1000gap,136.088byte,3,944s | Cùng timeout30s/cận256.000byte; chỉ thêm thống kê để chẩn đoán. Lần FAIL thiếu mẫu trước vẫn giữ, chưa xác định căn nguyên. |
| APK nhóm code2 | Build exit0; chữ ký v2 hợp lệ; zipalign16K đạt | `evidence/03-package-v2.txt`, `04-signature.txt`, `04-alignment.txt`, `04-apk-audit.json`. |
| Cài đè và mở APK nhóm trên AVD | Success; hash bản cài khớp;3prefs giữ nguyên byte | Lượt audit hoàn chỉnh xác minh cài đè **code2 lên code2**, trước/sau cài và sau mở app cùng hash từng XML. Màn Offline/PWM0/AUTO khóa: `evidence/05-v2-offline.png`. Không suy thành HTTPS hoặc cập nhật trên điện thoại. |

**Unit/lint/10Android/allocation dùng code1**, APK app **b31ac740afa3515c8cc5a2a344061e170c63a5972ca4beef2058c3ac7c5c736f**. Mã sản phẩm không đổi trong đợt bàn giao; chỉ test/hồ sơ được bổ sung. Những ca UI khác và8fault tái dùng bằng chứng cùng hash ở [phone-readiness](../phone-readiness/KET_LUAN.md), không nhận là chạy mới.

**Hai dòng APK/cài đè dùng code2**, build từ merge commit `032f260b198f26e3d064ca76385fc5db6f9a6228`. So byte ZIP: cả10DEX giữ nguyên nội dung, đổi thứ tự một số tên classesN.dex; resources và native lib giữ nguyên. Entry nội dung khác ngoài việc hoán vị DEX là AndroidManifest.xml do tăng versionCode. Bản nhóm được kiểm riêng metadata/chữ ký/cài đè, không nhận là chạy mới toàn bộ suite trên code2.

Ba lần chuẩn bị audit dừng tại kiểm tra của công cụ: giả định tên DEX không đổi, đòi toàn bộ stdout adb phải chỉ là `Success`, và UIAutomator trả `null root` ngay sau launch. Log04/05/06 giữ nguyên. Lần07 dùng so hash tập DEX, nhận dòng cuối Success và lấy XML mới có retry giới hạn như helper cũ: **exit0**, không đổi sản phẩm để xử lý các lỗi công cụ này. Lượt05 đã cài APK mới nhưng dừng trước hoàn tất audit; không gọi bộ hash prefs lượt07 là bằng chứng lần đầu1→2.

## APK và cài đặt

- Tệp: `apk/sumo-controller-ui-test-1.0-v2.apk`; **30.943.197byte (~29,51MiB)**. APK bàn giao riêng, được Git bỏ qua. [Checksum](apk/SHA256SUMS.txt), [audit/binding](evidence/04-apk-audit.json).
- SHA256: `40b22dbee250894bae9d7a0f9b3f0beb0fdac7dbd3af9aed259267af9c777078`.
- Yêu cầu **Android16/API36 trở lên**. Có lib ARM64/ARMv7/x86/x86_64 trong bản trước; vẫn cần thử thực tế trên máy của nhóm.
- Package `com.aistudy.myapplication`, versionName1.0, **versionCode2**.
- Giữ signer debug hiện tại, cert SHA256 `d97c9cee077a242672a3d3b19276d6c9d76983ce862c59b0268b9a154c2915ba`. Đây là khóa debug thử nghiệm, chưa phải khóa phân phối production. Các bản thử tiếp phải giữ cùng signer và tăng versionCode; không tự chuyển khóa làm người dùng phải gỡ app.

1. Gửi nguyên tệp APK cho nhóm qua kênh tin cậy, kèm hướng dẫn này và SHA256SUMS.txt. Trên điện thoại kiểm phiên bản Android trước.
2. Tải APK về, mở bằng trình quản lý tệp. Cho phép nguồn đó cài ứng dụng khi Android hỏi rồi xác nhận cài; không cần USB.
3. Nếu đã có app, chọn cập nhật/cài đè. Khi báo chữ ký khác hoặc phiên bản đang cài cao hơn, dừng và báo lại; **không gỡ app hoặc xóa dữ liệu** để lách lỗi.
4. Mở app ngang, kiểm Offline/PWM0/0/AUTO khóa. Thử D-Pad/joystick khi Offline, ba tab cấu hình, Lưu/Hủy/validation, đóng mở app giữ cấu hình, Chẩn đoán/sao chép/xuất. Không nối xe.
5. Gửi lỗi kèm model máy, Android, bản app, thao tác tái hiện, ảnh và file nhật ký. `10.0.2.2` chỉ dùng trên AVD, không dùng làm IP receiver của điện thoại.

Hiện gửi APK thủ công rồi cài đè trên điện thoại, không cần USB. Nút Cập nhật trong app chưa có kênh HTTPS thật đã kiểm chứng; không coi việc gửi file APK là đã hoàn thành tính năng cập nhật trong app.

Đóng gói bằng Gradle hiện có, không thêm dependency/SDK hoặc tạo khóa mới:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_TOOL_OPTIONS='"-Djdk.net.unixdomain.tmpdir=C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT\.gradle"'
.\gradlew.bat :app:assembleDebug -PsumoVersionCode=2 --console=plain
```

## Giới hạn giữ nguyên

STOP từng vượt10ms; cadence50 từng vượt60ms ở điều kiện khác. Không chạy thêm batch tìm lượt đẹp, không đóng FAIL cũ. Soak30phút chưa đạt trên UI cuối. Điện thoại ARM/API36, UDP điện thoại, HTTPS A→B→C, I11, hosting, khóa phân phối/backup, firmware và xe thật còn chưa kiểm chứng. Bản debug này phục vụ cài thử, chưa đủ điều kiện điều khiển xe hoặc nghiệm thu toàn bộ ứng dụng.
