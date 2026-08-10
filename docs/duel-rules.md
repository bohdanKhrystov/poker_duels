# Poker and duel rules

This is the specification the engine implements. Where poker has house variations, the choice
made here is the choice the engine makes. Ambiguity in this document is a bug — raise a ticket
rather than guessing in code.

---

## Part 1 — Heads-up No-Limit Texas Hold'em

Two players. One 52-card deck. No jokers, no burn cards (they change nothing and only add
state).

### Positions and blinds

Heads-up reverses the usual arrangement, and this is the single most common thing to get wrong:

- The **button is the small blind**.
- The button acts **first** before the flop and **last** on every street after it.
- The non-button posts the big blind.
- The button alternates every hand.

### Order of play

| Street | Board | Dealt |
| --- | --- | --- |
| Preflop | — | two hole cards each |
| Flop | 3 cards | |
| Turn | 4th card | |
| River | 5th card | |
| Showdown | | best five of seven |

A hand ends early the moment one player folds; no further cards are dealt and no cards are
revealed.

### Betting

No-limit. Legal actions depend on whether there is a bet to face:

| Situation | Legal |
| --- | --- |
| No bet outstanding | `check`, `bet` |
| Facing a bet | `fold`, `call`, `raise` |
| Facing an all-in you cannot cover | `fold`, `call` (for your remaining stack) |

Rules:

- **Minimum bet** is one big blind.
- **Minimum raise** is the size of the largest previous bet or raise on the current street.
  Facing a bet of 40 into a pot after a bet of 10 and a raise to 40, the next raise must be to
  at least 70.
- An **all-in for less than a full raise does not reopen the betting** for a player who has
  already acted and faced the full raise. It is callable, not re-raisable, by that player.
- A player may always go all-in for their remaining stack.
- A bet larger than the opponent's remaining stack is capped: the excess is never at risk and
  is **returned as an uncalled bet** when the hand resolves.

### Showdown

- Best five cards of the seven available. Both hole cards need not be used; neither need be.
- Hand ranking, high to low:
  `straight flush > four of a kind > full house > flush > straight > three of a kind >
  two pair > one pair > high card`.
- Aces are high or low for straights: `A-2-3-4-5` is the lowest straight ("the wheel").
  There is no `Q-K-A-2-3`.
- Suits never break ties. Equal hands **split the pot**; an odd chip goes to the player out of
  position (the big blind).
- Reveal order: the last aggressor on the river shows first; if there was no river bet, the
  player out of position shows first. The loser may muck.

### Chips

Chip counts are integers. There are no fractional chips anywhere in the engine.

**Invariant, asserted in tests:** the sum of both stacks plus all pots is constant for the
entire hand. Every event that moves chips is checked against it.

---

## Part 2 — The duel format

> ### ⚠ Open decision — DEC-001
>
> The exact duel format is **not final**. It shapes how the game feels more than any UI choice,
> and it should be decided by playing, not by argument. The engine therefore takes the format
> as **configuration**, and the values below are the default the first playable build ships
> with. Changing them must never require an engine change.
>
> Settle this before v0.2 and record the outcome as an ADR.

### Default: freezeout

A duel is a **freezeout** — the two players start with equal stacks and play until one of them
holds every chip.

| Setting | Default |
| --- | --- |
| Starting stack | 100 big blinds (10 000 chips at 50/100) |
| Blind schedule | levels rise every 10 hands |
| End condition | one player holds all chips |
| Expected length | 20–45 hands, roughly 5–15 minutes |
| Reward | winner gets **1 duel coin** |

Blind levels (chips):

| Level | Hands | Small / Big |
| --- | --- | --- |
| 1 | 1–10 | 50 / 100 |
| 2 | 11–20 | 75 / 150 |
| 3 | 21–30 | 100 / 200 |
| 4 | 31–40 | 150 / 300 |
| 5 | 41+ | 200 / 400, doubling every 10 hands thereafter |

The escalating schedule is what guarantees a duel terminates. Without it a heads-up match can
run indefinitely.

### Alternative under consideration

**Fixed-length match** — a set number of hands (25 or 50), winner is whoever holds more chips
at the end. Lower variance and a predictable duration, but the ending is an arithmetic
comparison rather than a knockout, which suits a "duel" much less well. Recorded here so the
decision is made deliberately.

### Rejected

- **Single hand.** Far too much variance; the better player would win barely more than half
  the time. Contradicts the whole premise of a ranked ladder.
- **Race to first pot won.** Same problem.

---

## Part 3 — Things the engine does *not* decide

These belong to the server, not to the rules:

- action time limits and what happens on timeout,
- disconnection, reconnection and abandonment,
- rating changes,
- rematch and challenge flow.

The engine is given complete actions and produces complete events. If a player runs out of
time, it is the *server* that decides to submit a fold on their behalf.
