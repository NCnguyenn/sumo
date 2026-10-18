# Independent review: Phase 1 + Phase 2

User authorized G0 and sequential P1/P2 with joint G1/G2 review, stop before Phase 3. Existing implementation and historical red/green evidence are already present; do not reimplement completed work.

No Git repository. Package 15-review-package.txt contains all current new Kotlin/Python sources and tests as additions. Read reports ../BIEN_BAN_DUYET.md and ../../phase-1/BIEN_BAN_DUYET.md for evidence, not as substitutes for code inspection.

Read-only review; write your full findings/verdict only to 15-independent-review.md in this directory. Do not edit source, tests, or other reports. Do not rerun the full suites; root is verifying them. Tiny non-mutating reproductions are allowed. Check spec compliance and quality with concrete sequence/reproducer, path:line and severity for each defect; return separate compliance/quality verdicts. No review work outside authorized P1/P2.

## G0 requirements (verbatim)

## 5. Phạm vi và quy tắc đề xuất cho G0

### 5.1 Phạm vi bản thử

Giữ một module và cấu trúc hiện có, lõi Kotlin thuần, Compose, một bộ điều phối gửi, API nền tảng cho socket/lưu cấu hình/cài APK, Python 3.13 thư viện chuẩn cho simulator. Không cần thư viện joystick, DI framework, server điều khiển, tài khoản hoặc CI/CD.

Đề xuất duyệt **D-Pad 4 hướng và joystick**; cả vũ khí hold/toggle; Boost, Brake, Kill, quick turn, limiter, trạng thái, Settings và updater theo kế hoạch. D-Pad 8 hướng/nút cua riêng đề nghị không áp dụng **cho bản thử** vì joystick đã có hướng chéo; đây vẫn là quyết định chờ người dùng, chưa tự loại khỏi phạm vi toàn dự án.

Chọn **UDP tới simulator** cho Phase 2–5: receiver bind `127.0.0.1:8888` trên host; emulator dùng `10.0.2.2:8888`. Endpoint xe thật để chưa cấu hình đến khi có thông tin. Chưa chọn Bluetooth/WebSocket hay UDP cho xe thật.

AUTO giữ trong UI và model nhưng mặc định khóa, kèm lý do. Chỉ cho bật khi: phiên Online đã nhận capability AUTO, cấu hình và quy tắc tự hành/thể lệ đã được duyệt, đang trung tính, không pointer điều khiển/Boost/vũ khí/Brake, không Kill/macro; gửi yêu cầu, chỉ hiện AUTO có hiệu lực sau ACK hợp lệ. Simulator mặc định không quảng bá AUTO; fixture capability có thể kiểm tra chuyển trạng thái với PWM 0, không giả lập thuật toán tự hành. R10 vẫn có điều kiện/chưa kiểm chứng thật, chưa ghi không áp dụng. Trong AUTO, manual/Boost/quick turn bị khóa; Brake/Kill/mất liên lạc buộc dừng và về MANUAL, không tự chạy lại.

### 5.2 Thứ tự ưu tiên và thao tác

1. **Kill hoặc lỗi/mất quyền phiên**: PWM 0, Boost 0, hủy macro và pointer; vũ khí về 0 trong simulator. Kill latch đến thao tác mở khóa rõ ràng; lỗi phiên đóng phiên, không tự reconnect để tiếp tục chạy. W=0/K=0 ở fail-safe chỉ là trạng thái mô phỏng; cách hạ cơ cấu/driver thật cần duyệt ở G6.
2. **Brake đang giữ**: PWM 0, B=0, K=1 gửi ngay; hủy macro, W=0 theo đề xuất simulator. Nhả Brake gửi K=0 và vẫn PWM 0; phải nhấc các ngón cũ rồi thao tác mới để di chuyển. K=1 biểu thị yêu cầu active brake, không đồng nghĩa đã chứng minh phanh thật.
3. **Macro hợp lệ**: một lần nhấn quick turn, chỉ khởi động khi Online/MANUAL/trung tính và không có thao tác khác. Receiver xoay trái `(-255,+255)` trong thời gian đã cấu hình; nhả nút quick turn không hủy macro một lần. Hết thời gian PWM 0. Kill/Brake/cancel/nền/mất phiên hoặc chạm điều hướng mới hủy macro và về 0; chạm hủy đó không đồng thời khởi động lái. Không khôi phục hướng/Boost trước macro.
4. **Điều khiển thường hợp lệ**: trộn hướng, limiter và calibration; vũ khí có pointer riêng. Mỗi pointer có chủ sở hữu cố định từ down tới up/cancel, không biến pointer Boost thành hướng khi kéo sang vùng khác.

Hai hướng D-Pad đối nhau triệt tiêu trên trục tương ứng khi các pointer còn hợp lệ. Chọn D-Pad hoặc joystick, không trộn hai nguồn; đổi lựa chọn dừng/xóa thao tác cũ. Với cả D-Pad và joystick, nhả hoặc ra khỏi vùng chạm hợp lệ/mất capture của một pointer điều hướng áp dụng quy tắc 5.2.1; trượt trở vào không tự nhận lại thao tác. Joystick chỉ clamp tọa độ tới biên khi pointer còn hợp lệ; D-Pad không tự kích hoạt nút lân cận. Cancel toàn gesture xóa toàn bộ pointer. Kill luôn tiếp cận được, có chữ/semantics và vùng chạm tối thiểu 48 dp; không dùng riêng màu để báo trạng thái.

**Nhả bất kỳ pointer điều hướng hợp lệ nào** yêu cầu snapshot PWM 0 ngay, B hiệu lực 0 và vô hiệu các pointer điều hướng cũ theo 5.2.1; không đợi tick 20–50 ms. Nhả riêng Boost chỉ bỏ Boost, hướng của ngón khác còn nguyên. Nhả hướng thông thường không tự hạ vũ khí đang do ngón khác giữ hoặc đang toggle: W vẫn theo trạng thái vũ khí. Nhả/cancel pointer vũ khí hold đưa W về 0; toggle chỉ đảo một lần khi down hợp lệ, up không đảo. Kill/Brake/lỗi phiên/cancel toàn cục/ra nền/đổi mode/mở Settings/update xóa cả latch toggle theo đề xuất W=0.

Mở khóa Kill yêu cầu Online, tất cả ngón đã nhấc, macro dừng, giữ nút Mở khóa **1000 ms** rồi nhận ACK unlock. Không xóa latch khi chỉ thả Kill, Reconnect hoặc mở lại màn hình. Nếu chưa có mạng, UI ghi chưa xác nhận mở khóa. Sau unlock/reconnect/khôi phục Activity luôn trung tính, chỉ nhận gesture mới; không lưu chuyển động/pointer/macro vào saved state hay cấu hình.

