# Lượt33 — tracking và cadence50, chưa duyệt G3/G4

Ngày17/10/2026. Đã kết thúc đợt có giới hạn: profile chẩn đoán trước/sau, một tối ưu theo bằng chứng,49 test liên quan và **đúng một batch nghiệm thu STOP500/cadence50**. Không Phase5/release, không subagent, không chạy lại97/24/8. Bằng chứng20/30 của32 giữ nguyên; không sửa thuật toán lịch chung.

## Nguyên nhân và thay đổi

`ControlSession.send` encode trước rồi `ReplyTracker.recordSent` encode/validate lại cùng frame và bỏ bytes. Trace trước có361 stack Session.send hoàn chỉnh, cả361 encode2 lần; trong mỗi recordSent encode1 lần. Đây là chi phí lặp đã xác định, **không chứng minh nó là nguyên nhân duy nhất của mẫu1201/9.832ms ở32**. Map/pruning không có bằng chứng đủ để tối ưu; giữ nguyên.

Sửa nhỏ: `recordSent(frame,now,template)` trả bytes đã encode/validate; Session dùng gửi luôn. Giữ encoder, kiểm tra frame/seq/ACK, retry cùng frame, intentFloor, replay, cửa sổ256 và pruning. Truyền templateD đúng sau handshakeS. Runtime chỉ đổi tên mốc `encode_*` thành `prepare_*`, vì prepare nay gồm encoder và recordSent. **Không so trực tiếp tracking cũ với after-stage mới để tuyên bố nhanh hơn**; cost vẫn nằm trong full processing wake→send và trace method.

`11-scope-unchanged.json`: đảo đúng patch reuse/đổi tên thì SHA cả3 file sản phẩm khớp baseline32. Các nhánh wait/poll/deadline/floor/urgent/owner không đổi; reducer/codec/UDP/settings/Gradle cũng không đổi. Do đó chỉ nghiệm thu STOP500 và50ms theo yêu cầu, không thêm cohort20/30.

## Profile riêng, không phải nghiệm thu

`TrackingProfileTest` chạy mỗi nhóm400 mẫu: full Session chuẩn bị gửi, tracker ACK thường, tracker không ACK để đi qua prune256. Protocol clock0/ACK kiểm soát; ART method trace dual wall/thread-CPU, bộ đếm allocation theo thread, probe64KiB xác nhận counter có hoạt động. Tất cả1200 dòng/bản ở `tracking-{before,after}.csv`; giữ cả khởi tạo/resize, không loại warmup/outlier.

| Chẩn đoán | Trước | Sau |
|---|---:|---:|
| Stack Session.send hoàn chỉnh / encode mỗi lần |361 /2|400 /1|
| Allocation prepare trung vị(byte/object) |3024 /79|1616 /44|
| Tổng wall400 prepare(ms) |1366.1870|883.1643|
| Tổng CPU thread400 prepare(ms) |1282.899396|767.145569|
| Tracker ACK / window allocation trung vị(byte) |1504 /1504|1504 /1504|

Giảm phần prepare1408 byte trung vị bằng reuse; recordSent vẫn thực hiện đầy đủ encode/validation một lần. Map window vẫn có cấp phát resize (max3568 byte), không sửa vì chưa chứng minh là nguồn chính. CPU dưới method trace có222/400 và243/400 mẫu prepare bằng0 do resolution/accounting; không hiểu là không tốn CPU. Tracing rất nhiễu, CPU đôi khi khác wall.

**Giới hạn profile:** buffer64MB overflow ở cả hai bản; giữ nguyên file `.trace`, header và prefix stack, không giả là đủ stack cả1200 lượt. Prefix vẫn đi qua Session sent-map pruning:107/146 lần `first`, không mismatch parser; stack pending-window cuối chưa đủ, chỉ có timing/allocation đầy đủ ở CSV. Coverage khác nhau và tracing overhead khiến bảng wall/CPU không phải A/B độ trễ sản phẩm hoặc nghiệm thu. Không kết luận kiểu “tối ưu này giải quyết mọi outlier”.

Lượt01 chạy xong nhưng AGP uninstall làm mất file trong external-files trước khi pull; chỉ còn log/XML và lỗi02, không dùng làm kết quả profile. Lượt03 sửa nơi lưu sang `additionalTestOutputDir` do AGP thu tự động;08 là profile sau sửa. Hai bản dữ liệu hợp lệ được giữ riêng, không chọn cohort đẹp.

## Test trước/sau và một đợt nghiệm thu

| Bằng chứng | Kết quả |
|---|---|
|05-encode-count-red |Phân tích trace trước:361/361 serialize2 lần, assertion yêu cầu1 fail, exit1; không chạy lại profile để tìm mẫu đẹp|
|06-return-bytes-red / XML |1 test fail vì tracker trả Unit/bỏ bytes đã kiểm tra, trước sửa|
|07-related-check / XML |**49/49**, exit0:ReplyTracker16, Codec6, Session18, Scheduling5, Runtime3, Trace1; golden bytes/retry/invalid-frame/seq/ACK/replay/prune/STOP/Kill/Brake/pointer/macro và clock|
|08-after-profile,09-after-analysis |Profile1/1; assertion serialize1 lần cho400 stack gửi hoàn chỉnh đạt, exit0|
|10-selector-keeps-old-failures |Chỉ thêm chọn target cho test/script; khi đọc lại cohort32 vẫn nguyên1281 mẫu50/637 fail, exit1 mong đợi. Mặc định vẫn20/30/50, không đổi regex/cận/min-count|
|12-one-acceptance-batch / XML |**2/2 Android**, exit0,3m12s; không bật method trace/alloc counting, UDP simulator thật, STOP500 và giữ50ms64 giây|
|13-stop-check |exit0;500 release đủ, không mẫu t1/t2>10ms;2343 interval30 phụ đều trong20–50, không thay bằng chứng20/30 chuyên biệt|
|13-cadence50-check |exit1;1283 interval đủ;420>50ms; giữ mọi mẫu/cậnG0|

