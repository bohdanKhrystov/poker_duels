# Board

The index. Conventions live in [`README.md`](README.md).

**Now:** `EPIC-01` is **done**. `EPIC-02` in flight: `STORY-0201`, `STORY-0202` (bar one follow-up) and `STORY-0204` are done — the server boots, the protocol is defined, and per-recipient projection is guarded by two adversarial properties.

Startable right now: `python3 .github/scripts/lint_tickets.py --startable`

---

## Epics

| ID | Title | Status | Milestone |
| --- | --- | --- | --- |
| [EPIC-00](epics/EPIC-00-ways-of-working.md) | Ways of working | **in progress** | v0.1 |
| [EPIC-01](epics/EPIC-01-poker-engine.md) | Poker engine | **done** | v0.1 |
| [EPIC-02](epics/EPIC-02-duel-server.md) | Duel server — rooms, WebSocket protocol, persistence | **ready** | v0.1 |
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
| **[STORY-0104](stories/STORY-0104-core-domain-model.md)** Core domain model — *schema 2* | | | **done** |
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
| | [TASK-010419](tasks/TASK-010419-engine-result.md) EngineResult and the rejection invariant | XS | **done** |
| | [TASK-010420](tasks/TASK-010420-domain-immutability-test.md) Reflective immutability test over the domain types | S | **done** |
| | [TASK-010421](tasks/TASK-010421-poker-engine-interface.md) PokerEngine interface and a no-op implementation | XS | **done** |
| | [TASK-010422](tasks/TASK-010422-betting-projection.md) Fold betting events into a state | S | **done** |
| | [TASK-010423](tasks/TASK-010423-dealer-projection.md) Fold dealer events into a state | S | **done** |
| | [TASK-010424](tasks/TASK-010424-settlement-projection-tests.md) Settlement projection tests and chip conservation | S | **done** |
| | [TASK-010425](tasks/TASK-010425-state-projection.md) StateProjection — the one entry point that folds events into a state | S | **done** |
| | [TASK-010426](tasks/TASK-010426-engine-contract-suite.md) PokerEngineContract — the reusable engine test suite | S | **done** |
| | [TASK-010427](tasks/TASK-010427-contract-detects-drift.md) Prove the contract suite catches a drifting engine | XS | **done** |
| **[STORY-0105](stories/STORY-0105-betting-rounds.md)** Betting rounds — *schema 2* | | | **done** |
| | [TASK-010505](tasks/TASK-010505-heads-up-seat-order.md) Name the heads-up blind and action order once | XS | **done** |
| | [TASK-010506](tasks/TASK-010506-post-the-blinds.md) Open a hand by posting both blinds | S | **done** |
| | [TASK-010507](tasks/TASK-010507-deal-hole-cards.md) Deal the hole cards and put the action on the button | S | **done** |
| | [TASK-010508](tasks/TASK-010508-legal-actions-core.md) Compute the legal actions at an ordinary decision point | S | **done** |
| | [TASK-010509](tasks/TASK-010509-legal-actions-all-in.md) Restrict the legal actions around an all-in | S | **done** |
| | [TASK-010510](tasks/TASK-010510-action-validation.md) Turn an illegal action into the reason it is illegal | S | **done** |
| | [TASK-010511](tasks/TASK-010511-action-to-event.md) Turn an accepted action into the event that records it | S | **done** |
| | [TASK-010512](tasks/TASK-010512-default-engine.md) Handle one betting action in a real engine | S | **done** |
| | [TASK-010513](tasks/TASK-010513-round-completion.md) Decide whether the betting round has anyone left to act | S | **done** |
| | [TASK-010514](tasks/TASK-010514-pass-the-action.md) Pass the action to the other seat while the round runs | S | **done** |
| | [TASK-010515](tasks/TASK-010515-engine-contract-test.md) Run the engine contract against the real engine | XS | **done** |
| | [TASK-010516](tasks/TASK-010516-fold-ends-the-hand.md) End the betting the moment a player folds | XS | **done** |
| | [TASK-010522](tasks/TASK-010522-contract-fixture-deck.md) Give the contract fixtures a deck consistent with their board | XS | **done** |
| | [TASK-010517](tasks/TASK-010517-street-advance.md) Close the round and deal the next street | S | **done** |
| | [TASK-010518](tasks/TASK-010518-all-in-run-out.md) Run the board out when nobody can bet again | S | **done** |
| | [TASK-010519](tasks/TASK-010519-opening-run-out.md) Do not stall a hand whose blinds leave nobody able to act | S | **done** |
| | [TASK-010520](tasks/TASK-010520-hand-walkthrough-test.md) Play one scripted hand from blinds to showdown | S | **done** |
| | [TASK-010521](tasks/TASK-010521-betting-invariant-property.md) Assert the betting invariants over a thousand random hands | S | **done** |
| **[STORY-0106](stories/STORY-0106-showdown-and-pots.md)** Showdown and pots — *schema 2* | | | ready |
| | [TASK-010604](tasks/TASK-010604-uncalled-bet-arithmetic.md) Compute the uncalled part of a bet | S | **done** |
| | [TASK-010605](tasks/TASK-010605-settle-to-one-winner.md) Settle a swept hand to a single winner | S | **done** |
| | [TASK-010606](tasks/TASK-010606-split-pot-odd-chip.md) Split a pot between two winners, odd chip out of position | S | **done** |
| | [TASK-010607](tasks/TASK-010607-fold-awards-the-pot.md) A fold awards the pot and ends the hand | S | **done** |
| | [TASK-010608](tasks/TASK-010608-showdown-fixtures-hole-cards.md) Give the synthetic showdown fixtures hole cards | XS | **done** |
| | [TASK-010609](tasks/TASK-010609-terminal-check-by-what-it-accepts.md) Pin the random hand's ending by what it accepts, not by its street | XS | **done** |
| | [TASK-010610](tasks/TASK-010610-showdown-winners.md) Decide who wins a showdown | S | **done** |
| | [TASK-010611](tasks/TASK-010611-river-close-settles.md) A closed river settles the showdown | S | **done** |
| | [TASK-010612](tasks/TASK-010612-run-out-settles.md) A run-out settles the showdown it reaches | S | **done** |
| | [TASK-010613](tasks/TASK-010613-settlement-invariants-property.md) Settlement invariants over a thousand random hands | S | **done** |
| | [TASK-010614](tasks/TASK-010614-folded-cards-in-no-event.md) A folded hand appears in no event, over a thousand hands | S | **done** |
| | [TASK-010617](tasks/TASK-010617-mucked-cards-in-no-event.md) Extend the secrecy suite from the fold to the muck | S | **done** |
| | [TASK-010618](tasks/TASK-010618-last-aggressor-field.md) Carry the last aggressor on GameState | XS | **done** |
| | [TASK-010619](tasks/TASK-010619-betting-records-the-aggressor.md) A bet, raise or full all-in records its seat as the last aggressor | S | **done** |
| | [TASK-010620](tasks/TASK-010620-new-street-clears-the-aggressor.md) A dealt street clears the last aggressor, a closed round does not | XS | **done** |
| | [TASK-010621](tasks/TASK-010621-new-hand-clears-the-aggressor.md) A new hand starts with no last aggressor | XS | **done** |
| | [TASK-010622](tasks/TASK-010622-reveal-order.md) Decide who shows at a showdown, and in what order | S | **done** |
| | [TASK-010623](tasks/TASK-010623-showdown-emits-the-reveals.md) A showdown emits HandRevealed for the hands that are shown | S | **done** |
| | [TASK-010624](tasks/TASK-010624-tie-reveals-both-hands.md) A tied showdown reveals both hands, the river aggressor first | S | **done** |
| | [TASK-010616](tasks/TASK-010616-split-with-uncalled-bet.md) Pin a split pot that also returns an uncalled bet | XS | **done** |
| **[STORY-0107](stories/STORY-0107-duel-format-and-match.md)** Duel format and match — *schema 2* | | | ready |
| | [TASK-010704](tasks/TASK-010704-blind-level.md) A blind level that can double | S | **done** |
| | [TASK-010706](tasks/TASK-010706-end-condition.md) The two duel end conditions as a sealed type | XS | **done** |
| | [TASK-010711](tasks/TASK-010711-duel-outcome.md) DuelOutcome, the result of a finished duel | XS | **done** |
| | [TASK-010705](tasks/TASK-010705-blind-schedule.md) Which blinds a hand number plays | S | **done** |
| | [TASK-010707](tasks/TASK-010707-duel-format.md) DuelFormat and the default freezeout | S | **done** |
| | [TASK-010708](tasks/TASK-010708-match-state.md) MatchState, what survives between two hands | S | **done** |
| | [TASK-010709](tasks/TASK-010709-start-next-hand.md) Deal the match's next hand at its blinds | S | **done** |
| | [TASK-010710](tasks/TASK-010710-record-hand.md) Fold a finished hand back into the match | S | **done** |
| | [TASK-010712](tasks/TASK-010712-evaluate-the-end-condition.md) Decide whether a match is over, and who won | S | **done** |
| | [TASK-010713](tasks/TASK-010713-random-duel-harness.md) Play a whole duel from one seed | S | **done** |
| | [TASK-010714](tasks/TASK-010714-duel-invariants.md) Button, blinds and chips across a whole duel | S | **done** |
| | [TASK-010715](tasks/TASK-010715-duel-termination-property.md) Every default duel terminates | S | **done** |
| | [TASK-010716](tasks/TASK-010716-fixed-length-duel.md) A fixed-length duel is decided on chips | S | **done** |
| | [TASK-010725](tasks/TASK-010725-match-event-hierarchy.md) MatchEvent, its own hierarchy, and MatchFinished | S | **done** |
| **[STORY-0108](stories/STORY-0108-event-log-replay-simulation.md)** Log, replay, simulation — *schema 2* | | | ready |
| | [TASK-010804](tasks/TASK-010804-hand-log.md) HandLog, the replayable record of one hand | S | **done** |
| | [TASK-010805](tasks/TASK-010805-replay-a-hand.md) Replay a hand from its log | S | **done** |
| | [TASK-010806](tasks/TASK-010806-replay-divergence.md) Replay rejects a log that does not match the engine | S | **done** |
| | [TASK-010807](tasks/TASK-010807-replay-identity-property.md) Record and replay is an identity over 200 hands | S | **done** |
| | [TASK-010808](tasks/TASK-010808-poker-ai-module.md) The poker-ai module, where bots and the harness live | XS | **done** |
| | [TASK-010809](tasks/TASK-010809-random-bot.md) Bot, and a RandomBot picking among legal actions | S | **done** |
| | [TASK-010813](tasks/TASK-010813-serialization-dependency.md) Take the kotlinx.serialization dependency behind a narrowed guard | S | **done** |
| | [TASK-010827](tasks/TASK-010827-simulation-invariants.md) The invariants a simulated hand must never break | S | **done** |
| | [TASK-010814](tasks/TASK-010814-card-serializer.md) A card serialises as its own notation | S | **done** |
| | [TASK-010815](tasks/TASK-010815-player-action-serializable.md) PlayerAction is serializable under a short type name | S | **done** |
| | [TASK-010816](tasks/TASK-010816-concrete-events-serializable.md) Every betting and dealer event is serializable | S | **done** |
| | [TASK-010817](tasks/TASK-010817-game-event-hierarchy-serializable.md) The whole GameEvent hierarchy serialises polymorphically | S | **done** |
| | [TASK-010818](tasks/TASK-010818-hand-log-serializable.md) A hand log round-trips through JSON | S | **done** |
| | [TASK-010819](tasks/TASK-010819-hand-log-codec-and-version.md) Read and write a hand log, refusing an unknown version | S | **done** |
| | [TASK-010823](tasks/TASK-010823-blind-types-serializable.md) The blind types are serializable | XS | **done** |
| | [TASK-010824](tasks/TASK-010824-duel-format-serializable.md) DuelFormat and its end condition are serializable | S | **done** |
| | [TASK-010825](tasks/TASK-010825-match-event-serializable.md) DuelOutcome and MatchFinished are serializable | S | **done** |
| | [TASK-010820](tasks/TASK-010820-match-log.md) MatchLog, the record of a whole duel | S | **done** |
| | [TASK-010821](tasks/TASK-010821-logged-duel-player.md) Play a whole duel and keep its log | S | **done** |
| | [TASK-010822](tasks/TASK-010822-replay-a-match.md) Replay a whole duel from its log | S | **done** |
| | [TASK-010826](tasks/TASK-010826-match-log-codec.md) Read and write a match log, version guard included | S | **done** |
| | [TASK-010828](tasks/TASK-010828-duel-simulator.md) Simulate one duel between two bots, checking after every action | S | **done** |
| | [TASK-010829](tasks/TASK-010829-simulation-runner.md) Run a thousand duels and report on them | S | **done** |
| | [TASK-010830](tasks/TASK-010830-soak-run.md) A hundred thousand duels, off the default test task | S | **done** |
| | [TASK-010831](tasks/TASK-010831-nested-hand-log-version.md) A hand log with an unknown version is refused inside a match log too | S | **done** |

