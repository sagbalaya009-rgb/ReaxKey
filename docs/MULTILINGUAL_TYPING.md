# Multilingual Typing

## 1. Goal

Provide a reliable normal keyboard rather than attempting to clone every Gboard intelligence feature.

## 2. Language-pack architecture

Define data-driven language packs:

```text
LanguagePack
- localeTag
- displayName
- alphabet rows/layers
- shifted variants
- long-press alternatives
- decimal separator
- punctuation preferences
- dictionary/suggestion provider optional
```

Initial implementation should prove the architecture with at least English plus additional packs chosen during implementation. Adding a language must not require rewriting the keyboard engine.

## 3. Language switching

- one active language: globe key can open/system-switch depending settings,
- multiple enabled packs: language key cycles local packs; long-press can expose system keyboard picker,
- current language visible on spacebar or accessible label.

## 4. Suggestions/autocorrect

The full product may include an on-device dictionary suggestion layer, but it must be local and must not require capturing/storing message histories.

If suggestion quality is not yet adequate, ship accurate typing without misleading autocorrect rather than destructive guesses.

## 5. Required typing correctness

- Unicode-safe deletion,
- diacritics through long-press or language layout,
- sentence capitalization,
- caps lock,
- numbers/symbols,
- email/URL-specific key affordances,
- action/enter behavior,
- no crashes on unusual editor types.
