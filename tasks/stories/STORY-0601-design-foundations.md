---
id: STORY-0601
title: Establish the design foundations — tokens and preview cards
type: story
status: ready
parent: EPIC-06
module: design
labels: [design]
depends_on: []
---

## Goal

The design language exists as one canonical token sheet (`design/tokens/tokens.css`) and three
foundation preview cards — colors, type, spacing — reviewable in the claude.ai/design project,
with the sync workflow written down. After this story, every later design in the epic inherits
its values from the token sheet instead of choosing new ones.

## Why

First story of `EPIC-06`: screens and graphics compose these tokens, so they exist first. It
runs now, in parallel with `EPIC-02`, so the look is settled before `EPIC-03` builds screens.

## Design notes

- Direction per the epic: Lichess-not-casino, dark-first, quiet. One steel-blue accent.
  Classic red/black suits on a warm paper-white card face — the brightest surface anywhere.
  The duel coin is steel, never gold. Amber exists only as the turn-timer warning.
- Token names carry the `--pd-` prefix. Dark palette only; light is out of scope epic-wide.
- Preview cards are self-contained HTML: the **first line** is `<!-- @dsCard group="…" -->`
  (the claude.ai/design pane indexes cards from that marker), all styles inline, no external
  requests. Each card inlines the token values it demonstrates; `tokens.css` stays canonical,
  and each card's verify greps pin the token *names* so a rename cannot drift silently.
- The claude.ai/design project is **Poker Duels**, id
  `f943b442-533a-4a81-b9f9-99c8a348b524`. The sync procedure lives in `design/README.md`
  and is executed with the `DesignSync` tool from a Claude session.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-060101](../tasks/TASK-060101-design-token-sheet.md) | The canonical design token sheet | done |
| [TASK-060102](../tasks/TASK-060102-colors-preview-card.md) | The Colors preview card | done |
| [TASK-060103](../tasks/TASK-060103-type-preview-card.md) | The Type preview card | done |
| [TASK-060104](../tasks/TASK-060104-spacing-preview-card.md) | The Spacing preview card | done |
| [TASK-060105](../tasks/TASK-060105-design-directory-readme.md) | The design directory README and sync procedure | done |
| [TASK-060106](../tasks/TASK-060106-token-name-drift-check.md) | The token-name drift check | done |
| [TASK-060107](../tasks/TASK-060107-align-the-shipped-foundation-cards.md) | Align the shipped foundation cards | done |
| [TASK-060108](../tasks/TASK-060108-card-surface-tokens.md) | The card resting shadow and back texture become tokens | done |
| [TASK-060110](../tasks/TASK-060110-no-bare-suit-anywhere.md) | No bare suit glyph anywhere, enforced in the drift check | done |
| [TASK-060111](../tasks/TASK-060111-drift-check-compares-values.md) | The drift check compares values, not only names | ready |
| [TASK-060112](../tasks/TASK-060112-drift-gate-reads-the-graphics.md) | The drift gate reads the graphics | backlog |
| [TASK-060113](../tasks/TASK-060113-inlined-symbols-match-their-canonicals.md) | The gallery's inlined symbols match their canonicals | backlog |
| [TASK-060114](../tasks/TASK-060114-lockup-constants-join-the-drift-gate.md) | The lockup constants join the drift gate | backlog |
| [TASK-060115](../tasks/TASK-060115-the-coin-glint-is-born-on-the-sheet.md) | The coin glint is born on the sheet | ready |
| [TASK-060116](../tasks/TASK-060116-the-css-coins-consume-the-glint-token.md) | The CSS coins consume the glint token | backlog |
| [TASK-060117](../tasks/TASK-060117-the-colors-cards-coin-matches-the-canonical.md) | The Colors card's coin matches the canonical | backlog |

## Acceptance criteria

- [x] The three foundation cards render in the claude.ai/design Design System pane, grouped
      Colors / Type / Spacing.
- [x] The human has seen them there and signed off on the direction.
      *(Sign-off recorded 2026-08-15, in-session, after the 14-card sync.)*
- [ ] `design/README.md` lets a fresh session repeat the sync without rediscovering anything.

## Out of scope

- Component designs — buttons, inputs, seat plates — arrive with the table screen story.
- Any web-client code: `EPIC-03`.
- A light palette, print styles, marketing pages.
