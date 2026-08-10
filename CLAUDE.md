# Poker Duels

Ranked **heads-up Texas Hold'em duels**. No money. The winner of a duel gets one *duel coin*
and a place on the leaderboard. Think Lichess, not casino.

Two things are being built at the same time, and both are deliverables:

- **Product A — Poker Duels**: the game.
- **Product B — the AI Software Factory**: the documented, reproducible process by which one
  person and Claude Code built Product A. Tickets, ADRs and metrics *are* that product, so the
  paper trail has to be honest and current.

---

## Working agreement — read this before touching anything

1. **One ticket at a time.** Work is defined by a file under `tasks/`. No ticket, no code —
   write the ticket first.
2. **Small changes.** Aim for ≤ 300 changed lines and ≤ 10 files in context per ticket.
   If it doesn't fit, split the ticket instead of growing the change.
3. **Read narrowly.** Read the ticket, the documents it links, and the files it names.
   Do not read the whole repository — context is the scarce resource on this project.
4. **Branch per ticket → PR into `develop` → squash merge.** See `CONTRIBUTING.md`.
5. **Tests ship with the ticket**, never as a follow-up.
6. **Leave the trail.** Update the ticket's `status`, and record any non-obvious decision as an
   ADR in `docs/adr/`.

## Where to look

| You need | Read |
| --- | --- |
| What we're building and why | `docs/vision.md` |
| Modules and dependency rules | `docs/architecture.md` |
| The engine contract | `docs/adr/ADR-0001-event-sourced-engine-contract.md` |
| Poker rules and what a "duel" is | `docs/duel-rules.md` |
| How tickets and agents work | `docs/workflow.md`, `tasks/README.md` |
| Branching, PRs, commit format | `CONTRIBUTING.md` |
| What's in flight right now | `tasks/BOARD.md` |

## Non-negotiables

- **`poker-engine` is a pure Kotlin library.** No networking, no file or console I/O, no clock,
  no framework types, no `kotlin.random.Random` at call sites. It depends on nothing.
  Everything else depends on it.
- **Determinism.** All randomness goes through an injected `Rng`. Same seed + same action
  sequence ⇒ byte-identical game, every time. This is what makes replay, tests, and bot
  training possible.
- **The server is authoritative.** A client may never assert game facts — not the cards it
  holds, not the pot, not the winner. Even without money on the table, the client is untrusted.
- **A player is never sent information they should not have.** Hole cards are filtered per
  recipient before broadcast, in the engine's own projection layer, not ad hoc in the transport.
- **English** for all code, tickets, docs and commit messages.

## Style

- Kotlin: official style, explicit visibility on public API, `data class` + `val` only —
  no `var` in domain types.
- Prefer sealed hierarchies and exhaustive `when` over enums with side tables.
- No comments that restate the code. Comment *why*, never *what*.
- Public engine API gets KDoc; internals generally don't.
