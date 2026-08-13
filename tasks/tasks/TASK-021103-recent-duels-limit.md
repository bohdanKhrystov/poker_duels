---
schema: 2
id: TASK-021103
title: Parse, default and cap the recent-duels limit
type: task
status: backlog
parent: STORY-0211
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, http]
depends_on: [TASK-021102]
verify:
  - ./gradlew :poker-server:test --tests '*RecentDuelsLimitTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`duelLimitOrNull(raw)` turns the `limit` query parameter into the number of duels to read — ten
when it is absent, at most fifty ever, and `null` when the value is not a positive number.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecentDuelsLimit.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/RecentDuelsLimitTest.kt` | create |

Read, do not modify:
`tasks/stories/STORY-0211-read-path-coins-and-recent-duels.md` (the "recent" design note).

## Scope

- New package `duels.poker.server.http`. One file, three public declarations with KDoc:

  ```kotlin
  public const val DEFAULT_DUEL_LIMIT: Int = 10
  public const val MAX_DUEL_LIMIT: Int = 50

  public fun duelLimitOrNull(raw: String?): Int? {
      if (raw == null) return DEFAULT_DUEL_LIMIT
      // Parsed as Long, not Int: "99999999999" is a limit above the cap, and clamping it is a
      // better answer than calling a number the client plainly meant a syntax error.
      val requested = raw.toLongOrNull() ?: return null
      if (requested <= 0) return null
      return requested.coerceAtMost(MAX_DUEL_LIMIT.toLong()).toInt()
  }
  ```

- `null` means **refuse** — the caller answers `400`. A `null` return and a `null` argument mean
  opposite things here, so say it in the KDoc: an absent parameter is the default, an unparseable
  one is a rejection.
- The cap exists because an unbounded `limit` is a denial-of-service parameter, and clamping rather
  than refusing keeps a client that asks for too much working. KDoc says so.
- No Ktor types in this file: it takes a `String?` and returns an `Int?`, which is what makes it
  testable without a server. No database, no coroutines.

## Out of scope

- Reading the parameter off a `call`, and turning `null` into a `400` — `TASK-021110`.
- Paging, cursors or filters. v0.1 has no history screen; `EPIC-04` owns the one it gets.

## Tests

`RecentDuelsLimitTest`, JUnit 5, package `duels.poker.server.http`. Plain tests, no Ktor and no
database. Assert against `DEFAULT_DUEL_LIMIT` and `MAX_DUEL_LIMIT`, not against `10` and `50`
spelled again.

| Test | Proves |
| --- | --- |
| `anAbsentLimitIsTheDefault` | `duelLimitOrNull(null) == DEFAULT_DUEL_LIMIT` |
| `aLimitWithinTheCapIsHonoured` | `duelLimitOrNull("25") == 25` and `duelLimitOrNull("1") == 1` |
| `aLimitAboveTheCapIsClamped` | `duelLimitOrNull("51")` and `duelLimitOrNull("999")` are both `MAX_DUEL_LIMIT`, and `duelLimitOrNull("50") == MAX_DUEL_LIMIT` |
| `aLimitLargerThanAnIntIsClampedNotRejected` | `duelLimitOrNull("99999999999") == MAX_DUEL_LIMIT` |
| `aNonNumericLimitIsRejected` | `duelLimitOrNull("abc")`, `duelLimitOrNull("")` and `duelLimitOrNull(" ")` are each `null` |
| `aZeroOrNegativeLimitIsRejected` | `duelLimitOrNull("0")` and `duelLimitOrNull("-1")` are each `null` |

## Acceptance criteria

- [ ] `RecentDuelsLimitTest.anAbsentLimitIsTheDefault` passes
- [ ] `RecentDuelsLimitTest.aLimitWithinTheCapIsHonoured` passes
- [ ] `RecentDuelsLimitTest.aLimitAboveTheCapIsClamped` passes
- [ ] `RecentDuelsLimitTest.aLimitLargerThanAnIntIsClampedNotRejected` passes
- [ ] `RecentDuelsLimitTest.aNonNumericLimitIsRejected` passes
- [ ] `RecentDuelsLimitTest.aZeroOrNegativeLimitIsRejected` passes
- [ ] `RecentDuelsLimit.kt` names no `io.ktor` type, no `DataSource` and no SQL string
- [ ] `RecentDuelsLimitTest.kt` contains no bare `10` or `50` literal in an assertion — both come
      from the constants
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
