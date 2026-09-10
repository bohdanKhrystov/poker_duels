---
schema: 2
id: TASK-121607
title: The chip flight is read from the animation, not the frame
type: task
status: ready
parent: STORY-1216
module: qa
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [qa, harness, motion]
depends_on: []
verify:
  - node scripts/qa/drive.mjs --selftest
  - node scripts/qa/drive.mjs --selftest-animations
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`scripts/qa/drive.mjs` gains a verb that reports the animations running on a page at the instant it
is asked, so a motion too short to screenshot can be observed rather than assumed.

## Why this exists

`EPIC-13`'s Definition of done has exactly one open box, and it has been open since 2026-09-05. Four
of its five clauses were observed on that drive at `866ebc21` across 31 hands — the acting seat's
outline and `YOUR TURN`/`THEIR TURN`, the countdown and timebank ticking without a gap against
wall-clock, `Your rival | Call 100` and the bare `Fold`/`Check` marks, and `Pot 150` driving the
sizing row's `pot` preset from `Raise to 200` to `Raise to 300`.

The fifth clause — **chips that move** — is not drivable that way, and the box says so in its own
words:

> `--pd-motion-chip-flight` is a 420 ms one-shot replayed on a key change, which a screenshot
> cadence cannot reliably sample, so its absence from a frame log is **not evidence of absence**.
> […] A future driver can read `document.getAnimations()` immediately after an act instead.

That sentence is this ticket. The box currently rests on the human's eye at
[`design/components/seat-and-pot.html`](../../design/components/seat-and-pot.html), which genuinely
runs the motion — `ADR-0115` §4's own route, and a legitimate one. This adds the mechanical route
beside it so the box can close without waiting on a person.

## Scope

One verb in `scripts/qa/drive.mjs`, in the shape its existing verbs take.

`animations <cdpPort>` evaluates `document.getAnimations()` in the page and prints, per animation,
enough to identify it: the `animationName` (or the `id`), the `currentTime`, and the effect target's
class list or a stable selector. Nothing is asserted here — this ticket ships an **instrument**,
and a drive reads it.

`--selftest-animations` proves the verb against a fixture page this self-test creates, with a known
short animation, in the shape `delay.mjs`'s self-tests take: hermetic, no product, no daemon.

## Out of scope

- **Ticking `EPIC-13`'s Definition-of-done box.** This ticket ships the instrument; the drive that
  reads it and the box that records it are separate, and the box may still be closed by the human's
  eye first — `ADR-0115` §4 leaves that route open and this does not retire it.
- **Asserting anything about `--pd-motion-chip-flight` itself.** No gate in this ticket names that
  token. A gate that pinned a motion's presence would be a coverage claim about the product, which
  `ADR-0089` §§2b–2c refuses from a browser.
- **`design/components/seat-and-pot.html`** and every other card.
- **The reduced-motion form.** `ADR-0115` decides it; observing it is a drive's business.

## Files

| File | Change |
| --- | --- |
| `scripts/qa/drive.mjs` | the `animations` verb, and `--selftest-animations` |

## Tests

| Assertion | What it rejects |
| --- | --- |
| the verb names a running animation on the fixture | a verb that prints an empty list whatever the page is doing |
| the verb prints an **empty** list on a fixture with no animation | a verb that reports something regardless — the positive control's twin |

**The mutation to probe, and report:** make the verb return `[]` unconditionally. The first
assertion must go red. If it does not, the assertion is checking the harness rather than the page —
which is the exact defect that produced `STORY-1310`'s rejected `P3` row, where every reading was
equally true of a page nothing had touched.

Back the file up with `cp` and restore with `cp`, never `git checkout --`.

## What this unblocks

`EPIC-13`'s last Definition-of-done box, by giving it an instrument that does not depend on catching
a 420 ms window with a screenshot.
