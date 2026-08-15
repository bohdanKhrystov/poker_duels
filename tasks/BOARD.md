# Board

The index. Conventions live in [`README.md`](README.md).

**Now:** `EPIC-01` and `EPIC-02` are **done** — the engine, and a duel server that plays a whole
duel over two real sockets against PostgreSQL, pays the winner a coin, and survives a disconnect.
`EPIC-06` (design) runs in parallel on disjoint files, see `ADR-0023`. `EPIC-03` (web client) is
next and not yet written.

`EPIC-11` (status notifications) is **ahead of `EPIC-03`, `EPIC-04` and `EPIC-05` in the
queue**, by the human's instruction on 2026-08-15. Those three are the first epics long enough
for a silent stall to cost a night, so the run learns to report itself before it runs them
rather than after the first one goes quiet. It touches no file any of them touch —
`scripts/notify/`, `docs/notifications.md` and the skill.

Startable right now: `python3 .github/scripts/lint_tickets.py --startable`

---

## Epics

| ID | Title | Status | Milestone |
| --- | --- | --- | --- |
| [EPIC-00](epics/EPIC-00-ways-of-working.md) | Ways of working | **in progress** | v0.1 |
| [EPIC-01](epics/EPIC-01-poker-engine.md) | Poker engine | **done** | v0.1 |
| [EPIC-02](epics/EPIC-02-duel-server.md) | Duel server — rooms, WebSocket protocol, persistence | **done** | v0.1 |
| [EPIC-03](epics/EPIC-03-web-client.md) | Web client — table, lobby, duel flow | **ready** | v0.1 |
| [EPIC-04](epics/EPIC-04-identity-and-profiles.md) | Identity and profiles | **backlog** | v0.2 |
| EPIC-05 | Ranking, duel coins and leaderboard | *not written* | v0.3 |
| [EPIC-06](epics/EPIC-06-design-system-and-art.md) | Design system and art | **done** | v0.2 |
| EPIC-07 | Infrastructure and delivery | *not written* | v0.2 |
| EPIC-08 | Analysis and decision quality | *not written* | later |
| EPIC-09 | Bots and simulation | *not written* | later |
| EPIC-10 | The AI software factory — the case study | *not written* | continuous |
| [EPIC-11](epics/EPIC-11-status-notifications.md) | Status notifications — the run reports itself | **in progress** | v0.1 |

Numbers 03–05 and 07–10 are **reserved**, not planned in detail. Epics are written when the one
before them is close to done, because writing them earlier means rewriting them. `EPIC-06` is
the recorded exception: opened ahead of its slot because design shares no file with the server
work (`ADR-0024`).

---

## EPIC-00 — Ways of working

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| **[STORY-0001](stories/STORY-0001-repository-and-ticket-system.md)** Repository, docs, tickets | | | ready |
| | [TASK-000101](tasks/TASK-000101-bootstrap-repository.md) Bootstrap repository and ticket system | M | **done** |
| | [TASK-000102](tasks/TASK-000102-enable-branch-protection.md) Enable branch protection | S | **done** |
| | [TASK-000103](tasks/TASK-000103-token-lean-agent-workflow.md) Token-lean agent workflow | S | **in-review** |

