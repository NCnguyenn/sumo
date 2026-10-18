# Kết quả sửa và kiểm chứng bổ sung G5

17/10/2026. **G5 ĐÃ CHỐT CÓ ĐIỀU KIỆN cho chức năng AVD theo quyết định người dùng; hiệu năng STOP/cadence vẫn là lỗi mở, chặn dùng xe và release.** Đây không phải PASS toàn bộ tiêu chí hiệu năng. G3/G4 chưa được tự xác nhận; không tự chuyển Phase6/7.

## Quyết định cuối của người dùng

Sau khi được cung cấp APK,110unit/6Android/8fault và kết quả STOP2/500>10ms,max11,417ms cùng cadence chưa ổn định, người dùng chọn **“Chốt G5 CÓ ĐIỀU KIỆN cho chức năng AVD; giữ lỗi hiệu năng mở, chặn dùng xe và release”** trong chat ngày17/10/2026. Quyết định chốt phạm vi chức năng AVD, không đổi số đo hoặc ngưỡng10ms/60ms, không xác nhận người dùng đã trực tiếp lái thử trên điện thoại. Không cần đo thêm để thay kết quả FAIL.

Điều kiện bắt buộc còn mở: giải quyết/kiểm chứng STOP≤10ms và cadence theo phạm vi được duyệt trên môi trường mục tiêu trước khi dùng xe hoặc release; kiểm chứng điện thoại/HTTPS/API36/I11/firmware như kế hoạch khi có đủ điều kiện và quyền tiếp tục. Soak08 vẫn là binary cũ; không biến thành soak của binary cuối. G6/G7 và quyền đóng gói chưa được duyệt.

## Kiểm chứng tiếp ngày18/10/2026

Không đổi mã sản phẩm/test/tool, SDK/dependency hoặc APK. `evidence/30-owner-scheduler-diagnostic/source-and-binary-binding.json` đối chiếu63file nguồn với cohort21, tất cả khớp; app/test giữ SHA256 `92abb62d…`/`c52c73db…`. Lệnh unit ở phiên tiếp nhận báo UP-TO-DATE, chỉ tái dùng110/110 cũ, không phải lượt110test thực thi mới.

Đã phân rã toàn bộ5STOP lỗi ởcohort20/21: một mẫu20 mất11,523ms trong send, mẫu khác queue11,0233ms; mẫu21 có queue5,9391/6,0039ms và dispatch trước prepare0,7573/3,3509ms. Cadence50 chậm nhất20 gồm wake overshoot15,5473ms và prepare16,4394ms. Chưa có nguyên nhân chung đủ căn cứ sửa scheduler/codec/transport; không suy mọi lỗi doAVD hoặcGC.

Giả thuyết mới được kiểm: permission callback ở owner có gọi Binder đồng bộ mỗi poll hay không. AVD hỗ trợ `atrace sched binder_driver dalvik` không cần root; đính chính nhận định tiếp nhận trước rằng thiếu công cụ kernel. Trace không ủng hộ giả thuyết Binder mỗi poll: chỉ1transaction trên owner trong lượtSTOP,0 trongcadence. SDK nguồn có cache quyền. Không bỏ hoặc cache lại kiểm tra quyền theo phỏng đoán.

Lượt chẩn đoán trên cùngAPK: `31-owner-traced-stop/`1/1test thu500STOP,max t1=3,9444/t2=4,1192ms; `32-owner-traced-cadence50/`1/1test,n1285,min35,4534/max52,4731ms,0>60ms (646>50ms). Checker của hai lượt exit0. **Có tracing nên đây không phải cohort nghiệm thu, không đóng FAIL20/21 hoặc xác nhận ổn định.** TraceSTOP ring overwrite2531238/2751383entries, không dùng phần thiếu để quy nguyên nhân; cadence1105126/1105126không overwrite. Raw nén lossless và JSON/CSV/lệnh giữ ở30–32. Không đổi ưu tiên/power policy host; AVD mở headless với renderer `swiftshader`, không coi điều kiện này đồng nhất cohort20/21.

Đã lấp khoảng trống độc lập trênAPK cuối tại `33-final-functional-soak/`: **25/25Android,0fail/skip**, gồm23caUI/Settings/updater (Phase5Regression5,ControllerUi10,SettingsUpdate8),1soak+1probe. `final-binding.json` đối chiếu nguồn/app/test cùngcohort21. Không cộng số ca trùng từlượt19/20/31/32. Soak **30phút0,668giây,600chu kỳ,20reconnect,10Kill/mở khóa**,20checkpointworker/trace1/1,FDsocket14→10sau đóng; audit60.557frame gửi/áp dụng,21SID,57.469tick trung tính sauSTOP,0hồi sinh chuyển động/đảo seq. `soak-summary.json`,CSV,XML,raw hai phía giữ riêng; audit dùng lại `analyze_soak.audit/self_check`, không chuyển sốliệu08sang33. Không thấy crash/ANRapp tronglog; FD gồmUnixsocket và chỉ lấy mẫu20mốc, không phải profiler liên tục.

