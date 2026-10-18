"""A07/A09 permission UI + real installer Cancel; no new APK is installed."""
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET
from run_android import junit

ROOT = Path(__file__).resolve().parents[4]
ADB = str(Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe')
PACKAGE = 'com.aistudy.myapplication'
OUT = ROOT / 'docs/testing/phase-5/evidence/14-installer-complete'


def main():
    assert not OUT.exists(), 'Preserve every attempt'
    OUT.mkdir(); commands = []
    def adb(*args, check=True, timeout=40):
        parts = [ADB, '-s', 'emulator-5554', *args]
        result = subprocess.run(parts, capture_output=True, timeout=timeout)
        commands.append(dict(command=parts, exit_code=result.returncode, output=(result.stdout + result.stderr).decode('utf-8', errors='replace')))
        if check: assert result.returncode == 0, commands[-1]
        return result.stdout
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == b'1'
    import hashlib
    expected = hashlib.sha256((ROOT / '.gradle/phase3-fixture-b.apk').read_bytes()).hexdigest()
    assert adb('shell', 'run-as', PACKAGE, 'sha256sum', 'files/test-b.apk').decode().split()[0] == expected
    before = adb('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/sumo_settings.xml')
    (OUT / 'before-settings.xml').write_bytes(before)
    def screen(name):
        adb('shell', 'uiautomator', 'dump', '/sdcard/sumo-phase5-installer.xml')
        raw = adb('shell', 'cat', '/sdcard/sumo-phase5-installer.xml')
        (OUT / (name + '.xml')).write_bytes(raw)
        return ET.fromstring(raw)
    def switch(tree):
        nodes = [node for node in tree.iter('node') if node.get('checkable') == 'true' and node.get('clickable') == 'true']
        assert len(nodes) == 1, 'Expected one Android permission toggle'
        return nodes[0]
    def tap(node):
        x0, y0, x1, y1 = map(int, re.findall(r'\d+', node.get('bounds', '')))
        adb('shell', 'input', 'tap', str((x0 + x1) // 2), str((y0 + y1) // 2))
    def settings():
        adb('shell', 'am', 'start', '-W', '-a', 'android.settings.MANAGE_UNKNOWN_APP_SOURCES', '-d', 'package:' + PACKAGE)
    log = None; handle = None; originally_allowed = None
    try:
        adb('shell', 'appops', 'get', PACKAGE, 'REQUEST_INSTALL_PACKAGES')
        settings(); current = screen('unknown-source-before')
        originally_allowed = switch(current).get('checked') == 'true'
        if not originally_allowed: tap(switch(current))
        current = screen('unknown-source-granted')
        assert switch(current).get('checked') == 'true'
        (OUT / 'unknown-source-granted.png').write_bytes(adb('exec-out', 'screencap', '-p'))
        adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
        handle = (OUT / 'logcat.txt').open('wb')
        log = subprocess.Popen([ADB, '-s', 'emulator-5554', 'logcat', '-T', '1', '-v', 'monotonic'],
                               stdout=handle, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        raw = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', PACKAGE + '.InstallerPlatformTest',
                  '-e', 'candidate', '/data/user/0/' + PACKAGE + '/files/test-b.apk',
                  PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner', timeout=120).decode('utf-8', errors='replace')
        (OUT / 'instrumentation.txt').write_text(raw, encoding='utf-8')
        result = junit(raw, OUT / 'instrumentation.xml')
        assert result == dict(tests=1, failures=0, skipped=0, successful=1) and 'OK (1 test)' in raw
        (OUT / 'native-installer-prompt.png').write_bytes(adb('exec-out', 'cat', '/sdcard/sumo-native-installer-prompt.png'))
        after = adb('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/sumo_settings.xml')
        (OUT / 'after-settings.xml').write_bytes(after)
        assert before == after, 'Installer prompt/Cancel modified settings'
        (OUT / 'summary.json').write_text(json.dumps(dict(initially_allowed=originally_allowed, granted_through_android_settings_ui=True,
            native_installer_prompt_and_cancel=True, version_code_after=1, settings_byte_identical=True,
            limitation='Local signed v2 fixture; no HTTPS download or successful A-to-B update'), indent=2))
        print('A07/A09 Android Settings grant + real PackageInstaller prompt/Cancel passed; settings unchanged')
    finally:
        if log is not None: log.terminate(); log.wait(timeout=5)
        if handle is not None: handle.close()
        if originally_allowed is False:
            settings(); current = screen('unknown-source-cleanup')
            if switch(current).get('checked') == 'true': tap(switch(current))
            assert switch(screen('unknown-source-restored')).get('checked') == 'false'
            adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
        (OUT / 'commands.json').write_text(json.dumps(commands, indent=2), encoding='utf-8')


if __name__ == '__main__': main()
