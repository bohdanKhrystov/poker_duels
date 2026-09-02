---
id: STORY-1309
title: The table counts down, and the pause leaves the screen
type: story
status: blocked
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

## Blocked on `DEC-120` — the architect's

**This story is not splittable into tickets until `ADR-0113` merges.** It renders a fact whose frame
and shape `DEC-120` chooses, and it inherits `DEC-120`'s answer on what becomes of
`presence-countdown.ts`, `graceRemainingMillis` and the `DUEL_PAUSED` path — *"the architect's to
dismantle or reuse"* (`ADR-0108` §6). Writing tickets against a guess would produce a client reading
a field that does not exist.

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

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *blocked — split after `ADR-0113` merges, then run `/plan-story STORY-1309`* | — |

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
