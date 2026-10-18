"""Phase 4 AVD-only driver; requires locally built debug + androidTest APKs."""
from pathlib import Path
import argparse
import json
import queue
import subprocess
import sys
import threading
import time


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--serial',required=True)
    parser.add_argument('--adb',default=str(Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe'))
    parser.add_argument('--only',help='rerun one failed scenario label while preserving the other recorded cases')
    parser.add_argument('--evidence-dir',type=Path,default=Path('docs/testing/phase-4/evidence'))
    args=parser.parse_args()
    root=Path(__file__).resolve().parent.parent
    evidence=root/args.evidence_dir
    evidence.mkdir(parents=True,exist_ok=True)
    def adb(*command,timeout=120):
        result=subprocess.run([args.adb,'-s',args.serial,*command],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=timeout)
        if result.returncode: raise RuntimeError(result.stdout+result.stderr)
        return result.stdout+result.stderr
    assert args.serial.startswith('emulator-') and adb('shell','getprop','ro.kernel.qemu').strip()=='1','Only an explicitly selected AVD is allowed'
    installs=[]
    for apk in ('app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'):
        installs.append(dict(command=[args.adb,'-s',args.serial,'install','-r',apk],output=adb('install','-r',str(root/apk))))
    (evidence/'fault-install-avd.json').write_text(json.dumps(installs,indent=2),encoding='utf-8')
    cases=[('drop-rx',150,'rx'),('drop-reply',150,'rx'),('delay',150,'rx'),('delay',350,'rx'),
           ('delay',150,'reply'),('duplicate',150,'rx'),('reorder',150,'rx'),('malformed',150,'rx')]
    summary=json.loads((evidence/'android-fault-summary.json').read_text(encoding='utf-8')) if args.only else []
    for name,delay,target in cases:
        label=f'{name}-{delay}-{target}'
        if args.only and args.only!=label:continue
        fault_after=80 if name=='delay' and delay==350 else 20
        log=evidence/f'android-fault-{label}.jsonl'
        command=[sys.executable,str(root/'tools/sumo_simulator.py'),'--host','127.0.0.1','--port','8888',
                 '--scenario',name,'--delay-ms',str(delay),'--delay-target',target,'--fault-after',str(fault_after),'--log',str(log)]
        server=subprocess.Popen(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8')
        item=dict(scenario=name,delay_ms=delay,target=target,server_command=command,passed=False)
        try:
            ready=queue.Queue()
            threading.Thread(target=lambda:ready.put(server.stdout.readline()),daemon=True).start()
            assert ready.get(timeout=5).strip()=='READY 8888','Receiver did not start; no unrelated process is stopped'
            adb('shell','am','force-stop','com.aistudy.myapplication')
            test_args=['shell','am','instrument','-w','-r','-e','class','com.aistudy.myapplication.AndroidFaultTest',
                       '-e','scenario',name,'-e','delay',str(delay),'com.aistudy.myapplication.test/androidx.test.runner.AndroidJUnitRunner']
            output=adb(*test_args)
            (evidence/f'android-fault-{label}.txt').write_text('COMMAND: '+json.dumps([args.adb,'-s',args.serial,*test_args])+'\n'+output,encoding='utf-8')
            assert 'OK (1 test)' in output and 'FAILURES!!!' not in output,'Instrumentation did not pass'
            time.sleep(.4)  # allow receiver watchdog/held packets to log after the app has closed
            rows=[json.loads(line) for line in log.read_text(encoding='utf-8').splitlines() if line.strip()]
            assert any(row['applied']['L'] or row['applied']['R'] for row in rows),'No receiver motion evidence'
            assert any(row['event']=='scenario' for row in rows),'Fault was never activated'
            assert rows[-1]['applied']['L']==rows[-1]['applied']['R']==0,'Receiver not stopped'
            if name=='drop-rx' or (name=='delay' and delay==350):
                assert any(row['event']=='watchdog' for row in rows),'No watchdog evidence'
            if name=='duplicate':
                assert sum(row['event']=='macro-start' for row in rows)==1,'Macro was not executed exactly once'
            if name=='reorder':
                assert any(row['reason']=='duplicate-or-old-seq' for row in rows),'Old frame was not replayed/rejected'
            stopped=False;motion=False
            for row in rows:
                moving=bool(row['applied']['L'] or row['applied']['R'])
                if row['event']=='macro-start':stopped=False;motion=False
                if stopped:assert not moving,'Motion revived after an applied STOP'
                if moving:motion=True
                if motion and row['event'] in ('accept','watchdog') and not moving:stopped=True
            item.update(passed=True,records=len(rows))
        except Exception as error:
            item['error']=str(error)
        finally:
            server.terminate()
            try:server.wait(timeout=3)
            except subprocess.TimeoutExpired:server.kill();server.wait(timeout=3)
        summary=[old for old in summary if (old['scenario'],old['delay_ms'],old['target'])!=(name,delay,target)]
        summary.append(item)
        (evidence/'android-fault-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
        print(json.dumps(dict(case=label,passed=item['passed'],error=item.get('error')),ensure_ascii=False),flush=True)
    return 0 if all(item['passed'] for item in summary) else 1


if __name__=='__main__':sys.exit(main())
