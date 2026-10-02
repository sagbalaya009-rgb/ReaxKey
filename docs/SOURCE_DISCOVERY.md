# Source discovery

Implementation in progress against the owner specification (2026-10-02).

MediaSourceRecognizer consumes one metadata snapshot, avoiding repeated broad queries per app.
SourceDiscoveryEngine publishes progressive, cancellable results. Recognizers select exclusive
shared-media buckets (WhatsApp, WhatsApp Business, Telegram, Downloads, Pictures, generic phone),
existing validated tree grants and imported files. Counts describe supported accessible metadata;
indexing validates real decoder bounds and may yield fewer valid reactions. No fabricated counts.

Android storage references:
- https://developer.android.com/training/data-storage/shared/media
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/about/versions/14/changes/partial-photo-video-access

Physical source discovery, guided picker positioning and persisted-access behavior remain unverified
until the new implementation is installed. Prior device typing and PNG/JPEG/GIF/WebP receiver tests
passed on TECNO KL5 / Android 14. CI run 36974128678 passed for pre-discovery commit 805ffa3.

## Implementation checkpoint

Recognizers operate over a shared metadata snapshot. MediaStore rows are MIME-filtered and counted
in a single query, with progressive results; individual files are decoded only during ingestion.
Known shared-path patterns are candidate evidence, not guarantees. Filenames never determine app
attribution. When relative-path metadata is unavailable, items remain generic Phone media.
Existing grants are validated through their document provider and counted with cancellation.
Generic providers may use opaque document IDs; only external-storage IDs receive path restrictions.

The picker is launched only from an explicit grant explanation or the personal-folder fallback.
Only real prior tree URIs are used as initial locations. No synthetic app-directory URI is presented
as a found source. On return, a directory is validated and a persisted read grant is used.

Shared source buckets are exclusive. Identical stable URIs across source records reuse the same
logical media ID; disconnected sources can hand over to an accessible record while preserving
favorite/label/collection state. Imported bytes deduplicate by SHA-256. Different providers that
expose the same bytes under unrelated URIs are not globally hashed on every scan; this remains a
practical deduplication limit. Explicit reimport restores a removed reaction; background rescans do not.

New UI: discovery-first onboarding step, Add reactions, grouped sources, partial-access language,
privacy disclosure, success screen, share preview and optional collection for bulk imports.
The owner-referenced earlier Exact UI Redesign document was not available; the supplied dark
background, blue/violet CTA and card guidance was applied to the current app.

Automated coverage includes attribution, no source/denied metadata, partial access, progressive
10k-row metadata, missing grants, invalid/private candidates, duplicate source URIs and reimport.
Physical permission/reconnect, provider disappearance during a real scan, process death while a
scan runs and animated-media performance are remaining test gates, not claimed as passed.
