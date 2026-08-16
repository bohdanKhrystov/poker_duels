---
id: STORY-0402
title: The read path carries the display name
type: story
status: backlog
parent: EPIC-04
module: poker-server
labels: [server, http, profiles, read-path]
depends_on: [STORY-0401]
---

## Goal

A result line names the opponent instead of printing a UUID at a human: `RECENT_DUELS_SQL` gains one
more join, and `DuelSummaryResponse` gains `opponentDisplayName: String?` — the name as it stands
when the list is requested, or `null` when that opponent never set one.

## Why

`STORY-0401` stores the name; a stored name nobody can read is not a feature. `EPIC-03`'s results
list renders `opponentPlayerId` today and pointed here. `STORY-0408`'s paging and `STORY-0409`'s
search both build on this query, and `STORY-0413`'s screen shows what it returns.

## Design notes

- **One more join, still one query** ([`ADR-0021`](../../docs/adr/ADR-0021-a-profile-gains-a-display-name.md)):
  `JOIN player p ON p.id = o.player_id`, selecting `p.display_name`. `STORY-0211`'s no-N+1 rule
  holds — the opponent row is already joined; this reads one more column off it.
- **`opponentPlayerId` stays.** The id is the stable identity a client correlates on; the name is a
  label. Both travel.
- **Nullable, no default, no placeholder.** The server never invents `Anonymous` or `Player-3F2A`
  — `ADR-0029` §6 sharpens `ADR-0021`'s reason: a server-minted name would be a name inside a unique
  namespace that two players could hold at once.
- **The name is read at request time**, so a name set later correctly labels a duel played today,
  and nothing is snapshotted into `duel_result`. That is `ADR-0021`'s current-name model, and
  [`ADR-0039`](../../docs/adr/ADR-0039-v01-offers-no-account-deletion.md) forbids denormalising it
  into the result row.
- **The widening breaks every call site**, exactly as `ProfileResponse`'s did: the field takes no
  default, so `PostgresProfileReads` and every test that constructs a `DuelSummaryResponse` change
  with it. `STORY-0401`'s builder (`ProfileDtoFixtures`) is where those call sites already live, so
  the change is the DTO, the reader and the builder — and the proof is a separate ticket.
- `docs/protocol.md`'s duel-summary table gains the field, and
  `HttpEndpointDocumentationTest.theDocumentDoesNotCallANonNullFieldNullable` already reflects over
  the DTO, so the document cannot claim the wrong nullability.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0402` once `STORY-0401` has merged.* | — |

## Acceptance criteria

- [ ] A duel against a named opponent reads back that opponent's name, asserted through the real
      query against the database container.
- [ ] A duel against an opponent who never set a name reads back `null` — asserted in the same test
      run as the case above, so the field is proven to vary rather than to be constant.
- [ ] A name set *after* a duel finished appears on that duel's line.
- [ ] The recent-duels query is still one statement, and returns one row per duel.
- [ ] `docs/protocol.md` documents `opponentDisplayName`, and the documentation test agrees with the
      DTO.

## Out of scope

- Paging, filters and search — `STORY-0408` and `STORY-0409`.
- Any client rendering — `STORY-0411` and `STORY-0413`.
- Showing another player's profile: `/api/me` means me. `EPIC-05` owns what a leaderboard row links
  to.
