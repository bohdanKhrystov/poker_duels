---
schema: 2
id: TASK-030715
title: The derivation guards read the numbers that reach the DOM as attributes
type: task
status: done
parent: STORY-0307
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, duel, test]
depends_on: [TASK-030714]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'counts the ceiling that reaches the player only as a slider bound'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'counts a number that reaches the player only as an attribute'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows no number the turn does not carry'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows no number the view does not carry'
  - cd web-client && npm run check
---

## Goal

Both whole-surface derivation guards see every number that reaches a player, including the ones that
never become words. Today they scan text nodes, `aria-label` and `title` only — so a figure the
client worked out for itself and put in a `min`, `max` or `value` attribute ships green through both.

## Why this is a real hole, not a hypothetical one

In `ActionBar.tsx` the amount control renders `max={actions.allInTo}`. Whenever `allowed` includes
`BET` or `RAISE` but **excludes** `ALL_IN`, that attribute is the ceiling's only route to the player:
there is no all-in button to print it and no label to speak it. Neither guard renders that
combination — `bar-no-derivation.test.tsx` uses the fixture's default `[FOLD, CALL, RAISE, ALL_IN]`,
where an all-in button prints the figure anyway, and `[CHECK]`, where there is no slider at all. A
corrupted ceiling is therefore invisible to both files today. Found by a deep review of
`STORY-0307`, not by a failing test, which is the point.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/bar-no-derivation.test.tsx` | modify — the sweep, and the test that exercises it |
| `web-client/src/table/no-derivation.test.tsx` | modify — the same sweep, and a test that keeps it honest |
| `web-client/src/table/ActionBar.tsx` | read — `max`, `min`, `value`, `step` on the range input |
| `web-client/src/table/turn-fixture.ts` | read — `aTurn`, `aLegalActions` |
| `web-client/src/table/chips.ts` | read — `formatChips`, for the "is not printed" half |

## Scope

- In **both** files' `numbersOnScreen`, add a third source beside the text nodes and the spoken
  strings — the numeric attributes `min`, `max` and `value`:

  ```tsx
  // Nor is everything a player receives a word. `min`, `max` and `value` reach
  // the DOM as attributes and nothing prints them, so a bound the client worked
  // out for itself is invisible to a text-and-aria scan. Measured, not reasoned
  // about: with BET or RAISE allowed and ALL_IN withheld, `max={allInTo}` is the
  // only place that ceiling appears anywhere in the bar.
  const bounds = [...container.querySelectorAll("[min], [max], [value]")]
    .flatMap((element) => [
      element.getAttribute("min"),
      element.getAttribute("max"),
      element.getAttribute("value"),
    ])
    .filter((value): value is string => value !== null);
  ```

  and join `bounds` into the digit match alongside the two sources already there. In
  `no-derivation.test.tsx` the sweep reads `copy`, not `container`, so it runs **after** the
  `role="img"` cards are removed, exactly as the spoken sweep does.
- Add one test to each file, named and shaped below.
- Nothing else. Both files stay green against today's components: every attribute the bar renders
  already holds a figure the server sent.

## Out of scope

- **`step`.** It is `1` on the range input — a control's granularity, not a figure about the duel —
  and `1` is in no `LegalActions`, so sweeping it turns `shows no number the turn does not carry`
  red. If the sweep is red, the fix is to stop sweeping `step`, never to widen the permitted set.
- Any change to `ActionBar.tsx`, `DuelTable.tsx` or any component. This ticket adds no behaviour and
  is expected to find no violation today; it makes tomorrow's finding possible.
- `aria-valuenow`, `aria-valuemin`, `aria-valuemax` and `data-*`. Nothing renders them; a sweep with
  no renderer is the dead extension this ticket exists to avoid.
- Extracting the two scanners into one shared module. That is a third file and a refactor of merged
  guards; if it is worth doing it is worth its own ticket.

## Tests

### `bar-no-derivation.test.tsx`, in `describe("the bar offers and derives nothing")`

| Test | Proves |
| --- | --- |
| `counts the ceiling that reaches the player only as a slider bound` | for **both** `["CHECK", "BET"]` and `["FOLD", "CALL", "RAISE"]` — the two shapes with an amount control and no `ALL_IN` button — `allInTo` appears in no text node, no `aria-label` and no `title`, formatted or plain, **and** `numbersOnScreen` returns it anyway; and every number it returns is still one of the four the turn carries |

The `toContain(ceiling)` line is the one that matters: without it the test would pass against a
scanner that returned an empty array, and the sweep could be deleted without a single red test.

```tsx
for (const allowed of [
  ["CHECK", "BET"],
  ["FOLD", "CALL", "RAISE"],
] as const) {
  const turn = aTurn({
    legalActions: aLegalActions({ allowed: [...allowed] }),
  });
  const { container, unmount } = render(
    <ActionBar turn={turn} rejection={null} refusal={null} send={vi.fn()} />,
  );
  const ceiling = turn.legalActions.allInTo;

  const printedOrSpoken = [
    container.textContent ?? "",
    ...[...container.querySelectorAll("[aria-label], [title]")].flatMap(
      (element) => [
        element.getAttribute("aria-label") ?? "",
        element.getAttribute("title") ?? "",
      ],
    ),
  ].join(" ");
  expect(printedOrSpoken).not.toContain(formatChips(ceiling));
  expect(printedOrSpoken).not.toContain(String(ceiling));

  expect(numbersOnScreen(container)).toContain(ceiling);

  const fromTheTurn = new Set([
    turn.legalActions.callTo,
    turn.legalActions.minBetTo,
    turn.legalActions.minRaiseTo,
    turn.legalActions.allInTo,
  ]);
  expect(
    numbersOnScreen(container).filter((n) => !fromTheTurn.has(n)),
  ).toEqual([]);

  unmount();
}
```

### `no-derivation.test.tsx`, in `describe("the table renders and never derives")`

| Test | Proves |
| --- | --- |
| `counts a number that reaches the player only as an attribute` | the table's sweep is live. `DuelTable` renders no `min`, `max` or `value` today, so a probe element carrying `max="987654"` — a figure in no `PlayerView` field — is appended to the rendered container and must come back from `numbersOnScreen` |

```tsx
const { container } = render(<DuelTable view={aView()} />);
const probe = document.createElement("input");
probe.setAttribute("type", "range");
probe.setAttribute("max", "987654");
container.appendChild(probe);