Khi ra nền, mất focus/tương tác, hủy gesture toàn cục, I/O lỗi hoặc quyền LAN bị thu hồi: ưu tiên trạng thái dừng, cố gửi snapshot cuối khi socket còn dùng được, đóng phiên và hủy công việc đang gửi. Không hứa app bị kill/mất mạng vẫn gửi được STOP; receiver phải tự watchdog độc lập. Trước Settings nâng cao/update cũng dừng phiên như vậy; limiter trên màn hình lái là trường duy nhất áp dụng live.

#### 5.2.1 Nhả nhiều pointer điều hướng

**ĐỀ XUẤT CHỜ DUYỆT — thuộc D04, chưa triển khai.** Nhả hoặc mất vùng chạm của bất kỳ pointer điều hướng hợp lệ nào lập tức yêu cầu `PWM=(0,0)`, `B hiệu lực=0`. Mọi pointer điều hướng cũ còn giữ bị vô hiệu để phát sinh chuyển động, nhưng vẫn theo dõi việc chúng đã nhấc hay chưa. Cả pointer vừa trượt ra ngoài vùng chạm vẫn phải nhấc thực sự trước khi nhận lại điều hướng; kéo trở vào không thay thế thao tác chạm mới.

Pointer hợp lệ là pointer đã được nhận từ down và chưa bị vô hiệu, kể cả khi Tiến + Lùi đang triệt tiêu làm PWM 0. Nhóm pointer điều hướng cũ được xác định tại thời điểm nhả/mất vùng; không gồm pointer Boost/vũ khí. Pointer chạm sớm trong khi chờ không được bổ sung thành hướng hợp lệ và không tự có hiệu lực khi nhóm cũ đã nhấc hết.

Trong khi chưa nhấc hết các pointer điều hướng cũ, mọi tick, move của chúng và chạm điều hướng đến sớm đều không được tạo lại PWM khác 0 hoặc B hiệu lực 1. Sau khi nhấc hết, đầu ra vẫn 0 ở các tick tiếp theo; **chỉ down điều hướng mới sau mốc đó** mới được nhận. Một chạm đến sớm rồi vẫn giữ không tự trở thành chạm mới khi pointer cũ cuối cùng nhấc; move/tick của chạm đến sớm đó cũng bị bỏ qua tới khi có down hợp lệ mới.

Việc chờ nhấc tay này chỉ xét pointer **điều hướng**, không yêu cầu nhấc Boost/vũ khí. Pointer Boost đang giữ vẫn được theo dõi nhưng B hiệu lực luôn 0 khi chưa có điều hướng mới hợp lệ; nếu sau đó down điều hướng hợp lệ trong khi Boost vẫn giữ, Boost có thể hiệu lực trở lại theo hướng mới. Nhả riêng Boost không kích hoạt quy tắc vô hiệu điều hướng. Nhả điều hướng không xóa pointer vũ khí HOLD hoặc latch TOGGLE; W tiếp tục theo quy tắc vũ khí đã đề xuất. Kill, Brake, reconnect và cancel toàn cục giữ nguyên quy tắc ở 5.2 và có ưu tiên tương ứng.

Reducer phải công bố trạng thái hiệu lực đã dừng; tick của bộ điều phối đọc trạng thái đó, không tính lại hướng từ danh sách pointer cũ hoặc bật lại B từ riêng pointer Boost. Đây là làm rõ đề xuất an toàn cho R04/R16/R20; không diễn giải nguyên tắc đa ngón thành việc giữ hướng cũ sau một lần nhả điều hướng.

#### 5.2.2 Chuỗi sự kiện và đầu ra dự kiến

**Tất cả các hàng sau là ĐỀ XUẤT CHỜ DUYỆT / CHƯA CHẠY**, sẽ đưa vào `ControlReducerTest` sau khi G0 được duyệt. Mỗi chuỗi độc lập: Online/MANUAL, limiter 60%, calibration 1/0/1/1/100, không Kill/Brake/macro, W=0 nếu không ghi khác. PWM ghi theo `(trái,phải)`, B là Boost **hiệu lực**, không phải riêng trạng thái ngón đang giữ. “Tick tiếp theo” nghĩa mọi tick khi chưa có down điều hướng mới hợp lệ; không chỉ một tick. Quy tắc thời gian yêu cầu STOP ≤10 ms giữ theo mục 8, chưa có số đo cho các chuỗi này.

