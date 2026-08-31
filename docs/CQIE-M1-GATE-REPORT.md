# CQIE M1 Gate Report

```text
Task: CQIE-WEBVIEW-OFFLINE-IMPORT / M1 baseline and CQIE fixture
Decision: CONDITIONAL GO

Baseline and scope:
- Baseline authority: Sleepy v1.0.39 at fa96b3c, preparation commit 114ee74.
- Frozen scope: CQIE school selection, real WebView login/page reachability, authenticated schema capture, sanitized fixture, and privacy-safe evidence.
- Expected route: Terra High terra_worker, then Luna Medium luna_verifier.
- Actual route: GPT-5 root-direct; effort and named child roles were not exposed by the platform.

Acceptance evidence:
- CQIE appears as 重庆工程学院 and opens the frozen /enroll/CourseStuSelectionList URL.
- User-owned login completed in WebView; app data replacement preserved the authenticated session.
- Fixed same-origin GET returned HTTP 200 and a 31-row data array after matching the public frontend's JSON-decoded Bearer behavior.
- Raw API data remained in WebView memory. Only schema/type and scheduling-field projections left the page.
- Synthetic fixture covers continuous weeks, odd/even discrete weeks, no-time rows, and a synthetic wholeWeekOccupy boundary.
- x86_64 APK installed and cold-launched; initial cold launch was 2356 ms.
- Screenshots are privacy-safe empty/public states; AndroidRuntime fatal check was clear.

Checks run (command, exit, result):
- Gradle focused M1 tests: exit 0; 30 tests across CQIE entry, fixture privacy, school lifecycle/consistency, URL/origin/probe/TLS behavior.
- Gradle assembleDebug: exit 0.
- adb install -r app-x86_64-debug.apk: success.
- App-internal CQIE probe: ok=true, status=200, kind=SCHEMA, __count=31.
- Privacy scan: jwt_like=0, cas_code_query=0, cookie_value=0.
- git diff --check: exit 0 (line-ending conversion warnings only).

Checks skipped (reason and compensation):
- Full unit/lint suite deferred to final cross-milestone gates; M1 used focused tests plus assembleDebug.
- Independent luna_verifier review unavailable because no named child/subagent tool or actual identity evidence was exposed.

Focused repair loops:
- Removed query/fragment from all WebView URL logs after detecting a query-bearing authentication callback in existing logging; device log was immediately cleared.
- Two expected HTTP 401 probes identified the frontend token encoding/header behavior without persisting values; final in-app probe returned HTTP 200.
- One Gradle invocation combined --tests with assembleDebug incorrectly; split commands then passed.
- CQIE WebView now cancels SSL errors and disables HTTPS mixed content; unrelated school behavior remains unchanged.

Independent read-only review:
- Required Luna Medium luna_verifier was unavailable and no same-root review is claimed as independent.
- Root read-only diff/privacy review found and repaired URL logging and CQIE TLS findings; no open M1 code finding remains.

Safety boundaries:
- No password, access/refresh token, Cookie, raw response, private WebView state, or authenticated screenshot was persisted or committed.
- No production/database/server mutation occurred.
- Remote writes were not performed in M1.

Unsupported claims avoided:
- This report does not claim route compliance or independent verification.
- The synthetic whole-week row is a boundary fixture, not proof that the observed response contained that flag.

Known limits and residual risk:
- CQIE schema is based on one authenticated response and may evolve.
- wholeWeekOccupy=true was not observed; three notArrangeTimeAndRoom=true rows were observed.
- M2 must replace the M1 schema probe/log callback with production response classification and parsing before release.
```
