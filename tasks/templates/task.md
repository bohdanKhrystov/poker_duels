---
id: TASK-EESSTT
title: Imperative one-line description
type: task
status: backlog
parent: STORY-EESS
module: poker-engine
estimate: S
labels: []
depends_on: []
---

## Goal

One sentence. What will be true when this is merged that is not true now.

## Context

Only what the agent cannot work out from the files. Link the two or three documents worth
reading — never "read the architecture docs" in general.

- [`docs/...`](../../docs/...) — why it is relevant

## Scope

- The specific things to build.
- Concrete enough that "done" is not a judgement call.

## Out of scope

- The neighbouring things that will be tempting.
- Where they live instead: `TASK-......`, or "not yet ticketed".

## Files

| File | Action |
| --- | --- |
| `path/to/File.kt` | create |
| `path/to/Other.kt` | modify |

## Acceptance criteria

- [ ] Checkable, not a matter of taste.
- [ ] Each one independently verifiable.
- [ ] Named edge cases that must behave correctly.

## Tests

- `TestClassName` — what it proves.
- Property: an invariant that must hold across generated input.

## Definition of done

Standard for every task — do not restate it in the ticket:
build green, tests green, `/code-review` run with findings fixed or answered, CI green, ticket
status `done`, `BOARD.md` updated, and **squash-merged into `develop`** by a PR linking this
ticket. A task is not done until its PR is merged.