| Ca / bước | Chuỗi sự kiện hoặc trạng thái pointer | Đầu ra ngay sau sự kiện | Đầu ra ở các tick tiếp theo |
|---|---|---|---|
| L17.1 — Tiến + Lùi | p0 down Tiến | PWM=(153,153), B=0, W=0 | Giữ (153,153), B=0, W=0 |
| L17.2 | p1 down Lùi, p0 vẫn giữ | PWM=(0,0), B=0, W=0 do hai hướng đối nhau | Giữ (0,0), B=0, W=0 |
| L17.3 | p1 up Lùi, p0 Tiến còn giữ | Yêu cầu PWM=(0,0), B=0, W=0; p0 bị vô hiệu | Vẫn (0,0), B=0, W=0 kể cả p0 move; không trở về (153,153) |
| L17.4 | p0 up, đã nhấc hết điều hướng cũ | PWM=(0,0), B=0, W=0; chỉ hết điều kiện chờ nhấc tay | Vẫn (0,0), B=0, W=0; không tự chạy |
| L17.5 | p2 down Tiến mới sau khi nhấc hết | PWM=(153,153), B=0, W=0 | Giữ (153,153), B=0, W=0 |
| L18.1 — Tiến + Trái | p0 down Tiến, rồi p1 down Trái | Lần lượt PWM=(153,153) rồi (0,153), B=0, W=0 theo mixer | Khi cả hai còn hợp lệ: (0,153), B=0, W=0 |
| L18.2 | p1 up Trái, p0 Tiến còn giữ | Yêu cầu PWM=(0,0), B=0, W=0; p0 bị vô hiệu | Vẫn (0,0), B=0, W=0 kể cả p0 move; không trở về (153,153) |
| L18.3 | p0 up, sau đó p2 down Tiến mới | Sau up: (0,0), B=0, W=0; chỉ sau down mới: (153,153), B=0, W=0 | Trước down mới vẫn 0; sau down mới giữ (153,153), B=0, W=0 |
| L19.1 — Tiến + Boost | p0 down Tiến, p1 down Boost | Khi cả hai giữ: PWM=(255,255), B=1, W=0 | Giữ (255,255), B=1, W=0 |
| L19.2 | p1 up Boost, p0 Tiến còn giữ | PWM=(153,153), B=0, W=0; p0 vẫn hợp lệ | Giữ (153,153), B=0, W=0; không yêu cầu p0 nhấc/chạm lại |
| L20.1 — Tiến + Boost | p0 down Tiến, p1 down Boost | Khi cả hai giữ: PWM=(255,255), B=1, W=0 | Giữ (255,255), B=1, W=0 |
| L20.2 | p0 up Tiến, p1 Boost còn giữ | Yêu cầu PWM=(0,0), B=0, W=0 dù pointer Boost còn giữ | Vẫn (0,0), B=0, W=0; tick hoặc move Boost không tạo hướng |
| L20.3 | p2 down Tiến mới; điều hướng cũ đã nhấc, p1 Boost vẫn giữ | PWM=(255,255), B=1, W=0 theo hướng mới | Giữ (255,255), B=1, W=0; không cần nhấc Boost để nhận hướng mới |
| L21.H1 — Vũ khí HOLD | p2 giữ vũ khí, p0 giữ Tiến | PWM=(153,153), B=0, W=1; pointer vũ khí còn nguyên | Giữ (153,153), B=0, W=1 |
| L21.H2 | p0 up điều hướng, p2 vũ khí còn giữ | Yêu cầu PWM=(0,0), B=0, W=1; không xóa p2 | Vẫn (0,0), B=0, W=1 khi HOLD còn giữ |
| L21.H3 | p2 up vũ khí; chưa có hướng mới | PWM=(0,0), B=0, W=0 theo HOLD | Vẫn (0,0), B=0, W=0 |
| L21.T1 — Vũ khí TOGGLE | Bật toggle bằng down/up vũ khí trước đó, rồi p0 giữ Tiến | PWM=(153,153), B=0, W=1; latch toggle bật | Giữ (153,153), B=0, W=1 |
| L21.T2 | p0 up điều hướng | Yêu cầu PWM=(0,0), B=0, W=1; latch toggle còn nguyên | Vẫn (0,0), B=0, W=1; không cần pointer vũ khí còn giữ |
| L21.T3 | Nhấn vũ khí lần kế tiếp để tắt toggle; chưa có hướng mới | PWM=(0,0), B=0, W=0; up nút vũ khí không đảo lần nữa | Vẫn (0,0), B=0, W=0 |

Ca L22 trong kế hoạch bổ sung mất vùng chạm, kéo trở vào và down đến trước khi nhấc hết: sau sự kiện mất vùng phải giữ PWM/B hiệu lực 0 ở cả tick và move, đến khi đủ điều kiện down mới tại 5.2.1. Không dùng các bảng dự kiến này làm bằng chứng test đã chạy.

### 5.3 Mixer và Boost

Đề xuất tọa độ joystick: x dương sang phải, y dương tiến; đổi dấu y màn hình tại UI. D-Pad tiến/lùi `(0,±1)`, trái/phải `(∓1,0)`. Giá trị runtime NaN/vô hạn cho cả cặp PWM 0; số hữu hạn ngoài [-1,1] clamp trước khi tính. Cấu hình NaN/vô hạn bị từ chối lưu.

Joystick dùng dead zone xuyên tâm: r=hypot(x,y); r≤deadZone thì 0, ngoài vùng chết dùng biên độ `min(1, sensitivity*(min(r,1)-deadZone)/(1-deadZone))` theo vector đơn vị. D-Pad dùng vector trục đầy đủ, không chịu sensitivity/deadZone. Nhân trục quay với turnPercent/100; trộn `left=y+x`, `right=y-x`, chia cả hai cho `max(1,abs(left),abs(right))`, rồi nhân gain từng bánh. PWM thường = kết quả ×255×limiter/100. Làm tròn biên độ tuyệt đối gần nhất, .5 lên, rồi gắn lại dấu và clamp ±255; limiter 30/60/100% ở calibration mặc định cho tiến ±77/153/255.

Boost chỉ có hiệu lực ngoài trung tính: bỏ limiter, chia cặp sau gain cho max biên độ của cặp rồi ×255; giữ dấu và tỷ lệ hai bánh, ít nhất một bánh đạt biên độ 255. Ví dụ joystick chéo có thể là `(255,0)`, không ép bánh đang 0 chạy. Đây là **làm rõ cần duyệt** cho câu “ép PWM lên 100%” của SRS, không mặc định firmware đã làm giống app. Receiver bản thử coi L/R là PWM đã tính, B chỉ ghi trạng thái, **không nhân/boost lần hai**. Quick turn riêng luôn ±255; thời gian 200 ms không được coi là góc 180° đã hiệu chỉnh.

## 6. Danh mục thông số đề xuất

Các miền sau là **miền phần mềm cho bản thử**, không chứng minh giới hạn cơ khí. Giá trị mặc định calibration giữ 1/0/1/1/100 đúng kế hoạch để không đổi kỳ vọng các test cơ bản.

| Trường | Mặc định đề xuất | Miền hợp lệ | Hiệu lực và cách chứng minh |
|---|---|---|---|
| Limiter | 60% | Số nguyên 30–100 | Live trên mixer; F01 giữ cùng APK/version và so PWM 77/153/255 |
| sensitivity | 1.0 | Số hữu hạn 0.25–2.0 | Joystick sau vùng chết, F03 so đầu ra với cùng tọa độ |
| deadZone | 0.0 | Số hữu hạn 0–0.30 | Bán kính normalized; F03 kiểm tra tâm/biên và rescale |
| leftGain/rightGain | 1.0/1.0 | Mỗi số hữu hạn 0.50–1.00 | Bù tỷ lệ bánh; không cho vượt ±255, F03; cần hiệu chỉnh xe thật |
| turnPercent | 100% | Số nguyên 30–100 | Trọng số trục quay cho điều khiển thường; F03. Không giảm tốc quick turn SRS |
| Quick turn duration | 200 ms | Số nguyên 1–2000 ms | App lưu, receiver thực thi timer từ snapshot đã ACK; F02: 200→350 ms |
| Weapon interaction | HOLD | HOLD hoặc TOGGLE | Áp dụng ở reducer, gửi W đầy đủ; cả hai chế độ bắt buộc kiểm tra |
| Input selection | D_PAD | D_PAD hoặc JOYSTICK | Đổi khi đã dừng/xóa input; U02/U05 |
| Endpoint bản thử | 10.0.2.2, 8888 | IPv4 literal 4 octet 0–255, không wildcard/multicast/broadcast; port nguyên 1–65535 | Chỉ nhãn simulator; F07/I09 đóng endpoint cũ rồi mở mới. Bản xe chưa có mặc định được xác nhận |
| Send interval | 30 ms | Số nguyên 20–50 ms | Đổi một scheduler duy nhất sau dừng; F04 đo 20/30/50; STOP/Kill không chờ |
| Drive command template | `S:{L},{R},{B},{W},{K}\n` | Chỉ template này hoặc alias `D:{L},{R},{B},{W},{K}\n` đã định nghĩa trong SUMO-SIM/1 | Chỉnh prefix S→D làm thay đổi byte thật, simulator hiểu cùng schema; C10/F05 kiểm tra. Không tùy ý đảo/bỏ trường an toàn |
| Tham số cơ cấu do firmware thực thi | Chưa có cho xe thật | Chỉ mở trường khi có capability/schema/miền và đơn vị đã duyệt | Q/ACK ở mục 7; lỗi/timeout/unsupported giữ cấu hình hiệu lực cũ, F09 |
| Feed cập nhật | Chưa cấu hình | HTTPS tuyệt đối, host hợp lệ, không userinfo/fragment; chỉ đích quản trị đã xác nhận | Chưa có URL thì thông báo chưa cấu hình; không dựng URL giả; A01/A08 |

