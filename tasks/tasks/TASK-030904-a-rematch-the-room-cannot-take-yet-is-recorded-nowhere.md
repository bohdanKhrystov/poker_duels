---
schema: 2
id: TASK-030904
title: A rematch the room cannot take yet is recorded nowhere
type: task
status: ready
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, store, rooms]
depends_on: [TASK-030903]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +542 passed \(542\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rematch the room cannot take yet enters no state'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'every other refusal is still recorded'
  - cd web-client && npm run check
---

## Goal

`Failure(REMATCH_UNAVAILABLE)` leaves the store exactly as it found it, because the server recorded
nothing and the same offer may be sent again.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `web-client/src/protocol/protocol.gen.ts` | read — `ProtocolError` now carries `REMATCH_UNAVAILABLE` |

## Scope

- `case "Failure"` gains one guard ahead of what it already does:

  ```ts
  case "Failure":
    // ADR-0044 §6 documents REMATCH_UNAVAILABLE as transient: nothing was recorded and the
    // same offer may be sent again, so there is no state to enter and no screen to change.
    if (message.error === "REMATCH_UNAVAILABLE") return state;
    return { ...state, refusal: message.error };
  ```

- Exactly one value is named. Every other `ProtocolError` keeps the behaviour it has on `develop`,
  including `UNKNOWN_ROOM`, which `TASK-030909` turns into the frame that ends a rematch.

## Out of scope

- Rendering either refusal — `TASK-030909` (the control) and `TASK-030914` (the screen).
- `UNKNOWN_ROOM` clearing `outcome`, `view` or `roomCode`. `ADR-0044` §6 says the client *says so
  and offers the way back to the lobby*; it does not navigate on the player's behalf, and the
  reducer keeps its rule that a refusal changes nothing a frame established. Not ticketed.
- Retrying. The client never re-sends on the player's behalf.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Two added.

Both start from the same state — a `DuelFinished` applied, then `RematchOffered(1)` — so the pair
distinguishes *this one error value* from the branch, rather than proving the vaguer claim that
`Failure` sometimes does nothing.

| Test | Proves |
| --- | --- |
| `a rematch the room cannot take yet enters no state` | `Failure(REMATCH_UNAVAILABLE)` returns the **same object** (`toBe`) it was given, so `refusal` stays `null`, the standing offer stays `[1]`, and `duel-store.ts` notifies nobody |
| `every other refusal is still recorded` | `Failure(ROOM_FULL)` on that same state returns a **different** object whose `refusal` is `"ROOM_FULL"` and whose `rematchOffers` is still `[1]` |

## Proof

| Command | Proves |
| --- | --- |
| `Tests 542 passed (542)` | two added to 540 |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | typechecks: `REMATCH_UNAVAILABLE` is a member of the generated `ProtocolError` union, so a typo in the string fails the build rather than the test |

**Name the edit that makes each assertion red:**

1. Delete the guard → `a rematch the room cannot take yet enters no state` fails on the `toBe`.
   Revert.
2. Widen the guard to `return state` for every error → `every other refusal is still recorded`
   fails. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the duel state > a rematch the room cannot take yet enters no state` passes
- [ ] `the duel state > every other refusal is still recorded` passes
- [ ] The first test asserts object identity with `toBe`, not a field-by-field comparison
- [ ] No existing test in `duel-state.test.ts` differs from `develop`
- [ ] `npm run --silent test` reports `Tests  542 passed (542)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
