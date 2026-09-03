---
schema: 2
id: TASK-130803
title: The turn allowance and the timebank are read from configuration at startup
type: task
status: done
parent: STORY-1308
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, clock, config]
depends_on: [TASK-130802]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.config.ServerConfigTest'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.config.ServerConfigTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==47 else 1)"
  - sh -c 'test "$(grep -c "duel.turnMillis" poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt)" -eq 1'
  - sh -c 'test "$(grep -c "duel.timebankMillis" poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt)" -eq 1'
  - ./gradlew :poker-server:check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ServerConfig` reads `duel.turnMillis` / `TURN_MILLIS` and `duel.timebankMillis` /
`TIMEBANK_MILLIS` once at startup and hands both to `roomTimeouts()`, so a deployment can move
either number without a code change (`ADR-0113` §3, `ADR-0013`'s rule inherited).

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

## Scope

- Add `turnMillis: Long = RoomTimeouts.DEFAULT_TURN_MILLIS` and
  `timebankMillis: Long = RoomTimeouts.DEFAULT_TIMEBANK_MILLIS` to `ServerConfig`, defaulted for
  the same reason `disconnectGraceMillis` is.
- Add the four companion constants beside the disconnect window's, in the same shape:
  `DEFAULT_TURN_MILLIS`, `TURN_MILLIS_KEY = "duel.turnMillis"`, `TURN_MILLIS_ENV = "TURN_MILLIS"`,
  and the three matching `…TIMEBANK…` ones.
- `from(...)` resolves both through the existing `resolve(...)` helper — environment over
  configuration key over default — and refuses a non-integer with the same
  `requireNotNull(… .toLongOrNull())` message shape the window already uses.
- `roomTimeouts()` passes both into `RoomTimeouts` by **name**, so a later field cannot be picked
  up positionally.

## Out of scope

- **Removing `duel.disconnectGraceMillis` / `DISCONNECT_GRACE_MILLIS`.** They stay until
  `TASK-130810`; this ticket is purely additive.
- Anything that *uses* either number. `TASK-130804` computes the deadline; `TASK-130806` reads
  `timeouts.turnMillis`.
- `GraceWindowConfigTest` — untouched here; `TASK-130810` replaces it.

## Tests

`ServerConfigTest` — 41 tests today, 47 after this ticket. **No existing test is edited or
removed**; six are added.

| Test | Proves |
| --- | --- |
| `theShippedTurnAllowanceIsTheDeclaredOne` | An empty `MapApplicationConfig` yields `RoomTimeouts.DEFAULT_TURN_MILLIS` |
| `theShippedTimebankIsTheDeclaredOne` | An empty `MapApplicationConfig` yields `RoomTimeouts.DEFAULT_TIMEBANK_MILLIS` |
| `theKeyMovesTheTurnAllowance` | `"duel.turnMillis" to "45000"` yields `45_000L` |
| `theKeyMovesTheTimebank` | `"duel.timebankMillis" to "90000"` yields `90_000L` |
| `theEnvironmentOutranksTheKeyForBothNumbers` | With both keys set **and** `TURN_MILLIS`/`TIMEBANK_MILLIS` in the environment, both fields carry the environment's values — asserted for **both** numbers with **different** values, so one resolver serving both cannot pass |
| `aNonIntegerRefusesBothNumbers` | `"duel.turnMillis" to "half a minute"` and `"duel.timebankMillis" to "three"` each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `ServerConfigTest.theShippedTurnAllowanceIsTheDeclaredOne` passes
- [ ] `ServerConfigTest.theShippedTimebankIsTheDeclaredOne` passes
- [ ] `ServerConfigTest.theKeyMovesTheTurnAllowance` passes
- [ ] `ServerConfigTest.theKeyMovesTheTimebank` passes
- [ ] `ServerConfigTest.theEnvironmentOutranksTheKeyForBothNumbers` passes
- [ ] `ServerConfigTest.aNonIntegerRefusesBothNumbers` passes
- [ ] `ServerConfigTest` reports exactly **47** tests
- [ ] `ServerConfig.kt` names `duel.turnMillis` exactly once and `duel.timebankMillis` exactly
      once — each key is declared in one constant and never repeated as a literal
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
