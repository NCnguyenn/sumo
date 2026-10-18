# Kết quả Phase 2 và hồ sơ trình duyệt chung G1/G2

**Quyết định hiện hành:** Người dùng đã duyệt G1/G2 theo hồ sơ cuối 60/60 và cho phép thực hiện liên tục Phase 3+4, không chờ duyệt giữa đợt. Nội dung “chờ duyệt” còn dưới đây là lịch sử; không sửa bằng chứng cũ thành số liệu mới. Trình G3/G4 sau đợt và dừng trước Phase 5.

Ngày 17/10/2026, Asia/Bangkok. **G0 đã được người dùng duyệt; G1/G2 CHỜ DUYỆT CHUNG.** Phase 1 đủ tiêu chí kỹ thuật đã chuyển Phase 2 theo ủy quyền riêng cho đợt Phase 1+2. Không ghi người dùng đã xem hoặc duyệt các kết quả dưới đây. Dừng trước Phase 3, không tạo bản release.

## Phạm vi và kết quả thực tế

Hợp đồng bản thử: **SUMO-SIM/1-draft1**, theo [G0 mục 7](../phase-0/BIEN_BAN_DUYET.md). Đã có codec ASCII/LF, model frame/reply và ReplyTracker Kotlin thuần; receiver/CLI Python thư viện chuẩn, engine có clock truyền vào và các scenario xác định. Không có dependency mới hoặc thay đổi SDK/Gradle. AUTO mặc định tắt, caps=3; caps=7/11 chỉ phục vụ fixture mode/cấu hình, không phải tự hành hoặc cơ cấu xe thật.

| Kiểm tra cuối | Kết quả thật | Bằng chứng |
|---|---|---|
| Toàn bộ unit test module Android trên JVM | **60 chạy, 0 failure/error/skip**: 59 chức năng và 1 mẫu | [18-final-gradle.txt](evidence/18-final-gradle.txt), [18-final-xml/](evidence/18-final-xml/) |
| Phần Phase 1 trong lượt trên | Mixer 8 + reducer 28 + settings 3 = **39 chức năng** | L01–L22, F01/F03 lõi và ba hồi quy calibration; [hồ sơ G1](../phase-1/BIEN_BAN_DUYET.md) |
| Phần Phase 2 trong lượt trên | Codec 6 + tracker 13 + UDP liên tiến trình 1 = **20** | Ba XML protocol trong thư mục kết quả cuối; gồm ba hồi quy ACK |
| Simulator self-test | **31 chạy, 0 failure/error/skip** | Xác minh lại [16b-current-simulator.txt](evidence/16b-current-simulator.txt), [XML](evidence/16b-current-simulator.xml); Python không đổi sau đó |
| Đối chiếu byte Kotlin → receiver Python độc lập | **25/25 vector khớp request và ACK dự kiến** | Xác minh lại [16-current-crosscheck.txt](evidence/16-current-crosscheck.txt), [kotlin-vectors.tsv](evidence/kotlin-vectors.tsv); codec/fixture không đổi sau đó |
| Lint toàn module | Exit 0, **0 error, 12 warning baseline** | [18-lint.xml](evidence/18-lint.xml); đối chiếu baseline Phase 0 trong kiểm tra hồ sơ cuối |

60 là số phương thức JUnit, 31 là số phương thức unittest; nhiều phương thức có vòng lặp/assert cho nhiều dữ liệu biên. Không cộng 25 vector thành 25 unit test, không dùng test mẫu để kết luận C/L/F. Lượt 13 lịch sử là 54/54; lượt 18 có thêm sáu phương thức hồi quy sau rà soát độc lập, không sửa lại số liệu của lượt cũ.

## Red trước green và lệnh đã chạy

Mỗi log dưới đây có lệnh, output thực và exit code. XML được sao lưu theo từng lượt; không thay kết quả lỗi bằng kết quả xanh. Các bộ lọc JVM chỉ chọn lớp/ca tương ứng, không dùng `--continue` để che lỗi.

