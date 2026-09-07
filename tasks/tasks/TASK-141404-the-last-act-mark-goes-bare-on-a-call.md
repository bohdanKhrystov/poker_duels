---
schema: 2
id: TASK-141404
title: The last-act mark goes bare on a call
type: task
status: done
parent: STORY-1414
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, table]
depends_on: [TASK-141403]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/action-text.test.ts > "${TMPDIR:-/tmp}/mark-text-after.txt" 2>&1; grep -qF 'Tests  11 passed (11)' "${TMPDIR:-/tmp}/mark-text-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx > "${TMPDIR:-/tmp}/plate-after.txt" 2>&1; grep -qF 'Tests  19 passed (19)' "${TMPDIR:-/tmp}/plate-after.txt"
  - cd web-client && cp src/table/action-text.ts "${TMPDIR:-/tmp}/mark.fixed.ts" && perl -0pi -e 's/actionVerb\("CALL"\), amount: null/actionVerb("CALL"), amount: event.to/' src/table/action-text.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/action-text.test.ts > "${TMPDIR:-/tmp}/mark-text-before.txt" 2>&1; FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx > "${TMPDIR:-/tmp}/plate-before.txt" 2>&1; cp "${TMPDIR:-/tmp}/mark.fixed.ts" src/table/action-text.ts; cmp -s "${TMPDIR:-/tmp}/mark.fixed.ts" src/table/action-text.ts && grep -qF "FAIL  src/table/action-text.test.ts > the action text > says Call for a call, bare" "${TMPDIR:-/tmp}/mark-text-before.txt" && grep -qF 'Tests  1 failed | 10 passed (11)' "${TMPDIR:-/tmp}/mark-text-before.txt" && grep -qF "FAIL  src/table/SeatPlate.test.tsx > a seat plate > prints a fold, a check and a call bare" "${TMPDIR:-/tmp}/plate-before.txt" && grep -qF 'Tests  1 failed | 18 passed (19)' "${TMPDIR:-/tmp}/plate-before.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The table's last-act mark says `Call`, bare — it joins `Fold` and `Check` and carries no figure —
because the button now names a price that `PlayerCalled` cannot carry. `Bet`, `Raise to` and
`All in` marks keep their totals and still agree with their buttons figure for figure.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/action-text.ts` | modify |
| `web-client/src/table/action-text.test.ts` | modify |
| `web-client/src/table/SeatPlate.test.tsx` | modify |

Read [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md) §4 —
it is the whole specification — and
[`ADR-0109`](../../docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md) §2,
the clause it amends. **Nothing else is opened.**

## Scope

- **One arm of `lastActText`:**

  ```ts
      case "PlayerCalled":
        return { verb: actionVerb("CALL"), amount: null };
  ```

  Write it in that spelling: the fourth `verify:` command reintroduces the defect by matching
  exactly `actionVerb("CALL"), amount: null`. The `PlayerFolded`, `PlayerChecked`, `PlayerBet`,
  `PlayerRaised` and `PlayerAllIn` arms are byte-unchanged.
- **`lastActText`'s KDoc is corrected**, because it currently states the rule this ticket breaks —
  *"the figure is the event's own total for a call, bet, raise or all-in, and null for a fold or
  check"*. Words to this effect, the sentences yours and the facts not: the figure is the event's
  own total for a **bet, raise or all-in** and null for a **fold, a check and a call**; `ADR-0109`
  §2's rule — *the mark says what the actor's own button said, no more and no less* — is what
  **forces** this and is what survives it; and the mark cannot print the price because
  `PlayerCalled` carries only `sequence`, `seat` and `to`, and after the act the caller's
  `committedThisStreet` **is** `to`, so the term the subtraction needs is gone by the time the mark
  exists (`ADR-0122` §4).
- **`SeatPlate.tsx` is not opened and does not change.** It already renders
  `act.amount === null ? act.verb : \`${act.verb}${NBSP}${formatChips(act.amount)}\`` (`:62-68`), so
  a null amount is drawn bare by the code that already draws `Fold` and `Check` bare. If your diff
  contains `SeatPlate.tsx`, something has been widened.
