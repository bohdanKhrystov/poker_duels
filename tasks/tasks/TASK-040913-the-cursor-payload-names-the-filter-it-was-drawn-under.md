---
schema: 2
id: TASK-040913
title: The cursor payload names the filter it was drawn under, and a mismatch decodes to null
type: task
status: done
parent: STORY-0409
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, http, history, paging, filters, cursor]
depends_on: [TASK-040912]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelCursorTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

An encoded cursor carries the fingerprint of the filter that minted it, and `duelCursorOrNull`
answers `null` for a cursor handed back under any other filter — decided by the one re-encode line
that already decides validity.

## Files

**Four files, and the linter caps `files_touched` at three.** Both functions change signature, and
Kotlin does not compile a call site left behind. Exactly four files call one or both of them, and
the whole repository was searched to be sure of that number: `DuelCursor.kt` itself (the re-encode
line inside `duelCursorOrNull`), `DuelCursorTest.kt` (every fixture), `ProfileRoutes.kt` (both
functions, once each), and `ProfileRouteTest.kt` (each function once). The frontmatter says `3`;
the fourth is `ProfileRouteTest.kt`, whose entire change is **two call sites** — named here so the
count is honest rather than hidden. `TASK-040907` and `TASK-040807` paid the same price and
recorded it the same way.

**Do not dodge the fourth file with a default argument.** `encoded(filter: DuelFilter = NONE)` would
keep every call site compiling and is exactly the shape `ADR-0057` rejects: it makes it possible to
encode or decode a cursor *without saying which filter you are doing it under*, so the one caller
who forgets restores the silent reinterpretation with no test failing. Both parameters are required.

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | modify — both functions and both KDocs |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelCursorTest.kt` | modify — every fixture re-cut, three tests added, one renamed |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify — `respondWithDuels` order, `recentDuelsPage`, one KDoc sentence |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify — two call sites, nothing else |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelFilter.kt` | read — `fingerprint()` from `TASK-040912` |

## Scope

- **`encoded` takes the filter, and the payload is three parts:**

  ```kotlin
  public fun encoded(filter: DuelFilter): String =
      Base64.getUrlEncoder().withoutPadding()
          .encodeToString("$finishedAt|$duelId|${filter.fingerprint()}".toByteArray(Charsets.UTF_8))
  ```

  `DuelCursor` stays `(finishedAt, duelId)`. The fingerprint is **not** a field: a cursor is a
  position, and the filter is context supplied at both ends of the encoding (`ADR-0057` §1). The
  separator stays safe by construction — an `Instant`, a `UUID` and unpadded base64url all cannot
  contain `|`.
- **`duelCursorOrNull(raw: String, filter: DuelFilter)` adds no refusal path.** It splits on `|`
  requiring exactly **three** parts, parses the first two exactly as it does today, **never parses
  the third**, and still ends on one line — now
  `DuelCursor(finishedAt, duelId).takeIf { it.encoded(filter) == raw }`. Do not add an `if`
  comparing `parts[2]` to anything: a wrong fingerprint, a non-canonical instant, an upper-cased id
  and mangled base64 must all fail in one place and in one way, or validity stops living in one
  function.
- **`respondWithDuels`'s refusal order becomes identity, limit, filter, cursor.** The swap is
  forced: the cursor cannot be decoded before the filter it is decoded under exists. Move the whole
  filter block above the cursor block and leave the cursor block otherwise as it is, with
  `duelCursorOrNull(rawCursor, filter)`. Identity stays first — that is the only ordering here that
  is a security property, and the comment saying so does not move.
- **`recentDuelsPage` takes the filter** and mints `nextCursor` with `cursor.encoded(filter)`. That
  is what makes the client's next request valid: the page's own filter is the one bound into the
  cursor it hands out. Everything else about that function — the probe row, the `subList`, building
  from `page.last()` rather than the probe — is untouched.
- **`profileRoutes`'s KDoc**: the sentence "among the three, the limit is checked first, then the
  cursor, then the filter" becomes limit, then filter, then cursor, and gains one clause — a cursor
  decoded under a filter other than the one that issued it is refused with the same `400` and the
  same empty body as a cursor that does not decode (`ADR-0057` §5). Do **not** introduce a second
  status code anywhere; this endpoint's whole refusal vocabulary is one status and no body.