| Lượt | Lệnh/nhóm được chạy | Kết quả và bằng chứng trong `evidence/` |
|---|---|---|
| 01 → 02 | Gradle `--tests *.CommandCodecTest` | Red exit 1, **6/6 fail** tại stub chưa triển khai; green exit 0, **6/6 pass**. `01-codec-red.txt`/`01-red-xml/`, `02-codec-green.txt`/`02-green-xml/` |
| 03 → 04 | Tracker riêng rồi codec + tracker | Red exit 1, **9/9 fail** đúng logic tracker còn thiếu; green exit 0, **15/15 pass**. `03-reply-tracker-red.txt`/`03-red-xml/`, `04-reply-tracker-green.txt`/`04-green-xml/` |
| 05 → 05b → 06 | `py -3.13 tools/sumo_simulator.py --self-test --junit <XML>` | 05 exit 1: 29 chạy, 28 failure/1 error do test kiểm tra `None` sai. Sửa test trước triển khai receiver; 05b exit 1: **29 failure/0 error**, stub không có hành vi yêu cầu. 06 exit 0: **29/29 pass**. Giữ cả ba log/XML `05*`, `05b*`, `06*` |
| 07 → 08 | Gradle `--tests *.SimulatorInteropTest` | Red exit 1, **1 fail** tại assertion CLI chưa trả `READY <port>`; green exit 0, **1 pass** sau triển khai CLI UDP. `07-udp-cli-red.txt`/`07-red-xml/`, `08-udp-cli-green.txt`/`08-green-xml/` |
| 09 | Python `--check-fixtures .../kotlin-vectors.tsv --log .../crosscheck-applied.jsonl` | Exit 0, **25/25**. Python tự parse/apply/encode ACK, không gọi encoder ứng dụng |
| 10 → 11 | Ca `C05_okMustConfirmRequestedKillUnlockModeAndWheelState`, rồi cả 3 lớp protocol | Red exit 1, **1 fail** vì ACK OK nhưng sai state Kill vẫn được nhận. Thêm đối chiếu state với request đang chờ; green exit 0, **17/17 pass**. `10-ack-semantics-red.txt`/`10-red-xml/`, `11-ack-semantics-green.txt`/`11-green-xml/` |
| 12 → 12b | Python self-test cuối, thêm 2 ca CLI và xuất trace | 12 exit 1 do biến import `Path` che tên trong runner; **chưa chạy test**, không có XML PASS. Sửa runner; 12b exit 0, **31/31 pass**, log/XML/trace đã lưu |
| 13 | Toàn module unit + lint | Exit 0, **54/54 pass**, lint 0 error/12 warning; `13-module-final.txt`, `13-final-xml/`, `13-lint.xml` |
| 16 | Xác minh lại nguồn hiện có trước review | Gradle 54/54, simulator 31/31, crosscheck 25/25 đạt ngoài sandbox. Sandbox trước đó chặn cache lock/UDP self-send; giữ log lỗi, không sửa mã để che lỗi môi trường |
| 17a → 17b | Mixer/reducer, thêm hồi quy D-Pad chéo với mức quay thấp | Red: 36 test, 2 fail PWM; green: 36/36. [Báo cáo sửa](evidence/17-fix-report.md), XML riêng từng lượt |
| 17c → 17d | Ba lớp protocol, thêm hồi quy ACK khi ý định đổi nhanh hơn RTT | Red: 20 test, 2 fail RTT/progress; green: 20/20. Giữ guard chống áp dụng ACK cũ tới reducer |
| 17e → 18 | Toàn module unit + lint sau hai bản sửa | Cả hai lượt exit 0, **60/60 pass**, lint 0 error/12 warning. Lượt 18 do root chạy xác minh cuối |

Lệnh Gradle dùng JBR 25.0.2, Gradle 9.5.0/AGP 9.3.3, môi trường như G0/G1. Chạy tại gốc workspace bằng PowerShell:

~~~powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_TOOL_OPTIONS = '"-Djdk.net.unixdomain.tmpdir=C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT\.gradle"'
./gradlew.bat --offline :app:testDebugUnitTest :app:lintDebug --console=plain
py -3.13 tools/sumo_simulator.py --self-test --junit docs/testing/phase-2/evidence/12b-simulator-final.xml --log docs/testing/phase-2/evidence/scenarios-applied.jsonl
py -3.13 tools/sumo_simulator.py --check-fixtures docs/testing/phase-2/evidence/kotlin-vectors.tsv --log docs/testing/phase-2/evidence/crosscheck-applied.jsonl
~~~

