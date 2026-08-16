---
id: EPIC-02
title: Duel server
type: epic
status: in-progress
module: poker-server
labels: [server, ktor, websocket, persistence]
---

## Goal

A Ktor server that two browsers can connect to and play a real duel through. It creates a room,
hands out a link, seats two players, runs `poker-engine` behind a WebSocket, sends each player
exactly the view the engine says they are entitled to, survives a dropped connection, and — when
the duel ends — writes the result and the duel coin to PostgreSQL and can read them back.

When this epic closes, one test starts the server, connects two sockets, plays a duel to a
declared winner, awards the coin, and reads the winner's balance back over HTTP. That test is the
epic; everything else is how it is made to pass.

## Why now

`EPIC-01` is done: the engine plays a hand and a whole duel, logs it, replays it, and refuses to
know anything about the outside world. It is a library nobody can reach. This epic is the thing
that makes the game exist for a second person — the first half of `docs/vision.md`'s success
condition, *"send a link, she opens it in a browser"*.

It also unblocks `EPIC-03`: the client cannot be built against a protocol that has not been
written, and per [`ADR-0003`](../../docs/adr/ADR-0003-technology-stack.md) its TypeScript types
are generated from the Kotlin ones rather than hand-written, so the definition has to exist here
first.

## Scope

- `poker-server` module, Ktor application, typed configuration.
- The wire protocol: one Kotlin definition of every message, versioned, and the TypeScript types
  generated from it.
- Per-recipient views. The engine has `StateProjection` but not the redaction half of
  [`architecture.md`](../../docs/architecture.md)'s projection table — `PlayerView` does not
  exist yet, and nothing may be broadcast until it does.
- Sessions, the socket lifecycle, and the disconnect grace period of
  [`ADR-0013`](../../docs/adr/ADR-0013-disconnect-grace-period.md).
- Rooms: create, join by code, seat exactly two, rematch.
- The duel runner: the engine behind the socket, server-authoritative per
  [`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md).
- PostgreSQL: schema, migrations, connection pool, repositories
  ([`ADR-0011`](../../docs/adr/ADR-0011-postgres-in-v01.md)).
- Device-bound anonymous profiles, duel results and duel coins
  ([`ADR-0012`](../../docs/adr/ADR-0012-device-bound-anonymous-profiles.md),
  [`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md) — winner `+1`, loser `−1`, a draw
  nothing, and a balance that may be negative).
- The **read path**: a player can ask for their coin balance and their recent duel results. Stored
  is not the same as visible, and a value nobody can ask for is a value that does not exist.
- An end-to-end test that plays a real duel over a real socket against a real database.

## Out of scope

| Not here | Where |
| --- | --- |
| Any rendering — table, lobby, results screen | EPIC-03 |
| Accounts, passwords, the claim flow `ADR-0012` demands | EPIC-04 |
| Ratings, seasons, leaderboard, coin economy beyond one counter | EPIC-05 |
| Docker images, hosting, deployment, TLS termination | EPIC-07 — a local `docker-compose` for Postgres is in scope, production delivery is not |
| Server-side bots as opponents | EPIC-09 — a bot drives the *client* side of the end-to-end test, nothing more |
| Equity, decision quality, replay analysis | EPIC-08 |
| Any change to the rules of poker | Nowhere. The engine is done and this epic adds no rule to it |
| Surviving a restart mid-duel | Deliberately not required — `ADR-0011` says in-flight duel state need not be durable |
| Full duel history with paging, filters, search | EPIC-04 — v0.1 shows a handful of recent results |

## Non-negotiables this epic is most likely to break

Stated here because they are cheap to violate at a transport boundary and expensive to notice:

- **The server never filters cards.** Redaction happens inside the engine's projection layer
  (`STORY-0204`) and the transport calls it. A `ServerMessage` that carries a `GameState`, a
  `Deck`, an `Rng` or a seed is a defect, and `STORY-0202` asserts structurally that none can.
- **A client may never assert a game fact.** A `ClientMessage` expresses one thing: an intent.
  Whose turn it is, what an action costs and who won are all server answers.
- **The engine gains nothing.** No clock, no coroutine, no database, no Ktor type. The grace
  period is a server timer whose expiry arrives at the engine as an ordinary `Fold`.
- **Folded and mucked cards appear in no event, anywhere** — already true in the engine, and this
  epic must not weaken it.

## Stories

