# Board

The index. Conventions live in [`README.md`](README.md).

**Now:** `EPIC-01` in flight via `/build-epic`. `STORY-0101`–`STORY-0103` are done; `STORY-0104` is split into schema-2 tickets and being worked.

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
| **[STORY-0101](stories/STORY-0101-engine-module-scaffold.md)** Module and build scaffold — *schema 2* | | | **done** |
| | [TASK-010101](tasks/TASK-010101-gradle-wrapper-and-root-build.md) Gradle wrapper, settings, catalog | S | **done** |
| | [TASK-010102](tasks/TASK-010102-poker-engine-module.md) poker-engine module + running test | XS | **done** |
| | [TASK-010103](tasks/TASK-010103-engine-dependency-rule.md) Enforce engine depends on nothing | XS | **done** |
| | [TASK-010104](tasks/TASK-010104-ktlint-and-detekt.md) ktlint and detekt | S | **done** |
| | [TASK-010105](tasks/TASK-010105-kotest-property-testing.md) kotest property testing | XS | **done** |
| | [TASK-010106](tasks/TASK-010106-build-ci-workflow.md) Build and test CI workflow | XS | **done** |
| | [TASK-010107](tasks/TASK-010107-configuration-cache-safe-checks.md) Config-cache-safe checkNoDependencies | XS | **done** |
| **[STORY-0102](stories/STORY-0102-cards-deck-shuffle.md)** Cards, deck, shuffle — *schema 2* | | | **done** |
| | [TASK-010201](tasks/TASK-010201-rank-and-suit.md) Rank and Suit enums | S | **done** |
| | [TASK-010202](tasks/TASK-010202-card-value-type.md) Card as a value class | S | **done** |
| | [TASK-010203](tasks/TASK-010203-card-notation.md) Format and parse poker notation | S | **done** |
| | [TASK-010204](tasks/TASK-010204-cards-test-helper.md) `cards("As Kd")` test helper | S | **done** |
| | [TASK-010205](tasks/TASK-010205-splitmix64-rng.md) Rng and SplitMix64Rng | S | **done** |
| | [TASK-010206](tasks/TASK-010206-immutable-deck.md) Immutable Deck | S | **done** |
| | [TASK-010207](tasks/TASK-010207-fisher-yates-shuffle.md) Fisher–Yates shuffle | S | **done** |
| | [TASK-010208](tasks/TASK-010208-shuffle-determinism-test.md) Recorded orderings for two seeds | XS | **done** |
| | [TASK-010209](tasks/TASK-010209-shuffle-distribution-test.md) Shuffle distribution | S | **done** |
| | [TASK-010210](tasks/TASK-010210-no-ambient-random-test.md) No ambient randomness, asserted | XS | **done** |
| **[STORY-0103](stories/STORY-0103-hand-evaluator.md)** Hand evaluator — *schema 2* | | | **done** |
| | [TASK-010301](tasks/TASK-010301-hand-category.md) HandCategory, ordered low to high | XS | **done** |
| | [TASK-010302](tasks/TASK-010302-hand-rank.md) HandRank, comparable lexicographically | S | **done** |
| | [TASK-010303](tasks/TASK-010303-straight-detection.md) Detect a straight, including the wheel | S | **done** |
| | [TASK-010304](tasks/TASK-010304-rank-groups.md) Group ranks by count into tiebreak order | S | **done** |
| | [TASK-010305](tasks/TASK-010305-reference-evaluator.md) Reference five-card evaluator | S | **done** |
| | [TASK-010306](tasks/TASK-010306-evaluator-rule-tests.md) Pin the wheel, non-wraparound, suit irrelevance | S | **done** |
| | [TASK-010307](tasks/TASK-010307-exhaustive-five-card-counts.md) Exhaustive five-card category counts | S | **done** |
| | [TASK-010308](tasks/TASK-010308-seven-card-best-of-five.md) Seven-card best-of-five evaluation | S | **done** |
| | [TASK-010309](tasks/TASK-010309-seven-card-brute-force-test.md) Brute-force check of seven-card evaluation | S | **done** |
| | [TASK-010310](tasks/TASK-010310-fast-evaluator.md) Bitmask five-card evaluator | S | **done** |
| | [TASK-010311](tasks/TASK-010311-fast-evaluator-seven-card-equivalence.md) Fast and reference agree on seven cards | XS | **done** |
| **[STORY-0104](stories/STORY-0104-core-domain-model.md)** Core domain model — *schema 2* | | | ready |
| | [TASK-010405](tasks/TASK-010405-street-enum.md) Street enum with board size and successor | XS | **done** |
| | [TASK-010406](tasks/TASK-010406-board-value-type.md) Board value type that can only hold 0, 3, 4 or 5 cards | XS | **done** |
| | [TASK-010407](tasks/TASK-010407-seat-state.md) Seat state and its construction invariants | S | **done** |
| | [TASK-010408](tasks/TASK-010408-seat-chip-transitions.md) Seat chip transitions — commit, award, collect | S | **done** |
| | [TASK-010409](tasks/TASK-010409-game-state.md) GameState fields and construction invariants | S | **done** |
| | [TASK-010410](tasks/TASK-010410-game-state-derived.md) GameState derived properties and seat update | S | **done** |
| | [TASK-010411](tasks/TASK-010411-game-state-test-fixture.md) handState test fixture for game states | XS | **done** |
| | [TASK-010412](tasks/TASK-010412-player-actions.md) PlayerAction hierarchy and ActionType | S | **done** |
| | [TASK-010413](tasks/TASK-010413-rejection-reasons.md) Rejection reasons for an illegal action | XS | **done** |
| | [TASK-010414](tasks/TASK-010414-legal-actions.md) LegalActions descriptor | S | **done** |
| | [TASK-010415](tasks/TASK-010415-game-event-base.md) GameEvent base and hand lifecycle events | S | **done** |
| | [TASK-010416](tasks/TASK-010416-betting-events.md) Betting events | S | **done** |
| | [TASK-010417](tasks/TASK-010417-dealer-events.md) Dealer events for street progress and showdown | S | **done** |
| | [TASK-010418](tasks/TASK-010418-settlement-events.md) Settlement events — uncalled bet, pot award, hand finished | XS | **done** |
| | [TASK-010419](tasks/TASK-010419-engine-result.md) EngineResult and the rejection invariant | XS | ready |
| | [TASK-010420](tasks/TASK-010420-domain-immutability-test.md) Reflective immutability test over the domain types | S | backlog |
| | [TASK-010421](tasks/TASK-010421-poker-engine-interface.md) PokerEngine interface and a no-op implementation | XS | backlog |
| | [TASK-010422](tasks/TASK-010422-betting-projection.md) Fold betting events into a state | S | backlog |
| | [TASK-010423](tasks/TASK-010423-dealer-projection.md) Fold dealer events into a state | S | backlog |
| | [TASK-010424](tasks/TASK-010424-settlement-projection-tests.md) Settlement projection tests and chip conservation | S | backlog |
| | [TASK-010425](tasks/TASK-010425-state-projection.md) StateProjection — the one entry point that folds events into a state | S | backlog |
| | [TASK-010426](tasks/TASK-010426-engine-contract-suite.md) PokerEngineContract — the reusable engine test suite | S | backlog |
| | [TASK-010427](tasks/TASK-010427-contract-detects-drift.md) Prove the contract suite catches a drifting engine | XS | backlog |
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

