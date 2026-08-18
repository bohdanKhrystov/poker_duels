# Board

The index. Conventions live in [`README.md`](README.md).

**Now:** `EPIC-01` is **done**, and so was `EPIC-02` — the engine, and a duel server that plays a
whole duel over two real sockets against PostgreSQL, pays the winner a coin, and survives a
disconnect. `EPIC-02` **reopened on 2026-08-16** for two stories:
[`ADR-0044`](../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) answers `DEC-023`
and puts the rematch's wire half in `STORY-0213`, and
[`ADR-0045`](../docs/adr/ADR-0045-presence-belongs-to-the-table.md) answers `DEC-038` and puts the
pause state's wire half in `STORY-0214` — both where the code lives, rather than in `EPIC-03`. They
land one at a time, `0213` first, because each moves `PROTOCOL_VERSION`.
`EPIC-06` (design) runs in parallel on disjoint files, see `ADR-0023`. `EPIC-03` (web client) is
in progress.

`EPIC-11` (status notifications) is **ahead of `EPIC-03`, `EPIC-04` and `EPIC-05` in the
queue**, by the human's instruction on 2026-08-15. Those three are the first epics long enough
for a silent stall to cost a night, so the run learns to report itself before it runs them
rather than after the first one goes quiet. It touches no file any of them touch —
`scripts/notify/`, `docs/notifications.md` and the skill.

`EPIC-04` (identity and profiles) **opened on 2026-08-16**: seventeen stories written from the
vision and the shipped ADRs, and `STORY-0401` — the display name, its canonical form and its write
path — split into eighteen tickets. It starts with a migration rather than with the credential
chain, because all three schema ADRs number their migration *at merge time* and one of them has to
go first.

Startable right now: `python3 .github/scripts/lint_tickets.py --startable`

---

## Epics

| ID | Title | Status | Milestone |
| --- | --- | --- | --- |
| [EPIC-00](epics/EPIC-00-ways-of-working.md) | Ways of working | **in progress** | v0.1 |
| [EPIC-01](epics/EPIC-01-poker-engine.md) | Poker engine | **done** | v0.1 |
| [EPIC-02](epics/EPIC-02-duel-server.md) | Duel server — rooms, WebSocket protocol, persistence | **in progress** — closed 2026-08-14, reopened for `STORY-0213` | v0.1 |
| [EPIC-03](epics/EPIC-03-web-client.md) | Web client — table, lobby, duel flow | **ready** | v0.1 |
| [EPIC-04](epics/EPIC-04-identity-and-profiles.md) | Identity and profiles | **ready** — 17 stories written 2026-08-16, `STORY-0401` and `STORY-0402` done | v0.2 |
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
| | [TASK-000104](tasks/TASK-000104-a-second-branch-cannot-claim-the-same-protocol-version.md) A second branch cannot claim the same PROTOCOL_VERSION | S | **done** |

`TASK-000102` is **done**. The repository went public on 2026-08-13, which made protection and
Actions minutes free at once, and `develop` is now protected: a pull request and two green checks
to land, no force pushes, no deletions. Required approvals are deliberately **0** — one would
deadlock the agent run, since an agent cannot approve its own PR.

`TASK-000104` is **startable**, and lives here rather than under any epic because the rule it
enforces spans three of them. `DEC-040` is answered by
[`ADR-0047`](../docs/adr/ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md): the lock is a
**claim ledger**, `docs/protocol-versions.md`, one row per version naming a fingerprint of the wire
shape that number means. Two branches appending a row for the same number **conflict textually**, so
git refuses the second merge before a check runs — where the constant itself merges clean, because
both sides made the identical `2` → `3` edit. One JUnit test on `:poker-server:check` proves the
last row is `PROTOCOL_VERSION` and its fingerprint is the live wire's, which fails every wrong way
of resolving that conflict and, for free, an unversioned wire change. `STORY-0213`, `STORY-0214` and
`STORY-0405` all hold unlanded bumps and share the lock today; each now also pays a hand-written
ledger row, and no wire-shape change can skip a version bump.

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
| [STORY-0213](stories/STORY-0213-the-wire-carries-a-rematch.md) | The wire carries a rematch | | **ready** |
| [STORY-0214](stories/STORY-0214-the-wire-names-an-absent-opponent.md) | The wire names an absent opponent | | **ready** |

Stories are written; tickets come from `/plan-story` as each is reached.

**`STORY-0213` reopened this epic on 2026-08-16.**
[`ADR-0044`](../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) answers `DEC-023`:
`ClientMessage` gains `OfferRematch` (no fields — the socket's `RoomMembership` names the room),
`ServerMessage` gains `RematchOffered(seat)` for **both** seats, and the rematch's start is the
opening `Snapshot` it already produces rather than a frame of its own. Refusals collapse to
`UNKNOWN_ROOM` and a new, transient `REMATCH_UNAVAILABLE`; `PROTOCOL_VERSION` takes the next number
free when it lands, alongside `ADR-0027`'s and `ADR-0028`'s unlanded bumps. The epic's own scope
line already promised *rematch* and shipped it as far as `RoomRegistry.offerRematch`; this is the
wire message it stopped short of. The metrics below are as measured at the first close and are not
re-measured for it.

**`STORY-0214` followed it on the same argument.**
[`ADR-0045`](../docs/adr/ADR-0045-presence-belongs-to-the-table.md) answers `DEC-038`: `ADR-0028`
specified `SeatPresence`, `OpponentPresence`, `ActedForAbsentSeat` and `Room.presenceOf` two days
after `STORY-0208` closed, and none of it was ever ticketed — no Kotlin type, no `docs/protocol.md`
row, nothing in `protocol.gen.ts`. The scope line promised *the disconnect grace period of
`ADR-0013`* and the epic shipped a room that knows exactly who is gone without telling the player
who stayed. `STORY-0208` is **not** reopened; this is a sibling, so the closed story's ledger is
extended rather than rewritten, and `STORY-0214` is outside the metrics table too. **The two wire
stories land one at a time** — both move `PROTOCOL_VERSION` and both edit the same four files;
`STORY-0213` is in front, and two branches each moving 2 → 3 would merge clean and green, which is
the failure the order exists to prevent. The client half is `EPIC-03`'s `STORY-0313`, not a reopened
`STORY-0310`.

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
password only, for now, and the account screens are designed for one credential). `DEC-043` →
[`ADR-0048`](../docs/adr/ADR-0048-a-password-has-one-rule-and-it-is-length.md) (8 to 128 code
points after NFC and nothing else examined — the maximum is a hashing-work bound, not a strength
rule, and the ADR says plainly that it refuses a short password and not a common one —
unblocking `STORY-0404`).

