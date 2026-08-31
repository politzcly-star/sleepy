# CQIE Goal Contract

```text
Goal ID: CQIE-WEBVIEW-OFFLINE-IMPORT
Objective: Deliver a CQIE-specific WebView refresh flow that safely stores the last successful timetable in Room for offline weekly viewing, plus emulator-verified x86_64 and phone-ready arm64 debug APKs.
Non-goals: background credential login; server-side credential storage; generic Unicorn support; app-store publication; unrelated refactors.
S-level / route: S4, one milestone at a time using the project AGENTS.md route.
Acceptance checks:
- CQIE selection, login, authenticated endpoint fetch, preview, and successful import work.
- Week rules and whole-week entries are covered by sanitized fixtures and focused tests.
- 401, redirect, network, empty, malformed, and persistence failures retain the old Room snapshot.
- Offline relaunch and week navigation work after one successful import.
- CQIE-focused tests and assembleDebug pass; full test/lint results add no failures beyond the recorded v1.0.39 baseline; emulator installation and launch pass.
- x86_64 and arm64 debug APKs plus hashes are reported.
Automatic repair budget: 3 focused attempts per unchanged blocker

## Allowed Files / Areas
<!-- harness:allowed-paths:start -->
- app/build.gradle.kts
- gradle.properties
- app/src/main/
- app/src/test/
- docs/
- README.md
- .codex/harness-state/
<!-- harness:allowed-paths:end -->

## Forbidden Actions
<!-- harness:forbidden-actions:start -->
- secrets, credentials, tokens, cookies, unsanitized personal API responses, or private browser state
- destructive actions, production database writes or migrations, deployment/restart, paid actions
- changes to server/, Gradle wrapper, upstream history, tags, or unrelated parsers without fresh Human authorization
<!-- harness:forbidden-actions:end -->

## Stop Conditions
<!-- harness:stop-conditions:start -->
- CQIE response schema materially contradicts the frozen product decisions
- a required action or file falls outside the allowed boundary
- fresh authority is required for protected browser, credential, or remote actions
- verification cannot continue safely
- the same blocker remains after three focused repairs
<!-- harness:stop-conditions:end -->
```

The Human authorized repository preparation and implementation through debug APK delivery on 2026-08-31. Authentication entry remains a user action when the CQIE login screen is reached.
