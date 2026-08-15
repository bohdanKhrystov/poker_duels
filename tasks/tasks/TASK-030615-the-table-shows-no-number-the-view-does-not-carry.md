---
schema: 2
id: TASK-030615
title: The table shows no number the view does not carry
type: task
status: done
parent: STORY-0306
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, duel, testing, authority]
depends_on: [TASK-030614]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +186 passed \(186\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows no number the view does not carry'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the street the view names, not the one the board looks like'
  - cd web-client && npm run check
---

## Goal

`CLAUDE.md`'s non-negotiable made executable for this screen: *the server is authoritative; a client
may never assert a game fact.* One test renders the finished table and asserts that **every figure
on it is a figure the `PlayerView` carries** — so a pot summed, a call priced or a stack totalled is
a red suite, not a code review someone has to notice.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/no-derivation.test.tsx` | create |
| `web-client/src/table/DuelTable.tsx` | read — what is under test |
| `web-client/src/table/view-fixture.ts` | read — `aView`, `aSeat` |
| `web-client/src/protocol/protocol.gen.ts` | read — the `PlayerView` field list |

## Scope

- No production file changes. This ticket adds a guard over what already merged.
- The fixture is chosen so the guard can fire, and the choice is the ticket:

  ```tsx
  // Every number is distinct, and no two of them add, subtract or halve into a
  // third: any figure the table works out for itself therefore lands outside the
  // allowed set instead of colliding with a legitimate one. The board is three
  // cards on the turn, so a street read off the card count is the wrong street.
  const VIEW: PlayerView = aView({
    viewerSeat: 0,
    handNumber: 14,
    buttonSeat: 1,
    street: "TURN",
    board: { cards: ["As", "7d", "2c"] },
    pot: 4850,
    betToMatch: 950,
    minRaiseTo: 1900,
    seatToAct: 0,
    smallBlind: 75,
    bigBlind: 150,
    seats: [
      aSeat({
        index: 0,
        stack: 13000,
        committedThisStreet: 100,
        committedThisHand: 1200,
        holeCards: ["Ah", "Ks"],
      }),
      aSeat({
        index: 1,
        stack: 3750,
        committedThisStreet: 400,
        committedThisHand: 800,
        holeCards: [],
      }),
    ],
  });
  ```

  The arithmetic that matters: `950 − 100 = 850` and `950 − 400 = 550` are the two "to call" figures,
  `13000 + 3750 = 16750` the stacks totalled, `4850 + 500` the pot with the street's chips folded
  in — and none of the five is in the view. **If a number here is changed, check that again**, or
  the guard silently stops guarding.

- Three helpers, verbatim:

  ```tsx
  /** Every number `view` carries, in any field a screen could reach. */
  function numbersIn(view: PlayerView): Set<number> {
    return new Set([
      view.viewerSeat,
      view.handNumber,
      view.buttonSeat,
      view.pot,
      view.betToMatch,
      view.minRaiseTo,
      view.seatToAct ?? 0,
      view.smallBlind,
      view.bigBlind,
      ...view.seats.flatMap((seat) => [
        seat.index,
        seat.stack,
        seat.committedThisStreet,
        seat.committedThisHand,
      ]),
    ]);
  }

  /**
   * Every text node under `container`, joined by a space.
   *
   * Not `textContent`: that runs the last word of one element into the first of
   * the next, and `\b` then fails to see a word boundary that the player's eye
   * sees plainly — a banner reading "You win" beside a card would slip a `\bwin\b`
   * guard entirely. Measured, not reasoned about.
   */
  function wordsOnScreen(container: HTMLElement): string {
    const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
    const parts: string[] = [];
    for (let node = walker.nextNode(); node !== null; node = walker.nextNode()) {
      parts.push(node.textContent ?? "");
    }
    return parts.join(" ");
  }

  /**
   * Every number the table puts in front of the player, with the card faces
   * removed first: a rank glyph is a character the server sent, not a figure.
   */
  function numbersOnScreen(container: HTMLElement): number[] {
    const copy = container.cloneNode(true) as HTMLElement;
    copy.querySelectorAll('[role="img"]').forEach((card) => card.remove());
    const digits = wordsOnScreen(copy).match(/\d[\d,]*/g) ?? [];
    return digits.map((run) => Number(run.replaceAll(",", "")));
  }
  ```

  **The `[role="img"]` removal is required**, not tidiness: a `7` of diamonds prints the character
  `7`, and without stripping the card elements the guard would flag the deck.

## Out of scope

- The card and hand-name halves of the same claim — `TASK-030616`, which appends to this file.
- Changing any component. If this guard goes red, the component is wrong, not the guard.

## Tests

`web-client/src/table/no-derivation.test.tsx`, describe block
`"the table renders and never derives"`.

| Test | Proves |
| --- | --- |
| `shows no number the view does not carry` | the fixture's own arithmetic independence is asserted first — **no two of its numbers sum, differ, double or halve into a third** — and then `numbersOnScreen(container)` is non-empty and every value in it is in `numbersIn(VIEW)`, where "on screen" includes **`aria-label` and `title`, not only text nodes**. The independence is asserted rather than claimed in a comment because it had already rotted once: `950 x 2 = 1900` and `75 x 2 = 150` were in the fixture, so a client deriving `minRaiseTo` from the bet or the big blind from the small would have landed on a legitimate number and passed. Both reach the player: one is read aloud, the other shown on hover. A text-only scan ships `Tests 186 passed (186)` against a derived total planted in either. Verified in all three placements, not predicted |
| `names the street the view names, not the one the board looks like` | `getByText(/· Turn$/)` resolves while `queryByText(/· Flop$/)` is `null`, on a view whose board holds three cards |

Two tests. One hundred and eighty-four exist, so the suite reports **186**.

The `expect(shown.length).toBeGreaterThan(0)` line is not decoration: without it, a table that
rendered nothing at all would satisfy "no number the view does not carry" forever.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 186 passed (186)` | the two ran and the hundred-and-eighty-four before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red.** The claim is universal — *no number the view does
not carry* — so these are four different ways for a figure to appear, not one shape four times. All
four were run against this exact test file, and each was reverted afterwards:

1. **Pot arithmetic** — in `PotStrip.tsx`, `formatChips(view.pot + view.seats.reduce((t, x) => t +
   x.committedThisStreet, 0))` → `shows no number the view does not carry` fails with `expected [
   5350 ] to deeply equal []`.
2. **A price the client worked out** — in `DuelTable.tsx`, add `<p>to call {view.betToMatch -
   rival.committedThisStreet}</p>` under the bet line → fails with `expected [ 550 ] to deeply
   equal []`.
3. **A total the client worked out** — in `DuelTable.tsx`, add `<p>{view.seats.reduce((t, x) => t +
   x.stack, 0)} behind</p>` beside the pot → fails with `expected [ 16750 ] to deeply equal []`.
4. **The street inferred from the board** — in `PotStrip.tsx`, `{["Preflop", "Preflop", "Preflop",
   "Flop", "Turn", "River"][view.board.cards.length]}` → `names the street the view names, not the
   one the board looks like` fails with `Unable to find an element with the text: /· Turn$/`.

Quote all four in the PR.

## Acceptance criteria

- [ ] `the table renders and never derives > shows no number the view does not carry` passes
- [ ] `the table renders and never derives > names the street the view names, not the one the board looks like` passes
- [ ] No file outside `web-client/src/table/no-derivation.test.tsx` is changed
- [ ] `npm run --silent test` reports `Tests  186 passed (186)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
