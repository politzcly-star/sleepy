# CQIE M3 Gate Report

```text
Task: CQIE-WEBVIEW-OFFLINE-IMPORT / M3 offline-safe refresh
Decision: CONDITIONAL GO

Baseline and scope:
- Input checkpoint: 70dd47e (completed M2).
- Frozen M3 scope: preview and confirmation, selected-table atomic replacement, Room migration,
  persistent whole-week/no-time items, cancellation/failure preservation, and weekly presentation.
- Root route remained the platform-selected model. Required Terra High terra_worker and Luna Medium
  luna_verifier child roles were unavailable, so no delegated implementation or independent review
  is claimed.
- CodeGraph was unavailable; review used rg, source-tree traversal, DAO/repository callers, generated
  Room validation through instrumentation, and focused/full test entry points.

Implemented behavior:
- CQIE parse results stay in memory until confirmation. Back/dismiss clears the pending preview and
  performs no Room write.
- The preview names the target table and lists every scheduled and unscheduled item. Whole-week and
  no-time rows are represented separately rather than receiving invented periods.
- Room v4 adds cqie_unscheduled_courses with an explicit 3-to-4 migration; the production builder no
  longer uses destructive migration fallback.
- replaceCqieSnapshot validates a non-empty result, updates or creates the intended timetable, and
  replaces scheduled and unscheduled collections in one Room transaction. Existing target names are
  preserved, repeated refreshes reuse the target ID, and first import creates a default target.
- A failed persistence operation rolls back table metadata and both collections. Old course alarm IDs
  are cancelled only after a successful commit, preventing stale reminders after replacement.
- The weekly screen shows a bounded, scrollable full-width band for unscheduled items in the selected
  week. These items do not enter the timed grid, today view, reminders, or widgets.

Checks run (command, exit, result):
- CQIE entity/parser focused JVM tests plus assembleDebug: exit 0.
- Focused committed Robolectric Room suite: exit 0; 5 tests, 0 failures/errors/skips.
- Full testDebugUnitTest on latest M3 sources: exit 0; 586 tests, 0 failures/errors/skips.
- connectedDebugAndroidTest on Sleepy_CQIE_API_36_1_x86_64: exit 0; 5 tests, 0 failures/errors/skips.
  The temporary device harness covered first import, successful target replacement, forced SQLite
  persistence rollback, zero-valid-row preservation, and manual v3-to-v4 migration with prior data
  retained. The same tests are committed under the contract-allowed app/src/test path and run with
  Robolectric; app/src/androidTest is not part of the delivered change.
- assembleDebug: exit 0 after the final M3 code changes.
- git diff --check: exit 0; only Git's existing LF-to-CRLF worktree notices were emitted.
- One filtered connected-test invocation failed before test execution because PowerShell interpreted
  the Gradle -P runner argument as a task. The complete androidTest source set was then run directly
  and passed all five tests.

Root review and repairs:
- Found and repaired stale course-level alarms after atomic replacement by retaining old IDs until the
  transaction commits, then cancelling those alarms before normal rescheduling.
- Found and repaired unbounded whole-week content that could displace the weekly grid.
- Found and repaired an ambiguous confirmation dialog by displaying the replacement target name.
- No open M3 code finding remains in the root review.

Authenticated runtime evidence:
- M2 previously proved the production same-origin fetch and parser against the authenticated endpoint:
  response length 27873, 31 source rows, 28 scheduled records, and 3 unscheduled source rows.
- Instrumentation installation cleared the WebView session. A subsequent user login attempt still left
  the app on the privacy-safe path https://a.cqie.edu.cn/cas/login, so latest-code live cancellation,
  confirmation, Room count, and repeated-refresh checks are not claimed here.
- These live checks remain a mandatory M4/final acceptance condition and require only user credential
  entry; the implementation and local automated verification can continue without credential access.

Independent read-only review:
- Required Luna Medium luna_verifier was unavailable. The root review above is not represented as
  independent verification, so full S4 route compliance cannot be claimed.

Safety boundaries:
- No password, bearer token, Cookie, CAS ticket/code, raw authenticated response, private WebView
  state, personal Room export, or identity-bearing screenshot was read, persisted, or committed.
- Test data is synthetic. No remote write, production mutation, destructive cleanup, or deployment
  occurred.

Residual risk / conditions:
- Final acceptance still requires the user to complete WebView login and a latest-build run proving
  preview cancellation, successful import, exact 28+3 persistence, replacement without duplication,
  and visible weekly unscheduled content.
- M4 must still prove offline cold restart, week/today/widget behavior, lint non-regression, distinct
  package/label, final ABI artifacts, privacy-safe screenshots/log scan, and hashes.
```
