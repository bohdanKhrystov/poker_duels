# Board

The index. Conventions live in [`README.md`](README.md).

**Now:** `EPIC-01` is **done**, and so was `EPIC-02` — the engine, and a duel server that plays a
whole duel over two real sockets against PostgreSQL, pays the winner a coin, and survives a
disconnect. `EPIC-02` **reopened on 2026-08-16** for two stories:
[`ADR-0044`](../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) answers `DEC-023`
and puts the rematch's wire half in `STORY-0213`, and
[`ADR-0045`](../docs/adr/ADR-0045-presence-belongs-to-the-table.md) answers `DEC-038` and puts the
pause state's wire half in `STORY-0214` — both where the code lives, rather than in `EPIC-03`. They
land one at a time, `0213` first, because each moves `PROTOCOL_VERSION`. `STORY-0213` was split on
2026-08-23 into seven tickets and **runs**: the wire step is fifteen files, none of them separable,
which `ADR-0068` settles by letting a ticket declare the true count and name the merged gates that
forbid splitting it — and `ADR-0069`, after an implementation attempt found three more than the
split had, by deleting the ceiling, checking the count against the ticket's own *Files* table, and
sizing a bump by **probing** the gates rather than remembering a list.
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

**`STORY-0405` unparked on 2026-08-23** — `STORY-0213` and `STORY-0214` both merged, freeing
`ADR-0047`'s one-bumping-branch-at-a-time lock — and split into **twenty-four** tickets. It is the
root of the epic's remaining eight stories, so the split was sized by the `ADR-0070` probe rather
than by estimate: **two** atomic tickets, each measured against the full gate set until it exited
`0`. `TASK-040502` (the wire and the version) is **27** files; `TASK-040511` (`ADR-0030` §4's
player-keyed profile read, and every route resolving identity through one resolver) is **25**. In
both, failures appeared *after* the compiler was green — a golden TypeScript declaration, three
`vitest` frame assertions, a route double's recorded key, and ten files' worth of ktlint — which is
the clearest evidence yet that a red run names a prefix and not a set. The split also corrects two
sentences in the story: the bump is `5`, not `3`, and it is the **second** ticket rather than the
last, because `ProtocolVersionLedgerTest` will not let a wire field land before its number.

`EPIC-05` (ranking, duel coins and leaderboard) was written on 2026-08-19, parked the same day, and
**unparked the same day**: `DEC-055` — *what is a season, and what does one do to a duel coin?* — is
answered by [`ADR-0061`](../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md).
A season is **one calendar month in UTC, derived and never stored**; the ladder is a window over it;
a boundary **does nothing**, and `player.coin_balance` is never reset. The product-owner run took the
branch the vision licenses and declined the other out loud: a reset would make *"a counter of duels
won"* false, and that is the human's to change, not an ADR's. `STORY-0501` is `ready` with no
migration, `STORY-0505` is **`dropped`** because the crossing it was written for turns out to be no
code at all, and **every decision on the epic's critical path is now answered** — `STORY-0502` is
gated by none and waits only on being split. `DEC-056` is answered by
[`ADR-0063`](../docs/adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md):
**nothing gates a place**, a nameless player has a row that reads `No name`, and the farming vector
`ADR-0012` gated on this epic is accepted out loud until the ladder is served on a public address.
`DEC-058` is answered by
[`ADR-0064`](../docs/adr/ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md):
**tied players share one rank number** — `1 + the number of players standing strictly higher`, so
the ladder prints `3, 3, 5` — and the order rows sit in is not a ranking, which leaves the tiebreak
key inside `DEC-061` where it belongs. `DEC-059` is answered by
[`ADR-0065`](../docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md): **the ladder hands a
player their own row** — one self line above the rows, stating their rank and their season standing,
served with the page — and **the profile strip keeps the all-time coin and gains nothing**, so
`STORY-0502` ships two aggregates in one response and `STORY-0503` marks no row. `DEC-060` (a
finished season on a screen) and `DEC-061` (a page over a season aggregate) were raised by
`DEC-055`'s answer; `DEC-060` blocks nothing today, and **`DEC-061` is answered** by
[`ADR-0066`](../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md):
**the ladder is computed per request** — nothing stores a standing, so a duel is on the ladder the
instant it commits — **and a walk is pinned to the instant it began**, so it returns every player of
the ladder *as it stood at that cutoff* exactly once, is **not live**, and carries one named
exception where a row can still be seen twice or missed. `STORY-0502` is gated by no decision and
waits only on `/plan-story`; `STORY-0503` waits on `STORY-0502` landing.

Startable right now: `python3 .github/scripts/lint_tickets.py --startable`

---

## Epics

| ID | Title | Status | Milestone |
| --- | --- | --- | --- |
| [EPIC-00](epics/EPIC-00-ways-of-working.md) | Ways of working | **in progress** | v0.1 |
| [EPIC-01](epics/EPIC-01-poker-engine.md) | Poker engine | **done** | v0.1 |
| [EPIC-02](epics/EPIC-02-duel-server.md) | Duel server — rooms, WebSocket protocol, persistence | **done** — 14 of 14 stories; closed 2026-08-14, reopened for `STORY-0213` and `STORY-0214`, both of which merged, closing it again on 2026-08-26 | v0.1 |
| [EPIC-03](epics/EPIC-03-web-client.md) | Web client — table, lobby, duel flow | **done** — 14 of 14 stories done (`STORY-0309` closed on 2026-08-24); `STORY-0313` unblocked on 2026-08-24 when `STORY-0214` merged and is **split into fifteen**, all fifteen done, closing the epic on 2026-08-24 — it raised `DEC-070` (how long the server's own action stays on screen, the **product owner's**), answered on 2026-08-24 by [`ADR-0075`](../docs/adr/ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md), so **nothing in the story is blocked**; `STORY-0314` **closed on 2026-08-24**, five of five, leaving `STORY-0313` the only story left in the epic | v0.1 |
| [EPIC-04](epics/EPIC-04-identity-and-profiles.md) | Identity and profiles | **in progress** — 12 of 17 stories done; `STORY-0405` unparked on 2026-08-23 when `STORY-0213` and `STORY-0214` merged, and is **split into 24 tickets** with `TASK-040501` startable. It raised `DEC-069` (the sign-in budget's two numbers, the architect's), answered on 2026-08-24 by [`ADR-0074`](../docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md) — nothing in the story is blocked. `STORY-0416` was split out of numerical order on 2026-08-25 into **29 tickets**, since it depends only on the finished `STORY-0405` while `0412`, `0414`, `0415` and `0417` all trace through `DEC-054`; it raised `DEC-071` (the product owner's) and `DEC-072`, `DEC-073`, `DEC-074` (the architect's), blocking six of its own tickets and nothing else. `DEC-072` was answered on 2026-08-25 by [`ADR-0077`](../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md), which also raised `DEC-075` — blocking nothing — and `DEC-073` the same day by [`ADR-0079`](../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md), leaving all four answered — `DEC-074` on 2026-08-25 by [`ADR-0080`](../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md), which resolves a contradiction between `ADR-0031` §4 and §5 rather than filling a gap. Both answers were then folded back into the split: `TASK-041627` was re-cut into six tickets as `ADR-0077` §Consequences required, and `ADR-0078` turned `TASK-041601`'s conditionally-parked collation follow-up into `TASK-041635`, taking the story to **35 tickets**. Three further corrections on 2026-08-25 took it to **38**, none of them a new decision: `ADR-0079` §Consequences named a defect against `TASK-041607`/`TASK-041608`/`TASK-041625` — `ADR-0031` §5's fifteen-minute resend suppression was built on one of the two mail paths — so `claimPending` now answers `ClaimPendingResult` rather than `Unit`, `TASK-041636` enforces the rule inside the writing transaction and `TASK-041637` makes the handler mail only when it wrote; and `TASK-041638` widens `TASK-041606`'s shape gate, which held for functions and for nothing else. `DEC-075` — raised by `ADR-0077` and blocking nothing — was answered on 2026-08-25 by [`ADR-0081`](../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md), **leaving `EPIC-04` with no open decision at all**; `ADR-0080` and `ADR-0081` were then folded back into the split on the same day, correcting five tickets, adding none and moving no status. A later planner pass took it to **40**, on two findings from tickets that had already run rather than on any decision: `TASK-041624`'s deliberately empty fixture tables were filled from `ADR-0078` §6 and it gained a fourth test, and `TASK-041639` and `TASK-041640` were added because `TASK-041614` shipped a title claiming an atomicity none of its five tests hold. A later pass took it to **41**, again on a ticket that had already run: `TASK-041641` carries the two `PostgresProfileReadsTest` methods `TASK-041616` named in its *Tests* section but, being `atomic:`, could not hold — its six-row *Files* table is its whole change and that file is not in it, because no gate names it. Splitting them out rather than inventing a fourth `atomic:` item is the point: an item must name a merged gate that fails on the smaller commit, and `TASK-041616`'s probe reached green without that file, which is the proof there is none. **`TASK-041607` merged and `TASK-041616` is now the story's single startable ticket**. `STORY-0412` was split on 2026-08-26 into **27**, out of numerical order because `STORY-0416`'s chain is stalled behind `DEC-076` while `STORY-0414`, `0415` and `0417` all trace through `0412`; `TASK-041201` is startable and two tickets — the last two in the chain — are `blocked` on `DEC-077`, **the product owner's**, which asks what the product calls the screen a player opens to reach an account from a browser that does not hold it. `ADR-0076` §1 left the screen count to the story and the answer is **two**; one of the two words was found already merged rather than coined, since `ADR-0050` §3, `ADR-0036` and `ADR-0056` §2 each say *account* to a player | v0.2 |
| [EPIC-05](epics/EPIC-05-ranking-duel-coins-and-leaderboard.md) | Ranking, duel coins and leaderboard | **done** — 4 stories built, 49 tickets; `STORY-0504` and `STORY-0505` dropped by `ADR-0067` and `ADR-0061` §5; 7 decisions answered by `ADR-0061`–`ADR-0067` | v0.3 |
| [EPIC-06](epics/EPIC-06-design-system-and-art.md) | Design system and art | **done** | v0.2 |
| EPIC-07 | Infrastructure and delivery | *not written* — **carries one unfiled ticket, described below**: `player_display_name_unique`'s `COLLATE "und-x-icu"` is gated by nothing | v0.2 |
| EPIC-08 | Analysis and decision quality | *not written* | later |
| EPIC-09 | Bots and simulation | *not written* | later |
| EPIC-10 | The AI software factory — the case study | *not written* | continuous |
| [EPIC-11](epics/EPIC-11-status-notifications.md) | Status notifications — the run reports itself | **in progress** | v0.1 |

Numbers 03–05 and 07–10 are **reserved**, not planned in detail. Epics are written when the one
before them is close to done, because writing them earlier means rewriting them. `EPIC-06` is
the recorded exception: opened ahead of its slot because design shares no file with the server
work (`ADR-0024`).

### `EPIC-07` owes one ticket that could not be filed

**`V3`'s `player_display_name_unique` carries `COLLATE "und-x-icu"` and nothing gates it.** Found on
2026-08-25 while writing `TASK-041635`, which does exactly this job for `V8`'s
`recovery_email_address_unique`, and recorded in that ticket's *Out of scope* because widening its
catalog filter to reach a second table would make an unrelated migration fail a `STORY-0416` test.
It is **not** `STORY-0416`'s and has not been smuggled in.

It is filed nowhere because it can be: `tasks/README.md` requires every task to have a `parent` one
level up, and the only stories that own `V3` — `STORY-0401` and `STORY-0410` — are **done**. So it
is written here instead, in the epic slot a planner must read before writing `EPIC-07`, rather than
into an ADR nobody re-opens.

**Which story should own it: `EPIC-07`'s deployment story, the one that fixes the production
Postgres.** `ADR-0029` §1's whole reason for pinning the clause is host-dependent collation
differences *"between the test container and production deployment"*, and `EPIC-07` is the first
moment a deployment exists to disagree with the container. Until then the assertion is real but the
risk it names is hypothetical.

The ticket is small and its shape is already known. It modifies
`poker-server/src/test/kotlin/duels/poker/server/db/MigrationsTest.kt`, which **already** queries
`pg_indexes` for `player_display_name_unique` and asserts only that it exists. The surface is
`pg_index.indcollation`, `unnest`ed `WITH ORDINALITY` and **`LEFT JOIN`**ed to `pg_collation`,
filtered by the table and never by index name, asserting `collname = "und-x-icu"` **and**
`collprovider = "i"` — `TASK-041635`'s idiom verbatim, including the reasons: an `INNER JOIN`
silently drops non-collatable keys and takes the vacuity guard with it, and
`information_schema.columns.collation_name` is the wrong surface, reporting `NULL` with and without
the clause because the collation is on the index expression rather than the column.

**One thing must be re-probed rather than copied.** `TASK-041635`'s behavioural fixture uses the
`İ` / `i` + U+0307 pair, the one fold that differs between `und-x-icu` and `postgres:16-alpine`'s
musl default. `display_name` carries `player_display_name_nfc` (`CHECK (display_name IS NFC
NORMALIZED)`) and a 1–32 length check, which `recovery_email.address` does not, so that exact pair
may not be insertable there. Probe it before writing the behavioural half; the catalog half needs no
fixture and reddens on any platform.

---

## EPIC-00 — Ways of working

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| **[STORY-0001](stories/STORY-0001-repository-and-ticket-system.md)** Repository, docs, tickets | | | ready |
| | [TASK-000101](tasks/TASK-000101-bootstrap-repository.md) Bootstrap repository and ticket system | M | **done** |
| | [TASK-000102](tasks/TASK-000102-enable-branch-protection.md) Enable branch protection | S | **done** |
| | [TASK-000103](tasks/TASK-000103-token-lean-agent-workflow.md) Token-lean agent workflow | S | **in-review** |
| | [TASK-000104](tasks/TASK-000104-a-second-branch-cannot-claim-the-same-protocol-version.md) A second branch cannot claim the same PROTOCOL_VERSION | S | **done** |
| | [TASK-000105](tasks/TASK-000105-two-build-files-that-were-never-source.md) Two build files that were never source | XS | done |

`TASK-000102` is **done**. The repository went public on 2026-08-13, which made protection and
Actions minutes free at once, and `develop` is now protected: a pull request and two green checks
to land, no force pushes, no deletions. Required approvals are deliberately **0** — one would
deadlock the agent run, since an agent cannot approve its own PR.

`TASK-000105` is **startable**. `poker-server/build.gradle.kts.bak` and `.tmp` are tracked on
`develop` from a stale commit, nothing in the repository references either name, and three separate
coders have now reported them independently — every agent whose ticket names the real build file
pays to work out which of the three it is. It deletes both, adds `*.bak` and `*.tmp` to
`.gitignore`, and names `.claude/settings.json.bak` — a third tracked match, under agent
configuration rather than build output — as deliberately left rather than swept.

`TASK-000104` is **done**, and lives here rather than under any epic because the rule it
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
| **[STORY-0213](stories/STORY-0213-the-wire-carries-a-rematch.md)** The wire carries a rematch — *schema 2* | | | **done** |
| | [TASK-021301](tasks/TASK-021301-the-wire-gains-a-rematch-and-the-version-takes-its-step.md) OfferRematch and RematchOffered reach the wire, and PROTOCOL_VERSION takes its step | S | **done** |
| | [TASK-021302](tasks/TASK-021302-one-offer-reaches-both-seats-and-starts-no-duel.md) One seat's offer puts RematchOffered on both sockets and starts no duel | S | **done** |
| | [TASK-021303](tasks/TASK-021303-the-second-offer-starts-the-duel-with-the-button-moved.md) The second offer starts a fresh duel, with the button on the other seat | S | **done** |
| | [TASK-021304](tasks/TASK-021304-a-repeat-offer-is-answered-and-records-nothing.md) A repeat offer is answered, not refused, and records nothing new | S | **done** |
| | [TASK-021305](tasks/TASK-021305-three-ways-to-hold-no-seat-answer-one-frame.md) Three ways to hold no seat answer one indistinguishable UNKNOWN_ROOM | S | **done** |
| | [TASK-021306](tasks/TASK-021306-rematch-unavailable-is-transient-and-provably-so.md) REMATCH_UNAVAILABLE is transient, and the same offer succeeds afterwards | S | **done** |
| | [TASK-021307](tasks/TASK-021307-a-standing-offer-survives-a-reconnect.md) A standing offer is restated to a returning socket, after its DuelFinished | S | **done** |
| | [TASK-021308](tasks/TASK-021308-the-guest-offers-and-the-frame-names-seat-one.md) The guest offers first, and both frames name seat 1 | XS | **done** |
| **[STORY-0214](stories/STORY-0214-the-wire-names-an-absent-opponent.md)** The wire names an absent opponent — *schema 2* | | | **done** |
| | [TASK-021401](tasks/TASK-021401-disconnect-answers-with-a-room-and-its-frames.md) RoomRegistry.disconnect answers with a room and the frames it produced | S | **done** |
| | [TASK-021402](tasks/TASK-021402-the-wire-names-presence-and-the-version-takes-its-step.md) OpponentPresence and ActedForAbsent reach the wire, and PROTOCOL_VERSION takes its step | S | **done** |
| | [TASK-021403](tasks/TASK-021403-room-presence-of-projects-the-three-states.md) Room.presenceOf projects a seat's presence from state the room already keeps | S | **done** |
| | [TASK-021404](tasks/TASK-021404-a-drop-builds-the-away-frame-for-the-other-seat.md) A drop builds AWAY and the configured window, for the other seat only | S | **done** |
| | [TASK-021405](tasks/TASK-021405-the-away-frame-reaches-the-opponents-socket.md) The AWAY frame reaches the opponent's socket, from inside the NonCancellable block | S | **done** |
| | [TASK-021406](tasks/TASK-021406-an-act-after-the-countdown-would-have-hit-zero.md) An Act sent after the client's countdown would have reached zero is still refused | XS | **done** |
| | [TASK-021407](tasks/TASK-021407-expiry-says-absent-before-the-fold-it-explains.md) Expiry says ABSENT before the fold it explains, and an abandoned room says nothing | S | **done** |
| | [TASK-021408](tasks/TASK-021408-fold-absent-marks-every-action-it-takes.md) foldAbsent marks every action it takes for an absent seat, to both seats | S | **done** |
| | [TASK-021409](tasks/TASK-021409-a-checked-down-absent-seat-is-marked-as-a-check.md) A checked-down absent seat is marked as a check, where a fold is not legal | XS | **done** |
| | [TASK-021410](tasks/TASK-021410-a-resume-tells-both-sides-where-they-stand.md) A resume tells the returning seat where its opponent stands, and the seat that stayed only if it changed | S | **done** |
| | [TASK-021411](tasks/TASK-021411-the-other-seat-drops-and-every-frame-names-the-mirror.md) The host is the seat that goes, and every presence frame names the mirror image | S | **done** |
| | [TASK-021412](tasks/TASK-021412-the-disconnection-kdoc-says-what-outbound-carries.md) Disconnection's KDoc says what outbound carries, not that it is empty | XS | **done** |
| | [TASK-021413](tasks/TASK-021413-a-finished-resume-names-the-other-seat-too.md) A finished room's resume names the other seat, and the test says so | XS | **done** |

**`STORY-0214` is split into eleven tickets and starts on `TASK-021401`.** The chain is linear.
`TASK-021401` is startable today because it is the one ticket needing none of `ADR-0028`'s new
types — `Disconnection(room, outbound)` is built from `Room` and `Addressed`, both of which exist —
so the story moved while `DEC-066` was answered.

**The wire step is thirteen files, and thirteen was measured.** `TASK-021402` was sized by the
[`ADR-0070`](../docs/adr/ADR-0070-a-blast-radius-is-complete-only-when-the-gates-are-green.md) §2
probe: a throwaway stub of every declaration this story adds, run through the commands
`.github/workflows/build.yml` runs — `./gradlew check -PrequireDocker=true`, then `npm ci`,
`npm run check` and `npm run build` in `web-client/` — with the minimal propagation applied at every
path a failure named, and run again. Seven iterations, stopping on **exit 0** with 1285 tests run
and none skipped. Then reverted.

It is deliberately **not** `TASK-021301`'s seventeen. That number is a fact about that ticket:
`STORY-0214` adds no `ProtocolError` value and no `ClientMessage` variant, so
`ServerMessageHandshakeTest`'s golden error list, `TypeScriptDeclarationsTest`'s golden
`ClientMessage` union and `connection.test.ts` are all untouched here. Reusing it would have
over-declared by four and taught the next planner to copy again.

**The probe found what four readings had not, and `ADR-0071` has answered it.**
`ProtocolDiscriminatorTest.everyDiscriminatorIsShortAndUnique` has failed the build on any
discriminator over 16 characters since `TASK-020210`, and `ADR-0028` §1 specified
`@SerialName("ActedForAbsentSeat")` — eighteen. `OpponentPresence` is exactly sixteen and passes.
Shortening the serial name, renaming the type and raising the limit were three different answers
with different consequences on the wire, so it was `DEC-066` and the architect's rather than a
propagation a coder may take.
[`ADR-0071`](../docs/adr/ADR-0071-a-discriminator-is-its-kotlin-type-name.md) renames **the type**:
`ActedForAbsent`, fourteen characters, Kotlin name and `@SerialName` identical — the invariant every
one of the other fourteen subtypes already held, now stated, with a divergence named a defect. The
16-character gate is left **unedited and explicitly unratified**: it has no reason recorded anywhere
and the same JSON already carries the engine's nineteen-character `UncalledBetReturned` inside
`Events`, but *"the name I already wrote is two over"* is the one evidence that must never move a
threshold. `TASK-021402` stays at **thirteen** files — the new name fits, so
`ProtocolDiscriminatorTest.kt` is not a fourteenth row — and is unblocked and `ready`,
`TASK-021401` having landed.

