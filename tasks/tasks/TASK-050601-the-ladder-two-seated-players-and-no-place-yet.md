---
schema: 2
id: TASK-050601
title: The ladder, read from the same application the duel is played in — two seated players and no place yet
type: task
status: ready
parent: STORY-0506
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, e2e, leaderboard, tests]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.theLadderKnowsBothDuellistsAndPlacesNeitherBeforeTheyPlay' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`GET /api/standings` is readable over HTTP from inside the same running application that hosts the
two WebSocket clients, and the *before* half of every later assertion in this story exists: two
seated players who have finished nothing are on no ladder and hold no place.

## Why this is the first ticket and not a formality

Every criterion in `STORY-0506` is a **difference**, and a difference needs a before. This ticket
is that before, and it is the one place in the story where the answer is *nothing*: `ADR-0061` §4 —
*"the ladder is results, not players"* — so two profiles that exist and have duelled nobody appear
on no row. `ADR-0065` §4's three answers are what distinguish *"known, and placed nowhere"* from
*"who are you"*, and both are asserted here because a `self` that collapsed them would still look
right on every later test in this class.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt` | create |

Read, do not edit:

- `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketCoinsTest.kt` — the class shape to
  copy: `@Timeout(120)`, `@BeforeEach` calling `PostgresTestSupport.requireDocker()` then
  `freshMigratedDatabase()`, and a private `HttpClient.profileOf` that decodes with `protocolJson`.
- `poker-server/src/test/kotlin/duels/poker/server/e2e/E2eServer.kt` — `installDuelServer`,
  `freshMigratedDatabase`.
- `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` — `openSocketDuel`,
  `HOST_DEVICE`, `GUEST_DEVICE`, `SocketDuel.seat`.
- `poker-server/src/main/kotlin/duels/poker/server/protocol/http/StandingsDtos.kt` —
  `StandingsResponse`, `StandingRow`, `SelfStandingResponse`.

## Scope

- A new test class `SocketLadderTest` in `duels.poker.server.e2e`, set up exactly like
  `SocketCoinsTest`: `@Timeout(120)`, `PostgresTestSupport.requireDocker()` as the **first**
  statement of `@BeforeEach`, then `dataSource = freshMigratedDatabase()`.
- One private read helper on the class:

  ```kotlin
  private suspend fun HttpClient.ladder(deviceId: String?, limit: Int = LADDER_LIMIT, after: String? = null): StandingsResponse
  ```

  It builds `/api/standings?limit=$limit` (appending `&after=$after` only when `after` is
  non-null), sets the `DEVICE_ID_HEADER` header only when `deviceId` is non-null, asserts the
  status is `200 OK` with a message naming the device id, and decodes the body with
  `protocolJson.decodeFromString` — never client-side content negotiation, exactly as
  `SocketCoinsTest.profileOf` does.
- A private `const val LADDER_LIMIT = 10` at file scope, used as the helper's default.
- The one test below.
- No production file is created or modified by this ticket; every file it touches is under
  `poker-server/src/test/`.

## Out of scope

- **Any recorded or played duel.** Nothing in this ticket writes a `duel_result` row; the fixture
  helpers that do arrive in `TASK-050602`.
- **Walking more than one page.** `walkLadder` arrives in `TASK-050605`, which is the first test
  that needs it.
- **Asserting the season string against a literal.** The season here is whichever month the test
  runs in, so there is no golden string to write. Assert only that `season` is non-blank; naming
  the month is `TASK-050303`'s, on the client.
- **A `playerId` query parameter** — `ADR-0065` §3 and `ADR-0067` §2. No request in this file, in
  this ticket or any later one, carries one.

## Tests

`SocketLadderTest`, `-PrequireDocker=true`.

**Fixture.** A fresh migrated database, `installDuelServer(dataSource)`, and `openSocketDuel()` —
which resolves a profile for `HOST_DEVICE` and one for `GUEST_DEVICE` and seats them. No duel is
played and no result is recorded.

| Test | Proves |
| --- | --- |
| `theLadderKnowsBothDuellistsAndPlacesNeitherBeforeTheyPlay` | four requests to the same endpoint against the same fixture: with `HOST_DEVICE` and with `GUEST_DEVICE`, `rows` is **empty** and `self` is that device's own `playerId` with `rank == null` and `coins == null`; with the device id `"e2e-stranger"`, which this server has never seen, `self` is **`null`**; with no `X-Device-Id` header at all, `self` is **`null`**. Every response is `200` — this endpoint has no `401` |

Take the two `playerId` values to compare `self` against from
`PostgresProfileReads(dataSource).profileOf(DeviceId(HOST_DEVICE))` and the same for the guest, so
the assertion compares two independently obtained values rather than the response with itself.

**Named mutations.** Listing every `player` row on the ladder instead of every player with a
result row — the `ADR-0061` §4 mistake — puts two empty-handed profiles on the ladder and reddens
the `rows.isEmpty()` half. Collapsing `ADR-0065` §4's three answers into two, by answering
`SelfStandingResponse(id, null, null)` for an unknown device or `null` for a known one with no
duels, reddens the `"e2e-stranger"` half or the `HOST_DEVICE` half respectively — which is why one
device id is not enough here.

## Acceptance criteria

- [ ] `SocketLadderTest.theLadderKnowsBothDuellistsAndPlacesNeitherBeforeTheyPlay` passes
- [ ] That test makes **four** requests: `HOST_DEVICE`, `GUEST_DEVICE`, `"e2e-stranger"`, and one
      with no `X-Device-Id` header, and asserts `200` on all four
- [ ] It asserts `rows` is empty, `self.rank == null` and `self.coins == null` for both duellists,
      and `self == null` for both the unknown device and the header-less request
- [ ] `PostgresTestSupport.requireDocker()` is the first statement of the class's `@BeforeEach`, so
      a missing Docker daemon fails the build under `-PrequireDocker=true` instead of skipping
- [ ] Every file this ticket creates or modifies is under `poker-server/src/test/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
