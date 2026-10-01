# Troubleshooting Guide

## Keyboard does not appear in Android settings

Check:

- service extends `InputMethodService`,
- manifest service declaration,
- `android.permission.BIND_INPUT_METHOD`,
- input-method metadata XML,
- app installed successfully,
- no manifest merge error.

## Keyboard enabled but does not show

Check:

- user selected it as current IME,
- target field accepts text input,
- service/window creation errors in Logcat,
- keyboard view returned from service lifecycle.

## Text keys show but type nothing

Check `currentInputConnection`; use `commitText` and ensure the connection is valid. Avoid relying on raw key events.

## Media exists but scanner finds nothing

Check:

- source type,
- runtime permission state,
- SAF tree persisted permission,
- MIME filtering,
- whether media is in another app's private/app-specific directory that Android intentionally blocks.

Do not “fix” this by requesting root/accessibility/all-files permission without architectural review.

## Media preview works but sending fails

Check:

- target `EditorInfo` MIME list,
- selected content MIME type,
- content URI not `file://`,
- temporary read grant,
- provider configuration,
- target may simply not implement rich-content reception.

Test first against `:testbed` to separate sender bugs from third-party incompatibility.

## GIF appears as still image

Check decoder configuration and target app behavior separately. Keyboard preview animation does not guarantee receiving app renders animation.

## SAF source stops working later

Persisted permission may have been revoked, document moved, provider unavailable, or user cleared app data. Mark source unhealthy and guide reconnect.

## Build fails after AI handoff

Do not let the next model rewrite blindly. First:

1. read latest Git diff/log,
2. run Gradle with stack trace,
3. inspect the *first* meaningful error,
4. check dependency/version compatibility,
5. fix minimally,
6. add/update regression test,
7. update `PROJECT_STATE.md`.
