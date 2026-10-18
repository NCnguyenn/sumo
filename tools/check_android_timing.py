"""Verify G0 timing boundaries against the actual STOP500 instrumentation log."""
from pathlib import Path
import argparse
import csv
import json
import math
import re


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--results',type=Path,required=True)
    parser.add_argument('--output-prefix',type=Path,required=True)
    args=parser.parse_args()
    source=next(args.results.rglob('*StopLatency*.txt'))
    full=source.read_text(encoding='utf-8',errors='replace')
    assert 'BEGIN STOP500' in full and 'END STOP500' in full,'Incomplete measurement cohort'
    text=full.split('BEGIN STOP500',1)[1].split('END STOP500',1)[0]
    records=[]
    for m in re.finditer(r'wire t0_ns=(-?\d+) t1_ns=(\d+) t2_ns=(\d+) cause=(.*?) hex=([0-9a-f]+)',text):
        t0,t1,t2=map(int,m.group(1,2,3))
        records.append(dict(t0_ns=t0,t1_ns=t1,t2_ns=t2,cause=m[4],raw=bytes.fromhex(m[5]).decode('ascii')))
    stops=[r for r in records if r['cause'].startswith('Release(') and r['raw'].endswith('|S:0,0,0,0,0\n')]
    assert len(stops)==500,len(stops)
    assert all(r['t2_ns']>=r['t1_ns']>=r['t0_ns']>=0 for r in stops)
    with Path(str(args.output_prefix)+'.csv').open('w',encoding='utf-8',newline='') as file:
        writer=csv.writer(file);writer.writerow(['t0_ns','t1_ns','t2_ns','before_send_ms','return_send_ms','raw'])
        for r in stops:writer.writerow([r['t0_ns'],r['t1_ns'],r['t2_ns'],(r['t1_ns']-r['t0_ns'])/1e6,(r['t2_ns']-r['t0_ns'])/1e6,r['raw'].rstrip()])
    def stats(values):
        v=sorted(values)
        return dict(n=len(v),p50=v[math.ceil(.5*len(v))-1],p95=v[math.ceil(.95*len(v))-1],p99=v[math.ceil(.99*len(v))-1],maximum=max(v))
    before=[(r['t1_ns']-r['t0_ns'])/1e6 for r in stops];after=[(r['t2_ns']-r['t0_ns'])/1e6 for r in stops]
    periods=[(b['t1_ns']-a['t1_ns'])/1e6 for a,b in zip(records,records[1:]) if a['cause']==b['cause']=='periodic' and
             a['raw'].startswith('V1:') and b['raw'].startswith('V1:') and a['raw'].split(',')[0]==b['raw'].split(',')[0]]
    summary=dict(source=str(source),samples=500,t1_minus_t0_ms=stats(before),t2_minus_t0_ms=stats(after),before_over_10=sum(v>10 for v in before),
                 return_over_10=sum(v>10 for v in after),periodic30_adjacent_without_intervening_event=stats(periods),periodic_outside_20_50=sum(v<20 or v>50 for v in periods))
    summary['periodic_sample_note']='Incidental STOP500 intervals; separate CadenceTest/check_android_cadence.py requires >=1000 intervals at each of 20/30/50 ms'
    Path(str(args.output_prefix)+'.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
    print(json.dumps(summary,indent=2))
    assert not summary['before_over_10'] and not summary['return_over_10'],'G0 STOP <=10 ms failed; retain every slow sample'
    assert not summary['periodic_outside_20_50'],'G0 periodic intervals 20..50 ms failed; full cadence cohort checked separately'


if __name__=='__main__':main()
