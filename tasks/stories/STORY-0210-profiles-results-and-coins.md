---
id: STORY-0210
title: Profiles, duel results and duel coins
type: story
status: done
parent: EPIC-02
module: poker-server
labels: [server, persistence, profiles, coins]
depends_on: [STORY-0207, STORY-0209]
---

## Goal

The device-bound anonymous profile becomes real and durable: it is created on first contact, it
accumulates a row per duel it played, and its coin balance is `wins − losses` — a number the rest
of the system can trust after a restart.

## Why

[`ADR-0012`](../../docs/adr/ADR-0012-device-bound-anonymous-profiles.md) makes the profile the
thing a duel result attaches to, and [`ADR-0011`](../../docs/adr/ADR-0011-postgres-in-v01.md) makes
it durable, because *"the process restarted" cannot be an acceptable way to lose a coin balance*.
`docs/vision.md` is blunt about it: the reward in this game **is** the record.

This is where the two halves of the epic meet — the duel chain produces a result, the database
chain stores it — and it implements the two ports their consumers declared.

## Design notes

- This story implements ports it does not own: `PlayerDirectory` (declared in `STORY-0205`) and
  `DuelResultSink` (declared in `STORY-0207`), against the schema from `STORY-0209`. That is
  `ADR-0011`'s repository boundary: the game logic never acquires a database shape.
- Profile creation per `ADR-0012`: a device id seen for the first time creates exactly one profile
  row and returns it; the same id thereafter returns the same profile. Creation happens on the
  socket handshake only — see `STORY-0211` for why a stray HTTP GET must not mint one.
- **The coin rule, in one place, per
  [`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md):** the winner `+1`, the loser `−1`, a
  draw `0` to both. It reads `DuelOutcome` — including `DuelOutcome.isDraw`, which the engine
  already computes — and maps it to two signed deltas. That mapping is one function, because
  `ADR-0014` explicitly anticipates being superseded by a floating or opponent-weighted award, and
  a rule expressed once is a rule that can be replaced once.
- **Balances are signed and are never floored.** A player whose only duel was a loss is at `−1`,
  and that is the intended, reachable-on-day-one case, not an error state. Any `coerceAtLeast(0)`,
  `max(0, …)` or unsigned type in this story is a defect, and `ADR-0014` says why: flooring makes a
  losing streak indistinguishable from never having played.
- One transaction per finished duel: the duel row, both participant rows and both coin deltas go in
  together or not at all. A duel result without its coin rows is an inconsistency nobody would
  notice until it mattered.
- **Idempotent on duel id.** Recording the same finished duel twice — a retry, a reconnect, a
  redelivery — awards coins once. This is the failure the ledger shape makes cheap to prevent and
  expensive to repair.
- Concurrency: two duels finishing at once for one player must both land. Balance is either derived
  by summing deltas or updated with an atomic increment in SQL — never read-modify-write in Kotlin.
- No auth, no claim flow, no display-name editing: `ADR-0012` puts all of that in `EPIC-04`, which
  it also obliges to exist.
- **`DEC-014` — open.** `STORY-0211`'s result line wants the hands played in each duel, and the
  `duel` table has no column for it. `DuelOutcome.handsPlayed` exists only at the moment this story
  writes the row, so a column added later cannot be backfilled for duels already played. Does
  `duel` gain `hands_played` in a `V2` migration, written here before any real duel exists — or
  does the read path do without it? Nothing in this story is blocked either way: the answer is
  strictly additive to the write path.
- The write path is a plain `PostgresDuelResultStore`, not a `DuelResultSink` implementation.
  `STORY-0207` declares that port at its consumer, the duel runner, and points it here; this story
  cannot implement a signature that does not exist yet, and pre-empting it would leave two
  declarations of the same port.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-021001](../tasks/TASK-021001-coin-award-rule.md) | Map a `DuelOutcome` to two signed coin deltas, in one function | ready |
| [TASK-021002](../tasks/TASK-021002-finished-duel-record.md) | Describe a finished duel as the write path's input | backlog |
| [TASK-021003](../tasks/TASK-021003-postgres-player-directory.md) | Resolve a device id to a durable profile, creating it at most once | backlog |
| [TASK-021004](../tasks/TASK-021004-concurrent-first-contact.md) | Prove concurrent first contact from one device creates one profile | backlog |
| [TASK-021005](../tasks/TASK-021005-duel-result-store-rows.md) | Record a finished duel as one duel row and two result rows, in one transaction | backlog |
| [TASK-021006](../tasks/TASK-021006-move-the-coins-in-the-same-transaction.md) | Move both coin balances inside the same transaction, by SQL increment | backlog |
| [TASK-021007](../tasks/TASK-021007-failed-write-leaves-no-row.md) | Prove a failure part-way through recording leaves no row and no coin behind | backlog |
| [TASK-021008](../tasks/TASK-021008-a-draw-pays-nothing.md) | Prove a drawn duel is recorded and pays nobody | backlog |
| [TASK-021009](../tasks/TASK-021009-idempotent-on-duel-id.md) | Make recording idempotent on the duel id, so a retry pays once | backlog |
| [TASK-021010](../tasks/TASK-021010-balances-are-never-floored.md) | Prove ten losses read back as minus ten, and that nothing floors a balance | backlog |
| [TASK-021011](../tasks/TASK-021011-concurrent-duels-both-land.md) | Prove two duels finishing at once for one player both land | backlog |
| [TASK-021012](../tasks/TASK-021012-survives-a-restart.md) | Prove profiles, results and balances survive a restart | backlog |

## Acceptance criteria

- [ ] A device id seen for the first time creates exactly one profile; seeing it again creates
      none. Asserted under concurrent first contact from two connections.
- [ ] Recording a finished duel writes one duel row, two participant rows and two coin deltas in a
      single transaction; an induced failure part-way leaves no row behind.
- [ ] Recording the same finished duel twice awards coins once, asserted on the balance.
- [ ] The winner's balance rises by exactly 1 and the loser's falls by exactly 1; a drawn duel
      moves neither.
- [ ] **A brand-new profile whose only duel was a loss has a balance of exactly `−1`** — stored,
      read back and unclamped.
- [ ] Ten consecutive losses give `−10`; no code path clamps, floors or absolute-values a balance.
- [ ] The coin award lives in exactly one function; a test asserts the mapping from `DuelOutcome`
      to deltas for win, loss and draw in one place.
- [ ] Profiles, results and balances survive a restart, asserted by restarting the application
      against the same container and reading them back.

## Out of scope

- Reading a profile's results back for display — `STORY-0211`. This story owns the write path and
  the balance's correctness.
- Ranking, seasons, leaderboards or any second currency — `EPIC-05`.
- Accounts, credentials, the claim flow `ADR-0012` demands — `EPIC-04`.
- Persisting the `MatchLog` — `DEC-008`.
