---
schema: 2
id: TASK-030405
title: Events narrate, and change no field a Snapshot established
type: task
status: ready
parent: STORY-0304
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store]
depends_on: [TASK-030404]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +78 passed \(78\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'appends events to the narration log in order'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'changes no field a snapshot or a pending turn established'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "does not populate a seat's hole cards from a HandRevealed event"
  - cd web-client && npm run check
---

## Goal

An `Events` frame appends to `state.narration` and changes nothing else — not `view`, not
`pendingTurn` — even when one of the events is a `HandRevealed`.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `web-client/src/protocol/protocol.gen.ts` | read — `GameEvent`'s member list and `HandRevealed`'s fields |

## Scope

- Add one case to the `switch` in `applyServerMessage`, after `case "Rejected":`:

  ```ts
  case "Events":
    return { ...state, narration: [...state.narration, ...message.events] };
  ```

- This is the whole implementation: append, nothing else. There is no `if` and no second `switch`
  inspecting `message.events` for a `HandRevealed`, a `HoleCardsDealt`, or any other member of
  `GameEvent`. `STORY-0304`'s design notes call this out directly: *"A reducer that rebuilds the
  table from events has re-implemented the rules in TypeScript, and it will be right until the
  first hand where it is not."* The `Snapshot` that always follows a hand's events is where a
  revealed hand actually reaches state — `TASK-030403`.
- `[...state.narration, ...message.events]` is a new array; `state.narration` itself is never
  mutated in place, matching `readonly GameEvent[]` on the type.

## Out of scope

- `DuelFinished` — `TASK-030406`.
- Deriving anything from the narration log — a hand log or an animation is `STORY-0306` onward's
  concern, reading `state.narration` as data, not this ticket's.
- Capping or trimming the narration log. Nothing in this story's acceptance criteria asks for it,
  and a duel is short enough that this is not yet a real constraint.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Three `it` blocks,
appended after `TASK-030404`'s three. **Those fourteen existing tests are not edited.** The second
and third new tests reuse `samplePlayerView` / `sampleSeat` from `TASK-030403` unchanged.

| Test | Proves |
| --- | --- |
| `appends events to the narration log in order` | two separate `Events` frames, each with one event, leave `state.narration` equal to both events in the order they arrived — proving append, not replace |
| `changes no field a snapshot or a pending turn established` | `Snapshot` then `YourTurn` then `Events` (three real `applyServerMessage` calls) leaves `state.view` and `state.pendingTurn` exactly as they were before the `Events` call |
| `does not populate a seat's hole cards from a HandRevealed event` | `Snapshot` (seat 1's `holeCards: []`) then `Events` carrying `{type:"HandRevealed", sequence, seat:1, cards:["2c","7h"]}` leaves `state.view.seats[1].holeCards` as `[]` |

Three tests. Seventy-five exist, so the suite reports **78**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 78 passed (78)` | the three tests ran and the fourteen before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red:**

1. Change the case to `narration: message.events` (replace, not append) → `appends events to the
   narration log in order` fails: after the second call, `state.narration` has length one instead
   of two. Revert.
2. Add a branch that sets `pendingTurn: null` whenever `message.events` is non-empty → `changes no
   field a snapshot or a pending turn established` fails, `expected null to equal {...}`. Revert.
3. Add a fold over `message.events` that writes a matching `HandRevealed.cards` into
   `view.seats[seat].holeCards` → `does not populate a seat's hole cards from a HandRevealed
   event` fails, finding `["2c","7h"]` where `[]` was expected. Revert.

Quote all three in the PR. The third is the story's fourth acceptance criterion made executable.

## Acceptance criteria

- [ ] `the duel state > appends events to the narration log in order` passes
- [ ] `the duel state > changes no field a snapshot or a pending turn established` passes
- [ ] `the duel state > does not populate a seat's hole cards from a HandRevealed event` passes
- [ ] `npm run --silent test` reports `Tests  78 passed (78)`
- [ ] The fourteen `it` blocks from `TASK-030401` through `TASK-030404` are unedited, and their
      assertions are byte-identical
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
