---
schema: 2
id: TASK-030803
title: The coin line states the one coin the duel moved, and no balance
type: task
status: backlog
parent: STORY-0308
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, result, coin]
depends_on: [TASK-030802]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +258 passed \(258\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'gives the winner the coin'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "takes the loser's coin, in a true minus sign"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'moves no coin on a draw, or without a seat'
  - cd web-client && npm run check
---

## Goal

The verdict names the coin that changed hands — `+1`, `−1` or nothing — as the constant `ADR-0014`
fixes, never as a balance the client counted.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/result/outcome-text.ts` | modify — one exported function appended |
| `web-client/src/result/outcome-text.test.ts` | modify — one describe block appended |
| `web-client/src/table/card-text.ts` | read — how `TASK-030618` writes a glyph by codepoint |

## Scope

- Appended to `outcome-text.ts`, verbatim:

  ```ts
  /**
   * What the coin did, in the one figure `ADR-0014` fixes: the winner gains one,
   * the loser loses one, a draw moves none. `null` is "say nothing" — a draw
   * prints no coin line at all, because a coin that did not move is not news.
   *
   * This is stated, not counted. The *balance* is the server's and is read from
   * `GET /api/me` (`STORY-0311`); a client that added this delta to a number it
   * held would be asserting a fact about the economy.
   */
  export function coinLine(verdict: Verdict): string | null {
    switch (verdict) {
      case "win":
        return "+1 duel coin";
      case "loss":
        return "−1 duel coin";
      case "draw":
      case "unknown":
        return null;
    }
  }
  ```

- The minus is **U+2212 MINUS SIGN** (`−`) as a literal character in the source, never the ASCII
  hyphen-minus `-`. `docs/protocol.md` and `STORY-0311` write the negative balance the same way, and
  a hyphen beside a `+` in the design's mono, tabular face is visibly the wrong glyph. The test
  below rejects the lookalike rather than trusting the two literals to match.
- `"unknown"` prints no coin: a client that does not know which seat it holds cannot say which way
  the coin went, and guessing is the derivation this story exists to refuse.

## Out of scope

- Any balance, total or lifetime count. `STORY-0311` reads `GET /api/me`; this story shows one
  duel's delta and nothing cumulative.
- The coin's drawing — `TASK-030804`.
- Colour. The word is here, the `text-win` / `text-loss` class is `TASK-030805`'s.

## Tests

`web-client/src/result/outcome-text.test.ts`, a second describe block `"the coin line"` below the
existing `"the verdict"` block.

| Test | Proves |
| --- | --- |
| `gives the winner the coin` | `coinLine("win")` is exactly `"+1 duel coin"` |
| `takes the loser's coin, in a true minus sign` | `coinLine("loss")` is exactly `"−1 duel coin"`, and does not contain the ASCII hyphen `"-"` |
| `moves no coin on a draw, or without a seat` | `coinLine("draw")` and `coinLine("unknown")` are both `null` |

The second test asserts the codepoint and rejects the lookalike, the lesson `TASK-030618` paid for:
a literal compared against an identical literal proves the two files agree, not that either is
right.

```ts
it("takes the loser's coin, in a true minus sign", () => {
  expect(coinLine("loss")).toBe("−1 duel coin");
  expect(coinLine("loss")).not.toContain("-");
});
```

Three tests. Two hundred and fifty-five exist, so the suite reports **258**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 258 passed (258)` | the three ran and the two hundred and fifty-five before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks — the `switch` is exhaustive over `Verdict` without a `default` |

**Name the edit that makes each assertion red:**

1. Write the loss as `"-1 duel coin"` with a hyphen → `takes the loser's coin, in a true minus sign` fails on both assertions. Revert.
2. Return `"0 duel coins"` for a draw → `moves no coin on a draw, or without a seat` fails with `expected '0 duel coins' to be null`. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the coin line > gives the winner the coin` passes
- [ ] `the coin line > takes the loser's coin, in a true minus sign` passes
- [ ] `the coin line > moves no coin on a draw, or without a seat` passes
- [ ] `outcome-text.ts` contains no arithmetic operator: the coin is a constant, not a sum
- [ ] The `"the verdict"` describe block is byte-identical to `TASK-030802`'s
- [ ] `npm run --silent test` reports `Tests  258 passed (258)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
