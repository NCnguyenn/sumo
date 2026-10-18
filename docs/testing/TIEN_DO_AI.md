# Checkpoint — G5 chốt có điều kiện cho chức năng AVD

18/10/2026, Asia/Bangkok. Workspace `C:\Users\CHI NGUYEN\AndroidStudioProjects\IoT`.

## Mới nhất — merge và APK cài thử cho nhóm, 18/10

- Người dùng đã cho phép kiểm tra, commit, merge vào main local, push GitHub và đóng gói APK mới để gửi nhóm. Quyền mới áp dụng mã/tài liệu và **APK debug thử UI/cấu hình/nhật ký, không nối xe**; thay chỉ dẫn lịch sử không commit/push trong các đợt trước. Không tự nghiệm thu xe, đóng lỗi timing hoặc phát hành production.
- Rà soát trước merge không tìm thấy lỗi mới chặn phạm vi này. Hai FAIL Android cũ có lỗi đồng bộ test: đọc output ngay sau enqueue, và chờ nhãn macro200ms thoáng qua. Đã dùng wait output+ACK với timeout cũ; ca mở updater giữa macro dùng fixture1000ms, app mặc định vẫn200ms. Không đổi mã sản phẩm hay ngưỡng STOP/cadence trong đợt bàn giao.
- Build/lint exit0, **120/120 unit mới đạt**,0skip; lint0lỗi/17cảnh báo. Unit allocation thu đủ1000gap/136.088byte trong3,944s; thêm counters để chẩn đoán, không tăng timeout30s hoặc giới hạn256.000byte. FAIL cũ vẫn giữ; chưa chứng minh căn nguyên lần thiếu mẫu cũ.
- **10/10 Android mới đạt**,0skip ở `phase-5/evidence/42-group-test-regression/`, gồm cả hai ca FAIL cũ. App vẫn đúng SHA256 b31ac740afa3515c8cc5a2a344061e170c63a5972ca4beef2058c3ac7c5c736f; test APK đã đổi. Không gọi 120+10 là toàn bộ test mới của APK tăng versionCode.
- Đã commit5138d9b, merge main032f260 và push origin/main; build APK nhóm version1.0/code2 từ main bằng signer debug d97c9c… đang có, không đổi package/key. Tệp `group-test-2026-10-18/apk/sumo-controller-ui-test-1.0-v2.apk`,SHA256 **40b22dbee250894bae9d7a0f9b3f0beb0fdac7dbd3af9aed259267af9c777078**,30.943.197byte. Chữ kýv2/zipalign16K đạt;10DEX cùng byte nhưng hoán vị tên,resources/native không đổi.
- Audit hoàn chỉnh07 exit0: cài đècode2→code2 Success, bản cài đúngSHA,3prefs trước/sau cài/sau mở giống nguyên byte; ảnh/XML Offline/PWM0/AUTO khóa đã xem. Các lỗi assert công cụ04/05/06 giữ nguyên, không nhận hash prefs lượt07 là lần đầu1→2. Chi tiết ở [bàn giao nhóm](group-test-2026-10-18/BAN_GIAO_NHOM.md). APK không đưa vào Git/Release; tệp bàn giao riêng để người dùng gửi nhóm, yêu cầu Android16+.
- Đã dừng app/đóng AVD do đợt này mở; không còn QEMU/thiết bị ADB/receiver8888–8889. `group-test-2026-10-18/evidence/08-final-environment.json` ghi trạng thái. Bước tiếp theo thuộc người thử: nhận APK và kiểm trên điện thoại theo hướng dẫn; chưa tự chuyển Phase6/7.
- STOP/cadence và soak30phút của UI cuối vẫn mở. Điện thoại/API36/HTTPS/khóa phân phối ổn định/firmware/xe thật chưa kiểm chứng; G5 có điều kiện, G3/G4 chờ duyệt.

## Quyết định và phạm vi

