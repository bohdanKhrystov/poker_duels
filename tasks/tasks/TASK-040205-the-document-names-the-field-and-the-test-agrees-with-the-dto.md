---
schema: 2
id: TASK-040205
title: The document names the field, and the test agrees with the DTO
type: task
status: backlog
parent: STORY-0402
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, docs, http, protocol]
depends_on: [TASK-040204]
verify:
  - grep -q '| opponentDisplayName |' docs/protocol.md
  - ./gradlew :poker-server:test --tests '*HttpEndpointDocumentationTest.theDocumentMarksTheOpponentDisplayNameNullable'
  - ./gradlew :poker-server:test --tests '*HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`docs/protocol.md`'s duel-summary table documents `opponentDisplayName`, and a test proves the
document's nullability claim against the DTO rather than against a reader's goodwill.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify — the duel-summary table only, lines 124–133 |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | read — the KDoc this row must agree with |

## Scope

- One row added to the table under **"Each duel summary in the array contains:"**, and nowhere else.
  `HttpEndpointDocumentationTest` scopes its checks to the text between that marker and
  `## Protocol Errors`; a row outside those bounds is invisible to every assertion here.
- The first cell must be exactly `opponentDisplayName` — `theDocumentedFieldNamesAllExist` looks up
  every documented first cell as a property of `DuelSummaryResponse` and fails on a typo. The type
  cell reads `string or null`, matching the `displayName` row in the profile table. The semantics
  cell says the name is the opponent's current one, read at request time so a name set later labels
  an older duel (`ADR-0021`), that `null` means they never set one, and that the server fabricates
  no placeholder (`ADR-0029` §6).
- **Replace the stale clause in the `opponentPlayerId` row.** It currently points at `DEC-016` "for
  the question of displaying opponent names"; that question is answered by `ADR-0021` and the answer
  is the row directly below it. The replacement states the split: the id is the stable identity a
  client correlates on, `opponentDisplayName` is the label, and both travel.
- **The `opponentPlayerId` row must not contain the substring `null` in any case.**
  `theDocumentDoesNotCallANonNullFieldNullable` reads every row belonging to a non-null property and
  fails if the text mentions nullability — so a rewrite that says "never null" turns a green suite
  red. The same holds for the `duelId`, `outcome`, `coinDelta`, `handsPlayed` and `finishedAt` rows,
  which this ticket does not touch.
- One test added, modelled line for line on the existing `theDocumentMarksTheDisplayNameNullable`:
  reflect `DuelSummaryResponse::class.memberProperties` for `opponentDisplayName`, assert its return
  type `isMarkedNullable`, then assert the row for it in `duelSummarySection` mentions `null`. Both
  halves are required — the reflection half is what stops the document from being the only source of
  truth.
- Style: block body, **no explicit `: Unit`** (ktlint's `no-unit-return` fails the build), final
  expression an assertion.

## Out of scope

- Every other section of `docs/protocol.md`, including the profile and set-name sections
  `TASK-040118` already wrote.
- Any production code. If this ticket needs a change under `src/main/`, the previous four tickets are
  wrong and that is a finding, not a fix to make here.
- What a client renders for `null`. That is a product choice `ADR-0021` deliberately left to the
  client, and `STORY-0411` owns it.

## Tests

`HttpEndpointDocumentationTest`

| Test | Proves |
| --- | --- |
| `theDocumentMarksTheOpponentDisplayNameNullable` | `DuelSummaryResponse.opponentDisplayName` is a nullable type **and** its documented row mentions `null` — the document and the DTO cannot drift apart in either direction |

Two existing tests do the rest of the work and must stay green untouched:
`theDocumentedFieldNamesAllExist`, which refuses a documented field that does not exist on the DTO,
and `theDocumentDoesNotCallANonNullFieldNullable`, which refuses a non-null field described as
nullable.

## Acceptance criteria

- [ ] `HttpEndpointDocumentationTest.theDocumentMarksTheOpponentDisplayNameNullable` passes, and
      asserts both the reflected nullability and the row's text
- [ ] `HttpEndpointDocumentationTest.theDocumentedFieldNamesAllExist` passes with the new row present
- [ ] `HttpEndpointDocumentationTest.theDocumentDoesNotCallANonNullFieldNullable` passes — the
      rewritten `opponentPlayerId` row contains no `null` in any case
- [ ] The new row sits between `Each duel summary in the array contains:` and `## Protocol Errors`
- [ ] `docs/protocol.md` no longer describes `DEC-016` as an open question
- [ ] Nothing under `poker-server/src/main/` changes in this diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
