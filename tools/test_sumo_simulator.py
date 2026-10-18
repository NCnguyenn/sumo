"""Independent receiver assertions for the approved simulator contract."""
import unittest
import json
import socket
import subprocess
import sys
from pathlib import Path
from sumo_simulator import Receiver, Scenario, check_fixtures

SID = "0123456789abcdef"
NONCE = "1111111111111111"
PEER = ("127.0.0.1", 50000)
TRACE_PATH = None


class Clock:
    def __init__(self): self.value = 0
    def __call__(self): return self.value


class SimulatorTests(unittest.TestCase):
    def setUp(self):
        self.clock = Clock()
        self.logs = []
        self.r = self.make()

    def make(self, caps=3):
        ids = iter(f"{int(SID, 16)+i:016x}" for i in range(20))
        return Receiver(clock=self.clock, sid_factory=lambda: next(ids), caps=caps, log=self.logs.append)

    def tearDown(self):
        if TRACE_PATH:
            with Path(TRACE_PATH).open('a',encoding='utf-8',newline='\n') as output:
                for record in self.logs: output.write(json.dumps(dict(case=self.id(),**record))+'\n')

    def frame(self, seq, body="S:0,0,0,0,0", x=0, mode=0, tid=0, duration=0, sid=SID):
        return f"V1:{sid},{seq},{x},{mode},{tid},{duration}|{body}\n".encode()

    def connect(self, r=None, caps=3):
        r = r or self.r
        self.assertEqual(f"H1:{NONCE},{SID},{caps}\n".encode(), r.handle(f"H1:{NONCE}\n".encode(), PEER))
        self.assertFalse(r.active)
        reply = r.handle(self.frame(1), PEER)
        self.assertEqual(f"A1:{SID},1,OK,0,0,0,0,0,0,0,0,0\n".encode(), reply)
        self.assertTrue(r.active)
        return r

    def send(self, seq, body="S:0,0,0,0,0", **kwargs):
        return self.r.handle(self.frame(seq, body, **kwargs), PEER)

    def zero(self, r=None):
        r = r or self.r
        self.assertEqual((0, 0, 0), (r.state["L"], r.state["R"], r.state["B"]))

    def test_C01_C02_frozen_vectors_from_contract(self):
        self.assertEqual(25, check_fixtures(Path(__file__).resolve().parents[1] / "docs/testing/phase-2/fixtures.tsv"))

    def test_C01_state_flags_alias_and_no_double_boost(self):
        self.connect()
        self.send(2, "S:200,200,1,1,0")
        self.assertEqual((200, 200, 1, 1), tuple(self.r.state[k] for k in ("L", "R", "B", "W")))
        self.send(3, "D:-153,153,0,0,0")
        self.assertEqual((-153, 153), (self.r.state["L"], self.r.state["R"]))

    def test_C03_handshake_requires_neutral_seq_one(self):
        self.r.handle(f"H1:{NONCE}\n".encode(), PEER)
        reply = self.send(1, "S:153,153,0,0,0")
        self.assertIsNotNone(reply)
        self.assertIn(b",BAD,", reply)
        self.assertFalse(self.r.active)
        self.zero()

    def test_C03_wrong_peer_sid_and_pre_handshake_frames_do_not_apply(self):
        self.assertIsNone(self.send(1, "S:153,153,0,0,0"))
        self.connect()
        self.assertIsNone(self.r.handle(self.frame(2, "S:153,153,0,0,0"), ("127.0.0.1", 50001)))
        self.assertIsNone(self.send(2, "S:153,153,0,0,0", sid="2222222222222222"))
        self.zero()

    def test_C03_C04_malformed_frames_never_refresh_watchdog(self):
        self.connect(); self.send(2, "S:153,153,0,0,0")
        valid = self.frame(3)
        bad = [valid[:-1], valid[:-1]+b"\\n", valid[:-1]+b"\r\n", valid+valid, valid+b"x",
               valid.replace(b"S:0,0", b"S:256,0"), valid.replace(b"S:0,0", b"S:-256,0"),
               valid.replace(b"S:0,0", b"S:NaN,0"), valid.replace(b"S:0,0", b"S:+1,0"),
               valid.replace(b"S:0,0", b"S: 1,0"), self.frame(4,"S:0,0,2,0,0"),
               self.frame(5,"S:0,0,0,0"), self.frame(6,"S:0,0,0,0,0,0"),
               b"x"*256+b"\n", b"\xff\n", valid.replace(b"S:", b"S:\x00"),
               self.frame(7,tid=1), self.frame(8,duration=200), self.frame(9,tid=1,duration=2001)]
        for data in bad:
            self.clock.value += 1
            self.r.handle(data, PEER)
            self.assertEqual(0, self.r.last_valid)
        self.clock.value=300; self.r.tick(); self.zero(); self.assertIsNone(self.r.sid)

    def test_C03_number_boundary_and_exhausted_sequence_cannot_wrap(self):
        self.connect(); self.send(2147483647, "S:153,153,0,0,0")
        self.assertEqual(2147483647, self.r.last_seq)
        self.assertIsNone(self.send(2147483648))
        self.send(2, "S:-153,-153,0,0,0")
        self.assertEqual(153, self.r.state["L"])

    def test_C03_raw_legacy_commands_are_fixture_only(self):
        self.connect(); self.send(2, "S:153,153,0,0,0")
        for line in [b"M:0,0\n",b"M:200,200\n",b"M:-200,-200\n",b"M:-180,180\n",b"M:180,-180\n",
                     b"B:1\n",b"B:0\n",b"W:1\n",b"W:0\n",b"K:1\n",b"K:0\n",b"T:180\n",b"S:200,200,1,0,0\n"]:
            self.assertIsNone(self.r.handle(line,PEER))
        self.assertEqual(0,self.r.last_valid)

    def test_C06_payload_rejection_consumes_seq_without_extending_watchdog(self):
        self.connect(); self.send(2,"S:153,153,0,0,0")
        self.clock.value=100
        self.assertIn(b",BAD,",self.send(100,"UNKNOWN"))
        self.assertEqual(100,self.r.last_seq); self.assertEqual(0,self.r.last_valid)
        self.send(99,"S:-153,-153,0,0,0"); self.assertEqual(153,self.r.state["L"])
        self.clock.value=300; self.r.tick(); self.zero()

    def test_C06_duplicate_has_cached_ack_but_does_not_extend_lease(self):
        self.connect(); original=self.frame(2,"S:153,153,0,0,0")
        reply=self.r.handle(original,PEER)
        self.clock.value=200; self.assertEqual(reply,self.r.handle(original,PEER))
        self.assertIsNone(self.send(2,"S:-153,-153,0,0,0"))
        self.assertEqual(0,self.r.last_valid)
        self.clock.value=300; self.r.tick(); self.zero()

    def test_C07_watchdog_299_300_301_and_retired_sid(self):
        self.connect(); self.send(2,"S:255,255,1,0,0")
        self.clock.value=299; self.r.tick(); self.assertEqual(255,self.r.state["L"])
        self.clock.value=300; self.r.tick(); self.zero(); self.assertIsNone(self.r.sid)
        self.clock.value=301; self.assertIsNone(self.send(100,"S:255,255,1,0,0")); self.zero()

    def test_C07_watchdog_overrides_long_macro(self):
        self.connect(); self.send(2,tid=1,duration=1000)
        self.assertEqual(1,self.r.state["turnActive"])
        self.clock.value=300; self.r.tick(); self.zero(); self.assertEqual(0,self.r.state["turnActive"])

    def test_C07_hello_during_active_session_does_not_keep_it_alive(self):
        self.connect(); self.send(2,"S:153,153,0,0,0")
        self.clock.value=299
        self.assertEqual(f"N1:{NONCE},BUSY\n".encode(),self.r.handle(f"H1:{NONCE}\n".encode(),PEER))
        self.clock.value=300; self.r.tick(); self.zero()

    def test_offer_duplicate_does_not_extend_1000ms_deadline(self):
        first=self.r.handle(f"H1:{NONCE}\n".encode(),PEER)
        self.clock.value=999; self.assertEqual(first,self.r.handle(f"H1:{NONCE}\n".encode(),PEER))
        self.clock.value=1000; self.r.tick(); self.assertIsNone(self.r.sid)
        offer=self.r.handle(f"H1:{NONCE}\n".encode(),PEER)
        self.assertNotEqual(first,offer)

    def test_C08_macro_repeats_once_does_not_reset_timer(self):
        self.connect(); self.send(2,tid=1,duration=200)
        self.clock.value=100; self.send(3,tid=1,duration=200)
        self.clock.value=199; self.r.tick(); self.assertEqual(1,self.r.state["turnActive"])
        self.clock.value=200; self.r.tick(); self.zero()
        self.send(4,tid=1,duration=200); self.zero(); self.assertEqual(1,self.r.turn_count)

    def test_C08_macro_id_duration_conflicts_and_new_id_while_busy(self):
        self.connect(); self.send(2,tid=1,duration=200)
        self.assertIn(b",BAD,",self.send(3,tid=1,duration=350))
        self.assertIn(b",BUSY,",self.send(4,tid=2,duration=200))
        self.clock.value=200; self.r.tick(); self.send(5,tid=2,duration=350)
        self.assertEqual(2,self.r.turn_count)
        self.assertIn(b",BAD,",self.send(6,tid=1,duration=200))

    def test_C08_C09_stop_brake_kill_cancel_without_macro_restart(self):
        for body,x in [("S:0,0,0,0,0",0),("S:0,0,0,0,1",0),("S:0,0,0,0,0",1)]:
            self.clock.value=0; self.r=self.make(); self.connect(); self.send(2,tid=1,duration=200)
            self.send(3,body,x=x); self.zero(); self.send(4,tid=1,duration=200)
            self.zero(); self.assertEqual(1,self.r.turn_count)

    def test_C09_old_motion_after_stop_and_kill_never_applies(self):
        self.connect(); old=self.frame(2,"S:153,153,0,0,0")
        self.r.handle(old,PEER); self.send(3); self.r.handle(old,PEER); self.zero()
        self.send(4,x=1); self.r.handle(old,PEER); self.zero(); self.assertEqual(1,self.r.state["X"])

    def test_C09_receiver_restart_does_not_accept_old_boot_sid(self):
        self.connect(); old=self.frame(2,"S:153,153,0,0,0")
        newer=Receiver(clock=self.clock,sid_factory=lambda:"2222222222222222")
        newer.handle(f"H1:{NONCE}\n".encode(),PEER)
        newer.handle(self.frame(1,sid="2222222222222222"),PEER)
        self.assertIsNone(newer.handle(old,PEER)); self.zero(newer)

    def test_kill_persists_across_timeout_and_new_handshake_until_unlock(self):
        self.connect(); self.send(2,x=1); self.clock.value=300; self.r.tick()
        self.r.handle(f"H1:{NONCE}\n".encode(),PEER); sid=self.r.sid
        reply=self.r.handle(self.frame(1,sid=sid),PEER)
        self.assertIn(b",0,0,0,0,0,1,0,0,0\n",reply)
        self.assertIn(b",LOCKED,",self.r.handle(self.frame(2,"S:153,153,0,0,0",sid=sid),PEER))
        self.r.handle(self.frame(3,"U",sid=sid),PEER); self.assertEqual(0,self.r.state["X"]); self.zero()

    def test_unlock_and_close_are_safe_explicit_commands(self):
        self.connect(); self.send(2,"S:153,153,0,0,0")
        self.assertIn(b",BUSY,",self.send(3,"U")); self.assertEqual(153,self.r.state["L"])
        reply=self.send(4,"C"); self.assertIn(b",OK,",reply); self.zero(); self.assertIsNone(self.r.sid)
        self.assertIsNone(self.send(5,"S:153,153,0,0,0"))

    def test_auto_guard_and_manual_inputs_do_not_override_auto(self):
        self.connect(); self.assertIn(b",UNSUPPORTED,",self.send(2,mode=1))
        self.r=self.make(caps=7); self.connect(caps=7)
        self.send(2,"S:153,153,0,0,0"); self.assertIn(b",BUSY,",self.send(3,mode=1))
        self.send(4); self.send(5,mode=1); self.assertEqual(1,self.r.state["mode"])
        self.send(6,"S:153,153,0,0,0"); self.assertEqual(1,self.r.state["mode"]); self.zero()
        self.send(7,"S:0,0,0,0,1"); self.assertEqual(0,self.r.state["mode"]); self.assertEqual(1,self.r.state["K"])
        self.send(8); self.send(9,mode=1); self.send(10,x=1); self.assertEqual(0,self.r.state["mode"]); self.zero()

    def test_config_is_atomic_idempotent_and_does_not_refresh_watchdog(self):
        self.r=self.make(caps=11); self.connect(caps=11)
        self.clock.value=100
        self.assertEqual(f"Q1:{SID},2,1,0,100,OK\n".encode(),self.send(2,"Q:1,0,100"))
        self.assertEqual((0,100),self.r.config_pair)
        self.send(3,"Q:1,0,100"); self.assertEqual((0,100),self.r.config_pair)
        self.assertIn(b",BAD\n",self.send(4,"Q:1,0,101")); self.assertEqual((0,100),self.r.config_pair)
        self.send(5,"Q:2,10,181"); self.assertEqual((0,100),self.r.config_pair)
        self.assertEqual(0,self.r.last_valid); self.clock.value=300; self.r.tick(); self.assertIsNone(self.r.sid)

    def test_config_unsupported_or_busy_keeps_previous_pair(self):
        self.connect(); self.assertIn(b",UNSUPPORTED\n",self.send(2,"Q:1,0,100"))
        self.assertEqual((0,90),self.r.config_pair)
        self.r=self.make(caps=11); self.connect(caps=11); self.send(2,"S:153,153,0,0,0")
        self.assertIn(b",BUSY\n",self.send(3,"Q:1,0,100")); self.assertEqual((0,90),self.r.config_pair)

    def test_scenario_drop_rx_does_not_refresh_watchdog(self):
        self.connect(); self.send(2,"S:153,153,0,0,0")
        s=Scenario(self.r,"drop-rx",fault_after=0)
        self.clock.value=100; self.assertEqual([],s.receive(self.frame(3,"S:255,255,1,0,0"),PEER))
        self.assertEqual(0,self.r.last_valid); self.clock.value=300; s.poll(); self.zero()

    def test_scenario_drop_reply_applies_but_does_not_send_ack(self):
        self.connect(); s=Scenario(self.r,"drop-reply",fault_after=0)
        self.clock.value=100; self.assertEqual([],s.receive(self.frame(2,"S:153,153,0,0,0"),PEER))
        self.assertEqual(153,self.r.state["L"]); self.assertEqual(100,self.r.last_valid)

    def test_scenario_delay_rx_150_and_350_cannot_revive_expired_sid(self):
        for delay in (150,350):
            self.clock.value=0; self.r=self.make(); self.connect()
            s=Scenario(self.r,"delay",fault_after=0,delay_ms=delay,delay_target="rx")
            self.assertEqual([],s.receive(self.frame(2,"S:153,153,0,0,0"),PEER)); self.zero()
            self.clock.value=delay-1; self.assertEqual([],s.poll()); self.zero()
            self.clock.value=delay; replies=s.poll()
            if delay==150: self.assertEqual(153,self.r.state["L"]); self.assertEqual(1,len(replies))
            else: self.zero(); self.assertEqual([],replies); self.assertIsNone(self.r.sid)

    def test_scenario_delayed_reply_is_returned_at_exact_due_time(self):
        self.connect(); s=Scenario(self.r,"delay",fault_after=0,delay_target="reply",delay_ms=150)
        self.assertEqual([],s.receive(self.frame(2,"S:153,153,0,0,0"),PEER)); self.assertEqual(153,self.r.state["L"])
        self.clock.value=149; self.assertEqual([],s.poll())
        self.clock.value=150; self.assertEqual(1,len(s.poll()))

    def test_scenario_duplicate_macro_and_reorder_after_stop(self):
        self.connect(); s=Scenario(self.r,"duplicate",fault_after=0)
        self.assertEqual(2,len(s.receive(self.frame(2,tid=1,duration=200),PEER))); self.assertEqual(1,self.r.turn_count)
        self.r=self.make(); self.connect(); s=Scenario(self.r,"reorder",fault_after=0)
        self.assertEqual([],s.receive(self.frame(2,"S:153,153,0,0,0"),PEER))
        self.assertEqual(1,len(s.receive(self.frame(3),PEER))); self.zero()
        self.clock.value=99; self.assertEqual([],s.poll())
        self.clock.value=100; s.poll(); self.zero(); self.assertEqual(3,self.r.last_seq)

    def test_scenario_malformed_replies_are_deterministic(self):
        self.connect(); s=Scenario(self.r,"malformed",fault_after=0)
        first=s.receive(self.frame(2),PEER)[0][1]
        second=s.receive(self.frame(3),PEER)[0][1]
        third=s.receive(self.frame(4),PEER)[0][1]
        self.assertEqual(11,len(first.decode().strip().split(':',1)[1].split(',')))
        self.assertNotIn(SID.encode(),second)
        self.assertIn(b",5,OK,",third)

    def test_cli_rejects_invalid_endpoint_limits_and_capabilities(self):
        script=Path(__file__).with_name('sumo_simulator.py')
        for args in [('--host','0.0.0.0'),('--port','65536'),('--fault-after','-1'),('--delay-ms','-1'),('--caps','0')]:
            result=subprocess.run([sys.executable,str(script),*args],capture_output=True,text=True,timeout=5)
            self.assertEqual(2,result.returncode)
            self.assertIn('sumo_simulator:',result.stderr)

    def test_cli_port_in_use_reports_error_and_keeps_owner_alive(self):
        with socket.socket(socket.AF_INET,socket.SOCK_DGRAM) as owner:
            owner.bind(('127.0.0.1',0)); owner.settimeout(1)
            endpoint=owner.getsockname()
            result=subprocess.run([sys.executable,str(Path(__file__).with_name('sumo_simulator.py')),
                                   '--port',str(endpoint[1])],capture_output=True,text=True,timeout=5)
            self.assertEqual(2,result.returncode); self.assertIn('sumo_simulator:',result.stderr)
            owner.sendto(b'owner-alive',endpoint)
            self.assertEqual(b'owner-alive',owner.recvfrom(32)[0])


if __name__ == "__main__": unittest.main()
