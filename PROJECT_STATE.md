# Project State

## Checkpoint — physical Android validation in progress

Branch: build/full-reaxkey. Draft PR: https://github.com/sagbalaya009-rgb/ReaxKey/pull/1

All 11 modules are implemented. Companion Library/Collections/Sources/Settings, imports,
SAF/MediaStore scanning, Room/FTS, multilingual typing and rich-content delivery are wired.
No Internet permission in either merged debug or release manifest.

## Executed verification (2026-10-02)

- Latest debug gate: test lint assembleDebug :app:assembleDebugAndroidTest passed, 556 tasks.
- 35 desktop tests: 33 passed, 2 explicitly skipped on Windows because AndroidX FileProvider
  assumes Unix separators. Those import tests remain enabled in Linux CI.
- Three instrumentation tests passed on TECNO KL5 / Android 14 (API 34): real PNG/JPEG/GIF/WebP
  decoder imports, deduplication/outgoing reads, private-root refusal. QA sample media retained
  intentionally for interactive receiver testing.
- Found and fixed a real bounds-only bitmap decode bug that rejected valid imports.
- On-device setup UI and keyboard appearance inspected. Typed "hi" through ReaxKey.
- Actual keyboard-to-testbed PNG (320 bytes) and GIF (42 bytes) grants read successfully.
- Dark media-panel contrast fixed; ordinary typing no longer recreates unchanged key views.
- Source disconnect/late-scan race and imported-source counts have regression coverage.
- Unsigned release AAB built successfully after import correction (902 tasks); subsequent
  keyboard view changes need final release rebuild. No signing secret configured.
- Prior pushed integrated commit 5a0968a passed GitHub CI run 36937581714. Current changes
  need a new CI run, particularly the Linux-only FileProvider tests.
- Generated desktop SQLite measurements: 100/1000/10000 items; 10k index 7586 ms,
  search 27 ms, rescan 9672 ms. These are NOT phone media-scan or rendering benchmarks.

## Next steps / incomplete gates

Complete testbed JPEG/WebP/fallback and internal-search checks, then SAF permission lifecycle,
rotation/process recreation, theme/language checks and device performance. Record real target-app
compatibility only after observed tests. No third-party messaging has been sent.
Refresh release bundle and latest CI, update compatibility matrix, commit and push checkpoints.
Physical coverage remains partial; do not call the product complete or production-ready.
Play signing/account/listing/policy and real purchase/update validation remain owner/platform gates.

Parent sources remain read-only and untouched. Current APK is app/build/outputs/apk/debug/app-debug.apk;
receiver APK is testbed/build/outputs/apk/debug/testbed-debug.apk.

CI follow-up: Linux valid import passed; invalid-image test exposed AndroidX's cached test-directory root. Test setup now reattaches the provider with actual manifest metadata for each Robolectric sandbox. Local app tests pass again; Linux rerun pending.
