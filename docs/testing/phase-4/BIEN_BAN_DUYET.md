# Hồ sơ Phase 4 — kết nối và tích hợp

**Cập nhật sau Phase5,17/10/2026:** ngoại lệ người dùng cho chạy phần độc lập Phase5 không phải duyệt G3/G4. [Biên bản Phase5](../phase-5/BIEN_BAN_DUYET.md) giữ cohort mới: STOP500 max6,6546ms đạt; cadence20/30 đạt,50ms626/1283 interval>50,max58,4052ms vẫn FAIL. Soak30phút600chu kỳ/20reconnect và8fault đạt; API37 đã thử lại. I11 vẫn CHƯA KIỂM CHỨNG: AVD production không cho root để chặn WAN giữ LAN. Không sửa scheduler/tracking hoặc thay bằng chứng34 bên dưới.

Ngày 17/10/2026, Asia/Bangkok. G0/G1/G2 đã được người dùng duyệt. Phase 4 được thực hiện tiếp Phase 3 theo ủy quyền liên tục; không ghi người dùng đã xem hoặc duyệt các kết quả mới. **G3/G4 CHỜ DUYỆT CHUNG; dừng trước Phase 5/release.**

Ứng dụng đã chạy UI → reducer → UDP → Python simulator thật trên AVD API37. Chức năng đã có bằng chứng; **chưa đạt đủ điều kiện nghiệm thu** vì cadence50ms còn vượt cận và cập nhật qua kênh phân phối thật chưa kiểm chứng. Lượt34 đã sửa codec,54/54 JVM liên quan đạt; STOP500 max6,9630ms đạt cohort, cadence50 max151,6225ms chưa đạt. Cadence20/30ms giữ bằng chứng32. Không suy thành bảo đảm thời gian thực hoặc xe thật.

## Mã và hành vi đã triển khai

`RobotTransport`/`UdpRobotTransport` dùng UDP IPv4, socket/port nguồn mới khi mở lại, `DatagramChannel` nonblocking và buffer257 byte để phát hiện frame vượt256. Không thêm thư viện. `ControlSession` quản lý SUMO-SIM/1-draft1 và `ControlRuntime` là một worker sở hữu toàn bộ socket/thứ tự gửi. UI chỉ gửi sự kiện; không có timer gửi ở Activity hoặc updater.

HELLO/nonce → offer SID → seq1 S trung tính → ACK hợp lệ mới Online. Alias D chỉ dùng sau xác nhận S bắt buộc. Ping lấy RTT từ frame đã gửi và ACK hợp lệ; sai peer/SID/seq/future/replay không gia hạn phiên. Bắt tay1000ms, retry250ms; ACK hết hạn300ms thì trung tính/Offline, cố STOP/C cuối trước đóng socket khi còn quyền. Không tự reconnect hoặc phát lại gesture cũ.

Tick dùng cấu hình20–50ms, không phát bù một loạt tick quá hạn. Snapshot mới nhất thay các yêu cầu lái chưa gửi. Nhả điều hướng hợp lệ, Kill và thay trạng thái Brake phát ngay trong xử lý event, kể cả down mới nằm sau STOP trong cùng batch. Packet chuyển động đã vào mạng vẫn phụ thuộc seq/ACK/watchdog bên nhận; không hứa UDP bảo đảm giao hàng. Pointer hướng cũ/chạm sớm không tái tạo PWM/B ở tick sau. W giữ theo HOLD/TOGGLE; Kill/Brake/cancel/ra nền đóng đúng trạng thái đã duyệt.

Macro giữ ID/duration đến ACK mới hợp lệ báo hoàn tất; receiver sở hữu timer, gói lặp không khởi chạy lại. Unlock cần thao tác giữ1s hợp lệ và ACK; local timer không tự mở khóa. Q/capability/ACK cập nhật nguyên cặp sau ACK khớp; ACK config mất thì khóa lái đến khi đối soát cùng cặp, không tự coi bản cũ đã xác nhận. Không cung cấp góc/vị trí cơ khí xe chưa có firmware xác nhận.

