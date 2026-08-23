---
schema: 2
id: TASK-030912
title: The rematch begins, and the button is on the other side
type: task
status: backlog
parent: STORY-0309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, lobby, tests]
depends_on: [TASK-030911]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +562 passed \(562\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'the snapshot after a finish returns the table with the button on the other side'
  - cd web-client && npm run check
---

## Goal

The story's whole point, asserted end to end from frames: after both seats have offered, the
opening `Snapshot` puts the table back with the button on the other seat and no trace of the result.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify — one test added |
| `web-client/src/table/SeatPlate.tsx` | read — the button marker is `aria-label="the button"` |
| `web-client/src/table/DuelTable.tsx` | read — the plates are named `You` and `Your rival` |

## Scope

- **No production file changes.** Everything this asserts was built by `TASK-030903` (the reducer
  clearing `outcome` on `Snapshot`) and `TASK-030910` (the branch order). This is the assertion
  that says so, which nothing currently makes.
- One test, driving the exact frame sequence the server sends: the duel's opening `Snapshot`, the
  `DuelFinished`, the rival's `RematchOffered`, then the rematch's opening `Snapshot`. There is no
  started frame to wait for (`ADR-0044` §4).

## Out of scope

- Sending the second offer. This client is the one that offered second, and `ADR-0044` §4 sends it
  no `RematchOffered` for its own — the opening frames are its answer. The test applies frames; it
  does not click.
- Stacks, blinds or hand numbers. The table renders what the view carries and this test reads only
  the button.
- Any reset of `narration`.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. One added.

| Test | Proves |
| --- | --- |
| `the snapshot after a finish returns the table with the button on the other side` | see the sequence below |

The sequence, with this client at **seat 0**:

1. `RoomJoined(code, seat 0)`, then the module's existing `SNAPSHOT` fixture (`buttonSeat: 0`).
   Assert `getByLabelText("the button")` sits inside the plate whose text contains `You` and not
   `Your rival` — read with `closest("div")` from the marker.
2. `DuelFinished`, then `RematchOffered(1)`. Assert the region *the result* is on screen.
3. A second `Snapshot`, identical to the fixture except `buttonSeat: 1`. Assert:
   - the region *the result* is `null`;
   - `Pot 30` is on screen — the table is back;
   - `getByLabelText("the button")` now sits inside the plate whose text contains `Your rival`;
   - the screen's text matches `/rematch/i` nowhere.

Two readings of the marker, at two button seats, one duel apart: a table that ignored `buttonSeat`,
or a screen that kept the result up, fails on a different assertion each.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 562 passed (562)` | one added to 561 |
| the `--reporter=verbose` grep | the name exists |

**Name the edit that makes each assertion red:**

1. Remove `outcome: null` from the reducer's `Snapshot` case → the third step's *the result is
   gone* assertion fails, and so does `Pot 30`. Revert.
2. Give the second `Snapshot` `buttonSeat: 0` instead of `1` → the marker never moves and the last
   plate assertion fails; this proves the test is reading `buttonSeat` and not the plate order.
   Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the lobby > the snapshot after a finish returns the table with the button on the other side` passes
- [ ] The test asserts the button marker's position **twice**, once at `buttonSeat: 0` and once at `buttonSeat: 1`
- [ ] No production file under `web-client/src` differs from `develop`
- [ ] Every pre-existing `it` block in `Lobby.test.tsx` is unchanged
- [ ] `npm run --silent test` reports `Tests  562 passed (562)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
