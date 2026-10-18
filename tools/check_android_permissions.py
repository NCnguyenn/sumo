"""Exercise API 37 LAN deny/grant/revoke using the real permission controller on an AVD."""
from pathlib import Path
import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--serial',required=True)
    parser.add_argument('--receiver-log',required=True,type=Path)
    parser.add_argument('--evidence-dir',type=Path,default=Path('docs/testing/phase-4/evidence'))
    args=parser.parse_args()
    root=Path(__file__).resolve().parent.parent
    out=root/args.evidence_dir
    out.mkdir(parents=True,exist_ok=True)
    executable=str(Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe')
    transcript=[]
    def adb(*parts):
        result=subprocess.run([executable,'-s',args.serial,*parts],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=30)
        transcript.append(dict(command=[executable,'-s',args.serial,*parts],exit=result.returncode,output=result.stdout+result.stderr))
        if result.returncode:raise RuntimeError(result.stdout+result.stderr)
        return result.stdout+result.stderr
    def dump(name):
        adb('shell','uiautomator','dump','/sdcard/sumo-phase4-window.xml')
        raw=adb('shell','cat','/sdcard/sumo-phase4-window.xml')
        (out/(name+'.xml')).write_text(raw,encoding='utf-8')
        return ET.fromstring(raw)
    def center(node):
        x0,y0,x1,y1=map(int,re.findall(r'\d+',node.get('bounds','')))
        return str((x0+x1)//2),str((y0+y1)//2)
    def click(tree,test):
        node=next(n for n in tree.iter('node') if test(n))
        adb('shell','input','tap',*center(node))
        return center(node)
    def screenshot(name):
        data=subprocess.check_output([executable,'-s',args.serial,'exec-out','screencap','-p'],timeout=15)
        (out/(name+'.png')).write_bytes(data)
    def records():return [json.loads(s) for s in args.receiver_log.read_text(encoding='utf-8').splitlines() if s.strip()]
    package='com.aistudy.myapplication';permission='android.permission.ACCESS_LOCAL_NETWORK'
    assert args.serial.startswith('emulator-') and adb('shell','getprop','ro.kernel.qemu').strip()=='1'
    assert int(adb('shell','getprop','ro.build.version.sdk').strip())>=37
    try:
        adb('install','-r',str(root/'app/build/outputs/apk/debug/app-debug.apk'))
        adb('shell','am','force-stop',package)
        adb('shell','pm','revoke',package,permission)
        adb('shell','pm','clear-permission-flags',package,permission,'user-set','user-fixed')
        adb('shell','am','start','-W','-n',package+'/.MainActivity')
        tree=dump('permission-before')
        forward=center(next(n for n in tree.iter('node') if 'TIẾN' in n.get('content-desc','')))
        click(tree,lambda n:n.get('text') in ('Kết nối','Kết nối lại'))
        time.sleep(.3)
        tree=dump('permission-dialog-deny')
        click(tree,lambda n:n.get('resource-id','').endswith('/permission_deny_button') or n.get('text') in ("Don't allow",'Don’t allow','Không cho phép'))
        tree=dump('permission-denied')
        assert any('Offline' in n.get('text','') for n in tree.iter('node'))
        assert 'granted=false' in next(line for line in adb('shell','dumpsys','package',package).splitlines() if permission+': granted=' in line)
        screenshot('permission-denied')
        click(tree,lambda n:n.get('text') in ('Kết nối','Kết nối lại'))
        time.sleep(.3)
        tree=dump('permission-dialog-allow')
        click(tree,lambda n:n.get('resource-id','').endswith('/permission_allow_button') or n.get('text') in ('Allow','Cho phép'))
        time.sleep(1)
        assert 'granted=true' in next(line for line in adb('shell','dumpsys','package',package).splitlines() if permission+': granted=' in line)
        screenshot('permission-granted')
        count=len(records())
        holder=subprocess.Popen([executable,'-s',args.serial,'shell','input','swipe',*forward,*forward,'1500'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        due=time.monotonic()+2
        while not any(r['applied']['L'] for r in records()[count:]) and time.monotonic()<due:time.sleep(.02)
        assert any(r['applied']['L'] for r in records()[count:]),'No motion before permission revocation'
        adb('shell','pm','revoke',package,permission)
        holder.wait(timeout=3);time.sleep(.4)
        assert records()[-1]['applied']['L']==records()[-1]['applied']['R']==0
        offers=sum(r['event']=='offer' for r in records())
        adb('shell','am','start','-W','-n',package+'/.MainActivity')
        tree=dump('permission-revoked-reopen')
        assert any('Offline' in n.get('text','') for n in tree.iter('node'))
        assert any('PWM 0 / 0' in n.get('text','') for n in tree.iter('node'))
        assert sum(r['event']=='offer' for r in records())==offers,'Unexpected automatic reconnect'
        screenshot('permission-revoked-reopen')
        (out/'permission-summary.json').write_text(json.dumps(dict(denial_offline=True,grant_via_system_ui=True,motion_before_revoke=True,
            receiver_stopped_after_revoke=True,reopen_offline_neutral=True,automatic_reconnect=False),indent=2),encoding='utf-8')
        print('API37 real permission deny/grant/revoke + neutral reopen passed')
    finally:
        (out/'permission-commands.json').write_text(json.dumps(transcript,ensure_ascii=False,indent=2),encoding='utf-8')
        adb('shell','pm','grant',package,permission)


if __name__=='__main__':main()