- Người dùng chọn trong chat: **“Chốt G5 CÓ ĐIỀU KIỆN cho chức năng AVD; giữ lỗi hiệu năng mở, chặn dùng xe và release”** sau khi nhận kết quả mới. Không suy thành người dùng đã trực tiếp thử trên điện thoại.
- G0/G1/G2 đã duyệt; G3/G4 vẫn CHỜ DUYỆT CHUNG. Không tự chuyển Phase6/7, dùng xe, upload hoặc release.
- Ngoại lệ đã duyệt trước đo: cấu hình50ms trên AVD chấm20–60ms;20/30ms vẫn20–50ms;STOP vẫn≤10ms. Không đổi thêm ngưỡng hoặc bỏ mẫu FAIL.
- Đợt tiếp tục18/10 đã hoàn tất kiểm chứng độc lập trên cùng APK cuối; không đổi mã/test/tool/SDK/dependency. Không chạy lặp timing chọn lượt đẹp; giữ nguyên quyết định gate và mọi FAIL.

## Mã và bằng chứng cuối

- `ControlSession.revision` và cache snapshot ở `ControlRuntime` giảm cấp phát poll rỗng2ms; giữ lịch/ưu tiên/ACK/seq/replay. `MainActivity` bỏ CPU sampler chẩn đoán mặc định; mốc monotonic và trace gói vẫn giữ.
- `ControlRuntimeTest` thêm test allocation và publish trạng thái; red488.160→green136.376byte/1000gap, lượt cuối136.088byte. JVM, không suy thành latency ART.
- `tools/check_android_cadence.py` chỉ bật cận60ms với `--g5-avd-50-jitter`; mặc địnhG0 giữ nguyên. `tools/test_android_cadence.py`6/6; red01/green02 và hồi quy05 giữ FAIL626mẫu của log08.
- **110/110 unit**: Gradle `:app:testDebugUnitTest`, exit0 ở `phase-5/evidence/16-g5-avd-approval/23-final-unit-check.txt` và `23-final-unit-xml/`. Lint0lỗi/17cảnh báo và app/test APK build hoàn tất ở log20; toàn lệnh20 từng exit1 vì timeout test allocation, được sửa và kiểm lại ở23.
- **6/6 Android trên APK cuối**, gồm4UDP/tích hợp+2thu timing: `phase-5/evidence/20-g5-profiler-off-cohort/`. Test thu mẫu đạt không đồng nghĩa ngưỡng timing đạt.
- **8/8 fault trên APK cuối**: `phase-5/evidence/22-g5-final-faults/`, wrapper `16-g5-avd-approval/27-final-faults.txt` exit0.
- 28/28UI/Settings/UDP ở `19-g5-related-functional/` là build cache trước khi bỏ CPU sampler; không cộng trùng với6 hoặc nói đã chạy đủ37ca trên APK cuối.
- Soak08/Python31/31/vector25/25 giữ lịch sử. **Soak33 trên APK cuối đã chạy/audit riêng:** 30 phút 0,668 giây, 600 chu kỳ, 20 reconnect, 10 Kill/mở khóa; 60.557 frame/21 SID, không revival/đảo seq; 20 checkpoint worker/trace 1/1, FD socket 14→10 sau đóng. Bằng chứng: `33-final-functional-soak/soak-summary.json`.
- **25/25 Android, 0 fail/skip trên APK cuối** ở33: 23 UI/Settings/updater + soak + probe. Không cộng với6 hoặc28 cũ thành tổng ca độc lập. `final-binding.json` xác nhận63 file nguồn khớp cohort21, app/test hash không đổi; unit/lint/UDP/8fault tái dùng bằng chứng đã khớp.

## Lỗi mở và môi trường