Settings/updater/ra nền/mất focus gửi STOP/C và khóa thao tác cũ. Gate mở tác vụ phụ sau ACK đóng hoặc deadline watchdog; thông báo deadline không xác nhận xe thật đã dừng. Quyền LAN API37 có nhánh từ chối/cấp/thu hồi; mọi lỗi socket/permission đều kết thúc phiên an toàn. Main giữ màn hình sáng khi Online, lưu Kill riêng và không lưu lệnh chạy.

## Bằng chứng test trước / sau triển khai

Các log giữ COMMAND/exit và XML theo từng lượt tại `evidence/`. Dùng JBR25, Gradle9.5 offline và JAVA_TOOL_OPTIONS đường socket ngắn như Phase3. Lệnh filter đầy đủ nằm đầu log; không cộng các lượt lặp thành tổng test khác nhau.

| Lượt | Kích thích và kết quả thật |
|---|---|
| 01→02 | Session/UDP14 ca đỏ tại stub →14 xanh; scheduler, immediate STOP/Kill, ACK/session, close gate, macro/unlock/config, loopback/close |
| 03→04 | Worker2 ca đỏ →2 xanh; sender đang bận không flush chuyển động cũ, callback tác vụ phụ chỉ sau close ACK/watchdog |
| 05→06 | Android kết nối1 ca đỏ chưa Online →1 xanh với UDP simulator Python thật qua10.0.2.2:8888 |
| 07→08 | 2 ca đỏ: aliasD sai confirmS và timeout chưa cố STOP/C cuối; sửa → xanh |
| 09 | 2 JVM interop xanh:9 scenario mạng xác định + restart Python process; log hai phía `runtime-*.jsonl` |
| 10 | 4 Android end-to-end xanh: đa ngón/HOLD/Brake/Kill/unlock thật, lifecycle, Settings/endpoint/calibration/macro350/interval50 |
| 11→12 | Callback installer mất sau process-state đỏ → Main nạp kết quả lưu; full12 **92/92 JVM,20/20 Android** xanh |
| 13 | Phân tích500 STOP lượt12 **FAIL**: max trước send37.0377ms, sau send37.3233ms; có24/26 mẫu vượt10ms;2755 cặp tick30ms có17 ngoài20–50ms |
| 14 | Buffer nhỏ/tái sử dụng + trace bỏ String.format;94 JVM xanh/lint0 lỗi. STOP cohort dừng sau74 nhả do ACKtimeout: giữ FAIL và log, không xem như500 mẫu |
| 15/15b | 8 Android fault scenario:7/8 xanh ban đầu; delay350 chưa có chuyển động trước lỗi nên fixture chạy lại fault-after80, xanh. Giữ `15a-original-fault-summary.json` và log lỗi cũ; đây là sửa fixture, không sửa simulator để nhận gói cũ |
| 16 | 7 Android cấu hình/updater xanh, bao gồm giá trị PWM hiệu chuẩn từ joystick thực qua UDP |
| 17 | Permission AVD thật: deny → không Online; grant → điều khiển; revoke khi giữ chuyển động → process bị Android kết thúc/watchdog receiver dừng; mở lại Offline/trung tính. `permission-summary.json`, XML/ảnh/commands |
| 18→19 |94 JVM/21 Android xanh;500 release hoàn tất nhưng ngưỡng10ms vẫn **FAIL** (11 mẫu vượt;max t1=33.8693ms,t2=34.4531ms).656 cặp tick không đủ1000, không tự ghi cadence đạt |
| 20→22 | Đo1000 poll rỗng đỏ:2286.5264ms với DatagramSocket timeout; chuyển NIO nonblocking đạt. Thêm3 ca đỏ: STOP rồi down mới cùng batch, xung Brake cùng batch và ACK từ chối lái; sửa → **97/97 JVM** xanh, poll AVD xanh |
| 23 | Hồi quy NIO:97 JVM xanh, Android20/22 xanh; SystemUI RenderThread SIGSEGV trùng mất Compose hierarchy I09, STOP500 dừng sau95 nhả/ACKtimeout. Giữ log lỗi; không suy mọi lỗi chắc chắn từ GPU |
| 24→26 | Khởi động AVD GPU software, giữ dữ liệu; chỉnh UI theo phản hồi. **97/97 JVM,24/24 Android**, exit0; lint0 lỗi/17 cảnh báo. Có đủ500 release và1000+ cadence ở mỗi mức20/30/50 |
| 27 | Phân tích log26: STOP và cadence **FAIL** theo đúng cậnG0; CSV/JSON giữ mọi mẫu, bảng dưới |
| 28 | Driver trên NIO/UI mới **8/8 scenario đạt**, log hai phía/thông số lỗi trong thư mục `28-fault-nio/`. Không ghi đè bằng chứng15 |
| 29 | Thử I11: tắt Wi-Fi/data làm WAN lẫn routeLAN mất; không đủ điều kiện có endpoint hợp lệ, không chạy tới assertion app. Đã khôi phục mạng; **chưa kiểm chứng I11 trực tiếp** |
| 30 | Sau chỉnh Settings/updater thành dialog ngang và thêm binding feed, chạy lại **4/4 AndroidSimulatorTest đạt**; tham số/đa ngón/lifecycle vẫn đúng qua UDP. Hai simulator tạo riêng và đã cleanup đúng PID |
| 32 | Sửa hiệu năng theo yêu cầu riêng: test clock/latch đỏ trước sửa → **31/31 test liên quan xanh**; đúng một batch **2/2 Android thu mẫu đạt**. Script gốc STOP exit0, cadence exit1. Không chạy lại97/24/8; số liệu và phần chưa đạt bên dưới |
| 33 | Profile tracking trước/sau, reuse bytes đã validate; **49/49 JUnit** liên quan và một batch **2/2 Android**. STOP500 vẫn đạt, cadence50 **chưa đạt**; không đo lại20/30 vì thuật toán lịch không đổi |
| 34 | Sửa cấp phát codec sau test đỏ1896 byte/frame → xanh232 byte/frame trên JVM; **54/54 JVM** và một batch **2/2 Android thu mẫu**. STOP500 max6,9630ms đạt; cadence50 có641/1282 mẫu>50ms,max151,6225ms **CHƯA ĐẠT**; không đổi lịch/ngưỡng |