Sensitivity/deadZone/gain/turnPercent/duration/endpoint/interval/template/weapon mode/input mode chỉ sửa sau khi phiên lái đã dừng; không cho lưu giữa gesture. Validation toàn bản, hủy hoặc sai một trường giữ nguyên dữ liệu và giá trị hiệu lực cũ. Không cho tùy chỉnh PWM ±255, ngưỡng STOP 10 ms, watchdog 300 ms, quyền bỏ qua chữ ký hay tắt fail-safe.

Template được biên dịch từ danh mục cho phép, đúng mỗi placeholder, không CR/newline lồng, không mã thực thi; UI nhận cách viết `\n` rồi codec chuyển thành một byte LF. Prefix khác chỉ được bổ sung sau khi hợp đồng receiver được duyệt; chưa có chức năng gửi chuỗi tùy ý đến xe. Các mẫu M/B/W/K/T gốc vẫn là fixture đối chiếu SRS, không coi tự động tương thích với receiver mới.

Lưu cấu hình nhỏ bằng API nền tảng, có `schemaVersion=1`, ghi nguyên bản cấu hình đã validation. Phân biệt “đã lưu trên app”, “đang chờ receiver” và “đã ACK áp dụng”; duration/thiết lập cơ cấu chỉ báo hiệu lực receiver sau phản hồi khớp. Sau khi dừng/đóng phiên để sửa Settings, nếu cần gửi cấu hình receiver thì mở phiên mới trung tính, chỉ gửi neutral/config, khóa lái tới khi đối soát ACK; không khôi phục phiên/thao tác cũ. Lần mở lại/nâng APK giữ giá trị, thêm mặc định chỉ trường mới; schema lạ/lỗi migration giữ bản cũ để phục hồi, báo lỗi và khóa điều khiển thay vì xóa toàn bộ. Không lưu pointer, mode AUTO đang chạy, Boost, macro hoặc phiên UDP. Reset mặc định cần xác nhận và trạng thái dừng. F06/F08/F10 và A10/A11 phải kiểm tra thật ở các phase sau.

## 7. Hợp đồng bản thử SUMO-SIM/1-draft1

**Toàn bộ mục này chờ duyệt G0, chưa phải firmware contract.** Khung ASCII, một datagram UDP chứa đúng một frame, một LF cuối (`0x0A`), không CR/NUL/BOM/trailing bytes, tối đa **256 byte gồm LF**. Trong bảng/mã dưới đây `\n` biểu diễn một byte LF, không phải hai byte backslash+n. Số thập phân không dấu `+`, không khoảng trắng; cờ 0/1; PWM -255…255. SID/nonce là 16 ký tự hex thường, không toàn 0; seq và operation ID 1…2147483647, không wrap, hết miền phải tạo phiên mới.

### 7.1 Mở phiên, xác nhận và hết hạn

- App mở socket mới cho mỗi lần Reconnect, nonce mới và input trung tính; gửi `H1:<nonce>\n`. Receiver tạo SID ngẫu nhiên mới, ràng buộc với peer IP/port và lần boot; trả `H1:<nonce>,<sid>,<caps>\n`. Bit capability: 1=state/weapon boolean, 2=timed turn, 4=AUTO, 8=servo config fixture. Simulator thường caps=3. Đây là capability kỹ thuật, không thay thế phê duyệt thể lệ/cơ khí.
- Offer sống 1000 ms; cùng nonce/peer trong thời gian đó nhận lại cùng SID, không làm mới hạn offer. Khi đã có phiên active, HELLO không chiếm quyền/thay phiên: trả `N1:<nonce>,BUSY\n`. Offer hết hạn thì SID cũ vô hiệu.
- App xác nhận bằng frame seq=1, X=0, mode=0, turnId=0, duration=0, S toàn 0. Chỉ sau ACK hợp lệ mới Online. Receiver đang Kill vẫn giữ Kill trong ACK; handshake không mở khóa. Timeout bắt tay 1000 ms; retry HELLO 250 ms, tối đa trong deadline đó; không tự lặp reconnect vô hạn.
- Frame điều khiển có seq tăng toàn cục trong SID; chỉ nhận peer/SID đúng. Sau khi envelope hợp lệ, seq mới được ghi làm mốc đã thấy kể cả payload bị từ chối; không áp dụng frame seq thấp hơn. Duplicate trả lại ACK đã biết nếu còn cache, không thực thi lại và không làm mới watchdog. Frame không parse được nguồn/SID/seq thì bỏ, không thay trạng thái.
- Sau **300 ms không có frame điều khiển mới, hợp lệ, được áp dụng**, receiver đưa motor về 0, B/W/K=0, hủy macro, về MANUAL và **hủy SID**. Dữ liệu lỗi, duplicate, HELLO, Q/config hoặc SID hết hạn không kéo dài watchdog. Kill latch vẫn giữ nếu đã bật. Timer phải tick độc lập với socket receive. Sau timeout phải handshake lại; gói tới muộn của SID cũ không được làm chạy lại.
- App Offline sau 300 ms từ ACK tiến triển cuối cùng (hoặc hết deadline bắt tay), hủy input/macro và đóng phiên; lỗi I/O/quyền báo ngay. Không có reply không được báo Online. Chỉ ACK đúng endpoint/SID, thuộc seq thực đã gửi, lớn hơn seq đã ACK và còn trong cửa sổ 300 ms mới cập nhật trạng thái/ping. RTT lấy clock monotonic app từ send tới ACK đó, không phải độ trễ một chiều.

