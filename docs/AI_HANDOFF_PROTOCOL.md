# AI Handoff Protocol

The owner may switch among ChatGPT, Claude, Kimi, and Gemini when limits are reached. This protocol prevents divergence.

## Golden rule

**Never hand off by copying a stale code folder or pasting random files into a new chat. GitHub/repository HEAD is the only source of truth.**

## Before an AI starts

It must read:

1. `AGENTS.md`
2. `README.md`
3. `PROJECT_STATE.md`
4. relevant architecture docs
5. latest Git log/diff

Then it runs the baseline build/tests.

## Before an AI stops

It must:

- finish or revert partially broken work,
- make the repository buildable,
- run tests/build,
- commit coherent changes,
- update `PROJECT_STATE.md` with exactly what changed,
- note known failures with reproduction steps,
- never leave uncommitted mystery edits unless explicitly documented.

## Handoff note template

```text
Last completed stage:
Branch:
Latest commit:
Build command/result:
Tests/result:
Physical device result:
Implemented:
Known issues:
Files intentionally changed:
Next exact objective:
Do not rewrite:
```

## Prompt discipline

Avoid prompts such as “build the whole keyboard however you want.”

Prefer:

- cite the stage from `IMPLEMENTATION_PLAN.md`,
- require current repo inspection,
- require tests,
- require build success,
- require state/docs update,
- forbid architecture rewrites unless justified.

## When models disagree

Repository docs win unless a model verifies a platform fact has changed. If it has, update the relevant doc and ADR with a source and reason before restructuring implementation.
