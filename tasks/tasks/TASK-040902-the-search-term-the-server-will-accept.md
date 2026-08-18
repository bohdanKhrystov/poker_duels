---
schema: 2
id: TASK-040902
title: The search term the server will accept, counted in code points
type: task
status: done
parent: STORY-0409
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, http, history, filters, search]
depends_on: [TASK-040901]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelFilterTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`opponentSearchOrNull` decides which strings are a usable opponent search term and refuses the rest,
before any of them reaches SQL.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelFilter.kt` | modify — one function, one constant |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelFilterTest.kt` | modify — five tests added |
| `poker-server/src/main/kotlin/duels/poker/server/http/DisplayName.kt` | read — `canonicalDisplayNameOrNull`, the shape and the 32 |
| `poker-server/src/main/resources/db/migration/V3__player_display_name.sql` | read — the column's own `CHECK`s |

## Scope

```kotlin
private const val MAX_OPPONENT_SEARCH_CODE_POINTS = 32

public fun opponentSearchOrNull(raw: String): String? {
    val normalised = Normalizer.normalize(raw, Normalizer.Form.NFC)
    if (normalised.isBlank()) return null
    if (normalised.codePointCount(0, normalised.length) > MAX_OPPONENT_SEARCH_CODE_POINTS) return null
    return normalised
}
```

Four decisions, each of which the KDoc must state as a *reason*, not as a restatement of the code:

- **NFC, because the column is NFC.** `V3__player_display_name.sql` enforces
  `CHECK (display_name IS NFC NORMALIZED)` and `canonicalDisplayNameOrNull` normalises before
  storing, so a term typed on a keyboard that produces NFD would match a name it is character-for-
  character equal to — and silently return nothing. Same reason `ADR-0048` §1 normalises before
  hashing: compare like with like.
- **Blank is refused, not treated as absent.** `respondWithDuels`'s existing comment says it for
  `after`: *"that would turn a broken cursor into a silent first page, the one behaviour this
  endpoint exists to refuse."* `opponent=` is a client that meant to send something. `isBlank()`
  covers `""` and a run of spaces, and a single space is therefore refused too.
- **Nothing is trimmed.** A stored name may hold an interior `U+0020` (`ADR-0029` §3 permits one,
  refusing only doubled spaces), so ` Hal` is a real substring of `Big Hal` and trimming would make
  it unfindable. This is the one place this parser deliberately does *less* than
  `canonicalDisplayNameOrNull`.
- **32 code points, because a name is at most 32 code points.** `player_display_name_length` is
  `CHECK (char_length(display_name) BETWEEN 1 AND 32)`, and PostgreSQL's `char_length` counts
  characters, so a longer term can match nothing and is refused rather than sent to the database.
  Counted with `codePointCount`, never `String.length` — `PasswordPolicy.kt` states the reason and
  this file repeats it in one line, not a paragraph.
- The `Normalizer` import belongs in the `java.*` group **at the end** of the import list, after
  `duels.poker.server.protocol.http.DuelOutcomeLabel`. This repository's ktlint puts `java.*`,
  `javax.*` and `kotlin.*` last.

## Out of scope

- Refusing control, format or doubled-space characters the way `canonicalDisplayNameOrNull` does. A
  term holding one simply matches nothing, which is the honest answer; a second refusal rule would
  have to be documented and tested for no behavioural gain.
- A minimum length above 1. One character is a legitimate search over a short history.
- Any SQL — `TASK-040904`. The term is a string here and nothing more.
- Combining the two parameters — `TASK-040908`.

## Tests

`DuelFilterTest`, added below the outcome tests `TASK-040901` wrote. Every one of the existing tests
in the file keeps its assertions.

| Test | Proves |
| --- | --- |
| `aTermIsReturnedInItsNfcForm` | **Write both strings as Kotlin escapes, never as literal characters** — an editor, a copy-paste or this ticket's own rendering will silently normalise them and leave a test that asserts a string equals itself. The argument is `"e\u0301lodie"` (7 code points: `e` followed by combining acute) and the result must equal `"\u00e9lodie"` (6 code points). The test asserts **both** that the result equals the composed form **and** that it is not equal to the argument, so it cannot pass on a function that returns its input unchanged, and it fails loudly if the two literals ever collapse into one |
| `aBlankTermIsRefused` | `""` and `"   "` are both `null`. Present and empty is a refusal, not an absent parameter |
| `aTermOfThirtyTwoCodePointsIsAcceptedAndThirtyThreeIsNot` | `"a".repeat(32)` comes back, `"a".repeat(33)` is `null` — the boundary pair. One of the two alone cannot tell `>` from `>=` |
| `theLimitCountsCodePointsNotUtf16Units` | `"\uD834\uDD1E".repeat(32)` — written as the surrogate escapes, one astral code point per repeat, so **32 code points and 64 UTF-16 units** — is **accepted**, and `"\uD834\uDD1E".repeat(33)` is refused. This is the falsifying pair against a `String.length` bound, which refuses the first |
| `aTermKeepsTheSpacesItWasGiven` | `opponentSearchOrNull(" Hal")` returns `" Hal"` with its leading space intact — it fails against any implementation that calls `trim()`, which is the specific wrong implementation this test exists to catch |

## Acceptance criteria

- [ ] `DuelFilterTest.aTermIsReturnedInItsNfcForm` passes and asserts the result differs from the
      argument as well as equalling the composed form
- [ ] `DuelFilterTest.aBlankTermIsRefused` passes for both `""` and a run of spaces
- [ ] `DuelFilterTest.aTermOfThirtyTwoCodePointsIsAcceptedAndThirtyThreeIsNot` passes and asserts
      both sides of the boundary
- [ ] `DuelFilterTest.theLimitCountsCodePointsNotUtf16Units` passes, with the 32-code-point astral
      string **accepted**
- [ ] `DuelFilterTest.aTermKeepsTheSpacesItWasGiven` passes
- [ ] `opponentSearchOrNull` contains no call to `trim`, `lowercase` or `uppercase` — folding is the
      database's job in `TASK-040904`, under the collation `ADR-0029` §1 pins
- [ ] Every test `TASK-040901` added to `DuelFilterTest` passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
