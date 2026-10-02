# Project State

## Current checkpoint — automatic discovery tested with phone media access

Branch: build/full-reaxkey. Draft PR: https://github.com/sagbalaya009-rgb/ReaxKey/pull/1
Pushed discovery checkpoint: ad895e7. Prior GitHub CI run 36974128678 passed on Linux,
including both FileProvider import tests. Remote status for ad895e7 is pending verification.

## Implemented in this stage

- SourceDiscoveryEngine and recognizer contract; evidence-based exclusive shared-media groups,
  persisted-tree validation, imported-source discovery, partial/denied/error states and confidence.
- One MIME-filtered shared-media metadata query with progressive results. No filename attribution,
  private-storage probing or guessed guaranteed WhatsApp folder.
- Discovery-first Add reactions/onboarding; grouped source home; permission explanation; known
  tree URI hints; zero-result and success states; blue/violet CTA, dark palette, 20dp cards.
- Share previews, one bulk confirmation and optional collection. Explicit reimport restores
  removed media while preserving favorites; background rescans preserve removal tombstones.
- Exact stable URI deduplication across source records and metadata preservation on source handover.
- Source health reconciliation for lost tree grants and revoked shared-media access.
- Shared image groups use medium reaction confidence and are unchecked by default. Discovery
  describes them as possible reaction images, including ordinary photos; app-owned and granted
  exact sources remain preselected.

## Executed evidence

- Latest discovery debug build, unit tests and lint passed with reduced Gradle concurrency after
  one host out-of-memory daemon crash. Final suite has 55 tests: 53 passed, two Windows-only
  FileProvider skips; revoked-permission and broad-media-selection regressions passed.
- Unsigned discovery release AAB built successfully (902 tasks). Final health adjustments require
  another CI release build; no production signing credentials or publication.
- Latest discovery build installed on TECNO KL5 / Android 14 (API 34). Onboarding, source home and
  discovery rendered and responded. Before permission, four app-owned QA imports were found;
  Phone media offered access choices; no WhatsApp/Telegram source was fabricated.
- The owner granted image access in Android. The phone then exposed 1,415 WhatsApp-attributed
  image records and two Downloads image records. These are accessible image counts, not verified
  reaction counts. Both shared-media groups were unchecked by default; only the four imported
  QA reactions were checked. No personal media was bulk-added.
- Previous physical tests: three native-provider/import tests passed. Keyboard typed hi; receiver
  read PNG 320, JPEG 816, GIF 42 and WebP 586 bytes through temporary URI grants. Unsupported-field
  explanation/Share action observed. See docs/COMPATIBILITY_MATRIX.md for limits.
- No Internet, contacts, accessibility or all-files permission in the app.

## Next exact actions

Commit and verify Linux CI. With explicit user source selection, add chosen sources, verify
ingested counts and immediate IME availability. If a saved source needs access, use its known URI
hint and let the owner approve the Android directory. Verify persistence after app/IME recreation.

## Remaining gates and limits

Third-party delivery, animation fidelity, sustained phone performance, full source disappearance/
process-death coverage and Play release gates remain incomplete. Cross-provider byte-identical
files with unrelated URIs are not globally hashed during every scan. The earlier Exact UI Redesign
specification referenced in the new attachment was not present; supplied visual requirements were
applied to current screens. Do not claim full product completion.

Parent sources remain read-only. Artifacts: app/build/outputs/apk/debug/app-debug.apk,
testbed/build/outputs/apk/debug/testbed-debug.apk, app/build/outputs/bundle/release/app-release.aab.
The phone holds the latest successful build-discovery-device-fix-retry.log debug APK.

