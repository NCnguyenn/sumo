"""Supplement the unchanged acceptance scripts; retain every measured row."""
import argparse
import csv
import json
import math
import re
from pathlib import Path


def stats(values):
    v=sorted(values)
    return dict(n=len(v),minimum=v[0],p50=v[math.ceil(.5*len(v))-1],
                p95=v[math.ceil(.95*len(v))-1],p99=v[math.ceil(.99*len(v))-1],maximum=v[-1])


def records(text):
    found=[]
    pattern=r'wire t0_ns=(-?\d+) t1_ns=(\d+) t2_ns=(\d+) cause=(.*?) hex=([0-9a-f]+)([^\r\n]*)'
    for m in re.finditer(pattern,text):
        row=dict(t0_ns=int(m[1]),t1_ns=int(m[2]),t2_ns=int(m[3]),cause=m[4],raw=bytes.fromhex(m[5]).decode('ascii'))
        row.update({k:int(v) for k,v in re.findall(r'(\w+)=(-?\d+)',m[6])})
        assert 'encode_start_ns' in row,'Missing stage diagnostics'
        found.append(row)
    return found


def stages(r):
    start=r['dequeued_ns'] if r['t0_ns']>=0 else r['work_ns']
    assert start<=r['encode_start_ns']<=r['encode_end_ns']<=r['t1_ns']<=r['t2_ns']
    result=dict(event_ms=(r['encode_start_ns']-start)/1e6,
        encode_ms=(r['encode_end_ns']-r['encode_start_ns'])/1e6,
        tracking_ms=(r['t1_ns']-r['encode_end_ns'])/1e6,send_ms=(r['t2_ns']-r['t1_ns'])/1e6,
        pre_send_cpu_ms=r['pre_send_cpu_ns']/1e6,send_cpu_ms=r['send_cpu_ns']/1e6,
        pre_send_wall_minus_cpu_ms=(r['t1_ns']-start-r['pre_send_cpu_ns'])/1e6,
        send_wall_minus_cpu_ms=(r['t2_ns']-r['t1_ns']-r['send_cpu_ns'])/1e6,
        processing_ms=(r['t1_ns']-start)/1e6,
        wake_lateness_ms=(r['wake_ns']-r['deadline_ns'])/1e6,
        wait_actual_ms=(r['wake_ns']-r['wait_start_ns'])/1e6,
        wait_requested_ms=r['requested_wait_ns']/1e6,
        wake_overshoot_ms=(r['wake_ns']-r['wait_start_ns']-r['requested_wait_ns'])/1e6,
        previous_work_ms=r['prev_work_ns']/1e6,previous_trace_submit_ms=r['prev_trace_submit_ns']/1e6)
    if r['t0_ns']>=0:
        assert r['t0_ns']<=r['enqueued_ns']<=r['dequeued_ns']
        result.update(post_ms=(r['enqueued_ns']-r['t0_ns'])/1e6,
                      worker_queue_ms=(r['dequeued_ns']-r['enqueued_ns'])/1e6,
                      total_ms=(r['t2_ns']-r['t0_ns'])/1e6)
        assert abs(sum(result[k] for k in ('post_ms','worker_queue_ms','event_ms','encode_ms','tracking_ms','send_ms'))-result['total_ms'])<1e-8
    return result


def write_csv(path,rows):
    with path.open('w',encoding='utf-8',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)


def summarize(rows):
    return {key:stats([r[key] for r in rows]) for key in rows[0] if key.endswith('_ms')}


