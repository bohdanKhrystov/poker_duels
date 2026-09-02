---
schema: 2
id: TASK-130102
title: The never-derives guard admits one named sum and no other
type: task
status: ready
parent: STORY-1301
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, table, guard]
depends_on: [TASK-130101]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/no-derivation.test.tsx 2>&1 | grep -qE "Tests +7 passed \(7\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/no-derivation.test.tsx 2>&1 | grep -qF "admits the pot total and still refuses a second derived figure"
  - awk 'index($0, "PotStrip") { n++ } END { exit (n != 0) }' web-client/src/table/no-derivation.test.tsx
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 npm run --silent build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`no-derivation.test.tsx` admits **exactly one** derived quantity — the pot total
[`ADR-0107`](../../docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md) §1 defines — and
goes on failing for any second, so the guard is narrowed by the sentence that narrowed it and not by
a hole.

## Why this is its own ticket, and what the order costs

**The guard reddens the instant the strip prints the total.** Measured 2026-09-02 by applying the
sum to `PotStrip.tsx` and running the whole client suite:

```
Tests  1 failed | 987 passed (988)
FAIL  src/table/no-derivation.test.tsx > the table renders and never derives >
      shows no number the view does not carry
AssertionError: expected [ 6625 ] to deeply equal []
```

One failure, one number, one file. So the narrowing and the behaviour are either one commit or two
in this order, and they are two because `ADR-0107`'s *Consequences* calls this **"a real weakening
of a test whose whole value was having no exceptions"** — a weakening earns a diff a reviewer can
read on its own, not one buried beside a docstring edit and two new strip tests.

**The cost of that order, said plainly and not hidden:** for the life of one pull request the guard
admits a number nothing on screen produces. The new test below bounds it — it asserts the allowed
set is exactly **one** member wider than the view's own numbers, names which member, and shows a
*different* sum of two view fields still being rejected. It passes before `TASK-130103` and after,
so it is never a placeholder.

**The guard computes the sum itself and never imports it.** A guard that borrowed the production
sum could not catch a wrong one; a `verify:` command refuses the string `PotStrip` anywhere in this
file for exactly that reason.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/no-derivation.test.tsx` | modify |
| `docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md` | read |

## Scope

- Add two file-local helpers above `wordsOnScreen`, with comments that say in the file's own words
  that this is a narrowing and why it is admissible (`ADR-0107` §5 — three server-stated facts, one
  sum a merged ADR already defined, computed on this same screen since `ADR-0100` §6 landed):

  ```tsx
  function potTotal(view: PlayerView): number {
    return view.seats.reduce(
      (sum, seat) => sum + seat.committedThisStreet,
      view.pot,
    );
  }

  function allowedNumbers(view: PlayerView): Set<number> {
    return new Set([...numbersIn(view), potTotal(view)]);
  }
  ```

- In `shows no number the view does not carry`, change **two lines** and nothing else:
  `const allowed = numbersIn(VIEW);` becomes `const allowed = allowedNumbers(VIEW);`, and the
  fixture-independence sweep's input `[...numbersIn(VIEW)].filter((value) => value > 1)` becomes
  `[...allowedNumbers(VIEW)].filter((value) => value > 1)`. The three sweep assertions (`a * 2`,
  `a + b`, `Math.abs(a - b)`) are untouched — the sweep just runs over a set one member wider, which
  is what makes the carve-out *narrow*: it now also proves that **no pair of the fixture's own
  numbers lands on the pot total**, so a second derived figure cannot hide inside the exception.
  Measured: the existing fixture passes the wider sweep with zero violations.

- Add exactly one test to the same `describe`, `admits the pot total and still refuses a second
  derived figure`, with its own small fixture. Written out because every number in it is read off
  that fixture:

  ```tsx
  const VIEW: PlayerView = aView({
    pot: 5675,
    betToMatch: 1450,
    seats: [
      aSeat({ index: 0, committedThisStreet: 125 }),
      aSeat({ index: 1, committedThisStreet: 825 }),
    ],
  });

  const allowed = allowedNumbers(VIEW);
  expect(allowed.size).toBe(numbersIn(VIEW).size + 1);
  expect(allowed.has(6625)).toBe(true); // 5,675 + 125 + 825, off the fixture above

  const { container } = render(<DuelTable view={VIEW} />);
  const probe = document.createElement("span");
  probe.textContent = String(VIEW.pot + VIEW.betToMatch); // 7,125 — a second sum
  container.appendChild(probe);

  expect(numbersOnScreen(container).filter((n) => !allowed.has(n))).toEqual([
    7125,
  ]);
  ```

  The probe mirrors the existing `counts a number that reaches the player only as an attribute`,
  which appends a node to prove the sweep's reach rather than to assert something about the table.

- Change nothing else. The other five tests, their fixtures, `numbersIn`, `wordsOnScreen`,
  `numbersOnScreen`, `spokenOnScreen`, `CARD_NAME` and `HAND_TALK` all stay byte-identical.

## Out of scope

- **`PotStrip.tsx` and `PotStrip.test.tsx`.** The behaviour is `TASK-130103`. This ticket must leave
  the strip printing `view.pot`; the whole client suite is green either way, which is the point.
- **Hoisting the four duplicated `VIEW` literals** in this file into one module constant. Tempting
  while you are here and a much larger diff than the narrowing; not ticketed.
- **Widening the carve-out to "any sum of view fields".** `ADR-0107` §5 admits *one named quantity*.
  A second one needs a new ADR, and the new test exists to fail if one arrives.
- **`GameState.potTotal` on the wire**, which would remove the need for the carve-out entirely.
  `ADR-0107` §5 leaves it to an architect and `EPIC-13` opens no engine.

## Tests

`the table renders and never derives` — `web-client/src/table/no-derivation.test.tsx`

| Test | Proves |
| --- | --- |
| `admits the pot total and still refuses a second derived figure` | `allowedNumbers` is exactly one member wider than `numbersIn`, that member is `6625` (= 5,675 + 125 + 825 off its own fixture), and a *different* sum of two view fields — `pot + betToMatch` = 7,125 — is still reported as not allowed |
| `shows no number the view does not carry` *(amended)* | Unchanged in intent: every figure on screen is a view field or the one admitted sum. The independence sweep now runs over the allowed set, so it additionally proves no pair of the fixture's numbers doubles, sums or differences onto the carve-out |

The other five tests in the file are untouched and must go on passing unchanged.

**The count in `verify:` is 7 and was measured, not computed**: `no-derivation.test.tsx` runs 6 tests
today (measured 2026-09-02), this ticket adds exactly one, and the amended file was run at 7 passing
before this ticket was written.

## Acceptance criteria

- [ ] `admits the pot total and still refuses a second derived figure` passes
- [ ] `shows no number the view does not carry` passes with `allowedNumbers(VIEW)` as both its
      allowed set and its sweep input
- [ ] `no-derivation.test.tsx` reports exactly `Tests 7 passed (7)`
- [ ] The string `PotStrip` appears nowhere in `no-derivation.test.tsx` — the guard computes the sum
      it guards
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