`TASK-000102` is **done**. The repository went public on 2026-08-13, which made protection and
Actions minutes free at once, and `develop` is now protected: a pull request and two green checks
to land, no force pushes, no deletions. Required approvals are deliberately **0** — one would
deadlock the agent run, since an agent cannot approve its own PR.

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
| **[STORY-0203](stories/STORY-0203-generated-typescript-protocol.md)** Generated TypeScript protocol types — *schema 2* | | **done** |
| | [TASK-020301](tasks/TASK-020301-descriptor-to-typescript-type-reference.md) Map a serial descriptor to a TypeScript type reference | S | **done** |
| | [TASK-020302](tasks/TASK-020302-interface-for-a-class-descriptor.md) Emit a TypeScript interface for a class or object descriptor | S | **done** |
| | [TASK-020303](tasks/TASK-020303-unions-for-enums-and-sealed-hierarchies.md) Emit a TypeScript union for an enum and for a sealed hierarchy | S | **done** |
| | [TASK-020304](tasks/TASK-020304-walk-both-roots-into-ordered-declarations.md) Walk both message roots into an ordered list of declarations | S | **done** |
| | [TASK-020305](tasks/TASK-020305-assemble-the-file-header-and-version.md) Assemble the file with its header and the protocol version alias | XS | **done** |
| | [TASK-020306](tasks/TASK-020306-every-discriminator-is-its-serial-name.md) Every variant's TypeScript discriminator is its SerialName | S | **done** |
| | [TASK-020307](tasks/TASK-020307-generate-task-writes-the-committed-file.md) generateProtocolTypes writes the committed TypeScript file | S | **done** |
| | [TASK-020308](tasks/TASK-020308-verify-task-fails-the-build-on-drift.md) verifyProtocolTypes byte-compares on every check | S | **done** |
| | [TASK-020309](tasks/TASK-020309-ci-typechecks-the-generated-file.md) CI typechecks the generated file under strict | XS | **done** |
| | [TASK-020310](tasks/TASK-020310-the-protocol-document-names-the-generated-file.md) The protocol document names the generated file and its command | XS | **done** |
| **[STORY-0205](stories/STORY-0205-sessions-and-socket-lifecycle.md)** Sessions and the socket lifecycle — *schema 2* | | **done** |
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
| | [TASK-020511](tasks/TASK-020511-second-socket-policy.md) Decide and enforce what a second socket for one device id does | S | **done** |
| | [TASK-020512](tasks/TASK-020512-socket-uses-configured-limits.md) The socket enforces the operator's frame limits | S | **done** |
| | [TASK-020513](tasks/TASK-020513-the-concurrency-test-races-on-its-own-list.md) The concurrency test races on its own result list | XS | **done** |
| **[STORY-0206](stories/STORY-0206-rooms-and-matchmaking.md)** Rooms, join links and rematch — *schema 2* | | **done** |
| | [TASK-020601](tasks/TASK-020601-server-clock.md) Declare the injectable ServerClock and a test clock that never sleeps | XS | **done** |
| | [TASK-020602](tasks/TASK-020602-room-code-type.md) A RoomCode value type that only accepts a human-typable code | S | **done** |
| | [TASK-020603](tasks/TASK-020603-room-code-source.md) Mint room codes from an injected secure source, never from the engine Rng | S | **done** |
| | [TASK-020604](tasks/TASK-020604-room-state.md) The Room value and its four states, with the seating invariants in the type | S | **done** |
| | [TASK-020605](tasks/TASK-020605-room-join.md) Seat the second player, and refuse the third | S | **done** |
| | [TASK-020606](tasks/TASK-020606-room-finish-and-abandon.md) Finish, abandon and touch a room | S | **done** |
| | [TASK-020607](tasks/TASK-020607-room-rematch.md) Both seats must offer before a rematch starts, and the button changes sides | S | **done** |
| | [TASK-020608](tasks/TASK-020608-room-timeouts.md) RoomTimeouts, the two idle limits a room is reaped against | XS | **done** |
| | [TASK-020609](tasks/TASK-020609-room-registry-create.md) A RoomRegistry that creates a room under a code nobody else holds | S | **done** |
| | [TASK-020610](tasks/TASK-020610-room-registry-join.md) Join by code under the room's lock, so a hundred racing joiners seat exactly one | S | **done** |
| | [TASK-020611](tasks/TASK-020611-room-registry-lifecycle.md) Finish, abandon and offer a rematch through the registry | S | **done** |
| | [TASK-020612](tasks/TASK-020612-reap-idle-rooms.md) Reap idle rooms on the injected clock, and never a room that is playing | S | **done** |
| | [TASK-020614](tasks/TASK-020614-concurrent-room-creation.md) Two concurrent creators never receive the same room code | S | **done** |
| | [TASK-020613](tasks/TASK-020613-room-timeouts-in-server-config.md) Read the room idle limits from ServerConfig instead of a literal | S | **done** |
| | [TASK-020615](tasks/TASK-020615-room-registry-finish-is-called-by-nobody.md) RoomRegistry.finish is called by no production code — remove it or say why it stays | XS | **done** |
| [STORY-0207](stories/STORY-0207-duel-runner.md) | The duel runner — the engine behind the socket | **done** |
| | [TASK-020701](tasks/TASK-020701-hand-seed-source.md) Draw each hand's seed from an injected secure source, never from the engine Rng | XS | **done** |
| | [TASK-020702](tasks/TASK-020702-per-seat-broadcast.md) Every outbound frame is addressed to one seat and built by the engine's projection layer | S | **done** |
| | [TASK-020703](tasks/TASK-020703-your-turn-frame.md) The seat on turn gets YourTurn with the engine's legal actions, and the other seat gets nothing | S | **done** |
| | [TASK-020704](tasks/TASK-020704-duel-runner-value.md) The DuelRunner value — a live hand, its match, its logs, and the invariants tying them together | S | **done** |
| | [TASK-020705](tasks/TASK-020705-open-a-hand-and-a-duel.md) Open a hand from a seed, and open the duel's first one | S | **done** |
| | [TASK-020706](tasks/TASK-020706-guard-inbound-actions.md) A replayed frame is dropped and a frame acting for the opponent is refused, before the engine sees either | S | **done** |
| | [TASK-020707](tasks/TASK-020707-hand-boundary-and-duel-end.md) Fold a finished hand back into the duel, deal the next one, or end the duel | S | **done** |
| | [TASK-020708](tasks/TASK-020708-apply-an-inbound-action.md) An inbound Act reaches the engine, and its result reaches exactly the seats entitled to it | S | **done** |
| | [TASK-020709](tasks/TASK-020709-duel-result-sink-port.md) Declare the DuelResultSink port at its consumer, so this story stays free of the database | XS | **done** |
| | [TASK-020710](tasks/TASK-020710-play-a-duel-through-the-runner.md) A harness that plays a whole duel through the runner, seeing only what a client would see | S | **done** |
| | [TASK-020711](tasks/TASK-020711-chips-conserved-from-the-clients-side.md) Chips are conserved in what the client sees, not just in what the engine knows | S | **done** |
| | [TASK-020712](tasks/TASK-020712-nothing-secret-leaves-the-runner.md) No opponent's card and no hand seed ever leaves the runner, and transport filters nothing itself | S | **done** |
| | [TASK-020713](tasks/TASK-020713-the-log-replays-the-duel-the-server-played.md) The MatchLog the runner wrote replays into the duel the server actually played | S | **done** |
| | [TASK-020714](tasks/TASK-020714-host-the-live-runner-in-a-room.md) Give the live DuelRunner a home in the room, and publish the duel when it ends | S | **done** |
| | [TASK-020716](tasks/TASK-020716-distinctive-seeds-close-the-seed-check.md) Distinctive seeds close the hand-one hole in the seed-leak check | XS | **done** |
| | [TASK-020717](tasks/TASK-020717-a-finished-duel-is-recorded-at-least-once.md) A finished duel is recorded at least once, not at most once | S | **done** |
| | [TASK-020718](tasks/TASK-020718-the-document-pins-the-wire-vocabulary.md) The wire vocabulary is pinned in one place — the protocol document | XS | **done** |
| | [TASK-020732](tasks/TASK-020732-tests-pin-the-version-by-its-literal.md) Three tests pin the protocol version by its literal instead of by the constant | XS | **done** |
| | [TASK-020719](tasks/TASK-020719-protocol-version-two.md) The wire protocol moves to version 2 | XS | **done** |
| | [TASK-020720](tasks/TASK-020720-duel-finished-message.md) ServerMessage.DuelFinished carries the duel's outcome | XS | **done** |
| | [TASK-020721](tasks/TASK-020721-finished-duel-frames.md) The projection layer builds the finished-duel frames, and only it may | S | **done** |
| | [TASK-020722](tasks/TASK-020722-a-finished-duel-tells-both-seats.md) A duel that ends says so, in the same step that ends it | S | **done** |
| | [TASK-020723](tasks/TASK-020723-connection-directory.md) A directory of live connection writers, keyed by the player behind them | S | **done** |
| | [TASK-020724](tasks/TASK-020724-the-registry-names-its-seed-source.md) A room registry says which seed source its duels draw from | XS | **done** |
| | [TASK-020725](tasks/TASK-020725-seating-yields-the-opening-frames.md) Seating the second player hands back the opening hand's frames | S | **done** |
| | [TASK-020726](tasks/TASK-020726-socket-dependencies-carry-rooms-and-writers.md) The socket's dependencies carry the rooms and the connection directory | XS | **done** |
| | [TASK-020727](tasks/TASK-020727-room-joined-message.md) ServerMessage.RoomJoined names the room and the seat the server gave you | XS | **done** |
| | [TASK-020728](tasks/TASK-020728-client-messages-name-a-room.md) ClientMessage learns to open a room and to join one by code | XS | **done** |
| | [TASK-020729](tasks/TASK-020729-a-writer-findable-by-its-player.md) A live connection's writer is findable by the player behind it | S | **done** |
| | [TASK-020730](tasks/TASK-020730-deliver-an-addressed-to-its-seat.md) Each Addressed is encoded once and written to that seat's writer only | S | **done** |
| | [TASK-020731](tasks/TASK-020731-room-messages-reach-the-registry.md) CreateRoom and JoinRoom reach the registry, and the opening hand reaches both seats | S | **done** |
| | [TASK-020715](tasks/TASK-020715-an-act-frame-reaches-the-duel.md) An Act arriving on a socket reaches the duel, and the duel's frames reach both sockets | S | **done** |
| | [TASK-020733](tasks/TASK-020733-a-rematch-hands-back-its-opening-frames.md) A rematch hands back its opening frames, the way seating does | S | **done** |
| | [TASK-020734](tasks/TASK-020734-the-rejoin-path-cannot-assume-the-room-survived.md) The rejoin path cannot assume the room survived its own refusal | XS | **done** |
| **[STORY-0208](stories/STORY-0208-disconnect-grace-period.md)** Disconnect, grace period and reconnect — *schema 2* | | **done** |
| | [TASK-020801](tasks/TASK-020801-room-timeouts-carry-the-grace-window.md) RoomTimeouts carries the disconnect grace window | XS | **done** |
| | [TASK-020802](tasks/TASK-020802-the-grace-window-is-configuration.md) The grace window is configuration, read once in ServerConfig | XS | **done** |
| | [TASK-020803](tasks/TASK-020803-a-paused-duel-has-its-own-protocol-error.md) A paused duel has its own protocol error, and the document lists it | XS | **done** |
| | [TASK-020804](tasks/TASK-020804-the-room-records-who-is-gone.md) The room records which seats are inside a grace window and which are absent | S | **done** |
| | [TASK-020805](tasks/TASK-020805-disconnect-reconnect-and-expiry-on-the-room.md) Disconnect starts the window, reconnect clears it, expiry makes the seat absent | S | **done** |
| | [TASK-020806](tasks/TASK-020806-an-absent-seat-folds-when-the-turn-reaches-it.md) An absent seat folds, as an ordinary action, whenever the turn reaches it | S | **done** |
| | [TASK-020807](tasks/TASK-020807-a-paused-room-refuses-an-action.md) A paused room refuses an action and moves nothing | S | **done** |
| | [TASK-020808](tasks/TASK-020808-the-room-folds-for-a-seat-nobody-is-in.md) The room folds for a seat nobody is sitting in, so the duel never stalls | S | **done** |
| | [TASK-020809](tasks/TASK-020809-the-registry-starts-the-window.md) The registry starts a seat's window on its own clock and configured limit | S | **done** |
| | [TASK-020810](tasks/TASK-020810-the-frames-a-returning-player-is-entitled-to.md) The frames a returning player is entitled to, rebuilt through the projection layer | S | **done** |
| | [TASK-020811](tasks/TASK-020811-the-registry-resumes-a-returning-player.md) The registry resumes a returning player, and nobody else | S | **done** |
| | [TASK-020812](tasks/TASK-020812-the-window-running-out-folds-the-hand.md) The window running out folds the hand, and both seats gone ends the room | S | **done** |
| | [TASK-020813](tasks/TASK-020813-a-closing-socket-tells-the-room-its-seat-is-gone.md) A closing socket tells the room its seat is gone, unless a newer socket took it | S | **done** |
| | [TASK-020814](tasks/TASK-020814-a-returning-socket-picks-up-where-it-left-off.md) A returning socket picks up where it left off, and another device does not | S | **done** |
| | [TASK-020815](tasks/TASK-020815-the-configured-window-decides-the-instant.md) The configured window decides the instant, on a clock that never sleeps | S | **done** |
| | [TASK-020816](tasks/TASK-020816-an-absent-seat-gives-up-with-a-legal-action.md) An absent seat gives up with an action the engine will accept | S | **done** |
| **[STORY-0209](stories/STORY-0209-postgres-schema-and-migrations.md)** PostgreSQL — schema, migrations, pool — *schema 2* | | **done** |
| | [TASK-020901](tasks/TASK-020901-database-dependencies.md) Database dependencies in the catalog and server build | XS | **done** |
| | [TASK-020902](tasks/TASK-020902-database-settings-in-server-config.md) Database URL, credentials and pool size from ServerConfig | S | **done** |
| | [TASK-020903](tasks/TASK-020903-postgres-test-harness.md) One PostgreSQL container, and what a missing Docker means | S | **done** |
| | [TASK-020904](tasks/TASK-020904-initial-schema-and-flyway.md) Initial schema, applied with Flyway | S | **done** |
| | [TASK-020905](tasks/TASK-020905-signed-coin-columns.md) A negative coin balance round-trips through PostgreSQL | S | **done** |
| | [TASK-020906](tasks/TASK-020906-schema-constraints.md) The schema refuses a duplicate device id or result row | S | **done** |
| | [TASK-020907](tasks/TASK-020907-hikari-connection-pool.md) A HikariCP pool from ServerConfig | S | **done** |
| | [TASK-020908](tasks/TASK-020908-migrate-at-startup.md) Migrate at startup; a second startup is a no-op | S | **done** |
| | [TASK-020909](tasks/TASK-020909-local-development-database.md) A local database for a fresh clone | S | **done** |
| | [TASK-020910](tasks/TASK-020910-pin-docker-api-version.md) The test JVM speaks a Docker API version modern daemons accept | XS | **done** |
| | [TASK-020911](tasks/TASK-020911-expose-container-coordinates.md) The test harness hands out database coordinates without a cast | XS | **done** |
| [STORY-0210](stories/STORY-0210-profiles-results-and-coins.md) | Profiles, duel results and duel coins | **done** |
| | [TASK-021001](tasks/TASK-021001-coin-award-rule.md) Map a DuelOutcome to two signed coin deltas, in one function | S | **done** |
| | [TASK-021002](tasks/TASK-021002-finished-duel-record.md) Describe a finished duel as the write path's input | S | **done** |
| | [TASK-021003](tasks/TASK-021003-postgres-player-directory.md) Resolve a device id to a durable profile, creating it at most once | S | **done** |
| | [TASK-021004](tasks/TASK-021004-concurrent-first-contact.md) Prove concurrent first contact from one device creates one profile | XS | **done** |
| | [TASK-021005](tasks/TASK-021005-duel-result-store-rows.md) Record a finished duel as one duel row and two result rows, in one transaction | S | **done** |
| | [TASK-021006](tasks/TASK-021006-move-the-coins-in-the-same-transaction.md) Move both coin balances inside the same transaction, by SQL increment | S | **done** |
| | [TASK-021007](tasks/TASK-021007-failed-write-leaves-no-row.md) Prove a failure part-way through recording leaves no row and no coin behind | XS | **done** |
| | [TASK-021008](tasks/TASK-021008-a-draw-pays-nothing.md) Prove a drawn duel is recorded and pays nobody | XS | **done** |
| | [TASK-021009](tasks/TASK-021009-idempotent-on-duel-id.md) Make recording idempotent on the duel id, so a retry pays once | S | **done** |
| | [TASK-021010](tasks/TASK-021010-balances-are-never-floored.md) Prove ten losses read back as minus ten, and that nothing floors a balance | S | **done** |
| | [TASK-021011](tasks/TASK-021011-concurrent-duels-both-land.md) Prove two duels finishing at once for one player both land | S | **done** |
| | [TASK-021012](tasks/TASK-021012-survives-a-restart.md) Prove profiles, results and balances survive a restart | S | **done** |
| | [TASK-021013](tasks/TASK-021013-the-store-satisfies-the-sink.md) The Postgres store satisfies the DuelResultSink port | S | **done** |
| | [TASK-021014](tasks/TASK-021014-hands-played-column.md) The duel table records how many hands were played | S | **done** |
| | [TASK-021015](tasks/TASK-021015-migration-tests-live-with-migrations.md) The migration tests live with the migrations | XS | **done** |
| [STORY-0211](stories/STORY-0211-read-path-coins-and-recent-duels.md) | The read path — my coins and my recent duels | **done** |
| | [TASK-021101](tasks/TASK-021101-read-path-response-types.md) Declare the profile and duel-summary response types | S | **done** |
| | [TASK-021102](tasks/TASK-021102-outcome-from-a-stored-delta.md) Read won, lost or drew off a stored coin delta | XS | **done** |
| | [TASK-021103](tasks/TASK-021103-recent-duels-limit.md) Parse, default and cap the recent-duels limit | XS | **done** |
| | [TASK-021104](tasks/TASK-021104-profile-reads-port-and-balance.md) Read a device's profile and balance behind a ProfileReads port | S | **done** |
| | [TASK-021105](tasks/TASK-021105-the-balance-read-back-is-the-stored-one.md) Prove the balance read back is the one the duels wrote, minus one included | XS | **done** |
| | [TASK-021106](tasks/TASK-021106-recent-duels-query.md) Read a player's recent duels with their opponent in one query | S | **done** |
| | [TASK-021107](tasks/TASK-021107-newest-first-capped-and-mine-only.md) Prove the duel list is newest first, capped, and nobody else's | S | **done** |
| | [TASK-021108](tasks/TASK-021108-a-draw-is-visible-in-the-list.md) Prove a drawn duel is visible in both players' lists | XS | **done** |
| | [TASK-021109](tasks/TASK-021109-the-profile-endpoint.md) Answer GET /api/me for a known device, refuse anything else | S | **done** |
| | [TASK-021110](tasks/TASK-021110-the-recent-duels-endpoint.md) Answer GET /api/me/duels with a bounded, ordered list | S | **done** |
| | [TASK-021111](tasks/TASK-021111-endpoints-against-the-database.md) Read a just-finished duel and its coin back over HTTP, against the database | S | **done** |
| | [TASK-021112](tasks/TASK-021112-document-both-endpoints.md) Document both read endpoints in docs/protocol.md | S | **done** |
| | [TASK-021113](tasks/TASK-021113-one-json-for-the-wire.md) The HTTP routes encode with the same Json the tests assert against | XS | ~~dropped~~ |
| | [TASK-021114](tasks/TASK-021114-hands-played-reaches-the-client.md) Hands played reaches the client instead of null | S | **done** |
| | [TASK-021115](tasks/TASK-021115-the-document-test-checks-claims-not-words.md) The protocol document says handsPlayed is null, and its test cannot tell | S | **done** |
| **[STORY-0212](stories/STORY-0212-end-to-end-duel-over-a-socket.md)** A real duel over a real socket, end to end — *schema 2* | | **done** |
| | [TASK-021201](tasks/TASK-021201-the-servers-real-collaborators.md) Build the server's real collaborators from config and a DataSource | S | **done** |
| | [TASK-021202](tasks/TASK-021202-the-composition-root-installs-every-route.md) One composition root installs the socket and both HTTP routes, and main calls it | S | **done** |
| | [TASK-021203](tasks/TASK-021203-the-route-kdocs-name-their-production-installer.md) Both route KDocs name their production installer instead of a story that has landed | XS | **done** |
| | [TASK-021204](tasks/TASK-021204-a-test-server-on-a-real-database.md) A test server on a real database, with the hand seeds the test chooses | S | **done** |
| | [TASK-021205](tasks/TASK-021205-two-real-sockets-one-room.md) Two real sockets create and join one room by code | S | **done** |
| | [TASK-021206](tasks/TASK-021206-the-clients-play-the-duel-to-a-winner.md) The two clients play a whole duel over the socket to a declared winner | S | **done** |
| | [TASK-021207](tasks/TASK-021207-no-client-ever-received-the-others-cards.md) Neither client ever received the other's hole cards before the reveal | S | **done** |
| | [TASK-021208](tasks/TASK-021208-chips-are-conserved-in-the-frames-received.md) Chips are conserved in the frames the two clients actually received | S | **done** |
| | [TASK-021209](tasks/TASK-021209-the-coins-read-back-over-http.md) The winner's coin is one higher and the loser's one lower, read back over HTTP | S | **done** |
| | [TASK-021210](tasks/TASK-021210-the-duel-in-both-recent-duel-lists.md) The duel appears in both players' recent duels with opposite deltas | S | **done** |
| | [TASK-021211](tasks/TASK-021211-a-dropped-socket-rejoins-and-the-duel-ends-the-same.md) A dropped socket rejoins inside the window and the duel ends the same way | S | **done** |
| | [TASK-021212](tasks/TASK-021212-something-drives-the-periodic-sweeps.md) Something drives the periodic sweeps in the server that ships | S | **done** |
| | [TASK-021213](tasks/TASK-021213-the-sweep-period-is-configuration.md) The sweep period is configuration, read once in ServerConfig | XS | **done** |
| | [TASK-021214](tasks/TASK-021214-a-test-filter-names-one-class.md) A test filter names one class, so a green run cannot have run nothing | XS | **done** |
| | [TASK-021215](tasks/TASK-021215-a-logging-backend-so-a-swallowed-failure-is-visible.md) A logging backend, so a swallowed sweep failure is visible | S | **done** |

