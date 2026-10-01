# ReaxKey — Engineering Repository

> **Product identity is now locked for development:** **ReaxKey**. The app is planned as a **one-time paid Google Play app** with no ads, no subscription, and no required account. Core keyboard/media functionality remains local-first and offline-capable.

## What this project is

A privacy-first Android keyboard that lets a user type normally and access their own stickers, GIFs, images, and reaction media from one keyboard across compatible apps.

The application consists of:

- a native Android IME (`InputMethodService`),
- a companion library/management app,
- local media discovery and indexing,
- rich-content insertion into compatible target apps,
- local search, favorites, recents, collections, and source management,
- multilingual keyboard layouts,
- light and dark themes,
- no required backend and no required account,
- one-time paid Google Play distribution; no subscription or ads,
- no AI/vision model in the initial implementation; vision is a later on-device extension.

## Non-negotiable platform truth

The app **must not** claim that it can silently read every sticker from every installed app. Modern Android sandboxing blocks access to other apps' private directories. Media discovery must use Android-supported access paths: MediaStore where appropriate, Storage Access Framework (SAF) user-selected folders/files, Android photo/media permissions where justified, and explicit import/share flows.

Rich media insertion is also cooperative: a target editor must advertise accepted MIME types for `commitContent`. If it does not, the keyboard must gracefully explain the limitation and offer an explicit share fallback rather than using hacks.

## Repository map

- `AGENTS.md` — mandatory instructions for ChatGPT Work, Claude, Gemini, Kimi, or any coding agent.
- `PROJECT_STATE.md` — canonical project status and current stage.
- `work/CHATGPT_WORK_MASTER_PROMPT.md` — paste this into ChatGPT Work to start building from scratch.
- `docs/PRODUCT_SPEC.md` — full product behavior.
- `docs/ARCHITECTURE.md` — modules, boundaries, data flow, and lifecycle.
- `docs/ANDROID_IME.md` — keyboard-specific behavior and Android IME rules.
- `docs/MEDIA_INGESTION.md` — how media is discovered, indexed, imported, and refreshed.
- `docs/RICH_CONTENT.md` — how stickers/GIFs are inserted into apps.
- `docs/PERMISSIONS_PRIVACY.md` — permissions, sandboxing, privacy, and security.
- `docs/DATA_MODEL.md` — Room schema and indexing model.
- `docs/UI_UX.md` — onboarding, main app, keyboard panels, themes, and accessibility.
- `docs/SEARCH.md` — pre-vision search/ranking and future semantic-search seam.
- `docs/MULTILINGUAL_TYPING.md` — text keyboard behavior and language-pack architecture.
- `docs/TESTING.md` — automated tests, physical-device tests, compatibility matrix, testbed app.
- `docs/BUILD_RELEASE.md` — Android Studio, APK generation, phone installation, CI, signing.
- `docs/COMMERCIAL_DISTRIBUTION.md` — one-time paid Play Store model, entitlement boundaries, and commercial release rules.
- `docs/IMPLEMENTATION_PLAN.md` — staged build sequence for the complete app; stages are construction order, not an MVP.
- `docs/DEFINITION_OF_DONE.md` — release gates.
- `docs/TROUBLESHOOTING.md` — common failure modes.
- `docs/AI_HANDOFF_PROTOCOL.md` — how to move between ChatGPT/Claude/Kimi/Gemini without corrupting the codebase.
- `docs/ADR.md` — architectural decisions and reasons.
- `docs/SOURCES.md` — current Android documentation used to ground platform behavior.

## Technical baseline

- Kotlin
- Gradle Kotlin DSL
- AndroidX
- Jetpack Compose for companion-app screens
- View-based IME surface for maximum reliability inside `InputMethodService`
- Room / SQLite
- Coroutines + Flow
- WorkManager
- MediaStore + Storage Access Framework
- AndroidX Core `InputConnectionCompat` / Commit Content API
- Coil or an equivalent current stable image loader with animated GIF/WebP support
- JUnit + AndroidX Test + Espresso/Compose testing where relevant

The coding agent must select **current stable compatible dependency versions at build time** rather than blindly copying stale version numbers from documentation.

## Package naming

Use the development application ID / namespace:

`com.reaxkey.keyboard`

Treat the application ID as release-critical. Before the first Google Play upload, verify it is unique and final because changing the Play package identity later means publishing a different app listing. Visible branding strings still live in centralized resources/configuration so copy can evolve without architecture churn.

## First build target

The first executable checkpoint is not a feature-limited product. It is the first construction gate:

1. project builds,
2. app installs,
3. onboarding can open Android keyboard settings,
4. IME can be enabled and selected,
5. keyboard appears in a normal text field,
6. a basic key can commit text,
7. automated CI can produce a debug APK.

Then continue through every stage in `docs/IMPLEMENTATION_PLAN.md` until the complete definition of done is met.
