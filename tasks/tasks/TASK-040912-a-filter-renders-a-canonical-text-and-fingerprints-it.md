---
schema: 2
id: TASK-040912
title: A filter renders one canonical line per axis, and fingerprints to eleven characters
type: task
status: done
parent: STORY-0409
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, http, history, filters, cursor]
depends_on: [TASK-040911]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelFilterTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A `DuelFilter` can say which set it names, as a canonical text and as an eleven-character
fingerprint of that text — so `TASK-040913` has something to bind a cursor to.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelFilter.kt` | modify — two `internal` functions below `duelFilterOrNull` |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelFilterTest.kt` | modify — eight new tests appended |
| `docs/adr/ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md` | read — §2, §3 and §8 only |

## Scope

- **`internal fun DuelFilter.canonicalText(): String`** — one line per axis that actually narrows
  the read, in the fixed order `outcome` then `opponent`, each line

  ```
  "<axis>:<utf8ByteLength>:<value>\n"
  ```

  The outcome's value is `outcome.name` (`WON`, `LOST`, `DREW`). An axis whose value is `null`
  contributes **nothing at all** — not an empty segment, not a placeholder — so `DuelFilter.NONE`
  renders the empty string. `DuelFilter(WON, "Halvard")` renders
  `"outcome:3:WON\nopponent:7:Halvard\n"`.
- **The length is the UTF-8 byte count of the value** — `value.toByteArray(Charsets.UTF_8).size` —
  never `String.length` and never `codePointCount`. The count exists so the byte count consumes the
  value exactly and no value can fake a line boundary; a count in some other unit does not do that.
  (This is the one place in this file that deliberately counts bytes: `opponentSearchOrNull`
  correctly counts *code points*, because it is bounding a name against a column, not delimiting a
  digest input. Both are right; they answer different questions.)
- **`internal fun DuelFilter.fingerprint(): String`** — the first 8 bytes of the SHA-256 of the
  canonical text, as unpadded URL-safe base64, which is always eleven characters:

  ```kotlin
  Base64.getUrlEncoder().withoutPadding().encodeToString(
      MessageDigest.getInstance("SHA-256")
          .digest(canonicalText().toByteArray(Charsets.UTF_8))
          .copyOf(8),
  )
  ```

- **Both functions are `internal`, and both live in `DuelFilter.kt`.** Nothing outside
  `poker-server` may depend on the hash's shape (`ADR-0057` §3): it travels on the wire only inside
  an opaque string, and a client that parses it out has left the contract. Do not make either
  `public`, and do not move either into `DuelCursor.kt` — §4 keeps `DuelCursor` from ever learning
  what an axis is, so that adding an axis touches this file and nothing else.
- **Imports**: `java.security.MessageDigest` and `java.util.Base64` join the existing
  `java.text.Normalizer` in this file's trailing `java.*` group, alphabetically
  (`java.security` → `java.text` → `java.util`). This repo's ktlint puts `java.*`, `javax.*` and
  `kotlin.*` imports **at the end**, which is what the file already does.
- **The KDoc explains why, not what.** The three rules of `ADR-0057` §8 bind every future change to
  `DuelFilter` and belong here, where the next person to add an axis will read them: a new axis is
  **appended** to the order and never inserted, an existing axis is never renamed, and an absent
  axis contributes nothing — which together are what make every cursor already in flight stay
  byte-identical when a third axis lands. Say also that the fingerprint is a **consistency check and
  never an authorisation check** (§7): it is unkeyed, anybody who knows the scheme can mint one, and
  the read is safe only because it is keyed off the player the server resolved.

## Out of scope

- `DuelCursor.kt`, the three-part payload, and both functions gaining a `DuelFilter` parameter —
  `TASK-040913`. Nothing calls `fingerprint()` when this ticket merges, which is expected: it is
  `internal` and tested, not dead-in-the-sense-that-matters.
- Any change to `DuelFilter`, `duelFilterOrNull`, `duelOutcomeOrNull` or `opponentSearchOrNull`.
  The canonical form is the **parsed** value precisely because those four already canonicalise, so
  none of them needs to do anything new.
- A third filter axis, and any change to `docs/protocol.md`.

## Tests

`DuelFilterTest` — appended. The existing tests in this file observe neither new function and none
of them changes.

