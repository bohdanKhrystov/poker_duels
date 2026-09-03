---
schema: 2
id: TASK-130801
title: Extract one seat's given-up decision from foldAbsent's loop
type: task
status: done
parent: STORY-1308
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, duel]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.duel.AbsentSeatsTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.duel.AbsentSeatsTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==22 else 1)"
  - sh -c 'test "$(grep -c "legalActions(" poker-server/src/main/kotlin/duels/poker/server/duel/AbsentSeats.kt)" -eq 1'
  - ./gradlew :poker-server:check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`AbsentSeats.kt` exposes `giveUpDecision(step, seat, seeds)` — the single-seat body `foldAbsent`'s
loop already runs — so that the turn clock's expiry can reuse `ADR-0023`'s conduct as **code**
rather than re-implementing it (`ADR-0113` §4).

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/AbsentSeats.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/duel/AbsentSeatsTest.kt` | modify |

## Scope

- Extract the body of `foldAbsent`'s `while (true)` loop — the `legalActions` read, the
  `Fold`/`Check` choice, the `Act` frame, the `act` call and the `ActedForAbsent` mark addressed
  to **both** seats — into `public fun giveUpDecision(step: DuelStep, seat: Int, seeds: HandSeedSource): DuelStep`.
- `giveUpDecision` returns `step` **unchanged and identical** (`===`) when there is no live hand,
  no seat on turn, the seat on turn is not `seat`, neither `FOLD` nor `CHECK` is legal, or the
  decision point cannot be read. It never loops.
- `foldAbsent` keeps its signature, its KDoc and its behaviour **verbatim**, and calls
  `giveUpDecision` from its loop: `absent` still decides which seats it plays, and the loop still
  stops the moment a call makes no progress.
- The legal-set read exists **once** in the file after this ticket — a second copy is the
  re-implementation `ADR-0113` §4 refuses, and the `legalActions(` count gate gate is what says so.

## Out of scope

- Any clock, deadline or timebank. `giveUpDecision` takes no `now`. `TASK-130808` is the caller
  that decides a seat is out of time.
- Any behaviour change to `foldAbsent`. Every existing `AbsentSeatsTest` assertion stands
  unchanged; this ticket only adds tests.
- `Room`, `RoomRegistry`, the wire, the engine.

## Tests

`AbsentSeatsTest` — 19 tests today, 22 after this ticket. **No existing test is edited or
removed**; three are added.

| Test | Proves |
| --- | --- |
| `giveUpDecisionFoldsTheSeatFacingABet` | With a bet outstanding, `giveUpDecision` submits `Fold` for the named seat and prepends one `ActedForAbsent` addressed to seat 0 and seat 1 |
| `giveUpDecisionChecksTheSeatOwedNothing` | At a free decision point, it submits `Check` — never a call, bet, raise or all-in |
| `giveUpDecisionLeavesAStepItCannotActOnIdentical` | Called for the seat that is **not** on turn, it returns the very step it was given (`assertSame`), marks nothing and submits nothing |

## Acceptance criteria

- [ ] `AbsentSeatsTest.giveUpDecisionFoldsTheSeatFacingABet` passes
- [ ] `AbsentSeatsTest.giveUpDecisionChecksTheSeatOwedNothing` passes
- [ ] `AbsentSeatsTest.giveUpDecisionLeavesAStepItCannotActOnIdentical` passes
- [ ] `AbsentSeatsTest` reports exactly **22** tests, so the three above exist and the nineteen
      already there still run
- [ ] `AbsentSeats.kt` contains exactly **one** `legalActions(` call site
- [ ] `./gradlew :poker-server:check` is green, so `foldAbsent`'s merged behaviour is unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