Stories are written; tickets come from `/plan-story` as each is reached.

---

## EPIC-06 — Design system and art

The design track: authored in `design/`, mirrored to the claude.ai/design project **Poker
Duels** for visual review, landed through the ordinary ticket lifecycle — `ADR-0024`. Runs in
parallel with `EPIC-02`; no shared file.

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| **[STORY-0601](stories/STORY-0601-design-foundations.md)** Design foundations — tokens and preview cards — *schema 2* | | | **done** |
| | [TASK-060101](tasks/TASK-060101-design-token-sheet.md) The canonical design token sheet | S | **done** |
| | [TASK-060102](tasks/TASK-060102-colors-preview-card.md) The Colors preview card | S | **done** |
| | [TASK-060103](tasks/TASK-060103-type-preview-card.md) The Type preview card | S | **done** |
| | [TASK-060104](tasks/TASK-060104-spacing-preview-card.md) The Spacing preview card | S | **done** |
| | [TASK-060105](tasks/TASK-060105-design-directory-readme.md) The design directory README and sync procedure | XS | **done** |
| | [TASK-060106](tasks/TASK-060106-token-name-drift-check.md) The token-name drift check | S | **done** |
| | [TASK-060107](tasks/TASK-060107-align-the-shipped-foundation-cards.md) Align the shipped foundation cards | S | **done** |
| | [TASK-060108](tasks/TASK-060108-card-surface-tokens.md) The card resting shadow and back texture become tokens | S | done |
| | [TASK-060110](tasks/TASK-060110-no-bare-suit-anywhere.md) No bare suit glyph anywhere, enforced in the drift check | XS | **done** |
| | [TASK-060111](tasks/TASK-060111-drift-check-compares-values.md) The drift check compares values, not only names | S | **done** |
| | [TASK-060112](tasks/TASK-060112-drift-gate-reads-the-graphics.md) The drift gate reads the graphics | S | **done** |
| | [TASK-060113](tasks/TASK-060113-inlined-symbols-match-their-canonicals.md) The gallery's inlined symbols match their canonicals | S | **done** |
| | [TASK-060114](tasks/TASK-060114-lockup-constants-join-the-drift-gate.md) The lockup constants join the drift gate | S | **done** |
| | [TASK-060115](tasks/TASK-060115-the-coin-glint-is-born-on-the-sheet.md) The coin glint is born on the sheet | XS | **done** |
| | [TASK-060116](tasks/TASK-060116-the-css-coins-consume-the-glint-token.md) The CSS coins consume the glint token | XS | **done** |
| | [TASK-060117](tasks/TASK-060117-the-colors-cards-coin-matches-the-canonical.md) The Colors card's coin matches the canonical | XS | **done** |
| | [TASK-060118](tasks/TASK-060118-the-gallery-lede-names-every-mirrored-token.md) The gallery lede names every mirrored token | XS | **done** |
| | [TASK-060119](tasks/TASK-060119-the-coin-face-is-born-on-the-sheet.md) The coin face is born on the sheet | XS | **done** |
| | [TASK-060120](tasks/TASK-060120-the-lockup-coins-consume-the-face-token.md) The lockup coins consume the face token | XS | **done** |
| | [TASK-060121](tasks/TASK-060121-semicolonless-declaration-enters-the-gate.md) The value gate reads CSS values correctly | S | **done** |
| | [TASK-060122](tasks/TASK-060122-the-design-gate-runs-in-ci.md) The design gate runs in CI | XS | **done** |
| | [TASK-060123](tasks/TASK-060123-the-gates-remaining-silent-edges.md) The lockup clause cannot go quiet | XS | **done** |
| **[STORY-0602](stories/STORY-0602-duel-table-screen.md)** Design the duel table — components and the screen — *schema 2* | | | **done** |
| | [TASK-060201](tasks/TASK-060201-playing-card-component.md) The playing-card component | S | **done** |
| | [TASK-060202](tasks/TASK-060202-seat-plate-and-pot.md) The seat plate and pot strip | S | **done** |
| | [TASK-060203](tasks/TASK-060203-action-bar.md) The action bar | S | **done** |
| | [TASK-060204](tasks/TASK-060204-duel-table-screen.md) The duel table screen, in play | S | **done** |
| | [TASK-060205](tasks/TASK-060205-table-states.md) The table's other moments — waiting and showdown | S | **done** |
| | [TASK-060206](tasks/TASK-060206-action-bar-off-state-parity.md) The off-state's hidden sizing row mirrors the live content | XS | **done** |
| | [TASK-060207](tasks/TASK-060207-the-fold-ending.md) The fold ending — a win with nothing shown | S | **done** |
| | [TASK-060208](tasks/TASK-060208-the-in-play-table-shows-the-hidden-hand.md) The in-play table shows the hidden hand | XS | **done** |
| | [TASK-060209](tasks/TASK-060209-the-states-mirrors-use-the-live-bars-elements.md) The states' hidden mirrors use the live bar's elements | XS | **done** |
| | [TASK-060210](tasks/TASK-060210-screens-consume-card-surface-tokens.md) The duel-table screens consume the card-surface tokens | XS | **done** |
| **[STORY-0603](stories/STORY-0603-graphics.md)** Draw the graphics — suit glyphs, the duel coin, the wordmark — *schema 2* | | | **done** |
| | [TASK-060301](tasks/TASK-060301-suit-glyph-set.md) The suit-glyph set | S | **done** |
| | [TASK-060302](tasks/TASK-060302-duel-coin.md) The duel coin | S | **done** |
| | [TASK-060303](tasks/TASK-060303-wordmark-card.md) The wordmark card | S | **done** |
| | [TASK-060304](tasks/TASK-060304-graphics-gallery-card.md) The graphics gallery card | S | **done** |
| | [TASK-060305](tasks/TASK-060305-wordmark-keeps-its-coin-in-forced-colors.md) The wordmark keeps its coin in forced colors | XS | **done** |
| **[STORY-0604](stories/STORY-0604-lobby-and-flow.md)** Design the duel flow — create, join, result, rematch — *schema 2* | | | **done** |
| | [TASK-060401](tasks/TASK-060401-create-and-share-screen.md) The create-and-share screen | S | **done** |
| | [TASK-060402](tasks/TASK-060402-join-screen.md) The join screen | S | **done** |
| | [TASK-060403](tasks/TASK-060403-duel-end-screen.md) The duel-end screen | S | **done** |
| | [TASK-060404](tasks/TASK-060404-rematch-states-card.md) The rematch states | S | **done** |
| | [TASK-060405](tasks/TASK-060405-flow-vocabulary-earns-a-component-card.md) The flow vocabulary earns a component card | S | **done** |
| | [TASK-060406](tasks/TASK-060406-the-typed-code-door.md) The typed-code door | S | **done** |

