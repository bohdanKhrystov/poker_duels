---
schema: 2
id: TASK-030902
title: A finished duel begins the result screen with no offer standing
type: task
status: done
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, store, rooms]
depends_on: [TASK-030901]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +538 passed \(538\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'an offer that arrived before the finish does not reach the result screen'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'an offer that arrives after the finish stands'
  - cd web-client && npm run check
---

## Goal

`DuelFinished` is where the client enters its result screen, so it starts that screen with nothing
recorded: an offer a client was holding when the duel ended does not survive into it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |

## Scope

- `case "DuelFinished"` gains `rematchOffers: []`, beside the `pendingTurn`, `rejection` and
  `refusal` it already clears.
- One comment, saying *why* rather than *what*: `ADR-0044` §5 makes the server restate a standing
  offer **after** a returning socket's `DuelFinished` and never before, precisely because this
  frame is where the client enters its result screen. Clearing here is the client half of that
  commitment — without it, the ordering the server took on buys nothing.

## Out of scope

- `Snapshot` — `TASK-030903`.
- Any other field. `outcome`, `view` and `narration` behave exactly as they do on `develop`.
- The screen. Nothing renders `rematchOffers` yet.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Two added.

The claim here is about **order**, not about presence, so both tests apply the *same two frames*
and differ only in which comes first. A reducer that recorded offers and never cleared them passes
one and fails the other; a reducer that ignored `RematchOffered` fails the other one.

| Test | Proves |
| --- | --- |
| `an offer that arrived before the finish does not reach the result screen` | `RematchOffered(1)` then `DuelFinished` leaves `rematchOffers` equal to `[]`, while `outcome` is the outcome that frame carried |
| `an offer that arrives after the finish stands` | `DuelFinished` then `RematchOffered(1)` — the same two frames the other way round — leaves `rematchOffers` equal to `[1]`, and `outcome` unchanged |

Both use **seat 1**, so neither can be satisfied by a reducer that only ever handles seat 0.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 538 passed (538)` | two added to 536 |
| the two `--reporter=verbose` greps | both names exist |

**Name the edit that makes each assertion red:**

1. Remove `rematchOffers: []` from the `DuelFinished` case → `an offer that arrived before the
   finish does not reach the result screen` fails with `[1]` against `[]`. Revert.
2. Make `RematchOffered` return `state` unconditionally → `an offer that arrives after the finish
   stands` fails. Revert.

Quote both in the PR, and say in the PR body that the two tests differ only in frame order.

## Acceptance criteria

- [ ] `the duel state > an offer that arrived before the finish does not reach the result screen` passes
- [ ] `the duel state > an offer that arrives after the finish stands` passes
- [ ] The two new tests apply the same two frames and differ only in the order they apply them
- [ ] No existing test in `duel-state.test.ts` differs from `develop`
- [ ] `npm run --silent test` reports `Tests  538 passed (538)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
