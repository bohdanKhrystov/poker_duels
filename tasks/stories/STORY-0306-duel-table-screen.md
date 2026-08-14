---
id: STORY-0306
title: The duel table renders a PlayerView
type: story
status: backlog
parent: EPIC-03
module: web-client
labels: [client, ui, duel]
depends_on: [STORY-0305]
---

## Goal

The table screen: board, pot, both stacks, blinds and button, your two hole cards face-up and your
opponent's face-down until the server reveals them. Everything on it comes from the last `Snapshot`.

## Why

It is the game. Every other screen in the epic exists to get a player to this one or to tell them
what happened on it.

## Design notes

- Everything drawn comes from `Snapshot.view`: `board.cards`, `pot`, `street`, `buttonSeat`,
  `smallBlind`, `bigBlind`, and per seat `stack`, `committedThisStreet`, `hasFolded`, `isAllIn`,
  `holeCards`. Nothing is derived: the street is `view.street`, never inferred from how many board
  cards there are, because those two disagree at exactly the moments that matter.
- A card is the two-character string the engine writes — rank char then suit char, `"As"`, `"Td"`,
  `"2c"`. The client splits it **for display only**: the suit character picks `--pd-suit-red` or
  `--pd-suit-black` and the glyph. It attaches no ordering, no value and no comparison to it.
- **An empty `holeCards` means "not entitled to see", and renders as a card back**
  (`--pd-card-back`). It is not "no cards" and never renders as a gap — a seat that folded is shown
  by `hasFolded`, and a seat that is all in by `isAllIn`. Rendering absence as absence is how a
  client accidentally tells its player something.
- A reveal arrives as a `Snapshot` in which the opponent's `holeCards` is populated — the engine's
  projection decides that, having seen `HandRevealed`. The table renders whichever cards the
  snapshot contains and never asks why.
- Visual values come from the token layer (`STORY-0302`) and the table design of `EPIC-06`'s
  `STORY-0602` where it is merged. Nothing new is invented here; a value the design system lacks is
  an `EPIC-06` ticket. The cards are the brightest thing on the screen (`STORY-0601`).
- Layout follows the design's responsive behaviour. No separate mobile build, no second component
  tree.
- No animation, no sound, no chip movement in this story. The event log exists in the store and
  drives nothing yet.

## Tasks