- Cohort21 STOP500: max trước send9,8024ms, sau send11,4174ms; **2/500 sau send>10ms, FAIL**. Cadence20/30/50 đủ1245/1339/1282interval, max30,9317/38,7934/54,4796ms, đạt cận đã duyệt trong thí nghiệm power policy.
- Cohort Normal20 trên cùng APK cuối còn33/1281cadence50>60ms,max76,8416ms;STOP3/500>10ms,max14,0058ms. Không coi cohort21 là bảo đảm ổn định ở mọi điều kiện.
- Mọi cohort16/17/18/20/21 và CSV/raw/JSON giữ trong hồ sơ. Không quy hết lỗi cho AVD/Windows hoặc tuyên bố tối ưu đã sửa latency.
- Thí nghiệm cũ đã khôi phục QEMU Normal/power0/0. Phiên18/10 không đổi ưu tiên/power policy; đã tắt trace/receiver/AVD do phiên mở. `33-final-functional-soak/final-environment.json` xác nhận không còn QEMU/UDP8888/8889/thiết bị ADB; ảnh/XML trước đóng chứng minh app Offline, PWM0, AUTO khóa.
- Điện thoại/HTTPS A→B→C/API36/I11/hosting/khóa phân phối+backup/firmware và xe thật CHƯA KIỂM CHỨNG. Trước dùng xe hoặc release phải giải quyết lỗi hiệu năng, kiểm chứng các điều kiện mục tiêu và có quyền tiếp tục.

## Đầu ra để đọc tiếp

- [Biên bản G5](phase-5/BIEN_BAN_DUYET.md), [báo cáo bổ sung](phase-5/KET_QUA_BO_SUNG_G5.md), [quyết định phạm vi](phase-5/DIEU_CHINH_G5_AVD.md), [ma trận R01–R23](phase-5/MA_TRAN_R01_R23.md).
- [APK debug AVD cuối](phase-5/apk/sumo-phase5-avd-debug-v1-g5-check.apk),versionCode1/min36/target37, SHA256 `92abb62d00eb6f841eb8b9eb158d4b0be4b98bd39c6e05bd39b69de79613e5e3`. Bản08 giữ riêng, không phải bản phân phối điện thoại.
- `phase-5/evidence/16-g5-avd-approval/25-final-build-binding.json` ràng buộc nguồn/test/binary; `28-final-avd-handoff.txt` xác nhận APK thực cài cùng hash. Hồ sơ29 ghi quyết định và kiểm tra tài liệu/checksum cuối, không chạy thêm test.

## Tiếp nhận và xác minh độc lập

- Đã tiếp nhận dự án, nạp đủ 5 tài liệu theo thứ tự; bảo toàn toàn bộ mã nguồn, artifact APK cuối (SHA256 `92abb6...`) và quyết định G5 có điều kiện.
- Lệnh tiếp nhận `:app:testDebugUnitTest` exit0 nhưng task **UP-TO-DATE**: tái dùng kết quả110/110 cũ, không phải110 test thực thi mới. Python simulator31/31 và cadence checker6/6 đã thực thi đạt.
- 18/10: trace scheduler/Binder/ART có sẵn và chạy được qua `atrace`; đính chính nhận định trước rằng thiếu công cụ kernel. Phân rã5STOP lỗi cohort20/21 ở `phase-5/evidence/30-owner-scheduler-diagnostic/prior-outlier-partition.json`; chưa xác định nguyên nhân chung, không sửa phỏng đoán.
- Chẩn đoán trên cùng APK: STOP500 max4,1192ms (`31-owner-traced-stop/`); cadence50 n1285,max52,4731ms (`32-owner-traced-cadence50/`). Không thay FAIL cũ bằng lượt có tracing. Giả thuyết Binder mỗi poll không được ủng hộ; trace STOP có ring overwrite, cadence không overwrite. Raw nén không mất sự kiện.
- Đã chạy `python -X utf8 docs/testing/phase-5/evidence/run_android.py --output docs/testing/phase-5/evidence/33-final-functional-soak --classes Phase5RegressionTest,ControllerUiTest,SettingsUpdateTest,SoakTest`: exit0, 25/25, 1856,589 giây; audit dùng lại `analyze_soak.audit/self_check`, mọi frame/receiver/tài nguyên đạt. Không build/release/upload; APK bàn giao giữ hash ở trên.
- File đổi: checkpoint, biên bản G5, báo cáo bổ sung, ma trận R, hướng dẫn AVD, mô tả dự án; kế hoạch chỉ đồng bộ metadata/gate/kết quả, không viết lại các bước. Bằng chứng mới30–33; không đụng nguồn app/test/tool hoặc artifact cũ.
- **Chưa giải quyết STOP/cadence:** có công cụ trace nhưng hai chẩn đoán không tái hiện vi phạm; chưa đủ căn cứ sửa app. Bước tiếp: khi có giả thuyết mới, bắt mẫu lỗi đồng thời scheduler/ART trace không overwrite và mốc từng đoạn trên điều kiện mục tiêu có ghi nhận, hoặc lỗi deterministic. Thiết bị/HTTPS/API36/I11/hosting/khóa/firmware vẫn thiếu như trên; không tự chuyển phase.

