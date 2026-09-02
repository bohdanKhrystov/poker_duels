---
id: STORY-1303
title: The acting seat is marked, and the mark moves
type: story
status: ready
parent: EPIC-13
module: web-client
labels: [client, design, table]
depends_on: [STORY-1302]
---

## Goal

A player glancing at the duel table can tell whose turn it is without reading, because the seat on
turn carries a mark and the seat that is not does not.

## Why

`EPIC-13` item 1, and the human's first sentence: *"I want player to be higlighted when is their
turn; it shoud be some animation like pulsing or running circle."* Today the only thing that says
whose turn it is is `seat-status.ts`'s two words, `Your turn` / `Their turn` — correct and mute.
The epic's whole complaint is a table that is right and quiet.

**It is third because the clock sits on it.** `STORY-1307`'s card draws a countdown at the acting
seat; drawing it against a seat that already carries its mark is one composition, drawing it first
and adding the mark afterwards is two. And **the phone budget is spent in order** — `ADR-0103`
froze the fit and listed exhaustively what may give, so each card in this epic proves the fit
*including everything merged before it*, and the earlier a standing element lands the more of the
budget it is measured against.

**No `DEC` decides which drawing.** *Pulsing or running circle* is a choice between two drawings and
the human is the one who looks (`ADR-0024` §3, and `EPIC-13` *Design first*'s closing paragraph).
One question underneath it is not taste and is registered — see below.

## Design notes

- **The card is the first ticket and merges before the implementing ticket is startable**
  (`EPIC-13` *Design first*, `ADR-0091` §2). It owes **both states, drawn, named**: *acting* and
  *waiting*, at both seats — the hero on turn with the rival waiting, and the rival on turn with the
  hero waiting. A card showing one state of a two-state mark leaves the same debt `ADR-0091` §5
  registers, in a smaller shape.
- **The card offers the drawing; the human accepts it.** Pulsing, a running circle, a static ring, a
  plate treatment — the card may offer more than one and the verdict is the human's visual one
  (`ADR-0024` §3), which **may trail the merge**, so an unattended run never stalls at a pane.
- **The mark mints no string.** `Your turn` and `Their turn` already ship in `seat-status.ts` and
  this story neither replaces nor duplicates them without the card saying so. If the card wants a
  word this table does not already render, that is `ADR-0046`'s register and a stop, not an invented
  sentence.
- **It states no fact the server did not send.** The acting seat is the view's own `seatToAct`, the
  same field `seat-status.ts` already reads as `isToAct`; the mark renders that field and nothing
  else, so `no-derivation.test.tsx`'s invariant is true of it by construction.
- **Presence outranks the turn, and the mark inherits that order.** `seat-status.ts`'s comment
  records `ADR-0046` §1: `Away` and `Timed out` are printed instead of `Their turn`, *"because
  `Their turn` on a seat nobody is sitting at blames a pause on thinking"*. The card says what the
  mark does at an `AWAY` or `ABSENT` seat that is nonetheless on turn; it may not contradict that
  order.
- **It must be absent from the host-alone table.** `STORY-1302` lands a table with `view === null`
  and `ADR-0110` §3 forbids any game fact there. There is no acting seat before the opening
  `Snapshot`, so the mark does not render — this story's tests say so explicitly rather than leaving
  it to chance.
- **The fit is the card's to prove** (`ADR-0103`). If the mark cannot be placed at 390 × 664 without
  something giving, that reopens `ADR-0103`'s give list rather than being quietly spent.

### The one thing here that is not taste: `DEC-124` — **answered 2026-09-02**

> **Merged before the split ran.**
> [`ADR-0115`](../../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md)
> resolves it: **no fact lives only in motion**, and `prefers-reduced-motion: reduce` is honoured
> wholesale, with no in-product toggle. Its §6 names what this story owes — a card drawing
> *waiting*, *acting — moving* and *acting — at rest*, an at-rest mark that answers *whose turn* by
> itself, and `--pd-motion-*` tokens beside the sheet's one reduced-motion block. **No ticket below
> is `blocked`.** The paragraphs that follow are the record of why the question was raised.

This story and `STORY-1306` introduce **the product's first continuous motion**. Nothing in
`docs/vision.md`, `docs/adr/` or `design/tokens/tokens.css` says what this product does for a player
whose system asks for reduced motion: there is no `prefers-reduced-motion` rule anywhere in the
repository, no motion token, and no accessibility stance. `ADR-0102` §4 licensed a client-owned
display *schedule* and explicitly *"fixes no duration, no animation and no transition"*, so it does
not reach this.

**`DEC-124` is registered, the product owner's**: does a surface this product animates owe a still
form, and what governs it? It is not a choice between two drawings, so `ADR-0024` §3 does not place
it with the human's eye — a card cannot render a media query.

**What it blocks, precisely:** the split writes the **card ticket `ready`** — a card draws the
*acting* and *waiting* states either way — and every **implementing ticket `blocked`** on
`DEC-124`. It does not block this story, its card, or the epic.

## Tasks

Split on 2026-09-02, four tickets, one chain. `DEC-124` was **answered and merged the same day** by
[`ADR-0115`](../../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md),
so nothing here is `blocked` — the section below is kept as the record of why it was raised.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-130301](../tasks/TASK-130301-the-first-motion-tokens-and-the-marks-two-forms.md) | The first motion tokens, the sheet's one still-block, and the seat's mark in both forms | ready |
| [TASK-130302](../tasks/TASK-130302-the-two-screen-cards-carry-the-moving-mark.md) | The two screen cards carry the moving mark, and nothing else on them moves | backlog |
| [TASK-130303](../tasks/TASK-130303-the-acting-seats-mark-moves-and-the-still-mark-stays.md) | The acting seat's mark moves on the table, and the still mark stays beside it | backlog |
| [TASK-130304](../tasks/TASK-130304-the-mark-is-at-the-seat-the-server-named.md) | The mark is at the seat the server named, and nowhere before it names one | backlog |