- **`ProfileRouteTest.kt` gets exactly two edits**, both mechanical:
  `aCursorFromTheQueryReachesThePort` sends `cursor.encoded(DuelFilter.NONE)`, and
  `aFullPageReportsTheCursorOfItsLastRow` decodes with `duelCursorOrNull(it, DuelFilter.NONE)`.
  Both requests carry no filter parameter, so `DuelFilter.NONE` is the filter the route itself uses
  at both ends and both tests keep asserting exactly what they assert today. No other test in that
  file changes, no assertion is weakened, and **no new test is added there** — `TASK-040914` owns
  the endpoint's four filter/cursor combinations.

### Re-cutting `DuelCursorTest`, which is where this goes wrong quietly

Every hand-built `base64Of(...)` fixture in that file is a **two**-part payload today. After this
change a two-part payload is refused by the `parts.size != 3` guard — so every one of those
assertions keeps passing while proving nothing about the thing its name claims. Each must gain a
third part:

- `aPayloadWhoseHalvesDoNotParseIsRefused` — both fixtures gain `|<the NONE fingerprint>`, so they
  are refused for the unparseable instant and the unparseable id, as named.
- `aNonCanonicalPayloadIsRefused` — both fixtures gain the same third part, so they are refused for
  the non-canonical instant and the upper-cased id, as named. These two are the whole point of the
  canonical re-encode check; a version of them that fails on part-count instead proves nothing.
- `aPayloadWithoutExactlyTwoPartsIsRefused` is **renamed** `aPayloadWithoutExactlyThreePartsIsRefused`
  and its fixtures become a well-formed two-part payload (valid instant, valid id, no fingerprint)
  and a four-part one.
- Hold `"47DEQpj8HBQ"` — the fingerprint of `DuelFilter.NONE` — in one `private const val` beside
  `SAMPLE_DUEL_ID`, **written as a literal**, never as `DuelFilter.NONE.fingerprint()`. A fixture
  computed from the code under test cannot catch that code changing.

### The padding arithmetic, which must be re-verified rather than assumed

`theEncodedFormCarriesNoPadding` and `aPayloadWithPaddingIsRefused` are both load-bearing only
because the fixture payload is not a multiple of three. It was `27 + 1 + 36 = 64` bytes. It becomes
`64 + 1 + 11 = 76`, and `76 ≡ 1 (mod 3)`, so the padded encoder still emits exactly two `=` and both
assertions keep catching a padded encoder. `aPayloadWithPaddingIsRefused`'s comment says "64 bytes";
it must say 76 and why 76 is still not a multiple of three. Its `assertTrue(padded.endsWith("=="))`
line does not change and must not be removed — it is the line that would notice the day this stops
being true. (`ADR-0057` §3 records the counterfactual: sixteen hex characters instead of eleven
base64url ones would have made the payload 81, a multiple of three, and turned both assertions
vacuous.)

### Two files that genuinely do not change, and why

- **`PostgresProfileReadsTest.kt`** builds cursors with the `DuelCursor(finishedAt, duelId)`
  constructor, which is unchanged, and calls neither `encoded` nor `duelCursorOrNull`.
- **`DuelHistoryPagingDatabaseTest.kt`** only ever echoes the opaque `nextCursor` string back
  through the endpoint, and every walk in it is unfiltered — so both ends of every round trip use
  `DuelFilter.NONE` and the walk still closes.

Neither is in the budget. If either stops compiling or fails, something in this ticket was done
differently from the description above; do not "fix" them.

## Out of scope

- Any endpoint-level test of a cursor replayed under a different filter — `TASK-040914`.
- `docs/protocol.md` and `HttpEndpointDocumentationTest` — `TASK-040915`. The document's gap
  sentence is knowingly false between this ticket and that one; that ordering is `ADR-0057` §9's.
- A distinguishable status, a response body, or a log line for a mismatch. §5 answers a flat `400`
  and prices the debugging cost of doing so.
- Signing, keying or any change to `PROTOCOL_VERSION`. The cursor is opaque, not unforgeable (§7),
  and `RecentDuelsResponse` is reachable from neither message root.

## Tests

`DuelCursorTest` — existing tests re-cut as above (each now passes a `DuelFilter` where it called a
no-argument function), plus three new ones.

