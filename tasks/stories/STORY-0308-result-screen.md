---
id: STORY-0308
title: The result screen — who won, and the coin
type: story
status: backlog
parent: EPIC-03
module: web-client
labels: [client, ui, duel]
depends_on: [STORY-0307]
---

## Goal

`DuelFinished` ends the duel visibly: who won or that it was a draw, the final stacks, how many
hands it took, and the coin that changed hands — and from there, a way back to the lobby.

## Why

A duel that stops without a declared winner is not a duel. `ADR-0017` made the server say when it
ends precisely so the client could show it, and `docs/vision.md`'s success condition is *"someone
wins"*.

## Design notes

- `DuelOutcome` is `{winner: number | null, handsPlayed, finalStacks}`. **`winner: null` is a draw**
  — a real, recordable outcome (`ADR-0015` writes two result rows of zero for it), rendered as a
  draw and never as an error, a missing value or a loss.
- The win/loss reading is `outcome.winner === mySeat`, with `mySeat` from the store. There is no
  other way to know, and comparing stacks to guess is exactly the forbidden kind of derivation.
- **The coin is not on the socket.** The delta is `ADR-0014`'s and can be stated from the outcome —
  winner `+1`, loser `−1`, draw `0` — but the *balance* is the server's: it comes from re-reading
  `GET /api/me` (`STORY-0311`) after the duel ends. The client never increments a counter it holds;
  a locally incremented balance is a client asserting a fact about the economy.
- Vocabulary and look per `docs/vision.md` and `STORY-0601`: *duel, rival, rematch*. The coin is
  steel (`--pd-coin`), never gold. Win and loss use `--pd-win` / `--pd-loss`. No confetti, no
  jackpot, no "you're on fire".
- Rematch belongs to `STORY-0309` and is blocked on `DEC-023`. **Do not stub a rematch button here**
  — a dead control that silently does nothing is worse than an absent one, and a control that fakes
  it with `CreateRoom` loses the button-seat alternation the room owns.
- Until rematch exists, the way on from this screen is back to the lobby, where a new room can be
  created.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0308`.* | — |

## Acceptance criteria

- [ ] `winner === mySeat` renders a win, `winner === null` renders a draw, and any other seat
      renders a loss — three named tests, one per case.
- [ ] Hands played and both final stacks render exactly as sent.
- [ ] The coin delta rendered is `+1` for the winner, `−1` for the loser and nothing for a draw.
- [ ] No balance is displayed that the client computed itself; the balance shown is the one the
      profile read returned.
- [ ] From the result screen, the lobby is reachable and creating a new room works.

## Out of scope

- Rematch — `STORY-0309`, blocked on `DEC-023`.
- Hand history, replay, "you should have called" — `EPIC-08`.
- Rating changes, leaderboard position — `EPIC-05`.
- Sharing a result, screenshots, social anything.
