# CQIE M4 Gate Report

```text
Task: CQIE-WEBVIEW-OFFLINE-IMPORT / M4 packaging and device UX
Decision: CONDITIONAL GO

Baseline and scope:
- Input checkpoint: 092f208 (completed M3).
- Frozen M4 scope: distinct CQIE package and label, final unit/build/lint gates, x86_64 device
  installation and UX checks, offline restart, ABI artifacts and hashes, authenticated cancel/import/
  replacement checks, privacy review, commit, and authorized push.
- The root route remained the platform-selected model. Required Terra High terra_worker and Luna
  Medium luna_verifier roles were unavailable, so no delegated implementation or independent review
  is claimed.
- CodeGraph was unavailable; review used rg, source-tree and caller inspection, project-native tests,
  Room count-only queries, APK metadata, adb runtime state, and manual emulator UX checks.

Implemented packaging:
- applicationId is com.lingion.sleepy.cqie; Kotlin namespace and launch class remain unchanged.
- All packaged locale labels and the About heading identify the app as Sleepy CQIE.
- GPL-3.0 LICENSE and upstream README attribution remain unchanged.
- Robolectric is 4.16.1, which supports the installed Android API used by the committed Room tests.
- CQIE imports use the authoritative 12-period school schedule supplied by the user. This is isolated
  from Sleepy's global defaults and does not change another school's import behavior.

Checks run (command, exit, result):
- testDebugUnitTest plus assembleDebug: exit 0; 588 tests, 0 failures/errors/skips; both ABI APKs
  produced. Two focused tests assert all 12 CQIE periods and JSON round-trip behavior.
- lintDebug: expected exit 1; exactly 15 errors and 361 warnings, identical to the recorded upstream
  baseline. No lint baseline was added. First inherited finding remains widget_scroll_clip.xml:5.
- aapt dump badging: package com.lingion.sleepy.cqie, version 1.0.39-debug, label Sleepy CQIE in
  default/en/es/ja/zh-CN/zh-TW resources, launch class com.lingion.sleepy.MainActivity, x86_64 code.
- adb install -r of app-x86_64-debug.apk: success on Sleepy_CQIE_API_36_1_x86_64.
- Final package cold launch: success; com.lingion.sleepy.cqie/com.lingion.sleepy.MainActivity became
  top-resumed and no AndroidRuntime fatal was present.
- git diff --check: exit 0 apart from Git LF-to-CRLF worktree notices.
- Allowed-path audit from 092f208: all M4 paths are within the frozen contract.
- GPL-3.0 LICENSE and README are byte-for-byte unchanged from upstream v1.0.39; no lint baseline exists.

Device UX evidence using synthetic-only Room data:
- First launch without a table renders a nonblank, usable empty state.
- Table ID 1 contains one scheduled SyntheticCourse and one SyntheticWholeWeek item for weeks 1-3.
- Week 2 shows both records; week 4 keeps the scheduled row while omitting the out-of-week whole-week
  item. The today view shows only the scheduled row.
- The whole-week item is visible in a dedicated band and never receives an invented period.
- A real launcher WeekGrid widget was pinned and rendered the cached scheduled row.
- Airplane mode was enabled with no active network and ping reporting network unreachable. After a
  force-stop/cold launch, both applicable cached Room records remained visible; airplane mode was then
  restored to disabled.
- Privacy-safe 1080x2400 screenshots are stored under docs/evidence/cqie-m4-*.png. Each was visually
  reviewed and contains only empty or explicitly synthetic data.

Authenticated latest-build device evidence:
- Starting Room snapshot: table ID 1 with 1 scheduled and 1 unscheduled synthetic row.
- Same-origin fetch from https://njw.cqie.edu.cn/enroll/CourseStuSelectionList completed with body
  length 27873. The dedicated parser returned 28 scheduled and 3 unscheduled records.
- The preview exposed all 31 records to the user. System Back cancelled it; count-only Room queries
  remained 1/1, proving preview cancellation made no database change.
- A second fetch and confirmation logged tableId=1 records=31. Room became 28/3 on table ID 1.
- A third fetch and confirmation again logged tableId=1 records=31; Room stayed 28/3, proving replacement
  rather than append/duplication.
- Week navigation changed from week 1 to week 2. Week 2 displayed the dedicated unscheduled heading;
  only privacy-safe header crops were inspected and no personal course names were saved. Today opened
  without an AndroidRuntime fatal.
- After enabling airplane mode, the active default network was none and ping reported network
  unreachable. A force-stop/cold launch completed in 3039 ms, Room stayed 28/3, and offline week 2
  still displayed the unscheduled heading. Airplane mode was restored to disabled.
- On the final APK, an online authenticated page was opened, airplane mode was enabled, and manual
  refresh returned kind=NETWORK status=0. Room remained 28/3 with its existing time configuration.
- Final cold launch completed in 2709 ms with MainActivity top-resumed and no AndroidRuntime fatal.

Authoritative CQIE schedule evidence:
- The pre-repair table exposed 8 nodes and the inherited starts 08:00, 08:55, 10:00, and 10:55.
- The repaired live refresh persisted nodesPerDay=12 and exactly these periods:
  1 08:30-09:15; 2 09:25-10:10; 3 10:30-11:15; 4 11:25-12:10;
  5 14:00-14:45; 6 14:55-15:40; 7 16:00-16:45; 8 16:55-17:40;
  9 19:00-19:45; 10 19:55-20:40; 11 20:50-21:35; 12 21:45-22:30.
- The corrected import retained table ID 1 and counts 28/3.

Final artifacts:
- app/build/outputs/apk/debug/app-x86_64-debug.apk
  20,689,897 bytes
  SHA-256 932F341B59EF849A996D981A5D9456B886509C24258B4CDD105044AF009CA194
- app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
  20,690,791 bytes
  SHA-256 7F9B703395702FCAB374DA554E938BCB91EC4021EC50545473CC857DB8E2720F

Privacy and safety review:
- All M4 screenshots contain synthetic or empty state only; authenticated preview screenshots are
  intentionally forbidden.
- CQIE text scan found no credential, Cookie, CAS value, or concrete bearer value. Expected matches
  are limited to the in-WebView accessToken variable, tests asserting it is not bridged, and contract
  prose describing same-origin authorization.
- No raw authenticated response, WebView state, personal Room export, emulator data, login input, or
  secret-bearing log is persisted or committed.
- Room runtime checks query only target table IDs, aggregate scheduled/unscheduled counts, unscheduled
  week/kind groupings, and non-personal period configuration.
- No production mutation, destructive cleanup, deployment, tag/history rewrite, or upstream push was
  performed.
- Final app-process log scan counted jwt_like=0, bearer_value=0, cookie_value=0, cas_query_value=0,
  and androidruntime_fatal=0.

Independent read-only review:
- Required Luna Medium luna_verifier was unavailable. Root review is not represented as independent
  verification, so full S4 route compliance cannot be claimed.

Residual risk:
- The authenticated schema is based on one account/term and may evolve.
- The required independent Luna verifier remained unavailable. Product acceptance is complete, but
  this report does not claim full S4 route compliance or independent verification.
```
