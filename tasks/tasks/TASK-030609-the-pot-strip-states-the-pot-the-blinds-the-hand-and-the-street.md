---
schema: 2
id: TASK-030609
title: The pot strip states the pot, the blinds, the hand and the street
type: task
status: backlog
parent: STORY-0306
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, duel, ui, design]
depends_on: [TASK-030608]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +167 passed \(167\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the street the view names even when the board disagrees'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the pot from the view and not from what the seats put in'
  - cd web-client && npm run check
---

## Goal

The design's pot row, with every figure on it read straight off the view: the pot is `view.pot` and
not a sum, and the street is `view.street` and not a count of board cards.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/PotStrip.tsx` | create |
| `web-client/src/table/PotStrip.test.tsx` | create |
| `web-client/src/table/view-fixture.ts` | read — `aView`, `aSeat` |
| `web-client/src/protocol/protocol.gen.ts` | read — `PlayerView` and the six `Street` values |
| `design/screens/duel-table.html` | read — the `.pot` rule and its meta line. **Read only: never edit anything under `design/`** |

## Scope

- The whole file, verbatim:

  ```tsx
  import type { ReactElement } from "react";
  import type { PlayerView, Street } from "../protocol";
  import { formatChips } from "./chips";

  // The street the view names, written out. A side table rather than a switch so
  // `tsc` fails the day the wire grows a street this screen has no word for.
  const STREET_NAMES: Record<Street, string> = {
    PREFLOP: "Preflop",
    FLOP: "Flop",
    TURN: "Turn",
    RIVER: "River",
    SHOWDOWN: "Showdown",
    COMPLETE: "Hand complete",
  };

  /**
   * The pot and the hand's standing facts, every one of them read straight off
   * the view: the pot is `view.pot` and not a sum of what the seats put in, and
   * the street is `view.street` and not a count of board cards — those two
   * disagree at exactly the moments that matter.
   */
  export function PotStrip(props: { view: PlayerView }): ReactElement {
    const { view } = props;
    return (
      <div className="flex items-baseline gap-4 px-2 py-3">
        <span className="font-mono text-large tabular-nums">
          Pot&nbsp;{formatChips(view.pot)}
        </span>
        <span className="text-small text-text-muted">
          Blinds {formatChips(view.smallBlind)}/{formatChips(view.bigBlind)} ·
          Hand {view.handNumber} · {STREET_NAMES[view.street]}
        </span>
      </div>
    );
  }
  ```

- **The street name is a text addition to an existing design slot**, not a new element: the design's
  `.pot .meta` reads `Blinds 75/150 · Hand 14`, and the story's third acceptance criterion requires
  the street to render. A third `·`-separated item is the smallest way to satisfy both. Say so in
  the PR; if `EPIC-06` words it differently later it is one line.
- `Record<Street, string>` is deliberate: it is exhaustive by construction, so a seventh street on
  the wire is a `tsc` error rather than a blank on the screen.
- `&nbsp;` after `Pot` is the design's — the amount never wraps away from its label.
  `@testing-library`'s default matcher normalises it, so `getByText("Pot 2,450")` matches.
- Classes are already in `prettier-plugin-tailwindcss`'s order; run `npm run format` and expect no
  diff. `text-large` → `--pd-fs-large`, `text-small` → `--pd-fs-small`,
  `text-text-muted` → `--pd-text-muted`, `gap-4`/`px-2`/`py-3` → the `--pd-space-*` ladder.

## Out of scope

- The showdown banner (`You win 4,850` and the hand name) that the states screen swaps into this
  same row. It needs the winner and a hand rank, neither of which is in a `PlayerView` — it is
  `STORY-0308`'s, and naming a hand here would be the client deciding a game fact.
- `betToMatch` and `minRaiseTo`. They are in the view and are the action bar's (`STORY-0307`);
  nothing on this row shows them.

## Tests

`web-client/src/table/PotStrip.test.tsx`, describe block `"the pot strip"`.

| Test | Proves |
| --- | --- |
| `writes the pot the view carries, grouped` | `aView({ pot: 2450 })` renders text `Pot 2,450` |
| `writes the blinds and the hand number the view carries` | `aView({ smallBlind: 75, bigBlind: 150, handNumber: 14 })` matches `/Blinds 75\/150 · Hand 14/` |
| `names the street the view names` | all six `Street` values render `Preflop`, `Flop`, `Turn`, `River`, `Showdown`, `Hand complete`, each matched by `` new RegExp(`· ${name}$`) `` with an `unmount()` between renders |
| `names the street the view names even when the board disagrees` | `aView({ street: "TURN", board: { cards: ["As", "7d", "2c"] } })` matches `/· Turn$/` and `queryByText(/· Flop$/)` is `null` |
| `takes the pot from the view and not from what the seats put in` | `aView({ pot: 4850, seats: [aSeat({ committedThisHand: 10 }), aSeat({ index: 1, committedThisHand: 10 })] })` renders `Pot 4,850` |

Five tests. One hundred and sixty-two exist, so the suite reports **167**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 167 passed (167)` | the five ran and the hundred-and-sixty-two before them still do |
| the two `--reporter=verbose` greps | the two anti-derivation tests exist by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Read the street off the board: `{["Preflop", "Preflop", "Preflop", "Flop", "Turn",
   "River"][view.board.cards.length]}` → `names the street the view names even when the board
   disagrees` fails with `Unable to find an element with the text: /· Turn$/`, and `names the street
   the view names` with the same for `/· Flop$/`. Revert.
2. Sum the pot: `formatChips(view.seats.reduce((total, seat) => total + seat.committedThisHand, 0))`
   → `takes the pot from the view and not from what the seats put in` fails with `Unable to find an
   element with the text: Pot 4,850`, and `writes the pot the view carries, grouped` with `Unable to
   find an element with the text: Pot 2,450`. Revert.
3. Drop `formatChips` and interpolate `view.pot` → the same two failures. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the pot strip > writes the pot the view carries, grouped` passes
- [ ] `the pot strip > writes the blinds and the hand number the view carries` passes
- [ ] `the pot strip > names the street the view names` passes
- [ ] `the pot strip > names the street the view names even when the board disagrees` passes
- [ ] `the pot strip > takes the pot from the view and not from what the seats put in` passes
- [ ] `PotStrip.tsx` contains no `reduce` and no `.length`
- [ ] `npm run --silent test` reports `Tests  167 passed (167)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
