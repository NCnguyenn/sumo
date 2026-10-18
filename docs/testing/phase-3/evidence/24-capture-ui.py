"""Capture the installed debug UI on the explicitly selected AVD; no fake state."""
from pathlib import Path
import json
import re
import subprocess
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[4]
output=Path(__file__).parent
adb=str(Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe')
commands=[]
def run(*args):
    command=[adb,'-s','emulator-5554',*map(str,args)]
    result=subprocess.run(command,capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=30)
    commands.append(dict(command=command,exit=result.returncode,stdout=result.stdout,stderr=result.stderr))
    assert result.returncode==0,result.stderr
    return result.stdout

def tree():
    run('shell','uiautomator','dump','/sdcard/sumo-final.xml')
    return ET.fromstring(run('shell','cat','/sdcard/sumo-final.xml'))

def tap(label):
    node=next((node for node in tree().iter('node') if node.get('text')==label or node.get('content-desc')==label),None)
    assert node is not None,f'No visible control: {label}'
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds','')))
    run('shell','input','tap',(x1+x2)//2,(y1+y2)//2)

def capture(name):
    tree()  # wait for the native UI to be idle after transitions
    run('pull','/sdcard/sumo-final.xml',output/f'{name}.xml')
    run('shell','screencap','-p','/sdcard/sumo-final.png')
    run('pull','/sdcard/sumo-final.png',output/f'{name}.png')

assert run('shell','getprop','ro.kernel.qemu').strip()=='1'
try:
    assert 'Success' in run('install','-r',root/'app/build/outputs/apk/debug/app-debug.apk')
    run('shell','am','start','-W','-n','com.aistudy.myapplication/.MainActivity')
    capture('ui-final-landscape')
    tap('Cấu hình');capture('ui-final-settings')
    tap('Điều hướng: D_PAD');tap('Lưu toàn bộ');capture('ui-final-joystick')
    tap('Cấu hình');tap('Điều hướng: JOYSTICK');tap('Lưu toàn bộ')
    tap('Cập nhật');capture('ui-final-updater');tap('Đóng')
    run('shell','wm','size','900x1600');capture('ui-final-small')
finally:
    run('shell','wm','size','reset')
    (output/'24-ui-capture-commands.json').write_text(json.dumps(commands,ensure_ascii=False,indent=2),encoding='utf-8')
print('Captured 5 real screens; restored D-Pad and physical window size.')