**`STORY-0213` is split and starts on its first ticket.** `TASK-021301` is the wire step — two
message types, one `ProtocolError` value, the version, both documents and both generated client
artifacts — and the split found it is irreducibly **twelve** files against a three-file cap, with no
two of them separable: `ADR-0047`'s ledger test is what forbids a wire shape whose fingerprint no
version row claims, and `ProtocolDocumentationTest` forbids the document moving either before or
after the Kotlin. That was `DEC-063`, and
[`ADR-0068`](../docs/adr/ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md)
answers it: the gates do not move, `files_touched` becomes a true count, and a ticket held together
by a merged gate declares the true count and names the gates in `atomic:`.

**Twelve turned out to be fifteen, which was `DEC-064`.** The coder implemented all twelve declared
files correctly — that work stands at `c904503` — then found three more the change forces: two
exhaustive `when`s over `ServerMessage` in **test** sources (`SocketDuel.kt`, `SocketSecrecyTest.kt`)
and `web-client/src/protocol/connection.test.ts`, whose fixtures run through the client's own version
comparison. It stopped and raised a decision rather than growing the ticket, which is `ADR-0068` §3
working exactly as designed.
[`ADR-0069`](../docs/adr/ADR-0069-the-blast-radius-is-probed-not-remembered.md) answers it: the
ceiling is **deleted** rather than raised, `files_touched` must equal the ticket's own *Files* table,
and a bump is sized by **probing** the gates rather than by remembering a list.