Lệnh tái chạy sẽ ghi lại các output cùng tên; hồ sơ hiện tại giữ kết quả thực của lượt 12b/13/09. Native stdout/stderr đã được thu trực tiếp bằng `2>&1 | Tee-Object`, kèm `$LASTEXITCODE`; không dùng transcript thiếu output để chứng minh pass. Các lượt Gradle đỏ/xanh cũng dùng `--offline :app:testDebugUnitTest --console=plain` và bộ lọc ghi đầy đủ trong dòng `COMMAND` của log.

## Ánh xạ C01–C10 tới test đã chạy

Tên ngắn dưới đây là hậu tố phương thức trong XML; toàn bộ suite cuối không có skip. Tên phương thức đầy đủ có thể tra trong XML tương ứng.

| Mã | Bằng chứng chức năng |
|---|---|
| C01 | `CommandCodecTest.C01_legacyCompositeAndSrsSamplesAreExactLfBytes`, `C01_C02_exactFrozenKotlinVectorsForIndependentPythonReceiver`; Python `test_C01_C02_frozen_vectors_from_contract`, `test_C01_state_flags_alias_and_no_double_boost`; UDP interop exact ACK bytes. S gốc chỉ kiểm tra format, frame vận hành phải có V1 |
| C02 | Codec `C02_decodeEveryReplyTypeAndPendingState`; fixture Kill/U/macro/mode/Q; Python `test_kill_persists_across_timeout_and_new_handshake_until_unlock`, `test_unlock_and_close_are_safe_explicit_commands`, `test_auto_guard_and_manual_inputs_do_not_override_auto`, hai ca config; ACK turn/mode/unlock ở reducer G1 |
| C03 | Codec `C03_C04_rejectMalformedLfAsciiBoundsAndCounts`, `C03_outgoingFrameSemanticValidationCannotBeBypassed`; Python `test_C03_handshake_requires_neutral_seq_one`, `test_C03_wrong_peer_sid_and_pre_handshake_frames_do_not_apply`, `test_C03_number_boundary_and_exhausted_sequence_cannot_wrap`, `test_C03_raw_legacy_commands_are_fixture_only` và ca malformed |
| C04 | Ca malformed ở cả Kotlin/Python assert thiếu LF, literal backslash+n, CRLF, NUL, byte ngoài ASCII, ghép hai frame, trailing bytes, >256 byte bị từ chối |
| C05 | **13 ca ReplyTrackerTest**: peer/nonce/SID/seq chưa gửi, handshake/TTL, duplicate/old ACK, ACK intent cũ sau STOP, ACK OK sai state, Q không nuôi Online/ping, Close/new begin; bổ sung luồng PWM đổi/RTT50 ms, ACK cũ sai state, ACK an toàn bị thay thế. UDP interop nhận datagram giả từ **socket/port khác thật**, xác nhận không Online |
| C06 | Python `test_C06_payload_rejection_consumes_seq_without_extending_watchdog`, `test_C06_duplicate_has_cached_ack_but_does_not_extend_lease`, ca malformed, HELLO và Q/config; sai/gói cũ không nuôi deadline |
| C07 | Python `test_C07_watchdog_299_300_301_and_retired_sid`, `test_C07_watchdog_overrides_long_macro`, `test_C07_hello_during_active_session_does_not_keep_it_alive`; clock điều khiển được, 300 ms PWM/flags về 0, SID hủy, Kill latch giữ |
| C08 | Python `test_C08_macro_repeats_once_does_not_reset_timer`, `test_C08_macro_id_duration_conflicts_and_new_id_while_busy`; cùng ID seq mới hoặc datagram duplicate không reset timer/đếm chạy; UDP trace có đúng **1 macro-start** |
| C09 | Python `test_C09_old_motion_after_stop_and_kill_never_applies`, `test_C08_C09_stop_brake_kill_cancel_without_macro_restart`, `test_C09_receiver_restart_does_not_accept_old_boot_sid`, ca reorder/delay350; UDP Close/new SID rồi gói SID cũ bị bỏ. Restart là vòng đời receiver/phiên trong test, chưa thử Activity/app Android |
| C10 | Codec `C10_templatesAreWhitelistedAndAffectWireBytes`; bytes S/D thực khác nhau, cùng schema/validation, template bỏ/đảo trường hoặc chèn lệnh bị chặn. Python/UDP xác nhận D áp dụng đúng PWM |

