"""AVD-only native network check; restores Wi-Fi/mobile-data in finally."""
from pathlib import Path
import json
import subprocess
import time

root=Path(__file__).resolve().parents[4]
adb=str(Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe')
evidence=Path(__file__).parent
commands=[]
def run(*args,timeout=90):
    command=[adb,'-s','emulator-5554',*args]
    result=subprocess.run(command,capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=timeout)
    commands.append(dict(command=command,exit=result.returncode,stdout=result.stdout,stderr=result.stderr))
    return result.stdout+result.stderr

assert run('shell','getprop','ro.kernel.qemu').strip()=='1'
wifi=run('shell','settings','get','global','wifi_on').strip()
mobile=run('shell','settings','get','global','mobile_data').strip()
summary=dict(passed=False)
try:
    for apk in ('app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'):
        assert 'Success' in run('install','-r',str(root/apk))
    run('shell','svc','wifi','disable');run('shell','svc','data','disable')
    deadline=time.monotonic()+10
    while True:
        state=run('shell','dumpsys','connectivity')
        default=next(line for line in state.splitlines() if line.startswith('Active default network:'))
        if default.endswith('none') or time.monotonic()>=deadline:break
        time.sleep(.25)
    summary['active_default']=default
    summary['wan_route']=run('shell','ip','route','get','1.1.1.1').strip()
    summary['lan_route']=run('shell','ip','route','get','10.0.2.2').strip()
    assert default.endswith('none'),'AVD still has a default Internet network'
    assert 'unreachable' in summary['wan_route'].lower(),'WAN route was not removed'
    assert 'dev eth0' in summary['lan_route'],'Host LAN is not available independently'
    output=run('shell','am','instrument','-w','-r','-e','class','com.aistudy.myapplication.AndroidFaultTest',
               '-e','scenario','normal','com.aistudy.myapplication.test/androidx.test.runner.AndroidJUnitRunner')
    assert 'OK (1 test)' in output and 'FAILURES!!!' not in output,'Local UDP UI test failed'
    summary['passed']=True
except Exception as error:summary['error']=str(error)
finally:
    run('shell','svc','wifi','enable' if wifi=='1' else 'disable')
    run('shell','svc','data','enable' if mobile=='1' else 'disable')
    (evidence/'29-network-commands.json').write_text(json.dumps(commands,ensure_ascii=False,indent=2),encoding='utf-8')
    (evidence/'29-network-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False,indent=2))
raise SystemExit(0 if summary['passed'] else 1)