**Fifteen turned out to be seventeen, which was `DEC-065`.** The probe missed two more files on the
same ticket one day later, both merged tests that fail at **execution** rather than compilation:
`ServerMessageHandshakeTest.theErrorSetIsExactlyWhatIsDeclared` (nine hard-coded `ProtocolError`
names) and `TypeScriptDeclarationsTest.aSealedHierarchyIsAUnionOfItsVariants` (the exact
`ClientMessage` union string). `ADR-0069` §3's probe **(b)** runs compile-level commands, and its
edit says *"each sealed hierarchy"* so an enum entry trips nothing.
[`ADR-0070`](../docs/adr/ADR-0070-a-blast-radius-is-complete-only-when-the-gates-are-green.md)
answers it: **one probe, the CI gate set by reference, run in a loop until it exits 0** — a red run
names only a *prefix* of the blast radius, so the completeness of an enumeration is an exit code and
nothing else. `ADR-0069` §2's stop stands with one bounded exception: **a merged gate may complete
the *Files* table**, under four conditions, so a one-line propagation is a row and a quoted failure
message rather than a third `DEC`. The ticket is `ready` at `files_touched: 17`, and seventeen is
written in that ticket and nowhere else. The six tickets behind it are ordinary one- and two-file
tickets and run straight through once it lands.

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
| DEC-077 | **The product owner's** — what does the product call the screen a player opens to reach an account from a browser that does not hold it, and therefore what is that screen's permanent slug? Raised on 2026-08-26 when `STORY-0412` was split. [`ADR-0076`](../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §1 makes a slug *"the lowercase ASCII form of a word the product already says to a player"* and states that the ADR **coins no player-facing vocabulary** — a screen needing a word the product does not yet say is a product question. The story settled the count `ADR-0076` §1 left to it (**two** account screens) and found one name already merged rather than coined: `ADR-0050` §3's confirmation text, `ADR-0036` and `ADR-0056` §2 each say *account* to a player, so `#/account` ships in `TASK-041222` with nothing invented. The second screen has only a **verb** in the merged record — `ADR-0050` §3's *"You stay signed in here"* — and a slug wants a noun, as `duels`, `leaderboard`, `reset` and `verify` each are. An address is player-facing text this product owns **forever** (`ADR-0076` §Consequences) and `ADR-0081` fixed two neighbouring slugs before this story named its screens, so a word chosen inside a ticket reads as settled to everyone who arrives later. **Not the human's**: it adds nothing to and takes nothing from the vision's *What it is* / *What it is not*, costs nothing and moves no roadmap row, and the *Positioning* sentence the product owner already derives from is the input. Blocks `TASK-041226` and `TASK-041227` — the last two tickets in the story — and nothing else | [`STORY-0412`](stories/STORY-0412-the-account-screens.md) | before STORY-0412 closes |
| DEC-060 | **The product owner's** — does a **finished** season ever become reachable from a screen, and how is one chosen? Raised by [`ADR-0061`](../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §7: a finished season is never *gone* — it recomputes exactly from rows nothing rewrites — but v0.3 ships no way to ask for one, so on the first of a month the previous ladder is computable, unreachable, and **nothing records who won it**. A selector is a control on a screen `ADR-0060` already said would crowd; *never* is a complete answer and needs saying out loud. Blocks nothing today | [`ADR-0061`](../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) | before the first season boundary after the ladder ships |

`DEC-076` → [`ADR-0082`](../docs/adr/ADR-0082-a-handle-is-read-from-a-proven-address-never-from-a-player-id.md)
on 2026-08-26 — **a handle is read from a proven address, never from a player id.** Raised and
answered in one PR, because an implementation attempt found the gap rather than a planner:
`RecoveryMailer.sendPasswordReset(address, token, handle)` needs a login handle and **nothing in this
codebase could obtain one from a `PlayerId`** — `issue` answers a `Boolean`, `verifiedOwnerOf` a
`PlayerId?`, `PostgresPlayerDirectory` resolves device ids only, and `Credentials` declares four
members with `verifyCurrent`'s KDoc recording that a reverse lookup was deliberately refused.
**`RecoveryEmails` gains one member and it is keyed by an address**: `resetRecipientOf(address:
EmailAddress): ResetRecipient?`, answering `ResetRecipient(playerId, handle)` in one statement that
joins `recovery_email` to `credential`. **There is no `PlayerId` overload and there must never be
one** — the fence is the argument type rather than the member name, because obtaining a handle
requires already holding a **verified** recovery address, which is the exact secret the endpoint
exists to refuse to disclose. That is `ADR-0031` §5's own test for `verify-email`'s `409` (*"a `409`
is acceptable exactly when the caller already holds the secret it would otherwise disclose"*),
applied to a lookup instead of a status code. The `WHERE` clause is `SELECT_VERIFIED_OWNER_SQL`'s
**character for character**, pinned `und-x-icu` fold included, so two reads of one table can never
disagree about which row an address names; `c.kind = 'password'` matches `REWRITE_CREDENTIAL_SQL`
verbatim; and it is a **`JOIN`, never a `LEFT JOIN`**, so an unknown address, a pending-only address
and a verified address whose owner holds no `password` credential all answer `null` and the third —
unreachable under §3 — is answered rather than handed to the route as a null handle.
**`verifyCurrent`'s refusal is upheld, not overturned**, and becomes a build failure for the first
time: a test asserts **`Credentials` declares no member returning `String` or `String?`**, green
today and reddening on exactly `handleOf(playerId): String?` in the one file anybody would write it.
**`PasswordResets.issue` keeps its `Boolean`** — `ADR-0031` §5's two outcomes, seven merged
assertions and four test doubles untouched. That was the closest call, and it is recorded rather than
skated over: `Issued(handle)` / `Suppressed` is the direction `ClaimPendingResult`'s own KDoc
recommends and the read would have been free inside a transaction that already writes `credential`,
but `issue(playerId, newResetToken())` **is** a `PlayerId → handle` function with a side effect, and
a verified address whose owner holds no password credential leaves `Issued` nothing honest to put in
a non-null `String`. **The read need not share `issue`'s transaction**: verified in source, the only
`UPDATE credential` in this repository sets `secret_hash`, so a handle cannot go stale — the precise
difference from §5's suppression window, which must share a connection because a pre-check on a
separate one is a read-then-write window. **A handle-less owner mints no token**, the route returning
before `issue`, so no row spends a fifteen-minute window for a mail that cannot be sent. Costs
recorded rather than discovered: **a read whose product is a login handle now exists and did not
yesterday**, fenced by an argument type and a KDoc rather than by an impossibility proof;
**`RecoveryEmails` reads a third table**, so both its *"two recovery tables"* charter and its *"no
member returns a `String` that could be one"* sentence are amended in the same commit;
**`ResetRecipient` is a plain `data class`, so `"$recipient"` prints the handle** — deliberately not
redacted, since `ADR-0031` §6.3 protects the *address* and inventing a rule it did not make would
also make every `assertEquals` failure in its tests unreadable, **with the trigger written down**
(the first log line anywhere on the reset path); **two reads of `recovery_email` now carry one
`WHERE` clause in two string constants** and nothing fails if one is edited alone; the join adds a
third caller to an unindexed `credential (player_id)` scan, with the index named as one later ticket
rather than three; and **`kind = 'password'` as a literal goes ambiguous** the day `DEC-027` admits a
second kind carrying an identifier. **The gate is a tripwire, not a proof**, and says so: a handle
read added to another type passes it, and so does one wrapped in a value class, since Kotlin
reflection reports a `@JvmInline` return type as the wrapper rather than as `String`. Chosen partly
because it is the cheapest to unwind — one member and one `data class`, deleted in a single commit —
and the more emphatic answer, a dedicated `ResetMailRecipients` port, stays available at the cost of
moving two declarations; it lost on blast radius, being a new port, a new implementation, a new
`recoveryRoutes` parameter, two wiring edits and **four new stub objects** in the four route-test
files that call `recoveryRoutes` positionally. **No migration, no index, no protocol version, no
`recoveryRoutes` parameter, and `RecoveryMailer` byte-unchanged** — `handle` stays a `String` and its
merged KDoc reason is not disturbed for one call site. **Unblocks `TASK-041626`**, which sources the
handle from the port read, needs a fixture whose verified owner actually holds a `password`
credential (today's `insertPlayer` helpers do not), and gains
`theMailCarriesTheOwnersOwnHandle` over **two** players with **different** handles, so a constant
cannot pass; **`TASK-041630` needs one acceptance criterion widened** from two distinct argument
strings to three, and nothing else. **Raises no `DEC`, and nothing here is the product owner's or the
human's** — `ADR-0031` §1 already settled that the handle belongs in this mail, and §5 already
deferred its wording to `STORY-0412`.

`DEC-075` → [`ADR-0081`](../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md)
on 2026-08-25 — **a mailed link is a fragment route, and the token is the segment behind the slug.**
**Both mailed links become fragment routes on the client's single address**: `RecoveryLinks` returns
`"$baseUrl/#/reset/$token"` and `"$baseUrl/#/verify/$token"`, so recovery works on an object store
serving one file at `/` and **`EPIC-07` needs no rewrite rule**. **The token has not left the
fragment** — it is now the second segment of one, which is *not* a URL path segment and is still
never transmitted, never logged and never in a `Referer`. `ADR-0031` §4's entropy, SHA-256 at rest,
one hour, single use and refusal of a query string stand **byte-unchanged**, and `TASK-041620` is
untouched and uncontradicted: the server still decodes the token from a body and contains no
`queryParameters`. **A recovery link contains no `?` at all** — kept absolute rather than
conditional, which is what decided the close call against `#/reset?token=…`: *a `?` is fine, but only
after the first `#`* is a rule broken by deleting two characters from a string that still looks
ordinary, inside a function no test outside its own file exercises. **The slugs are `reset` and
`verify`, fixed here rather than left to `STORY-0417`.** The register's premise cut the other way,
because these two addresses are **minted by the server into a mail**: a slug one module writes and
another parses is a contract with no shared artifact, and a story picking it later has to reach back
into `RecoveryLinks` to keep the ends equal. Neither word is coined — `ADR-0031` §4 wrote `reset` and
`ADR-0077` §6 wrote `verify` — and `STORY-0417` keeps every other address `ADR-0076` §1 gives it,
including the account screen's and the *forgot password* screen's. **`verify` and `reset` are
answered the same way**: a `404` is deterministic, so the immediate second attempt mails the same
dead link, and a failed verification is what *creates* the total-loss state, since `ADR-0031` §3
makes an unverified address recover nothing while the player believes they have opted in. **A stale
or already-spent link is a screen that renders and a `400` on submission, never a routing outcome** —
the client never inspects the token and must not learn how, because `ADR-0080` deliberately left no
liveness oracle for `password_reset`; a **missing** token is an empty input rather than an unknown
address, so `#/reset` after the replace and after a reload renders the same screen; and the sentence
a player reads is already `STORY-0417`'s under `ADR-0080` and `ADR-0031`, so **nothing here is the
product owner's**. On the client, `screen.ts` gains `tokenFromHash`, matches on the **first fragment
segment**, and the token is read **once at mount** into component state before the address is
replaced with `hashForScreen(screen)` — a screen that re-derives it afterwards finds nothing, which
is `ADR-0076` §5's trap in a second place. Costs recorded rather than discovered: **the two ends
agree by two literals in two modules and nothing mechanical**, since a fragment crosses no wire and
`protocol.gen.ts` cannot carry it, so a divergence lands every mailed link on the lobby silently and
costs the player one of four recovery mails an hour; `#/duels/anything` now renders the record, a
real widening of `ADR-0076` §7; **a reset link opened in a tab already seated is destroyed** by
`ADR-0076` §3's store-outranks-the-address rule, at fifteen minutes' cost, and §3 is not carved out
for it; `/reset` and `/verify` become dead addresses a host with a rewrite rule will happily serve;
and two player-facing words are fixed in a URL before `STORY-0412` names the screens. **Erred toward
the `404` being unacceptable rather than toward the most recent ADR**: `ADR-0076`'s six costs are
legible and survivable, while a `404` here is silent, deterministic, invisible to every test in this
repository — Vite serves `index.html` for unknown paths — and permanent. **Blocked nothing**, and it
was a one-function change exactly as `ADR-0077` §6 promised. **`TASK-041633` changes in two string
literals and its `DEC-075` note**; its other four tests, its no-`?` criterion, its no-encoder refusal
and its `Host`-header sweep survive verbatim. `TASK-041632` and `TASK-041620` are unchanged.

`DEC-074` → [`ADR-0080`](../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md)
on 2026-08-25 — **the password is judged before the token is touched, so a refusal costs no link.**
**`ADR-0031` §5's precondition gives way; §4 stands byte-unchanged.** The handler runs three steps
and no others: decode ⇒ `400`; `passwordIsLongEnough` **and** `passwordIsWithinTheWorkBound` ⇒
`422`, with **no connection taken and no statement executed**; then `consume` ⇒ `204` / `400`. So a
`422` never touches the row — same `token_hash`, same `issued_at`, same `expires_at` — the same link
works on the next submission while it lives, and §5's fifteen-minute suppression still sees it, so a
`forgot-password` pressed in frustration stays the complete no-op §5 designed. The order is not new:
`ADR-0048` §2 already puts the maximum *"before Argon2 runs and before the identifier is looked
up"*, and here the token **is** the identifier. The register's disclosure worry runs the other way —
the branch is chosen entirely by the caller's own password, so the `422` is byte-identical for a
live token, an expired one and a string the caller invented, and `400`-versus-`422` reports
**nothing** about `password_reset`; every other order makes it a liveness report. The endpoint stays
unbudgeted and its cheapest refusal gets cheaper; a token probe still costs one `DELETE … RETURNING`
and is still spent by the finding. **Costs**: a `422` no longer proves the link is alive and nothing
else does either, so a player can be refused twice for one attempt and `STORY-0417`'s form must move
from *password refused* to *link expired* without contradicting itself; a stranger holding no token
can make the endpoint answer `422`, which is licensed only while the policy stays a published pure
function — **the day a breach corpus or any row-reading rule joins it, this endpoint must be
budgeted or the rule moved behind the lookup**; and `ADR-0031` §5 now reads wrong on its own.
**`TASK-041620` is unchanged and needs no re-cut** — the step lands in front of `consume` — with one
fixture constraint: every request in `ResetPasswordRouteTest`, including the two expecting `400`,
must carry a `newPassword` of 8–128 code points. **`TASK-041629` gains the check and loses one named
test**, `aBadTokenStillAnswersFourHundredNotFourHundredAndTwentyTwo`, whose order is reversed; what
it defended is asserted the other way — a fabricated token and a live one produce indistinguishable
`422`s — and `aRefusedPasswordLeavesTheTokenAsTheDecisionSays` resolves to a second request
answering `204`. `TASK-041617` transcribes the corrected sentence rather than §5's. **Unblocks
`TASK-041629`.** Raises no `DEC`, and nothing is the product owner's or the human's.

`DEC-073` → [`ADR-0079`](../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md)
on 2026-08-25 — **five to attach, ten to forget, and the attach budget is the only cap on the mail
it causes.** **The four numbers: `forgot-password` admits `10` per remote address per rolling
`60000` ms; `recovery-email` admits `5` per `60000` ms.** Four `ServerConfig` values in the pattern
the existing two pairs use (`AUTH_FORGOT_PASSWORD_MAX_ATTEMPTS`,
`AUTH_FORGOT_PASSWORD_WINDOW_MILLIS`, `AUTH_RECOVERY_EMAIL_MAX_ATTEMPTS`,
`AUTH_RECOVERY_EMAIL_WINDOW_MILLIS`), a `forgotPasswordLimits()`
and a `recoveryEmailLimits()`, and **a separate `AttemptBudget` instance for each endpoint** —
`ADR-0074` §1's reason applied verbatim, since one shared instance lets either endpoint spend the
other's budget. **The register's premise was checked before it was used, and it cut the other way.**
It is true that `ADR-0074`'s argument does not transfer, because over budget here is answered `202`
and a refused player is told nothing; but invisible collateral is a reason to be *generous*, not
tight — a limiter nobody can perceive is one nobody can work around. **What decided the numbers is
that `ADR-0031` §5's fifteen-minute rule covers one of the two mail paths, not both.** Its sentence
reads naturally as covering both and the split built it on one: `TASK-041613` puts the check inside
`PasswordResets.issue`, while `TASK-041607` fixes `RecoveryEmails.claimPending` as returning `Unit`
and `TASK-041608` has it `DELETE`-then-`INSERT` unconditionally — so **a verification mail goes out
on every successful attach, for ever**. That asymmetry is the whole decision. **On
`forgot-password` the address budget adds nothing** against the attack it is most often accused of
enabling: the durable rule caps one account at four recovery mails an hour across *every* source
address at once, which is exactly what an address key cannot do. Its one unique job is an aggregate
cap across distinct victims, and that is bought back by a second source address and is hard to aim,
since causing a single mail needs an address already *verified* here and nothing in this system
discloses which those are — §5's `202` conflates five cases, §6.3 keeps the address out of every
response, message and log, and `verifiedOwnerOf` returns an id. So it is set where it cannot bite:
the player it would refuse has lost their password, is told mail is on its way, and `ADR-0031`'s
Consequences make no recovery a **total, permanent** loss of the account. Ten per sixty seconds is
this repository's pair for an aggregating key on a repeated action (`ADR-0022` §2, `ADR-0074` §1).
**On `recovery-email` the budget is the only cap on outbound verification mail, and the caller
chooses the recipient** — unbudgeted, one account and one script is a spam relay with a
`RecoveryMailer` in front of it, and the cost is not the bill (§7 defers that to `EPIC-07`) but a
sender domain that stops being delivered, which breaks recovery for everyone who opted in. It is
also a **second door to the current-password guess**: §3 makes the password what stands between a
minute at an unattended browser and permanent ownership of an account, and `ADR-0074` priced that
guess at ten a minute at the front door. Five, with no refund, keeps the front door the cheaper one
by a factor of two, halves the mail rate to 300 an hour, and is beyond what a once-per-account setup
act uses. `ADR-0074`'s objection to five — *"a per-person number applied to a group"* — is answered
rather than ignored: it holds at sign-in, which is what a whole café does on arrival, and not for
recovery setup. **Sixty seconds on both**, because the window is the recovery time and `ADR-0074`'s
burst-becomes-a-lockout interaction is worse where the lockout is invisible; it also holds
`AttemptBudget`'s per-key list at a fifteenth of what sign-up's window already accepts. **An
over-budget attempt still counts** — one rule for every limiter here, no fork of a shared type whose
KDoc warns against exactly that "simplification" — and here it is also the rule that *works*: `202`
gives a sprayer no feedback and no reason to pace, so counting caps a hammerer at one window's worth
in total, where not counting would hand the same sprayer one window's worth every minute for ever.
**Placement was the half of the question that is not a number, and it differs per endpoint.**
`recovery-email` admits after the `401`, the decode and `ADR-0078`'s syntax `400`, and **before the
Argon2 verify** — a check after the hash bounds no pool, and a check before identity would let
unauthenticated traffic spend a signed-in player's budget. `forgot-password` admits **after the
`202` is written**, the only budget in this system consulted after its response, because `admit`
takes a `Mutex` and `TASK-041626` makes the write-first ordering the timing defence rather than an
optimisation. **Neither endpoint calls `refund`**, and neither can answer `429`. **The key is §5's
and no part of it is the architect's**: `origin.remoteAddress` alone, and no `X-Forwarded-*` until
`EPIC-07` installs the plugin. Costs recorded rather than discovered: **an attacker can switch off
password recovery for everybody behind one address, silently and for as long as they care to pay ten
requests a minute, while every one of them is told the product is sending mail** — the price of an
address key on an endpoint whose refusal is invisible, chosen because the only key that would bound
it is the submitted address, which hands a stranger a switch over one *named* player's recovery;
five on an aggregating key means a classroom setting recovery up together waits a minute; `202` on
the attach path now means one more thing, beside `ADR-0078`'s already-recorded *a player can believe
recovery is on when it is not*; 300 mails an hour from one source is still a relay, just a slower
one; the per-key list still grows with an attacker's rate times the window; and an operator now
holds four limiters and eight numbers, with nothing anywhere reporting that a budget refused
something. **One residual, named and conditioned so it cannot evaporate: the attach path has no
per-account resend suppression.** On the best reading §5 already requires one and the split
under-built it, so it is a defect against `TASK-041607`, `TASK-041608` and `TASK-041625` rather than
a new question — the mechanism, the number and the column all already exist. **A ticket for the
planner, due before `EPIC-07` configures a sender**, which is the same condition that binds the
budgets themselves. **Unblocks `TASK-041628`. Raises no `DEC`, and nothing here was the product
owner's or the human's**: no clause touches what a player is told (§5 fixed that), none needs a paid
service, and nothing is sent under any answer until a transport exists.

`DEC-071` → [`ADR-0078`](../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md)
on 2026-08-25 — **the mail is the only real check on an address, so the syntax rule refuses almost
nothing.** Derived from `docs/vision.md`'s *"One duel coin per win… a counter of duels won"*, which
fixes the proportion — nothing here is worth a corpus, a blocklist, a network call or a bill — and
its *Positioning* sentence, which decides what fills the gap: the same pair `ADR-0048` used one
decision earlier, on the neighbouring question. **The asymmetry was resolved on `ADR-0031`'s own
text rather than on taste.** §3 requires verification before an address can do anything and §2 notes
that *"a player whose address is stored has, by construction, received mail at it"* — so the system
already holds an exact deliverability check, and a syntax rule can only report earlier and can only
be wrong in the direction that costs an account. **The rule is four clauses**: at least one `@`; the
first code point is not `@`; the last is not `@`; no ASCII control character (`U+0000`–`U+001F`,
`U+007F`); at most **254 code points**, RFC 5321's path limit in the unit `ADR-0029` §2 and
`ADR-0048` §1 already fixed. There is no separate minimum — clauses one to three make `a@b` the
shortest thing that passes. **Clause four is not a syntax rule and is labelled as such**: no
`addr-spec` holds a control character in any position, so it denies no mailbox that exists, and it is
there because a line terminator inside an address is the one thing this predicate could hand
`EPIC-07`'s unwritten transport that would harm somebody who is not a player of this game. **The rule
runs where an address enters and nowhere else**: `POST /api/auth/recovery-email` only, with
`forgot-password` keeping its unconditional `202` and consulting no predicate — `ADR-0048` §2's
*never on the lookup path* one endpoint pair over, and the single property that keeps a future
tightening free, since a stored address is never re-judged. **Nothing is canonicalised.**
`emailAddressOrNull` returns the input unchanged, departing from `ADR-0048` §5 and `ADR-0029` §2 on
purpose: those strings are *compared*, an address is a **delivery target**, and §2 fixed the stored
form as what the player typed *"because that is what must be delivered to."* **Deliberately not
checked, each written as a decision**: deliverability in any form, DNS and MX, whether the domain is
spelled correctly, disposable-address lists, role addresses, plus-address stripping, unicode
conversion, a dot in the domain, quoting and domain literals, and any whitespace that is not a
control character. **The refusal is `400` with an empty body**, byte-identical to a failed decode,
and the client says one sentence that names no mailbox, no domain and no other account; the only
constraint placed on the silent path is that **a `202` may not be rendered as recovery being on**,
since §3 leaves `hasRecoveryEmail` false until the link is followed. Both fixture tables are in §6,
each carrying the entry that distinguishes this answer from the first regex anyone would write.
Costs recorded rather than discovered: **the endpoint's only feedback now fires almost never**, so in
practice only a bare handle and an empty field produce a `400` and every other mistake is answered
`202` and silence; **`Bob Smith <bob@example.com>`, a trailing space and a trailing newline are all
accepted** and all undeliverable; a player can believe recovery is on when it is not, mitigated only
by a flag they have to look at; the predicate is **never an invariant over `recovery_email`**, which
is exactly the price of the reversibility; plus-addressing and normalisation variants let **two rows
be one mailbox**, which `ADR-0063` already tolerates until the ladder is public; and a permissive
rule hands `EPIC-07` a measurably higher bounce rate, which is deliverability risk outside the
software and stays where `ADR-0031` §7 put it. **`TASK-041601`'s parked catalog assertion now has its
condition met** — *"if the answer admits non-ASCII, this becomes a ticket"*, and it does — so a
ticket is owed, and it is the planner's to cut. **Unblocks `TASK-041624` and `TASK-041625`.**
**Nothing here was the human's**: no clause needs a paid service, the one alternative that would is
refused on that ground and named as the human's if anybody wants it, and nothing is sent to any
address under any answer. Raises no `DEC`.

`DEC-072` → [`ADR-0077`](../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md)
on 2026-08-25 — **no sender is an implementation, detachment is a decorator, and a test binds
neither.** **No sender configured is `NoRecoveryMailer`, a `public object` with two empty bodies —
never a null**, so no route branches on configuration and the property `TASK-041627` exists to prove
belongs to the *type* rather than to three handlers each remembering the same `if`. **Detachment is
`DetachedRecoveryMailer(delegate, scope, log)`, a decorator over the same port**, so no route file
holds a `CoroutineScope`, a `launch` or a `Job`; the two compose outermost-first, and
`TASK-041606`'s shape test is untouched because it asserts over the interface and never over an
implementation. **The delivery scope is a supervisor child of the application's job**, built in
`duelServer` beside `scheduleSweeps` — `ADR-0025`'s argument applied unchanged, so stopping the
server cancels every in-flight send, one failure cancels no sibling, and it is deliberately *not* the
application scope itself, which also carries a ticker that never completes and so can never be
quiesced. **The server may exit with mail pending**: pending mail is cancelled, not drained, because
a drain timeout is a number about a transport nobody has measured, and adding one later is one
`withTimeoutOrNull(join)`. **A failed send is logged once and nothing else happens** — the member
name and `failure::class.simpleName`, with **no message and no stack trace**, because a transport's
own exception is the likeliest place a recipient address ever reaches a log and `ADR-0031` §6.3
admits no exception; **no `player_id`**, since §6.2 deliberately gives the port none and §6.4's
ceiling is not a floor; and **no success line**, which would be the delivery log §6.4 warns about.
**Nothing above the port is retried and no row is compensated.** Retry policy is transport-shaped
and stays `EPIC-07`'s *behind* the port; a compensating delete would destroy live tokens for mail
that actually arrived, since a relay that times out its acknowledgement throws what a relay that
never delivered throws — and §5 refused to invalidate an outstanding token precisely so a
double-click cannot destroy the link the player is about to use. **`baseUrl` is a `ServerConfig`
field** (`server.baseUrl`, `BASE_URL`, defaulting to the Vite dev origin): **absent is the default,
present-but-malformed refuses to start** like every other field, and **`RecoveryLinks` is the only
place either URL is constructed**. **What a test can await — the load-bearing clause: the test binds
an undecorated recording double**, so the send is an ordinary suspend call inside the handler and
both `assertEquals(1, mailer.sent.size)` and `assertEquals(emptyList(), mailer.sent)` are decidable
with no join, no channel and no timeout. **Absence is what forced the shape**: no await proves a
negative, four criteria in this story assert one, and the only implementation of *nothing was sent*
against a live scope is a timeout, which is a sleep dressed as an assertion. The Ktor test-engine
mechanism was **measured against this repository, not assumed**. **No test asserts about a mail
through `duelServer`**; the decorator is unit-tested on its own; and **§5's `202`-before-the-send
ordering is not gated and cannot be** — it stays a review criterion, exactly as `TASK-041626`'s
Proof step 3 already predicted. Costs recorded rather than discovered: **a lost reset mail costs the
player fifteen minutes of silence they cannot distinguish from anything else**, the direct price of
no retry, since the row is committed with `issued_at = now` before the send can fail and §5
suppresses the retry (the *verification* path does not pay it — a second claim replaces the first at
once); the failure log names a class and nothing else, so a broken relay must be reproduced outside
the server; **nothing in `STORY-0416` exercises the real composition**, which `EPIC-07` will be first
to run; three new files whose combined behaviour is to do nothing; and **`TASK-041627` is now bigger
than its Files table**, which is the planner's to re-cut. **Chose no transport, and named the one
clause that could not be answered provider-independently** — the retry policy — leaving it to
`EPIC-07` rather than guessing it. **Unblocks `TASK-041625`, `TASK-041626` and `TASK-041627`. Raises
`DEC-075`, answered above by `ADR-0081`.**

`DEC-054` → [`ADR-0076`](../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md)
on 2026-08-25 — **a screen the player chose has an address; a screen the server gave has none.**
**Yes, the client gets addresses**, and an address is a URL **fragment**: `/` for the first screen,
`/#/duels`, `/#/leaderboard`, and one slug per screen for `STORY-0412` — however many screens that
story turns out to have, which is the story's call. A slug is the lowercase ASCII form of a word the
product **already says to a player**, written as a literal rather than derived from `HISTORY_HEADING`
at runtime, so a restyled heading in `EPIC-06` cannot break a link and this ADR coins no player-facing
vocabulary. **The waiting screen, the duel table and the result screen get no address, ever**: all
three are chosen by frames — `RoomJoined`, the first `Snapshot`, `DuelFinished` — so an address
claiming a seat would be a client asserting a game fact, and false the moment `RoomRegistry` reaps
the room; a reload while seated lands on `/` and `ADR-0072`'s memory plus the frames put the player
back. **The store outranks the address**: `Lobby.tsx` keeps its branch order, a player on `#/duels`
whom a frame seats is shown the duel (`ADR-0060` §5), and the fragment is replaced with `/` so the
address never lies. **The fragment beats a path segment on a sentence already merged in
`room-link.ts`** — *"a path segment would 404 on reload against a static host with no rewrite rule,
and `EPIC-07` has not chosen one"* — and `EPIC-07` still has **no file in `tasks/epics/`**; it beats
a query parameter because a query is sent to every host's access log and would ride along in the
invite link a host copies out of the waiting screen. `#/duels` rather than `#duels`, because a bare
fragment is an element identifier the browser hunts for and scrolls to. **What carries it is two
owned files and no new dependency**: a pure, framework-free `screen.ts` and a `use-screen.ts` over
`useSyncExternalStore` — the primitive `ADR-0032` §3 already chose — with one trap named because it
is silent: `pushState` and `replaceState` fire **neither** `popstate` nor `hashchange`, so a push is
an assignment to `location.hash` and a replace notifies the module's own subscribers. ***Back* is
defined at every boundary**: opening a chosen screen pushes, so browser *Back* returns to the first
screen **in the same document** — no reload, no second socket, the store untouched; `ADR-0060` §4's
in-page control **replaces**, so a lobby↔record ping-pong cannot grow the stack; *Back* on the first
screen or on an address the player typed leaves the client, which is what *Back* means everywhere;
the duel screens pushed nothing, so nothing there changes, and **no `beforeunload` and no
confirmation** is added — `ADR-0073` §4 refused one on a comparable path and whether one is ever
offered stays the product owner's. **`DuelResult`'s `<a href="/">` and the waiting screen's *Back to
the lobby* stay real page loads**, because they are store boundaries rather than screen boundaries:
routing them client-side would ship `ADR-0075`'s four-field presence leak in the same change that
fixed *Back*, so that hole stays unreachable and its ticket stays exactly where `ADR-0075` left it.
**There is no address a player is not entitled to see**: an address selects a screen and grants
nothing, it is not a capability — `#/duels` opened in another browser shows *that* browser's record,
because the read is authenticated by what that browser holds — nothing in the space is gated
(`ADR-0036`, `ADR-0060` §3), and an unknown or renamed fragment renders the first screen with no
error, because a fragment is not a request and there is nothing to refuse. Costs recorded rather than
discovered: the destination now has **two spellings**, undoing the tidiness `ADR-0060` §2 bought with
a single constant; **the screen survives a reload and the state inside it does not** — `#/duels`
lands on page one under no filter, since `ADR-0057` forecloses a cursor in a link, so the address
quietly promises more than it delivers; **two navigation authorities** held apart by prose and a
branch order, with nothing mechanical to catch the next plausible-looking address for a server-owned
state; a **second hand-rolled `useSyncExternalStore` source**, taking `ADR-0032`'s owned-lines trade
twice; the in-page and the browser *Back* **coexist and are not the same operation**, so `ADR-0073`
§5's collision is defined rather than removed; and **every screen story from here owes an address**,
`STORY-0412` first. Forecloses an address for anything the server decides — permanently, and **by
rule rather than by omission** — along with server-side rendering, which `ADR-0026` never had. Chosen
partly because it is the cheapest thing here to unwind: two files and two `useState` flags, with no
host, no build, no rewrite rule and no dependency to undo. No Kotlin, no frame, no protocol step, no
deployment requirement. **Unblocks `STORY-0412`'s split.** Raises no `DEC`.

`DEC-070` → [`ADR-0075`](../docs/adr/ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md)
on 2026-08-24 — **the server's mark lives as long as the absence that produced it.** Derived from
`docs/vision.md`'s *Positioning* sentence — the same one `ADR-0046` derived its string set from —
and from `ADR-0046` §2's already-shipped rule that a presence line clears on a frame and *never on a
timer, never on a fade*. **Two of the four candidates died on the server's own code, not on taste.**
`AbsentSeats.kt` returns `listOf(Addressed(0, mark), Addressed(1, mark)) + next.outbound`, and `act`
composes that outbound through `framesFor` = `broadcast + turnFor`, where `broadcast` carries the
comment *"Always emit Snapshot frame"* and does exactly that, for every applied action, to **both**
seats. So the mark and the `Snapshot` describing the mark's own action are **consecutive frames in
one delivery** — *clears on the next `Snapshot`*, and *clears on the next `YourTurn`*, which rides
the same delivery, would each clear the mark microseconds after it was set, and `ADR-0046` §4 would
be implemented, tested and never read by a human being. `ADR-0043` §3's precedent does not transfer:
neither `rejection` nor `Your rival is back.` arrives welded to a `Snapshot`. **The mark is a
status, not a notice** — `foldAbsent` gives up every turn that reaches an absent seat, so during an
absence the mark replaces itself at nearly every decision point and can only go stale once the
server stops acting. **Exactly two frames take it off**: an `OpponentPresence` carrying `PRESENT`,
and `DuelFinished`. Two keys in two case bodies of `duel-state.ts`; every other frame —
`Snapshot`, `Events`, `YourTurn`, `Rejected`, `RematchOffered`, `Failure`, `RoomJoined` — leaves it
exactly as it was, and there is **no timer, no fade and no dismiss control**. It clears on the
**frame**, not on a transition, so unlike `rivalReturned` it carries no bookkeeping: a `PRESENT` at
a resuming client whose rival never left still clears it, and that is right, because the mark is
about whether the server is *acting* — a state — while a return is a transition. **The words do not
change**: `ADR-0046` §4's six sentences stand and no seventh string is added. The failure `DEC-070`
was raised on becomes impossible by construction rather than unlikely — the single frame that puts
*Your rival is back.* on screen is the same frame that takes the mark off, in the same reducer call.
Costs recorded rather than discovered: **a mark can be older than the hand on screen** — the rival
is absent, the present player is on the button and folds pre-flop, and the turn never reaches the
absent seat, so a line from three hands ago sits under the current one; it stays true, it stays
under a presence line that says the server is still acting, and nothing ties it to its hand. That is
the price of §4's *no action log* and it **is the cost being chosen**. **A mark naming this client's
own seat has no clearing frame but `DuelFinished`**, because `OpponentPresence` is
recipient-relative and the wire has no *you are present again*; a protocol step was deliberately not
requested for a race whose whole window is one delivery. **`serverAction` clears on `DuelFinished`
as a boundary guard**, not because that frame says anything about absence, so that no mark survives
into a rematch without resting on a three-link argument about `PRESENT` always arriving first. And
**the store still has no room boundary** — `rivalPresence`, `graceRemainingMillis` and
`rivalReturned` are cleared by nothing at a duel or room boundary, unreachable today only because
`DuelResult.tsx`'s way back is an `<a href="/">` that rebuilds `initialState()`; the day `DEC-054`
replaces it with a route, that is one ticket and not this decision's. Forecloses the *shape* rather
than the wording: if an action log ever ships, the mark attaches to its line by `(handNumber,
actionSequence)` and takes that line's lifetime, superseding this ADR — which is why the whole frame
stays in the store. **Unblocks `TASK-031314` and `TASK-031315`**, both now `backlog` behind
`TASK-031313` like the other twelve, with the clearing rule and four store tests written into
`TASK-031314` and one screen test into `TASK-031315`. No Kotlin, no frame, no protocol step, no
stored data. Raises no `DEC`.

`DEC-069` → [`ADR-0074`](../docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md)
on 2026-08-24 — **sign-in is ten wrong passwords a minute per address, reserved before the hash and
refunded when right.** The numbers are `auth.signInMaxAttempts` / `AUTH_SIGN_IN_MAX_ATTEMPTS` = `10`
and `auth.signInWindowMillis` / `AUTH_SIGN_IN_WINDOW_MILLIS` = `60000`, on a **second**
`AttemptBudget` instance so sign-up cannot spend sign-in's budget. They were chosen on an asymmetry
the arithmetic makes plain: against a guesser the plausible range is nearly **insensitive** — no
number saves `ADR-0048`'s accepted `password`, and every number puts a ten-million corpus years out
of reach of one address — while the **collateral is not**, so the pair is `ADR-0022` §2's ten per
rolling sixty seconds rather than sign-up's five per fifteen minutes. The second half was the real
decision: `ADR-0027` §6 meters *failures* but the pool is only protected if the check sits *before*
the hash, and one `admit` call cannot do both. Peek-then-record was rejected outright — concurrent
requests from one address all peek `true` and all hash, which is an unbounded burst per window
through the obvious implementation. So `AttemptBudget` gains **one method, `refund`**, sign-in
reserves at step 2 and refunds only on success, and the reservation also caps how many verifications
one address can have **in flight**. An over-budget attempt **still counts**, keeping one rule across
all three limiters; the sixty-second window — not a forked type — is what stops that compounding, and
the ADR says plainly that an exhausted address clears sixty seconds after its last *attempt*, not its
last *failure*. Over budget answers exactly as a wrong password does: no `429`, no `Retry-After`, no
wire change, and `ADR-0056` §1's ban on a client-side throttled state here stands. Cost named rather
than buried: **eleven wrong passwords from one address inside a minute lock everyone behind it out of
their own accounts, told only that their password is wrong** — chosen over the alternative key, which
locks the *account*; **no account is ever locked by this design**, which is why nothing here is the
product owner's. Unblocks `TASK-040523` — no decision holds it now, and it is `backlog` behind
`TASK-040522` like the rest of the chain, at five files with `atomic:`. `TASK-040519` gains
`refund` and `TASK-040520` gains sign-in's config pair; both are unstarted. Raises no `DEC`.

`DEC-068` → [`ADR-0073`](../docs/adr/ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md)
on 2026-08-23 — **the waiting screen says *Back to the lobby*, and the room stays open.** Derived
from `docs/vision.md`'s *Positioning* sentence — the same one `ADR-0046` derived its string set from
— and from `ADR-0022`, which already gives a `WAITING` room one way to end and no other. **The
screen gains a way out**, calling `ADR-0072` §4's `forgetRoom()` from an event handler. **The control
reads `Back to the lobby`, byte-identical to what `DuelResult.tsx` already renders** for the same
action on the same memory: one phrase for one action, no new vocabulary. **It does nothing to the
room** — the room stays `WAITING`, the host keeps seat 0, the code keeps resolving, and the idle
timeout is still the only thing that ends it — and **exactly one line says so**: *The room stays
open. That link still works for your rival, and it brings you back.* Every clause was checked in
source: `Room.join`'s `WAITING` branch seats a rival, and the host's own follow of the link is
`ALREADY_SEATED` answered as `RoomJoined(code, seat)`. **The line names no duration**, because the
client owns no clock against a server window (`ADR-0072` §6); the already-shipped *No duel room has
that code.* is the correction once the room is reaped. **No confirmation of any kind** — the action
destroys nothing, and a dialog would assert that it does. **Two strings are the whole addition**,
and a third needs a new ADR, which is what makes `STORY-0314`'s *the words are the ADR's* criterion
enforceable. Refused by name: *Cancel* / *Close the room* (claims the room is gone; `Room.join` will
still seat a rival at that code), *Leave* (claims the seat is vacated; `ADR-0072` §4 refused
`leaveRoom` for a function nobody sees), *Back* alone (the lobby's two panel swaps say it and change
nothing), *forfeit* / *sit out*, and any duration. **`design/screens/create-duel.html`'s waiting
frame gains both strings verbatim**, as `EPIC-06`'s work; `STORY-0314` does not wait on it. Costs:
**a host pulled into a duel after pressing this has a store that never saw `RoomJoined`** — `deliver`
addresses by player id, so the lobby tab gets the opening `Snapshot` and renders the table, but
`duel-state.ts` sets `mySeat` and `roomCode` only on `RoomJoined`, so the result screen loses its
seat and a reload does not rejoin; **a rival opening the link after the host walked away gets a duel
with an absent seat**, and the host can lose a coin without seeing a card — both predate this
control, which only makes the route one press. Forecloses one thing on purpose: **this control never
quietly becomes a room-closing control** — an invite that dies with its host is a frame, Kotlin in
`EPIC-02` and a protocol step, and it must arrive with different words. **Unblocks `STORY-0314`**
and nothing else. Raises no `DEC`.

`DEC-067` → [`ADR-0072`](../docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md)
on 2026-08-23 — **a tab remembers its room until the player leaves it, and the way back is what
forgets.** The client half moves: `boot.ts`'s `DuelFinished` branch is **deleted**, `pd.roomCode`
means *the room this tab is seated in* and never *the screen this tab should show*, and exactly two
things clear it — the player leaving, and `Failure(UNKNOWN_ROOM)` answering this tab's **own**
rejoin, `TASK-031010`'s `rejoining` guard kept verbatim. The asymmetry was **verified, not
inferred**: `boot.test.ts`'s merged, green *sends no JoinRoom on the Welcome after a duel has
finished* is a standing assertion that no socket this tab opens ever rejoins after a finish, so
`DuelSocket`'s restatement loop runs for nobody. `DuelClient` gains `forgetRoom()` beside `store`
and `send`, `DuelProvider` an optional third prop, `useForgetRoom()` beside `useSend()`, and
`ADR-0032` §3's *event handlers only* extends to it. The way back stays an `<a href="/">` and gains
one `onClick` — `removeItem` is synchronous — with `DuelResult` still a function of its props. A
stale code strands nobody: a boot holding a reaped code is refused, forgets, and lands at the lobby
with *No duel room has that code.* **Blocks and unblocks no ticket**; `STORY-0309` may not be called
done without the transport half it now decides. Names one gap it does not close — the *waiting for
your rival* screen still has no way out — and raises no `DEC`.

`DEC-063` → [`ADR-0068`](../docs/adr/ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md) on 2026-08-23 (the gates that make a `PROTOCOL_VERSION` bump atomic do not move; `files_touched` becomes a true count, and a ticket held together by a merged gate declares up to twelve files and names its gates in `atomic:`. `ADR-0047` §6's *"five artifacts"* is replaced by a procedure rather than another number). **Unblocks `TASK-021301`, `STORY-0213`, `STORY-0214` and `STORY-0405`.**

`DEC-065` → [`ADR-0070`](../docs/adr/ADR-0070-a-blast-radius-is-complete-only-when-the-gates-are-green.md)
on 2026-08-23 — **registered and answered in the same PR**, because an implementation attempt raised
it rather than a planner, for the second time on one ticket. `ADR-0069` §3's probe missed two more
files, both merged tests that fail at **execution**: `ServerMessageHandshakeTest` hard-codes nine
`ProtocolError` names, `TypeScriptDeclarationsTest` hard-codes the exact `ClientMessage` union
string, and probe **(b)** runs `compileTestKotlin` and `tsc` while §3's own justification sentence
claims full checks. **The obvious repair — make (b) full — would still have missed both**: the
probe's `ServerMessage` variant breaks `DuelSocket.kt` in *main* sources, so `check` dies at
`compileKotlin` and never reaches `test`, and **a red run names only a prefix of the blast radius**.
That is what happened in round one, where side **(a)** was already full. Four procedures have now
failed the same way — a list of four files, of five, of twelve, and now of commands and of language
constructs — and **every one was a copy**, which drifts from what it copies. So: **one probe**, all
stub edits applied together; **its commands are `.github/workflows/build.yml`'s, by reference and
verbatim**, so a check added to CI joins every probe with no ADR edited and no document may publish
a narrower one; **the probe is a loop that ends on `exit 0`**, because **there is no prefix of
green**; and the stub edit is the story's **declared surface** — every declaration added, removed,
renamed or re-valued, an enum entry included. `ADR-0069` §2's stop stands with **one bounded
exception**: a coder may add a *Files* row and continue when a merged gate fails naming the path,
the ticket's own authorised edits are what make it fail, the edit is **propagation not decision**
(no behaviour, no new test, **no assertion weakened or derived away**), and the full gate set then
exits 0. Costs: the probe becomes the most expensive step in planning; the coder gains a unilateral
edit of its own contract whose third condition is judgement; `files_touched` becomes
as-implemented; a probe without Docker is quietly narrower than CI; and four documents now answer
one question in three days. **Unblocks `TASK-021301` at `files_touched: 17`**, and with it
`STORY-0213`, `STORY-0214` and `STORY-0405`. `lint_tickets.py` is deliberately untouched.

`DEC-064` → [`ADR-0069`](../docs/adr/ADR-0069-the-blast-radius-is-probed-not-remembered.md)
on 2026-08-23 — **registered and answered in the same PR**, because an implementation attempt raised
it rather than a planner. `ADR-0068`'s own tripwire fired on the first ticket it governed: the coder
implemented all twelve declared files correctly, found three more the change forces
(`SocketDuel.kt` and `SocketSecrecyTest.kt` — exhaustive `when`s over `ServerMessage` in **test**
sources — and `web-client/src/protocol/connection.test.ts`), stopped rather than deciding, and cited
§5. **The blast radius is probed, not remembered, and a ticket's size is its own *Files* table.**
The **ceiling is deleted, not raised**: the set is monotone in a gate count nobody controls — four,
then five, then twelve, now fifteen — and `ADR-0068` §5 said *"whatever replaces the five must not
be another number"* while its own §3 wrote twelve into the linter, which is the contradiction that
stalled. In its place, on a ticket declaring `atomic:`, **`files_touched` must equal the edit-row
count of that ticket's own *Files* table** (≥ 4), which is `ADR-0068` §7's unbuilt residual, built,
and scoped to `atomic:` tickets so the nine merged under-declaring tickets stay green. **A file the
*Files* table does not name stops the ticket, at any count** — the rule that actually fired.
`ADR-0068` §5's enumeration was a *verification* procedure a planner could not run, so its twelve
came from reading; it becomes a **probe** — a throwaway one-line change made only to be read and
reverted — which finds test sources because the compiler and `vitest` do not know which directory a
file is in. **And a version literal in a fixture references the constant**: exactly one test per
side pins the number (`ProtocolJsonTest`, and the client's derived `version.test.ts`), a fixture
needing a *different* version writes `PROTOCOL_VERSION ± 1`, and everything else reads the constant.
Found while checking: **five client fixtures carry `protocolVersion: 2`, survive the bump silently
and will assert a version that does not exist** — `Welcome.protocolVersion` is generated as `number`
so `tsc` sees nothing. Costs: the guard surface narrows to two deliberate tests; **no mechanical
brake on an atomic ticket's size remains**; the probe costs two `check` runs and must be reverted;
and the rules moved twice in two days. **Unblocks `TASK-021301`** at `files_touched: 15` with the
twelve files already implemented at `c904503` untouched, **and with it `STORY-0213`, `STORY-0214`
and `STORY-0405`**. **Names one ticket** — convert the five silently-stale client fixtures
(`Lobby.test.tsx`, `duel-state.test.ts`, `duel-provider.test.tsx`, `duel-store.test.ts`,
`frames.test.ts`) to `PROTOCOL_VERSION` under `ADR-0069` §4; one XS client ticket, after
`TASK-021301` lands — and raises no `DEC`.

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

`DEC-055` → [`ADR-0061`](../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md)
(**a season is one calendar month in UTC, derived and never stored; the ladder is a window over it;
a boundary does nothing, and `player.coin_balance` is never reset.** Answered by the product owner on
2026-08-19, from two vision sentences that pull opposite ways — *"a counter of duels won"* that is
*"not a balance"* is why nothing is destroyed at a boundary, and *"Ranked results over a season"* is
why the ladder is scoped rather than lifetime. Bounds are half-open, the identifier is the month, and
a duel belongs to the season containing its `finished_at`, so one finishing exactly on a boundary is
in the **new** season. No table, no column, no migration, no operator, no job. A standing is the
`SUM(coin_delta)` inside the window, and the ladder is **results, not players** — a row exists for
whoever finished a duel that season, including at `0` for a draw. The ladder shows the current season
only, prints the season standing rather than the strip's number, and **names the season from the
response** rather than the browser's clock. `ADR-0014` is neither superseded nor amended. The reset
branch was **declined for want of authority, not on the merits** — it would make the vision's *What
it is* false and belongs to the human. Costs recorded: the ladder empties on the first of every
month; nothing records who won a season; the strip and the ladder disagree from the second season on,
which contradicts one of this epic's own non-negotiables on purpose; the read becomes an aggregate
over a join with no index; and past standings recompute rather than freeze, so `ADR-0039`'s eventual
deletion would silently edit them. **Drops `STORY-0505`**, raises `DEC-060` and `DEC-061`).

`DEC-056` → [`ADR-0063`](../docs/adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
(**nothing gates a place on the ladder, and the farming vector is accepted out loud until the ladder
is public.** Answered by the product owner on 2026-08-21, derived from the vision. The ladder lists
exactly the set `ADR-0061` §4 defines — a row for whoever finished at least one duel in the season —
**narrowed by nothing**: no minimum duels, no minimum standing, no account, no display name, no
profile age, no opponent-diversity rule, so `STORY-0502` adds no `WHERE` beyond the season window and
`STORY-0503` filters no row it was sent. **A player with no display name has a row and it prints
`nameOrNone`'s `No name`** — the question `ADR-0058` parked here by name — with the wire still
carrying `null`. **Two profiles that only ever duel each other are an ordinary pair**, because that
pair is the vision's founding case, *"the author wanted to play quick heads-up duels against his
sister"*, and no server fact separates it from a farm. Two properties are pinned so a later gate
cannot break them quietly: a rank is a position among **everyone** who played that season, and a
season's standings **sum to exactly zero**. The decisive finding was in `duel-rules.md`: a farmed
duel is not merely certain but **fast** — two colluding profiles shove all-in and finish in under a
minute against an honest duel's *"20–45 hands, roughly 5–15 minutes"* — so any rule counting duels is
cleared by the farmer first and taxes the honest player instead. `ADR-0012`'s gate is **discharged
rather than amended**: it is applied at the event that ADR names, *"when the leaderboard goes
public"*, which `EPIC-05`'s out-of-scope table had already relocated to `EPIC-07`. The acceptance
expires at an **event, not a date** — the first time the ladder is served on a public address — or
earlier if a season ends with a standing shaped like a farm, which is recorded as a signal to look
and never as a rule, because one strong player against one regular rival looks the same. Costs
recorded rather than discovered: the farm stays possible **on purpose**, so the first ladder this
product shows can be topped by somebody who never beat anybody; `No name` can be the top row and a
ladder of them cannot be read; a nameless player cannot find themselves on it, which makes `DEC-059`
load-bearing rather than a nicety; a place gate is now more expensive to add than it was to refuse,
since a later one takes away places players have held; and with no threshold the top of the ladder is
noise for the first days of every season. **Unblocks nothing on its own** — `STORY-0502` and
`STORY-0503` still wait on `DEC-058`, `DEC-059` and, for the server, `DEC-061`. **Names one ticket**
for the planner: `EPIC-07`'s definition of done gains a line requiring this acceptance to be
re-affirmed in writing or replaced by a countermeasure before the ladder is served publicly. It
raises no `DEC` — the countermeasure's shape is a decision for the day the acceptance expires, and
`ADR-0063`'s alternative 4, a rate limit on finishing duels, is the shape to start from).

`DEC-058` → [`ADR-0064`](../docs/adr/ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md)
(**tied players share one rank number, and the order rows sit in is not a ranking.** Answered by the
product owner on 2026-08-21, derived from the vision — *"**A leaderboard.** Ranked results over a
season"* ranks a **result**, and two players whose season standings are the same integer have the
same result. A rank is **`1 + the number of players standing strictly higher`**: competition
ranking, so the ladder prints `3, 3, 5` and never `3, 4, 5` and never `3, 3, 4`, and the number
means a sentence a player can state — *there are exactly `rank − 1` players ahead of me this
season*. **A displayed rank and a position in a page are two numbers and only the rank is shown**, a
distinction that survives page two: a page may begin with the rank the previous page ended on, and a
repeated rank across two pages is not a duplicate row, because `STORY-0502`'s totality and
disjointness are properties of **players**. Nothing on the screen breaks a tie, and a tie is marked
by the repeated number and by nothing else in v0.3 — no `=`, no *tied with 12 others*, no styling.
The order tied rows are emitted in stays **the architect's** inside `DEC-061`, constrained in one
product-facing way: the key is a fact about **who a row is** — player id, name collation, profile
age — and never about **how that player did**, because a tiebreak on duels played or on who got
there first is a second ranking rule, `ADR-0014` reserves one for an ADR that *supersedes* it, and
it would mean the way to hold a rank is to stop playing. Costs recorded rather than discovered: **the
rank stops being free**, because it is a function of the whole ladder rather than of the page — under
ordinal ranking it would have been the page offset plus the row index — so `DEC-061` gets harder and
a keyset cursor cannot carry a position forward as if it were a rank; the ladder **cannot name a
leader for the first days of every month**, twelve times a year, and a player cannot tell where in a
190-row block of `5`s they sit, which makes `DEC-059` load-bearing for the second time; a jump from
`5` to `195` reads as missing rows, and §5 prints nothing that explains it; a season can end with
**no single winner**, so `DEC-060`'s *a single remembered winner* is not always well defined; every
future *top N* is ill-defined, being three people on a mature ladder and three hundred on day two;
and the size of a tie is invisible from any single screen. **Unblocks nothing on its own** —
`STORY-0502` still waits on `DEC-059` and `DEC-061`, `STORY-0503` on `DEC-059` and on `STORY-0502`.
**Names no ticket and raises no `DEC`**: whether a tie is ever marked with a glyph or a count is a
string in the ladder's text module and an ordinary ticket against `STORY-0503` if it is ever
wanted).

`DEC-059` → [`ADR-0065`](../docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md)
(**the ladder hands a player their own row, and the profile strip keeps the all-time coin.**
Answered by the product owner on 2026-08-21, derived from the vision — *"**A leaderboard.** Ranked
results over a season"* ranks a result **belonging to a player**, and a ladder whose own player
cannot locate their result ranks it for everybody except the person who produced it; and *"**One
duel coin per win.** Not chips, not currency, not a balance. A counter of duels won"* is why the
strip keeps printing that counter and gains nothing. The **ladder screen** renders one **self
line**, above the rows and below the season name, stating the requesting player's **rank** and
**season standing** — present whether or not their row is in the page on screen, and unchanged as
they walk pages, so a player is handed their row rather than asked to find it. **The profile strip
is untouched**: no rank, no season standing, no season name, `ProfileResponse` gains no field, and
`GET /api/me` never becomes a whole-ladder aggregate on a route that runs on every lobby load —
which answers what `STORY-0311` and `ADR-0061` §6 both parked here. Which number each surface shows
is pinned: strip = all-time `player.coin_balance`, ladder rows **and** self line = the season
`SUM(coin_delta)`, told apart by `ADR-0061` §6's season name and by `ADR-0060` never putting the two
on one screen. **`STORY-0502` therefore ships two aggregates in one response**, not one query: the
page, and one player's competition rank for a player who may be on no page it drew — mechanism left
to `DEC-061`. Three states, and the third is not a zero: a rank and a standing; **no place this
season** for a profile that finished no duel in it, printing no rank and never `0`, because `0` is a
real standing a draw earns (`ADR-0015`); and no line at all for a request with no known device — and
**the page is identical in all three**, so the ladder stays readable without a profile and is never
personalised. **Nothing else marks the player in v0.3**: no highlighted row, no *jump to me*, no
ladder total, no movement, no tie count, no link. Costs recorded rather than discovered: the ladder
read **stops being one query** and `DEC-061` gets harder for the second time in a day; the lobby
still does not say where you stand, so it is a click every time on a screen `ADR-0060` said would
crowd; the self line may disagree with the same player's row in the same response, which the ADR
permits on purpose rather than paying for a snapshot; **the ladder response is now per-requester**
and can no longer be cached as one public document, a bill `EPIC-07` pays; the founding two-player
ladder gets a line restating a row beneath it; and telling a player they are rank `5` makes *which
of these 190 rows is mine* a sharper question that this decision deliberately does not answer.
**Unblocks `STORY-0502` down to `DEC-061` alone and `STORY-0503` down to `STORY-0502` landing.**
Constrains `DEC-061` further — one player's rank cannot be derived from a page that player is not
on — leaves `DEC-057` and `DEC-060` untouched, and **names no ticket and raises no `DEC`**).

`DEC-061` → [`ADR-0066`](../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md)
(**the ladder is computed per request, and a walk is pinned to the instant it began.** Answered by
the architect on 2026-08-21, and both halves are taken. **Per request, from the ledger:** no
`season_standing` table, no materialised view, no summary column, no cache, no refresh job, no third
ticker sweep, and no change to the transaction that records a duel — `ADR-0061` §3's *"nothing writes
a season down, so nothing can disagree about one"* applied one level down, which is also what keeps a
duel on the ladder **the instant it commits**, with no window to explain to the player who just won.
The read is one `WITH standing AS (SUM(coin_delta) …)` over the season window, and SQL's `rank()`
**is** `ADR-0064` §1's competition rank rather than an expression anybody invents. **The page
guarantee is bought, not inherited**, and `STORY-0408`'s sentence is explicitly not claimed: a
request with no cursor mints `asOf = Instant.now(clock)`, the cursor carries it back, and the
query's upper bound is **the cutoff, never the season's end** — so a walk enumerates *the ladder as
it stood committed at the cutoff* and returns **every player of that ladder exactly once**, with the
rank each held then, so ranks never decrease down a walk and the self standing (`ADR-0065` §3) is
byte-identical on every page of it. **Two refusals are written down rather than glossed.** A walk is
**not live**: a duel finishing after the cutoff is in no page of it, a player whose first duel of the
season lands mid-walk has no row anywhere in it, and page forty is as old as page one — seeing it
means starting a new walk. And exactly-once has **one named exception**: a duel *committed* after a
page was drawn but stamped `finished_at` *before* the cutoff moves the pinned ladder underneath the
rest of the walk, so its **winner can be never returned** and its **loser returned twice**. Both are
accepted; what the cutoff buys is that the window is the width of one duel-recording transaction
instead of the width of the walk, where a live recompute would lose every player who wins while you
read and repeat every player who loses. The order is `coins DESC, player_id DESC` — `player.id` is
**forced rather than chosen**, as the only key that is identity (`ADR-0064` §4), unique, and
**immutable**, since a `display_name` takedown would move a row mid-walk — paged by one row-value
comparison, `DUELS_AFTER_SQL`'s idiom, with `recentDuelsPage`'s `limit + 1` probe row. The page and
the self standing are **two statements bounded by the same cutoff**, which agree without a
transaction, so `ADR-0065` §3's permission to be inconsistent goes unused. A cursor whose `asOf` sits
outside the season the server's clock is in is a flat `400` (`ADR-0057` §5), so a walk crossing a
month boundary restarts instead of serving August in September; and `ADR-0057` §7's *"a leaderboard
page"* MAC clause is shown **not to engage**, because `ADR-0065` §4 makes the page identical for
every reader, so a forged position leaks nothing that asking normally would not. Costs recorded
rather than discovered: **every page is a full-month aggregate and there are two per page**, so a
twenty-page walk is forty unindexed passes on a public, unauthenticated, unbudgeted read — the
sharpest cost, and there is still no measurement anywhere in the product; later pages are **stale**
and nothing on screen says so; **exactly-once carries its exception permanently in the contract**, so
a reader genuinely can see a row twice or miss one; a walk is refused twelve times a year at the
month boundary and every client must implement *restart the walk*; the cursor now carries a value the
server trusts from the client; two statements do the same expensive work twice; and a **live walk is
foreclosed** by construction. **Unblocks `STORY-0502`** — the last gate on it — leaves `DEC-057` and
`DEC-060` untouched, and raises no `DEC`. **Names one ticket** for the planner: an index for the
season window, `duel (finished_at)` in a new `V7__` migration carrying an `EXPLAIN`-backed
measurement in its `verify:` block, deliberately outside `STORY-0502` because the tables hold
hundreds of rows and an index added on imagination is a permanent write cost on the one transaction
where a coin moves).

`DEC-062` → [`ADR-0062`](../docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md)
(**the server has two clocks and neither answers the other's question.** `ServerClock`
(`System.nanoTime()`) measures **durations** — timeouts, grace windows, `ADR-0025`'s sweeps —
and `java.time.Clock` reports **dates**: injected as a parameter, `Clock.systemUTC()` in production,
`Clock.fixed` in a test. No new port and no third clock type — the wall clock is `java.time.Clock`
itself, which `PostgresDuelResultSink` already takes. `Instant.ofEpochMilli(clock.nowMillis())` is a
defect wherever it appears; the no-argument `Instant.now()`, `System.currentTimeMillis()`,
`LocalDate.now()`, `YearMonth.now()` and `ZoneId.systemDefault()` appear nowhere in
`poker-server/src/main`; there is exactly **one** `Clock.systemUTC()`, at the composition root; a
pure function takes the clock with **no default**; and the zone is a literal `ZoneOffset.UTC`, never
the clock's own. **Amends three merged ADRs in one clause each**, per `docs/adr/README.md`'s
convention that a correction goes in the status line and *supersede* is for a decision reversed:
`ADR-0061` §3 — *which season is it* is a function of the instant an injected `java.time.Clock`
reports, not of `ServerClock.nowMillis()` — and, found by searching for the sentence rather than the
id, `ADR-0027` §1's thirty-day session and `ADR-0031`'s twenty-four-hour and one-hour tokens, whose
lifetimes land in an `expires_at TIMESTAMPTZ` compared against SQL `now()` and would each have been
**born expired in 1970**. The durations are policy and do not move; both ADRs' in-memory rate-limit
windows keep `ServerClock` and are right as written. `STORY-0501`, `STORY-0405` and `STORY-0502`
carried the same instruction and are corrected too. `ServerClock` **keeps its name**: the rename
to `ElapsedClock.elapsedMillis()` is argued in full and declined for now — eleven Kotlin files that must land
atomically past a three-file ticket cap, and seven merged ADRs and fourteen merged tickets left
naming a type that would no longer exist — with the trigger recorded and made checkable: all four occurrences predate this ADR and came
from documents that now say the opposite, so **one more, in any document written after it**,
makes the name the cause and the rename a story on this argument. The cost is named rather than discovered: **the misleading name survives, so the
mistake stays expressible**, and the deterrent is a KDoc, an architecture section and a static-read
guard test, none of which can see a misused injected clock. **Unblocks `TASK-050106`** and names
three tickets for the planner: the KDoc that still points at `System.currentTimeMillis`, the
composition root's single `Clock.systemUTC()` before `STORY-0502`, and the guard test).

`DEC-057` → [`ADR-0067`](../docs/adr/ADR-0067-a-leaderboard-row-is-text-and-no-id-turns-into-a-profile.md)
(**a leaderboard row is text, it leads nowhere, and no id turns into a profile.** Answered by the
product owner on 2026-08-22, derived from the vision, whose two sentences answer different halves.
The **roadmap** answers *whether*: v0.3 is *"Leaderboard and seasons"* and v0.4 is *"Friends,
statistics, replay viewer"*, so a page about another player carrying duels played, a win/loss record
or a duel list is **statistics**, one milestone later — and deciding that a v0.4 thing **waits** is
applying the roadmap, while deciding it **moves** would be reordering it, which is the human's, so
only one of the two directions was ever available. *"**A leaderboard.** Ranked results over a
season"* answers *what*: the vision names **ranked results**, not ranked people — `ADR-0061` §4's
*"results, not players"* — and a result is discharged by a row, not by a person waiting behind it.
**A row is a rank, a name or `No name`, and a season standing, on one line** (`4 Ada 3`,
`215 No name −1`): not a link, not a button, not a control. This **confirms what `STORY-0503`
already ships** rather than changing it — `TASK-050313`'s *no `<a>`, no `link` role, no `button`
inside the section* assertion was written *"until `DEC-057` is answered"* and is now the decision.
**What a stranger reads is exactly four fields**: `rank`, `displayName`, `coins`, `playerId`. All
four already ship; what is new is that they are the **boundary** rather than the set the first story
needed. **What a stranger does not read is enumerated field by field** so that absence is testable:
the all-time `coin_balance`, duels played, wins, losses, draws, a win rate, the ladder total, the
**duel list** — `GET /api/me/duels` gains no player parameter, no opponent filter and no second
route — streaks, movement, last seen, online state, anything about a credential, handle, email,
device binding or session, and `displayNameRemoved`, which already *"never says anything about
another player"* (`ADR-0053`). **`playerId` stays on the wire and is a name for a row, never an
address**: the server serves five paths and not one of them answers *what about player X*, while a
client may still use the id as a list key and to correlate what it was already told (`ADR-0021`).
That is **`ADR-0029` §7 pushed from the other side** — §7 keeps a **name** from becoming an id, this
keeps an **id** from becoming a person, and together **no path in this product turns anything into a
lookup of a player**. **`STORY-0504` is `dropped`**, its premise false, with nothing moving out of
it: its one surviving assertion already ships in `TASK-050313`. Reopening is deliberately made a
decision and not a ticket: a new `DEC` naming **one field at a time**, an answer to what the subject
is asked and can turn off on a product with no opt-in (`ADR-0063` §1), no opt-out and no deletion
(`ADR-0039`) whose default player never signed up for anything (`ADR-0012`, `ADR-0036`) — **and the
human's call before any fact a player did not choose to publish is published**, which is where a
product decision starts having consequences outside the software. No escalation was needed for
*this* answer, because it publishes nothing new; the opposite answer would have needed one. Costs
recorded rather than discovered: **the ladder is a list of strangers and a player has no way to ask
who any of them are** — you lose a duel, open the ladder, see the name that beat you at rank 3, and
the gesture every ladder on the internet has trained you to make does nothing, so the vision's own
*rival* has nowhere to point; **`DEC-054` loses its sharpest argument**, since this epic's case for
it was *"a leaderboard row that leads to another player is a link, and a client with no addresses
cannot express one"*, and an address-less client just got more comfortable for another milestone;
the question is **deferred, not solved, and gets harder** — in v0.4 it arrives as a ticket saying
*just add a player page*, guarded by prose; the asymmetry is deliberate and uncomfortable, since a
player is published on a ladder they never opted into and can look up nobody, including whoever just
beat them, while the server holds all of it; and **v0.3 ships no social surface at all**, so the
first thing connecting two players outside a duel is now v0.4's *Friends*. **Forecloses nothing
structurally** — that is the point — but forecloses inside `EPIC-05` even the zero-disclosure
head-to-head view, which is a small ticket and still v0.4's row. Leaves `DEC-054`, `DEC-060` and
`DEC-008` untouched, **names no ticket and raises no `DEC`**).

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
carry no count because they may land in any order. All seventeen are merged. `TASK-030717` was
filed on 2026-08-16 by `TASK-030716`'s own second red edit — deleting `guard`'s
`message.handNumber != state.handNumber` line left the whole of `DuelActionTest` green, because
the suite's only staleness coverage replayed a frame from the *same* hand. The chain is strictly
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

**`STORY-0309` is split into fourteen** on 2026-08-23, on a **measured** baseline of 533 client
tests, cumulative counts **536 → 566**, and `TASK-030901` is startable now. `STORY-0213` merged, so
`protocol.gen.ts` carries `OfferRematch`, `RematchOffered` and `REMATCH_UNAVAILABLE`,
`PROTOCOL_VERSION` is 3, and the button `TASK-030807` refused to fake now has a wire to sit on. The
story writes **no Kotlin**, which is `EPIC-03`'s standing rule and the reason `ADR-0044` exists.

Splitting it found two things the story could not have known and one it must not decide:

- **`ADR-0044` §5's ordering had no client half.** The reducer's `default: return state` swallowed
  `RematchOffered` whole, so nothing distinguished an offer arriving *before* `DuelFinished` from one
  arriving after — the distinction the server took a commitment on, and the reason `DuelSocket`
  restates a standing offer only once the resumed frames have landed. `TASK-030902` clears the
  recorded offers on `DuelFinished`, `TASK-030913` asserts the order at the screen both ways round,
  and neither test proves presence: a screen that showed every offer it ever saw passes one and fails
  the other.
- **`Snapshot` did not clear `outcome`, and `Lobby.tsx` tests `outcome` before `view`** — so a
  rematch's opening frames would have left the result screen up forever. `TASK-030903` fixes it, and
  the blast radius was **probed rather than remembered**: the whole reducer change, applied at once,
  turns exactly one pre-existing test red (`starts with nothing the server has not sent`, whose
  `toEqual` compares the state object whole), and `TASK-030901` owns and fixes it. `TASK-030906`
  likewise owns `STORY-0308`'s `offers no rematch it cannot honour`, whose premise — `DEC-023` open,
  the wire unable to carry one — is gone; it is replaced by two narrower tests rather than left
  standing.
- **`DEC-067` was raised here and is now answered** by
  [`ADR-0072`](../docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md). The
  split found that `boot.ts` forgets the remembered room code on `DuelFinished` (`TASK-031009`, so a
  reload reaches the lobby), which means a tab that reloads or whose socket reopens on the result
  screen never re-`JoinRoom`s: as merged, a rematch survives no reconnect, and that player's own
  press answers `UNKNOWN_ROOM` from an empty `RoomMembership`. The answer deletes that branch and
  puts the forget on the way back instead — the memory names the room this tab is **seated in**, and
  only the player leaving or a refused rejoin clears it. It blocked **no ticket** — all fourteen
  apply frames to the store, as every screen test in `Lobby.test.tsx` already does — but the
  transport half of the story's fourth acceptance criterion is its work, and it is now
  `TASK-030915`–`TASK-030920`.

**Six more joined `STORY-0309` the same day**, cumulative counts **569 → 576**, and they are the
transport half `ADR-0072` decided: `boot.ts` gains `forgetRoom` and loses its `DuelFinished` branch,
`DuelProvider` an optional third prop and `useForgetRoom()`, `DuelResult` an `onLeave` on the anchor
it already had, and `Lobby.tsx` and `main.tsx` the wiring between them. Three things about that
split are worth keeping:

- **The blast radius was measured.** `ADR-0072`'s whole change was applied at once in a throwaway
  tree and the client's gate set run in full — `tsc`, ESLint, `prettier --check`, Vitest and
  `vite build`, which is what `build.yml` runs on a pull request. Exactly **two** merged tests turn
  red, both `TASK-031009`'s, both named in `ADR-0072` §9; `reconnect.test.tsx`, `src/e2e/`,
  `duel-provider.test.tsx`, `Lobby.test.tsx` and `DuelResult.test.tsx` are all untouched by it, and
  nothing fails to typecheck. `TASK-030919` deletes those two in the diff that invalidates them and
  replaces them with three.
- **The reversal is last, not first.** `TASK-031009`'s reason for the branch holds until the way
  back forgets: delete it any earlier and the way on from the result reloads straight back into the
  result screen, leaving the lobby unreachable for as long as the remaining tickets take to merge.
  So the screen is wired (`TASK-030918`) and only then is `boot.ts` reversed (`TASK-030919`).
- **One of that ticket's three tests cannot detect the reversal**, and it says so: restoring the
  branch turns two red and leaves the third green, because the branch has already forgotten the code
  by the time `forgetRoom()` is called. Run both ways to find that out. Its job is that the forget
  still reaches a finished room; the `! grep` on `boot.ts` is what proves the branch is gone.

**`DEC-068` was raised here and is answered by [`ADR-0073`](../docs/adr/ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md)**,
and it never touched the rematch: the *waiting for your rival* screen had no way out, in `Lobby.tsx`
or in `design/screens/create-duel.html`. The claim was checked rather than taken from the ADR —
`RoomRegistry.resume` declines a `WAITING` room, `Room.join` refuses a seated player with
`ALREADY_SEATED`, and `DuelSocket.replyToJoinRoom` answers that *exactly as a fresh seating would*,
so a host's reload lands back on the same screen and the routes out were a rival joining, the
ten-minute reap, or clearing storage. `forgetRoom()` is exactly what a control there would call,
which is why it was cheap to close now. The answer: the control exists, it reads **`Back to the
lobby`** — the string `DuelResult.tsx` already renders for the same action — it does **nothing** to
the room, and one line says so (*The room stays open. That link still works for your rival, and it
brings you back.*), with no confirmation and no duration printed anywhere. It is
[`STORY-0314`](stories/STORY-0314-a-host-can-leave-the-room-they-opened.md), now `ready` to split and
blocking nothing else — in particular not the transport half above.

`STORY-0213`'s planning defect is carried across deliberately: eight of its nine tests passed against
a hard-coded `seat = 0`, because every one drove the host into the offer. Here the seat comparison
has exactly one home — `TASK-030905`'s `rematchStand` — and its test holds the offers array constant
while moving the viewer, then repeats the flip around seat 0, so neither seat number can be the
constant. Every screen test seats this client at **1**.

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
| **[STORY-0307](stories/STORY-0307-action-bar.md)** The action bar — acting on your turn — *schema 2* | | **done** |
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
| **[STORY-0308](stories/STORY-0308-result-screen.md)** The result screen — who won, and the coin — *schema 2* | | **done** |
| | [TASK-030801](tasks/TASK-030801-a-duel-outcome-fixture-with-every-field-the-wire-declares.md) A DuelOutcome fixture with every field the wire declares | S | **done** |
| | [TASK-030802](tasks/TASK-030802-the-verdict-is-read-off-the-winner-and-your-seat.md) The verdict is read off the winner and your seat, and nothing else | S | **done** |
| | [TASK-030803](tasks/TASK-030803-the-coin-line-states-the-one-coin-the-duel-moved.md) The coin line states the one coin the duel moved, and no balance | XS | **done** |
| | [TASK-030804](tasks/TASK-030804-the-coin-mark-is-steel-and-says-nothing.md) The coin mark is steel, and says nothing a screen reader has to hear twice | XS | **done** |
| | [TASK-030805](tasks/TASK-030805-the-result-screen-declares-the-verdict-and-the-coin.md) The result screen declares the verdict and the coin beside it | S | **done** |
| | [TASK-030806](tasks/TASK-030806-the-result-states-the-hands-played-and-both-final-stacks.md) The result states the hands played and every final stack, exactly as sent | S | **done** |
| | [TASK-030807](tasks/TASK-030807-the-way-on-from-the-result-is-back-to-the-lobby.md) The way on is back to the lobby, and there is no dead rematch | XS | **done** |
| | [TASK-030808](tasks/TASK-030808-the-result-derives-no-winner-and-no-figure.md) The result derives no winner and shows no figure the outcome did not carry | S | **done** |
| | [TASK-030809](tasks/TASK-030809-the-duel-screen-shows-the-result-when-the-duel-ends.md) The duel screen shows the result when the duel ends | S | **done** |
| **[STORY-0309](stories/STORY-0309-rematch.md)** Rematch from the result screen — *schema 2* | | **done** |
| | [TASK-030901](tasks/TASK-030901-the-store-records-which-seats-have-offered.md) The store records which seats have offered a rematch | S | **done** |
| | [TASK-030902](tasks/TASK-030902-a-finished-duel-begins-the-result-screen-with-no-offer-standing.md) A finished duel begins the result screen with no offer standing | XS | **done** |
| | [TASK-030903](tasks/TASK-030903-the-snapshot-after-a-finish-is-the-rematch.md) The snapshot after a finish is the rematch, and clears the duel that ended | XS | **done** |
| | [TASK-030904](tasks/TASK-030904-a-rematch-the-room-cannot-take-yet-is-recorded-nowhere.md) A rematch the room cannot take yet is recorded nowhere | XS | **done** |
| | [TASK-030905](tasks/TASK-030905-whose-rematch-offer-it-is.md) Whose rematch offer it is, read from the seat the server gave this client | XS | **done** |
| | [TASK-030906](tasks/TASK-030906-the-result-panel-shows-the-rematch-it-is-handed.md) The result panel shows the rematch it is handed, and adds none of its own | S | **done** |
| | [TASK-030907](tasks/TASK-030907-the-rematch-control-offers-one-press.md) The rematch control offers one press, and a second press is harmless | S | **done** |
| | [TASK-030908](tasks/TASK-030908-the-control-says-who-has-offered.md) The control says who has offered, and reads it from either side | S | **done** |
| | [TASK-030909](tasks/TASK-030909-a-room-that-is-gone-retires-the-control.md) A room that is gone retires the control and says so | XS | **done** |
| | [TASK-030910](tasks/TASK-030910-the-result-screen-hands-over-the-control-and-the-press-reaches-the-wire.md) The result screen hands over the control, and the press reaches the wire | S | **done** |
| | [TASK-030911](tasks/TASK-030911-the-way-back-steps-aside-for-the-rematch.md) The way back steps aside for the rematch | XS | **done** |
| | [TASK-030912](tasks/TASK-030912-the-rematch-begins-and-the-button-changes-sides.md) The rematch begins, and the button is on the other side | S | **done** |
| | [TASK-030913](tasks/TASK-030913-an-offer-restated-after-a-rejoin-reaches-the-result-screen.md) An offer restated after a rejoin reaches the result screen, and one stated before it does not | XS | **done** |
| | [TASK-030914](tasks/TASK-030914-a-gone-room-ends-it-and-a-transient-refusal-does-not.md) A gone room ends the rematch, and a transient refusal leaves it live | XS | **done** |
| | [TASK-030915](tasks/TASK-030915-boot-can-forget-the-room-this-tab-remembers.md) Boot can forget the room this tab remembers | S | **done** |
| | [TASK-030916](tasks/TASK-030916-the-provider-carries-the-forget-down-to-the-screen.md) The provider carries the forget down to the screen | S | **done** |
| | [TASK-030917](tasks/TASK-030917-the-way-back-calls-the-forget-it-is-handed.md) The way back calls the forget it is handed, and still navigates | XS | **done** |
| | [TASK-030918](tasks/TASK-030918-the-result-screens-way-back-is-wired-to-boots-forget.md) The result screen's way back is wired to boot's forget | XS | **done** |
| | [TASK-030919](tasks/TASK-030919-a-finished-duel-forgets-nothing-and-the-next-socket-rejoins.md) A finished duel forgets nothing, and the next socket rejoins that room | S | **done** |
| | [TASK-030920](tasks/TASK-030920-from-a-resumed-sockets-frames-the-way-back-forgets-the-room.md) From a resumed socket's frames, the way back forgets the room | XS | **done** |
| **[STORY-0310](stories/STORY-0310-reconnect-and-resume.md)** Reconnect — the client resumes its seat — *schema 2* | | **done** |
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
| **[STORY-0311](stories/STORY-0311-profile-strip.md)** The profile strip — my coins and my recent duels — *schema 2* | | **done** |
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
| **[STORY-0312](stories/STORY-0312-whole-duel-through-the-client.md)** A whole duel through the client, frame by frame — *schema 2* | | **done** |
| | [TASK-031201](tasks/TASK-031201-played-duel-records-the-acts-it-sent.md) A played duel records the Act it sent, and the seat it sent it from | XS | **done** |
| | [TASK-031202](tasks/TASK-031202-a-whole-duel-as-one-seats-session-of-frames.md) A whole duel written down as each seat's own session of frames | S | **done** |
| | [TASK-031203](tasks/TASK-031203-one-task-writes-the-script-another-fails-on-drift.md) One Gradle task writes the duel script, another fails the build on drift | S | **done** |
| | [TASK-031204](tasks/TASK-031204-the-client-reads-the-script-and-proves-it-is-a-duel.md) The client reads the committed script, and proves it is a whole duel | S | **done** |
| | [TASK-031205](tasks/TASK-031205-the-script-replays-through-the-real-client.md) The script replays through the real client, from either seat, to the result | S | **done** |
| | [TASK-031206](tasks/TASK-031206-one-act-per-turn-the-frame-the-server-recorded.md) The client answers each turn through the bar, with the frame the server recorded | S | **done** |
| | [TASK-031207](tasks/TASK-031207-the-result-states-the-outcome-the-last-frame-carried.md) The result states the outcome the script's last frame carried, from either seat | S | **done** |
| | [TASK-031208](tasks/TASK-031208-no-rival-card-reaches-the-screen-before-the-reveal.md) No rival card reaches the screen before the frame that reveals it | S | **done** |
| | [TASK-031209](tasks/TASK-031209-a-hand-won-without-a-showdown-shows-no-rival-card.md) A hand won without a showdown shows no rival card at all | S | **done** |
| **[STORY-0313](stories/STORY-0313-the-table-names-an-absent-opponent.md)** The table names an absent opponent — *schema 2*, split 2026-08-24 into **fifteen**, all fifteen merged. `DEC-070` (the mark's lifetime) answered by [`ADR-0075`](../docs/adr/ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md) | | **done** |
| | [TASK-031301](tasks/TASK-031301-the-seats-status-line-learns-away-and-timed-out.md) The seat's status line learns Away and Timed out, and where they rank | S | **done** |
| | [TASK-031302](tasks/TASK-031302-the-line-that-explains-the-pause-and-the-one-that-says-nothing.md) The line that explains the pause, and the one that says nothing | S | **done** |
| | [TASK-031303](tasks/TASK-031303-the-store-holds-the-presence-the-server-stated.md) The store holds the presence the server stated, and counts the frames | S | done |
| | [TASK-031304](tasks/TASK-031304-a-rival-is-back-only-if-this-client-saw-them-go.md) A rival is back only if this client saw them go | S | done |
| | [TASK-031305](tasks/TASK-031305-whole-seconds-to-the-deadline-and-never-below-zero.md) Whole seconds to the deadline, and never below zero | XS | done |
| | [TASK-031306](tasks/TASK-031306-the-notice-says-the-state-and-counts-the-window-down.md) The notice says the state and counts the window down | S | done |
| | [TASK-031307](tasks/TASK-031307-the-plate-carries-the-presence-it-is-handed.md) The plate carries the presence it is handed | XS | done |
| | [TASK-031308](tasks/TASK-031308-the-presence-lands-on-the-rivals-plate-whichever-seat-that-is.md) The presence lands on the rival's plate, whichever seat that is | S | done |
| | [TASK-031309](tasks/TASK-031309-the-duel-screen-shows-the-notice-and-a-paused-action-has-a-reason.md) The duel screen shows the notice, and a paused action has a reason | S | done |
| | [TASK-031310](tasks/TASK-031310-a-resumed-client-renders-what-it-came-back-to.md) A resumed client renders what it came back to, and invents no return | XS | done |
| | [TASK-031311](tasks/TASK-031311-the-countdown-reaching-zero-changes-nothing-the-client-does.md) The countdown reaching zero changes nothing the client does | S | done |
| | [TASK-031312](tasks/TASK-031312-the-duel-screen-says-none-of-the-words-this-copy-refuses.md) The duel screen says none of the words this copy refuses | S | done |
| | [TASK-031313](tasks/TASK-031313-the-server-is-the-subject-of-every-action-it-took.md) The server is the subject of every action it took | S | done |
| | [TASK-031314](tasks/TASK-031314-the-store-keeps-the-most-recent-action-the-server-took.md) The store keeps the most recent action the server took, until the absence ends | S | done |
| | [TASK-031315](tasks/TASK-031315-the-duel-screen-names-the-server-as-the-actor.md) The duel screen names the server as the actor | S | done |
| [STORY-0314](stories/STORY-0314-a-host-can-leave-the-room-they-opened.md) | A host can leave the room they opened (`ADR-0073` fixed its words) | **done** — five of five |
| | [TASK-031401](tasks/TASK-031401-the-waiting-screen-offers-the-way-back-to-the-lobby.md) The waiting screen offers the way back to the lobby | S | done |
| | [TASK-031402](tasks/TASK-031402-one-line-says-the-room-stays-open.md) One line says the room stays open | XS | done |
| | [TASK-031403](tasks/TASK-031403-two-strings-are-the-whole-addition-and-nothing-stands-between-the-press-and-the-lobby.md) Two strings are the whole addition, and nothing stands between the press and the lobby | S | done |
| | [TASK-031404](tasks/TASK-031404-the-waiting-screen-offers-none-of-the-words-adr-0073-refuses.md) The waiting screen offers none of the words `ADR-0073` refuses | S | done |
| | [TASK-031405](tasks/TASK-031405-the-press-leaves-nothing-on-the-wire-and-the-next-socket-rejoins-nothing.md) The press leaves nothing on the wire, and the next socket rejoins nothing | S | done |

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
| **[STORY-0405](stories/STORY-0405-sign-in-the-session-and-what-the-socket-presents.md)** Sign-in, the session, and what the socket presents | | | **done** — 26 merged (24 split on 2026-08-23, plus `TASK-040525` and `TASK-040526` filed on 2026-08-24 from gaps found by mutation); `DEC-069` answered by `ADR-0074` |
| | [TASK-040501](tasks/TASK-040501-the-version-is-answered-before-an-identity-is-minted.md) The version question is answered before any identity is minted | S | **done** |
| | [TASK-040502](tasks/TASK-040502-the-wire-carries-a-token-names-the-player-and-the-version-takes-its-step.md) The wire carries a session token, names the player, and PROTOCOL_VERSION takes its step | S | **done** |
| | [TASK-040503](tasks/TASK-040503-a-mismatched-version-mints-nothing.md) A version mismatch mints no device id and creates no profile | XS | **done** |
| | [TASK-040504](tasks/TASK-040504-a-session-token-is-256-bits-url-safe-and-unpadded.md) A session token is 256 bits, URL-safe and unpadded | XS | **done** |
| | [TASK-040505](tasks/TASK-040505-the-session-store-is-a-port-and-a-double-that-knows-nothing.md) The session store is a port, and a double that has issued nothing | XS | **done** |
| | [TASK-040506](tasks/TASK-040506-issue-writes-one-row-a-digest-and-thirty-days.md) issue writes one row, a digest, and thirty days from the injected clock | S | done |
| | [TASK-040507](tasks/TASK-040507-playerof-reads-through-the-expiry.md) playerOf reads through the expiry, and a clock thirty days on refuses | S | done |
| | [TASK-040508](tasks/TASK-040508-delete-removes-the-row-and-says-the-same-thing-twice.md) delete removes the row, and says the same thing twice | XS | done |
| | [TASK-040509](tasks/TASK-040509-the-directory-finds-a-profile-without-creating-one.md) The directory can find a profile without creating one | S | done |
| | [TASK-040510](tasks/TASK-040510-one-resolver-and-an-invalid-session-never-falls-back.md) One resolver, and an invalid session never falls back to the device | S | done |
| | [TASK-040511](tasks/TASK-040511-the-read-path-follows-the-resolved-player.md) The profile read follows the resolved player, and every route resolves the same way | S | done |
| | [TASK-040512](tasks/TASK-040512-a-signed-in-request-reads-the-sessions-profile.md) A signed-in request reads the session's profile, and the device beside it is ignored | S | done |
| | [TASK-040513](tasks/TASK-040513-the-sign-in-body-is-two-fields-and-the-answer-carries-the-token-once.md) The sign-in body is two fields, and its answer carries the token exactly once | XS | done |
| | [TASK-040514](tasks/TASK-040514-sign-in-the-credential-decides-and-a-stranger-learns-nothing.md) POST /api/auth/sign-in — the credential decides, and a stranger learns nothing | S | done |
| | [TASK-040515](tasks/TASK-040515-sign-out-is-one-delete-and-two-hundred-and-four-either-way.md) POST /api/auth/sign-out — one delete, 204 either way, and no socket closes | XS | done |
| | [TASK-040516](tasks/TASK-040516-the-document-names-sign-in-and-sign-out.md) The document names sign-in and sign-out, and the test that reads it keeps its bearings | S | done |
| | [TASK-040517](tasks/TASK-040517-the-socket-is-handed-the-resolver.md) The socket's dependencies carry the resolver | XS | done |
| | [TASK-040518](tasks/TASK-040518-the-socket-presents-the-session-and-an-invalid-one-is-refused.md) The socket presents the session, and an invalid one is refused rather than downgraded | S | done |
| | [TASK-040519](tasks/TASK-040519-a-budget-is-a-rolling-window-and-over-budget-still-counts.md) A budget is a rolling window, an over-budget attempt still counts, and a slot can be refunded | S | done |
| | [TASK-040520](tasks/TASK-040520-the-sign-up-budget-is-two-config-values.md) The two auth budgets are four configuration values with defaults | S | done |
| | [TASK-040521](tasks/TASK-040521-sign-up-over-budget-answers-429.md) Sign-up over budget answers 429, and the budget meters the hash | S | done |
| | [TASK-040522](tasks/TASK-040522-the-document-names-the-seventh-answer.md) The document names sign-up's seventh answer | XS | done |
| | [TASK-040523](tasks/TASK-040523-sign-in-carries-a-budget-of-its-own.md) Sign-in carries a budget of its own | S | done |
| | [TASK-040524](tasks/TASK-040524-signed-in-here-reading-there-against-the-database.md) Signed in here, reading there — the whole flow against the database | S | done |
| | [TASK-040525](tasks/TASK-040525-a-blank-credential-is-invalid-not-absent.md) A blank credential is invalid, not absent | XS | done |
| | [TASK-040526](tasks/TASK-040526-a-refund-returns-one-slot-even-with-a-window-behind-it.md) A refund returns one slot even with a window behind it | XS | done |
| **[STORY-0406](stories/STORY-0406-the-claim-proven-and-the-device-revoked.md)** The claim proven, and the device binding revoked | | | **done** — 23 merged (21 split on 2026-08-24, plus `TASK-040622` and `TASK-040623` filed from gaps found mid-story); `DEC-041` (`ADR-0049`) and `DEC-045` (`ADR-0050`) settled the schema, the endpoint, both guards and the session sweep |
| | [TASK-040601](tasks/TASK-040601-the-device-binding-is-a-row-and-player-loses-the-column.md) The device binding becomes a row of its own, and player loses its column | S | done |
| | [TASK-040602](tasks/TASK-040602-the-profile-says-whether-the-device-route-is-live.md) The profile says whether the device route is still live | S | done |
| | [TASK-040603](tasks/TASK-040603-a-revoked-binding-is-final-and-the-database-says-so.md) A revoked binding is final, and the database is what refuses to undo it | S | done |
| | [TASK-040604](tasks/TASK-040604-one-live-binding-per-device-one-per-player-and-a-pair-that-never-returns.md) One live binding per device, one per player, and a pair that never comes back | S | done |
| | [TASK-040605](tasks/TASK-040605-a-revoked-device-resolves-to-a-new-profile-never-the-one-it-left.md) A revoked device resolves to a new, empty profile — never the one it left | S | done |
| | [TASK-040606](tasks/TASK-040606-the-session-token-digest-is-one-function-in-one-file.md) The session-token digest is one internal function, in one file | S | done |
| | [TASK-040607](tasks/TASK-040607-the-device-binding-port-and-the-double-that-counts-its-calls.md) The device-binding port, and the double that counts what it was asked | S | done |
| | [TASK-040608](tasks/TASK-040608-revoking-is-one-update-and-one-delete-in-one-transaction.md) Revoking is one UPDATE and one DELETE, in one transaction | S | done |
| | [TASK-040609](tasks/TASK-040609-delete-api-me-device-takes-a-session-or-nothing.md) DELETE /api/me/device takes a session, or it takes nothing | S | done |
| | [TASK-040610](tasks/TASK-040610-no-credential-no-revocation.md) No credential, no revocation — and the refusal writes nothing | S | done |
| | [TASK-040611](tasks/TASK-040611-the-composition-root-installs-the-device-route.md) The composition root builds the bindings and installs the device route | XS | done |
| | [TASK-040612](tasks/TASK-040612-the-document-names-the-device-endpoint.md) The document names the device endpoint, and the section markers still chain | S | done |
| | [TASK-040613](tasks/TASK-040613-signed-out-everywhere-still-signed-in-here.md) Signed out everywhere, and still signed in here | S | done |
| | [TASK-040614](tasks/TASK-040614-the-revoked-device-says-hello-and-is-a-stranger.md) The revoked device says Hello and is seated as a stranger | S | done |
| | [TASK-040615](tasks/TASK-040615-revoke-then-the-password-reaches-the-same-profile.md) Revoke, then the password reaches the same profile, coins and name | S | done |
| | [TASK-040616](tasks/TASK-040616-p1-and-p2-in-one-helper-and-the-proof-neither-subsumes-the-other.md) P1 and P2 in one helper, and the proof that neither subsumes the other | S | done |
| | [TASK-040617](tasks/TASK-040617-both-copies-of-the-ledger-assertions-come-from-the-shared-helper.md) Both copies of the ledger assertions come from the shared helper | S | done |
| | [TASK-040618](tasks/TASK-040618-the-scenario-anonymous-a-duel-a-name-an-account.md) The scenario, steps one to four — anonymous, a duel, a name, an account | S | done |
| | [TASK-040619](tasks/TASK-040619-a-duel-opened-under-a-session-token.md) A duel can be opened under a session token, not only a device id | S | done |
| | [TASK-040620](tasks/TASK-040620-the-scenario-the-token-a-second-account-and-back-to-anonymous.md) The scenario, steps five to eleven — the token, a second account, and back | S | done |
| | [TASK-040621](tasks/TASK-040621-the-scenario-ends-with-a-revocation-and-nothing-escapes-it.md) The scenario ends with a revocation, and no identity endpoint escapes it | S | done |
| | [TASK-040622](tasks/TASK-040622-the-backfill-moves-a-row-that-was-already-there.md) The backfill moves a row that was already there | XS | done |
| | [TASK-040623](tasks/TASK-040623-an-unknown-device-alone-is-refused-too.md) An unknown device, alone, is refused too | XS | done |
| **[STORY-0407](stories/STORY-0407-recovery-from-a-device-never-seen.md)** Recovery — signing in from a device that has never been seen | | | **done** — nine tickets, all test work, since every behaviour it asserts already shipped. Eight extend one scenario, `RecoveryOnAFreshBrowserTest`; the ninth scans source, because a negative over the schema goes ungated by default |
| | [TASK-040701](tasks/TASK-040701-the-device-binding-snapshot-comes-from-one-place.md) The device_binding snapshot comes from one place | XS | done |
| | [TASK-040702](tasks/TASK-040702-a-duel-the-recovered-account-will-remember.md) A duel the recovered account will remember | S | done |
| | [TASK-040703](tasks/TASK-040703-the-name-and-the-password-that-make-the-account-recoverable.md) The name and the password that make the account recoverable | XS | done |
| | [TASK-040704](tasks/TASK-040704-a-browser-never-seen-signs-in-and-the-socket-names-no-device.md) A browser never seen signs in, and the socket names no device | S | done |
| | [TASK-040705](tasks/TASK-040705-the-fresh-browser-reads-back-the-same-profile-and-the-same-duels.md) The fresh browser reads back the same profile and the same duels | S | done |
| | [TASK-040706](tasks/TASK-040706-the-recovery-leaves-no-row-in-either-table-a-profile-occupies.md) The recovery leaves no row in either table a profile occupies | S | done |
| | [TASK-040707](tasks/TASK-040707-a-wrong-password-from-the-fresh-browser-issues-no-session.md) A wrong password from the fresh browser issues no session | S | done |
| | [TASK-040708](tasks/TASK-040708-signing-out-returns-the-fresh-browser-to-nothing.md) Signing out returns the fresh browser to nothing, and the original device to itself | S | done |
| | [TASK-040709](tasks/TASK-040709-one-statement-in-the-whole-server-creates-a-profile.md) One statement in the whole server creates a profile | S | done |
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
| **[STORY-0409](stories/STORY-0409-history-filters-and-search.md)** History filters and search | | | **done** |
| | [TASK-040912](tasks/TASK-040912-a-filter-renders-a-canonical-text-and-fingerprints-it.md) A filter renders one canonical line per axis, and fingerprints to eleven characters | S | **done** |
| | [TASK-040913](tasks/TASK-040913-the-cursor-payload-names-the-filter-it-was-drawn-under.md) The cursor payload names the filter it was drawn under, and a mismatch decodes to null | S | **done** |
| | [TASK-040914](tasks/TASK-040914-over-http-a-cursor-is-refused-under-any-filter-but-its-own.md) Over HTTP, a cursor is refused under every filter but the one that issued it | S | **done** |
| | [TASK-040915](tasks/TASK-040915-the-document-states-the-refusal-instead-of-promising-it.md) The document states the refusal instead of promising it | XS | **done** |
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
| | [TASK-040911](tasks/TASK-040911-the-document-contracts-both-filters-and-what-each-refuses.md) The document contracts both filters, and what each of them refuses | S | **done** |
| [STORY-0410](stories/STORY-0410-the-display-name-product-rules.md) The display-name product rules — screened when set, takeable away | | | done |
| | [TASK-041001](tasks/TASK-041001-the-migration-test-derives-its-version-list.md) The migration test derives its version list from the migrations it applies | XS | **done** |
| | [TASK-041002](tasks/TASK-041002-the-name-registry-its-guards-and-the-takedown.md) The fifth migration creates the name registry, its guards and the takedown function | S | **done** |
| | [TASK-041003](tasks/TASK-041003-a-name-is-registered-before-it-is-held.md) Setting a name registers it first, and a refused claim rolls the whole transaction back | S | **done** |
| | [TASK-041004](tasks/TASK-041004-three-fixtures-register-the-name-they-hand-a-player.md) Three fixtures register the name they hand a player | S | **done** |
| | [TASK-041005](tasks/TASK-041005-the-uniqueness-fixtures-register-every-name.md) The uniqueness fixtures register every name, and the fold refuses before the index does | S | **done** |
| | [TASK-041006](tasks/TASK-041006-the-permanence-fixtures-register-only-what-must-land.md) The permanence fixtures register only the names that must land | S | **done** |
| | [TASK-041007](tasks/TASK-041007-the-schema-test-keeps-its-refusals-raw.md) The display-name schema test registers what must land and keeps its refusals raw | S | **done** |
| | [TASK-041008](tasks/TASK-041008-the-fold-that-refuses-a-case-variant-is-the-registrys.md) The fold that refuses a case variant is the registry's, and the schema test says so | S | **done** |
| | [TASK-041009](tasks/TASK-041009-the-held-race-moves-to-the-registry-row.md) The held race moves to the registry row, and the probe that waits for it follows | S | **done** |
| | [TASK-041010](tasks/TASK-041010-a-display-name-may-only-be-a-registered-name.md) The sixth migration makes a display name a registered name or nothing | S | **done** |
| | [TASK-041011](tasks/TASK-041011-a-registered-name-is-never-released.md) A registered name is never released, and the only change it may take is TAKEN to RETIRED | S | **done** |
| | [TASK-041012](tasks/TASK-041012-the-takedown-is-one-function-call.md) retire_display_name takes the name away and leaves the profile unset | S | **done** |
| | [TASK-041013](tasks/TASK-041013-the-permanence-trigger-has-exactly-one-exception.md) The permanence trigger has exactly one exception, and it is a transition | S | **done** |
| | [TASK-041014](tasks/TASK-041014-a-takedown-moves-no-coin.md) A takedown moves no coin | XS | **done** |
| | [TASK-041015](tasks/TASK-041015-a-retired-name-is-spent-for-everybody.md) A retired name is spent for everybody, including the player it was taken from | S | **done** |
| | [TASK-041016](tasks/TASK-041016-a-blocked-name-is-refused-and-the-screen-fails-closed.md) A blocked name is refused when it is set, and the screen fails closed | S | **done** |
| | [TASK-041017](tasks/TASK-041017-the-port-test-builds-its-profile-through-the-builder.md) The port test builds its profile through the shared builder | XS | **done** |
| | [TASK-041018](tasks/TASK-041018-the-profile-says-the-name-was-removed.md) The profile says the name was removed, from one correlated EXISTS | S | **done** |
| | [TASK-041019](tasks/TASK-041019-two-players-in-one-database.md) Two players in one database, and only one of them reads true | S | **done** |
| | [TASK-041020](tasks/TASK-041020-a-takedown-is-invisible-to-everybody-else.md) A takedown is invisible to everybody else, and its two strings live where they should | S | **done** |
| | [TASK-041021](tasks/TASK-041021-the-document-contracts-the-removed-name.md) The protocol document contracts the removed-name field | XS | **done** |
| | [TASK-041022](tasks/TASK-041022-the-operations-document-is-the-only-call-site.md) docs/operations.md is the takedown's only call site | S | **done** |
| | [TASK-041023](tasks/TASK-041023-the-guard-that-closes-the-second-orphan-path-is-asserted.md) The guard that closes the second orphan path is asserted | XS | dropped |
| | [TASK-041024](tasks/TASK-041024-the-kotlin-half-of-fail-closed-is-isolated.md) The Kotlin half of fail-closed is isolated | XS | **done** |
| **[STORY-0411](stories/STORY-0411-the-name-in-the-client.md)** The name in the client — shown, and settable | | | **done** |
| | [TASK-041101](tasks/TASK-041101-one-fixture-builds-every-profile-a-test-uses.md) One fixture builds every profile and duel line a test uses | S | **done** |
| | [TASK-041102](tasks/TASK-041102-the-reads-tests-build-through-the-fixture.md) The strip's read tests build through the fixture | S | **done** |
| | [TASK-041103](tasks/TASK-041103-the-components-profiles-build-through-the-fixture.md) The component tests build their profiles through the fixture | XS | **done** |
| | [TASK-041104](tasks/TASK-041104-the-profile-read-carries-the-name-and-the-removal.md) The profile read carries the name, and whether one was removed | S | **done** |
| | [TASK-041105](tasks/TASK-041105-a-duel-line-carries-the-opponents-name-and-no-id.md) A duel line carries the opponent's name, and still not their id | S | **done** |
| | [TASK-041106](tasks/TASK-041106-one-put-sets-the-name-and-every-answer-is-its-own-outcome.md) One PUT sets the name, and every answer is its own outcome | S | **done** |
| | [TASK-041107](tasks/TASK-041107-the-words-the-name-surface-says.md) The words the name surface says, and which of them leave a way back | S | **done** |
| | [TASK-041108](tasks/TASK-041108-the-surface-shows-a-name-or-offers-to-set-one.md) The name surface shows the name, or offers to set one and says what that costs | S | **done** |
| | [TASK-041109](tasks/TASK-041109-the-surface-says-a-name-was-removed-only-when-it-was.md) The surface says a name was removed, only to the player it happened to | XS | **done** |
| | [TASK-041110](tasks/TASK-041110-the-surface-sends-once-and-shows-what-came-back.md) The surface sends once, and shows the name that came back | S | **done** |
| | [TASK-041111](tasks/TASK-041111-each-refusal-says-its-own-sentence.md) Each refusal says its own sentence, and only two leave the form | S | **done** |
| | [TASK-041112](tasks/TASK-041112-the-write-reaches-the-tree-the-read-already-does.md) The write reaches the tree the same way the read already does | XS | **done** |
| | [TASK-041113](tasks/TASK-041113-the-lobby-shows-the-name-surface-and-the-table-does-not.md) The lobby shows the name surface, and the duel table never does | XS | **done** |
| | [TASK-041114](tasks/TASK-041114-the-word-for-a-player-with-no-name.md) The word for a player who has no name | XS | **done** |
| | [TASK-041115](tasks/TASK-041115-the-strip-prints-the-players-own-name.md) The strip prints the player's own name, or what stands for none | XS | **done** |
| | [TASK-041116](tasks/TASK-041116-a-duel-line-names-the-opponent.md) A duel line names the opponent it was played against | XS | **done** |
| | [TASK-041117](tasks/TASK-041117-no-name-on-the-screen-is-built-from-a-player-id.md) No name on the screen is built from a player id, and a takedown is invisible | S | **done** |
| **[STORY-0412](stories/STORY-0412-the-account-screens.md)** The account screens — sign up, sign in, sign out, and which routes are live | | | **ready** — split into **27** on 2026-08-26, unblocked by [`ADR-0076`](../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) and split out of order because `STORY-0416`'s chain is stalled behind `DEC-076` and `STORY-0414`, `0415` and `0417` all trace through this one. **Two** account screens, which `ADR-0076` §1 left to this story: `#/account`, whose word is not coined (`ADR-0050` §3, `ADR-0036` and `ADR-0056` §2 each say *account* to a player), and a sign-in screen whose word the product does not yet say — raised as `DEC-077`, **the product owner's**, blocking `TASK-041226` and `TASK-041227` and nothing else. `TASK-041201` is the single startable ticket. Three things were found already settled and needed no decision: `ADR-0050` §4 makes `deviceRouteLive` the whole of what the screen reads, so no `hasCredential` field is asked for and *a credential exists* is derived from holding a session (sign-in is the only endpoint in `docs/protocol.md` that issues one); an identity change is a **document reload**, because `ADR-0075` records three presence fields cleared at no store boundary and `ADR-0076` §6 keeps two controls as page loads for that reason; and `ADR-0081` §1's first-segment rule fixes how `screen.ts` matches. One criterion is met in a different shape and it is written down rather than absorbed — *sign-out during a live duel warns first* becomes an **unconditional** warning, because `ADR-0076` §3's branch order makes the account screen unreachable while a frame has seated the tab, so a duel-conditional branch is one no fixture can reach |
| | [TASK-041201](tasks/TASK-041201-the-address-of-a-screen-is-a-pure-function-of-its-fragment.md) The address of a screen, as a pure function of its fragment | XS | **done** |
| | [TASK-041202](tasks/TASK-041202-the-hook-that-carries-the-address-and-the-trap-that-is-silent.md) The hook that carries the address, and the trap that makes a stale render look like React | S | **ready** |
| | [TASK-041203](tasks/TASK-041203-the-lobby-reads-the-address-instead-of-two-flags.md) The lobby reads the address instead of two flags, and Back stops leaving the client | S | backlog |
| | [TASK-041204](tasks/TASK-041204-the-store-outranks-the-address-and-the-address-stops-lying.md) The store outranks the address, and a seated player's address stops lying | S | backlog |
| | [TASK-041205](tasks/TASK-041205-the-token-this-browser-holds-lives-under-one-key.md) The session token this browser holds lives under one key | XS | backlog |
| | [TASK-041206](tasks/TASK-041206-hello-carries-the-session-and-the-device-id-still-never-moves.md) Hello carries the session this browser holds, and the device id still never moves | S | backlog |
| | [TASK-041207](tasks/TASK-041207-the-profile-carries-whether-the-device-route-is-live.md) The profile carries whether the device route is still live | S | backlog — `atomic:` at **4**, probed under `ADR-0070` |
| | [TASK-041208](tasks/TASK-041208-a-profile-body-with-no-device-route-is-not-a-profile.md) A profile body with no device route is not a profile | XS | backlog |
| | [TASK-041209](tasks/TASK-041209-a-fetch-that-carries-the-session-this-browser-holds.md) A fetch that carries the session this browser holds | S | backlog |
| | [TASK-041210](tasks/TASK-041210-every-me-read-goes-out-under-the-session.md) Every read under `/api/me` goes out under the session | S | backlog |
| | [TASK-041211](tasks/TASK-041211-the-words-the-account-screen-says.md) The words the account screen says, including the refusal that is about nobody | S | backlog |
| | [TASK-041212](tasks/TASK-041212-sign-up-and-the-refusal-that-is-about-nobody.md) Sign-up, seven outcomes, and the one refusal that is about nobody | S | backlog |
| | [TASK-041213](tasks/TASK-041213-sign-in-stores-the-token-and-one-answer-covers-both-refusals.md) Sign-in stores the token, carries no credential of its own, and reloads | S | backlog |
| | [TASK-041214](tasks/TASK-041214-sign-out-clears-the-token-and-only-the-token.md) Sign-out clears the token and only the token, leaves the room, and reloads | S | backlog |
| | [TASK-041215](tasks/TASK-041215-stopping-this-device-signing-in-and-the-two-refusals.md) Stopping this device signing in, and the two refusals that are not failures | S | backlog |
| | [TASK-041216](tasks/TASK-041216-the-four-account-calls-reach-a-screen-through-one-provider.md) The four account calls reach a screen through one provider | XS | backlog |
| | [TASK-041217](tasks/TASK-041217-the-account-screen-states-which-routes-sign-in.md) The account screen states which routes sign in to this profile, in both states | S | backlog |
| | [TASK-041218](tasks/TASK-041218-the-sign-up-form-on-the-account-screen.md) The sign-up form — one credential, and the strip is the same profile afterwards | S | backlog |
| | [TASK-041219](tasks/TASK-041219-a-throttled-sign-up-says-so-keeps-what-was-typed-and-retries-nothing.md) A throttled sign-up says so, keeps what was typed, and retries nothing | S | backlog |
| | [TASK-041220](tasks/TASK-041220-stopping-this-device-with-one-confirmation-and-three-facts.md) Stopping this device signing in, offered only where it is safe, with three facts first | S | backlog |
| | [TASK-041221](tasks/TASK-041221-signing-out-asks-first-and-says-what-it-costs.md) Signing out asks first, and says what it costs before it acts | S | backlog |
| | [TASK-041222](tasks/TASK-041222-the-account-screen-has-an-address-and-the-lobby-has-the-door.md) The account screen has an address, and the lobby has the door | S | backlog |
| | [TASK-041223](tasks/TASK-041223-the-account-calls-reach-the-real-transport.md) The account calls reach the real transport, and sign-in reaches it carrying nothing | S | backlog |
| | [TASK-041224](tasks/TASK-041224-no-secret-reaches-a-url-and-no-body-carries-a-player-id.md) No secret reaches a URL, and no request body carries a player id | S | backlog |
| | [TASK-041225](tasks/TASK-041225-the-sign-in-form.md) The sign-in form, and one sentence for both ways it can be refused | S | backlog |
| | [TASK-041226](tasks/TASK-041226-the-sign-in-screens-word-and-its-slug.md) The sign-in screen's word, and the address that word becomes | XS | **blocked** — `DEC-077` |
| | [TASK-041227](tasks/TASK-041227-the-sign-in-screen-at-its-address-and-the-door-to-it.md) The sign-in screen at its address, and the door to it from the account screen | S | **blocked** — `DEC-077` |
| **[STORY-0413](stories/STORY-0413-the-history-screen.md)** The history screen — pages, filters, search | | | **done** |
| | [TASK-041301](tasks/TASK-041301-a-filter-and-a-cursor-become-exactly-one-path.md) A filter and a cursor become exactly one path, and nothing else | S | **done** |
| | [TASK-041302](tasks/TASK-041302-one-page-of-the-record-and-the-cursor-that-names-the-next.md) One page of the record, and the cursor that names the next one | S | **done** |
| | [TASK-041303](tasks/TASK-041303-one-endpoint-keeps-one-parse.md) One endpoint keeps one parse — the strip's read delegates | XS | **done** |
| | [TASK-041304](tasks/TASK-041304-a-refused-cursor-restarts-the-walk-once.md) A refused cursor restarts the walk, once, and never reaches the player | S | **done** |
| | [TASK-041305](tasks/TASK-041305-the-words-the-history-screen-says.md) The words the history screen says, and the two empties that must differ | S | **done** |
| | [TASK-041306](tasks/TASK-041306-the-page-walk-is-a-reducer-that-appends.md) The page walk is a reducer that appends, and never sorts | S | **done** |
| | [TASK-041307](tasks/TASK-041307-a-new-filter-drops-the-cursor-and-the-rows.md) A new filter drops the cursor and the rows it belonged to | XS | **done** |
| | [TASK-041308](tasks/TASK-041308-the-screen-renders-the-page-in-the-order-it-arrived.md) The screen renders the page in the order it arrived, and derives no fact | S | **done** |
| | [TASK-041309](tasks/TASK-041309-four-states-and-the-two-empty-ones-differ.md) Four states, and the two empty ones say different things | S | **done** |
| | [TASK-041310](tasks/TASK-041310-another-page-is-offered-until-there-is-none.md) Another page is offered until the server names none, and then never asked for | S | **done** |
| | [TASK-041311](tasks/TASK-041311-the-outcome-filter-is-four-choices.md) The outcome filter is four choices, and choosing one starts a new walk | S | **done** |
| | [TASK-041312](tasks/TASK-041312-the-search-box-sends-the-term-the-player-typed.md) The search box sends the term the player typed, and nothing else | S | **done** |
| | [TASK-041313](tasks/TASK-041313-the-screen-a-player-can-actually-reach.md) The screen a player can actually reach, reading through the real transport | S | **done** |
| | [TASK-041314](tasks/TASK-041314-no-player-id-reaches-the-history-screen.md) No player id reaches the history screen, and the suite counts itself | S | **done** |
| | [TASK-041315](tasks/TASK-041315-the-show-more-button-says-what-the-copy-module-says.md) The show-more button says what the copy module says | XS | **done** |
| | [TASK-041316](tasks/TASK-041316-the-app-test-hands-the-history-read-a-storage.md) The App test hands the history read a Storage | XS | **done** |
| [STORY-0414](stories/STORY-0414-claimed-here-recovered-there.md) Claimed here, recovered there, end to end | | | backlog |
| [STORY-0415](stories/STORY-0415-the-offer-after-a-first-win.md) The offer — an account after a first win, dismissed for good | | | backlog |
| [STORY-0416](stories/STORY-0416-the-recovery-email-and-the-password-reset.md) The recovery email, verified, and the password reset | | | **in progress** — split into **29** on 2026-08-25, out of numerical order because it depends only on `STORY-0405`, and now **41**: `TASK-041627` was re-cut into six, `ADR-0078` added `TASK-041635`, `ADR-0079`'s named defect added `TASK-041636` and `TASK-041637`, `TASK-041606`'s landing Notes added `TASK-041638`, `TASK-041614`'s landing Notes added `TASK-041639` and `TASK-041640`, and `TASK-041641` took the two database tests `TASK-041616` named but could not carry inside an `atomic:` table no gate names that file in. **Nothing waits on a decision, and `EPIC-04` now has none open at all** — `DEC-074` was answered on 2026-08-25 by [`ADR-0080`](../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md), so the password is judged before the token is touched, and `DEC-075` the same day by [`ADR-0081`](../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md), so a mailed link is a fragment route and the token is the segment behind the slug. `TASK-041617` is the single startable ticket. `DEC-073` was answered on 2026-08-25 by [`ADR-0079`](../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md) — ten a minute for `forgot-password`, five for `recovery-email`, an over-budget attempt still counting, and the placement in each handler — and `DEC-071` and `DEC-072` were answered the same day by [`ADR-0078`](../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md) and [`ADR-0077`](../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md), unblocking four. `DEC-072` was answered on 2026-08-25 by [`ADR-0077`](../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md) — the mail seam, its scope and lifetime, its failure semantics, `baseUrl`, and what a test can await. **Nothing is the human's**: `ADR-0031` §7 already defers the transport, and therefore any bill, to `EPIC-07` |
| | [TASK-041601](tasks/TASK-041601-three-tables-that-cannot-become-a-mailing-list.md) Three tables that cannot become a mailing list | S | done |
| | [TASK-041602](tasks/TASK-041602-two-strangers-may-both-claim-one-address-and-nothing-cascades.md) Two strangers may both claim one address, and nothing cascades | S | done |
| | [TASK-041603](tasks/TASK-041603-an-address-that-redacts-itself.md) An address that redacts itself | XS | done |
| | [TASK-041604](tasks/TASK-041604-two-tokens-minted-the-way-a-session-token-is.md) Two tokens, minted the way a session token is | S | done |
| | [TASK-041605](tasks/TASK-041605-one-digest-for-both-recovery-tokens.md) One digest for both recovery tokens | XS | done |
| | [TASK-041606](tasks/TASK-041606-a-port-that-can-send-exactly-two-mails.md) A port that can send exactly two mails | S | done |
| | [TASK-041607](tasks/TASK-041607-the-port-where-a-pending-address-and-a-proven-one-both-live.md) The port where a pending address and a proven one both live | S | done |
| | [TASK-041608](tasks/TASK-041608-a-second-claim-replaces-the-first-in-one-transaction.md) A second claim replaces the first, in one transaction | S | done |
| | [TASK-041609](tasks/TASK-041609-the-first-to-verify-takes-the-address.md) The first to verify takes the address | S | done |
| | [TASK-041610](tasks/TASK-041610-a-pending-address-answers-exactly-as-an-unknown-one.md) A pending address answers exactly as an unknown one | S | done |
| | [TASK-041611](tasks/TASK-041611-erasing-an-address-is-one-statement-and-so-is-forgetting-a-stale-one.md) Erasing an address is one statement, and so is forgetting a stale one | S | done |
| | [TASK-041612](tasks/TASK-041612-the-existing-ticker-forgets-unproven-addresses-too.md) The existing ticker forgets unproven addresses too | S | done |
| | [TASK-041613](tasks/TASK-041613-one-live-reset-token-and-a-quarter-hour-of-silence.md) One live reset token, and a quarter hour of silence | S | done |
| | [TASK-041614](tasks/TASK-041614-one-statement-spends-the-token-and-the-same-transaction-ends-every-session.md) One statement spends the token, and the same transaction ends every session | S | done |
| | [TASK-041615](tasks/TASK-041615-a-session-holder-proves-the-password-they-already-have.md) A session holder proves the password they already have | S | done |
| | [TASK-041616](tasks/TASK-041616-the-profile-says-recovery-is-on-and-never-what-the-address-is.md) The profile says recovery is on, and never what the address is | S | done |
| | [TASK-041617](tasks/TASK-041617-five-endpoints-and-a-field-written-down.md) Five endpoints and a field, written down | S | done |
| | [TASK-041618](tasks/TASK-041618-a-token-from-the-mailbox-proves-the-address.md) A token from the mailbox proves the address | S | done |
| | [TASK-041619](tasks/TASK-041619-three-ways-to-fail-verification-and-one-answer-for-all-of-them.md) Three ways to fail verification, and one answer for all of them | S | done |
| | [TASK-041620](tasks/TASK-041620-a-reset-takes-a-token-in-a-body-and-never-in-a-url.md) A reset takes a token in a body, and never in a URL | S | done |
| | [TASK-041621](tasks/TASK-041621-two-submissions-of-one-link-and-only-one-of-them-works.md) Two submissions of one link, and only one of them works | S | done |
| | [TASK-041622](tasks/TASK-041622-a-reset-signs-you-out-everywhere-including-here.md) A reset signs you out everywhere, including here | S | done |
| | [TASK-041623](tasks/TASK-041623-taking-the-address-back-costs-the-password.md) Taking the address back costs the password | S | done |
| | [TASK-041624](tasks/TASK-041624-which-strings-are-an-address.md) Which strings are an address | S | done |
| | [TASK-041625](tasks/TASK-041625-attaching-an-address-costs-the-current-password.md) Attaching an address costs the current password | S | done |
| | [TASK-041626](tasks/TASK-041626-four-different-things-happen-and-the-caller-reads-the-same-answer.md) Four different things happen, and the caller reads the same answer | S | **ready** |
| | [TASK-041627](tasks/TASK-041627-a-sender-that-sends-nothing.md) A sender that sends nothing | S | done |
| | [TASK-041628](tasks/TASK-041628-two-budgets-that-say-nothing-when-they-refuse.md) Two budgets that say nothing when they refuse | S | backlog |
| | [TASK-041629](tasks/TASK-041629-a-good-token-and-a-password-the-policy-refuses.md) A good token, and a password the policy refuses | S | backlog |
| | [TASK-041630](tasks/TASK-041630-a-decorator-that-detaches-over-the-same-port.md) A decorator that detaches, over the same port | S | backlog |
| | [TASK-041631](tasks/TASK-041631-a-failed-send-stays-inside-the-scope-and-names-a-class.md) A failed send stays inside the scope, and its log line names a class | S | backlog |
| | [TASK-041632](tasks/TASK-041632-the-origin-every-recovery-link-is-built-from-is-configuration.md) The origin every recovery link is built from is configuration | S | backlog |
| | [TASK-041633](tasks/TASK-041633-one-function-builds-both-recovery-links-and-no-header-reaches-it.md) One function builds both recovery links, and no header reaches it | S | backlog |
| | [TASK-041634](tasks/TASK-041634-a-build-with-no-sender-is-a-valid-build.md) A build with no sender is a valid build | S | backlog |
| | [TASK-041635](tasks/TASK-041635-the-fold-the-address-index-depends-on-written-down-in-the-catalog.md) The fold the address index depends on, written down in the catalog | S | done |
| | [TASK-041636](tasks/TASK-041636-the-attach-path-gets-the-quarter-hour-of-silence-the-reset-path-has.md) The attach path gets the quarter hour of silence the reset path has | S | done |
| | [TASK-041637](tasks/TASK-041637-the-second-attach-in-a-quarter-hour-is-answered-the-same-and-mails-nothing.md) The second attach in a quarter hour is answered the same, and mails nothing | S | done |
| | [TASK-041638](tasks/TASK-041638-the-shape-gate-holds-for-four-more-shapes-and-names-the-one-it-cannot.md) The shape gate holds for four more shapes, and names the one it cannot | S | backlog |
| | [TASK-041639](tasks/TASK-041639-a-reset-that-cannot-write-the-password-spends-no-token.md) A reset that cannot write the password spends no token | XS | **done** |
| | [TASK-041640](tasks/TASK-041640-a-failure-between-the-password-and-the-sessions-undoes-both.md) A failure between the password and the sessions undoes both | S | **done** |
| | [TASK-041641](tasks/TASK-041641-the-profiles-recovery-flag-is-that-players-and-a-pending-address-is-not-one.md) The profile's recovery flag is that player's, and a pending address is not one | XS | done |
| [STORY-0417](stories/STORY-0417-the-recovery-screens.md) The recovery screens — attach an address, and reset a password | | | backlog |

`STORY-0416`'s rows are in **id order**, and `depends_on` is the **sequence**; since `ADR-0077` and
`ADR-0078` landed the two no longer coincide. `TASK-041627` was re-cut into six — `ADR-0077`
§Consequences said it would be — and one edge moved: `TASK-041627` now ships `NoRecoveryMailer` and
runs **before** `TASK-041625`, because `TASK-041626`'s no-sender case binds that object and a seam
has to exist before its consumers. The chain is `…041608 → 041636 → 041609 → …041624 → 041627 →
041625 → 041637 → 041626 → 041630 → 041631 → 041632 → 041633 → 041634 → 041628 → 041629`.
`TASK-041635` and `TASK-041638` hang off merged tickets and are independent of all of it:
`TASK-041635` gates `V8`'s `COLLATE "und-x-icu"`, the clause `TASK-041601` proved no behaviour of
its own could reach, and nothing else in the story touches `RecoveryEmailSchemaTest.kt`;
`TASK-041638` is the only other ticket that touches `RecoveryMailerShapeTest.kt`; and
`TASK-041639 → TASK-041640` hang off the merged `TASK-041614`, are the only tickets that touch
`PostgresPasswordResetsConsumeTest.kt`, and change no production code at all. The count was
**still 38** after `ADR-0080` and `ADR-0081` — both are corrections to tickets that already existed,
and neither adds or removes one — and is **41** since those last two and `TASK-041641`, which is
the only ticket in the story that touches `PostgresProfileReadsTest.kt` and, like `041639` and
`041640`, changes no production code at all. **`V3`'s
`player_display_name_unique` carries the identical collation clause and is gated by nothing**; it is
not this story's and is deliberately not filed here — see the unfiled ticket under `EPIC-07` above.

**`ADR-0079` §Consequences named a defect against three of these tickets rather than raising a
`DEC`, and it is folded in as of 2026-08-25.** `ADR-0031` §5's fifteen-minute resend suppression —
*"a mail is sent only if the player has no live token issued within the last 15 minutes, read from
`issued_at`"* — names both endpoints and the split built it on one, so a verification mail would
have followed **every** successful attach for ever, leaving the attach budget as the only cap on
the mail it causes. Three tickets were amended and two added. `TASK-041607` now declares
`claimPending: ClaimPendingResult` (`Claimed` / `Suppressed`) instead of `Unit` — it is a port
declaration whose `verify:` is `compileKotlin`, so the type it fixes is what every later ticket
branches on, and the window in which it is free to be wrong closes at the next ticket.
`TASK-041608` answers `Claimed` unconditionally and now advances its fixture clock sixteen minutes
between its two claims, so the ticket that adds the rule moves none of its assertions. **The check
sits inside the writing transaction, not in the route** (`TASK-041636`): a handler judging the
window would need the `issued_at` of a *pending* address, which is exactly the port member
`TASK-041607` refuses under §3, and it would be a read-then-write window in which two concurrent
attaches both write and both mail. `TASK-041637` then makes the handler mail only on `Claimed`.
`ADR-0079`'s deadline binds the pair: **before `EPIC-07` configures a sender.**

**`ADR-0080` and `ADR-0081` were folded in on 2026-08-25, adding no ticket and moving no status.**
`ADR-0080` reaches four. `TASK-041629` gains the policy check between decode and `consume` and
**loses a test it can no longer state**: `aBadTokenStillAnswersFourHundredNotFourHundredAndTwentyTwo`
asserts the order the ADR reverses, since a fabricated token with a 7-code-point password now
answers `422`. It is replaced by the sharper property the ADR names — *the `422` for a fabricated
token and the `422` for a live one are indistinguishable* — which is a **negative**, and a test that
two things are the same passes trivially when both are broken alike; so that test asserts the
equality, asserts each status **is** `422`, and closes with a `204` on the minted token, and its
Proof breaks those three one at a time. `aRefusedPasswordLeavesTheTokenAsTheDecisionSays` resolves
to *a second request with the same token and an 8-code-point password answers `204`* and is renamed
for it. `TASK-041620` **stands** — same three-step shape, same four tests, same criteria — plus one
fixture constraint that is ungated in its own ticket and is stated with its Proof step saying so:
every request in `ResetPasswordRouteTest`, **including the two that expect `400`**, carries a
`newPassword` of 8 to 128 code points, or `TASK-041629`'s *passes unchanged* becomes unsatisfiable
the day the check lands in front of the lookup. `TASK-041617` transcribes `ADR-0080` §2's corrected
sentence rather than `ADR-0031` §5's parenthetical, and `TASK-041614`'s *Out of scope* had pointed
the password policy at `TASK-041620` where it meant `TASK-041629`. `ADR-0081` reaches one:
`TASK-041633`'s two literals become `"$baseUrl/#/reset/$token"` and `"$baseUrl/#/verify/$token"`,
while its no-`?` criterion, its no-encoder refusal, its `Host`-header sweep and four of its six tests
survive verbatim — which is `ADR-0081` §2's point rather than luck, because the token in
`#/reset/abc` is a path segment **of the fragment** and is still never transmitted, never logged and
never in a `Referer`. The rule stays *a recovery link contains no `?`*, absolute and one character
wide, which is exactly what `neitherLinkCarriesTheTokenInAQueryString` already gates; a query section
would make it conditional and the edit that breaks it would be two deleted characters in a string
that still looks ordinary.

**Stale *blocked on `DEC-0NN`* prose was swept at the same time**, from thirteen ticket bodies whose
`status:` had moved at landing while the prose did not: `TASK-041613`, `041617`, `041618`, `041619`,
`041620`, `041622`, `041623`, `041625`, `041626`, `041628`, `041629`, `041632` and `041633`. Three
of those were `## Blocked` sections rather than cross-references — `TASK-041626` (`DEC-072`),
`TASK-041628` (`DEC-073`) and `TASK-041629` (`DEC-074`) — and each is now an `## Unblocked` section
carrying the answer it was waiting for, in `TASK-041625`'s already-merged idiom, including the note
that the `blocked` label in the front matter is a historical marker and the `status:` is not
`blocked`. **`TASK-041624` was deliberately left alone at the time**, because it is a *shell*: its two
fixture tables were empty by design and were written to be filled from `ADR-0078` §6, so the fold-in
was a re-plan of its Tests rather than a prose correction, and half of it would have read as all of
it.

**That fold-in is done.** Both tables now carry §6's seven accepted and seven refused strings
verbatim, the ticket is `S` rather than `XS`, and it grew a **fourth test**, which is the part worth
recording. A rule that refuses almost nothing is easy to gate vacuously: a table-driven test over the
answering ADR's own fixtures is passed by an implementation that is *a lookup of those fixtures*, in
either direction — `raw in ACCEPTED` and `raw !in REFUSED` each satisfy every entry of both tables,
and neither is the rule. `theRuleIsAppliedToStringsInNoFixtureTable` asserts eight strings that appear
in neither table and in the ADR nowhere, one per clause plus both sides of the 254-code-point ceiling,
and Proof steps 5 and 6 are the two lookups, each predicted to redden that test **alone**. Its eighth
string is astral — 135 code points and 259 UTF-16 units — and is the only fixture in the file that
tells `codePointCount` from `String.length`, since every string `ADR-0078` §6 names is BMP; step 8 is
that mutation. Nothing about the answer moved: the rule, the `400`, the empty body and the refusal to
canonicalise are §1, §4 and §5 as merged.

**`TASK-041614` landed with the same shape of gap, and it is now two tickets rather than a note.** Its
title claims *the same transaction ends every session*, and **splitting the transaction at either
boundary leaves all five of its tests green** — the coder found it and declined to add a sixth test
its Tests table did not name, and the reviewer reproduced both splits. `TASK-041639` closes the first
boundary with the fixture the reviewer built and verified: a live reset token for a player with **no**
`password` credential, which `V8`'s foreign key to `player` alone makes reachable. Under the shipped
atomic code the credential `UPDATE` affects zero rows, the token delete rolls back with it, and the
same token still works once the credential exists; under a `commit()` placed after the token spend it
does not. `TASK-041640` closes the second with a wrapped `DataSource`, because the session delete is
unconditional but for `player_id` and **no data fixture can make it fail** — the harness is
`PostgresDeviceBindingsTest`'s, already merged and already proven, and the ticket names the
duplicate-JVM-class-name trap that copying it into the same package would otherwise spring. Its
wrapper aims by SQL rather than by call index and records what it hit, because a rollback at either
boundary leaves the identical end state and **no assertion made after the fact can tell the two
apart**; that is its Proof step 2, and it predicts one assertion reddening rather than the whole test.
Both tickets say in as many words that `rollback()` → `commit()` is **not** the mutation to try:
Postgres downgrades a `COMMIT` on an aborted transaction to a rollback server-side, so it reddens
nothing and a green run after it reads as evidence of an inert test. Neither ticket changes a line of
production code; if either fails against `develop` unmutated, that is a defect and a third ticket.

Why this was worth two tickets when `TASK-041608`'s identically-shaped gap was worth a note: there, a
split leaves an *identical* end state, since a `DELETE` plus an `INSERT` has nothing to roll back.
Here a split leaves a state **strictly worse than no reset at all** — the owner believes they have
recovered, the attacker stays signed in, and the `204` says nothing — and no later ticket in the
chain would incidentally re-exercise it.

`TASK-041638` closes the other half of a promise `TASK-041606` landed with. Its
`RecoveryMailerShapeTest` asserts over `declaredMemberFunctions`, exactly as its Scope named, and
the reviewer then probed four shapes that all pass with every test green — a property, a nested
type, a companion member, and the port **extending a second interface**, since *declared* excludes
inherited members. `ADR-0031` §6.2 makes that test the mechanism carrying the vision's *"Never used
for contact or marketing"*, and that promise has two halves: *in a diff a reviewer reads*, which
holds for all four, and *a test asserts it*, which did not. Probing for this ticket found a **fifth**
shape none of the four covered — a **member extension function**, which `declaredMemberFunctions`
excludes by definition. The ticket asserts four surfaces (`declaredMemberProperties`,
`declaredMemberExtensionFunctions`, `nestedClasses`, `supertypes`), uses one control type carrying
all four forbidden shapes at once because four assertions that something is empty all pass when the
reflection is wrong, and **names in the file's own KDoc the surface it deliberately does not
assert**: a *top-level* extension function on `RecoveryMailer`, which reads at a call site exactly
like a member and is invisible to every read over the `KClass`. Closing that needs a classpath scan
and a new dependency; the residual is bounded, since an extension can only re-aim one of the two
permitted templates and cannot introduce a third.

`STORY-0413` was split on 2026-08-19 into **fourteen**, on top of what `STORY-0409` and `STORY-0411`
actually landed, and eleven of them are unblocked. The read grows a second function rather than a
wider one — `readDuelPage` carries the query, the `nextCursor` the client has been discarding since
`STORY-0408` shipped it, and `ADR-0057` §5's one-shot restart, while `readRecentDuels` keeps its
signature so the lobby strip stays outside this story's blast radius; `TASK-041303` then deletes the
duplicate parse, so the endpoint keeps one. The page walk is a pure reducer whose `filtered` event
answers `initialHistory`, which is what makes *"a cursor cannot outlive the filter that produced
it"* a property rather than a rule somebody remembers. The suite's own count is asserted in
**one** ticket (`TASK-041314`, at 472) with the arithmetic that produced it, because during
`STORY-0411` a mid-story count change forced four tickets to be corrected at once.

Two decisions were raised, **both the product owner's**, each blocking exactly one ticket at the end
of that chain and nothing before it, and **both were answered on 2026-08-19**. `DEC-052` — when the
history search fires — is answered by
[`ADR-0059`](../docs/adr/ADR-0059-the-record-is-searched-when-the-player-submits.md): **on submit**,
by Enter or by a button reading *Search*, and typing sends nothing. `ADR-0057` binds the cursor to
its filter, so a debounced pause would not waste a request — it would throw away the player's place
in the record and search a term they had not finished typing. `DEC-053` — how a player reaches the
record and leaves it — is answered by
[`ADR-0060`](../docs/adr/ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md): **its
own screen**, replacing the first screen, in by one control reading *Your duels* beneath the strip
and out by one reading *Back*, offered only where a player is not in a duel. The lobby does not grow
a section, because `STORY-0412`, `STORY-0415` and `EPIC-05` all inherit whatever this got, and four
stacked sections are the whole product on the screen a new player sees first. It **raised `DEC-054`**
— URL routes and a working browser *Back*, the architect's — which blocks nothing today.

`TASK-041312` and `TASK-041313` stay **blocked** until a planner transcribes what the two ADRs name
into them: for `TASK-041312`, one Scope bullet, the word `SEARCH`, the test `asks nothing while the
player types, and once when the search is submitted` and its `verify:` line (`ADR-0059` §5); for
`TASK-041313`, the door, the way back and the test `leaves the first screen for the record, and comes
back to it` (`ADR-0060` §7). Both stay at three files, and **`TASK-041314`'s 472 does not move** —
its arithmetic already budgeted one ADR-named test in each of the two.

Two decisions were raised while splitting, both the architect's, both blocking exactly one story.
`DEC-041` (the shape of device revocation) is answered by
[`ADR-0049`](../docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md) — the edge
leaves `player` for its own `device_binding` table, so `ADR-0030` §2 gains no fourth writer because
the column it protected no longer exists, and revoking is final in the database rather than by
convention. It unblocks `STORY-0406` and raised `DEC-045`, since answered by
[`ADR-0050`](../docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md):
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

---

## EPIC-05 — Ranking, duel coins and leaderboard

Written on 2026-08-19, parked the same day, and **unparked the same day** by
[`ADR-0061`](../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md), which
answers `DEC-055`: a season is one calendar month in UTC, derived and never stored; the ladder is a
window over it; a boundary does nothing, and `player.coin_balance` is never reset. The epic is v0.3 —
[`docs/vision.md`](../docs/vision.md)'s *"Leaderboard and seasons"* — and the coin it ranks by is
already paid: [`ADR-0014`](../docs/adr/ADR-0014-duel-coin-economy.md) chose a signed `wins − losses`
balance over a win count *for this epic*, in as many words, and
[`ADR-0029`](../docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md) kept a deterministic
collation so this leaderboard would have an index. What was missing was not code but a definition,
and `ADR-0061` supplies it — at the price of the ladder's number no longer being the profile strip's
number, which the epic's own non-negotiables had forbidden and which that ADR contradicts on purpose,
in writing, with both affected stories rewritten in the same change.

It needs none of `EPIC-04`'s credential chain —
[`ADR-0036`](../docs/adr/ADR-0036-an-account-is-offered-never-required.md) makes an anonymous
profile fully ranked — so the six stories still open there gate nothing here. It does inherit
[`ADR-0060`](../docs/adr/ADR-0060-the-record-is-its-own-screen-and-the-lobby-is-the-door.md): the
ladder is its own screen and its door is the **fifth** control on the first screen, which is the
crowding that ADR predicted in advance. It strengthens the case for `DEC-054` without answering it —
a row that leads to another player is a *link*, and a client with no addresses cannot express one.

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| [STORY-0501](stories/STORY-0501-a-season-is-a-bounded-thing.md) A season is a bounded thing, and every finished duel belongs to one | | | **done** — 6 tickets |
| | [TASK-050101](tasks/TASK-050101-a-season-is-a-year-and-a-month.md) A season is a year and a month, and its identifier is `2026-08` | XS | **done** |
| | [TASK-050102](tasks/TASK-050102-a-seasons-bounds-are-half-open.md) A season's bounds are half-open, and December ends in January | S | **done** |
| | [TASK-050103](tasks/TASK-050103-the-season-an-instant-falls-in.md) The season an instant falls in, in UTC, whatever the reader's clock says | S | **done** |
| | [TASK-050104](tasks/TASK-050104-a-duel-belongs-to-the-season-it-finished-in.md) A duel belongs to the season it finished in, never the one it started in | XS | **done** |
| | [TASK-050105](tasks/TASK-050105-nothing-here-moves-a-coin.md) Nothing this story adds moves a coin, writes a migration, or reaches the engine | S | **done** |
| | [TASK-050106](tasks/TASK-050106-the-current-season-from-an-injected-clock.md) The current season, read from an injected clock and never from a system clock | S | **done** |
| [STORY-0502](stories/STORY-0502-the-standings-read-path.md) The standings read path — ordered, paged, and a rank the server computes | | | **done** — 19 tickets |
| | [TASK-050201](tasks/TASK-050201-the-composition-root-owns-the-wall-clock.md) The composition root owns the one wall clock, and no component mints its own | XS | **done** |
| | [TASK-050202](tasks/TASK-050202-a-standings-cursor-carries-the-walks-cutoff.md) A standings cursor carries the walk's cutoff, and one from another season does not decode | S | **done** |
| | [TASK-050203](tasks/TASK-050203-the-wire-shape-a-row-a-season-and-a-self-standing.md) The wire shape — a row, the season, and a self standing that is never a zero | S | **done** |
| | [TASK-050204](tasks/TASK-050204-the-port-and-the-query-one-ordered-page.md) The port and the query — one ordered page, narrowed by nothing else | S | **done** |
| | [TASK-050205](tasks/TASK-050205-tied-players-share-a-rank-and-a-rank-is-not-an-offset.md) Tied players share a rank, a rank is not a row's offset, a tie may span a boundary | S | **done** |
| | [TASK-050206](tasks/TASK-050206-the-ladder-is-results-not-players.md) The ladder is results, not players — a draw earns a row, one duel is enough | S | **done** |
| | [TASK-050207](tasks/TASK-050207-the-window-not-the-column-and-a-season-sums-to-zero.md) The number is the window and not the column, and a season sums to exactly zero | S | **done** |
| | [TASK-050208](tasks/TASK-050208-the-port-answers-one-players-own-standing.md) The port answers one player's own standing, against the whole ladder | S | **done** |
| | [TASK-050209](tasks/TASK-050209-the-route-answers-a-page-and-pins-the-walk.md) The route answers a page, names its season, and pins the walk to one cutoff | S | **done** |
| | [TASK-050210](tasks/TASK-050210-the-page-the-route-serves-and-the-self-it-carries.md) The probe row, the last page, the empty ladder, and the self object's three shapes | S | **done** |
| | [TASK-050211](tasks/TASK-050211-the-routes-refusals-a-bad-limit-a-bad-cursor-and-last-months-walk.md) The route's refusals — a bad limit, a bad cursor, and a walk from last month | S | **done** |
| | [TASK-050212](tasks/TASK-050212-the-shipped-server-installs-the-ladder-route.md) The shipped server installs the ladder route, on the wall clock the root owns | XS | **done** |
| | [TASK-050213](tasks/TASK-050213-over-http-every-player-once-and-page-twos-ranks.md) Over HTTP — every player exactly once, and page two's ranks are the ladder's | S | **done** |
| | [TASK-050214](tasks/TASK-050214-a-duel-that-commits-mid-walk-is-in-no-page-of-it.md) A duel stamped at the cutoff is in no page of the walk, and the ranks stay the cutoff's | S | **done** |
| | [TASK-050215](tasks/TASK-050215-the-named-exception-and-the-walk-that-sees-it.md) The named exception — the loser twice, the winner never, and a new walk that sees both | S | **done** |
| | [TASK-050216](tasks/TASK-050216-the-response-tells-a-player-where-they-stand.md) The response tells a player where they stand, on the page drawn and off it | S | **done** |
| | [TASK-050217](tasks/TASK-050217-three-answers-one-page-and-a-read-that-creates-nothing.md) Three answers about the reader, one page for everybody, a read that creates nothing | S | **done** |
| | [TASK-050218](tasks/TASK-050218-the-document-contracts-the-ladder-and-its-promise.md) The document contracts the ladder — every parameter, the promise, and both refusals | S | **done** |
| | [TASK-050219](tasks/TASK-050219-nothing-stores-a-standing.md) Nothing stores a standing — no table, no column, no view, and no migration | XS | **done** |
| [STORY-0503](stories/STORY-0503-the-ladder-is-a-screen.md) The ladder is a screen, reached from the first screen and left by one control | | | **done** — 15 tickets |
| | [TASK-050301](tasks/TASK-050301-a-row-and-a-page-parse-in-the-order-they-arrived.md) A ladder row parses, and the page keeps the order it arrived in | S | **done** |
| | [TASK-050302](tasks/TASK-050302-the-self-standing-two-numbers-two-nulls-or-nothing.md) The self standing is two numbers, two nulls, or nothing — and it carries no player id | S | **done** |
| | [TASK-050303](tasks/TASK-050303-the-words-the-ladder-says-and-a-season-named-without-a-clock.md) The words the ladder says — a row line, and a season named without a clock | S | **done** |
| | [TASK-050304](tasks/TASK-050304-the-read-the-device-id-is-optional-and-there-is-no-401.md) The ladder read — the device id is optional here, and there is no 401 to fear | S | **done** |
| | [TASK-050305](tasks/TASK-050305-the-walk-appends-and-a-failed-request-un-reads-nothing.md) The ladder walk is a reducer that appends, and a failed request un-reads nothing | S | **done** |
| | [TASK-050306](tasks/TASK-050306-the-season-and-the-self-standing-are-the-first-pages.md) The season and the self standing are the first page's, and later pages do not move them | S | **done** |
| | [TASK-050307](tasks/TASK-050307-the-screen-prints-the-page-in-the-order-it-arrived.md) The screen asks for the first page and prints it in the order it arrived, ranks and all | S | **done** |
| | [TASK-050308](tasks/TASK-050308-the-screen-filters-nothing.md) The screen filters nothing — a nameless row and a negative standing are ordinary rows | XS | **done** |
| | [TASK-050309](tasks/TASK-050309-the-screen-names-the-season-the-response-carried.md) The screen names the season the response carried, and its four states are four sentences | S | **done** |
| | [TASK-050310](tasks/TASK-050310-the-self-lines-two-sentences-and-no-number-where-there-is-no-place.md) The self line has two sentences, and the one for no place prints no number at all | S | **done** |
| | [TASK-050311](tasks/TASK-050311-the-self-line-above-the-rows-for-a-player-on-no-page-drawn.md) The self line sits above the rows, and states a standing for a player on no page drawn | S | **done** |
| | [TASK-050312](tasks/TASK-050312-another-page-appends-and-page-twos-ranks-are-the-ladders.md) Another page appends, page two's ranks are the ladder's, and the self line does not move | S | **done** |
| | [TASK-050313](tasks/TASK-050313-a-flat-ladder-is-an-ordinary-ladder-and-a-tie-is-marked-by-nothing.md) A flat ladder is an ordinary ladder, a tie is marked by nothing, and no row leads anywhere | S | **done** |
| | [TASK-050314](tasks/TASK-050314-the-screen-a-player-can-reach-one-control-in-one-control-back.md) The ladder is a screen a player can reach — one control in, one control back | S | **done** |
| | [TASK-050315](tasks/TASK-050315-the-door-survives-a-failed-profile-read-and-stands-down-for-a-duel.md) The door survives a failed profile read, and stands down for a duel in progress | S | **done** |
| [STORY-0504](stories/STORY-0504-what-a-row-leads-to.md) What a row leads to — another player, seen by a stranger | | | **dropped** — `ADR-0067`: a row leads nowhere |
| [STORY-0505](stories/STORY-0505-a-season-ends-and-the-record-survives-it.md) A season ends, and the record survives it | | | **dropped** — `ADR-0061` §5: a boundary does nothing, so there is no crossing to write |
| [STORY-0506](stories/STORY-0506-a-duel-moves-a-rank.md) A duel moves a rank, end to end | | | **done** — 9 tickets |
| | [TASK-050601](tasks/TASK-050601-the-ladder-two-seated-players-and-no-place-yet.md) The ladder, read from the same application the duel is played in — two seated players and no place yet | S | **done** |
| | [TASK-050602](tasks/TASK-050602-the-ladder-the-duel-arrives-into-a-shared-rank-a-skipped-one-and-last-month-left-out.md) The ladder the duel arrives into — a shared rank, a skipped one, and last month left out | S | **done** |
| | [TASK-050603](tasks/TASK-050603-a-played-duel-moves-two-standings-by-one-each-and-nobody-elses.md) A played duel moves two standings by one each — measured as a difference, on a ladder that was never zero | S | **done** |
| | [TASK-050604](tasks/TASK-050604-the-winner-overtakes-the-loser-on-a-ladder-that-had-them-the-other-way-round.md) The winner overtakes the loser on a ladder that had them the other way round | S | **done** |
| | [TASK-050605](tasks/TASK-050605-every-standing-is-that-players-own-season-results.md) Every standing on the ladder is that player's own season results, and the ladder totals zero | S | **done** |
| | [TASK-050606](tasks/TASK-050606-the-players-own-place-after-the-duel-on-the-page-and-off-it.md) The player's own place after the duel — served on the page drawn and off it | S | **done** |
| | [TASK-050607](tasks/TASK-050607-a-player-whose-only-duel-was-a-loss-has-a-row-and-it-reads-minus-one.md) A player whose only duel was a loss has a row, and it reads minus one | S | **done** |
| | [TASK-050608](tasks/TASK-050608-the-ladder-and-the-profile-strip-agree-here-and-part-company-for-an-older-record.md) The ladder and the profile strip agree for both duellists — and part company for an older record | S | **done** |
| | [TASK-050609](tasks/TASK-050609-a-draw-moves-nobody-and-is-still-two-rows.md) A draw moves nobody, and is still two rows and two places | S | **done** |

`STORY-0506` is the only one whose acceptance criteria were writable on the day the epic was
written, because they depend on no open decision: a duel played through the socket moves the winner
`+1` and the loser `−1`, asserted as a difference rather than as a value, and the coins on the
ladder sum to the `coin_delta`s stored for the same scope. **Critical path:**
`0501 → 0502 → 0503 → 0506`, and it now begins with a ticket rather than a decision. `STORY-0505`'s
one assertion no other story owned — *the ladder read for a season returns only that season's duels*
— moved to `STORY-0502` when it was dropped, so nothing is lost with it, and the epic loses its only
genuine parallel pair along with it.

The two architect questions the epic named and deliberately did not number became **`DEC-061`**, a
single decision rather than two: `ADR-0061` supplied their missing premise and coupled them, because
whether a standing is materialised decides what a page can guarantee over an ordering that is
recomputed while it is walked. Both are **answered** by
[`ADR-0066`](../docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md) —
computed per request, and a walk pinned to the instant it began — so `STORY-0502` raises nothing at
split time and is gated by nothing.

`STORY-0502` was split on 2026-08-21 into **nineteen** tickets, strictly linear: the run is
sequential and six of them accumulate tests in one file each. It begins with `ADR-0062` §7's ticket
**(b)** — *"due before `STORY-0502`"* and still unwritten — because the ladder route is the first
production caller that needs a wall clock it did not construct itself; `serverComponents` gains
`wallClock: Clock = Clock.systemUTC()`, `PostgresDuelResultSink` loses its own default, and a grep
holds `src/main` to exactly one `Clock.systemUTC()`. Ticket **(a)** and ticket **(c)** are not
included and stay unwritten. Four traps the story invites are owned by name rather than hoped away:
the **self line computed from the page** is `TASK-050216`, which asks with a player on page three
and again with one on page one, because an implementation that finds the requester among the rows it
drew is correct exactly when the player can already see themselves; the **accepted anomaly** of
`ADR-0066` §4 is `TASK-050215`, which puts winner and loser on opposite sides of the cursor and
asserts the loser **twice** and the winner **never**, with `TASK-050214` asserting the
at-the-cutoff duel that disturbs nothing — neither file claims `STORY-0408`'s *total and disjoint*,
which is explicitly not inherited; **rank versus row position** is `TASK-050205` at the port and
`TASK-050213` over HTTP, both pinning literal rank sequences with ties straddling page boundaries,
where an offset implementation reads `[3, 4]` for the ladder's `[2, 4]`; and **a fixture already in
tie order** is refused everywhere — every fixture names its creation order and its recording order,
and both differ from the answer. The two id-sensitive fixtures assign roles from
`sortedByDescending { it.id.value }` rather than `UUID.compareTo`, because PostgreSQL orders `uuid`
by bytes and Java by two signed longs. `TASK-050219` turns `ADR-0066` §1 into a durable guard —
no table, no column and no materialised view naming a standing or a season — and `ADR-0066` §8's
index stays **named and unwritten**, as its own §8 asks. The story raised **no decision**:
`DEC-056`, `DEC-058`, `DEC-059` and `DEC-061` were answered by `ADR-0063` through `ADR-0066` before
the split, and `DEC-057` and `DEC-060` are untouched — no `playerId` parameter and no season
parameter, greps included.

`STORY-0501` was split on 2026-08-19 into **six** tickets, linear because five of them touch one
file. It ships a *function* and not a schema — no table, no column, no migration, no operator, no
job (`ADR-0061` §3) — and `TASK-050105` turns that into commands rather than prose: no `V<n>__` file
in the branch, no file under `poker-engine`, no file under `web-client`, and a Testcontainers test
that snapshots `player.coin_balance` and every `duel_result` row either side of exercising every
season function. With `STORY-0505` dropped, that test is now the **only** place in the product where
*a season moved no coin* is checked. `TASK-050103` owns the cost `ADR-0061` named out loud — a UTC
boundary meeting a locale-rendered time — and pins it with instants half an hour either side of a
month boundary plus a default-zone test that catches a `ZoneId.systemDefault()` implementation even
on a CI runner already in UTC. The split raised **`DEC-062`**, the architect's, and blocked exactly
one ticket with it: the ADR and the story both named `ServerClock` as the source of *which season is
it*, and that clock measures `System.nanoTime()`. It is **answered** —
[`ADR-0062`](../docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md), same day:
the wall clock is an injected `java.time.Clock`, `ServerClock` keeps measuring durations and nothing
else, and `ADR-0061` §3 is amended in that one clause. `TASK-050106` is unblocked and names the type
it takes, the absence of a default, and how its tests pin an instant — `Clock.fixed` for the two
fixed cases and a private movable subclass for the one that crosses a year boundary.
