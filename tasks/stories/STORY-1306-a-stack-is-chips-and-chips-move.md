---
id: STORY-1306
title: A stack is chips, and chips move
type: story
status: done
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

### What the split settled, 2026-09-03 — three notes above are corrected

Written by the planner against `develop`, and each correction is measured or cited rather than
reasoned about. Where these disagree with the notes above, these win.

1. **`DEC-124` is answered and merged.** `ADR-0115` landed on 2026-09-02 in the same PR that
   registered it. **No ticket here is `blocked`** — the chain is one line and `TASK-130601` is the
   single startable ticket.
2. **The flight's duration is a `--pd-motion-*` token in the sheet, not a store parameter.**
   `ADR-0115` §4 is merged and later than the note above: *"a motion value — duration, delay,
   easing, travel distance — is a design value, and `ADR-0024` §2 already says where those are
   born."* §3 draws the line the note was groping for: **a step changes what the screen states;
   motion is how the change is drawn.** A chip flight is a *how*, so it is not a step, it takes no
   place in `ADR-0102` §1's queue, and it needs no seam. The story's criterion is met by a stronger
   route than it guessed: `boot.ts` is not opened, `drive-duel.tsx` is not touched, no frame is
   re-recorded, and the four recorded-frame suites are untouched **by construction** rather than by
   a `0`. `ADR-0102`'s own trap is escaped by its own named escape — decoration over a transition
   that already happened — so no architect's `DEC` is owed.
3. **The chip is minted in `design/tokens/tokens.css` and drawn on the cards, not as an SVG under
   `design/graphics/`.** The coin exists in both forms and the *client* draws it from
   `--pd-coin-face`; the chip follows that half. An SVG would additionally owe `check-drift.sh`
   clause 4's `pd-NAME (#hex)` mirror pairs and a cloud `_ds_manifest.json` entry no ticket can
   make, and nothing in this story needs one.
4. **A pile is never a count.** The client's pile is a fixed three discs at every amount, because a
   pile sized from a figure invents a denomination — *what one chip is worth* — which no server ever
   sent and `docs/vision.md` refuses. The numeral beside it is the whole of the amount. A growing
   pile, if the human's eye wants one, is a repair ticket **and** a product owner's `DEC`.
5. **`ADR-0107` §4 is half-answered by `ADR-0115` §1, and the card asks the other half.** The
   **stack numeral cannot go** — a stack stated only as a drawing is a fact living only in a
   drawing. Only the rival's `committed` bet-line is genuinely open, because its amount is already
   inside `Pot` (`ADR-0107` §1). `TASK-130603` draws the retired alternative as its own frame; the
   client ships the additive half only and retiring the line is a later ticket that exists only if
   the verdict asks.
6. **The blast radius is empty, probed and not estimated.** A silent `aria-hidden` pile planted in
   `SeatPlate`, `PotStrip` and `DuelTable`'s bet line at once left the client suite at **1053 of
   1053** green — so no merged test file is in any ticket's budget. Planting a pile that *spoke* the
   seat's own stack reddened exactly one test (`SeatPlate.test.tsx`'s `speaks the mark to nobody`);
   planting one that spoke a *derived* figure reddened `no-derivation.test.tsx` twice. Both counts
   are gated in the tickets because both are live.
7. **The fourth client guard `ADR-0091` §4 promises does not exist yet.** `color-literals.ts` refuses
   colour literals only; nothing refuses a raw length inside a Tailwind arbitrary value, and
   `DuelTable.tsx` carries several today. The story's criterion is met the other way — chip geometry
   is `var(--pd-chip-size)` and no literal length is written — and gated by requiring the token names
   in `app.css` and in the built bundle. Building the guard is not this story's.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-130601](../tasks/TASK-130601-mint-the-chip-and-draw-the-pile-at-rest-in-flight-and-stilled.md) | Mint the chip, its flight values, and the pile at rest, in flight and stilled | ready |
| [TASK-130602](../tasks/TASK-130602-the-two-table-cards-carry-the-pile-and-the-host-alone-frames-carry-none.md) | The two table cards carry the pile, and the host-alone frames carry none | backlog |
| [TASK-130603](../tasks/TASK-130603-the-card-asks-whether-the-bet-line-survives-the-chips.md) | The card asks whether the bet-line survives the chips | backlog |
| [TASK-130604](../tasks/TASK-130604-a-pile-of-chips-is-a-drawing-and-it-says-nothing.md) | A pile of chips is a drawing, it arrives, and it says nothing | backlog |
| [TASK-130605](../tasks/TASK-130605-a-seats-stack-is-drawn-in-chips-and-the-numeral-still-says-how-many.md) | A seat's stack is drawn in chips, and the numeral still says how many | backlog |
| [TASK-130606](../tasks/TASK-130606-the-pot-is-drawn-in-chips-and-the-figure-still-names-the-total.md) | The pot is drawn in chips, and the award line takes them away again | backlog |
| [TASK-130607](../tasks/TASK-130607-a-bets-chips-stand-at-the-bet-line-and-there-is-still-only-one.md) | A bet's chips stand at the rival's bet line, and there is still only one | backlog |
| [TASK-130608](../tasks/TASK-130608-no-chip-before-the-server-has-named-a-stack.md) | No chip before the server has named a stack | backlog |

## Acceptance criteria

- [ ] The chip exists as minted design vocabulary — `--pd-chip-*` and `--pd-motion-chip-*` born in
      `design/tokens/tokens.css`, worked with the human, and no card names a `--pd-` the sheet does
      not declare (`TASK-130601`; the `design/graphics/` form is corrected in *What the split
      settled* §3)
- [ ] The cards draw every named state — stack at rest, the same pile at a hundredth of the stack,
      stack mid-flight, stack **stilled**, a busted seat with none, pot at rest, pot receiving, a
      stack receiving an award, and the host-alone table with no chips at all — with
      `design/check-drift.sh` exiting 0, merged before the ticket that implements them is startable
- [ ] The card states, in its own frames, whether the rival's `committed` bet-line survives the
      chips (`ADR-0107` §4), with the stack numeral standing in both because `ADR-0115` §1 requires
      it; the human's verdict may trail the merge and acting on it is a later ticket
- [ ] The rendered table draws chips against a seat's stack, the pot's figure and the rival's bet
      line, and **prints no number the view does not carry** — `no-derivation.test.tsx` green, with
      its admitted sum still exactly one
- [ ] **The flight is decoration over a change that has already happened**: an `animation` that runs
      once from an offset **to** the resting position, its duration a `--pd-motion-*` token
      (`ADR-0115` §4), nothing animated out, `boot.ts` and `drive-duel.tsx` unopened and the four
      recorded-frame suites unedited
- [ ] Every amount is legible with motion stopped — the stack numerals, `Pot N` and `committed N`
      all stand, and `prefers-reduced-motion: reduce` needs no new rule because the sheet's one
      merged block already stills `animation` and `transition` product-wide
- [ ] No chip renders on the host-alone table (`view === null`), and no pile speaks an
      `aria-label` or a `title` once one does
- [ ] `web-client/src/styles/` holds a byte-identical copy of `design/tokens/tokens.css`, and chip
      geometry is `var(--pd-chip-size)` rather than a raw length in a Tailwind arbitrary value
- [ ] The document still fits at 390 × 664 under `ADR-0103` — every pile rides inside a row that
      already exists, so no box grows; stated in the PR, never gated (`ADR-0089` §2b)

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