`DEC-023` → [`ADR-0044`](../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md)
(a rematch is one client intent and one room fact: `OfferRematch` in, `RematchOffered(seat)` out to
both seats, idempotent on a repeat and restated after a reconnect's frames. No started frame — after
a `DuelFinished`, a `Snapshot` **is** the rematch, which nothing else can produce. Two refusals:
`UNKNOWN_ROOM` ends it, a new transient `REMATCH_UNAVAILABLE` does not. No deadline on the wire,
because a countdown the client may not act on is cosmetic. `PROTOCOL_VERSION` moves one step, taking
the next free number when it lands. **The server half is `EPIC-02`'s `STORY-0213`**, which reopens
that epic; `STORY-0309` is `ready` and writes no Kotlin).

`DEC-038` → [`ADR-0045`](../docs/adr/ADR-0045-presence-belongs-to-the-table.md)
(`ADR-0028`'s server half ships from `EPIC-02` as `STORY-0214`, on `ADR-0044`'s argument applied to
the case that ADR named and declined to file. `STORY-0208` stays `done` — a sibling extends a closed
ledger where an edit would rewrite it. Presence takes its **own** `PROTOCOL_VERSION` step, and
`ADR-0028` §8's rule gains mechanics: the bump is the story's last ticket, the number is read from
`develop` plus one at that moment and never written down in advance, and **at most one
protocol-bumping branch is open at a time** — `STORY-0213`, then `STORY-0214`, then `STORY-0405`. The
client half is a **new** story, `STORY-0313`, not a reopened `STORY-0310`: four of `ADR-0028` §5's
five emission points reach the player who *stayed*, at the table, so presence is a table feature
reconnect observes once. The costs recorded rather than discovered: two branches both moving 2 → 3
merge clean and green, so the version lock has no CI gate; `EPIC-03` gains a story only `EPIC-02` can
unblock; `EPIC-02` gains a second story outside its metrics ledger; and `STORY-0214`'s first ticket
must delete a passing `TASK-020806` test that `ADR-0028` deliberately retracted).

`DEC-039` → [`ADR-0046`](../docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md)
(the words a player reads for the three presence states and for an action the server took. Minted and
answered in the same change — it had lived as `STORY-0313`'s open input, never as a numbered row. The
seat plate says `Away` and `Timed out`, and nothing for `PRESENT`. The line that explains them says
*Your rival is away. The duel is paused.*, *Your rival did not come back. The duel continues, and the
server acts for them.* and *Your rival is back.* — the last **only** when this client previously held
`AWAY` or `ABSENT`, because a resuming client is always sent `PRESENT` and its rival never left. The
countdown carries no word of its own, and **nothing a player reads changes when it reaches zero**. An
action the server took names the server as the subject — *The server folded for your rival.* — never
the rival as actor, never `auto-fold`, never a cause the server cannot see, never the cash-game
*sitting out*. Derived from the vision's *Positioning* sentence; `ADR-0028` had reserved these words
to the human and the ADR says so rather than assuming. The costs recorded rather than discovered:
`Timed out` collides with any future per-action turn clock; the mark has no home in a client that
renders no action log; the return line needs the store to remember what it was last told; and *the
server acts for them* teaches a player how to beat an absent seat. **`STORY-0313` is now
splittable** the day `STORY-0214` merges).

`DEC-040` → [`ADR-0047`](../docs/adr/ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md)
(the `PROTOCOL_VERSION` lock is a **claim ledger**, `docs/protocol-versions.md`: one row per version
naming a 16-hex fingerprint of the wire shape that number means. Two branches appending a row for the
same number **conflict textually**, so git refuses the second merge before any check runs — verified
with `git merge-file`, where the constant merges clean at exit 0 and the ledger exits 1 with both
claims in the markers. One JUnit test on `:poker-server:check` — no new CI job, no Gradle task, no
network, no production code — asserts versions ascend by one, the last row is `PROTOCOL_VERSION`, and
its fingerprint equals the live descriptors', so every wrong way of resolving that conflict fails and
an unversioned wire change fails too. There is deliberately **no** command that writes the row: one
that regenerates the ledger is one that overwrites another branch's claim. Rejected: a CI check
against `origin/develop`, which is unsound without `strict = true` because GitHub does not re-run a
PR when its base moves, and unrunnable in an agent worktree with no `origin/develop`; and branch
protection, which `TASK-000102` already measured and decided the other way. The costs recorded rather
than discovered: a bump now costs a hand-written row and a deliberate red-then-green cycle; every
wire-shape change now forces a version bump with no escape hatch; only the last row is verifiable;
and `Int` and `Long` both project to `number`, so what escapes the fingerprint is exactly what
escapes `verifyProtocolTypes`).

`DEC-037` → [`ADR-0043`](../docs/adr/ADR-0043-a-rejection-closes-no-decision-point.md)
(a `Rejected` closes no decision point: the reducer keeps `pendingTurn`, clears `rejection` on the
next `YourTurn`, `Snapshot` or `DuelFinished`, and counts refusals so the bar's existing remount key
lifts its in-flight lock. The server is not changed — `guard` proves the identity the client already
holds is still valid after a rejection. Five files exceed one schema-2 ticket, so `TASK-030712` is
the store half and the bar half is a sibling the planner files).

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

`STORY-0307` is merged to its last ticket: twelve tickets, cumulative counts **194 → 232** from the
measured baseline of 190, plus five follow-ups filed during the run (`TASK-030713`–`17`), which
carry no count because they may land in any order. Sixteen are merged; `TASK-030717` is the one
still open, filed on 2026-08-16 by `TASK-030716`'s own second red edit — delete `guard`'s
`message.handNumber != state.handNumber` line and the whole of `DuelActionTest` stays green, because
the suite's only staleness coverage replays a frame from the *same* hand. The chain is strictly
linear. The bar it builds is
the **only place this client asserts anything**, so the same constraint appears one level sharper:
it draws `legalActions.allowed` and no other control, clamps its amount control to bounds the server
sent rather than working any out, copies `handNumber`, `actionSequence` and `seat` verbatim into the
one `Act` a click sends, and then disables everything until the server's next frame. A third guard
(`TASK-030710`) asserts every digit run in the bar is one of the four amounts `LegalActions` carries.

Three deliberate departures from the design, recorded in the story: `ALL_IN` is a fourth button
because `BettingRules` offers it in almost every legal set; the `−`/`+` stepper becomes a range
input because no increment is on the wire and inventing one would be a raising rule; and the three
pot-fraction sizing chips go, because they are the bet presets the story puts out of scope.

**`DEC-037` is answered** by
[`ADR-0043`](../docs/adr/ADR-0043-a-rejection-closes-no-decision-point.md): a `Rejected` closes no
decision point, so the reducer keeps `pendingTurn`, clears `rejection` on the next `YourTurn`,
`Snapshot` or `DuelFinished`, and counts refusals in a new `rejectionCount` that the bar's remount
key consumes. No server or protocol change. The fix is five files, which is two files more than a
schema-2 ticket may touch, so `TASK-030712` is now the **store half** and `TASK-030713` the **bar
half** — `ActionBar.tsx`, `ActionBar.test.tsx`, `Lobby.tsx`. `STORY-0307`'s fifth acceptance
criterion closes when both land.

**Three more follow-ups**, all named by the same ADR or by a review of the merged bar, none of them
new scope:

