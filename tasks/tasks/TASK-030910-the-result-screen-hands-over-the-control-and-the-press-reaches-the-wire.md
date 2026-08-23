---
schema: 2
id: TASK-030910
title: The result screen hands over the control, and the press reaches the wire
type: task
status: done
parent: STORY-0309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, lobby, result, ui]
depends_on: [TASK-030909]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +561 passed \(561\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends one OfferRematch when the rematch is pressed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows your own offer only once the server states it'
  - cd web-client && npm run check
---

## Goal

The rematch is on the screen and the press is on the wire: pressing it sends `OfferRematch`, and
the screen changes only when the server's own frame comes back.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one import, one prop on the existing branch |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two added |
| `web-client/src/result/RematchControl.tsx` | read — the props |

## Scope

- One import, in the order `npm run format` leaves it:
  `import { RematchControl } from "../result/RematchControl";`
- The existing `state.outcome !== null` branch keeps its comment and its position above
  `state.view`, and gains one prop:

  ```tsx
  return (
    <DuelResult
      outcome={state.outcome}
      mySeat={state.mySeat}
      rematch={
        <RematchControl
          offers={state.rematchOffers}
          mySeat={state.mySeat}
          refusal={state.refusal}
          onOffer={() => send({ type: "OfferRematch" })}
        />
      }
    />
  );
  ```

- `mySeat` is `state.mySeat`, never a literal. `offers` is `state.rematchOffers`, never derived.
- Nothing else in `Lobby.tsx` moves: no hook is added, the branch order is unchanged, and the
  screen still sends only from event handlers (`ADR-0032`).

## Out of scope

- The rematch beginning — `TASK-030912`.
- A restated offer after a rejoin — `TASK-030913`.
- Refusals on the screen — `TASK-030914`.
- Any optimism. Nothing is applied on click; the chip appears because `RematchOffered` arrived.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Two added.

Both seat this client at **1**, so a screen passing a literal `mySeat={0}` reads its own offer as
its rival's and fails.

| Test | Proves |
| --- | --- |
| `sends one OfferRematch when the rematch is pressed` | after `RoomJoined(seat 1)` and a `DuelFinished`, one click on `Rematch` leaves `send` with `toHaveBeenCalledTimes(1)` and `toHaveBeenCalledWith({ type: "OfferRematch" })` — no code, no seat, no duel on the frame |
| `shows your own offer only once the server states it` | immediately after that click the `Rematch` button is **still** on screen and the chip is absent; then `store.apply({ type: "RematchOffered", seat: 1 })` and the chip `Rematch offered — waiting for your rival` is on screen with the button gone |

The second is the epic's rule made executable: the two observations are one click apart, so a
screen that set its own state optimistically passes the first half and fails the second's
*before* assertion.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 561 passed (561)` | two added to 559 |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | `state.rematchOffers` and `state.refusal` typecheck against the control's props |

**Name the edit that makes each assertion red:**

1. Set local state on click and render the chip from it → `shows your own offer only once the
   server states it` fails on the assertion made *before* the frame arrives. Revert.
2. Pass `mySeat={0}` as a literal → `shows your own offer only once the server states it` fails,
   because this client sat at seat 1 and the chip becomes the rival line. Revert.

Quote both in the PR, and say in the PR body that the `outcome` branch still precedes the `view`
branch.

## Acceptance criteria

- [ ] `the lobby > sends one OfferRematch when the rematch is pressed` passes
- [ ] `the lobby > shows your own offer only once the server states it` passes
- [ ] `Lobby.tsx` still contains no `useEffect` and no `useRef`
- [ ] In `Lobby.tsx`, `if (state.outcome !== null)` still appears before `if (state.view !== null)`, which still appears before `if (state.roomCode !== null)`
- [ ] Every pre-existing `it` block in `Lobby.test.tsx` is unchanged from `develop`
- [ ] `npm run --silent test` reports `Tests  561 passed (561)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
