# Independent re-review of the two Phase 1–2 fixes

What this change does: The mixer now applies radial preprocessing only to joystick input, preserving full D-pad axes before turn weighting. The reply tracker now counts validated acknowledgments of superseded requests as communication progress while withholding them from the reducer.

Date: 2026-10-17. This re-review covers only findings 1 and 2 in `15-independent-review.md`, their fix delta in `18-review-diff.txt`, directly affected code/callers, and regression evidence. The original review report is preserved. No source or tests were edited, and no test suite was rerun by this reviewer.

## Finding resolutions

### Finding 1 — [P2] Healthy session expires with changing commands and in-flight ACKs: RESOLVED

Reviewed `C:/Users/CHI NGUYEN/AndroidStudioProjects/IoT/app/src/main/java/com/aistudy/myapplication/protocol/ReplyTracker.kt:70`–`:117`, especially the ordering at lines 95, 104, and 107–110.

Peer/SID, actually-sent sequence, increasing ACK sequence, frame age, and successful state-to-request matching are still checked before progress changes. A valid control ACK updates `lastProgress`, RTT, `lastAck`, and pending-frame retirement before the superseded-intent guard returns null. This gives the receiver credit for a fresh response without letting its obsolete state confirm the current reducer intent. Invalid or duplicate responses still cannot extend progress. Configuration acknowledgments remain outside control-progress updates.

The original 30 ms changing-PWM / 50 ms ACK-delay sequence is now covered by `C05_changingDriveAcksKeepSessionAliveWithoutConfirmingSupersededState`. It verifies 50 ms RTT, retained SID/Online status past t=301, null returns for obsolete states, acceptance of the current ACK, rejection of its duplicate, and timeout exactly 300 ms after the last valid progress. Thus the fix also preserves eventual loss-of-contact detection.

`C05_supersededMismatchedAckCannotMaintainProgress` verifies that mismatched PWM, wrong peer, and unsent sequence do not maintain the session. `C05_supersededSafetyAcksOnlyAdvanceCommunication` checks that obsolete motion, neutral, Kill, unlock, AUTO, macro, and close acknowledgments are withheld from the reducer while the current request can still be confirmed. Existing matching/STOP tests remain in the passing suite. These are tracker-level guarantees; Android sender/lifecycle integration remains out of scope.

Inspected red evidence: `17c-p2-red-xml` contains 20 tests with two failures, both asserting expected RTT 50 versus actual 1. Inspected green evidence: `17d-p2-green-xml` contains the same 20 tests with no failures/errors/skips.

### Finding 2 — [P2] D-pad diagonals receive joystick radial normalization: RESOLVED

Reviewed `C:/Users/CHI NGUYEN/AndroidStudioProjects/IoT/app/src/main/java/com/aistudy/myapplication/control/DriveMixer.kt:16`–`:42` and both production callers in `C:/Users/CHI NGUYEN/AndroidStudioProjects/IoT/app/src/main/java/com/aistudy/myapplication/control/ControlReducer.kt:40` and `:44`–`:45`.

The D-pad caller explicitly selects `InputMode.D_PAD`; the joystick caller retains the default joystick path. Only joystick executes radius, dead-zone, and sensitivity preprocessing. Both paths retain finite-input rejection, input clamp, turn weighting, common wheel normalization, gains, limiter/Boost, symmetric rounding, and PWM bounds. D-pad center still returns zero through the shared zero-peak branch, without a divide-by-zero path.

`F03_dpadForwardRightUsesFullAxesWithLowTurnCalibration` and `F03_dpadForwardLeftUsesFullAxesWithLowTurnCalibration` cover the original turnPercent=30 / limiter=60 cases and require (153,82) and (82,153). Both run with default joystick calibration and sensitivity=0.25/deadZone=0.3, proving those joystick settings do not leak into D-pad output. `F03_joystickDiagonalsKeepRadialNormalizationWithLowTurnCalibration` preserves joystick results (141,76) and (76,141). Existing mixer/calibration tests remain passing.

Inspected red evidence: `17a-p1-red-xml` contains 36 tests with the two exact original PWM failures: expected (153,82) versus actual (141,76), and expected (82,153) versus actual (76,141). Inspected green evidence: `17b-p1-green-xml` contains the same 36 tests with no failures/errors/skips.

## Spec-compliance verdict

**Pass for the two reviewed fixes; both prior findings are closed.** The current code resolves the identified G0 §7.1 communication-progress and §5.3 D-pad mixing deviations. No new spec defect was found in these changes or their directly affected paths. This removes the two blockers raised by the original independent review and supports presenting G1/G2 for the user's joint acceptance. It is not acceptance on the user's behalf and does not authorize Phase 3.

## Quality verdict

**Ship these fixes within Phase 1–2.** Both changes target the original root cause with small code changes and regression tests that failed for the reported reasons before the fixes. Existing validation and reducer protection remain present. No new dependency, framework, or unrelated feature was added. There are no new actionable findings from this limited re-review.

## Verification evidence and limits

Inspected `18-final-gradle.txt`: `BUILD SUCCESSFUL`, `EXIT: 0`, with unit tests and lint executed. The seven XML files in `18-final-xml` total **60 tests, 0 failures, 0 errors, 0 skipped**: reducer 28, mixer 8, settings 3, codec 6, tracker 13, interoperability 1, and sample 1. The current lint XML contains 12 warnings and no errors; the root verification identifies these as the baseline warnings. These are independently read execution artifacts, not a new run by this reviewer.

Python receiver sources are unchanged by these fixes. Their prior review and 31-test verification remain applicable; they were not re-reviewed or rerun here.

Not checked: Phase 3 implementation, Android UI/lifecycle or scheduler integration, STOP ≤10 ms, real-network or hardware behavior. This report closes two specific findings and does not claim exhaustive system correctness or physical safety validation.
