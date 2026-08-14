---
schema: 2
id: TASK-030402
title: YourTurn sets a pending turn identified verbatim by the message
type: task
status: backlog
parent: STORY-0304
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store]
depends_on: [TASK-030401]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +67 passed \(67\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sets a pending turn identified verbatim by the message'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'replaces rather than merges a pending turn already set'
  - cd web-client && npm run check
---

## Goal

A `YourTurn` sets `state.pendingTurn` to exactly the `handNumber`, `actionSequence` and
`legalActions` the message carried — copied, never recomputed.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `web-client/src/protocol/protocol.gen.ts` | read — exact shape of `YourTurn` and `LegalActions` |

## Scope

- Add one case to the `switch` in `applyServerMessage`, immediately after `case "RoomJoined":`:

  ```ts
  case "YourTurn":
    return {
      ...state,
      pendingTurn: {
        handNumber: message.handNumber,
        actionSequence: message.actionSequence,
        legalActions: message.legalActions,
      },
    };
  ```

- `handNumber` and `actionSequence` are copied field by field, not spread from `message` as a
  whole — `YourTurn` carries a fourth field, `legalActions`, and `PendingTurn.legalActions` is
  named as its own field rather than nested under a wire-shaped wrapper, so an accidental
  `...message` would leave a stray `type: "YourTurn"` in `pendingTurn`.
- Nothing here validates, clamps or renumbers `handNumber` or `actionSequence`. A turn is a stale
  echo the moment the client changes either number, and a stale echo is exactly how the server
  rejects a late click — see `STORY-0304`'s design notes.
- A second `YourTurn` **replaces** `pendingTurn` outright — the object literal above is a full
  replacement, never a merge with whatever was already there.
- If a test builds a `legalActions` fixture as its own `const` before calling
  `applyServerMessage`, give it an explicit `: LegalActions` annotation (imported from
  `"../protocol"`). Without one, TypeScript widens `allowed: ["CHECK", "BET"]` to `string[]`,
  which no longer satisfies `readonly ActionType[]` and fails `npm run check`'s typecheck step —
  a literal passed inline as part of the call is contextually typed and does not need this.

## Out of scope

- `Snapshot`, `Rejected` and `DuelFinished` clearing a pending turn — `TASK-030403`,
  `TASK-030404`, `TASK-030406`.
- Validating that `legalActions.allowed` is non-empty or that the seat matches `mySeat`. The
  server decides who may act; the store stores what it is told.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Two `it` blocks,
appended after `TASK-030401`'s four. **Those four are not edited**: this ticket adds a case none
of them exercises.

| Test | Proves |
| --- | --- |
| `sets a pending turn identified verbatim by the message` | `applyServerMessage(initialState(), {type:"YourTurn", handNumber:3, actionSequence:7, legalActions})` has `pendingTurn` equal to `{handNumber:3, actionSequence:7, legalActions}` exactly |
| `replaces rather than merges a pending turn already set` | a second `YourTurn` with `actionSequence:3` and a different `legalActions.allowed` leaves `pendingTurn.actionSequence` at `3` and `pendingTurn.legalActions.allowed` matching only the second message, with no trace of the first |

Two tests. Sixty-five exist, so the suite reports **67**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 67 passed (67)` | the two tests ran and the four before them still do |
| the two `--reporter=verbose` greps | both exist by name |
| `npm run check` | typechecks, lints, formats, and re-runs the protocol boundary guard over this file |

**Name the edit that makes each assertion red:**

1. Change `actionSequence: message.actionSequence` to `actionSequence: message.actionSequence + 1`
   → `sets a pending turn identified verbatim by the message` fails: `pendingTurn` is compared
   whole with `toEqual`, so the failure reads `expected { handNumber: 3, …(2) } to deeply equal {
   handNumber: 3, …(2) }` rather than naming the field directly — the PR should show the expanded
   diff, which does name `actionSequence: 8` where `7` was expected. Revert.
2. Change the case to spread the previous `pendingTurn` and only overwrite `handNumber` and
   `actionSequence`, dropping `legalActions: message.legalActions` from the explicit fields (an
   attempt to "merge" that reads as reasonable) → `replaces rather than merges a pending turn
   already set` fails. Because the first `YourTurn` in the test starts from `pendingTurn: null`,
   spreading it forward carries nothing, so `legalActions` is missing from both turns and the
   assertion throws `TypeError: Cannot read properties of undefined (reading 'allowed')` rather
   than a clean mismatch — which is itself the point: a merge here does not gracefully fall back,
   it silently drops a field the caller needs. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the duel state > sets a pending turn identified verbatim by the message` passes
- [ ] `the duel state > replaces rather than merges a pending turn already set` passes
- [ ] `npm run --silent test` reports `Tests  67 passed (67)`
- [ ] The four `it` blocks from `TASK-030401` are unedited, and their assertions are byte-identical
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
