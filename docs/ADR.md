# Architecture Decision Record

## ADR-012 — Kotlin compiler alignment for the image loader

Coil 3.6.3 resolves Kotlin 2.4.10 metadata. The baseline Kotlin 2.2.10 compiler
correctly rejected it during the library integration build. Upgrade KGP and the
matching Compose compiler to 2.4.10 using AGP's documented buildscript override.
Do not suppress metadata compatibility checks. See
https://developer.android.com/build/releases/agp-9-0-0-release-notes#kotlin-gradle-plugin.

## ADR-011 — Initial reproducible toolchain (2026-10-01)

Use the installed stable AGP 9.3.2 / Gradle 9.5.0 / JDK 17 toolchain, API 37,
and AGP's built-in Kotlin 2.2.10 with the matching Compose compiler plugin.
Minimum Android is API 26. Compose BOM 2026.02.01 and Activity 1.13.0 are pinned.
AGP 9.4 is available but requires a different Gradle distribution; begin with the
compatible installed stable line to establish a measured baseline before upgrades.
Compatibility checked against https://developer.android.com/build/releases/agp-9-3-0-release-notes
and https://developer.android.com/jetpack/androidx/releases/compose-kotlin.
Modules are introduced with their first functional implementation rather than empty placeholders.

## ADR-001 — Native Android/Kotlin

**Decision:** Native Kotlin Android app.

**Why:** IME, storage permissions, content URIs, and Android lifecycle are platform-heavy. Native APIs reduce abstraction risk.

## ADR-002 — Local-first / no backend

**Decision:** Core product uses local database/index and does not require account/cloud.

**Why:** The value is local personal media. A backend adds privacy, cost, and failure modes without being necessary.

## ADR-003 — Compose app, View-based IME

**Decision:** Companion app uses Compose; IME starts with stable View-based UI.

**Why:** Compose is efficient for ordinary activities, while `InputMethodService` window/lifecycle integration is a sensitive surface where conventional Views reduce setup complexity.

## ADR-004 — Room + DataStore

**Decision:** Room stores structured media/index data; DataStore stores preferences.

## ADR-005 — URI-first media

**Decision:** Persist content URIs/source grants rather than forcing raw paths or copying every file.

**Why:** Modern Android storage is content-provider/URI-centric.

## ADR-006 — No Android security bypasses

**Decision:** No root, Accessibility scraping, hidden APIs, or attempts to enter other apps' private directories.

**Why:** Security, reliability, platform policy, and user trust.

## ADR-007 — Runtime rich-content capability

**Decision:** Direct media send is based primarily on the target editor's advertised MIME support.

**Why:** App behavior varies by editor and version; static app-name assumptions are brittle.

## ADR-008 — Vision deferred behind interface

**Decision:** No vision model now. Reserve semantic provider and DB origin fields.

**Why:** The user explicitly wants it later; the first complete build should solve storage/index/retrieval/send robustly without model complexity.

## ADR-009 — ReaxKey package identity

**Decision:** Use `com.reaxkey.keyboard` as the development application ID/namespace, while keeping visible copy centralized in resources.

**Why:** The product name is selected and a stable package simplifies Android/Play development. The identifier must still be verified before the first public Play upload because package identity is effectively permanent for that listing.

## ADR-010 — One-time paid Play Store model

**Decision:** ReaxKey is distributed as a paid Google Play app with a single upfront purchase. No subscription, ads, required account, or in-app "Pro" paywall.

**Why:** The product is a focused personal utility. Upfront pricing aligns incentives, preserves privacy, and avoids adding billing/backend complexity to core keyboard functionality.

**Implementation consequence:** Do not add Google Play Billing or an entitlement backend for the base product. Play Store controls acquisition. If future optional purchases are ever introduced, create a new ADR before adding Billing Library or network entitlement logic.
