---
schema: 2
id: TASK-040106
title: The canonical form refuses the invisible and the doubled space
type: task
status: backlog
parent: STORY-0401
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, http, identity, unicode, security]
depends_on: [TASK-040105]
verify:
  - ./gradlew :poker-server:test --tests '*DisplayNameTest.twoSpacesInARowAreRefused'
  - ./gradlew :poker-server:test --tests '*DisplayNameTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`canonicalDisplayNameOrNull` refuses every character `ADR-0029` §3 names: anything in Unicode
category `Cc` or `Cf`, any whitespace that is not `U+0020`, and two consecutive spaces — each of
which produces a name that renders as another name and is then permanent.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/DisplayName.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/DisplayNameTest.kt` | modify |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §3, including what it deliberately does **not** decide |

## Scope

- Extend the existing function; do not add a second entry point. The refusals run **after** trim and
  NFC, on the canonical form, so a name is judged as it will be stored.
- Refuse any code point whose `Character.getType` is `CONTROL` or `FORMAT` — `Cc` and `Cf`. That
  covers `U+0000`–`U+001F`, `U+200B`, `U+200D`, `U+202E` and `U+FEFF` without enumerating them, and
  a category test is what keeps the next zero-width character from arriving unrefused.
- Refuse any whitespace other than `U+0020`: tab, newline, `U+00A0`, `U+2003`, and the rest.
- Refuse two or more consecutive `U+0020`. Leading and trailing are already gone by the trim.
- **This is not a script or alphabet rule.** Cyrillic, Greek and CJK names are accepted. `ADR-0029`
  §3 says so, and `ADR-0038` records that the homoglyph case was considered and not closed.

## Out of scope

- The blocklist — `STORY-0410`, which is a different question with a different answer shape.
- Any change to the trim/NFC/bound behaviour `TASK-040105` landed. The tests from that ticket keep
  their bodies and their names; this ticket only adds.

## Tests

`DisplayNameTest`, added to the existing class.

| Test | Proves |
| --- | --- |
| `aControlCharacterIsRefused` | a name carrying `U+0007` between two letters returns `null` |
| `aZeroWidthCharacterIsRefused` | each of `U+200B`, `U+200D` and `U+FEFF` inside a name returns `null` — all three asserted, not one standing for the category |
| `aBidirectionalOverrideIsRefused` | `U+202E` returns `null`; this is the spoof the `Cf` rule is for |
| `aTabOrNewlineIsRefused` | `"bo\tb"` and `"bo\nb"` both return `null` |
| `anExoticSpaceIsRefused` | `U+00A0` and `U+2003` inside a name both return `null` |
| `twoSpacesInARowAreRefused` | `"Bob  Smith"` returns `null`, while `"Bob Smith"` is accepted — the pair is the point |
| `aSingleInteriorSpaceIsKept` | `"Bob Smith"` canonicalises to itself |
| `nonLatinScriptsAreAccepted` | a Cyrillic name and a CJK name both canonicalise to themselves — the refusal is of the invisible, not of the unfamiliar |

## Acceptance criteria

- [ ] All eight tests above pass
- [ ] The refusal of zero-width characters enumerates all three code points listed, and each one is
      asserted separately
- [ ] `twoSpacesInARowAreRefused` also asserts the single-space name is accepted in the same test
      run, so the rule is shown to discriminate
- [ ] `nonLatinScriptsAreAccepted` passes — a refusal implemented as "ASCII only" fails here, which
      is why this test exists
- [ ] The nine tests from `TASK-040105` keep their bodies and names, and no assertion in them is
      weakened
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
