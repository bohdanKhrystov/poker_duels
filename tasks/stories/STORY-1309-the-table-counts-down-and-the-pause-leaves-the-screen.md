---
id: STORY-1309
title: The table counts down, and the pause leaves the screen
type: story
status: done
parent: EPIC-13
module: web-client
labels: [client, table, clock]
depends_on: [STORY-1308]
---

## Goal

The duel table shows one countdown at the acting seat, ticking once a second toward the deadline the
server stated, in the four states `STORY-1307`'s card draws — and *The duel is paused.* leaves the
screen with the pause it named.

## Why

**A deadline nobody can see buys nothing.** `STORY-1308` makes the server state one and enforce it;
this is the half the human asked for in as many words — *"clock shoud visibly change each second"* —
and the half that lets a waiting player answer *"how long will I wait?"* by looking at the table
rather than by guessing.

It also lands the copy half of `ADR-0108`'s amendment. `ADR-0046` §2's table has a row whose occasion
is gone, and a sentence that stays on screen after its cause leaves the product is the failure mode
this repository files ADRs about.

## `DEC-120` is answered, and this story is split

`ADR-0113` merged on 2026-09-02 and `STORY-1308` landed all twelve of its server and store tickets,
so the guess this story refused to make is now a merged fact: `ServerMessage.TurnClock` at
`PROTOCOL_VERSION` 6, `turnClock` anchored at arrival in `duel-state.ts`, `graceRemainingMillis` and
the `DUEL_PAUSED` entry deleted, and `presence-countdown.ts` kept for renaming here
(`ADR-0113` §7). **Nothing paints any of it yet.** That is this story, split on 2026-09-04 into
twelve tickets, one chain, `TASK-130901` startable.

**What draws the countdown, and what makes it tick — settled, not assumed.** The store ticks it, by
re-arming `ADR-0102` §4's existing `schedule` seam once a second while a clock is live, reading a
monotonic clock `boot.ts` supplies as a parameter; React redraws because it subscribes
(`ADR-0032` §3). No component owns a timer. The merged source is `ADR-0113` §6 — *"the clock source
is monotonic and injected … it reaches **the store** as a parameter at `web-client/src/store/boot.ts`,
**the seam `ADR-0102` §4 already established** for `REVEAL_STEP_MS`"*, and *"every tick recomputes
from the anchored deadline; nothing is decremented"* — reinforced by `EPIC-13`'s own gloss that
*"a ticking countdown is the same shape"* as `ADR-0102`'s client-owned pacing clock, and by that
mechanism's existing second instance, `advanceReveal`/`armTick`, which already moves the store's
state on a timer and no message at all. No `DEC` is owed for it.

## Design notes

- **The card is merged before the implementing ticket is startable** — `STORY-1307` is that card, and
  it is this story's real prerequisite as well as its `depends_on` ancestor. This story transcribes
  it; it does not re-derive a drawing.
- **One countdown, at the acting seat, visible to both players, ticking once per second**
  (`ADR-0108` §5). The client **interpolates the ticks between frames and asserts nothing** — the
  licensed shape is `ADR-0102`'s, and `presence-countdown.ts`'s arithmetic is the pattern: a
  server-sent deadline turned into whole seconds, floored at zero, thirteen lines.
- **Zero is not an event** (`ADR-0046` §3, re-applied by `ADR-0108` §5). Nothing a player reads
  changes when the countdown reaches zero until a server frame carries the consequence. **The
  enforced expiry may trail the visible zero; the screen never invents the act.** A test pins this
  directly, because it is the one place a client could assert a game fact.
- **The 30 s and the bank are visibly distinct, and both banks are public** (`ADR-0108` §5). The
  rival's remaining bank is on screen; hiding it would make the wait it buys unexplainable.
- **The four states are the card's**: *regular*, *running out*, *on timebank*, *expired*. The client
  renders what the card drew; which is which is not re-decided here.
- **The pause's copy leaves with the pause** (`ADR-0108` §4 and §Consequences). *"The duel is
  paused."* and the `DUEL_PAUSED` refusal lose their occasion. **What stands in its place — if
  anything — is derived under `ADR-0046`'s register, by this story, against `STORY-1307`'s card.**
  The register is two shapes and no third: short capitalised fragments for what a seat is doing,
  plain full sentences for anything that needs explaining. **An expiry is never called a *forfeit***
  (`ADR-0046` §5), because under `ADR-0108` §3 the word is false. If no sentence inside the register
  fits, the honest answer is that the line simply goes — and if the story finds it needs a **new**
  string that the register cannot produce, that is a stop and a new ADR.
- **`ADR-0046`'s other clauses stand**: *Away* and *Timed out* keep their seats,
  `absent-action-text.ts`'s server-action line keeps working, and `ADR-0075` still bounds that mark's
  lifetime.
- **The clock draws nothing on the host-alone table** (`ADR-0110` §3) — no seat is on turn before the
  opening `Snapshot`.
- **`DEC-108` is not answered here.** *"When the table says* The duel is paused.*, may the action bar
  stay enabled?"* — the product owner's, open, and `ADR-0108` says explicitly that it becomes a
  question about a sentence that is leaving the screen. **This story removes the sentence and does
  not decide `DEC-108`**; whether landing it moots the question is the product owner's to record when
  they answer, not this story's to assume.

## What the split settled, and the two judgements it made rather than raised

