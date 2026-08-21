---
schema: 2
id: TASK-050202
title: A standings cursor carries the walk's cutoff, and one from another season does not decode
type: task
status: done
parent: STORY-0502
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, http, leaderboard, cursor]
depends_on: [TASK-050201]
verify:
  - ./gradlew :poker-server:test --tests '*StandingsCursorTest'
  - ./gradlew :poker-server:ktlintCheck
  - "grep -qF 'fun standingsCursorOrNull(raw: String, season: Season): StandingsCursor?' poker-server/src/main/kotlin/duels/poker/server/http/StandingsCursor.kt"
  - "grep -qF 'season.contains(' poker-server/src/main/kotlin/duels/poker/server/http/StandingsCursor.kt"
---

## Goal

A standings cursor is the triple `ADR-0066` §2 fixes — the walk's cutoff, a standing and a player id
— it survives its own round trip, and a cursor whose cutoff is not in the season the caller names
comes back `null`.

## The shape, from the ADR rather than from taste

[`ADR-0066`](../../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md) §2
gives the type and the decoding contract; copy them rather than reinvent:

```kotlin
public data class StandingsCursor(val asOf: Instant, val coins: Int, val playerId: UUID) {
    public fun encoded(): String   // base64url, unpadded, "$asOf|$coins|$playerId"
}

public fun standingsCursorOrNull(raw: String, season: Season): StandingsCursor?
```

`DuelCursor`/`duelCursorOrNull` in the same package is the worked example for every mechanical part
— the `Base64.getUrlEncoder().withoutPadding()`, the three-way split, the `takeIf { it.encoded() ==
raw }` canonical check, the two `try` blocks. Two deliberate differences: there is **no filter
fingerprint** (`ADR-0066` §2 — the ladder takes no client-supplied predicate, so there is nothing to
fingerprint), and there **is** a season check.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsCursor.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsCursorTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | read |

## Scope

- One data class and one top-level function in one new file. The file holds two top-level
  declarations, so ktlint's filename rule is satisfied by naming it for the class.
- `encoded()` is `base64url`, **unpadded**, over `"$asOf|$coins|$playerId"` in that order. The
  separator is safe by construction: an `Instant`, an `Int` and a `UUID` cannot contain `|`.
- `standingsCursorOrNull` returns `null` — never throws, never a partial value — for every one of:
  not base64; a payload that does not split into exactly three parts; an unparseable instant, a
  coins value that is not an `Int`, or an unparseable id; a payload that decodes but is **not** the
  canonical encoding of what it decodes to; and an `asOf` for which `season.contains(asOf)` is
  false.
- `season` is `duels.poker.server.season.Season` and the check is its own `contains`, so the
  half-open window is not re-implemented here.
- KDoc says what the `null` means and what it does not: opaque, **not unforgeable** — `ADR-0066` §7
  is explicit that this is a consistency check and never an authorisation control, because
  `ADR-0065` §4 makes the page identical for every reader.

## Out of scope

- **The `400`.** Turning `null` into a status code is `TASK-050209`'s route. This function knows no
  HTTP.
- **A filter fingerprint** (`ADR-0066` §2), **a rank inside the cursor** (§5), and **`limit`**
  (§2, `ADR-0057` §6 — it stays outside and may change mid-walk). None of the three appears in this
  file.
- **A keyed MAC.** `ADR-0066` §7 works through why `ADR-0057` §7's condition is not met here and
  adds none.
- **Minting the cutoff.** `Instant.now(clock)` belongs to the route (`TASK-050209`); this file names
  no clock and imports none.
- Touching `DuelCursor` in any way. It is on the list to be **read**.

## Tests

`StandingsCursorTest`, in `duels.poker.server.http`. JUnit `@Test`, `kotlin.test` assertions. Every
fixture uses `Season(2026, 8)` and an `asOf` inside August 2026 unless the test is about being
outside it.

| Test | Proves |
| --- | --- |
| `encodesToTheExactStringTheServerHandsOut` | `StandingsCursor(Instant.parse("2026-08-15T12:00:00Z"), 3, UUID.fromString("00000000-0000-4000-8000-000000000001")).encoded()` is **literally** `MjAyNi0wOC0xNVQxMjowMDowMFp8M3wwMDAwMDAwMC0wMDAwLTQwMDAtODAwMC0wMDAwMDAwMDAwMDE` — the expected value is a string literal in the test, never a re-encoding of the same call |
| `survivesItsOwnRoundTripIncludingANegativeStanding` | a cursor at `coins = -2` decodes back to an equal `StandingsCursor` under the same season; the minus sign is in the payload and the assertion is on the whole data class |
| `refusesACursorFromBeforeTheSeasonBegan` | `asOf` one millisecond before `season.start` is `null` |
| `refusesACursorStampedAtTheSeasonsEnd` | `asOf == season.endExclusive` is `null`, and `asOf == season.start` decodes — the window is half-open at both ends and this is the pair that says so |
| `refusesAPayloadThatIsNotItsOwnCanonicalEncoding` | three inputs, each `null`: the **padded** spelling of a valid cursor (`…MDE=`), the same payload with an upper-cased id, and the same instant written `2026-08-15T12:00:00+00:00` |
| `refusesWhatDoesNotDecode` | not base64 at all, the empty string, a two-part payload and a four-part payload are each `null` |

**Named mutations.** Deleting the `season.contains(asOf)` check reddens
`refusesACursorFromBeforeTheSeasonBegan` and `refusesACursorStampedAtTheSeasonsEnd`. Returning the
decoded cursor instead of `takeIf { it.encoded() == raw }` reddens
`refusesAPayloadThatIsNotItsOwnCanonicalEncoding`. Dropping `.withoutPadding()` reddens
`encodesToTheExactStringTheServerHandsOut` — and nothing else would, which is why that literal is
written out rather than computed.

## Acceptance criteria

- [ ] `StandingsCursorTest.encodesToTheExactStringTheServerHandsOut` passes, and the expected value
      is a literal in the test file rather than a call to `encoded()`
- [ ] `StandingsCursorTest.survivesItsOwnRoundTripIncludingANegativeStanding` passes
- [ ] `StandingsCursorTest.refusesACursorFromBeforeTheSeasonBegan` passes
- [ ] `StandingsCursorTest.refusesACursorStampedAtTheSeasonsEnd` passes, asserting both ends
- [ ] `StandingsCursorTest.refusesAPayloadThatIsNotItsOwnCanonicalEncoding` passes on all three
      inputs
- [ ] `StandingsCursorTest.refusesWhatDoesNotDecode` passes on all four inputs
- [ ] `StandingsCursor.kt` contains no `fingerprint`, no `rank`, no `limit` and no `Clock`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
