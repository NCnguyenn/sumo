# Sửa hiệu năng lượt32 — một đợt đo sau sửa, giữ nguyên cậnG0

Các log, XML, CSV/JSON và script bổ sung nằm cùng thư mục hồ sơ này. Đã rà Runtime/Session/UDP và các caller, thêm kiểm tra lịch bằng clock điều khiển và logging bằng latch trước triển khai. `01-schedule-logging-red.txt`/`01-red-xml`:4 test,3 fail đúng nguyên nhân (mốc30 trôi33→63; send30 kéo dài đến65 gây pump gửi bù; logger giữ STOP). Test floor20 có sẵn trong lượt này xanh làm đối chứng. `02-buffer-red.txt`/XML:1 fail trước thêm biên direct buffer, không gọi đây là nguyên nhân lịch. `03-related-check.txt` là lỗi compile thiếu import assert, đã sửa; không tính như red chức năng.

`ControlSession` giữ deadline nanosecond monotonic theo mốc gốc, bỏ slot đã qua sau send; không lấy `now+period`. `ControlRuntime` đợi phần deadline còn lại bằng nanosecond, trần poll ACK2ms; tái dùng batch và tránh snapshot thừa khi kiểm tra idle. Tick thường có floor20ms từ khi send trước hoàn tất, nên guard có thể đẩy tick20 muộn; không hứa chính xác20ms. STOP/Kill/Brake bỏ qua floor, gửi trên cùng owner, không phát bù burst hoặc tạo sender thứ hai. Seq và khóa pointer cũ vẫn được kiểm tra.

Logcat và định dạng/hex của bản ghi wire chạy ở thread riêng; chuỗi log sự kiện/hex receive của Session vẫn được tạo trên owner. Đã loại việc chờ sink Logcat khỏi owner, chưa loại mọi cấp phát chẩn đoán. Hàng chẩn đoán có giới hạn8192; flush trong hai test phải xác nhận không mất record, nếu mất thì test fail. UDP dùng direct send buffer256 tái sử dụng, không cắt frame quá cỡ. Main chỉ nối CPU clock/flush cho phép đo; không đổi UI, cấu hình, codec, ACK contract hoặc Gradle.

`05-final-related-check.txt`/`05-related-xml`: **31/31 JUnit**, exit0 (Scheduling5, Session18, Runtime3, UDP4, Trace1); build debug/androidTest đạt. Có nanosecond remaining-wait, bỏ slot quá hạn, floor20, ưu tiên STOP/Kill/Brake, seq liên tiếp, pointer cũ, logger chậm và exact-length buffer. `07-one-timing-batch.txt`/`07-android-results`: **2/2 Android**, exit0,3m44s; vẫn500 release và giữ28/40/64 giây tại20/30/50ms như phép đo gốc. Chỉ thêm stage/CPU/heap và flush log, không đổi kích thích, bộ lọc hoặc ngưỡng. Hai script chấm gốc khớp SHA trước sửa (`08-acceptance-scripts-unchanged.json`).

| STOP,500 mẫu | p50 sau(ms) | p95 | p99 | Max trước → sau | >10ms trước → sau |
|---|---:|---:|---:|---:|---:|
| t1−t0 |0.6255|1.6752|2.4044|8.9006 →3.3828|0 →0|
| t2−t0 |1.0112|2.1304|2.9897|13.2437 →3.5330|1 →0|

STOP **ĐẠT trong cohort này**, `08-stop-check.txt` exit0, CSV đủ500;1481 interval30ms phụ đều trong20–50ms. Không thay bằng chứng cadence chuyên biệt. Mẫu STOP chậm nhất201 phân rã(ms): post0.0153 + chờ worker1.0117 + xử lý event0.1372 + encode0.1118 + tracking1.1284 + send1.1286 =3.5330. Max riêng chờ worker2.8863, xử lý1.3916, encode1.0368, send1.7461; các max này thuộc các mẫu khác nhau.