### Số đo hiệu năng lượt26–27, trước sửa32

Cohort26 dùng500 nhả hướng trên root Compose và ACK UDP thật. Cùng clock monotonic Android: t0 vào callback, t1 ngay trước send, t2 sau send trả. Kiểm tra busy bằng latch/worker JVM riêng; chưa định lượng từng thời khắc sender bận trong cohort Android. `27-stop-timing.csv/json` giữ đủ500 mẫu.

| Mốc đo (ms) | p50 | p95 | p99 | Max | Số mẫu >10ms |
|---|---:|---:|---:|---:|---:|
| t1−t0 |0.6258|2.0061|4.6610|8.9006|0/500|
| t2−t0 |1.1044|2.7155|4.9647|13.2437|1/500|

Mẫu496 là mẫu vượt cận, thời gian trong send4.3431ms; không bỏ mẫu này hoặc chạy lặp để chọn một cohort đẹp. G0 yêu cầu max cả hai mốc ≤10ms nên kết luận vẫn **CHƯA ĐẠT**.

CadenceTest đo giữ hướng cùng APK ở20/30/50ms, phân loại event tức thời riêng. `27-cadence-timing.csv/json` giữ các interval theo thứ tự; script có tự kiểm tra rằng mẫu chậm60ms không bị loại và không nối interval qua event.

| Cấu hình | n | Min(ms) | p50 | p95 | p99 | Max | <20ms | >50ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
|20ms|1324|9.4501|21.0475|23.2249|27.9233|89.4633|214|2|
|30ms|1287|17.9880|31.0792|33.0078|35.7388|45.0387|1|0|
|50ms|1246|39.9760|51.1289|53.8879|60.3988|101.4665|0|1049|