31 self-test còn kiểm tra lỗi endpoint/miền CLI và port đang dùng: process trả exit 2, socket chủ cũ vẫn tự gửi/nhận được. Không dừng tiến trình khác để lấy port.

## Fixture, log và phạm vi thời gian

[fixtures.tsv](fixtures.tsv) có **25 vector cố định**, được chép từ hợp đồng G0 và thêm chuỗi seq đúng, không sinh đáp án từ encoder ứng dụng. Kotlin encode rồi đối chiếu request hex chính xác và decode reply; file [kotlin-vectors.tsv](evidence/kotlin-vectors.tsv) chứa byte encoder thật. Python độc lập áp dụng request và so ACK do chính receiver encode với reply fixture. Hai file giống từng byte, SHA-256: `4051776450d294dfca7c7f8fd319b0116ad5f8ae6fbdcc4e5b209ea1ff0de54f`.

Nonce/SID cố định chỉ dùng fixture; CLI vận hành tạo SID bằng `secrets.token_hex(8)`, ràng buộc peer và phiên. Có chặn seq vượt Int.MAX_VALUE/wrap, seq cũ, confirm không trung tính; timeout hủy SID. Duplicate cache ACK không thực thi lại hoặc làm mới watchdog. Q áp dụng cả cặp nguyên khối/idempotent nhưng không giữ watchdog lái. Tracker đối chiếu ACK với request đã gửi, chặn ý định cũ sau STOP; heartbeat cùng ý định vẫn được ACK tiến triển khi tick mới đang chờ. Chưa có xác thực mật mã hoặc tương thích firmware được xác nhận trong hợp đồng bản thử này.

| Log đã lưu | Môi trường / giới hạn |
|---|---|
| [udp-kotlin-python.jsonl](evidence/udp-kotlin-python.jsonl) | **26 record** từ ca JUnit dùng DatagramSocket Kotlin và tiến trình Python thật qua loopback; 2 offer, 18 accept, 4 reject, 1 macro-start, 1 retire. Clock receiver là monotonic host. Socket timeout trong test chỉ xác nhận không nhận ACK cho SID cũ, không đo giới hạn xe thật |
| [crosscheck-applied.jsonl](evidence/crosscheck-applied.jsonl) | **30 record** replay 25 vector Kotlin qua receiver Python với thời gian/SID cố định; có macro-complete/config-applied. Không phải đo mạng |
| [scenarios-applied.jsonl](evidence/scenarios-applied.jsonl) | **229 record** từ self-test, gắn test ID; clock kiểm soát được cho engine/scenario, gồm 10 watchdog và 8 macro-start. Hai ca CLI dùng process/socket thật; lỗi mạng dùng hàng đợi xác định, không giả làm mất gói ngoài mạng thật |

Mỗi record có `at_ms`, peer, SID/seq, `payload_hex`, accept/reject/lý do và `applied` (PWM/cờ/mode/macro), không chỉ raw bytes. Tick CLI độc lập với nhận datagram, socket timeout 10 ms; con số 10 ms này là độ chờ vòng lặp simulator, **không phải bằng chứng đạt STOP app ≤10 ms**. Deadline watchdog logic kiểm tra tại 299/300/301 bằng clock giả; jitter scheduler của OS/MCU cần đo ở phase phù hợp.

Các scenario đã assert: normal; drop-rx; drop-reply vẫn áp dụng lệnh; delay RX150 ms nhận trước expiry/RX350 ms không hồi sinh SID; delay reply150 ms đúng hạn; duplicate macro một lần; reorder di chuyển cũ sau STOP100 ms không áp dụng; malformed lần lượt thiếu trường/sai SID/future seq. Sai peer kiểm tra riêng bằng unit và socket thật. Delay350 reply cũ/TTL được kiểm tra bằng tracker không chấp nhận ACK muộn, không phải đo độ trễ Android.

CLI mặc định chỉ bind IPv4 loopback (port8888); `--port 0` để kiểm thử tự chọn port. Chạy thủ công cho Phase 4 sau khi được phép:

~~~powershell
py -3.13 tools/sumo_simulator.py --host 127.0.0.1 --port 8888 --scenario normal --log docs/testing/phase-4/simulator-normal.jsonl
~~~

