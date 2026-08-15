# Poker Duels

Ranked **heads-up Texas Hold'em duels**. No money — the winner gets one *duel coin* and a
leaderboard place. Lichess, not casino.

Two deliverables: **the game**, and **the documented process** by which one person and Claude
Code built it. Tickets, ADRs and metrics *are* the second product, so keep the trail honest.

## Working agreement

1. **One ticket at a time.** Work is a file under `tasks/`. No ticket, no code.
2. **Read only what the ticket names.** Never survey the repository — context is the scarce
   resource here, and the ticket's file list is the budget.
3. **Done is an exit code**: every command in the ticket's `verify:` block exits 0.
4. **Never widen scope.** Something you found outside the ticket becomes a new ticket.
5. **Never guess a decision.** No ADR covers it → register `DEC-NNN`, block the ticket, and route
   it: technical to the `architect` agent, product to the `product-owner` agent, which derives its
   answer from `docs/vision.md`. Only a decision that would *change* the vision waits for the human.
   An ADR is an answer once it is **merged**, and a PR that is only an ADR plus its register rows is
   merged without asking.
6. **Tests ship with the ticket**, and a task is not done until its PR is merged into `develop`.

## Non-negotiables

- **`poker-engine` is a pure Kotlin library.** No networking, I/O, clock, framework types, or
  `kotlin.random.Random`. It depends on nothing; everything depends on it.
- **Determinism.** All randomness goes through the injected `Rng`. Same seed + same actions ⇒
  byte-identical game, always.
- **The server is authoritative.** A client may never assert a game fact.
- **Hole cards are filtered per recipient** in the engine's projection layer — never ad hoc in
  transport. Folded and mucked cards appear in no event, anywhere.
- **English** everywhere: code, tickets, docs, commits.

## Style

Kotlin official style. `data class` + `val` — no `var` in domain types. Sealed hierarchies with
exhaustive `when` over enums with side tables. Explicit visibility on public API, KDoc on the
public engine API. Comment *why*, never *what*.

## Where to look

| You need | Read |
| --- | --- |
| What we're building, and what we refuse to build | `docs/vision.md` |
| Modules, dependency rules, the engine contract | `docs/architecture.md` |
| Poker rules and what a duel is | `docs/duel-rules.md` |
| How agents, tickets and PRs work | `docs/workflow.md`, `tasks/README.md` |
| Why something is the way it is | `docs/adr/` |
| What to do next | `tasks/BOARD.md` |