---

## Open decisions

| ID | Question | Where | Due |
| --- | --- | --- | --- |
| DEC-002 | Evaluator performance budget, how it is measured, and whether `HandRank` becomes a packed integer | [`STORY-0103`](stories/STORY-0103-hand-evaluator.md) | before benchmark tooling lands |
| DEC-037 | **The architect's** — after a `Rejected`, is the decision point still open on the client, and when does a rejection stop being shown? The server sends no fresh `YourTurn` after one and `duel-state.ts` clears `pendingTurn`, so one rejected action ends that player's hand with both players still connected | [`TASK-030712`](tasks/TASK-030712-after-a-rejection-the-player-can-act-again.md) | before STORY-0307 closes |

**Answered.** Seven product decisions were put to the human on 2026-08-15 and all seven
answered, each recorded as its own ADR. `DEC-001` →
[`ADR-0035`](../docs/adr/ADR-0035-a-duel-is-a-freezeout.md) (a duel is a freezeout; the numbers
stay configuration). `DEC-025` →
[`ADR-0036`](../docs/adr/ADR-0036-an-account-is-offered-never-required.md) (never required —
anonymous play stays fully ranked, and an account is offered after a first win, dismissibly).
`DEC-030` → [`ADR-0037`](../docs/adr/ADR-0037-the-device-is-a-credential-until-revoked.md) (the
device signs in until the player revokes it). `DEC-017` →
[`ADR-0038`](../docs/adr/ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md) (a
blocklist screens, an operator may take a name away, and a name taken away is retired forever —
unblocking `STORY-0410`). `DEC-029` →
[`ADR-0039`](../docs/adr/ADR-0039-v01-offers-no-account-deletion.md) (no deletion in v0.1, with
the schema forbidden from foreclosing one). `DEC-009` →
[`ADR-0040`](../docs/adr/ADR-0040-a-duel-may-be-watched-without-hole-cards.md) (a duel may be
watched live, minus every hole card, through a third projection in the engine). `DEC-031` →
[`ADR-0041`](../docs/adr/ADR-0041-a-handle-and-a-password-are-the-only-credential.md) (handle and
password only, for now, and the account screens are designed for one credential).

