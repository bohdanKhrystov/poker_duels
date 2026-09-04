---
schema: 2
id: TASK-130908
title: The four states are each reachable on the table, and reaching zero changes nothing else
type: task
status: backlog
parent: STORY-1309
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, table, clock]
depends_on: [TASK-130907]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 39) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx -t "changes nothing a player reads when the countdown reaches zero" 2>&1 | awk '/^ *Tests +[0-9]+ passed/ { n = $2 } END { exit !(n >= 1) }'
  - awk '{ n += gsub(/text-warn/, "&") } END { exit (n != 1) }' web-client/src/table/DuelTable.test.tsx
  - awk '{ n += gsub(/text-accent/, "&") } END { exit (n != 1) }' web-client/src/table/DuelTable.test.tsx
  - awk '{ n += gsub(/text-text-faint/, "&") } END { exit (n != 1) }' web-client/src/table/DuelTable.test.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Each of the four states `STORY-1307`'s card drew — *regular*, *running out*, *on timebank*,
*expired* — is reachable on the assembled table and asserted there by name, and a countdown that
has reached zero with no server frame behind it changes nothing else on the screen.

## Why this is its own ticket, and why it is the sharp one

`TASK-130903` proved the four treatments as a function of two numbers, and `TASK-130906` proved each
treatment reaches a class. Neither proves the **table** reaches all four, and a wiring that pinned
one treatment would pass both.

The last test is the one place a client could assert a game fact. `ADR-0113` §6, and `ADR-0108` §5
before it: *"A countdown whose deadline has passed and whose expiry frame has not arrived holds at
zero and does nothing … reaching zero enables no control, sends nothing, marks no hand lost, assumes
no act."* The enforced expiry trails the visible zero by up to a sweep period plus latency
(`ADR-0113` §5), so **this is not an edge case — it is a second that happens on every timeout**, and
what the screen must do in it is nothing.

Asserting *nothing changed* by naming the things that did not change is how such a test rots: it
passes for whatever the author happened to list. So the test compares the **whole rendered tree**
before and after, with the clock's own spans removed from both, and requires them to be identical.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/DuelTable.test.tsx` | modify |
| `web-client/src/table/turn-clock.ts` | read |
| `design/components/seat-and-pot.html` | read — the four rows and their figures |

## Scope

- **Four added tests, no source change.** `DuelTable.tsx`, `SeatPlate.tsx` and `turn-clock.ts` are
  all merged and correct; if one of these tests fails, the repair is a ticket against that file and
  not an edit here.
- **Each treatment's class appears exactly once in the test file**, gated — so the four tests
  cannot collapse into one that asserts the same class four times.
- **Every figure below is the card's**, so a wrong shape fails against the drawing.
- **The zero test states time; it does not spend it.** Two renders at two stated readings against
  the same anchored clock. No timer, real or fake, is installed.

## Out of scope

- **The store's tick.** Merged in `TASK-130905`; this file states its readings directly.
- **`Lobby`.** The end-to-end tick under a real store is `TASK-130910`.
- **The action bar.** Whether its controls are live while a clock runs is `DEC-108`'s neighbourhood,
  open and the product owner's, and nothing here touches or answers it.
- **`ActedForAbsent`.** The frame that follows an expiry is the server's, and the table already
  renders its mark (`ADR-0075`, `TASK-130406`); this ticket asserts only that the screen does not
  produce one on its own.

## Tests

`DuelTable.test.tsx` — **4** added to the 35 `TASK-130907` left, so the file reports **39**.

| Test | Proves |
| --- | --- |
| `draws the fresh allowance regular, and its last seconds running out` | at 24 000 ms left the figure is `24` and its span carries the regular colour; at 6 000 ms left the figure is `6` and its span carries `text-warn`. The card's rows A and B, on the assembled table |
| `draws the bank's own time on timebank` | past `turnEndsAt` with 167 000 ms of bank left, the figure is `2:47` under `text-accent`, and that seat's own bank reads `Timebank 2:47` — the same string twice, which is exactly what the card draws and the one row where the two figures must agree |
| `draws a spent clock expired, holding at zero` | past `expiresAt`, the figure is `0` under `text-text-faint` and that seat's bank reads `Timebank 0:00` |
| `changes nothing a player reads when the countdown reaches zero` | one clock, two renders — one at `expiresAt − 3 000` and one at `expiresAt + 9 000`, with **no frame in between**. Strip every element carrying a clock colour class from both trees and assert the remaining `innerHTML` is byte-identical: the stacks, the pot, the street, the board, the seat statuses (`Their turn` still, because the server still says so), both banks' labels and every mark are unmoved. Then assert positively that the second tree contains no `/The server (folded|checked)/` text — the screen never invents the act |

## Acceptance criteria

- [ ] `DuelTable.test.tsx` reports at least **39** passing tests and none failing
- [ ] `changes nothing a player reads when the countdown reaches zero` passes when run alone by name
- [ ] Each of the other three tests above passes, by name
- [ ] `DuelTable.test.tsx` names `text-warn`, `text-accent` and `text-text-faint` exactly once each
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
