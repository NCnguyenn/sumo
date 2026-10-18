# Ma trận R01–R23 — Phase5

PASS dưới đây chỉ áp dụng phần mềm trên AVD API37 và SUMO-SIM/1-draft1. Cột hệ thống giữ riêng phần không thể chứng minh bằng giả lập. **Người dùng đã chốt G5 CÓ ĐIỀU KIỆN cho chức năng AVD; STOP/cadence là lỗi mở chặn dùng xe/release.** Không dùng kết quả này thay G6 hoặc duyệt G3/G4.

**Cập nhật sau sửa và kiểm chứng18/10:** J mới=`evidence/16-g5-avd-approval/23-final-unit-xml/`110/110; APK cuối có6/6Android ở20 (4UDP+2thu timing),8/8fault ở22,**25/25AndroidUI/Settings/updater+soak/probe ở33** trên cùngSHA256 `92abb62d…`; không cộng ca trùng. Soak33 được chạy/audit riêng trênAPK cuối:30phút0,668giây,600chu kỳ,20reconnect,60.557frame/21SID,0revival. Soak08 giữ lịch sử. Trace chẩn đoán30–32 không đóngFAILSTOP/cadence20/21. Xem [báo cáo bổ sung](KET_QUA_BO_SUNG_G5.md); các mãJ/A/P/N/S/T ban đầu dưới đây vẫn trỏ hồ sơ lịch sử.

Mã bằng chứng: **J**=`evidence/08-final-unit-xml/` (108/108); **A**=`08-final-android/instrumentation.xml`/logcat và hai receiver JSONL (36/37, installer thiếu quyền được ghi riêng); **P**=`04-python.xml`/trace (31/31), `08-kotlin-python-vectors.jsonl` (25/25); **N**=`13-faults/`/`13-fault-scenarios.xml` (8/8 cấu hình của một phương thức); **S**=`09-soak-summary.json`/resource CSV/full raw; **T**=`09-stop.*`, `09-cadence.*`. Các tên tệp rút gọn thuộc `evidence/`.