| Cadence sau sửa | n | Min(ms) | p50 | p95 | p99 | Max | <20ms | >50ms | Kết luận |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
|20ms|1293|20.4793|21.6666|22.9286|23.4023|25.8384|0|0|Đạt cohort|
|30ms|1337|21.7866|29.9861|31.3657|31.9416|34.0212|0|0|Đạt cohort|
|50ms|1281|32.6513|49.9940|51.6336|52.1065|60.1096|0|637|**CHƯA ĐẠT**|

`08-cadence-check.txt` exit1; CSV giữ3911 interval, gồm cả interval đầu32.6513ms và mọi mẫu chậm. Không chạy batch thứ hai, không chọn cohort hoặc nới cận. `analyze_stages.py` chỉ bổ sung chẩn đoán, tự kiểm tra tổng các đoạn và đẳng thức interval = bước deadline + chênh lateness; không thay hai script nghiệm thu. `08-stages-check.txt` là lỗi trùng key trong script bổ sung; sửa script rồi phân tích lại đúng log đã thu ở `09-stages-check.txt`/`10-stage-investigation.txt`, exit0, không chạy lại phép đo.

**Phân biệt mã và môi trường:** tại50ms,1280/1281 bước deadline đúng50ms; bước đầu32.8488ms do floor sau event, giữ nguyên trong thống kê. p50 interval49.9940ms thay51.1289ms trước sửa: lỗi trôi lịch đã tái hiện/sửa bằng clock, nhưng không loại được jitter. Mẫu954: interval51.7860ms, worker thức dậy muộn2.7807ms; poll yêu cầu0.3525ms nhưng thực tế3.1135ms, xử lý trước send0.2444ms (CPU0.231557ms), encode0.1188ms, send0.1591ms. Đây là bằng chứng chậm trong chờ/wakeup trên môi trường đo, không phải tất cả đều encode/send lâu.

Mẫu1201 lớn nhất60.1096ms: worker muộn1.7754ms, xử lý trước send9.9471ms, trong đó encode0.0685ms, **tracking9.8320ms**, send0.1057ms; CPU trước send9.908890ms. Đoạn tracking gồm `ReplyTracker.recordSent` (có encode/validation lần nữa), map sent/pruning và clock. Chưa có stack profile để quy9.832ms cho riêng hàm, cấp phát, ART/JIT hay nguyên nhân khác; **không quy toàn bộ lỗi còn lại cho AVD**. CPU clock trong VM có sai số/khác biệt accounting (có chênh wall−CPU âm nhỏ), giữ cả giá trị gốc. `08-stages-{stop,cadence}.csv` và `08-stages.json` giữ tất cả timestamp/đoạn, không chỉ các ví dụ.

**Logging/allocation:** test latch chứng minh sink chậm không chặn STOP; tại các wire sample cadence, queue trace_pending max0 và flush đều đạt. Đã bỏ list mới mỗi poll, snapshot kiểm tra idle và ByteBuffer.wrap heap mỗi send; validation vẫn giữ. Heap toàn process/test vẫn tăng350,224,384 byte/63GC trong STOP và48,070,656 byte/9GC trong cadence50; số liệu20/30 và8 snapshot ở JSON. Đây không phải allocation riêng worker hoặc A/B profiler; không dùng blocking-gc-count0 để suy không có pause, cũng chưa chứng minh GC gây mẫu1201. Log GC gốc được giữ.

**Quyết định đề nghị, chưa duyệt:** ghi nhận STOP và cadence20/30 đạt đúng cohort AVD; giữ cadence50 **CHƯA ĐẠT**, G3/G4 **CHỜ DUYỆT CHUNG**, giữ cậnG0. Cho phép một đợt tiếp theo có mục tiêu profile đoạn tracking9.832ms (gồm validation lặp) trước khi quyết định tối ưu/đo lại, rồi đo điện thoại khi thiết bị được xác nhận. Muốn đổi chuẩn jitter ở cấu hình50ms phải là quyết định yêu cầu riêng trước nghiệm thu, chưa áp dụng tolerance mới. Không tiếp tục chỉnh mò hoặc tự chuyển Phase5/release. Hosting/khóa ký/điện thoại/firmware/xe thật vẫn thiếu như hồ sơ trước.

