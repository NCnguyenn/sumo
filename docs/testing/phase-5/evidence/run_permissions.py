import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[4]
OUT = ROOT / 'docs/testing/phase-5/evidence/11-permissions-label-fix'
assert not OUT.exists()
OUT.mkdir()
command = [sys.executable, '-u', str(ROOT / 'tools/sumo_simulator.py'), '--host', '127.0.0.1', '--port', '8888', '--log', str(OUT / 'receiver.jsonl')]
server = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, creationflags=subprocess.CREATE_NO_WINDOW)
try:
    ready = server.stdout.readline()
    assert ready.strip() == 'READY 8888', ready
    print(json.dumps(dict(start=command, pid=server.pid, ready=ready.strip())), flush=True)
    test = [sys.executable, '-X', 'utf8', str(ROOT / 'tools/check_android_permissions.py'), '--serial', 'emulator-5554',
            '--receiver-log', str(OUT / 'receiver.jsonl'), '--evidence-dir', str(OUT)]
    print('COMMAND: ' + json.dumps(test), flush=True)
    result = subprocess.run(test)
    print('EXIT_CODE:', result.returncode, flush=True)
finally:
    server.terminate(); server.wait(timeout=5)
    print('CLEANUP own receiver', server.pid, flush=True)
raise SystemExit(result.returncode)