**67 tasks total.** `STORY-0101`–`STORY-0104` are migrated to schema 2; stories 0105–0108 are still schema 1
and get split by `/plan-story` just before they are worked. Stories 0105–0108 stay in `backlog` until `STORY-0104` merges — their tasks are
written against types that do not exist yet, and specifying them any earlier would mean
rewriting them.

---

## Open decisions

| ID | Question | Where | Due |
| --- | --- | --- | --- |
| DEC-001 | What exactly is one duel? | [`docs/duel-rules.md`](../docs/duel-rules.md) | before v0.2 |
| DEC-002 | Evaluator performance budget, how it is measured, and whether `HandRank` becomes a packed integer | [`STORY-0103`](stories/STORY-0103-hand-evaluator.md) | before benchmark tooling lands |
| — | Public repo or GitHub Pro, to enable branch protection? | [`TASK-000102`](tasks/TASK-000102-enable-branch-protection.md) | before v0.1 |

---

## Metrics

Updated as epics close. See [`docs/workflow.md`](../docs/workflow.md) — these are only worth
recording if they are recorded when unflattering.

| | EPIC-01 | Total |
| --- | --- | --- |
| Tasks completed | 42 / 64 | 43 / 67 |
| Accepted on first review | — | — |
| Average review iterations | — | — |
| Test lines / production lines | — | — |
| Tasks re-scoped mid-flight | — | — |
| Reviews skipped (must stay 0) | 0 | 0 |
| Tickets promoted haiku → sonnet | 1 | 1 |
| Average coder dispatches per ticket | — | — |
| Manual human edits | — | — |
