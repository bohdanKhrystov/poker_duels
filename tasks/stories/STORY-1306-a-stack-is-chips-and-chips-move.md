---
id: STORY-1306
title: A stack is chips, and chips move
type: story
status: ready
parent: EPIC-13
module: design
labels: [design, graphics, client, table]
depends_on: [STORY-1305]
---

## Goal

A player sees their stack as chips rather than only as a numeral, and sees chips travel to the pot
when a bet is made and back to a stack when a hand is won.

## Why

`EPIC-13` item 6, and the human's sixth sentence: *"player shoud have visible representation of
their stack in chips; when bet is maid chips going to the pot; when player won chips goin to their
stack; shoud be animated."*

**It has no art to build on.** `design/graphics/` holds the coin, the suits and the wordmark; there
is no chip. `web-client/src/table/chips.ts` is digit grouping (`formatChips`), not a chip.

**It is last among the unblocked table stories on purpose.** It is the largest drawing change in the
epic and it draws the table as it finally stands: after `STORY-1301`'s pot total, `STORY-1302`'s
host-alone state, and the two seat marks. `ADR-0107` §4 hands it a question that only exists once
the pot names the total — *"whether the bet-lines keep standing once `EPIC-13` item 6 makes stacks
and bets drawn chips is that item's card question and the human's eye"* — and the answer is worth
nothing until the number it is about has landed.

## Design notes

- **Two kinds of ticket, and `ADR-0091` §3 splits them.** A chip is **new visual language**, so
  minting it is worked **interactively with the human** — *"taste does not survive a verify block"*
  — exactly as `EPIC-06` worked its graphics. Composing the minted chip into a screen card is an
  ordinary dispatched ticket: `module: design`, `review: light`, `design/README.md`'s conventions.
  The split says which of its tickets are which, and it says so in the ticket, because an
  interactive ticket dispatched to a coder produces a proposal nobody's eye graded.
- **`tokens/tokens.css` is the only place a design value is born** (`design/README.md`). A chip that
  needs a colour, a radius or a size the sheet does not declare mints it there first, and
  `design/check-drift.sh` fails any card naming a `--pd-` the sheet does not declare. The vendored
  copy under `web-client/src/styles/` must move with it or the client job fails.
- **`ADR-0091` §4's fourth client guard applies**: a raw length literal inside a Tailwind arbitrary
  value fails the client job. Chip sizes are tokens or named exemptions, never `-[44px]`.
- **The card draws every state it has, named** (`EPIC-13` *Design first*, `ADR-0091` §2), and merges
  before the implementing ticket is startable. At minimum: a stack **at rest**; a stack **mid-move**
  toward the pot; the pot **receiving**; a stack **receiving** an award; and — because
  `STORY-1302` landed it — the **host-alone table**, where `ADR-0110` §3 forbids any game fact, so
  there are no chips at all and the card says so as a drawn frame rather than as a note.
- **`ADR-0107` §4's open question is this card's**: do the rival's `committed` bet-line and the stack
  numeral keep standing once chips are drawn? The card offers the answer, the human's eye gives it
  (`ADR-0024` §3). **The story does not decide it and no ticket may decide it silently.**
- **The chips state no fact the server did not send.** A pile renders `SeatView`'s stack and the
  pot's own figure; it prints no number of its own. `no-derivation.test.tsx` must stay green, and
  `ADR-0107` §5's narrowing admits **exactly one** named sum — a chip drawing may not become a
  second.
- **Pacing follows `ADR-0102`'s shape**: the client owns the schedule and states no fact the server
  did not send. `ADR-0102` §4's precedent is the shape to copy — **a duration named once, at a
  seam** (`REVEAL_STEP_MS` in `boot.ts`, reaching the store as a parameter), *"a feel number and the
  cheapest thing here to be wrong about"*, with **zero meaning synchronous** so
  `drive-duel.tsx` and the four recorded-frame suites are untouched.
- **The trap `ADR-0102` already hit, named so nobody re-derives it.** Its rejected alternative
  *"Animate the board with CSS and change no state"* failed because *"the store replaces the array a
  millisecond later and kills the animation mid-flight"*. A chip animation keyed off a value the
  store replaces is the same failure. **The escape is bounded: if the chips need a step in
  `ADR-0102` §1's queue — that is, if a fact has to be withheld to make the motion readable — that
  is a `DEC` for the architect and not a wider story.** Decoration over a transition that already
  happened is not that, and needs no ADR.
- **The fit is the card's to prove** (`ADR-0103`). Chips are furniture on a screen that stands
  0.09375 px from the fence (`ADR-0106` §4, `STORY-1215`). If they do not fit at 390 × 664, that
  re-opens `ADR-0103`'s give list rather than being quietly spent.
- **`DEC-124` blocks the implementing tickets, not the card.** This story and `STORY-1303` introduce
  the product's first continuous motion, and nothing merged says what this product does for a player
  whose system asks for reduced motion — see `STORY-1303`'s *Design notes* for the registration. The
  split writes the minting and card tickets `ready` and every client ticket `blocked` on `DEC-124`.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *not yet split — run `/plan-story STORY-1306`* | — |

## Acceptance criteria

- [ ] A chip exists under `design/graphics/`, minted interactively with the human, self-contained
      and naming no `--pd-` token `design/tokens/tokens.css` does not declare
- [ ] A card under `design/screens/` draws every named state — stack at rest, stack mid-move, pot
      receiving, stack receiving an award, and the host-alone table with no chips — with
      `design/check-drift.sh` exiting 0, merged before any implementing ticket is startable
- [ ] The card states, in its own frames, whether the rival's `committed` bet-line and the stack
      numeral survive the chips (`ADR-0107` §4), and the human's verdict is recorded
- [ ] The rendered table draws chips against a seat's stack and the pot's figure and **prints no
      number the view does not carry** — `no-derivation.test.tsx` green, with its admitted sum still
      exactly one
- [ ] The animation's duration is a named constant reaching the store as a parameter, and at `0` the
      client behaves byte-for-byte as it does today — the four recorded-frame suites unedited and
      `drive-duel.tsx` unchanged
- [ ] No chip renders on the host-alone table (`view === null`)
- [ ] `web-client/src/styles/` holds a byte-identical copy of `design/tokens/tokens.css`, and the
      client's literal guard finds no raw length inside a Tailwind arbitrary value
- [ ] The document still fits at 390 × 664 under `ADR-0103` — `scrollHeight ≤ clientHeight`, read
      and pasted as text

## Out of scope

- **`prefers-reduced-motion` behaviour** — `DEC-124`, the product owner's, registered in
  `STORY-1303`.
- **A step in `ADR-0102` §1's queue.** If the motion needs one, that is an architect's `DEC` and
  this story stops; it is not a wider ticket.
- **The pot's arithmetic.** `STORY-1301` fixed what `Pot` names; this story draws it and does not
  change it.
- **The award line.** `ADR-0095` still replaces the figure at `COMPLETE` and still states only
  received amounts.
- **The engine.** `EPIC-13` *Out of scope* by name: *"chips and marks are drawings."*
- **Sound.** Nothing in the feedback asks for it and the vision does not.