| Nghiệm thu | Lượt32 | Lượt33 |
|---|---:|---:|
| STOP max t1−t0 / t2−t0(ms) |3.3828 /3.5330|6.3577 /8.8851|
| STOP số mẫu>10ms |0 /500|0 /500 — **đạt cohort**|
| Cadence50 n / số<20 / số>50ms |1281 /0 /637|1283 /0 /420|
| Cadence50 min /p50 /p95 /p99 /max(ms) |32.6513 /49.9940 /51.6336 /52.1065 /60.1096|34.1609 /47.2893 /62.4068 /63.5466 /64.8975|

Cadence50 **CHƯA ĐẠT**; số vi phạm giảm nhưng tail/max xấu hơn, không tuyên bố cải thiện tổng thể. STOP đạt cận nhưng chậm hơn32. Không đo lại/chọn mẫu hoặc đổi sang nghiệm thu p99. `13-stop.csv`, `13-cadence50.csv`, logcat và simulator JSONL giữ toàn bộ mẫu.20/30 giữ hồ sơ32, chưa có cohort mới trên build33.

## Tách jitter và outlier

`jitter32.csv/json`, `jitter33.csv/json` phân rã toàn bộ1281/1283 cặp, không đổi kết quả chấm. Với từng cặp: interval = bước deadline + Δwake-lateness + Δprocessing(wake→send). Ở33, **1283/1283 bước deadline đúng50ms**, không giảm cấu hình hoặc gửi bằng chu kỳ khác. Trong420 interval>50,409 có phần tăng wake-lateness lớn hơn phần tăng processing dương,11 ngược lại/bằng; ở32 là599/38 trong637. Đây là phân rã wall time, không phải stack-level attribution cho từng stall.

Mẫu33 số351 lớn nhất:64.8975 =50 +13.9058(Δwake) +0.9917(Δprocessing) ms. Worker muộn14.6805ms; poll yêu cầu0.6977ms nhưng mất15.3642ms. Processing1.3887ms gồm trước prepare0.6896 + encode/recordSent0.6624 + sent-map/đến send0.0367; CPU trước send0.669912ms, send0.2729ms. Mẫu686 wake muộn nhất16.9319ms, poll1.5171→18.4333ms, processing0.3859ms (prepare0.1920), CPU0.360780ms; interval61.4532ms.

Toàn cohort33: processing p99/max1.4807/5.0939ms; riêng encode+recordSent p99/max0.9134/2.1963ms; sau prepare p99/max0.2352/1.4745ms. Wake-lateness p99/max15.5431/16.9319ms. Lượt32 mẫu1201 có Δprocessing9.7448ms và Δwake0.3648ms; stack lúc ấy không có nên vẫn chưa quy9.832ms cho riêng hàm/GC. Giữ cả giá trị wake âm/CPU0, không clip để làm đẹp.

Hai phía đều có phần cần xét: đã loại chi phí serialize lặp theo bằng chứng; lần nghiệm thu33 phần lớn vi phạm nằm ở tăng trễ wakeup, vẫn có biến thiên xử lý. Host cùng AVD/software nhưng tải/memory khác32 (`11-acceptance-environment.json`:CPU56%,free~1.2GB); chưa có scheduler trace để quy từng stall cho Windows/vCPU/ART/GC. Không kết luận tất cả do AVD hoặc tất cả do tracking. Phân bố jitter50 quanh mốc50 khiến một mức lateness tăng bất kỳ đã tạo interval>50, ngay cả khi code chạy nhanh.

## Quyết định còn chờ

Giữ **G3/G4 CHỜ DUYỆT CHUNG**, cadence50 **CHƯA ĐẠT** theo cậnG0; không sửa thêm trong đợt này. **Đề nghị ưu tiên giữ cậnG0**, chốt điện thoại/Android thực rồi cho một cohort độc lập cùng binary và chẩn đoán scheduler/wakeup; chưa có bằng chứng thiết bị thật sẽ đạt, không hứa thay môi trường là hết lỗi.

Nếu người dùng muốn thay yêu cầu50ms danh định bằng một budget jitter tối đa, phải duyệt riêng một giới hạn mọi-mẫu trước cohort tương lai; không lấy p99 thay hard max hoặc áp tolerance mới lên cohort33 để gọi đạt. Chưa áp dụng quyết định này. Hosting/khóa ký/firmware/xe thật và giới hạn nghiệm thu vật lý giữ như hồ sơ trước. Simulator riêng đã cleanup đúng PID; chưa upload/release.

Kiểm tra bàn giao: `15-final-audit.json` xác nhận49/49 unit,2/2 Android, duy nhất BEGIN STOP500/CADENCE50, đủ500/1283 mẫu và hash APK/source khớp build đã đo. `15-source-after.sha256`/`15-data.sha256` giữ hash nguồn và dữ liệu. Đã cài/mở lại cùng debug APK trênAVD5554 sau AGP cleanup (`15-avd-handoff.txt`), không coi là phép đo mới hoặc cập nhật HTTPS.
