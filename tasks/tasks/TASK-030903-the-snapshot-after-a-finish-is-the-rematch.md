---
schema: 2
id: TASK-030903
title: The snapshot after a finish is the rematch, and clears the duel that ended
type: task
status: done
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, store, rooms]
depends_on: [TASK-030902]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +540 passed \(540\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'the snapshot after a finish clears the result it replaces'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'the snapshot after a finish clears the offers that started it'
  - cd web-client && npm run check
---

## Goal

A `Snapshot` arriving on a finished duel is the rematch's first frame, so it clears the result and
the offers that produced it — the one thing standing between the store and a table that never
comes back.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `web-client/src/lobby/Lobby.tsx` | read — the `state.outcome !== null` branch this unblocks sits **above** `state.view` |

## Scope

- `case "Snapshot"` gains `outcome: null` and `rematchOffers: []`, beside the `pendingTurn`,
  `rejection` and `refusal` it already clears.
- One comment: `ADR-0044` §4 — there is no started frame, and after a `DuelFinished` a `Snapshot`
  can only mean a new duel has begun in the same room, because `resumeFrames` gives a finished duel
  `finishedFrames` alone. `Lobby.tsx` tests `state.outcome` **before** `state.view`, so a result
  that is never cleared is a table that never returns.

## Out of scope

- `narration`. Nothing under `web-client/src` reads it — checked — so clearing it changes no screen
  and would only churn a field. Not ticketed.
- `mySeat` and `roomCode`, which a rematch does not change: the same room, the same seats.
- Returning the table on screen. That is `TASK-030912`, which asserts it end to end.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Two added.

| Test | Proves |
| --- | --- |
| `the snapshot after a finish clears the result it replaces` | `DuelFinished` then `Snapshot` leaves `outcome` `null` and `view` equal to the snapshot's view — the result is gone and the table's state is the new duel's |
| `the snapshot after a finish clears the offers that started it` | `DuelFinished`, `RematchOffered(1)`, `Snapshot` leaves `rematchOffers` equal to `[]` and `outcome` `null` |

## Proof

| Command | Proves |
| --- | --- |
| `Tests 540 passed (540)` | two added to 538 |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | the whole client suite still passes — **measured while splitting this story**: with `outcome: null` and `rematchOffers: []` added to this case, no pre-existing test in any of the 75 files changes colour |

**Name the edit that makes each assertion red:**

1. Remove `outcome: null` → `the snapshot after a finish clears the result it replaces` fails.
   Revert.
2. Remove `rematchOffers: []` → `the snapshot after a finish clears the offers that started it`
   fails. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the duel state > the snapshot after a finish clears the result it replaces` passes
- [ ] `the duel state > the snapshot after a finish clears the offers that started it` passes
- [ ] No existing test in `duel-state.test.ts` differs from `develop`
- [ ] `npm run --silent test` reports `Tests  540 passed (540)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
