"""Report every interval; G0 stays strict unless the approved G5 AVD exception is selected."""
import argparse
import csv
import json
import math
from pathlib import Path
import re


def intervals(text):
    rows=[]
    for match in re.finditer(r'wire t0_ns=-?\d+ t1_ns=(\d+) t2_ns=\d+ cause=(.*?) hex=([0-9a-f]+)',text):
        raw=bytes.fromhex(match[3]).decode('ascii')
        rows.append((int(match[1]),match[2],raw.split(',')[0]))
    return [(b[0]-a[0])/1e6 for a,b in zip(rows,rows[1:]) if a[1]==b[1]=='periodic' and a[2]==b[2]]


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--results',type=Path,required=True)
    parser.add_argument('--output-prefix',type=Path,required=True)
    parser.add_argument('--targets',type=int,nargs='+',choices=(20,30,50),default=(20,30,50))
    parser.add_argument('--g5-avd-50-jitter',action='store_true',
                        help='Approved G5 AVD scope only: allow up to 60 ms at configured 50 ms; other gates stay unchanged')
    args=parser.parse_args()
    policy='G5_AVD_50_PLUS_10' if args.g5_avd_50_jitter else 'G0_STRICT'
    # The parser preserves slow samples and excludes pairs interrupted by an event.
    def line(t,cause='periodic'):
        return f'wire t0_ns=-1 t1_ns={t} t2_ns={t+1} cause={cause} hex=56313a612c310a\n'
    assert intervals(line(0)+line(60000000)+line(61000000,'Release(id=0)')+line(62000000)+line(92000000))==[60.,30.]
    source=next(args.results.rglob('*CadenceTest*.txt'))
    full=source.read_text(encoding='utf-8',errors='replace')
    summary=[]
    with Path(str(args.output_prefix)+'.csv').open('w',encoding='utf-8',newline='') as file:
        writer=csv.writer(file);writer.writerow(['configured_ms','sample','interval_ms'])
        for target in args.targets:
            begin=f'BEGIN CADENCE{target}';end=f'END CADENCE{target}'
            assert begin in full and end in full,f'Incomplete cohort {target} ms'
            samples=intervals(full.split(begin,1)[1].split(end,1)[0])
            assert samples and all(v>0 for v in samples),'Missing/nonmonotonic timestamps'
            for i,value in enumerate(samples):writer.writerow([target,i+1,value])
            ordered=sorted(samples)
            upper=60 if args.g5_avd_50_jitter and target==50 else 50
            summary.append(dict(configured_ms=target,n=len(samples),minimum=min(samples),p50=ordered[math.ceil(.5*len(samples))-1],
                p95=ordered[math.ceil(.95*len(samples))-1],p99=ordered[math.ceil(.99*len(samples))-1],maximum=max(samples),
                below_20=sum(v<20 for v in samples),above_50=sum(v>50 for v in samples),
                upper_limit_ms=upper,above_limit=sum(v>upper for v in samples)))
    Path(str(args.output_prefix)+'.json').write_text(json.dumps(dict(source=str(source),policy=policy,measurements=summary),indent=2),encoding='utf-8')
    print(json.dumps(summary,indent=2))
    assert all(row['n']>=1000 and row['below_20']==row['above_limit']==0 for row in summary),f'{policy} cadence failed/incomplete; do not discard jitter'


if __name__=='__main__':main()
