# Project state — 2026-10-03

Branch: `build/full-reaxkey`. Existing draft PR: https://github.com/sagbalaya009-rgb/ReaxKey/pull/1. Parent ChatGPT-project `sources/` is read-only and untouched.

## Current product

ReaxKey opens on Stickers with a Settings tab. It has a conventional QWERTY keyboard, symbol pages and a Media panel. The library is sticker-focused: broad photos, screenshots and Downloads are excluded. It supports Android-backed discovery where media is accessible, SAF folder grants, individual import and share-to-ReaxKey. Room stores the local index, favorites, labels, collections and use counts. The IME checks receiving editor MIME support and sends a temporary content-URI grant, with an explicit Share fallback. The app has no account, ads, billing, backend or Internet permission.

## Physical observations

On TECNO KL5 / Android 14 (API 34), the owner approved the WhatsApp Stickers folder in Android's picker. That directory contained 10,834 WebP files via ADB; ReaxKey indexed 10,675 stickers and displayed them in the app and IME. Android's automatic MediaStore view did not expose that folder despite image permission. A scan left queued after Android stopped it; explicit Refresh was repaired to replace the stale unique work, and the source subsequently reached OK at 10,675. The separate receiver read an actual WebP sticker with a temporary URI grant (18,014 bytes); normal typing and ABC return were observed. Earlier receiver tests covered PNG, JPEG, GIF and WebP fixtures and unsupported MIME fallback. After the 1.0.0 debug build was installed, the owner physically retested Telegram and Snapchat and reported both working (2026-10-03). Exact third-party app versions and media formats remain unknown. WhatsApp sending and other apps are NOT TESTED.

## Release pass

Default app identity is `com.reaxkey.keyboard`; min/compile/target SDK are 26/37/37. Version is 1.0.0/code 1. Google's 2026 phone/tablet minimum for new submissions is target API 36. The release manifest has optional image permissions and WorkManager permissions, but no Internet, contacts, camera, mic, location or all-files permission. Backup and device transfer are disabled. Release bundle signing is configured through external environment variables; no upload key is present, so the generated AAB is **unsigned**.

Executed locally on this working tree: `test lint assembleDebug :app:assembleDebugAndroidTest :app:bundleRelease` passed (861 tasks; log `build/release-final-gate.log`). Lint completed with seven non-fatal warnings: newer Gradle/dependency versions, `dataExtractionRules` API scope, and optional resource shrinking. The API-scope warning is acceptable because `allowBackup=false` disables backup on older versions; dependency upgrades and resource shrinking are deferred to avoid unmeasured changes to verified behavior in this pass. Generated release manifest confirms version 1.0.0/code 1 and target 37. `jarsigner -verify` reports the AAB unsigned. Debug APK and unsigned AAB exist under `app/build/outputs/`.

The phone reappeared over ADB after reconnection. The 1.0.0 debug APK installed with data preserved. The first instrumentation run exposed a stale test expecting ordinary JPEG-photo import, which the sticker-only product intentionally rejects. The test was corrected without changing production media code; `:app:assembleDebugAndroidTest` passed and the installed suite then passed **4/4** tests, including real decoder/import, JPEG rejection, duplicate/outgoing URI and provider boundary checks. ADB confirmed installed version 1.0.0/code 1 and the ReaxKey IME remained selected. UI hierarchy checks confirmed a populated sticker grid, no empty state, and grid return after force-stop/relaunch. The owner's Telegram/Snapchat success report applies to this installed build; it does not verify exact third-party versions/formats, reboot persistence or a Play-delivered update.

CI separates the debug artifacts from the explicitly unsigned bundle. Play listing copy, privacy policy source/HTML, Data Safety draft, release notes, asset plan, signing/version guidance, publication checklist and a release gate register are under `docs/`. These are prepared drafts; the owner must supply publisher/support identity, host the privacy policy, capture final screenshots and complete Play Console declarations.

## Open gates

See `docs/RELEASE_BLOCKERS.md`. In particular: secure upload key and signed AAB; Google Play review of optional `READ_MEDIA_IMAGES`; wider IME regression, Play-track acquisition/update and broader API/accessibility checks. No `v1.0.0-rc1` tag or production-ready claim while these remain open.
