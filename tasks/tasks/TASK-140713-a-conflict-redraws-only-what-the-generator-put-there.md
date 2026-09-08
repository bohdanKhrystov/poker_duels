---
schema: 2
id: TASK-140713
title: A conflict redraws, and only over what the generator put there
type: task
status: backlog
parent: STORY-1407
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, profile, lobby]
depends_on: [TASK-140712]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && grep -qF 'outcome.kind === "conflict" && value === drawn' src/profile/NameAsk.tsx
  - cd web-client && grep -qF 'suggestName(random, drawn)' src/profile/NameAsk.tsx
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/NameAsk.test.tsx > "${TMPDIR:-/tmp}/ask-after.txt" 2>&1; grep -qF 'Tests  11 passed (11)' "${TMPDIR:-/tmp}/ask-after.txt"
  - cd web-client && cp src/profile/NameAsk.tsx "${TMPDIR:-/tmp}/ask.fixed.tsx" && perl -0pi -e 's{outcome\.kind === "conflict" && value === drawn}{outcome.kind === "conflict"}' src/profile/NameAsk.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/NameAsk.test.tsx > "${TMPDIR:-/tmp}/c1.txt" 2>&1; cp "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx; cmp -s "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx && grep -qF 'Tests  1 failed | 10 passed (11)' "${TMPDIR:-/tmp}/c1.txt" && grep -qF 'never overwrites a name the player typed' "${TMPDIR:-/tmp}/c1.txt"
  - cd web-client && cp src/profile/NameAsk.tsx "${TMPDIR:-/tmp}/ask.fixed.tsx" && perl -0pi -e 's{outcome\.kind === "conflict" && value === drawn}{outcome.kind !== "named" && value === drawn}' src/profile/NameAsk.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/NameAsk.test.tsx > "${TMPDIR:-/tmp}/c2.txt" 2>&1; cp "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx; cmp -s "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx && grep -qF 'Tests  1 failed | 10 passed (11)' "${TMPDIR:-/tmp}/c2.txt" && grep -qF 'redraws on a conflict and on nothing else' "${TMPDIR:-/tmp}/c2.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A name refused as taken costs the player one press and no thought: the field takes a **fresh**
suggestion. But only when the field still holds the string the generator put there — a name the
player typed is never overwritten, and no other outcome redraws anything.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameAsk.tsx` | modify |
| `web-client/src/profile/NameAsk.test.tsx` | modify |

Read [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) §5 and
[`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md) §4.
**Nothing outside the table above is changed.**

## Scope

- **The surface remembers the last string the generator put in the field**, in the state
  `TASK-140711` already created for the draw, and the settle handler reads it:

  ```ts
  if (outcome.kind === "conflict" && value === drawn) {
    const next = suggestName(random, drawn);
    setDrawn(next);
    setValue(next);
  }
  ```

  Two `verify:` gates hold both lines, because both mutations below turn on their exact shape.
- **An equality, not a dirty flag.** `ADR-0137` §5 spells the rule this way on purpose: a flag is
  something somebody has to remember to clear, and a player who edits a suggestion back to exactly
  what it was has, in every sense the product can observe, not edited it.
- **`replacing` is passed**, so `TASK-140707`'s bounded redraw cannot hand back the string that was
  just refused — up to four draws, then the last, and never a spin.
- **`conflict` alone.** `rejected` cannot describe a suggestion at all — `TASK-140702`'s tests make a
  `400` impossible on anything this generator produces — and the other outcomes are not about the
  name. A mutation that widens the condition to *every refusal* reddens the test that says so.
- **The redraw costs nothing on the write budget.** It sends no request: it is a redraw in the
  browser. `ADR-0134` §6 meters the string that was **spent**, not the attempt that was made, so a
  `409` refunds and a player unlucky twice is no closer to a `429` — that clause is `STORY-1409`'s to
  build and nothing here may make it necessary sooner.

## Out of scope

- **A *suggest another* control.** Refused in `STORY-1407`'s *Design notes*: the field is editable,
  and this ticket covers the one case that needs a fresh draw without a press of its own.
- **Storing a suggestion, or remembering the ones already refused.** `ADR-0137` §§5, 8: nothing is
  stored, and the only string the generator avoids is the single `replacing`.
- **Any change to the write path, `set-name.ts` or `name-text.ts`.**

## Tests

`web-client/src/profile/NameAsk.test.tsx`, `describe("the name ask")` — the eight before, unchanged,
plus:

| Test | Proves |
| --- | --- |
| `puts a fresh suggestion in a field the player never touched` | with a scripted source drawing tuple A then tuple B, a `conflict` on the untouched A leaves the field holding exactly B, and B is not A |
| `never overwrites a name the player typed` | the same scripted source, but the player types `Ravenpost` first: after the `conflict` the field still holds `Ravenpost`, and `random` was **not** called again |
| `redraws on a conflict and on nothing else` | an `unavailable` outcome over an untouched field leaves it holding A, and calls `random` no further |

## Acceptance criteria

- [ ] All eleven tests pass: `npx vitest run src/profile/NameAsk.test.tsx` reports
      `Tests  11 passed (11)`
- [ ] The typed-name test asserts both the field's contents **and** that the source was not called
      again — the string alone cannot tell a redraw that happened to produce the same value from no
      redraw at all
- [ ] Dropping `&& value === drawn` fails `never overwrites a name the player typed` and nothing
      else: `Tests  1 failed | 10 passed (11)`, file restored byte-for-byte
- [ ] Widening `outcome.kind === "conflict"` to every refusal fails
      `redraws on a conflict and on nothing else` and nothing else: `Tests  1 failed | 10 passed (11)`,
      file restored byte-for-byte
- [ ] `suggestName(random, drawn)` is what the redraw calls, so the refused string cannot come back
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