**What the split settled, having read `develop` rather than the story's own notes.**

- **The still form already ships**, so `ADR-0115` §1 costs nothing to satisfy and everything to
  keep. `.seat.on-turn { border-left-color: var(--pd-accent) }` and `SeatPlate.tsx`'s
  `border-l-accent` already mark the acting seat against a 2 px slot that is reserved off-turn.
  This story adds motion *beside* that mark and never in place of it; `TASK-130303` gates both
  class names in one assertion so a coder cannot trade the edge for the animation.
- **Nothing `--pd-motion-*` exists yet**, and neither does any `prefers-reduced-motion` rule. This
  story mints both (`TASK-130301`), which is why minting and the first card are one ticket: a
  period chosen before anything was drawn at it could not be corrected by a card ticket that does
  not hold the sheet.
- **The mark renders nothing when `view === null`, so `null-view.test.tsx` does not redden.**
  `Lobby.tsx` renders `WaitingTable` in that state, and `WaitingTable` draws its own seat rows and
  never mounts `SeatPlate` — the only component the mark lives in. The mark also speaks nothing:
  no `aria-label`, no `title`, no `role`, no text, so the guard's `spoken()` closure and its digit
  sweep are both untouched. `TASK-130304` asserts the absence anyway, with a positive control on
  the live table, because a selector that matches nothing anywhere would pass forever.
- **No new frame, and `role="img"` does not move.** The two live frames on `duel-table.html`
  already draw the hero on turn with the rival waiting, and `duel-table-states.html`'s
  `Waiting — their turn` frame already draws the mirror; the *at rest* state is drawn once, on the
  component card. So `role="img"` stays at 16 and 24, `class="frame"` at 6 and 3, and every one of
  those is a refusal gate in `TASK-130302`.
- **`design/components/seat-and-pot.html` is the mark's home**, not a screen card. It is the
  canonical the two screens copy, it already draws the seat *"in its states"* including both
  on-turn seats and an `.away` row, and it is 129 lines — the whole three-state drawing fits one
  `S` diff there and would not fit in a new table frame.

## Acceptance criteria

- [ ] A card under `design/` draws **both** named states — *acting* and *waiting* — at both seats,
      at `ADR-0103`'s phone size, with `design/check-drift.sh` exiting 0, merged before any
      implementing ticket is startable
- [ ] On a rendered table the seat matching the view's `seatToAct` carries the mark and the other
      seat does not — asserted by a named test with the turn on **each** seat in turn, so a
      hard-coded seat fails it
- [ ] A named test fixes what the mark does at a seat that is on turn and `AWAY`/`ABSENT`, and it
      agrees with `seat-status.ts`'s `ADR-0046` §1 order
- [ ] The mark is absent from the host-alone table (`view === null`) — asserted by a named test
- [ ] `no-derivation.test.tsx` stays green: the mark prints no figure and derives nothing
- [ ] The document still fits at 390 × 664 under `ADR-0103` — `scrollHeight ≤ clientHeight`, read
      and pasted as text

## Out of scope

- **The countdown.** How long the acting seat has is `EPIC-13` item 4 —
  [`ADR-0108`](../../docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md),
  `STORY-1307`–`STORY-1309`. This story marks *who*, never *how long*, and draws no timer.
- **The last act.** What the rival just did is
  [`ADR-0109`](../../docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md)
  and `STORY-1304`. Two marks, two lifetimes, two cards.
- **A second reduced-motion rule.** `ADR-0115` §4 puts the product's *one* block in
  `design/tokens/tokens.css` (`TASK-130301`). No surface, card or component adds its own beyond the
  copy each self-contained card carries, and no client code reads the media query.
- **Any change to `Your turn` / `Their turn`** beyond what the merged card draws.
- **The engine.** Nothing here opens `poker-engine`.
