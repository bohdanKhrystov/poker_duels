---
schema: 2
id: TASK-050218
title: The document contracts the ladder — every parameter, the promise, and both refusals
type: task
status: ready
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, docs, leaderboard]
depends_on: [TASK-050217]
verify:
  - ./gradlew :poker-server:test --tests '*HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:ktlintCheck
  - "grep -qF '### Standings endpoint' docs/protocol.md"
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- docs/protocol-versions.md poker-server/src/main)"
---

## Goal

`docs/protocol.md` contracts `GET /api/standings` — its parameters, its refusals, which season it
serves, the three answers about the reader, and the walk's promise **with both of its refusals** —
and a test agrees with the DTOs rather than trusting the prose.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/StandingsDtos.kt` | read |

## The test bound this ticket moves, and why it is in the budget

`HttpEndpointDocumentationTest` bounds each documented section by the heading that **follows** it,
and today `duelSummarySection` runs from `"Each duel summary in the array contains:"` to
`"## Protocol Errors"`. A new `### Standings endpoint` section between those two would be swallowed
by it, and `theDocumentedFieldNamesAllExist` would then read standings fields as duel-summary
fields and fail. So exactly one string in that file changes:

```kotlin
sectionBetween("Each duel summary in the array contains:", "### Standings endpoint")
```

**That end marker is the only edit to an existing line of the test file.** No assertion moves, none
is added to an existing test, and none is weakened; every one of the file's current tests must pass
untouched afterwards.

## Scope

- A new `### Standings endpoint` section in `docs/protocol.md`, placed after the duel-summary table
  and **before** `## Protocol Errors`, carrying:
  - **Method and path** — `GET /api/standings`.
  - **Authentication** — none required. The `X-Device-Id` header is optional and changes **only**
    the self standing; the page is identical for every reader, and there is no `401` on this
    endpoint. This is the sentence a reader will most expect to be wrong, so it is explicit.
  - **Query parameters** — `limit` (optional, defaults to `10`, capped at `50`, `400` on
    non-numeric, zero or negative) and `after` (optional, opaque, echoed back unchanged, `400` and
    nothing read when it does not decode **or** when its instant lies outside the season the server
    is in, indistinguishably, remedy identical: drop it and ask for the first page). It says there
    is **no** season parameter and no player parameter, and that a past season is not offered.
  - **The response fields** — `season`, `rows`, `nextCursor`, `self`, and the row's `rank`,
    `playerId`, `displayName`, `coins`, each in the `| Field | Type | Semantics |` table shape the
    other sections use, with `displayName` documented as *string or null* and `null` meaning never
    set.
  - **The three answers about the reader** (`ADR-0065` §4) — a rank and a standing; *no place this
    season*, which prints no rank and is **not** `0`; and no `self` at all for a request with no
    known device.
  - **The walk's promise and both refusals** (`ADR-0066` §4), as three statements and not one:
    every player of the ladder **as it stood committed at the walk's cutoff** returned exactly once,
    with the rank held then; *a walk is not live* — a duel finishing after the cutoff is in no page
    of it and page forty is as old as page one; and the **named exception** — a duel committed after
    a page was drawn but stamped before the cutoff can have its winner never returned and its loser
    returned twice.
  - **A repeated rank is not a duplicate row** (`ADR-0064` §2), and a walk crossing a month boundary
    is refused and restarted (`ADR-0066` §7).
- The words *total and disjoint* do **not** appear in this section. That is
  `GET /api/me/duels`' sentence, it is not inherited here (`ADR-0066` §2), and copying it would make
  the document claim something the endpoint does not do.

## Out of scope

- **Editing `GET /api/me/duels`' paragraph**, including its *total and disjoint* sentence, which
  stays exactly as it is and stays true of that endpoint.
- **`docs/protocol-versions.md` and `PROTOCOL_VERSION`.** These endpoints are plain HTTP, are not
  `ServerMessage`s, and this story takes no version step — so no row is appended and `ADR-0047`'s
  lock is never contended for.
- **`docs/architecture.md`, `docs/duel-rules.md`, the ADRs.** None changes here.
- Any production code.

## Tests

`HttpEndpointDocumentationTest`, in `duels.poker.server.http`. Add a
`standingsSection = sectionBetween("### Standings endpoint", "## Protocol Errors")` beside the
existing section fields and four tests:

| Test | Proves |
| --- | --- |
| `theDocumentContractsTheStandingsEndpoint` | the section names `GET /api/standings`, `limit`, `after`, `400`, and states that no authentication is required and that only the current season is served |
| `theDocumentedStandingsFieldNamesAllExist` | every field name in the section's tables is a property of `StandingsResponse`, `StandingRow` or `SelfStandingResponse`, by reflection — the same shape `theDocumentedFieldNamesAllExist` already uses |
| `theDocumentStatesThePromiseAndBothRefusals` | the section contains the cutoff promise, the *not live* refusal and the named exception, and does **not** contain `total and disjoint` |
| `theDocumentStatesTheThreeAnswersAboutTheReader` | the section names the rank-and-standing answer, the *no place this season* answer including that it is not `0`, and the absent-`self` answer |

**Named mutations.** Deleting the exception paragraph reddens the third test — which is the point of
writing it down: if a later design removes the anomaly, `TASK-050215` goes red and this sentence has
to change with it. Renaming a DTO property without editing the table reddens the second.

## Acceptance criteria

- [ ] `docs/protocol.md` carries a `### Standings endpoint` section between the duel-summary table
      and `## Protocol Errors`
- [ ] `HttpEndpointDocumentationTest.theDocumentContractsTheStandingsEndpoint` passes
- [ ] `HttpEndpointDocumentationTest.theDocumentedStandingsFieldNamesAllExist` passes
- [ ] `HttpEndpointDocumentationTest.theDocumentStatesThePromiseAndBothRefusals` passes, including
      the assertion that `total and disjoint` is absent from the standings section
- [ ] `HttpEndpointDocumentationTest.theDocumentStatesTheThreeAnswersAboutTheReader` passes
- [ ] The only edit to an existing line of `HttpEndpointDocumentationTest.kt` is
      `duelSummarySection`'s end marker, and all twenty tests already in that file pass with their
      assertions unchanged
- [ ] `docs/protocol-versions.md` is unchanged and `PROTOCOL_VERSION` does not move
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