Đủ số mẫu; cả ba mức **chưa đạt** yêu cầu mọi interval20–50ms. Hai script phân tích exit1 được lưu ở `27-timing-check.txt`; suite chức năng vẫn exit0 vì nó xác nhận thu mẫu/ACK, không tự kết luận timing. Những interval30ms xuất hiện trong STOP500 cũng được báo riêng, không thay ba cohort giữ hướng. Mốc phản hồi hệ thống <30ms trên motor/thiết bị thật chưa đo; RTT không thay thế phép đo đó.

### Sửa hiệu năng lượt32 — một batch sau sửa, giữ nguyên cậnG0

Chi tiết nguyên nhân, lệnh/exit, phân rã thời gian và giới hạn bằng chứng: [KET_QUA_HIEU_NANG.md](evidence/32-performance-fix/KET_QUA_HIEU_NANG.md). Test clock/latch trước sửa tái hiện3 lỗi: trôi deadline33→63ms, send kéo dài gây gửi bù, logging chặn STOP. Sau sửa **31/31 JUnit liên quan**, build debug/androidTest đạt; đúng **một batch2/2 Android** thu đủ mẫu. Không chạy lại97/24/8.

Runtime/Session dùng deadline monotonic nanosecond, bỏ slot quá hạn, chờ phần deadline còn lại; tick thường giữ floor20ms, STOP/Kill/Brake bỏ qua floor. Một owner giữ seq/thứ tự gửi; logger riêng không gửi lệnh. Tái sử dụng batch/direct UDP buffer; giữ validation và pointer cũ. Không đổi UI, Gradle, ACK contract hoặc cấu hình. Chỉ thêm stage/CPU/heap/flush vào hai phép đo; hai script chấm gốc khớp SHA trước sửa.

| STOP,500 mẫu | Max trước → sau(ms) | >10ms trước → sau |
|---|---:|---:|
| t1−t0 |8.9006 →3.3828|0 →0|
| t2−t0 |13.2437 →3.5330|1 →0|

STOP **ĐẠT cohort**, script exit0; CSV đủ500. Ví dụ mẫu201(ms): post0.0153 + chờ worker1.0117 + xử lý0.1372 + encode0.1118 + tracking1.1284 + send1.1286 =3.5330.

| Cadence sau sửa | n | Min(ms) | p50 | p95 | p99 | Max | <20 / >50ms |
|---|---:|---:|---:|---:|---:|---:|---:|
|20ms|1293|20.4793|21.6666|22.9286|23.4023|25.8384|0 / 0|
|30ms|1337|21.7866|29.9861|31.3657|31.9416|34.0212|0 / 0|
|50ms|1281|32.6513|49.9940|51.6336|52.1065|60.1096|0 / 637|

Cadence20/30 đạt cohort; **50ms CHƯA ĐẠT**, script exit1. Giữ3911 interval, cả mẫu đầu và mọi mẫu chậm; không đo lại hoặc nới ngưỡng.1280/1281 bước deadline50ms đúng50ms, bước đầu32.8488ms do floor sau event. Cadence20 p50 cao hơn trước do guard20ms và môi trường, không tuyên bố mọi tốc độ đều cải thiện.

**Mã và môi trường đều còn phần cần xét:** mẫu50ms số954 thức dậy muộn2.7807ms (poll yêu cầu0.3525 nhưng mất3.1135ms), xử lý trước send0.2444ms. Mẫu1201 lớn nhất60.1096ms có worker muộn1.7754ms và xử lý9.9471ms, trong đó **tracking9.8320ms**, CPU trước send9.908890ms. Đoạn này có `ReplyTracker.recordSent` encode/validation lại và map/pruning; chưa có stack profile để quy cho riêng hàm hoặc GC. Không kết luận chỉ do AVD. CPU accounting trong VM có sai số, giữ giá trị gốc.

Logger chậm không chặn STOP trong test latch; flush xác nhận đủ record, queue tại mẫu cadence max0. Heap toàn process/test vẫn tăng48,070,656 byte/9GC trong cadence50; không dùng bộ đếm tổng để kết luận GC gây mẫu1201. Raw log và CSV phân rã mọi mẫu nằm ở `evidence/32-performance-fix/`; script bổ sung chỉ phân tích cùng dữ liệu, không thay tiêu chí nghiệm thu.

