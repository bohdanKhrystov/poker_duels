---
schema: 2
id: TASK-030805
title: The result screen declares the verdict and the coin beside it
type: task
status: done
parent: STORY-0308
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, ui, result]
depends_on: [TASK-030804]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +264 passed \(264\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'declares a victory when the winner is your seat'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'declares a defeat when the winner is the other seat'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'declares a draw, and moves no coin'
  - cd web-client && npm run check
---

## Goal

`DuelResult` exists: one framed panel that states the verdict in the design's word and colour, with
the coin that moved beneath it — and nothing beneath that yet.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/result/DuelResult.tsx` | create |
| `web-client/src/result/DuelResult.test.tsx` | create |
| `web-client/src/result/outcome-text.ts` | read — `verdictOf`, `verdictHeadline`, `coinLine` |
| `web-client/src/result/outcome-fixture.ts` | read — `anOutcome` |
| `design/screens/duel-end.html` | read — `.frame`, `.verdict`, `.coinline` |

## Scope

- The whole of `DuelResult.tsx`, verbatim:

  ```tsx
  import type { ReactElement } from "react";
  import type { DuelOutcome } from "../protocol";
  import { CoinMark } from "./CoinMark";
  import {
    coinLine,
    verdictHeadline,
    verdictOf,
    type Verdict,
  } from "./outcome-text";

  /**
   * The result screen: who won, and the coin.
   *
   * Both verdicts get the same panel, the design's point — losing must not feel
   * like a different, smaller product. Everything on it is read off the
   * `DuelOutcome` the server sent and the seat the server gave this client;
   * nothing here compares a stack, adds a chip or names a hand.
   */
  export function DuelResult(props: {
    outcome: DuelOutcome;
    mySeat: number | null;
  }): ReactElement {
    const verdict = verdictOf(props.outcome, props.mySeat);
    const coin = coinLine(verdict);
    return (
      <section
        aria-label="the result"
        className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
      >
        <h2
          className={`text-display leading-tight font-bold ${verdictColour(verdict)}`}
        >
          {verdictHeadline(verdict)}
        </h2>
        {coin !== null && (
          <p className="flex items-center gap-3 font-mono">
            <CoinMark />
            <span className={verdictColour(verdict)}>{coin}</span>
          </p>
        )}
      </section>
    );
  }

  /**
   * The design's two colours, and neither when there is no side to take: a draw
   * and an unread seat keep the body colour.
   */
  function verdictColour(verdict: Verdict): string {
    switch (verdict) {
      case "win":
        return "text-win";
      case "loss":
        return "text-loss";
      case "draw":
      case "unknown":
        return "";
    }
  }
  ```

- The panel is a labelled `section`, so a test and a screen reader both reach it as
  `region` named *the result* — the same shape the bar's `aria-label="your move"` takes.
- `max-w-[380px]` is the design's `.frame` width. `--pd-win` / `--pd-loss` arrive as `text-win` and
  `text-loss`, which the theme already exposes; no token and no theme line changes.

## Out of scope

- The hand count and the final stacks — `TASK-030806`, in this same file.
- The way back to the lobby — `TASK-030807`.
- Rematch. `STORY-0309` owns it and it is blocked on `DEC-023`; a stub here would be a dead control,
  and one faked with `CreateRoom` would lose the button-seat alternation the room owns.
- Showing this component anywhere — `TASK-030809` puts it on the duel screen.

## Tests

`web-client/src/result/DuelResult.test.tsx`, describe block `"the result screen"`.

| Test | Proves |
| --- | --- |
| `declares a victory when the winner is your seat` | with `winner: 1` and `mySeat: 1`, the heading reads `Victory`, its class list holds `text-win`, and the text `+1 duel coin` is on screen |
| `declares a defeat when the winner is the other seat` | with `winner: 1` and `mySeat: 0`, the heading reads `Defeat`, its class list holds `text-loss`, and the text `−1 duel coin` is on screen |
| `declares a draw, and moves no coin` | with `winner: null`, the heading reads `Draw`, no text matching `/duel coin/` is found, and the container holds no `[aria-hidden="true"]` element — the mark is absent, not merely unlabelled |
| `says the duel is over when the client holds no seat` | with `mySeat: null` and a named winner, the heading reads `Duel over`, and no coin line is on screen |

The first two use `winner: 1` against the fixture's `finalStacks: [19400, 4600]`, so the seat
holding the larger stack is the loser: a panel that read the verdict off the chips would print the
opposite word in both.

```tsx
it("declares a defeat when the winner is the other seat", () => {
  render(<DuelResult outcome={anOutcome({ winner: 1 })} mySeat={0} />);

  const heading = screen.getByRole("heading", { name: "Defeat" });
  expect(heading.className.split(" ")).toContain("text-loss");
  expect(screen.getByText("−1 duel coin")).toBeDefined();
});
```

Four tests. Two hundred and sixty exist, so the suite reports **264**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 264 passed (264)` | the four ran and the two hundred and sixty before them still do |
| the three `--reporter=verbose` greps | the win, the loss and the draw each exist by name |
| `npm run check` | typechecks — `verdictColour` is exhaustive, and `mySeat` is passed as `number \| null` |

**Name the edit that makes each assertion red:**

1. Render the coin line unconditionally → `declares a draw, and moves no coin` fails on the
   `/duel coin/` query. Revert.
2. Give the heading `text-win` whatever the verdict → `declares a defeat when the winner is the
   other seat` fails on the class list. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the result screen > declares a victory when the winner is your seat` passes
- [ ] `the result screen > declares a defeat when the winner is the other seat` passes
- [ ] `the result screen > declares a draw, and moves no coin` passes
- [ ] `the result screen > says the duel is over when the client holds no seat` passes
- [ ] `DuelResult.tsx` does not name `finalStacks` — the stacks are `TASK-030806`'s line — and
      compares no chip amount to any other
- [ ] `npm run --silent test` reports `Tests  264 passed (264)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
