---
schema: 2
id: TASK-021403
title: Room.presenceOf projects a seat's presence from state the room already keeps
type: task
status: done
parent: STORY-0214
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, rooms, presence]
depends_on: [TASK-021402]
verify:
  - ./gradlew :poker-server:test --tests '*RoomPresenceProjectionTest'
  - ./gradlew :poker-server:test --tests '*RoomPresenceTest'
---

## Goal

`Room.presenceOf(seat, now)` answers the `OpponentPresence` frame describing that seat, derived from
`gracePeriods` and `absentSeats` alone. `Room` stores no new field and still reads no clock.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomPresenceProjectionTest.kt` | create |

Read `room/RoomPresenceTest.kt` for the `waitingRoom()`/`playingRoom()` fixtures to mirror,
`protocol/SeatPresence.kt`, and `docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md` §2.
Nothing else.

## Scope

- `public fun presenceOf(seat: Int, now: Long): ServerMessage.OpponentPresence` on `Room`, KDoc'd.
  A `Room` method returning a `ServerMessage` is precedented — `Room.act` builds the `DUEL_PAUSED`
  failure today.
- The three branches are exactly the three states `Room` already distinguishes:
  `seat in gracePeriods` → `AWAY` with `deadline - now`; `seat in absentSeats` → `ABSENT`;
  otherwise → `PRESENT`.
- **Clamped at zero.** A deadline already past yields `AWAY` with `0`, never a negative — which the
  frame's own `require` would refuse anyway, so the clamp is what keeps this function total.
- **No new field, anywhere.** No `copy`, no write-back, no clock read: `now` is handed in.
- `require(seat in 0..1)`, matching every other seat-taking member of `Room`.

## Out of scope

- **Calling it.** No `RoomRegistry`, no `DuelSocket`, no emission: `TASK-021404`, `TASK-021406`
  and `TASK-021409`.
- Reading a clock inside `Room` — `RoomRegistry` computes `now` and hands it in, and that is the
  rule `TASK-020805` already established.
- Changing `isPaused`, `gracePeriods`, `absentSeats`, `disconnect`, `reconnect` or `expireGrace`.

## Tests

`RoomPresenceProjectionTest` — a new file. Fixtures mirror `RoomPresenceTest`: `host`/`guest`,
`code = RoomCode("2B7KMNPQ")`, `playingRoom()` from `Room.open(...).join(guest, now = 2_000L)`.
No clock, no `Thread.sleep`, no `kotlin.random`.

**Every test names both seats.** A `presenceOf` that ignored its `seat` argument, or that read a
fixed seat, would pass a suite that only ever asks about one — so each row below asserts the seat
under test *and* its opposite in the same room.

| Test | Proves |
| --- | --- |
| `aSeatNobodyIsWaitingForIsPresent` | on `playingRoom()`, `presenceOf(0, 5_000)` and `presenceOf(1, 5_000)` are both `OpponentPresence(PRESENT, null)` |
| `aSeatInsideItsWindowIsAwayWithWhatIsLeft` | with `gracePeriods = mapOf(1 to 30_000L)`, `presenceOf(1, 5_000)` is `AWAY` with `25_000L`, and `presenceOf(0, 5_000)` is `PRESENT` — the pair is the assertion |
| `theOtherSeatsWindowIsTheOneReported` | with `gracePeriods = mapOf(0 to 9_000L, 1 to 30_000L)`, `presenceOf(0, 1_000)` is `AWAY` with `8_000L` and `presenceOf(1, 1_000)` is `AWAY` with `29_000L` — two different non-zero durations, so a constant cannot pass |
| `aWindowThatHasRunOutIsAwayWithZero` | with `gracePeriods = mapOf(1 to 30_000L)`, `presenceOf(1, 45_000)` is `AWAY` with `0L`, not a negative — `ADR-0028` §2's legal frame |
| `aWindowEndingExactlyNowIsAwayWithZero` | `presenceOf(1, 30_000)` is `AWAY` with `0L` — the boundary |
| `aSeatWhoseWindowRanOutIsAbsent` | with `absentSeats = setOf(1)`, `presenceOf(1, 5_000)` is `ABSENT` with `null`, and `presenceOf(0, 5_000)` is `PRESENT` |
| `bothSeatsGoneAreBothAbsent` | with `absentSeats = setOf(0, 1)`, both answer `ABSENT` |
| `aSeatOutsideTheTableIsRefused` | `presenceOf(2, 0)` and `presenceOf(-1, 0)` each throw `IllegalArgumentException` |
| `presenceOfStoresNothing` | calling `presenceOf` twice with different `now` leaves the room equal to the one it was called on — `assertEquals(room, room)` after both calls, and the second call still answers from the same `gracePeriods` |

## Acceptance criteria

- [ ] Every test method in the table above passes
- [ ] `RoomPresenceTest` passes unchanged — this ticket adds a projection and changes no state, so
      no assertion in that file is touched
- [ ] `Room.kt` gains one function and no field: the primary constructor's parameter list is
      byte-identical to the one on `develop`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