Kết luận khi kết thúc32: STOP/cadence20/30 đạt cohort, cadence50 chưa đạt; đề nghị profile tracking. Người dùng đã giao lượt33 bên dưới, **chưa duyệt G3/G4**. Bằng chứng32 giữ nguyên; không áp tolerance mới.

### Lượt33 — profile tracking và nghiệm thu STOP500/cadence50

Chi tiết/giới hạn và lệnh đầy đủ: [KET_QUA_HIEU_NANG.md](evidence/33-tracking-profile/KET_QUA_HIEU_NANG.md). Profile ART trước/sau có wall/CPU thread/allocation/stack;361 stack gửi hoàn chỉnh trước đều encode2 lần,400 stack sau encode1. Allocation prepare trung vị3024→1616 byte. Trace64MB overflow được giữ/đánh dấu; CSV1200 dòng/bản đủ, profiling có nhiễu và clock/ACK kiểm soát, **không thay dữ liệu nghiệm thu**. Lượt01 mất output do AGP uninstall, giữ log/XML/lỗi pull; lượt03 sửa nơi lưu output, không chọn cohort timing.

`ReplyTracker.recordSent` trả bytes đã encode/validate cho Session gửi; giữ nguyên validation, seq/ACK/replay/map/prune. Runtime chỉ đổi mốc chẩn đoán encode→prepare vì cost nay bao gồm tracker; không so đoạn tracking cũ với đoạn sau prepare để che chi phí. `11-scope-unchanged.json` xác nhận đảo đúng patch reuse/rename thì SHA cả3 file sản phẩm khớp32, lịch/wait/floor/owner không đổi. Red05 double-encode, red06 bỏ bytes → **49/49 JUnit** liên quan xanh (`07-related-check.txt`/XML), assertion stack09 xanh. Bộ chọn test/script chỉ thêm target50, mặc định vẫn20/30/50; phân tích lại32 vẫn đủ1281/637 mẫu fail, không đổi cận.

Nghiệm thu duy nhất `12-one-acceptance-batch.txt`/XML: **2/2**, exit0,3m12s, không method trace/alloc counting; simulator UDP thật. Chấm STOP exit0, cadence50 exit1, đủ500/1283 mẫu; giữ toàn bộ CSV/log. Không chạy lại97/24/8 hoặc cohort20/30.

| Chỉ số |32|33|
|---|---:|---:|
| STOP max t1 /t2(ms),500 mẫu |3.3828 /3.5330|6.3577 /8.8851 —0 vượt10ms, đạt cohort|
| Cadence50 n /số>50ms |1281 /637|1283 /420 —**CHƯA ĐẠT**|
| Cadence50 min /p50 /p95 /p99 /max(ms) |32.6513 /49.9940 /51.6336 /52.1065 /60.1096|34.1609 /47.2893 /62.4068 /63.5466 /64.8975|

STOP vẫn đạt nhưng chậm hơn32;50 có ít vi phạm hơn nhưng tail/max xấu hơn, không tuyên bố cải thiện tổng thể.20/30 giữ bằng chứng32, chưa có cohort mới trên build33. Mọi1283 bước deadline33 đúng50ms, không âm thầm giảm cấu hình. Phân rã interval =50 +Δwake +Δprocessing: trong420 mẫu vượt,409 phần tăng wake lớn hơn phần tăng processing dương,11 ngược lại/bằng. Mẫu351 lớn nhất64.8975ms có Δwake13.9058 +Δprocessing0.9917; poll0.6977→15.3642ms, processing1.3887ms (prepare0.6624). Wake muộn max16.9319ms; processing max5.0939ms, riêng prepare(encode+tracker) max2.1963ms. Không quy tất cả cho AVD/tracking; chưa có sched trace để quy trễ wake cho Windows/vCPU/ART/GC. Profile xác định chi phí serialize lặp, không xác nhận nguyên nhân riêng của9.832ms cũ.

