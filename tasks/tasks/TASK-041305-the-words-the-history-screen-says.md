---
schema: 2
id: TASK-041305
title: The words the history screen says, and the two empties that must differ
type: task
status: done
parent: STORY-0413
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, history, copy]
depends_on: [TASK-041304]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/history/history-text.test.ts 2>&1 | grep -qE 'Tests +3 passed \(3\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells an empty record from an empty filter'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'gives the four states four different sentences'
  - cd web-client && npm run check
---

## Goal

Every word the history screen says lives in one file, and the difference between *you have played no
duels* and *no duel matches this* is one function with one `if` rather than an agreement between two
branches of a component.

## Files

| File | Action |
| --- | --- |
| `web-client/src/history/history-text.ts` | create |
| `web-client/src/history/history-text.test.ts` | create |

Read, not edited: `web-client/src/profile/name-text.ts` (the shape and register to follow),
`web-client/src/profile/ProfileStrip.tsx` (the voice already shipped: *"No profile yet."*, *"No duels
yet."*), [`ADR-0058`](../../docs/adr/ADR-0058-where-a-name-would-be-the-client-prints-no-name.md) §1
and §4.

## Scope

- Exactly these exports, and no others:

  ```ts
  export const HISTORY_HEADING = "Your duels";
  export const LOADING_RECORD = "Loading your duels…";   // U+2026 HORIZONTAL ELLIPSIS
  export const NO_DUELS = "No duels yet.";
  export const NO_MATCH = "No duel matches this.";
  export const READ_FAILED = "Your duels did not load. Reload the page to try again.";
  export const MORE = "Show more";
  export const OUTCOME_LEGEND = "Outcome";
  export const EVERY_OUTCOME = "All";
  export const OPPONENT_LABEL = "Opponent name";
  export function emptyLine(filtered: boolean): string;
  ```

- `emptyLine(true)` is `NO_MATCH`, `emptyLine(false)` is `NO_DUELS`. It exists as a function for the
  reason `nameOrNone` does: the two facts are *different facts about the world* and a component that
  chose between them inline would be a second place able to get it wrong.
- The strings are **golden**. They are typed as literals in this file and nowhere else, and the test
  asserts them character for character — including the full stops, the U+2026 in `LOADING_RECORD`
  and the absence of one on the three labels, which sit in control slots rather than sentences.
- `NO_DUELS` is the strip's own sentence, deliberately: the same fact on two surfaces should not have
  two spellings. `ProfileStrip.tsx` keeps its own literal — changing it is not this story's — and the
  test in this file is what makes a future divergence a visible edit rather than a silent one.
- `READ_FAILED` follows `name-text.ts`'s `unavailable` sentence: state what did not happen, then the
  one thing the player can do. It never says *error*, never names a status, and never offers a retry
  button this story does not build.
- KDoc on `emptyLine` naming `STORY-0413`'s rule and why one function decides.

## Out of scope

- The three outcome words. **A refusal, not an omission:** `outcomeWord` in `profile/profile-text.ts`
  already spells *Won*, *Lost* and *Drew*, a row already prints them, and a filter labelled with a
  second copy could drift from the rows it filters. `TASK-041311` labels the three controls by
  calling that function.
- `No name`. `nameOrNone` owns it (`ADR-0058` §2), and re-exporting it here would be the second
  decision point that ADR exists to prevent.
- Whatever word a search control needs. `TASK-041312` owns it, because `DEC-052` decides whether
  there is a control at all.
- Any colour, weight or type. `EPIC-06` owns the visual language; this file authors strings.

## Tests

`web-client/src/history/history-text.test.ts`, describe block `"the history screen's words"`.

| Test | Proves |
| --- | --- |
| `states every sentence exactly, character for character` | All nine constants asserted against their literals. Fails against any edit — a lost full stop, a `...` in place of the ellipsis, a re-worded failure line — so a change to what a player reads is a change to this test rather than a silent one. This is the only file where the literals are written twice, on purpose |
| `tells an empty record from an empty filter` | `emptyLine(false)` and `emptyLine(true)` in **one** test: each equal to its constant, and the two asserted **not equal** to each other. Fails against a function that ignores its argument — which two single-input tests could not tell from a constant, and which is exactly the defect `STORY-0413` names: *you have played no duels* and *no duel matches this* are different facts |
| `gives the four states four different sentences` | `new Set([LOADING_RECORD, emptyLine(false), emptyLine(true), READ_FAILED])` has size 4, and each of the four has non-zero length. Fails against any state reusing another's sentence — a screen that said *"No duels yet."* while it was still loading, or that answered a failed read with an empty record |

Three tests, in a new file: `npm run test -- src/history/history-text.test.ts` reports **3**.

## Acceptance criteria

- [ ] `the history screen's words > states every sentence exactly, character for character` passes,
      asserting all nine constants
- [ ] `the history screen's words > tells an empty record from an empty filter` passes, asserting
      both inputs and their inequality in one test
- [ ] `the history screen's words > gives the four states four different sentences` passes
- [ ] `grep -c 'No name' web-client/src/history/history-text.ts` returns `0`
- [ ] `grep -cE '"(Won|Lost|Drew)"' web-client/src/history/history-text.ts` returns `0`
- [ ] `npm run test -- src/history/history-text.test.ts` reports `Tests  3 passed (3)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