## GitHub và ảnh giao diện — yêu cầu bổ sung 18/10

- Người dùng chỉ định `https://github.com/NCnguyenn/sumo.git` và cho phép đẩy dự án lên repo này, kèm ảnh toàn bộ giao diện. Đây là quyền công bố mã nguồn/tài liệu/ảnh; chưa phải quyền phát hành APK hoặc release. Quyết định G5 và lỗi hiệu năng giữ nguyên.
- Khởi tạo Git nhánh `main`, remote `origin` trỏ đúng repo trên. README gốc dẫn tới [bộ ảnh giao diện](../screenshots/README.md).
- Chụp14ảnh thật,2400×1080, trên AVD API37 với APK cuối SHA256 `92abb62d00eb6f841eb8b9eb158d4b0be4b98bd39c6e05bd39b69de79613e5e3`: D-Pad/joystick Offline/Online, Kill/mở khóa, TOGGLE, toàn bộ phần Cấu hình, xác nhận reset/nguồn cập nhật, lỗi đầu vào và hộp Cập nhật.
- `docs/screenshots/2026-10-18/capture-manifest.json` lưu hash ảnh, APK và trạng thái khôi phục. URL example.com chỉ mở xác nhận rồi Hủy, không lưu/kiểm tra từ mạng; thông báo cài đặt cũ trong ảnh cập nhật được ghi chú rõ.
- Đã sao lưu và khôi phục khớp3file SharedPreferences của AVD; kết thúc Offline,PWM0/0,B0/W0/K0. Đóng receiver loopback và AVD do phiên chụp mở. Không sửa mã app/test/Gradle, không rebuild hoặc chạy lại bộ test.
- `.gitignore` loại khóa ký, cấu hình máy, build/APK và log/trace thô lớn; toàn bộ dữ liệu gốc vẫn giữ ở máy. `.gitattributes` giữ nguyên byte phục vụ đối chiếu checksum. Không đưa APK lên GitHub trong đợt này.

## Giao diện compact và Chẩn đoán —18/10/2026