SID/nonce chống nhận nhầm phiên và replay phiên đã hết hạn, **không phải xác thực mật mã**. Bản thử chỉ bind loopback trên máy phát triển. Khi chốt kết nối xe thật cần đánh giá mạng/đối thủ và hợp đồng firmware; không coi UDP thử là kênh đã được bảo vệ cho thi đấu.

### 7.2 Snapshot, Kill, mode, macro và cấu hình

Khung snapshot: `V1:<sid>,<seq>,<X>,<mode>,<turnId>,<durationMs>|S:<L>,<R>,<B>,<W>,<K>\n`. Alias D thay S được hỗ trợ đúng cùng 5 trường. X=1 yêu cầu Kill, X=0 **không** tự mở Kill latch; mode 0=MANUAL, 1=AUTO. turnId=duration=0 nghĩa không macro và hủy macro đang chạy. Không macro thì duration bắt buộc 0. Macro khác 0 yêu cầu duration 1–2000, L/R/B/W/K/X/mode đều 0 trong yêu cầu; receiver thực hiện bánh -255/+255, B=0. Snapshot định kỳ giữ cùng turnId/duration để duy trì phiên trong lúc macro, không ghi đè PWM macro bằng L/R=0.

Mỗi lần bấm quick turn có ID tăng riêng trong phiên. Receiver giữ ID cao nhất, duration và trạng thái xong/hủy đến hết SID. ID mới chỉ được bắt đầu khi đầu ra trước đó trung tính, MANUAL, không Brake/Kill/vũ khí và không macro khác đang chạy; lặp ID đang chạy được phép duy trì, không cần motor đang 0. ID mới bắt đầu đúng một lần; cùng ID không khởi động lại kể cả đã xong; cùng ID đổi duration bị từ chối, ID cũ hơn bị từ chối. Snapshot turnId=0 kết thúc/hủy macro nhưng không xóa lịch sử ID. Macro timer độc lập chu kỳ gửi; hết timer dừng dù app còn gửi ID đó. Nếu ACK kết thúc bị mất, app tiếp tục cùng ID vẫn không xoay lần nữa.

Kill hợp lệ ép 0, hủy macro, W/K/B=0 và về MANUAL. Brake K=1 ép 0, B/W=0, macro=0, mode=MANUAL. Receiver kiểm tra mâu thuẫn trường; yêu cầu thường không hợp lệ không duy trì watchdog. Khi đã Kill, yêu cầu chuyển động bị LOCKED. Unlock riêng: `V1:<sid>,<seq>,0,0,0,0|U\n`, chỉ chấp nhận từ phiên đúng, receiver đang trung tính và không Brake/macro; ACK trả X=0. App chỉ sinh U qua thao tác mở khóa ở mục 5.2. Vào AUTO chỉ được chấp nhận từ đầu ra trung tính, không macro/Brake/Kill và có capability cần thiết; request mode1 phải có L/R/B/W/K/X/turnId/duration đều 0, heartbeat AUTO cũng giữ các trường này 0 để không chèn lệnh manual. Fixture chỉ giữ PWM0; motor tự hành thật thuộc firmware chưa xác nhận. Thoát AUTO bằng request mode0 trung tính được phép ngay, atomically dừng trước khi về MANUAL; không yêu cầu chờ thuật toán tự hành tự dừng. Kill/Brake luôn ưu tiên. App chờ ACK trước khi hiển thị mode có hiệu lực và chỉ nhận thao tác manual mới sau đó.

Đóng phiên: envelope trung tính với payload `C`, receiver dừng/hủy macro, về MANUAL, giữ Kill latch hiện tại, ACK rồi retire SID. App cố gửi STOP/C trước khi đóng, không chờ mạng trên UI; nếu không nhận ACK ghi chưa xác nhận, receiver phải dựa watchdog. Trước tải/cài update, nếu không có ACK dừng/đóng thì đợi ít nhất deadline watchdog của phiên cũ, hiển thị chưa xác minh dừng vật lý, không tự tuyên bố xe đã dừng.

ACK state: `A1:<sid>,<seq>,<result>,<L>,<R>,<B>,<W>,<K>,<X>,<mode>,<turnId>,<turnActive>\n`. State trong ACK là **đầu ra áp dụng hiện tại** của receiver. result thuộc `OK`, `LOCKED`, `BAD`, `UNSUPPORTED`, `BUSY`; lỗi có envelope đủ tin cậy mới trả ACK, không làm mới watchdog. ACK không đồng nghĩa bánh đã dừng cơ học. Sender không giữ hàng đợi snapshot chuyển động cũ: mọi lần encode lấy state mới nhất trong một bộ điều phối; STOP/Kill xóa việc gửi lỗi thời đang chờ và phát ngay. Gói đã lên mạng dựa seq/SID phía nhận để loại cũ.

Các mức cơ cấu vật lý chưa xác định nên chưa bật trường đó cho xe thật. Để test F09, fixture caps=11 có thể hỗ trợ `V1:<sid>,<seq>,0,0,0,0|Q:<configId>,<homeDeg>,<activeDeg>\n`; hai giá trị nguyên 0–180, mặc định fixture 0/90, chỉ áp dụng khi neutral/MANUAL. Receiver validation và áp dụng **cả cặp nguyên khối**, không áp dụng từng trường; reply `Q1:<sid>,<seq>,<configId>,<homeDeg>,<activeDeg>,<result>\n` echo bản cấu hình yêu cầu, chỉ `OK` mới xác nhận đã lưu cả cặp. `UNSUPPORTED`/`BAD` không thay cấu hình receiver. Cùng configId/cặp giá trị được retry với seq mới và ACK idempotent; cùng ID nhưng cặp khác bị từ chối. App chỉ commit cặp sau ACK đúng. Nếu timeout có thể ACK đã mất sau khi receiver áp dụng: giữ bản app cũ, ghi **receiver chưa xác nhận**, khóa điều khiển đến khi đối soát/gửi lại bản cấu hình có ACK; không tuyên bố receiver chắc chắn còn bản cũ. Không dùng 0/90 làm mặc định cơ khí xe. Firmware chưa xác nhận servo/miền thì capability này tắt, trường ẩn hoặc ghi không hỗ trợ; không tự gửi góc vào cơ cấu chưa biết. Q/ACK cấu hình không duy trì watchdog lái và không thay thế ACK state để báo Online.

Gói S nguyên bản và các lệnh M/B/W/K/T gốc có fixture exact bytes riêng để đối chiếu, nhưng **không được gửi raw bên ngoài V1** trong SUMO-SIM/1. Phải kiểm tra lại firmware tại Phase 6; không có cơ chế tự đoán giao thức hoặc fallback raw khi handshake lỗi.