| Test | Proves |
| --- | --- |
| `theEmptyFilterRendersNoLines` | `DuelFilter.NONE.canonicalText()` is exactly `""`. Fails against the natural implementation that renders a placeholder or an empty segment for an absent axis |
| `eachNarrowingAxisRendersOneLengthDelimitedLine` | `DuelFilter(DuelOutcomeLabel.WON, "Halvard").canonicalText()` is exactly `"outcome:3:WON\nopponent:7:Halvard\n"`. Fails against the reversed axis order, a missing trailing newline, a missing count, and any separator other than `:` |
| `anAbsentAxisContributesNothingAtAll` | `DuelFilter(DuelOutcomeLabel.WON, null).canonicalText()` is `"outcome:3:WON\n"` and `DuelFilter(null, "Halvard").canonicalText()` is `"opponent:7:Halvard\n"`. This is the assertion `ADR-0057` §8 rests on: it fails against `"outcome:3:WON\nopponent:0:\n"`, which is the natural thing to write and the thing that would invalidate every cursor in flight the day a third axis is added |
| `theLengthIsTheValuesUtf8ByteCount` | `DuelFilter(null, "H\u00e5kon").canonicalText()` is `"opponent:6:H\u00e5kon\n"` — six, not five. Write the `å` as the escape `\u00e5` on both sides, not as a literal character: a source file saved in NFD spells that character `a` + `U+030A` (combining ring above), which is two code points and three bytes rather than one and two, making the whole term seven bytes and the expectation wrong for a reason nobody would think to look for. `H\u00e5kon` is five characters, five code points and **six** UTF-8 bytes, so this one fixture fails against `String.length` and against `codePointCount` alike, both of which answer five |
| `theEmptyFilterFingerprintsToTheDigestOfNothing` | `DuelFilter.NONE.fingerprint()` is exactly `"47DEQpj8HBQ"` — the first 8 bytes of SHA-256 over no input. Fails against a different digest, a different truncation width, hex, or a padded alphabet |
| `aFilterFingerprintsToItsGoldenVector` | `DuelFilter(DuelOutcomeLabel.WON, "Halvard").fingerprint()` is exactly `"J8qDx13CI_o"`. This is §8.3's golden vector at the filter level, and it is the **only** fixture here that would fail if the rendering changed but stayed self-consistent — every other expectation is written out by hand precisely so that it cannot be regenerated from a changed implementation |
| `filtersDifferingInOneAxisFingerprintDifferently` | `DuelFilter(WON, "Halvard")`, `DuelFilter(LOST, "Halvard")` and `DuelFilter(WON, "Halvar")` have three pairwise-distinct fingerprints. Fails against an implementation that hashes only one axis, or that hashes the axis names without the values |
| `aFingerprintIsElevenCharactersAndUnpadded` | both fingerprints above have `length == 11` and contain no `=`. `TASK-040913`'s padding arithmetic depends on exactly eleven: a padded encoder answers twelve and would silently make `theEncodedFormCarriesNoPadding` in `DuelCursorTest` vacuous |

The two golden literals were computed from `ADR-0057` §2's rendering before this ticket was
written, not read off an implementation. `"47DEQpj8HBQ"` is also stated in the ADR itself, so the
two agree independently.

## Acceptance criteria

- [ ] `DuelFilterTest.theEmptyFilterRendersNoLines` passes
- [ ] `DuelFilterTest.eachNarrowingAxisRendersOneLengthDelimitedLine` passes
- [ ] `DuelFilterTest.anAbsentAxisContributesNothingAtAll` passes
- [ ] `DuelFilterTest.theLengthIsTheValuesUtf8ByteCount` passes
- [ ] `DuelFilterTest.theEmptyFilterFingerprintsToTheDigestOfNothing` passes
- [ ] `DuelFilterTest.aFilterFingerprintsToItsGoldenVector` passes
- [ ] `DuelFilterTest.filtersDifferingInOneAxisFingerprintDifferently` passes
- [ ] `DuelFilterTest.aFingerprintIsElevenCharactersAndUnpadded` passes
- [ ] All fifteen tests already in `DuelFilterTest` still pass unchanged — this ticket adds two
      functions and edits none of them, so nothing already in that file observes a different value
- [ ] `canonicalText` and `fingerprint` are both declared `internal`, and neither `public` nor
      `private`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
