---
id: STORY-0602
title: Design the duel table — components and the screen
type: story
status: ready
parent: EPIC-06
module: design
labels: [design]
depends_on: [STORY-0601]
---

## Goal

The heart of v0.1 exists as a design: the playing-card, seat and action-bar components, and
the duel table screen composed from them — in-play, waiting and showdown — all built from the
merged token sheet and reviewable in the claude.ai/design pane.

## Why

Every other screen is furniture around this one. `EPIC-03` builds the real table from this
design, so the components are drawn before the client exists — and the token sheet
(`STORY-0601`) is exercised for the first time by real composition, which is where a palette
proves itself or breaks.

## Design notes

- Conventions as `STORY-0601` and `design/README.md`: `@dsCard` first line, self-contained,
  inline token copies, name-pinning greps, no external request. Type in rem; focus is the
  outline pair (`--pd-focus` / `--pd-focus-offset`).
- **Playing card**: 5:7 ratio, corner index top-left (rank over suit glyph), three reference
  widths — board 72px, hole 96px, history mini 40px. The back is `--pd-card-back` with the
  diagonal-stripe pattern and inset light border. An undealt slot is a dashed hairline
  outline, so the board always shows its five places.
- **Seat plate**: display name, stack in mono `tabular-nums`, dealer button as a "D" pill.
  On turn: an accent left edge plus micro-caps "YOUR TURN" / "THEIR TURN". In a grace
  window: the plate goes faint with "reconnecting…" (`ADR-0013` vocabulary).
- **Pot strip** between the seats: "Pot 2,450" in mono; "Blinds 75/150 · Hand 14" small
  muted beside it — hand 14 plays level-2 blinds per docs/duel-rules.md.
- **Action bar**: at most three actions, exactly the engine's legal set (no fold exists
  until something is owed) — Check/Call (ghost, amount shown), Bet/Raise (accent fill,
  amount shown), Fold only when facing a bet — beneath a sizing row: min · ⅓ · ½ · pot ·
  all-in chips and a stepper. Both rows are reserved in every state so the bar never
  changes height; disabled is faint; off turn the action row carries "Waiting for
  ImKate…". Every control shows the focus outline.
- **Screen**: one column, max 560px, the page *is* the table (no oval). Opponent top, board
  and pot center, hero's cards bottom, action bar pinned beneath. Works at 360px.
- **States**: a second card shows the waiting frame and the showdown frame — winner banner
  in `--pd-win` ("ImKate wins 4,900 with two pair"), the loser mucking per `ADR-0008`.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-060201](../tasks/TASK-060201-playing-card-component.md) | The playing-card component | done |
| [TASK-060202](../tasks/TASK-060202-seat-plate-and-pot.md) | The seat plate and pot strip | done |
| [TASK-060203](../tasks/TASK-060203-action-bar.md) | The action bar | done |
| [TASK-060204](../tasks/TASK-060204-duel-table-screen.md) | The duel table screen, in play | done |
| [TASK-060205](../tasks/TASK-060205-table-states.md) | The table's other moments — waiting and showdown | done |
| [TASK-060206](../tasks/TASK-060206-action-bar-off-state-parity.md) | The off-state's hidden sizing row mirrors the live content | done |
| [TASK-060207](../tasks/TASK-060207-the-fold-ending.md) | The fold ending — a win with nothing shown | done |
| [TASK-060208](../tasks/TASK-060208-the-in-play-table-shows-the-hidden-hand.md) | The in-play table shows the hidden hand | done |
| [TASK-060209](../tasks/TASK-060209-the-states-mirrors-use-the-live-bars-elements.md) | The states' hidden mirrors use the live bar's elements | done |
| [TASK-060210](../tasks/TASK-060210-screens-consume-card-surface-tokens.md) | The duel-table screens consume the card-surface tokens | ready |

## Acceptance criteria

- [x] The five cards render in the claude.ai/design pane under **Components** and
      **Screens**.
- [x] The human has seen the table there and signed off on it.
      *(Components approved on sight 2026-08-14; full sign-off recorded 2026-08-15,
      in-session, after the 14-card sync.)*
- [ ] Every value on every card traces to `design/tokens/tokens.css`.

## Out of scope

- Lobby, join, result and rematch screens — `STORY-0604`.
- SVG card faces and the coin — `STORY-0603`; the component uses typographic corners.
- Any web-client code — `EPIC-03`.
- Motion/animation — not yet ticketed.