- `TASK-030714` — `refusal` is never cleared, so `Failure{DUEL_PAUSED}`'s sentence outlives the
  attempt it describes by the rest of the duel. `ADR-0043` calls it *"the same lifetime bug in a
  different field"* and leaves it one ticket; it applies the ADR's own rule to the second field.
- `TASK-030715` — both whole-surface derivation guards scan text, `aria-label` and `title`, and no
  numeric attribute. `max={actions.allInTo}` reaches the DOM with no printed or spoken echo whenever
  `BET`/`RAISE` is allowed and `ALL_IN` is not, so a corrupted ceiling is invisible to both. Neither
  guard renders that combination today; the ticket closes the hole and exercises it.
- `TASK-030716` — `poker-server` proves the invariant the client now leans on. A rejection that
  advanced the hand number would turn every retry into a `STALE_FRAME` the server drops in silence,
  and `ADR-0043` records that nothing tests it.
- `TASK-030717` — the untested branch `TASK-030716` reported on its way past: a frame naming a
  **stale hand** whose `actionSequence` still fits is refused by one line nothing exercises. One
  test, with the control frame that proves which branch fired.

`STORY-0308` is next and is split: **nine tickets**, cumulative counts **250 → 275** from a measured
baseline of 247, and `TASK-030801` is startable now. It implements the owner's finished
`design/screens/duel-end.html` rather than inventing a layout, and lands in a new
`web-client/src/result/` beside `src/lobby/` and `src/table/`.

The story's constraint is the epic's, one step further than the bar's: **the client asserts nothing
and derives nothing about the result.** The verdict is `outcome.winner === mySeat` and nothing else —
`TASK-030808` renders one outcome with **equal final stacks** twice, from either seat, so a verdict
computed from chips cannot produce the two different words the test demands. The coin is stated, not
counted: `+1`, `−1` or nothing, `ADR-0014`'s constant, while the *balance* stays the server's and
arrives with `STORY-0311`'s `GET /api/me`. `winner: null` is rendered as a draw, never as an error or
a loss (`ADR-0015`), and a client holding no seat says *Duel over* rather than guessing a side.

Three departures from the design, recorded in the story: no rival name on the defeat line, because
none is on the wire (`ADR-0021`, `DEC-017`); no duration, because `DuelOutcome` carries no clock; and
no rematch button, because `STORY-0309` owns it and the wire could not carry one when `STORY-0308`
was split — `TASK-030807` has the test that stops a later coder adding a dead one. `DEC-023` is now
answered (`ADR-0044`), so the button arrives with `STORY-0309`, once `EPIC-02`'s `STORY-0213` has
put `OfferRematch` and `RematchOffered` on the wire. The way on is a plain `<a href="/">`: the
reducer clears nothing a frame established, so the lobby is reached by starting from an empty store.

**`STORY-0310` is split into thirteen**, on a baseline of 275, and `TASK-031001` is startable now.
It is the **client** half of a path the server already serves: `STORY-0208` shipped the grace
period, `TASK-020810`/`TASK-020811` rebuild the frames a returning seat is entitled to through the
projection layer, and `TASK-020814` proves a returning socket picks up where it left off. So no
protocol is designed here — the recipe is reopen, `Hello`, `JoinRoom`, and the client's only new
state is the room code, beside the device id, under a key `src/protocol/` owns. The reconnect loop
lives in `src/protocol/reconnecting.ts` because `boundary.test.ts` forbids the word `WebSocket`
anywhere else; the *rejoin* lives in `boot.ts` because `ADR-0032` puts every message-triggered send
there. `VERSION_MISMATCH` ends the loop entirely and `UNKNOWN_ROOM` ends only the resume — different
reasons, different reach — and `TASK-031013` turns *"no test sleeps on a real clock"* from a promise
into a check. **No pause state, and that is now settled rather than pending**: `ADR-0028` answered
`DEC-018`, but `OpponentPresence` exists in no Kotlin file and no generated type, and this epic
writes no Kotlin. `DEC-038` asked who ships it and
[`ADR-0045`](../docs/adr/ADR-0045-presence-belongs-to-the-table.md) answers — `EPIC-02`'s
`STORY-0214`, behind `STORY-0213` in the version queue — with the rendering in a **new** story,
`STORY-0313`, because four of the five presence frames reach the player who stayed, at the table.
`STORY-0310` keeps all thirteen tickets and gains nothing. The copy that story renders is settled too
— [`ADR-0046`](../docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md) answers `DEC-039` with
`Away`, `Timed out`, *Your rival is back.* and a mark that names **the server** as the actor, so
`STORY-0313` is splittable the day `STORY-0214` merges rather than stalling on a question.

**Two follow-ups joined `STORY-0310` on 2026-08-16**, both found while splitting `STORY-0311` and
both about a test that cannot fail. `TASK-031014`: `TASK-031003` shipped `send`, `status` and
`close` and its test list named none of them — `drops a send made while no socket is open` reads
only the socket the frame would *not* reach, so deleting the `live` gate leaves it green, and
nothing anywhere calls `close()` or reads `status` on a reconnecting connection. `TASK-031015`:
`virtual-time.test.ts` flags every test file that names a timer without installing fake ones, and
does not flag **itself** only because one of its own fixture strings happens to spell
`vi.useFakeTimers(` — delete that fixture and the guard starts reporting its own source. The
exemption becomes explicit, and a test proves the exemption is what does the work.

**`STORY-0311` is split into eleven**, on a baseline of 316, and it starts once those two land —
cumulative counts **326 → 358**, with the follow-ups taking 316 to 320 first. It is the **client**
half of a read path `EPIC-02` already serves (`STORY-0211`), so no server changes: `GET /api/me` and
`GET /api/me/duels`, authenticated by the `X-Device-Id` the socket module already stores under one
key. The client derives nothing — the balance, the deltas and the outcomes come off the response
verbatim, and `TASK-031111` renders a balance of `5` beside duels summing to `0` so a client that
added them up prints the wrong number. `−1` is a correct balance (`ADR-0014`) and is rendered as
`−1`, in U+2212 as the result screen already prints it; a `401` is the ordinary first visit and
renders *no profile yet* with no alert; an empty duel list is an answer, not a `404`.

Two placement questions were settled rather than guessed. The fetch lives in a new
`web-client/src/profile/` — *the HTTP module* the epic's non-negotiables already name — with the
`fetch` and the `Storage` injected, and **not** in `src/protocol/`, which is exempt from the
boundary guard by path. And the read runs above the tree in a provider whose context defaults to
`null`: [`ADR-0032`](../docs/adr/ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) already
settled that this data *"is not a frame and does not enter this store"* and left *"how HTTP profile
data reaches screens"* to this story, so the duel store and `bootDuelClient` are both out — a
read-only `GET` needs neither one-per-tab nor a lifetime outside the tree. Defaulting to `null`
rather than throwing is what keeps `Lobby.test.tsx` and `App.test.tsx` rendering exactly what they
render today. Whether the client grows a *shared* HTTP data layer stays `EPIC-04`'s; this seam is
one file and is meant to be replaced by it. **No opponent is named** — the client's `RecentDuel`
drops `opponentPlayerId` at the parse, so no component has anywhere to leak it from.

