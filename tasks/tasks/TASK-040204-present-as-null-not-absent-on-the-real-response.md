---
schema: 2
id: TASK-040204
title: Present as null, not absent, on the response the route actually writes
type: task
status: backlog
parent: STORY-0402
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, http, wire, tests, identity]
depends_on: [TASK-040203]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest.aDuelSummaryEncodesTheOpponentNameItWasGiven'
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest.aDuelSummaryWithNoOpponentNameEncodesTheFieldAsNull'
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest.aDuelLineOnTheWireCarriesTheOpponentsName'
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest.aDuelLineForAnUnnamedOpponentCarriesTheFieldAsNull'
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest'
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Two claims, each proven with two distinct inputs: a duel line carries the opponent's name verbatim,
and a line with no name carries `"opponentDisplayName":null` in the bytes the route writes — present,
not omitted.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | read — `duelSummaryResponse`, whose `opponentDisplayName` default is exactly what these four tests must not lean on |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | read — `RecentDuelsResponse`'s KDoc, which spells out the two different `Json` configurations at issue |

## Scope

- Four tests added, two per file. **Nothing existing moves** — no assertion, no test name, no JSON
  literal in either file changes.
- **Every one passes `opponentDisplayName` explicitly, including the `null` case.** A value asserted
  only at the builder's default cannot be told apart from a constant, and `null` is the default.
- **Both files are needed, and the reason is that they use different `Json`s.** `ProfileDtosTest`
  encodes with `protocolJson`, which sets `encodeDefaults = true` — it would happily encode a
  defaulted property and so **cannot** catch someone giving `DuelSummaryResponse.opponentDisplayName`
  a default. `ProfileRouteTest` goes through `module()`, whose `ContentNegotiation { json() }` has
  `encodeDefaults = false`; there, a defaulted property vanishes from the body. The route test is the
  only assertion in the repository that fails if the field acquires a default, and that is why it
  exists.
- Assertions are on the **encoded text**, as the neighbouring tests in both files already do, so
  *present as `null`* is distinguishable from *absent*.
- `ProfileRouteTest`'s existing `FakeProfileReads` is used as it is: hand it summaries built by
  `duelSummaryResponse(...)`. No fake changes shape.
- Style: block bodies, **no explicit `: Unit`** (ktlint's `no-unit-return` fails the build), and each
  test's final expression is an assertion — a test body that produces a value is silently never run.

## Out of scope

- Anything against the database — `TASK-040202` and `TASK-040203` own the reader's side.
- `docs/protocol.md` — `TASK-040205`.
- The client. `TASK-031103`'s parse names five keys and ignores the rest; `STORY-0411` decides what a
  client does with this one.

## Tests

`ProfileDtosTest`

| Test | Proves |
| --- | --- |
| `aDuelSummaryEncodesTheOpponentNameItWasGiven` | a summary built with an explicit `opponentDisplayName = "Ingrid"` encodes with `"opponentDisplayName":"Ingrid"` in the text, and round-trips back equal to itself |
| `aDuelSummaryWithNoOpponentNameEncodesTheFieldAsNull` | a summary built with an explicit `opponentDisplayName = null` encodes with `"opponentDisplayName":null` **present** in the text |

`ProfileRouteTest`

| Test | Proves |
| --- | --- |
| `aDuelLineOnTheWireCarriesTheOpponentsName` | `GET /api/me/duels`, answered by a fake holding one summary with `opponentDisplayName = "Torvald"`, returns a body containing `"opponentDisplayName":"Torvald"` |
| `aDuelLineForAnUnnamedOpponentCarriesTheFieldAsNull` | the same request, answered by a fake holding one summary with an explicit `null`, returns a body containing `"opponentDisplayName":null` — the field is written by the real response pipeline rather than dropped by it |

## Acceptance criteria

- [ ] `ProfileDtosTest.aDuelSummaryEncodesTheOpponentNameItWasGiven` passes
- [ ] `ProfileDtosTest.aDuelSummaryWithNoOpponentNameEncodesTheFieldAsNull` passes and asserts on the
      JSON **text**, failing if the property is omitted rather than encoded as `null`
- [ ] `ProfileRouteTest.aDuelLineOnTheWireCarriesTheOpponentsName` passes
- [ ] `ProfileRouteTest.aDuelLineForAnUnnamedOpponentCarriesTheFieldAsNull` passes and asserts on
      `response.bodyAsText()`, not on a decoded object
- [ ] All four pass `opponentDisplayName` explicitly rather than accepting the builder's default
- [ ] The two names used are distinct from each other and neither is the empty string
- [ ] Every test already in `ProfileDtosTest` and `ProfileRouteTest` passes with its assertions
      unchanged
- [ ] No test method in the diff declares `: Unit`, and every one ends in an assertion
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
