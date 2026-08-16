---
id: STORY-0402
title: The read path carries the display name
type: story
status: ready
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

## What `STORY-0401` already did, so this story does not redo it

Split on 2026-08-17, after all eighteen `STORY-0401` tickets merged. Four of them shrank this story
and one of them shaped it:

- `TASK-040107` routed **every** test-side `DuelSummaryResponse` through `ProfileDtoFixtures`, and
  said in its own comment that `STORY-0402` widens that DTO next. So the widening is the DTO, the
  reader and the builder — three files — and no test file joins the budget. That is the move four
  `STORY-0401` tickets needed a fourth file for, made once, in advance.
- `TASK-040108` set the precedent for the field itself: nullable, **no default**, because
  `ContentNegotiation { json() }` has `encodeDefaults = false` and a defaulted property would be
  absent from the body rather than present as `null`.
- `TASK-040109` set the precedent for its proof: two distinct inputs, the `null` one passed
  explicitly, asserted on encoded text.
- `V3`, `canonicalDisplayNameOrNull`, `PUT /api/me/name` and `ProfileResponse.displayName` all
  shipped. Nothing here re-tests them.

## Two constraints that bind this story, and what they turned out to mean

**`TASK-031103` dropped `opponentPlayerId` at the client parse, and nothing here reopens it.** That
ticket's stated reason was that *"no display name exists yet, so the only thing the client could
print is a raw identifier in front of a player"* — it removed the id from the client's `RecentDuel`,
not from the wire. `ADR-0021` keeps `opponentPlayerId` on the wire deliberately, and this story adds
a second field the client parse also ignores, because that parse names five keys one by one and
drops every other. So: no client file changes, no client test moves, and `STORY-0411` decides what a
client does with the name. The one thing a ticket here must not do is "helpfully" tidy the client.

**`ADR-0038` is not reopened either, and it anticipated this.** It settles that a name is screened
by a blocklist *when it is set* — on the write path, in `STORY-0410` — and that an operator can take
one away afterwards, at which point the read path reads the current column and the name simply stops
appearing. It also states outright that homoglyph impersonation is **not** solved and that a
restriction to close it "was on the table and was not chosen". A read path that shows a name someone
chose is the accepted consequence of that decision, named in the ADR, not a new question. No
`DEC-NNN` is raised by this story.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-040201](../tasks/TASK-040201-the-duel-line-joins-the-opponents-row-and-carries-their-name.md) | The duel line joins the opponent's row and carries their name | ready |
| [TASK-040202](../tasks/TASK-040202-a-named-opponent-an-unnamed-one-and-a-name-set-afterwards.md) | A named opponent, an unnamed one, and a name set after the duel | backlog |
| [TASK-040203](../tasks/TASK-040203-three-duels-three-opponents-one-prepared-statement.md) | Three duels, three opponents, one prepared statement | backlog |
| [TASK-040204](../tasks/TASK-040204-present-as-null-not-absent-on-the-real-response.md) | Present as `null`, not absent, on the response the route actually writes | backlog |
| [TASK-040205](../tasks/TASK-040205-the-document-names-the-field-and-the-test-agrees-with-the-dto.md) | The document names the field, and the test agrees with the DTO | backlog |

One linear chain. `040202` and `040203` both edit `PostgresProfileReadsTest.kt` and are consecutive
for that reason; `040205` is last because documenting a field before it exists fails
`theDocumentedFieldNamesAllExist`.

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
