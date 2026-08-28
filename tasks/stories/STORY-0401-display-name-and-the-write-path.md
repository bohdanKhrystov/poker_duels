---
id: STORY-0401
title: player.display_name, its canonical form, and the write path
type: story
status: done
parent: EPIC-04
module: poker-server
labels: [server, http, profiles, schema, identity]
depends_on: []
---

## Goal

A player sets a name once, over `PUT /api/me/name`, and `GET /api/me` reads it back. The name is
unique case-insensitively, permanent once set, and canonical — trimmed, NFC, 1–32 code points, with
no invisible characters — and the database, not a hope about every write path, is what guarantees
it.

## Why

`EPIC-03` renders a UUID where a name belongs and said so in its own out-of-scope table.
[`ADR-0021`](../../docs/adr/ADR-0021-a-profile-gains-a-display-name.md) is **Accepted and unbuilt**:
the migration chain on disk stops at `V2`, there is no `display_name` column, no `ProfileWrites`
port and nothing to join. This story is where that shape becomes code.

It goes first in `EPIC-04` for a second reason the ADRs both name. `ADR-0027` §1, `ADR-0031` §7 and
`ADR-0029` §8 each say their migration takes *the next free `V<n>` at merge time*, which is a race
between three stories. Landing this one first settles it: the display name is `V3`, and the
credential chain numbers itself from `V4` with nothing to negotiate.

## Design notes

- **The schema is [`ADR-0029`](../../docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md)
  §1, §2 and §4, transcribed.** One new migration file carries all of it: the nullable column,
  `player_display_name_length` (1–32, from `ADR-0021`), `player_display_name_trimmed`,
  `player_display_name_nfc`, the unique index on `lower(display_name COLLATE "und-x-icu")`, and the
  `player_display_name_permanent` trigger. `V1` and `V2` are byte-unchanged — a schema change is a
  new file, always.
- **The canonical form is Kotlin's, the guarantee is Postgres'.** The write path trims, normalises
  to NFC, counts **code points** (not UTF-16 units), and refuses `Cc`/`Cf`, any whitespace that is
  not `U+0020`, and two consecutive spaces — `ADR-0029` §3. The three `CHECK`s exist so that a
  second write path added in a year cannot bypass the rule. Both are tested; the trigger and the
  checks are guards that should never fire in normal operation, so each is proven to fire by
  planting a real violation.
- **The write gets its own port.** `ProfileWrites` in `duels.poker.server.http`,
  `PostgresProfileWrites` in `duels.poker.server.db` — `ProfileReads`' contract is that nothing on
  it creates or mutates, and tests rely on that (`ADR-0021`). The port returns a sealed
  `SetNameResult` — `NameSet(profile) | NameTaken | AlreadyNamed` — never an exception, and
  `SQLSTATE` is translated inside `db` and never reaches a route.
- **The write is one statement and the index is the reservation** (`ADR-0029` §5):
  `UPDATE player SET display_name = ? WHERE id = ? AND display_name IS NULL`. One row is success,
  `23505` is `NameTaken`, zero rows means read the stored name and answer `NameSet` if it equals the
  requested canonical form (the idempotent retry) or `AlreadyNamed` otherwise.
- **The endpoint's five answers** are `ADR-0029` §5's table: `401` absent/blank/unknown device,
  `200` with `ProfileResponse`, `400` for a name the canonical form refuses, `409` for a collision,
  `403` for a player who already holds a different name. Identity is resolved **before** the body is
  read, as on the existing routes. `DisplayName.kt` sits beside `RecentDuelsLimit.kt` in
  `duels.poker.server.http`, which is where that file's precedent puts a request-value rule.
- **The request body is `{"name": "…"}`**, a `SetNameRequest` DTO in
  `duels.poker.server.protocol.http` — the convention every other endpoint in that file already
  follows, not a new decision. A body that is not that object is a `400`, the same answer a refused
  name gets, so the endpoint has one shape of failure to describe.
- **`ProfileResponse.displayName: String?` lands here, not in `STORY-0402`.** `ADR-0029` §5 requires
  the `200` to carry the canonical string the player now owns — that is `ADR-0002` in miniature, and
  it saves a round trip. `DuelSummaryResponse.opponentDisplayName` and the join are `STORY-0402`'s.
- **No default value on the wire field**, per `ADR-0021`: `Application.module()` installs a `Json`
  with `encodeDefaults = false`, so a defaulted property would vanish from the response body. That
  makes the widening break every call site at once — hence the builder ticket that goes first, which
  `STORY-0402` then reuses for `DuelSummaryResponse`.
- **The server fabricates no placeholder.** `null` means never set. What a client renders for it is
  `STORY-0411`'s, inside `EPIC-06`'s language.
