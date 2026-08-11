# Board

The index. Conventions live in [`README.md`](README.md).

**Now:** `EPIC-01` in flight via `/build-epic`. `TASK-010106` closes `STORY-0101`.

Startable right now: `python3 .github/scripts/lint_tickets.py --startable`

---

## Epics

| ID | Title | Status | Milestone |
| --- | --- | --- | --- |
| [EPIC-00](epics/EPIC-00-ways-of-working.md) | Ways of working | **in progress** | v0.1 |
| [EPIC-01](epics/EPIC-01-poker-engine.md) | Poker engine | **ready** | v0.1 |
| EPIC-02 | Duel server — rooms, WebSocket protocol, persistence | *not written* | v0.1 |
| EPIC-03 | Web client — table, lobby, duel flow | *not written* | v0.1 |
| EPIC-04 | Identity and profiles | *not written* | v0.2 |
| EPIC-05 | Ranking, duel coins and leaderboard | *not written* | v0.3 |
| EPIC-06 | Design system and art | *not written* | v0.2 |
| EPIC-07 | Infrastructure and delivery | *not written* | v0.2 |
| EPIC-08 | Analysis and decision quality | *not written* | later |
| EPIC-09 | Bots and simulation | *not written* | later |
| EPIC-10 | The AI software factory — the case study | *not written* | continuous |

Numbers 02–10 are **reserved**, not planned in detail. Epics are written when the one before
them is close to done, because writing them earlier means rewriting them.

---

## EPIC-00 — Ways of working

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| **[STORY-0001](stories/STORY-0001-repository-and-ticket-system.md)** Repository, docs, tickets | | | ready |
| | [TASK-000101](tasks/TASK-000101-bootstrap-repository.md) Bootstrap repository and ticket system | M | **done** |
| | [TASK-000102](tasks/TASK-000102-enable-branch-protection.md) Enable branch protection | S | blocked |
| | [TASK-000103](tasks/TASK-000103-token-lean-agent-workflow.md) Token-lean agent workflow | S | **in-review** |

`TASK-000102` is blocked by GitHub: protection and rulesets both require a paid plan on a
private repository. Until it clears, the branch model is convention, not enforcement.

---

