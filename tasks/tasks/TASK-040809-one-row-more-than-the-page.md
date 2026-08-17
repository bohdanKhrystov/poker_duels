---
schema: 2
id: TASK-040809
title: One row more than the page, and the last page says there is no next
type: task
status: ready
parent: STORY-0408
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, http, history, paging]
depends_on: [TASK-040808]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The endpoint knows whether there is another page without a second query, and says so in
`nextCursor` — which is the cursor of the row it returned last, or `null`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | read |

## Scope

- The route asks the port for `limit + 1` rows. If more than `limit` come back there is another
  page: respond with the **first `limit`** of them and `nextCursor` = `DuelCursor` of the row at
  index `limit - 1`, encoded. Otherwise respond with all of them and `nextCursor = null`.
- **The extra row is a probe and is never serialised.** `duels.size` never exceeds `limit`, and the
  cursor never names the probe row — a cursor taken from the extra row skips a duel on the next
  request, silently, forever.
- The cursor is built from the returned row's own text:
  `DuelCursor(Instant.parse(row.finishedAt), UUID.fromString(row.duelId))`. Both strings were
  produced by this server from database values (`Instant.toString()`, `UUID.toString()`), so both
  parse and this is not a place for a `try`.
- The cap still governs what is **returned**: `duelLimitOrNull` clamps to `MAX_DUEL_LIMIT` and the
  probe asks for one more than the clamped value. `limit + 1` is a read of 51 rows at the ceiling,
  and paging is the reason the ceiling is now usable rather than a reason to raise it.
- Keep the assembly in a private function so `profileRoutes` stays well inside detekt's `LongMethod`
  threshold — `TASK-040808` already moved the handler out; put the trim-and-encode step beside it as
  its own small function rather than folding it back into the route lambda.

## This ticket owns two existing assertions

Both are in `ProfileRouteTest`, both move by exactly one, and both stay exact equalities:

- `anAbsentLimitAsksForTheDefault`: `assertEquals(DEFAULT_DUEL_LIMIT, reads.lastLimitRequested)`
  becomes `DEFAULT_DUEL_LIMIT + 1`.
- `aLimitAboveTheCapIsClamped`: `assertEquals(MAX_DUEL_LIMIT, reads.lastLimitRequested)` becomes
  `MAX_DUEL_LIMIT + 1`.

That is the probe, and it is the whole mechanism by which the last page can say it is the last one
without a second round trip. `aNonNumericLimitIsABadRequest` asserts the port was never called and
does **not** change. Nothing else in the file changes and no assertion is weakened.

## Out of scope

- Reading `after` or refusing a malformed cursor — `TASK-040808`, already merged.
- Any database test. The port is faked here; `TASK-040810` proves the same behaviour against
  PostgreSQL over real HTTP.
- Making the probe configurable, or reading `limit + 1` in the SQL layer instead. The port's
  contract is "give me n rows after this cursor"; how many the route asks for is the route's business.

## Tests

`ProfileRouteTest`, against `FakeProfileReads`. Give the fake a fixed list per player as it already
takes, and drive the page size with `?limit=3` so the boundary is small enough to read.

| Test | Proves |
| --- | --- |
| `aFullPageReportsTheCursorOfItsLastRow` | the fake holds **four** rows and the request asks `?limit=3`: the body carries exactly three duels, and `nextCursor` decodes (via `duelCursorOrNull`) to the cursor of the **third** row — not the fourth. This is the off-by-one that silently loses a duel, so assert the decoded value, not merely that the field is non-null |
| `anExactlyFullRecordReportsNoNextPage` | the fake holds **three** rows and the request asks `?limit=3`: three duels and `"nextCursor":null`. The probe found nothing, and this is the case a naive *returned == limit ⇒ there is more* gets wrong — it would issue a cursor whose page is empty and tell the client there is more when there is not |
| `aShortPageReportsNoNextPage` | the fake holds two rows, `?limit=3`: two duels and `"nextCursor":null` |
| `anEmptyRecordReportsNoNextPage` | the fake holds none: the body is exactly `{"duels":[],"nextCursor":null}` and the status is `200` |

Three, four and two rows against a limit of three are chosen so the boundary is observable in both
directions: one fixture over the limit, one exactly on it, one under it. A single fixture cannot
tell the probe from a coincidence.

## Acceptance criteria

- [ ] All four tests above pass
- [ ] `aFullPageReportsTheCursorOfItsLastRow` decodes `nextCursor` and asserts it equals the cursor
      of the third row
- [ ] `anAbsentLimitAsksForTheDefault` asserts `DEFAULT_DUEL_LIMIT + 1` and
      `aLimitAboveTheCapIsClamped` asserts `MAX_DUEL_LIMIT + 1`; `aNonNumericLimitIsABadRequest` is
      unchanged
- [ ] Every other test in `ProfileRouteTest` passes with its assertions unchanged
- [ ] No response in any test carries more than `limit` duels
- [ ] `./gradlew :poker-server:detekt` reports no issue
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
