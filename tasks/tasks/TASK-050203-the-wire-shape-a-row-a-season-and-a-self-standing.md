---
schema: 2
id: TASK-050203
title: The wire shape — a row, the season it was computed for, and a self standing that is never a zero
type: task
status: done
parent: STORY-0502
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, http, leaderboard, wire]
depends_on: [TASK-050202]
verify:
  - ./gradlew :poker-server:test --tests '*StandingsDtosTest'
  - ./gradlew :poker-server:ktlintCheck
  - "! grep -qF 'asOf' poker-server/src/main/kotlin/duels/poker/server/protocol/http/StandingsDtos.kt"
  - "! grep -qiE 'tieCount|sharedRank|ladderTotal|movement|streak' poker-server/src/main/kotlin/duels/poker/server/protocol/http/StandingsDtos.kt"
  - "! grep -qE '^ *val .* = ' poker-server/src/main/kotlin/duels/poker/server/protocol/http/StandingsDtos.kt"
---

## Goal

The three types the standings endpoint answers with exist, serialise the way the wire needs, and
make `ADR-0065` §4's third answer — *no place this season* — impossible to confuse with a standing
of `0`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/StandingsDtos.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/StandingsDtosTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | read |

## Scope

Three `@Serializable` data classes, exactly these fields, in this declaration order — the order is
the JSON's order and later tickets assert literal JSON:

```kotlin
public data class StandingRow(
    val rank: Int,
    val playerId: String,
    val displayName: String?,
    val coins: Int,
)

public data class SelfStandingResponse(
    val playerId: String,
    val rank: Int?,
    val coins: Int?,
)

public data class StandingsResponse(
    val season: String,
    val rows: List<StandingRow>,
    val nextCursor: String?,
    val self: SelfStandingResponse?,
)
```

- **`SelfStandingResponse` carries an `init` that requires `rank` and `coins` to be null together**:
  `require((rank == null) == (coins == null))`. `ADR-0065` §4 — a player with a profile who finished
  no duel this season has **no rank and no standing**, and printing `0` for them states something
  false, because `0` is a real standing a draw earns (`ADR-0015`). A half-filled self standing is
  not a state this product has.
- **`season` is the wire form** `ADR-0061` §1 fixes — `"2026-08"`, which is `Season.toString()`.
  `August 2026` is `STORY-0503`'s string and does not appear here.
- **`displayName` is `String?` and `null` means never set.** No placeholder, no `No name` —
  `ADR-0029` §6 and `ADR-0058`; the client owns that word.
- **No property has a default value**, for the reason `RecentDuelsResponse`'s KDoc already gives:
  `ContentNegotiation { json() }` runs with `encodeDefaults = false`, so a defaulted `nextCursor`
  would be present in every test's JSON and absent from the real last page. A `verify:` line greps
  for a defaulted `val` declaration to keep it that way — KDoc may still spell `null` freely.
- KDoc on every property, in the style of `ProfileDtos.kt`. `self`'s KDoc states the three answers
  and which shape each takes: both numbers, both `null`, or the whole object `null`.

## Out of scope

- **An `asOf` field.** `ADR-0066` §9: `STORY-0503` gains no *as of* label and the screen's only time
  word is the season name. The cutoff travels inside the cursor and nowhere else — a `verify:` line
  greps this file for the word, **KDoc included**, so write *the walk's cutoff* if it needs saying.
- **A tie marker or a count of who shares a rank** (`ADR-0064` §5), **a ladder total, movement or a
  streak** (`ADR-0065` §7). A `verify:` line greps for those words.
- **Anything that names the order among equals** — `ADR-0064` §3–§4: the tiebreak key is never
  presented, so no field carries it.
- **A change to `ProfileResponse`** — `ADR-0065` §2. It is on the list to be **read** so this file
  matches its conventions, and it must come out of this ticket byte-identical.
- Generated TypeScript. `ProtocolTypeScript` walks the two socket message roots only; these types
  are not reachable from either and no generator changes.

## Tests

`StandingsDtosTest`, in `duels.poker.server.protocol.http`, using `protocolJson` exactly as
`ProfileDtosTest` does.

| Test | Proves |
| --- | --- |
| `aRowEncodesEveryFieldWithItsNullDisplayName` | a row with `displayName = null` encodes to the literal `{"rank":1,"playerId":"p-1","displayName":null,"coins":2}` |
| `aNegativeStandingSurvivesTheRoundTrip` | a row at `coins = -3` encodes with `"coins":-3` and decodes back equal — `ADR-0014`'s first loss is an ordinary row |
| `theResponseNamesItsSeasonInWireForm` | a response built with `Season(2026, 8).toString()` encodes `"season":"2026-08"` |
| `theLastPageCarriesNextCursorAsNullRatherThanOmittingIt` | a response with `nextCursor = null` and `self = null` encodes JSON **containing** `"nextCursor":null` and `"self":null` — the defaults trap, caught at the shape rather than at the endpoint |
| `aSelfStandingIsBothNumbersOrNeither` | `SelfStandingResponse("p-1", 3, null)` and `SelfStandingResponse("p-1", null, 2)` each throw `IllegalArgumentException`; `SelfStandingResponse("p-1", null, null)` and `SelfStandingResponse("p-1", 3, 2)` both construct |
| `aSelfStandingOfZeroIsNotTheSameAsNoPlace` | `SelfStandingResponse("p-1", 4, 0)` encodes `"coins":0` and decodes back equal, and is `!=` `SelfStandingResponse("p-1", null, null)` — the two answers `ADR-0065` §4 says an implementation collapses |

**Named mutations.** Deleting the `init` require reddens `aSelfStandingIsBothNumbersOrNeither`.
Giving `nextCursor` or `self` a `= null` default reddens
`theLastPageCarriesNextCursorAsNullRatherThanOmittingIt`. Reordering the properties of `StandingRow`
reddens `aRowEncodesEveryFieldWithItsNullDisplayName`, which is why that expectation is a literal
string.

## Acceptance criteria

- [ ] `StandingsDtosTest.aRowEncodesEveryFieldWithItsNullDisplayName` passes against a literal JSON
      string
- [ ] `StandingsDtosTest.aNegativeStandingSurvivesTheRoundTrip` passes
- [ ] `StandingsDtosTest.theResponseNamesItsSeasonInWireForm` passes
- [ ] `StandingsDtosTest.theLastPageCarriesNextCursorAsNullRatherThanOmittingIt` passes
- [ ] `StandingsDtosTest.aSelfStandingIsBothNumbersOrNeither` passes on all four inputs
- [ ] `StandingsDtosTest.aSelfStandingOfZeroIsNotTheSameAsNoPlace` passes
- [ ] `ProfileDtos.kt` is unchanged and `ProfileDtosTest` passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
