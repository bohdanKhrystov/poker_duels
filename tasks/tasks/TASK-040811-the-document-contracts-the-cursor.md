---
schema: 2
id: TASK-040811
title: The document contracts the cursor and the paging rule, and a test agrees with the DTO
type: task
status: done
parent: STORY-0408
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, docs, protocol, history, paging]
depends_on: [TASK-040810]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`docs/protocol.md` states the cursor's contract and the paging rule, and the reflection test proves
the document's field names against `RecentDuelsResponse` rather than trusting them.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify — the `### Recent duels endpoint` section |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | read — `RecentDuelsResponse` |

## Scope — the document

Everything new goes inside `### Recent duels endpoint` and **before the line
`Each duel summary in the array contains:`**. See the trap below.

- The `after` query parameter, as a bullet beside `limit`: optional; **opaque**; the exact string a
  previous response returned in `nextCursor`, echoed back unchanged; absent means the newest page;
  a value that does not decode is `400 Bad Request` and nothing is read. A client never constructs
  one — `ADR-0002`, the server is authoritative.
- The response table gains a row: `nextCursor` — string or null; the cursor to send as `after` for
  the next page, and `null` on the last page. Always present.
- The paging rule, in prose: pages are **total and disjoint** — every duel appears exactly once,
  with no gap and no duplicate, even when a duel finishes between two requests. The order is
  `finishedAt` then `duelId`, both descending. `limit` keeps its default of `10` and its cap of
  `50`, and the cap governs how many duels a page returns.

## The trap this ticket exists to avoid

`HttpEndpointDocumentationTest.duelSummarySection` is the span from
`Each duel summary in the array contains:` to `## Protocol Errors`, and
`theDocumentedFieldNamesAllExist` reflects **every** `| field |` row in that span against
`DuelSummaryResponse`. A `| nextCursor |` row placed after that marker fails a test that has
nothing to do with this change, and the failure names the wrong thing. Put the new table row with
`duels`, above the marker.

## Scope — the test

- A new section value beside the existing ones:
  `sectionBetween("### Recent duels endpoint", "Each duel summary in the array contains:")`.
- Assert **properties ⇒ documented**, never the reverse for this section: it contains the query
  parameter bullets as well as the response table, and a documented-⇒-exists check would demand
  that `after` be a property of `RecentDuelsResponse`. Keep `after` a bullet, not a table row, for
  the same reason.

## Out of scope

- Any change to `DuelSummaryResponse`, its section, or the existing tests over it.
- The client's half of the contract — `STORY-0413`.
- `PROTOCOL_VERSION`, `protocol.gen.ts` and `docs/protocol-versions.md`: `RecentDuelsResponse` is
  reachable from neither message root, so none of them moves (`TASK-040806` recorded this).

## Tests

`HttpEndpointDocumentationTest`

| Test | Proves |
| --- | --- |
| `theRecentDuelsSectionNamesEveryFieldTheResponseHas` | `RecentDuelsResponse::class.memberProperties` is non-empty, and every property name appears in the recent-duels section's field table — so `nextCursor` cannot be added to the DTO and left undocumented |
| `theRecentDuelsSectionDocumentsTheCursor` | that section contains `after`, `opaque` and `400` — the parameter, its contract and its refusal |
| `theDocumentMarksTheNextCursorNullable` | `RecentDuelsResponse.nextCursor` is `isMarkedNullable`, and the row `rowFor(recentDuelsSection, "nextCursor")` exists and mentions `null` — the same pairing this file already makes for `displayName` and `opponentDisplayName` |

## Acceptance criteria

- [ ] All three tests above pass
- [ ] `theRecentDuelsSectionNamesEveryFieldTheResponseHas` asserts the reflected property set is
      non-empty before iterating it
- [ ] The document's `nextCursor` row sits above the line `Each duel summary in the array contains:`
- [ ] `after` appears in the document as a bullet, not as the first cell of a table row
- [ ] Every test already in `HttpEndpointDocumentationTest` passes with its assertions unchanged —
      in particular `theDocumentedFieldNamesAllExist` and `theDocumentDoesNotCallANonNullFieldNullable`
- [ ] `docs/protocol.md` states the default `10` and the cap `50` in the form
      `defaults to \`10\`` and `capped at \`50\`` that `theDocumentStatesTheLimitDefaultAndCap`
      already matches
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
