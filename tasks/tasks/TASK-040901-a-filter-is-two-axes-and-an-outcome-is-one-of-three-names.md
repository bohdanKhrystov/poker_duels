---
schema: 2
id: TASK-040901
title: A filter is two axes, and an outcome is one of exactly three names
type: task
status: ready
parent: STORY-0409
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, http, history, filters]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelFilterTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

There is a type that says which duels a history read wants — `DuelFilter` — and a parser that turns
the `outcome` query parameter into a `DuelOutcomeLabel` or refuses it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelFilter.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelFilterTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | read — the shape this file copies |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/DuelOutcomes.kt` | read — `DuelOutcomeLabel`, `outcomeOf` |

## Scope

- `DuelFilter.kt` holds **two** top-level declarations, the data class and the parser, and is named
  after the class — exactly as `DuelCursor.kt` holds `DuelCursor` and `duelCursorOrNull`. (ktlint's
  `standard:filename` only demands the name match when a file holds a *single* declaration, and this
  one holds two; naming it after the class anyway is what the neighbour does.)

  ```kotlin
  public data class DuelFilter(val outcome: DuelOutcomeLabel?, val opponent: String?) {
      public companion object {
          public val NONE: DuelFilter = DuelFilter(outcome = null, opponent = null)
      }
  }

  public fun duelOutcomeOrNull(raw: String): DuelOutcomeLabel? =
      DuelOutcomeLabel.entries.firstOrNull { it.name == raw }
  ```

- **`raw` is not nullable, and `null` means refuse** — the same contract `duelCursorOrNull` states
  in its KDoc: *"a missing query parameter means the newest page, and that default is the caller's
  business, not this function's"*. Absence is `TASK-040908`'s business; this function only judges a
  string somebody actually sent.
- **The accepted spellings are the enum's own names**, `WON`, `LOST` and `DREW` — the exact strings
  `DuelSummaryResponse.outcome` already puts on the wire, so a client can feed a duel line's own
  outcome straight back as a filter. A lower-case spelling is refused: accepting one would be a
  second rule with a second set of edge cases, and the endpoint has no other case-insensitive
  parameter.
- Match with `entries.firstOrNull { it.name == raw }`, never `DuelOutcomeLabel.valueOf(raw)` inside a
  `try`. `valueOf` throws on every rejected value, which turns the ordinary refusal path into an
  exception and puts detekt's `TooGenericExceptionCaught`/`SwallowedException` in the way for
  nothing.
- The `opponent` field exists from the start and **nothing constructs it non-null yet** —
  `TASK-040902` writes the rules for a search term and `TASK-040908` is what first fills the field.
  It is declared now so the type is not re-shaped twice; say so in its KDoc.
- KDoc on both declarations, as the public engine-facing style requires: what a `null` axis means
  (*this axis does not narrow the read*), and that `NONE` is the read the endpoint already performs.

## Out of scope

- Parsing the `opponent` parameter — `TASK-040902`.
- Turning two query parameters into one `DuelFilter?` — `TASK-040908`.
- Any SQL, any port signature, any route — `TASK-040903` onwards.
- Case-insensitive outcome names, an `ALL` value, and more than one outcome at a time. Widening the
  accepted set later is additive; narrowing it is not.

## Tests

`DuelFilterTest`

| Test | Proves |
| --- | --- |
| `everyOutcomeLabelParsesFromItsOwnName` | asserts `DuelOutcomeLabel.entries.isNotEmpty()` **first**, then that every entry `e` satisfies `duelOutcomeOrNull(e.name) == e`. The name is a universal claim, so the sweep enumerates rather than samples, and the non-empty assertion is what stops it passing over an empty set. It fails against a hand-written `when` that names two labels of the three — the specific wrong implementation this test exists to catch |
| `aLowerCaseOutcomeIsRefused` | `duelOutcomeOrNull("won")` and `duelOutcomeOrNull("Drew")` are both `null` — case-insensitive acceptance is a rule this parameter does not have, and pinning it stops one arriving by accident |
| `anOutcomeThatIsNotALabelIsRefused` | `"FOLDED"`, `"WON "` (trailing space) and `""` are all `null` — nothing is trimmed and nothing is guessed |
| `noFilterNarrowsNeitherAxis` | `DuelFilter.NONE.outcome` and `DuelFilter.NONE.opponent` are both `null`, so the constant cannot drift into meaning something |

## Acceptance criteria

- [ ] `DuelFilterTest.everyOutcomeLabelParsesFromItsOwnName` passes and asserts the entry set is
      non-empty before it iterates
- [ ] `DuelFilterTest.aLowerCaseOutcomeIsRefused` passes
- [ ] `DuelFilterTest.anOutcomeThatIsNotALabelIsRefused` passes
- [ ] `DuelFilterTest.noFilterNarrowsNeitherAxis` passes
- [ ] `duelOutcomeOrNull` takes a non-nullable `String` and contains no `try`, no `catch` and no
      `valueOf`
- [ ] `DuelFilter.kt` is the only file created under `main`, and nothing outside it and its test
      changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
