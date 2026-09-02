---
id: STORY-1215
title: The duel table's last sub-pixel, and the headroom it buys
type: story
status: done
parent: EPIC-12
module: web-client
labels: [design, layout, R2, process]
depends_on: []
---

## Goal

The duel table's document height at 390 × 664 is a **true** `≤ clientHeight`, with room to spare,
so no future fraction can flip a met `R2` reading into a filed defect and no round ever has to run
[`ADR-0106`](../../docs/adr/ADR-0106-a-sub-pixel-residual-is-a-fit-and-one-pixel-is-the-fence.md)
§5's second read.

## Why

`TASK-121402` took 48 px of duplicated outer padding off `main` and left the document at a true
**664.90625 px** against 664 — `scrollHeight` reads 665. `ADR-0106` §1 rules that a true excess
strictly under one CSS pixel **is a fit**, so that ticket merged as scoped and **this is not a
defect**. §4 files the remainder once anyway, and says what it is worth:

- the column stands **0.09375 px** from the fence, so a line-height, a padding step or a font
  metric added anywhere in its five children turns a met reading into a filed `R2` defect with
  nobody touching layout, and no compile-time gate can see it coming;
- and at a true fit the tolerance costs nothing to operate — the integers read `≤` and stop.

**Not a round story, and no due date.** Nothing here was filed by a round: `EPIC-12` §Termination
rule 1 puts it in the ordinary backlog the same way it put `STORY-1214` there, so `STORY-1213`'s
`A(1) = 3` and its verdict stand unrecomputed and no `A(N)` or `B(N)` moves when this lands. It
sits here rather than under `STORY-1214` because that story is `done` and this work is not its
subject.

## Design notes

Everything below is merged and is not re-litigated by the ticket:

- **The residual is not a defect** (`ADR-0106` §1), and **the fence is one CSS pixel** that no
  round, ticket, review, triage or vision-derived ADR may widen (§2). Nothing in this story
  touches either.
- **The closer spends `ADR-0103` §3.1 whitespace and nothing further down the give list**
  (`ADR-0106` §4). The rival's face-down hand, the hero's hole cards and the board keep every
  number they have; advancing a merged give order over 0.9 px is the disproportion the order
  exists to prevent.
- **If §3.1 cannot yield the pixel, the stop rule fires again** and the next `DEC` says so —
  `ADR-0103` §3 and `ADR-0106` §4. That is a legitimate ending for the ticket, not a failure, and
  it is the conduct that earned `TASK-121402` its merge.
- **The mechanism was measured, not guessed** — `ADR-0106` §4 delegates it to the ticket, and the
  measurement disagrees with the ADR's own starting guess. At 390 the `--wgap` clamp's **floor is
  inert**: `100cqi` resolves to 390px, `(390 − 220) / 21.25` is exactly 8, and the computed padding
  stays 8px with the floor at 8px, 4px, 2px or 0px. The **ramp** is what has to move, in the two
  files that transcribe it — the card and the client.
- **The card owns the number**, so `ADR-0103` §4's composing path applies unchanged: both endpoints
  are tokens `design/tokens/tokens.css` already declares, which makes it composing rather than
  minting (`ADR-0091` §3), an ordinary dispatched change whose visual verdict is the human's and
  may trail (`ADR-0024` §3).

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-121501](../tasks/TASK-121501-the-columns-whitespace-gives-one-token-step-at-the-phone.md) | The column's whitespace gives one token step at the phone | done |

## Acceptance criteria

- [ ] At 390 × 664, at the preflop decision beat on the running client,
      `document.documentElement.getBoundingClientRect().height` is `≤ clientHeight` **and**
      `scrollHeight ≤ clientHeight` — both readings pasted into the PR body as text
- [ ] The card and the client state the same `--wgap` rule, gated by `verify:` in both files
- [ ] 720 × 900 still reads `scrollHeight ≤ clientHeight` and the laptop's whitespace is unchanged

## Out of scope

- **The three non-integer heights in `DuelTable.tsx` and `ActionBar.tsx`** that the 0.90625 px is
  made of. They are not a defect and rounding them is not whitespace; no ticket asks for it.
- **Any give below `ADR-0103` §3.1**, and any change to `--bw`, `--miniw` or `--herow`.
- **`DEC-103` and `DEC-104`** — open, the product owner's, untouched.
- **Anything a round would file.** This story runs no round, reports no `A(N)`, and brings no
  stack up beyond the measurement its one ticket names.
