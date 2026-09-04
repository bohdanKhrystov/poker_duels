---
id: STORY-1310
title: The refresh paths nobody drove, driven and written down
type: story
status: ready
parent: EPIC-13
module: web-client
labels: [client, qa, refresh]
depends_on: [STORY-1301]
---

## Goal

Each of the six refresh and navigation paths `ADR-0112` §6 names is driven against a running stack
and its result written down, so `EPIC-13` closes on evidence rather than on a symptom nobody looked
for again.

## Why

**The human reported something and it did not reproduce.** `EPIC-13` measured five paths on
2026-09-02 with `scripts/qa/drive.mjs` and `location.reload()`: a host in a live duel on a bare `/`,
a rival on `?room=CODE`, a host on the waiting screen, and `#/leaderboard` on a room-free browser all
**survive**. What reproduced was the inverse — a browser **holding a room** having its fragment
erased.

**The epic refuses to close on that.** Its Definition of done: *"The reported refresh symptom is
either reproduced and fixed, or recorded as not reproducible with the paths that were tried written
down — `EPIC-13` does not close on a symptom nobody looked for again."* `ADR-0112` §6 turns that into
a list and adds the sentence this story exists to satisfy: ***"A dismissal without the attempt does
not satisfy the DoD row."***

**Its findings feed `STORY-1311`**, and one of them —
[`ADR-0086`](../../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)
§6's accept path — is a collision `ADR-0112` derived rather than observed, and which it resolved in
`ADR-0086`'s favour. A derivation is not a measurement.

**It runs early, before `STORY-1302`**, because two of the six paths are driven in a **waiting**
state and `STORY-1302` retires the waiting screen. Measuring the product the human reported on is the
point; `STORY-1311` covers whatever `STORY-1302` changes.

## Design notes

- **The six paths, verbatim from `ADR-0112` §6**, each owed a written result:
  1. a refresh **on the result screen** — a held `FINISHED` room, `outcome` standing;
  2. a refresh **during a runout** — `ADR-0102` §5 says the reload jumps to the end; confirm no lobby
     shows on the way;
  3. a **genuinely dropped socket** — a reconnect through `reconnecting.ts`, **not** a reload;
  4. **real latency**, where the rejoin round-trip is visible — a lobby flash localhost sampling
     could not see;
  5. the **`AccountOffer` accept path** of `ADR-0112` §5, whose failure is so far derived rather than
     observed;
  6. a **mailed link opened while a room is held**, in both a waiting and a playing state.