| Test | Proves |
| --- | --- |
| `aCursorRoundTripsUnderTheFilterThatEncodedIt` | for `f = DuelFilter(DuelOutcomeLabel.WON, "Halvard")` **and** for `DuelFilter.NONE`, `duelCursorOrNull(cursor.encoded(f), f)` equals `cursor`. Two filters, not one: an implementation that appends a fingerprint only when an axis is set passes the first and fails the second |
| `aCursorIsRefusedUnderAnyOtherFilter` | with `a = DuelFilter(WON, null)`: `duelCursorOrNull(cursor.encoded(a), DuelFilter(LOST, null))`, `duelCursorOrNull(cursor.encoded(a), DuelFilter(WON, "Halvard"))` and `duelCursorOrNull(cursor.encoded(a), DuelFilter.NONE)` are all `null`, and so is `duelCursorOrNull(cursor.encoded(DuelFilter.NONE), a)`. Both directions, because "unfiltered accepts anything" and "filtered accepts anything" are two different bugs |
| `theEncodedFormIsItsGoldenVector` | `DuelCursor(Instant.parse("2026-08-13T10:02:03.000004Z"), UUID.fromString(SAMPLE_DUEL_ID)).encoded(DuelFilter(WON, "Halvard"))` is exactly `"MjAyNi0wOC0xM1QxMDowMjowMy4wMDAwMDRafDBmOGZhZDViLWQ5Y2ItNDY5Zi1hMTY1LTcwODY3NzI4OTUwZXxKOHFEeDEzQ0lfbw"`. §8.3's golden vector: every other fixture in this file round-trips through the implementation and so survives a rendering change silently; this literal does not |
| `aPayloadWithoutExactlyThreePartsIsRefused` | *(renamed)* a two-part payload and a four-part payload are both `null` |
| `aPayloadWhoseHalvesDoNotParseIsRefused` | *(re-cut)* with a valid third part present, `yesterday\|…` and `…\|not-a-uuid` are still refused — for the reason the name gives, not for the part count |
| `aNonCanonicalPayloadIsRefused` | *(re-cut)* with a valid third part present, a non-canonical instant and an upper-cased id are still refused |
| `theEncodedFormCarriesNoPadding` | *(re-cut)* the 76-byte payload's encoding contains no `=` |
| `aPayloadWithPaddingIsRefused` | *(re-cut)* the padded encoding of that 76-byte payload ends `==` and is refused |

The golden vector was computed from `ADR-0057` §2 and §3 before this ticket was written; it decodes
to `2026-08-13T10:02:03.000004Z|0f8fad5b-d9cb-469f-a165-70867728950e|J8qDx13CI_o`, whose third part
is `TASK-040912`'s golden fingerprint for the same filter. If the implementation disagrees with it,
the implementation is wrong — do not regenerate the literal.

## Acceptance criteria

- [ ] `DuelCursorTest.aCursorRoundTripsUnderTheFilterThatEncodedIt` passes
- [ ] `DuelCursorTest.aCursorIsRefusedUnderAnyOtherFilter` passes
- [ ] `DuelCursorTest.theEncodedFormIsItsGoldenVector` passes
- [ ] `DuelCursorTest.aPayloadWithoutExactlyThreePartsIsRefused` passes, and no test named
      `aPayloadWithoutExactlyTwoPartsIsRefused` remains in the file
- [ ] `DuelCursorTest.aPayloadWhoseHalvesDoNotParseIsRefused` and
      `DuelCursorTest.aNonCanonicalPayloadIsRefused` both pass **with three-part fixtures** — all
      four `base64Of(...)` arguments across those two tests contain exactly two `|` characters
      each, so each is refused by the re-encode check and not by the part-count guard. (The
      part-count guard's own test is the only place a one- or three-`|` payload belongs.)
- [ ] `DuelCursorTest.theEncodedFormCarriesNoPadding` and `DuelCursorTest.aPayloadWithPaddingIsRefused`
      both pass, and the latter still asserts `padded.endsWith("==")`
- [ ] `duelCursorOrNull`'s body contains exactly one comparison against `raw`, and no comparison
      against `parts[2]`
- [ ] Neither `encoded` nor `duelCursorOrNull` declares a default value for its `DuelFilter`
      parameter
- [ ] `ProfileRouteTest` passes with exactly two lines changed —
      `aCursorFromTheQueryReachesThePort` and `aFullPageReportsTheCursorOfItsLastRow` each now pass
      `DuelFilter.NONE`; every other test in that file is byte-identical and no assertion in it is
      weakened or deleted
- [ ] `PostgresProfileReadsTest` and `DuelHistoryPagingDatabaseTest` pass unchanged — neither is
      edited, because neither calls either function
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
