---
id: STORY-0212
title: A real duel over a real socket, end to end
type: story
status: backlog
parent: EPIC-02
module: poker-server
labels: [server, testing, end-to-end]
depends_on: [STORY-0208, STORY-0211]
---

## Goal

One test starts the server, connects two real WebSocket clients, plays a complete duel to a
declared winner, survives a disconnection on the way, awards the coins, and reads the balance back
over HTTP. Nothing between the socket and the database is mocked.

## Why

Every other story in this epic is tested at its own boundary, and boundaries are exactly where
integrations fail: a message that serialises perfectly and is never sent, a view that redacts
correctly and is broadcast unfiltered, a coin written in a transaction nobody commits. This is the
test that would catch all three, and it is the epic's definition of done written as an executable.

It is also the closest thing to `docs/vision.md`'s success condition that can exist before there is
a client: *two players, one room, one complete duel, someone wins*.

## Design notes

- `testApplication` with the real routes, real `RoomRegistry`, real `DuelRunner`, real repositories
  and a Testcontainers PostgreSQL. If anything in the path is a double, the test is worth less than
  the sum of the unit tests it duplicates.
- Two real WebSocket clients, each doing the full handshake from `STORY-0205` and receiving a
  device id, so both profiles are created the way a browser would create them.
- The clients are driven by `poker-ai`'s `RandomBot`, choosing among the `LegalActions` the *server*
  sent them. That is the honest information set — a client that picked from the engine's state would
  be testing the engine, which `EPIC-01` already did 100 000 times.
- Determinism: the server's per-hand seeds are injected by the test, so a failure is reproducible
  and the report names the seeds. This is the same discipline as the engine's simulation harness.
- The test asserts the security property from the only place it can be observed honestly — the
  frames as received: across the whole duel, neither client ever received a card belonging to the
  other before the engine emitted `HandRevealed`.
- One disconnection mid-duel, reconnected inside the grace period on virtual time, so `ADR-0013`'s
  happy path is covered end to end and not only in `STORY-0208`'s unit tests.
- Chip conservation is asserted from the broadcasts alone: the two players' stacks in the views
  they received sum to the duel's starting chips at every point.
- If the test is fast it runs on the default `test` task. If it is not, it gets its own tagged
  Gradle task the way `poker-ai`'s `soakTest` did — visible and run by CI, never quietly excluded.
- The story adds no production behaviour. If it discovers a defect, that defect becomes a ticket
  against the story that owns it, per `CLAUDE.md`'s rule on discovering work mid-task.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Tickets are produced by `/plan-story STORY-0212`.* | — |

## Acceptance criteria

- [ ] Two clients connect, create and join a room by code, and play a full duel over a real socket
      to a `MatchFinished` naming a winner.
- [ ] No frame delivered to either client contained the opponent's hole cards before the engine
      revealed them.
- [ ] Dropping one client's socket mid-duel and reconnecting inside the window resumes the same
      duel and does not change its outcome.
- [ ] After the duel, `GET /api/me` shows the winner one coin higher and the loser one coin lower
      than before it started — the loser's balance may be negative and the assertion does not clamp
      it.
- [ ] The duel appears in both players' recent-duels responses, with opposite signed deltas.
- [ ] Chips are conserved across the duel as computed from the received frames alone.
- [ ] The whole test runs on a clean clone in CI with no setup beyond Docker, and reports the seeds
      needed to reproduce any failure.

## Out of scope

- Any browser. This is two socket clients, not Playwright — `EPIC-03` owns UI testing.
- Load, soak or performance testing. One duel proves the path works; throughput is a question
  nobody has yet.
- New production code. Every behaviour this test asserts is owned by an earlier story.
