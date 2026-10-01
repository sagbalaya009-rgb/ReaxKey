# Android IME Specification

## 1. Foundation

Implement the keyboard as a class extending `InputMethodService`.

The service must be declared with the Android input-method binding permission and IME metadata XML. The onboarding activity never tries to silently set itself as the default keyboard; it opens the appropriate system UI and lets the user choose.

## 2. UI technology

Use Jetpack Compose for the companion app. Prefer a stable View-based keyboard surface inside `InputMethodService` (custom view/layout + RecyclerView or equivalent) because IME lifecycle/window integration is more predictable. Compose inside IME may be revisited after the baseline is stable.

## 3. Keyboard layers

Required:

- alphabetic layout,
- shift/caps states,
- numbers,
- symbols,
- backspace with repeat,
- enter/action key,
- spacebar,
- language switch,
- emoji entry point,
- stickers/media entry point,
- GIF entry point/filter,
- return-to-letters control.

## 4. Text entry

Normal typing uses `InputConnection.commitText()` or appropriate editing methods rather than raw key events.

Required editor-aware behavior:

- honor input type where reasonable,
- numeric/password/email/URL layouts,
- handle `imeOptions` action labels,
- auto-capitalization based on sentence/editor state,
- cursor-safe deletion using Unicode-aware logic,
- no learning/collection in password fields.

## 5. Secure-field privacy mode

When the editor is a password or other sensitive field:

- do not persist surrounding text,
- do not add typed data to diagnostics,
- do not use typed content to update suggestions,
- do not record key events,
- keep media library access available only if it does not require observing typed content,
- display no clipboard history feature (the product does not need one).

## 6. IME onboarding integration

Companion app state machine:

```text
NOT_ENABLED
  -> open Settings.ACTION_INPUT_METHOD_SETTINGS
ENABLED_NOT_SELECTED
  -> show InputMethodManager.showInputMethodPicker()
SELECTED
  -> continue media setup/test
```

Re-check state in `onResume` because user changes settings outside the app.

## 7. Performance budgets

Targets, validated on a mid-range physical Android phone:

- IME surface usable quickly after invocation; avoid DB migrations/scans on opening.
- Media tab should render cached first results without full-library rescan.
- Search keystrokes debounced modestly but feel immediate.
- Thumbnail decoding must be bounded and cancellable.
- Background indexing never blocks key input.

## 8. Keyboard switching

Provide a visible globe/language/keyboard switch action that invokes the appropriate system input-method switching behavior. Do not trap the user in the custom IME.

## 9. Haptics and sound

Follow system/user settings. Add toggles only if reliable; defaults should respect Android configuration.
