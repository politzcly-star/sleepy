# Project Profile

```text
Project: Sleepy-CQIE
Repository Root: D:\Sleepy-CQIE
Primary Commands:
- install: %LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-x86_64-debug.apk
- test: java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain testDebugUnitTest
- build: java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain assembleDebug
- lint/typecheck: java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain lintDebug
- run: start Sleepy_CQIE_API_36_1_x86_64, install the x86_64 debug APK, then launch com.lingion.sleepy/.MainActivity (until the CQIE applicationId milestone changes it)

Main Areas:
- app/src/main/assets/schools.json
- app/src/main/java/com/lingion/sleepy/data/jw
- app/src/main/java/com/lingion/sleepy/data/dao
- app/src/main/java/com/lingion/sleepy/data/repository
- app/src/main/java/com/lingion/sleepy/ui/screen/imports
- app/src/test/java/com/lingion/sleepy/data/jw
- app/src/test/resources/jw_fixtures
Forbidden Areas:
- server/ unless the Human explicitly expands scope
- upstream history, release tags, and unrelated parsers
- raw credentials, access tokens, cookies, or unsanitized CQIE responses
Generated / Runtime Paths:
- .gradle/
- build/
- app/build/
- .codex/harness-state/

Daily Root Route: gpt-5.6-luna xhigh for S0-S2 when the Human/platform selects it
Actual Identity Evidence: expected + actual model, effort, role, topology when exposed
S1 QA Trigger: first-check failure, narrow repair, or boundary only
S2 QA: luna_qa (gpt-5.6-luna medium, read-only, same-family; not independent)
S3/S4 Implementation: terra_worker (gpt-5.6-terra high)
S3/S4 Review: luna_verifier (gpt-5.6-luna medium, read-only, independent)
Topology: max_threads=2, max_depth=1, one active child, no parallel writers

CodeGraph: unavailable
Fallback if unavailable: rg + source tree + parser registry/test entry points + manual caller/DAO impact review

Database Boundary: local Room only; replace cached courses only after a non-empty CQIE payload passes schema and course validation, and keep the previous snapshot on 401, empty, malformed, or network failure
Deployment / Server Boundary: no production server or store deployment; deliver locally built debug APKs for emulator and USB installation
Secrets / Privacy Boundary: login remains inside an on-device WebView; never log, persist, commit, or include passwords, bearer tokens, cookies, or an unsanitized personal response in fixtures
Browser Profile Boundary: use the signed-in Chrome profile only for the user-authorized GitHub fork; do not inspect cookies, saved passwords, local storage, or unrelated history

Server Alias (read-only, if configured):
- none
```
