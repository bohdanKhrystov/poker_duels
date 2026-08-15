---
schema: 2
id: TASK-030606
title: A hand is two places wide, whatever the view carries
type: task
status: done
parent: STORY-0306
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, duel, ui, secrecy]
depends_on: [TASK-030605]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +154 passed \(154\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'draws two backs when the view carries no card'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is two places wide whatever the view carries'
  - cd web-client && npm run check
---

## Goal

The story's third design note, made structural: **an empty `holeCards` renders as card backs, never
as a gap** — so a hand occupies two places whether the server showed it or not, and absence tells
the player nothing.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/Hand.tsx` | create |
| `web-client/src/table/Hand.test.tsx` | create |
| `web-client/src/table/PlayingCard.tsx` | read — `CardFace`, `CardBack` |

## Scope

- The whole file, verbatim:

  ```tsx
  import type { ReactElement } from "react";
  import { CardBack, CardFace } from "./PlayingCard";

  /**
   * A seat's two cards, always two places wide.
   *
   * A place the view carries a card for is drawn face up; a place it does not is
   * drawn face down. An empty `holeCards` means "not entitled to see" — never "no
   * cards" — so a hand is never a gap and never narrows. Whether a seat folded is
   * `hasFolded`'s to say, and whether it is all in is `isAllIn`'s.
   */
  export function Hand(props: {
    cards: readonly string[];
    hiddenLabel: string;
  }): ReactElement {
    return (
      <>
        {[0, 1].map((place) => {
          const card = props.cards.at(place);
          return card === undefined ? (
            <CardBack key={place} label={place === 0 ? props.hiddenLabel : null} />
          ) : (
            <CardFace key={place} card={card} />
          );
        })}
      </>
    );
  }
  ```

- **`props.cards.at(place)`, not `props.cards[place]`.** `noUncheckedIndexedAccess` is off in
  `tsconfig.json`, so the index form types as `string` and `card === undefined` becomes a `tsc`
  error (TS2367). `.at()` returns `string | undefined` and is available at `target: ES2022`.
- The iteration is over `[0, 1]` — the *places* — and never over `props.cards`. That is the whole
  ticket: mapping the array is what turns an empty hand into an empty row.
- Only the first hidden place is named; the second is `aria-hidden`, as
  `duel-table.html`'s `.oppcards` does.
- `Hand` returns a fragment and sets no width, gap or `--w`. Its caller owns the row, because the
  hero's row and the rival's row are different widths in the design (96px and 40px).

## Out of scope

- Any use of the component. `TASK-030613` puts two of these on the table.
- Any reading of `hasFolded` or `isAllIn`. A hand renders from `holeCards` and nothing else; the
  seat's state is the seat plate's to show (`TASK-030610`).
- A hand of more than two cards. Hold'em deals two, and a third element would be a client asserting
  a rule.

## Tests

`web-client/src/table/Hand.test.tsx`, describe block `"a hand"`.

| Test | Proves |
| --- | --- |
| `draws two faces when the view carries two cards` | `["Ah", "Ks"]` renders `ace of hearts` and `king of spades`, and `queryByRole("img", { name: "your rival's hidden hand" })` is `null` |
| `draws two backs when the view carries no card` | `[]` renders one element named `your rival's hidden hand`, `container.childElementCount` is `2`, and `container.textContent` is `""` |
| `is two places wide whatever the view carries` | for each of `[]`, `["Ah"]`, `["Ah", "Ks"]` and `["Ah", "Ks", "2c"]`, `container.childElementCount` is `2`. The fourth case carries the title's weight: the first three render two places under "always two" *and* under "at least two", so a hand that widens to `Math.max(2, cards.length)` ships `Tests 154 passed (154)` against them — verified, not predicted. A hand that widens breaks the row exactly as badly as one that narrows |

Three tests. One hundred and fifty-one exist, so the suite reports **154**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 154 passed (154)` | the three ran and the hundred-and-fifty-one before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks — the `.at()` detail is a `tsc` matter the runner cannot see — lints, formats |

**Name the edit that makes each assertion red.** The claim under test is universal — *a hand is two
places wide whatever the view carries* — so the three edits are three different ways to break it,
not one shape three times. All three were run against this exact test file:

1. Iterate the array: `{props.cards.map((_unused, place) => {` → `draws two backs when the view
   carries no card` fails with `Unable to find an accessible element with the role "img" and name
   "your rival's hidden hand"` and `is two places wide whatever the view carries` with `expected +0
   to be 2 // Object.is equality`. This is the "renders absence as absence" bug the story names.
   Revert.
2. Label both backs — `label={props.hiddenLabel}` → `draws two backs when the view carries no card`
   fails with `Found multiple elements with the role "img" and name "your rival's hidden hand"`.
   Revert.
3. Let the hidden label carry something about the hand behind it — `` label={place === 0 ?
   `${props.hiddenLabel} (${props.cards.length})` : null} `` → `draws two backs when the view
   carries no card` fails with `Unable to find an accessible element with the role "img" and name
   "your rival's hidden hand"`. Revert. A back that knows anything is the leak.

Quote all three in the PR.

## Acceptance criteria

- [ ] `a hand > draws two faces when the view carries two cards` passes
- [ ] `a hand > draws two backs when the view carries no card` passes
- [ ] `a hand > is two places wide whatever the view carries` passes
- [ ] `Hand`'s props type is exactly `{ cards: readonly string[]; hiddenLabel: string }` — it is
      handed no `SeatView` and so can read no `hasFolded` or `isAllIn`
- [ ] The `.map(` call in `Hand.tsx` is on `[0, 1]`; `props.cards` is only ever indexed with `.at()`
- [ ] `npm run --silent test` reports `Tests  154 passed (154)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
