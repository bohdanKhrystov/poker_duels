---
id: STORY-1308
title: The server states a deadline and plays the seat whose clock ran out
type: story
status: blocked
parent: EPIC-13
module: poker-server
labels: [server, protocol, clock, atomic]
depends_on: [STORY-1307]
---

## Goal

A seat on turn has 30 seconds plus whatever remains of one 3-minute timebank, the server states the
deadline as a fact a client can count down to, and when it is spent the server gives up that seat's
one decision with `ADR-0023`'s conduct — never the duel.

## Why

**This is what closes the cost `ADR-0105` accepted by name**: *"a player who wants out of a duel is
stuck — no resign, **no turn clock**, and a `PLAYING` room is never reaped."* `ADR-0105` §1 then made
*Create a duel room* refuse a player holding a running duel, which is defensible only while that duel
can actually end. `EPIC-13`'s Definition of done states the repair as a product fact: *"A player who
stops acting no longer holds their rival's duel open indefinitely."*

It is the largest item in the epic, it is the **only** one that moves the wire, and nothing exists to
build on: no clock, deadline or timebank exists in the engine, the server or the client.

## Blocked on `DEC-120` — the architect's

**This story is not splittable into tickets until `ADR-0113` merges.** `ADR-0108` §6 fixes what must
be true and writes no repair; `DEC-120` is registered for the mechanism and is being answered now.
What it owns, verbatim from §6:

- which frame states the deadline the client counts down to — a field on an existing message or a
  new one;
- whether an expiry is a **server-synthesised act** through the ordinary act path (`foldAbsent`'s
  shape) or a **new room event**;
- what schedules expiry and at what resolution — the enforced expiry **may trail** the stated
  deadline by a sweep period (`ADR-0025`) and **may never precede it**;
- how a **resuming** client learns the live deadline;
- what becomes of `disconnectGraceMillis`, `GraceExpiry`, the `DUEL_PAUSED` path and
  `graceRemainingMillis` as `presence-countdown.ts`'s input.

**If `ADR-0113` puts the grace window's retirement in a second version step, this story splits in
two at split time and `EPIC-13`'s table is re-cut.** That is a re-cut of a story boundary before any
ticket exists, not a scope change to a written one.

## Design notes

Everything below is `ADR-0108`, merged, and the mechanism must satisfy it.

- **One clock, and it does not care about the socket** (§1). 30 s per decision, fresh every time the
  turn arrives; then the seat's **timebank** — one budget of 3 minutes per player per duel, carried
  across hands, never refilled within the duel, fresh for a rematch because a rematch is a new duel.
  The clock runs **only** while its seat is on turn, and **keeps running when the socket drops**:
  dropping the connection is never a way to gain time, stop time or freeze a rival. Both numbers are
  **configuration, not literals** — `ADR-0013`'s rule, inherited.
