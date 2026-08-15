---
id: EPIC-06
title: Design system and art
type: epic
status: done
module: design
labels: [design]
---

## Goal

A visual identity for Poker Duels — design tokens, screen designs and game art — built as
versioned files under `design/` and mirrored to a claude.ai/design project where the look is
reviewed visually. The web client (EPIC-03) consumes these tokens and graphics rather than
inventing its own.

## Why now

EPIC-03 needs a settled look before its screens are built, and design work touches no file the
server epic touches, so it runs in parallel with EPIC-02 without contention. Opened ahead of
its roadmap slot (v0.2) for exactly that reason — the epic row was reserved, the work starts
where it cannot collide.

## Direction

Per `docs/vision.md`: **Lichess, not casino.** Dark, quiet, fast, minimal. The vocabulary is
duelling — *challenge, duel, rematch, rival, streak, season* — never *buy-in, bankroll,
jackpot*. Refused outright: felt green, gold, mahogany, chip-bundle iconography. The playing
cards are the brightest objects on any screen, and the duel coin is steel, not gold.

## Scope

- Design tokens: palette, type, spacing, radii, surfaces — `STORY-0601`.
- The duel table screen design.
- The graphics set: card faces, the duel coin, the wordmark — all SVG.
- Lobby and duel-flow screen designs: create, join, result, rematch.
- The sync workflow between `design/` and the claude.ai/design project.

## Out of scope

- Implementing any of it in the web client — that is EPIC-03.
- A light theme. The product is dark-first; light is a later story here.
- Marketing or landing-page design.

## Ways of working — a recorded deviation

Two norms bend in this epic, deliberately and in the open:

- **Design tasks are worked interactively** in a Claude session, with the human reacting to
  rendered output in claude.ai/design — not dispatched to a coder agent. Taste does not
  survive a verify block. The verify commands still gate structure: markers, token names,
  file presence.
- **Preview cards are display artifacts.** The `S` budget reads as *one card, one file*, not
  ≤120 lines — a self-contained HTML preview does not compress to that.

The review gate holds in adapted form: the real review of a design task is visual, in the
claude.ai/design pane, by the human.

## Stories

| ID | Title | Status |
| --- | --- | --- |
| [STORY-0601](../stories/STORY-0601-design-foundations.md) | Design foundations — tokens and preview cards | done |
| [STORY-0602](../stories/STORY-0602-duel-table-screen.md) | Design the duel table — components and the screen | done |
| [STORY-0603](../stories/STORY-0603-graphics.md) | Draw the graphics — suit glyphs, the duel coin, the wordmark | done |
| [STORY-0604](../stories/STORY-0604-lobby-and-flow.md) | Design the duel flow — create, join, result, rematch | done |

**The epic is closed.** All 43 tickets are merged across four stories, every story is
`done`, and the human signed off the full set in the claude.ai/design pane on
2026-08-15: 16 cards — three foundation, four components, seven screens, a gallery and
the wordmark.

## Definition of done

- [x] Every story is `done` or `dropped`.
- [x] The claude.ai/design project shows the full system: foundations, table, graphics, flow.
      *(16 cards synced 2026-08-15; manifest verified by read-back.)*
- [x] EPIC-03 builds its first screen without inventing a single color or size.
      *(`STORY-0302` is done: `design/tokens/tokens.css` is vendored into the client
      under a byte-identity test, and `color-literals.ts` fails the client check on a
      literal outside the token layer.)*

## Metrics

Closed 2026-08-15. Feeds the Product B case study.

| | |
| --- | --- |
| Tasks completed | 43, across four stories |
| Accepted on first review | 14 of 43 |
| Average review iterations | 1.7 (a second round was the norm; `TASK-060121` took four) |
| Test lines / production lines | n/a — design cards carry no tests; the invariant gate is the test, and it grew from 1 clause to 6 |
| Tasks re-scoped mid-flight | 4 (`TASK-060112` split, `TASK-060114` narrowed, `TASK-060121` re-approached three times, `TASK-060123` unbundled) |
| Manual human edits | 0 design files; the human's contribution was direction and two sign-offs |

**What the reviews caught that the author did not.** A `-1 duel coin` on the duel-end
screen, contradicting the vision's coin-counts-wins rule; a join screen citing
`ADR-0021` after `ADR-0029` had made display names permanent; sub-AA contrast on the
rematch chip; a lockup that grew 5px when it became a state; and three separate
attempts at the value reader that each introduced a worse defect than the one they
fixed — the third caught only because the reviewer ran the gate rather than reading it.

**What it cost to learn.** The run used the `/code-review` multi-angle mechanism on
`review: light` tickets for its first 40 PRs, against `ADR-0007`'s rule that `light`
means one reviewer subagent. That inverted the epic's economics — roughly 40M subagent
tokens, two session-limit outages — and, worse, manufactured work: ten-angle reviews of
one-line tickets produced 12 follow-up tickets that each drew their own review. The last
11 PRs ran the documented mechanism at 20–45k tokens each and still caught real defects.
The lesson for the case study is that review depth is a scope decision, not a quality
dial.