`DEC-035` → [`ADR-0034`](../docs/adr/ADR-0034-the-value-gate-reads-css-string-aware.md)
(the value gate reads CSS regions string-aware, and refuses by name any shape it cannot read
rather than returning a partial set). The ADR left this row for the driver to strike and seven
board PRs passed without it — a deferred strike is a strike nobody owns.
`DEC-034` → [`ADR-0033`](../docs/adr/ADR-0033-component-anatomy-is-born-in-its-canonical-card.md)
(a component's anatomy is born in its canonical card; the sheet holds the vocabulary.
Minted here as `DEC-032` and renumbered — the ADR directory's register had taken that
number first, so `ADR-0033`'s immutable header cites the original minting; the question
itself, restated there in full, is the durable reference).
`DEC-021` → [`ADR-0024`](../docs/adr/ADR-0024-design-follows-the-code-workflow.md)
(design follows the code workflow, in the repository, mirrored to claude.ai/design).
`DEC-004` → [`ADR-0008`](../docs/adr/ADR-0008-loser-mucks-at-showdown.md) (the loser
mucks). `DEC-005` → [`ADR-0009`](../docs/adr/ADR-0009-match-events-are-their-own-hierarchy.md)
(match events are their own hierarchy). `DEC-006` →
[`ADR-0010`](../docs/adr/ADR-0010-engine-takes-a-serialization-dependency.md) (the engine may
depend on `kotlinx.serialization`; `checkNoDependencies` is narrowed, not deleted). `DEC-019` →
[`ADR-0025`](../docs/adr/ADR-0025-one-ticker-coroutine-drives-both-sweeps.md) (one ticker
coroutine on the application scope drives both sweeps, on a configured fixed delay). `DEC-020` →
[`ADR-0023`](../docs/adr/ADR-0023-an-absent-seat-checks-when-nothing-is-owed.md) (an absent seat
checks when nothing is owed, folds when facing a bet; `ADR-0013` narrowed, `poker-engine`
unchanged). `DEC-007` →
[`ADR-0020`](../docs/adr/ADR-0020-typescript-protocol-from-serial-descriptors.md) (an emitter we
own over the serial descriptors), which unblocks `STORY-0203`.

**Answered by making the repository public** on 2026-08-13: branch protection and unlimited
Actions minutes both come free there, so `TASK-000102` is done rather than dropped.


---

## Metrics

Updated as epics close. See [`docs/workflow.md`](../docs/workflow.md) — these are only worth
recording if they are recorded when unflattering.

| | EPIC-01 | EPIC-02 | Total |
| --- | --- | --- | --- |
| Tasks completed | 131 / 131 | 173 / 174 | 305 / 308 |
| Accepted on first review | — | 50 / 65 † | — |
| Average review iterations | — | 1.25 † | — |
| Test lines / production lines | — | 3.09 : 1 | — |
| Tasks re-scoped mid-flight | — | 3 † | — |
| Reviews skipped (must stay 0) | 0 | **0** | **0** |
| Tickets promoted haiku → sonnet | 1 | 0 † | 1 |
| Average coder dispatches per ticket | — | 1.25 † | — |
| Manual human edits | — | 0 † | — |

† Measured over the **65 tickets of the 2026-08-13/14 unattended run**, the only stretch of
`EPIC-02` with a kept ledger. The earlier ~109 tickets ran in sessions that recorded no per-ticket
data, so these are honest for the run and silent about the rest rather than extrapolated. One
ticket was dropped (`TASK-021113`); `TASK-000102` is counted under `EPIC-00`.

**What the run cost, and where.** Fifteen of the 65 needed a second coder dispatch. Only two were
coder error in production code — the rest were a stale spec, a defect the coder was right to refuse
to paper over, or a test that passed for the wrong reason. Three tickets were re-scoped mid-flight
(`TASK-020719` split when it hit the three-file cap, `TASK-020808` amended twice after `ADR-0023`
invalidated its assertions, `TASK-021214` narrowed when its target landed first). Four tickets were
created from review findings and all four merged; two live bugs were found and fixed that no test
had been able to see.

**The recurring defect was never a broken feature.** It was an assertion that could not fail: a
vacuous ordering check, a `--tests` filter matching a sibling suite, a regression test whose fixture
did not control the race it guarded, a KDoc whose greps passed while the sentence was false, a
`logger.error` with no backend behind it. Eight such themes are recorded in
[`docs/workflow.md`](../docs/workflow.md); the cheapest fix in every case was naming the failure mode
in the coder's brief before the work started.

---

## EPIC-03 — Web client

