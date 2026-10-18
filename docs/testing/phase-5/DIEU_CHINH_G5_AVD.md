# Điều chỉnh phạm vi nghiệm thu G5 trên AVD

Ngày17/10/2026. Người dùng đã chọn **“Duyệt phương án này để tiếp tục kiểm tra”** trong chat, trước lượt đo mới. Đây là duyệt phạm vi/tiêu chí, chưa phải xác nhận đã xem kết quả mới.

## Phạm vi và cận đo được duyệt

G5 đợt này nghiệm thu phần mềm trên AVD API37 với SUMO-SIM/1-draft1. Giữ nguyên cấu hình và lịch danh nghĩa20/30/50ms.

| Cấu hình | Cận mọi interval định kỳ | Số mẫu tối thiểu |
|---|---|---:|
|20ms|20–50ms|1000|
|30ms|20–50ms|1000|
|50ms|20–60ms, cho phép jitter tối đa+10ms riêng AVD|1000|

STOP500 vẫn dùng t0 callback app, t1 trước send, t2 sau send; mọi t1−t0 và t2−t0 phải≤10ms. Kill, ưu tiên STOP/Brake, watchdog300ms, seq/ACK/replay và chống phục hồi chuyển động không thay đổi. Không bỏ outlier hoặc dùng phân vị thay max.

## Cách chấm

`tools/check_android_cadence.py` mặc định vẫn là `G0_STRICT`, mọi cấu hình20–50ms. Thêm cờ `--g5-avd-50-jitter` để chọn rõ `G5_AVD_50_PLUS_10`. JSON giữ số mẫu `above_50` để đối chiếu cận cũ, bổ sung `upper_limit_ms` và `above_limit`. CSV luôn giữ mọi interval.

Chỉ dùng lượt đo mới sau khi được duyệt để kết luận G5 theo tiêu chí này. Hồ sơ lỗi cũ, đặc biệt09-cadence và các lượtPhase4-32/33/34, không đổi kết luận. Công cụ mặc định phải tiếp tục báo FAIL với626mẫu>50ms của lượt08.

## Những mục tiếp tục chưa kiểm chứng

Người dùng đồng ý để điện thoại, HTTPS A→B→C, API36, I11 (LAN còn route khi mất Internet) và xe/firmware thật ở trạng thái CHƯA KIỂM CHỨNG. Khóa ký phân phối/backup và hosting chưa được xác nhận. Không biến các mục này thành PASS hoặc coi bản debug là bản dùng thi đấu.

Điều chỉnh này chỉ áp dụng G5 trong phạm vi AVD, không duyệt G3/G4, không sửa tiêu chí nghiệm thu hệ thống G6, không cho phép Phase6/7/release/upload. Kết quả mới được báo riêng; người dùng vẫn duyệt kết quả sau khi nhận hồ sơ.

## Trạng thái thực hiện

Phạm vi/tiêu chí đã được duyệt; cohort16 đầu tiên sau duyệt cho cadence đạt cận mới nhưng phát hiện STOP2/500>10ms. Sau đó sửa cấp phát snapshot và bỏ CPU sampler tạo build17/20; hai thí nghiệm môi trường18/21 kiểm tra ưu tiên/power policy của QEMU, đều khôi phục trạng thái gốc. Mọi cohort và lý do thực hiện được giữ, không chấm lại hồ sơ lỗi cũ hoặc bỏ mẫu. Cohort21 vẫn STOP2/500>10ms,max11,4174ms; trước quyết định cuối chưa đủ điều kiện G5 đạt toàn bộ. Chi tiết tại [kết quả bổ sung](KET_QUA_BO_SUNG_G5.md).

## Quyết định chốt cuối — G5 có điều kiện

Sau khi nhận kết quả mới, người dùng chọn **“Chốt G5 CÓ ĐIỀU KIỆN cho chức năng AVD; giữ lỗi hiệu năng mở, chặn dùng xe và release”** (chat17/10/2026). Vì vậy G5 chốt có điều kiện về chức năng AVD; STOP/cadence không được đổi thành PASS, ngưỡng không nới thêm. G3/G4 không được tự duyệt. Không tự chuyển Phase6/7 hoặc dùng xe/release khi lỗi hiệu năng và các điều kiện phần cứng chưa giải quyết. Đây là quyết định phạm vi/gate có ghi nhận, không phải bằng chứng người dùng đã trực tiếp thử app hoặc nghiệm thu hệ thống thật.
