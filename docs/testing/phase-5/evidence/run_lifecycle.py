"""E03/F06 on the selected AVD: force-stop, real reboot, resize; preserve settings."""
import json
from pathlib import Path
import queue
import re
import subprocess
import sys
import threading
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[4]
ADB = str(Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe')
PACKAGE = 'com.aistudy.myapplication'
OUT = ROOT / 'docs/testing/phase-5/evidence/10-lifecycle-keyboard-fix'


def main():
    assert not OUT.exists(), 'Preserve previous attempts'
    OUT.mkdir()
    transcript = []
    def adb(*args, check=True, timeout=40):
        parts = [ADB, '-s', 'emulator-5554', *args]
        result = subprocess.run(parts, capture_output=True, timeout=timeout)
        transcript.append(dict(command=parts, exit_code=result.returncode, output=(result.stdout + result.stderr).decode('utf-8', errors='replace')))
        if check: assert result.returncode == 0, transcript[-1]
        return result.stdout
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == b'1'
    original = adb('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/sumo_settings.xml')
    (OUT / 'original-settings.xml').write_bytes(original)
    size = adb('shell', 'wm', 'size').decode()
    assert 'Override size' not in size, 'Do not discard an existing display override'
    children = []; server = None; current_log = OUT / 'receiver.jsonl'
    def rows():
        return [json.loads(line) for line in current_log.read_text().splitlines() if line.strip()]
    def serve(label, scenario='normal'):
        nonlocal server, current_log
        if server is not None: server.terminate(); server.wait(timeout=5)
        current_log = OUT / (label + '.jsonl')
        parts = [sys.executable, '-u', str(ROOT / 'tools/sumo_simulator.py'), '--host', '127.0.0.1', '--port', '8889',
                 '--scenario', scenario, '--fault-after', '20', '--log', str(current_log)]
        server = subprocess.Popen(parts, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, creationflags=subprocess.CREATE_NO_WINDOW)
        children.append(server); transcript.append(dict(start=parts, pid=server.pid))
        ready = queue.Queue(); threading.Thread(target=lambda: ready.put(server.stdout.readline()), daemon=True).start()
        assert ready.get(timeout=5).strip() == 'READY 8889'
    def tree(name):
        adb('shell', 'uiautomator', 'dump', '/sdcard/sumo-phase5-lifecycle.xml')
        data = adb('shell', 'cat', '/sdcard/sumo-phase5-lifecycle.xml')
        (OUT / (name + '.xml')).write_bytes(data)
        return ET.fromstring(data)
    def center(node):
        x0, y0, x1, y1 = map(int, re.findall(r'\d+', node.get('bounds', '')))
        return str((x0 + x1) // 2), str((y0 + y1) // 2)
    def tap(node): adb('shell', 'input', 'tap', *center(node))
    def open_neutral(name, require_offline=True):
        offers = sum(row['event'] == 'offer' for row in rows())
        adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
        current = tree(name)
        if require_offline:
            assert any('Offline' in node.get('text', '') for node in current.iter('node'))
        assert any('PWM 0 / 0' in node.get('text', '') for node in current.iter('node'))
        assert offers == sum(row['event'] == 'offer' for row in rows()), 'Automatic reconnect'
        (OUT / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
        return current
    def start_hold(name):
        current = tree(name + '-before')
        tap(next(node for node in current.iter('node') if node.get('text') == 'Kết nối'))
        deadline = time.monotonic() + 6
        count = len(rows())
        while not any(row['event'] == 'accept' and row['active'] for row in rows()[count:]) and time.monotonic() < deadline:
            time.sleep(.05)
        current = tree(name + '-online')
        assert any('Online' in node.get('text', '') for node in current.iter('node'))
        direction = next(node for node in current.iter('node') if 'TIẾN' in node.get('content-desc', ''))
        x, y = center(direction)
        parts = [ADB, '-s', 'emulator-5554', 'shell', 'input', 'swipe', x, y, x, y, '5000']
        holding = subprocess.Popen(parts, stdout=subprocess.PIPE, stderr=subprocess.PIPE, creationflags=subprocess.CREATE_NO_WINDOW)
        children.append(holding)
        transcript.append(dict(start=parts, pid=holding.pid))
        count = len(rows()); deadline = time.monotonic() + 3
        while not any(row['applied']['L'] or row['applied']['R'] for row in rows()[count:]) and time.monotonic() < deadline:
            time.sleep(.02)
        assert any(row['applied']['L'] or row['applied']['R'] for row in rows()[count:]), 'No actual receiver motion before interruption'
        return holding
    def stopped():
        deadline = time.monotonic() + 3
        while time.monotonic() < deadline:
            applied = rows()[-1]['applied']
            if not any(applied[key] for key in ('L', 'R', 'B', 'W', 'turnActive')): return
            time.sleep(.02)
        raise AssertionError('Receiver did not stop')
    def start_macro(name):
        current = open_neutral(name + '-before')
        count = len(rows())
        tap(next(node for node in current.iter('node') if node.get('text') == 'Kết nối'))
        deadline = time.monotonic() + 5
        while not any(row['event'] == 'accept' and row['active'] for row in rows()[count:]):
            assert time.monotonic() < deadline, 'No neutral handshake ACK'
            time.sleep(.01)
        tap(next(node for node in current.iter('node') if node.get('content-desc', '').startswith('QUAY NHANH')))
        deadline = time.monotonic() + 2
        while not any(row['event'] == 'macro-start' for row in rows()[count:]):
            assert time.monotonic() < deadline, 'Macro did not start before the fault'
            time.sleep(.01)
        assert any(',2000|' in bytes.fromhex(row['payload_hex']).decode('ascii') for row in rows()[count:])
        return current
    try:
        serve('receiver')
        current = open_neutral('initial')
        tap(next(node for node in current.iter('node') if node.get('text') == 'Cấu hình'))
        current = tree('settings')
        original_record = ET.fromstring(original).find("string[@name='record']")
        record = original_record.text if original_record is not None else ''
        old_port = dict(line.split('=', 1) for line in record.splitlines() if '=' in line).get('port', '8888')
        field = next(node for node in current.iter('node') if node.get('class') == 'android.widget.EditText' and node.get('text') == old_port)
        tap(field)
        adb('shell', 'input', 'keyevent', 'KEYCODE_MOVE_END')
        for _ in range(len(old_port)): adb('shell', 'input', 'keyevent', 'KEYCODE_DEL')
        adb('shell', 'input', 'text', '8889')
        adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
        current = tree('settings-edited')
        assert any(node.get('class') == 'android.widget.EditText' and node.get('text') == '8889' for node in current.iter('node'))
        tap(next(node for node in current.iter('node') if node.get('text') == 'Lưu toàn bộ'))
        current = tree('settings-saved')
        configured = adb('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/sumo_settings.xml')
        (OUT / 'configured-settings.xml').write_bytes(configured)
        configured_record = ET.fromstring(configured).find("string[@name='record']").text
        assert 'port=8889\n' in configured_record
        # A long, valid fixture makes cancellation distinguishable from natural completion.
        document = ET.fromstring(configured)
        stored = document.find("string[@name='record']")
        stored.text, replacements = re.subn(r'^duration=\d+$', 'duration=2000', stored.text, flags=re.MULTILINE)
        assert replacements == 1
        configured_record = stored.text
        fixture = ET.tostring(document, encoding='utf-8', xml_declaration=True)
        (OUT / 'macro-test-fixture.xml').write_bytes(fixture)
        adb('shell', 'am', 'force-stop', PACKAGE)
        seeded = subprocess.run([ADB, '-s', 'emulator-5554', 'shell', 'run-as', PACKAGE, 'sh', '-c', '"cat > shared_prefs/sumo_settings.xml"'],
                                input=fixture, capture_output=True, timeout=30)
        transcript.append(dict(command='set valid duration=2000 fixture through app UID; original record preserved', exit_code=seeded.returncode))
        assert seeded.returncode == 0
        current = start_macro('update-during-macro')
        tap(next(node for node in current.iter('node') if node.get('text') == 'Cập nhật'))
        stopped()
        current = tree('update-cancels-macro')
        assert any('Điều khiển đã dừng' in node.get('text', '') for node in current.iter('node'))
        assert not any(row['event'] == 'macro-complete' for row in rows()), 'Macro ended naturally before updater cancellation'
        assert any(row['reason'] == 'closed' for row in rows()), 'No close acknowledgement'
        tap(next(node for node in current.iter('node') if node.get('text') == 'Đóng'))
        serve('macro-drop-rx', 'drop-rx')
        start_macro('network-loss-during-macro')
        deadline = time.monotonic() + 4
        while not any(row['event'] == 'watchdog' for row in rows()):
            assert time.monotonic() < deadline, 'No watchdog during macro packet loss'
            time.sleep(.02)
        stopped()
        assert not any(row['event'] == 'macro-complete' for row in rows())
        current = tree('macro-network-loss-offline')
        assert any('Offline' in node.get('text', '') for node in current.iter('node'))
        assert any('PWM 0 / 0' in node.get('text', '') for node in current.iter('node'))
        open_neutral('macro-network-loss-reopen')
        serve('receiver-lifecycle')
        hold = start_hold('force-stop')
        adb('shell', 'am', 'force-stop', PACKAGE); hold.wait(timeout=8); stopped()
        assert any(row['event'] == 'watchdog' for row in rows()), 'Force-stop must exercise receiver watchdog'
        open_neutral('after-force-stop')
        assert ET.fromstring(adb('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/sumo_settings.xml')).find("string[@name='record']").text == configured_record
        old_boot = adb('shell', 'cat', '/proc/sys/kernel/random/boot_id').strip()
        adb('reboot')
        adb('wait-for-device', timeout=180)
        deadline = time.monotonic() + 180
        while (adb('shell', 'getprop', 'sys.boot_completed', check=False).strip() != b'1' or
               adb('shell', 'cat', '/proc/sys/kernel/random/boot_id', check=False).strip() == old_boot):
            assert time.monotonic() < deadline, 'AVD reboot timeout'
            time.sleep(1)
        adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
        adb('shell', 'wm', 'dismiss-keyguard', check=False)
        open_neutral('after-real-reboot')
        after_reboot = adb('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/sumo_settings.xml')
        (OUT / 'after-reboot-settings.xml').write_bytes(after_reboot)
        assert ET.fromstring(after_reboot).find("string[@name='record']").text == configured_record
        hold = start_hold('resize')
        adb('shell', 'wm', 'size', '900x1600'); hold.wait(timeout=8); stopped()
        open_neutral('after-resize', require_offline=False)
        adb('shell', 'wm', 'size', 'reset')
        open_neutral('after-size-restored', require_offline=False)
        (OUT / 'summary.json').write_text(json.dumps(dict(update_cancels_active_2000ms_macro=True, drop_rx_cancels_macro_via_watchdog=True,
            force_stop_while_moving=True, receiver_watchdog=True,
            real_avd_reboot=True, nondefault_configuration_retained=True, resize_while_moving=True,
            offline_neutral_no_automatic_reconnect=True, source='same Phase5 debug APK; no install/uninstall/clear-data'), indent=2))
        print('E03/F06 force-stop, reboot and resize passed')
    finally:
        adb('shell', 'am', 'force-stop', PACKAGE, check=False)
        adb('shell', 'wm', 'size', 'reset', check=False)
        restore = subprocess.run([ADB, '-s', 'emulator-5554', 'shell', 'run-as', PACKAGE, 'sh', '-c', '"cat > shared_prefs/sumo_settings.xml"'],
                                 input=original, capture_output=True, timeout=30)
        transcript.append(dict(command='restore original settings XML via app UID', exit_code=restore.returncode))
        for child in reversed(children):
            if child.poll() is None: child.terminate(); child.wait(timeout=5)
        (OUT / 'commands.json').write_text(json.dumps(transcript, indent=2), encoding='utf-8')
        assert restore.returncode == 0


if __name__ == '__main__': main()