- Người dùng duyệt mockup v3, yêu cầu sửa app và gửi ảnh thật. Thực hiện tại workspace gốc trên nhánh `codex/ui-compact-diagnostics`; giữ thiết kế/mockup sẵn có. Chưa commit/push UI mới; main/remote trước đó giữ nguyên. Không chuyển Phase6/7 hoặc đổi quyết định gate.
- Đã sửa ControllerScreen/SettingsDialog/AppUpdateDialog/theme và nối MainActivity. Có4giao diện với3tab cấu hình; dải cảnh báo không đổi vị trí nút lái, Boost vẫn lớn nhất trong nhóm tấn công, Kill riêng. Pointer/reducer/mixer và điều kiện nhận ACK/replay giữ nguyên; ReplyTracker thêm lý do loại, ControlSession thêm trace.
- Thêm DiagnosticLog/DiagnosticsScreen: dữ liệu thật, filter nhóm, TX/RX/bị loại, ACK cũ, nguồn lỗi có bằng chứng hoặc Chưa xác định, clipboard/CreateDocument/lịch sử lỗi. Log qua worker có sẵn, status đọc immutable không chờ file I/O, UI250ms, ring240dòng. Phân biệt app yêu cầu với TX cuối; giữ metadata phiên khi open thất bại; lưu/khôi phục pending export khi Activity tái tạo.
- **118/118unit** (117ca dự án+1mẫu), lint0lỗi/17cảnh báo. Đợt34:29/29trên bản trung gian. Đợt35:34/35,0skip;1test Chẩn đoán đọc trước lô UI250ms. Chỉ sửa test chờ dữ liệu; đợt36 **5/5**. Đối chiếu cùng APK app cho **33case chức năng phân biệt đạt**, thêm2case thu timing riêng; không cộng trùng cohort. **8/8fault** mới đạt. Báo cáo và máy kiểm tra tại `ui-compact-2026-10-18/`.
- Timing chỉ1batch35: STOP500 t2max3,8244ms,0>10; cadence20/30/50 n1289/1337/1282,max26,9885/34,3807/53,4177ms,đạt cận AVD đã duyệt. Giữ mọi mẫu và FAIL cũ; **không kết luận đã sửa ổn định STOP/cadence**, G5 vẫn có điều kiện/G3G4 chờ duyệt, chặn xe/release.
- **13ảnh thật** tại `docs/screenshots/ui-compact-2026-10-18/` (12app ngang+1picker Android dọc), có mất ACK thật/ACK153cũ và phiên lỗi giữ sau mở lại. Đã xuất file thực20.059byte. Khôi phục khớp3SharedPreferences; app Offline,PWM0/0,B0W0K0; receiver do phiên chụp mở đã đóng. Kiểm tra XML cũ sau restart của công cụ được giữ và sửa bằng tên XML mới, không phải lỗi app.
- APK debug mới giữ riêng `ui-compact-2026-10-18/apk/sumo-ui-compact-debug.apk`, SHA256 `c8f88cbcb3dd5fb2abdcd1bb7eb0608b2d2c1b27b9bd3ffa7b7d9aafbfcd9f55`. Bản cài AVD khớp; APK G5 cũ92abb6…giữ nguyên. Chưa soak30phút trên bản UI mới, chưa API36/máy thật/HTTPS/I11/hosting/khóa/firmware/xe; chưa upload/release.
- Điểm đọc tiếp: `ui-compact-2026-10-18/KET_QUA.md`, `final-summary.json`, gallery/manifest và kế hoạch `docs/superpowers/plans/2026-10-18-compact-ui-diagnostics.md`. Các log/XML red/green và kết quả FAIL giữ nguyên. Bước tiếp theo cần theo yêu cầu mới của người dùng; không tự nối phase hoặc phát hành.
- Kết thúc đợt UI: đã đóng AVD do phiên này mở, xác nhận không còn UDP8888/8889 (`ui-compact-2026-10-18/final-environment.json`); kiểm tra liên kết, diff và hash source/ảnh/APK đạt. Không còn lệnh build/test/chụp đang chạy.

## Kiểm tra cài thử điện thoại — đã lập kết luận

