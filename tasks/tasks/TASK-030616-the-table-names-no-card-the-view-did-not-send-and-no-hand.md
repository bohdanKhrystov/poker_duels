---
schema: 2
id: TASK-030616
title: The table names no card the view did not send, and no hand
type: task
status: ready
parent: STORY-0306
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, duel, testing, secrecy, authority]
depends_on: [TASK-030615]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +189 passed \(189\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names exactly the cards the view carries and no others'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts no rank and no suit on a hand the view did not send'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names no hand and declares no winner'
  - cd web-client && npm run check
---

## Goal

The other half of `TASK-030615`'s claim: the table names **exactly** the eight card-shaped things
the view accounts for and no ninth, puts no rank or suit anywhere near a hand it was not sent, and
never says which hand anyone has or who is winning.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/no-derivation.test.tsx` | modify — append three `it` blocks and one constant |
| `web-client/src/table/DuelTable.tsx` | read — what is under test |

## Scope

- No production file changes. This ticket appends to `TASK-030615`'s file and reuses its `VIEW`
  fixture and its `wordsOnScreen` helper unchanged. `TASK-030615`'s two `it` blocks are not edited.
- One constant, above the describe block:

  ```tsx
  const HAND_TALK =
    /\b(pair|trips|set|straight|flush|full house|quads|high card|wins?|won|loses?|loser|winner|beats)\b/i;
  ```

  It is a closed vocabulary on purpose: these are the words a client would have to have decided
  something to write. Nothing the table renders today is in it — `committed`, `Folded`, `All in`,
  `Your turn`, `Preflop`…`River` and the card names are all clear of it.
- The card assertion is an **exact list, in document order**, not a set of `getBy` calls: a set of
  presence checks cannot see a ninth card appearing, and a ninth card is the failure mode.

## Out of scope

- Loosening `HAND_TALK` when `STORY-0308` legitimately needs to name a hand. That screen is a
  different component; this guard is over `DuelTable` and stays strict.
- Any component change.

## Tests

`web-client/src/table/no-derivation.test.tsx`, describe block
`"the table renders and never derives"`. Three `it` blocks appended after `TASK-030615`'s two.

| Test | Proves |
| --- | --- |
| `names exactly the cards the view carries and no others` | `screen.getAllByRole("img").map((card) => card.getAttribute("aria-label"))` equals, in order: `your rival's hidden hand`, `ace of spades`, `seven of diamonds`, `two of clubs`, `turn card, not yet dealt`, `river card, not yet dealt`, `ace of hearts`, `king of spades` — **and no element outside a `role="img"` carries a card name in an `aria-label` or a `title`.** That list only reads card elements, so `aria-label="queen of clubs"` on anything else ships `Tests 189 passed (189)` without the second check, and a screen reader would say it |
| `puts no rank and no suit on a hand the view did not send` | the element named `your rival's hidden hand` has a parent row whose `textContent` is `""` and whose `innerHTML` matches no `aria-label="` other than the rival's own |
| `names no hand and declares no winner` | **neither** `wordsOnScreen(container)` **nor** `spokenOnScreen(container)` matches `HAND_TALK`. Text alone is not enough: `"Two pair"` in an `aria-label` and `"You win"` in a `title` each shipped `Tests 189 passed (189)` against a text-only scan. Both are spoken to a player — one read aloud, one on hover — and neither is a text node |

Three tests. One hundred and eighty-six exist, so the suite reports **189**, which is the story's
total: fifty-eight tests over the hundred and thirty-one that existed at `4936f0f`.

Eight labels, and only eight: one hidden hand for the rival (its partner is `aria-hidden`), three
dealt board cards, two undealt board places, and your two.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 189 passed (189)` | the three ran and the hundred-and-eighty-six before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red.** Both claims here are universal — *no card the view
did not send*, *no hand named, no winner declared* — so each is broken in several different shapes.
All six were run against this exact test file, and each was reverted afterwards:

1. **A back that knows its card** — in `Hand.tsx`, `` label={place === 0 ?
   `${props.hiddenLabel}: ace of clubs` : null} `` → `names exactly the cards the view carries and
   no others` fails with `expected [ …(8) ] to deeply equal [ 'your rival\'s hidden hand', …(7) ]`,
   and `puts no rank and no suit on a hand the view did not send` with `Unable to find an accessible
   element with the role "img" and name "your rival's hidden hand"`.
2. **An undealt place drawn as a card** — in `BoardCards.tsx`, replace the `CardSlot` branch with
   `<CardFace key={place} card="Ac" />` → `names exactly the cards the view carries and no others`
   fails with `expected [ 'your rival\'s hidden hand', …(7) ] to deeply equal [ 'your rival\'s
   hidden hand', …(7) ]` — same length, different labels.
3. **A ninth card element** — in `Hand.tsx`, return an extra `<span role="img" aria-label="king of
   clubs" />` for the second hidden place → the same test fails with `expected [ 'your rival\'s
   hidden hand', …(8) ] to deeply equal [ …(7) ]`, and `puts no rank and no suit on a hand the view
   did not send` with `expected '<span role="img" aria-label="your riv…' not to match
   /aria-label="(?!your rival)/`.
4. **A hand name in the pot meta** — in `PotStrip.tsx`, append `· Two pair, aces and sevens` →
   `names no hand and declares no winner` fails with `expected 'Your rival D 3,750 committed   400
   Po…' not to match /\b(pair|trips|set|straight|flush|ful…/i`.
5. **A winner banner, with or without an amount** — in `DuelTable.tsx`, add `<p>You win</p>` beside
   the pot, and separately `<p>You win 4,850</p>` → the same test fails both times.
   **Both were run.** The bare form is the one that matters: before `wordsOnScreen` joined text
   nodes with a space, `<p>You win</p>` sat directly beside the board's `A♠︎` in `textContent`, the
   `\b` after `win` did not exist, and the guard passed. The amount-carrying form was caught either
   way. This is why the helper is not `container.textContent`.
6. **A loser line** — in `DuelTable.tsx`, add `<p>loser</p>` in the rival's block → the same test
   fails with `expected 'Your rival D 3,750 committed   400 lo…' not to match /…/i`.

Quote all six in the PR.

## Acceptance criteria

- [ ] `the table renders and never derives > names exactly the cards the view carries and no others` passes
- [ ] `the table renders and never derives > puts no rank and no suit on a hand the view did not send` passes
- [ ] `the table renders and never derives > names no hand and declares no winner` passes
- [ ] `TASK-030615`'s two `it` blocks and all three of its helpers are unedited
- [ ] No file outside `web-client/src/table/no-derivation.test.tsx` is changed
- [ ] `npm run --silent test` reports `Tests  189 passed (189)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