- **Expiry gives up the decision with `ADR-0023`'s conduct, marked as the server's** (§2). Read the
  engine's `legalActions` at that decision point; send `Fold` when `FOLD` is legal, `Check`
  otherwise. **Never a call, bet, raise or all-in; never a chip.** The act goes down the ordinary act
  path and is marked to both seats as an act **the server** took (`ADR-0028`'s mark) — a timed-out
  player is never presented as having chosen what the clock chose. The always-fold alternative stays
  rejected: at a free decision point the engine does not offer `FOLD`, and **the engine is not open
  here.**
- **An expiry ends nothing by decree** (§3). It costs the seat one decision; a fold costs the hand;
  **nothing costs the duel.** No count of expiries forfeits it, no timeout awards it, and no coin
  moves except by `ADR-0014` on the outcome the engine reaches. `RoomRegistry`'s single `sink.record`
  call site stays the only settle path — **this is the resolution of the coin question `DEC-115` was
  routed on, and no ticket may add a second path.**
- **The timebank replaces the grace window, and the pause goes with it** (§4). `ADR-0013`'s fixed
  window is retired; §4's table is the one regime, row by row, including *both seats gone* reaching
  today's unchanged abandon path. **The duel never pauses**: no action of the present player is
  refused because a rival's socket dropped, and `DUEL_PAUSED`'s occasion leaves the product.
  Reconnecting mid-clock **resumes with what is left**, state resent through the projection layer —
  `ADR-0013`'s reconnect promise, kept verbatim.
- **`ADR-0046`'s other clauses all stand** (§Amends): the away fact is still told, every act the
  server takes is still marked, and *Away* and *Timed out* keep their seats.
- **`poker-engine` stays closed** (§6) — no clock, no new action — and **timer tests inject time
  rather than sleep** (`ADR-0013`'s own rule, inherited with the machinery). Determinism is
  unaffected: same seed, same actions, byte-identical game.

### The wire, and the lock

- **It moves the wire**, so the version step is claimed in `ADR-0047`'s ledger under its
  **one-bumping-branch-at-a-time lock**. **This story serialises against anything else that moves
  `PROTOCOL_VERSION`.** Nothing else in `EPIC-13` moves it — `ADR-0107`, `ADR-0109`, `ADR-0110`,
  `ADR-0111` and `ADR-0112` each say so in as many words — so within this epic the lock is
  uncontended, and the check is against the rest of the board rather than against a sibling story.
- **The version-bumping ticket is `atomic:`, sized by `ADR-0070`'s probe run to green.** Not by
  memory, not by another ticket's file list however recent: stub every declaration the change adds,
  removes, renames or re-values — **an enum entry is a declaration** — all in one tree; run **the
  commands `.github/workflows/build.yml` runs on a pull request, verbatim and in full**, read from
  that file; turn every path the run names into a *Files* row with its failing gate as the *why it
  cannot be fewer*; apply the minimal propagation; **run again, and stop only when the gate set exits
  0.** A red run names a prefix — **there is no prefix of green.** Then revert; `git status` is the
  list.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *blocked — split after `ADR-0113` merges, then run `/plan-story STORY-1308`* | — |

## Acceptance criteria

- [ ] A seat on turn is given 30 s, fresh at every decision point, from configuration and not a
      literal
- [ ] A seat that spends its 30 s spends its timebank; the bank is 3 minutes per player per duel,
      carries across hands, never refills within the duel, and is fresh in a rematch
- [ ] The clock spends nothing during a runout, during the rival's turn, or between hands
- [ ] Dropping the socket neither pauses nor extends the clock — a named test proves an away seat on
      turn is owed exactly what a connected seat is owed, with time injected and never slept
- [ ] An exhausted clock produces `Fold` where `FOLD` is legal and `Check` where it is not, never a
      chip, marked to **both** seats as the server's act
- [ ] No expiry ends a duel, awards a duel, or moves a coin — a named test drives a duel to a chip
      holder through repeated expiries and proves the coin moved once, on the outcome
- [ ] The deadline is a server-stated fact on the wire, and a resuming client is told the live
      deadline again
- [ ] `DUEL_PAUSED` has no occasion: an action sent while the rival's socket is down is applied
- [ ] `PROTOCOL_VERSION` is bumped once, the ledger row is claimed, and the bumping ticket is
      `atomic:` with a *Files* count equal to its own table and every row naming its gate
- [ ] `poker-engine` has no diff

## Out of scope

- **Resigning a duel.** `EPIC-13` *Out of scope*, and the human answered *"not now"* on 2026-09-02.
- **Reaping a `PLAYING` room.** `ADR-0105`'s third named cost; the sweep is `ADR-0025`'s.
- **A forfeit, in any form** — immediate or after a count of expiries. `ADR-0108` §3 and
  §Alternatives refuse it, and `ADR-0046` §5 forbids the word.
- **A second countdown, or a surviving pause.** Both are foreclosed by construction (§Consequences).
- **The client's rendering.** The countdown, the four drawn states and the pause's copy leaving the
  screen are `STORY-1309`.
- **Tuning 30 s or 3 m.** Configuration; a change is a value, not a ticket here.
- **An *I'm back* control for a connected ghost.** `ADR-0108` §Consequences names the cost and
  refuses the repair: if a griefer turns up, that is a new `DEC`.
- **The engine.** `poker-engine` stays pure and clock-free.