### 7.3 Ví dụ byte và trạng thái mong đợi

Nonce `1111111111111111`, SID `0123456789abcdef` dưới đây chỉ là fixture cố định; chạy thật phải tạo mới. Các dòng độc lập là **mẫu dự kiến chưa chạy**; khi thực hiện theo chuỗi phải dùng seq tăng và bảo đảm điều kiện trung tính tương ứng.

| Tác động | Frame app → simulator dự kiến | ACK/đầu ra dự kiến |
|---|---|---|
| HELLO | `H1:1111111111111111\n` | `H1:1111111111111111,0123456789abcdef,3\n` |
| Xác nhận/đứng yên | `V1:0123456789abcdef,1,0,0,0,0|S:0,0,0,0,0\n` | `A1:0123456789abcdef,1,OK,0,0,0,0,0,0,0,0,0\n` |
| Tiến limiter 60% | `V1:0123456789abcdef,2,0,0,0,0|S:153,153,0,0,0\n` | `A1:0123456789abcdef,2,OK,153,153,0,0,0,0,0,0,0\n` |
| Giữ Boost khi tiến | `V1:0123456789abcdef,3,0,0,0,0|S:255,255,1,0,0\n` | `A1:0123456789abcdef,3,OK,255,255,1,0,0,0,0,0,0\n` |
| Nhả hướng, Boost vẫn chạm | `V1:0123456789abcdef,4,0,0,0,0|S:0,0,0,0,0\n` | Hai bánh 0, B hiệu lực 0; ACK seq4 toàn 0 |
| W hold/toggle bật lúc đứng yên | `V1:0123456789abcdef,5,0,0,0,0|S:0,0,0,1,0\n` | `A1:0123456789abcdef,5,OK,0,0,0,1,0,0,0,0,0\n` |
| Nhả hold/toggle tắt | `V1:0123456789abcdef,6,0,0,0,0|S:0,0,0,0,0\n` | W=0; ACK seq6 toàn 0 |
| Giữ Brake | `V1:0123456789abcdef,7,0,0,0,0|S:0,0,0,0,1\n` | `A1:0123456789abcdef,7,OK,0,0,0,0,1,0,0,0,0\n` |
| Nhả Brake | `V1:0123456789abcdef,8,0,0,0,0|S:0,0,0,0,0\n` | K=0 và PWM 0; không chạy lại hướng cũ |
| Kill | `V1:0123456789abcdef,9,1,0,0,0|S:0,0,0,0,0\n` | `A1:0123456789abcdef,9,OK,0,0,0,0,0,1,0,0,0\n` |
| Unlock có chủ ý | `V1:0123456789abcdef,10,0,0,0,0|U\n` | `A1:0123456789abcdef,10,OK,0,0,0,0,0,0,0,0,0\n` |
| Yêu cầu AUTO trên caps=3 | `V1:0123456789abcdef,11,0,1,0,0|S:0,0,0,0,0\n` | `A1:0123456789abcdef,11,UNSUPPORTED,0,0,0,0,0,0,0,0,0\n`; vẫn MANUAL |
| AUTO trên fixture caps=7 | Cùng frame mode1, trong phiên fixture được phép riêng | ACK `OK`, mode1, PWM0; chỉ test mode, không chứng minh tự hành |
| Về MANUAL | `V1:0123456789abcdef,12,0,0,0,0|S:0,0,0,0,0\n` | ACK mode0/PWM0 trước thao tác mới |
| Quick turn lần 1, 200 ms | `V1:0123456789abcdef,13,0,0,1,200|S:0,0,0,0,0\n` | `A1:0123456789abcdef,13,OK,-255,255,0,0,0,0,0,1,1\n` |
| Giữ heartbeat macro cùng ID | `V1:0123456789abcdef,14,0,0,1,200|S:0,0,0,0,0\n` | Không reset timer; sau 200 ms ACK PWM0, turnId1/active0 |
| Hủy macro/STOP | `V1:0123456789abcdef,15,0,0,0,0|S:0,0,0,0,0\n` | PWM0/macro hủy; nhận lại seq13 không đổi trạng thái/watchdog |
| Đổi template đã hỗ trợ | `V1:0123456789abcdef,16,0,0,0,0|D:153,153,0,0,0\n` | Byte prefix thay đổi, đầu ra như S; template khác whitelist bị chặn tại app |
| Đóng phiên | `V1:0123456789abcdef,17,0,0,0,0|C\n` | ACK PWM0 rồi SID vô hiệu; frame seq18 của SID cũ bị bỏ |
| Config trên fixture caps=11, phiên riêng | `V1:0123456789abcdef,2,0,0,0,0|Q:1,0,100\n` | `Q1:0123456789abcdef,2,1,0,100,OK\n`; caps=3 trả UNSUPPORTED, không áp dụng |

Gói đúng seq nhưng mất STOP trên mạng vẫn có thể duy trì đầu ra trước đó đến khi có snapshot mới/watchdog; UDP không bảo đảm giao nhận. Seq chỉ loại được gói đã bị thứ tự mới vượt qua, không tự đo tuổi gói từ timestamp hai máy. Đề xuất này không hứa dừng cơ học trong 10 ms khi mất mạng; H04/H05/H08 phải kiểm chứng receiver thật và giới hạn mạng tại G6.


## Phase 1 and 2 plan (verbatim)

## 6. Phase 1 — Lõi điều khiển và an toàn bằng unit test

**Mục tiêu:** Chứng minh đầu ra PWM và trạng thái đúng trước khi nối UI/mạng.  
**Tệp:** `ControlState.kt`, `DriveMixer.kt`, `ControlReducer.kt` và hai test tương ứng trong bảng mục 3.  
**Phụ thuộc:** G0 đã duyệt. **Đầu ra dùng cho Phase 2–4:** `WheelPwm`, `ControlState` và reducer theo mục 4.

### Các bước

- [x] 1.1 Viết test tham số cho D-Pad, joystick, limiter, Boost và giá trị biên; chạy để thấy lỗi đúng nguyên nhân.
- [x] 1.2 Viết phép trộn tối thiểu, clamp/normalize theo D02; không đổi ý nghĩa hai bánh để làm test xanh.
- [x] 1.3 Viết test nhiều pointer, thứ tự nhấn/nhả, cancel, Kill/unlock và mode; L17–L22 theo D04 đã duyệt, assert ngay sau sự kiện và các tick/move tiếp theo.
- [x] 1.4 Triển khai reducer giữ trạng thái theo pointer; nhả riêng Boost/vũ khí không làm mất hướng hợp lệ. D04 đã duyệt: nhả/mất vùng một pointer điều hướng dừng PWM/B và vô hiệu điều hướng cũ, chỉ nhận down mới sau khi nhấc hết; không xóa HOLD/TOGGLE vì nhả điều hướng.
- [x] 1.5 Viết test trường hợp tranh chấp: hai hướng đối nhau, Boost không có hướng, Brake khi Boost, Kill khi đang xoay.
- [x] 1.6 Chạy toàn bộ test logic, ghi hồ sơ G1; đủ kỹ thuật chuyển Phase 2 theo ủy quyền và trình duyệt chung sau đó.
- [x] 1.7 Thêm test F01/F03 lõi cho limiter và calibration lấy từ cấu hình; phần UI/store vẫn ở Phase 3.

