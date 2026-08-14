---
id: EPIC-06
title: Design system and art
type: epic
status: ready
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
| [STORY-0601](../stories/STORY-0601-design-foundations.md) | Design foundations — tokens and preview cards | ready |
| [STORY-0602](../stories/STORY-0602-duel-table-screen.md) | Design the duel table — components and the screen | ready |
| [STORY-0603](../stories/STORY-0603-graphics.md) | Draw the graphics — suit glyphs, the duel coin, the wordmark | ready |
| [STORY-0604](../stories/STORY-0604-lobby-and-flow.md) | Design the duel flow — create, join, result, rematch | ready |

All four stories are written. The human's visual sign-off is recorded for
STORY-0601/0602/0603 and partially for STORY-0604 (its fifth screen, the typed-code
door, has not landed); each story flips `done` in the PR that lands its last ticket.

## Definition of done

- [ ] Every story is `done` or `dropped`.
- [ ] The claude.ai/design project shows the full system: foundations, table, graphics, flow.
- [ ] EPIC-03 builds its first screen without inventing a single color or size.

## Metrics

Filled in when the epic closes; feeds the Product B case study.

| | |
| --- | --- |
| Tasks completed | |
| Accepted on first review | |
| Average review iterations | |
| Test lines / production lines | |
| Tasks re-scoped mid-flight | |
| Manual human edits | |
