# Project State

## Current checkpoint — automatic discovery implemented; device permission pending

Branch: build/full-reaxkey. Draft PR: https://github.com/sagbalaya009-rgb/ReaxKey/pull/1
Prior pushed checkpoint: 805ffa3; GitHub CI run 36974128678 passed on Linux, including both
FileProvider import tests. Current discovery changes are being verified for a new commit.

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

## Executed evidence

- Discovery debug build, unit tests and lint passed. Final suite has 54 tests: 52 passed,
  two Windows-only FileProvider skips; revoked-permission regression passed.
- Unsigned discovery release AAB built successfully (902 tasks). Final health adjustments require
  another CI release build; no production signing credentials or publication.
- New discovery build installed on TECNO KL5 / Android 14 (API 34). Onboarding, source home and
  discovery rendered and responded. Before permission, four app-owned QA imports were found;
  Phone media offered access choices; no WhatsApp/Telegram source was fabricated.
- Android runtime dialog is OPEN awaiting the owner's choice: Allow limited access / Allow all /
  Don't allow. Do not automatically accept this choice or any directory approval.
- Previous physical tests: three native-provider/import tests passed. Keyboard typed hi; receiver
  read PNG 320, JPEG 816, GIF 42 and WebP 586 bytes through temporary URI grants. Unsupported-field
  explanation/Share action observed. See docs/COMPATIBILITY_MATRIX.md for limits.
- No Internet, contacts, accessibility or all-files permission in the app.

## Next exact actions

Latest tests/lint/debug passed (556 tasks); commit and verify Linux CI. After owner chooses
phone access, rerun/observe discovery, add selected sources, verify real counts and immediate IME
availability. If a saved source needs access, use its known URI hint and let the owner approve the
Android directory. Verify persistence after app/IME recreation and record WhatsApp facts.

## Remaining gates and limits

Third-party delivery, animation fidelity, sustained phone performance, full source disappearance/
process-death coverage and Play release gates remain incomplete. Cross-provider byte-identical
files with unrelated URIs are not globally hashed during every scan. The earlier Exact UI Redesign
specification referenced in the new attachment was not present; supplied visual requirements were
applied to current screens. Do not claim full product completion.

Parent sources remain read-only. Artifacts: app/build/outputs/apk/debug/app-debug.apk,
testbed/build/outputs/apk/debug/testbed-debug.apk, app/build/outputs/bundle/release/app-release.aab.
The phone currently holds the discovery build from the successful build-discovery-check.log run;
subsequent test/wave/health refinements await installation after the permission choice.