Split into schema-2 tickets on 2026-08-15, against the merged design
(`design/screens/duel-table.html`, `design/screens/duel-table-states.html`) and `ADR-0032`. Strictly
ordered: every ticket after the first touches a file an earlier one wrote, so exactly one is
startable at a time. The whole screen lands in `web-client/src/table/`, takes its `PlayerView` as a
prop, and reaches the store only in the last ticket.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-030601](../tasks/TASK-030601-a-chip-amount-is-grouped-the-same-way-wherever-it-runs.md) | A chip amount is grouped the same way wherever it runs | done |
| [TASK-030602](../tasks/TASK-030602-a-card-string-splits-into-a-rank-character-and-a-suit-glyph.md) | A card string splits into a rank character and a suit glyph | ready |
| [TASK-030603](../tasks/TASK-030603-a-card-says-its-name-aloud-and-carries-no-number.md) | A card says its name aloud, and carries no number | backlog |
| [TASK-030604](../tasks/TASK-030604-a-card-back-and-an-undealt-board-place.md) | A card back and an undealt board place | backlog |
| [TASK-030605](../tasks/TASK-030605-a-face-up-card-draws-its-rank-its-suit-and-the-suits-colour.md) | A face-up card draws its rank, its suit and the suit's colour | backlog |
| [TASK-030606](../tasks/TASK-030606-a-hand-is-two-places-wide-whatever-the-view-carries.md) | A hand is two places wide, whatever the view carries | backlog |
| [TASK-030607](../tasks/TASK-030607-the-board-is-five-places-wide-whatever-the-street.md) | The board is five places wide, whatever the street | backlog |
| [TASK-030608](../tasks/TASK-030608-a-playerview-fixture-with-every-field-the-wire-declares.md) | A PlayerView fixture with every field the wire declares | backlog |
| [TASK-030609](../tasks/TASK-030609-the-pot-strip-states-the-pot-the-blinds-the-hand-and-the-street.md) | The pot strip states the pot, the blinds, the hand and the street | backlog |
| [TASK-030610](../tasks/TASK-030610-a-seats-status-is-read-off-the-view-never-off-its-cards.md) | A seat's status is read off the view, never off its cards | backlog |
| [TASK-030611](../tasks/TASK-030611-the-seat-plate-shows-the-name-the-button-and-the-stack.md) | The seat plate shows the name, the button and the stack | backlog |
| [TASK-030612](../tasks/TASK-030612-the-duel-table-seats-the-views-two-players-around-the-board.md) | The duel table seats the view's two players around the board | backlog |
| [TASK-030613](../tasks/TASK-030613-your-hand-is-face-up-and-your-rivals-is-face-down.md) | Your hand is face up and your rival's is face down | backlog |
| [TASK-030614](../tasks/TASK-030614-the-reserved-line-states-what-the-rival-has-committed.md) | The reserved line states what the rival has committed this street | backlog |
| [TASK-030615](../tasks/TASK-030615-the-table-shows-no-number-the-view-does-not-carry.md) | The table shows no number the view does not carry | backlog |
| [TASK-030616](../tasks/TASK-030616-the-table-names-no-card-the-view-did-not-send-and-no-hand.md) | The table names no card the view did not send, and no hand | backlog |
| [TASK-030617](../tasks/TASK-030617-the-lobby-hands-the-live-view-to-the-duel-table.md) | The lobby hands the live view to the duel table | backlog |

**Three decisions the split made, recorded here so a reviewer reads them as chosen rather than
found:**

- **The action bar is not in this story at all, not even off.** The design's `.bar.off` keeps five
  sizing chips and a stepper carrying a live raise amount, and every one of those figures would have
  to be worked out from `LegalActions` — which lives in `state.pendingTurn` and belongs to
  `STORY-0307`. That story adds the bar live and off together. Nothing here reserves space for it.
- **The seats are labelled `You` and `Your rival`.** No `PlayerView` field carries a name, and
  `STORY-0311` says plainly that no opponent name is rendered until `ADR-0021` and `DEC-017` land.
  The design's `ImKate` is a mock.
- **The bet line reads `committed 400`, not the design's `bets 400`.** `committedThisStreet` says
  how much is out; it does not say whether a blind, a call, a bet or a raise put it there, and
  "bets" would be the client naming an action the server never sent (`TASK-030614`).

Two smaller notes: the client's card back carries no inset ring, because the design's is a raw
`rgba(...)` literal that `STORY-0302`'s guard forbids outside the token layer and no token expresses
it (`TASK-030604`); and the street is written as a third `·`-separated item in the pot strip's meta
line, which is the smallest way to satisfy this story's third acceptance criterion inside an
existing design slot (`TASK-030609`). Both are one-line changes if `EPIC-06` decides otherwise.

## Acceptance criteria

- [ ] Given a `Snapshot` where the opponent's `holeCards` is empty, the rendered output contains no
      rank and no suit for that seat, and two card backs.
- [ ] Given a `Snapshot` where both seats' `holeCards` are populated, four card faces render.
- [ ] Pot, both stacks, blinds, button seat and street render exactly the values in the view, for a
      snapshot on each of preflop, flop, turn and river.
- [ ] A folded seat and an all-in seat each render their state from `hasFolded` / `isAllIn`, not
      from the presence of cards.
- [ ] No colour literal is added: the client's token check still passes.

## Out of scope

- Acting — `STORY-0307`. The table renders `seatToAct` but offers no buttons.
- The result screen — `STORY-0308`.
- Reconnect behaviour — `STORY-0310`.
- Animation, sound, card-dealing motion, hand history — later.
- Any "opponent is away" indicator — `DEC-018`.
