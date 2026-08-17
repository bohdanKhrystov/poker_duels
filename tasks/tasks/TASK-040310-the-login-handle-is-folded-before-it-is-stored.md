---
schema: 2
id: TASK-040310
title: The login handle is folded before it is stored, and the fold is ASCII
type: task
status: backlog
parent: STORY-0403
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, identity]
depends_on: [TASK-040309]
verify:
  - ./gradlew :poker-server:test --tests '*LoginHandleTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`loginHandleOrNull` turns what a player typed into the one form that is ever stored — lowercase
ASCII, 3 to 32 characters of `[a-z0-9._-]` beginning with `[a-z0-9]` — or returns `null`, so
`Bob` and `bob` are one account and nothing else is.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/LoginHandle.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/LoginHandleTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/DisplayName.kt` | read — `canonicalDisplayNameOrNull` is the shape and the KDoc style this follows, and the deliberate contrast |
| `docs/adr/ADR-0031-an-optional-verified-recovery-email.md` | read — §1, the rule and why it lives in the write path rather than in a `CHECK` |

## Scope

- `public fun loginHandleOrNull(raw: String): String?` in `duels.poker.server.auth`.
- **The fold is ASCII and only ASCII**: map `A`–`Z` to `a`–`z` by code point and leave every other
  character exactly as it was. Do **not** call `String.lowercase()`, and do not call it with a
  locale either:
  - the no-argument form is locale-sensitive, so `"I"` folds to `ı` on a Turkish-locale host and to
    `i` everywhere else — the same host-dependent class of bug `ADR-0029` §1 pinned a collation to
    avoid;
  - `Locale.ROOT` fixes that and introduces a worse one: `U+212A KELVIN SIGN` folds to `k`, so a
    handle nobody can type on a keyboard becomes a handle that collides with one somebody owns.
- Order: fold, then length in characters (3–32 — every accepted character is ASCII, so characters,
  code points and UTF-16 units are the same count here and the KDoc says so), then the permitted
  set, then the first character.
- **Nothing is trimmed.** `ADR-0031` §1 enumerates the permitted characters and space is not among
  them, so `" bob"` is refused rather than repaired. A fold that silently repairs input is a second
  rule that the sign-in path would have to reproduce byte for byte or accounts would go missing.
- KDoc saying what the contrast with the display name is: a display name is shown, so what the
  player typed has to survive and the fold lives in a database index; a handle is never shown to
  anybody, so only its canonical form needs to exist.

## Out of scope

- Any endpoint that calls this — `STORY-0404` and `STORY-0405`.
- Any `CHECK` constraint. `ADR-0031` §1 is explicit that the rule lives in the write path, because
  `credential` is generic across kinds and a character rule is the part most likely to move.
- A rule about what a *password* may be. That is
  [`ADR-0048`](../../docs/adr/ADR-0048-a-password-has-one-rule-and-it-is-length.md), enforced at
  sign-up — it adds nothing to the handle and nothing to this ticket.
- Changing a handle after sign-up. `ADR-0031` §1: no endpoint updates `credential.identifier`.

## Tests

`LoginHandleTest`. Test bodies are block bodies with **no** explicit `: Unit`.

| Test | Proves |
| --- | --- |
| `anAlreadyFoldedHandleIsReturnedUnchanged` | `"bob.the-builder_7"` comes back identical |
| `uppercaseAsciiIsFolded` | `"Bob"` → `"bob"`, and `"BIG"` → `"big"` — the second pins that `I` folds to ASCII `i` on any host locale |
| `theKelvinSignIsRefusedRatherThanFoldedToK` | `"boK"` returns `null`, not `"bok"` — the fold is ASCII |
| `everyPermittedCharacterIsAccepted` | loops over all 39 of `a`–`z`, `0`–`9`, `.`, `_`, `-`, asserting `"ab$c"` comes back unchanged for each |
| `everyCharacterOutsideTheSetIsRefused` | a named list — space, `@`, `+`, `/`, `!`, `é`, `\t`, `\n`, `​` — each refused when placed mid-handle, asserted as a collected list of the ones that got through |
| `theLengthBoundsAreThreeAndThirtyTwo` | all four cases: 2 refused, 3 accepted, 32 accepted, 33 refused |
| `aHandleMustStartWithALetterOrDigit` | all four cases: leading `.`, `_` and `-` refused, leading digit accepted |
| `aHandleWithSurroundingSpacesIsRefusedRatherThanTrimmed` | `" bob "` returns `null`, and the test asserts it is not `"bob"` |
| `theEmptyStringIsRefused` | `""` returns `null` |

## Acceptance criteria

- [ ] All nine tests above pass
- [ ] `everyPermittedCharacterIsAccepted` enumerates the whole set of 39 characters in a loop — a
      test naming three of them proves nothing about the other thirty-six
- [ ] `theLengthBoundsAreThreeAndThirtyTwo` asserts both the accepted and the refused side of each
      boundary; two of its four cases are the accepted ones
- [ ] `aHandleMustStartWithALetterOrDigit` covers all three refused leading characters
- [ ] `theKelvinSignIsRefusedRatherThanFoldedToK` passes, and `LoginHandle.kt` contains no call to
      `lowercase(`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
