# ADR-0014 — The winner takes a coin, the loser gives one, a draw pays nothing

- **Status:** Accepted
- **Date:** 2026-08-12
- **Constrains:** `EPIC-02` (persistence), `EPIC-05` (ranking and leaderboard)

## Context

`docs/vision.md` says the winner of a duel gets one duel coin. It does not say what happens to the
loser, or to a draw — and that gap decides the shape of the data: whether a balance can ever
decrease, and therefore whether it can go negative.

It is not a cosmetic question. A balance that only ever rises is a count of wins; a balance that
falls too is a net record. Those are different numbers with different meanings, and the leaderboard
in `EPIC-05` will be sorted by whichever one this decision produces.

## Decision

- The **winner gains one** duel coin.
- The **loser loses one**.
- A **draw pays nothing** to either player.

A balance is therefore `wins − losses`, and **may be negative**.

## Consequences

**What it buys.** The balance is a genuine net record rather than a participation count. A player
who has lost more than they have won shows a number that says so, which is the honest thing and the
thing a ranked ladder needs. It also gives every duel a real stake in both directions — the reason
to play carefully is that the number can move the wrong way.

**What it costs.**

- **Negative balances are legal and must be handled everywhere**: the schema stores a signed
  integer, the read path returns it unflinchingly, and the client will eventually have to render
  `−3` without treating it as an error state. A `UInt` or an unsigned column here would be a bug
  waiting for the first losing streak.
- A new player's first loss puts them at −1. That is intended, and it is the case to check first
  when the display work lands.
- Deliberately **not** floored at zero. Flooring would make a long losing streak indistinguishable
  from never having played, which destroys the meaning the decision exists to create.

**What it forecloses.** Little. If the balance should later float — a rating rather than a count,
or an asymmetric award weighted by opponent strength — that is a new ADR superseding this one, and
the storage (a signed integer plus a per-duel result row) already supports it.

## Alternatives considered

**Winner gains, loser unaffected.** The literal minimum reading of `vision.md`. Rejected once the
loser's side was decided: it makes the coin a count of wins, and a ladder sorted by wins rewards
volume over skill.

**Floor the balance at zero.** Avoids showing a negative number to a struggling player. Rejected
because it silently collapses two very different players onto the same value, and because hiding
the number does not change the record — it just makes the record untrue.

**Draws pay half a coin, or one each.** Rejected: it makes the balance non-integral or inflates the
supply, and a draw is already a rare outcome — only a `FixedHands` duel can end level, and
`Freezeout` cannot draw at all.
