---
id: STORY-0408
title: Duel history, paged over the whole record
type: story
status: ready
parent: EPIC-04
module: poker-server
labels: [server, http, read-path, history]
depends_on: [STORY-0402]
---

## Goal

A player can read every duel they have ever played, a page at a time, and the pages are **total and
disjoint**: each duel appears exactly once, in one order, with no gap and no duplicate — even when a
duel finishes between two page requests.

## Why

`EPIC-02` shipped *"a handful of recent results"* — a capped, offset-free list — and explicitly
called the rest ours. `STORY-0413`'s screen needs pages, and `STORY-0409`'s filters need a paging
rule that survives them.

## Design notes

- **The totality rule is the epic's definition of done**, and it decides the mechanism: `LIMIT` with
  `OFFSET` cannot satisfy it, because a duel inserted between two requests shifts every later row and
  a reader either sees one twice or misses one. **Keyset (seek) paging on the order the list already
  has** — `(finished_at DESC, duel_id DESC)`, the exact tuple `RECENT_DUELS_SQL` already orders by —
  does satisfy it, because a cursor names a row rather than a position.
- **The cursor is opaque to the client and carries that tuple.** It is returned by the server with
  each page and handed back verbatim; a client that constructs one is asserting a game fact, which
  `ADR-0002` forbids. A malformed or unparseable cursor is a `400`, never a silent first page.
- **`GET /api/me/duels` keeps its current contract for a caller that sends no cursor**, so
  `STORY-0311`'s profile strip does not break: the default limit and the cap stay, an absent cursor
  means the newest page, and the response gains a next-page field that is absent or null on the last
  page.
- **One query per page, still no N+1**, and the same single join `STORY-0402` added.
- **The cap stays.** An unbounded `limit` is a denial-of-service parameter, and paging is not a
  reason to remove the ceiling — it is the reason the ceiling is now usable.
- The endpoint stays player-keyed off the resolved identity, so a cursor from one player cannot read
  another's rows even if it is handed over.

## Tasks

Split on 2026-08-18, against what `STORY-0401`–`STORY-0404` actually landed. The chain is linear on
purpose: the run is sequential, and `PostgresProfileReads.kt`, `PostgresProfileReadsTest.kt`,
`ProfileRoutes.kt` and `ProfileRouteTest.kt` are each touched by more than one ticket, so two
startable tickets would be two tickets editing one file.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-040801](../tasks/TASK-040801-a-cursor-is-a-duels-place-in-the-list.md) | A cursor is one duel's place in the list, and it survives the round trip | ready |
| [TASK-040802](../tasks/TASK-040802-the-read-takes-a-cursor-and-compares-the-whole-tuple.md) | The read takes a cursor, and PostgreSQL compares the whole tuple | backlog |
| [TASK-040803](../tasks/TASK-040803-seven-duels-in-pages-of-three-each-exactly-once.md) | Seven duels in pages of three, each exactly once | backlog |
| [TASK-040804](../tasks/TASK-040804-a-duel-that-finishes-between-two-pages.md) | A duel that finishes between two pages repeats nothing and skips nothing | backlog |
| [TASK-040805](../tasks/TASK-040805-two-duels-in-the-same-instant-still-page.md) | Two duels that finished in the same instant still page | backlog |
| [TASK-040806](../tasks/TASK-040806-the-response-says-whether-there-is-a-next-page.md) | The response says whether there is a next page, as null and not as absent | backlog |
| [TASK-040807](../tasks/TASK-040807-the-port-takes-the-cursor-and-the-doubles-follow.md) | The port's duel read takes the cursor, and both doubles follow | backlog |
| [TASK-040808](../tasks/TASK-040808-the-endpoint-accepts-a-cursor-and-refuses-a-malformed-one.md) | The endpoint accepts a cursor, and a malformed one is a 400 that reads nothing | backlog |
| [TASK-040809](../tasks/TASK-040809-one-row-more-than-the-page.md) | One row more than the page, and the last page says there is no next | backlog |
| [TASK-040810](../tasks/TASK-040810-over-http-against-the-database-every-duel-once.md) | Over HTTP, against the database — every duel exactly once, and one player's cursor | backlog |
| [TASK-040811](../tasks/TASK-040811-the-document-contracts-the-cursor.md) | The document contracts the cursor and the paging rule, and a test agrees with the DTO | backlog |