- Run `npm run format` before `npm run check`.

## Out of scope

- **`actionText`**, in the same file. `TASK-141402` settled the button; this ticket must leave its
  `CALL` arm, its parameters and its KDoc byte-identical.
- **The wire.** `PlayerCalled` gains no field, no `PROTOCOL_VERSION` step, no server or engine file.
  `ADR-0122` §Consequences names that field as the **reversal path** if the bare mark reads worse in
  the hand than it does on paper, and deliberately does not register it (`ADR-0105` §6).
- **`DuelTable.test.tsx`.** Its `heroCall` fixture (`:289`) asserts the mark's **placement** and that
  `950` is gone — never the mark's text — so it is green before and after and must not be edited.
  It was checked, not assumed.
- **The design cards.** `TASK-141401` already drew this; that is `ADR-0091` §2's *before or with*.
- **`ADR-0109`'s own file and its index row** — `TASK-141405`.

## Tests

No test is added or removed: `action-text.test.ts` holds **11** before and after, and
`SeatPlate.test.tsx` holds **19**. Two merged tests move, and they are the two — counted, not
guessed — whose assertions this change invalidates.

`action-text.test.ts` → `describe("the action text")`

| Test | Change |
| --- | --- |
| `says Call with the call's own total` → **`says Call for a call, bare`** | The same two `lastActText` calls, at `to: 400` and `to: 925`, now both expect `{ verb: "Call", amount: null }`. Two **different** totals, so a mark that echoed one hard-coded figure would still fail. The title moves because it states the opposite of the decision |

`SeatPlate.test.tsx` → `describe("a seat plate")`

| Test | Change |
| --- | --- |
| `prints a fold and a check bare` → **`prints a fold, a check and a call bare`** | The fold and check blocks are unchanged. After them, `unmount()` the check's render and add a loop over `to` of `[400, 925]` rendering `{ type: "PlayerCalled", sequence: 1, seat: 0, to }`: each renders exactly one `.last-act`, its `textContent` is the string `"Call"`, and it matches no `/\d/` — the assertion that actually forbids a figure. `unmount()` inside the loop, because `plate(...)` renders into the document |
| `prints the act's own total on a call, a bet, a raise and an all-in` → **`prints the act's own total on a bet, a raise and an all-in`** | The `PlayerCalled` row leaves the `cases` table; the bet (`Bet 950`), raise (`Raise to 2,300`) and all-in (`All in 13,400`) rows and every assertion in the loop are otherwise unchanged. Nothing is weakened: the row moved to the test above, which asserts something stronger about it |

**Both must be red against the old behaviour, and the `verify:` block proves it rather than takes
your word for it.** The fourth command copies `action-text.ts` aside, rewrites the `PlayerCalled`
arm back to `amount: event.to`, runs each of the two files separately, restores the file
byte-for-byte (`cmp -s`), and then requires four captured strings: each file's `FAIL` line naming
the test by its full path, and each file's own `Tests  1 failed | …` count. All four were
**measured** by running that probe.

## Acceptance criteria

- [ ] `says Call for a call, bare` passes, asserting `{ verb: "Call", amount: null }` for `to: 400`
      and for `to: 925`
- [ ] `prints a fold, a check and a call bare` passes, asserting `textContent` is `"Call"` and
      matches no digit, at `to: 400` and at `to: 925`
- [ ] `prints the act's own total on a bet, a raise and an all-in` passes with its three remaining
      rows unchanged
- [ ] `action-text.test.ts` holds exactly 11 tests and `SeatPlate.test.tsx` exactly 19, all passing
- [ ] With the `PlayerCalled` arm returning `event.to` again, exactly one test in each file fails —
      asserted by the fourth `verify:` command, which restores the file afterwards
- [ ] `SeatPlate.tsx`, `DuelTable.tsx`, `DuelTable.test.tsx` and `actionText` are byte-identical to
      the commit this ticket branches from
- [ ] `lastActText`'s KDoc no longer says the figure is the event's total for a call, and names
      `ADR-0122` §4
- [ ] `cd web-client && npm run --silent check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