**Kết thúc đợt33:** giữ cậnG0, cadence50 CHƯA ĐẠT, **G3/G4 CHỜ DUYỆT CHUNG**. Đề nghị ưu tiên chốt điện thoại/Android thực và cho cohort độc lập cùng binary kèm chẩn đoán wakeup; chưa có bằng chứng máy thật sẽ đạt. Nếu muốn một budget jitter tối đa thay yêu cầu50 danh định, phải duyệt riêng giới hạn mọi-mẫu trước cohort mới, không thay hard max bằng p99 hoặc áp ngược lên33. Chưa thực hiện phương án đó; không chỉnh tiếp/đo lặp, không Phase5/release. Các chặn hosting/khóa ký/xe thật giữ nguyên.

## Ánh xạ phạm vi I / F / A

| Ca | Bằng chứng / giới hạn |
|---|---|
| I01/F04 | ControlSessionTest clock điều khiển20/30/50, không catch-up burst; Android đổi50 từ Settings; CadenceTest đo thực20/30/50 cùng APK. Kiểm tra chức năng tách khỏi chuẩn jitter chưa được phép nới |
| I02/I03/L17–L22 | SessionTest, RuntimeTest chặn send bằng latch để tái hiện busy; root pointer thật/receiver hai phía và STOP500. Urgent frame không bị gộp mất trước down mới; số đo10ms không suy từ việc test chức năng xanh |
| I04 | HELLO/SID/seq1 và ACK tracker thật; không Online/ping chỉ từ send. Sai nguồn/SID/seq/malformed không gia hạn |
| I05/I06/I07/I08 | JVM/Python process restart + Android fault driver8 nhánh drop-rx/drop-reply/delayRX150/350/delayReply150/duplicate/reorder/malformed. ACKtimeout/oldSID không phục hồi gesture. Watchdog300ms là simulator, không chứng minh MCU |
| I09/A13 | Close ACK/deadline gate clock test và worker; Android mở Settings khi giữ hướng, đổiendpoint8889 rồi reconnect mới. Updater dùng cùng gate, không phát sender riêng |
| I10 | Native deny/grant/revoke trên API37 ở lượt17. Chưa có API36 hoặc điện thoại Android16 để đối chiếu |
| I11 | Đường điều khiển dùng IPv4 UDP nội bộ, unit loopback/AVD→host không gọi cloud/DNS/HTTP. Lượt29 tắt Wi-Fi/data: default network=none và WAN không có route, nhưng LAN10.0.2.2 cũng không còn route. Fixture không đủ điều kiện, chưa chứng minh trực tiếp LAN vẫn hoạt động khi mất Internet |
| I12 | Close socket/no blocking read, fresh source port/reconnect;1000 poll rỗng NIO trên AVD; lỗi I/O/permission và không nhân owner bằng worker test. Soak30 phút thuộc Phase5, chưa thực hiện |
| F01/F03/F07 | Android limiter30/60/100→77/153/255, gain trái.7/phải.8, sensitivity1.5/deadZone.2, joystick70%→100/115; không rebuild giữa các giá trị |
| F02/F05/F09 | Macro350ms receiver thực thi1 lần, templateD sau S confirm; config capability/matchedACK/timeout/unsupported bằng fixture engine. Góc quay/phanh/cơ cấu vật lý chưa kiểm chứng |
| A07/A09/A10/A14 | Updater logic + package parser/receiver đã kiểm tra; hosting/khóa phân phối/điện thoại chưa có. Lượt21 Phase3 xác nhận prompt và Cancel thật, giữversion1/cấu hình; nhánh storage-failure vẫn tổng hợp. Chưa cài bản mới qua HTTPS. Không dùng adb install thay bằng chứng cập nhật HTTPS |

## Điều kiện G3/G4

