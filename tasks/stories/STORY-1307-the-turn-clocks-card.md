---
id: STORY-1307
title: The turn clock's card — regular, running out, on timebank, expired
type: story
status: done
parent: EPIC-13
module: design
labels: [design, table, clock]
depends_on: [STORY-1306]
---

## Goal

A merged card draws every state of the turn clock — regular time, running out, on the timebank, and
expired — at both seats and at `ADR-0103`'s phone size, so the two implementing stories transcribe
an accepted drawing instead of deriving one from prose.

## Why

**It is the half of `EPIC-13` item 4 that no decision blocks.**
[`ADR-0108`](../../docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md)
§5 states what the player sees as requirements rather than layout, and it names the states the card
owes by name. `DEC-120` — the architect's — decides *how the deadline is carried, enforced and
resumed*, which is a mechanism the card does not draw. Splitting the card off is what lets the
epic's largest item start now instead of waiting.

It is also the last card in the epic's card sequence, and it draws the table as it will finally
stand: after the acting-seat mark it sits beside (`STORY-1303`) and after the chips that changed the
seat plates (`STORY-1306`). Every earlier card proved `ADR-0103`'s fit against everything merged
before it; this one proves it against all of them.

## Design notes

Everything below is `ADR-0108`, merged, and the card draws it rather than reopening it.

- **One countdown: the acting seat's clock, visible to both players, ticking once per second**
  (§5) — the human's *"clock shoud visibly change each second"* — counting down to a deadline the
  **server stated**, with the client interpolating between frames and asserting nothing.
- **Four named states, drawn and not noted** (§5, and `EPIC-13` *Design first* rule 1): *regular*,
  *running out*, *on timebank*, *expired*. A card showing one state of a control that has four
  leaves the same debt `ADR-0091` §5 registers, in a smaller shape.
- **The 30 s and the bank are visibly distinct** (§5). A player must be able to see the bank begin
  to spend. **Both banks are public facts of the table** — hiding the rival's would make the wait it
  buys unexplainable to the person waiting — so the card draws the rival's remaining bank too.
- **Zero is not an event** (§5, re-applying `ADR-0046` §3): nothing a player reads changes when the
  countdown reaches zero until a server frame carries the consequence. The *expired* state the card
  draws is the state **after** that frame, and the card says so.
- **The numbers are 30 s and 3 m, and they are configuration, not literals** (§1). The card draws
  them as the values they are today and does not hard-code them into a rule.
- **The clock runs only while its seat is on turn** (§1) — a runout, the rival's turn and the gap
  between hands spend nothing — **and it keeps running when the socket drops.** So the card owes,
  within its four states, what an `AWAY` seat's clock looks like: §4's table says an away seat on
  turn has *exactly the same 30 s plus remaining bank*, and an away seat whose clock is exhausted is
  `ABSENT` and played without a fresh clock.
- **No new strings are chosen by the ADR** (§5). *"The duel is paused."* leaves the screen when the
  pause leaves the product, and what stands in its place is derived **under `ADR-0046`'s register,
  by the story that lands it, against this card.** `ADR-0046`'s register is two shapes and no third:
  short capitalised fragments for what a seat is doing, plain full sentences for anything that needs
  explaining. **One constraint is this ADR's: an expiry is never called a *forfeit*** — `ADR-0046`
  §5 forbids the word because it is false, and under `ADR-0108` §3 it stays false. If the card cannot
  find its words inside that register, that is a stop and a new ADR, never an invented sentence.
- **It composes, it does not mint** (`ADR-0091` §3), unless the human's chosen drawing needs a token
  the sheet does not declare — in which case that ticket is minting and is worked interactively.
- **The fit is the card's to prove** (`ADR-0103`), against everything this epic has merged before it.
  If the clock does not fit at 390 × 664, that re-opens `ADR-0103`'s give list rather than being
  quietly spent.
- **It draws nothing on the host-alone table.** `ADR-0110` §3 forbids any game fact before the
  opening `Snapshot`, and there is no seat on turn there.

**What this card does not decide, and must not imply.** Which frame carries the deadline, whether an
expiry is a server-synthesised act or a new room event, what schedules the sweep, and what becomes
of `disconnectGraceMillis`, `GraceExpiry`, the `DUEL_PAUSED` path and `graceRemainingMillis` are all
`DEC-120`'s, the architect's (`ADR-0108` §6). A card that drew a mechanism would be answering it.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-130701](../tasks/TASK-130701-the-clocks-vocabulary-and-the-fresh-allowance.md) | Draw the turn clock, its two allowance states, and the timebank at both seats | ready |
| [TASK-130702](../tasks/TASK-130702-the-bank-spending-zero-and-the-frame-that-follows.md) | Draw the clock on the timebank, holding at zero, and the plate after the server acted | backlog |
| [TASK-130703](../tasks/TASK-130703-an-away-seats-clock-and-an-absent-seats-lack-of-one.md) | Draw ADR-0108's presence table, and retire the grace window's row | backlog |
| [TASK-130704](../tasks/TASK-130704-the-two-table-cards-carry-the-clock-and-both-timebanks.md) | The two table cards carry the clock and both timebanks, and the host-alone frames carry none | backlog |

**The still form of a ticking clock, settled here rather than left to a coder.** `ADR-0115` §3
already names the clock by name: *"each second's numeral is a step. A smooth sub-second depletion
drawn between them is a how, and is what a reduced form skips."* So **the still form of the clock
is the clock** — a numeral changing each second is information arriving, not motion, and a player
with `prefers-reduced-motion: reduce` reads the same figure at the same second as everyone else.
The split takes that structurally, the way `STORY-1306` took `ADR-0115` structurally: **the clock
animates nothing at all.** `@keyframes`, `animation:`, `animation-` and `transition:` are pinned at
their present counts on all three cards, so no keyframe, shorthand or longhand can reach it and the
reduced-motion form is byte-identical by construction. `TASK-130701` draws the *at rest* row anyway,
because a reader who does not know this will assume a clock must tick visually.

## Acceptance criteria

- [ ] A card under `design/` draws **four named states** — *regular*, *running out*, *on timebank*,
      *expired* — with each state's name written on the frame
- [ ] The card draws the countdown at the acting seat and **both** seats' remaining banks
- [ ] The card draws an `AWAY` seat on turn and an `ABSENT` seat, and its drawing agrees with
      `ADR-0108` §4's table
- [ ] The card renders at `ADR-0103`'s 390 × 664 with the table's merged furniture — the acting-seat
      mark, the last-act mark, the chips — and does not overflow
- [ ] Every string on the card is either already shipped or derived inside `ADR-0046`'s register,
      and the words *forfeit* and *forfeited* appear nowhere
- [ ] `design/check-drift.sh` exits 0 and the card names no `--pd-` token
      `design/tokens/tokens.css` does not declare
- [ ] The human's visual verdict is recorded (it may trail the merge, `ADR-0091` §3)

## Out of scope

- **Every mechanism question** — `DEC-120`, the architect's: the deadline's frame, the expiry's
  shape, the sweep's resolution, the resume, and the grace window's machinery.
- **Any client or server code.** This story ships a card. The countdown is `STORY-1309`; the
  server's deadline and enforcement are `STORY-1308`.
- **`DEC-108`** — whether the bar may stay enabled under *The duel is paused.* It stays open, and
  `ADR-0108` explicitly does not answer it.
- **A second countdown of any kind.** `ADR-0108` §Consequences forecloses it by construction.
- **The engine.** `poker-engine` stays clock-free (`ADR-0108` §6, `EPIC-13` *Out of scope*).
