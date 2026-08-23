---
schema: 2
id: TASK-030914
title: A gone room ends the rematch, and a transient refusal leaves it live
type: task
status: done
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, lobby, tests]
depends_on: [TASK-030913]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +566 passed \(566\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the room is gone, and keeps the way back'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the rematch live when the room cannot take one yet'
  - cd web-client && npm run check
---

## Goal

The story's two refusals, asserted from the frame to the pixel: one ends the rematch and says so,
the other changes nothing at all.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two tests added |
| `web-client/src/result/RematchControl.tsx` | read — the two branches these frames reach |

## Scope

- **No production file changes.** `TASK-030904` made the reducer drop `REMATCH_UNAVAILABLE`,
  `TASK-030909` made `UNKNOWN_ROOM` retire the control, and `TASK-030910` connected `state.refusal`
  to the prop. This ticket asserts the whole path, which no single one of them does.
- Both tests press the button first, so the refusal is answering something.

## Out of scope

- Navigating to the lobby on the player's behalf. `ADR-0044` §6 is *the client says so and offers
  the way back*, and `DuelResult`'s `Back to the lobby` link is that way back — the first test
  asserts it is still there rather than that the screen changed itself.
- Re-sending after `REMATCH_UNAVAILABLE`. The control stays live; the player presses it again or
  does not.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Two added.

Both start identically — `RoomJoined(seat 1)`, `DuelFinished`, one click on `Rematch` — and differ
only in the `Failure` that follows, so the pair proves the client distinguishes the two values
rather than treating every refusal alike.

| Test | Proves |
| --- | --- |
| `says the room is gone, and keeps the way back` | `Failure(UNKNOWN_ROOM)` ⇒ `That duel room is gone.` is on screen, no `Rematch` button remains, and the `Back to the lobby` link is still there with `href="/"` |
| `leaves the rematch live when the room cannot take one yet` | `Failure(REMATCH_UNAVAILABLE)` ⇒ the `Rematch` button is still on screen and enabled, `That duel room is gone.` is absent, and no refusal text of any kind appears; a second click then leaves `send` with `toHaveBeenCalledTimes(2)` |

The second test's second click is the transient claim: nothing was recorded, so the same offer may
be sent again.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 566 passed (566)` | two added to 564 |
| the two `--reporter=verbose` greps | both names exist |

**Name the edit that makes each assertion red:**

1. Delete the reducer's `REMATCH_UNAVAILABLE` guard **and** broaden `RematchControl`'s check to
   `props.refusal !== null` → `leaves the rematch live when the room cannot take one yet` fails on
   the vanished button. Revert both.

**Corrected at landing.** This section first listed those two edits as *independently* fatal to that
test. Neither is: run separately, the test still passes. The property is defended twice over — the
reducer never lets `REMATCH_UNAVAILABLE` reach the control, and the control retires only on
`UNKNOWN_ROOM` — so either guard alone keeps the button live, and only removing both makes it
disappear. Confirmed by running all three combinations, by the coder and again in review.

A test needing two simultaneous mutations reads like a test nothing can break, and is its opposite:
it guards a property two independent layers protect.

Quote both in the PR.

## Acceptance criteria

- [ ] `the lobby > says the room is gone, and keeps the way back` passes
- [ ] `the lobby > leaves the rematch live when the room cannot take one yet` passes
- [ ] Both tests click the `Rematch` button before applying their `Failure` frame
- [ ] No production file under `web-client/src` differs from `develop`
- [ ] `npm run --silent test` reports `Tests  566 passed (566)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