Trước đóngAVD đã mở lại app và xác minh ảnh/XML thật: Offline,PWM0,AUTOkhóa. Đã tắttrace, hai receiver vàAVD do phiên này mở; QEMU giữNormal, không đổi powerpolicy. Bản debug bàn giao vàSHAkhông đổi; không rebuild/release/upload. Điện thoại/HTTPS/API36/I11/hosting/khóa ký/firmware/xe thật vẫnCHƯA KIỂM CHỨNG.

**Phần chưa giải quyết:** cácFAILSTOP/cadence20/21 vẫn có hiệu lực. Chưa bắt được mẫu vi phạm kèmtraceđầy đủ để phân biệt chờkernel/host/ARTvới phần xử lýapp; hai chẩn đoán mới không tái hiệnFAIL. Điều kiện tiếp tục sửa hiệu năng: có mẫu lỗi đồng thời với scheduler/ARTtrace khôngoverwrite và mốc từngđoạn trên điều kiệnmục tiêu được ghi rõ, hoặc lỗi deterministic trongtest. Không bắtbuộc thiếu công cụkernel; không sửa hoặc lặpđo không có giảthuyếtmới. G5giữ cóđiềukiện, chặnxe/release.

## Mã đã sửa

- `ControlSession.revision` ghi nhận sự kiện/trạng thái thay đổi; `ControlRuntime` dùng lại snapshot ở các poll rỗng2ms. Không thay lịch20/30/50, ưu tiên STOP/Kill/Brake, codec, seq/ACK/replay hoặc gate đóng phiên.
- `MainActivity` dùng runtime mặc định, không bật CPU sampler chẩn đoán trong vòng điều khiển. Mốc monotonic t0/t1/t2 và log gói vẫn giữ nguyên. Các trường CPU trong trace của bản này là0; không dùng chúng để suy chi phí CPU.
- `tools/check_android_cadence.py` thêm cờ rõ `--g5-avd-50-jitter`, chỉ mức50ms được cận60ms. Mặc định vẫn G0 nghiêm ngặt. JSON giữ `above_50` và thêm `upper_limit_ms/above_limit`; CSV không loại mẫu.

Không đổi Gradle/SDK/dependency/identity/khóa hoặc firmware. Hai test JVM mới bao phủ cấp phát poll rỗng và việc cache vẫn công bố điều khiển/Kill/limiter/đóng phiên. Test cấp phát ban đầu đo lẫn heartbeat/fixture, sau đó tách riêng gap poll rỗng: red488.160→green136.376byte/1000gap. Lượt cuối136.088byte; đây là JVM, không phải ART/latency Android. Timeout thu mẫu tăng8→30s sau khi build song song làm thiếu mẫu; vẫn giữ1000gap và budget256.000byte, không hạ điều kiện assertion.

## Kiểm tra đã hoàn tất

`evidence/16-g5-avd-approval/` lưu lệnh/exit/XML/red/green/bản quyết định trước đo.

- **110/110 unit đạt**,0fail/error/skip: `23-final-unit-check.txt`, `23-final-unit-xml/`. Lint **0lỗi/17cảnh báo**, app/test APK build xong trong `20-profiler-off-unit-lint-build.txt` (lượt này unit có timeout, được sửa fixture và kiểm lại ở23; không gọi toàn lệnh20 là thành công).
- **6/6 Android trên APK cuối**: `20-g5-profiler-off-cohort/`, gồm4UDP/tích hợp và2ca thu timing. Test thu mẫu xanh không đồng nghĩa ngưỡng đạt.
- **8/8 lỗi mạng trên APK cuối**: `22-g5-final-faults/`, mấtRX/reply,delayRX150/350,delayReply150,trùng,đảo,malformed. Receiver không áp dụng lại chuyển động cũ; wrapper27 exit0.
- **28/28 Android UI/Settings/UDP** sau sửa cache, trước bỏ CPU sampler: `19-g5-related-functional/`. Không cộng với6 thành34ca độc lập hoặc nói đã chạy toàn37ca trên APK cuối.
- Công cụ chấm **6/6 Python test đạt** ở02. Hồi quy05 trên log08 vẫn trả FAIL với626mẫu>50ms khi không bật cờ; không chấm ngược hồ sơ cũ thành PASS.
- Soak08,Python31/31 vàvector25/25 ởhồsơ08–15 giữ lịch sử. Soak mới33 trênAPK cuối đã đạt vàauditriêng nhưmục18/10; không gánsoak08choAPKcuối. Bằngchứngunit/lint/UDP/faultđược táidùng vì nguồn vàbinarykhớp, không gọi làlượtchạymới.

