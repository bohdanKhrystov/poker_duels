---
schema: 2
id: TASK-030618
title: The suit glyphs are asserted by codepoint, not by a matching literal
type: task
status: backlog
parent: STORY-0306
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, duel, ui, test]
depends_on: [TASK-030617]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +190 passed \(190\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'draws each suit in text presentation, not as an emoji'
  - cd web-client && npm run check
---

## Goal

Close the one hole `TASK-030602` left open: every assertion about a suit glyph today compares the
module's string to a **string literal typed in a test file**, so a variation selector dropped from
source and test together is invisible.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/card-text.test.ts` | modify — append one `it` to the existing `describe("a card string")` |

## Scope

- `TASK-030602` writes each suit as its character followed by `U+FE0E` (VARIATION SELECTOR-15).
  Without it, some platforms render an emoji playing-card suit instead of a text glyph. That ticket
  said so, and instructed *"copy them from this ticket; do not retype them"* — because retyping is
  the way the selector gets lost.

- **The existing assertions cannot detect that.** `expect(cardText("As")).toEqual({ suit: "♠︎", … })`
  compares a literal in `card-text.ts` against a literal in `card-text.test.ts`. A coder who retypes
  the glyph retypes it in both places, and the two agree. Verified on `develop`: stripping `U+FE0E`
  from `card-text.ts`, `card-text.test.ts` and `PlayingCard.test.tsx` together ships
  `Tests 167 passed (167)`.

- The fix is to assert in a **different representation**, one a typo cannot reproduce — the
  codepoints themselves:

  ```ts
  it("draws each suit in text presentation, not as an emoji", () => {
    // Deliberately not compared against a glyph literal. Every other assertion
    // in this file compares one literal to another, so a variation selector
    // dropped from the source and the expectation together is invisible to all
    // of them — the two have a common cause. Codepoints do not.
    const VARIATION_SELECTOR_15 = 0xfe0e;
    const SUIT_CHARACTERS: Record<string, number> = {
      s: 0x2660,
      h: 0x2665,
      d: 0x2666,
      c: 0x2663,
    };

    for (const [suit, character] of Object.entries(SUIT_CHARACTERS)) {
      const glyph = cardText(`A${suit}`)?.suit ?? "";
      expect([...glyph].map((c) => c.codePointAt(0))).toEqual([
        character,
        VARIATION_SELECTOR_15,
      ]);
    }
  });
  ```

- **All four suits, not one.** The test's name is a universal claim, and one suit cannot stand for
  four — this story has shipped that mistake five times.

## Out of scope

- Editing `card-text.ts`. Its glyphs are correct today, byte-verified; this ticket adds the guard
  that keeps them correct. If the assertion fails on first run, that is a finding — report it rather
  than editing the source to match.
- The existing four `it` blocks in `describe("a card string")`. They stay byte-identical.
- `PlayingCard.test.tsx`. Its two glyph literals stay as they are: with this guard in place, a
  selector dropped from `card-text.ts` reddens here regardless of what the other files say.

## Tests

One `it`, appended to the existing `describe("a card string")` in
`web-client/src/table/card-text.test.ts`.

| Test | Proves |
| --- | --- |
| `draws each suit in text presentation, not as an emoji` | for each of `s`, `h`, `d`, `c`, the glyph's codepoints are exactly `[suit character, 0xFE0E]` |

One test. One hundred and eighty-nine exist, so the suite reports **190**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 190 passed (190)` | the test ran and the hundred-and-eighty-nine before it still do |
| the `--reporter=verbose` grep | it exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes the assertion red:**

1. Strip `U+FE0E` from **all three** files that carry a glyph literal — `card-text.ts`,
   `card-text.test.ts` and `PlayingCard.test.tsx`. Before this ticket that shipped
   `Tests 167 passed (167)`; after it, this test fails with `expected [ 9824 ] to deeply equal
   [ 9824, 65038 ]`. Revert. **This is the whole point of the ticket** — quote both numbers in the PR.
2. Change `d: 0x2666` to `d: 0x2665` in the test's own table → fails with
   `expected [ 9830, 65038 ] to deeply equal [ 9829, 65038 ]`, proving the table is read and not
   ignored. Revert.

## Acceptance criteria

- [ ] `a card string > draws each suit in text presentation, not as an emoji` passes
- [ ] The assertion compares **numbers**, and no glyph literal appears in the new `it` block
- [ ] All four suits are covered
- [ ] `web-client/src/table/card-text.ts` is byte-identical to `develop`
- [ ] `npm run --silent test` reports `Tests  190 passed (190)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
