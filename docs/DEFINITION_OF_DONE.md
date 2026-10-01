# Definition of Done — Complete App

The complete Android app is ready when all applicable items below are true.

## Build

- Clean clone builds with documented commands.
- Debug APK generated locally and by CI.
- Release build can be produced with an external signing configuration.
- No secrets in repository history.

## Onboarding

- Detects IME enabled state.
- Opens Android keyboard settings.
- Invokes keyboard picker.
- Re-checks state on return.
- Media-source setup is understandable.
- Denying permissions does not trap the user.

## Typing

- Reliable alphabet typing.
- Shift/caps.
- Numbers/symbols.
- Unicode-safe deletion and repeat.
- Enter/editor actions.
- Appropriate input-type behavior.
- Multiple language packs.
- Keyboard/system switching.
- Light/dark mode.
- Sensitive-field privacy mode.

## Media

- User-authorized sources connect successfully.
- MediaStore/SAF/import paths work as designed.
- Supported media metadata is indexed.
- Incremental rescans avoid obvious duplicates.
- Revoked/missing sources degrade gracefully.
- Favorites/collections/recents work.
- Search remains local and responsive.

## Rich content

- MIME capabilities checked at runtime.
- Supported content sends through content URI + commit mechanism.
- Temporary permissions handled correctly.
- Unsupported fields show accurate fallback UX.
- Testbed coverage exists.
- Compatibility matrix documents real-app tests.

## Privacy/security

- No keystroke/typed-text logging.
- No backend/account requirement.
- No private-app-storage bypass.
- Core build does not need Internet permission.
- Diagnostics are sanitized.
- No broad file-manager permission shortcut.

## Quality

- Unit tests pass.
- Room migration tests pass.
- Android lint/static checks pass or documented exceptions exist.
- Physical device testing completed for platform-sensitive features.
- Large-library and process-death cases tested.
- Documentation matches current implementation.
- `PROJECT_STATE.md` is current.
