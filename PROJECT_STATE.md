# Project State

## Current checkpoint — integrated application, platform validation pending

- Branch: build/full-reaxkey
- Last previously pushed checkpoint: 7c275ed (Room/search).
- Current checkpoint SHA: `git log -1` after this file's commit.
- Implemented: all 11 documented modules; companion Library/Collections/Sources/Settings; explicit imports/share receiver; SAF/MediaStore scanners; Room/FTS; favorites/collections/tags; DataStore; multilingual typing; internal media search; rich-content sender and explicit Share fallback; separate testbed.
- Executed: `gradlew.bat test lint assembleDebug --max-workers=2` passed after integration (519 tasks).
- Tests: 21 unit/Robolectric tests passed, including Room migration and repository/FTS tests. Further integration regression coverage is being added next.
- APK: app/build/outputs/apk/debug/app-debug.apk.
- Testbed APK: testbed/build/outputs/apk/debug/testbed-debug.apk.
- Lint: zero errors; warnings remain (dependency updates, touch-click advisory, testbed icon/backup configuration, deprecated API/KTX suggestions). Review before release.
- CI: GitHub run 36936083405 passed for 7c275ed and uploaded an APK. New integrated checkpoint must be verified remotely.
- ADB: operational; no device connected when checked.
- Physical/emulator/third-party compatibility: NOT TESTED. Product is NOT COMPLETE under docs/DEFINITION_OF_DONE.md.

## Next exact objective

Add and run import/deletion and rich-content/IME regression tests, generated 100/1000/10000-item search measurements, finalize external-signing release/AAB configuration, audit merged permissions, review remaining lint, and verify CI on the latest checkpoint. Then request the owner's phone for physical Android validation; do not claim compatibility before observation.

## Known limitations to resolve or verify

- No emulator/physical app UI inspection yet; IME lifecycle, scrolling, field actions, SAF grants, animated rendering and temporary URI grants need platform QA.
- Index removal uses tombstones so rescans cannot silently restore removed media.
- Background scan failure marks the source unavailable; reconnect/rescan is exposed.
- Theme/language/haptic preferences are wired to app and IME.
- Large-library search has bounded SQL/FTS queries; measurements are still pending.
- Imports are capped at 50 MB each; animated WebP detection reads its format header. No vision model.
- Upload signing key/Play Console work remains external; no production publication authorized.

The parent synced sources directory is read-only and untouched. A draft PR tracks the branch: https://github.com/sagbalaya009-rgb/ReaxKey/pull/1
