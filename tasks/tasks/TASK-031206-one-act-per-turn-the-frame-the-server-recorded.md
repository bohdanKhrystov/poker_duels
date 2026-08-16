---
schema: 2
id: TASK-031206
title: The client answers each turn through the bar, with the frame the server recorded
type: task
status: ready
parent: STORY-0312
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, test, end-to-end]
depends_on: [TASK-031205]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +370 passed \(370\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends one Act for each YourTurn, and the frame the server recorded'
  - cd web-client && npm run check
---

## Goal

Every `YourTurn` in the script is answered by a click on the real action bar, and every frame the
client sends is the frame the server's own recording says it should be.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/drive-duel.tsx` | modify |
| `web-client/src/e2e/whole-duel.test.tsx` | modify |

Read, do not modify: `web-client/src/table/ActionBar.tsx` (the buttons and the amount control),
`web-client/src/table/action-text.ts` (the verb each `ActionType` prints),
`web-client/src/table/act-frame.ts` (the frame a click builds).

## Scope

- `driveScriptedDuel` stops skipping `"client"` steps. For each one it performs the recorded action
  **through the screen**, never by calling `send`:
  - the button is found by the verb `actionVerb` prints for the recorded action's discriminator —
    `Fold`, `Check`, `Call`, `Bet`, `Raise to`, `All in` — matched as an anchored prefix of the
    button's accessible name, because `Call`, `All in` and both amount buttons carry a figure after
    the verb;
  - when the recorded action carries a `to` (`Bet`, `Raise`), the amount control is set first with
    `fireEvent.change(getByRole("slider"), { target: { value: String(action.to) } })`, then the
    button is clicked. The recorded totals are the server's own `minBetTo`, `minRaiseTo` or
    `allInTo`, so every one of them is inside the `min`/`max` the bar took off the same `YourTurn`;
  - the click is inside `act(...)`, like every other step.
- If no such button is on screen, throw naming the step index, the hand number and the accessible
  names that *were* on screen. A driver that silently skipped a turn would make every assertion below
  pass by doing nothing.
- Nothing else about the driver changes: the same wiring, the same `onStep`, still no timer.

## The test this ticket takes over

`sends the handshake and nothing more, because nothing asked it to act` (`TASK-031205`) asserts
`sent` is exactly two frames. This ticket makes the client act, so that assertion cannot stand and is
**replaced** — not weakened — by `sends the handshake before it acts, and never after the duel ends`
below, which keeps the handshake claim and adds the two the acting introduces. It is the only test in
`whole-duel.test.tsx` that changes: `plays every frame of the script and ends on the result screen`
and `is on the table, not the lobby, while the duel is running` keep their bodies exactly.

## Out of scope

- The result screen's numbers — `TASK-031207`.
- Cards — `TASK-031208`, `TASK-031209`.
- Any change to `ActionBar.tsx`, `act-frame.ts` or `action-text.ts`. If a test here is red because of
  a defect in one of them, report it and file a ticket; fix it here only if the fix is one line.
- Rejections and refusals. The recorded policy answers only what it was asked, so the script carries
  no `Rejected` — `ADR-0043`'s behaviour is `STORY-0307`'s and is already tested there.

## Tests

`web-client/src/e2e/whole-duel.test.tsx`, same describe block. Every test runs over **both** seats.

| Test | Proves |
| --- | --- |
| `sends one Act for each YourTurn, and the frame the server recorded` | the `Act` frames in `sent`, parsed, deep-equal the parsed `frame` of each `"client"` step, in order and one for one. Compared as parsed objects rather than as strings, so key order is not the claim; compared against the recorded `frame` rather than against a frame rebuilt here, so the oracle is the server's |
| `echoes each turn's own handNumber and actionSequence, and its own seat` | for every `Act` sent, `handNumber` and `actionSequence` equal those of the `YourTurn` step that preceded it, and `action.seat` equals `viewerSeat`; and across the run more than one distinct `handNumber` and more than three distinct `actionSequence`s appear, so a client that copied one identity everywhere would fail |
| `sends the handshake before it acts, and never after the duel ends` | the first two frames of `sent` are `Hello` then `JoinRoom{code: roomCode}`, no `Act` precedes them, and the number of frames sent equals `2 + the number of "client" steps` — so nothing is sent after the `DuelFinished` and nothing is sent twice |

Three tests added, one replaced. Three hundred and sixty-seven exist, so the suite reports **370**.

## Proof

**Name the edit that makes each assertion red** — run each, quote it in the PR, revert:

1. In `act-frame.ts`, read `handNumber` off the view instead of the turn → `sends one Act for each
   YourTurn, and the frame the server recorded` and `echoes each turn's own handNumber and
   actionSequence, and its own seat` both fail.
2. In `ActionBar.tsx`, drop the `sent` lock so a click can fire twice, and click twice in the driver
   → `sends the handshake before it acts, and never after the duel ends` fails on the count.
3. In the driver, skip a `"client"` step whose action is a `Fold` → `sends one Act for each YourTurn,
   and the frame the server recorded` fails, which is what the "throw if the button is missing" rule
   is there to guarantee.

## Acceptance criteria

- [ ] `a whole duel through the client > sends one Act for each YourTurn, and the frame the server recorded` passes
- [ ] `a whole duel through the client > echoes each turn's own handNumber and actionSequence, and its own seat` passes
- [ ] `a whole duel through the client > sends the handshake before it acts, and never after the duel ends` passes
- [ ] `plays every frame of the script and ends on the result screen` and `is on the table, not the
      lobby, while the duel is running` pass with their bodies unchanged
- [ ] The driver reaches the bar through `fireEvent` on a button and a slider, and never calls
      `client.send`, `actFrame` or `socket.send` directly
- [ ] The driver throws, naming the step, when the recorded action's button is not on screen
- [ ] No production file differs
- [ ] `npm run --silent test` reports `Tests  370 passed (370)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
