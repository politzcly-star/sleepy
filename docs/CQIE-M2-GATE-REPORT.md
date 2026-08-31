# CQIE M2 Gate Report

```text
Task: CQIE-WEBVIEW-OFFLINE-IMPORT / M2 production fetch and parser
Decision: CONDITIONAL GO

Baseline and scope:
- Input checkpoint: f3961d7 (completed M1).
- Frozen M2 scope: CQIE protocol registration, exact-origin WebView fetch, body-free error
  classification, dedicated JSON parser, scheduling edge cases, and focused/full JVM evidence.
- Expected route: Terra High terra_worker, then Luna Medium luna_verifier.
- Actual route: GPT-5 root-direct; named child roles and independent verifier tooling were not exposed.

Acceptance evidence:
- schools.json declares type=cqie and the selector displays CQIE 教务（直连）.
- FetchKind.CQIE is selected only for https://njw.cqie.edu.cn on the default TLS port.
- WebView JavaScript reads only cqu_edu_ACCESS_TOKEN, JSON-decodes it in page memory, and sends it
  only to the fixed relative GET /api/enrollment/timetable/student endpoint.
- Native receives a response body only for SUCCESS. Failure callbacks contain only a fixed kind and
  optional numeric HTTP status: wrong origin, session expired, login redirect/page, network, empty,
  malformed JSON, HTTP error, or bridge error.
- CQIE strict mode cancels TLS errors, blocks mixed content, strips URL query/fragment from logs, and
  suppresses all WebView console message bodies.
- CqieParser returns separate scheduled and unscheduled collections. It rejects blank/malformed JSON,
  missing data, and non-success business status.
- Continuous weeks, pure odd/even weeks, arbitrary discrete weeks, discrete period runs, no-time
  projects, missing schedules, and whole-week projects have focused tests.
- The synthetic detection fixture raises the fixture matrix to the required minimum and contains no
  credentials, session data, or personal timetable data.
- Authenticated emulator run: fixed endpoint body length 27873; dedicated parser produced 28 scheduled
  records from the observed 31-row response and reached the confirmation dialog. The three no-time
  source rows are represented by CqieParseResult.unscheduled and will become visible/persistent in M3.

Checks run (command, exit, result):
- Focused CQIE/protocol/registry/school/detection JVM suite: exit 0.
- Full testDebugUnitTest: first run 577/578 due to the pre-existing category allowlist omitting cqie;
  narrow test-only repair applied; final latest-code run 579/579, 0 skipped.
- assembleDebug: exit 0.
- adb install -r app-x86_64-debug.apk: success on Sleepy_CQIE_API_36_1_x86_64.
- Authenticated production fetch: SUCCESS; parser returned 28 scheduled records; confirmation UI shown.
- App-tag privacy scan: jwt_like=0, cas_query_values=0, cookie_values=0.
- AndroidRuntime fatal scan: 0.
- git diff --check: exit 0 (line-ending conversion warnings only).

Focused repair loops:
- Updated protocol test category allowlist after the first full-suite failure; affected and full suites
  then passed.
- Suppressed CQIE console message bodies after the whole-device log scan showed unrelated system
  cookie terminology; app-tag rescan remained zero for sensitive values.
- Direct adb launch of the non-exported import Activity was correctly denied by Android; validation
  continued through the normal in-app navigation without changing manifest exposure.

Checks deferred:
- Room migration, preview of unscheduled items, atomic replacement/rollback, and old-data preservation
  are M3 scope.
- Lint comparison, offline restart/week/today/widget flow, final ABI artifacts, and SHA-256 are M4 scope.

Independent read-only review:
- Required Luna Medium luna_verifier was unavailable; no same-root review is claimed as independent.
- Root read-only protocol/privacy review found and repaired console-body logging risk. No open M2 code
  finding remains.

Safety boundaries:
- No password, token, Cookie, raw authenticated response, private WebView state, or authenticated
  screenshot was persisted or committed.
- Response content existed only in WebView/native process memory long enough to parse and preview.
- No remote write, production mutation, or destructive database action occurred.

Residual risk:
- The endpoint schema is based on one observed account/term and may evolve.
- M2's legacy confirmation count includes only scheduled records; M3 must present and atomically store
  the three unscheduled/whole-week records before delivery.
- Independent verifier evidence remains unavailable, so this milestone cannot claim full route compliance.
```
