# 1.0.0 release gate register — 2026-10-02

The source tree is a release candidate workstream, not an approved Play submission. Severity describes the effect on shipment; open gates are not waived by a passing build.

| Severity | Gate | Evidence / action |
|---|---|---|
| BLOCKER | Signed upload bundle and Play App Signing | Local bundle is unsigned. Owner must create and safeguard an upload key outside Git, then sign and verify the AAB before upload. |
| BLOCKER | Play photo/video-permission eligibility | The app declares `READ_MEDIA_IMAGES` for optional automatic shared-sticker discovery. Google's review may require a declaration and may reject broad access if SAF/picker access is judged sufficient. Resolve in Play Console; if rejected, revise the permission and source UX before submission. Do not assert approval. |
| HIGH | Final physical regression on exact release build | Device 1.0.0 debug installation, 4/4 instrumentation tests, populated grid and process restart passed. Owner reports Telegram/Snapchat working, but both remain untested by the agent on this exact build. Recheck typing, secure fields, themes, both apps and reboot before tagging. |
| HIGH | Play track acquisition/update test | Requires signed bundle, Play Console and test track. Confirm paid acquisition, install and upgrade preserve grants/data before production. |
| NORMAL | Broader device/API coverage | Physical QA is on Android 14; API 36/37 behavior, font scaling, TalkBack, dark/light contrast and reboot need additional devices or emulator. |
| NON-BLOCKING FUTURE | Unsupported messaging apps | WhatsApp sending and all apps not in the matrix remain unverified; no universal claim. |

The 2026 Play phone/tablet target requirement is API 36 or higher; ReaxKey targets API 37. The package is `com.reaxkey.keyboard`, min API 26, version 1.0.0/code 1. No account, ads, backend, Play Billing or Internet permission were found in the product manifest/source audit. Unit tests, lint, debug and AAB builds are separate checks; their results belong in `PROJECT_STATE.md`.

Do not create `v1.0.0-rc1` while a BLOCKER or HIGH engineering gate remains open.
