---
schema: 2
id: TASK-040915
title: The document states the refusal instead of promising it
type: task
status: ready
parent: STORY-0409
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, docs, protocol, history, paging, filters]
depends_on: [TASK-040914]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.HttpEndpointDocumentationTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`docs/protocol.md` says a cursor replayed under a different filter is refused, in the present tense,
and no longer says the refusal is decided but not yet built.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify — the `after` bullet in the Recent duels endpoint section, and nothing else |
| `poker-server/src/test/kotlin/duels/poker/server/http/HttpEndpointDocumentationTest.kt` | modify — one test added |

## Scope

- In the `after` bullet, **replace** everything from `This cursor names a position` to the end of
  the bullet — the two sentences `TASK-040911` deliberately wrote as a placeholder, ending
  `and is decided but not yet built.` The first four sentences of the bullet are still true and do
  not change.
- The replacement says, in the present tense:
  - the cursor names a position in the `finishedAt`/`duelId` order **and in the filtered set that
    produced it**, because it carries a fingerprint of that filter;
  - a cursor handed back under a **different filter** is `400 Bad Request` and nothing is read —
    and that this covers an unfiltered cursor sent with a filter, and a filtered cursor sent with
    none;
  - that this refusal is **indistinguishable** from a cursor that does not decode: same status,
    same empty body, and the same remedy, which is to drop the cursor and ask for the newest page
    of the current filter;
  - that a client changing a filter therefore starts a new page walk rather than reusing
    `nextCursor`;
  - that `limit` is **not** part of the fingerprint and may change mid-walk (`ADR-0057` §6) — the
    one thing a client can vary without invalidating what it holds;
  - cite `ADR-0057`.
- **Do not introduce a second status code.** `ADR-0057` §5 rejected `409 Conflict` by name: the
  client's remedy is identical, a retryable-looking status invites a retry that can never succeed,
  and this endpoint's entire refusal vocabulary is one status and no body. Widening `400` later is
  additive; taking a `409` back is not.
- **Do not describe the payload's shape.** The cursor is opaque; that it contains a fingerprint is
  worth stating because it explains the refusal, but the three-part layout, the digest and the
  encoding are internal to the server (`ADR-0057` §3) and a client that parses them has left the
  contract.

## Out of scope

- Every other section of `docs/protocol.md`, including the `outcome` and `opponent` bullets
  `TASK-040911` wrote, the response table, and the Paging paragraph.
- Any production code. If a claim here turns out not to match `respondWithDuels`, the document is
  wrong or the code is — say which in the PR; do not change code under a docs ticket.
- Any change to the existing `theRecentDuelsSectionDocumentsTheCursor`, whose three assertions
  (`after`, `opaque`, `400`) all stay true and all keep passing.

## Tests

`HttpEndpointDocumentationTest` — one test appended. `assertFalse` comes from
`org.junit.jupiter.api.Assertions.assertFalse`, imported alphabetically before the `assertTrue`
this file already imports from the same class.

| Test | Proves |
| --- | --- |
| `theRecentDuelsSectionSaysACursorIsRefusedUnderAnotherFilter` | on `recentDuelsSection`: it contains `"different filter"` and `"ADR-0057"`, and it contains **neither** `"not yet"` nor `"409"` |

The two negative assertions are the ones that carry the ticket, and each names a specific wrong
edit. Without `not yet`, the obvious way to do this ticket — append a sentence describing the
refusal and leave the placeholder standing — passes while the document contradicts itself in
consecutive sentences. Without `409`, a well-meaning edit inventing a more informative status for a
mismatch passes while documenting behaviour the server does not have. Both phrases occur elsewhere
in `docs/protocol.md` and in neither case inside this section, so both assertions are about this
bullet and nothing else: `not yet` appears once more in the socket section, and `409` in the sign-up
and set-name tables.

## Acceptance criteria

- [ ] `HttpEndpointDocumentationTest.theRecentDuelsSectionSaysACursorIsRefusedUnderAnotherFilter`
      passes
- [ ] `HttpEndpointDocumentationTest.theRecentDuelsSectionDocumentsTheCursor` and
      `HttpEndpointDocumentationTest.theRecentDuelsSectionDocumentsTheFilters` both still pass
      unchanged — neither is edited, and every phrase they match on survives the rewrite
- [ ] The phrase `not yet built` appears nowhere in `docs/protocol.md`
- [ ] `git diff --stat` on the merge shows two changed files, and `docs/protocol.md`'s diff touches
      only the `after` bullet
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
