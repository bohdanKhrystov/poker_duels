---
id: STORY-0604
title: Design the duel flow — create, join, result, rematch
type: story
status: ready
parent: EPIC-06
module: design
labels: [design]
depends_on: [STORY-0601, STORY-0602]
---

## Goal

The v0.1 loop outside the table exists as screens: create a duel and share its link, take
the offered seat, see the duel end with its coin, and offer the rematch — all composed from
the merged tokens and components.

## Why

The success condition is one sentence: *send a link, she opens it in a browser, we play,
someone wins, we hit Rematch* (docs/vision.md). The table (STORY-0602) covers "we play";
this story draws everything around it.

## Design notes

- Vocabulary is duelling, never gambling: *challenge, duel, rematch, streak*. The room code
  is the invite (`ADR-0022`) — the create screen shows the code huge and typable, the link
  beside it, one copy action.
- Join is one decision: the room found, who waits there, one accent action ("Take the
  seat"). No forms; a display name, once chosen, is permanent and simply shown
  (`ADR-0029`, amending `ADR-0021`) — what renders for a null name, and how a nameless
  player picks one, is `STORY-0411`'s call, not these cards'.
- Duel end: "Victory" / "Defeat" in the hero sizes, the winner's coin +1 — the coin
  counts duels won, no debit exists (`docs/vision.md`) — hands played, and the one
  accent action — Rematch. Defeat is stated in `--pd-loss`, never softened.
- Rematch needs both seats (`STORY-0206`): show mine-offered ("Rematch offered — waiting
  for ImKate"), theirs-offered ("ImKate offers a rematch"), both states quiet.
- Waiting-for-opponent on the create screen is the empty table's seat: dashed outline plus
  the code, nothing animated.
- Cards are group `Screens`; conventions per `design/README.md`; every value traces to the
  sheet (check-drift must pass).

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-060401](../tasks/TASK-060401-create-and-share-screen.md) | The create-and-share screen | done |
| [TASK-060402](../tasks/TASK-060402-join-screen.md) | The join screen | done |
| [TASK-060403](../tasks/TASK-060403-duel-end-screen.md) | The duel-end screen | done |
| [TASK-060404](../tasks/TASK-060404-rematch-states-card.md) | The rematch states | done |
| [TASK-060405](../tasks/TASK-060405-flow-vocabulary-earns-a-component-card.md) | The flow vocabulary earns a component card | done |
| [TASK-060406](../tasks/TASK-060406-the-typed-code-door.md) | The typed-code door | done |

## Acceptance criteria

- [ ] The story's five screens — create, join, duel-end, the rematch states, the
      typed-code door — render in the pane under **Screens**.
      *(Ticks when the typed-code door — `TASK-060406` — joins the other four in the
      pane.)*
- [ ] The human has seen them there and signed off.
      *(Partial record 2026-08-15: the four landed screens were signed off in-session
      after the 14-card sync. Ticks only when the human has seen the fifth screen
      there and signed it off.)*
- [ ] The whole loop — create → join → play → end → rematch — can be walked card to card
      with no invented step.

## Out of scope

- Leaderboard, seasons, profiles — v0.2/v0.3 epics.
- Matchmaking of strangers — later; the link is the only door in v0.1.
- Any web-client code — `EPIC-03`.
