"""I11: block WAN inside the AVD, keep host LAN/loopback; always restore own rules."""
import json
from pathlib import Path
import queue
import subprocess
import sys
import threading
import time
from run_android import junit

ROOT = Path(__file__).resolve().parents[4]
ADB = str(Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe')
PACKAGE = 'com.aistudy.myapplication'
OUT = ROOT / 'docs/testing/phase-5/evidence/12-no-internet'
CHAIN = 'SUMO_P5_TEST'


def main():
    assert not OUT.exists(), 'Preserve prior evidence'
    OUT.mkdir()
    transcript = []
    def adb(*args, check=True, timeout=45):
        parts = [ADB, '-s', 'emulator-5554', *args]
        result = subprocess.run(parts, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=timeout)
        transcript.append(dict(command=parts, exit_code=result.returncode, output=result.stdout + result.stderr))
        if check: assert result.returncode == 0, transcript[-1]
        return result
    assert adb('shell', 'getprop', 'ro.kernel.qemu').stdout.strip() == '1'
    original_root = 'uid=0(' in adb('shell', 'id').stdout
    made = []; attached = []; server = None; before = {}
    try:
        adb('root'); adb('wait-for-device')
        assert 'uid=0(' in adb('shell', 'id').stdout, 'No root-capable AVD: I11 fixture blocked'
        for tool in ('iptables', 'ip6tables'):
            before[tool] = adb('shell', tool, '-S').stdout
            assert adb('shell', tool, '-S', CHAIN, check=False).returncode != 0, 'Do not touch a preexisting chain'
            adb('shell', tool, '-N', CHAIN); made.append(tool)
            for subnet in (('127.0.0.0/8', '10.0.2.0/24') if tool == 'iptables' else ('::1/128',)):
                adb('shell', tool, '-A', CHAIN, '-d', subnet, '-j', 'RETURN')
            adb('shell', tool, '-A', CHAIN, '-j', 'REJECT')
            adb('shell', tool, '-I', 'OUTPUT', '1', '-j', CHAIN); attached.append(tool)
        adb('shell', 'ip', '-4', 'route', 'get', '10.0.2.2')
        wan = adb('shell', 'ping', '-c', '1', '-W', '1', '1.1.1.1', check=False)
        assert wan.returncode != 0, 'WAN unexpectedly reachable'
        for tool in made: adb('shell', tool, '-L', CHAIN, '-v', '-n', '-x')
        parts = [sys.executable, '-u', str(ROOT / 'tools/sumo_simulator.py'), '--host', '127.0.0.1', '--port', '8888', '--log', str(OUT / 'receiver.jsonl')]
        server = subprocess.Popen(parts, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, creationflags=subprocess.CREATE_NO_WINDOW)
        ready = queue.Queue(); threading.Thread(target=lambda: ready.put(server.stdout.readline()), daemon=True).start()
        assert ready.get(timeout=5).strip() == 'READY 8888'
        result = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', PACKAGE + '.AndroidFaultTest',
                     '-e', 'scenario', 'normal', PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner', timeout=120)
        raw = result.stdout + result.stderr
        (OUT / 'instrumentation.txt').write_text(raw, encoding='utf-8')
        outcome = junit(raw, OUT / 'instrumentation.xml')
        assert outcome == dict(tests=1, failures=0, skipped=0, successful=1) and 'OK (1 test)' in raw
        time.sleep(.4)
        rows = [json.loads(line) for line in (OUT / 'receiver.jsonl').read_text().splitlines()]
        assert any(row['applied']['L'] or row['applied']['R'] for row in rows), 'No LAN receiver motion'
        assert rows[-1]['applied']['L'] == rows[-1]['applied']['R'] == 0
        for tool in made: adb('shell', tool, '-L', CHAIN, '-v', '-n', '-x')
        (OUT / 'summary.json').write_text(json.dumps(dict(wan_blocked_by_avd_ipv4_and_ipv6_output_rules=True,
            retained_routes='127.0.0.0/8,10.0.2.0/24,::1/128', numeric_wan_probe_failed=True,
            real_android_ui_and_udp_ack_succeeded=True, receiver_motion_then_stop=True,
            limitation='Controlled guest firewall fixture, not an Internet outage on a physical phone'), indent=2))
        print('I11 real Android session passed with WAN blocked and host LAN retained')
    finally:
        if server is not None:
            server.terminate(); server.wait(timeout=5)
        for tool in reversed(attached): adb('shell', tool, '-D', 'OUTPUT', '-j', CHAIN, check=False)
        for tool in reversed(made):
            adb('shell', tool, '-F', CHAIN, check=False); adb('shell', tool, '-X', CHAIN, check=False)
        restored = {tool: adb('shell', tool, '-S', check=False).stdout for tool in before}
        (OUT / 'firewall-restored.json').write_text(json.dumps({tool: restored[tool] == rules for tool, rules in before.items()}, indent=2))
        if not original_root:
            adb('unroot', check=False); adb('wait-for-device', check=False)
        (OUT / 'commands.json').write_text(json.dumps(transcript, indent=2), encoding='utf-8')
        assert restored == before, 'Review guest firewall restoration'


if __name__ == '__main__': main()