The browser half. It renders what the server sends and asserts nothing: no legal action is
computed here, no winner decided, no card inferred. Types come from `protocol.gen.ts`, generated
from the Kotlin descriptors and byte-checked in CI — the client never hand-writes a wire type.

`DEC-022` is answered by [`ADR-0026`](../docs/adr/ADR-0026-vite-and-npm-drive-the-web-client.md):
Vite and npm, Node 24 pinned in `web-client/.nvmrc`, Vitest, and the client's checks as their own
parallel CI job. `STORY-0301`, the scaffold every other story stands on, is **done**: `npm run check`
typechecks, lints, format-checks and tests the client in its own CI job. `STORY-0302` is **done**
too — `design/tokens/tokens.css` is the client's only source of colours and sizes, and the check
fails on a literal outside the token layer.

`STORY-0303` is **done** too: `src/protocol/` — the one module in the client that ever sees a raw
frame — decodes every inbound message, says `Hello`, and turns `Welcome` and `Failure` into
connection state, built against a wire that is about to move twice without needing to change for
either bump.

`STORY-0304` is **done**: `src/store/duel-state.ts` folds a `ServerMessage` into the state every
screen reads, computing nothing a message did not already carry — no legality, no pot arithmetic,
no hand rank, no winner.

`STORY-0305` is **done**: the lobby creates a room, joins by a pasted code, shows the invite link,
names a refusal and leaves the waiting panel on the first `Snapshot`.
[`ADR-0032`](../docs/adr/ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) is what made it
buildable — one store and one connection per tab, wired by a framework-free `bootDuelClient` that
`main.tsx` calls once **outside** the component tree, read through `useDuelState()`
(`useSyncExternalStore`, no store library) and sent through `useSend()` from event handlers only.
*"Exactly one `JoinRoom` after `Welcome`"* is therefore structural rather than a `useRef` guard each
screen re-invents: `TASK-030507` mounts the tree in a real `<StrictMode>` and counts frames, and
moving that send into a screen effect makes it send **three**.

`STORY-0306` is **done**: eighteen tickets replaced the one-line *"The duel has begun."* placeholder
with the table itself, implementing the owner's finished design — `design/screens/duel-table.html`
and `duel-table-states.html` from `STORY-0602` — rather than inventing a layout.

The story's own constraint was `CLAUDE.md`'s non-negotiable made executable: **the table renders the
`PlayerView` and derives nothing from it.** No pot summed, no call priced, no street read off the
board's length, no hand named, no winner declared. Two whole-table guards carry it — one extracts
every digit run from the DOM and asserts each is a field of the view, the other pins all eight card
`aria-label`s in document order — and ten enumerated violations were run against them.

`STORY-0307` is next and is split: twelve tickets, cumulative counts **194 → 232** from the measured
baseline of 190. `TASK-030701` is startable now; the chain is strictly linear. The bar it builds is
the **only place this client asserts anything**, so the same constraint appears one level sharper:
it draws `legalActions.allowed` and no other control, clamps its amount control to bounds the server
sent rather than working any out, copies `handNumber`, `actionSequence` and `seat` verbatim into the
one `Act` a click sends, and then disables everything until the server's next frame. A third guard
(`TASK-030710`) asserts every digit run in the bar is one of the four amounts `LegalActions` carries.

Three deliberate departures from the design, recorded in the story: `ALL_IN` is a fourth button
because `BettingRules` offers it in almost every legal set; the `−`/`+` stepper becomes a range
input because no increment is on the wire and inventing one would be a raising rule; and the three
pot-fraction sizing chips go, because they are the bet presets the story puts out of scope.

**`DEC-037` blocks the twelfth ticket only.** After a `Rejected` the server sends no fresh
`YourTurn` and the reducer clears `pendingTurn`, so a rejected action currently ends that player's
hand while both players are still connected. Which layer reopens it is the architect's, and eleven
tickets ship without the answer.

