"""Read ART v3 dual-clock method traces and all diagnostic allocation rows."""
import argparse
import collections
import csv
import json
import math
import struct
from pathlib import Path


def stats(values):
    v=sorted(values)
    return dict(n=len(v),minimum=v[0],p50=v[math.ceil(.5*len(v))-1],p95=v[math.ceil(.95*len(v))-1],
                p99=v[math.ceil(.99*len(v))-1],maximum=v[-1],total=sum(v))


def trace(path):
    data=path.read_bytes();end=data.index(b'*end\n')+5
    section='';methods={};threads={};header={}
    for line in data[:end].decode('utf-8',errors='replace').splitlines():
        if line.startswith('*'):section=line
        elif section=='*methods':
            p=line.split('\t');methods[int(p[0],16)]=(p[1],p[2])
        elif section=='*threads':
            key,name=line.split('\t',1);threads[int(key)]=name
        elif '=' in line:
            key,value=line.split('=',1);header[key]=value
    magic,version,offset,epoch,size=struct.unpack_from('<4sHHQH',data,end)
    assert (magic,version,size)==(b'SLOW',3,14),(magic,version,size)
    assert header['clock']=='dual'
    target=next(tid for tid,name in threads.items() if name=='sumo-tracking-profile')
    stack=[];results=[];method_totals=collections.defaultdict(lambda:[0,0,0]);mismatch=0
    focus={'com.aistudy.myapplication.control.ControlSession':'send',
           'com.aistudy.myapplication.protocol.ReplyTracker':'recordSent'}
    for tid,encoded,cpu,wall in struct.iter_unpack('<HIII',memoryview(data)[end+offset:]):
        if tid!=target:continue
        mid=encoded & ~3;action=encoded & 3;name=methods[mid]
        if action==0:
            node=dict(mid=mid,cpu=cpu,wall=wall,children_cpu=0,children_wall=0)
            if focus.get(name[0])==name[1]:
                node['focus']=dict(method='.'.join(name),cpu_start_us=cpu,wall_start_us=wall,encodes=0,
                    under_session=any(methods[x['mid']]==('com.aistudy.myapplication.control.ControlSession','send') for x in stack),
                    pattern_compiles=0,map_calls=0,first_calls=0)
            for ancestor in stack:
                if 'focus' not in ancestor:continue
                f=ancestor['focus']
                if name==('com.aistudy.myapplication.protocol.CommandCodec','encodeControl'):f['encodes']+=1
                if name[0]=='java.util.regex.Pattern' and name[1]=='compile':f['pattern_compiles']+=1
                if name[0] in ('java.util.HashMap','java.util.LinkedHashMap'):f['map_calls']+=1
                if name[0].startswith('kotlin.collections.') and name[1]=='first':f['first_calls']+=1
            stack.append(node)
        else:
            if not stack or stack[-1]['mid']!=mid:
                mismatch+=1;continue
            node=stack.pop();dcpu=cpu-node['cpu'];dwall=wall-node['wall']
            assert dcpu>=0 and dwall>=0
            if stack:
                stack[-1]['children_cpu']+=dcpu;stack[-1]['children_wall']+=dwall
            if 'focus' in node:
                results.append(dict(**node['focus'],cpu_us=dcpu,wall_us=dwall,
                    exclusive_cpu_us=dcpu-node['children_cpu'],exclusive_wall_us=dwall-node['children_wall']))
            if any('focus' in x for x in stack):
                total=method_totals['.'.join(name)];total[0]+=1;total[1]+=dcpu;total[2]+=dwall
    return dict(header=header,target_thread=target,records=(len(data)-end-offset)//size,mismatched_exits=mismatch,
        incomplete_stack_depth=len(stack),complete_focus_calls=results,
        descendant_methods=[dict(method=k,calls=v[0],inclusive_cpu_us=v[1],inclusive_wall_us=v[2])
                            for k,v in sorted(method_totals.items(),key=lambda pair:pair[1][2],reverse=True)])


def main():
    p=argparse.ArgumentParser();p.add_argument('--prefix',type=Path,required=True);p.add_argument('--require-single-encode',action='store_true')
    args=p.parse_args();prefix=args.prefix
    with prefix.with_suffix('.csv').open(encoding='utf-8') as f:rows=list(csv.DictReader(f))
    assert len(rows)==1200,len(rows)
    groups={}
    for group in ('session-prepare','tracker-ack','tracker-window'):
        r=[v for v in rows if v['group']==group];assert len(r)==400
        groups[group]={key:stats([int(v[key]) for v in r]) for key in ('wall_ns','cpu_ns','allocated_bytes','allocated_objects')}
        groups[group]['cpu_zero_samples']=sum(int(v['cpu_ns'])==0 for v in r)
    result=dict(metadata=json.loads(prefix.with_suffix('.json').read_text(encoding='utf-8')),groups=groups,
                trace=trace(prefix.with_suffix('.trace')),
                note='Diagnostic only. All rows retained. Intrusive tracing and finite CPU-clock resolution prevent using these times as acceptance. Overflow/truncated stack is explicitly retained.')
    assert result['metadata']['allocation_supported'],'Allocation probe unsupported; cannot interpret zeros as no allocation'
    result_path=Path(str(prefix)+'-analysis.json');result_path.write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(dict(groups=groups,header=result['trace']['header'],
                         completed_focus_calls=len(result['trace']['complete_focus_calls']),mismatched_exits=result['trace']['mismatched_exits']),indent=2))
    if args.require_single_encode:
        sends=[r for r in result['trace']['complete_focus_calls'] if r['method'].endswith('ControlSession.send')]
        assert len(sends)>=256,'Need complete stacks spanning map pruning, not a missing/empty trace'
        assert all(r['encodes']==1 for r in sends),f'Each send must serialize once; observed {collections.Counter(r["encodes"] for r in sends)}'


if __name__=='__main__':main()
