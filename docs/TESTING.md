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
