---
schema: 2
id: TASK-041315
title: The show-more button says what the copy module says
type: task
status: done
parent: STORY-0413
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, history, copy, regression]
depends_on: [TASK-041312]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers another page in the words the copy module holds'
  - cd web-client && npm run check
---

## Goal

`history-text.ts` exports `MORE = "Show more"`. `HistoryScreen.tsx` renders the bare identifier as
JSX text:

```tsx
<button type="button" …>
  MORE
</button>
```

JSX treats that as the literal string `MORE`, not a reference — so the player reads **MORE** and the
copy module's sentence never reaches a screen. Shipped in `TASK-041310`.

**Nothing caught it, and the reason is worth recording.** Every test queries the control by
`{ name: "MORE" }`, which is exactly what the button renders, so the tests agree with the defect.
`TASK-041305`'s literal-assertion discipline covers the copy module and stops at its edge: it proves
`MORE` holds `"Show more"`, and nothing proves anything renders it.

## Scope

- `HistoryScreen.tsx`: render `{MORE}`, importing it alongside the other copy already taken from
  `history-text.ts`.
- `HistoryScreen.test.tsx`: every query for that control names the **copy**, not the identifier. The
  tests that find it by `{ name: "MORE" }` become `{ name: MORE }` — a reference, so they follow the
  constant if it ever changes.

## Out of scope

- Any other control's label. **A refusal, not an omission:** a sweep for hardcoded strings across the
  client is a different ticket with a different shape, and widening this one would hide a one-line
  fix inside it. If such a sweep is wanted, `TASK-041117`'s `nameOrNone` sweep is the pattern.
- The copy itself. `TASK-041305` authored `"Show more"` and this ticket does not reopen it.

## Tests

`HistoryScreen.test.tsx`. One test added; the existing queries change from a literal to a reference.

| Test | Proves |
| --- | --- |
| `offers another page in the words the copy module holds` | The rendered control's accessible name is the **literal** `"Show more"`, written out in the test rather than referenced — so it fails both against today's `MORE` and against a future edit that changes the constant without meaning to. **Fails against** the shipped defect: the button currently reads `MORE` |

Asserting the literal here is deliberate and is `TASK-041305`'s rule applied one layer out: the
module's test proves the constant holds the sentence, and this proves the sentence reaches the
screen. A test written as `{ name: MORE }` alone would pass against a constant changed to anything.

## Acceptance criteria

- [ ] `HistoryScreen.test.tsx` contains a test asserting the control's accessible name is the literal
      `"Show more"`
- [ ] `grep -c '^\s*MORE\s*$' web-client/src/history/HistoryScreen.tsx` returns `0`
- [ ] No other query in the file names the control by the string `"MORE"`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