def main():
    p=argparse.ArgumentParser();p.add_argument('--results',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    args=p.parse_args()
    # Known durations must remain visible, including off-CPU/blocked wall time.
    fixture=dict(t0_ns=0,enqueued_ns=1_000_000,dequeued_ns=4_000_000,work_ns=4_000_000,
        encode_start_ns=6_000_000,encode_end_ns=7_000_000,t1_ns=8_000_000,t2_ns=13_000_000,
        pre_send_cpu_ns=2_000_000,send_cpu_ns=1_000_000,wait_start_ns=1_000_000,wake_ns=4_000_000,
        requested_wait_ns=2_000_000,prev_work_ns=0,prev_trace_submit_ns=0,trace_pending=0,deadline_ns=0)
    f=stages(fixture)
    assert [f[k] for k in ('post_ms','worker_queue_ms','event_ms','encode_ms','tracking_ms','send_ms','total_ms')]==[1,3,2,1,1,5,13]
    stop_source=next(args.results.rglob('*StopLatencyTest*.txt'));cadence_source=next(args.results.rglob('*CadenceTest*.txt'))
    stop_text=stop_source.read_text(encoding='utf-8',errors='replace');cadence_text=cadence_source.read_text(encoding='utf-8',errors='replace')
    stop_cohort=stop_text.split('BEGIN STOP500',1)[1].split('END STOP500',1)[0]
    stops=[r for r in records(stop_cohort) if r['cause'].startswith('Release(') and r['raw'].endswith('|S:0,0,0,0,0\n')]
    assert len(stops)==500,len(stops)
    stop_rows=[dict(sample=i+1,**r,**stages(r)) for i,r in enumerate(stops)]
    write_csv(args.output/'08-stages-stop.csv',stop_rows)
    summary=dict(stop_source=str(stop_source),cadence_source=str(cadence_source),stop=summarize(stop_rows),
        stop_over_10=[r for r in stop_rows if r['total_ms']>10],worst_stop=max(stop_rows,key=lambda r:r['total_ms']),cadence=[])
    cadence_rows=[]
    for interval in (20,30,50):
        text=cadence_text.split(f'BEGIN CADENCE{interval}',1)[1].split(f'END CADENCE{interval}',1)[0]
        all_rows=records(text);group=[]
        for a,b in zip(all_rows,all_rows[1:]):
            if a['cause']!=b['cause'] or b['cause']!='periodic' or a['raw'].split(',')[0]!=b['raw'].split(',')[0]:
                continue
            group.append(dict(configured_ms=interval,sample=len(group)+1,interval_ms=(b['t1_ns']-a['t1_ns'])/1e6,
                deadline_step_ms=(b['deadline_ns']-a['deadline_ns'])/1e6,
                previous_lateness_ms=(a['t1_ns']-a['deadline_ns'])/1e6,
                lateness_ms=(b['t1_ns']-b['deadline_ns'])/1e6,**b,**stages(b)))
        assert len(group)>=1000,len(group)
        for r in group:
            assert abs(r['interval_ms']-(r['deadline_step_ms']+r['lateness_ms']-r['previous_lateness_ms']))<1e-8
        cadence_rows.extend(group)
        summary['cadence'].append(dict(configured_ms=interval,stats=summarize(group),
            outside=sum(r['interval_ms']<20 or r['interval_ms']>50 for r in group),
            worst_interval=max(group,key=lambda r:r['interval_ms']),max_trace_pending=max(r['trace_pending'] for r in group),
            latest_worker_wake=max(group,key=lambda r:r['wake_lateness_ms'])))
    write_csv(args.output/'08-stages-cadence.csv',cadence_rows)
    heaps=[]
    for text in (stop_text,cadence_text):
        for m in re.finditer(r'PERF_HEAP (BEGIN|END) (STOP500|CADENCE\d+) ns=(\d+) stats=\{([^}]*)\}',text):
            heaps.append(dict(marker=m[1],cohort=m[2],ns=int(m[3]),stats=dict(re.findall(r'([^=, ]+)=([^,}]+)',m[4]))))
    assert len(heaps)==8,len(heaps)
    summary['heap_snapshots']=heaps
    summary['heap_deltas']=[]
    for cohort in ('STOP500','CADENCE20','CADENCE30','CADENCE50'):
        begin=next(r['stats'] for r in heaps if r['cohort']==cohort and r['marker']=='BEGIN')
        end=next(r['stats'] for r in heaps if r['cohort']==cohort and r['marker']=='END')
        summary['heap_deltas'].append(dict(cohort=cohort,deltas={key:int(end[key])-int(begin[key])
            for key in begin.keys() & end.keys() if begin[key].isdigit() and end[key].isdigit()}))
    summary['notes']=['All 500 releases and every interval retained. Acceptance scripts unchanged.',
        'CPU clocks have finite resolution; signed wall-minus-CPU values are retained.',
        'Queue wait includes worker busy time and scheduling; wake overshoot is meaningful for an empty timed poll.',
        'Heap counters cover the whole app/test, not just the control worker; they do not attribute individual stalls to GC.']
    (args.output/'08-stages.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False),encoding='utf-8')
    print(json.dumps(dict(stop_samples=len(stops),cadence_samples=len(cadence_rows),heap_snapshots=len(heaps)),indent=2))


if __name__=='__main__':main()
