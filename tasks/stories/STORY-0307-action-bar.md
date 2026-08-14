---
id: STORY-0307
title: The action bar — acting on your turn
type: story
status: backlog
parent: EPIC-03
module: web-client
labels: [client, ui, duel]
depends_on: [STORY-0306]
---

## Goal

When `YourTurn` arrives the client offers exactly the actions the server sent and no others; a click
sends `Act` echoing that turn's identity verbatim; a `Rejected` is shown as the server worded it and
the bar becomes usable again.

## Why

It is the only place a player ever asserts anything, and therefore the only place `ADR-0002` can be
broken from the client side. It is also the last piece needed to play a hand at all.

## Design notes

- **The buttons are `legalActions.allowed`** — a list of `ActionType` (`FOLD`, `CHECK`, `CALL`,
  `BET`, `RAISE`, `ALL_IN`). The client renders the set it was given: it must not hide an action it
  thinks is bad, add one it thinks is legal, or reorder by cleverness. If `CHECK` is absent, there
  is no check button, and the client does not know why.
- **Amounts are server-sent**: `callTo`, `minBetTo`, `minRaiseTo`, `allInTo`. A slider is clamped to
  `[minRaiseTo, allInTo]` — clamping a control to bounds the server sent is presentation; deriving
  those bounds from stacks and blinds is a rule, and it is forbidden.
- `Act` carries `handNumber` and `actionSequence` copied from the `YourTurn` that opened the turn,
  and a `PlayerAction` whose `seat` is `legalActions.seat`. `Bet.to` and `Raise.to` are the **total
  committed on this street**, not a delta — the field is named `to` for that reason and the server
  will reject a delta as `AmountTooSmall`.
- **Nothing is optimistic.** After sending, controls are disabled until the next `YourTurn`,
  `Rejected` or `Snapshot`. No chip moves, no stack changes, no "you called" text until a `Snapshot`
  says so. An optimistic client is a client that has computed a game fact.
- Each `Rejection` variant renders from its own fields: `ActionNotAllowed{attempted, allowed}`,
  `AmountTooSmall{attempted, minimum}`, `AmountTooLarge{attempted, maximum}`,
  `NotYourTurn{seatToAct}`, `HandComplete`. The client shows the server's numbers and does not
  re-word them into a rule of its own.
- `Failure{NOT_YOUR_TURN | NOT_IN_DUEL | DUEL_PAUSED}` can arrive here too. `DUEL_PAUSED` says
  explicitly: your action was not applied, **do not re-send**. The client shows it and does not
  retry — what else it shows is `DEC-018`, unanswered, so today it shows only that the action did
  not land.
- No timer. Nothing on the wire carries a clock, and inventing a countdown would be the client
  asserting a fact about a duel.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0307`.* | — |

## Acceptance criteria

- [ ] Given `allowed: [FOLD, CALL, RAISE]`, exactly those three controls render and no check button
      appears.
- [ ] The `Act` frame's `handNumber` and `actionSequence` equal the `YourTurn`'s exactly, including
      when a `Snapshot` arrived between the two.
- [ ] A raise sends `Raise{seat, to}` with the total for the street, and a value below `minRaiseTo`
      cannot be submitted from the control.
- [ ] With no pending turn, every control is disabled and no frame is sent by any click.
- [ ] `Rejected{AmountTooSmall}` renders the server's `minimum` and re-enables the bar; a second
      action can then be sent.
- [ ] `Failure{DUEL_PAUSED}` sends nothing further and shows that the action was not applied.

## Out of scope

- Keyboard shortcuts, bet presets (½ pot, pot), a bet-sizing memory — none are on the wire and all
  are product choices nobody has made.
- Turn timers or any countdown.
- The result screen — `STORY-0308`.
- What a player sees while the opponent is away — `DEC-018`.
