---
schema: 2
id: TASK-130808
title: The room gives up the turn of a seat that is out of time or absent
type: task
status: backlog
parent: STORY-1308
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, clock, duel]
depends_on: [TASK-130807]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.RoomAbsentSeatTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.RoomAbsentSeatTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==18 else 1)"
  - sh -c '! grep -q "foldAbsentSeats" poker-server/src/main/kotlin/duels/poker/server/room/Room.kt'
  - sh -c 'test "$(grep -c "giveUpDecision(" poker-server/src/main/kotlin/duels/poker/server/room/Room.kt)" -eq 1'
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`Room.foldAbsentSeats(seeds)` becomes `Room.giveUpTurn(now, seeds)`, widened from *the seat on turn
is absent* to *the seat on turn is **out of time** or absent* — and an expiry costs that seat
exactly one decision, through `giveUpDecision`, never a second path into the engine
(`ADR-0113` §§4, 5).

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomAbsentSeatTest.kt` | modify |

## Scope

- Rename `foldAbsentSeats(seeds)` to `giveUpTurn(now: Long, seeds: HandSeedSource)` and widen it:
  it moves when the seat on turn is in `absentSeats` **or** when `turnDeadline` names that seat and
  `now >= turnDeadline.expiresAt`.
- The **timed-out** seat's decision is given up **once**, by
  `giveUpDecision(DuelStep(runner, emptyList()), seat, seeds)`, and that seat is **not** put into
  `foldAbsent`'s absent set. The result is then handed to `foldAbsent(step, absentSeats, seeds)`,
  exactly parallel to `Room.act`'s `foldAbsent(advanceDuel(…), absentSeats, seeds)`. *An expiry
  costs the seat one decision* is enforced by which argument the seat is passed as.
- **Presence decides the latch, not the clock.** A seat that timed out while its socket was down —
  today, a seat in `gracePeriods` — moves into `absentSeats` in the same returned room, and is
  thereafter played without delay at every decision the turn brings it. A seat that timed out while
  connected latches nothing and is never `ABSENT`.
- **The both-gone abandon is checked before the give-up, not after**: a room whose give-up would
  leave both seats `ABSENT` is returned abandoned instead, with no frames — today's behaviour,
  unchanged in meaning.
- Still pure and total, still `null` when nothing moves, still never throwing.
- `RoomRegistry`'s existing second pass calls `it.giveUpTurn(now, handSeeds)` with the `now` that
  pass already read once.

## Out of scope

- **The sweep's own shape** — its two passes, its pre-check and its type names are
  `TASK-130809`. This ticket only changes what the second pass calls.
- `gracePeriods` → `awaySeats`, `isPaused`, `presenceOf`'s arity — `TASK-130810`. Read *away* as
  `seat in gracePeriods` here; the rename comes later and changes no behaviour.
- Any forfeit, in any form. `ADR-0108` §3 and `ADR-0046` §5: the word does not appear.

## Tests

`RoomAbsentSeatTest` — 11 today, **18** after. Every existing test stands; the calls to
`foldAbsentSeats(seeds)` become `giveUpTurn(now, seeds)` with a `now` before any deadline, so their
subject — an absent seat on turn is played — is unchanged and no assertion is weakened.

| Test | Proves |
| --- | --- |
| `aSeatOutOfTimeIsPlayedThoughItIsPresent` | With `turnDeadline.expiresAt <= now` and an empty `absentSeats`, the turn is given up |
| `aSeatInsideItsDeadlineIsNotPlayed` | At `expiresAt - 1` the method answers `null` and the runner is identical |
| `aTimedOutSeatGivesUpExactlyOneDecision` | When the give-up leaves the same seat on turn again, that second decision is **not** taken: exactly one action is added to the log |
| `aTimedOutConnectedSeatIsNeverLatchedAbsent` | After the give-up, `absentSeats` is still empty |
| `aTimedOutAwaySeatIsLatchedAbsent` | A seat that was away when its clock ran out is in `absentSeats` in the returned room |
| `aLatchedSeatsNextDecisionIsPlayedAtTheSameInstant` | The following decision that lands on that latched seat is played in the same call, with no deadline in between |
| `bothSeatsGoneAbandonsInsteadOfPlayingItOut` | A give-up that would leave both seats `ABSENT` returns an abandoned room and no frames |

## Acceptance criteria

- [ ] Each of the seven tests above passes, by name
- [ ] `RoomAbsentSeatTest` reports exactly **18** tests
- [ ] `Room.kt` names `foldAbsentSeats` nowhere
- [ ] `Room.kt` contains exactly **one** `giveUpDecision(` call site — the expiry reuses
      `ADR-0023`'s conduct rather than re-deciding it
- [ ] `./gradlew check -PrequireDocker=true` is green, so no merged sweep or socket test changed
      behaviour
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
