# Project State

## Current status

**Stage: Documentation / pre-implementation architecture complete**

No Android implementation should be assumed to exist yet.

## Product identity

Product name: **ReaxKey**

Repository: **ReaxKey** (private during development)

Development package/application ID: `com.reaxkey.keyboard`

Keep visible product strings centralized even though the product name is selected. Package identity must be reviewed before the first Play upload because it becomes a long-lived release identifier.

## Confirmed decisions

- Android first.
- Native Kotlin.
- Companion app + system IME in one installed Android application.
- Light and dark mode only; no theme marketplace.
- Multiple language layouts supported through language packs.
- Core function: user-owned stickers/GIFs/images available through the keyboard.
- Local-first; no backend/account required.
- Vision model postponed. Architecture must expose a future semantic tag/embedding provider seam.
- Users are guided through Android settings to enable/select the keyboard during onboarding.
- Android security model is respected; no private-directory bypassing.
- Rich content uses supported content-URI / Commit Content mechanisms.
- Commercial model: one-time paid Google Play app.
- No subscription, no ads, no required account, and no artificial feature tiering.
- No Google Play Billing integration is required for the base paid-app model unless in-app purchases are deliberately added later.

## Next implementation action

Follow `work/CHATGPT_WORK_MASTER_PROMPT.md` from the repository root. The coding agent must also read `docs/COMMERCIAL_DISTRIBUTION.md` before release-related work.

## Status update format

When an agent completes a stage, replace this section with:

- Stage completed
- Commit SHA
- Build result
- Tests run and result
- Physical device checks performed
- Known limitations
- Next stage
