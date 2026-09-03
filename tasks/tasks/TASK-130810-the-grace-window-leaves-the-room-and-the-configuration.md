---
schema: 2
id: TASK-130810
title: The grace window leaves the room and the configuration, and away becomes a lookup
type: task
status: backlog
parent: STORY-1308
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 17
atomic:
  - the Kotlin compiler — removing a data-class property, an enum-free public val, two methods and two parameters reddens every named-argument call site and every reader in one step, and no intermediate tree has both the old and the new name
  - ktlint standard:filename — GraceWindowConfigTest holds one class, so renaming it to the subject that survives forces the file rename in the same commit
  - RoomTimeoutsTest and ServerConfigTest — merged tests assert the removed constant and the removed configuration key by name
labels: [server, clock, config, atomic]
depends_on: [TASK-130809]
verify:
  - sh -c '! grep -rq "gracePeriods\|isPaused\|expireGrace\|disconnectGraceMillis\|DISCONNECT_GRACE" poker-server/src'
  - sh -c 'test -f poker-server/src/test/kotlin/duels/poker/server/room/TurnClockConfigTest.kt'
  - sh -c 'test ! -e poker-server/src/test/kotlin/duels/poker/server/room/GraceWindowConfigTest.kt'
  - sh -c 'test "$(grep -rlF walkTopDown poker-server/src/test --include=*.kt | wc -l | tr -d " ")" -eq 5'
  - sh -c 'test "$(grep -c "awaySeats" poker-server/src/main/kotlin/duels/poker/server/room/Room.kt)" -ge 6'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.RoomPresenceProjectionTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.RoomPresenceProjectionTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==9 else 1)"
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ADR-0013`'s fixed window is gone from the server: `Room.gracePeriods` becomes
`awaySeats: Set<Int>`, `isPaused` and `expireGrace` are deleted, `presenceOf` and `disconnect` lose
their time arguments, and `disconnectGraceMillis` leaves `RoomTimeouts` and `ServerConfig`. Presence
stops being time-derived and becomes a lookup (`ADR-0113` §§3, 7).

## Why this is `atomic:`, and how the count was reached

The count is a **measured reference set**, not a green probe: `grep -rl` over `poker-server/src` for
`gracePeriods`, `isPaused`, `expireGrace(`, `presenceOf` and
`disconnectGraceMillis|DISCONNECT_GRACE` on `develop` at `360bcacf`, minus `RoomPausedTest.kt`
(deleted by `TASK-130805`), with `GraceExpiryTest.kt` read as its successor
`TurnClockExpiryTest.kt` (`TASK-130809`), plus the one file rename ktlint's filename rule forces.
**Say so in the PR body.** **This count is provisional, and `ADR-0070` §4 is not the way to correct it.** §4 lets a coder add
a path its own green run names, but its closing sentence excludes *"a rename, a refactor"* by name
— and deleting the grace window across room and configuration is a refactor. So do **not** add a
row under §4 here.

The reason the count is a reading rather than a green probe is structural, not an omission: this
ticket's baseline is the tree left by `TASK-130809`, which does not exist while the story is being
planned, so no probe could have reached green. The **scheduler** re-runs `ADR-0070` §1's loop
against that baseline and replaces this table before the ticket is dispatched, exactly as
`ADR-0070` §5 re-sized `TASK-021301` after it was written. If the loop still names a path this
table lacks, **stop and say so** — that is a finding about the sizing, not a licence to widen it.

## Files

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify | The removals themselves: the property, the `val`, two methods and two parameters |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify | Kotlin compiler: `disconnect(seat, deadline)`, `presenceOf(seat, now)`, `room.gracePeriods` and `timeouts.disconnectGraceMillis` |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomTimeouts.kt` | modify | The removed field and its two constants |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify | Kotlin compiler: the removed field, its three constants and its `resolve` branch |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/E2eServer.kt` | modify | Kotlin compiler: it names `ServerConfig.DEFAULT_DISCONNECT_GRACE_MILLIS` |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomTimeoutsTest.kt` | modify | Merged test: three assertions name the removed constant and field |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify | Merged test: five assertions name the removed key, env var and field |
| `poker-server/src/test/kotlin/duels/poker/server/room/GraceWindowConfigTest.kt` | delete | ktlint `standard:filename` — the class's subject is the window this commit removes, and a one-class file cannot outlive its class's name |
| `poker-server/src/test/kotlin/duels/poker/server/room/TurnClockConfigTest.kt` | create | The successor suite; it carries `noServerTestWaitsOnRealTime`'s `Thread.sleep` scan across **unchanged**, because that guard is the only thing in the repository enforcing `ADR-0013`'s inject-time rule |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomPresenceTest.kt` | modify | Kotlin compiler: nine `disconnect(seat, deadline)` calls and every `expireGrace` and `isPaused` reader |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomDisconnectTest.kt` | modify | Kotlin compiler: `gracePeriods`, `isPaused` and the removed timeout field |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomPresenceProjectionTest.kt` | modify | Kotlin compiler: every `presenceOf(seat, now)` call |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomAbsentSeatTest.kt` | modify | Kotlin compiler: `gracePeriods` and `isPaused` |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomResumeTest.kt` | modify | Kotlin compiler: `gracePeriods`, `isPaused`, `presenceOf` and the removed timeout field |
| `poker-server/src/test/kotlin/duels/poker/server/room/TurnClockExpiryTest.kt` | modify | Kotlin compiler: `isPaused` and the removed timeout field, carried over from `GraceExpiryTest` |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketDisconnectTest.kt` | modify | Kotlin compiler: `awaitRoom(rooms, code) { it.isPaused }` and `TEST_DISCONNECT_GRACE_MILLIS` |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketReconnectTest.kt` | modify | Kotlin compiler: `gracePeriods` and `isPaused` |

## Scope

- `Room.gracePeriods: Map<Int, Long>` → `awaySeats: Set<Int>`, with `init` keeping every seat rule
  it has today: seats in `0..1`, seat 1 only when a guest is seated, and no seat both away and
  latched absent.
- `Room.isPaused` and `Room.expireGrace(now)` are **deleted**, not deprecated. A predicate that used
  to read `it.isPaused` reads `it.awaySeats.isNotEmpty()`.
- `Room.disconnect(seat, deadline)` → `disconnect(seat)`: it marks the seat away and starts nothing.
- `Room.presenceOf(seat, now)` → `presenceOf(seat)`: `ABSENT` if latched, `AWAY` if the socket is
  down, else `PRESENT`. One lookup, no arithmetic, no clock.
- `RoomTimeouts` loses `disconnectGraceMillis`, `DEFAULT_DISCONNECT_GRACE_MILLIS` and its mention in
  `DEFAULT`; `ServerConfig` loses the field, `DEFAULT_DISCONNECT_GRACE_MILLIS`,
  `DISCONNECT_GRACE_MILLIS_KEY`, `DISCONNECT_GRACE_MILLIS_ENV` and its `resolve` branch.
- `RoomRegistry.disconnect` no longer computes a deadline; the presence frame it builds for the seat
  that stayed is `disconnected.presenceOf(seat)`, and the rule about which seats are owed a frame is
  unchanged.
- `TurnClockConfigTest` replaces `GraceWindowConfigTest` case for case: where a test drove a
  configured **window** to a fold, it drives a configured **turn allowance** to one, through
  `expireTurnClocks()`; `aLongWindowCostsNoRealTime`'s `@Timeout(5)` and
  `noServerTestWaitsOnRealTime`'s scan both move across unchanged.

## Out of scope

- Anything on the wire. `PROTOCOL_VERSION` does not move: no `ClientMessage` or `ServerMessage`
  changes shape here, `docs/protocol-versions.md` gains no row, and `SeatPresence`'s three values
  stay exactly as they are. Only the KDoc that says a grace period is running and the duel is paused
  changes.
- `absentSeats`, kept **verbatim** with its meaning intact: latched on a timeout taken while away,
  cleared by reconnect, played without delay by `foldAbsent`.
- The client — `TASK-130811` and `STORY-1309`.

## Tests

`RoomPresenceProjectionTest` stays at **9**: every `presenceOf(seat, now)` becomes `presenceOf(seat)`
and the three `AWAY`-with-a-duration expectations become `AWAY`. `RoomPresenceTest`,
`RoomDisconnectTest`, `RoomAbsentSeatTest`, `RoomResumeTest`, `TurnClockExpiryTest`,
`DuelSocketDisconnectTest` and `DuelSocketReconnectTest` each keep every test they have; only the
names and arities their assertions reach through change, and **no assertion is weakened, derived
away or deleted**. `RoomTimeoutsTest` loses its three disconnect-window assertions and
`ServerConfigTest` its five, because their subject is removed; both keep every other test.

`TurnClockConfigTest` — the successor to `GraceWindowConfigTest`'s 6 tests, at **6**:

| Test | Proves |
| --- | --- |
| `theShippedDefaultsAreTheDeclaredOnes` | An empty config yields `RoomTimeouts.DEFAULT_TURN_MILLIS` and `DEFAULT_TIMEBANK_MILLIS` |
| `aFiveSecondAllowanceExpiresAtFiveSeconds` | With `duel.turnMillis = 5000` and `duel.timebankMillis = 1`, no expiry at 5 000 ms and a fold at 5 001 ms |
| `aFortyFiveSecondAllowanceExpiresAtFortyFiveSeconds` | The same at a second, different value, so a constant cannot pass |
| `theEnvironmentAloneMovesTheAllowance` | `TURN_MILLIS` in the environment with no configuration key moves the instant the fold happens |
| `aLongAllowanceCostsNoRealTime` | `@Timeout(5)` with the clock advanced 45 s — the injected clock, not the wall |
| `noServerTestWaitsOnRealTime` | Carried across **unchanged**: no file under `poker-server/src/test` contains `Thread.sleep` |

## Acceptance criteria

- [ ] `gracePeriods`, `isPaused`, `expireGrace`, `disconnectGraceMillis` and `DISCONNECT_GRACE`
      appear nowhere under `poker-server/src`
- [ ] `TurnClockConfigTest.kt` exists, `GraceWindowConfigTest.kt` does not, and each of the six
      tests above passes by name
- [ ] Exactly **5** files under `poker-server/src/test` still use `walkTopDown`, so the
      `Thread.sleep` scan moved with its file and no source-scanning guard was lost
- [ ] `RoomPresenceProjectionTest` reports exactly **9** tests
- [ ] `Room.kt` names `awaySeats` at least six times — the property, its `init` rules, `disconnect`,
      `reconnect` and `presenceOf`
- [ ] `./gradlew check -PrequireDocker=true` is green
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