- **A browser drives this client for a QA round, never for a gate** —
  [`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
  §2's three standing conditions: **no dependency, no gate, no coverage claim.** So **no ticket here
  may put a browser drive in a `verify:` block**, and no result here may be cited as coverage. The
  deliverable is a record; the gates are the ordinary suites plus
  `python3 .github/scripts/lint_tickets.py`, and the readings are pasted into the PR body as text —
  the shape `STORY-1215` used and `ADR-0106` §4 licensed.
- **The record lives in this story and its tickets**, as a path-by-path table with the observed
  result, the commit driven, and the stack it ran against. A path that cannot be driven says so and
  says why — that is a result, not a gap.
- **A defect found is a ticket, not a widening.** If a path reproduces a lobby flash, a lost screen
  or a spent token, that becomes its own ticket under this story or `STORY-1311`, with a **non-browser
  gate** — a unit or integration test that fails on the defect and passes on the repair. If only a
  browser can see it, it is filed the way `EPIC-12` files a defect and is repaired against a
  reproduction by hand (`ADR-0089` §4).
- **What is already measured is not re-measured.** The five paths in `EPIC-13`'s *What is already
  true* stand; this story adds the six that were not covered.
- **A mailed link refused mid-duel must not spend its token** (`ADR-0112` §5) — path 6 checks the
  token is still usable after the duel, because that is the one path whose failure is silent and
  permanent.

## The record

**This is the deliverable.** One row per path, filled in by the ticket that drives it, and nothing
here is a gate: `ADR-0089` §2b forbids a browser standing between a pull request and `develop`, so
what a `verify:` block can check is that the row exists, that it is no longer a placeholder, and
that the ordinary suites are still green. Whether the sentence in it is *true* is a human's verdict
on the readings pasted into the PR body — the shape `STORY-1215` used and `ADR-0106` §4 licensed.

The placeholder is the word standing in the `result` cells below. It appears in this file only
inside this table, so a count of it is a count of paths still owed.

| id | path | result | commit | stack |
| --- | --- | --- | --- | --- |
| `P1` | a refresh **on the result screen** — a held `FINISHED` room, `outcome` standing | NOT-YET-DRIVEN | — | — |
| `P2` | a refresh **during a runout** — `ADR-0102` §5 says the reload jumps to the end; no lobby on the way | NOT-YET-DRIVEN | — | — |
| `P3` | a **genuinely dropped socket** — a reconnect through `reconnecting.ts`, not a reload | NOT-YET-DRIVEN | — | — |
| `P4` | **real latency** — the rejoin round trip made wide enough to see | NOT-YET-DRIVEN | — | — |
| `P5` | the **`AccountOffer` accept path** of `ADR-0112` §5, so far derived rather than observed | NOT-YET-DRIVEN | — | — |
| `P6a` | a **mailed link opened while a room is held** — the room **waiting** | NOT-YET-DRIVEN | — | — |
| `P6b` | a **mailed link opened while a room is held** — the room **playing** | NOT-YET-DRIVEN | — | — |

**The `stack` cell names which of two the reading came from**, because on one of them a green
reading means nothing:

- `bare` — Vite on `5173`, the server on `8080`, no relay. The product as it ships, and the reading
  that counts when something **is** seen.
- `delayed <n>ms` — Vite moved to `5273` and `scripts/qa/delay.mjs` listening on **`5173`** in front
  of it, so the browser's origin, the invite link and every hard-coded `5173` inside `drive.mjs`
  are unchanged. The reading that counts when **nothing** is seen.

**Why the second reading is not optional.** `drive.mjs`'s `wait` and `absent` sample `#root` every
250 ms, and `record` cannot be armed across a page load — its `MutationObserver` dies with the
document. On localhost the whole rejoin round trip is shorter than one sample, so *"no lobby
appeared"* on a bare stack is a statement about the instrument and not about the product. `EPIC-13`
already recorded exactly that: *"No lobby flash was observable at the sampling resolution
`drive.mjs` allows."* So a path whose finding is a **negative** is driven twice, and its row says
which reading it is.

**What a reload can be observed with, given that.** `open` prints `#root`'s text the moment it first
has content — the first paint, and the only pre-frame observation available across a navigation;
`record` armed immediately after `open`, then `frames`, catches every transition from there on.
Both belong in the PR body verbatim.

## Tasks

Split on 2026-09-04 into **nine** tickets, one chain. The first two build the instrument, because
five of the seven readings are races a bare localhost stack cannot resolve, and one of them — a
socket that drops without the page dying — has no driver verb at all today.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-131001](../tasks/TASK-131001-a-loopback-relay-puts-milliseconds-in-front-of-the-stack.md) | A loopback relay puts milliseconds in front of the stack | ready |
| [TASK-131002](../tasks/TASK-131002-the-relay-learns-to-cut.md) | The relay learns to cut, so a socket drops without the page dying | backlog |
| [TASK-131003](../tasks/TASK-131003-p1-a-refresh-on-the-result-screen.md) | `P1` — a refresh on the result screen | backlog |
| [TASK-131004](../tasks/TASK-131004-p5-the-account-offers-accept-is-observed.md) | `P5` — the account offer's accept, observed rather than derived | backlog |
| [TASK-131005](../tasks/TASK-131005-p2-a-refresh-during-a-runout.md) | `P2` — a refresh during a runout | backlog |
| [TASK-131006](../tasks/TASK-131006-p3-a-genuinely-dropped-socket.md) | `P3` — a genuinely dropped socket | backlog |
| [TASK-131007](../tasks/TASK-131007-p4-the-rejoin-round-trip-made-visible.md) | `P4` — the rejoin round trip made visible | backlog |
| [TASK-131008](../tasks/TASK-131008-p6-a-mailed-link-over-a-held-room.md) | `P6a`/`P6b` — a mailed link over a held room | backlog |
| [TASK-131009](../tasks/TASK-131009-the-record-read-whole.md) | The record read whole, and every finding given an owner | backlog |

## Acceptance criteria

- [ ] All six of `ADR-0112` §6's paths are driven, or a path is recorded as undrivable with the
      reason — a table in this story with one row per path, the observed result, and the commit
- [ ] The `AccountOffer` accept path's behaviour is **observed**, not derived, and the observation is
      compared against `ADR-0112` §5's resolution in `ADR-0086`'s favour
- [ ] The mailed-link path records whether the token is still usable afterwards
- [ ] No `verify:` block in any ticket of this story runs a browser (`ADR-0089` §2)
- [ ] Every defect the drive finds is filed as its own ticket naming a non-browser gate, or recorded
      as browser-only with `ADR-0089` §4's by-hand reproduction requirement written into it
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0

## Out of scope

- **The repair itself.** What the client does about a held room and a chosen screen is `ADR-0112`'s
  answer, its mechanism is `DEC-123`, and both land in `STORY-1311`.
- **A QA round.** This story runs no `/qa-cycle`, reports no `A(N)` or `B(N)`, and moves no verdict
  table. It is a targeted reproduction attempt an ADR asked for by name.
- **Any coverage claim.** `ADR-0089` §2c: a drive is a statement about one run, on one machine, at
  one commit.
- **The engine and the server.** Nothing here opens either.
