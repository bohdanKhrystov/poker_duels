---
id: STORY-0505
title: A season ends, and the record survives it
type: story
status: dropped
parent: EPIC-05
module: poker-server
labels: [server, seasons, persistence]
depends_on: [STORY-0502]
---

## Dropped — `ADR-0061` §5, on 2026-08-19

**The decision that killed it:**
[`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md),
answering `DEC-055`. **A season boundary does nothing.** No job runs, nothing is written, nothing is
rewritten, nothing is archived, and `player.coin_balance` is never reset. The ladder shows a
different set of duels on 1 September than on 31 August because the clock moved and for no other
reason. There is no crossing to write, so there is no story here — the premise below turned out to be
false, which is exactly what `dropped` is for.

The file stays, unrewritten below this line, because the epic predicted this story might not survive
its decision and the trail is worth more than the tidiness.

**Where its work went.** One assertion was this story's alone — *the ladder read for a season returns
only that season's duels* — and it moved to `STORY-0502`'s acceptance criteria, in both directions
plus the inside case. Everything else it asserted is covered: attribution across a boundary is
`STORY-0501`'s, *no `duel` or `duel_result` row is rewritten* is trivially true of code that does not
exist, and idempotence has nothing to be idempotent about. `STORY-0506` dropped its dependency on
this story and lost nothing.

**What it does *not* mean.** A finished season is not gone: `ADR-0061` §7 keeps it exactly
recomputable from rows nothing rewrites. What no longer exists is any code that runs at a boundary,
and what does not yet exist is any way for a player to *ask* for a past season — that is `DEC-060`,
and the sharpest cost the ADR records is that nothing anywhere remembers who won a season.

---

## Goal

*(As written on 2026-08-19, before the decision landed.)*

The first season boundary passes without anybody being on call for it: the ladder scopes itself to
the new season, and whatever `DEC-055` says should survive the crossing — a stored standing, a
readable archive, or nothing at all — does.

## Why

`STORY-0501` gives a season a boundary; nothing acts on one. A season that never ends is not a
season, and the crossing is the single moment in this epic where data can be lost. It is also the
one place in the whole product where a coin could be destroyed, which is why it is a story of its
own rather than a clause inside the read path.

## Why this is the riskiest story in the epic

Every other story here is a read. This one runs at a moment nobody is watching, exactly once per
season, against a database that has never done it before — and if `DEC-055` answers that balances
reset, it is the first and only code in the product that reduces a coin balance without a duel
having been lost. `ADR-0014`'s arithmetic and every conservation property in the repository were
written on the assumption that nothing does that. The tasks must treat this as chip conservation
work, not as a scheduled job.

## Design notes

**Settled, and true under every answer:**

- **The crossing is driven by `ServerClock`**, and testable by moving the clock rather than by
  waiting. `EPIC-02` already has the pattern: `ADR-0025` puts one ticker coroutine on the
  application scope driving both sweeps, with a configured period and a throwing pass logged and
  retried. A season boundary is a third thing that happens on a schedule, and it either joins that
  ticker or states why not.
- **Crossing twice does what crossing once does.** A restart mid-crossing, a second sweep in the
  same period, or two instances must not double anything. Idempotence is a property with a test,
  not a code comment.
- **No duel row and no `duel_result` row is ever rewritten.** Whatever the crossing produces, it is
  additive; the per-duel record is the ground truth every answer to `DEC-055` reconstructs from.
- **Migrations are immutable** — a new `V<n>__` file, numbered at merge time.
- **The engine learns nothing**, and no season boundary reaches `poker-engine`.

**Blocked on `DEC-055`.** The answer decides whether this story is a rollover that resets
`player.coin_balance` and writes an archived standing, a much smaller piece of work that only moves
which window the ladder reads, or nothing at all — in which case this story is `dropped`. A v0.3
with no season that ever ends contradicts the vision's own roadmap row, so that branch is the
product owner's to escalate rather than to choose quietly.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Never split, and never will be. `ADR-0061` §5 — a boundary runs no code.* | — |

## Acceptance criteria

- [ ] A duel finished before the boundary and a duel finished after it land in different seasons,
      and the ladder read for each season returns only its own — both directions asserted, from two
      inputs.
- [ ] Crossing the boundary twice leaves the database in the same state as crossing it once,
      asserted by running the crossing twice and comparing.
- [ ] No `duel` or `duel_result` row is modified by the crossing: a test snapshots both tables
      before and after and asserts they are identical.
- [ ] Coins are conserved across the crossing in whatever sense `DEC-055` defines: the sum of what
      the ladder reports after the crossing equals the sum of the `coin_delta`s inside that season's
      window. If the answer resets balances, this criterion is the one that proves the reset was
      exactly a reset and not a loss.
- [ ] The crossing is driven by the clock, asserted by moving the clock, with no `Thread.sleep` and
      no real waiting anywhere in the test.
- [ ] A crossing that throws is logged and retried rather than killing the ticker — the behaviour
      `ADR-0025` already specifies for the other two sweeps.
- [ ] `./gradlew :poker-engine:check` passes with no change to `poker-engine`.

## Out of scope

- **A screen that shows a past season** — `STORY-0503` renders whatever the read path serves; a
  season selector is only work if `DEC-055` asks for one, and then it is that story's.
- **Telling a player their season ended** — no notification, no email, no `ServerMessage`. This epic
  adds none of the three, and an end-of-season announcement is a product decision nobody has asked
  for.
- **Season-end rewards of any kind** — the vision's *What it is not* refuses bonuses, and a coin has
  nothing to be spent on by design.
- **Backfilling seasons over duels already played** — unnecessary, because `duel.finished_at` is
  `NOT NULL` on every row and any window is reconstructible.