**One follow-up joined `STORY-0311` on 2026-08-16**, found while splitting `STORY-0312` and deferred
twice before that. `TASK-031112`: nothing pins that the strip keeps the server's order of recent
duels. `TASK-031108`'s fixture is two rows already sorted descending by date — and ascending by hand
count, and ascending by id — so a client-side `sort` on any of the three ships green, and
`TASK-031111`'s guard reads the surface for leaks and takes no position on order. Three rows in an
order monotone in **no** field close it; `TASK-021107` already proved the server returns them newest
first and capped, so the client's whole job is to not touch them.

**`STORY-0312` is split into nine**, on a baseline of 358, and it starts once that follow-up lands —
cumulative counts **364 → 377**, with `TASK-031112` taking 358 to 359 first. Three of the nine are
`poker-server` and add no client test. It is the epic's **end-to-end** proof and `TASK-021206`'s
mirror on the other side of the wire: a whole duel, frame by frame, through the real boot, the real
store and the real screens over a `FakeSocket`.

Two things were settled rather than guessed. **How the fixture is obtained** — `DEC-022` left it a
ticket-level choice, and the split takes [`ADR-0020`](../docs/adr/ADR-0020-typescript-protocol-from-serial-descriptors.md)'s
existing answer instead of inventing a second mechanism: `generateDuelScript` writes
`web-client/src/e2e/scripted-duel.gen.json` from the server's own `ProtocolCodec`, `verifyDuelScript`
regenerates into `build/` and fails `:poker-server:check` on any byte of drift, and the generator
runs off the **test** runtime classpath so no fixture builder reaches the production jar. The frames
are therefore real *and* cannot rot, while the client's replay needs no JVM, server or database.
And **the script holds both seats' sessions, not one**, so every client-side claim is made twice from
two genuinely different inputs — two hole-card sets, two verdicts, two `Act` sequences. That is the
answer to the hazard that a value asserted only at a fixture default cannot be told from a constant.

The secrecy claim is over the **rendered DOM at every step**, never over the store, and it names the
secret rather than inferring it: `TASK-031202` reads the rival's real hole cards out of the *rival's*
own frames, and `TASK-031208` sweeps for them by three routes — a card element's `aria-label`, the
spoken name anywhere, and the raw `"Ah"` string — with four planted violations, the last of which
doctors a `Snapshot` frame before it reaches the decoder, exactly as `TASK-021207` doctored one a
layer down. `TASK-031209` then closes both directions: the hands in which the rival's cards appeared
must equal, set for set, the hands a `HandRevealed` named.

