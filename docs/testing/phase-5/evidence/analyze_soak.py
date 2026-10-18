"""Audit every recorded soak frame, resource checkpoint and receiver application."""
import csv
import json
from pathlib import Path
import re

HERE = Path(__file__).resolve().parent
WIRE = re.compile(r'wire .*?cause=(.*?) hex=([0-9a-f]+)')


def audit(lines):
    allowed = False
    directions = set()
    frames = {}
    latest = {}
    neutral_ticks = 0
    for line in lines:
        pressed = re.search(r'event Press\(id=(\d+), button=FORWARD\)', line)
        if pressed:
            directions.add(int(pressed[1]))
            allowed = True
        released = re.search(r'event (?:Release|Leave)\(id=(\d+)\)', line)
        if released and int(released[1]) in directions:
            directions.remove(int(released[1]))
            allowed = False
        if any(marker in line for marker in ('event Kill', 'event Cancel', 'send hello', 'closed confirmed=')):
            allowed = False
            directions.clear()
        match = WIRE.search(line)
        if not match: continue
        raw = bytes.fromhex(match[2]).decode('ascii')
        if not raw.startswith('V1:'): continue
        head, body = raw.strip().split('|', 1)
        sid, seq, kill, mode, turn, duration = head[3:].split(',')
        seq = int(seq)
        key = sid, seq
        if key in frames:
            assert seq == 1 and frames[key]['raw'] == raw, 'Repeated non-handshake sequence'
            continue
        assert seq > latest.get(sid, 0), 'Sequence order reversed'
        latest[sid] = seq
        moving = False
        if body.startswith(('S:', 'D:')):
            left, right, boost, weapon, brake = map(int, body[2:].split(','))
            moving = left != 0 or right != 0
            assert not moving or (allowed and kill == '0'), f'Uncommanded motion {raw}'
            assert not boost or moving, 'Boost without effective direction'
            if not allowed and match[1] == 'periodic': neutral_ticks += 1
        frames[key] = dict(raw=raw, allowed=allowed, moving=moving)
    return frames, neutral_ticks


def self_check():
    def line(seq, left, kill=0):
        raw = f'V1:0123456789abcdef,{seq},{kill},0,0,0|S:{left},{left},0,0,0\n'
        return 'wire cause=periodic hex=' + raw.encode().hex()
    valid = ['event Press(id=0, button=FORWARD)', line(1, 153), 'event Release(id=0)', line(2, 0), line(3, 0)]
    assert audit(valid)[1] == 2
    for bad in (valid + [line(4, 153)], ['event Press(id=0, button=FORWARD)', line(1, 153, 1)],
                ['event Press(id=0, button=FORWARD)', line(2, 153), line(1, 153)]):
        try: audit(bad)
        except AssertionError: pass
        else: raise AssertionError('Auditor hid replay/motion after STOP/Kill')


def main():
    self_check()
    results = HERE / '08-final-android'
    records = [json.loads(line) for line in next((results / 'test-output').rglob('soak.jsonl')).read_text().splitlines()]
    begin = next(row for row in records if row['event'] == 'begin')
    finish = records[-1]
    checkpoints = [row for row in records if row['event'] == 'checkpoint']
    assert finish['event'] == 'finish' and finish['complete'] is True
    assert finish['elapsed_ms'] - begin['elapsed_ms'] >= 1800000
    assert finish['cycles'] >= 500 and finish['reconnects'] >= 20
    assert len(checkpoints) == 20 and all(row['worker'] == row['trace'] == 1 and row['sockets'] <= begin['sockets'] for row in checkpoints)
    assert finish['worker'] == finish['trace'] == 0 and finish['sockets'] < begin['sockets']
    raw = (results / 'logcat-StopLatencyTest-CadenceTest.txt').read_text(encoding='utf-8', errors='replace')
    start = raw.index('SumoPhase5: {"event":"begin"')
    end = raw.index('SumoPhase5: {"event":"finish"', start)
    frames, neutral_ticks = audit(raw[start:end].splitlines())
    accepted = 0
    last_applied = {}
    for row in map(json.loads, (results / 'simulator-8888.jsonl').read_text().splitlines()):
        key = row['sid'], row['seq']
        if row['event'] != 'accept' or key not in frames: continue
        assert row['seq'] > last_applied.get(row['sid'], 0), 'Receiver applied an old sequence'
        last_applied[row['sid']] = row['seq']
        state = row['applied']
        assert not (state['L'] or state['R']) or frames[key]['allowed'], 'Receiver motion without fresh direction'
        assert not state['X'] or not any(state[k] for k in ('L', 'R', 'B', 'W', 'turnActive'))
        accepted += 1
    assert accepted > 1000 and len(last_applied) >= 21
    haptic = raw.split('BEGIN HAPTIC_WEAPON_ONCE', 1)[1].split('END HAPTIC_WEAPON_ONCE', 1)[0]
    assert haptic.count('haptic weapon') == 1
    assert 'ANR in com.aistudy.myapplication' not in raw
    assert 'Process: com.aistudy.myapplication,' not in raw or 'FATAL EXCEPTION' not in raw
    with (HERE / '09-soak-resources.csv').open('w', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=['event', 'elapsed_ms', 'cycles', 'reconnects', 'kills', 'worker', 'trace', 'sockets', 'threads', 'heap_bytes'], extrasaction='ignore')
        writer.writeheader(); writer.writerows(records)
    summary = dict(duration_ms=finish['elapsed_ms'] - begin['elapsed_ms'], cycles=finish['cycles'], reconnects=finish['reconnects'], kills=finish['kills'],
        resource_checkpoints=len(checkpoints), worker_max=max(r['worker'] for r in records), trace_max=max(r['trace'] for r in records),
        sockets_begin=begin['sockets'], sockets_max=max(r['sockets'] for r in records), sockets_after_close=finish['sockets'],
        issued_frames=len(frames), applied_frames_checked=accepted, sessions=len(last_applied), neutral_periodic_frames_after_stop=neutral_ticks,
        unwanted_motion=0, weapon_haptic_calls_for_one_hold=1, app_crash_anr_in_captured_log=False,
        limitation='All socket FDs of the app process (including Unix sockets); 20 stabilized resource checkpoints. No hardware/phone claim.')
    (HERE / '09-soak-summary.json').write_text(json.dumps(summary, indent=2), encoding='utf-8')
    print(json.dumps(summary, indent=2))


if __name__ == '__main__': main()
