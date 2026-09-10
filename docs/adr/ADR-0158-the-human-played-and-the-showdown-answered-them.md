# ADR-0158 — The human played, and the showdown answered them

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** the human's own instruction of 2026-09-10 — *"make all you can to make client ux
  better, more straightforward and fix bugs; all decisions are on you"* — read against their
  feedback of 2026-09-06 in [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md):
  *"if we went to showdown villan card shoud be show"*, *"winning combination higlited, winning
  cards + board card that make combination"*, and the name ask *"before duel"*.
- **Where the answer came from:** **the human's stated wishes, taken at their word.** The earlier
  answers were derived from the vision by the product-owner agent while the human's annotations
  were treated as reports; this ADR treats them as the ruling they read as, on the human's own
  later instruction that the decisions were theirs to hand over.
- **Supersedes** [`ADR-0126`](ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md) whole
  — the table marks the five that took the pot. **Amends**
  [`ADR-0008`](ADR-0008-loser-mucks-at-showdown.md) for one shape and leaves the rest standing:
  a hand that is all in and called turns both hands face up before the runout, and nothing is
  mucked from a runout; a hand that reaches the river with a decision still behind it keeps
  `ADR-0008`'s muck. **Answers** the question
  [`ADR-0120`](ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
  §5 named and did not register.

## Decision

1. **A runout shows both hands first.** When betting ends with nobody able to act — all in and
   called — the engine emits `HandRevealed` for both seats, the last aggressor first, *before* the
   first street of the runout is dealt. The showdown that follows reveals no hand twice. With no
   decision left to protect, a hidden hand conceals nothing the game needs concealed; what it
   conceals is the one thing an all-in is for, watching the board decide with both hands known.
2. **The award names the five.** `PotAwarded` carries `hand`: the winner's best five of their two
   hole cards and the board, from the same evaluation that chose the winner. Empty for a pot won
   on a fold. Every card in it is public by the time the event exists.
3. **The table marks them, on the last beat only.** At the hand's final beat the five carry an
   accent ring and the cards that did not play stand at reduced opacity. While a runout is still
   turning cards, and mid-hand, every card is drawn alike. No hand is *named* — `ADR-0095` §3
   stands; the mark is a statement made without words.
4. **The runout keeps its suspense.** Every step but the last draws the pot and the stacks as they
   stood before the award — each seat's award taken back out of its stack and put back in the pot,
   nothing committed on the street — so a stack never knows the winner before the river does.
   The figures are the server's, combined by subtraction only.
5. **The rival is named across the table.** A `SeatNames` frame (protocol 7) follows every
   `RoomJoined` and reaches both seats the moment the guest is seated. The seat plate and the
   result ledger name the rival by it, and fall back to *Your rival*.
6. **A first visitor is asked for a name at their first press.** The profile is re-read once the
   handshake has minted the device id, so the ask stands on a first visit, as `ADR-0119` §1
   intended and the mount-time read could never deliver.

## Consequences

`CardSecrecyTest` treats a hand that ran out as one with nothing to muck, and asserts the muck
for every other showdown as before. `EPIC-08`'s analysis board gains the loser's hand for every
all-in — the one shape where the log now holds it — and nothing else. The design cards that drew
a mucked runout (`design/screens/duel-table-states.html`, *"you win, the loser mucks"*) describe
the river shape only.
