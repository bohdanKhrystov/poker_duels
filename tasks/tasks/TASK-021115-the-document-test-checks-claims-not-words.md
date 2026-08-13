---
schema: 2
id: TASK-021115
title: The protocol document says handsPlayed is null, and its test cannot tell
type: task
status: done
parent: STORY-0211
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, documentation, test-strength]
depends_on: [TASK-021114]
verify:
  - ./gradlew :poker-server:test --tests '*HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - ./gradlew :poker-server:check
---

## Goal

Two problems, one small and one that matters.

**The small one.** `docs/protocol.md` still says `handsPlayed` is "currently always `null`". Since
`TASK-021114` it carries a real hand count. The document is wrong.

**The one that matters.** `HttpEndpointDocumentationTest` did not notice. It asserts that certain
substrings appear in the document — `"GET /api/me"`, the header name, `"401"`, the limit numbers —
and never that the document's *claims* match the code. So a statement can become false and the
suite stays green.

That is the failure mode a documentation test exists to prevent. A document nobody checks is merely
out of date; a document with a passing test beside it is trusted, and trusted wrong documentation
sends a client author down a path the server will not honour.

Found during the `TASK-021114` review.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify |

## Scope

- Correct the `handsPlayed` line: it is a non-null count of the hands the duel lasted.
- **Strengthen the test so a false claim fails it.** Assert against values the code actually
  exposes rather than against literals — the existing assertions already reference
  `DEFAULT_DUEL_LIMIT` and `MAX_DUEL_LIMIT`, which is the right idea; extend it.

  For a nullability claim there is a mechanical check available: the document should not describe a
  field as nullable when its DTO property is non-null. Reflect over `DuelSummaryResponse` and
  `ProfileResponse` and assert that no field the document calls "always `null`" or "nullable" is a
  non-null property. That turns a prose claim into something the compiler's own type information
  can refute.
- **Do not weaken `ProtocolDocumentationTest`**, and do not add a table row matching
  ``^\| `Name` \|`` — that pattern is read as a claim that a protocol *message* of that name exists,
  and `TASK-021112` had to shape its tables around it.

## Tests

| Name | Asserts |
| --- | --- |
| `theDocumentDoesNotCallANonNullFieldNullable` | reflecting over the response DTOs, no property the document describes as null or nullable is actually non-null — this is the assertion that would have caught the stale line |
| `theDocumentedFieldNamesAllExist` | every field name the document lists for a response actually exists on that DTO, so a renamed field cannot leave the document describing something gone |

Prove the first is real: temporarily reinstate the "always `null`" wording for `handsPlayed`,
confirm the test **fails**, restore it, confirm green. Report the observed output — without that,
the new test is only asserted to be stronger.

## Done

All three `verify:` commands exit 0, the document describes `handsPlayed` correctly, and
reinstating the false claim fails the suite.
