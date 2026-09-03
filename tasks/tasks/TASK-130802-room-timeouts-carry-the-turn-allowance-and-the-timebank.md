---
schema: 2
id: TASK-130802
title: RoomTimeouts carries the turn allowance and the timebank
type: task
status: done
parent: STORY-1308
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, clock, config]
depends_on: [TASK-130801]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.room.RoomTimeoutsTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.room.RoomTimeoutsTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==10 else 1)"
  - sh -c 'test "$(grep -c "30 \* 1000L" poker-server/src/main/kotlin/duels/poker/server/room/RoomTimeouts.kt)" -eq 1'
  - ./gradlew :poker-server:check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`RoomTimeouts` carries `turnMillis` and `timebankMillis`, both required positive and both
defaulted to `ADR-0108` §1's numbers, so the turn clock's two durations are **configuration and
not literals** before anything reads them.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomTimeouts.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomTimeoutsTest.kt` | modify |

## Scope

- Add `val turnMillis: Long = DEFAULT_TURN_MILLIS` and
  `val timebankMillis: Long = DEFAULT_TIMEBANK_MILLIS` to `RoomTimeouts`, after
  `disconnectGraceMillis`, each with `require(… > 0)` in `init`.
- Add `DEFAULT_TURN_MILLIS = 30 * 1000L` and `DEFAULT_TIMEBANK_MILLIS = 3 * 60 * 1000L` to the
  companion, and pass both into `RoomTimeouts.DEFAULT`.
- Both fields carry defaults for the reason `disconnectGraceMillis` already gives in its own
  comment: a reaping test builds `RoomTimeouts(waitingMillis = …, finishedMillis = …)` and must
  not have to state a turn clock.
- The KDoc names the two durations and cites `ADR-0108` §1 for the numbers and
  `ADR-0113` §3 for where they are read.

## Out of scope

- **Removing `disconnectGraceMillis`.** It stays until `TASK-130810`; this ticket is purely
  additive, so no existing caller and no existing test changes.
- Reading either value from `ServerConfig` — `TASK-130803`.
- Anything that computes a deadline from them — `TASK-130804`.

## Tests

`RoomTimeoutsTest` — 5 tests today, 10 after this ticket. **No existing test is edited or
removed**; five are added.

| Test | Proves |
| --- | --- |
| `theShippedTurnAllowanceIsThirtySeconds` | `RoomTimeouts.DEFAULT.turnMillis == DEFAULT_TURN_MILLIS`, and that constant is `30_000` |
| `theShippedTimebankIsThreeMinutes` | `RoomTimeouts.DEFAULT.timebankMillis == DEFAULT_TIMEBANK_MILLIS`, and that constant is `180_000` |
| `aNonPositiveTurnAllowanceIsRefused` | `RoomTimeouts(1, 1, turnMillis = 0)` and `… = -1` both throw `IllegalArgumentException` |
| `aNonPositiveTimebankIsRefused` | `RoomTimeouts(1, 1, timebankMillis = 0)` and `… = -1` both throw `IllegalArgumentException` |
| `aReapingTestNeedStateNoClock` | `RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000)` still constructs, and carries both defaults |

## Acceptance criteria

- [ ] `RoomTimeoutsTest.theShippedTurnAllowanceIsThirtySeconds` passes
- [ ] `RoomTimeoutsTest.theShippedTimebankIsThreeMinutes` passes
- [ ] `RoomTimeoutsTest.aNonPositiveTurnAllowanceIsRefused` passes
- [ ] `RoomTimeoutsTest.aNonPositiveTimebankIsRefused` passes
- [ ] `RoomTimeoutsTest.aReapingTestNeedStateNoClock` passes
- [ ] `RoomTimeoutsTest` reports exactly **10** tests
- [ ] `RoomTimeouts.kt` contains exactly one `30 * 1000L`, so the allowance is declared once and
      as a computed duration rather than as a bare `30000` somewhere else in the file
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
