---
id: STORY-0308
title: The result screen — who won, and the coin
type: story
status: ready
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
- Rematch belongs to `STORY-0309`, which waits on `EPIC-02`'s `STORY-0213` for the frames
  (`DEC-023`, answered by `ADR-0044`). **Do not stub a rematch button here**
  — a dead control that silently does nothing is worse than an absent one, and a control that fakes
  it with `CreateRoom` loses the button-seat alternation the room owns.
- Until rematch exists, the way on from this screen is back to the lobby, where a new room can be
  created.

## Tasks

Split into schema-2 tickets on 2026-08-16, against the owner's finished design
(`design/screens/duel-end.html`) rather than an invented layout. The screen lands in
`web-client/src/result/`, beside `src/lobby/` and `src/table/`, and reaches the store only in the
last ticket. Strictly ordered: every ticket after the first touches a file an earlier one wrote, so
exactly one is startable at a time. Cumulative suite counts **250 → 275**, from a measured baseline
of **247**.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-030801](../tasks/TASK-030801-a-duel-outcome-fixture-with-every-field-the-wire-declares.md) | A `DuelOutcome` fixture with every field the wire declares | ready |
| [TASK-030802](../tasks/TASK-030802-the-verdict-is-read-off-the-winner-and-your-seat.md) | The verdict is read off the winner and your seat, and nothing else | backlog |
| [TASK-030803](../tasks/TASK-030803-the-coin-line-states-the-one-coin-the-duel-moved.md) | The coin line states the one coin the duel moved, and no balance | backlog |
| [TASK-030804](../tasks/TASK-030804-the-coin-mark-is-steel-and-says-nothing.md) | The coin mark is steel, and says nothing a screen reader has to hear twice | backlog |
| [TASK-030805](../tasks/TASK-030805-the-result-screen-declares-the-verdict-and-the-coin.md) | The result screen declares the verdict and the coin beside it | backlog |
| [TASK-030806](../tasks/TASK-030806-the-result-states-the-hands-played-and-both-final-stacks.md) | The result states the hands played and every final stack, exactly as sent | backlog |
| [TASK-030807](../tasks/TASK-030807-the-way-on-from-the-result-is-back-to-the-lobby.md) | The way on is back to the lobby, and there is no dead rematch | backlog |
| [TASK-030808](../tasks/TASK-030808-the-result-derives-no-winner-and-no-figure.md) | The result derives no winner and shows no figure the outcome did not carry | backlog |
| [TASK-030809](../tasks/TASK-030809-the-duel-screen-shows-the-result-when-the-duel-ends.md) | The duel screen shows the result when the duel ends | backlog |

### Three departures from the design, and why

Recorded here rather than argued in a review, in the shape `STORY-0307` used:

- **The defeat line reads `−1 duel coin`, not *"The coin goes to ImKate"*.** There is no name on the
  wire — `PlayerView` carries none, `ADR-0021` adds one later and `DEC-017` decides its product
  rules — and this story's own acceptance criterion asks for `−1`. The table's *Your rival* is the
  vocabulary the client has.
- **No duration.** The design's meta line reads *17 hands · 12 minutes · you took the whole stack*.
  `DuelOutcome` is `{winner, handsPlayed, finalStacks}`: a minute count would have to be timed by the
  client, and how the last pot was won is not on the wire at all. The hand count and both final
  stacks stay; the rest goes.
- **No rematch button.** The design draws one. `STORY-0309` owns it and waits on `EPIC-02`'s
  `STORY-0213` for the frames (`DEC-023` → `ADR-0044`), and this story says plainly not to stub it — `TASK-030807` has a test that keeps it from being added by
  kindness.

The way on is a plain `<a href="/">`, not a store reset: the reducer clears nothing a frame
established, so returning to the lobby means starting from an empty store, and a full load is how a
client with one connection per tab (`ADR-0032`) and no router does that. It also drops the invite's
`?room=`, which a soft reset would leave for `TASK-030504` to rejoin the finished room from.

**The fourth acceptance criterion closes by absence.** No balance is displayed here at all, and
`TASK-030808` proves it: every figure on the panel is `handsPlayed`, a final stack, or the coin's
`1`. The balance itself arrives with `STORY-0311`'s `GET /api/me` and is the server's answer, never a
counter this screen incremented.

## Acceptance criteria

- [ ] `winner === mySeat` renders a win, `winner === null` renders a draw, and any other seat
      renders a loss — three named tests, one per case.
- [ ] Hands played and both final stacks render exactly as sent.
- [ ] The coin delta rendered is `+1` for the winner, `−1` for the loser and nothing for a draw.
- [ ] No balance is displayed that the client computed itself; the balance shown is the one the
      profile read returned.
- [ ] From the result screen, the lobby is reachable and creating a new room works.

## Out of scope

- Rematch — `STORY-0309`, which consumes `EPIC-02`'s `STORY-0213` (`DEC-023` → `ADR-0044`).
- Hand history, replay, "you should have called" — `EPIC-08`.
- Rating changes, leaderboard position — `EPIC-05`.
- Sharing a result, screenshots, social anything.