## EPIC-01 — Poker engine

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| **[STORY-0101](stories/STORY-0101-engine-module-scaffold.md)** Module and build scaffold — *schema 2* | | | ready |
| | [TASK-010101](tasks/TASK-010101-gradle-wrapper-and-root-build.md) Gradle wrapper, settings, catalog | S | **done** |
| | [TASK-010102](tasks/TASK-010102-poker-engine-module.md) poker-engine module + running test | XS | **done** |
| | [TASK-010103](tasks/TASK-010103-engine-dependency-rule.md) Enforce engine depends on nothing | XS | **done** |
| | [TASK-010104](tasks/TASK-010104-ktlint-and-detekt.md) ktlint and detekt | S | **done** |
| | [TASK-010105](tasks/TASK-010105-kotest-property-testing.md) kotest property testing | XS | **done** |
| | [TASK-010106](tasks/TASK-010106-build-ci-workflow.md) Build and test CI workflow | XS | **ready** |
| | [TASK-010107](tasks/TASK-010107-configuration-cache-safe-checks.md) Config-cache-safe checkNoDependencies | XS | **done** |
| **[STORY-0102](stories/STORY-0102-cards-deck-shuffle.md)** Cards, deck, shuffle | | | ready |
| | [TASK-010201](tasks/TASK-010201-card-rank-suit.md) Rank, Suit, Card | S | backlog |
| | [TASK-010202](tasks/TASK-010202-card-notation.md) Poker notation parse and format | S | backlog |
| | [TASK-010203](tasks/TASK-010203-rng-and-deck.md) Seeded Rng and immutable Deck | M | backlog |
| | [TASK-010204](tasks/TASK-010204-determinism-tests.md) Determinism and distribution tests | S | backlog |
| **[STORY-0103](stories/STORY-0103-hand-evaluator.md)** Hand evaluator | | | ready |
| | [TASK-010301](tasks/TASK-010301-hand-rank-model.md) HandCategory and HandRank | S | backlog |
| | [TASK-010302](tasks/TASK-010302-reference-evaluator.md) Reference five-card evaluator | M | backlog |
| | [TASK-010303](tasks/TASK-010303-seven-card-evaluator.md) Seven-card best-of-five | S | backlog |
| | [TASK-010304](tasks/TASK-010304-evaluator-test-suite.md) Exhaustive and property tests | M | backlog |
| | [TASK-010305](tasks/TASK-010305-evaluator-performance.md) Fast evaluator and budget | M | backlog |
| **[STORY-0104](stories/STORY-0104-core-domain-model.md)** Core domain model | | | ready |
| | [TASK-010401](tasks/TASK-010401-game-state.md) GameState and sub-models | M | backlog |
| | [TASK-010402](tasks/TASK-010402-player-actions.md) PlayerAction and legality | S | backlog |
| | [TASK-010403](tasks/TASK-010403-game-events.md) GameEvent and EngineResult | M | backlog |
| | [TASK-010404](tasks/TASK-010404-engine-contract-tests.md) PokerEngine and contract suite | M | backlog |
| **[STORY-0105](stories/STORY-0105-betting-rounds.md)** Betting rounds | | | backlog |
| | [TASK-010501](tasks/TASK-010501-blinds-and-action-order.md) Blinds, button, action order | M | backlog |
| | [TASK-010502](tasks/TASK-010502-action-legality.md) Legality and min-raise | M | backlog |
| | [TASK-010503](tasks/TASK-010503-street-progression.md) Round completion, street advance | M | backlog |
| | [TASK-010504](tasks/TASK-010504-betting-property-tests.md) Betting invariant tests | S | backlog |
| **[STORY-0106](stories/STORY-0106-showdown-and-pots.md)** Showdown and pots | | | backlog |
| | [TASK-010601](tasks/TASK-010601-pot-accounting.md) Pot accounting and uncalled bets | M | backlog |
| | [TASK-010602](tasks/TASK-010602-showdown-resolution.md) Showdown, splits, reveal order | M | backlog |
| | [TASK-010603](tasks/TASK-010603-hand-completion.md) Hand completion and history | S | backlog |
| **[STORY-0107](stories/STORY-0107-duel-format-and-match.md)** Duel format and match | | | backlog |
| | [TASK-010701](tasks/TASK-010701-duel-format.md) DuelFormat and blind schedule | S | backlog |
| | [TASK-010702](tasks/TASK-010702-match-progression.md) Match progression | M | backlog |
| | [TASK-010703](tasks/TASK-010703-match-conclusion.md) Match conclusion and result | S | backlog |
| **[STORY-0108](stories/STORY-0108-event-log-replay-simulation.md)** Log, replay, simulation | | | backlog |
| | [TASK-010801](tasks/TASK-010801-event-log-format.md) Versioned event log format | M | backlog |
| | [TASK-010802](tasks/TASK-010802-replay.md) Replay a match from its log | S | backlog |
| | [TASK-010803](tasks/TASK-010803-simulation-harness.md) Simulation harness and fuzzing | M | backlog |

**36 tasks total.** `STORY-0101` is migrated to schema 2; stories 0102–0108 are still schema 1
and get split by `/plan-story` just before they are worked. Stories 0105–0108 stay in `backlog` until `STORY-0104` merges — their tasks are
written against types that do not exist yet, and specifying them any earlier would mean
rewriting them.

---

## Open decisions

| ID | Question | Where | Due |
| --- | --- | --- | --- |
| DEC-001 | What exactly is one duel? | [`docs/duel-rules.md`](../docs/duel-rules.md) | before v0.2 |
| — | Public repo or GitHub Pro, to enable branch protection? | [`TASK-000102`](tasks/TASK-000102-enable-branch-protection.md) | before v0.1 |

---

## Metrics

Updated as epics close. See [`docs/workflow.md`](../docs/workflow.md) — these are only worth
recording if they are recorded when unflattering.

| | EPIC-01 | Total |
| --- | --- | --- |
| Tasks completed | 6 / 33 | 7 / 36 |
| Accepted on first review | — | — |
| Average review iterations | — | — |
| Test lines / production lines | — | — |
| Tasks re-scoped mid-flight | — | — |
| Reviews skipped (must stay 0) | 0 | 0 |
| Tickets promoted haiku → sonnet | 1 | 1 |
| Average coder dispatches per ticket | — | — |
| Manual human edits | — | — |