**Neither rematch nor presence is in it**, and the story now says so rather than leaving it implied:
`OfferRematch`/`RematchOffered` (`ADR-0044`) and `OpponentPresence`/`ActedForAbsentSeat`
(`ADR-0045`) exist in no Kotlin file and no generated type, so a script the server's own encoder
produced cannot contain one. `DEC-024` — whether a two-browser run exists at all — stays open and
stays the architect's; its answer changes nothing here, which is what the story said when it was
written and is still true.

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
| | [TASK-030703](tasks/TASK-030703-the-act-frame-echoes-the-turns-identity-verbatim.md) The Act frame echoes the turn's identity verbatim | S | **done** |
| | [TASK-030704](tasks/TASK-030704-the-bar-exists-in-every-state-and-waits-in-most.md) The bar exists in every state, and waits in most of them | S | **done** |
| | [TASK-030705](tasks/TASK-030705-one-button-per-action-the-server-allowed.md) One button per action the server allowed, and not one more | S | **done** |
| | [TASK-030706](tasks/TASK-030706-the-amount-control-is-clamped-to-the-bounds-the-server-sent.md) The amount control is clamped to the bounds the server sent | S | **done** |
| | [TASK-030707](tasks/TASK-030707-a-click-sends-one-act-and-the-bar-goes-quiet.md) A click sends one Act, and the bar goes quiet until the next turn | S | **done** |
| | [TASK-030708](tasks/TASK-030708-a-rejection-reads-from-its-own-fields.md) A rejection reads from its own fields, in the server's numbers | S | **done** |
| | [TASK-030709](tasks/TASK-030709-the-bar-states-what-the-server-refused.md) The bar states what the server refused, and retries nothing | S | **done** |
| | [TASK-030710](tasks/TASK-030710-the-bar-shows-no-number-and-offers-no-action-the-turn-did-not-carry.md) The bar shows no number and offers no action the turn did not carry | S | **done** |
| | [TASK-030711](tasks/TASK-030711-the-duel-screen-puts-the-bar-under-the-table.md) The duel screen puts the bar under the table and sends what it built | S | **done** |
| | [TASK-030712](tasks/TASK-030712-after-a-rejection-the-player-can-act-again.md) A rejection leaves the decision point open (store half, `ADR-0043`) | S | **done** |
| | [TASK-030713](tasks/TASK-030713-the-bar-comes-back-after-a-rejection.md) The bar comes back after a rejection, at the same decision point (bar half, `ADR-0043`) | S | **done** |
| | [TASK-030714](tasks/TASK-030714-the-refusal-sentence-stops-when-the-server-next-speaks.md) The refusal sentence stops when the server next speaks | S | **done** |
| | [TASK-030715](tasks/TASK-030715-the-derivation-guards-read-numeric-attributes.md) The derivation guards read the numbers that reach the DOM as attributes | S | **done** |
| | [TASK-030716](tasks/TASK-030716-a-rejection-leaves-the-act-identity-valid.md) The server proves a rejection leaves the client's Act identity valid | S | **done** |
| | [TASK-030717](tasks/TASK-030717-a-frame-from-an-earlier-hand-is-dropped.md) A frame from an earlier hand is dropped, though its sequence fits | XS | **done** |
| **[STORY-0308](stories/STORY-0308-result-screen.md)** The result screen — who won, and the coin — *schema 2* | | **ready** |
| | [TASK-030801](tasks/TASK-030801-a-duel-outcome-fixture-with-every-field-the-wire-declares.md) A DuelOutcome fixture with every field the wire declares | S | **done** |
| | [TASK-030802](tasks/TASK-030802-the-verdict-is-read-off-the-winner-and-your-seat.md) The verdict is read off the winner and your seat, and nothing else | S | **done** |
| | [TASK-030803](tasks/TASK-030803-the-coin-line-states-the-one-coin-the-duel-moved.md) The coin line states the one coin the duel moved, and no balance | XS | **done** |
| | [TASK-030804](tasks/TASK-030804-the-coin-mark-is-steel-and-says-nothing.md) The coin mark is steel, and says nothing a screen reader has to hear twice | XS | **done** |
| | [TASK-030805](tasks/TASK-030805-the-result-screen-declares-the-verdict-and-the-coin.md) The result screen declares the verdict and the coin beside it | S | **done** |
| | [TASK-030806](tasks/TASK-030806-the-result-states-the-hands-played-and-both-final-stacks.md) The result states the hands played and every final stack, exactly as sent | S | **done** |
| | [TASK-030807](tasks/TASK-030807-the-way-on-from-the-result-is-back-to-the-lobby.md) The way on is back to the lobby, and there is no dead rematch | XS | **done** |
| | [TASK-030808](tasks/TASK-030808-the-result-derives-no-winner-and-no-figure.md) The result derives no winner and shows no figure the outcome did not carry | S | **done** |
| | [TASK-030809](tasks/TASK-030809-the-duel-screen-shows-the-result-when-the-duel-ends.md) The duel screen shows the result when the duel ends | S | **done** |
| [STORY-0309](stories/STORY-0309-rematch.md) | Rematch from the result screen (needs `STORY-0213`) | **ready** |
| **[STORY-0310](stories/STORY-0310-reconnect-and-resume.md)** Reconnect — the client resumes its seat — *schema 2* | | **ready** |
| | [TASK-031001](tasks/TASK-031001-the-room-code-lives-under-one-key-this-module-owns.md) The room code lives under one storage key this module owns | XS | **done** |
| | [TASK-031002](tasks/TASK-031002-the-retry-delay-doubles-to-a-ceiling-and-spends-the-jitter.md) The retry delay doubles to a ceiling and spends the jitter it is handed | XS | **done** |
| | [TASK-031003](tasks/TASK-031003-a-closed-socket-is-reopened-on-virtual-time.md) A closed socket is reopened, on virtual time, when the backoff says so | S | **done** |
| | [TASK-031004](tasks/TASK-031004-a-socket-the-tab-replaced-starts-no-retry-of-its-own.md) A socket the tab has replaced starts no retry of its own | S | **done** |
| | [TASK-031005](tasks/TASK-031005-a-version-mismatch-ends-the-retry-loop-for-good.md) A version mismatch ends the retry loop for good | XS | **done** |
| | [TASK-031006](tasks/TASK-031006-the-tabs-one-connection-is-the-one-that-comes-back.md) The tab's one connection is the one that comes back | XS | **done** |
| | [TASK-031007](tasks/TASK-031007-boot-remembers-each-room-the-server-seats-it-in.md) Boot remembers each room the server seats it in | S | **done** |
| | [TASK-031008](tasks/TASK-031008-with-no-code-in-hand-boot-rejoins-the-room-it-remembers.md) With no code in hand, boot rejoins the room it remembers | S | **done** |
| | [TASK-031009](tasks/TASK-031009-a-finished-duel-is-forgotten-so-the-lobby-stays-reachable.md) A finished duel is forgotten, so the way back to the lobby stays open | XS | **done** |
| | [TASK-031010](tasks/TASK-031010-a-room-that-is-gone-is-forgotten-and-no-socket-resumes-into-it.md) A room that is gone is forgotten, and no socket resumes into it | S | **done** |
| | [TASK-031011](tasks/TASK-031011-the-reopened-socket-says-hello-then-rejoins-once-each.md) The reopened socket says Hello, then rejoins, once each | S | **done** |
| | [TASK-031012](tasks/TASK-031012-the-table-repaints-from-the-snapshot-that-followed-the-resume.md) The table repaints from the snapshot that followed the resume | S | **done** |
| | [TASK-031013](tasks/TASK-031013-no-client-test-sleeps-on-a-real-clock.md) No client test sleeps on a real clock | XS | **done** |
| | [TASK-031014](tasks/TASK-031014-the-reconnecting-connections-own-surface-is-proven.md) The reconnecting connection's own send, status and close are proven | S | **done** |
| | [TASK-031015](tasks/TASK-031015-the-virtual-time-guard-exempts-itself-on-purpose.md) The virtual-time guard exempts itself on purpose, not by accident | XS | **done** |
| **[STORY-0311](stories/STORY-0311-profile-strip.md)** The profile strip — my coins and my recent duels — *schema 2* | | ready |
| | [TASK-031101](tasks/TASK-031101-one-get-carrying-the-device-id-the-server-reads.md) One GET, carrying the device id, with three answers | S | **done** |
| | [TASK-031102](tasks/TASK-031102-the-profile-read-states-the-balance-the-server-sent.md) The profile read states the balance the server sent, sign and all | S | **done** |
| | [TASK-031103](tasks/TASK-031103-the-recent-duels-read-drops-the-opponent.md) The recent-duels read keeps every field but the opponent's identifier | S | **done** |
| | [TASK-031104](tasks/TASK-031104-every-outcome-every-sign-and-what-the-parse-refuses.md) Every outcome, every sign, and what the duel parse refuses | S | **done** |
| | [TASK-031105](tasks/TASK-031105-the-words-a-duel-line-is-made-of.md) The words a profile line is made of | S | **done** |
| | [TASK-031106](tasks/TASK-031106-one-read-answers-the-whole-strip.md) One read answers the whole strip, or none of it | S | **done** |
| | [TASK-031107](tasks/TASK-031107-the-strip-states-the-balance-or-says-there-is-none.md) The strip states the balance, or says there is no profile yet | S | **done** |
| | [TASK-031108](tasks/TASK-031108-one-line-per-recent-duel-and-a-word-when-there-are-none.md) One line per recent duel, and a word when there are none | S | **done** |
| | [TASK-031109](tasks/TASK-031109-the-read-runs-once-above-the-tree-and-nowhere-else.md) The strip's read runs once above the tree | S | **done** |
| | [TASK-031110](tasks/TASK-031110-the-lobby-shows-the-strip-and-the-duel-does-not.md) The lobby shows the strip, and a duel in progress does not | S | **done** |
| | [TASK-031111](tasks/TASK-031111-the-strip-names-no-opponent-and-counts-no-coin.md) The strip names no opponent and counts no coin | S | **done** |
| | [TASK-031112](tasks/TASK-031112-the-strip-keeps-the-order-the-server-sent.md) The strip lists recent duels in the order the server sent them | XS | **done** |
| **[STORY-0312](stories/STORY-0312-whole-duel-through-the-client.md)** A whole duel through the client, frame by frame — *schema 2* | | ready |
| | [TASK-031201](tasks/TASK-031201-played-duel-records-the-acts-it-sent.md) A played duel records the Act it sent, and the seat it sent it from | XS | **done** |
| | [TASK-031202](tasks/TASK-031202-a-whole-duel-as-one-seats-session-of-frames.md) A whole duel written down as each seat's own session of frames | S | **done** |
| | [TASK-031203](tasks/TASK-031203-one-task-writes-the-script-another-fails-on-drift.md) One Gradle task writes the duel script, another fails the build on drift | S | **done** |
| | [TASK-031204](tasks/TASK-031204-the-client-reads-the-script-and-proves-it-is-a-duel.md) The client reads the committed script, and proves it is a whole duel | S | **done** |
| | [TASK-031205](tasks/TASK-031205-the-script-replays-through-the-real-client.md) The script replays through the real client, from either seat, to the result | S | **done** |
| | [TASK-031206](tasks/TASK-031206-one-act-per-turn-the-frame-the-server-recorded.md) The client answers each turn through the bar, with the frame the server recorded | S | **done** |
| | [TASK-031207](tasks/TASK-031207-the-result-states-the-outcome-the-last-frame-carried.md) The result states the outcome the script's last frame carried, from either seat | S | **done** |
| | [TASK-031208](tasks/TASK-031208-no-rival-card-reaches-the-screen-before-the-reveal.md) No rival card reaches the screen before the frame that reveals it | S | **done** |
| | [TASK-031209](tasks/TASK-031209-a-hand-won-without-a-showdown-shows-no-rival-card.md) A hand won without a showdown shows no rival card at all | S | **done** |
| [STORY-0313](stories/STORY-0313-the-table-names-an-absent-opponent.md) | The table names an absent opponent (needs `STORY-0214`) | **blocked** |

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

