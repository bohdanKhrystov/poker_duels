---
schema: 2
id: TASK-030804
title: The coin mark is steel, and says nothing a screen reader has to hear twice
type: task
status: ready
parent: STORY-0308
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, ui, result, coin]
depends_on: [TASK-030803]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +260 passed \(260\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'draws the coin from the coin tokens'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is decorative, and names nothing'
  - cd web-client && npm run check
---

## Goal

The duel coin has a mark to sit beside its line: the design's steel disc, drawn from the token that
composes it, and silent to assistive technology.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/result/CoinMark.tsx` | create |
| `web-client/src/result/CoinMark.test.tsx` | create |
| `design/screens/duel-end.html` | read — `.coin`, and the forced-colors note beside it |
| `web-client/src/styles/tokens.css` | read — `--pd-coin-face` and what it composes |

## Scope

- The whole of `CoinMark.tsx`, verbatim:

  ```tsx
  import type { ReactElement } from "react";

  /**
   * The duel coin's mark. Steel, never gold: `docs/vision.md` says it counts
   * duels rather than glitters, and `--pd-coin-face` is the one place that face
   * is composed — the same lighting as `design/coin/duel-coin.svg`.
   *
   * Decorative on purpose. The line beside it already says what moved and by
   * how much, so a mark that named itself would make a screen reader say
   * "coin" twice.
   */
  export function CoinMark(): ReactElement {
    return (
      <span
        aria-hidden="true"
        className="inline-block h-6 w-6 rounded-pill forced-colors:border"
        style={{ background: "var(--pd-coin-face)" }}
      />
    );
  }
  ```

- The face arrives through `style`, not a Tailwind colour utility, because `--pd-coin-face` is a
  `radial-gradient` — an image, not a colour — and `bg-*` would emit an invalid
  `background-color`. It is still a token reference, so the colour-literal guard
  (`TASK-030202`) passes: it flags hex and functional notations, never `var(--pd-*)`.
- `h-6 w-6` is the theme's `--spacing-6`, 24px against the design's 22px: the token scale is the
  client's size vocabulary (`TASK-030206`) and a `[22px]` literal would be the one hard-coded
  measurement on the screen.
- `forced-colors:border` keeps the disc a visible ring where a contrast theme has stripped the
  gradient — the design's own note, in Tailwind's variant.

## Out of scope

- The full `duel-coin.svg`. The design's note says the SVG carries ceremonial duty *if the client
  wants it larger*; this screen wants the 22px mark, and vendoring an asset is `EPIC-06` work.
- Animation, spin, shine, a coin that lands. `docs/vision.md` refuses the casino, and `STORY-0601`
  refuses confetti.
- Putting the mark on screen — `TASK-030805` places it beside the coin line.

## Tests

`web-client/src/result/CoinMark.test.tsx`, describe block `"the coin mark"`.

| Test | Proves |
| --- | --- |
| `draws the coin from the coin tokens` | the rendered element's `style` attribute contains `var(--pd-coin-face)`, and its class list contains `rounded-pill` — the disc is round and its face is the token |
| `is decorative, and names nothing` | the element carries `aria-hidden="true"`, has no `aria-label` and no `title`, and its `textContent` is empty |

```tsx
it("draws the coin from the coin tokens", () => {
  const { container } = render(<CoinMark />);

  const mark = container.firstElementChild!;
  expect(mark.getAttribute("style")).toContain("var(--pd-coin-face)");
  expect(mark.className.split(" ")).toContain("rounded-pill");
});
```

Asserting the class and the style attribute — not computed CSS — is the precedent
`PlayingCard.test.tsx` set for `text-suit-red`: jsdom loads no stylesheet, so the name of the rule
is the only thing a test can hold.

Two tests. Two hundred and fifty-eight exist, so the suite reports **260**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 260 passed (260)` | the two ran and the two hundred and fifty-eight before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, format-checks, and the colour-literal guard passes over the new file |

**Name the edit that makes each assertion red:**

1. Replace the style with `className="bg-coin"` → `draws the coin from the coin tokens` fails: the style attribute is gone. Revert.
2. Swap `aria-hidden` for `aria-label="duel coin"` → `is decorative, and names nothing` fails. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the coin mark > draws the coin from the coin tokens` passes
- [ ] `the coin mark > is decorative, and names nothing` passes
- [ ] `CoinMark.tsx` contains no hex or `rgb(`/`hsl(` literal, and no pixel literal
- [ ] `npm run --silent test` reports `Tests  260 passed (260)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