**Bằng chứng thực hiện:** [biên bản G1](../../testing/phase-1/BIEN_BAN_DUYET.md), lượt 01–06 red/green; G1 cuối 37/37 JUnit (36 chức năng + 1 mẫu), lint 0 error/12 warning. Lượt hồi quy toàn module sau Phase 2: 54/54; không thay số lịch sử G1 bằng số test mới.

Ví dụ test đầu ra theo đề xuất D02, chỉ viết/chạy sau khi D02/G0 được duyệt; đặt trong `DriveMixerTest.kt`:

~~~kotlin
package com.aistudy.myapplication.control

import org.junit.Assert.assertEquals
import org.junit.Test

class DriveMixerTest {
    @Test
    fun forwardAtSixtyPercentIs153OnBothWheels() {
        assertEquals(
            WheelPwm(153, 153),
            DriveMixer.mix(x = 0f, y = 1f, limitPercent = 60, boost = false)
        )
    }

    @Test
    fun boostAtCenterDoesNotCreateMovement() {
        assertEquals(
            WheelPwm(0, 0),
            DriveMixer.mix(x = 0f, y = 0f, limitPercent = 60, boost = true)
        )
    }
}
~~~

Đây là ví dụ test cụ thể cho hai ca, không thay thế các ca trong bảng:

| Test | Kích thích | Kỳ vọng dự kiến, chỉ áp dụng sau duyệt G0 |
|---|---|---|
| L01 | Tiến/lùi tại limiter 100% | `255,255` / `-255,-255` |
| L02 | Trái/phải tại 100% | Hai bánh ngược dấu, đúng chiều |
| L03 | Tiến/lùi tại limiter 30%, 60%, 100% | Biên độ 77, 153, 255 theo D02; giữ đúng dấu và không vượt mức |
| L04 | Giữ Tiến, giữ/nhả Boost | Tăng tối đa rồi trở lại limiter; hướng không mất |
| L05 | Không có hướng, Boost bật | Hai bánh 0 |
| L06 | Nhả điều hướng khi Boost còn giữ | Hai bánh/B hiệu lực 0 ngay và các tick sau, không tự phục hồi từ pointer cũ; chuỗi đầy đủ ở L20 |
| L07 | Pointer 0 Tiến, pointer 1 Boost; nhả pointer 1 | Tiến còn, Boost tắt |
| L08 | Thứ tự ngón đảo lại hoặc pointer ID không liên tiếp | Kết quả độc lập với thứ tự/ID |
| L09 | Joystick trung tâm, bốn góc và chéo | Không vượt ±255; tâm bằng 0; chiều đúng |
| L10 | X/Y NaN/vô hạn hoặc hữu hạn ngoài [-1,1] | NaN/vô hạn tạo PWM 0; hữu hạn được clamp/normalize, không vượt ±255 |
| L11 | Cơ cấu hold và toggle, chạm lặp | Chuyển trạng thái đúng chế độ; release không đảo toggle lần nữa |
| L12 | Brake khi đang chạy/Boost | Hành vi hãm ưu tiên đúng D03; không chỉ đổi màu nút |
| L13 | Kill khi có nhiều ngón hoặc macro | Trạng thái chuyển động bị khóa, không hồi lại khi thả một ngón |
| L14 | Unlock/reconnect rồi chưa có thao tác mới | Trung tính |
| L15 | Cancel, đổi mode điều khiển, chuyển MANUAL/AUTO | Không giữ đầu vào cũ hoặc cho manual chen vào AUTO |
| L16 | Xoay nhanh rồi Kill/Brake/cancel | Macro bị hủy theo hợp đồng |
| L17 | Tiến + Lùi → nhả Lùi khi Tiến còn giữ → nhấc hết → down Tiến mới | Limiter 60%/calibration mặc định: (153,153) → (0,0) khi hai hướng đối nhau → nhả Lùi vẫn (0,0), B=0 và pointer Tiến cũ bị vô hiệu; mọi tick/move giữ 0, nhấc hết vẫn 0; chỉ down mới cho (153,153) |
| L18 | Tiến + Trái → nhả Trái khi Tiến còn giữ | Trước nhả: (0,153) theo mixer tại 60%; ngay sau nhả và mọi tick/move của Tiến cũ: (0,0), B=0; nhấc hết chưa down vẫn 0, down Tiến mới cho (153,153) |
| L19 | Tiến + Boost → nhả riêng Boost | (255,255), B=1 → (153,153), B=0 ngay và các tick sau; pointer Tiến vẫn hợp lệ, không yêu cầu nhấc/chạm lại; mở rộng assert chuỗi của L04/L07 |
| L20 | Tiến + Boost → nhả Tiến khi Boost còn giữ | Ngay sau nhả và mọi tick/move: (0,0), B hiệu lực=0; Boost không tự sinh hướng. Khi điều hướng cũ đã nhấc, down Tiến mới trong lúc Boost vẫn giữ được (255,255), B=1; không yêu cầu nhấc Boost |
| L21 | Nhả điều hướng khi vũ khí HOLD hoặc TOGGLE đang bật, chạy riêng từng chế độ | Ngay sau nhả và mọi tick: PWM=(0,0), B=0, W=1; HOLD giữ pointer vũ khí, chỉ up vũ khí đưa W=0; TOGGLE giữ latch W=1 dù không còn pointer vũ khí, chỉ down vũ khí lần kế tiếp mới tắt. Nhả điều hướng không thay thế cancel toàn cục/Kill/Brake |
| L22 | D-Pad/joystick mất vùng chạm hợp lệ, kéo trở vào; down điều hướng đến trước khi pointer cũ nhấc hết; đổi thứ tự và ID pointer | PWM/B=0 ngay lúc mất vùng và mọi tick/move; pointer ra ngoài vẫn phải nhấc thực sự. Down đến sớm bị bỏ qua, giữ/move nó sau khi pointer cũ cuối cùng nhấc không phục hồi chạy; phải có down mới sau mốc nhấc hết. Lặp L17–L21 với ID không liên tiếp/thứ tự đảo; W giữ theo chế độ |

