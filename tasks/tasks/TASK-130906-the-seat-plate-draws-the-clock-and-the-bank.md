---
schema: 2
id: TASK-130906
title: The seat plate draws the countdown and the seat's timebank, and speaks neither
type: task
status: backlog
parent: STORY-1309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, table, clock]
depends_on: [TASK-130905]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 19) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 7) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 30) }'
  - cd web-client && FORCE_COLOR=0 npm run --silent build && grep -rqF '.text-warn{color:var(--color-warn)}' dist/assets && grep -rqF '.text-text-faint{color:var(--color-text-faint)}' dist/assets && grep -rqF '.text-accent{color:var(--color-accent)}' dist/assets
  - awk '{ n += gsub(/aria-label/, "&") } END { exit (n != 1) }' web-client/src/table/SeatPlate.tsx
  - awk '{ n += gsub(/text-warn/, "&") } END { exit (n != 1) }' web-client/src/table/SeatPlate.tsx
  - awk '{ n += gsub(/String.fromCharCode\(0xa0\)/, "&") } END { exit (n != 1) }' web-client/src/table/SeatPlate.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`SeatPlate` renders the two spans `STORY-1307`'s card put on the plate — the countdown at the seat
on turn, and `Timebank 3:00` at every seat — in the treatment the table hands it, printing both and
speaking neither.

## What the card draws, transcribed

| The card | The plate |
| --- | --- |
| `.clock` — mono, tabular, `font-size: 1rem`, `--pd-text` | `font-mono tabular-nums text-large text-text` |
| `.clock.running-out` — `--pd-warn` | `text-warn` in place of `text-text` |
| `.clock.on-timebank` — `--pd-accent` | `text-accent` |
| `.clock.expired` — `--pd-text-faint` | `text-text-faint` |
| `.timebank` — mono, tabular, `0.6875rem`, `--pd-text-muted` | `font-mono tabular-nums text-micro text-text-muted` |
| `Timebank&nbsp;3:00` | `Timebank` + `NBSP` + the figure, `SeatPlate.tsx`'s own runtime-built separator |

**One deviation, named rather than hidden: the card's `.clock` is `font-size: 1rem`, and the type
scale has no `1rem` step** — `--pd-fs-body` is `0.9375rem` and `--pd-fs-large` is `1.125rem`. A raw
length in a Tailwind arbitrary value is the shape `ADR-0091` §4 names as failing, and minting a new
step is *minting*, which `ADR-0091` §3 puts with the human and not in a dispatched ticket. So this
ticket **composes from the settled vocabulary** and takes `text-large`, which keeps the property the
drawing is actually making — the clock is the largest figure on the plate, larger than the name and
larger than the stack. The pane's verdict may overrule it in one line (`ADR-0024` §3,
`ADR-0091` §3's trailing verdict); a rejection is a repair ticket against the card and against this
class, and neither is more than a token's worth of work.

