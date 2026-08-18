---
schema: 2
id: TASK-040908
title: Two query parameters become one filter, or one refusal
type: task
status: done
parent: STORY-0409
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, http, history, filters]
depends_on: [TASK-040907]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelFilterTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

One function turns the raw `outcome` and `opponent` query parameters into a `DuelFilter`, or into a
single refusal — so the route has one thing to check instead of two.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelFilter.kt` | modify — one function |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelFilterTest.kt` | modify — five tests added |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | read — how `respondWithDuels` handles an absent versus an unparseable `after` |

## Scope

```kotlin
public fun duelFilterOrNull(outcome: String?, opponent: String?): DuelFilter? {
    val parsedOutcome = outcome?.let { duelOutcomeOrNull(it) ?: return null }
    val parsedOpponent = opponent?.let { opponentSearchOrNull(it) ?: return null }
    return DuelFilter(parsedOutcome, parsedOpponent)
}
```

- **This is where absence and refusal finally part company**, and it is the only function in the
  file whose arguments are nullable. `null` **argument** means the parameter was not in the query
  string and that axis does not narrow the read; `null` **return** means a parameter was there and
  the server will not act on it. `respondWithDuels` already draws that line for `after` and its
  comment says why: *"No `takeIf { it.isNotBlank() }` here: that would turn a broken cursor into a
  silent first page, the one behaviour this endpoint exists to refuse."* A present-but-unusable
  `outcome` or `opponent` gets the same treatment.
- The `return null` inside `let` is a non-local return, which compiles because `let` is inline. It
  is deliberate: one bad axis refuses the whole filter, rather than being quietly dropped while the
  other axis narrows. A client that sent `?outcome=won&opponent=Halvard` and received a page
  filtered only by name would have no way to tell.
- KDoc must say all of the above in two or three sentences, and say that both arguments come
  straight from `request.queryParameters[...]` with nothing done to them first.

## Out of scope

- The route — `TASK-040909`. Nothing here touches `ProfileRoutes.kt`.
- Any third axis. Adding one later is one more `let` and one more field.
- Changing `duelOutcomeOrNull` or `opponentSearchOrNull`. Both keep non-nullable parameters; this
  function is the only place that knows what an absent parameter means.

## Tests

`DuelFilterTest`, added below the tests `TASK-040901` and `TASK-040902` wrote, all of which keep
their assertions.

| Test | Proves |
| --- | --- |
| `bothParametersAbsentIsNoFilter` | `duelFilterOrNull(null, null) == DuelFilter.NONE` — the request the endpoint already serves |
| `bothParametersPresentNarrowBothAxes` | `duelFilterOrNull("LOST", "Halvard") == DuelFilter(DuelOutcomeLabel.LOST, "Halvard")`. Two axes at once, so a function that overwrote one with the other, or dropped the second, fails |
| `oneAxisAloneLeavesTheOtherNull` | `duelFilterOrNull("WON", null)` gives `DuelFilter(WON, null)` **and** `duelFilterOrNull(null, "Halvard")` gives `DuelFilter(null, "Halvard")` — both directions, because one alone cannot tell a copied field from a correct one |
| `anUnusableOutcomeRefusesTheWholeFilter` | `duelFilterOrNull("won", "Halvard")` is `null` — a lower-case outcome is refused even though the opponent term is perfectly good. Fails against an implementation that drops the bad axis and returns a name-only filter |
| `anUnusableOpponentRefusesTheWholeFilter` | `duelFilterOrNull("WON", "")` is `null`, and so is `duelFilterOrNull(null, " ")` — present and blank is a refusal, not an absent parameter, whether or not the other axis is good |

## Acceptance criteria

- [ ] `DuelFilterTest.bothParametersAbsentIsNoFilter` passes
- [ ] `DuelFilterTest.bothParametersPresentNarrowBothAxes` passes
- [ ] `DuelFilterTest.oneAxisAloneLeavesTheOtherNull` passes and asserts both directions
- [ ] `DuelFilterTest.anUnusableOutcomeRefusesTheWholeFilter` passes with a **valid** opponent term
      alongside the invalid outcome
- [ ] `DuelFilterTest.anUnusableOpponentRefusesTheWholeFilter` passes with a **valid** outcome
      alongside the invalid opponent
- [ ] Every test already in `DuelFilterTest` passes with its assertions unchanged
- [ ] `ProfileRoutes.kt` is not in the diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