expect(numbersOnScreen(container)).toContain(987654);
```

A probe rather than a real assertion about the table, and deliberately: the thing under test here is
the guard's reach, and the table has nothing to reach for yet. Delete the sweep and this test is the
one that goes red.

## Proof

| Command | Proves |
| --- | --- |
| the two greps for the new names | both new tests exist by name and ran |
| the two greps for `shows no number the …` | neither existing guard was renamed or dropped while its scanner was edited |
| `npm run check` | typechecks, lints, formats, and every existing test still passes. No command greps a total: the tickets around this one land in any order and each moves it |

**Name the edit that makes each assertion red**, and quote all three in the PR:

1. Delete the `bounds` sweep from `bar-no-derivation.test.tsx` → `counts the ceiling that reaches
   the player only as a slider bound` fails on `toContain`. Revert.
2. Delete the `bounds` sweep from `no-derivation.test.tsx` → `counts a number that reaches the
   player only as an attribute` fails. Revert.
3. Add `element.getAttribute("step")` to the bar's sweep → `shows no number the turn does not carry`
   fails on the stray `1`. Revert; do not widen the permitted set.

## Acceptance criteria

- [ ] `counts the ceiling that reaches the player only as a slider bound` passes, and renders both
      `["CHECK", "BET"]` and `["FOLD", "CALL", "RAISE"]`
- [ ] It asserts `expect(numbersOnScreen(container)).toContain(ceiling)` — the sweep is proved to
      find the number, not merely to find nothing wrong
- [ ] `counts a number that reaches the player only as an attribute` passes
- [ ] Both files sweep exactly `min`, `max` and `value`, and neither sweeps `step`
- [ ] `shows no number the turn does not carry` and `shows no number the view does not carry` both
      still pass, unedited and unweakened
- [ ] No file outside `web-client/src/table/` is touched, and no component changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
