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
- The clients choose uniformly among the `LegalActions` the *server* sent them. That is the honest
  information set — a client that picked from the engine's state would be testing the engine, which
  `EPIC-01` already did 100 000 times. `poker-ai`'s `RandomBot` is that same policy but cannot be
  called from a client: its `choose` takes a `GameState`, which is precisely the thing a client must
  not hold. So the harness mirrors it rather than importing it, exactly as `TASK-020710` already
  decided for the runner-level harness, and `TASK-021206` cites that precedent.
- Determinism: the server's per-hand seeds are injected by the test, so a failure is reproducible
  and the report names the seeds. This is the same discipline as the engine's simulation harness.
- The test asserts the security property from the only place it can be observed honestly — the
  frames as received: across the whole duel, neither client ever received a card belonging to the
  other before the engine emitted `HandRevealed`.
- One disconnection mid-duel, reconnected inside the grace period on virtual time, so `ADR-0013`'s
  happy path is covered end to end and not only in `STORY-0208`'s unit tests.
- Chip conservation is asserted from the broadcasts alone: the two players' stacks in the views
  they received sum to the duel's starting chips at every point.
- The test runs on the default `test` task, which CI already runs as `./gradlew check
  -PrequireDocker=true`. One duel over an in-memory transport costs seconds; if it ever stops being
  cheap, giving it its own tagged task the way `poker-ai`'s `soakTest` has one is a new ticket, not
  a guess made while planning.
- The story adds no production *behaviour*. It does add the composition root — `TASK-021201` and
  `TASK-021202` — because the socket and both HTTP routes are still installed only by tests, and
  their own KDoc names this story as the owner of the `DataSource` that would let `module()` install
  them. Wiring existing behaviour together is what "end to end" requires; anything else it discovers
  becomes a ticket against the story that owns it, per `CLAUDE.md`.

### Blocked on `DEC-019`

The composition root is where a scheduler for `RoomRegistry.reap()` and `expireGracePeriods()` would
go, and `DEC-019` has not decided what drives them. **`TASK-021212` is blocked on it and is the only
ticket here that is.** `TASK-021201` and `TASK-021202` deliberately install no sweeper — that gap is
recorded, not forgotten. Nothing else in this story needs one: the reconnect in `TASK-021211`
happens inside the window and in-process, so no window ever has to expire, and no clock is injected.

`DEC-020` — what an absent seat does where `Fold` is illegal — reaches no ticket here either.
`TASK-021211` reconnects before the turn ever comes back to the dropped seat, so no seat is ever
absent when the engine asks it to act.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-021201](../tasks/TASK-021201-the-servers-real-collaborators.md) | Build the server's real collaborators from config and a DataSource | ready |
| [TASK-021202](../tasks/TASK-021202-the-composition-root-installs-every-route.md) | One composition root installs the socket and both HTTP routes, and main calls it | backlog |
| [TASK-021203](../tasks/TASK-021203-the-route-kdocs-name-their-production-installer.md) | Both route KDocs name their production installer instead of a story that has landed | backlog |
| [TASK-021204](../tasks/TASK-021204-a-test-server-on-a-real-database.md) | A test server on a real database, with the hand seeds the test chooses | backlog |
| [TASK-021205](../tasks/TASK-021205-two-real-sockets-one-room.md) | Two real sockets create and join one room by code | backlog |
| [TASK-021206](../tasks/TASK-021206-the-clients-play-the-duel-to-a-winner.md) | The two clients play a whole duel over the socket to a declared winner | backlog |
| [TASK-021207](../tasks/TASK-021207-no-client-ever-received-the-others-cards.md) | Neither client ever received the other's hole cards before the reveal | backlog |
| [TASK-021208](../tasks/TASK-021208-chips-are-conserved-in-the-frames-received.md) | Chips are conserved in the frames the two clients actually received | backlog |
| [TASK-021209](../tasks/TASK-021209-the-coins-read-back-over-http.md) | The winner's coin is one higher and the loser's one lower, read back over HTTP | backlog |
| [TASK-021210](../tasks/TASK-021210-the-duel-in-both-recent-duel-lists.md) | The duel appears in both players' recent duels with opposite deltas | backlog |
| [TASK-021211](../tasks/TASK-021211-a-dropped-socket-rejoins-and-the-duel-ends-the-same.md) | A dropped socket rejoins inside the window and the duel ends the same way | backlog |
| [TASK-021212](../tasks/TASK-021212-something-drives-the-periodic-sweeps.md) | Something drives the periodic sweeps in the server that ships | blocked |

The chain is linear: `021201 → 021202 → 021203 → 021204 → 021205 → 021206 → 021207 → 021208 →
021209 → 021210 → 021211`. `TASK-021211` also depends on `TASK-020814`, the socket half of
reconnection, which `STORY-0208` owns. `TASK-021212` hangs off `TASK-021202` and waits on `DEC-019`.

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
