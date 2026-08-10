---
schema: 2
id: TASK-EESSTT
title: Imperative one-line description
type: task
status: backlog
parent: STORY-EESS
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: []
depends_on: []
verify:
  - ./gradlew :poker-engine:test --tests '*TheTestClass'
---

## Goal

One sentence. What is true after this merges that is not true now.

## Files

Everything the implementer may open. **Five at most** — this list is the context budget.

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../Thing.kt` | create |
| `poker-engine/src/test/kotlin/.../ThingTest.kt` | create |

## Scope

- Two to four bullets. Concrete enough that "done" is not a judgement call.

## Out of scope

- The neighbouring things that will be tempting.
- Where they live instead: `TASK-......`, or "not yet ticketed".

## Tests

Name the class and each test method. The `verify` command runs these, so the names here and the
names there must match exactly.

`ThingTest`

| Test | Proves |
| --- | --- |
| `doesTheObviousThing` | … |
| `rejectsTheBadInput` | … |

## Acceptance criteria

One line per test above, phrased so a small model can check it by reading a test report rather
than by forming an opinion.

- [ ] `ThingTest.doesTheObviousThing` passes
- [ ] `ThingTest.rejectsTheBadInput` passes
- [ ] Every command in `verify:` exits 0

> Never write a criterion like *"handles edge cases correctly"* or *"is well designed"*. If it
> cannot be a passing test, it cannot be a criterion — sharpen it or split the ticket.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