**The plate does not grow.** Its height is set by the name-and-status column — `0.9375rem × 1.5`
plus a `min-h-[1.5em]` status at `0.6875rem` — and a single-line `1.125rem` numeral is shorter than
that. Say so in the PR with a measurement, not a claim (`ADR-0103`, and `ADR-0089` §2b keeps a
browser out of the gates).

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/SeatPlate.tsx` | modify |
| `web-client/src/table/SeatPlate.test.tsx` | modify |
| `web-client/src/table/turn-clock.ts` | read — `SeatClock` and `ClockTreatment` |
| `design/components/seat-and-pot.html` | read |

## Scope

- **One new optional prop:** `clock?: SeatClock | null`, the whole `{ figure, treatment, bank }`
  `turn-clock.ts` returns. Absent or `null` renders neither span, which is what every merged caller
  of `SeatPlate` gets today and is why `no-derivation.test.tsx` and `DuelTable.test.tsx` are pinned
  unmoved below.
- **`figure === null` renders no clock span at all** — not an empty one. A seat that is not on turn
  has no countdown, and the card draws none.
- **`bank === null` renders no timebank span.** Before the first `TurnClock` there is no bank to
  state.
- **The treatment reaches CSS through a side table**, not a chain of ternaries:

  ```ts
  const CLOCK_COLOUR: Record<ClockTreatment, string> = {
    regular: "text-text",
    "running-out": "text-warn",
    "on-timebank": "text-accent",
    expired: "text-text-faint",
  };
  ```

  Exhaustive over the union by construction, which is the house style. `text-warn` appears **once**
  in the file and a gate says so.
- **Placement, from the card's *away, on turn* row**, which is the only row drawing all of them at
  once: the clock goes after the last-act pill and before the button; the timebank goes after the
  button and before the chip pile. Both are children of the existing flex row, the `.last-act`
  idiom the card documents — *"`.who` gives ground by truncating the name first and the plate never
  grows a pixel taller."*
- **The plate keeps exactly one `aria-label` and zero `title`.** Neither span carries a name, a
  label or a tooltip: the numerals are text nodes and nothing else. Gated at one, which is the
  merged `speaks the mark to nobody` test's own count.
- **The non-breaking space is the file's existing `NBSP` constant**, built at runtime by
  `String.fromCharCode(0xa0)`. Do not type the glyph and do not write a `\u` escape — gated at one
  occurrence.
- **No animation, no transition, no `key` that remounts.** `ADR-0115` §3 settles it: *"each
  second's numeral is a step. A smooth sub-second depletion drawn between them is a how, and is
  what a reduced form skips."* `STORY-1307` took that structurally and so does this — the two spans
  carry no motion utility of any kind, so there is nothing for a reduced-motion query to still and
  the at-rest form is the form. Do not add a `key` on the figure either; that is the chip pile's
  remount idiom and it exists to replay a flight.
- **Run `npm run format` before `format:check`** — `prettier-plugin-tailwindcss` reorders class
  lists.

## Out of scope

- **Which seat gets a clock and what it reads.** `turn-clock.ts` decided that in `TASK-130903`;
  this component is handed the answer and never computes one. It must not import `TurnClockState`,
  `secondsRemaining`, `clockFigure` or `bankFigure`.
- **`DuelTable`.** `TASK-130907` passes the prop.
- **The token sheet.** Every class above resolves through a merged `--pd-` token; nothing is minted
  here, and a wrong value is a repair ticket.
- **`seat-status.ts`.** `Away` and `Timed out` keep their seats untouched (`ADR-0046` §1).

## Tests

`SeatPlate.test.tsx` — **4** added to the 15 it has, so the file reports **19**. Each extends the
file's existing `plate()` helper with the new prop.

| Test | Proves |
| --- | --- |
| `draws the countdown the table handed it, in the treatment it named` | `{ figure: "24", treatment: "regular", bank: "3:00" }` puts `24` on the plate under `text-text`; `{ figure: "6", treatment: "running-out" }` puts `6` under `text-warn`. Two treatments, because one cannot tell a side table from a constant |
| `draws the other two treatments too` | `on-timebank` gives `text-accent` and `expired` gives `text-text-faint`, so all four of the card's states are reachable from this component |
| `draws the bank behind the word Timebank, with a non-breaking space` | with `bank: "3:00"`, `getByText("Timebank 3:00")` resolves — the separator asserted as the character, not as a space |
| `draws neither span when the table handed it no clock` | with the prop absent: no element matching `.text-warn`, no text matching `/Timebank/`, and `getByText("500")` — the stack — still resolves. The plate keeps exactly one `[aria-label]` and zero `[title]` |

The 15 merged tests do not move; `speaks the mark to nobody` in particular still counts one
`aria-label` and zero `title`, and it must, because these two spans deliberately carry neither.

`no-derivation.test.tsx` (7) and `DuelTable.test.tsx` (30) are pinned unmoved: both render
`DuelTable` without the new prop, which reaches `SeatPlate` as `undefined` and draws nothing.
**Measured, not assumed** — a plant of two unconditional spans (`24` and `Timebank 3:00`) into this
component reddened exactly two tests, both in `no-derivation.test.tsx`, and nothing else in the
1 062-test suite.

## Acceptance criteria

- [ ] `SeatPlate.test.tsx` reports at least **19** passing tests and none failing
- [ ] Each of the four tests above passes, by name
- [ ] `no-derivation.test.tsx` still reports at least **7** passing and none failing
- [ ] `DuelTable.test.tsx` still reports at least **30** passing and none failing
- [ ] `SeatPlate.tsx` contains exactly one `aria-label`, exactly one `text-warn` and exactly one
      `String.fromCharCode(0xa0)`
- [ ] The production bundle emits `.text-warn{color:var(--color-warn)}`,
      `.text-text-faint{color:var(--color-text-faint)}` and `.text-accent{color:var(--color-accent)}`
      — the rules themselves, read out of `dist/assets` **in the same command as the build that
      wrote them**, not the class names in source and not a stale directory. Measured on `develop`:
      `--color-warn` is declared in the theme block and used by nothing, so no `.text-warn` rule is
      emitted today and this gate is red until the plate uses it
- [ ] `cd web-client && npm run check` and `npm run build` both exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
