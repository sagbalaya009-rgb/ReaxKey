# AGENTS.md — Mandatory AI Engineering Rules

This file is authoritative for every coding agent working on this repository.

## Mission

Build a complete, production-quality Android universal media keyboard. The user is not expected to code. The coding agent owns implementation quality, debugging, tests, documentation, and keeping the repository coherent.

## Hard rules

1. **Git is the single source of truth.** Read the current repository before changing anything.
2. **Do not rewrite working architecture casually.** Extend through documented seams.
3. **Do not invent Android capabilities.** Verify platform-sensitive behavior against current Android documentation.
4. **Do not attempt to bypass Android sandboxing, scoped storage, or app-private storage protections.**
5. **Do not use Accessibility Service, root, hidden APIs, screen scraping, notification scraping, or private-app-directory tricks to obtain stickers.**
6. **Do not log, upload, store, or analyze users' typed text.** This is a keyboard; privacy expectations are unusually high.
7. **No required backend. No account requirement.** Core functionality is offline/local-first.
8. **No INTERNET permission in the core app unless a future feature explicitly requires it and the project owner approves it.**
9. **Vision/semantic image understanding is deferred.** Build a clear interface for it, but do not add a model until requested.
10. **Every feature must have tests where practical.** Every bug fix gets a regression test when feasible.
11. **Never commit signing keys, keystores, passwords, tokens, or secrets.**
12. **Keep the project buildable after each commit.** If a large change is required, use a branch and coherent commits.
13. **Update `PROJECT_STATE.md` after every meaningful stage.**
14. **Update docs when behavior or architecture changes.**
15. **Do not claim an app is supported until tested or explicitly marked unverified.**

## Required pre-change routine

Before coding:

- read `README.md`, `PROJECT_STATE.md`, and relevant `docs/*` files;
- inspect Gradle/configuration and existing tests;
- run the fastest meaningful build/test command;
- state the intended change in a short plan;
- implement;
- run tests/build again;
- inspect diff for accidental changes;
- update docs/state;
- commit with a meaningful message.

## Architecture boundaries

Expected modules:

- `:app` — Android application, manifest, main activity, dependency wiring.
- `:ime` — Android library containing `InputMethodService`, typing engine, media keyboard surface, rich-content sender.
- `:core:model` — domain models and interfaces.
- `:core:data` — Room database/repositories.
- `:core:media` — MediaStore/SAF/import scanning and URI handling.
- `:core:search` — lexical search/ranking and future semantic provider interface.
- `:core:ui` — shared UI resources/components.
- `:feature:onboarding` — setup/permission/IME activation flow.
- `:feature:library` — media library management.
- `:feature:settings` — theme, language, privacy, storage sources, diagnostics.
- `:testbed` — development-only companion app that declares rich-content MIME acceptance so `commitContent` can be tested deterministically.

Avoid needless modules beyond these unless they clearly reduce coupling.

## Coding standards

- Kotlin idioms; avoid Java unless required by an API/sample interop.
- Structured concurrency with coroutines.
- Flows for observable local data.
- Room migrations are explicit and tested.
- No global mutable singleton state for UI/domain logic.
- Repository interfaces between storage and UI.
- URI-first media handling; avoid unnecessary file copies.
- Main-thread work must stay lightweight.
- Thumbnail generation/decoding is bounded and cache-aware.
- Handle process death and revoked persisted URI permissions.
- All strings user-visible through Android resources.
- Accessibility content descriptions where needed.

## IME-specific rules

- Use `InputMethodService` and `InputConnection` for text.
- Prefer `commitText()` to raw key events for normal typing.
- Use target editor MIME declarations to decide whether rich content can be committed.
- Use `InputConnectionCompat.commitContent()` / platform equivalent with temporary URI permission grants.
- Never assume WhatsApp, Telegram, Instagram, Snapchat, Discord, or any other app supports the same rich-content path.
- Maintain a compatibility layer based on observed capabilities, not hard-coded marketing claims.
- Password/secure fields trigger privacy mode: no usage-learning updates, no media-recents mutation from typed content, no surrounding-text collection beyond what is essential to perform an explicit key action.

## Definition of "done"

A task is not done because code was generated. It is done only when:

- code compiles,
- tests relevant to the change pass,
- the behavior is verified on an emulator/physical device when platform integration is involved,
- failures are handled,
- docs/state are updated,
- no secrets or debug hacks were introduced.

## Commercial invariants

- Product name: **ReaxKey**.
- Development application ID: `com.reaxkey.keyboard`.
- Planned distribution: one-time paid Google Play app.
- Do not add subscriptions, ads, accounts, Billing Library, or an entitlement backend unless the owner explicitly changes this contract.
- Do not weaken offline/local-first behavior merely to enforce payment.
- Debug APKs are development artifacts; Play Store purchase controls commercial acquisition.
