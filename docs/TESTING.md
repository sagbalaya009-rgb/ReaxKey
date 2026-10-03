# Testing Strategy

## 1. Test pyramid

### Unit tests

- media classification,
- MIME compatibility,
- search normalization/ranking,
- deduplication identity,
- source-health transitions,
- typing state machine,
- language-pack parsing,
- URI permission decision logic.

### Room tests

- DAO queries,
- FTS/search behavior,
- migrations,
- stale-source reconciliation.

### Instrumentation tests

- onboarding state screens,
- permission denial/recovery,
- SAF persisted permission flow where testable,
- companion library filters,
- IME service basic creation.

### Testbed app

Build `:testbed`, a tiny separate debug application with fields that advertise controlled MIME support:

- accepts `image/gif`,
- accepts `image/webp`,
- accepts `image/png,image/jpeg`,
- accepts no rich content.

The testbed also has a multiline plain-text field for horizontal, vertical and
diagonal hold-space cursor checks without using a messaging app.

This lets tests verify the sender without depending entirely on WhatsApp/Telegram behavior.

## 2. Physical phone test matrix

The owner can connect an Android phone to Android Studio via USB debugging or install CI-built debug APK directly.

Mandatory manual checks:

- install/update without data loss,
- onboarding enable flow,
- input-method picker,
- alphabet typing,
- shift/caps,
- backspace repeat,
- enter/actions,
- language switching,
- dark/light mode,
- media-source connection,
- initial + incremental scan,
- GIF/animated preview,
- sticker send to testbed,
- target-app compatibility,
- source permission revoked then repaired,
- device reboot then IME still works,
- process killed then reopened,
- low-storage behavior,
- large library behavior,
- password field privacy mode.

## 3. Real-app compatibility record

Maintain `docs/COMPATIBILITY_MATRIX.md` once testing starts, with columns:

- App
- App version
- Android version/device
- Text entry
- PNG/JPEG direct
- GIF direct
- WebP direct
- Share fallback
- Date tested
- Notes

No undocumented blanket compatibility claims.

## 4. Performance tests

Use generated/sample libraries to evaluate:

- 100 items,
- 1,000 items,
- 10,000 items where feasible.

Measure:

- initial scan duration,
- incremental scan duration,
- search latency,
- media-panel open latency,
- scrolling smoothness,
- memory during animated media grid.

## 5. Build gates

Before merging a feature:

- `./gradlew test`
- relevant lint/static checks
- `./gradlew assembleDebug`
- instrumentation tests when affected
- manual phone validation for IME/storage/rich-content changes

## Windows and physical-device regression coverage (2026-10-02)

Desktop suite: 35 tests, 33 pass and two Windows skips. AndroidX FileProvider's
canonical path checks assume `/`; import integration tests run unchanged on Linux CI.
The production provider roots are not widened to accommodate a host-OS test limitation.

Device command: `adb shell am instrument -w com.reaxkey.keyboard.test/androidx.test.runner.AndroidJUnitRunner`.
Three tests passed on TECNO KL5, Android 14. Optional `-e retainFixtures true` leaves clearly named
QA sample imports for manual rich-content tests; default test runs delete their generated copies.
These tests use native image decoders, restricted provider roots, real imports and outgoing reads.
Actual cross-app grant delivery is additionally verified through the separate receiver app.

Generated SQLite benchmark output is printed by LargeLibraryTest at 100, 1000 and 10000 rows.
It does not measure real SAF scanning, animated-grid memory or phone frame time.

## Automatic reaction discovery contract — 2026-10-02 (supersedes manual-source-first UX)

The primary add/onboarding action is Find my reactions, never an immediate folder picker.
SourceDiscoveryEngine uses pluggable recognizers over supported metadata: authorized MediaStore
rows, persisted document-tree grants, and owned imports. Named-app attribution requires path-segment
or provider evidence, not installed-app checks or filenames. Results distinguish AVAILABLE,
AVAILABLE_REQUIRES_USER_GRANT, PARTIALLY_AVAILABLE, NOT_FOUND, PRIVATE_OR_INACCESSIBLE,
PERMISSION_REQUIRED and ERROR, with explicit confidence and observed counts.

Discovery describes candidates; shared MediaAccess ingestion scans them through WorkManager;
Room flows update app/IME search without restart. Shared-media buckets are mutually exclusive.
No raw filesystem crawling, production shell/ADB, protected app directories, contacts, chat or
notification access. Only public Android APIs. Existing access is checked before permission requests.
Partial grants remain partial. Manual folder selection is an advanced fallback; any initial picker
URI comes from actual provider evidence or a previous grant, never a guessed guaranteed app path.
The user must approve Android's directory grant. Picker positioning is a hint, never a promise.

Sources UI separates Found on your phone and Added by you. Add reactions orders automatic discovery,
individual import, personal-folder connection, then share help. Empty/lost states use human wording.
Shared imports preview once and accept bulk media together. App visuals use #0B0D10 dark background,
blue/violet accents and 20dp cards. Automated recognizer tests and physical discovery/grant lifecycle
checks are mandatory; record observed results without claiming universal app compatibility.

See docs/SOURCE_DISCOVERY.md for implementation and verification details.

## Typing upgrade verification — 2026-10-03

Robolectric `KeyboardTest` covers basic typing and media-panel return, boundary correction and immediate undo, suggestion prefix replacement and punctuation preservation, password/no-suggestions editors, tap versus hold, diagonal cursor movement/cancel, a missing `InputConnection`, logical-line fallback and surrogate-pair-safe horizontal fallback. The offline model test checks high-confidence typo cases, capitalization, unknown-word noncorrection, three-use learning and clearing pending learning. The app unit suite, debug APK, Android test APK and lint run locally; see `PROJECT_STATE.md` for the recorded result.

On TECNO KL5 / Android 14, the upgraded debug APK was installed over the previous
version with app data retained. In the testbed's plain-text fields, `teh` + Space
became `the `, immediate Backspace restored `teh`, and tapping the `the` suggestion
committed one replacement. A hold-space drag moved the insertion point left without
inserting a space. In the multiline field, hold-space drags moved up, down and
diagonally across a newline before a typed marker established the insertion point.
The populated Media panel opened and returned to typing. The separate receiver read
a real WebP sticker from the library through the new IME (`image/webp`, 4,822 bytes).
The installed instrumentation suite passed 4/4 tests.

Still check wrapped lines, search/password/email/URL fields, portrait/landscape and
dark/light appearance, plus actual Telegram and Snapchat sticker sending on the
upgraded build. Record app versions and observed outcomes. WhatsApp and other apps
remain unverified until tested. Automated tests and the local receiver do not
establish third-party compatibility or subjective touch feel.
