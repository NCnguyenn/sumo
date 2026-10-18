# Independent review of Phase 1 + Phase 2

What this change does: The implementation adds a pure Kotlin control reducer, motor mixer, validated settings, SUMO-SIM/1 codec/reply tracking, and an independent Python UDP simulator. It provides meaningful safety tests and keeps Android UI, persistent storage, scheduling, and hardware integration outside the authorized Phase 1–2 scope.

Review date: 2026-10-17. Scope: the requirements in `15-review-brief.md`, the addition-only package `15-review-package.txt`, the current Kotlin/Python sources and all six functional Kotlin test classes plus Python tests, and the G1/G2 implementation reports. There is no Git repository. No application source or tests were changed in this review.

Assumed load: one controller and one loopback simulator; snapshots every 20–50 ms; permitted input/calibration values and delayed/reordered replies within the 300 ms acceptance window. The findings below do not depend on high throughput, hostile networking, or any Phase 3 feature.

## Must fix

### 1. [P2] A healthy session expires while changing commands have normal in-flight ACKs

**Location:** `C:/Users/CHI NGUYEN/AndroidStudioProjects/IoT/app/src/main/java/com/aistudy/myapplication/protocol/ReplyTracker.kt:47` and `:95` (timeout at `:116`).

**What this is:** `recordSent` advances `intentFloor` whenever any command field changes, including ordinary wheel PWM. `accept` drops every ACK below that floor before updating progress or RTT. This tries to stop a reply to an earlier command from confirming the current command, but also treats valid network progress as missing communication.

**Reproducible sequence:** Use SID `0123456789abcdef`, endpoint `127.0.0.1:8888`, and a fake monotonic clock. Begin/offer at t=0, record neutral seq1 at t=0, and accept its matching neutral OK ACK at t=1; the tracker is Online. Record drive seq2 with PWM=(10,10) at t=10, then seq3 with PWM=(20,20) at t=40. At t=60 deliver `A1:0123456789abcdef,2,OK,10,10,0,0,0,0,0,0,0\n`. It was actually sent, has the correct peer/SID, is greater than the last accepted ACK, and is only 50 ms old, yet `accept` returns null because the floor is now 3. Send seq4=(30,30) at t=70; ACK seq3 at t=90 is rejected the same way. Continue a new PWM every 30 ms and deliver each matching ACK 50 ms after its send: seq11=(100,100) is sent at t=280; ACK seq10 arrives at t=300 and is discarded. `tick(301)` clears SID and Online because the last recorded progress remains t=1. There was no packet loss, malformed frame, or receiver timeout. At a steady 20 ms send interval, even 25 ms RTT can trigger the same failure.

**Expected versus actual:** G0 §7.1 permits progress from a matching, actually sent, increasing sequence whose age is under 300 ms. Such ACKs should keep communication alive. They must not be allowed to undo a newer STOP/Kill/mode/unlock decision. The implementation currently conflates those two decisions and disconnects during ordinary changing joystick output.

**Fix:** Separate valid communication progress from confirmation of the current control intent. Validate ACKs against the recorded frame and use eligible increasing control ACKs to maintain connection/RTT, while retaining a separate guard against applying obsolete control state to the reducer. A narrowly scoped alternative can preserve barriers for safety/transition intents without advancing the barrier for every ordinary PWM update; it must still prevent stale mode, unlock, macro, or STOP confirmations. Do not simply remove all stale-intent protection.

**Required regression:** A deterministic tracker test with the above changing snapshots and delayed ACKs must remain Online beyond 300 ms. Retain the existing old-motion-after-STOP and Kill/unlock/mode matching tests. The present `C05_unchangedHeartbeatAckCanProgressWhileNewerTickIsInFlight` only tests identical payloads and cannot detect this failure.

**If skipped:** A responsive receiver is reported Offline during steering, the session is unnecessarily retired, and the driver must reconnect. This is a concrete availability/correctness defect, not evidence that the receiver can bypass its watchdog.

### 2. [P2] D-pad diagonals wrongly receive joystick radial normalization

