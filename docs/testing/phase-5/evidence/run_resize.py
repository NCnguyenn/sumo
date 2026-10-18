"""E03 resize asserts neutral/no replay; staying Online is permitted by E03."""
import json
from pathlib import Path
import queue
import re
import subprocess
import sys
import threading
import time
import xml.etree.ElementTree as ET
from run_android import ROOT, ADB, PACKAGE

OUT = ROOT / 'docs/testing/phase-5/evidence/10-resize'


def main():
    assert not OUT.exists(); OUT.mkdir(); transcript = []
    def adb(*args, binary=False):
        command = [ADB, '-s', 'emulator-5554', *args]
        result = subprocess.run(command, capture_output=True, timeout=40)
        transcript.append(dict(command=command, exit_code=result.returncode,
            output='<PNG>' if binary else (result.stdout + result.stderr).decode('utf-8', errors='replace')))
        assert result.returncode == 0, transcript[-1]
        return result.stdout
    def write(data):
        result = subprocess.run([ADB, '-s', 'emulator-5554', 'shell', 'run-as', PACKAGE, 'sh', '-c', '"cat > shared_prefs/sumo_settings.xml"'], input=data, capture_output=True)
        assert result.returncode == 0
    def tree(name):
        adb('shell', 'uiautomator', 'dump', '/sdcard/sumo-phase5-resize.xml')
        raw = adb('shell', 'cat', '/sdcard/sumo-phase5-resize.xml'); (OUT / (name + '.xml')).write_bytes(raw)
        return ET.fromstring(raw)
    def center(node):
        a, b, c, d = map(int, re.findall(r'\d+', node.get('bounds', '')))
        return str((a + c) // 2), str((b + d) // 2)
    def rows(): return [json.loads(line) for line in (OUT / 'receiver.jsonl').read_text().splitlines()]
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == b'1'
    assert b'Override size' not in adb('shell', 'wm', 'size')
    original = adb('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/sumo_settings.xml')
    (OUT / 'original-settings.xml').write_bytes(original)
    server = None; hold = None
    try:
        adb('shell', 'am', 'force-stop', PACKAGE)
        write((ROOT / 'docs/testing/phase-5/evidence/10-lifecycle-keyboard-fix/macro-test-fixture.xml').read_bytes())
        command = [sys.executable, '-u', str(ROOT / 'tools/sumo_simulator.py'), '--port', '8889', '--log', str(OUT / 'receiver.jsonl')]
        server = subprocess.Popen(command, stdout=subprocess.PIPE, text=True, creationflags=subprocess.CREATE_NO_WINDOW)
        ready = queue.Queue(); threading.Thread(target=lambda: ready.put(server.stdout.readline()), daemon=True).start()
        assert ready.get(timeout=5).strip() == 'READY 8889'
        adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity'); current = tree('before')
        connect = next(n for n in current.iter('node') if n.get('text') == 'Kết nối')
        adb('shell', 'input', 'tap', *center(connect)); current = tree('online')
        forward = next(n for n in current.iter('node') if 'TIẾN' in n.get('content-desc', '')); x, y = center(forward)
        index = len(rows())
        hold = subprocess.Popen([ADB, '-s', 'emulator-5554', 'shell', 'input', 'swipe', x, y, x, y, '5000'], creationflags=subprocess.CREATE_NO_WINDOW)
        deadline = time.monotonic() + 3
        while not any(r['applied']['L'] for r in rows()[index:]):
            assert time.monotonic() < deadline; time.sleep(.02)
        adb('shell', 'wm', 'size', '900x1600'); hold.wait(timeout=8)
        current = tree('resized')
        assert any('PWM 0 / 0 · B0' in n.get('text', '') for n in current.iter('node'))
        (OUT / 'resized.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
        sample = rows()[index:]; first = next(r for r in sample if r['applied']['L'])
        zero = next(r for r in sample if r['at_ms'] >= first['at_ms'] and r['event'] == 'accept' and not r['applied']['L'] and not r['applied']['R'])
        assert zero['at_ms'] - first['at_ms'] < 5000, 'No STOP before injected finger release'
        assert all(not r['applied']['L'] and not r['applied']['R'] for r in sample if r['at_ms'] >= zero['at_ms'])
        offers = sum(r['event'] == 'offer' for r in rows())
        adb('shell', 'wm', 'size', 'reset'); current = tree('restored')
        assert any('PWM 0 / 0 · B0' in n.get('text', '') for n in current.iter('node'))
        assert sum(r['event'] == 'offer' for r in rows()) == offers
        (OUT / 'summary.json').write_text(json.dumps(dict(resize_while_held=True, stop_before_release_ms=zero['at_ms'] - first['at_ms'],
            no_motion_after_stop=True, restored_size_neutral=True, no_new_session_on_resize=True,
            note='E03 requires no resumed motion; Online+neutral is allowed for resize/Leave, unlike background or reboot'), indent=2))
        print('E03 resize/restore while held: STOP before release, no replay, same session')
    finally:
        adb('shell', 'am', 'force-stop', PACKAGE); adb('shell', 'wm', 'size', 'reset'); write(original)
        if hold is not None and hold.poll() is None: hold.terminate(); hold.wait(timeout=5)
        if server is not None: server.terminate(); server.wait(timeout=5)
        (OUT / 'commands.json').write_text(json.dumps(transcript, indent=2), encoding='utf-8')


if __name__ == '__main__': main()