### What the split settled, and what it sharpened

Three things were decided while splitting, each written into the ticket that carries it rather than
left for a coder to guess:

- **The cursor is opaque, not unforgeable.** No signature, no HMAC, no key. The read is keyed off
  the player the *server* resolved, so a forged cursor names a position inside the forger's own
  history and nothing else; opacity is the contract and unforgeability would buy a key, its rotation
  and its config to defend nothing. `TASK-040801` records the reasoning. Changing it later is an
  ADR, not a ticket.
- **`nextCursor` is present and `null` on the last page, never absent.** `ContentNegotiation`'s
  `Json` has `encodeDefaults = false` while `protocolJson` has it `true`, so a defaulted field is
  present in every test's JSON and missing from the wire — the trap `ADR-0053` records for
  `ProfileResponse`, in the same position here. `TASK-040806` owns it, and it is why
  `ProfileDtosTest`'s exact-string assertion moves.
- **The port gains a parameter rather than a second port.** `ProfileReads.recentDuelsOf` takes
  `after: DuelCursor? = null`, so the composition root, `ServerComponents` and `Application.kt` are
  untouched and no half-used second read path is left behind. The cost is that `TASK-040807` breaks
  two test doubles at compile time and therefore touches four files; that ticket says so in its body.

And one acceptance criterion above is sharper in the tickets than it is here. *"A cursor issued to
one player returns nothing for another player"* is literally true only when the second player has
no duels older than that instant: a keyset cursor is a **position, not a permission**, and what
actually defends the row is that the query is keyed off the resolved device. `TASK-040810` therefore
asserts the general property — **no cross-player row** — with a second player who *does* have a duel
in range, so the test cannot pass by returning an empty page.

Two things this story deliberately does not do, recorded so they are not rediscovered:

- **No index and no migration.** `duel.finished_at` and `duel_result.player_id` are unindexed today,
  and adding one is a new `V<n>` that would race `STORY-0410`'s migration number (`ADR-0029` §8).
  Keyset paging is correct without it; it is not yet *fast* at a size v0.1 does not have. Not
  ticketed anywhere yet.
- **No client.** `web-client/src/profile/recent-duels.ts` reads `duels` and ignores every other key,
  so nothing there breaks; the history screen is `STORY-0413`.

## Acceptance criteria

- [ ] `N` duels read in pages of `k` return each duel exactly once, in one order, with no gap and no
      duplicate — asserted by collecting every page and comparing the multiset to the stored rows,
      with `N` not a multiple of `k`.
- [ ] The same holds when a new duel is inserted **between** two page requests: no already-returned
      duel is returned again, and none is skipped.
- [ ] Two duels that finished in the same instant are still ordered totally and paged without
      duplication — the tie-break column is what this proves.
- [ ] A request with no cursor returns the newest page and behaves exactly as `GET /api/me/duels`
      does today, including the default and the cap.
- [ ] The last page reports that there is no next page, and asking again returns an empty page
      rather than an error.
- [ ] A malformed cursor answers `400` and reads nothing.
- [ ] A cursor issued to one player returns nothing for another player.
- [ ] `docs/protocol.md` contracts the cursor and the paging rule.

## Out of scope

- Filters and search — `STORY-0409`.
- The screen — `STORY-0413`.
- The hands themselves: history lists results, not cards. `DEC-008` is unanswered and the `MatchLog`
  is not persisted — `EPIC-08`.
