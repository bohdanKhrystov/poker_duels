---
schema: 2
id: TASK-040914
title: Over HTTP, a cursor is refused under every filter but the one that issued it
type: task
status: done
parent: STORY-0409
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, http, history, paging, filters, cursor]
depends_on: [TASK-040913]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`GET /api/me/duels` pages on when a cursor comes back under the filter that issued it, and answers
`400 Bad Request` without reading anything when it comes back under any other filter — including no
filter at all.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify — four tests and one private fixture helper |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | read — `respondWithDuels` and `recentDuelsPage` |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | read — `duelCursorOrNull` |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelFilter.kt` | read — `DuelFilter` and `DuelFilter.NONE` |

**No production file changes.** `TASK-040913` already shipped the behaviour these tests pin; this
ticket is the endpoint-level proof of it, in the same relation `TASK-040910` stood in to
`TASK-040909`. If a test here fails, the fix belongs in the test unless it is a genuine defect in
`respondWithDuels` — and then it is a one-line fix in `ProfileRoutes.kt`, which becomes a second
modified file and should be called out in the PR rather than smuggled in.

## Scope

- **Every test uses `FakeProfileReads`**, the double declared at the bottom of `ProfileRouteTest.kt`.
  It records `cursorsRequested` and `filtersRequested` on every call. **Never use
  `FixedProfileReads` from `AuthRouteDoubles.kt`**: it observes neither the cursor nor the filter
  and returns an empty list for everyone, so every assertion below would pass against a route that
  ignored both.
- **The fake chooses its rows by cursor and ignores the filter**, deliberately: whether a filtered
  page holds the filtered rows was settled against the real database by `TASK-040910`. What is under
  test here is only which filter the route *mints* a cursor under and which filter it *decodes* one
  under, and the fake is blind to exactly the right thing for that.
- Add one private helper beside the existing ones so the four tests do not each restate a fixture:

  ```kotlin
  // Two rows and `limit=1`, so the first page always carries a nextCursor to hand back.
  private fun twoDuels(): List<DuelSummaryResponse>
  ```

  returning a `WON` duel at `2026-08-15T13:00:00Z` with id `aaaaaaaa-…` and a `LOST` duel at
  `2026-08-15T12:00:00Z` with id `bbbbbbbb-…`, built with `duelSummaryResponse` the way the tests
  above it do.
- **Each test mints its cursor by making a real request** rather than by calling
  `DuelCursor(...).encoded(...)` directly. A hand-built cursor would prove only that
  `duelCursorOrNull` refuses it — which `DuelCursorTest` already proves — and would not prove that
  the route mints under the filter of the request being served, which is the half that makes the
  next request work.
- **"Nothing is read" is asserted by counting**, not by emptiness: the minting request legitimately
  reads once, so the refused request must leave `reads.cursorsRequested.size` at exactly `1`.

## Out of scope

- Any change to `ProfileRoutes.kt`, `DuelCursor.kt` or `DuelFilter.kt`.
- Any test of *which rows* a filtered page holds — `TASK-040906` and `TASK-040910` own that,
  against the database.
- A test that the same filter written in a different query-parameter order pages the same walk.
  It has no falsifier in this design and would be a vacuous test: `respondWithDuels` reads
  `outcome` and `opponent` by name and hands `duelFilterOrNull`'s **parsed** `DuelFilter` to
  `encoded`, so the query string's order is invisible to every line that could get it wrong. The
  property is real (`ADR-0057` §2) and is structural, not observable.
- `docs/protocol.md` — `TASK-040915`.

## Tests

`ProfileRouteTest` — four tests appended near the existing cursor tests.

| Test | Proves |
| --- | --- |
| `aCursorFollowedUnderTheSameFilterPagesOn` | `?limit=1&outcome=WON` answers `200` with a non-null `nextCursor`; handing that cursor back as `?limit=1&outcome=WON&after=…` answers `200`, the page holds the **second** duel, and `reads.cursorsRequested.last()` is the `DuelCursor` of `twoDuels()`'s **first** row, built as `DuelCursor(Instant.parse(row.finishedAt), UUID.fromString(row.duelId))` — asserting the decoded value, not merely that a cursor arrived. Fails against a route that mints under `DuelFilter.NONE` regardless of the request (the replay would be `400`), and against one that refuses every cursor |
| `aCursorFromOneFilterIsRefusedUnderAnother` | a cursor minted under `?limit=1&outcome=WON`, handed back as `?limit=1&outcome=LOST&after=…`, answers `400` and `reads.cursorsRequested.size` is still `1`. Fails against a route that calls `duelCursorOrNull` without the filter — the silent reinterpretation this whole re-plan exists to close |
| `anUnfilteredCursorIsRefusedUnderAFilter` | a cursor minted by `?limit=1` with no filter parameter, handed back as `?limit=1&outcome=WON&after=…`, answers `400` and nothing is read. Fails against an implementation that treats `DuelFilter.NONE` as *no fingerprint* and so lets an unfiltered cursor open any filtered walk |
| `aFilteredCursorIsRefusedWithNoFilter` | a cursor minted under `?limit=1&opponent=Halvard`, handed back as `?limit=1&after=…` with no filter, answers `400` and nothing is read. The same bug as above, pointed the other way: a route that only fingerprints when a filter is present passes the previous test's minting half and fails here |

All four assert `HttpStatusCode` values, a decoded `RecentDuelsResponse`, and list sizes on the
double — nothing that requires reading prose to judge.

## Acceptance criteria

- [ ] `ProfileRouteTest.aCursorFollowedUnderTheSameFilterPagesOn` passes
- [ ] `ProfileRouteTest.aCursorFromOneFilterIsRefusedUnderAnother` passes
- [ ] `ProfileRouteTest.anUnfilteredCursorIsRefusedUnderAFilter` passes
- [ ] `ProfileRouteTest.aFilteredCursorIsRefusedWithNoFilter` passes
- [ ] Each of the three refusal tests asserts `HttpStatusCode.BadRequest` **and** that
      `reads.cursorsRequested` has exactly one entry afterwards
- [ ] Every test above constructs `FakeProfileReads`; the string `FixedProfileReads` appears
      nowhere in `ProfileRouteTest.kt`
- [ ] Every test already in `ProfileRouteTest` still passes unchanged — this ticket appends tests
      and a helper and edits no existing one, and it changes no production code, so nothing already
      in the file observes a different value
- [ ] `git diff --stat` on the merge shows one changed file
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
