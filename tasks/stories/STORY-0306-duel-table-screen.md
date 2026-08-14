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

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0306`.* | — |

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
