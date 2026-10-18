"""Decompose every 50 ms interval; diagnostic groupings never change acceptance."""
import argparse
import csv
import json
import math
import re
from pathlib import Path


def stats(values):
    v=sorted(values)
    return dict(n=len(v),minimum=v[0],p50=v[math.ceil(.5*len(v))-1],p95=v[math.ceil(.95*len(v))-1],
                p99=v[math.ceil(.99*len(v))-1],maximum=v[-1])


def records(text):
    rows=[]
    for m in re.finditer(r'wire t0_ns=(-?\d+) t1_ns=(\d+) t2_ns=(\d+) cause=(.*?) hex=([0-9a-f]+)([^\r\n]*)',text):
        row=dict(t0_ns=int(m[1]),t1_ns=int(m[2]),t2_ns=int(m[3]),cause=m[4],raw=bytes.fromhex(m[5]).decode('ascii'))
        row.update({k:int(v) for k,v in re.findall(r'(\w+)=(-?\d+)',m[6])})
        assert 'work_ns' in row and ('prepare_end_ns' in row or 'encode_end_ns' in row)
        rows.append(row)
    return rows


def main():
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    args=p.parse_args()
    text=args.source.read_text(encoding='utf-8',errors='replace').split('BEGIN CADENCE50',1)[1].split('END CADENCE50',1)[0]
    data=records(text);rows=[]
    stage='prepare_end_ns' if 'prepare_end_ns' in data[0] else 'encode_end_ns'
    for a,b in zip(data,data[1:]):
        if a['cause']!=b['cause'] or b['cause']!='periodic' or a['raw'].split(',')[0]!=b['raw'].split(',')[0]:continue
        def wake(r):return (r['wake_ns']-r['deadline_ns'])/1e6
        def processing(r):return (r['t1_ns']-r['wake_ns'])/1e6
        row=dict(sample=len(rows)+1,interval_ms=(b['t1_ns']-a['t1_ns'])/1e6,
            deadline_step_ms=(b['deadline_ns']-a['deadline_ns'])/1e6,
            wake_lateness_ms=wake(b),processing_ms=processing(b),
            delta_wake_ms=wake(b)-wake(a),delta_processing_ms=processing(b)-processing(a),
            after_stage_ms=(b['t1_ns']-b[stage])/1e6,pre_send_cpu_ms=b['pre_send_cpu_ns']/1e6,
            previous_after_stage_ms=(a['t1_ns']-a[stage])/1e6,
            t1_ns=b['t1_ns'],seq=int(b['raw'].split(',')[1]),deadline_ns=b['deadline_ns'],wake_ns=b['wake_ns'])
        start='prepare_start_ns' if stage=='prepare_end_ns' else 'encode_start_ns'
        row.update(stage_ms=(b[stage]-b[start])/1e6,before_stage_ms=(b[start]-b['wake_ns'])/1e6,
            send_ms=(b['t2_ns']-b['t1_ns'])/1e6,
            wait_requested_ms=b['requested_wait_ns']/1e6,wait_actual_ms=(b['wake_ns']-b['wait_start_ns'])/1e6)
        assert abs(row['processing_ms']-row['stage_ms']-row['before_stage_ms']-row['after_stage_ms'])<1e-8
        assert abs(row['interval_ms']-(row['deadline_step_ms']+row['delta_wake_ms']+row['delta_processing_ms']))<1e-8
        rows.append(row)
    assert rows
    with args.output.with_suffix('.csv').open('w',encoding='utf-8',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
    over=[r for r in rows if r['interval_ms']>50]
    result=dict(source=str(args.source),stage_schema=stage,samples=len(rows),below20=sum(r['interval_ms']<20 for r in rows),above50=len(over),
        stats={key:stats([r[key] for r in rows]) for key in rows[0] if key.endswith('_ms')},
        exact50_deadline_steps=sum(r['deadline_step_ms']==50 for r in rows),
        positive_wake_change_larger_than_positive_processing_change=sum(max(0,r['delta_wake_ms'])>max(0,r['delta_processing_ms']) for r in over),
        positive_processing_change_at_least_as_large=sum(max(0,r['delta_wake_ms'])<=max(0,r['delta_processing_ms']) for r in over),
        all_over55_diagnostic_only=[r for r in rows if r['interval_ms']>55],
        worst_interval=max(rows,key=lambda r:r['interval_ms']),latest_wake=max(rows,key=lambda r:r['wake_lateness_ms']),
        note='Every interval retained. processing is wake..send; stage is encode-only in32 or encode+recordSent in33. Additive wall decomposition is not stack-level attribution; CPU clock approximate in VM. >55 lists diagnostics only; acceptance remains every interval20..50.')
    args.output.with_suffix('.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(result,indent=2))


if __name__=='__main__':main()
