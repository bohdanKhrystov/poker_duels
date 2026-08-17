---
schema: 2
id: TASK-040801
title: A cursor is one duel's place in the list, and it survives the round trip
type: task
status: done
parent: STORY-0408
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, http, history, paging]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelCursorTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`(finishedAt, duelId)` — the exact tuple the duel list already orders by — encodes to one opaque
string, decodes back from exactly that string, and from nothing else.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelCursorTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecentDuelsLimit.kt` | read — the sibling parser whose shape this copies: a top-level `…OrNull` that answers `null` for *refuse* |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read — `RECENT_DUELS_SQL`'s `ORDER BY d.finished_at DESC, d.id DESC` is the tuple this type names |

## Scope

- `public data class DuelCursor(val finishedAt: java.time.Instant, val duelId: java.util.UUID)`,
  with a member `public fun encoded(): String`.
- `encoded()` is
  `Base64.getUrlEncoder().withoutPadding().encodeToString("$finishedAt|$duelId".toByteArray(UTF_8))`.
  URL-safe because the value travels in a query string; unpadded because `=` in a query string is
  the one character that most often gets re-encoded on the way back.
- `public fun duelCursorOrNull(raw: String): DuelCursor?` — `null` for anything the server would not
  itself have produced. In order: base64url-decode (catching `IllegalArgumentException`), read as
  UTF-8, `split("|")` and require **exactly two** parts, `Instant.parse` and `UUID.fromString` (each
  catching `Exception`), then the canonical check below.
- **The canonical check**: the decoded cursor's own `encoded()` must equal `raw`, or the answer is
  `null`. Two different strings must never name one row — `2026-08-13T10:00:00.000Z` and
  `2026-08-13T10:00:00Z` are the same instant, and an upper-cased UUID is the same id.
- The separator is safe by construction: neither `Instant.toString()` nor `UUID.toString()` can
  contain `|`, so a part count other than two is a refusal rather than a parse.
- The parameter is **not** nullable. Absent is the caller's business: a missing query parameter
  means *the newest page*, and this function never sees it (`RecentDuelsLimit.kt` splits the same
  way — it answers a default for absent, `null` for refuse; here absent never arrives).

## Out of scope

- **Signing the cursor.** It is opaque, not unforgeable, and the difference is deliberate: the read
  is keyed off the player the *server* resolved (`ADR-0002`, and `STORY-0408`'s own note that a
  handed-over cursor still reads no other player's rows), so a forged cursor can only name a
  position inside the forger's own history. An HMAC would add a key, its rotation and its config to
  defend nothing. If that ever changes it is an ADR, not a ticket.
- Reading anything, the SQL, the route, the response field — `TASK-040802`, `TASK-040806`,
  `TASK-040808`.
- Building a cursor from a `DuelSummaryResponse`. `TASK-040809` does that in the route, where the
  row is.

## Tests

`DuelCursorTest`

| Test | Proves |
| --- | --- |
| `aCursorEncodesAndDecodesBackToItself` | `DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"))` round-trips to an equal `DuelCursor` — microsecond precision, which is what PostgreSQL stores, survives |
| `theEncodedFormCarriesNoPadding` | that same cursor's `encoded()` contains no `=` |
| `theEncodedFormShowsNeitherHalfInTheClear` | that same `encoded()` contains neither `2026-` nor `0f8fad5b` — *opaque* asserted rather than claimed |
| `aStringThatIsNotBase64IsRefused` | `duelCursorOrNull("not a cursor!!")` and `duelCursorOrNull("")` are both `null` |
| `aPayloadWithoutExactlyTwoPartsIsRefused` | base64url of `2026-08-13T10:00:00Z` alone, and of `2026-08-13T10:00:00Z\|x\|y`, are both `null` |
| `aPayloadWhoseHalvesDoNotParseIsRefused` | base64url of `yesterday\|0f8fad5b-…` and of `2026-08-13T10:00:00Z\|not-a-uuid` are both `null` |
| `aNonCanonicalPayloadIsRefused` | base64url of `2026-08-13T10:00:00.000Z\|0f8fad5b-…` and of the same instant with the UUID **upper-cased** are both `null` — both parse, and both are refused because neither is what `encoded()` would emit |

**The padding fixture is load-bearing, not decoration.** `2026-08-13T10:02:03.000004Z|<uuid>` is 64
bytes, which is not a multiple of three, so a padded encoder produces two `=` and
`theEncodedFormCarriesNoPadding` fails. A whole-second instant (`…T10:00:00Z|<uuid>`) is 57 bytes,
a multiple of three, pads to nothing, and would pass against a padded encoder while proving
nothing. Use the sub-second fixture.

**Do not add a test asserting the URL-safe alphabet.** Measured: for every payload this type can
produce, standard and URL-safe base64 emit identical text, so such a test passes against
`Base64.getEncoder()` and proves nothing. The alphabet is a construction requirement above; the
padding is the part that is observable.

## Acceptance criteria

- [ ] `DuelCursorTest.aCursorEncodesAndDecodesBackToItself` passes with a microsecond-precision instant
- [ ] `DuelCursorTest.theEncodedFormCarriesNoPadding` passes, and its fixture's payload length is not
      a multiple of three
- [ ] `DuelCursorTest.theEncodedFormShowsNeitherHalfInTheClear` passes
- [ ] `DuelCursorTest.aStringThatIsNotBase64IsRefused` passes and asserts both inputs
- [ ] `DuelCursorTest.aPayloadWithoutExactlyTwoPartsIsRefused` passes and asserts both inputs
- [ ] `DuelCursorTest.aPayloadWhoseHalvesDoNotParseIsRefused` passes and asserts both inputs
- [ ] `DuelCursorTest.aNonCanonicalPayloadIsRefused` passes and asserts both inputs
- [ ] `duelCursorOrNull` takes a non-nullable `String` and never throws for any input above
- [ ] No test method in the diff declares `: Unit`, and every one ends in an assertion
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