| Story | Title | Status |
| --- | --- | --- |
| **[STORY-0301](stories/STORY-0301-web-client-toolchain.md)** The web-client toolchain and its first green check — *schema 2* | | **done** |
| | [TASK-030101](tasks/TASK-030101-manifest-node-pin-and-locked-install.md) A manifest, a Node pin and a locked install for web-client | S | **done** |
| | [TASK-030102](tasks/TASK-030102-prettier-never-reads-the-generated-file.md) Prettier formats the client and never reads the generated file | XS | **done** |
| | [TASK-030103](tasks/TASK-030103-strict-tsconfig-keeps-the-generated-file-in-the-program.md) A strict tsconfig that keeps the generated file inside the typechecked program | XS | **done** |
| | [TASK-030104](tasks/TASK-030104-an-app-root-that-mounts-one-component.md) An app root that mounts one trivial component | XS | **done** |
| | [TASK-030105](tasks/TASK-030105-vite-builds-a-bundle-that-contains-the-app.md) Vite builds a production bundle that contains the app | XS | **done** |
| | [TASK-030106](tasks/TASK-030106-vitest-runs-one-component-test-in-jsdom.md) Vitest renders the app in jsdom and asserts what it shows | XS | **done** |
| | [TASK-030107](tasks/TASK-030107-eslint-lints-the-client-and-not-the-generated-file.md) ESLint lints the client and never the generated file | XS | **done** |
| | [TASK-030108](tasks/TASK-030108-the-dev-server-proxies-api-and-ws-to-ktor.md) The dev server proxies /api and /ws to the Ktor server | XS | **done** |
| | [TASK-030109](tasks/TASK-030109-one-npm-run-check-runs-all-four-checks.md) One npm run check runs every check CI will run | XS | **done** |
| | [TASK-030110](tasks/TASK-030110-ci-gains-a-client-job-and-drops-the-ad-hoc-tsc.md) CI gains a client job and drops the ad-hoc npx tsc in the same diff | XS | **done** |
| | [TASK-030111](tasks/TASK-030111-contributing-says-how-to-check-the-client.md) CONTRIBUTING says how to install and check the web client | XS | **done** |
| **[STORY-0302](stories/STORY-0302-design-tokens-in-the-client.md)** The design tokens are the client's only colours and sizes — *schema 2* | | **done** |
| | [TASK-030201](tasks/TASK-030201-vendor-the-token-sheet-byte-for-byte.md) Vendor the token sheet into the client, byte for byte | S | **done** |
| | [TASK-030202](tasks/TASK-030202-the-check-fails-on-a-colour-literal.md) The client's check fails on a colour literal outside the token layer | S | **done** |
| | [TASK-030203](tasks/TASK-030203-tailwind-installs-and-its-vite-plugin-runs.md) Tailwind installs from the lockfile and its Vite plugin runs | XS | **done** |
| | [TASK-030204](tasks/TASK-030204-the-tokens-and-tailwind-reach-the-bundle.md) The tokens and Tailwind reach the bundle through one stylesheet | XS | **done** |
| | [TASK-030205](tasks/TASK-030205-the-themes-colours-are-the-tokens.md) The theme's colours are the tokens and nothing else | S | **done** |
| | [TASK-030206](tasks/TASK-030206-the-themes-sizes-are-the-tokens.md) The theme's type, spacing and radii are the tokens and nothing else | S | **done** |
| | [TASK-030207](tasks/TASK-030207-prettier-sorts-tailwind-classes.md) Prettier sorts Tailwind classes and still never reads the generated file | XS | **done** |
| | [TASK-030208](tasks/TASK-030208-the-app-root-is-styled-through-the-theme.md) The app root is styled through the theme, proven by a test | XS | **done** |
| | [TASK-030209](tasks/TASK-030209-contributing-says-the-token-sheet-is-a-copy.md) CONTRIBUTING says the client's token sheet is a copy | XS | **done** |
| **[STORY-0303](stories/STORY-0303-typed-socket-and-handshake.md)** The typed socket — handshake and device identity — *schema 2* | | **done** |
| | [TASK-030301](tasks/TASK-030301-nothing-outside-the-protocol-module-declares-a-wire-type.md) Nothing outside src/protocol declares a wire type or touches a raw frame | S | **done** |
| | [TASK-030302](tasks/TASK-030302-the-protocol-version-is-typed-against-the-generated-alias.md) The protocol version the client sends is typed against the generated alias | XS | **done** |
| | [TASK-030303](tasks/TASK-030303-the-frame-codec-decodes-only-what-the-union-names.md) The frame codec decodes only what the generated union names | S | **done** |
| | [TASK-030304](tasks/TASK-030304-the-device-id-lives-under-one-key-this-module-owns.md) The device id lives under one storage key this module owns | XS | **done** |
| | [TASK-030305](tasks/TASK-030305-the-socket-url-comes-from-the-pages-own-origin.md) The socket URL is derived from the page's own origin | XS | **done** |
| | [TASK-030306](tasks/TASK-030306-a-websocket-double-the-handshake-tests-drive-by-hand.md) A WebSocket double the handshake tests drive by hand | XS | **done** |
| | [TASK-030307](tasks/TASK-030307-on-open-the-client-says-hello-with-the-device-id-it-holds.md) On open the client says Hello with the device id it holds | S | **done** |
| | [TASK-030308](tasks/TASK-030308-every-inbound-frame-reaches-the-listener-or-is-dropped.md) Every inbound frame reaches the listener, and an unreadable one is logged and dropped | S | **done** |
| | [TASK-030309](tasks/TASK-030309-welcome-makes-the-connection-ready-and-persists-the-device-id.md) Welcome makes the connection ready and persists the device id the server issued | S | **done** |
| | [TASK-030310](tasks/TASK-030310-a-refusal-keeps-the-socket-a-version-mismatch-ends-the-session.md) A refusal keeps the socket, and a version mismatch ends the connection for good | S | **done** |
| | [TASK-030311](tasks/TASK-030311-one-call-opens-the-duel-socket-with-no-network-in-the-test.md) One call opens the duel socket, and the test that proves it touches no network | S | **done** |
| | [TASK-030312](tasks/TASK-030312-the-protocol-document-says-what-a-client-cannot-read.md) docs/protocol.md says what a client does with a frame it cannot read | XS | **done** |
| | [TASK-030313](tasks/TASK-030313-the-fake-socket-is-a-no-op-when-nothing-is-listening.md) The fake socket is a no-op when nothing is listening | XS | **done** |
| **[STORY-0304](stories/STORY-0304-client-store.md)** The store — state is the last frame the server sent — *schema 2* | | **done** |
| | [TASK-030401](tasks/TASK-030401-the-store-starts-empty-and-room-joined-sets-the-seat.md) The store starts empty, and RoomJoined sets the seat and room code | S | **done** |
| | [TASK-030402](tasks/TASK-030402-your-turn-sets-a-pending-turn-identified-verbatim.md) YourTurn sets a pending turn identified verbatim by the message | S | **done** |
| | [TASK-030403](tasks/TASK-030403-a-snapshot-replaces-the-view-and-a-disagreeing-seat-is-defined.md) A Snapshot replaces the view wholesale, and a disagreeing seat is a defined outcome | S | **done** |
| | [TASK-030404](tasks/TASK-030404-a-rejected-clears-the-pending-turn-and-leaves-the-view-alone.md) A Rejected clears the pending turn and leaves the view untouched | XS | **done** |
| | [TASK-030405](tasks/TASK-030405-events-narrate-and-change-no-field-a-snapshot-established.md) Events narrate, and change no field a Snapshot established | S | **done** |
| | [TASK-030406](tasks/TASK-030406-duel-finished-records-the-outcome-and-clears-the-pending-turn.md) DuelFinished records the outcome verbatim and clears the pending turn | XS | **done** |
| **[STORY-0305](stories/STORY-0305-lobby-and-room-link.md)** The lobby — create a room, join by code, share the link — *schema 2* | | **done** |
| | [TASK-030501](tasks/TASK-030501-the-store-is-subscribable-and-notifies-only-when-the-state-moved.md) The store is subscribable, and notifies only when the state moved | S | **done** |
| | [TASK-030502](tasks/TASK-030502-a-failure-reaches-the-state-through-the-reducer.md) A Failure reaches the state through the reducer, and a join that lands clears it | S | **done** |
| | [TASK-030503](tasks/TASK-030503-boot-joins-the-tabs-one-connection-to-its-one-store.md) Boot joins the tab's one connection to its one store | S | **done** |
| | [TASK-030504](tasks/TASK-030504-the-code-from-the-url-joins-on-welcome-exactly-once.md) The code the URL carried joins on Welcome, exactly once | S | **done** |
| | [TASK-030505](tasks/TASK-030505-a-component-reads-the-store-through-use-duel-state.md) A component reads the store through useDuelState, and re-renders only when it moved | S | **done** |
| | [TASK-030506](tasks/TASK-030506-use-send-hands-a-screen-the-boot-created-send.md) useSend hands a screen the boot-created send, and a missing provider says so | XS | **done** |
| | [TASK-030507](tasks/TASK-030507-a-strictmode-double-mount-sends-no-second-joinroom.md) A StrictMode double mount sends no second JoinRoom | S | **done** |
| | [TASK-030508](tasks/TASK-030508-the-framework-free-store-modules-import-nothing-from-react.md) The framework-free store modules import nothing from react | XS | **done** |
| | [TASK-030509](tasks/TASK-030509-the-invite-carries-the-code-as-a-query-parameter.md) The invite carries the code as a query parameter, trimmed and upper-cased | S | **done** |
| | [TASK-030510](tasks/TASK-030510-the-lobby-creates-a-room-and-joins-by-a-pasted-code.md) The lobby creates a room, and joins by a pasted code it trims and upper-cases | S | **done** |
| | [TASK-030511](tasks/TASK-030511-a-joined-room-shows-its-code-and-a-selectable-invite-link.md) A joined room shows its code and a selectable invite link | S | **done** |
| | [TASK-030512](tasks/TASK-030512-copy-the-link-where-there-is-a-clipboard.md) Copy the link where there is a clipboard, and keep it in reach where there is not | S | **done** |
| | [TASK-030513](tasks/TASK-030513-unknown-room-and-room-full-each-get-their-own-message.md) UNKNOWN_ROOM and ROOM_FULL each get their own message, and nothing retries | S | **done** |
| | [TASK-030514](tasks/TASK-030514-the-first-snapshot-ends-the-wait-and-no-other-frame-does.md) The first Snapshot ends the wait, and no other frame does | S | **done** |
| | [TASK-030515](tasks/TASK-030515-main-boots-the-client-once-and-renders-the-lobby.md) main.tsx boots the client once and renders the lobby under the provider | S | **done** |
| | [TASK-030516](tasks/TASK-030516-one-connection-per-tab-booted-in-main-and-nowhere-else.md) One connection per tab, booted in main.tsx and nowhere else | S | **done** |
| **[STORY-0306](stories/STORY-0306-duel-table-screen.md)** The duel table renders a PlayerView — *schema 2* | | **done** |
| | [TASK-030601](tasks/TASK-030601-a-chip-amount-is-grouped-the-same-way-wherever-it-runs.md) A chip amount is grouped the same way wherever it runs | XS | **done** |
| | [TASK-030602](tasks/TASK-030602-a-card-string-splits-into-a-rank-character-and-a-suit-glyph.md) A card string splits into a rank character and a suit glyph | S | **done** |
| | [TASK-030603](tasks/TASK-030603-a-card-says-its-name-aloud-and-carries-no-number.md) A card says its name aloud, and carries no number | S | **done** |
| | [TASK-030604](tasks/TASK-030604-a-card-back-and-an-undealt-board-place.md) A card back and an undealt board place | S | **done** |
| | [TASK-030605](tasks/TASK-030605-a-face-up-card-draws-its-rank-its-suit-and-the-suits-colour.md) A face-up card draws its rank, its suit and the suit's colour | S | **done** |
| | [TASK-030606](tasks/TASK-030606-a-hand-is-two-places-wide-whatever-the-view-carries.md) A hand is two places wide, whatever the view carries | S | **done** |
| | [TASK-030607](tasks/TASK-030607-the-board-is-five-places-wide-whatever-the-street.md) The board is five places wide, whatever the street | S | **done** |
| | [TASK-030608](tasks/TASK-030608-a-playerview-fixture-with-every-field-the-wire-declares.md) A PlayerView fixture with every field the wire declares | S | **done** |
| | [TASK-030609](tasks/TASK-030609-the-pot-strip-states-the-pot-the-blinds-the-hand-and-the-street.md) The pot strip states the pot, the blinds, the hand and the street | S | **done** |
| | [TASK-030610](tasks/TASK-030610-a-seats-status-is-read-off-the-view-never-off-its-cards.md) A seat's status is read off the view, never off its cards | S | **done** |
| | [TASK-030611](tasks/TASK-030611-the-seat-plate-shows-the-name-the-button-and-the-stack.md) The seat plate shows the name, the button and the stack | S | **done** |
| | [TASK-030612](tasks/TASK-030612-the-duel-table-seats-the-views-two-players-around-the-board.md) The duel table seats the view's two players around the board | S | **done** |
| | [TASK-030613](tasks/TASK-030613-your-hand-is-face-up-and-your-rivals-is-face-down.md) Your hand is face up and your rival's is face down | S | **done** |
| | [TASK-030614](tasks/TASK-030614-the-reserved-line-states-what-the-rival-has-committed.md) The reserved line states what the rival has committed this street | XS | **done** |
| | [TASK-030615](tasks/TASK-030615-the-table-shows-no-number-the-view-does-not-carry.md) The table shows no number the view does not carry | S | **done** |
| | [TASK-030616](tasks/TASK-030616-the-table-names-no-card-the-view-did-not-send-and-no-hand.md) The table names no card the view did not send, and no hand | S | **done** |
| | [TASK-030617](tasks/TASK-030617-the-lobby-hands-the-live-view-to-the-duel-table.md) The lobby hands the live view to the duel table | XS | **done** |
| | [TASK-030618](tasks/TASK-030618-the-suit-glyphs-are-asserted-by-codepoint-not-by-a-matching-literal.md) The suit glyphs are asserted by codepoint, not by a matching literal | XS | **done** |
| **[STORY-0307](stories/STORY-0307-action-bar.md)** The action bar — acting on your turn — *schema 2* | | **ready** |
| | [TASK-030701](tasks/TASK-030701-a-turn-fixture-with-every-field-the-wire-declares.md) A turn fixture with every field the wire declares | XS | **done** |
| | [TASK-030702](tasks/TASK-030702-each-action-says-its-verb-and-carries-the-servers-figure.md) Each action says its verb and carries the server's figure | S | **done** |
| | [TASK-030703](tasks/TASK-030703-the-act-frame-echoes-the-turns-identity-verbatim.md) The Act frame echoes the turn's identity verbatim | S | ready |
| | [TASK-030704](tasks/TASK-030704-the-bar-exists-in-every-state-and-waits-in-most.md) The bar exists in every state, and waits in most of them | S | backlog |
| | [TASK-030705](tasks/TASK-030705-one-button-per-action-the-server-allowed.md) One button per action the server allowed, and not one more | S | backlog |
| | [TASK-030706](tasks/TASK-030706-the-amount-control-is-clamped-to-the-bounds-the-server-sent.md) The amount control is clamped to the bounds the server sent | S | backlog |
| | [TASK-030707](tasks/TASK-030707-a-click-sends-one-act-and-the-bar-goes-quiet.md) A click sends one Act, and the bar goes quiet until the next turn | S | backlog |
| | [TASK-030708](tasks/TASK-030708-a-rejection-reads-from-its-own-fields.md) A rejection reads from its own fields, in the server's numbers | S | backlog |
| | [TASK-030709](tasks/TASK-030709-the-bar-states-what-the-server-refused.md) The bar states what the server refused, and retries nothing | S | backlog |
| | [TASK-030710](tasks/TASK-030710-the-bar-shows-no-number-and-offers-no-action-the-turn-did-not-carry.md) The bar shows no number and offers no action the turn did not carry | S | backlog |
| | [TASK-030711](tasks/TASK-030711-the-duel-screen-puts-the-bar-under-the-table.md) The duel screen puts the bar under the table and sends what it built | S | backlog |
| | [TASK-030712](tasks/TASK-030712-after-a-rejection-the-player-can-act-again.md) After a rejection the player can act again | S | **blocked** (`DEC-037`) |
| [STORY-0308](stories/STORY-0308-result-screen.md) | The result screen — who won, and the coin | backlog |
| [STORY-0309](stories/STORY-0309-rematch.md) | Rematch from the result screen | **blocked** |
| [STORY-0310](stories/STORY-0310-reconnect-and-resume.md) | Reconnect — the client resumes its seat | backlog |
| [STORY-0311](stories/STORY-0311-profile-strip.md) | The profile strip — my coins and my recent duels | backlog |
| [STORY-0312](stories/STORY-0312-whole-duel-through-the-client.md) | A whole duel through the client, frame by frame | backlog |

