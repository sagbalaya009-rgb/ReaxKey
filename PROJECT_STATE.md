# Project State

## Checkpoint: build foundation and initial IME/onboarding implementation

- Date: 2026-10-01
- Branch: build/full-reaxkey
- Base documentation commit: 48ada57
- Current checkpoint commit: see `git log -1` (this file is committed with the checkpoint).
- Implemented: Gradle wrapper, five functional modules, Compose setup screen, enable/select recheck, ephemeral test field, basic View IME, language-pack parser and shift state engine, four pack assets, CI workflow.
- Executed: `gradlew.bat test lint assembleDebug --max-workers=2` passed.
- Unit tests: 10 passed (setup state, shift/caps, locale casing, pack parsing).
- Lint: no errors; warnings for pinned dependency upgrades, app icon and Android 12 backup rules. Address icon/backup in next change.
- APK: app/build/outputs/apk/debug/app-debug.apk.
- CI: configured; remote run not yet verified.
- Physical/emulator validation: not run. No compatibility claims.
- Completion: NOT COMPLETE. Stage 0 local build gate passed; Stage 1 physical gate pending.
- Next objective: integrate full typing engine and languages, then Room/DataStore and authorized media ingestion. Continue all remaining construction stages; do not stop for the phone until computer-side work is ready.
- Pending: media/data/search/collections/settings/testbed/release pipeline, substantial device tests and full definition-of-done gates.

The synced parent sources directory is read-only and is not part of this checkout.

## Typing implementation checkpoint
- Integrated shift/caps, numbers/symbols, editor actions, numeric/email/URL fields, secure-field behavior, held-delete, language switching/alternatives, emoji and dark mode.
- Executed test/lint/assembleDebug again: passed, 10 tests; physical behavior remains unverified.
- Added launcher icon and explicit backup exclusions. Remaining lint notices are dependency updates and an accessibility touch-listener advisory requiring device validation.
- Next: Room/DataStore, media ingestion, and testbed.


## Data/search checkpoint
- Room v2 schema, explicit 1-to-2 migration, repository, source health, favorites/usage, collections, removal tombstones, FTS search and DataStore preferences implemented.
- Full test/lint/APK build passed. Follow-up model/data/search test run passed including the migration preservation test (21 tests total across these modules).
- Search queries are parameterized and capped at 200 results. No typed-text columns exist.
- Data/media UI integration is next; current APK still displays keyboard setup only.

