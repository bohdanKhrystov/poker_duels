---
schema: 2
id: TASK-030920
title: From a resumed socket's frames, the way back forgets the room
type: task
status: backlog
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, store, tests]
depends_on: [TASK-030919]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +576 passed \(576\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'forgets the room when the player leaves the result screen'
  - cd web-client && grep -qF 'forgetRoom={client.forgetRoom}' src/store/reconnect.test.tsx
  - cd web-client && npm run check
---

## Goal

The whole seam is one assertion: a tab whose socket reopened onto a finished duel is holding the
room code, and pressing the way back removes it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/reconnect.test.tsx` | modify — the render helper gains one prop, one test added |
| `docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md` | read — §9's last paragraph, the end-to-end assertion this ticket owes |

## Scope

- **No production file changes.** `TASK-030915`–`TASK-030919` built every piece; this asserts them
  joined up.
- `renderDuelScreen` gains `forgetRoom={client.forgetRoom}` on the `DuelProvider` it renders. Its
  KDoc already says it wires the screen *exactly as `main.tsx` wires the app*, and this is what
  keeps that true. The four existing tests in the file are unaffected.
- Two imports grow: `fireEvent` from `@testing-library/react` and `readRoomCode` from `../protocol`.
- Why this file: `ADR-0072` §4's prop is **optional**, so a `main.tsx` that omitted it would compile
  and silently ship the defect the ADR exists to remove; §9 answers that with one end-to-end
  assertion *from the frames a resumed socket delivers*, and this is the only harness that boots a
  real client over the real retry loop and mounts the real `Lobby` over it.

## Out of scope

- `main.tsx` itself, which no test can read (`ADR-0032`). `TASK-030918`'s grep is its only gate, and
  `ADR-0072` says in Consequences that this is bought deliberately.
- A second tab, and the adoption fight `ADR-0018` settles. Named in the ADR's Consequences as a
  window this widens; not asserted here and not fixed here.
- Any new production behaviour. If this test does not pass with the five tickets above merged, the
  bug is in one of them.

## Tests

`web-client/src/store/reconnect.test.tsx`, describe block `"a tab whose socket dropped"`. One added,
beside `shows the duel that ended while the socket was down`, whose frames it reuses.

| Test | Proves |
| --- | --- |
| `forgets the room when the player leaves the result screen` | first socket: `Welcome`, `RoomJoined(ABCDEFGH, seat 0)`, a `Snapshot`, then closed; 250 ms of virtual time; second socket: `Welcome`, `RoomJoined(ABCDEFGH, seat 0)`, `DuelFinished` ⇒ `readRoomCode(storage)` is `"ABCDEFGH"`; one click on `Back to the lobby` ⇒ it is `null` |

Both observations are load-bearing and neither can be dropped. Without the first, a client that had
never written the code — or that forgot it on `DuelFinished`, which is the behaviour
`TASK-030919` reversed — passes the second and reads as proof of a forget that never happened.

The two sockets are not decoration either: this is the case `ADR-0044` §5 and `ADR-0072` exist for —
the tab's socket dropped, a new one rejoined, the duel had ended meanwhile — and it is exactly the
run that was impossible before this work.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 576 passed (576)` | one added to 575 |
| the `--reporter=verbose` grep | the name exists |
| `grep -qF 'forgetRoom={client.forgetRoom}' src/store/reconnect.test.tsx` | the harness mirrors `main.tsx`, which is the whole point of the assertion |

**Name the edit that makes the assertion red:**

1. Drop `forgetRoom={client.forgetRoom}` from `renderDuelScreen` → the test fails on the second
   assertion, because `useForgetRoom()` returns the no-op. Revert. This is the defect the optional
   prop makes possible, caught here and nowhere else.
2. Delete the first assertion and re-run → still green, which is why it is written down: quote both
   in the PR and say which of the two failures each one catches.

## Acceptance criteria

- [ ] `a tab whose socket dropped > forgets the room when the player leaves the result screen` passes
- [ ] The test asserts `readRoomCode(storage)` **both** before and after the click
- [ ] `renderDuelScreen` passes `forgetRoom={client.forgetRoom}`
- [ ] No production file under `web-client/src` differs from `develop`
- [ ] Every pre-existing `it` block in `reconnect.test.tsx` is unchanged from `develop`
- [ ] `npm run --silent test` reports `Tests  576 passed (576)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