---

## EPIC-11 — Status notifications

Ahead of `EPIC-03`/`04`/`05` by the human's instruction on 2026-08-15. Settled by
[`ADR-0042`](../docs/adr/ADR-0042-the-run-reports-itself-every-two-hours.md) (`DEC-036`):
Telegram over stdlib Python, reports composed from repository state so a dead agent is not a
missing report, an in-session two-hourly cron, and the stop report carried by a `Stop` hook.

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| **[STORY-1101](stories/STORY-1101-send-a-real-message.md)** Send a real message — *schema 2* | | | **done** |
| | [TASK-110101](tasks/TASK-110101-credentials-resolve-and-redact.md) Resolve credentials, and redact the token from everything | S | **done** |
| | [TASK-110102](tasks/TASK-110102-send-one-telegram-message.md) Send one message to the Telegram Bot API | S | **done** |
| | [TASK-110103](tasks/TASK-110103-notify-cli-and-doctor.md) The notify CLI, and a doctor that proves the channel | S | **done** |
| | [TASK-110104](tasks/TASK-110104-the-suite-runs-in-ci.md) Run the notifier suite in CI | XS | **done** |
| **[STORY-1102](stories/STORY-1102-compose-the-report.md)** Compose the report from repository state — *schema 2* | | | **done** |
| | [TASK-110201](tasks/TASK-110201-the-run-state-breadcrumb.md) The run-state breadcrumb the agent stamps | S | **done** |
| | [TASK-110202](tasks/TASK-110202-read-the-board.md) Read ticket and epic status out of the board | S | **done** |
| | [TASK-110203](tasks/TASK-110203-compose-the-status-report.md) Compose the status report, degrading section by section | S | **done** |
| **[STORY-1103](stories/STORY-1103-the-four-reports.md)** The four reports the run owes — *schema 2* | | | **done** |
| | [TASK-110301](tasks/TASK-110301-heartbeat-dedupes-on-the-window.md) The heartbeat sends once per window, whoever fires it | S | **done** |
| | [TASK-110302](tasks/TASK-110302-stop-and-budget-reports.md) The stop and budget reports, and the cron line | S | **done** |
| | [TASK-110303](tasks/TASK-110303-the-stop-hook.md) The Stop hook, and the one line that registers it | S | **done** |
| | [TASK-110304](tasks/TASK-110304-build-epic-reports.md) build-epic gains its reporting duties | S | **done** |
| [STORY-1104](stories/STORY-1104-a-heartbeat-that-outlives-the-session.md) A heartbeat that outlives the session | | | backlog |

`STORY-1104` is written and deliberately unstarted — the launchd option the human declined on
2026-08-15. It is kept so that reversing that judgement costs one command rather than one
rediscovery, and its tickets are written only if it is ever started.