**The pause's replacement, derived.** `ADR-0046` §2's `AWAY` row is two sentences with two jobs: the
first states the fact, the second *"answers what does this mean for me"*. Only the second lost its
occasion, and `ADR-0108` §4 names what answers that question now — *"the present player's answer to
how long will I wait? is the rival's clock"* — which is on screen by `TASK-130910`. So the line keeps
`Your rival is away.` and stops there: not a new string, not a deleted row. Deleting the row outright
was considered and refused, because §2's table already distinguishes *a line* from *nothing at all*
and this is not that row, and because the redundancy with §1's `Away` status word is merged,
deliberate and left to the design by §6. `TASK-130911` carries the argument in full so it can be
argued with.

**Two values were chosen from the settled vocabulary rather than registered, and both are one line to
overrule at the pane (`ADR-0024` §3, `ADR-0091` §3's trailing verdict):**

- **`RUNNING_OUT_SECONDS = 10`.** No merged source fixes the switch point; the merged card fixes its
  bounds by drawing `24` regular and `6` running out, so anything in 7…24 agrees with the drawing the
  human accepted. Named once, in one file — `ADR-0102` §4's feel-number precedent.
- **The clock's type size is `text-large`.** The card draws `.clock` at `font-size: 1rem` and the type
  scale has no `1rem` step (`--pd-fs-body` is `0.9375rem`, `--pd-fs-large` is `1.125rem`). A raw
  length inside a Tailwind arbitrary value is the shape `ADR-0091` §4 names as failing, and minting a
  new step is *minting*, which `ADR-0091` §3 puts with the human. So the client composes from what is
  merged, keeping the property the drawing makes — the clock is the largest figure on the plate.
  **A finding for a separate ticket, not this story's to fix:** the merged card's own `1rem` is a size
  not born in the sheet, which `ADR-0024` §2 says every size must be.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-130901](../tasks/TASK-130901-the-countdown-loses-its-presence-prefix.md) | `presence-countdown.ts` becomes `countdown.ts`, and its citation re-points | ready |
| [TASK-130902](../tasks/TASK-130902-the-clocks-two-figures-are-the-cards-own.md) | The clock's figure and the bank's figure are the two shapes the card drew | backlog |
| [TASK-130903](../tasks/TASK-130903-one-seat-draws-the-clock-and-two-numbers-choose-its-treatment.md) | One seat draws the clock, and two server-stated numbers choose its treatment | backlog |
| [TASK-130904](../tasks/TASK-130904-the-reducer-keeps-a-second-hand-and-knows-when-to-stop.md) | The reducer keeps a second hand, and stops it once the clock can no longer move | backlog |
| [TASK-130905](../tasks/TASK-130905-the-store-owns-the-tick-and-boot-names-the-clock.md) | The store arms the second hand, and boot names both the clock and the period | backlog |
| [TASK-130906](../tasks/TASK-130906-the-seat-plate-draws-the-clock-and-the-bank.md) | The seat plate draws the countdown and the seat's timebank, and speaks neither | backlog |
| [TASK-130907](../tasks/TASK-130907-the-table-draws-one-countdown-at-the-acting-seat.md) | The table draws one countdown, at the acting seat, and both seats' banks | backlog |
| [TASK-130908](../tasks/TASK-130908-the-four-states-are-reachable-and-zero-is-not-an-event.md) | The four states are each reachable on the table, and reaching zero changes nothing else | backlog |
| [TASK-130909](../tasks/TASK-130909-the-derivation-guard-admits-the-clocks-own-figures.md) | The derivation guard admits the clock's own figures and no other new number | backlog |
| [TASK-130910](../tasks/TASK-130910-the-duel-screen-hands-the-table-its-clock.md) | The duel screen hands the table its clock, and it visibly changes each second | backlog |
| [TASK-130911](../tasks/TASK-130911-the-pause-leaves-the-line-that-named-it.md) | *The duel is paused.* leaves the line that named it, and the sentence before it stays | backlog |
| [TASK-130912](../tasks/TASK-130912-the-notice-stops-testing-a-sentence-that-left.md) | The presence notice stops testing a sentence that left, and tests the one that stayed | backlog |

**The order is one chain and the last two are last on purpose.** The countdown reaches the screen
before the sentence promising a pause leaves it, because the clock *is* the replacement for that
sentence's second clause (`ADR-0108` §4: *"The present player's answer to how long will I wait? is
the rival's clock"*). Landing the removal first would leave a merged commit in which an away seat
explains nothing at all.

## Acceptance criteria

- [ ] The table renders exactly one countdown, at the acting seat, and both players see it — a named
      test with the turn on **each** seat in turn, so a hard-coded seat fails it
- [ ] The countdown decreases by one each second against injected time and never sleeps
- [ ] Reaching zero changes nothing a player reads until a server frame arrives — a named test holds
      the client past the deadline with no frame and asserts the screen states no act
- [ ] Both seats' remaining timebanks render
- [ ] The four states the card draws are each reachable and each asserted by name
- [ ] *The duel is paused.* renders nowhere, and a grep proves the string is gone from client source
- [ ] *Away*, *Timed out* and the server-action line still render as `ADR-0046` and `ADR-0075` fix
      them
- [ ] No countdown renders on the host-alone table (`view === null`)
- [ ] `no-derivation.test.tsx` stays green — the countdown derives seconds from a server-stated
      deadline and states no game fact
- [ ] The document still fits at 390 × 664 under `ADR-0103` — `scrollHeight ≤ clientHeight`, read
      and pasted as text

## Out of scope

- **The deadline's wire shape, the expiry's enforcement, the sweep and the resume** — `STORY-1308`
  and `DEC-120`.
- **`DEC-108`.** Open, the product owner's; not answered here.
- **A second countdown.** Foreclosed by `ADR-0108` §Consequences.
- **The word *forfeit*, in any state.** `ADR-0046` §5.
- **Any new string the register cannot produce.** That is a stop and a new ADR.
- **The engine.** Nothing here opens `poker-engine`.