---

## EPIC-04 — Identity and profiles

Opened 2026-08-16: seventeen stories written, `STORY-0401` split into eighteen tickets and merged in
full. `STORY-0402` was split on 2026-08-17 into **five**, and it is short for a reason worth keeping:
`TASK-040107` routed every test-side `DuelSummaryResponse` through one builder ahead of time, so
widening the DTO costs three files instead of the four that four `STORY-0401` tickets each needed.
Two more
stories than the epic's scoping table named — [`ADR-0031`](../docs/adr/ADR-0031-an-optional-verified-recovery-email.md)
was accepted after that table was written and its recovery email, verification and reset need a home
(`STORY-0416`, `STORY-0417`). `STORY-0404` and `STORY-0406` are re-cut because
[`ADR-0030`](../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §1 collapses
sign-up and the claim into one endpoint.

`STORY-0401` goes first, ahead of the credential chain the epic called its critical path, for a
reason all three schema ADRs state: each says its migration takes *the next free `V<n>` at merge
time*. Landing the display name first makes it `V3` and leaves the rest nothing to race over.

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| **[STORY-0401](stories/STORY-0401-display-name-and-the-write-path.md)** `player.display_name`, its canonical form, and the write path — *schema 2* | | | **done** |
| | [TASK-040101](tasks/TASK-040101-the-third-migration-adds-the-name-and-its-guarantees.md) The third migration adds the name and its four guarantees | S | **done** |
| | [TASK-040102](tasks/TASK-040102-the-checks-refuse-what-they-were-written-to-refuse.md) The three checks refuse what they were written to refuse | S | **done** |
| | [TASK-040103](tasks/TASK-040103-one-name-whatever-case-it-is-typed-in.md) One name, whatever case it is typed in | S | **done** |
| | [TASK-040104](tasks/TASK-040104-permanence-fires-and-only-on-the-column-it-names.md) Permanence fires, and only on the column it names | S | **done** |
| | [TASK-040105](tasks/TASK-040105-the-canonical-name-is-trimmed-nfc-and-counted-in-code-points.md) The canonical name is trimmed, NFC, and counted in code points | S | **done** |
| | [TASK-040106](tasks/TASK-040106-the-canonical-form-refuses-the-invisible-and-the-doubled-space.md) The canonical form refuses the invisible and the doubled space | S | **done** |
| | [TASK-040107](tasks/TASK-040107-one-builder-makes-every-profile-dto-a-test-uses.md) One builder makes every profile DTO a test uses | S | **done** |
| | [TASK-040108](tasks/TASK-040108-profile-response-carries-the-name-the-row-holds.md) `ProfileResponse` carries the name the row holds | XS | **done** |
| | [TASK-040109](tasks/TASK-040109-the-name-is-on-the-wire-and-it-is-the-one-stored.md) The name is on the wire, and it is the one stored | S | **done** |
| | [TASK-040110](tasks/TASK-040110-the-profile-writes-port-and-its-sealed-answer.md) The `ProfileWrites` port, its sealed answer, and no lookup by name | S | **done** |
| | [TASK-040111](tasks/TASK-040111-one-statement-three-answers.md) One statement, three answers | S | **done** |
| | [TASK-040112](tasks/TASK-040112-two-writers-one-name.md) Two writers, one name: the loser is refused and keeps its nothing | S | **done** |
| | [TASK-040113](tasks/TASK-040113-the-one-field-the-request-body-carries.md) The one field the request body carries | XS | **done** |
| | [TASK-040114](tasks/TASK-040114-the-server-it-ships-with-can-write-a-name.md) The server it ships with can write a name | XS | **done** |
| | [TASK-040115](tasks/TASK-040115-put-api-me-name-identity-first-then-the-name.md) `PUT /api/me/name`: identity first, then the name it accepts | S | **done** |
| | [TASK-040116](tasks/TASK-040116-the-two-refusals-a-client-must-tell-apart.md) The two refusals a client must tell apart | XS | **done** |
| | [TASK-040117](tasks/TASK-040117-a-name-set-over-http-comes-back-on-the-next-read.md) A name set over HTTP comes back on the next read | S | **done** |
| | [TASK-040118](tasks/TASK-040118-document-the-name-endpoint.md) Document the name endpoint and what each answer means | S | **done** |
| **[STORY-0402](stories/STORY-0402-the-read-path-carries-the-display-name.md)** The read path carries the display name — *schema 2* | | | **done** |
| | [TASK-040201](tasks/TASK-040201-the-duel-line-joins-the-opponents-row-and-carries-their-name.md) The duel line joins the opponent's row and carries their name | XS | **done** |
| | [TASK-040202](tasks/TASK-040202-a-named-opponent-an-unnamed-one-and-a-name-set-afterwards.md) A named opponent, an unnamed one, and a name set after the duel | S | **done** |
| | [TASK-040203](tasks/TASK-040203-three-duels-three-opponents-one-prepared-statement.md) Three duels, three opponents, one prepared statement | S | **done** |
| | [TASK-040204](tasks/TASK-040204-present-as-null-not-absent-on-the-real-response.md) Present as `null`, not absent, on the response the route actually writes | S | **done** |
| | [TASK-040205](tasks/TASK-040205-the-document-names-the-field-and-the-test-agrees-with-the-dto.md) The document names the field, and the test agrees with the DTO | XS | **done** |
| **[STORY-0403](stories/STORY-0403-credentials-storage-and-hashing.md)** Credentials — the schema, the hash, and a port that returns none — *schema 2* | | | **done** |
| | [TASK-040301](tasks/TASK-040301-the-fourth-migration-adds-the-credential-and-the-session.md) The fourth migration adds the credential and the auth session | S | **done** |
| | [TASK-040302](tasks/TASK-040302-one-identifier-one-kind-one-row.md) One identifier, one kind, one row — and the player it points at must exist | S | **done** |
| | [TASK-040303](tasks/TASK-040303-one-token-hash-one-row-and-nothing-cascades.md) One token hash, one row — and no foreign key cascades | S | **done** |
| | [TASK-040304](tasks/TASK-040304-bouncy-castle-argon2id-against-the-published-vector.md) Bouncy Castle on the classpath, pinned to the published Argon2id vector | S | **done** |
| | [TASK-040305](tasks/TASK-040305-the-phc-string-this-project-writes.md) The PHC string this project writes, and the one function that writes it | S | **done** |
| | [TASK-040306](tasks/TASK-040306-the-parser-refuses-every-string-we-did-not-write.md) The parser accepts what we wrote and refuses everything else | S | **done** |
| | [TASK-040307](tasks/TASK-040307-two-values-that-print-a-redaction.md) Two values that print a redaction, in every form a string can take | S | **done** |
| | [TASK-040308](tasks/TASK-040308-hash-a-secret-prove-a-secret-compare-in-constant-time.md) Hash a secret, prove a secret, and compare the tags in constant time | S | **done** |
| | [TASK-040309](tasks/TASK-040309-four-verifications-at-a-time-and-no-more.md) Four verifications at a time, and no more | S | **done** |
| | [TASK-040310](tasks/TASK-040310-the-login-handle-is-folded-before-it-is-stored.md) The login handle is folded before it is stored, and the fold is ASCII | S | **done** |
| | [TASK-040311](tasks/TASK-040311-the-credentials-port-answers-a-player-id-or-nothing.md) The `Credentials` port answers a `PlayerId` or nothing | S | **done** |
| | [TASK-040312](tasks/TASK-040312-postgres-credentials-writes-one-row-and-reads-no-hash-back.md) `PostgresCredentials` writes one row and reads no hash back | S | **done** |
| | [TASK-040313](tasks/TASK-040313-an-unknown-identifier-costs-what-a-wrong-secret-costs.md) An unknown identifier costs exactly what a wrong secret costs | S | **done** |
| | [TASK-040314](tasks/TASK-040314-nothing-public-returns-a-hash.md) Nothing public returns a hash, and the sweep proves it can tell | S | **done** |
| **[STORY-0404](stories/STORY-0404-sign-up-an-account-for-the-profile-already-here.md)** Sign-up — one endpoint, attaching an account to the profile already here | | | **done** |
| | [TASK-040401](tasks/TASK-040401-one-rule-for-a-password-and-it-is-length.md) One rule for a password, and it is length | S | **done** |
| | [TASK-040402](tasks/TASK-040402-the-hasher-hashes-the-nfc-form.md) The hasher hashes the NFC form, in the one place a secret becomes bytes | XS | **done** |
| | [TASK-040403](tasks/TASK-040403-the-port-can-ask-what-a-player-already-holds.md) The port can ask whether a player already holds a kind of credential | XS | **done** |
| | [TASK-040404](tasks/TASK-040404-one-select-for-that-player-and-that-kind.md) One SELECT, and it answers for that player and that kind only | S | **done** |
| | [TASK-040405](tasks/TASK-040405-the-sign-up-body-is-two-fields-and-it-prints-neither.md) The sign-up body is two fields, and it prints neither | S | **done** |
| | [TASK-040406](tasks/TASK-040406-the-doubles-a-sign-up-route-test-records-against.md) The doubles every sign-up route test records against | S | **done** |
| | [TASK-040407](tasks/TASK-040407-the-handle-is-judged-first-then-the-password.md) The handle is judged first, then the password | S | **done** |
| | [TASK-040408](tasks/TASK-040408-sign-up-identity-first-then-the-body.md) POST /api/auth/sign-up — identity first, then the body | S | **done** |
| | [TASK-040409](tasks/TASK-040409-one-create-with-the-player-the-server-resolved.md) One create, with the player the server resolved | S | **done** |
| | [TASK-040410](tasks/TASK-040410-no-outcome-carries-a-body-and-nothing-prints-a-secret.md) No outcome carries a body, and nothing on the path can print a secret | S | **done** |
| | [TASK-040411](tasks/TASK-040411-the-server-it-ships-with-can-sign-up.md) The server it ships with can sign up | S | **done** |
| | [TASK-040412](tasks/TASK-040412-one-credential-row-and-the-player-table-untouched.md) One credential row, and the player table untouched across it | S | **done** |
| | [TASK-040413](tasks/TASK-040413-the-coin-a-duel-paid-survives-the-sign-up.md) The coin a duel paid is still there after the sign-up | S | **done** |
| | [TASK-040414](tasks/TASK-040414-the-document-names-the-sign-up-endpoint.md) The document names the sign-up endpoint, and a test agrees with the code | S | **done** |
| [STORY-0405](stories/STORY-0405-sign-in-the-session-and-what-the-socket-presents.md) Sign-in, the session, and what the socket presents (needs `STORY-0213`, `STORY-0214`) | | | backlog |
| [STORY-0406](stories/STORY-0406-the-claim-proven-and-the-device-revoked.md) The claim proven, and the device binding revoked (needs `STORY-0405`) | | | backlog |
| [STORY-0407](stories/STORY-0407-recovery-from-a-device-never-seen.md) Recovery — signing in from a device that has never been seen | | | backlog |
| **[STORY-0408](stories/STORY-0408-duel-history-paged-over-the-whole-record.md)** Duel history, paged over the whole record | | | **done** |
| | [TASK-040801](tasks/TASK-040801-a-cursor-is-a-duels-place-in-the-list.md) A cursor is one duel's place in the list, and it survives the round trip | S | **done** |
| | [TASK-040802](tasks/TASK-040802-the-read-takes-a-cursor-and-compares-the-whole-tuple.md) The read takes a cursor, and PostgreSQL compares the whole tuple | S | **done** |
| | [TASK-040803](tasks/TASK-040803-seven-duels-in-pages-of-three-each-exactly-once.md) Seven duels in pages of three, each exactly once | S | **done** |
| | [TASK-040804](tasks/TASK-040804-a-duel-that-finishes-between-two-pages.md) A duel that finishes between two pages repeats nothing and skips nothing | S | **done** |
| | [TASK-040805](tasks/TASK-040805-two-duels-in-the-same-instant-still-page.md) Two duels that finished in the same instant still page | XS | **done** |
| | [TASK-040806](tasks/TASK-040806-the-response-says-whether-there-is-a-next-page.md) The response says whether there is a next page, as null and not as absent | XS | **done** |
| | [TASK-040807](tasks/TASK-040807-the-port-takes-the-cursor-and-the-doubles-follow.md) The port's duel read takes the cursor, and both doubles follow | XS | **done** |
| | [TASK-040808](tasks/TASK-040808-the-endpoint-accepts-a-cursor-and-refuses-a-malformed-one.md) The endpoint accepts a cursor, and a malformed one is a 400 that reads nothing | S | **done** |
| | [TASK-040809](tasks/TASK-040809-one-row-more-than-the-page.md) One row more than the page, and the last page says there is no next | S | **done** |
| | [TASK-040810](tasks/TASK-040810-over-http-against-the-database-every-duel-once.md) Over HTTP, against the database — every duel exactly once, and one player's cursor | S | **done** |
| | [TASK-040811](tasks/TASK-040811-the-document-contracts-the-cursor.md) The document contracts the cursor and the paging rule, and a test agrees with the DTO | S | **done** |
| [STORY-0409](stories/STORY-0409-history-filters-and-search.md) History filters and search | | | ready |
| | [TASK-040901](tasks/TASK-040901-a-filter-is-two-axes-and-an-outcome-is-one-of-three-names.md) A filter is two axes, and an outcome is one of exactly three names | S | **done** |
| | [TASK-040902](tasks/TASK-040902-the-search-term-the-server-will-accept.md) The search term the server will accept, counted in code points | S | **done** |
| | [TASK-040903](tasks/TASK-040903-the-read-takes-a-filter-and-an-outcome-is-a-sign.md) The read takes a filter, and an outcome is the sign of the stored delta | S | **done** |
| | [TASK-040904](tasks/TASK-040904-the-search-is-a-substring-of-the-opponents-name.md) The search is a substring of the opponent's name, folded under the pinned collation | S | **done** |
| | [TASK-040905](tasks/TASK-040905-the-search-term-is-not-a-language.md) The search term is not a language, and an unnamed opponent is not a match | XS | **done** |
| | [TASK-040906](tasks/TASK-040906-paging-inside-a-filter-is-still-total-and-disjoint.md) Paging inside a filter is still total and disjoint, across an insert that matches it | S | **done** |
| | [TASK-040907](tasks/TASK-040907-the-port-takes-the-filter-and-both-doubles-follow.md) The port's duel read takes the filter, and both doubles follow | XS | **done** |
| | [TASK-040908](tasks/TASK-040908-two-parameters-become-one-filter-or-one-refusal.md) Two query parameters become one filter, or one refusal | XS | **done** |
| | [TASK-040909](tasks/TASK-040909-the-endpoint-reads-the-filter-and-refuses-what-it-refuses.md) The endpoint reads the filter, and refuses what the parsers refuse | S | **done** |
| | [TASK-040910](tasks/TASK-040910-over-http-against-the-database-a-filtered-page.md) Over HTTP, against the database — a filtered page is exactly the filtered rows | S | **done** |
| | [TASK-040911](tasks/TASK-040911-the-document-contracts-both-filters-and-what-each-refuses.md) The document contracts both filters, and what each of them refuses | S | ready |
| [STORY-0410](stories/STORY-0410-the-display-name-product-rules.md) The display-name product rules — screened when set, takeable away | | | backlog |
| [STORY-0411](stories/STORY-0411-the-name-in-the-client.md) The name in the client — shown, and settable | | | backlog |
| [STORY-0412](stories/STORY-0412-the-account-screens.md) The account screens — sign up, sign in, sign out, and which routes are live | | | backlog |
| [STORY-0413](stories/STORY-0413-the-history-screen.md) The history screen — pages, filters, search | | | backlog |
| [STORY-0414](stories/STORY-0414-claimed-here-recovered-there.md) Claimed here, recovered there, end to end | | | backlog |
| [STORY-0415](stories/STORY-0415-the-offer-after-a-first-win.md) The offer — an account after a first win, dismissed for good | | | backlog |
| [STORY-0416](stories/STORY-0416-the-recovery-email-and-the-password-reset.md) The recovery email, verified, and the password reset | | | backlog |
| [STORY-0417](stories/STORY-0417-the-recovery-screens.md) The recovery screens — attach an address, and reset a password | | | backlog |

Two decisions were raised while splitting, both the architect's, both blocking exactly one story.
`DEC-041` (the shape of device revocation) is answered by
[`ADR-0049`](../docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md) — the edge
leaves `player` for its own `device_binding` table, so `ADR-0030` §2 gains no fourth writer because
the column it protected no longer exists, and revoking is final in the database rather than by
convention. It unblocks `STORY-0406` and raised `DEC-045`, since answered by
[`ADR-0050`](../docs/adr/ADR-0050-revoking-the-device-signs-the-player-out-everywhere-but-here.md):
**one button** — revoking ends every other session and keeps the revoking one, so `STORY-0406`
ships the `auth_session` `DELETE` with the endpoint rather than as a later PR.
`DEC-042` (the operator's force-rename path) is answered by
[`ADR-0051`](../docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md) — **one table is the
whole namespace**: `name_registry` holds names in use, blocked names and retired names as three
values of one column, a string never leaves it, and the operator's path is
`retire_display_name(player_id, expected_name)` called from `psql` rather than an endpoint nobody
could authenticate without the role system `ADR-0038` refused. It collapses `ADR-0038`'s three
sources of truth into one `INSERT` — which, read literally, had been a `READ COMMITTED` race rather
than a checklist. It unblocks `STORY-0410` and raised `DEC-046`, since answered by
[`ADR-0052`](../docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md): the player it
happened to **is told**, on the surface where a name is set, and nobody else is told anything.
Silence lost on a defect rather than on taste — the affected player's obvious next move is to retype
their own name, `ADR-0051` §2 answers `409`, and `STORY-0411` was on course to render that as
*taken*, which the server knows to be false about a string nobody holds and nobody ever can. That
ADR in turn raised `DEC-047`, since answered by
[`ADR-0053`](../docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md): `ProfileResponse` gains
`displayNameRemoved: Boolean`, **with no default value** — the route's `Json` has
`encodeDefaults = false` while `protocolJson` has it `true`, so a defaulted field would be present
in every test's JSON and absent from the wire for almost every player. It is computed by one
correlated `EXISTS`, never a `LEFT JOIN`: a player may hold two retired names, and a join returns
two rows for one profile — a bug that needs a second takedown before it appears.
`PROTOCOL_VERSION` does not move; `ProfileResponse` is reachable from neither `ClientMessage` nor
`ServerMessage`, so the fingerprint does not see it.
Neither blocks the epic, and neither is the human's — every product question this epic had was
answered on 2026-08-15.

`STORY-0403` was split on 2026-08-17 into **fourteen**, and it is the longest chain in the epic for
the reason its title carries: everything from sign-up to recovery stands on it, and the whole story
lands with no endpoint, so every guarantee has to be structural. Two more decisions came out of the
split and neither blocked a ticket — `DEC-043` (what may a password be), now answered by
[`ADR-0048`](../docs/adr/ADR-0048-a-password-has-one-rule-and-it-is-length.md) ahead of
`STORY-0404`, and `DEC-044` (what the day the Argon2 cost is raised costs), now answered by
[`ADR-0054`](../docs/adr/ADR-0054-a-raised-argon2-cost-is-a-ledger-entry-and-a-rehash.md): a closed
append-only ledger of parameter sets in source, a rehash in `verify` after a *correct* password, and
**nothing built until the day the cost actually changes** — which is today one commit away from
locking out every existing account. The downgrade refusal cannot compare against the current cost,
since every historical entry is weaker by definition, so it turns on a fixed `ARGON2_FLOOR` equal to
the first set ever shipped: appending a weak set reddens the pinned literals, inserting one breaks
the strict ascent, and **prepending** one — which a bare ascent check allows — is below the floor.