Đổi `--scenario` thành `drop-rx`, `drop-reply`, `delay`, `duplicate`, `reorder`, `malformed`. `--fault-after 2` xử lý bình thường 2 datagram V1 đầu (confirm, lệnh đầu), HELLO không tính; `delay` nhận `--delay-ms 150` hoặc `350` và `--delay-target rx`/`reply`; `reorder` cố định 100 ms. Các lệnh này là cách tái hiện, **chưa chạy tích hợp app Android ở Phase 4**. Emulator sau này dùng `10.0.2.2:8888` để tới host; chưa nối màn hình Hello Android tới simulator.

## Rà soát, thay đổi và quyết định

Đã tự rà soát nguồn/caller/test theo kế hoạch, sau đó có [rà soát độc lập](evidence/15-independent-review.md) theo skill requesting-code-review. Hai lỗi được phát hiện thêm: D-Pad chéo với turnPercent thấp dùng nhầm chuẩn hóa joystick, và ACK của PWM thay đổi bị loại trước khi ghi nhận tiến triển liên lạc. Cả hai đã có test đỏ, sửa tối thiểu và test xanh trong [báo cáo sửa](evidence/17-fix-report.md). ACK cũ hợp lệ có thể duy trì liên lạc/RTT nhưng vẫn trả null để không áp dụng state cũ vào reducer. Các lỗi trước đó (wildcard IPv4 octet 000, turnId không reset, ACK OK sai state) và lỗi test harness/runner giữ log lịch sử riêng.

[Rà soát lại độc lập](evidence/18-independent-rereview.md) xác nhận cả hai lỗi đã xử lý, không có phát hiện mới trong các thay đổi và nhánh liên quan. Kết luận phù hợp yêu cầu và chất lượng cho hai bản sửa là đạt; người dùng vẫn cần duyệt nghiệm thu G1/G2. Báo cáo review đọc nguồn/test/XML, không giả là một lần chạy suite mới.

Nguồn mới: `control/`, `settings/ControllerSettings.kt`, `protocol/`, 6 lớp test chức năng, `tools/sumo_simulator.py`, `tools/test_sumo_simulator.py`; `.gitignore` thêm `__pycache__/`. 35 tệp gốc trong baseline G1 không đổi hash, gồm MainActivity/Manifest/Gradle/test mẫu. Không có Git nên dùng [18-source-final.sha256](evidence/18-source-final.sha256) định danh 51 tệp nguồn/fixture hiện tại (SHA-256 của manifest: `65a9cb63aebfb64297a3cbf5a1aa781b64a0b52777d135a5ab50609aa2517ff1`), không giả có commit. [source-final.sha256](evidence/source-final.sha256) và lượt 14 giữ nguyên để định danh nguồn trước hai bản sửa; không dùng hash cũ mô tả nguồn hiện tại. [Đối chiếu nguồn](evidence/18-source-verification.json) xác nhận baseline gốc, Python/codec không đổi và fixture khớp. Hồ sơ G0/kế hoạch/mô tả/checkpoint được đồng bộ quyết định và kết quả.

**Kết luận kỹ thuật:** G1 (L01–L22/F01/F03 lõi) và G2 (C01–C10 bản thử) đủ bằng chứng trong phạm vi đợt được ủy quyền, không có ca chức năng đang fail/skip. **G1/G2 vẫn CHỜ DUYỆT CHUNG** để người dùng xem bảng đầu ra, test/log và quyết định nghiệm thu. Không tự duyệt thay người dùng hoặc chuyển Phase 3.

**Chưa kiểm chứng:** UI đa chạm/lifecycle Android, SettingsStore/migration, lịch gửi/STOP app ≤10 ms, cấp quyền LAN, updater/cài APK, mạng tới xe/firmware/watchdog MCU/độ trễ <30 ms/góc quay/phanh vật lý. Không phát hành APK mới trong đợt. Hosting HTTPS, khóa ký/chủ quản/backup và endpoint xe thật còn thiếu theo G0; phải chốt trước phân phối/tích hợp, không chặn hai phase độc lập này. Sau khi G1/G2 được duyệt mới thực hiện Phase 3 theo kế hoạch và phạm vi được cho phép.
