---
schema: 2
id: TASK-120909
title: The away countdown takes the shape ADR-0046 §3 names
type: task
status: done
parent: STORY-1209
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [qa, uat, bug, medium]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/PresenceNotice.test.tsx 2>&1 | grep -qF "the countdown is separated from the line it counts under"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/PresenceNotice.test.tsx 2>&1 | grep -qF "the countdown carries the numeral shape ADR-0046 fixes"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/table/PresenceNotice.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The grace countdown reads as a number under a sentence, in the shape
[`ADR-0046`](../../docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md) §3 fixes, instead of
a bare integer glued to a full stop.

## The defect

Round 1 of `/qa-cycle uat regression`, 2026-08-30, commit `c05ee695`. Reported under check **a**
with the observer's own uncertainty attached; **adjudicated at triage as check (c)**, because
`design/screens/duel-table-states.html` draws no away frame at all, so there is no card content to
contradict. Its real merged source is the ADR.

**Shipped**, read from the DOM:

    <p>Your rival is away. The duel is paused.<span class="font-mono tabular-nums">50</span></p>

which a player reads as **`Your rival is away. The duel is paused.57`** — zero whitespace, unlike
`committed <span>100</span>` elsewhere on the same screen, which is preceded by a literal space.

**`ADR-0046` §3 legislates the numeral and the client does not follow it:**

> It starts from the frame's `graceRemainingMillis` … counts down in whole seconds, reaches zero
> and stays there. **It carries no word of its own** — `The duel is paused.` above it is its label —
> and there is no second string for zero. … **The design fixes the numeral's shape (`0:45`, `45s`)**.

A bare `50` is neither `0:45` nor `45s`, and the missing separator makes the sentence read as a
number attached to its own punctuation. `PresenceNotice.tsx:37–45` renders `{presenceLine(…)}`
immediately followed by the `<span>`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/PresenceNotice.tsx` | modify |
| `web-client/src/table/PresenceNotice.test.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify | `cd web-client && NO_COLOR=1 npm run --silent check` — updates to expected countdown values to match the new format with `s` suffix |

## Scope

- **Give the countdown one of the two shapes `ADR-0046` §3 names**, and separate it from its label,
  so the line reads as a sentence with a number under it.
- **Nothing else in `ADR-0046` §3 moves**: no word of its own, no second string at zero, no colour
  change, no sound, and it still stops at zero and stays there.
- The three presence lines themselves are `presence-text.ts`'s and are correct; they are not touched.

## Out of scope

- **`design/screens/duel-table-states.html`'s missing away and back frames.** The card draws
  waiting, showdown-win and fold-win and nothing for away or back. That is card composition owed to
  `ADR-0091` §5's retrofit story, named in `TASK-120911`'s *Out of scope*, and it is not repaired
  from a bug ticket.
- **`presence-countdown.ts`'s arithmetic.** `secondsRemaining` is correct; its rendering is not.

## Tests

`PresenceNotice.test.tsx`

| Test | Proves |
| --- | --- |
| `the countdown is separated from the line it counts under` | the rendered text of the notice does not contain `paused.` immediately followed by a digit — the exact string a player read |
| `the countdown carries the numeral shape ADR-0046 fixes` | over **two** different remaining values, the rendered numeral matches the chosen shape in both — one value cannot tell a format from a constant |

## Acceptance criteria

- [ ] `PresenceNotice.test.tsx > the countdown is separated from the line it counts under` passes
- [ ] `PresenceNotice.test.tsx > the countdown carries the numeral shape ADR-0046 fixes` passes over
      two distinct remaining values
- [ ] Reverting `PresenceNotice.tsx` alone reddens both
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
