---
schema: 2
id: TASK-130901
title: presence-countdown.ts becomes countdown.ts, and its citation re-points
type: task
status: done
parent: STORY-1309
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, table, clock]
depends_on: []
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/countdown.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 3) }'
  - sh -c 'test ! -e web-client/src/table/presence-countdown.ts'
  - sh -c 'test ! -e web-client/src/table/presence-countdown.test.ts'
  - sh -c '! grep -rqF "presence-countdown" web-client/src'
  - sh -c 'grep -qF "Math.max(0, Math.ceil((deadlineMillis - nowMillis) / 1000))" web-client/src/table/countdown.ts'
  - sh -c 'grep -qF "ADR-0108" web-client/src/table/countdown.ts'
  - sh -c '! grep -qF "ADR-0028" web-client/src/table/countdown.ts'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`secondsRemaining` lives at `web-client/src/table/countdown.ts` under a name that no longer says
*presence*, with its arithmetic byte-identical and its citation moved to the rule it now serves.

## Why this is exactly `ADR-0113` §7's row

The dismantle table says it in one line: *`web-client/src/table/presence-countdown.ts` — **kept, and
renamed** `countdown.ts` with its test — `secondsRemaining`'s body does not change, and its citation
re-points from `ADR-0028` §3 to `ADR-0108` §5, the same rule at a wider occasion.*

The function's only caller left when `PresenceNotice` stopped counting a grace window
(`TASK-130805`); every ticket after this one in `STORY-1309` calls it about a turn clock instead.
The rename is therefore the whole change: **no line of arithmetic moves**, and a gate compares the
expression as a fixed string so it cannot.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/presence-countdown.ts` → `web-client/src/table/countdown.ts` | rename |
| `web-client/src/table/presence-countdown.test.ts` → `web-client/src/table/countdown.test.ts` | rename |

## Scope

- `git mv` both files. The test's import becomes `from "./countdown"`.
- **The body of `secondsRemaining` does not change.** Signature, `Math.ceil`, the `Math.max(0, …)`
  clamp and the parameter names all stay as they are. A gate greps the expression as a fixed
  string.
- **The KDoc's citation, and only that.** `Zero is not an event (ADR-0028 §3)` becomes
  `Zero is not an event (ADR-0108 §5)`. Rewrite the surrounding sentence only as far as removing
  the word *window* requires — the paragraph explains `Math.ceil`, and that reason is unchanged.
  After this ticket the file names `ADR-0108` and does not name `ADR-0028`, both gated.
- **The three merged tests keep their names and their numbers.** They are about arithmetic, not
  about a grace window, so nothing in them is about to become false.

## Out of scope

- **Any new function.** The clock's figures are `TASK-130902`; which seat draws one and in which
  treatment is `TASK-130903`.
- **Any caller.** Nothing imports this module after the rename either; `TASK-130903` is the first
  to.
- **`PresenceNotice.tsx` and `presence-text.ts`.** The pause's copy is `TASK-130911` and
  `TASK-130912`.

## Tests

`countdown.test.ts` — the three merged tests, moved and not edited beyond the import path.

| Test | Proves |
| --- | --- |
| `counts whole seconds up to the deadline` | 47 000 ms is 47 s, 46 001 ms is still 47, 46 000 ms is 46 |
| `reaches zero and stays there` | at, one millisecond past and ten minutes past the deadline, all 0 |
| `reads both of its arguments` | the same deadline at two different readings gives 47 and 27 |

## Acceptance criteria

- [ ] `countdown.test.ts` reports at least **3** passing tests and none failing
- [ ] `web-client/src/table/presence-countdown.ts` and `presence-countdown.test.ts` no longer exist
- [ ] No file under `web-client/src` contains the string `presence-countdown`
- [ ] `countdown.ts` contains `Math.max(0, Math.ceil((deadlineMillis - nowMillis) / 1000))`
- [ ] `countdown.ts` names `ADR-0108` and does not name `ADR-0028`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