**Location:** `C:/Users/CHI NGUYEN/AndroidStudioProjects/IoT/app/src/main/java/com/aistudy/myapplication/control/ControlReducer.kt:44`–`:45`; calculation in `C:/Users/CHI NGUYEN/AndroidStudioProjects/IoT/app/src/main/java/com/aistudy/myapplication/control/DriveMixer.kt:24`–`:27`.

**What this is:** The reducer sends full D-pad axes to the joystick mixer with sensitivity=1 and deadZone=0. That disables the named sensitivity/dead-zone settings, but still divides a diagonal vector by its radius before applying `turnPercent`.

**Reproducible sequence:** Construct settings with `inputMode=D_PAD`, limiter=60, calibration `sensitivity=1, deadZone=0, leftGain=1, rightGain=1, turnPercent=30`. Connect, press FORWARD with pointer 0, then RIGHT with pointer 1. The first press produces (153,153). With both held, full D-pad axes are x=1,y=1. G0 §5.3 requires turn=0.3, forward=1, pre-normalized wheels=(1.3,0.7), shared scale=1.3; after limiter and rounding the expected result is **(153,82)**. Current code first makes forward=1/sqrt(2) and turn=0.3/sqrt(2). Their sum is below 1, so wheel normalization does not restore the lost magnitude; actual output is **(141,76)**. Reversing the turn direction gives expected (82,153) versus actual (76,141). No Boost is needed.

**Expected versus actual:** D-pad uses full axis vectors, while radial normalization belongs to the joystick path. At turnPercent=100 the later wheel normalization happens to cancel this difference, which is why the existing default-calibration diagonal tests pass. The defect is visible at the approved low end of turnPercent, below approximately 41.42%.

**Fix:** Keep the D-pad vector unmodified until turn weighting and wheel mixing; apply radial dead-zone/sensitivity normalization only to joystick input. Reuse the shared downstream wheel mixing, gains, limiter/Boost and symmetric rounding so the two input paths do not acquire divergent safety behavior.

**Required regression:** Add reducer assertions for FORWARD+RIGHT and FORWARD+LEFT with turnPercent=30 and limiter=60, including exact expected PWM above. Keep joystick radial/dead-zone tests and existing D-pad sensitivity/dead-zone independence tests. The current F03 tests cover a single D-pad axis and a joystick pivot, not this interaction.

**If skipped:** A supported calibration silently changes D-pad diagonal speed and violates the approved PWM formula; calibration results do not match the documented contract.

## Spec-compliance verdict

**Not ready to accept G1/G2 without fixing findings 1 and 2.** The main reducer safety behavior, validation boundaries, strict frame syntax, sequence/session fencing in the receiver, independent watchdog, macro idempotence, configuration ACK matching, and simulator fault scenarios are present. These two supported-input cases remain outside the behavior established by the passing tests.

Phase 3 absence is intentional and is not a finding. In particular, no missing UI, SettingsStore/migration, Android sender, or updater has been treated as a Phase 1–2 defect. Continuous physical holding of the future unlock UI and lifecycle-to-disconnect wiring cannot be established from these isolated core APIs alone.

## Quality verdict

**Fix 1 and 2 first.** The code is compact, uses the existing module and standard libraries, keeps hardware claims limited, and has substantial functional rather than cosmetic tests. The missing cases are interactions between two independently tested features: changing intent plus delayed ACKs, and D-pad diagonals plus turn calibration. No independent source-backed safety defect was found in the Python receiver during this review. This is not a guarantee of correctness for every possible frame sequence.

## Verification and limits

This review used source/event tracing and exact arithmetic, not a new full test-suite run. The root agent separately reported fresh JVM unit/lint and Python 31/31 verification on unchanged sources; those runs are evidence that the existing tests pass, not that the two missing cases are covered. Existing historical red/green logs and interoperability evidence were inspected as context, not substituted for code review. No production integration currently calls these new classes outside their tests, consistent with the explicit stop before Phase 3.

Not checked: Android UI/lifecycle, real sender timing, STOP ≤10 ms, network behavior to hardware, physical braking, firmware security, or hardware calibration. No Phase 3 implementation, source fix, or full suite rerun was performed by this reviewer.