- **A name is never an authentication factor.** No function on either port takes a name and returns
  a player, a device or a profile, and a test asserts that structurally over the public API
  (`ADR-0029` §7).

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-040101](../tasks/TASK-040101-the-third-migration-adds-the-name-and-its-guarantees.md) | The third migration adds the name and its four guarantees | ready |
| [TASK-040102](../tasks/TASK-040102-the-checks-refuse-what-they-were-written-to-refuse.md) | The three checks refuse what they were written to refuse | backlog |
| [TASK-040103](../tasks/TASK-040103-one-name-whatever-case-it-is-typed-in.md) | One name, whatever case it is typed in | backlog |
| [TASK-040104](../tasks/TASK-040104-permanence-fires-and-only-on-the-column-it-names.md) | Permanence fires, and only on the column it names | backlog |
| [TASK-040105](../tasks/TASK-040105-the-canonical-name-is-trimmed-nfc-and-counted-in-code-points.md) | The canonical name is trimmed, NFC, and counted in code points | backlog |
| [TASK-040106](../tasks/TASK-040106-the-canonical-form-refuses-the-invisible-and-the-doubled-space.md) | The canonical form refuses the invisible and the doubled space | backlog |
| [TASK-040107](../tasks/TASK-040107-one-builder-makes-every-profile-dto-a-test-uses.md) | One builder makes every profile DTO a test uses | backlog |
| [TASK-040108](../tasks/TASK-040108-profile-response-carries-the-name-the-row-holds.md) | `ProfileResponse` carries the name the row holds | backlog |
| [TASK-040109](../tasks/TASK-040109-the-name-is-on-the-wire-and-it-is-the-one-stored.md) | The name is on the wire, and it is the one stored | backlog |
| [TASK-040110](../tasks/TASK-040110-the-profile-writes-port-and-its-sealed-answer.md) | The `ProfileWrites` port, its sealed answer, and no lookup by name | backlog |
| [TASK-040111](../tasks/TASK-040111-one-statement-three-answers.md) | One statement, three answers | backlog |
| [TASK-040112](../tasks/TASK-040112-two-writers-one-name.md) | Two writers, one name: the loser is refused and keeps its nothing | backlog |
| [TASK-040113](../tasks/TASK-040113-the-one-field-the-request-body-carries.md) | The one field the request body carries | backlog |
| [TASK-040114](../tasks/TASK-040114-the-server-it-ships-with-can-write-a-name.md) | The server it ships with can write a name | backlog |
| [TASK-040115](../tasks/TASK-040115-put-api-me-name-identity-first-then-the-name.md) | `PUT /api/me/name`: identity first, then the name it accepts | backlog |
| [TASK-040116](../tasks/TASK-040116-the-two-refusals-a-client-must-tell-apart.md) | The two refusals a client must tell apart | backlog |
| [TASK-040117](../tasks/TASK-040117-a-name-set-over-http-comes-back-on-the-next-read.md) | A name set over HTTP comes back on the next read | backlog |
| [TASK-040118](../tasks/TASK-040118-document-the-name-endpoint.md) | Document the name endpoint and what each answer means | backlog |

## Acceptance criteria

- [ ] A player with no name sets one over `PUT /api/me/name` and reads it back on `GET /api/me`,
      asserted end to end against the database container.
- [ ] The stored name is the canonical form: trimmed, NFC, 1–32 code points — asserted with an
      input that differs from its canonical form in every one of those three ways.
- [ ] A second player cannot take the same name in any case, and is told so with `409`.
- [ ] A player who already holds a name cannot change it: the endpoint answers `403`, the trigger
      refuses the same `UPDATE` with `23001`, and re-sending the *identical* name answers `200`.
- [ ] A refused name — empty, over 32 code points, containing a control character, a zero-width
      joiner, a tab or a doubled space — is a `400` and writes nothing.
- [ ] An absent, blank or unknown device id answers `401` and creates no row.
- [ ] `V1` and `V2` are byte-unchanged, and the chain reports exactly `1`, `2`, `3`.
- [ ] No function on `ProfileReads` or `ProfileWrites` takes a name and returns an identity,
      asserted structurally over the public API.
- [ ] `docs/protocol.md` contracts the endpoint, and `HttpEndpointDocumentationTest` checks the
      claim rather than the words.

## Out of scope

- **The blocklist, the retired-name set, and the operator force-rename** —
  [`ADR-0038`](../../docs/adr/ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md), all
  three in `STORY-0410`. This story's uniqueness consults exactly one source of truth: names in use.
- The join that names an opponent, and `DuelSummaryResponse.opponentDisplayName` — `STORY-0402`.
- Anything a client renders, including what it shows for a `null` name — `STORY-0411`.
- Credentials, sessions, accounts — `STORY-0403` onwards. Nothing here authenticates with a name.
- `poker-engine`, which learns nothing from this story.