**131 tasks total.** All stories are migrated to schema 2. `STORY-0107` and `STORY-0108` run concurrently: `STORY-0108`'s
hand-level tickets need only the engine as it stands, while its match-level tail waits on `STORY-0107`.

---

## EPIC-02 — Duel server

Two roots are startable at once: `STORY-0201` (`:poker-server`) and `STORY-0204` (`poker-engine`) —
different modules, no shared file. Off the scaffold, `STORY-0202`, `STORY-0206` and `STORY-0209`
are three independent branches.

Critical path: `0201 → 0202 → 0205 → 0207 → 0210 → 0211 → 0212`.

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| **[STORY-0201](stories/STORY-0201-server-module-scaffold.md)** Server module and build scaffold — *schema 2* | | **done** |
| | [TASK-020101](tasks/TASK-020101-poker-server-module.md) Add the `:poker-server` module and its Ktor dependencies | S | **done** |
| | [TASK-020102](tasks/TASK-020102-server-module-smoke-test.md) Assert the engine and Ktor are on the classpath | XS | **done** |
| | [TASK-020103](tasks/TASK-020103-server-config.md) Read every tunable from one typed `ServerConfig` | S | **done** |
| | [TASK-020104](tasks/TASK-020104-application-conf.md) Ship `application.conf` and load `ServerConfig` from it | S | **done** |
| | [TASK-020105](tasks/TASK-020105-health-route.md) Boot Ktor on Netty and answer `GET /health` | S | **done** |
| | [TASK-020106](tasks/TASK-020106-content-negotiation-and-websockets.md) Install `ContentNegotiation` and `WebSockets` | S | **done** |
| **[STORY-0204](stories/STORY-0204-player-view-projection.md)** `PlayerView` — per-recipient projection — *schema 2* | | **done** |
| | [TASK-020401](tasks/TASK-020401-board-serializable.md) Make `Board` serializable | XS | **done** |
| | [TASK-020402](tasks/TASK-020402-seat-view.md) A seat as a recipient may see it | S | **done** |
| | [TASK-020406](tasks/TASK-020406-event-filter-per-seat.md) Filter an event for one recipient | S | **done** |
| | [TASK-020409](tasks/TASK-020409-observed-duel-harness.md) A duel harness recording every state and event | S | **done** |
| | [TASK-020403](tasks/TASK-020403-player-view-type.md) The `PlayerView` type | S | **done** |
| | [TASK-020404](tasks/TASK-020404-player-view-of.md) Project a state into one seat's view | S | **done** |
| | [TASK-020405](tasks/TASK-020405-player-view-reveal.md) Show a hand the engine has already revealed | S | **done** |
| | [TASK-020407](tasks/TASK-020407-revealed-seats.md) Name the seats a hand has already revealed | XS | **done** |
| | [TASK-020408](tasks/TASK-020408-player-view-carries-no-secret.md) Assert a view carries no deck, rng or seed | S | **done** |
| | [TASK-020410](tasks/TASK-020410-view-leak-property.md) No view shows a card its viewer may not see, over 1000 duels | S | **done** |
| | [TASK-020411](tasks/TASK-020411-event-stream-leak-property.md) No filtered event stream leaks a card, over 1000 duels | S | **done** |
| **[STORY-0202](stories/STORY-0202-wire-protocol.md)** The wire protocol, defined once in Kotlin — *schema 2* | | **done** |
| | [TASK-020201](tasks/TASK-020201-protocol-json-and-version.md) Serialization plugin, PROTOCOL_VERSION, one shared Json | S | **done** |
| | [TASK-020202](tasks/TASK-020202-rejection-serializable.md) Rejection serializable, explicit discriminators | S | **done** |
| | [TASK-020203](tasks/TASK-020203-legal-actions-serializable.md) LegalActions serializable, defaults on the wire | S | **done** |
| | [TASK-020204](tasks/TASK-020204-client-message.md) ClientMessage — intent only | S | **done** |
| | [TASK-020205](tasks/TASK-020205-server-message-handshake.md) ProtocolError, Welcome and Failure | S | **done** |
| | [TASK-020206](tasks/TASK-020206-server-message-duel.md) Snapshot, Events, YourTurn, Rejected | S | **done** |
| | [TASK-020207](tasks/TASK-020207-handshake.md) handshake() refuses a version mismatch | S | **done** |
| | [TASK-020208](tasks/TASK-020208-protocol-codec.md) ProtocolCodec — typed failure, never a throw | S | **done** |
| | [TASK-020209](tasks/TASK-020209-codec-refuses-junk.md) One bad frame is a value, not an exception | S | **done** |
| | [TASK-020210](tasks/TASK-020210-explicit-discriminators.md) Every discriminator is an explicit @SerialName | S | **done** |
| | [TASK-020211](tasks/TASK-020211-no-forbidden-payload.md) No seed goes out, no card comes in | S | **done** |
| | [TASK-020212](tasks/TASK-020212-protocol-doc.md) docs/protocol.md and the test that keeps it honest | S | **done** |
| | [TASK-020213](tasks/TASK-020213-frame-limits.md) A frame too large or too deeply nested is refused before parsing | S | **done** |
| [STORY-0203](stories/STORY-0203-generated-typescript-protocol.md) | Generated TypeScript protocol types | blocked |
| **[STORY-0205](stories/STORY-0205-sessions-and-socket-lifecycle.md)** Sessions and the socket lifecycle — *schema 2* | | ready |
| | [TASK-020501](tasks/TASK-020501-websocket-test-client.md) Put the WebSocket test client and coroutines on the poker-server classpath | S | **done** |
| | [TASK-020502](tasks/TASK-020502-player-directory-port.md) Declare the PlayerDirectory port and an in-memory implementation for tests | S | **done** |
| | [TASK-020503](tasks/TASK-020503-device-id-source.md) Mint opaque device ids from an injected secure random source | S | **done** |
| | [TASK-020504](tasks/TASK-020504-session-registry.md) A SessionRegistry that maps a connection to a session and drops it exactly once | S | **done** |
| | [TASK-020505](tasks/TASK-020505-connection-writer.md) One writer per connection, fed by a channel | S | **done** |
| | [TASK-020506](tasks/TASK-020506-socket-dependencies-and-fixtures.md) Bundle the socket's collaborators into SocketDependencies with a test fixture | XS | **done** |
| | [TASK-020507](tasks/TASK-020507-ws-route-handshake-gate.md) Open /ws behind a mandatory handshake and one writing coroutine | S | **done** |
| | [TASK-020508](tasks/TASK-020508-session-lifecycle.md) Resolve the profile on Welcome and drop the session on every close path | S | **done** |
| | [TASK-020509](tasks/TASK-020509-frame-loop-survives-junk.md) A bad frame mid-session earns a Failure and never closes the socket | S | **done** |
| | [TASK-020510](tasks/TASK-020510-hostile-frame-does-not-kill-the-socket.md) A nesting bomb or an oversized frame is answered, not fatal, at the socket | XS | **done** |
| | [TASK-020511](tasks/TASK-020511-second-socket-policy.md) Decide and enforce what a second socket for one device id does | S | blocked |
| | [TASK-020512](tasks/TASK-020512-socket-uses-configured-limits.md) The socket enforces the operator's frame limits | S | **done** |
| **[STORY-0206](stories/STORY-0206-rooms-and-matchmaking.md)** Rooms, join links and rematch — *schema 2* | | ready |
| | [TASK-020601](tasks/TASK-020601-server-clock.md) Declare the injectable ServerClock and a test clock that never sleeps | XS | **done** |
| | [TASK-020602](tasks/TASK-020602-room-code-type.md) A RoomCode value type that only accepts a human-typable code | S | **done** |
| | [TASK-020603](tasks/TASK-020603-room-code-source.md) Mint room codes from an injected secure source, never from the engine Rng | S | **done** |
| | [TASK-020604](tasks/TASK-020604-room-state.md) The Room value and its four states, with the seating invariants in the type | S | **done** |
| | [TASK-020605](tasks/TASK-020605-room-join.md) Seat the second player, and refuse the third | S | **done** |
| | [TASK-020606](tasks/TASK-020606-room-finish-and-abandon.md) Finish, abandon and touch a room | S | backlog |
| | [TASK-020607](tasks/TASK-020607-room-rematch.md) Both seats must offer before a rematch starts, and the button changes sides | S | backlog |
| | [TASK-020608](tasks/TASK-020608-room-timeouts.md) RoomTimeouts, the two idle limits a room is reaped against | XS | **done** |
| | [TASK-020609](tasks/TASK-020609-room-registry-create.md) A RoomRegistry that creates a room under a code nobody else holds | S | **done** |
| | [TASK-020610](tasks/TASK-020610-room-registry-join.md) Join by code under the room's lock, so a hundred racing joiners seat exactly one | S | backlog |
| | [TASK-020611](tasks/TASK-020611-room-registry-lifecycle.md) Finish, abandon and offer a rematch through the registry | S | backlog |
| | [TASK-020612](tasks/TASK-020612-reap-idle-rooms.md) Reap idle rooms on the injected clock, and never a room that is playing | S | backlog |
| | [TASK-020614](tasks/TASK-020614-concurrent-room-creation.md) Two concurrent creators never receive the same room code | S | backlog |
| | [TASK-020613](tasks/TASK-020613-room-timeouts-in-server-config.md) Read the room idle limits from ServerConfig instead of a literal | S | **done** |
| [STORY-0207](stories/STORY-0207-duel-runner.md) | The duel runner — the engine behind the socket | backlog |
| [STORY-0208](stories/STORY-0208-disconnect-grace-period.md) | Disconnect, grace period and reconnect | backlog |
| **[STORY-0209](stories/STORY-0209-postgres-schema-and-migrations.md)** PostgreSQL — schema, migrations, pool — *schema 2* | | **blocked: no Docker** |
| | [TASK-020901](tasks/TASK-020901-database-dependencies.md) Database dependencies in the catalog and server build | XS | **done** |
| | [TASK-020902](tasks/TASK-020902-database-settings-in-server-config.md) Database URL, credentials and pool size from ServerConfig | S | **done** |
| | [TASK-020903](tasks/TASK-020903-postgres-test-harness.md) One PostgreSQL container, and what a missing Docker means | S | **done** |
| | [TASK-020904](tasks/TASK-020904-initial-schema-and-flyway.md) Initial schema, applied with Flyway | S | backlog |
| | [TASK-020905](tasks/TASK-020905-signed-coin-columns.md) A negative coin balance round-trips through PostgreSQL | S | backlog |
| | [TASK-020906](tasks/TASK-020906-schema-constraints.md) The schema refuses a duplicate device id or result row | S | backlog |
| | [TASK-020907](tasks/TASK-020907-hikari-connection-pool.md) A HikariCP pool from ServerConfig | S | **done** |
| | [TASK-020908](tasks/TASK-020908-migrate-at-startup.md) Migrate at startup; a second startup is a no-op | S | backlog |
| | [TASK-020909](tasks/TASK-020909-local-development-database.md) A local database for a fresh clone | S | backlog |
| | [TASK-020910](tasks/TASK-020910-pin-docker-api-version.md) The test JVM speaks a Docker API version modern daemons accept | XS | **done** |
| | [TASK-020911](tasks/TASK-020911-expose-container-coordinates.md) The test harness hands out database coordinates without a cast | XS | backlog |
| [STORY-0210](stories/STORY-0210-profiles-results-and-coins.md) | Profiles, duel results and duel coins | backlog |
| [STORY-0211](stories/STORY-0211-read-path-coins-and-recent-duels.md) | The read path — my coins and my recent duels | backlog |
| [STORY-0212](stories/STORY-0212-end-to-end-duel-over-a-socket.md) | A real duel over a real socket, end to end | backlog |

