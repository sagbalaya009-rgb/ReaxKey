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

## 10. Typing upgrade (2026-10-03)

The existing View-based `ReaxKeyService` and `MediaPanel`/`RichContentSender` remain in place. The typing surface adds a single rounded globe/language/Media control bar and a three-candidate strip. English suggestions use a small on-device frequency list, prefix matching, a few preceding-word weights and words deliberately repeated three times in a session. Only trusted words are persisted in local app preferences; Settings > Typing can disable learning or clear them. Other languages currently retain reliable manual typing without predictions.

Autocorrect runs only at space or punctuation boundaries and only for a curated list of high-confidence English typos. Immediate Backspace restores the original word; unrelated typing clears that undo. Password/PIN/OTP-like editors, editors requesting no personalized learning, numeric fields, URL, email and filter/code-like fields do not use text-derived suggestions or correction. No typed sentence history is saved or transmitted.

A space tap remains a space. A 330 ms hold enters cursor mode and shows an anchored circular bubble; movement accumulates in separate axes with approximately 8–11 dp per step and mild speed adjustment. Android editor DPAD movement handles visual lines when supported. If vertical movement does not advance and the editor exposes extracted text, a newline-aware logical-line fallback preserves approximate column. These semantics cannot infer arbitrary wrapped-line geometry in every host app. The IME defers selection-triggered redraw while a space gesture is active.