Đợt34 đã kết thúc đúng một batch sau sửa codec theo yêu cầu tự sửa tiếp của người dùng; chưa có quyền chuyển Phase5 hoặc release. Chi tiết mới nhất: [kết quả34](evidence/34-completion/KET_QUA_SUA.md), CSV/JSON và raw log trong cùng thư mục. Allocation JVM giảm không chứng minh cadence cải thiện:641/1282 mẫu vượt50ms,max151,6225ms, xấu hơn33 về số vi phạm và max. Mẫu lớn nhất có wake trễ104,1400ms, xử lý sau wake1,4385ms; chưa xác định tác nhân gây trễ worker. STOP500 vẫn đạt cohort. Chỉ đánh dấu đạt kỹ thuật từng mục khi có log thực. Chưa tuyên bố toàn bộ G3/G4 đạt: cadence50ms còn vượt cận, thiếu kênh phân phối/ký/thiết bị và người dùng trực tiếp thử. Simulator không chứng minh tương thích firmware hoặc hành vi xe thật.

## Đầu ra và quyết định còn chờ

- Test mới: JVM ControlSession/ControlRuntime/UdpRobotTransport/ControlTrace/SessionSimulatorInterop; Android AndroidSimulator/AndroidFault/AndroidTransport/StopLatency/Cadence. Công cụ `tools/check_android_timing.py`, `check_android_cadence.py`, `run_android_faults.py`, `check_android_permissions.py`; không thêm framework hoặc dependency.
- Mã mới: `connection/{RobotTransport,UdpRobotTransport}.kt`, `control/{ControlSession,ControlRuntime,ControlEvent,ControlTrace}.kt`; Main nối điều phối thực. UI/Settings/updater/signing nằm trong hồ sơG3. Lõi cũ chỉ thêm helper nội bộ xác định pointer hợp lệ; không viết lại mixer/codec/simulator.
- Bằng chứng cuối nguồn/artifact: `evidence/31-source-final.sha256`, `31-baseline-audit.json`, `31-artifact.json`, `31-final-verification.json`. SDK/dependency/wrapper và nguồn codec/Python cũ được đối chiếu với baseline, không đổi. Không có Git nên không tạo commit/PR hoặc tự ghi đã merge.
- Kết quả đối chiếu: toàn module97/97 JVM (60 đã có +11 Phase3 +26 Phase4); hồi quy Android26 là24/24. Các lượt cuối cho phần bị thay đổi: Controller10/10 ở Phase3-23, Settings8/8 ở Phase3-30 (cửa sổ nhỏ), UDP integration4/4 ở Phase4-30, fault8/8 ở Phase4-28, prompt/Cancel1/1 ở Phase3-21. Không cộng các lượt thành số test độc lập hoặc nói đã chạy một suite29 ca cuối.
- Python31/31 và25/25 vector là bằng chứng Phase2 đã duyệt, source/fixture không đổi; không chạy lại chỉ để lặp xác nhận. JVM integration vẫn gọi Python process thật theo các test liên quan.

| Nội dung cần người dùng quyết định | Trạng thái / đề nghị |
|---|---|
| UI và chức năng phần mềm trên AVD | Đề nghị xem ảnh mới/hồ sơG3/G4 và trực tiếp thử, chưa ghi đã được duyệt |
| Hiệu năng STOP/cadence |34 STOP500 max6,9630ms đạt;20/30 giữ32. Cadence50 còn641/1282 interval>50ms,max151,6225ms, **CHƯA ĐẠT**; thiếu thiết bị thật để kiểm chứng độc lập. Đổi budget jitter phải duyệt riêng, chưa áp dụng |
| Phân phối và cập nhật thật | Cần hosting/URL feed/đích upload, khóa ổn định/backup/fingerprint và điện thoại đã xác nhận để thử A→B→C; không tạo bí mật hoặc URL hoạt động giả |
| I11/môi trường và xe thật | Cần môi trường LAN còn route khi không có Internet để kiểm tra trực tiếp. Firmware/phanh/góc quay/watchdogMCU/độ trễ vật lý vẫn chưa kiểm chứng |

Sau khi G3/G4 được duyệt hoặc người dùng giao hướng xử lý các mục chưa đạt, bước kế tiếp mới là Phase5/hồi quy theo kế hoạch. Hiện dừng tại gate, không tự chạy soak/release hoặc chuyển phase.
