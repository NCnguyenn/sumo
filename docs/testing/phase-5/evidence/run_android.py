"""Recorded AVD instrumentation; never uninstall or clear application data."""
import argparse
import hashlib
import json
from pathlib import Path
import queue
import re
import subprocess
import sys
import tarfile
import threading
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[4]
ADB = str(Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe')
PACKAGE = 'com.aistudy.myapplication'


def junit(raw, path):
    cases = []
    status = {}
    active_key = None
    for line in raw.splitlines():
        if line.startswith('INSTRUMENTATION_STATUS: '):
            key, _, value = line[len('INSTRUMENTATION_STATUS: '):].partition('=')
            status[key] = value
            active_key = key
        elif line.startswith('INSTRUMENTATION_STATUS_CODE: '):
            code = int(line.rsplit(' ', 1)[1])
            if code != 1 and 'class' in status and 'test' in status:
                cases.append((status.copy(), code))
            status = {}
            active_key = None
        elif active_key and not line.startswith('INSTRUMENTATION_'):
            status[active_key] += '\n' + line
    failed = sum(code not in (0, -3, -4) for _, code in cases)
    skipped = sum(code in (-3, -4) for _, code in cases)
    suite = ET.Element('testsuite', name='Phase5-AVD', tests=str(len(cases)), failures=str(failed), errors='0', skipped=str(skipped))
    for case, code in cases:
        node = ET.SubElement(suite, 'testcase', classname=case['class'], name=case['test'])
        if code in (-3, -4):
            ET.SubElement(node, 'skipped').text = case.get('stack', case.get('stream', ''))
        elif code != 0:
            ET.SubElement(node, 'failure', message=f'Instrumentation status {code}').text = case.get('stack', case.get('stream', ''))
    ET.ElementTree(suite).write(path, encoding='utf-8', xml_declaration=True)
    return dict(tests=len(cases), failures=failed, skipped=skipped, successful=sum(code == 0 for _, code in cases))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--classes', required=True, help='comma-separated simple test class names')
    args = parser.parse_args()
    out = args.output.resolve()
    assert out.is_relative_to(ROOT / 'docs/testing/phase-5/evidence') and not out.exists(), 'Use a new evidence directory'
    out.mkdir()
    transcript = out / 'commands.jsonl'
    def command(parts, timeout=90):
        result = subprocess.run(parts, cwd=ROOT, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=timeout)
        with transcript.open('a', encoding='utf-8') as file:
            file.write(json.dumps(dict(command=parts, exit_code=result.returncode, output=result.stdout + result.stderr)) + '\n')
        assert result.returncode == 0, result.stdout + result.stderr
        return result.stdout
    def adb(*parts):
        return command([ADB, '-s', 'emulator-5554', *parts])
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1', 'Only the selected AVD is authorized'
    sources = {}
    for path in sorted([*ROOT.glob('app/src/**/*.kt'), *ROOT.glob('tools/*.py'), ROOT / 'app/build.gradle.kts']):
        sources[str(path.relative_to(ROOT))] = hashlib.sha256(path.read_bytes()).hexdigest()
    (out / 'source.json').write_text(json.dumps(sources, indent=2), encoding='utf-8')
    for apk in ('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'):
        adb('install', '-r', str(ROOT / apk))
    if 'InstallerPlatformTest' in args.classes:
        permission = adb('shell', 'appops', 'get', PACKAGE, 'REQUEST_INSTALL_PACKAGES')
        assert re.search(r'REQUEST_INSTALL_PACKAGES:\s*allow\b', permission), 'Grant Install unknown apps for this app in Android Settings before running the positive installer fixture'
    # Existing v2 debug fixture only opens/cancels the installer; it is never distributed or installed.
    fixture = ROOT / '.gradle/phase3-fixture-b.apk'
    assert fixture.is_file()
    pushed = '/data/local/tmp/sumo-phase5-b.apk'
    adb('push', str(fixture), pushed)
    adb('shell', 'run-as', PACKAGE, 'mkdir', '-p', 'files')
    # Windows file stdin stops at Ctrl-Z; adb push + in-guest copy preserves binary bytes.
    adb('shell', 'run-as', PACKAGE, 'cp', pushed, 'files/test-b.apk')
    assert adb('shell', 'run-as', PACKAGE, 'sha256sum', 'files/test-b.apk').split()[0] == hashlib.sha256(fixture.read_bytes()).hexdigest()
    relative_output = 'files/phase5-' + out.name
    external = '/data/user/0/' + PACKAGE + '/' + relative_output
    adb('shell', 'run-as', PACKAGE, 'mkdir', '-p', relative_output)
    children = []
    handles = []
    try:
        for port in (8888, 8889):
            parts = [sys.executable, '-u', str(ROOT / 'tools/sumo_simulator.py'), '--host', '127.0.0.1', '--port', str(port),
                     '--scenario', 'normal', '--log', str(out / f'simulator-{port}.jsonl')]
            server = subprocess.Popen(parts, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding='utf-8', creationflags=subprocess.CREATE_NO_WINDOW)
            children.append(server)
            ready = queue.Queue()
            threading.Thread(target=lambda process=server: ready.put(process.stdout.readline()), daemon=True).start()
            line = ready.get(timeout=5)
            with transcript.open('a', encoding='utf-8') as file:
                file.write(json.dumps(dict(start=parts, pid=server.pid, ready=line.strip())) + '\n')
            assert line.strip() == f'READY {port}', line
        for suffix in ('stdout', 'stderr'):
            handles.append((out / ('logcat-StopLatencyTest-CadenceTest.txt' if suffix == 'stdout' else 'logcat.stderr.txt')).open('w', encoding='utf-8'))
        logcat = subprocess.Popen([ADB, '-s', 'emulator-5554', 'logcat', '-T', '1', '-v', 'monotonic', '-b', 'main', '-b', 'system', '-b', 'crash'],
                                  stdout=handles[0], stderr=handles[1], creationflags=subprocess.CREATE_NO_WINDOW)
        children.append(logcat)
        classes = ','.join(PACKAGE + '.' + name for name in args.classes.split(','))
        parts = [ADB, '-s', 'emulator-5554', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', classes,
                 '-e', 'candidate', '/data/user/0/' + PACKAGE + '/files/test-b.apk', '-e', 'trackingProfileLabel', 'after',
                 '-e', 'additionalTestOutputDir', external, PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner']
        with transcript.open('a', encoding='utf-8') as file:
            file.write(json.dumps(dict(start=parts, time_utc=time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()))) + '\n')
        start = time.monotonic()
        result = subprocess.run(parts, cwd=ROOT, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=3000)
        raw = result.stdout + result.stderr
        (out / 'instrumentation.txt').write_text('COMMAND: ' + json.dumps(parts) + '\n' + raw + f'\nEXIT_CODE: {result.returncode}\n', encoding='utf-8')
        summary = junit(raw, out / 'instrumentation.xml')
        summary.update(exit_code=result.returncode, elapsed_seconds=time.monotonic() - start)
        (out / 'summary.json').write_text(json.dumps(summary, indent=2), encoding='utf-8')
        pulled = subprocess.run([ADB, '-s', 'emulator-5554', 'exec-out', 'run-as', PACKAGE, 'tar', '-cf', '-', relative_output],
                                capture_output=True, timeout=90)
        (out / 'test-output.tar').write_bytes(pulled.stdout)
        with transcript.open('a', encoding='utf-8') as file:
            file.write(json.dumps(dict(command='adb exec-out run-as package tar -cf - ' + relative_output, exit_code=pulled.returncode,
                                       stderr=pulled.stderr.decode('utf-8', errors='replace'))) + '\n')
        assert pulled.returncode == 0, pulled.stderr
        with tarfile.open(out / 'test-output.tar') as archive:
            archive.extractall(out / 'test-output', filter='data')
        adb('shell', 'dumpsys', 'activity', 'exit-info', PACKAGE)
        print(json.dumps(summary), flush=True)
        return 0 if result.returncode == 0 and summary['tests'] and not summary['failures'] and not summary['skipped'] and re.search(r'OK \(\d+ tests?\)', raw) else 1
    finally:
        for child in reversed(children):
            if child.poll() is None:
                child.terminate()
                try: child.wait(timeout=5)
                except subprocess.TimeoutExpired: child.kill(); child.wait(timeout=5)
        for file in handles: file.close()


if __name__ == '__main__':
    raise SystemExit(main())
