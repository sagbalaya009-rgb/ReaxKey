# Complete Implementation Plan

These are **construction stages**, not an MVP roadmap. The target is the complete product described in the repository.

## Stage 0 — Repository and build foundation

Deliver:

- Gradle multi-module Android project,
- application namespace/applicationId `com.reaxkey.keyboard`,
- debug build succeeds,
- CI builds debug APK,
- root docs preserved,
- baseline test infrastructure.

Gate: clean clone -> one documented command -> successful debug APK.

## Stage 1 — IME skeleton + onboarding

Deliver:

- `InputMethodService`,
- manifest/input-method XML,
- onboarding status state machine,
- open keyboard settings,
- show input method picker,
- companion test field,
- one functional alphabetic layout capable of committing text.

Gate: physical phone can enable/select keyboard and type.

## Stage 2 — Production typing engine

Deliver:

- alphabet, shift/caps,
- number/symbol layers,
- Unicode-safe delete + repeat,
- space/enter/IME actions,
- input-type aware layouts,
- privacy mode for sensitive fields,
- language-pack engine,
- light/dark keyboard surfaces,
- keyboard switch behavior.

Gate: reliable normal typing across varied Android fields.

## Stage 3 — Local data foundation

Deliver:

- Room schema,
- repositories,
- DataStore preferences,
- migrations and tests,
- source health model,
- no typed text persistence.

Gate: DB tests and migration tests pass.

## Stage 4 — Media ingestion

Deliver:

- MediaStore integration where permission strategy allows,
- SAF folder connection and persisted grants,
- file/photo selection,
- share/import receiver,
- app-owned import copies,
- media metadata/classification,
- incremental scan,
- WorkManager refresh,
- revoked-source handling.

Gate: a physical phone can connect sources, index media, revoke access, reconnect, and not lose app stability.

## Stage 5 — Companion library UI

Deliver:

- Library/Collections/Sources/Settings navigation,
- media grid,
- filters,
- favorites,
- collections,
- item details,
- remove-from-index vs delete-owned-copy semantics,
- rescan/reconnect UI,
- diagnostics export.

Gate: all indexed media can be managed without keyboard UI.

## Stage 6 — Search

Deliver:

- normalized lexical index,
- FTS or equivalent local search,
- weighted ranking,
- favorites/recency/frequency boosts,
- source/type filters,
- tests for ranking.

Gate: searches remain responsive on a large generated library.

## Stage 7 — Media keyboard surface

Deliver:

- Stickers/GIFs/Recent/Favorites tabs,
- internal keyboard search field,
- thumbnail grid,
- animated preview with bounded resource use,
- long-press actions,
- seamless return to typing.

Gate: opening/scrolling media panel does not freeze normal typing.

## Stage 8 — Rich-content sending

Deliver:

- capability reader from `EditorInfo`,
- content URI resolver/provider,
- temporary grants,
- `commitContent`,
- MIME-aware enabling/disabling,
- explicit Share fallback,
- deterministic `:testbed` receiver app.

Gate: testbed matrix passes for supported/unsupported MIME fields.

## Stage 9 — Real-app compatibility hardening

Deliver:

- compatibility matrix document,
- manual testing on major communication apps available to tester,
- fixes only through supported Android mechanisms,
- clear unsupported UX.

Gate: no compatibility claim exceeds evidence.

## Stage 10 — Multilingual completion

Deliver:

- multiple data-driven language packs,
- long-press diacritics,
- language switching,
- locale-correct punctuation,
- optional local dictionary suggestions if quality is adequate,
- language tests.

Gate: adding another language is primarily data/config, not engine rewrite.

## Stage 11 — Hardening

Deliver:

- accessibility pass,
- process-death/reboot tests,
- low-memory/large-library tests,
- battery/background-work review,
- security/privacy review,
- stale URI handling,
- crash-free happy paths,
- no internet permission in core build.

## Stage 12 — Release pipeline

Deliver:

- reproducible debug APK CI,
- release build instructions,
- signing instructions without secrets,
- versioning,
- changelog process,
- clean-install/update tests,
- current Google Play paid-app policy/release verification.

## Stage 13 — Future on-device vision (deferred until explicitly requested)

When requested:

- benchmark candidate on-device models,
- add semantic metadata provider implementation,
- keep processing opt-in/local,
- background indexing with thermal/battery constraints,
- generated tags/embeddings,
- semantic + lexical hybrid search,
- no breaking DB/UI redesign.
