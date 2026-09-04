---
id: STORY-1308
title: The server states a deadline and plays the seat whose clock ran out
type: story
status: done
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

## `DEC-120` is answered

**`ADR-0113` merged 2026-09-02** and resolves `DEC-120` in full: one `ServerMessage.TurnClock`
frame per decision point sent to both seats carrying **durations, never instants**; a deadline
**derived** from the live decision point rather than armed; an expiry that is a
**server-synthesised act** reusing `foldAbsent`'s single-seat body; `expireGracePeriods()` replaced
by `expireTurnClocks()`; and `isPaused`, `DUEL_PAUSED` and `graceRemainingMillis` **deleted, not
deprecated**. `ADR-0113` puts the grace window's retirement in the **same** version step, so this
story is **not** re-cut in two and `EPIC-13`'s table stands as written.

`STORY-1307`'s card merged the same day, so the design this story transcribes is accepted before
its first implementing ticket is startable (`ADR-0091` §2).

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

Split 2026-09-03. One chain, `TASK-130801` startable. Three tickets carry `atomic:`; only
`TASK-130805`'s count is the output of a **green** `ADR-0070` probe, and the other two say so.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-130801](../tasks/TASK-130801-one-seats-given-up-decision-is-one-function.md) | Extract one seat's given-up decision from `foldAbsent`'s loop | ready |
| [TASK-130802](../tasks/TASK-130802-room-timeouts-carry-the-turn-allowance-and-the-timebank.md) | `RoomTimeouts` carries the turn allowance and the timebank | backlog |
| [TASK-130803](../tasks/TASK-130803-the-turn-allowance-and-the-timebank-are-configuration.md) | The two numbers are read from configuration at startup | backlog |
| [TASK-130804](../tasks/TASK-130804-the-room-derives-the-deadline-and-debits-the-bank.md) | The room derives the deadline and debits the bank by arithmetic | backlog |
| [TASK-130805](../tasks/TASK-130805-the-wire-states-the-turn-clock-and-the-version-takes-its-step.md) | `TurnClock` reaches the wire, the pause leaves it, and `PROTOCOL_VERSION` takes its step — **`atomic:` 33, probe-green** | backlog |
| [TASK-130806](../tasks/TASK-130806-every-decision-point-restarts-the-clock-and-states-it.md) | Every act write-back restarts the clock and states it to both seats | backlog |
| [TASK-130807](../tasks/TASK-130807-a-fresh-duel-refills-both-banks-and-a-resume-restates-the-clock.md) | A fresh duel refills both banks, and a resume restates the clock | backlog |
| [TASK-130808](../tasks/TASK-130808-the-room-gives-up-the-turn-of-a-seat-out-of-time-or-absent.md) | The room gives up the turn of a seat that is out of time or absent | backlog |
| [TASK-130809](../tasks/TASK-130809-the-sweep-expires-turn-clocks-in-one-pass.md) | The sweep expires turn clocks in one pass through `act` — **`atomic:` 9** | backlog |
| [TASK-130810](../tasks/TASK-130810-the-grace-window-leaves-the-room-and-the-configuration.md) | The grace window leaves the room and the configuration — **`atomic:` 17** | backlog |
| [TASK-130811](../tasks/TASK-130811-the-store-anchors-the-clock-when-the-frame-arrives.md) | The store anchors a `TurnClock` when the frame arrives | backlog |
| [TASK-130812](../tasks/TASK-130812-the-test-plan-retires-the-pause-and-the-grace-window.md) | The test plan retires the pause case and the grace window it measured against | backlog |

**Where the version step sits, and why it is one ticket.** `TASK-130805` is the only ticket that
moves `PROTOCOL_VERSION`, and it carries no behaviour beyond the refusal it deletes: the frame is
declared and nothing sends it until `TASK-130806`. Its thirty-three files are the `git status` of a
probe run to **green** on 2026-09-03 — the stub was `ADR-0113` §9's four declarations, the loop took
seven Gradle rounds and eleven client rounds, and two paths appeared only after an earlier failure
was cleared. The bump cannot be split from the wire (`ProtocolVersionLedgerTest` rule 4), and the
wire cannot be split from the propagation (the Kotlin compiler, `tsc`, and two byte-comparing verify
tasks), which is what `atomic:` names.

**The other two `atomic:` tickets are renames, and their counts are measured, not probed.**
`TASK-130809` and `TASK-130810` each declare a reference set read off `grep -rl` on `develop` and
say so in the ticket; each instructs its coder to run `ADR-0070`'s loop first and to complete the
table under `ADR-0070` §4. `ADR-0113` §9's probe stub is the **wire's** declarations, so the wire's
radius is the only one the ADR's own procedure sizes.

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
