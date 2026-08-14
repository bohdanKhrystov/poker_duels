---
schema: 2
id: TASK-030502
title: A Failure reaches the state through the reducer, and a join that lands clears it
type: task
status: backlog
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store]
depends_on: [TASK-030501]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +91 passed \(91\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records the room the server does not know'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records a room that already has a rival in it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a refusal changes nothing a RoomJoined established'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a join that lands clears the refusal before it'
  - cd web-client && npm run check
---

## Goal

`DuelState` gains `refusal`, and a `Failure` frame stops falling through the reducer's `default`:
the one path by which a connection fact a screen must show reaches a screen (`ADR-0032` §3).

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `web-client/src/protocol/protocol.gen.ts` | read — `Failure` and the `ProtocolError` union |

## Scope

- Add `ProtocolError` to the existing `import type { … } from "../protocol";` statement — extend
  that one statement, do not add a second.
- Add one field to `DuelState`, last:

  ```ts
  readonly refusal: ProtocolError | null;
  ```

  and `refusal: null,` as the last entry of the object `initialState()` returns.
- Add one case to the `switch` in `applyServerMessage`, after `case "DuelFinished":`:

  ```ts
  case "Failure":
    return { ...state, refusal: message.error };
  ```

  The error is stored **verbatim**, as one of the nine `ProtocolError` strings. This reducer does
  not map it to a sentence, does not group `UNKNOWN_ROOM` with `ROOM_FULL`, and does not decide
  what a player sees — that is `TASK-030513`'s, one render source away.
- Change the existing `case "RoomJoined":` to clear the refusal as it lands:

  ```ts
  case "RoomJoined":
    return {
      ...state,
      mySeat: message.seat,
      roomCode: message.code,
      refusal: null,
    };
  ```

  A player who fumbles a code, is refused, and then joins the right room must not carry the old
  refusal into the waiting screen. State is the last frame the server sent, and the last frame
  said yes.
- Nothing here reads `Connection.status`. `ADR-0032` §3 rules that connection facts enter through
  the reducer and only through it; `status` is a mutable getter that notifies nobody and no screen
  may ever read it.

## Out of scope

- Rendering any of it — `TASK-030513`.
- `Welcome` growing a field of its own. It still falls through `default` and still returns the
  identical state reference, which `TASK-030501`'s store depends on.
- `VERSION_MISMATCH` deserving a screen of its own. It lands in `refusal` like the rest here;
  the outdated-client notice is not this story's.
- Clearing `refusal` on anything other than `RoomJoined`.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Four `it` blocks
appended after `TASK-030406`'s three.

**One existing assertion moves, and it is this ticket's to move.** `starts with nothing the server
has not sent` compares `initialState()` against a whole-object literal, so it must gain
`refusal: null,` as the last key of that literal. Nothing else in that test changes, and the
other nineteen `it` blocks are untouched — `RoomJoined sets the seat and the room code` asserts
only `mySeat` and `roomCode`, so the new `refusal: null` in that case does not disturb it.

| Test | Proves |
| --- | --- |
| `records the room the server does not know` | `Failure{UNKNOWN_ROOM}` from the initial state leaves `state.refusal` exactly `"UNKNOWN_ROOM"` |
| `records a room that already has a rival in it` | `Failure{ROOM_FULL}` leaves `state.refusal` exactly `"ROOM_FULL"` |
| `a refusal changes nothing a RoomJoined established` | `RoomJoined{code:"ABCDEFGH", seat:0}` then `Failure{ROOM_FULL}` leaves `mySeat` `0` and `roomCode` `"ABCDEFGH"` |
| `a join that lands clears the refusal before it` | `Failure{UNKNOWN_ROOM}` then `RoomJoined{code:"ABCDEFGH", seat:1}` leaves `state.refusal` `null` |

Four tests. Eighty-seven exist, so the suite reports **91**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 91 passed (91)` | the four tests ran and the eighty-seven before them still do |
| the four `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks (the new field must be present in `initialState`), lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Delete the whole `case "Failure":` → `records the room the server does not know` fails with
   `expected null to be 'UNKNOWN_ROOM' // Object.is equality` (and the `ROOM_FULL` test with it).
   Revert.
2. Drop `refusal: null` from the `RoomJoined` case → `a join that lands clears the refusal before
   it` fails with `expected 'UNKNOWN_ROOM' to be null`. Revert.
3. Add `roomCode: null` to the `Failure` case → `a refusal changes nothing a RoomJoined
   established` fails with `expected null to be 'ABCDEFGH' // Object.is equality`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the duel state > records the room the server does not know` passes
- [ ] `the duel state > records a room that already has a rival in it` passes
- [ ] `the duel state > a refusal changes nothing a RoomJoined established` passes
- [ ] `the duel state > a join that lands clears the refusal before it` passes
- [ ] `npm run --silent test` reports `Tests  91 passed (91)`
- [ ] In `duel-state.test.ts` the only pre-existing line that changed is the object literal in
      `starts with nothing the server has not sent`, which gained `refusal: null,` and nothing
      else; no other assertion is edited, removed or weakened
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
