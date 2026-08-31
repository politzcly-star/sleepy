# Sleepy CQIE Development Plan

## Product Decision

Build on Sleepy v1.0.39 and add a CQIE-specific WebView import protocol. The timetable shown during normal use comes only from the last successful Room snapshot. Network access is user-triggered: open the refresh screen, sign in to CQIE in WebView, fetch the current timetable through the authenticated page, preview it, and commit it locally.

This is preferable to a standalone Excel-only app because it preserves weekly navigation, widgets, reminders, manual edits, and offline use while still allowing authoritative refreshes. Excel remains a recovery source, not the primary synchronization path.

## Fixed Decisions

- Upstream: `lingion/sleepy` tag `v1.0.39`.
- Branch: `feature/cqie-import`.
- Build memory: Gradle uses a 3 GiB maximum heap because the upstream default 512 MiB is insufficient for concurrent KSP and D8 work on this project.
- CQIE page: `https://njw.cqie.edu.cn/enroll/CourseStuSelectionList`.
- CQIE endpoint: `GET https://njw.cqie.edu.cn/api/enrollment/timetable/student`.
- Authentication: the user signs in inside WebView; same-origin JavaScript performs the request with the active browser session and existing bearer authorization behavior.
- Offline behavior: every normal timetable view reads Room and requires no login or network.
- Refresh behavior: only validated data can replace the prior local snapshot.
- Failure behavior: HTTP 401, redirect/login HTML, network error, empty data, malformed JSON, or zero valid scheduled rows must leave the prior snapshot untouched and show an actionable error.
- Packaging: use app label `Sleepy CQIE` and a distinct application ID such as `com.lingion.sleepy.cqie`, while keeping the Kotlin namespace unchanged, so the custom build can coexist with upstream Sleepy.
- Licensing: retain GPL-3.0 license and upstream attribution.

## Acceptance Contract

1. CQIE appears in the school selector and opens the correct login/timetable page.
2. After login, manual refresh calls the CQIE endpoint and produces a preview without exposing credentials or tokens to logs.
3. The parser handles ordinary rows, continuous week ranges, discrete week sets, odd/even patterns if present, and whole-week/no-period items without silently dropping them.
4. Import is transactional. Failed validation and failed persistence do not partially modify or erase the previous timetable.
5. After one successful import, airplane-mode launch, week switching, daily view, and widgets use the cached timetable.
6. A later successful refresh replaces the intended timetable group and updates the visible weekly schedule.
7. Unit tests use sanitized fixtures and cover success, 401/session expiry, empty data, malformed data, discrete weeks, and whole-week entries.
8. CQIE-focused tests and `assembleDebug` pass; the full test/lint results introduce no regressions beyond the recorded v1.0.39 baseline; the x86_64 APK installs and launches on `Sleepy_CQIE_API_36_1_x86_64`.
9. An arm64 debug APK is produced for physical-phone installation.

## Milestones

### M1: Baseline and CQIE Fixture

- Confirm the unmodified branch builds and launches on the emulator.
- Add CQIE as a selectable school and open the real page in WebView.
- The user completes school login when prompted.
- Capture only the endpoint response needed for development, immediately remove names, student identifiers, tokens, and other personal values, then commit only the sanitized fixture.
- Freeze the observed response schema before writing production parsing logic.

Exit evidence: baseline test/build result, emulator launch evidence, documented sanitized schema, and fixture privacy review.

### M2: Protocol and Parser

- Add a dedicated `cqie` protocol modeled on the existing same-origin Wisedu fetch path.
- Add explicit response/error classification rather than routing CQIE JSON through unrelated HTML parsers.
- Parse course, teacher, location, weekday, periods, and week expressions into Sleepy's import model.
- Define a visible representation for whole-week/no-period entries; do not invent periods silently.

Exit evidence: focused parser/protocol tests for all fixture variants.

### M3: Offline-Safe Refresh

- Connect WebView fetch results to the existing preview/import flow.
- Reject 401/login redirects, empty payloads, malformed payloads, and zero-valid-row results before any Room write.
- Use the repository/DAO transaction boundary to replace the selected timetable group atomically.
- Confirm cancellation or process failure before confirmation leaves cached data unchanged.

Exit evidence: ViewModel/repository tests plus a before/after database behavior check.

### M4: CQIE Packaging and Device UX

- Apply the distinct application ID, app label, and CQIE-facing copy with minimal unrelated branding changes.
- Verify week navigation, offline relaunch, refresh, and error recovery on the local emulator.
- Build x86_64 and arm64 debug APKs and record SHA-256 hashes.

Exit evidence: passing gates, installed emulator build, launch screenshot, APK paths, sizes, and hashes.

## Recorded Upstream Baseline

Measured on the untouched v1.0.39 application sources on 2026-08-31:

- `assembleDebug`: passes and produces arm64-v8a, armeabi-v7a, and x86_64 APKs.
- `testDebugUnitTest`: 560 of 561 tests pass. `JwProtocolFixtureMatrixTest` counts 9 fingerprint fixtures while its assertion requires at least 10; adding the CQIE detection fixture is expected to satisfy the intended matrix count.
- `lintDebug`: reports 15 existing errors and 361 warnings. The errors are one missing Android namespace prefix in `widget_scroll_clip.xml`, 13 existing missing-translation findings, and one embedded BOM in `ScheduleParser.kt`.

CQIE work must not hide these results, create a blanket lint baseline, or introduce additional failures. Fixing unrelated upstream lint findings is optional and requires a clearly scoped change.

## Test Matrix

| Scenario | Expected result |
| --- | --- |
| First launch without import | Empty state; no crash and no forced login |
| Valid authenticated response | Preview shows all parsed courses; confirmation persists them |
| HTTP 401 or login HTML | Prompt to sign in again; old timetable remains |
| Network unavailable | Refresh fails clearly; normal timetable remains usable |
| Empty or malformed response | Import blocked; old timetable remains |
| Continuous weeks | Course appears in every intended week |
| Discrete/odd/even weeks | Course appears only in the listed weeks |
| Whole-week/no-period item | Preserved in the agreed visible form |
| App killed after import | Cached timetable survives relaunch |
| Later successful refresh | Selected timetable is atomically updated |

## Security and Privacy Rules

- Never request or store the CQIE password in native app storage.
- Never log request headers, bearer tokens, cookies, full WebView state, or raw personal responses.
- Do not commit emulator data, screenshots containing identity details, or unsanitized fixtures.
- Keep JavaScript injection restricted to the CQIE origin and the fixed endpoint.
- Do not add a remote server, analytics, or background credential refresh.

## Out of Scope

- Automatic background login or credential storage.
- A generic adapter for every Unicorn/优霓 deployment.
- Publishing to an app store or operating a synchronization server.
- Replacing Sleepy's existing import systems or unrelated UI redesign.

## Delivery Artifacts

- Source changes and focused tests on `feature/cqie-import`.
- Sanitized CQIE fixture and schema notes.
- Verification report with exact commands, comparison against the recorded upstream baseline, and residual risks.
- `app-x86_64-debug.apk` for emulator evidence.
- `app-arm64-v8a-debug.apk` for the user's Android phone.
