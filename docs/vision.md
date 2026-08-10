# Vision

## The one-sentence version

Two people, one link, one heads-up poker match, one winner — ranked, replayable, and free of
everything that makes online poker feel like a casino.

## Why this exists

It started as a personal need: the author wanted to play quick heads-up duels against his
sister and did not like any of the existing options. Every free poker site is a casino
simulator — chip bundles, slot machines in the lobby, gold everywhere, nine-handed tables,
tournaments, daily bonuses. None of that is wanted here.

The first success condition is small and concrete:

> **Send a link. She opens it in a browser. We play a full heads-up match. Someone wins.
> We hit Rematch.**

Everything else is downstream of that moment.

## What it is

- **Heads-up Texas Hold'em.** Two players. Never three.
- **A duel is a match, not a hand.** Variance across a single hand is enormous; a duel is long
  enough for decisions to matter. See `duel-rules.md`.
- **One duel coin per win.** Not chips, not currency, not a balance. A counter of duels won.
- **A leaderboard.** Ranked results over a season.
- **Replay and honest feedback.** Every hand is stored as an event log, so a match can be
  replayed and analysed afterwards.

## What it is not

- Not real-money gambling, and not a path to it.
- Not a casino: no chip purchases, no bonuses, no slots, no gold, no felt-and-mahogany styling.
- Not a multi-table poker room. No 6-max, no 9-max, no tournaments, no sit & go, no cash games.
- Not a GTO solver. We may *consume* analysis; we are not writing an equilibrium solver.
  See `adr/ADR-0005-analysis-behind-an-interface.md`.

## On variance

Poker is not a game of pure skill and we are not going to pretend otherwise in the marketing.
Variance is not a defect to be engineered away — it is the reason the game has survived a
century. The product line is not *"only skill"*; it is closer to:

> **Luck decides a hand. Skill decides whether you come back tomorrow.**

Where variance can be turned into a feature, it should be: showing a player that they lost the
match but made the better decisions is more interesting than hiding the maths.

## Positioning

The reference points are **Lichess** and **Chess.com**, not PokerStars. Dark, quiet, fast,
minimal. The vocabulary is duelling, not gambling: *challenge, duel, rematch, rival, streak,
season* — never *buy-in, bankroll, jackpot, bonus*.

## Two audiences, eventually

1. **Players.** People who want a fast ranked heads-up match.
2. **Developers.** The engine is a clean, deterministic, headless library. A bot API and a
   simulation harness make it usable for AI work. This audience is small but engaged.

## The second product

Running alongside the game is a case study: *how one person plus Claude Code built it*. The
repository is structured so that the process is legible — a ticketed backlog, decision records,
and metrics on how much of the work the agents actually got right. See `workflow.md`.

That means the discipline described in `CLAUDE.md` is not bureaucracy. It is the product.

## Roadmap

| Milestone | Contents |
| --- | --- |
| **v0.1** | Two browsers, one room link, one complete duel, rematch. No accounts. |
| **v0.2** | Persistent profile, duel coin counter, match history. |
| **v0.3** | Leaderboard and seasons. |
| **v0.4** | Friends, statistics, replay viewer. |
| **later** | Matchmaking, decision-quality analysis, bot API, further duel disciplines. |

The engine is built once and serves all of them.
