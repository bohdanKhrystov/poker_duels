---
schema: 2
id: TASK-030806
title: The result states the hands played and every final stack, exactly as sent
type: task
status: backlog
parent: STORY-0308
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, ui, result]
depends_on: [TASK-030805]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +268 passed \(268\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states the hand count and both final stacks'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'counts one hand in the singular'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'drops the owner words when the client holds no seat'
  - cd web-client && npm run check
---

## Goal

Under the coin, the duel's ledger: how many hands it took and what each seat was left holding, in
the server's own figures.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/result/DuelResult.tsx` | modify — one element and one helper added |
| `web-client/src/result/DuelResult.test.tsx` | modify — three tests appended |
| `web-client/src/table/chips.ts` | read — `formatChips`, the one way this client groups digits |
| `web-client/src/table/DuelTable.tsx` | read — where *You* and *Your rival* were first written |

## Scope

- One element, below the coin line and inside the same `section`:

  ```tsx
  <p className="text-small text-text-muted">{metaLine(props.outcome, props.mySeat)}</p>
  ```

- One helper beside `verdictColour`:

  ```tsx
  /**
   * The duel's ledger: the hand count, then one entry per final stack, in the
   * order the server sent them. Mapped rather than indexed — the wire says
   * `finalStacks` is an array and says nothing about its length, so the line
   * states what arrived instead of assuming two.
   *
   * The owner words are the table's own (`DuelTable`), and they need a seat: a
   * client that does not know which side it sat on states the stacks plainly
   * rather than guessing which is whose.
   */
  function metaLine(outcome: DuelOutcome, mySeat: number | null): string {
    const hands = `${outcome.handsPlayed} ${outcome.handsPlayed === 1 ? "hand" : "hands"}`;
    const stacks = outcome.finalStacks.map((stack, seat) =>
      mySeat === null
        ? formatChips(stack)
        : `${seat === mySeat ? "You" : "Your rival"} ${formatChips(stack)}`,
    );
    return [hands, ...stacks].join(" · ");
  }
  ```

- One import joins the file: `import { formatChips } from "../table/chips";` — the same grouping the
  table uses, so `19400` reads `19,400` on both screens, locale or no locale. The separator between
  parts is the design's middle dot `·` (U+00B7), as in `17 hands · 12 minutes`.
- The plural is grammar, not arithmetic: `handsPlayed` is printed, never adjusted.

## Out of scope

- Duration. The design's line reads *17 hands · 12 minutes*, and **there is no clock on the wire** —
  `DuelOutcome` is `{winner, handsPlayed, finalStacks}`. A minute count would have to be timed by
  the client, which is the derivation this story exists to refuse. The middle dot and the hand count
  are kept; the minutes are dropped.
- *"you took the whole stack"*. Nothing on the wire says how the last pot was won.
- Chip totals, differences, "you won 4,600 chips". Only the figures the outcome carries.
- The way back to the lobby — `TASK-030807`.

## Tests

`web-client/src/result/DuelResult.test.tsx`, appended to the `"the result screen"` block.

| Test | Proves |
| --- | --- |
| `states the hand count and both final stacks` | with the fixture and `mySeat: 0`, the panel shows the exact text `17 hands · You 19,400 · Your rival 4,600` |
| `counts one hand in the singular` | with `handsPlayed: 1`, the line begins `1 hand ·` and the string `1 hands` appears nowhere |
| `follows your seat when it says which stack is yours` | with `mySeat: 1`, the same outcome reads `17 hands · Your rival 19,400 · You 4,600` — the words follow the seat, not the array's order |
| `drops the owner words when the client holds no seat` | with `mySeat: null`, the line is `17 hands · 19,400 · 4,600` and contains neither `You` nor `Your rival` |

The first test asserts the whole line as one string rather than three fragments: a figure the panel
adds, doubles or re-orders changes that string and cannot hide between two looser queries.

```tsx
it("states the hand count and both final stacks", () => {
  render(<DuelResult outcome={anOutcome()} mySeat={0} />);

  expect(
    screen.getByText("17 hands · You 19,400 · Your rival 4,600"),
  ).toBeDefined();
});
```

Four tests. Two hundred and sixty-four exist, so the suite reports **268**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 268 passed (268)` | the tests ran and everything before them still does |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks — `metaLine` takes `mySeat: number \| null` and returns a string |

**Name the edit that makes each assertion red:**

1. Print `outcome.finalStacks[0]` and `outcome.finalStacks[1]` by index with fixed labels → `drops
   the owner words when the client holds no seat` fails. Revert.
2. Print `${outcome.handsPlayed} hands` unconditionally → `counts one hand in the singular` fails
   with `1 hands`. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the result screen > states the hand count and both final stacks` passes
- [ ] `the result screen > counts one hand in the singular` passes
- [ ] `the result screen > follows your seat when it says which stack is yours` passes
- [ ] `the result screen > drops the owner words when the client holds no seat` passes
- [ ] The four tests `TASK-030805` wrote are byte-identical
- [ ] `DuelResult.tsx` indexes `finalStacks` nowhere: the line is built by `map`
- [ ] `npm run --silent test` reports `Tests  268 passed (268)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