| ID | Title | Depends on | Status |
| --- | --- | --- | --- |
| [STORY-0201](../stories/STORY-0201-server-module-scaffold.md) | Server module and build scaffold | — | ready |
| [STORY-0202](../stories/STORY-0202-wire-protocol.md) | The wire protocol, defined once in Kotlin | 0201, 0204 | backlog |
| [STORY-0203](../stories/STORY-0203-generated-typescript-protocol.md) | Generated TypeScript protocol types | 0202 | blocked |
| [STORY-0204](../stories/STORY-0204-player-view-projection.md) | PlayerView: per-recipient projection in the engine | — | ready |
| [STORY-0205](../stories/STORY-0205-sessions-and-socket-lifecycle.md) | Sessions and the socket lifecycle | 0202 | backlog |
| [STORY-0206](../stories/STORY-0206-rooms-and-matchmaking.md) | Rooms, join links and rematch | 0201 | backlog |
| [STORY-0207](../stories/STORY-0207-duel-runner.md) | The duel runner: the engine behind the socket | 0205, 0206 | backlog |
| [STORY-0208](../stories/STORY-0208-disconnect-grace-period.md) | Disconnect, grace period and reconnect | 0207 | backlog |
| [STORY-0209](../stories/STORY-0209-postgres-schema-and-migrations.md) | PostgreSQL: schema, migrations, connection pool | 0201 | backlog |
| [STORY-0210](../stories/STORY-0210-profiles-results-and-coins.md) | Profiles, duel results and duel coins | 0207, 0209 | backlog |
| [STORY-0211](../stories/STORY-0211-read-path-coins-and-recent-duels.md) | The read path: my coins and my recent duels | 0210 | backlog |
| [STORY-0212](../stories/STORY-0212-end-to-end-duel-over-a-socket.md) | A real duel over a real socket, end to end | 0208, 0211 | backlog |
| [STORY-0213](../stories/STORY-0213-the-wire-carries-a-rematch.md) | The wire carries a rematch | 0206, 0207 | ready |

**This epic closed on 2026-08-14 and reopened on 2026-08-16 for `STORY-0213`.**
[`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) answers
`DEC-023`: the scope line above already promised *rematch*, and the epic shipped it as far as
`RoomRegistry.offerRematch` and stopped one wire message short of anyone reaching it. The
unfinished half returns to the epic that promised it and to the module that owns the code, rather
than to `EPIC-03`, whose own rule is that a client needing a new frame raises a decision instead of
editing Kotlin. The metrics below are as measured at the first close and are **not** re-measured for
this story; the reopening is recorded rather than hidden.

## What can run in parallel

`EPIC-01` was throttled by chains that were accidental rather than real. These are the branches
that are genuinely independent — no shared file, no shared type — and they should be worked as
branches, not as a queue:

- **Two roots.** `STORY-0201` (`poker-server`) and `STORY-0204` (`poker-engine`) share no module
  and no file. Both are startable the moment the epic opens.
- **Three branches off the scaffold.** Once `STORY-0201` merges, `STORY-0202` (protocol),
  `STORY-0206` (rooms) and `STORY-0209` (database) are mutually independent. Rooms deliberately
  know nothing about JSON — the registry is a plain component over `PlayerId`s — and the database
  story touches no protocol or room file. They meet later, at `STORY-0207` and `STORY-0210`.
- **`STORY-0203` is a leaf.** Nothing in this epic depends on the generated TypeScript;
  `EPIC-03` does. It is blocked on `DEC-007` and its being blocked stalls nothing here.
- **`STORY-0205` and `STORY-0209`** run alongside each other for the whole of their length.

And the honest non-parallelism, recorded so nobody tries to break it:

- `STORY-0207` wires the runner into the WebSocket route that `STORY-0205` owns, so it waits on
  it. Two stories editing that route at once would conflict on every ticket.
- `STORY-0202` waits on `STORY-0204` because a `ServerMessage` carries a `PlayerView`, and a
  protocol defined against a type that does not exist would be rewritten as soon as it did.

**Critical path:** `0201 → 0202 → 0205 → 0207 → 0210 → 0211 → 0212`. Everything else has slack.

## Open decisions

| ID | Question | Blocks |
| --- | --- | --- |
| `DEC-008` | Is the full `MatchLog` persisted in v0.1, and where — a column, a table per hand, or object storage? | nothing; out of scope for `STORY-0209` until answered. Due before `EPIC-08` |

`DEC-007` is **answered**, by
[`ADR-0020`](../../docs/adr/ADR-0020-typescript-protocol-from-serial-descriptors.md): an emitter
we own over the `SerialDescriptor`s, with a byte-comparing verify task failing CI on drift.
`STORY-0203` shipped on it.

The coin economy is **not** open: [`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md)
settles it. Winner `+1`, loser `−1`, draw nothing, balance `= wins − losses` and **may be
negative** — signed everywhere, floored nowhere.

## Definition of done

- [ ] Every story is `done`.
- [ ] One test starts the server, connects two real WebSocket clients, and plays a duel from the
      first deal to a declared winner.
- [ ] Across that duel, no frame delivered to either client contained the opponent's hole cards
      before the engine revealed them.
- [ ] An action from the seat not on turn, or answering a stale sequence, changes nothing.
- [ ] A dropped connection resumes inside the grace period and folds after it, with no test
      sleeping on a real clock.
- [ ] The winner's coin balance and their recent duel results can be read back over HTTP, and
      both survive a server restart.
- [ ] A player whose only duel was a loss reads back a balance of `−1`, unclamped.
- [ ] `poker-engine` still declares no dependency outside the `ADR-0010` allowlist.

## Metrics

Measured at the first close, 2026-08-14. `STORY-0213`, added when the epic reopened, is deliberately
outside this ledger — see the note under the stories table. Feeds the Product B case study.

| | |
| --- | --- |
| Tasks completed | |
| Accepted on first review | |
| Average review iterations | |
| Test lines / production lines | |
| Tasks re-scoped mid-flight | |
| Manual human edits | |
