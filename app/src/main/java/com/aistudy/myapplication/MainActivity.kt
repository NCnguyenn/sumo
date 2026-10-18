package com.aistudy.myapplication

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import com.aistudy.myapplication.control.*
import com.aistudy.myapplication.settings.*
import com.aistudy.myapplication.ui.*
import com.aistudy.myapplication.ui.theme.MyApplicationTheme
import com.aistudy.myapplication.updates.*
import com.aistudy.myapplication.diagnostics.*
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity:ComponentActivity() {
    private val reducer=ControlReducer()
    private var control by mutableStateOf(reducer.state)
    private var saved by mutableStateOf(StoredSettings())
    private var connection by mutableStateOf("Offline · chưa kết nối receiver bản thử")
    private var settingsOpen by mutableStateOf(false)
    private var updateOpen by mutableStateOf(false)
    private var diagnosticsOpen by mutableStateOf(false)
    @Volatile private var diagnosticLog:DiagnosticLog?=null
    private var diagnosticSummary by mutableStateOf(DiagnosticSummary())
    private var diagnosticSnapshot by mutableStateOf(DiagnosticSnapshot())
    private var diagnosticNow by mutableLongStateOf(System.nanoTime()/1_000_000L)
    private var pendingExport:String?=null
    private val exportLog=registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->
        val report=pendingExport;pendingExport=null
        if(uri!=null && report==null)problem="Nhật ký chờ xuất không còn; hãy mở Chẩn đoán và xuất lại. Chưa ghi dữ liệu vào file."
        if(uri!=null && report!=null)storageIo.execute {
            try {
                requireNotNull(contentResolver.openOutputStream(uri)).bufferedWriter(Charsets.UTF_8).use{it.write(report)}
                runOnUiThread{if(!isDestroyed)Toast.makeText(this,"Đã xuất nhật ký",Toast.LENGTH_SHORT).show()}
            }catch(e:Exception){runOnUiThread{if(!isDestroyed)problem="Không xuất được nhật ký: ${e.message}"}}
        }
    }
    private var problem by mutableStateOf<String?>(null)
    private var loadFailed=false
    private var persistedKill=false
    private var loaded=false
    private var wantsConnect=false
    private var linkWasOnline=false
    private var runtime:ControlRuntime?=null
    private var receiver by mutableStateOf<ControlState?>(null)
    private val lanPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted){wantsConnect=true;maybeConnect()}else {
            connection="Offline · quyền mạng nội bộ bị từ chối; nhấn Kết nối lại để thử lại"
            storageIo.execute{diagnosticLog?.record("${System.nanoTime()/1_000_000L} closed confirmed=false reason=Thiếu quyền mạng nội bộ trên điện thoại")}
        }
    }
    private val storageIo=Executors.newSingleThreadExecutor()
    private lateinit var store:ControllerSettingsStore
    private lateinit var updater:AppUpdateManager
    private var updateStatus by mutableStateOf(UpdateStatus())
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        pendingExport=savedInstanceState?.getString("sumo.pendingExport")
        persistedKill=getSharedPreferences("sumo_safety",MODE_PRIVATE).getBoolean("kill",false)
        if(persistedKill){reducer.kill();control=reducer.state}
        requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window,false)
        window.attributes=window.attributes.apply{layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES}
        WindowCompat.getInsetsController(window,window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars());systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        store=ControllerSettingsStore(this);updater=AppUpdateManager(this){updateStatus=it}
        InstallResultReceiver.result.value=getSharedPreferences("sumo_install_result",MODE_PRIVATE).getString("message","").orEmpty()
        storageIo.execute {
            diagnosticLog=DiagnosticLog(java.io.File(filesDir,"diagnostics"),appInfo="App ${updater.versionLabel()} · API ${Build.VERSION.SDK_INT}")
            val loaded=store.load()
            runOnUiThread {if(!isDestroyed)loaded.fold({
                saved=it;reducer.applySettings(it.controller);control=reducer.state
                runtime=ControlRuntime(it.controller,persistedKill,::hasLanPermission,{value->runOnUiThread{if(!isDestroyed){
                    control=value.control;saved=saved.copy(controller=value.settings);connection=value.message;receiver=value.applied
                    persistKill(value.control.killed)
                    if(linkWasOnline!=value.control.connected){linkWasOnline=value.control.connected;window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM);android.util.Log.i("SumoSession","haptic connected=$linkWasOnline")}
                    if(value.control.connected)window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }}},{android.util.Log.i("SumoSession",it);diagnosticLog?.record(it)})
                this.loaded=true
            },
                {loadFailed=true;problem="Không nạp được cấu hình; dữ liệu gốc vẫn giữ. Điều khiển bị khóa: ${it.message}"})}
        }
        setContent {
            MyApplicationTheme {
                val version=remember{updater.versionLabel()}
                val metadata="App $version · API ${Build.VERSION.SDK_INT} · ${saved.controller.host}:${saved.controller.port} · ${saved.controller.sendIntervalMs} ms"
                LaunchedEffect(Unit) {while(isActive) {
                    diagnosticLog?.let{diagnosticSummary=it.summary();if(diagnosticsOpen)diagnosticSnapshot=it.snapshot()}
                    diagnosticNow=System.nanoTime()/1_000_000L
                    delay(250)
                }}
                if(diagnosticsOpen)DiagnosticsScreen(diagnosticSnapshot,diagnosticLog?.previousFailure(),metadata,diagnosticNow,"${control.pwm.left} / ${control.pwm.right}",
                    {diagnosticsOpen=false},{history->
                        val clipboard=getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("Sumo diagnostic",diagnosticReport(history)))
                        Toast.makeText(this,"Đã sao chép nhật ký",Toast.LENGTH_SHORT).show()
                    },{history->pendingExport=diagnosticReport(history);exportLog.launch("sumo-diagnostic-${System.currentTimeMillis()}.txt")})
                else ControllerScreen(control,saved.controller,connection,::event,::connect,
                    {stopControl{if(!loadFailed)settingsOpen=true else problem="Cấu hình lỗi; không ghi đè bản gốc"}},
                    {stopControl{updateOpen=true}},::persistLimiter,receiver,diagnosticSummary,diagnosticNow,
                    {stopControl{diagnosticsOpen=true}})
                if(settingsOpen)SettingsDialog(saved,::saveSettings,{settingsOpen=false})
                if(updateOpen)AppUpdateDialog(updater.versionLabel(),saved.updateFeed,updateStatus,InstallResultReceiver.result.value,
                    {updater.check(saved.updateFeed)},{updater.download(saved.updateFeed)},{
                        if(packageManager.canRequestPackageInstalls())updater.install(saved.updateFeed)
                        else startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:$packageName")))
                    },updater::cancel,{updateOpen=false})
                val pending=InstallResultReceiver.pending.value
                LaunchedEffect(pending) {
                    if(pending!=null) {
                        InstallResultReceiver.pending.value=null
                        try{startActivity(pending)}catch(e:Exception){problem="Không mở được xác nhận Android: ${e.message}. Chưa cài bản mới."}
                    }
                }
                problem?.let{message->AlertDialog(onDismissRequest={problem=null},title={Text("Thông báo")},text={Text(message)},
                    confirmButton={TextButton(onClick={problem=null}){Text("Đóng")}})}
            }
        }
    }
    private fun diagnosticReport(history:Boolean):String {
        val s=saved.controller
        val metadata="App ${updater.versionLabel()} · API ${Build.VERSION.SDK_INT}\nEndpoint=${s.host}:${s.port} · SUMO-SIM/1-draft1\n"+
            "Interval=${s.sendIntervalMs}ms · limit=${s.limitPercent}% · input=${s.inputMode} · weapon=${s.weaponMode}\nCalibration=${s.calibration} · turn=${s.turnDurationMs}ms\nApp yêu cầu PWM ${control.pwm.left} / ${control.pwm.right}"
        return if(history)diagnosticLog?.previousFailure().orEmpty() else diagnosticLog?.report(metadata) ?: "Chưa có nhật ký"
    }
    private fun event(value:ControlEvent) {
        val callbackAt=System.nanoTime()
        if(value is ControlEvent.Press && value.button==ControlButton.WEAPON){window.decorView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);android.util.Log.i("SumoSession","haptic weapon")}
        if(value is ControlEvent.Limiter && loaded)saved=saved.copy(controller=saved.controller.copy(limitPercent=value.percent))
        val current=runtime
        if(current!=null)current.post(value,callbackAt)
        else if(value==ControlEvent.Kill){reducer.kill();control=reducer.state;persistKill(true)}
    }
    private fun hasLanPermission()=Build.VERSION.SDK_INT<37 || ContextCompat.checkSelfPermission(this,"android.permission.ACCESS_LOCAL_NETWORK")==PackageManager.PERMISSION_GRANTED
    private fun connect() {
        if(!loaded || loadFailed){problem="Chưa nạp được cấu hình; chưa mở socket";return}
        wantsConnect=true
        if(hasLanPermission())maybeConnect() else lanPermission.launch("android.permission.ACCESS_LOCAL_NETWORK")
    }
    private fun maybeConnect(){if(wantsConnect && loaded && hasWindowFocus()){wantsConnect=false;runtime?.connect()}}
    private fun stopControl(then:()->Unit={}) {
        wantsConnect=false
        val current=runtime
        if(current==null){reducer.disconnect();control=reducer.state;then()}
        else {control=control.copy(pwm=WheelPwm(0,0),boost=false,weapon=false,brake=false,connected=false,turnId=0,turnActive=false)
            current.stop{runOnUiThread{if(!isDestroyed)then()}}}
        receiver=null;window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private fun persistKill(killed:Boolean) {
        if(killed!=persistedKill){persistedKill=killed;getSharedPreferences("sumo_safety",MODE_PRIVATE).edit().putBoolean("kill",killed).apply()}
    }
    private fun saveSettings(value:StoredSettings) {
        stopControl{storageIo.execute {
            try{store.save(value);runtime?.applySettings(value.controller){runOnUiThread{if(!isDestroyed){saved=value;settingsOpen=false}}}}
            catch(e:Exception){runOnUiThread{problem="Không lưu: ${e.message}"}}
        }}
    }
    private fun persistLimiter() {
        val value=saved
        storageIo.execute{try{store.save(value)}catch(e:Exception){runOnUiThread{problem="Chưa lưu limiter: ${e.message}"}}}
    }
    override fun onSaveInstanceState(outState:Bundle){outState.putString("sumo.pendingExport",pendingExport);super.onSaveInstanceState(outState)}
    override fun onPause(){stopControl();super.onPause()}
    override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(!hasFocus)stopControl() else maybeConnect()}
    internal fun flushControlTrace()=runtime?.flushTrace()==true
    override fun onDestroy(){runtime?.close();updater.close();storageIo.shutdown();super.onDestroy()}
}