Stories are written; tickets come from `/plan-story` as each is reached.

---

## Open decisions

| ID | Question | Where | Due |
| --- | --- | --- | --- |
| DEC-001 | What exactly is one duel? | [`docs/duel-rules.md`](../docs/duel-rules.md) | before v0.2 |
| DEC-002 | Evaluator performance budget, how it is measured, and whether `HandRank` becomes a packed integer | [`STORY-0103`](stories/STORY-0103-hand-evaluator.md) | before benchmark tooling lands |

**Answered.** `DEC-004` → [`ADR-0008`](../docs/adr/ADR-0008-loser-mucks-at-showdown.md) (the loser
mucks). `DEC-005` → [`ADR-0009`](../docs/adr/ADR-0009-match-events-are-their-own-hierarchy.md)
(match events are their own hierarchy). `DEC-006` →
[`ADR-0010`](../docs/adr/ADR-0010-engine-takes-a-serialization-dependency.md) (the engine may
depend on `kotlinx.serialization`; `checkNoDependencies` is narrowed, not deleted).
| — | Public repo or GitHub Pro, to enable branch protection? | [`TASK-000102`](tasks/TASK-000102-enable-branch-protection.md) | before v0.1 |

---

## Metrics

Updated as epics close. See [`docs/workflow.md`](../docs/workflow.md) — these are only worth
recording if they are recorded when unflattering.

| | EPIC-01 | Total |
| --- | --- | --- |
| Tasks completed | 131 / 131 | 132 / 134 |
| Accepted on first review | — | — |
| Average review iterations | — | — |
| Test lines / production lines | — | — |
| Tasks re-scoped mid-flight | — | — |
| Reviews skipped (must stay 0) | 0 | 0 |
| Tickets promoted haiku → sonnet | 1 | 1 |
| Average coder dispatches per ticket | — | — |
| Manual human edits | — | — |