| R | Phần mềm AVD/simulator | Bằng chứng đã chạy | Giới hạn nghiệm thu hệ thống |
|---|---|---|---|
|R01 Android/ngang/fullscreen|PASS|A: ControllerUi U01/U10; `10-resize/resized.png/xml`|API37; API36/điện thoại CHƯA KIỂM CHỨNG|
|R02 D-Pad|PASS|A: Phase5Regression E01 bốn hướng; receiver PWM ±153; J DriveMixer/Reducer|4 hướng theo G0|
|R03 Joystick|PASS|A: deadzone.1/tâm/80%→119, Boost255; AndroidSimulator gain.7/.8→100/115; J8 Mixer|Bánh/motor thực CHƯA KIỂM CHỨNG|
|R04 STOP khi nhả|FAIL trên APK mới|Cohort21:500mẫu,max t1=9,8024/t2=11,4174ms,2t2>10; Normal20 có3t2>10,max14,0058ms. Kết quả08 đạt là lịch sử|Giữ cận10ms; chưa đo touch vật lý/motor|
|R05 Boost|PASS|A U03/AndroidSimulator, S nhấn/nhả, J L04–L07/L19–L20|Nhả Boost giữ hướng; không hướng thì B hiệu lực0|
|R06 HOLD/TOGGLE|PASS|A U04/Phase5Regression E01; nhả hướng giữ W; N lỗi phiên về an toàn|Cơ cấu thực CHƯA KIỂM CHỨNG|
|R07 Xoay180°|PASS macro; góc CHƯA KIỂM CHỨNG|A macro350ms; P dedup/watchdog; `10-lifecycle-keyboard-fix/` macro2000ms bị hủy khi updater/lỗi mạng|Thời gian không chứng minh góc180°|
|R08 Brake|PASS cờ/ưu tiên; hãm CHƯA KIỂM CHỨNG|A I02/U04/E01:K1,PWM/B/W0; nhả không phục hồi ngón cũ; J L12|Hiệu quả hãm/driver cần xe thật|
|R09 Limiter|PASS|A AndroidSimulator30/60/100→77/153/255; SettingsUpdate lưu/recreate; J L03/F01|Không rebuild giữa các giá trị|
|R10 MANUAL/AUTO|PASS phạm vi G0|A “AUTO khóa”; J L15/mode ACK/guard; P mode/timeout|AUTO hoạt động chưa được mở/phê duyệt|
|R11 Kill|PASS phần mềm|A ngón thứ ba Kill/ACK/unlock; S10Kill,21SID; J/P latch/replay|Dừng động cơ/cơ khí thực CHƯA KIỂM CHỨNG|
|R12 Kết nối/ping|PASS giao thức|A handshake/ACK/RTT; N8 lỗi; J Tracker16/Session18/UDP4|Không dùng send hoặc ICMP làm Online; mạng/điện thoại thật chưa thử|
|R13 Bản tin|PASS SUMO-SIM/1-draft1|J Codec6/interop, P31/25vector; A raw LF/SID/seq/ACK|Tương thích firmware CHƯA KIỂM CHỨNG|
|R14 Chu kỳ20–50ms|20/30PASS;50PASS cohort21 theo ngoại lệ, chưa đạt ổn định|G5AVD cho50ms cận60ms:21 n1282,max54,4796,0>60. Normal20 cùngAPK có33>60,max76,8416.626mẫu lỗi G0 của08 giữ nguyên|Không đổi lịch50; ngoại lệ không áp lên hồ sơ cũ hoặc G6|
|R15 Watchdog300ms|PASS simulator; MCU CHƯA KIỂM CHỨNG|P299/300/301; N drop-rx/delay350; macro-drop-rx/force-stop có watchdog thực của receiver|Không đo watchdog firmware|
|R16 Multi-touch|PASS AVD|A U03/U04/E02, N giữ hướng+Boost; S600chu kỳ/20reconnect; J L17–L22|Cảm ứng nhiều ngón điện thoại CHƯA KIỂM CHỨNG|
|R17 Haptic|PASS lời gọi; cảm giác CHƯA KIỂM CHỨNG|S audit:đúng1 lời gọi weapon cho một lần giữ; A/log haptic connected|Không có motor rung thật trên AVD|
|R18 Cấu hình giao thức|PASS phạm vi G0|A I09 templateD/endpoint8889/advanced; J/P validation/atomic ACK|Chỉ aliasS/D đã duyệt; giao thức xe chưa có|
|R19 Phản hồi<30ms|CHƯA KIỂM CHỨNG|T chỉ báo số đo nội bộ callback/send; RTT được hiển thị riêng|Chưa có clock/thiết bị chung để đo tới driver; không suy một chiều từ ACK|
|R20 Gián đoạn/an toàn|PASS phần mềm đã chạy|J L/C/I, A lifecycle/cancel; S61.097 frame không revival; N8; `10-*` force-stop/reboot/resize;11 LAN deny/grant/revoke|I11 CHƯA KIỂM CHỨNG do thiếu fixture giữ LAN khi chặn WAN; xe thật chưa thử|
|R21 Test/gate/bàn giao|PASS quy trình;G5chốt có điều kiện|Người dùng duyệt rõ chức năngAVD;STOP/cadence còn lỗi mở. Soak33đạt trênbinarycuối đượcauditriêng;G3/G4chưa duyệt,khôngrelease|Không tuyên bố nghiệm thu hệ thống;chặn dùngxe/release|
|R22 Chỉnh/lưu không build|PASS app; firmware/update CHƯA KIỂM CHỨNG|A F01/F03/F07/F08, J validation/schema, `10-*` port/duration khác mặc định giữ qua reboot|F09 MCU và F10 qua APK A→B→C chưa có điều kiện|
|R23 Cập nhật không USB|CHƯA KIỂM CHỨNG toàn luồng|J metadata/stream/hash/identity; A parser/tamper/gate; installer local được ghi riêng|Thiếu điện thoại, HTTPS/đích upload, khóa ổn định+backup; chưa A→B→C|

Số test không được cộng qua các lượt lặp;37 phương thức Android gồm test mẫu, probe và diagnostic. Test thu mẫu thành công không làm cadence50 đạt ngưỡng. Biên bản chính ghi kết quả permission/installer/I11 bổ sung và các giới hạn tương ứng.
