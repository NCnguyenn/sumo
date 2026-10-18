# Phân phối APK test và các điều kiện còn thiếu

`applicationId` giữ `com.aistudy.myapplication` theo G0 đã duyệt. APK debug cục bộ chỉ dùng AVD, ký bằng Android Debug hiện có; không dùng khóa đó làm chuỗi phân phối cho điện thoại. Chưa có hosting HTTPS, khóa riêng/backup và điện thoại đã xác nhận nên chưa upload hoặc nghiệm thu A→B→C qua Internet.

## Build cục bộ và build phân phối

Giữ JBR/Gradle/SDK như kế hoạch. APK dùng AVD:

~~~powershell
./gradlew.bat --offline :app:assembleDebug --console=plain
~~~

Build cho kênh test điện thoại cần chủ dự án cung cấp bốn biến môi trường **ngoài repository**, không in giá trị vào log:

| Biến | Nội dung |
|---|---|
| `SUMO_KEYSTORE` | Đường dẫn keystore đã tồn tại ngoài thư mục dự án |
| `SUMO_KEY_ALIAS` | Alias khóa đã chốt |
| `SUMO_STORE_PASSWORD` | Mật khẩu keystore |
| `SUMO_KEY_PASSWORD` | Mật khẩu khóa |

Lệnh phân phối, thay N bằng số nguyên lớn hơn mọi bản đã công bố của cùng identity, tối thiểu 2:

~~~powershell
./gradlew.bat --offline :app:assembleDebug -PsumoDistribution=true -PsumoVersionCode=N --console=plain
~~~

Không chạy nguyên chữ N. Gradle tự đặt tên hiển thị phiên bản `0.1.0-test.N`; thiếu khóa/alias/mật khẩu, version không hợp lệ hoặc keystore nằm trong dự án thì build thất bại, không fallback sang khóa emulator. Đường ra vẫn là `app/build/outputs/apk/debug/app-debug.apk`; sao chép sang tên/đường dẫn phiên bản trước công bố, không ghi đè byte khác dưới version đã phát hành. Không chạy `assembleRelease` trước G6. Alias, chủ quản, backup mã hóa và fingerprint tin cậy còn phải chốt với chủ dự án trước phân phối đầu.

## Kiểm tra và công bố

1. Chạy unit/lint/smoke phù hợp; ghi lỗi còn lại, nhất là các ngưỡng thời gian chưa đạt. Không dùng test mẫu làm bằng chứng điều khiển.
2. Kiểm tra chính APK định phát hành bằng `aapt2 dump badging`, `apksigner verify --verbose --print-certs` trong Build Tools 36.0.0 và SHA-256. Đối chiếu package, versionCode, versionName, minSdk, kích thước, cert với khóa đã chốt; không chỉ nhìn “verify success”.
3. Chủ dự án cung cấp host HTTPS/URL metadata/đích upload/quyền phù hợp. Upload APK vào đường dẫn phiên bản trước, tải lại đối chiếu đúng kích thước/hash, rồi mới công bố metadata.
4. Metadata JSON schemaVersion=1 gồm applicationId, versionCode, versionName, minSdk, apkUrl, sizeBytes, sha256, releaseNotes, channel=`test`. Lấy giá trị từ APK thật, không từ tên tệp. Feed ≤64 KiB; APK ≤100 MiB; APK và mọi redirect phải HTTPS/cùng host đã duyệt, tối đa 3 redirect, timeout kết nối 10 s/đọc 30 s. Chưa có URL thật thì không tạo một feed trông như đang hoạt động.
5. Trên điện thoại, lần đầu cài từ liên kết HTTPS; sau đó vào Cấu hình nhập URL feed, xác nhận nguồn mới. Cập nhật → Kiểm tra → Tải → Cài qua Android. Android có thể yêu cầu cho phép cài từ app này và xác nhận cài; quay lại nhấn Cài sau khi cấp quyền. Không gỡ app trước, không vượt kiểm tra chữ ký hoặc hạ version.
6. Trước mở updater, app kết thúc phiên lái, hủy input/macro và chờ ACK đóng hoặc deadline watchdog nếu không xác nhận được. Đây không phải xác nhận dừng vật lý. Sau cập nhật luôn Offline/trung tính, không tự nối hoặc phát lại chuyển động.
7. Ghi video A→B→C và version thật sau mỗi lần mở lại; so cấu hình trước/sau và migration. `adb install` trên AVD hoặc fixture installer cục bộ chỉ chứng minh phần tương ứng, không thay cho A10/A14 trên điện thoại qua Internet.

Nguồn kỹ thuật nền tảng: [PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller) và [cài đặt có xác nhận](https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)). Updater kiểm tra APK bằng API package của Android và đối chiếu signer đang cài; test sửa DEX trên AVD API37 đã bị từ chối trước phiên cài. Android vẫn kiểm tra lại khi cài. Chưa có bằng chứng tương ứng trên API36/điện thoại thật.

## Dữ liệu và thông tin cần bổ sung

Settings dùng một record đã validation, schema1; migration schema0 giữ giá trị có sẵn, thêm mặc định cho trường mới và giữ bản gốc phục hồi. Schema không hỗ trợ/hỏng không bị xóa; app khóa điều khiển và báo lỗi. Kill latch lưu riêng để không mất khi tạo lại màn hình; pointer, Boost, macro, chế độ AUTO đang chạy và SID không được khôi phục.

Còn thiếu: hosting/đích upload thực, khóa riêng và người giữ/backup, danh sách version đã công bố, điện thoại/phiên bản Android thực tế. Không gửi mật khẩu hoặc token vào chat/repository. Hoàn tất các mục này rồi mới thực hiện vòng phân phối điện thoại; hồ sơ G3/G4 hiện chỉ trình duyệt phần có bằng chứng, không ghi R23 đã nghiệm thu.