## Mọi cohort mới đều được giữ

Mỗi hàng đo đủ500STOP và≥1000interval/mức; raw/CSV/JSON và exit checker không bị thay. Cận cadence50 mới60ms,20/30 vẫn50ms; STOP vẫn10ms cho cảt1−t0 vàt2−t0.

| Cohort /điều kiện | STOP max t2−t0 /số>10ms | Cadence50 max /số>60ms | Kết luận |
|---|---:|---:|---|
|16:APK08, QEMU Normal|11,5856ms /2|57,0155ms /0|STOP FAIL|
|17:cache snapshot, QEMU Normal|18,0994ms /1|64,9209ms /92|STOP/cadence FAIL|
|18:cùng build17, QEMU AboveNormal tạm|14,3341ms /1|55,1964ms /0|STOP FAIL|
|20:cache+CPU sampler off, QEMU Normal|14,0058ms /3|76,8416ms /33|STOP/cadence FAIL|
|21:cùng build20, power throttling QEMU tắt tạm|11,4174ms /2|54,4796ms /0|STOP FAIL|

Không suy rằng cache/bỏ sampler/nâng ưu tiên đã sửa latency, vì các max vẫn vượt cận. Ở17 mẫu130, chờ queue14,867ms trong18,0994ms; ở16 mẫu498, prepare9,9571ms. Nhiều thành phần có thể trễ; log không chứng minh mọi lỗi do riêngAVD/Windows/GC/tracker.

Thí nghiệm18 chỉ đổi ưu tiên PID18996/Medium_Phone, đã khôi phụcNormal. Thí nghiệm21 chỉ đổi process power throttling từControlMask0/StateMask0 sang5/0 rồi khôi phục0/0; không đổi power plan/timer hệ thống. JSON `16-priority-environment.json` và `24-power-environment.json` xác nhận restored=true. Đây là thay đổi điều kiện đo có ghi nhận, không phải lượt lặp để bỏ kết quả xấu. Tài liệu [Microsoft SetProcessInformation](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-setprocessinformation) mô tả các cơ chế được thử; tài liệu đó không chứng minh nguyên nhân của mẫu trễ ở dự án này.

## Số đo21 theo tiêu chí đã duyệt

| Phép đo |n|min|p50|p95|p99|max|Vi phạm|
|---|---:|---:|---:|---:|---:|---:|---:|
|STOP t1−t0,ms|500|—|0,8332|3,5354|5,2812|9,8024|0>10|
|STOP t2−t0,ms|500|—|1,6686|4,8728|7,0782|11,4174|2>10|
|Cadence20,ms|1245|20,7291|22,4152|24,7931|26,8367|30,9317|0 ngoài20–50|
|Cadence30,ms|1339|21,3939|29,9723|31,8613|32,7545|38,7934|0 ngoài20–50|
|Cadence50,ms|1282|46,0671|49,9610|51,8572|52,7583|54,4796|0 ngoài20–60;624>50|

STOP checker26 exit1; cadence checker26 exit0. STOP vẫn là lỗi mở chặn dùng xe/release; quyết định chốt G5 có điều kiện không nới10ms hoặc dùng p99 để đổi kết luận.

## APK và phạm vi bàn giao

[APK debug sau sửa](apk/sumo-phase5-avd-debug-v1-g5-check.apk), versionCode1, SHA256 `92abb62d00eb6f841eb8b9eb158d4b0be4b98bd39c6e05bd39b69de79613e5e3`. Bản08 vẫn giữ riêng. File `25-final-build-binding.json` đối chiếu110unit,lint và hash app/test; nguồn không đổi trong cohort21. Đây là bản thử AVD được chốt G5 có điều kiện về chức năng, còn lỗi hiệu năng; không phải release hoặc APK phân phối điện thoại.

Điện thoại/HTTPS A→B→C/API36/I11/xe thật tiếp tục CHƯA KIỂM CHỨNG theo quyết định phạm vi. Giữ yêu cầuSTOP10ms và các giới hạn an toàn; không tuyên bố hoàn tất nghiệm thu hệ thống hoặc cấp quyền đóng gói.