**L17–L22 đã được G0 duyệt và đã chạy ở reducer Phase 1.** Bảng trên giữ kích thích/kỳ vọng, kết quả thực và tên test/XML được ánh xạ trong biên bản G1. Kiểm tra bằng sự kiện/đồng hồ kiểm soát được, không sleep. Ở Phase 4 còn phải assert tick sender chỉ đọc PWM/B hiệu lực đã dừng, không phục hồi từ pointer cũ; chưa coi unit test là tích hợp sender đã đạt.

Run:

~~~powershell
./gradlew.bat --offline :app:testDebugUnitTest --tests "com.aistudy.myapplication.control.DriveMixerTest" --tests "com.aistudy.myapplication.control.ControlReducerTest" --console=plain
~~~

**Kỳ vọng:** lệnh thoát 0, các ca mới đạt, không chỉ test phép cộng mẫu.

### Điều kiện duyệt G1

- [x] L01–L22 và F01/F03 lõi đạt kỹ thuật theo G0; có test/XML thực trong hồ sơ G1, không có ca skip.
- [x] Có bằng chứng test bắt lỗi trước khi triển khai phần logic tương ứng.
- [x] Không dùng sleep/wall clock để chứng minh tính đúng đắn của reducer.
- [x] Reducer không sinh PWM khác 0 sau nhả điều hướng, Kill hoặc mất quyền điều khiển cho tới thao tác/ACK mới hợp lệ theo G0; chưa kết luận cho sender/UI.
- [ ] Người dùng duyệt kết quả G1/bảng input-output sau đợt Phase 1+2. Thiết kế hành vi đã duyệt G0; kết quả test hiện CHỜ DUYỆT CHUNG G1/G2.

## 7. Phase 2 — Codec và robot mô phỏng độc lập

**Mục tiêu:** Chứng minh app mã hóa đúng và phía nhận hiểu đúng hợp đồng bản thử.  
**Tệp:** `CommandCodec.kt`, `ReplyTracker.kt`, 3 lớp test protocol, `tools/sumo_simulator.py`, `tools/test_sumo_simulator.py`.  
**Đầu vào:** trạng thái Phase 1 và bản tin G0. **Đầu ra:** codec, reply hợp lệ và simulator có log.

### Các bước

- [x] 2.1 Viết test exact bytes cho các bản tin đã duyệt, gồm newline và miền số/cờ.
- [x] 2.2 Triển khai encoder và decoder tối thiểu; giữ bản tin mẫu SRS trong test để kiểm tra tương thích đã chọn.
- [x] 2.3 Viết receiver Python chỉ dùng thư viện chuẩn: socket, time, json, argparse, unittest; không dùng lại encoder app làm oracle.
- [x] 2.4 Thêm trạng thái hai bánh, cơ cấu, phanh, Kill, mode và macro đúng hợp đồng; watchdog có clock kiểm soát được trong self-test.
- [x] 2.5 Thêm phản hồi hợp lệ, log JSONL và các scenario ở mục 13.3 để tái hiện lỗi.
- [x] 2.6 Chạy self-test và cross-check dữ liệu mẫu giữa Kotlin/Python; đề nghị duyệt chung G1/G2.

**Bằng chứng thực hiện:** [biên bản G2](../../testing/phase-2/BIEN_BAN_DUYET.md), red/green/fixture/trace/XML thực. Toàn module 54/54 JUnit, simulator 31/31 self-test, 25/25 vector Kotlin/Python; có 1 ca DatagramSocket ↔ tiến trình Python thật. Các lỗi mạng/watchdog kiểm tra bằng clock điều khiển được; chưa đo mạng tới xe thật.

Ví dụ test bản tin S gốc trong `CommandCodecTest.kt`:

~~~kotlin
package com.aistudy.myapplication.protocol

import com.aistudy.myapplication.control.WheelPwm
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandCodecTest {
    @Test
    fun compositePacketContainsEveryFlagAndOneNewline() {
        assertEquals(
            "S:200,200,1,0,0\n",
            CommandCodec.encodeDrive(
                pwm = WheelPwm(200, 200),
                boost = true,
                weapon = false,
                brake = false
            )
        )
    }
}
~~~

Nếu G0 chọn hợp đồng mở rộng, giữ test gói S này như test format gốc nếu format còn được hỗ trợ; thêm exact-bytes test cho toàn bộ frame mới. Không dùng test S để chứng minh đã gửi được Kill, mode hoặc metadata.

| Test | Kỳ vọng |
|---|---|
| C01 | Đứng yên, tiến/lùi, xoay, cờ 0/1 được mã hóa đúng từng byte |
| C02 | Kill/unlock, mode, thời gian và ID xoay có bản tin và phản hồi đúng G0 |
| C03 | Thiếu/thừa trường, quá dài, số lỗi, PWM ngoài biên, thiếu terminator bị từ chối theo hợp đồng |
| C04 | Không nhận nhầm chuỗi literal backslash-n thành newline |
| C05 | Reply sai phiên/thứ tự hoặc sai nguồn không làm Online hay cập nhật ping |
| C06 | Bản tin không hợp lệ không làm mới watchdog |
| C07 | Simulator có PWM 0 tại mốc 300 ms không có lệnh hợp lệ, kể cả đang Boost/macro |
| C08 | Macro cùng một ID được gửi lặp chỉ thực hiện một lần |
| C09 | Gói di chuyển cũ đến sau STOP/Kill không làm xe mô phỏng chạy lại |
| C10 | Sửa template hợp lệ cho kết quả mới; template lỗi bị chặn trước khi gửi |

Run:

~~~powershell
./gradlew.bat --offline :app:testDebugUnitTest --tests "com.aistudy.myapplication.protocol.CommandCodecTest" --console=plain
py -3.13 tools/sumo_simulator.py --self-test
~~~

**Kỳ vọng:** cả hai lệnh thoát 0. Self-test simulator phải có kiểm tra parser và watchdog, không chỉ in “OK”.

### Điều kiện duyệt G2

- [x] C01–C10 đạt kỹ thuật; log thể hiện trạng thái áp dụng, không chỉ raw bytes đã nhận.
- [x] Fixture bản tin và mã phiên bản SUMO-SIM/1-draft1 được lưu vào biên bản/checksum.
- [x] Simulator xử lý gói xấu và thời gian giả lập bằng assert.
- [x] Ghi rõ kết quả watchdog là ở simulator, chưa phải firmware.
- [ ] Người dùng duyệt bảng lệnh và log đối chiếu. CHỜ DUYỆT CHUNG G1/G2; dừng trước Phase 3.

