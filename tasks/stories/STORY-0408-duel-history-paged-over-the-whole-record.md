---
id: STORY-0408
title: Duel history, paged over the whole record
type: story
status: backlog
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

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0408` once `STORY-0402` has merged.* | — |

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
