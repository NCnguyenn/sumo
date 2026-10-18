"""SUMO-SIM/1 receiver for local tests, independent of the Android encoder."""
import argparse
import csv
import heapq
import ipaddress
import json
import re
import secrets
import socket
import time
import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


class Receiver:
    def __init__(self, clock=None, sid_factory=None, caps=3, log=None):
        if not 0 < caps <= 2147483647 or not caps & 1: raise ValueError("caps must include state capability")
        self.clock=clock or (lambda: time.monotonic_ns() // 1_000_000)
        self.sid_factory=sid_factory or (lambda: secrets.token_hex(8))
        self.log=log; self.caps=caps; self.sid=None; self.active=False
        self.last_valid=None; self.last_seq=0; self.turn_count=0; self.config_pair=(0,90)
        self.state=dict(L=0,R=0,B=0,W=0,K=0,X=0,mode=0,turnId=0,turnActive=0)
        self.peer=None; self.nonce=None; self.offer_deadline=None; self.previous_sid=None
        self.acks={}; self.highest_turn=0; self.turn_duration=0; self.turn_deadline=None
        self.config_id=0

    def emit(self,event,data=b"",peer=None,reason="",accepted=None):
        if self.log:
            self.log(dict(at_ms=self.clock(),event=event,sid=self.sid,seq=self.last_seq,
                          peer=peer or self.peer,payload_hex=data.hex(),reason=reason,accepted=accepted,
                          applied=dict(self.state),config=list(self.config_pair),active=self.active))

    @staticmethod
    def ident(value):
        if not re.fullmatch(r"[0-9a-f]{16}",value) or value=="0"*16: raise ValueError("invalid identity")
        return value

    @staticmethod
    def number(value,low=0,high=2147483647):
        if not re.fullmatch(r"-?[0-9]+" if low<0 else r"[0-9]+",value): raise ValueError("invalid integer")
        result=int(value)
        if not low<=result<=high: raise ValueError("integer out of range")
        return result

    @staticmethod
    def flag(value):
        if value not in ("0","1"): raise ValueError("flag must be 0 or 1")
        return int(value)

    @staticmethod
    def wire_text(data):
        if not 1<=len(data)<=256 or data[-1:]!=b"\n" or any(not 32<=c<=126 for c in data[:-1]):
            raise ValueError("ASCII frame requires exactly one final LF, maximum 256 bytes")
        return data[:-1].decode("ascii")

    def stop(self,brake=0,kill=None):
        if kill is None: kill=self.state["X"]
        self.state.update(L=0,R=0,B=0,W=0,K=brake,X=kill,mode=0,turnId=0,turnActive=0)
        self.turn_deadline=None

    def neutral(self):
        return all(self.state[k]==0 for k in ("L","R","B","W","K","turnActive"))

    def retire(self,reason):
        self.emit("retire",reason=reason)
        self.previous_sid=self.sid
        self.sid=None; self.peer=None; self.nonce=None; self.active=False
        self.offer_deadline=None; self.last_valid=None; self.last_seq=0; self.acks.clear()
        self.highest_turn=0; self.turn_duration=0; self.config_id=0

    def advance(self,now):
        if self.active and now-self.last_valid>=300:
            self.stop(); self.emit("watchdog",reason="300ms without fresh applied control"); self.retire("watchdog")
        elif self.turn_deadline is not None and now>=self.turn_deadline:
            self.state.update(L=0,R=0,B=0,turnActive=0); self.turn_deadline=None
            self.emit("macro-complete")
        if not self.active and self.sid is not None and now>=self.offer_deadline:
            self.retire("offer-expired")

    def tick(self): self.advance(self.clock())

    def ack(self,seq,result):
        values=",".join(str(self.state[k]) for k in ("L","R","B","W","K","X","mode","turnId","turnActive"))
        return f"A1:{self.sid},{seq},{result},{values}\n".encode("ascii")

    def cache(self,seq,data,reply):
        self.acks[seq]=(data,reply)
        while len(self.acks)>256: self.acks.pop(next(iter(self.acks)))

    def hello(self,text,data,peer,now):
        nonce=self.ident(text[3:])
        if self.active or (self.sid is not None and (self.peer!=peer or self.nonce!=nonce)):
            self.emit("reject",data,peer,"session-busy",False)
            return f"N1:{nonce},BUSY\n".encode("ascii")
        if self.sid is None:
            new_sid=self.ident(self.sid_factory())
            if new_sid==self.previous_sid: raise ValueError("SID must be fresh")
            self.sid=new_sid; self.peer=peer; self.nonce=nonce; self.offer_deadline=now+1000
            self.stop(); self.emit("offer",data,peer)
        return f"H1:{nonce},{self.sid},{self.caps}\n".encode("ascii")

    def drive(self,x,mode,tid,duration,body,now):
        p=body[2:].split(',')
        if len(p)!=5: raise ValueError("drive field count")
        left,right=self.number(p[0],-255,255),self.number(p[1],-255,255)
        boost,weapon,brake=(self.flag(v) for v in p[2:])
        zero=(left,right,boost,weapon,brake)==(0,0,0,0,0)
        if boost and left==right==0: raise ValueError("boost without direction")
        if x and (mode or tid or not zero): raise ValueError("contradictory Kill")
        if brake and (x or mode or tid or (left,right,boost,weapon)!=(0,0,0,0)): raise ValueError("contradictory Brake")
        if (mode or tid) and (x or not zero): raise ValueError("contradictory mode or turn")
        if mode and tid: raise ValueError("AUTO and macro cannot coexist")
        if x: self.stop(kill=1); return "OK"
        if self.state["X"]: return "OK" if zero and mode==tid==0 else "LOCKED"
        if brake: self.stop(brake=1); return "OK"
        if mode:
            if not self.caps & 4: return "UNSUPPORTED"
            if not self.neutral(): return "BUSY"
            self.stop(); self.state["mode"]=1; return "OK"
        if self.state["mode"]:
            if not zero or tid: return "BUSY"
            self.stop(); return "OK"
        if tid:
            if not self.caps & 2: return "UNSUPPORTED"
            if tid<self.highest_turn: return "BAD"
            if tid==self.highest_turn: return "OK" if duration==self.turn_duration else "BAD"
            if not self.neutral(): return "BUSY"
            self.highest_turn=tid; self.turn_duration=duration
            self.stop(); self.state.update(L=-255,R=255,turnId=tid,turnActive=1)
            self.turn_deadline=now+duration; self.turn_count+=1; self.emit("macro-start")
            return "OK"
        if self.state["turnActive"] and not zero: return "BUSY"
        self.turn_deadline=None
        self.state.update(L=left,R=right,B=boost,W=weapon,K=0,mode=0,turnId=0,turnActive=0)
        return "OK"

    def configure(self,seq,body):
        p=body[2:].split(',')
        if len(p)!=3: raise ValueError("config field count")
        cfg=self.number(p[0],1); home=self.number(p[1],0,180); active=self.number(p[2],0,180)
        if not self.caps & 8: result="UNSUPPORTED"
        elif self.state["X"]: result="LOCKED"
        elif not self.neutral() or self.state["mode"]: result="BUSY"
        elif cfg<self.config_id or (cfg==self.config_id and (home,active)!=self.config_pair): result="BAD"
        else:
            self.config_pair=(home,active); self.config_id=cfg; result="OK"
            self.emit("config-applied")
        return f"Q1:{self.sid},{seq},{cfg},{home},{active},{result}\n".encode("ascii"),result

    def handle(self,data,peer):
        now=self.clock(); self.advance(now)
        try:
            text=self.wire_text(data)
            if text.startswith("H1:"): return self.hello(text,data,peer,now)
            if not text.startswith("V1:") or text.count('|')!=1: raise ValueError("V1 envelope required")
            envelope,body=text[3:].split('|')
            p=envelope.split(',')
            if len(p)!=6: raise ValueError("envelope field count")
            sid=self.ident(p[0]); seq=self.number(p[1],1); x=self.flag(p[2]); mode=self.flag(p[3])
            tid=self.number(p[4]); duration=self.number(p[5],0,2000)
            if (tid==0 and duration!=0) or (tid>0 and duration==0): raise ValueError("turn metadata mismatch")
        except ValueError as error:
            self.emit("reject",data,peer,str(error),False); return None
        if sid!=self.sid or peer!=self.peer:
            self.emit("reject",data,peer,"wrong-peer-or-retired-sid",False); return None
        if seq<=self.last_seq:
            cached=self.acks.get(seq)
            self.emit("reject",data,peer,"duplicate-or-old-seq",False)
            return cached[1] if cached and cached[0]==data else None
        self.last_seq=seq
        if not self.active:
            if seq==1 and (x,mode,tid,duration)==(0,0,0,0) and body=="S:0,0,0,0,0":
                self.active=True; self.last_valid=now; self.offer_deadline=None; result="OK"
            else: result="BAD"
            reply=self.ack(seq,result); self.cache(seq,data,reply)
            self.emit("accept" if result=="OK" else "reject",data,peer,result,result=="OK")
            return reply
        control=True
        try:
            if body.startswith(("S:","D:")):
                result=self.drive(x,mode,tid,duration,body,now); reply=self.ack(seq,result)
            else:
                if (x,mode,tid,duration)!=(0,0,0,0): raise ValueError("non-neutral control envelope")
                if body=="U":
                    result="OK" if self.neutral() and self.state["mode"]==0 else "BUSY"
                    if result=="OK": self.stop(kill=0)
                    reply=self.ack(seq,result)
                elif body=="C":
                    self.stop(); reply=self.ack(seq,"OK")
                    self.emit("accept",data,peer,"closed",True); self.retire("close")
                    return reply
                elif body.startswith("Q:"):
                    control=False; reply,result=self.configure(seq,body)
                else: raise ValueError("unsupported payload")
        except ValueError:
            result="BAD"; reply=self.ack(seq,result)
        if result=="OK" and control: self.last_valid=now
        self.cache(seq,data,reply)
        self.emit("accept" if result=="OK" else "reject",data,peer,result,result=="OK")
        return reply


class Scenario:
    def __init__(self, receiver, name="normal", fault_after=2, delay_ms=150, delay_target="rx"):
        if name not in ("normal","drop-rx","drop-reply","delay","duplicate","reorder","malformed"): raise ValueError("unknown scenario")
        if fault_after<0 or delay_ms<0 or delay_target not in ("rx","reply"): raise ValueError("invalid scenario arguments")
        self.receiver=receiver; self.name=name; self.fault_after=fault_after; self.delay_ms=delay_ms; self.delay_target=delay_target
        self.count=0; self.order=0; self.queue=[]; self.held=None; self.malformed_count=0

    def later(self,delay,kind,data,peer):
        self.order+=1; heapq.heappush(self.queue,(self.receiver.clock()+delay,self.order,kind,data,peer))

    def deliver(self,data,peer):
        reply=self.receiver.handle(data,peer)
        return [] if reply is None else [(peer,reply)]

    @staticmethod
    def motion(data):
        try:
            body=data.split(b'|',1)[1]
            if not body.startswith((b'S:',b'D:')): return None
            p=body[2:].split(b',')
            return int(p[0])!=0 or int(p[1])!=0
        except (IndexError,ValueError): return None

    def receive(self,data,peer):
        self.receiver.tick()
        is_control=data.startswith(b'V1:')
        if is_control: self.count+=1
        if not is_control or self.count<=self.fault_after or self.name=="normal": return self.deliver(data,peer)
        self.receiver.emit("scenario",data,peer,self.name)
        if self.name=="drop-rx": return []
        if self.name=="delay" and self.delay_target=="rx":
            self.later(self.delay_ms,"rx",data,peer); return []
        if self.name=="reorder":
            motion=self.motion(data)
            if motion is True and self.held is None:
                self.held=(data,peer); return []
            replies=self.deliver(data,peer)
            if motion is False and self.held is not None:
                old,old_peer=self.held; self.held=None; self.later(100,"rx",old,old_peer)
            return replies
        replies=self.deliver(data,peer)
        if self.name=="drop-reply": return []
        if self.name=="delay":
            for target,reply in replies: self.later(self.delay_ms,"reply",reply,target)
            return []
        if self.name=="duplicate": return replies+self.deliver(data,peer)
        if self.name=="malformed":
            mutated=[]
            for target,reply in replies:
                prefix,fields=reply.decode('ascii').rstrip('\n').split(':',1); p=fields.split(',')
                kind=self.malformed_count%3; self.malformed_count+=1
                if kind==0: p=p[:-1]
                elif kind==1: p[0]='f'*16
                else: p[1]=str(int(p[1])+1)
                changed=(prefix+':'+','.join(p)+'\n').encode('ascii')
                self.receiver.emit("scenario-reply",changed,target,"malformed-"+str(kind))
                mutated.append((target,changed))
            return mutated
        return replies

    def poll(self):
        self.receiver.tick(); replies=[]; now=self.receiver.clock()
        while self.queue and self.queue[0][0]<=now:
            _,_,kind,data,peer=heapq.heappop(self.queue)
            if kind=="rx": replies.extend(self.deliver(data,peer))
            else: replies.append((peer,data))
        return replies


def check_fixtures(path, log=None):
    receivers={}; clocks={}; count=0
    with open(path,encoding='utf-8',newline='') as file:
        for row in csv.DictReader(file,delimiter='\t'):
            group=row['group']
            if group not in receivers:
                clocks[group]=[0]
                receivers[group]=Receiver(clock=lambda g=group:clocks[g][0],sid_factory=lambda:'0123456789abcdef',
                                           caps=11 if group=='servo' else 3,log=log)
            clocks[group][0]=int(row['at_ms'])
            actual=receivers[group].handle(bytes.fromhex(row['request_hex']),('127.0.0.1',50000))
            expected=None if row['reply_hex']=='-' else bytes.fromhex(row['reply_hex'])
            if actual!=expected: raise AssertionError(f"{row['name']}: expected {expected!r}, actual {actual!r}")
            count+=1
    return count


def self_test(junit_path=None,trace_path=None):
    import test_sumo_simulator
    SimulatorTests=test_sumo_simulator.SimulatorTests
    test_sumo_simulator.TRACE_PATH=trace_path
    if trace_path:
        output=Path(trace_path); output.parent.mkdir(parents=True,exist_ok=True); output.write_text('',encoding='utf-8')
    suite=unittest.defaultTestLoader.loadTestsFromTestCase(SimulatorTests)
    cases=list(suite)
    result=unittest.TextTestRunner(verbosity=2).run(suite)
    if junit_path:
        failures={t.id():message for t,message in result.failures}
        errors={t.id():message for t,message in result.errors}
        root=ET.Element("testsuite",name="sumo_simulator",tests=str(result.testsRun),
                        failures=str(len(failures)),errors=str(len(errors)),skipped=str(len(result.skipped)))
        for test in cases:
            case=ET.SubElement(root,"testcase",name=test.id())
            if test.id() in failures: ET.SubElement(case,"failure").text=failures[test.id()]
            if test.id() in errors: ET.SubElement(case,"error").text=errors[test.id()]
        output=Path(junit_path); output.parent.mkdir(parents=True,exist_ok=True)
        ET.ElementTree(root).write(output,encoding="utf-8",xml_declaration=True)
    return 0 if result.wasSuccessful() else 1


def main():
    parser=argparse.ArgumentParser(description="Local SUMO-SIM/1-draft1 test receiver; no firmware claims")
    mode=parser.add_mutually_exclusive_group()
    mode.add_argument("--self-test",action="store_true")
    mode.add_argument("--check-fixtures",type=Path)
    parser.add_argument("--junit")
    parser.add_argument("--host",default="127.0.0.1")
    parser.add_argument("--port",type=int,default=8888,help="0 chooses an ephemeral port for automated loopback tests")
    parser.add_argument("--scenario",choices=("normal","drop-rx","drop-reply","delay","duplicate","reorder","malformed"),default="normal")
    parser.add_argument("--fault-after",type=int,default=2,help="number of V1 datagrams processed normally before fault injection")
    parser.add_argument("--delay-ms",type=int,default=150)
    parser.add_argument("--delay-target",choices=("rx","reply"),default="rx")
    parser.add_argument("--caps",type=int,default=3,help="simulator fixture capabilities; default AUTO disabled")
    parser.add_argument("--log",type=Path)
    args=parser.parse_args()
    if args.self_test: return self_test(args.junit,args.log)
    if args.junit: parser.error("--junit requires --self-test")
    try:
        address=ipaddress.ip_address(args.host)
        if address.version!=4 or not address.is_loopback: raise ValueError("Phase 2 receiver binds IPv4 loopback only")
        if not 0<=args.port<=65535 or args.fault_after<0 or args.delay_ms<0: raise ValueError("invalid port or scenario limits")
        if args.log: args.log.parent.mkdir(parents=True,exist_ok=True)
        log_file=args.log.open('w',encoding='utf-8',newline='\n') if args.log else None
        try:
            def emit(record):
                if log_file: log_file.write(json.dumps(record,ensure_ascii=True)+'\n'); log_file.flush()
            if args.check_fixtures:
                count=check_fixtures(args.check_fixtures,emit)
                print(json.dumps(dict(vectors=count,passed=count)))
                return 0
            receiver=Receiver(caps=args.caps,log=emit)
            scenario=Scenario(receiver,args.scenario,args.fault_after,args.delay_ms,args.delay_target)
            with socket.socket(socket.AF_INET,socket.SOCK_DGRAM) as server:
                server.bind((args.host,args.port)); server.settimeout(0.01)
                print(f"READY {server.getsockname()[1]}",flush=True)
                while True:
                    for target,reply in scenario.poll(): server.sendto(reply,target)
                    try: data,peer=server.recvfrom(65536)
                    except socket.timeout: continue
                    for target,reply in scenario.receive(data,peer): server.sendto(reply,target)
        finally:
            if log_file: log_file.close()
    except KeyboardInterrupt:
        return 0
    except (OSError,ValueError,AssertionError) as error:
        print(f"sumo_simulator: {error}",file=sys.stderr)
        return 2


if __name__ == "__main__": sys.exit(main())
