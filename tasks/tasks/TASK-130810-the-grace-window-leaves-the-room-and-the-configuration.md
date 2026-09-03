---
schema: 2
id: TASK-130810
title: The grace window leaves the room and the configuration, and away becomes a lookup
type: task
status: ready
parent: STORY-1308
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 18
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
**Say so in the PR body.** **The re-probe ran, and the count moved 17 → 18.** Re-derived against `develop` at `6f274055`,
after `TASK-130801`–`TASK-130809` landed, the reference set gains
`poker-server/src/test/kotlin/duels/poker/server/room/TurnClockFramesTest.kt`: it uses
`disconnectGraceMillis` and `RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS` in five places, including
a default parameter, and this ticket deletes that constant.

**This is the second provisional count that file has broken.** `TASK-130806` created it, and both
`TASK-130809` (9 → 10) and this ticket were sized before it existed. A count computed against one
baseline is short by whatever landed after — which is the whole reason the re-probe below was
promised rather than the count trusted.

The one entry absent from today's tree, `TurnClockConfigTest.kt`, is the file this ticket *creates*
by renaming `GraceWindowConfigTest.kt`, and is correctly not in the reference set.

**This count is provisional, and `ADR-0070` §4 is not the way to correct it.** §4 lets a coder add
a path its own green run names, but its closing sentence excludes *"a rename, a refactor"* by name
— and deleting the grace window across room and configuration is a refactor. So do **not** add a
row under §4 here.

The reason the count is a reading rather than a green probe is structural, not an omission: this
ticket's baseline is the tree left by `TASK-130809`, which does not exist while the story is being
planned, so no probe could have reached green. The **scheduler** re-runs `ADR-0070` §1's loop
against that baseline and replaces this table before the ticket is dispatched, exactly as
`ADR-0070` §5 re-sized `TASK-021301` after it was written. If the loop still names a path this
table lacks, **stop and say so** — that is a finding about the sizing, not a licence to widen it.

## Two tests in this ticket's files are already vacuous, and compiling them is not enough

`TASK-130805`'s review proved by mutation that two of the tests this ticket must touch assert
nothing today. They are not broken by this ticket — they were hollowed out when the wire stopped
carrying a remaining-duration — but this ticket is where they come to hand, and a mechanical
arity fix would leave two green tests guarding nothing under names that promise otherwise.

- **`RoomPresenceProjectionTest.theOtherSeatsWindowIsTheOneReported`** — mutating `Room.presenceOf`
  to read `(1 - seat)` reddened four tests in the class and **not this one**: its fixture gives both
  seats a grace entry, so the swap its name describes is invisible to it. Give it a fixture where the two seats
  differ. Do **not** retire it: gate 7 pins the class at 9.
- **`RoomDisconnectTest.theRemainingIsTheConfiguredWindow`** — mutating `RoomRegistry.disconnect` to
  use `now + 1L` instead of the configured window reddened five other tests and **not this one**. It
  is now byte-identical in effect to `aDropTellsTheOtherSeatItIsAway`. With `disconnectGraceMillis`
  deleted there is no configured window left for it to be about, so retiring it is the likely answer
  — but state which, rather than leaving it compiling.

Both were confirmed by the mutation named, not by reading. Note this interacts with the pinned
count below: if `theOtherSeatsWindowIsTheOneReported` is retired rather than repaired,
`RoomPresenceProjectionTest` does not stay at **9**, and the pin must move with a measured figure.

## A live behaviour change `TASK-130809` left unpinned — and this ticket may dissolve it

`TASK-130809` deleted `RoomRegistry`'s own both-gone check and kept `Room.giveUpTurn`'s. Review
confirmed by experiment that the two disagree in exactly one direction and in one reachable state:

> one seat's grace window has fully elapsed (latched into `absentSeats`), while the *other*,
> currently-on-turn seat is still inside its own grace window but its per-decision turn clock has
> run out.

There `giveUpTurn`'s `gone = absentSeats + setOfNotNull(latched)` is `{0, 1}` and the room is
**abandoned**; the deleted check read `expired.absentSeats` alone, would have seen one seat, and
would have folded an ordinary decision instead. The superset direction is guaranteed by `Room`'s own
`init` (`require(gracePeriods.keys.none { it in absentSeats })`), so `latched` can only add.

**Nothing pins that behaviour.** Review reintroduced the deleted branch and all 21 tests stayed
green. `TurnClockExpiryTest`'s count gate is fixed at 21, so `TASK-130809` could not have added a
22nd without failing its own gate.

**Answer this before adding a test for it.** That state requires *a seat still inside its grace
window* — and this ticket deletes the grace window (`ADR-0113` §7: `gracePeriods` and
`disconnectGraceMillis` go). If no grace window exists, the state is unreachable and the concern
dissolves with it. Say which it is, in the PR:

- **unreachable after this ticket** — say so, name the deleted construct that made it reachable, and
  the concern is closed rather than carried
- **still reachable** — then it needs a test, and this ticket's own count gate must have room for it

Do not simply drop it. A behaviour change nobody pinned and nobody re-examined is how the epic's two
vacuous tests got there.

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
| `poker-server/src/test/kotlin/duels/poker/server/room/TurnClockFramesTest.kt` | modify | the Kotlin compiler: five uses of `disconnectGraceMillis` / `RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS`, including a default parameter at line 30 and a fixture at 177 |

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
- [ ] `RoomPresenceProjectionTest` reports exactly **9** tests — **repair the vacuous test, do not retire it.** Gate 7 pins 9, and retiring would make it 8 and fail the block. Repair is reachable: this ticket keeps a per-seat presence notion (`awaySeats`), so give the fixture two seats in *different* states and the swap its name describes becomes visible
- [ ] `theOtherSeatsWindowIsTheOneReported` and `theRemainingIsTheConfiguredWindow` are each either repaired so the named mutation reddens them, or deleted with a sentence saying why
- [ ] `Room.kt` names `awaySeats` at least six times — the property, its `init` rules, `disconnect`,
      `reconnect` and `presenceOf`
- [ ] `./gradlew check -PrequireDocker=true` is green
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
