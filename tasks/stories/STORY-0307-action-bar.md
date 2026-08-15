---
id: STORY-0307
title: The action bar — acting on your turn
type: story
status: ready
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
  retry — what else it shows was `DEC-018`, since answered by
  [`ADR-0028`](../../docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md), but nothing that
  answer adds is on protocol version 2, so today it still shows only that the action did not land.
- No timer. Nothing on the wire carries a clock, and inventing a countdown would be the client
  asserting a fact about a duel.

## Tasks

Split into schema-2 tickets on 2026-08-15, against the merged design
(`design/components/action-bar.html`, `design/screens/duel-table.html` and `duel-table-states.html`)
and the engine's own `BettingRules`. Strictly ordered: every ticket after the first touches a file
an earlier one wrote, so exactly one is startable at a time. The bar lands in
`web-client/src/table/`, beside the table it sits under, and reaches the store only in the last
ticket.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-030701](../tasks/TASK-030701-a-turn-fixture-with-every-field-the-wire-declares.md) | A turn fixture with every field the wire declares | ready |
| [TASK-030702](../tasks/TASK-030702-each-action-says-its-verb-and-carries-the-servers-figure.md) | Each action says its verb and carries the server's figure | backlog |
| [TASK-030703](../tasks/TASK-030703-the-act-frame-echoes-the-turns-identity-verbatim.md) | The `Act` frame echoes the turn's identity verbatim | backlog |
| [TASK-030704](../tasks/TASK-030704-the-bar-exists-in-every-state-and-waits-in-most.md) | The bar exists in every state, and waits in most of them | backlog |
| [TASK-030705](../tasks/TASK-030705-one-button-per-action-the-server-allowed.md) | One button per action the server allowed, and not one more | backlog |
| [TASK-030706](../tasks/TASK-030706-the-amount-control-is-clamped-to-the-bounds-the-server-sent.md) | The amount control is clamped to the bounds the server sent | backlog |
| [TASK-030707](../tasks/TASK-030707-a-click-sends-one-act-and-the-bar-goes-quiet.md) | A click sends one `Act`, and the bar goes quiet until the next turn | backlog |
| [TASK-030708](../tasks/TASK-030708-a-rejection-reads-from-its-own-fields.md) | A rejection reads from its own fields, in the server's numbers | backlog |
| [TASK-030709](../tasks/TASK-030709-the-bar-states-what-the-server-refused.md) | The bar states what the server refused, and retries nothing | backlog |
| [TASK-030710](../tasks/TASK-030710-the-bar-shows-no-number-and-offers-no-action-the-turn-did-not-carry.md) | The bar shows no number and offers no action the turn did not carry | backlog |
| [TASK-030711](../tasks/TASK-030711-the-duel-screen-puts-the-bar-under-the-table.md) | The duel screen puts the bar under the table and sends what it built | backlog |
| [TASK-030712](../tasks/TASK-030712-after-a-rejection-the-player-can-act-again.md) | After a rejection the player can act again | blocked (`DEC-037`) |

### The one decision the split could not take

**`DEC-037` — after a `Rejected`, is the decision point still open on the client, and when does a
rejection stop being shown?** The architect's. `DuelAction.act` returns only the `Rejected` frame,
so no fresh `YourTurn` follows one; `duel-state.ts` clears `pendingTurn` on `Rejected`
(`TASK-030404` pins it) and never clears `rejection`. Together those mean a rejected action ends the
player's hand and leaves its sentence on screen for the rest of the duel. Three shapes answer it and
they are not equivalent — reducer, component, or a server re-prompt this epic forbids itself — so
this story's **fifth acceptance criterion is unmet until `TASK-030712` unblocks**. Everything else
ships without it.

### Five decisions the split made, recorded as chosen rather than found

- **`ALL_IN` is a fourth button, not a sizing chip.** `BettingRules` adds `ALL_IN` whenever the
  opponent is contestable, so four actions is the ordinary set and three is the exception. The
  design's mocks show three because they draw the shell, not the legal set (`TASK-060203` says so
  itself); the story forbids hiding an action; the actions row is `flex`, so a fourth button narrows
  it and changes nothing else.
- **The aggressive line stays filled, and the all-in is only filled when nothing else is
  aggressive.** `RAISE`, else `BET`, else the last button the server named. That reproduces both of
  the design's states exactly and never makes an accidental stack-in the most prominent target.
- **The amount control is a range input, not the design's `−`/`+` stepper.** A stepper needs an
  increment, and no increment is on the wire — a big blind or a min-raise increment would be the
  client inventing a raising rule. A slider clamped to `[minRaiseTo, allInTo]` (the story's own
  words) reaches both endpoints exactly and needs none. The design's three pot-fraction chips go
  with it: they are the bet presets this story puts out of scope.
- **The off state reserves the sizing row and keeps nothing in it.** The design mirrors the live
  row's content, including the last amount, so the two wrap identically. A stale amount is a figure
  from a decision point that has passed, and `TASK-030710`'s guard would be right to fail it; the
  height is reserved with `min-h-7` instead.
- **"a `Snapshot` arrived between the two"** (second acceptance criterion) is read as *the `Act`
  copies its identity from the turn and never from the view*. The server sends `Snapshot` then
  `YourTurn`, and the store clears `pendingTurn` on a `Snapshot`, so a snapshot mid-turn correctly
  takes the bar off until the resume's `YourTurn` re-opens it. `TASK-030703` asserts the copying;
  nothing asserts an identity surviving a state the store does not keep.

## Acceptance criteria

- [ ] Given `allowed: [FOLD, CALL, RAISE]`, exactly those three controls render and no check button
      appears.
- [ ] The `Act` frame's `handNumber` and `actionSequence` equal the `YourTurn`'s exactly, including
      when a `Snapshot` arrived between the two.
- [ ] A raise sends `Raise{seat, to}` with the total for the street, and a value below `minRaiseTo`
      cannot be submitted from the control.
- [ ] With no pending turn, every control is disabled and no frame is sent by any click.
- [ ] `Rejected{AmountTooSmall}` renders the server's `minimum` and re-enables the bar; a second
      action can then be sent. *(Rendering: `TASK-030709`. Re-enabling: blocked on `DEC-037`,
      `TASK-030712`.)*
- [ ] `Failure{DUEL_PAUSED}` sends nothing further and shows that the action was not applied.

## Out of scope

- Keyboard shortcuts, bet presets (½ pot, pot), a bet-sizing memory — none are on the wire and all
  are product choices nobody has made.
- Turn timers or any countdown.
- The result screen — `STORY-0308`.
- What a player sees while the opponent is away — `DEC-018`.