- Kết luận và toàn bộ đường dẫn: [phone-readiness/KET_LUAN.md](phone-readiness/KET_LUAN.md), [JSON cuối](phone-readiness/final-summary.json), [ảnh thật](phone-readiness/screenshots/INDEX.md). **Cài thử UI/cấu hình/nhật ký: CÓ ĐIỀU KIỆN**, debug tạm và không nối xe. UDP điện thoại/API36/HTTPS A→B→C/khóa phân phối ổn định/firmware/xe chưa kiểm chứng. Giữ G5 có điều kiện/G3G4 chờ duyệt, chặn xe/release; không chuyển phase hoặc upload/commit/push.
- Đối chiếu ban đầu59checksum nguồn khớp hồ sơ compact, APK gốc c8f88c…/test322474…khớp cache; giữ nguyên artifact/bằng chứng cũ và thay đổi chưa commit của nhánh `codex/ui-compact-diagnostics`. AVD ban đầu không có package: cài mới, không gọi cài đè. Không có API36runtime, không đổi SDK/dependency/minSdk/version/key.
- Sửa lỗi tái hiện ở font130% và800×360dp/font150%: Controller cắtACK/Brake/footer,D-Padthiếu chỗ; Diagnostics giấuvalue/log và timestampwrap; updater bópstatusviewport. Giữ cảnh báo cố định,BOOSTlớnnhất,vùngchạm≥48dp;3regression bố cục đạt trênAPKcuối.
- Sửa lỗiunlockcócăn cứ:EndUnlocksaukhiđãgửiU/trướcACKtừngxóapending,gửiX1lại. Regressionphiênclockđiềukhiểnredtrướcsửa,green66/66unitliênquan sau sửa. EndUnlocknaychỉkếtthúchold;CancelUnlockriêngchongónkhác/ra ngoài;Kill/lifecyclehủy nhưcũ. SmokeUDPthậtU441,249s→nhấc441,302s→ACK441,463s,mởkhóaPWM0/0khôngrevival. Build/lintcuối0lỗi/17cảnhbáo.
- Unittoànmodulethựcchạy117/118,1FAILidle-pollawait30s;khôngrerun/nớitimeout,khônggọi118/118lịchsửlàtestmới.66testliênquansauunlockcóchồnglặp,khôngcộngtrùng.FullsuiteAPKcuốichưachạylại.
- Chỉ1batchtiming37trênbinarytrunggian c377…:STOP500t1max25,1894ms/5>10,t2max25,3868ms/6>10 **FAIL**;cadence20/30/50n1279/1337/1281,max36,6047/44,6013/59,8723đạtcậnAVD.Giữmọimẫu/FAILlịchsử,khôngquyAVDlànguyênnhân.KhôngđolạiAPKcuối/khôngđónglỗi.
- Chỉ1lượtthửsoak30phút:FAILngayvòngđầusau5,948s/0cycle/0checkpoint.TestcontainsOnlineđọcnhầm“chưaOnline”,chạmsớmtrướcACK;165frameđềuPWM0,appchặnđúng. SửateststartsWith,khôngsoaklượt2. **Chưa có bằng chứng ổn định30phút trênUI mới/APKcuối**,khôngkếtluậntăngheap/worker/socketdài hạn.
- Cohort37trunggian39/40(36chứcnăng+2thutiming+probeđạt,soakFAIL).39trênaa0…35/36,HủypickerFAILtestgiảđịnhmànkhôngrecreate;đãsửachờsurfaceOfflinetrungtính.40APKcuốibịAVDthoátcode0/adbOffline sau12cađạt;khởiđộnglạikhôngwipe/snapshotrollbackvàtiếp24ca41đạt22/24. Hợp **34/36caAndroidchứcnăngphânbiệtđạt,2FAILcònomở**,khônglặpchọnlượtđẹp. FAILI01sawPWM0sauACK255vàsessionclosechưarõnguyênnhân;FAILmacro200mskhôngbắtnhãntransientdùACKquay→0.8/8faultmớiAPKcuốiđạt.
- APKcụcbộ `phone-readiness/apk/sumo-phone-ui-test-debug.apk`,SHA256 **b31ac740afa3515c8cc5a2a344061e170c63a5972ca4beef2058c3ac7c5c736f**,version1.0/code1,min36/target37,debugcertd97c9c…v2verify.LibnativeARM64/ARMv7/x86/x86_64vàzipalign16Kkiểmthực,khôngsuyARMchạyđược.CàiđèAVDcùnghash/signature/version1giữbyte3prefsvàhistory,khônguninstall/cleardata;khôngphảinângversionHTTPStrongmáythật.
- 16ảnhcuối39–54cùnghashb31…,ảnhtrướcsửa/trunggian01–37giữriêng.Giữlịchsửupdatermã6làfixture,khôngxóađểtrôngđạt. MấtACKthậtcóApp0/0vàACKcũ71/72,nguồnlỗiChưaxácđịnh;ACKkhôngchứngminhđộngcơ. Filexuấtc377…19.397byte;filehiệntạiAPKcuối695bytequaAndroidpickerthật. Khôiphục3prefs/displaygốc,giữhistorylỗimới;ảnh54Offline/PWM0/B0W0K0/AUTOkhóa. Đãgửiadbemu killđóngAVDdo phiên mở,receiverloopbackđãđóng;environment/exit-info/checksumlưuởphone-readiness/evidence/.
