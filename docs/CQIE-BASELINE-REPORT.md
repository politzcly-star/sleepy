# CQIE Baseline Verification Report

Date: 2026-08-31

Source: `lingion/sleepy` tag `v1.0.39`

Commit: `fa96b3cdbf74158d925dcc0791abca2ec7ceb780`

Branch: `feature/cqie-import`

## Environment

- Windows 10.0.26200
- Java 21.0.8; project JVM target 17
- Gradle 9.3.1 through `gradle/wrapper/gradle-wrapper.jar`
- Android SDK platform 37.0 exposed to AGP 9.1 as the expected `android-37` directory junction
- Build Tools 37.0.0; Platform Tools 37.0.1
- AVD `Sleepy_CQIE_API_36_1_x86_64`, Android 36.1 Google APIs, Pixel 6 profile
- Gradle maximum heap 3 GiB; the implicit 512 MiB default failed in KSP and D8

## Commands and Results

### Workflow Harness

Command: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\harness-self-test.ps1` from `D:\个人工作流-v2`

Result: pass. Hook, routing, scope, goal-state, project-profile, migration, and health fixtures completed.

### Unit Tests

Command: `java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain testDebugUnitTest`

Result: baseline failure, 560 passed and 1 failed. `JwProtocolFixtureMatrixTest` asserted at least 10 fingerprint fixtures but v1.0.39 counted 9. Its source comment describes 9 positive samples plus one CAS sample; a CQIE protocol detection fixture should raise the matrix to its intended minimum. Do not conceal this baseline when comparing later runs.

### Lint

Command: `java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain lintDebug`

Result: baseline failure, 15 errors and 361 warnings. Existing errors comprise one missing Android namespace prefix in `widget_scroll_clip.xml`, 13 missing-translation findings for existing JW diagnostic/fetch strings, and one embedded BOM in `ScheduleParser.kt`. CQIE work must add no new findings; unrelated cleanup is not part of the goal.

### Debug APK Build

Command: `java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain assembleDebug`

Result: pass.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| `app-x86_64-debug.apk` | 20,123,323 | `E9B0B53D89478B6356F323715DF310F0FB81D7F26986557EBA1035ADE9C79C92` |
| `app-arm64-v8a-debug.apk` | 20,124,217 | `0BE61A73D91A22A6CE31357A31723EA30125DACF3DFE2A1C0E4AEB4970B2A7E5` |

### Emulator Install and Launch

- `adb install -r app-x86_64-debug.apk`: pass.
- `adb shell am start -W -n com.lingion.sleepy/.MainActivity`: pass, cold launch in 5,376 ms.
- `dumpsys activity`: `com.lingion.sleepy/.MainActivity` is top resumed and visible.
- AndroidRuntime fatal log check: no entries after launch.
- Screenshot inspection: nonblank empty-timetable screen rendered at 1080 x 2400 with navigation visible and no overlaps.

## Known Environmental Notes

- The pre-existing `Medium_Phone_API_36.1` AVD uses an ARM image and cannot run under the Windows QEMU2 emulator. It was preserved unchanged.
- `Sleepy_CQIE_API_36_1_x86_64` was created specifically for this project and is the supported local test device.
- AGP 9.1 warns that it was tested only through compile SDK 36.1. The upstream project nevertheless targets SDK 37 and builds successfully with the installed final Android 37 platform.

## Residual Risk

The CQIE endpoint response schema has not yet been captured. Production parsing must wait for a user-authenticated WebView session, and only a sanitized fixture may enter Git history.
