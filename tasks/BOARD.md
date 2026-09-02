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

`EPIC-13` (the living table) **opened on 2026-09-02** on the human's raw feedback after they played a
duel end to end, and opened **`backlog` on purpose**: the feedback is quoted verbatim in the epic as the
source, and six of its eight items were questions rather than instructions. **All six are now
answered**, by the `product-owner` agent, on 2026-09-02:
[`ADR-0107`](../docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md) (`DEC-114`) — `Pot`
names the total, every chip committed to the hand so far;
[`ADR-0108`](../docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md)
(`DEC-115`) — an expiry checks or folds the one decision and never forfeits the duel, and the timebank
**replaces** `ADR-0013`'s grace window;
[`ADR-0109`](../docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md)
(`DEC-117`) — one mark, the most recent act, cleared only by the next act or the next painted deal;
[`ADR-0110`](../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) (`DEC-116`) — the
waiting screen is retired and the host waits at the table, `ADR-0073`'s promises moving with them;
[`ADR-0111`](../docs/adr/ADR-0111-an-illegal-typed-amount-is-refused-in-the-servers-own-numbers.md)
(`DEC-118`) — an illegal typed amount is refused in the server's own numbers, never clamped; and
[`ADR-0112`](../docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md) (`DEC-119`) — only a
**running** duel refuses another screen, and the refusal restores the address.
Two **architect's** decisions were registered along the way and gate the wire work rather than the
split, and **both are now answered**: `DEC-120` by
[`ADR-0113`](../docs/adr/ADR-0113-the-turn-clock-is-derived-state-and-the-sweep-plays-the-seat.md) —
one `TurnClock` frame carrying durations, a deadline derived rather than armed, and an expiry that is
a synthesised act down the ordinary path — and `DEC-123` by
[`ADR-0114`](../docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md) — one
pure predicate ruling honour/refuse/hold, with a mailed screen holding until the first frame answers. **The epic is split: eleven stories, written 2026-09-02, none yet cut into tickets.** The eight items
are the seam, with two exceptions — item 4 becomes three stories (its card, its server half, its
client half) and item 8 becomes two, in both cases because the half no decision blocks should not
wait behind the half that does. **`DEC-120` and `DEC-123` are now answered** by `ADR-0113` and
`ADR-0114`, so `STORY-1308`, `STORY-1309` and `STORY-1311` are no longer blocked; `DEC-124` is
answered by [`ADR-0115`](../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md),
so `STORY-1303` and `STORY-1306` are unblocked too. **No story in this epic waits on a decision.** The order is one chain and it is not
arbitrary: the pot figure first because every later card copies it, the host-alone table second
because `ADR-0110` makes a table that is not a duel and every later surface must answer for it, then
the seat marks and the bar in the order in which each card proves `ADR-0103`'s phone fit against
everything merged before it. Two items needed no decision at all, because *pulsing or running
circle* is a choice between two drawings and `ADR-0024` §3 puts that in front of the human's eye rather
than in an ADR — but the split raised **`DEC-124`, the product owner's**, on the question underneath
that choice: this epic is the product's first continuous motion, and nothing merged says whether an
animated surface owes a still form to a player whose system asks for reduced motion. It blocked the
implementing tickets of `STORY-1303` and `STORY-1306` and nothing else — not their cards, not the
chip's minting, not the epic — and `ADR-0115` answered it the same day, before either story was
split. The epic writes down what it measured so no story re-discovers it: the pot is a display
decision and not a defect, the turn clock has nothing on `develop` to build on, and the bar has no input
element.

Startable right now: `python3 .github/scripts/lint_tickets.py --startable`

---

## Epics

| ID | Title | Status | Milestone |
| --- | --- | --- | --- |
| [EPIC-00](epics/EPIC-00-ways-of-working.md) | Ways of working | **in progress** | v0.1 |
| [EPIC-01](epics/EPIC-01-poker-engine.md) | Poker engine | **done** | v0.1 |
| [EPIC-02](epics/EPIC-02-duel-server.md) | Duel server — rooms, WebSocket protocol, persistence | **done** — 14 of 14 stories; closed 2026-08-14, reopened for `STORY-0213` and `STORY-0214`, both of which merged, closing it again on 2026-08-26 | v0.1 |
| [EPIC-03](epics/EPIC-03-web-client.md) | Web client — table, lobby, duel flow | **done** — 14 of 14 stories done (`STORY-0309` closed on 2026-08-24); `STORY-0313` unblocked on 2026-08-24 when `STORY-0214` merged and is **split into fifteen**, all fifteen done, closing the epic on 2026-08-24 — it raised `DEC-070` (how long the server's own action stays on screen, the **product owner's**), answered on 2026-08-24 by [`ADR-0075`](../docs/adr/ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md), so **nothing in the story is blocked**; `STORY-0314` **closed on 2026-08-24**, five of five, leaving `STORY-0313` the only story left in the epic. **The last thing standing was `DEC-024`, not a story**, and it is answered on 2026-08-28 by [`ADR-0088`](../docs/adr/ADR-0088-the-two-browser-proof-is-a-written-hand-check.md): **no automated two-browser test, no fifteenth story, no third CI job** — the two-browser proof is `ADR-0088` §2's eleven-step hand-check with §3's receipt, and the automated ceiling stays at `STORY-0312` plus `poker-server/.../e2e/`. The epic's file moves to `status: done` with it. **Accepted and named**: `main.tsx`, the real `WebSocket` call, `dist/` and two storage partitions are covered by no test — no test here has ever opened a TCP connection to the duel server — so a break in any of them is found at a release, not at a pull request | v0.1 |
| [EPIC-04](epics/EPIC-04-identity-and-profiles.md) | Identity and profiles | **done** — **17 of 17 stories, closed 2026-08-28** by `STORY-0417`'s twenty-three tickets. Nothing in it is open: `DEC-081`, the last decision standing between this epic and its close, was raised and answered on 2026-08-28. `STORY-0405` unparked on 2026-08-23 when `STORY-0213` and `STORY-0214` merged, and is **split into 24 tickets** with `TASK-040501` startable. It raised `DEC-069` (the sign-in budget's two numbers, the architect's), answered on 2026-08-24 by [`ADR-0074`](../docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md) — nothing in the story is blocked. `STORY-0416` was split out of numerical order on 2026-08-25 into **29 tickets**, since it depends only on the finished `STORY-0405` while `0412`, `0414`, `0415` and `0417` all trace through `DEC-054`; it raised `DEC-071` (the product owner's) and `DEC-072`, `DEC-073`, `DEC-074` (the architect's), blocking six of its own tickets and nothing else. `DEC-072` was answered on 2026-08-25 by [`ADR-0077`](../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md), which also raised `DEC-075` — blocking nothing — and `DEC-073` the same day by [`ADR-0079`](../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md), leaving all four answered — `DEC-074` on 2026-08-25 by [`ADR-0080`](../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md), which resolves a contradiction between `ADR-0031` §4 and §5 rather than filling a gap. Both answers were then folded back into the split: `TASK-041627` was re-cut into six tickets as `ADR-0077` §Consequences required, and `ADR-0078` turned `TASK-041601`'s conditionally-parked collation follow-up into `TASK-041635`, taking the story to **35 tickets**. Three further corrections on 2026-08-25 took it to **38**, none of them a new decision: `ADR-0079` §Consequences named a defect against `TASK-041607`/`TASK-041608`/`TASK-041625` — `ADR-0031` §5's fifteen-minute resend suppression was built on one of the two mail paths — so `claimPending` now answers `ClaimPendingResult` rather than `Unit`, `TASK-041636` enforces the rule inside the writing transaction and `TASK-041637` makes the handler mail only when it wrote; and `TASK-041638` widens `TASK-041606`'s shape gate, which held for functions and for nothing else. `DEC-075` — raised by `ADR-0077` and blocking nothing — was answered on 2026-08-25 by [`ADR-0081`](../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md), **leaving `EPIC-04` with no open decision at all**; `ADR-0080` and `ADR-0081` were then folded back into the split on the same day, correcting five tickets, adding none and moving no status. A later planner pass took it to **40**, on two findings from tickets that had already run rather than on any decision: `TASK-041624`'s deliberately empty fixture tables were filled from `ADR-0078` §6 and it gained a fourth test, and `TASK-041639` and `TASK-041640` were added because `TASK-041614` shipped a title claiming an atomicity none of its five tests hold. A later pass took it to **41**, again on a ticket that had already run: `TASK-041641` carries the two `PostgresProfileReadsTest` methods `TASK-041616` named in its *Tests* section but, being `atomic:`, could not hold — its six-row *Files* table is its whole change and that file is not in it, because no gate names it. Splitting them out rather than inventing a fourth `atomic:` item is the point: an item must name a merged gate that fails on the smaller commit, and `TASK-041616`'s probe reached green without that file, which is the proof there is none. A further pass on 2026-08-26 took it to **44**, on `ADR-0082` — `DEC-076`, raised by a coder on `TASK-041626` who found that nothing in the codebase could produce the login handle its mail needs, and answered in the same PR. `TASK-041642`, `TASK-041643` and `TASK-041644` land the address-keyed read, its behavioural tests and the gate that forecloses `Credentials.handleOf(playerId)`; the same reasoning as `TASK-041641` decided there were three of them rather than one, since `ADR-0070`'s probe reached green on three files and no gate names either test file. **`TASK-041642` is now the story's single startable ticket**, and `TASK-041626` sits `backlog` behind the three. `STORY-0412` was split on 2026-08-26 into **27**, out of numerical order because `STORY-0416`'s chain is stalled behind `DEC-076` while `STORY-0414`, `0415` and `0417` all trace through `0412`; `TASK-041201` is startable and two tickets — the last two in the chain — were `blocked` on `DEC-077`, **the product owner's**, which asked what the product calls the screen a player opens to reach an account from a browser that does not hold it. `ADR-0076` §1 left the screen count to the story and the answer is **two**; one of the two words was found already merged rather than coined, since `ADR-0050` §3, `ADR-0036` and `ADR-0056` §2 each say *account* to a player. `DEC-077` was answered on 2026-08-26 by [`ADR-0083`](../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md) — ***Sign in*** at **`#/sign-in`**, the hyphen taken from `POST /api/auth/sign-in` rather than coined, said as the heading and as the one door, refused to nobody, landing on `#/account` when it succeeds — which **leaves `EPIC-04` with no open decision again**. That answer was folded into the split on 2026-08-26, unblocking both tickets and taking `STORY-0412` to **29**: `TASK-041226`'s `^[a-z]+$` criterion widened to `^[a-z]+(-[a-z]+)*$` and its hyphen proof step inverted, the *Sign in* heading is queried **by role** because `SIGN_IN_LABEL` puts the identical string on the submit button beneath it, and §5's landing rule became `TASK-041229` rather than a fourth file on `TASK-041227` — the same reasoning as `TASK-041641` and `TASK-041642`, since the client gate is green with either half alone and a set of files no gate holds together is a split, not an `atomic:`. The same pass added `TASK-041228` on a finding from a ticket that had already run: `TASK-041202`'s proof step 3 predicted a `popstate`-for-`hashchange` swap would redden two tests and it reddens **none**, so the gate on `ADR-0076` §5's silent trap was itself silent | v0.2 |
| [EPIC-05](epics/EPIC-05-ranking-duel-coins-and-leaderboard.md) | Ranking, duel coins and leaderboard | **done** — 4 stories built, 49 tickets; `STORY-0504` and `STORY-0505` dropped by `ADR-0067` and `ADR-0061` §5; 7 decisions answered by `ADR-0061`–`ADR-0067` | v0.3 |
| [EPIC-06](epics/EPIC-06-design-system-and-art.md) | Design system and art | **done** | v0.2 |
| EPIC-07 | Infrastructure and delivery | *not written* — **carries one unfiled ticket, described below**: `player_display_name_unique`'s `COLLATE "und-x-icu"` is gated by nothing | v0.2 |
| EPIC-08 | Analysis and decision quality | *not written* | later |
| EPIC-09 | Bots and simulation | *not written* | later |
| EPIC-10 | The AI software factory — the case study | *not written* | continuous |
| [EPIC-11](epics/EPIC-11-status-notifications.md) | Status notifications — the run reports itself | **in progress** | v0.1 |
| [EPIC-12](epics/EPIC-12-quality-and-defect-repair.md) | Quality and defect repair — the cycle that tests, triages and stops | **ready** — `DEC-082` answered by [`ADR-0089`](../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md) on 2026-08-29: a browser may drive this client for a QA round, never for a gate, on three standing conditions (no dependency, no gate, no coverage claim), and `ADR-0088` §1's heading is amended to match while its body stands. **`DEC-083` answered** by [`ADR-0090`](../docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) the same day: **a skill may write the catalogue or run it, never both in one turn** — §2b's *"not another skill invoking it as a step"* is about **composition**, not automation alone, so its heading becomes *"No gate, and one caller"* and a cycle is started by **the human's own message and nothing else**. The missing suites (`EPIC-04`, `EPIC-05`, `EPIC-06`) are authored by a licensed **`qa-cases`** skill that lands them through ordinary reviewed PRs and whose terminal act is a report naming the command the human types next; every case it writes cites the merged source of its expectation, and one with no source is a `DEC` for the product owner rather than a case. Opened 2026-08-29 on the human's instruction. A `qa` agent scoped to an epic, a smoke run or a regression run; a `qa-manager` that triages and is the only thing that files bug tickets; a `qa-cycle` skill that runs the loop and **stops** it. Bugs are ordinary `task`s under a round story, which is what lets `build-epic` repair them unmodified and needs no change to a merged gate. Its hardest requirement is termination — five budgets and a convergence rule, because a loop that reports more defects every round is the failure it is designed against | v0.1 |
| [EPIC-13](epics/EPIC-13-the-living-table.md) | The living table — the turn clock, the chips, and the act just made | **ready** — **split into eleven stories on 2026-09-02**; **`STORY-1301` was cut into three tickets the same day** and is the one startable now, the other ten uncut, `STORY-1308`/`STORY-1309` wait on `DEC-120`, `STORY-1311` on `DEC-123`, and the split raised `DEC-124` (the product owner's — does an animated surface owe a still form?), which blocks only the implementing tickets of `STORY-1303` and `STORY-1306`. Opened 2026-09-02 on the human's raw feedback after playing a duel end to end and deliberately **not specified**; its six product decisions were all answered the same day by the `product-owner` agent, and the epic was split the same day. Eight items: two needed only a design card and the human's eye (the acting seat's mark, and chips that move) and six needed a merged answer first. [`ADR-0107`](../docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md) makes `Pot` the **total** — `PlayerView` already carries `committedThisStreet` and `Lobby.tsx:154` already summed it for the sizing row, so the same screen was showing both numbers and printing the smaller one; no wire moves. [`ADR-0108`](../docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md) is the largest: 30 s a move plus a 3 m timebank, an expiry that **plays the seat and never the duel**, and a timebank that **replaces** [`ADR-0013`](../docs/adr/ADR-0013-disconnect-grace-period.md)'s grace window — so the duel never pauses and `DUEL_PAUSED` loses its occasion. It closes a cost [`ADR-0105`](../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) accepted by name (*"no resign, **no turn clock**"*), and it is the one item that moves the wire, so it is `atomic:` by `ADR-0070`'s probe and serialised by `ADR-0047`'s lock. [`ADR-0109`](../docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md) gives the table **one** mark, cleared only by the next act or the next painted deal — never a timer. [`ADR-0110`](../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) **retires the waiting screen**: the host waits at the table, and `ADR-0073` §3's promises move with them so `ADR-0105` §2 still cites a sentence a surface renders. [`ADR-0111`](../docs/adr/ADR-0111-an-illegal-typed-amount-is-refused-in-the-servers-own-numbers.md) refuses an illegal typed amount in the server's own numbers — never clamps, never knowingly sends — with `ADR-0100` §5's driver guarantee explicitly intact. [`ADR-0112`](../docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md) answers the eighth item, added by the human the same day: **the reported refresh symptom did not reproduce** — a host in a duel on a bare `/`, a rival on `?room=`, the waiting screen and a hash route on a room-free browser all survive `location.reload()` — and what did reproduce is the inverse, a browser **holding a room** having its fragment erased. So only a **running** duel refuses another screen, the refusal restores the address, and `WAITING`/`FINISHED`/`ABANDONED` let the player look away; the story that lands it still owes a reproduction attempt on the six paths not yet driven. Two **architect's** decisions were registered along the way and gate the wire work rather than the split: `DEC-120` (the clock's mechanism) and `DEC-123` (the navigation guard's). Design-first is not re-legislated: [`ADR-0091`](../docs/adr/ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §2 already makes the card the split's first ticket, and the epic adds only that a card draws **every** state it has and merges before its implementing ticket is startable | v0.4 |

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
| | [TASK-000101](tasks/TASK-000101-bootstrap-repository.md) Bootstrap repository and ticket system | M | **in-review** |
| | [TASK-000102](tasks/TASK-000102-enable-branch-protection.md) Enable branch protection | S | **done** |
| | [TASK-000103](tasks/TASK-000103-token-lean-agent-workflow.md) Token-lean agent workflow | S | **in-progress** |
| | [TASK-000104](tasks/TASK-000104-a-second-branch-cannot-claim-the-same-protocol-version.md) A second branch cannot claim the same PROTOCOL_VERSION | S | **done** |
| | [TASK-000105](tasks/TASK-000105-two-build-files-that-were-never-source.md) Two build files that were never source | XS | done |
| | [TASK-000106](tasks/TASK-000106-the-board-and-the-ticket-file-are-one-register.md) The board and the ticket file are one register, and the linter reads both | S | **done** |

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

`TASK-000106` is **new**, filed on 2026-08-26, and it audits this file. `lint_tickets.py` reads 869
ticket files and never opens `BOARD.md`, so a ticket's `status:` and its board cell can disagree in
silence — which they have, twice at cost: a row reading `**ready**` over a file reading `backlog` made
a startable ticket invisible to `--startable` and it simply never started, and one story split wrote
29 ticket files with no rows at all. Measured on `develop` at `04f5b26a`: 869 files, 869 task rows,
a clean 1:1, and **two** disagreements, both from `EPIC-00` — `TASK-000101` (board `done`, file
`in-review`) and `TASK-000103` (board `in-review`, file `in-progress`). Both are **deliberately left
standing**, so that the check is what finds them rather than a planner quietly repairing two cells;
the ticket reconciles them in the same pull request, because otherwise the new gate exits 1 the moment
it lands. Whether those two tickets are *finished* is a separate question it does not answer.
>
> **Correction, measured on `develop` at `e9a18eb2`: both disagreements are gone.** `TASK-000101`
> reads `in-review` in the file and `**in-review**` on the board; `TASK-000103` reads `in-progress`
> in both. Nobody recorded repairing them, which is the failure this paragraph predicted — the cells
> were quietly reconciled and the evidence the gate was meant to catch went with them. They are **not**
> being re-broken to restore the fixture: a deliberate inconsistency that no gate yet reads is
> indistinguishable from a real one, and this is the second register in the repository that has now
> drifted without a trace. The gate ticket therefore needs a premise that does not depend on live
> drift existing when it is written — which is a planning decision, not a bookkeeping one, and is
> left open here rather than settled by the driver. It lives
under `STORY-0001` because the linter, the workflow that runs it and the board itself were all built
there, and because the drift it catches is `EPIC-00`'s own.

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
| **[STORY-0106](stories/STORY-0106-showdown-and-pots.md)** Showdown and pots — *schema 2* | | | **done** |
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
| **[STORY-0107](stories/STORY-0107-duel-format-and-match.md)** Duel format and match — *schema 2* | | | **done** |
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
| **[STORY-0108](stories/STORY-0108-event-log-replay-simulation.md)** Log, replay, simulation — *schema 2* | | | **done** |
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
| DEC-060 | **The product owner's** — does a **finished** season ever become reachable from a screen, and how is one chosen? Raised by [`ADR-0061`](../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §7: a finished season is never *gone* — it recomputes exactly from rows nothing rewrites — but v0.3 ships no way to ask for one, so on the first of a month the previous ladder is computable, unreachable, and **nothing records who won it**. A selector is a control on a screen `ADR-0060` already said would crowd; *never* is a complete answer and needs saying out loud. Blocks nothing today | [`ADR-0061`](../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) | before the first season boundary after the ladder ships |
| DEC-087 | **The architect's** — by what mechanism do the proofs of record load the built bundle a real user would receive: what serves `dist/` (the Ktor server, a static server, a `scripts/qa/stack.sh` mode), on what origin, and what supersedes [`ADR-0088`](../docs/adr/ADR-0088-the-two-browser-proof-is-a-written-hand-check.md) §2 step 3's `npm run dev` so the hand-check describes the same artifact? Raised 2026-08-30 by [`ADR-0093`](../docs/adr/ADR-0093-ready-for-real-users-is-said-of-the-shipped-artifact.md) §1a, whose readiness bar is unmeetable until this is answered. Blocks nothing running today — QA and UAT rounds continue against the dev server and their records say so | [`ADR-0093`](../docs/adr/ADR-0093-ready-for-real-users-is-said-of-the-shipped-artifact.md) | before anything is offered to real users |
| DEC-088 | **The product owner's** — when a UAT round's promotion gate has **more than three screens** each offering a question that clears the bar, which screens' questions take the three slots? Raised 2026-08-30 by `TASK-120708`, whose coder declined to invent the rule and named it instead. `ADR-0092` §5 sets the caps — at most three per round, one per screen — but supplies no ordering, and neither `.claude/agents/qa-manager.md` nor `.claude/agents/uat.md` does. Step 4's fix-set ordering (*`blocker` before `high`, then by how much of the product is blocked*) does **not** generalise: every promoted question has already cleared the same single bar, so there are no severity layers to sort by, and no merged source ranks one screen against another. Until it is answered, `qa-manager` improvises — and a UAT round walks 13 screens for 3 slots, so the first round hits this. *The three sharpest, by the manager's own judgment, recorded in the round story* is a complete answer. Blocks nothing — the cycle runs, it just promotes an unordered three | [`ADR-0092`](../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) | before the first UAT round |
| DEC-089 | **The product owner's** — on the `result` screen, should a post-verdict account-claim nudge share the verdict's own type weight, or should the verdict stay the single largest thing on the screen? `web-client/src/result/AccountOffer.tsx:25` renders the nudge's headline — *Your duel coins are only in this browser* — at `text-display leading-tight font-bold`, the same weight `DuelResult` gives *Victory* itself, and `design/screens/duel-end.html` draws nothing after *Rematch* to compare against (the card predates the offer by thirteen days; `ADR-0091` §5 registers the accretion as debt). No merged source ranks the two, so it is a question, never a finding. Raised 2026-08-30 at round 1 of `/qa-cycle uat regression`'s triage and promoted by `qa-manager` under [`ADR-0092`](../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) §5 — one of the three slots that round had, on one of three distinct screens, so `DEC-088`'s ordering question did not bite. It clears both halves of the bar: a concrete choice answerable in one sentence, and it bears on a player's ability to tell what is going on or what they may do. **Blocks nothing** — it gates no member of round 1's fix set, so the cycle continues (`ADR-0092` §5); an answer becomes a merged source either way, and any ticket it yields enters the **earliest subsequent** round's triage or the ordinary backlog, never the round that asked (`EPIC-12` §Termination rule 1). Recorded in [`STORY-1209`](stories/STORY-1209-round-1-uat-the-front-door-was-never-dressed.md) | [`STORY-1209`](stories/STORY-1209-round-1-uat-the-front-door-was-never-dressed.md) | before the cycle's next triage |
| DEC-090 | **The product owner's** — on the `account` screen, should *Attach a recovery address* — which asks for a *Current password* — appear on a device that has no password yet, or only once one exists? The section is offered on an unclaimed profile, where the field it requires cannot be filled. `ADR-0050` §4 settles what the screen **reads** (`deviceRouteLive`, and *no `ProfileResponse` field*) and no merged source settles what it **offers**, so the shipped state contradicts nothing and this is a question. Raised 2026-08-30 at round 1 of `/qa-cycle uat regression`'s triage and promoted by `qa-manager` under [`ADR-0092`](../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) §5 — one of the three slots that round had, on one of three distinct screens, so `DEC-088`'s ordering question did not bite. It clears both halves of the bar: a concrete choice answerable in one sentence, and it bears on a player's ability to tell what is going on or what they may do. **Blocks nothing** — it gates no member of round 1's fix set, so the cycle continues (`ADR-0092` §5); an answer becomes a merged source either way, and any ticket it yields enters the **earliest subsequent** round's triage or the ordinary backlog, never the round that asked (`EPIC-12` §Termination rule 1). Recorded in [`STORY-1209`](stories/STORY-1209-round-1-uat-the-front-door-was-never-dressed.md) | [`STORY-1209`](stories/STORY-1209-round-1-uat-the-front-door-was-never-dressed.md) | before the cycle's next triage |
| DEC-091 | **The product owner's** — on the `sign-in` screen, should *Back* on the sign-in screen return to the account screen it was opened from, or to the lobby? `ADR-0083` §2 settles where a **successful sign-in** lands (`#/account`) and nothing settles where a refusal or a retreat lands. The shipped *Back* returns to the lobby, two steps from where the player was. No merged source is contradicted. Raised 2026-08-30 at round 1 of `/qa-cycle uat regression`'s triage and promoted by `qa-manager` under [`ADR-0092`](../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) §5 — one of the three slots that round had, on one of three distinct screens, so `DEC-088`'s ordering question did not bite. It clears both halves of the bar: a concrete choice answerable in one sentence, and it bears on a player's ability to tell what is going on or what they may do. **Blocks nothing** — it gates no member of round 1's fix set, so the cycle continues (`ADR-0092` §5); an answer becomes a merged source either way, and any ticket it yields enters the **earliest subsequent** round's triage or the ordinary backlog, never the round that asked (`EPIC-12` §Termination rule 1). Recorded in [`STORY-1209`](stories/STORY-1209-round-1-uat-the-front-door-was-never-dressed.md) | [`STORY-1209`](stories/STORY-1209-round-1-uat-the-front-door-was-never-dressed.md) | before the cycle's next triage |
| DEC-093 | **The architect's** — does [`ADR-0092`](../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) §6's **baseline rule** extend to a round in which a screen-state becomes conformance-judgeable for the first time through **any** merged instrument, or only through a **card** merged in the previous round's repairs? Both §6 and `EPIC-12` §Termination rule 4 are written about a card. Round 3 met the other case: two frames of `design/screens/duel-table-states.html` and `rematch-states.html` were unreadable in rounds 1 and 2 — round 2 said so in as many words, *"check (a) on two of `duel-table-states.html`'s three frames is unreachable by any round with the verbs `drive.mjs` has"* — and became readable in round 3 only because `TASK-121008`'s `record`/`frames` verbs merged in round 2's repairs. The rule's stated **purpose** reaches that case exactly (*"the two rounds measured differently-sized judgeable sets"*); its **text** does not. Raised 2026-08-30 at round 3's triage, which **refused to extend the exemption itself**: a manager who widens a rule in the round that rule would save cannot prove it would have widened it anywhere else, and `STORY-1208` is the precedent for repairing this machinery by a merged sentence rather than by a generous reading at triage. **Not one of `ADR-0092` §5's three promotion slots** — those are the product owner's, and this is `CLAUDE.md` rule 5 routing. **Blocks nothing**: round 3's fix set is empty, `B(3) = 0` so rule 4 could not fire at any reading, and the cycle ended `PASS`. Answering it costs a future invocation a wrong `STOP_DIVERGING` or nothing at all. Recorded in [`STORY-1211`](stories/STORY-1211-round-3-uat-the-count-fell-to-zero-and-the-cycle-ends.md) | [`STORY-1211`](stories/STORY-1211-round-3-uat-the-count-fell-to-zero-and-the-cycle-ends.md) | before the next `/qa-cycle` invocation |
| DEC-094 | **The product owner's** — should a control that **no card draws** wear the client's control vocabulary, or is a bare control the intended treatment for navigation the cards do not draw? Concretely: the lobby's `Your duels`, `Leaderboard` and `Account` doors, and the `Back` on each secondary screen, all render with `className: ""` — computing as body text — beside a room-code input, `Create a duel room`, `Join the duel` and `Set my name` that carry the full recipe. No merged source is contradicted, which is why it is a question and not a finding: `design/tokens/tokens.css` is a `:root` sheet with **no selectors**, so an unclassed button contradicts nothing in it; `create-duel.html`'s front-door frame draws neither control and notes *"nothing else on the door — no lobby noise, no tables list"*; and `ADR-0060` §§2–4 settle the doors' **word**, **element** and **placement** and the way out's word, and say nothing about treatment. Raised 2026-08-30 at round 3's triage and promoted by `qa-manager` under [`ADR-0092`](../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) §5 — **one of the three slots that round had, and the only one spent**, on the `first` screen, so `DEC-088`'s ordering question did not bite. It clears both halves of the bar: a concrete choice answerable in one sentence, and it bears on whether a player can tell that three words under a form are things they may activate. **Two rounds have now spent a finding on it** — the four `Back`s in round 2, the three doors in round 3 — and only a merged answer closes it mechanically, as `ADR-0094` closed the join path. **Blocks nothing** — round 3's fix set is empty and the cycle ended `PASS`; an answer becomes a merged source either way, and any ticket it yields enters the **ordinary backlog now the cycle has ended**, never the round that asked (`EPIC-12` §Termination rule 1). A *no* earns a row in `docs/test-plan.md` §*Settled, and not a finding*, so a later round re-raising it would itself contradict a merged source. Recorded in [`STORY-1211`](stories/STORY-1211-round-3-uat-the-count-fell-to-zero-and-the-cycle-ends.md) | [`STORY-1211`](stories/STORY-1211-round-3-uat-the-count-fell-to-zero-and-the-cycle-ends.md) | before the cycle's next triage |
| DEC-102 | **The product owner's** — what does one press of the sizing row's stepper (`+`/`−`) move the dialled total by, and therefore which legal totals can the bar reach at all? Registered open 2026-08-31 by [`ADR-0101`](../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) §6. `ADR-0100`'s alternatives filed the step size under `DEC-101` — *"the step size is undecided and belongs with `DEC-101`'s control design"* — and `ADR-0101` §6 declines it: the question registered was what the **named presets** compute, a stepper is not a named preset, and neither `docs/vision.md` nor `docs/duel-rules.md` names a step, so an answer would be invented rather than derived. **It gates none of `ADR-0101` §§1–5** — the chips' arithmetic, their offer rule and `ADR-0100` §1's driver stand without it, and the merged `whole-duel.test.tsx` gate needs only `min` and `all-in` — so the rewritten `TASK-120908` is **not** blocked by it. The constraint whoever answers it must weigh: `duel-rules.md` §*Betting* says **No-limit**, the shipped slider reaches every legal total and five chips reach five, so whether the bar must keep a way to reach any legal total — and therefore whether the stepper may ship after the chips or must ship with them — is part of this question. Raised by an ADR, not promoted at a triage, so **not** one of `ADR-0092` §5's three slots | [`ADR-0101`](../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) §6 | before the sizing row's stepper ships |
| DEC-103 | **The product owner's** — may a compound status label break across a line so that a bare numeral or symbol is stranded away from the word or unit it belongs to? Observed at phone width: the pot strip's `Blinds 50/100 · Hand 1 · Preflop` wraps so that a bare `1` sits alone on its own line. **A proposed criterion, not a finding** — one of the two the observer proposed at round 1 of `/qa-cycle audit smoke`, recorded and routed by `qa-manager` under [`ADR-0096`](../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §3, which routes a proposal *exactly as* [`ADR-0092`](../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) §5 routes a question. Routed to the product owner because `docs/vision.md` settles it: *Positioning*'s *"Dark, quiet, fast, minimal"* and the Lichess/Chess.com benchmark the human qualified as *"not a licence to be less finished"* are the same source `R3` and `R4` are licensed by, and this asks about the integrity of a rendered phrase — the territory `R4` already occupies at the level of spacing. **It reaches the rubric no earlier than the next invocation** ([`ADR-0096`](../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §3 — the rubric is frozen for the invocation), and only as an amending ADR in [`ADR-0099`](../docs/adr/ADR-0099-the-rubric-is-the-adr-section-and-a-criterion-is-born-merged.md)'s form: `R6` is the next free id, the priority order restated as ids only, and [`ADR-0096`](../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md)'s index row annotated *rubric grown to 6* in the same PR. **Blocks nothing** — it gates no member of round 1's fix set, so the cycle continues. Recorded in [`STORY-1213`](stories/STORY-1213-round-1-audit-three-criteria-unmet-and-a-table-that-names-the-wrong-winner.md) | [`STORY-1213`](stories/STORY-1213-round-1-audit-three-criteria-unmet-and-a-table-that-names-the-wrong-winner.md) | before the next `/qa-cycle audit` invocation |
| DEC-104 | **The product owner's** — should the pot the table shows a player include the chips committed on the current street, or only the chips already swept in from completed streets? Observed: `Pot 0` stands for a whole betting street after both seats have committed chips, and moves only when the next street is dealt. **A proposed criterion, not a finding** — the second of the two proposed at round 1 of `/qa-cycle audit smoke`, routed by `qa-manager` under [`ADR-0096`](../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §3. Routed to the product owner because `docs/vision.md` settles it twice over: *What it is*' first line — *"Heads-up Texas Hold'em"* — is the licence [`ADR-0101`](../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) already used to rule that a control's word is the game's word, and *On variance*'s *"showing a player… is more interesting than hiding the maths"* is what `R1` is licensed by. **Two things the answer needs:** the product already computes both numbers and shows different ones in different places — `Lobby.tsx` passes `potIncludingStreet` (`ADR-0101`'s base) to `ActionBar` while `PotStrip` prints `view.pot` — and a *yes* is **not** a client change, since `view.pot` is server-authoritative and a client that summed it would be asserting a game fact (`CLAUDE.md`, `ADR-0002`, gated by `no-derivation.test.tsx`), so the *how* would be the architect's afterwards. **It reaches the rubric no earlier than the next invocation** ([`ADR-0096`](../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §3), and only as an amending ADR in [`ADR-0099`](../docs/adr/ADR-0099-the-rubric-is-the-adr-section-and-a-criterion-is-born-merged.md)'s form. **Blocks nothing.** Recorded in [`STORY-1213`](stories/STORY-1213-round-1-audit-three-criteria-unmet-and-a-table-that-names-the-wrong-winner.md) | [`STORY-1213`](stories/STORY-1213-round-1-audit-three-criteria-unmet-and-a-table-that-names-the-wrong-winner.md) | before the next `/qa-cycle audit` invocation |
| DEC-108 | **The product owner's** — when the table says *The duel is paused.*, may the action bar stay enabled? Raised 2026-09-01 by [`STORY-1214`](stories/STORY-1214-a-duel-played-by-hand-deadlocked-on-presence.md). Measured: under that notice every control read `disabled: false` — `Fold`, `Call 100`, `Raise to 200`, `All in 10,000` and all four sizing chips — and the human obeyed the sentence and waited while the duel was in fact playable. A script clicking through sees nothing wrong; a person reading the screen stops. **This is not filed as a defect, and deliberately so**: [`ADR-0046`](ADR-0046-the-table-says-away-timed-out-and-back.md) §6 declines the question **by name** — *"Whether the action bar's controls look disabled while the duel is paused"* — recording that `ADR-0028` §6 keeps `YourTurn` standing with `DUEL_PAUSED` as the refusal and that `STORY-0313`'s criteria *"already assume a live bar and an explained refusal"*. A live bar therefore contradicts no merged source, so a bug ticket would contradict a merged ADR and a QA case asserting the opposite would have no source (`ADR-0092` §5). What is genuinely open is whether *wait* and *you may act* should be sayable at the same moment at all — and, if not, whether the bar dims or the notice changes its words. **Blocks nothing**; `TASK-121401` writes `CORE-23` against the half that *is* merged (a client may never assert a game fact — the screen said paused while the server accepted the action) and leaves this untouched. The case that checks this answer is written when the answer merges | [`STORY-1214`](stories/STORY-1214-a-duel-played-by-hand-deadlocked-on-presence.md) | before the next `/qa-cycle audit` invocation |
| DEC-110 | **The architect's** — by what mechanism is a player who asks for a second room while holding a seat in a `PLAYING` room refused, and returned to the duel they are in? Registered open 2026-09-01 by [`ADR-0105`](../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §6, which fixes what must be true and deliberately writes no repair. The product half is answered: the request takes no seat, nothing moves, the player lands back on their own table, and the words are `ADR-0105` §4's. What is open is the carrier — a new `ProtocolError` value and the `PROTOCOL_VERSION` step `ADR-0047` prices, or an answer assembled from frames that already exist (`replyToJoinRoom`'s `ALREADY_SEATED` branch already answers a seated player with `RoomJoined(code, seat)` *"exactly as a fresh seating would"*, derived fresh from the registry). Three constraints are `ADR-0105` §6's, not this decision's: nothing is vacated or forfeited; the client must be able to **name** the room it puts the player back in, including a tab that has never heard of it; and the refusal must not meter as a guess when `ADR-0022` §2's failed-join budget is built — nothing implements it at `develop` (`JoinLimits` and `TOO_MANY_ATTEMPTS` do not exist). **Blocks the ticket that implements `ADR-0105` §1**, and nothing else | [`ADR-0105`](../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §6 | before `ADR-0105` §1 is cut into a ticket |
| DEC-111 | **The product owner's** — may one player hold more than one `WAITING` room at once? Registered open 2026-09-01 by [`ADR-0105`](../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md), which answered `DEC-109` for the **running** case and names this hole in its own Consequences rather than quietly covering it. Reachable in three presses of shipped controls: *Create a duel room*, `Back to the lobby` (`ADR-0073` §1, which forgets the room and keeps the seat), *Create a duel room* again. Each waiting room a player holds can be joined by a rival and become a duel that player is not at — `ADR-0013`'s window, `ADR-0023`'s absent seat, `ADR-0014`'s coin — so `ADR-0105` §1 refuses a **second seat taken while a duel runs** but does not stop two duels **starting** in rooms whose seats were taken earlier. `ADR-0073` already accepted that cost for **one** waiting room, in as many words; whether it survives being multiplied is the question, and it is `docs/vision.md`'s *"Not a multi-table poker room"* against `ADR-0073` §3's shipped promise that *"The room stays open"*. `ADR-0105`'s Alternatives records the shape this will have to weigh — refusing the **rival's** join, which lands a refusal on a blameless stranger. **Blocks nothing** | [`ADR-0105`](../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md) §Consequences | before the first public link |

`DEC-124` → [`ADR-0115`](../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md)
on 2026-09-02 — **motion never carries a fact, and reduced motion stills every surface.** Raised the
same day by the planner while splitting `EPIC-13`: the acting seat's mark and the moving chips are
the first continuous motion this product has ever had, and nothing merged reached the question — no
`prefers-reduced-motion` rule, no motion token, no accessibility stance (verified, not assumed).
**Every fact a surface states is stated by its still form** — the same surface at rest — and motion
only ever emphasises a fact, never carries one alone. **When the player's system asks, continuous
and decorative motion does not run**: the pulse holds as a steady mark, the chip flight is skipped
and the chips appear where they land; nothing hidden, nothing added, no second design, and no
in-product toggle — the system's signal is the whole interface. Only *how* the screen changes is
stilled, never *that* it changes: `ADR-0102`'s 600 ms steps, the clock's once-a-second change and
`ADR-0109`'s mark replacement are facts arriving in order and stand unchanged, so nothing is
amended. Governed globally, never per surface: a duration, delay, easing or travel distance is born
`--pd-motion-*` in `design/tokens/tokens.css` beside the sheet's one reduced-motion block, and a
card that gives a surface motion draws it **at rest as a named state**, so the human's pane verdict
covers both forms. Derived from *Positioning* — *"Dark, quiet, fast, minimal"*, the sentence
`ADR-0075` already used to rule an animation furniture — and deliberately **motion-sized**: no
stance on any accessibility standard, which would change what the product *is* and is the human's.
`STORY-1303`'s card owes *waiting* / *acting — moving* / *acting — at rest*; `STORY-1306`'s flight
is garnish over amounts the pot figure and stack numerals state still. Registers no new `DEC` and
unblocks both stories' implementing tickets.

`DEC-123` → [`ADR-0114`](../docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md)
on 2026-09-02 — **one predicate answers every ask, and a mailed screen waits for the first frame.**
The mechanism for `ADR-0112`. One pure module reads the room's standing off the frames the server
sent and rules *honour*, *refuse* or *hold*; the chosen-screen branches move above the store's, and
the refusal restores `/` in a `useLayoutEffect` so refusing and restoring are one commit. Two merged-
source findings forced the third value: `VerifyScreen` submits its token in a **mount effect**, so
rendering it is spending it, and a tab booting with a room in storage reads *no room* for the whole
rejoin round trip — so a store-only guard would fail on **every** attempt, not rarely.

`DEC-120` → [`ADR-0113`](../docs/adr/ADR-0113-the-turn-clock-is-derived-state-and-the-sweep-plays-the-seat.md)
on 2026-09-02 — **the turn clock is derived state, the wire carries what is left, and the sweep plays
the seat.** The mechanism for `ADR-0108`. One new `ServerMessage.TurnClock` states, per decision point
and to both seats, what is left of the 30 s and of both timebanks — **durations, never instants**, so
`ADR-0028` §2's no-shared-epoch argument stands. The deadline is **derived from the live decision
point rather than armed**, so the act/sweep race closes on the room's mutex plus the engine's sequence
guard with nothing to cancel. An expiry is a server-synthesised act down the ordinary act path,
reusing `foldAbsent`'s single-seat body, so `ADR-0023`'s conduct and the coin's single settle path
hold by construction. `expireGracePeriods()` becomes `expireTurnClocks()`; `isPaused`, `DUEL_PAUSED`
and `graceRemainingMillis` are deleted with the pause. **A trap no gate catches:**
`docs/test-plan.md`'s `CORE-23` asserts the `DUEL_PAUSED` refusal this removes, and nothing reads
that document.

`DEC-115` → [`ADR-0108`](../docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md)
on 2026-09-02 — **an expiry plays the seat, never the duel, and the timebank replaces the grace window.**
Raised the same day by the human: the 30 s move clock and the 3 m timebank were given; open were the
consequence of expiry and how the bank meets `ADR-0013`'s disconnect window. **On expiry the server gives
up that one decision with `ADR-0023`'s conduct** — fold facing a bet, check when checking is free, read
from the engine's legal set, marked on the wire as the server's act — **and the duel is never ended by
decree**: no count of timeouts forfeits, no coin moves except by `ADR-0014` on the outcome the engine
reaches, and a seat that never acts blinds off under the escalating schedule that already guarantees a
duel terminates. Derived from *What it is* — *"One duel coin per win… A counter of duels won."* and *"A
duel is a match, not a hand."* — the same derivation `ADR-0105` made and the human endorsed. **The
timebank replaces the grace window**: one clock answers *"the seat on turn is not acting"*, indifferent
to the socket — the human's own *"timebank also work for disconnection case"* — so disconnecting never
gains, stops or freezes time; an away seat whose clock is exhausted is `ABSENT` and played without delay
until it reconnects, exactly as today's post-window behaviour; a connected seat always gets a fresh 30 s,
because a reconnect is observable and attention is not. **The duel never pauses**: `DUEL_PAUSED`'s
occasion and *"The duel is paused."* leave when the clock lands — `ADR-0013` amended (seat-holding,
filtered resume and configuration stand), `ADR-0028`/`ADR-0046` amended by that clause alone. Costs
named: a connected ghost still spends ~30 s a decision across a blind-off, and a bank-empty disconnector
gets 30 s where the window gave 60. The mechanism — the deadline's frame, the `PROTOCOL_VERSION` step
under `ADR-0047`'s lock, the `atomic:` ticket sized by `ADR-0070`'s probe, expiry as synthesised act or
new room event — was registered as **`DEC-120`, the architect's**, and is answered by
[`ADR-0113`](../docs/adr/ADR-0113-the-turn-clock-is-derived-state-and-the-sweep-plays-the-seat.md).

`DEC-112` → [`ADR-0106`](../docs/adr/ADR-0106-a-sub-pixel-residual-is-a-fit-and-one-pixel-is-the-fence.md)
on 2026-09-02 — **a sub-pixel residual is a fit, and one pixel is the fence.** Raised the same day by
[`TASK-121402`](tasks/TASK-121402-the-duel-table-column-fits-the-phone-it-is-nested-in.md), whose 48 px
repair left the duel-table document at a true **664.90625 px against 664** — `scrollHeight` reads 665 —
with both action buttons at or above the fold and every other criterion met; the ticket's acceptance
criterion demanded `≤` and predicted 664 / 664, its Out of scope ordered the implementer to stop short
of the next give, and the implementer stopped and said so, which is `ADR-0103` §3's stop rule operating
at its first live boundary. Registered and answered in the same PR (the `DEC-039` path — it never
appeared in an open table). **The answer: yes, at exactly one boundary.** `ADR-0103` §1's
`scrollHeight ≤ clientHeight` stays the contract; a reading of exactly **one integer over** is judged on
the true geometry (`document.documentElement.getBoundingClientRect().height`, same stack, same beat),
and a true excess **strictly under one CSS pixel is met** — nothing readable can hide inside it — while
one pixel or more, or a two-integer excess, is `R2` `not met` as ever. **The fence cannot widen**: a
tolerance that can hold painted content is the *"relaxed phone bar"* `ADR-0096` §2 forbids by name, so
widening is the human's and no vision-derived ADR can reach it. **`TASK-121402` merges as scoped** — the
664 / 664 prediction was unreachable from the day it was written, because the defect table's 712 was
itself a rounded 712.90625 — and **the last 0.90625 px is re-filed once**, ordinary backlog, buying
headroom (the column stands 0.09375 px from the fence) and retiring the second read; its closer spends
`ADR-0103` §3.1 whitespace and nothing further down the give list. **Cut on 2026-09-02 as
[`TASK-121501`](tasks/TASK-121501-the-columns-whitespace-gives-one-token-step-at-the-phone.md)** under
[`STORY-1215`](stories/STORY-1215-the-duel-tables-last-sub-pixel-and-the-headroom-it-buys.md), and the
probe §4 asked for **disagrees with §4's own guess**: at 390 the clamp's floor is inert — `100cqi` is
390px and the ramp is exactly 8 there — so the **ramp** moves in the card and the client together, one
token step, for 23.09375 px of headroom. **The QA clause**: a sub-pixel
overflow may not be filed — not as a finding, a criterion, a `DEC` or precedent — and it shields nothing
else; clipped content (`R3`), imperceivable events (`R1`), unmeasured beats and any full-pixel excess
walk the ordinary path. Costs named: `ADR-0103` §1's one-`eval` contract is spent at the boundary — two
reads and a rule, until a residual ticket nothing forces to land — a citable tolerance now exists, the
product ships 0.09375 px from the fence, a criterion merges undischarged as literally written (carried
by the PR body's readings plus the ADR), and a classic-scrollbar window at phone width can grow a
scrollbar track for a rounded pixel. Qualifies `ADR-0103` §1 by one boundary case; upholds `ADR-0096`
§2 rather than relaxing it. Derived from the vision's first success condition — *"Send a link. She opens
it in a browser. We play a full heads-up match."* — read as `ADR-0103` read it: *fits* is for playing
unscrolled.

`DEC-109` → [`ADR-0105`](../docs/adr/ADR-0105-one-duel-at-a-time-and-the-refusal-hands-back-the-duel.md)
on 2026-09-01 — **one duel at a time, and the refusal hands the player back their duel.** Registered open the
same day by [`ADR-0104`](../docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md) §10,
which split it off rather than deciding it. **The answer is no**, and it is **derived from the vision, not
stated by the human**: *What it is not* — *"Not a multi-table poker room. No 6-max, no 9-max, no tournaments,
no sit & go, no cash games."* — with the **shape** of the answer taken from *What it is*: *"One duel coin per
win. Not chips, not currency, not a balance. A counter of duels won."* **While a player holds a seat in a
`PLAYING` room, `CreateRoom` and `JoinRoom` from that player take no seat**: the room they asked for is
untouched, the seat and duel they hold are untouched, and **no coin moves**. **§2 scopes *running* to
`PLAYING` alone** — a `WAITING` seat is **not** refused, because `ADR-0073` §3 renders *"The room stays open.
That link still works for your rival, and it brings you back."* on screen and refusing would make a shipped
promise false; a `FINISHED` seat is **not** refused, because `Room.offerRematch` agrees only when **both**
seats have offered, so nothing can start there without this player's own press. A seat inside `ADR-0013`'s
grace window **is** refused with the rest, which is the grace period working rather than an exception to it.
**§3 is the half that makes §1 defensible: the refusal hands the player back the duel they are in** — the
table as it stands, never a lobby, never a dialog — and doing it again does the same thing, because both
shipped routes in arrive holding no memory of that room (`Back to the lobby`, `ADR-0072` §5 and `ADR-0073`
§1, which forgets the room and keeps the seat; and a second `?room=` link, which `boot.ts` prefers over the
memory in as many words). **§4 fixes two sentences and forbids a third**: `You are already in a duel. Finish
it to start another.` — with a table of what the copy may not say (any duration, since no client owns a clock
against a server window; anything about the code that was asked for, since `ADR-0022` hands out no oracle; a
promise the link will last; the casino's furniture). Layout stays `EPIC-06`'s, on `ADR-0073` §6's precedent.
**§5: the player whose invite was clicked sees nothing, because nothing happened to them.** **§6 writes no
repair** — nothing moves, the client must be able to **name** the duel it returns to, and the refusal must
not meter as a guess when `ADR-0022` §2's budget is built (`JoinLimits` and `TOO_MANY_ATTEMPTS` do not exist
at `develop`) — and registers **`DEC-110`** for the architect to price the carrier. **§7 inherits `ADR-0104`
§9: this explains no fold.** **Vacating the first seat was the strongest rejected option and lost on the
coin**: `RoomRegistry` has exactly one `sink.record` call site, inside `act` and reached only with an
outcome, so **every coin this product has settled came from a duel the engine ran to a chip holder** — a
forfeit-on-navigation would be the product's first, the least reversible thing in the option set (a result
row and a ladder position against a refusal an afternoon undoes), and a commitment `docs/vision.md` does not
make. **Costs named**: **a player who wants out of a duel is stuck** — there is no resign, no turn clock, and
`Application.sweepPass` never reaps a `PLAYING` room, so their exit depends on their **rival** still acting;
the product's primary action, *Create a duel room*, acquires a state in which it says no; the player who clicked a
link and the friend who sent it are both left slightly in the dark, on purpose, because the alternative is
copy that guesses; one more string with nothing keeping it in one voice; and **§2's hole stays open** —
several `WAITING` rooms can each become a duel the player is not at, so *never in two duels at once* is what
this decision aims at and **not** what it guarantees, registered as **`DEC-111`**. Qualifies `ADR-0094` §1 by
exactly one case: opening the invite is taking the seat, except when the opener is already in a duel. **The authority is the human's, not the vision's.** The first draft cited *What it is not*'s *"Not a multi-table poker room…"*; review rejected that reading — every item in that sentence's list names a table or game **format**, and nothing in `docs/vision.md` speaks to per-player concurrency across duel instances — which by the product owner's own boundary made it the human's call. **Put to the human on 2026-09-02, who chose the same answer for a different reason**, and declined **resign** in the same breath (*not now — record it*), so the cost below is knowingly accepted rather than merely disclosed. `docs/vision.md` is unchanged. `ADR-0105` §*How this ADR was decided* carries the full record.

`DEC-107` → [`ADR-0104`](../docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md)
on 2026-09-01 — **a frame reaches the connection that is in the room it is about.** Raised the same day by
[`STORY-1214`](stories/STORY-1214-a-duel-played-by-hand-deadlocked-on-presence.md) out of a duel the human
played by hand and could not play, and measured on the wire: `OpponentPresence(AWAY, 60000)` arriving
**320 ms before the recipient sends `CreateRoom`**, with a roomless controlled negative clean in both
directions. **The answer: `deliver` resolves a seat to a player exactly as it does today and then asks for
that player's writer *for this room*** — a connection in another room, or in none, is skipped silently, which
is the idiom `deliver` already documents for a seat mid-reconnect.
**`ConnectionDirectory.writerFor(player)` is deleted**, not merely joined by a scoped sibling, so no unscoped
lookup survives for a later call site to reach for; `RoomMembership` moves to `duels.poker.server.session` and
is registered with the writer it belongs to, one object shared by reference with a **`@Volatile`** `code`,
because the ticker and the other seat's socket read it and a stale read is invisible on one thread.
**`deliver`'s signature does not change and all seven call sites are untouched** — the one edited line is
`connections.writerFor(player, room.code)`. **`PROTOCOL_VERSION` does not move**: no `ClientMessage` or
`ServerMessage` declaration changes, `ADR-0047` §2's fingerprint is unchanged, `docs/protocol-versions.md`
gains no row, and **`TASK-121403` is not `atomic:`**. *(That last clause is about the bump, and the bump is
real: the version does not move. The `ADR-0069` probe run for the 2026-09-02 re-cut found a different merged
gate — the Kotlin compiler — so the ticket does declare `atomic:`, at 6 files. **`ADR-0104` §6 is now amended**
to say so — corrected 2026-09-02, after review noted the correction was living only downstream, on the precedent
of `ADR-0068` §6 amending `ADR-0047`'s artifact count in the ADR itself. A ticket's size is measured, never
inferred from the absence of a version step.)* **Production is unchanged** — `disconnect()` still
builds the frame whether or not the other seat is connected to that room, because `RoomRegistry` knows nothing
of writers by design and `Room.presenceOf` is a pure projection; that half of the registered question is
answered *yes, unchanged*. **`ADR-0028` §1 is upheld rather than reopened**: `OpponentPresence` gains neither
a room nor a seat, because §1's reason — *"a second thing to get wrong at the one place that already decides
where a frame goes"* — is precisely the reason to make that one place correct. **The story's reading was
confirmed, and it understates the defect**: the widening is in the composition `seat → PlayerId →
writer-anywhere`, so it carries **nine of `ServerMessage`'s eleven subtypes**, counted from every
`Addressed(...)` construction in `poker-server` — `Snapshot`, `Events`, `YourTurn`, `DuelFinished`, `Rejected`,
`Failure`, `ActedForAbsent`, `RematchOffered`, `OpponentPresence`; only `Welcome` and `RoomJoined` are never
routed, and both are direct replies on the asking socket — and needs no race at all, since nothing on the wire says
*leave* (`ADR-0072`) and a held seat's frames follow the player wherever they connect. **No card can leak**:
the recipient is always the player the frame was projected for; the widening is about which of that player's
screens receives it. The client's store is scoped too — a `RoomJoined` naming a **different** room
re-initialises it, counters carrying over — and the ADR says in as many words that this half is **not** what
makes the system correct and guards only the one in-flight window a server check cannot close, which the
shipped client cannot currently open. **The unreproduced fold is treated as a mechanism that fits the report,
never as a cause** — `ActedForAbsent`, `Events` and the `Snapshot` after them reach a player who left the
room, and the same rule closes that path whether or not it was what happened. **Costs named**:
`ConnectionDirectory`'s documented ignorance of rooms is retracted; a `@Volatile` obligation nothing in the
type system asks for, whose omission fails invisibly and never on one thread; a silent drop gains a second
indistinguishable cause with no counter and no log; membership-before-delivery becomes load-bearing ordering
pinned by a test rather than a comment; the unscoped ***send to a player wherever they are*** is foreclosed
deliberately, with **no current victim** — `writerFor` has one call site and this is it, so the cost is
prospective and falls on whatever first needs it; and §4's client branch is unreachable from today's UI, so it can rot
green. **The deadline argued for the option not taken and is recorded anyway** — no client is deployed, so a
wire break is free today and never again; it was declined because the frame is not where the defect is.
`DEC-108` is untouched, and **`DEC-109` is split off for the product owner**.

`DEC-106` → [`ADR-0103`](../docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md)
on 2026-09-01 — **the table fits the phone, and the cards give before the numbers.** Raised out of round
1 of `/qa-cycle audit smoke` (run 2026-08-31; [`STORY-1213`](stories/STORY-1213-round-1-audit-three-criteria-unmet-and-a-table-that-names-the-wrong-winner.md),
`R2` `not met` at four decision beats: 885/664 preflop, `bottom: 820.578` on Fold and All-in facing
a raise, 868/664, 866/664) and by
[`TASK-121302`](tasks/TASK-121302-the-decision-fits-a-390-by-664-screen.md), which said in as many
words *"No `DEC` is needed here — this is conformance to a merged card"* and could not close it:
`min-height` is a floor, `flex-grow` only distributes slack, and the card holds **one** rule that
narrows with the column (`--bw`, read by the board alone) while the hero's hole cards and the
rival's mini hand are hardcoded at `--w:96px` and `--w:40px`. **Measured headless this run, the
merged card is 732 against 664 at 390 × 664** and 900/900 at 720 × 900, with `.bar` ending at 715.7
— so a client transcribing the card *perfectly* still fails `R2` by 68 px. The card was drawn for a
wider screen and describes no phone, and inventing the missing clamp is the guess `CLAUDE.md` rule 5
forbids. **The answer: the whole column fits 390 × 664 at every beat** (`scrollHeight ≤
clientHeight`, no scroll to act) **and it is one table at two widths** — every element in the same
order with the same words, measurements narrowing *continuously* with the column's own width, so
there is no width at which a different table appears and none of `ADR-0096` §4's three
second-surface tests is met. *"A player scrolls"* was a question but not an available answer:
`ADR-0096` §2 already rules it `R2` `not met` at either shape and forbids a relaxed phone bar by
name. **What gives, in order and exhaustively: whitespace → the rival's face-down hand → the
player's own hole cards, with a floor of never smaller than a board card → the board, last of the
card groups. Nothing else gives** — both stacks, the pot, the bet lines, the amount to call, the
sizing row and the action buttons keep their type size, their labels and their place (`R3`). The
action bar **may grow** — its two rows wrap at 390, measured 59 and 61.5 at 390 against 32 and 44.3 at 720 — because
wrapping is fitting, not giving; truncating or hiding is neither. Running out of the list re-opens a
decision rather than accepting a scroll, and a fit was probed to exist at **664/664** with nothing
removed. **`design/screens/duel-table.html` carries it**: amended so the two hardcoded widths narrow
like `--bw`, plus a **second frame at 390 × 664 whose markup is the same**, boxed in height as well
as width — a separate phone card is refused, and the line drawn is **markup identity, not file
count**. One frame answers every beat, because the card already reserves every slot so the table's
height does not vary by beat. **`TASK-121302` is rewritten by the planner, not amended, and blocked
on that design ticket** — design precedes client; its height-budget half (`min-height: 100dvh`,
`flex: 1`, one column where there are two) survives as necessary but not sufficient, and its file
set is measured rather than copied. **The named costs**: the player's own hand is smaller on a phone
— the size of what you read once traded for the presence of what you press every beat; the table
acquires a **size budget**, so a clock, a hand-strength line or chat must buy space from something
already there or not ship; round 1's fix set grows a dependency it did not have and `R2` is counted
`not met` again if round 2 runs before both tickets land (`ADR-0096` §5 — *filing does not reduce
`A(N)`, only repair does*), so the metric reads worse than the work is; two frames drift with no
gate that notices; and the give order becomes a merged constraint on design taste. `DEC-103` and
`DEC-104` stay **open** and are untouched, orientation stays `ADR-0097` §5's, and no viewport
smaller than 390 × 664 is promised. Registered and answered in the same PR (the `DEC-039` path); it
never appeared in an open table.

`DEC-105` → [`ADR-0102`](../docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md)
on 2026-09-01 — **a hand ends in steps, and the client owns the clock.** Registered open the same
day by `TASK-121301` and routed by `ADR-0096` §2, and answered for **the architect** in both halves.
**Where the pacing lives: `web-client`.** When a `Snapshot` says the hand is over, the store paints
it as a **sequence of steps** — one per `StreetDealt` in the `Events` frame that immediately
preceded it, then a final step carrying the whole snapshot and `ADR-0095`'s award line — and
**queues every frame that arrives while steps remain**, in arrival order, applying them when the
last step has stood. Exactly two fields lag: the board, as a **prefix of the server's own
`view.board.cards`** at a length that is its own length minus the cards carried by the steps still
to come, and the street label, which is that `StreetDealt`'s own `street`. A `Snapshot` that does
not end a hand is applied at once, with no timer — **ordinary play costs nothing, so `R1` cannot
regress at the other seven beats**. **What a step costs: 600 ms**, named once in
`web-client/src/store/boot.ts`, with **`0` meaning synchronous** so `drive-duel.tsx` boots at `0`
and `ADR-0100` §3's *no frame is re-recorded and none of the four e2e files is edited* survives
intact. **A reconnecting or reloading client jumps to the end**, structurally rather than by a flag:
`resumeFrames` passes `newEvents = emptyList()`, so a resuming seat is sent no `StreetDealt` and
there are no street steps to take. **The ticket's source reading was confirmed on three points and
incomplete on a fourth that decides the answer** — `DuelAction.kt`'s `act` calls `advance` in the
same call and `deliver` sends both hands' frames in one batch (the merged
`DuelActionTest.afoldEndsTheHandAndOpensTheNext` asserts snapshots for hand 1 and hand 2 in one
`outbound`), so a client that only sliced the board would be overwritten a millisecond into the
first step, and `ADR-0095` §4's banner is today drawn and erased in the same delivery. §6 meets *the
server is authoritative* head on: **the client may choose when to paint a fact the server sent,
never what the fact is** — `ADR-0095` §2 had already settled that rendering a server-stated value is
not asserting a game fact, and §3 fixes the invariant that **the only thing ever withheld is the
tail of the board**, with `no-derivation.test.tsx` byte-unchanged. **`PROTOCOL_VERSION` does not
move**, and would not have moved for a server-paced answer either (`ADR-0047` §2 hashes
declarations); the server option lost on the engine handing out one state per runout, a wall clock
in the authoritative path, and its inability to fix the batch it creates. Costs named: a runout's
**stacks settle at the first step**, so the outcome is legible from the seat plates 1.8 s early;
every hand ends 600 ms later; the screen trails the server by design for up to 2.4 s and a queued
`OpponentPresence` over-states its grace; the two seats pace independently; and `DEC-104`'s `Pot 0`
is held on screen longer, unfixed and still the product owner's. **`TASK-121301` is unblocked**: it
stays **one atomic `module: web-client` ticket**, re-cut whole by the planner — `Files`, `Tests` and
`verify:` all replaced, the `manual-verify` label kept — and its `status:` flips from `blocked` with
that re-cut, in the same PR, so this board and the ticket never disagree.

`DEC-101` → [`ADR-0101`](../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md)
on 2026-08-31 — **`pot` means a pot-sized raise, and the fractions share its base.** Registered open
hours earlier by `ADR-0100` §6, which settled the technical half — a named preset **computes the
quantity it is named for**, and the pot reaches `ActionBar` through `Lobby.tsx` with no wire change
— and refused this one: the card's single worked example (`Pot 2,450`, the rival `committed 400`,
`Call 400`, the `pot` chip selected, `Raise to 3,250`) fits **two** formulas that agree only because
the hero has committed nothing in that frame, and excludes the 3,650 a poker player expects. The
answer is one rule over one base. **The base is the pot as it will be after the call** — `P =
view.pot + both seats' committedThisStreet` (`ADR-0100` §6's number, unchanged) plus `toCall =
callTo − the acting seat's committedThisStreet`, which is what the call *costs* rather than what the
commitment must *become*. Then `min → minRaiseTo` (or `minBetTo`), `⅓ → callTo + floor(base / 3)`,
`½ → callTo + floor(base / 2)`, `pot → callTo + base`, `all-in → allInTo`, **rounding down** because
chips are integers and under-committing is the side the label promises. With no bet outstanding
`toCall` is 0 and the base is simply the pot, so the same rule gives *half the pot* when betting and
a half-pot raise when raising. **Licensed by the first line of the vision's *What it is*** —
*"Heads-up Texas Hold'em. Two players. Never three."* — the product is the game, so a control's word
is the game's word, and `docs/duel-rules.md` is where a house variation would have to be settled, by
a ticket, not by a label on a chip. **A fraction chip is offered only when its own amount is legal**
(`floor ≤ amount ≤ allInTo`): not clamped, not greyed, absent. So `TASK-120908`'s *"a preset the
stack cannot afford is not offered"* stands and gains the bottom end; hiding costs a shortcut and
never a legal amount, because the chip goes exactly when its amount is one the server would refuse;
and `min` and `all-in` are legal by construction and never absent, which is what keeps `ADR-0100`
§3's *no frame is re-recorded* true of this answer as well. **The named cost**:
`design/screens/duel-table.html` is now in arrears by one number in two places — the hero frame's
stepper and its `Raise to` button read `3,250` with `pot` selected and become **3,650** — so a
merged card is wrong, a design ticket exists that nobody planned, and until it lands a UAT round
reading the card would file a correct client as a defect. Also costed: the row changes shape between
turns, chips shifting under a finger on a clock; preflop the button gets `min` and `½` at the same
300 and **no `⅓` at all** (the base is exactly two big blinds, so a third-pot open does not exist in
heads-up hold'em), leaving an ordinary 2.2–2.5bb open reachable from no chip; and the chips can no
longer be defined from `LegalActions` alone, so `ADR-0100` §6's *"reversed by deleting one prop"* is
false — deleting it now deletes three chips. **`TASK-120908` keeps `ADR-0100` §7's six files**; what
moves inside them is that `Lobby.tsx` hands the bar one more published number (the acting seat's
`committedThisStreet`), its *each preset sets the amount its own name states* test needs at least
one frame where the hero has **already committed** — at `committedThisStreet: 0` the call's cost and
`callTo` are the same number, so a fixture there proves nothing about the term the whole
disagreement lived in — and `bar-no-derivation.test.tsx`'s first test survives untouched, because
the chips are labelled by name and carry no figures. The stepper's step was split off as `DEC-102`
and refused.

`DEC-100` → [`ADR-0100`](../docs/adr/ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md)
on 2026-08-31 — **the driver reaches an amount by pressing what a player presses.** Raised the same
day by `TASK-120908`, whose coder replaced the table's range input with the card's five presets, got
all three of its new tests and the whole of `ActionBar.test.tsx` green at 25, then hit **24 failures
across four merged e2e files** for a reason outside its two-file budget and routed rather than
widening scope: `drive-duel.tsx:125` reaches an arbitrary amount with
`fireEvent.change(getByRole("slider"))`, and deleting the input makes that query throw in
`whole-duel.test.tsx`, `duel-secrecy.test.tsx`, `claimed-here-recovered-there.test.tsx` and
`drive-duel.test.tsx`. The answer keeps the driver on the controls a player has: it finds the action
button by the recorded verb as it does today, **reads the total that button prints**, clicks if it
already matches, otherwise presses each sizing control in document order re-reading after every
press, and **throws** naming the recorded amount and every amount reached when none matches. That is
strictly more than the deleted line proved — `fireEvent.change` set a value and trusted it; this
proves the bar reached the amount before `whole-duel.test.tsx`'s
`expect(actsSent).toEqual(recordedActs)` proves it encoded it. **The premise the question was raised
on turned out to be false, and measuring it is what made the answer cheap**: the script is
generated, not authored — `playDuel` draws every bet/raise amount from `minRaiseTo` or `allInTo`
alone — and all four amount-carrying steps in the committed fixture land on a chip (three
`allInTo`, one `minRaiseTo`), so **no frame is re-recorded and not one of the four failing files is
edited**, which is the evidence that nothing they prove was traded away. §4 disposes of a second
breakage the two-file budget hid: three recorded raises are `Raise to allInTo`, so the sizing row
must set that total **without** sending, while one recorded `AllIn` needs an `ALL_IN` button — and
`BettingRules.kt` offers `ALL_IN` with no `RAISE` to a short stack facing a bet, so a chip can never
stand in for it. The card's three-button actions row is therefore **one drawn state, not a law over
all states**, and no card is in arrears. §5 refuses **by name** a driver-only slider, any test-only
prop, `data-testid` or exported setter, and any reach into state or `actFrame`. **The named cost**:
the driver's reach is now the sizing row's, so no scripted duel can ever exercise an **interior**
amount — nothing is lost today, because `playDuel` cannot produce one, but a rounding fault correct
at both boundaries and wrong between them is now invisible to the whole-duel proof, and the repair
is §8's stepping search plus a server policy change. Also costed: the bar can read all-in twice, and
`TASK-120908` grows from a two-file `S` into a **six-file atomic ticket that was blocked** on
`DEC-101` (answered hours later by `ADR-0101`, above) — `ActionBar.tsx`, `ActionBar.test.tsx`, `drive-duel.tsx`, `Lobby.tsx`,
`bar-no-derivation.test.tsx` and `turn-fixture.ts`, with `whole-duel.test.tsx` named as the merged
gate that forbids splitting it (`ADR-0068` §3). Its product half — what `pot`, `⅓` and `½` each
compute — was split off as `DEC-101` and left for the product owner. Registered and answered in the
same PR (the `DEC-039` path) — it never appeared in an open table.

`DEC-099` → [`ADR-0098`](../docs/adr/ADR-0098-the-wordmark-belongs-to-the-front-door-alone.md)
on 2026-08-31 — **the wordmark belongs to the front door alone.** Raised the same day by
`TASK-121004`, whose third scope item quotes `<h1 class="text-title">Poker Duels</h1>` and directs
dressing it as `create-duel.html`'s front-door frame draws — but the markup lives in
`web-client/src/App.tsx` above `<Lobby />`, not in the ticket's declared `Lobby.tsx`, and renders
unconditionally on every surface the client has, so dressing it in place would have put the front
door's wordmark on all of them. The coder shipped the other two scope items (PR #1234) and refused
to guess this one (`CLAUDE.md` rule 5). The answer is **mostly a reading of merged sources, and
says so**: eleven card files under `design/screens/` hold exactly one `.mark` between them —
`create-duel.html`'s front-door frame — the four secondary-screen cards were composed under the
shipped global heading and none drew it, and even the waiting frame gives its top to the code, so
the lockup renders on the first screen's pre-create branch and **nowhere else**, the `<h1>` leaves
`App.tsx`, and no product-name chrome replaces it. Confirmed by the vision's *Positioning*
(*"dark, quiet, fast, minimal"*) and the success condition, played on the screens the mark stays
off; `ADR-0060` §5 had already refused chrome above the table once. **The named cost**: `ADR-0094`
seats the invited half of the product straight into a dealt hand, so those players now never read
the product's name on-page — only the tab title — and the ADR refuses the cheap fix (persistent
chrome) without designing a substitute nobody has asked for. **No card is in arrears — the client
was.** The struck scope item becomes a follow-up client ticket over four files: `App.tsx` (the
`h1` leaves), `Lobby.tsx` (the front-door branch gains the lockup; `CoinMark` exists),
`App.test.tsx` (its four plain-title assertion sites cannot stand and are rewritten deliberately)
and `Lobby.test.tsx` (gains the lockup assertion; its zero-headings test meets the lockup).
Registered and answered in the same PR (the `DEC-039` path) — it never appeared in an open table.

`DEC-098` → [`ADR-0099`](../docs/adr/ADR-0099-the-rubric-is-the-adr-section-and-a-criterion-is-born-merged.md)
on 2026-08-31 — **the rubric is the ADR section, and a criterion is born merged.** Registered by
the planner splitting `STORY-1212` and answered the same day, before any round cited a criterion —
the free window its own *Due* named. The audit rubric is `ADR-0096` §2 **itself**, and **no
working document ever exists**. The deciding force sat in neither reading's list, because
`ADR-0096` §3 already routes every proposed criterion to a `DEC` whose answer is *"a merged PR
either way"* — so every future criterion arrives as merged ADR text whatever this decision said,
and a working document could only ever have held a **copy**. A copy of the audit's own law is a
second register from its first line — downstream of the ADRs forever, able to disagree with them
silently, checked by nothing — the shape `ADR-0092` §8 priced (*"two copies of a rule drift"*),
`TASK-120705` refused one focus earlier, and `STORY-1212` wrote into every ticket pending exactly
this answer. Growth is **one amending ADR** in the pattern `ADR-0092` §2 and `ADR-0097` §4 used on
`ADR-0090` §2's declared-file set: ids sequential and never reused (`R6` is next, forever), the
new criterion stated in §2's own three-column form, the **resulting priority order restated as ids
only — never another criterion's text** (`ADR-0096` §5 repairs top-down, so a rank must be taken;
ids have no wording to drift), and `ADR-0096`'s index row annotated *rubric grown to N* **in the
same PR** — the same-PR clause being load-bearing because its precedent lagged once: `ADR-0090`'s
row still read *"grown to four by 0092"* a day after `ADR-0097` §4 made the set five, observed and
repaired in the answering PR. **A round record cites `R<n>` and the ADR section that states it —
`R2` (`ADR-0096` §2) today — never a document path**, which was the half that mattered for
tickets: `TASK-121203` and `TASK-121205` already cite the decided form, so **neither changes a
word**, and their refusals to transcribe stop being *pending `DEC-098`* and become merged law. The
freeze (`ADR-0096` §3) becomes a commit fact — the rubric in force for an invocation is the
amendment chain as merged at the commit its first round names — and §6's metric stays countable
from two dated registers: growth ADRs in the index, invocations in the round stories. The named
costs: the rubric is never again one table once it grows (every consumer assembles; the reversal —
founding a document later, transcribing once at that day's size — is priced for the day the chain
is the demonstrated obstruction); the index annotation is a hand-maintained discipline, not a
gate; and a sixth criterion can never be cheaper than a full merged ADR — slow on purpose.
`ADR-0090` §2's declared-file set stays at **five** — this ADR adds no file anywhere — and
`ADR-0096` §7's *"the rubric"* deletion and *"the ADR that says why"* collapse into one act, so
reversing the audit got cheaper too.

`DEC-097` → [`ADR-0097`](../docs/adr/ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md)
on 2026-08-31 — **a resize is two numbers, and the audit's observer is the fifth declared file.**
Answered by the architect, both halves, without returning anything to the product owner: the
mechanism `ADR-0096` §4 needs exists, so §4 stands exactly as merged and its second shape does not
move to its own round. **(b), the load-bearing half.** `ADR-0089` §3 is a **three-way** partition —
acts a player's hands reach (*click, type, navigate, reload, clear storage*), reads (*anything*),
and application-state writes (*forbidden*) — so read-versus-write was the wrong axis, and both wrong
answers were expensive: a *read* licenses every field on the CDP call, a *write* makes §4
unbuildable. **A viewport resize is an act**, the sixth member of that first list, and §3 stands
byte-unchanged. `scripts/qa/drive.mjs` gains one verb, `size <width> <height>`, which sends
`Emulation.setDeviceMetricsOverride` with **`width`, `height`, `deviceScaleFactor: 0`, `mobile:
false` and no other field**, reads the viewport back from the page, and **exits 1 if it is not the
viewport it was asked for**. **The classification is a property of the fields, not of the method
name**, and that is measured rather than argued: with the app's own `width=device-width` meta,
`mobile: true` applies mobile shrink-to-fit, widens the layout viewport **390 → 520** to contain
overflowing content and makes an `R2` *not met* read **`met`** — a silent false pass in the
criterion `ADR-0096` predicts will fire hardest — while fabricating `screen` and
`devicePixelRatio`, which puts any finding outside `ADR-0089` §4's reproducibility test. It also
buys nothing: `pointer: coarse`, `hover: none`, `maxTouchPoints` and `ontouchstart` were unchanged
in all three states, because touch is a separate CDP domain. **Three measured facts make the verb
possible at all** in a driver that runs one process per verb: the override **survives the session
detaching**, **survives `Page.navigate`**, and is **per-target**. And the live-tab claim was
measured directly — across a 390 × 664 → 720 × 900 resize the page's JS identity value was
byte-identical (so a live socket and a seated player survive it), exactly one `resize` fired, and a
`min-height: 100dvh` column re-measured 664 → 900. So §4's two shapes are **two measurements of one
tab**, and `ADR-0018`'s mid-duel re-seat is never approached. **Both tabs are resized, read and
returned to `phone` together**, because moving only one confounds the shape with the seat and the
finding stops being attributable. `Browser.setWindowBounds` — the honest-by-construction fallback
`DEC-097` anticipated — is **rejected on measurement, not preference**: Chrome clamps a window to a
**500 px minimum width**, so a request for 390 × 664 produced a **500 × 577** viewport, and the
window carries 87 px of chrome the viewport does not, so 720 × 900 produced **720 × 813**.
`ADR-0096` §4's numbers are viewport numbers — 664 is the smallest `100dvh` the column is asked to
fill — and window sizing cannot express either of them. **(a) The declared-file set becomes five.**
`.claude/agents/audit.md` is licensed to **mention** `qa-cycle` in the one stack-lifecycle sentence
`qa.md` and `uat.md` both carry, and never to invoke it, on `ADR-0092` §2's precedent — and it is
amended because `ADR-0092` §8's own test is met, not because a focus was added: the briefs
contradict at the sentence level, since `ADR-0096` §2 froze `ADR-0092` §3 **byte-unchanged for `qa`
and `uat`**, where an observation with no card, token or literal behind it is a **question** capped
at three, while under the audit it is a **finding** against a rubric criterion. One file switched by
a scope word is the leak §8 built two files to make impossible, and the evidence it is not
hypothetical is round 3 promoting **zero** questions, correctly, on a product the human called raw.
`ADR-0092` §8 otherwise applies unamended — one manager, one ledger, no new skill, no `Write` on the
observer, and `qa-manager.md` still names the cycle nowhere. The check gains `audit` and was
verified to exit **0** on today's tree, **0** with the five, **1** with a sixth. **Portrait only.**
The human settled orientation after `ADR-0096` merged — ***"we are ok to support only one
orientation for mobile form factor"*** — and §5 records it where §4 left the question: no rotation
handling, no reflow decision, no second mobile shape, and `screenOrientation` is the third field
`size` pins by omission. `ADR-0089` §§2a, 2b and 2c are re-checked one at a time — no package
enters, the verb spawns no process so no denied verb is approached, `build.yml` keeps its two jobs —
with the corollary this focus needed said out loud: **a shape walked is not a surface supported.**
It also refines one stated reason in two merged ADRs without moving either decision: the ~500 px
clip `ADR-0092` §2 and `ADR-0096` §4 both attribute to headless *capture* belongs to window
*sizing*, and an overridden 390 × 664 captures faithfully at exactly 390 × 664 — so `shot` keeps
working at the shape the whole duel is walked at. **The costs are named rather than waved at**: a
resize is a real event, so the `record`/`frames` evidence `R1` rests on gets noisier at exactly the
beats §4 doubles; the current shape is *tab* state, so a **forgotten** restore has no catch, only a
printed transcript line; the field discipline is a convention in one file that fails toward a false
pass and no CI job may guard it, because §2b forbids one; and device emulation is foreclosed
permanently, so a future criterion about tap targets or hover-only affordances returns as a new
`DEC` rather than being widened into by a ticket.

`DEC-096` → [`ADR-0096`](../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md)
on 2026-08-31 — **the audit judges a whole duel against a frozen rubric, and no round may grow
it.** Raised by the human after playing the product in two browsers immediately following the
`/qa-cycle uat regression` that ended `PASS` with `B(3) = 0`. Three merged rules made every item
they hit unreportable, and the ADR diagnoses each rather than asserting the gap: `ADR-0092` §3's
classifier has **no merged source of the general kind** to contradict — every source it lists
describes a *particular screen*, so *the pot is illegible* could only ever be a question;
`EPIC-12` §Termination rule 2 puts everything that makes a product feel unfinished in the
`medium`/`low` band that is *"never scheduled by this cycle"*, and **seventeen `status: backlog`
tickets from `EPIC-12` rounds are sitting there now**; and coverage is **per-screen**, so pacing —
a property of a sequence — has no row to be reported in, which is why *the round ends immediately
after an all-in* went unseen while `poker-engine`'s `StreetProgression.runOutBoard` was already
emitting the flop, turn and river as three separate events *"so the log reads like the deal it
was"*. Answered by the product owner from `docs/vision.md`'s first success condition — ***"Send a
link. She opens it in a browser. We play a full heads-up match. Someone wins. We hit
Rematch."***, with *"Everything else is downstream of that moment"* — which is a sentence about
**one continuous act**, and so licenses a walk of eight **beats** rather than eleven screens, the
all-in runout among them. Three halves were **stated by the human and recorded verbatim, not
chosen**: the audit may file findings contradicting no merged source, the benchmark is category
quality with the vision's aesthetics, and audit findings are repaired regardless of severity.
**The merged-source rule is relocated, not removed** — a finding contradicts a **criterion** in a
merged, closed, five-line rubric — and **termination is the rubric**: `A(N)` counts *criteria*
answered `not met`, so the ceiling is known before a round starts; the rubric is **frozen for the
invocation**, no round may grow itself, and `PASS` at `A(N) = 0` says the list is satisfied and
never that the product is finished. **No severity and no backlog under this focus**: a finding
deferred by the eight-cap stays counted. Registered and answered in the same PR (the `DEC-039`
path), so it never sat in the open table above. Registered `DEC-097` — **the architect's**, the
mechanism — which
[`ADR-0097`](../docs/adr/ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md)
answered the same day, above, so §4 is buildable as merged. Its one escalation to the human — **is a phone a supported surface?** — was answered the
same day in their own words, ***"we have to support phone size"***, and §4 is rewritten around it
rather than reasoned toward it: the walk runs at **two shapes**, the whole duel at `phone`
**390 × 664** and `R2`/`R3` again at `laptop` **720 × 900**, with **one bar checked twice and never
two bars** — only the criteria's quantifier moves, and `A(N)` still counts criteria, so the round
got longer and the fixed point did not move. **`docs/vision.md` is not amended**: *"She opens it in
a browser"* and *"Two browsers, one room link"* name a browser and **no device**, so the human's
call resolves a silence in the direction the vision's own words already permit rather than
contradicting a sentence — and §4 names what would change that answer (a second layout, a reduced
feature set or a separate application would be a real second surface and would belong in the
vision). **Phone landscape was left open by §4 and is now answered** — the human's *"we are ok to support
only one orientation for mobile form factor"*, recorded by
[`ADR-0097`](../docs/adr/ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md) §5.

`DEC-095` → [`ADR-0095`](../docs/adr/ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md)
on 2026-08-30 — **the table states who took the pot, and never names a hand.** Answered by the
product owner, deriving from two sentences of `docs/vision.md`: *On variance* — *"**Luck decides a
hand.** Skill decides whether you come back tomorrow"* — for the banner, since the vision names the
hand as the unit at which luck lands and a table that resolves one in silence asks a player to
absorb a variance it never told them about; and *What it is* — *"Every hand is stored as an event
log, so a match can be replayed and analysed **afterwards**"*, with *Positioning* and the roadmap
(the replay viewer is v0.4) — for the refusal. Registered and answered in the same PR (the `DEC-039`
path), so it never sat in the open table above.

**The question.** `design/screens/duel-table-states.html` draws two hand-ending frames and the
client renders no banner at any tick — `PotStrip.tsx` has one `return` and no branch that could
carry one. The card's banner has two lines and only one is buildable: `PotAwarded` carries `seat`
and `amount`, so *You win 4,850* is a transcription, while *Two pair, aces and sevens* is on no
`GameEvent` and could only be computed by the client, which `CLAUDE.md`'s non-negotiables and
`ADR-0002` forbid and `no-derivation.test.tsx`'s `HAND_TALK` matcher gates.

**The answer: yes to the banner, no to the hand name.** At `street: COMPLETE` the table replaces
`Pot N` with exactly one line — `You win 4,850`, `Your rival wins 4,850`, or `Split pot — you win
2,425` for a split, always the **viewer's own** award because the odd chip can make the two shares
differ (`duel-rules.md` §Showdown). The facts line beside it is untouched, nothing is added beneath
it, and the line lives while the street is `COMPLETE` and goes when the next hand starts — never on
a timer, never on a fade (`ADR-0046` §2, `ADR-0075`). **No hand is named anywhere on the table**, at
any street, in text or in an `aria-label`; the card's showdown line goes, and so does its fold line,
because `settleHand` returns the uncalled bet *before* it awards the pot, so the printed amount and
the stack movement already agree to the chip. No engine, server, wire or `PROTOCOL_VERSION` change:
`PotAwarded` passes `EventRedaction` unfiltered and already lands in `DuelState.narration`.
**`no-derivation.test.tsx` is not touched** — its fixture is a `street: "TURN"` view, so a correctly
triggered banner leaves it green, and a coder who meets it red has built the wrong trigger.

**Costs named:** a player who cannot read seven cards is not helped, and that falls hardest on the
person the vision was written for; a deferred protocol change is a dearer one, and today a
made-hand field would cost a version bump with no deployed client to migrate; two human-accepted
card frames lose a line each; a reload during the between-hands pause loses the statement, because
the banner is a moment and no `PlayerView` field is added to make it survivable; and the split line
never states the rival's share. **It forecloses** client-side hand naming permanently, at every
street — by an ADR now, not only by a test — and forecloses naming a made hand *at the table*; the
event log still holds every revealed card, so a replay viewer (v0.4) or `ADR-0005`'s analysis
interface can name one from stored data with no wire commitment. **Reversal has a named trigger**:
the first duels played by people who are not the author showing that a player cannot say why they
lost, at which point the fix is a **server-sent** descriptor and the architect's wire question
arises. Nothing is raised for the architect today: `TASK-121101`'s technical half was conditional
on a *yes*.

`DEC-092` → [`ADR-0094`](../docs/adr/ADR-0094-opening-the-invite-is-taking-the-seat.md)
on 2026-08-30 — **opening the invite is taking the seat, and the two join cards are corrected to
it.** Answered by the product owner, deriving from the first success condition — *"Send a link.
She opens it in a browser. We play a full heads-up match. Someone wins. We hit Rematch."* — which
has five verbs and not one of them is *accepts*, and which the vision follows with *"Everything
else is downstream of that moment."* Registered and answered in the same PR (the `DEC-039` path),
so it never sat in the open table above.

**The question.** `design/screens/join-duel.html` draws an offered seat — *ImKate challenges you*,
a stakes line, a room-code chip, *Take the seat* — and `design/screens/enter-code.html` draws a
screen of its own for typing a code. The client has neither: an invite link seats the joining
player straight into a dealt hand, and a code is typed inline on the first screen. Three
consecutive UAT rounds (`STORY-1205`, `STORY-1209`, `STORY-1210`) filed the same `high`, because
`ADR-0092` §3a judges the shipped screen against its merged card and nothing said which artefact
was the product.

**The answer: the client is the product, and the cards are what change.** The invite path renders
no screen — presenting the code *is* taking the seat (`ADR-0022`: *"holding a room code is the
invite… Whoever presents it takes the second seat"*, priced as *"one click with zero friction for
the invited"*) — and the code field stays on the first screen, which `ADR-0060` §§1 and 4 already
describe as carrying *"the join form"* and *"the room code box"*. No client, server, wire or
`PROTOCOL_VERSION` change, and no `docs/test-plan.md` `expect` moves: `SMK-05`, `CORE-02` and
`CORE-05` describe the blessed product and keep passing. `TASK-120907` becomes a `module: design`
ticket; `enter-code.html` keeps its path (three registers cite it, `ADR-0092` §4's dedupe key
*is* the path) and stops claiming to be a screen; `join-duel.html` has no subject left, and
whatever the ticket does with it, no register may be left citing a path that is gone.

**Costs named:** a link is a commitment with no decline — the seated player's only exit is to walk
away and let `ADR-0023` play the hands out, at the cost of the coin `ADR-0014` stakes, and a link
forwarded to a group chat gives the seat to whoever clicks first with no way for the host to evict
them; the guest never learns who challenged them until they are in; two human-accepted card frames
are thrown away; two `backlog` tickets (`TASK-120907`, `TASK-120911`) now touch one file. **It
forecloses** a pre-join view of a room in v0.1, with the reversal trigger named: the day a duel's
terms stop being constant (`ADR-0035` leaves stack and blinds as configuration), the screen would
show a variable rather than a constant and the question is re-argued. **It does not license** the
client's copy winning by default — strings are decided string by string by whichever merged source
owns them, which is `TASK-120911`'s job, not this ADR's. Nothing is raised for the architect:
`TASK-120907`'s technical half was conditional on a *yes*.

`DEC-086` → [`ADR-0093`](../docs/adr/ADR-0093-ready-for-real-users-is-said-of-the-shipped-artifact.md)
on 2026-08-30 — **"ready for real users" is said of the shipped artifact, and the bar is two
facts, neither of them a test result.** Answered by the product owner, deriving from the success
condition's second clause — *"She opens it in a browser"*: what she opens is the artifact the
product serves her, and today no proof of record describes it — every layer, unit to UAT, loads
`npm run dev` on `localhost` while `dist/` is opened by nothing (`ADR-0088` gap 3, still true).

**Fact a: the artifact under test is the artifact shipped** — the proofs of record (the
`ADR-0088` §2 hand-check and the QA/UAT rounds the human reads) load the built bundle; the
serving mechanism is **`DEC-087`, the architect's**, registered open above, and until it is
answered the bar is unmeetable, on purpose. **Fact b: recovery is completable by a real user in
the offered deployment** — a mail transport bound, never `NoRecoveryMailer` — because
`ADR-0031` §7 scoped the senderless build's validity to *"development and tests"*, an offering
to real users is neither, `ADR-0087`'s acknowledgement promises a link that must be able to
arrive, and `ADR-0031`'s Consequences price the failure as the product's one total, permanent
loss. **Meeting the bar does not make the product ready**: it is a precondition on the phrase,
never a certificate — readiness stays the human's judgment made by reading, no record may ever
be cited as the bar being met (`ADR-0089` §2c and `ADR-0092` §2 stand), and the bar gates the
phrase, never the act of offering, which stays the human's own. The event is `ADR-0063` §5's —
strangers beyond *"people the author invited personally"* — so the founding moment is not
gated. Five absences are out under a written rule (in the bar only if failure falsifies the
success condition at first contact or destroys what a player cannot get back): history paging
past 10, ladder paging, the third device, the hundred-way tie, real network — each a known
absence a round reports without it bearing on the bar. Costs named: the UAT pass cannot end at
the phrase the human aimed it at; the bar waits on `DEC-087` and a transport; §1b may cost a
mail bill; enforcement is prose plus the review gate.

`DEC-085` → [`ADR-0092`](../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md)
on 2026-08-30 — **a UAT round is a second focus of the same cycle: it files what a merged source
settles and asks the product owner the rest.** Raised the same day by the human's request for a
second testing pass after `/qa-cycle`, over the same catalogue, focused on UX and design
conformance; registered and answered in the same PR by the architect, with two halves settled by
the human mid-decision: **a UX question raised during UAT is delegated to the `product-owner`**,
whose decision may create a bug ticket through `build-epic`, and **UAT runs now, filing the six
missing cards**, rather than waiting for `ADR-0091` §5's retrofit story.

`/qa-cycle uat <scope>` is the human's own message and chains from nothing — `ADR-0089` §2b as
amended by `ADR-0090` holds word for word, neither focus prints the other's command, and a
preceding QA cycle is practice, never a checked precondition, because the check would cite a round
as a gate (§2c). The three standing conditions are checked one at a time and hold: a `shot` verb
captures screenshots over CDP with Node built-ins, into the round's temp directory, never
committed, **no image-diff tooling ever**; a screenshot and a rendered card are **reads** under §3;
and §4 transposes to *observable by a human looking* — at the screen and the rendered card beside
it, by eye. **The classifier at the boundary is the merged source**: an observation files as a
finding only when it contradicts something merged — a card, a token, an owned literal, an ADR
section, a vision sentence — and a judgment with no merged source is a **question**, promoted by
`qa-manager` (at most three `DEC`s per round, one per screen, each a concrete choice answerable in
one sentence and bearing on a player's ability to tell what is going on) for the product owner,
whose answer becomes a merged source either way. **A missing card is `high`, outside `B(N)`** —
registered debt being collected, not decay — its repair **is** the card, its dedupe key is the
card's own path, and the screen is walked, not parked. `B(N)` gains its second and third
exclusions — missing cards and decision-born improvements — because counting either trips
`STOP_DIVERGING` on a cycle doing its job; a round that unlocks newly-carded screens is a
**baseline round** rule 4 skips, rule 5's three-round budget still binding; and every UAT verdict
line is qualified inline (`PASS (conformance unjudged on 6 of 7 screens)`) so a round that mostly
authored cards says so in the one line anyone reads. One new `uat` observer agent — opposite
refusal lists cannot share a file with `qa.md`, which stands byte-unchanged — one manager, one
skill, one ledger. `ADR-0090` §2's declared set grows three→four (`agents/uat.md`, mention-only).
Registers `DEC-086` open — the product owner's readiness bar — and hands the planner the
`SKILL.md`, `uat.md`, `qa-manager.md`, `shot`-verb and test-plan-section tickets. Costs named:
round 1's conformance is near-tautological until the pane verdicts land on cards composed from the
very screens they judge; up to nine Opus dispatches per invocation; three prose exclusions in a
count that used to be simple.

`DEC-084` → [`ADR-0091`](../docs/adr/ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md)
on 2026-08-30 — **design gets no agent: a new screen owes a card, and adoption is gated where it is
consumed.** Raised the same day by the human — *does design work need its own agent, or does it fit
the existing `coder`/`reviewer` workflow?* — registered and answered in the same PR by the
architect.

**No designer agent and no design skill**, because either would own no decision: `ADR-0024` §3 gave
taste to the human at the rendered card and structure to the `light` review, and neither recorded
failure was agent-shaped. `EPIC-04` and `EPIC-05` shipped six of the seven `Screen` members —
`duels`, `leaderboard`, `account`, `sign-in`, `verify`, `reset` — with no design card because
**nothing dispatched any design work at all**, and an agent nobody dispatches designs nothing. The
missing trigger becomes **the plan-story rule**, in `.claude/agents/planner.md` alone: a story
whose split adds a `Screen` member names the card it implements in `## Design notes`, or the
split's first ticket *is* the card; minting new visual language stays interactive with the human,
composing from the settled vocabulary is an ordinary dispatched ticket, and the human's visual
verdict may **trail the merge** — the named price is rework when a trailing look rejects a shipped
screen, bought so unattended runs never stall at a pane. `check-drift.sh` **stays out of
`web-client/`**: the zero-token grep that raised the question measured direct references while
`app.css`'s `@theme static` block rebinds every Tailwind utility to `var(--pd-*)` under three
merged client guards, so the real leak is raw lengths in arbitrary values — `380px` ×21, `560px`
×2, `460px`, `1.5em`, across fifteen files — and the client's own job gains a fourth guard that
refuses them, while the shell gate keeps guarding `design/` against itself in `tickets.yml`. The
debt is registered, not forgiven: `EPIC-06` reopens with a retrofit story for the six slugs (and
the account offer), its 2026-08-15 close standing as history, and `ADR-0024`'s generator trigger —
*"cards multiply or a grep actually fires"* — is recorded as **fired**, its promised ticket owed.
Hands the planner three cuts: the planner-rule edit, the length-guard ticket, the retrofit story.
Nothing in it was the product owner's.

`DEC-083` → [`ADR-0090`](../docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md)
on 2026-08-29 — **a skill may write the catalogue or run it, never both in one turn.** Raised the
same day by the human's request for *"one skill"* that writes the missing cases for existing
functionality and then runs a full QA cycle over them, and answered by the architect.

**The question was which of two readings `ADR-0089` §2b carries**, since a composite is literally
*"another skill invoking it as a step"* and plainly *"a human's command"* at the same time. §2 says
a condition that stops holding *"returns the question as a new `DEC-NNN`"* — so it was raised rather
than argued around. **The composition reading is taken, the automation reading rejected by name.**
*"A human's command"* forbids nothing unless it means the **immediate caller**: every automated
trigger has a human behind it at some distance, and provenance traced through one intermediary can
be traced through five. `ADR-0089` §Consequences gave §2b a future job — *"the sentence to point at"*
when someone proposes a nightly — and a clause satisfiable by relabelling the caller cannot do it.
§2's own closing sentence pre-refuses the argument form: *"condition **b** failing, not a refinement
of it."* And the composite's **sole** value is that the cycle starts while the human is elsewhere,
which makes it a **cron whose clock is the length of its first half** — so it fails even the
attended/unattended test the permissive reading proposes.

**§2b is amended in the open, heading and one sentence.** *"No gate"* becomes ***"No gate, and one
caller"***; *"a cycle is started by a human's command"* becomes *"a cycle is started by **the
human's own message and nothing else**"*, with `/qa-cycle` the first act of the turn it starts. The
rest of §2b and all of `ADR-0089` §§2a, 2c, 3, 4, 5, 6 stand **byte-unchanged**, as do `ADR-0088`
§1's body and §§2–5. No schedule is licensed: a nightly still needs `ADR-0089` §Alternatives 3's own
ADR, with rounds as evidence.

**What the human types instead — two commands, and the expensive half is still one.** `qa-cases` is
licensed to plan, write and land the suites `docs/test-plan.md` lists as *not yet written*
(`EPIC-04`, `EPIC-05`, `EPIC-06`) through `build-epic` and the ordinary review gate. It may not
bring the stack up, start a browser, dispatch `qa` or `qa-manager`, or invoke the cycle by any
route; **its terminal act is a report naming, verbatim, the command the human types next.** Then
`/qa-cycle epic EPIC-04`, typed by the human.

**On the worry that a composite would grade its own cases**: the `qa`/`qa-manager` separation
survives a composite untouched — `qa` still has no `Write` — so the blunt objection is answered by
the reviewed PR. What a diff-versus-ticket review cannot answer is whether a case's `expect` is a
claim the product ever made, and a composite hands `ADR-0089` §4 — *"a rule an agent follows, not an
exit code"* — a catalogue of **never-executed** cases in the same unattended stretch that step 4 is
merging production diffs. `SMK-03` is the evidence it is not theoretical. So every case `qa-cases`
writes carries a `source` column citing the merged decision its expectation transcribes, and **a
case with no merged source is not written**: it becomes a `DEC` for the **product owner**.

**A suite authored this way is provisional until its first round**, and `docs/test-plan.md`
§*Per-epic suites*' *"filled in when an epic is first tested, not before"* is amended in the same PR
to say so rather than left to disagree. Merged sources prove what was *decided*, not what *shipped*:
they cannot show that a screen exists, that a control is reachable, or that a literal has not moved.
So the provisional line stands on the suite until the round record that first runs it deletes it,
naming the cases that round corrected.

**The cost, named:** the three untested epics carry 24 Definition-of-done promises between them, so
an authoring pass roughly **doubles** a 26-case catalogue with cases nothing has ever run, and the
first round over one is spent largely telling a broken case from a broken product — `ADR-0089`'s
*"slower than the hand-check it was meant to relieve"*, arriving on round 1 rather than after months
of drift, and capable of ending **`PASS` with a dozen of its own cases found broken** because §4
excludes harness defects from `B(N)`. QA becomes the workflow's one unchainable step and a person
waits at an hour nobody can predict, **every round, forever**, against a failure that has not yet
happened once; the
condition binds committed files and not a prompt, so a human or a cron typing *"write the cases and
then run a cycle"* defeats it with nothing mechanical to catch them; three ADRs and two amended
headings now hold the browser rule; the `source` rule will slow the first authoring pass and raise
product-owner `DEC`s, so the catalogue arrives smaller and later than asked. Chosen in the direction
whose **mistake announces itself** — a visible wait generates its own evidence for reversal, an
invisible precedent about how conditions are read does not come out with a `git rm`.

`DEC-082` → [`ADR-0089`](../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
on 2026-08-29 — **a browser drives this client for a QA round, never for a gate.** Raised the same
day `EPIC-12` was written, and answered by the architect.

**It was inside `ADR-0088` §1's words, and the answer says so rather than reading them narrowly.**
*"No browser drives this client, here or in CI"* says *here*, and a driver under `scripts/qa/` is a
browser runner in this repository whatever invokes it. So the route is the one `ADR-0088` §5 priced:
amend the clause in the open. **The heading is amended and nothing else is** — it now reads *no
browser drives this client in CI, and no browser stands between a pull request and `develop`*.
§1's body, §§2–5 and `DEC-024`'s answer stand **byte-unchanged**: `EPIC-03` ships no fifteenth
story, §2's eleven-step hand-check remains the proof of record, `build.yml` keeps its two jobs, no
Playwright/Puppeteer/Selenium/WebDriver/Cypress dependency enters `web-client/package.json`, and
`ADR-0032` §4's *"still jsdom, still no network"* still holds of every test suite — the harness is
in none of them.

**Why the amendment and not a refusal.** §1's stated reasoning was a **ratio** — *"rejected on the
ratio, not on the principle"* — whose three cost terms were a third CI job, flake on pull requests
that are mostly markdown, and a `package.json` dependency. **None is incurred**: `build.yml` is
untouched, no PR waits on a case, and the driver has no `import` in 249 lines. And §2 of that same
ADR already has a person driving two browsers through this client, so what §1 separates is the
**position** — a hand-check that leaves a receipt, against a gate between a diff and `develop` —
not the browser.

**The permission is three conditions, jointly**: no browser dependency in any module; **no gate**
(no pull request, `verify:` block or ticket waits on a case; a human's command starts a cycle, not
a merge or a cron); **no coverage claim** (a QA round is a dated record, citable in no `Metrics`,
Definition of done or `verify:`). Any one failing returns the question as a new `DEC`. The driver
**reads anything and writes nothing** but `pd.roomCode`, because a case that seeds state to reach a
screen is a client asserting a game fact (`ADR-0002`).

**The clause `EPIC-12` §Termination lacked**: a failing case that does not reproduce by hand is a
**harness** defect — filed against `EPIC-12`, **excluded from `B(N)`**, never repaired in production
code. Without it a stale catalogue reads as a product getting worse and trips `STOP_DIVERGING` on a
healthy product, or step 4 merges a diff to satisfy a moved string. **The cost, named:** `ADR-0088`
§Consequences' precedent is weakened one day after it was set, `scripts/qa/` carries an unpinned
macOS-only Chrome path, `dist/` is still unproven, and the reproduce-by-hand rule is prose an agent
follows rather than an exit code.

`DEC-081` → [`ADR-0087`](../docs/adr/ADR-0087-forgot-your-password-is-a-door-on-the-sign-in-screen.md)
on 2026-08-28 — ***Forgot your password?* is a door on the sign-in screen, not a screen of its own.**
Raised the same day when `STORY-0417` was split, and answered by the product owner.

**Licensed by the vision's *Positioning* sentence** — *"The reference points are **Lichess** and
**Chess.com**, not PokerStars. Dark, quiet, fast, minimal"* — the same sentence `ADR-0083` derived
from. **Unlike `ADR-0083`, this one coins, and says so.** *Sign in* was merged player-facing text
before that ADR named a screen with it; *forgot your password* is player-facing text **nowhere** in
this product, so the search that answered `DEC-077` returns nothing here and the word had to be
chosen rather than found.

**The word is `FORGOT_PASSWORD_LABEL = "Forgot your password?"`**, one of four constants in
`web-client/src/account/recovery-text.ts`, with `FORGOT_PASSWORD_SUBMIT = "Send a link"`,
`FORGOT_PASSWORD_ACKNOWLEDGED` — *"If that address is verified on an account here, a link is on its
way. Follow it to set a new password."* — and `FORGOT_PASSWORD_FAILED`, a third literal holding
`SIGN_UP_FAILED`'s and `ATTACH_FAILED`'s six words. `ADDRESS_LABEL` and `CANCEL` are **imported, not
re-authored**.

**It is not a screen and mints no slug.** `Screen` gains no member, `hashForScreen` no case,
`ADR-0076` §1's address table no row: the count that ADR left to the story is settled at **zero** for
this flow, `ADR-0081` §3's grant of a *forgot password* screen's address is left **unspent**, and
`reset`, `verify` and every other sentence of §3 stand byte-unchanged.

**The door is on the sign-in screen, below the sign-in form, and conditional on nothing** — not the
first screen (`ADR-0083` §3), not the account screen — because the place to be told a password can be
replaced is the place the password was refused, and a door that appeared only after a failure would
make a player type a wrong password to find the way out. It opens a one-field form **in place of**
the sign-in form: never two forms in view, no password field anywhere in the flow, and the door's
words become that form's heading **from one literal** — a stated departure from `ADR-0083` §3's two
constants, on the ground that there is no screen and therefore no name.

**An address the product does not hold gets exactly what everybody else gets**: same sentence, same
controls, same layout, no second state, no hint and no count. The acknowledgement **states a rule
rather than reporting an outcome**, which is what makes one sentence honest across all five
situations `ADR-0031` §5 refuses to distinguish, and discharges `ADR-0078` §Consequences' *"honest
about a pending state rather than congratulatory about a `202`"*. **The form survives its own
success**, with what was typed still in it, because a player who mistyped has no other route back and
a repeat inside fifteen minutes sends nothing (`ADR-0031` §5, `ADR-0079`).

**Costs named rather than discovered**: the **first coined player-facing phrase** in this product and
the first question it asks a player, which loosens a discipline the next ticket will cite; **the
browser's *Back* cannot close the form** — it leaves the sign-in screen for `#/account` — which is
`ADR-0076`'s own harm in miniature and the strongest argument for the screen that was refused, paid
rather than answered; recovery still two navigations deep and advertised nowhere; **a mistyped
address is told the same thing as a correct one and nothing detects it**, the form staying visible
being the whole mitigation; a mode added to the one screen a player types a password into; and a
two-sentence string fixed before `EPIC-06` has letter-fit anything. **Forecloses** any feedback about
the address, by rule rather than omission, and a *forgot your handle* flow while `ADR-0031` §6.2
keeps the handle in the reset mail. **`#/forgot-password` is left open, not shut** — one table row
and one branch, additive — with the phrase fixed so no second `DEC` is needed for the word.

**Unblocks `STORY-0417`'s three held tickets.** No server, wire, schema or `PROTOCOL_VERSION` change,
and **nothing in it was the human's**.

`DEC-080` → [`ADR-0086`](../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)
on 2026-08-28 — **the offer's answer is `pd.accountOfferSettled`, owned beside the predicate it
feeds.** Raised on 2026-08-27 as the technical half of `DEC-079`, narrowed the same day by
`ADR-0085` §1 and §5 to the key and the module, and answered by the architect.

**The key is `pd.accountOfferSettled`, and `web-client/src/result/account-offer-settled.ts` is the
only production file that names the literal.** It exports
`ACCOUNT_OFFER_SETTLED_STORAGE_KEY`, `readOfferSettled(storage)` and `markOfferSettled(storage)`,
all over the injected `Storage`. The stored value is the sentinel `"1"`, and **anything
unrecognised — absent, blank, or any other string — reads as *not settled***, so the offer is made
again after the next win. That direction is not invented: `ADR-0085` §Consequences already chose
*"the side that risks asking twice over the side that risks never telling them"*. **The module
exports no way to clear the bit**, which turns `ADR-0085` §2's *"nothing in the product ever clears
it"* from a promise into a diff a reviewer sees, and **`signOut` is unchanged**, leaving the
signed-out-account-holder case exactly where `ADR-0085` named-and-did-not-solve it.

**Three measurements decided it, run against the real gate with throwaway files and reverted.**
`one-module-owns-each-storage-key.test.ts` scans with `String.includes`, so a file holding
`"pd.accountOfferSettled"` and a file holding `"pd.accountOffer"` are **both** returned by a scan
for the shorter string — which is why the obvious short name is refused, and why the `pd.`
namespace now carries a shape rule nothing enforces: **no key may contain another**. A second
writer placed in `src/result/` reddens a row whose owner sits in `src/protocol/`, so the scan is
whole-`src` and the owner's directory is free — which put the module in `result/` beside
`account-offer.ts`, `account-offer-text.ts` and `AccountOffer.tsx`, rather than in `protocol/`,
whose three keys are each a wire fact this one is not. And a literal only the test file holds
returns `[]`, so the `.test.ts` exclusion does the self-exclusion work and the new row may carry the
literal verbatim.

**The third row is written out verbatim in the ADR**, with the three properties that stop a source
scan being green and vacuous — presence, self-exclusion, and three keys resolving to three
*different* modules. **One clause is fixed for the wiring ticket**: `markOfferSettled` runs from the
accept anchor's click handler before the real page load `ADR-0076` §3 forces, on `DuelResult`'s
already-merged `onLeave` precedent — *"Storage operations are synchronous, so a handler that forgets
has finished before the page leaves"* — and **not** on the account screen's load, which the lobby's
own control also reaches and which would settle an offer never made.

**Costs stated rather than discovered.** A row that outlives its meaning with nothing able to remove
it — dead storage by design. A fourth key in a `pd.` namespace that is a convention and not a
mechanism, now carrying an unenforced shape rule. A storage clear that re-offers, made mechanical by
there being exactly one copy and nothing to reconstruct it from. A magic sentinel no other module
here uses, so a hand-written `true` in devtools reads as broken. One more literal that must stay a
single line forever. An unguarded `setItem`, the same exposure the other two key modules carry. And
a modified click on the accept control that settles the offer without leaving the page. It
**forecloses** any local record of *which* control was pressed, any count or timestamp, and
`protocol/` as the one place to ask what this browser stores. Chosen in the **reversible**
direction: one file and one test row, no wire, no schema, no migration.

**It records a defect it did not fix:** `pd.roomCode`, owned by `room-memory.ts`, has **no row in
the gate** — three production keys, two rows — which is the evidence for the ADR's deadline. The
row ships in the same ticket as the module, because *add the row next time* is a thing this
repository has already failed to do once. **Unblocks `STORY-0415`'s persistence, wiring and arc
tickets.** **Nothing in it was the product owner's**

`DEC-079` → [`ADR-0085`](../docs/adr/ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md)
on 2026-08-27 — ***"not again"* is this browser, and an answer is what spends the offer.** Raised
and answered the same day, by the product owner, **deriving rather than inventing**: the roadmap's
`v0.1` row — *"Two browsers, one room link, one complete duel, rematch. **No accounts.**"* — is the
vision counting a player as a browser at exactly the stage this offer addresses, since `ADR-0036`
shows it only to a player holding **no credential**; and *Positioning*'s *"Dark, quiet, fast,
minimal"* is what refuses a prompt that comes back after the player has answered it.

**The first half was decided by three measurements, not by taste.** `ADR-0036` §Consequences'
reason for *"it belongs on the profile"* — *"clearing storage would resurrect the prompt forever"* —
**is not true of the shipped client**: `device-id.ts` owns `pd.deviceId` in the same bucket
`main.tsx` injects everywhere, so clearing site data takes the device id too, `ADR-0049` §4's
`resolve` finds no live binding and **mints a fresh, empty profile**. Nothing is resurrected; the
profile is gone with its coins, and the returning browser sees no offer until *it* wins a duel.
Second, the two answers are **indistinguishable for every player who can see the offer**: it
requires no credential, and `ADR-0049` §1's `device_binding_live_player` index gives such a player
exactly one live browser, so the only route to a second browser is signing in — which switches the
offer off. Third, **two of `offerAccount`'s three terms are already browser-local** (`TASK-041502`'s
`signedIn` is *"whether this browser holds a session token"*), and a travelling third term would
give one predicate two ideas of who the player is, disagreeing in exactly the cases nobody tests.
So the bit is written and read **through the injected `Storage` and never sent**: no column, no
`GET /api/me` field, no endpoint, no wire change, no migration — and the story stays
`module: web-client`. **Which key and which module was `DEC-080`'s**, answered on 2026-08-28 by
[`ADR-0086`](../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md).

**Second half: an answer spends the offer, and nothing else does.** Both controls are answers —
taking it and *"Not now"* — and each is permanent. A `429`, a failed or abandoned sign-up, a
rematch, a reload, the passage of time and the mere fact of having been rendered spend nothing.
**An offer shown and never answered is made again after the next win**, because the prompt shares a
screen with *Rematch* and `ADR-0036` §Alternatives refused silence for the reason *"a player who has
never been told their coins are device-bound learns it by losing them"*. That also means the trigger
needs **no first-win fact from the server**: a win this browser has not answered for *is* the first
win, for everyone who has never answered.

**Costs stated rather than discovered.** The player who accepts and abandons is **never re-asked**,
and they are the player most likely to convert — the only door left is the lobby's account control.
The offer **reappears on a second browser** and nothing can know it is the same human. A mis-tap on
*Not now*, which sits beside *Rematch*, is final by design. A player who tapped past it is asked a
second time and may read that as being ignored. One more key in this browser's storage. And
`signedIn`'s browser-scoped meaning leaves a signed-out account holder offerable — **named, not
solved**. It **forecloses** any server-side record that a player was ever offered an account: no
funnel, no *asked-and-declined* metric, named because it is the first thing a growth argument asks
for. It **amends** `ADR-0036` §Consequences' storage clause and `ADR-0056` §5's *"and by nothing
else"*, leaving §5's holding — a `429` spends nothing — and `ADR-0036` §Decision, which was the
human's `DEC-025` call, byte-unchanged. Chosen in the **reversible** direction: a column is additive
while `EPIC-07` hosts nothing, a shipped field is not.

**Unblocks `STORY-0415`'s fifth, sixth and seventh tickets.** `TASK-041501`–`TASK-041504` are
unchanged; the story's third acceptance criterion gains one clause (*"…to a player who answered
it"*) and the unanswered case becomes its own criterion; `ADR-0056` §6's `STORY-0415` line is
restated. **Nothing in it was the human's**

`DEC-078` → [`ADR-0084`](../docs/adr/ADR-0084-a-criterion-that-speaks-in-shell-belongs-in-verify.md)
on 2026-08-26 — **a criterion is gated when a `verify:` command exits non-zero if it is false, and
nothing else gates anything.** Raised and answered the same day, after a planner measured two
candidate linter rules against the live backlog and found both unshippable. **Face (b)** — a
criterion quoting a shell command no `verify:` line runs — becomes a check in `lint_tickets.py`,
scoped to tasks that are `ready`, `in-progress` or `in-review`, matching by **word-subset of one
`verify:` line** rather than by substring. That relation is what dissolved the twelve-file blocker:
re-measured on `ef47e299`, the spelling false positives (`npm run test -- src/X.test.tsx` in the
criterion against the hardened `NO_COLOR=1 npm run --silent test -- … | grep -qE` in `verify:`) were
an artefact of exact-substring matching, and the true count is **18 criteria across 11 files** —
`backlog` × 16, `ready` × 2. The sixteen retire at each ticket's own `backlog` → `ready` flip, which
is a PR somebody writes anyway; `TASK-041219`'s two are repaired in the check's own PR, one file,
named in advance. **No grandfather list, no `filed:` date field, no twelve-file repair.**
**Face (a)** — a criterion demanding content in a file the *Files* table excludes, `TASK-041628`'s
shape — is **not** mechanised, and the ADR carries the re-measurement that says why: 206 tickets
flagged raw, and every narrowing leaves the legitimate refusals dominant, because *"`CardSecrecyTest`
passes with no change to the file"* is a refusal written in a demand's grammar and only *whether the
demanded thing already exists in the file* separates it from the defect. It becomes a written rule in
`tasks/README.md` instead — a criterion may demand new content only in a file the *Files* table
edits; about any other file it may say only *unchanged* or *still passes*; and one that cannot be met
without a forbidden edit **is the next ticket, filed in the same split**. The cost is named rather
than hidden: **`TASK-041628` would happen again today**, since its criterion is English and not
shell. The check ships as **its own ticket depending on `TASK-000106`**, three files, not folded into
it. Nothing is blocked and no `status:` moves

`DEC-077` → [`ADR-0083`](../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md)
on 2026-08-26 — **the second account screen is *Sign in*, and its address is never refused.** Raised
when `STORY-0412` was split and answered by the product owner, deriving rather than inventing:
`ADR-0050` §3's merged player-facing text already says *sign in* to a player twice, `docs/protocol.md`
already spells the act `POST /api/auth/sign-in`, and the vision's *Positioning* sentence — *"the
reference points are Lichess and Chess.com… Dark, quiet, fast, minimal"* — is what licensed the plain
word over a themed one. **The heading is `SIGN_IN_HEADING = "Sign in"`**, golden in `account-text.ts`
beside `ACCOUNT_HEADING`. **The slug is `sign-in`**, a literal in `screen.ts` and never derived from
the heading (`ADR-0076` §1), and **the hyphen is the product's own spelling** rather than a
slugifier's guess: the client's address and the server's path now read the same character for
character, and `signin` and `sign_in` are this product's spelling of nothing. That **widens
`ADR-0076` §1's *"a word"* to a hyphenated compound, for this one word**, with the surviving rule
written down — `[a-z-]` only, never leading or trailing, and still the lowercase form of something
the product already says. **The word is said twice from one constant**: the screen's heading, and the
single door to it on the account screen, offered only when `signedIn` is false. That is `ADR-0060`
§2's one-spelling-per-destination rule applied a third time; there is **no fourth door on the lobby**;
and `SIGN_IN_LABEL` stays a separate constant holding the same string, because a control's verb is
not a screen's name. **The address is refused to nobody.** A browser already holding a session token
that opens `#/sign-in` gets the screen, the fragment is not replaced, and **holding a token is not a
fourth branch** — `ADR-0076` §3's three store-owned branches still outrank it exactly as they do at
`#/account`, so a seated tab shows the duel and the address stops lying. The reason is that
`signedIn` is `readSessionToken(localStorage) !== null` — *this browser holds a string* — and nothing
in this client reacts to a `401` (`TASK-041209`), while `ADR-0050` §3 ends every **other** device's
session and leaves those devices holding a dead token. Those are precisely the browsers that need the
screen, so a bounce would hide their only way back behind a *Sign out* control offered to a player
who has just been signed out; and an address that refuses on a fact the client cannot check is
`ADR-0076` §2's forbidden shape. **A successful sign-in lands on `#/account`**, never back on the
sign-in screen: `account-text.ts` authors no *you are signed in* sentence, so the account screen's
routes statement is the only confirmation this product has that it worked. Costs recorded rather than
discovered: ***Sign in* is now said three times in one flow and twice on one screen**, carried by two
constants with nothing keeping them in step, so every test must query by role and a screen reader
hears the same two words twice; **`sign-in` is the first slug with a character outside `[a-z]`**,
which makes `TASK-041226`'s `^[a-z]+$` criterion wrong and leaves every later screen the hyphen
question; **the slug and the endpoint agree by two literals and nothing mechanical**, since a
fragment crosses no wire; **a signed-in browser can reach a working sign-in form** and swap identity
at an address rather than only behind a hidden door — legal and moving no coin (`ADR-0030` §6), but
now permitted at an address; **every sign-in pays one extra click** before the create and join
controls; and **the word is fixed before `EPIC-06` has letter-fit anything**, so a later restyle
leaves the destination two spellings. Chosen partly as the cheapest to unwind — two literals and a
constant, in no mail, on no wire, bookmarked by nobody, because nothing is deployed. **Unblocks
`TASK-041226` and `TASK-041227`**, the last two of twenty-seven, with three things for the planner to
fold in: the `^[a-z]+$` criterion widens, `TASK-041227` owns §5's landing rule and it reaches
`main.tsx` — a fourth file, outside its three-file budget — and the heading must be queried by role.
**Raises no `DEC`, and nothing in it was the human's.**

**Folded in on 2026-08-26**, and one of the three landed differently from the way it was written.
The criterion widened to `^[a-z]+(-[a-z]+)*$` and `TASK-041226`'s proof step on the slug's spelling
**inverted** — the hyphen is now the correct spelling and the underscore is what must fail. The
heading is queried by role in `TASK-041227`, gated by a criterion and by a proof step predicting
*"found multiple elements"*, and `TASK-041226` says plainly that it renders no DOM and so cannot
carry that gate itself. But §5's landing rule did **not** become `TASK-041227`'s fourth file: probed
under `ADR-0070`, the client gate is green with the screen and no landing rule *and* green with the
landing rule and no screen, so no merged gate refuses the intermediate state and `atomic:` would have
been a false claim. It is `TASK-041229`, two files, depending on `TASK-041227` — the same reasoning
that split `TASK-041641` out of `TASK-041616`. `TASK-041227` keeps three files and gains a test
`ADR-0083` §4 needs and the ADR did not ask for: a browser **holding a session token** opening
`#/sign-in` gets the screen, which no other fixture in that ticket can see.

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
produced cannot contain one. `DEC-024` — whether a two-browser run exists at all — was open and the
architect's when this story was written, and its answer changed nothing here, exactly as the story
predicted. It is now answered on 2026-08-28 by
[`ADR-0088`](../docs/adr/ADR-0088-the-two-browser-proof-is-a-written-hand-check.md).

> **`DEC-024` is answered, and the contradiction it left between two registers is gone.** The
> epic's board row read **done**, 14 of 14, while `EPIC-03`'s own `## Open decisions` table still
> listed `DEC-024` as due *before this epic closes* — arithmetic written before `STORY-0314`
> existed. The row was right and the table was stale.
> [`ADR-0088`](../docs/adr/ADR-0088-the-two-browser-proof-is-a-written-hand-check.md) settles it:
> **the two-browser proof is a written hand-check, not a CI job.** No browser runner, no fifteenth
> story, no third CI job — no Playwright, Puppeteer, Selenium, WebDriver or Cypress dependency, and
> `build.yml` keeps its two jobs. The automated ceiling stays where `STORY-0312` and
> `poker-server/.../e2e/` put it, and `EPIC-03`'s file moves to `status: done` here, with all
> fourteen story cells corrected to match the story files they name.
>
> **What the epic accepts by closing this way, named rather than assumed**, and each of the four
> fails *green*: `main.tsx`'s root render — seven nested providers into a `#root` that no test
> document has — is executed by no test, and `index.html`'s `<div id="root">` is asserted nowhere;
> `new WebSocket(socketUrl(window.location))` is never called — `index.test.ts` stubs the
> constructor and asserts the *string*, `dev-proxy.test.ts` asserts `vite.config.ts`'s *values*,
> and nothing forces the two to agree, so **no test in this repository has ever opened a TCP
> connection to the duel server**, the JVM suite included (it runs on `testApplication`, in
> process); `npm run build`'s exit code is the whole claim made about `dist/`; and two storage
> partitions are outside every test's notion. The regression window for all four is a **release**,
> not a pull request, and a failure has no bisect. The proof that stands in for them is `ADR-0088`
> §2 — eleven numbered steps, each with a condition that fails it — with §3's receipt recorded in
> `EPIC-03`'s own Definition of done. That receipt gates no merge and no ticket, and no agent can
> write it.
>
> `EPIC-02` had the same file-versus-row drift with no such contradiction — its only open decision,
> `DEC-008`, blocks nothing and is due before `EPIC-08` — so its file is corrected to `done` here
> too.

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
| **[STORY-0411](stories/STORY-0411-the-name-in-the-client.md)** The name in the client — shown, and settable | | | **done** — and **eighteen** tickets since 2026-08-26, when `TASK-041118` was filed against merged code. The story's `status:` is left at `done` deliberately: every criterion it lists is met, and what the eighteenth repairs is a **test that cannot fail**, not a behaviour that is missing. `TASK-041110`'s double-submit test dispatches two bare `fireEvent.click()` calls, each wrapped by `@testing-library/react` in its own `act()`, so React flushes between them and the second lands on an already-`disabled` button — the guard under test is the `isSubmitting` state, and deleting `NameSurface.tsx`'s `useRef` leaves all nine green |
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
| | [TASK-041118](tasks/TASK-041118-two-clicks-in-one-act-or-the-guard-is-not-under-test.md) Two clicks inside one act, or the in-flight guard is not the thing under test | XS | **done** — filed 2026-08-26 against merged code. `TASK-041110`'s double-submit test dispatches two bare `fireEvent.click()` calls, and `@testing-library/react` wraps each in its own `act()`, so React flushes between them and the second lands on a button already carrying `disabled`. The count of `1` measures the `isSubmitting` state; delete `NameSurface.tsx`'s `useRef` guard entirely and the test still passes. Both dispatches move inside one outer `act`, and the Proof is that mutation run **twice** — green before, red after |
| **[STORY-0412](stories/STORY-0412-the-account-screens.md)** The account screens — sign up, sign in, sign out, and which routes are live | | | **done** — **all 32 tickets merged**, closed 2026-08-27. Grew to **32** when `TASK-041232` was filed the same day it landed, on a limit `TASK-041225` had recorded rather than closed. **Two tickets were blocked and rewritten, both for the same root cause**, and it is the story's lesson: `TASK-041223` and then `TASK-041229` each specified a rendered-tree fixture over a stubbed `window.fetch` that **`App.test.tsx` line 41 forecloses** — `vi.mock("./main", …)` replaces the module wholesale for every test in that file — and the second inherited the defect because it was written against the first's *pre-amendment* shape and never updated when that one was rewritten. A ticket citing another ticket's mechanism inherits its defects. `TASK-041229` also carried a `verify:` line greping a test that **exists nowhere**, which is a gate a coder can satisfy by writing the test — and one did; a merged test must be pinned by a **count or its file**, never its name. One block was **my error**: I recorded the refusal guard as *genuinely unguarded* on a coder's measurement, and a planner re-measured and overturned it — mutating the `!== 200` branch leaves the suite green because a **401 returns as `refused` before reaching it**, so the mutation was never on a live path. Split into **27** on 2026-08-26, amended to **29** the same day and to **31** later that day, unblocked by [`ADR-0076`](../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) and split out of order because `STORY-0416`'s chain is stalled behind `DEC-076` and `STORY-0414`, `0415` and `0417` all trace through this one. **Two** account screens, which `ADR-0076` §1 left to this story: `#/account`, whose word is not coined (`ADR-0050` §3, `ADR-0036` and `ADR-0056` §2 each say *account* to a player), and a sign-in screen whose word the product did not yet say — raised as `DEC-077`, **the product owner's**, and **answered on 2026-08-26** by [`ADR-0083`](../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md): the screen is ***Sign in*** at **`#/sign-in`**, the word is said as the heading and as the one door from the account screen, the address is refused to nobody, and a successful sign-in lands on `#/account`. **Folded in on 2026-08-26**, which unblocked both tickets and left **the story with no open decision**: `TASK-041226`'s `^[a-z]+$` criterion widened to `^[a-z]+(-[a-z]+)*$` and its hyphen proof step inverted, `TASK-041227` kept three files and gained §4's *refused to nobody* test, and §5's landing rule became `TASK-041229` rather than a fourth file — no gate refuses the intermediate state, so four files with nothing holding them together is two tickets (`ADR-0068`, `ADR-0070`). The same pass added `TASK-041228`, taking the story to **29**: `TASK-041202`'s proof step 3 predicted that swapping `useScreen`'s subscription from `hashchange` to `popstate` would redden two tests and it reddens **none**, because this jsdom fires both events on a microtask for a hash assignment — the trap `ADR-0076` §5 names is silent and so was its gate. A later pass on 2026-08-26 took it to **31** and neither addition is a decision: `TASK-041231` sits between `TASK-041221` and `TASK-041222` because that ticket **could not compile** — `AccountScreen`'s `signedIn` prop is required and merged, its *Scope* authorised a `main.tsx` edit its *Files* table forbids, and there is **no prop path at all**, since `App.tsx` renders `<Lobby />` with no props and `TASK-041223` holds `main.tsx` but lands afterwards; the carrier is settled by the merged `useHistory`/`useLadder` contexts `Lobby.tsx` already imports. `TASK-041230` sits last and scans the client's production sources for the session-token key, closing the limit `TASK-041205`'s coder named unprompted, on `TASK-040709`'s merged two-needle pattern. `TASK-041203` is the single startable ticket. Three things were found already settled and needed no decision: `ADR-0050` §4 makes `deviceRouteLive` the whole of what the screen reads, so no `hasCredential` field is asked for and *a credential exists* is derived from holding a session (sign-in is the only endpoint in `docs/protocol.md` that issues one); an identity change is a **document reload**, because `ADR-0075` records three presence fields cleared at no store boundary and `ADR-0076` §6 keeps two controls as page loads for that reason; and `ADR-0081` §1's first-segment rule fixes how `screen.ts` matches. One criterion is met in a different shape and it is written down rather than absorbed — *sign-out during a live duel warns first* becomes an **unconditional** warning, because `ADR-0076` §3's branch order makes the account screen unreachable while a frame has seated the tab, so a duel-conditional branch is one no fixture can reach. A pass on 2026-08-27 took it to **32** and it is not a decision: `TASK-041232` sits last and closes the limit `TASK-041225` recorded rather than fixed — its anti-enumeration guard compares rendered *text*, so a refusal reason in a DOM **attribute** leaves all six of its tests green, measured twice during that ticket and once more here. It covers **both** forms in two files because `SignUpForm`'s `unavailable-handle` collapses two world-states in the merged copy itself, and the mechanism was settled by probing rather than by taste: the suggested `innerHTML` equality across `TASK-041225`'s own scenario **fails on the honest component**, since React reflects a controlled `value` into the DOM attribute and that scenario types different credentials for the two attempts |
| | [TASK-041201](tasks/TASK-041201-the-address-of-a-screen-is-a-pure-function-of-its-fragment.md) The address of a screen, as a pure function of its fragment | XS | **done** |
| | [TASK-041202](tasks/TASK-041202-the-hook-that-carries-the-address-and-the-trap-that-is-silent.md) The hook that carries the address, and the trap that makes a stale render look like React | S | **done** |
| | [TASK-041203](tasks/TASK-041203-the-lobby-reads-the-address-instead-of-two-flags.md) The lobby reads the address instead of two flags, and Back stops leaving the client | S | **done** |
| | [TASK-041228](tasks/TASK-041228-the-hook-answers-a-hashchange-and-ignores-a-popstate.md) The hook answers a hashchange and ignores a popstate, which no test can currently tell apart | XS | **done** |
| | [TASK-041204](tasks/TASK-041204-the-store-outranks-the-address-and-the-address-stops-lying.md) The store outranks the address, and a seated player's address stops lying | S | **done** |
| | [TASK-041205](tasks/TASK-041205-the-token-this-browser-holds-lives-under-one-key.md) The session token this browser holds lives under one key | XS | **done** |
| | [TASK-041206](tasks/TASK-041206-hello-carries-the-session-and-the-device-id-still-never-moves.md) Hello carries the session this browser holds, and the device id still never moves | S | **done** |
| | [TASK-041207](tasks/TASK-041207-the-profile-carries-whether-the-device-route-is-live.md) The profile carries whether the device route is still live | S | **done** — `atomic:` at **4**, probed under `ADR-0070` |
| | [TASK-041208](tasks/TASK-041208-a-profile-body-with-no-device-route-is-not-a-profile.md) A profile body with no device route is not a profile | XS | **done** |
| | [TASK-041209](tasks/TASK-041209-a-fetch-that-carries-the-session-this-browser-holds.md) A fetch that carries the session this browser holds | S | **done** |
| | [TASK-041210](tasks/TASK-041210-every-me-read-goes-out-under-the-session.md) Every read under `/api/me` goes out under the session | S | **done** |
| | [TASK-041211](tasks/TASK-041211-the-words-the-account-screen-says.md) The words the account screen says, including the refusal that is about nobody | S | **done** |
| | [TASK-041212](tasks/TASK-041212-sign-up-and-the-refusal-that-is-about-nobody.md) Sign-up, seven outcomes, and the one refusal that is about nobody | S | **done** |
| | [TASK-041213](tasks/TASK-041213-sign-in-stores-the-token-and-one-answer-covers-both-refusals.md) Sign-in stores the token, carries no credential of its own, and reloads | S | **done** |
| | [TASK-041214](tasks/TASK-041214-sign-out-clears-the-token-and-only-the-token.md) Sign-out clears the token and only the token, leaves the room, and reloads | S | **done** |
| | [TASK-041215](tasks/TASK-041215-stopping-this-device-signing-in-and-the-two-refusals.md) Stopping this device signing in, and the two refusals that are not failures | S | **done** |
| | [TASK-041216](tasks/TASK-041216-the-four-account-calls-reach-a-screen-through-one-provider.md) The four account calls reach a screen through one provider | XS | **done** |
| | [TASK-041217](tasks/TASK-041217-the-account-screen-states-which-routes-sign-in.md) The account screen states which routes sign in to this profile, in both states | S | **done** |
| | [TASK-041218](tasks/TASK-041218-the-sign-up-form-on-the-account-screen.md) The sign-up form — one credential, and the strip is the same profile afterwards | S | **done** |
| | [TASK-041219](tasks/TASK-041219-a-throttled-sign-up-says-so-keeps-what-was-typed-and-retries-nothing.md) A throttled sign-up says so, keeps what was typed, and retries nothing | S | **done** |
| | [TASK-041220](tasks/TASK-041220-stopping-this-device-with-one-confirmation-and-three-facts.md) Stopping this device signing in, offered only where it is safe, with three facts first | S | **done** |
| | [TASK-041221](tasks/TASK-041221-signing-out-asks-first-and-says-what-it-costs.md) Signing out asks first, and says what it costs before it acts | S | **done** |
| | [TASK-041231](tasks/TASK-041231-whether-this-browser-holds-a-token-reaches-the-tree.md) Whether this browser holds a token is read once, above the tree | S | **done** — inserted 2026-08-26 between `TASK-041221` and `TASK-041222`. `AccountScreen`'s `signedIn` prop is required and merged, and **there was no path to it**: `App.tsx` renders `<Lobby />` with no props and `Lobby()` takes none, so nothing `main.tsx` computes reaches the lobby except through a context. `TASK-041222` could not compile inside its three files and `TASK-041223` lands after it. The carrier needs no decision — `Lobby.tsx` already imports `useHistory` and `useLadder` from `../main` |
| | [TASK-041222](tasks/TASK-041222-the-account-screen-has-an-address-and-the-lobby-has-the-door.md) The account screen has an address, and the lobby has the door | S | **done** — amended 2026-08-26: `## Scope` authorised a `main.tsx` edit its *Files* table forbids, the seventh disagreement of that shape this run. The flag now arrives through `useSignedIn()` and `main.tsx` is refused in `## Out of scope`; `depends_on` moves to `TASK-041231` |
| | [TASK-041223](tasks/TASK-041223-the-account-calls-reach-the-real-transport.md) The account calls reach the real transport, and sign-in reaches it carrying nothing | S | **done** — amended 2026-08-26 to own the one assertion it moves. `plainFetch` is a **second** `window.fetch(`, so `TASK-041210`'s `wires all four reads…` expectation goes `1` → `2` in this diff, which is the redness that ticket designed rather than brittleness to route around. The contradicting *"every pre-existing test passes unchanged"* criterion is replaced, and its `grep -c 'authorizedFetch' … returns 1` — measured at **3** on the merged file, the same line-counting bug `TASK-041210`'s `## Notes` records — is corrected to `grep -o … \| wc -l`. **Amended again on 2026-08-27**, and the block is a mechanism defect rather than an open question: a `deep` review and two dispatches showed the title's security half was **ungated**. The `## Tests` table demanded a rendered-tree fixture that `App.test.tsx` line 35's `vi.mock("./main", …)` forecloses for every test in the file, so what got written read the account modules' own source and never touched `main.tsx` — rebinding **only** `signIn` to `apiFetch` left the suite green but for one count (`4` → `5`), and the ticket's own proof step 2 left it **754/754** green. Of the three routes the review offered, **route 1**: per-call **brace-bounded** source assertions on `main.tsx`, buildable inside the existing two files and the merged precedent of `TASK-041210`, whose `## Notes` record that *"assert the behaviour, not the text" inverts when the property under test is a property of the wiring*. The `signIn` rebind now reddens **two tests by name**, and a rebind that moves no count in the file at all reddens one alone. Every proof step is measured: three predictions were false and are gone, and `[^}]` versus a lazy `[\s\S]*?` is the difference between catching that rebind and not — both spans are held by `verify:`. **Landed 2026-08-27**, and the coder and the reviewer measured every proof step independently and agreed: rebinding **only** `signIn` reddens **three** tests, two of them **by name**, and the `revokeThisDevice` rebind that moves **no count in the file at all** reddens **one alone** — the case that justifies per-call spans over counts. One limit is recorded rather than closed: `apiFetch` rebuilt from `plainFetch` passes every assertion this ticket adds, and only `TASK-041210`'s exact `window.fetch(` count refuses it |
| | [TASK-041224](tasks/TASK-041224-no-secret-reaches-a-url-and-no-body-carries-a-player-id.md) No secret reaches a URL, and no request body carries a player id | S | **done** — landed 2026-08-27, and **every one of the five assertions was proved necessary by a mutation that reddens it alone**, which is what a sweep usually cannot show. Both vacuity suspicions were **refuted by measurement rather than argument**: the address-bar check reddens on a `window.location.hash` write in `sign-in.ts`, and its `toBe(before)` half earns its place separately — a hash change carrying *no* secret still reddens it; the `console` sweep reddens on a planted `console.warn` naming the token. All four forbidden body keys were proved individually, and `id` is an exact-key match through `Object.keys().includes()`, not a substring scan over the serialized body — the collision a short key invites. The `.md` note is cited by `STORY-0414` and its three limits match what mutation testing shows the sweep can and cannot see: a future caller outside these four functions that builds its own URL, a real browser's `Referer` (unobservable in jsdom), and `ADR-0081`'s fragment token. Neither the four account modules nor the two protocol helpers touch `window.location` or `console` today, so two of the five guards are regression nets over surfaces with no live traffic — recorded as such rather than counted as coverage |
| | [TASK-041225](tasks/TASK-041225-the-sign-in-form.md) The sign-in form, and one sentence for both ways it can be refused | S | **done** — landed 2026-08-27 in **two rounds**, and the second round exists because the coder's own weakest-assertion answer found the gap. `marks neither field, because the server named neither` originally checked `aria-invalid` and searched for one known sentence, so a **differently-worded** message beside a field — `<span>Check this field</span>` — left all six tests green. The `## Tests` row already required *"no message sits beside either one"*, so this was an **unmet criterion, not a widening**: it is also the leak the ticket exists to stop, since a per-field message tells an attacker the handle exists while the shared sentence pretends otherwise. The assertion is now a whole-content equality against `[SIGN_IN_REFUSED, HANDLE_LABEL, PASSWORD_LABEL, SIGN_IN_LABEL]` read from `account-text.ts`, and **both halves are measured live** — the free-text span and the `aria-invalid` attribute each redden it alone. The whitespace normalisation was checked to be a **no-op on correct output** rather than the thing making the assertion pass. Domination was ruled out the honest way: an *unconditional* forgot-password link reddens **two** tests, but a *conditional* one reddens `offers one credential…` **alone**, which is what proves that test still independently necessary. `sends nothing on a second submit while one is in flight` was proved against the trap that shipped vacuously in `NameSurface.test.tsx` — deleting the `submitInFlight` ref while leaving `disabled` untouched reddens it. **One limit is recorded rather than closed**: the anti-enumeration guard compares rendered *text*, so a leak into a DOM **attribute** (`data-reason`) is invisible to it and leaves all six green. That is the ticket's stated scope — the row gates what is *"on screen"* — and an attacker enumerating handles reads the network response directly rather than the DOM, so it is a real limit and not a live hole. It applies to `SignUpForm` identically and wants a story-level ticket, not a patch here |
| | [TASK-041226](tasks/TASK-041226-the-sign-in-screens-word-and-its-slug.md) The sign-in screen's word, and the address that word becomes | XS | **done** — landed 2026-08-27, unblocked on 2026-08-26 when [`ADR-0083`](../docs/adr/ADR-0083-the-second-account-screen-is-sign-in-and-its-address-is-never-refused.md) was folded in; its `^[a-z]+$` criterion widened to `^[a-z]+(-[a-z]+)*$` and its proof step on the hyphen inverted. **Both traps were measured rather than argued.** The golden test is not a tautology: mutating the **constant** `SIGN_IN_HEADING` reddens `states every sentence exactly, character for character` by name, and so does mutating `SIGN_IN_LABEL` — the two are **separate quoted literals**, not one aliased to the other, which is what `ADR-0083` requires and what a `const A = B` would have quietly destroyed. The address is gated by behaviour and not only by text: removing the `sign-in` case from `hashForScreen()` reddens `names a screen for every address, and an address for every screen`, so the round-trip test enumerates real behaviour rather than the map against itself — the tautology this ticket was built to avoid. `screen.test.ts` stayed out of the budget and out of the diff, as designed. One ungated criterion is recorded, not fixed: an acceptance criterion runs `grep -c '= "Sign in"'` expecting `2`, which is **not** in `verify:` so CI never runs it, and `grep -c` counts matching **lines** — it would read `1` if the two constants were ever written on one line |
| | [TASK-041227](tasks/TASK-041227-the-sign-in-screen-at-its-address-and-the-door-to-it.md) The sign-in screen at its address, and the door to it from the account screen | S | **done** — landed 2026-08-27, unblocked on 2026-08-26; it keeps three files and gains `ADR-0083` §4's *refused to nobody* test, while §5's landing rule became `TASK-041229`. **§4's pair is gated in both directions, measured**: bouncing a token-holder away from `#/sign-in` reddens `opens the sign-in screen to a browser that already holds a session token` **alone**, and un-hiding the door reddens `offers no way to sign in…` **alone** — the door is hidden *and* the address still works, which is the distinction a single branch on the token would have half-satisfied. Two results are worth keeping. The coder found **its own test settling prematurely**: the door is on screen for one tick after the click and is *also* named *Sign in*, so a `getByText("Sign in")` mutation stayed **green**; adding `queryByRole("heading", { name: ACCOUNT_HEADING })).toBeNull()` to the same `waitFor` forces past the transient, and the mutation now reddens with *Found multiple elements*. And `opens the sign-in screen at the address alone` is **dominated by design, not by accident** — no mutation reddens it alone precisely because §4 requires the address and the click to reach the identical branch with nothing differentiating them; the pre-existing `leaderboard` and `account` address tests have the same shape. The Tests row's second half was initially dropped and then met: `SignOutControl` needed **no** new carrier — `signOut` was already on `account-provider.tsx`'s `useAccount()`, which `Lobby.tsx` was already calling for `signUp` and `signIn`, so the driver's suspicion that this needed `main.tsx` (as `TASK-041231` did) was **wrong**. The **wiring** is gated too, not merely the rendering: forcing `signOut` to `undefined` in `Lobby.tsx` reddens the test. One known blind spot is recorded rather than closed — `keeps the first screen doors at three` does not catch a door being *renamed*, which four other pre-existing tests querying that door by name do catch |
| | [TASK-041229](tasks/TASK-041229-a-successful-sign-in-lands-on-the-account-screen.md) A successful sign-in starts the next boot on the account screen, with no way back to sign-in | S | **done** — landed 2026-08-27 on the amended ticket, and **all seven proof steps were reproduced by the reviewer with named outcomes**, which is the bar this ticket failed twice before clearing: swapping `replaceState` for a hash assignment, deleting the replace, and dropping the reload each redden `lands the next boot…` **alone**; binding `reloadAtAccount` to `signOut`, and *calling* the landing instead of passing it, each redden **two** tests; mutating `sign-in.ts`'s **401** branch reddens two merged `sign-in.test.ts` tests by name; and an added eighth test reddens the count gate. The coder called `window.location.reload()` = `2` its weakest assertion and **the reviewer disagreed on evidence** — proof step 6 shows dropping the reload is caught by **nothing else**, so that count is load-bearing rather than brittle. Blocked earlier the same day, after three dispatches, on **three defects in the ticket rather than in the code**; **amended the same day and held at `blocked` only until the amendment lands**, since nothing here waits on a decision. Two of the three defects were real. Its `## Tests` preamble specified the rendered tree over a stubbed `window.fetch`, inherited from `TASK-041223`'s **pre-amendment** shape — but `App.test.tsx` line **41** (not 35) mocks `./main` wholesale for every test in the file. And its third row named `sends sign-in with no credential of its own, even holding a session` as *"existing"* and greped it in `verify:`, when **that test exists nowhere**; it was `TASK-041223`'s pre-amendment name, replaced by `refuses to wrap sign-in…` (`App.test.tsx:238`), with the request-level guarantee in `sends no device id and no authorization of its own` (`sign-in.test.ts:135`). Both merged, both holding. That row is deleted, and the general rule is now written into the ticket: **a `verify:` line greping a test that must already exist is a gate satisfiable by creating it** — pin a merged test by a **count or its file**, never by its name. This ticket now pins `sign-in.test.ts` at **exactly 7 passing tests**, measured to redden on an added eighth. **The third defect was false, and re-measuring it is what chose the route**: the block recorded that mutating `sign-in.ts` to call the injected `reload` on **every** outcome reddens *"nothing in the whole suite"*. It reddens **two merged tests by name** — `reloads the document once a session exists, and not before` and `stores nothing when a 200 carries no token` — measured on `develop` at `604c8ea7` (773 → 771) and again against the projected diff (775 → 773). So the refusal property is **guarded where it lives**, and **route 3 was rejected**: the test it asks for — a 200 and a 401 against one fixture asserting `reload` once then never — already exists at `sign-in.test.ts:173`, so route 3 would have authored a duplicate of a passing merged test, the deleted third row's defect in reverse. **Route 2 taken, route 1 folded in**, still two files: `leaves a refused sign-in exactly where it was` becomes `hands the account landing to sign-in and never runs it here`, which pins the one refusal-shaped defect `main.tsx`'s source *can* see — a wiring that calls the landing itself instead of passing it, landing refused players on `#/account` too. PR #1096's `main.tsx` is carried over verbatim and re-measured sound; its `[\s\S]*?` span is not, as it would break `TASK-041223`'s merged `[\s\S]` = `0` gate. Two of the previous Proof section's steps recorded *"nothing reddens"* and **both are now gates**: binding the landing to `signOut` reddens two tests, and dropping the reboot from it reddens one on a `window.location.reload()` count of `2`. All six Proof steps were run, not predicted; `\([^}]*` on `App.test.tsx` moves `7` → `9` and this ticket owns that number |
| | [TASK-041230](tasks/TASK-041230-one-module-writes-the-session-tokens-key.md) One module writes the session token's key, and a scan is what says so | S | **done** — landed 2026-08-27, **the last ticket in `STORY-0412`**. All three ways a source scan goes green while proving nothing were checked by reproduction, not by reading: it asserts a **positive** before any absence, so the result set is never empty; it cannot match itself, because the walker excludes `*.test.ts` and the new file is one; and it discriminates on **two** keys owned by **two** modules — `"pd.sessionToken"` → `session-token.ts` and `"pd.deviceId"` → `device-id.ts`. Both mutations redden their own test alone: a shadow `const SHADOW = "pd.sessionToken"` in `room-memory.ts` names that file in the received set, and a helper that ignores its argument reddens the discrimination test. **Both documented limits were verified by trying them rather than asserted**: splitting the key as `"pd." + "sessionToken"` reddens the first test with an **empty** set — no plain-text scan can see a key assembled from parts — and a *second* write inside `session-token.ts` itself reddens nothing, deliberately, since the assertion is a file-name **set** and owning the key is that module's job. Two process notes: the coder **re-measured the ticket's reference figures against current HEAD** rather than trusting the stale `299ea851` it cites, and it confirmed the greps matched for real by inspecting raw bytes with `cat -v`, because this sandbox exports `FORCE_COLOR=3` which overrides `NO_COLOR=1`. Filed 2026-08-26. `TASK-041205` proves *its* module writes one key; nothing proves nothing else writes that key, and its coder said so unprompted. Copies `TASK-040709`'s merged `ProfileCreationIsOneStatementTest` including the vacuity guard its `## Notes` names — **two** search strings with **two different** expected answers, because one fixture default cannot tell a working scan from a helper returning a constant |
| | [TASK-041232](tasks/TASK-041232-two-refusals-reach-the-dom-as-the-same-markup.md) Two refusals reach the DOM as the same markup, attributes included | S | **done** — landed 2026-08-27 the same day it was filed, on the limit `TASK-041225` recorded rather than closed, and the limit is **real, not a live hole**: that ticket's anti-enumeration guard compares `screen.getByRole("status").textContent`, so a refusal reason parked in a DOM **attribute** with the visible sentence untouched leaves all six of its tests green — measured. Its sibling `marks neither field…`, strengthened in that same ticket to a whole-`container.textContent` equality, misses it too, because `textContent` cannot see attributes. Not a widening of `TASK-041225`: that ticket's `## Tests` row scopes the criterion to what is *"on screen"* and to *"rendered text"*, so the narrower guard meets it as written, and an attacker enumerating handles reads the network response rather than the DOM. **The mechanism was chosen by measurement, and two of the three candidates were ruled out by it.** The coder's suggested `container.innerHTML` equality across `TASK-041225`'s existing scenario **fails on the honest component today** — that scenario types different credentials for the two attempts and React reflects each into the `value` attribute, so the two markups differ over the player's own keystrokes; holding the typing fixed is what makes the strict form usable, and normalising the markup is what would make it a lie. A sweep for the reason string is not brittle and is **too weak**: against `data-reason-code={reason.length}` — a derived value, the reason never copied — the sweep stays green while the equality reddens. **The predicted cost is false in this shape**: a golden-string `innerHTML` assertion would redden on incidental markup, but an equality between two renders of the *same* component moves both sides together — a wrapper `div` plus a `className` change left all 19 tests green. **Both forms, one ticket, two files.** `SignUpForm` maps six kinds to six deliberately different sentences, so only its `unavailable-handle` carries the property, and it carries it because the merged copy itself collapses two world-states — *"That handle is taken, or this profile already has a password."* Recorded as a regression net over the same shape rather than as sign-in's threat model, which it does not have |
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
| **[STORY-0414](stories/STORY-0414-claimed-here-recovered-there.md)** Claimed here, recovered there, end to end | | | **done** — **all ten tickets merged**, closed 2026-08-27. **The story's residual limit, stated rather than left to be discovered**: every fixture mints sessions **serially** — B signs out before A boots again — so nothing ever holds two live tokens side by side, and a token issuer returning the **same** token for every player leaves all **811** tests green. That was measured, not guessed: a reviewer hard-coded exactly that and reran the whole suite. It wants its own ticket. What the story *does* prove end to end: a duel played anonymously, the coin the server sent, a name set, a credential claimed, and the same three facts read back on a **different device id** through a session token — with the first browser proved untouched three ways. **Five distinct false-negative mechanisms surfaced here and each looks exactly like *this property is unguarded***: a mutation on a branch the fixture never drives; a fixed-value probe that sets the baseline it is compared against; a probe confined to the diff budget while the mechanism lives outside it; a `throw` swallowed by a `catch` in the code under test; and — worst, because it ships that way — an **assertion** swallowed by a `catch` in the test itself. Two real defects were found in **merged** code: `accountServer` aliased its caller's fixture (`TASK-041404`), and `TASK-041409`'s sweep discarded its own failures. Split on 2026-08-27 into **ten**, with the mechanism **probed rather than remembered**: a scratch harness of the exact shape the tickets specify was built and run in a worktree first, and it changed the split twice. **A first boot makes zero HTTP requests** — `ProfileProvider`'s effect runs at mount, the device id arrives later from the `Welcome`, and `readFromApi` short-circuits on a null device id without asking (`api.ts:42`) — so every browser here is booted **twice**, once to mint and once to read, and `TASK-041406` states that as a test rather than leaving it to be discovered. **The committed script already carries two device ids and two player ids** (`device-seat-0`/`player-seat-0`, `device-seat-1`/`player-seat-1`, both from the server's own encoder), so *two clients, two device ids* needs nothing invented — and it hands the story its **discriminator**: the second browser's device id names a genuinely different player with a different balance, name and duel, which is what makes *reads back the first browser's balance* a wrong-answer-possible assertion instead of the only answer available. Two further measurements: a screen change needs `await screen.findBy*` because jsdom queues `hashchange` as a task (the hash read `#/account` while the old screen was still rendered), and `setTimeout` is forbidden outright since `virtual-time.test.ts` is a **text scan**; and `window.location.hash` plus `use-screen.ts`'s subscriber set are module-global, so the two browsers are never mounted at once. **The mock question is settled by citing the merged shape, not a description**: `Lobby.tsx:6` imports `useHistory`/`useLadder`/`useSignedIn` from `../main` with no prop path, so a test must mock the module — and `Lobby.test.tsx:40`'s **partial** `importOriginal` form is the one that works (verified in the probe), while `App.test.tsx:41`'s **wholesale** form is named in the story as the one to refuse, because it is what blocked and rewrote two `STORY-0412` tickets. **Two branches, not one chain**: `TASK-041401` (`drive-duel.tsx`) and `TASK-041402`–`TASK-041405` (`account-server.ts`) have disjoint *Files* tables and run concurrently; everything from `TASK-041406` is linear because four tickets share one test file. `TASK-041401` is the single startable ticket. **No decision was raised and none is open**: the four judgements are each derivable from something merged — `ADR-0027` names *the session outranks the device id* in its title, `ADR-0030` §1 makes a claim move nothing, the HTTP bodies come from the merged `meBody`/`duelRowBody` that ten test files already use, and the fake server's duel row is derived from the script's own `DuelFinished` so the duel reported is the duel played. `DEC-024` stays open, stays the architect's, and is refused rather than answered. `TASK-041409` is the extension `no-secret-in-a-url.md` explicitly asks a future caller's ticket to make — a browser driven through its **screens** is the caller that note says its own sweep cannot see |
| | [TASK-041401](tasks/TASK-041401-the-duel-driver-writes-into-the-storage-it-is-handed.md) The duel driver writes into the storage it is handed, and one module owns the double | S | **done** — landed 2026-08-27, first of `STORY-0414`. The coder's two mutations each reddened tests **1 and 2 together** and neither touched test 3, which left every assertion's necessity unproved — so the **review established it instead**, and all three are independent: breaking the default path reddens `a run given no storage still plays the whole script` **alone**, so it is not vacuous; hardcoding every device id to `device-seat-0` reddens `two seats driven into two storages hold two different device ids` **alone**, so tests 1 and 2 are separable rather than mutually dominated. The review also **overturned the coder's own account of its weakest assertion**: it argued `expect(run.storage).toBe(callerStorage)` was rescued by the value assertion beside it, but replacing the storage makes the identity check fail first, so lines 16–17 never execute — and the value check reads `callerStorage` directly, never `run.storage`. The identity assertion is load-bearing, not redundant. Suite 777/99 → **780/100** |
| | [TASK-041402](tasks/TASK-041402-two-players-keyed-by-the-device-id-each-one-holds.md) Two players, keyed by the device id each one holds, and every request written down | S | **done** — landed 2026-08-27 beside `TASK-041401`, the story's two independent heads run in parallel. This double is what every later ticket in `STORY-0414` builds on, so its assertions were each proved necessary: **all five** redden under a mutation of their own, including `refuses a request carrying no device id at all` (treat a missing id as `players[0]`) and `answers an unknown path with 500` (return `200`). The discrimination that keeps the whole story from going vacuous is real — the two fixtures differ in **four** fields, and the coin balances `100` and `37` are **mutually independent**: neither adds, subtracts, doubles or halves into the other, so a swapped or derived value cannot pass. Requests are recorded **before** routing, proved by a mutation that records after and reddens on refused calls. **Two limits are written down rather than discovered later**: the double never inspects `Authorization`, so a bearer-token bug passes straight through it until `TASK-041405` adds that seam, and paths beyond `/api/me` answer `500`, so a later test asserting only a status code there proves little. `S` is honest at 207 lines — 87 implementation, 120 tests. Suite **782/100** |
| | [TASK-041403](tasks/TASK-041403-the-record-each-player-keeps-and-the-name-each-one-sets.md) The record each player keeps, and the name each one sets | S | **done** — landed 2026-08-27. **The review failed this PR first and the failure did not survive being measured.** It found that dropping `nextCursor` reddens **two** tests rather than one and called that a proof violation — the observation was right, verified independently, but Proof step 1 says the parser test *"must redden"* while step 2 one line below says *"must redden alone"*. The ticket distinguishes the two phrasings deliberately, and the finding imported *alone* into the step that declines it. Sent back to be **run** rather than read — its first pass was explicitly static, *"without applying it"*, with every conclusion phrased *would fail* — and the measurement dissolved the finding: changing `nextCursor` to a **non-null** value, rather than dropping it, reddens the parser test **alone**. The blunter mutation was the only problem. All four new tests then measured: shared duels list reddens `answers each device id with its own duels` alone; the name written to every player reddens `a name set on one player is not set on the other` alone; `the name survives into the next profile read` is **dominated by test 3** on persistence mutations, recorded rather than papered over, and the ticket does not ask for *alone* there. Suite **785/101 → 789/101** — four tests, no new file |
| | [TASK-041404](tasks/TASK-041404-the-claim-and-the-credential-it-attaches-to-one-profile.md) The claim, and the credential it attaches to exactly one profile | S | **done** — landed 2026-08-27, and it **found a real defect in the merged double by way of two agents disagreeing**. The coder reported a coin-balance mutation reddening alone; the reviewer reported the same mutation leaving all 14 green. **Both were right about their own mutation.** `accountServer` did `players as ServerPlayer[]` — a **cast, not a copy** — so it mutated the caller's *module-level* fixture, shared by every test in the file, and `PUT /api/me/name` had been writing into it since `TASK-041403`. Against a test asserting *before == after*, a **fixed-value** mutation applied by an earlier test sets **both** sides and compares equal; a **relative** one still differs. Same bug, opposite verdicts, decided by the shape of the probe. Proved on a live path before concluding anything — replacing the mutation with a `throw` showed **five** tests reach that line — then fixed with `players.map((p) => ({ ...p }))`. **Every mutation result is byte-identical before and after the fix except the masked one**, which recovers from `14/14 green` to reddening `a claim moves no coin and renames nobody` **alone**; verified independently by the driver. The test was never vacuous — the mutation was masking itself. Four of five tests redden alone; `a claim attaches the credential to the device own player` is **dominated**, corroborated by the reviewer and re-confirmed on the clean fixture, because three tests share one assertion in service of three different properties. `S` is **under-estimated** at 297 lines against 207 and 213 for its neighbours, and that is the ticket's fault rather than the coder's — it maps to Scope. Suite **794/101** |
| | [TASK-041405](tasks/TASK-041405-the-session-outranks-the-device-id-and-sign-out-ends-it.md) The session outranks the device id, and signing out ends it | S | **done** — landed 2026-08-27 under a `deep` review that **ran every proof step rather than reading them**. The rule is gated in the shape that matters: both *outranks* tests send **mismatched** device-id/token pairs, so a double reading the device id first cannot pass — the read side was the open risk, since **none of the 14 pre-existing tests ever sent an `Authorization` header** and a token resolving to a fixed or most-recently-seen player would have satisfied all of them. **The ticket's own Proof step 1 is wrong and that is recorded rather than smoothed over**: inverting precedence reddens **three** tests, not the two it predicts — `a token naming no live session is refused rather than falling back` also depends on token-before-device ordering. The coder reported the discrepancy unprompted and the reviewer confirmed it; the implementation catches more than the ticket anticipated. `the device id still answers when no token is carried` is **dominated**, and the domination survived a real attempt to break it: a coarse *always answers A* mutation reddens 8 tests, a surgical one touching only the `device-seat-1` branch reddens 5, and neither isolates it — structural, because its assertion is a **byte-identical sub-request** of the merged `answers each device id with its own player`. Its fencing job is served, just redundantly, and the ticket mandates it. State isolation was verified by **experiment** rather than inspection, since `TASK-041404` shipped an aliasing bug here: two `accountServer` instances, sign in on one, and the other's `tokens` holds neither the entry nor a nonzero size. `PUT /api/me/name` is deliberately left device-id-only — `## Scope` names exactly the two GETs, and the coder refused to widen. Suite **799/101**, file at **19** |
| | [TASK-041406](tasks/TASK-041406-one-boot-of-the-whole-client-over-the-storage-it-is-handed.md) One boot of the whole client, over the storage and the server it is handed | S | **done** — landed 2026-08-27. **The rework the planner predicted did not happen**: it named this ticket in advance as the likeliest to come back, because its correctness rests on a `vi.hoisted` object read by a hoisted mock factory in another file, and because it is where a coder reaches for `App.test.tsx:41`'s **wholesale** `vi.mock` — the shape that blocked two tickets in `STORY-0412`. The coder used the **partial** `importOriginal` form (`Lobby.test.tsx:41-47`) and reports the wholesale one was never tempting once the partial one typechecked. **The review corrected the coder on two counts, by mutating outside the two-file budget the coder was held to** — a distinction worth keeping, because *no isolating mutation exists* and *none exists in the files I may touch* are different claims. Test 4 **can** be isolated: making `use-screen.ts` always return the first screen reddens it **alone**, so the reported structural coupling was an artefact of the budget. Mutation D reddens **only test 2**, not the two the coder reported, and the reviewer diagnosed why — test 1 never inspects `server.requests`, only that the device id was minted. **Mutation E is the masking trap again, confirmed**: a true module-level singleton storage reddens test 2 but leaves **test 3 green**, because the `Welcome` frame rewrites the device id so the shared storage's last write always matches the seat being read — a real limit of test 3, recorded rather than fixed, since closing it needs two interleaved clients the ticket forbids. Proof step 3 was **exceeded**: the ticket predicts two tests redden, three do. `AccountCalls`, `forgetRoom` and `setName` confirmed unreached by throw-probe, matching *Out of scope*. The harness wires `authorizedFetch(server.fetch, storage)` exactly as `main.tsx` does, but **no test signs in**, so the `Authorization: Bearer` branch is wired and unproven — `TASK-041407`'s gap, stated by this coder in advance. The ticket's `Files` list is **incomplete** for what it asks: the wire shape a `welcomeFrame` encodes and the accessible names the Tests table promises are in ten unnamed files, read under `ADR-0070` §4 which permits reading. Suite **799/101 → 803/102** |
| | [TASK-041407](tasks/TASK-041407-claimed-here-the-duel-the-coin-the-name-and-the-credential.md) Claimed here — the duel, the coin the server sent, the name, and the credential | S | **done** — landed 2026-08-27, and its most useful result is a **refusal to claim a gap closed**. Three tickets in a row have deferred the `Authorization: Bearer` branch of `authorized-fetch.ts` to the next one; this coder **instrumented it** instead of inferring — every recorded request in test 2 carries `Authorization: undefined` with `server.tokens.size === 0` — and the reviewer confirmed it the harder way, with a **throw** in that branch that **no test hits**. The branch is wired exactly as `main.tsx` wires it and remains **unproven**, because nothing in this ticket's scope signs in and sign-up mints no token. That is `TASK-041408`'s to close, now measured rather than assumed. The inherited hazard was **checked and not inherited**: neither test uses `TASK-041406`'s `boot A twice → cleanup → boot B twice` shape — test 1's not-equal half compares the DOM against a **computed string** from the fixture and never boots a second storage, so a module-singleton-storage bug has nothing to alias. All four mutations redden their target alone, including two applied **outside the one-file budget** in `account-server.ts`'s sign-up handler, where the mechanism lives. Literal discipline holds: `100` and `37` appear **exactly once** each, at the fixture, every assertion reading through `coinBalanceText`. **One real finding is recorded and deliberately not fixed**: navigating to `#/account` unmounts `ProfileStrip` and `NameSurface`, and `NameSurface`'s optimistic `wonName` does not survive it, so *Back* would show the input form rather than the claimed name — verified by both coder and reviewer, and the reason this test reboots instead of navigating. Suite **803/102 → 805/103** |
| | [TASK-041408](tasks/TASK-041408-recovered-there-a-different-device-reads-back-the-same-three-facts.md) Recovered there — a different device id reads back the same balance, name and duel | S | **done** — landed 2026-08-27, **the epic's central assertion, and it closes a gap three tickets had each honestly refused to claim**. `authorized-fetch.ts`'s `token !== null` branch — which attaches `Authorization: Bearer` — was wired as `main.tsx` wires it and had **never been exercised**. It is now, and proving it required noticing that the **probe itself was masked**: a `throw` in that branch is swallowed by `readFromApi`/`readDuelPage`'s own `catch` into `"unavailable"`, so the literal appears **nowhere** in the failure output and the run looks like an unrelated `findByLabelText` timeout. A `console.error` probe instead shows **3 hits from exactly 1 of 4 tests**. That is the **fourth** distinct way this story produced a false negative from a probe, after a mutation on a branch the fixture never drives, a fixed-value mutation that set the baseline it was compared against, and a mutation confined to a diff budget while the mechanism lived outside it. **The storage-aliasing hazard was real and is closed**: aliasing `storageB = storageA` reddened test 3 but left test 4's original form **passing all four**, because every device-id read happened after B's own write was last and A's data resolves server-side through the token. One added line closes it, and the reviewer proved it is the **sole** guard — delete `expect(readDeviceId(storageA)).toBe(PLAYER_SEAT_0.deviceId)` at `:447` and test 4 goes green under the alias again. **The ticket's own Proof §3 is wrong**: it predicts a sign-in 200 without a `sessionToken` reddens on a balance mismatch; it reddens **earlier**, at `readSessionToken(storageB)` — recorded as a ticket defect. The other identity is asserted unchanged twice over, server-side and client-side, and collapsing every token to one player reddens the seat-1 check. **One residual gap, stated precisely and larger than first thought**: the reviewer hard-coded every minted token to one player and reran the **whole suite** — **807 tests, 103 files, all green**. Nothing anywhere in the tree builds two concurrently-live sessions to prove distinct tokens resolve to distinct players. That is `TASK-041409`'s. Suite **805/103 → 807/103** |
| | [TASK-041409](tasks/TASK-041409-the-second-client-sends-no-player-id-and-is-told-who-it-is.md) The second client sends no player id, and is told who it is | S | **done** — landed 2026-08-27 after a **failed review, a block, and a promotion to `sonnet`**, and both defects it exposed were the same mechanism one level apart. The first: `the second client learns who it is only from an answer` never inspected the **response body**, and its fixture could not have — B signed up with **its own** handle, minting a credential for `player-seat-1`, so B's sign-in returned `player-seat-1`'s token and the test could never observe the identity its name promises. Restructured to merged test 4's pattern — **A signs up, B signs in with A's credentials**. The second, found by running the Proof step that had been skipped: the sweep's `expect()` calls sat **inside the `try` that guards `JSON.parse`**, so a failing assertion — itself a throw — was **swallowed by the `catch`** written for non-JSON bodies. Planting `id: "x"` left **all six tests green**. Split so the `try` guards only the parse; the reviewer confirmed no other assertion in the file is still trapped. **That is the fifth swallowed probe in this story and the first inside a test's own code** — the worst kind, because it needs no probe to go wrong and no mutation will ever reveal it except one that should have failed and did not. **Root cause of both: three of the ticket's four Proof steps were never run**, and step 4 names the catching mutation in advance. All four now measured — path, planted key, emptied log, wrong identity — plus each of the **three** forbidden keys (`playerId`, `player_id`, `id`) through `Object.keys().includes()` rather than a substring scan, the collision a key as short as `id` invites. `deviceId` correctly does **not** redden: it is a legitimate device credential, and the driver's brief was **wrong** to demand four keys — the coder cited the ticket's Scope and was right. Recorded as the ticket asks: `TASK-041408`'s test 4 reddens under the wrong-identity mutation too, so the positive half is **corroboration, not the sole guard**. Suite **807/103 → 809/103** |
| | [TASK-041410](tasks/TASK-041410-signing-out-there-leaves-the-first-browser-untouched.md) Signing out on the second client leaves it with no profile, and the first untouched | S | **done** — landed 2026-08-27, **the story's last ticket**. The title's second half is the discrimination property this story's tickets kept failing — `TASK-041404` shipped the assert-only-your-target version and had to be fixed — and here the first client is proved untouched **three independent ways**: `storageA`'s device id and session token unchanged after B's whole arc; A's balance and name **captured from the DOM** before B acts and re-read after B signs out; and a fresh `bootClient` over `storageA` with a cleared hash forcing a re-fetch through the stored device id. **The mutation that mattered was the storage alias**, because that exact shape passed silently in `TASK-041406` — a module-level singleton left its comparison green there, since the `Welcome` frame rewrote the device id so the shared storage's last write always matched the seat being read. Here `storageA = storageB` **reddens**, confirmed independently by the reviewer, so the story's closing assertion does not carry that hole. Both new tests have a mutation that reddens each **alone**: removing the device id in sign-out reddens the `readDeviceId(storageB)` check, skipping `forgetSessionToken` reddens the token check, and corrupting or clearing A's storage reddens the isolation test. Skipping `forgetRoomCode` reddens nothing, correctly — the room code is not this test's concern. Suite **809/103 → 811/103** |
| [STORY-0415](stories/STORY-0415-the-offer-after-a-first-win.md) The offer — an account after a first win, dismissed for good | | | **done** — **all nine tickets merged**, closed 2026-08-28, and **the split stopping at four was the story's best decision**. `ADR-0036` said the dismissal flag *"belongs on the profile"*; the story's own notes said *"stored under a key this module owns"*; `GET /api/me` carried neither that field nor any first-win fact. Both could not be built, so the planner wrote the four tickets unaffected by the answer and **held three**, because their `Files` tables and `Tests` tables were *determined* by it — an epic written around a guessed product decision reads as settled and shapes everything beneath it. **Two decisions, both answered by agents, neither escalated**: `DEC-079` → `ADR-0085` (*"not again"* is **this browser**; **an answer spends the offer**; a `429` and an abandoned sign-up settle nothing) and `DEC-080` → `ADR-0086` (the key is `pd.accountOfferSettled`, owned by one module, **no clearing function** — so *"nothing ever clears it"* is a diff a reviewer sees rather than a promise). The answers turned three held tickets into **five**, both extras forced by measurement rather than taste. **What the story proves end to end**: a first win offers an account, either control answers it, the answer survives a real second boot, and an offer *shown but unanswered* is made again — the last proved **differentially**, since losing the write and forcing the predicate redden **different** subsets, which a re-rendering harness could not produce. **Three things it does not prove, stated rather than discovered later**: `main.tsx`'s real `localStorage ?? nullStorage` binding is never executed (Vitest always takes the inert fallback per `DEC-032`, so that wiring is verified by reading source); no second browser is booted end to end, that resting on the unit test; and the edge `ADR-0085` §Consequences calls *named, not solved* survives — sign up without seeing the offer, sign out, win again, and you are offered an account you already hold. **Partially split** on 2026-08-27 into **four**, with `TASK-041501` the head and the other three depending on it for sequencing only, their `Files` tables pairwise disjoint so all three run in one batch. The split stopped at four on purpose. `ADR-0036` §Consequences says the dismissal flag *"belongs on the profile"*; this story's own design notes say *"stored under a key this module owns"* and its criterion asserts it *"through the injected storage"*. Those cannot both be built, and `GET /api/me` carries neither that field nor any first-win fact today — so **`DEC-079` (the product owner's)** asked whether *"not again"* is a fact about the player or about this browser, and what **spends** the offer, since `DEC-049` says *"only 'Not now' dismisses"* while this story says a second win never shows it; **`DEC-080` (the architect's, downstream)** asked what carries it on the wire and whether this `module: web-client` story grows a server half. **`DEC-079` was answered on 2026-08-27 by [`ADR-0085`](../docs/adr/ADR-0085-not-again-is-this-browser-and-an-answer-spends-the-offer.md)** — *"not again"* is **this browser**, held through the injected `Storage` and never sent, and **an answer spends the offer**: both controls settle it permanently, a `429` and an abandoned sign-up settle nothing, and an offer shown but unanswered is made again after the next win. That leaves the story with **no server half and no first-win fact to fetch**, and narrowed `DEC-080` to the key and the module. **`DEC-080` was answered on 2026-08-28 by [`ADR-0086`](../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)** — the key is `pd.accountOfferSettled`, owned by `web-client/src/result/account-offer-settled.ts` beside the predicate it feeds, storing the sentinel `"1"` with anything unrecognised reading as *not settled*, exporting no way to clear it, and adding the third row of `one-module-owns-each-storage-key.test.ts` verbatim. Neither decision ever blocked the four written tickets, which hold under either answer; the persistence, the `Lobby` wiring and the whole-client arc are still unwritten and are now writable in full — from `ADR-0085` §3's case table and `ADR-0086` §7's file list — with the story's third criterion gaining one clause. **The split's most useful measurement:** the accept control **cannot** be `useScreen().open("account")` — `ADR-0076` §3 is enforced by a `Lobby.tsx` effect that replaces any non-`first` screen back to `/` while `state.outcome !== null`, and rendering `Lobby` over a store holding a `DuelFinished` and then setting `#/account` measured `hash=""`, no account screen and *Victory* still on screen, while the same navigation with no outcome reached it. So the control is `<a href="/#/account">`, a real page load, which `ADR-0076` §6 requires of that screen's links anyway. Every ticket's Proof section was run rather than predicted: twelve mutations, each reddening the test it names, plus two vacuity checks on `queryBy…`-shaped negatives that both reddened. Suite **811/103**, and **822/106** with all four. **The split was completed on 2026-08-28, at nine tickets rather than seven**, and both extra tickets exist on measurements rather than on taste. `ADR-0086` §6 needs a **required `onAccept` prop on the merged `AccountOffer`** so the accept control can answer before the page loads; that is green on its own diff, so folding it into the wiring ticket would have been a set of files no gate holds together — `TASK-041506`. And `ADR-0086` §*What this does not settle* named the **`pd.roomCode` gap**, which is closable in six lines and is now `TASK-041509` rather than a discovery someone makes twice; `pd.roomCode` resolves to exactly one production file and a probe writing that literal into `store/boot.ts` reddens the row. **The wiring ticket is the story's one `atomic:`, at four files, and the fourth was measured, not guessed**: `App.test.tsx`'s `vi.mock("./main", …)` (line 41) takes no `importOriginal`, so adding `offerSettledHere`/`settleOfferHere` to `main.tsx` and importing them in `Lobby.tsx` without touching that file measured **`Tests 25 failed | 808 passed (833)`**, every failure reading `No "offerSettledHere" export is defined on the "./main" mock` — the third ticket this file has cost, after `TASK-041223` and `TASK-041229`. `ArcWiring` likewise has exactly two builders, so two required fields on it is a three-file ticket, and `tsc --noEmit` named the second one. Nineteen mutations were run across the five new tickets, each recorded with the state it was run in; the sharpest is `ADR-0085` §Alternatives' rejected *being shown spends it*, planted in `Lobby.tsx`'s render path, which reddens three tests across two levels and is the only thing standing between this client and that rule. **No new decision:** `ADR-0086` §*What this does not settle* leaves the wiring shape to the wiring ticket, and it is settled there in the open. Suite **822/106 → 836/107** across the five | v0.2 |
| | [TASK-041501](tasks/TASK-041501-the-words-the-offer-says.md) The words the offer says, and the one word ADR-0036 already chose | XS | **done** — landed 2026-08-27, head of `STORY-0415`. Golden-string discipline proved the right way round: **the constants were mutated, not the assertions**, and six mutations each redden the single test independently — each of the four values changed, an export added, an export deleted. The key-set assertion is what catches structure, the four `toBe`s catch characters, and the rename case the coder named for itself (`OFFER_HEADING` → `OFFER_TITLE` at the same value) is caught by the key set. **The ticket contradicts itself and that is recorded rather than papered over**: its `## Scope` requires *"KDoc on `OFFER_HEADING` and `OFFER_DISMISS` naming `ADR-0036`"*, while its own `verify:` gate demands **zero digits** anywhere in `account-offer-text.ts` — and `ADR-0036` contains digits. The coder satisfied the enforceable gate and dropped the citation, which is the right call when a ticket asks for two incompatible things, but a Scope requirement shipped unmet. The gate is **over-broad**: its real intent, from Scope, is no numeric literal in the **player-facing copy** — the four export values — not no digit in the file, and a narrower gate over the export strings would allow the ADR citation `CLAUDE.md` asks for as the canonical *why*. Same shape as the merged case where a comment containing `window.fetch(` broke an unrelated count gate: **a plain-text gate cannot tell code from prose, or copy from citation.** Wants a follow-up that adds the KDoc and narrows the gate; not changed here, because three tickets depend on this file and moving a merged gate under them is how the `TASK-041223`/`TASK-041229` inheritance problem began. Suite **811/103 → 812/104**
| | [TASK-041502](tasks/TASK-041502-whether-the-offer-is-made-at-all.md) Whether the offer is made — a win, no credential, and not already settled | S | **done** — landed 2026-08-28, and **every term of the predicate is independently guarded, measured**. Three booleans invite a suite that passes on the wrong combination; here dropping `verdict === "win"`, `!signedIn` or `!settled` each reddens exactly one test — `1 failed | 814 passed (815)` in every case. **The review went further than the coder and ran flips as well as drops**, which are different bugs: flipping `!signedIn` reddens tests 1 **and** 2, flipping `!settled` reddens 1 **and** 3, and inverting the whole predicate reddens all three. Test 1 sweeps all four verdict values with the other terms held constant; tests 2 and 3 each flip one field from the true case. The coder named its own weakest spot honestly — **multi-field deltas**, where no dedicated test isolates a loss *and* signed in *and* settled — and the review found no combination answerable wrongly with all three green. The module takes `settled` as an **input** and reads neither storage nor the network, which is `ADR-0085` applied: *"not again"* is this browser, held through the injected `Storage` by a **caller**, never sent. Five `verify:` lines pin that at zero — `coinBalance`, `finalStacks`, `length`, `localStorage`, `fetch` — with `length` forbidding the derived count the ticket refuses. Suite **812/104 → 815/105**
| | [TASK-041503](tasks/TASK-041503-the-offer-and-the-page-load-that-reaches-the-account-screen.md) The offer itself, and the page load that reaches the account screen | S | **done** — landed 2026-08-28, one of a **batch of three run concurrently** — the first three-way batch of the run, and it held: `account-offer.*`, `AccountOffer.*` and `DuelResult.*` are pairwise disjoint and all three depended only on the merged `TASK-041501`. **The planner's up-front measurement saved this ticket a dispatch**: the accept control **cannot** be `useScreen().open("account")`, because `Lobby.tsx` enforces `ADR-0076` §3 with an effect that replaces any non-`first` screen back to `/` while `state.outcome !== null` — rendering `Lobby` over a finished duel and setting `#/account` measured `hash=""`, no account screen, *Victory* still up. So it is `<a href="/#/account">`, a real page load, which §6 requires anyway, and four `verify:` lines pin it: `hashForScreen("account")` once, the `"#/account"` literal zero, `useScreen` zero, `localStorage` zero. **The leading slash is the whole difference between a page load and a hash change**, and dropping it reddens `leads to the account screen through a page load` on `expected '#/account' to be '/#/account'`. All four mutations redden one test each, including the vacuity check that matters: a planted `<form>` reddens `carries no form of its own`, so that absence is a real guard. Per `ADR-0085` the component decides **nothing** about persistence — it calls `onDismiss` and never touches storage — and all four strings come from the merged `account-offer-text.ts` rather than being retyped, which is the drift `TASK-041501` exists to prevent. Suite **812/104 → 816/105**
| | [TASK-041504](tasks/TASK-041504-the-result-screen-carries-an-offer-it-does-not-make.md) The result screen carries an offer it does not make, and gives nothing up for it | S | **done** — landed 2026-08-28, closing the **batch of three**. It modifies a file whose 13 merged assertions are pinned by a **count of 16, never by name** — deliberately, because `TASK-041229` greped a merged test by name, the name had been changed in an earlier amendment, and the gate became satisfiable by *writing* that test, which a coder then did. All 13 are intact and unrenamed. **Four vacuity mutations, each reddening one test alone**, and the pair that matters guards **opposite directions**: deleting `{props.offer}` reddens `puts the offer it is handed between the rematch and the way back` while `disables nothing it already carried when it carries an offer` stays green, and suppressing the coin line reddens the second while the first stays green. The absence assertion is real too — making the panel offer by itself (`{props.offer ?? <section aria-label="the offer">…</section>}`) reddens `adds no offer of its own`. The slot is **indifferent to the panel's internals** by design: it would not notice a different `aria-label`, different text, extra children, or a panel rendering nothing, and `AccountOffer` appears **zero** times in both files. What it does guarantee is placement between the rematch and the way back, no offer of its own, and that nothing already carried is suppressed. One correction to the record: the driver's brief asked for a vacuity check on `carries no form of its own`, which lives in **`AccountOffer.test.tsx`** (`TASK-041503`), not here — the coder ran the checks that exist. Suite **812/104 → 815/104**, three tests, no new file
| | [TASK-041505](tasks/TASK-041505-the-one-key-the-offers-answer-lives-under.md) The one key the offer's answer lives under, and the gate row that owns it | S | **done** — landed 2026-08-28, and it **closes a loop `ADR-0086` opened on evidence rather than principle**: that ADR requires the module **and** its scan row to land in one diff, because *"add the row next time"* had already failed silently — `pd.roomCode` has **no** row today, three production keys against two, and that gap is `TASK-041509`'s. **The new row is a real guard, proved both ways**: planting `pd.accountOfferSettled` in `Lobby.tsx` reddens it **alone**, naming both files, while a *different* key literal written from the **same** module keeps it green, because the scan collects **file names**, not occurrences. **The substring hazard is now demonstrated rather than argued** — renaming the key to the shorter `"pd.accountOffer"` reddens **exactly two** tests, since the scan matches by `String.includes`, which is precisely why `ADR-0086` refused the short name. Six mutations in all, each measured. The module ships with **no clearing function**, and that absence is the design: it makes `ADR-0085` §2's *"nothing ever clears it"* a diff a reviewer sees rather than a promise in prose — a planted clearing function reddens `exports no way back to an unanswered offer` alone. The planner's recorded trap was threaded: a KDoc containing *"export"* would push the `export ` count gate from 3 to 4, and the comment avoids it only because the pattern is space-terminated. Suite **822/106 → 828/107** |
| | [TASK-041506](tasks/TASK-041506-the-accept-control-is-an-answer-too.md) The accept control is an answer too, and says so before the page loads | XS | **done** — landed 2026-08-28. `ADR-0085` makes accepting an **answer**, not mere navigation: a player who accepts and abandons sign-up is never offered again, which that ADR names as its own sharpest cost, so `onAccept` is **required**. The subtle shape is **a navigation that also fires a handler** — a handler swallowing the navigation and navigation skipping the handler are both wrong and both look fine in a casual test — so the new test asserts all four halves: the handler ran once, `fireEvent.click` returned `true` so the dispatch was not cancelled, the `href` is still `/#/account`, and the *other* handler did not fire. `preventDefault` is pinned at zero so the gate and the test agree. **The review returned `fail` and I overturned it, on scope rather than on fact.** Its finding is **correct and I reproduced it**: wrapping the component in a bare `<form>` leaves all five tests green, because `queryByRole("form")` matches only a form with an **accessible name** — so that assertion in `carries no form of its own` is **dead**, and the test's real coverage is its two (redundant) `textbox` assertions. But those assertions are **merged code from `TASK-041503`**, untouched here; this diff changes only the `render()` calls and adds one test. Sending it back would have been the scope widening rule 4 forbids, so it is **filed as a follow-up instead**. The earlier measurements were not wrong either: `TASK-041503` planted a form **with an input**, which the `textbox` assertions do catch. Suite **822/106 → 823/106** |
| | [TASK-041507](tasks/TASK-041507-the-lobby-fills-the-offer-slot-and-answers-for-it.md) The lobby fills the offer slot, and either control answers it | S | **done** — landed 2026-08-28, the story's only `atomic:` ticket and **the one the planner named in advance as likeliest rework, for a reason that proved exactly right**: the two handlers must **differ** — `onDismiss` settles **and hides**, `onAccept` settles **and stops**, because the page load is what replaces the tree — and a coder making them identical passes two of three tests. **Proved absent in both directions**: stripping `onDismiss` to settle-only, and separately adding a hide to `onAccept`, each reddens `answers from either control, and only Not now takes the offer off the screen` **alone**. The driver reproduced the second independently — `1 failed | 54 passed (55)` — because the review had confirmed the handlers differ **by reading the diff** rather than by mutating, on the one property the whole ticket turns on. **The `atomic:` set is justified by measurement, not by argument**: reverting `App.test.tsx` alone gives **25 of its 37 tests failing**, every one `No "offerSettledHere" export is defined on the "./main" mock`, because that file's `vi.mock("./main", …)` takes no `importOriginal` and replaces the module wholesale — the **third** ticket this single mock has cost. `ADR-0085`'s explicitly rejected *being shown spends it*, planted in the render path, reddens **two** tests across two levels. `ADR-0086` §2 keeps the global `localStorage` reach in `main.tsx` alone; `Lobby.tsx` has `localStorage` and `pd.` both at **zero**. Size recorded honestly rather than rounded off: **164 changed lines against the ticket's own 138**, ~19% over, all explanatory comments in this file's existing convention. Suite **830/107 → 833/107** |
| | [TASK-041508](tasks/TASK-041508-the-offer-across-two-boots-of-a-whole-client.md) The offer across two boots of a whole client, answered and unanswered | S | **done** — landed 2026-08-28, **the story's last ticket**, and the risk it was written against was named for it in advance by `TASK-041507`'s coder: a harness whose "second boot" reuses the React tree would prove persistence **in appearance** while only re-exercising `offerSettled` state, and nothing already merged would notice, because none of it calls the real `offerSettledHere`/`settleOfferHere`. **The proof that the boot is real is differential, not by inspection** — a re-rendering harness would keep `Lobby`'s local state alive, so one dismissal would suppress the offer on every later boot **regardless** of the wiring; instead, losing the **write** reddens **two** tests with `offers it again to a browser that was shown it and answered nothing` staying **green**, while forcing the **predicate** reddens all **three**. **Different failing sets are only possible if each boot re-invokes the initializer and re-reads storage**, and the reviewer reproduced both independently. `ADR-0085`'s rejected *being shown spends it*, planted in `Lobby.tsx` outside the budget, reddens three tests across two files. Proof step 4 reproduced the third file's typecheck necessity to the **line and column** (`TS2739 … missing … offerSettled, settleOffer`). The `TASK-041406` sequential blind spot is **not inherited** — no shipped test aliases two storages, checked with a throwaway probe. The coder **measured `develop` itself** (833/107 stashed out, 836/107 applied) rather than trusting a driver figure, and found the ticket's own Proof quotes an older baseline with the same +3 delta — drift upstream, not in the ticket. Suite **833/107 → 836/107** |
| | [TASK-041509](tasks/TASK-041509-the-room-code-key-gets-the-row-it-never-had.md) The room code key gets the row it never had | XS | **done** — landed 2026-08-28, a small ticket repairing a **silent** gap. The one-module-owns-each-key gate had **three production keys and two rows** since it was created — `pd.roomCode`, owned by `protocol/room-memory.ts`, never had one. Nothing ever failed; the gate simply did not cover a third of what its name claimed, and it surfaced only because the architect **measured** the gate while answering `DEC-080` rather than reading it. That is the evidence `ADR-0086` cites for requiring a module and its scan row to land in the **same diff**. **The new row is a real guard, not a fourth decoration**: planting the literal in `store/boot.ts` reddens it **alone** — `expected [ 'boot.ts', 'room-memory.ts' ] to deeply equal [ 'room-memory.ts' ]` — with the other three green, confirmed independently by the reviewer. The literal is written out rather than imported, so the scan cannot match its own file, and `pd.roomCode` is written nowhere the scan cannot see: it is not assembled from constants nor split across lines. Suite **829/107 → 830/107** |
| [STORY-0416](stories/STORY-0416-the-recovery-email-and-the-password-reset.md) The recovery email, verified, and the password reset | | | **done** — **45 tickets, all merged**. Split into **29** on 2026-08-25, out of numerical order because it depends only on `STORY-0405`; grew to **41** and finished at **45**: `TASK-041627` was re-cut into six, `ADR-0078` added `TASK-041635`, `ADR-0079`'s named defect added `TASK-041636` and `TASK-041637`, `TASK-041606`'s landing Notes added `TASK-041638`, `TASK-041614`'s landing Notes added `TASK-041639` and `TASK-041640`, `TASK-041641` took the two database tests `TASK-041616` named but could not carry inside an `atomic:` table no gate names that file in, and **`ADR-0082` added `TASK-041642`, `TASK-041643` and `TASK-041644` on 2026-08-26**, taking it to **44** — and **45** the same day with `TASK-041645`, which gates `ADR-0079`'s four numbers after `TASK-041628`'s reviewer swapped two of them and watched the build stay green. Those three exist because `TASK-041626` came back **blocked** — the first genuine block of this run, and correct: `RecoveryMailer.sendPasswordReset(address, token, handle)` needs a login handle and nothing in the codebase could produce one. [`ADR-0082`](../docs/adr/ADR-0082-a-handle-is-read-from-a-proven-address-never-from-a-player-id.md) (`DEC-076`, raised and answered in one PR) answers it with one **address-keyed** member, `RecoveryEmails.resetRecipientOf(address): ResetRecipient?`, and **no `PlayerId` overload, ever**. They are three tickets rather than the one the ADR sketched because `ADR-0070`'s probe was run rather than a file list copied: the first red run named only `PostgresRecoveryEmails.kt`, the second named `VerificationSweepTest.kt` — invisible to the first, since Gradle stops at its first failing task — and the third exited 0, so the minimum commit is **three files** and neither test file is in it. `atomic:` would have had to name a gate that does not exist. `TASK-041642` is the `Credentials` gate and runs **first**, ahead of the ticket that has to produce a handle; `TASK-041626` moves to `backlog` behind the three and is the one status this pass touched. **Nothing waits on a decision, and `EPIC-04` now has none open at all** — `DEC-074` was answered on 2026-08-25 by [`ADR-0080`](../docs/adr/ADR-0080-the-password-is-judged-before-the-token-is-touched.md), so the password is judged before the token is touched, and `DEC-075` the same day by [`ADR-0081`](../docs/adr/ADR-0081-a-mailed-link-is-a-fragment-route-and-the-token-is-the-segment-behind-the-slug.md), so a mailed link is a fragment route and the token is the segment behind the slug. `TASK-041642` is the single startable ticket. `DEC-073` was answered on 2026-08-25 by [`ADR-0079`](../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md) — ten a minute for `forgot-password`, five for `recovery-email`, an over-budget attempt still counting, and the placement in each handler — and `DEC-071` and `DEC-072` were answered the same day by [`ADR-0078`](../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md) and [`ADR-0077`](../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md), unblocking four. `DEC-072` was answered on 2026-08-25 by [`ADR-0077`](../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md) — the mail seam, its scope and lifetime, its failure semantics, `baseUrl`, and what a test can await. **Nothing is the human's**: `ADR-0031` §7 already defers the transport, and therefore any bill, to `EPIC-07` |
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
| | [TASK-041626](tasks/TASK-041626-four-different-things-happen-and-the-caller-reads-the-same-answer.md) Four different things happen, and the caller reads the same answer | S | **done** |
| | [TASK-041627](tasks/TASK-041627-a-sender-that-sends-nothing.md) A sender that sends nothing | S | done |
| | [TASK-041628](tasks/TASK-041628-two-budgets-that-say-nothing-when-they-refuse.md) Two budgets that say nothing when they refuse | S | **done** |
| | [TASK-041629](tasks/TASK-041629-a-good-token-and-a-password-the-policy-refuses.md) A good token, and a password the policy refuses | S | **done** |
| | [TASK-041630](tasks/TASK-041630-a-decorator-that-detaches-over-the-same-port.md) A decorator that detaches, over the same port | S | **done** |
| | [TASK-041631](tasks/TASK-041631-a-failed-send-stays-inside-the-scope-and-names-a-class.md) A failed send stays inside the scope, and its log line names a class | S | **done** |
| | [TASK-041632](tasks/TASK-041632-the-origin-every-recovery-link-is-built-from-is-configuration.md) The origin every recovery link is built from is configuration | S | **done** |
| | [TASK-041633](tasks/TASK-041633-one-function-builds-both-recovery-links-and-no-header-reaches-it.md) One function builds both recovery links, and no header reaches it | S | **done** |
| | [TASK-041634](tasks/TASK-041634-a-build-with-no-sender-is-a-valid-build.md) A build with no sender is a valid build | S | **done** |
| | [TASK-041635](tasks/TASK-041635-the-fold-the-address-index-depends-on-written-down-in-the-catalog.md) The fold the address index depends on, written down in the catalog | S | done |
| | [TASK-041636](tasks/TASK-041636-the-attach-path-gets-the-quarter-hour-of-silence-the-reset-path-has.md) The attach path gets the quarter hour of silence the reset path has | S | done |
| | [TASK-041637](tasks/TASK-041637-the-second-attach-in-a-quarter-hour-is-answered-the-same-and-mails-nothing.md) The second attach in a quarter hour is answered the same, and mails nothing | S | done |
| | [TASK-041638](tasks/TASK-041638-the-shape-gate-holds-for-four-more-shapes-and-names-the-one-it-cannot.md) The shape gate holds for four more shapes, and names the one it cannot | S | **done** |
| | [TASK-041639](tasks/TASK-041639-a-reset-that-cannot-write-the-password-spends-no-token.md) A reset that cannot write the password spends no token | XS | **done** |
| | [TASK-041640](tasks/TASK-041640-a-failure-between-the-password-and-the-sessions-undoes-both.md) A failure between the password and the sessions undoes both | S | **done** |
| | [TASK-041641](tasks/TASK-041641-the-profiles-recovery-flag-is-that-players-and-a-pending-address-is-not-one.md) The profile's recovery flag is that player's, and a pending address is not one | XS | done |
| | [TASK-041642](tasks/TASK-041642-no-read-on-credentials-answers-with-a-string.md) No read on `Credentials` answers with a string | S | **done** |
| | [TASK-041643](tasks/TASK-041643-the-handle-comes-from-the-address-or-it-does-not-come.md) The handle comes from the address, or it does not come | S | **done** |
| | [TASK-041644](tasks/TASK-041644-three-states-answer-nothing-and-the-fourth-answers-a-handle.md) Three states answer nothing, and the fourth answers a handle | S | **done** — its `## Notes` gained a record on 2026-08-26: the ticket was over `estimate: S`'s 120-line cap at roughly 130–145 lines and there was no honest label, since schema 2 deleted `M`. The truthful fix was two tickets and it is no longer available. Recorded rather than acted on: what failed is that size was judged from **file count**, and `ADR-0070`'s probe sizes a file list, not a diff |
| | [TASK-041645](tasks/TASK-041645-the-two-recovery-budgets-are-numbers-a-test-reads.md) The two recovery budgets are numbers a test reads, not numbers a reviewer swaps unnoticed | S | **done** — filed 2026-08-26. `TASK-041628` shipped `ADR-0079`'s four numbers with **no test asserting any of them**: its reviewer swapped the two defaults and ran both `verify:` classes to a **green build**, because every `RecoveryBudgetsTest` fixture builds its own low `AttemptLimits` by that ticket's own mandate. Its criterion named `ServerConfigTest` while its *Files* table excluded it, and it is merged, so the fourth row is this ticket. The closing fixture recorded in its `## Notes` — `ServerConfig().forgotPasswordLimits()` — **does not compile**: `ServerConfig` has nine parameters with no default. Through `ServerConfig.from(…)` instead, which is also the only path `Application` uses |
| [STORY-0417](stories/STORY-0417-the-recovery-screens.md) The recovery screens — attach an address, and reset a password | | | **done** — **23 tickets, all merged**, closing `EPIC-04`. Split on 2026-08-28 into **20** tickets, with **three still unwritten**, and `TASK-041701` is the single startable one. It is `EPIC-04`'s last story. **The three unwritten ones are the *forgot password* flow's words, its form-or-screen and its door**; they were held because `DEC-081` — **the product owner's** — determines their `Files` and `Tests` tables, and [`ADR-0087`](../docs/adr/ADR-0087-forgot-your-password-is-a-door-on-the-sign-in-screen.md) **answered it on 2026-08-28**: ***Forgot your password?* is a door on the sign-in screen, not a screen of its own** — four constants in `recovery-text.ts`, **no slug and no row in `ADR-0076` §1's address table**, the door below the sign-in form and conditional on nothing, a one-field form opened **in place of** the sign-in form, and an address the product does not hold answered exactly like every other. **The planner writes those three next.** **The transport is written**: `TASK-041708` is one endpoint, one field, one status, and no answer moves a line of it — `STORY-0415`'s pattern, write what the answer cannot touch. **The inherited debt is the story's head**, `TASK-041701`: `ADR-0081` §1's first-segment rule rests on one merged test whose two inputs both put an already-known slug in front of a segment nothing reads, and the case it exists for — `#/verify/<opaque token>` — is unreachable until this story adds the slug. **Four measurements shaped the split rather than reasoning.** `hasRecoveryEmail` is **`atomic:` at six files**: a stub named two under `tsc` and **twenty-two failures across seven files** under vitest, of which **twenty were one fixture** — adding the field to `meBody` cleared six files, so a remembered list would have been wrong in both directions; `profile.test.ts` is deliberately **not** a row, because the gate set reached 0 without it (`TASK-041641`'s reasoning, and `ADR-0070` §4 excludes *adds a test*). Extending `AccountCalls` is **`atomic:` at five**, `tsc` naming the same four literal sites whether one member is added or four. Wiring `verify` and `reset` into `Lobby.tsx` **does not touch `App.test.tsx`** — measured green at `836 passed (836)` with a throwaway branch, because the calls arrive through `useAccount()` and no `../main` import is added, which is the trap that cost `TASK-041223`, `TASK-041229` and `TASK-041507`. And `clearToken` was measured end to end: a link at `#/verify/Xk93-QQ_z7~aa.bb` leaves `window.location.hash` at `"#/verify"` with the screen still rendered, so `ADR-0081` §5's read-once-then-replace works as written. **Every `verify:` below the head pins per-file counts, not a whole-suite total**, because four bundles have pairwise disjoint `Files` tables and an absolute figure is wrong the moment two are batched; the three tickets editing `Lobby.tsx` are strictly ordered by that file alone. `ADR-0083` §1 and §2 already fix where a finished reset sends the player — ***Sign in*** at `#/sign-in` — so the story does not name it a second time |
| | [TASK-041701](tasks/TASK-041701-two-mailed-addresses-and-the-opaque-segment-behind-the-slug.md) Two mailed addresses, and the opaque segment behind the slug | S | **done** — landed 2026-08-28, head of `STORY-0417`, nineteen tickets behind it. Five mutations, all measured, and **two reddened more than the ticket predicted** — three tests rather than one — because more of the suite calls `screenFromHash` with a `reset` slug than the Proof section named; the coder read that as a **stronger** result rather than a discrepancy, and the review confirmed the extra failures are genuine `reset`-slug tests and not collateral. The `?` guard is real and not hypothetical: `tokenFromHash("#/reset/ab?c=d")` returns **`"ab?c=d"`**, because jsdom keeps the `?` **inside** the fragment rather than splitting a query off it — measured by the planner, re-measured by the reviewer, and without it the `split("?")` mutation would test nothing. **The hand-off it wrote is the story's sharpest**, and it names a defect no type checker and no test in this file can catch: if `TASK-041702` re-derives the token from `window.location.hash` on a later render instead of stashing it before the fragment is replaced, `tokenFromHash("#/reset")` returns `null` — **the exact value this ticket's own merged test asserts is correct** — so every test here stays green while the mailed capability is silently dropped. That is `ADR-0081` §5's *"no type checker catches it"* trap restated one module up. Two things recorded rather than fixed: `ADR-0082` was **not** read, correctly, since the ticket never cites it and it governs no file here; and `hashForScreen`'s outer KDoc was **already** non-exhaustive before this ticket, omitting `sign-in`, and now also omits `account`, `verify` and `reset` — **the story's head and the inherited debt**. `Screen` gains `verify` and `reset`, `tokenFromHash` lands, and the two assertions `TASK-041201` could not reach are written against an opaque token and against `ab?c=d`, which jsdom was measured to leave whole inside the fragment |
| | [TASK-041702](tasks/TASK-041702-the-token-leaves-the-address-bar-and-the-screen-stays-where-it-is.md) The token leaves the address bar, and the screen stays where it is | S | **done** — landed 2026-08-28, and **the ticket's own specified mechanism was overturned by measurement**. Its `## Tests` table called for a **render counter** on a second hook instance; three spikes showed that cannot work here, because `useSyncExternalStore` **bails out of scheduling any re-render** when the snapshot is `Object.is`-equal — which `clearToken` guarantees, since the screen never moves. The counter read `1 → 1` even while `notify()` genuinely fired, in three variants including a fully-rendered DOM component. **A test built that way would have passed for the wrong reason forever.** The substitute spies on `screenFromHash`, because `checkIfSnapshotChanged` always reads the snapshot **to decide whether to bail**, so the read survives where the render does not — and that read literally *is* "the snapshot the subscriber received", the ticket's own phrase. **The ordering trap `TASK-041701` warned of is guarded**: the coder added a mutation of its own — re-deriving the screen from `window.location.hash` **after** a destructive replace instead of using the render-time closure — and it reddens two tests **by name**. The read happens at render time, before `clearToken`'s closure exists. **One rule is carried by a grep and by nothing else, and that is recorded rather than hidden**: swapping `replaceState` for `window.location.hash =` reddens **no test at all**; only `verify:`'s own `location.hash =` count (1→2) catches it, because *replace, never push* is unobservable from inside this file. `clearToken` calls `notify()` **directly and synchronously** rather than waiting on an event, since `history.replaceState` fires neither `hashchange` nor `popstate`. **A review returned `fail` claiming three tests were silently uncollected — 9 `it(` declarations against 6 collected — and the driver overturned it on measurement**: at the exact PR head, the file has 9 `it(` and vitest collects and passes **9**, with all three new names present. The reviewer's checkout was incomplete; the diff was never at fault — `useScreen().clearToken()`, `replaceState` plus `notify()`. Measured end to end: the hash lands at `"#/verify"` with the screen still rendered |
| | [TASK-041703](tasks/TASK-041703-the-profile-this-client-parses-carries-whether-recovery-is-on.md) The profile this client parses carries whether recovery is on — **`atomic:`, 6 files** | S | **done** — landed 2026-08-28, and **the six-file `atomic:` set was measured rather than remembered**, which is why it is right: adding `hasRecoveryEmail` made `tsc` name **2** files while vitest named **22 failures across 7**, and **20 of the 22 came from a single fixture**. A remembered list would have been wrong in both directions, and the compiler alone would have under-counted by four files. The coder's own blast radius matched the ticket's six exactly. **The vacuity question a new boolean always raises was settled by measurement**: every fixture supplies `false`, which is precisely the shape where nothing distinguishes *reading* the field from *ignoring* it — so copying `deviceRouteLive` into `hasRecoveryEmail` was planted, and **six tests redden**, proving the value is genuinely read. Removing the validation check reddens **nothing**, and that is **not** a gap: the missing-field case belongs to `TASK-041704` by this ticket's own *Out of scope*, and `TASK-041208`'s rule already makes a missing field return `null`. Nothing merged in `account-server.ts` was weakened — it still copies its players so instances cannot alias, its `tokens` map stays per-instance, and its body sweep still forbids three keys through `Object.keys().includes()` rather than a substring scan — probed, not remembered: `tsc` named two files, vitest named **22 failures across seven**, and repairing one fixture cleared six of them. `profile.test.ts` is not a row because the gate set reached 0 without it |
| | [TASK-041704](tasks/TASK-041704-a-body-with-no-recovery-flag-is-not-a-profile.md) A body with no recovery flag is not a profile, and the flag is that player's | XS | **done** — landed 2026-08-28, **closing a gap its predecessor left open on purpose and said so**. `TASK-041703` proved the new field is *read* — corrupting its value reddens six tests — but deleting the **validation** reddened **nothing**, because the missing-field case was scoped here by that ticket's own *Out of scope*. It now reddens: five mutations, each hitting one test alone. Deleting the `typeof … === "boolean"` check and defaulting to `false` each redden `refuses a body with no recovery flag, and one whose flag is not a boolean` on the **missing-field** case; copying `deviceRouteLive` in reddens `reads the recovery flag the server sent, in both of its states` on **both** cases; and hard-coding `true` and `false` redden it on the opposite case each. **That last pair is the two-inputs guard, and it matters here more than usual**: every pre-existing fixture in the repo supplies `false`, which is exactly the shape where nothing distinguishes reading a field from ignoring it. **A review returned `fail` claiming the two new tests were present but uncollected — 13 `it(` against 11 collected — and the driver overturned it on measurement**: at the exact PR head the file has 13 `it(` and vitest collects and passes **13**. That is the **second consecutive** review to fail a correct PR with this identical claim, both from contaminated checkouts of their own; the rule is now recorded — *a "tests are silently uncollected" finding is not actionable until the tree is proved to match the PR head*. One residual is recorded rather than treated as a defect: the wrong-type case tests a **string**, so `null`, a number, an object or an array are not separately exercised — but the shipped `typeof … === "boolean"` rejects all four, making that a question of redundant coverage rather than a hole in the parse — the tests the `atomic:` above could not carry, `TASK-041641`'s split applied again. Its two bodies set the two booleans **opposite**, so copying `deviceRouteLive` reddens |
| | [TASK-041705](tasks/TASK-041705-the-words-the-account-screen-says-about-recovery.md) The words the account screen says about recovery, and never the address | S | **done** — landed 2026-08-28, **eleven exports, exactly as Scope lists them**, with `ADR-0087`'s four `FORGOT_PASSWORD_*` constants deliberately **absent**: they belong to one of the three tickets that ADR unblocked, which are not yet written. **The coder blocked on that and it was the driver's brief at fault, not the ticket** — I said *"follow the ADR exactly, stop if they disagree"*, which turned a deliberate deferral into an apparent contradiction. The rule, restated: **the ticket outranks the brief on scope; the ADR wins on facts.** An ADR names the destination, a ticket says which step you are on. **The coder found its own vacuity risk**, which is the part worth keeping: emptying every constant would let `names no mailbox, no domain and no other account` pass, because a forbidden-substring sweep over empty strings finds nothing — so it added a **non-empty guard** and proved it load-bearing, the reviewer confirming that with the guard the emptied case still reddens and without it passes over nothing. That is the absence-assertion trap this epic shipped once before, caught this time by the author. Golden-string discipline proved in the direction that matters — **constants mutated, not assertions** — with seven mutations mapped to the tests that catch them. One process note: this coder worked **in the main repository rather than its worktree**, leaving `feat/recovery-text-TASK-041705` checked out there; nothing was lost, the branch matched the remote exactly, but every command aimed at *"the main repo"* was briefly operating on a feature branch — eleven golden constants and `recoveryLine`, with a sweep over every export for `@` and for any claim about a particular address |
| | [TASK-041706](tasks/TASK-041706-the-words-the-two-mailed-screens-say.md) The words the two mailed screens say, including what a reset costs | S | **done** — landed 2026-08-28, taking `recovery-text.ts` from **eleven exports to twenty**, with `ADR-0087`'s four `FORGOT_PASSWORD_*` constants still **absent** — they belong to one of the three tickets that ADR unblocked, and the deferral held in **both** directions this time. The rule that governs it is now on the record: **the ticket wins on scope, the ADR wins on facts.** An ADR names the destination; a ticket says which step you are on. I briefed that backwards on `TASK-041705` and blocked a coder for nothing. **The inherited vacuity guard was the thing to check, because it was written for eleven constants and now covers twenty**: `TASK-041705` found that emptying every constant would let the forbidden-substring sweep pass over nothing, so a guard that silently counted only the original eleven would exclude the nine new sentences while every test stayed green. It covers all twenty, and the key-set assertion does too. Five mutations, each mapped to the test that catches it, and one is instructive on its own: renaming `RESET_HEADING` reddens **the key-set assertion alone**, which is precisely what that assertion exists for — every `toBe` still matches, so only the shape of the module notices. The content gates hold: no `@` anywhere, `RESET_ENDS_EVERY_SESSION` exactly once, and none of `signed in`/`sessionToken`/`token is`, keeping `ADR-0031` §4's cost statable without naming the mechanism — nine more, both headings taken verbatim from `ADR-0081` §1's address-table rows rather than coined. `PASSWORD_REFUSED` is reused from `account-text.ts`, not re-spelled |
| | [TASK-041707](tasks/TASK-041707-attaching-an-address-costs-the-current-password.md) Attaching an address costs the current password, and the answer says nothing | S | **done** — landed 2026-08-28 after a **review that failed it on a measured gap**, which is the kind worth having. The ticket required asserting that `X-Device-Id` carries the stored id; the test **recorded** headers at three points and **never read them**, so deleting the header from the implementation left **every test passing**. The coder's own eight mutations were sound but none of them was *delete the header*, so the hole was invisible from inside its own proof set — which is the general lesson: a mutation suite proves what it thought to try. It matters here because this endpoint spends the current password to bind an address to **an account**, and the device id is what says which account; a request that silently loses it binds the wrong one or fails. Fixed, and **mutation 9 now reddens the first test alone**. The second finding was ordering: absence assertions ran **before** the body presence check, so a `not.toContain` could have passed over a request that was never made — reordered so presence comes first. Two of the original eight are worth keeping on the record: the address moved into a **path** reddens (a secret in a URL survives in referrers, logs and history), and `.trim()` on the address reddens `sends the address exactly as it was given` on a lost leading space — a transport that normalises an address is deciding something that is not its to decide |
| | [TASK-041708](tasks/TASK-041708-one-request-one-answer-and-nothing-to-read-into-it.md) One request, one answer, and nothing to read into it | XS | **done** — landed 2026-08-28, and its anti-enumeration proof is the shape this epic had to learn the hard way. `TASK-041225` shipped a guard that compared only what a **player could see**, and it passed while a distinguishing value sat in a **DOM attribute**; `TASK-041232` had to fix it. So the standard is sameness over the **whole answer** driven by **more than one input**, and here **three** different server responses — `400`, `429`, `500` — are asserted to produce **identical** outcome objects through `toEqual`. Mapping any one of them to a distinct outcome reddens `has exactly two outcomes, and every documented status is the first of them`. **The subtlest mutation is a header added conditionally on storage**, which is exactly how a *does this address belong to someone* oracle leaks: the test seeds storage and compares against the empty case, so a header that appears only when a token exists is caught. `X-Device-Id` is pinned at **0**, and no enumeration word — `unknown`, `registered`, `no such`, `not found` — appears in the source. **Written before `DEC-081` was answered, on purpose**, because no answer moved a line of it; `ADR-0087` landed since and changed nothing here, since this is the transport and not the screen. **Seven residual channels are named rather than implied** — response headers, a read `202` body, undocumented status codes, error text, side effects, timing (mitigated by `ADR-0031` §5 writing the response before the mail work), and infrastructure observers — all outside a transport module's boundary. Was: **written while its screen was held**: two outcomes and no third, with the three failing statuses asserted identical. `ADR-0087` answered `DEC-081` and moved no line of it |
| | [TASK-041709](tasks/TASK-041709-a-token-from-the-mailbox-in-a-body-and-never-in-a-path.md) A token from the mailbox, in a body and never in a path | S | **done** — landed 2026-08-28. A mailed token in a path, a query or a **header** leaks through referrers, access logs and browser history, permanently and to parties the player never chose, so all three surfaces are gated and all three were **measured**: the token in a path reddens on path equality, in a query reddens the test **and** the `?` gate, and in a header reddens through a sweep that reads header **values** via `Object.values()` — not keys, which is how a careless version of this test misses `X-Token` entirely. A planted `console.error` reddens both the log test and the `console.` gate. **The coder ran a vacuity check on its own test**, which is the part worth keeping: removing the sentinel still catches the logging mutation, so the spy detection does not depend on a magic value — a test that works only because of one is a refactor away from silence. Presence assertions precede every absence assertion, so nothing can pass over an empty request log. **What no test here can see is stated rather than implied**: the token can still leak through a `Referer`, a server access log or browser history, and none of those is mitigated by a test — they are mitigated by `ADR-0081`'s fragment routing, which is exactly why the no-`?` gate is **absolute rather than conditional**. `?` pinned at **zero anywhere in the file**, which forbids a ternary and optional chaining too; `ADR-0081` §2 chose an absolute rule over a conditional one for exactly that reason |
| | [TASK-041710](tasks/TASK-041710-a-reset-takes-a-token-and-a-password-and-comes-back-with-no-session.md) A reset takes a token and a password, and comes back with no session | S | **done** — landed 2026-08-28, **the last of the four recovery transports** and the only one carrying **both** a mailed token and a password in one request, under a `deep` review. **The review's first charge was a gap that had not been tried**: all five of the coder's mutations either *relocated* a secret or collapsed a status mapping, and **none deleted anything** — which is exactly how `TASK-041707` shipped a test that recorded headers at three points and never read them, its own eight mutations all sound and none of them *delete the header*. **A mutation suite proves only what it thought to try.** Here the deletions were tried and the news was good: omitting the token, omitting the password, and sending an **empty body** each redden `puts the token and the new password in a body…` through `toEqual`, so the test asserts the request's **contents** and not merely its shape. The header sweep reads `Object.values()`, so a secret in a header **value** is caught. **`ADR-0031` §4's reset-ends-every-session is proved non-vacuous the hard way**: an unwired `storage` fails `tsc` with `TS2339`, and once genuinely wired a real write moves `storage.length` 0→1 and reddens the test — and the reviewer went further, reproducing the ticket's **fixture-vacuity** step, which shows that stripping `sessionToken` from the fixture makes that same write undetectable. That is *why* the fixture carries the value. **Two limits are recorded rather than hidden**, neither a defect: `.trim()` on the password is caught by nothing, because the fixture has no whitespace to lose and the shipped code normalises nothing; and a token placed as a header **key** rather than a value is invisible to `Object.values`, which is the honest answer to *where could it still leak* given the code ships `headers: {}`. The `?` gate stays **absolute**: `ADR-0081`'s fragment routing, not any test, is what keeps a mailed token out of referrers and access logs — `review: deep`. Its success fixture deliberately carries a `sessionToken` the module must ignore, because a `204` with an empty body cannot tell a module that ignores one from a module that would have stored it |
| | [TASK-041711](tasks/TASK-041711-four-recovery-calls-on-the-seam-the-account-screens-already-use.md) Four recovery calls on the seam the account screens already use — **`atomic:`, 5 files** | S | done — the ticket's own Proof step 7 was measured **wrong, in the safe direction**. It predicted that binding `attachRecoveryEmail` to the unwrapped fetch reddens nothing, because the choice is "observable by no test in this client". It reddens two: the wrapped count falls 5 → 4 and the unwrapped rises 7 → 8. The client observes the binding after all. Those two merged `App.test.tsx` counts moved from 4/4 to 5/7 as **forced propagation** — the reviewer counted the occurrences in the new `main.tsx`, confirmed both assertions are still exact `toBe`, and confirmed a wrong value still reddens |
| | [TASK-041712](tasks/TASK-041712-the-account-screen-states-recovery-on-or-off-and-never-an-address.md) The account screen states recovery on or off, and never an address | S | done — landed 2026-08-28, deliberately reversing `TASK-041217`'s zero-grep criterion, which was a fence around that ticket and pointed here. Six mutations, all measured, all reddening as predicted. **The coder named its own weakest assertion and it was real**: the address sweep is `expect(container.textContent).not.toMatch(/@/)`, and `textContent` **excludes attributes** — an address in an `aria-label`, `title` or `placeholder` reaches the accessibility tree while the sweep stays green. Reviewer reproduced it: `aria-label="test@example.com"` on the recovery paragraph reddens nothing. **Not a defect here** — the ticket scopes the sweep to `textContent` in as many words — so it is recorded as a follow-up rather than a failure, and the gap is in the *criterion*, not the code |
| | [TASK-041713](tasks/TASK-041713-the-form-that-attaches-an-address-and-says-why-it-asks.md) The form that attaches an address, and says why it asks for the password | S | done — landed 2026-08-28. `type="text"`, not `type="email"`: `ADR-0078` makes the browser's own rule stricter than this product's, and a refusal the player cannot see is the one that costs an account. **The suite cannot see a normalising form, and the coder and reviewer measured it independently**: change the component to `attach(address.trim().toLowerCase(), currentPassword)` and **all seven tests stay green**, as does `npm run check`. The cause is not a weak assertion — `toHaveBeenCalledWith(ADDRESS, CURRENT)` is argument-exact — but the fixtures: `"zqx-address-zqx"` and `"zqx-current-zqx"` are **already trimmed and already lowercase**, so they are fixed points of the very transformation the test would need to detect. The shipped code is correct; what is missing is the ability to *notice* a regression, which would be the byte-for-byte violation `ADR-0078` forbids and `TASK-041707` proved the transport does not commit. **Follow-up, not a defect** — the ticket's `## Tests` never asks for it. Proof step 2 mismatched honestly too: one test predicted, three measured, because `ADDRESS` is 15 characters and crosses the mutation's own `length > 10` threshold |
| | [TASK-041714](tasks/TASK-041714-the-account-screen-carries-the-attach-form.md) The account screen carries the attach form, and only where it can be used | S | done — landed 2026-08-28, offered whether recovery is already on or not, so a player whose address stopped working is not stranded. **Four dispatches, two of them spent on a gate that could not be satisfied honestly.** The original `grep -oF 'RecoveryEmailForm' AccountScreen.tsx = 2` was **inverted**, not miscounted: the idiomatic shape scores **3** — named import, module path, JSX element — so the gate failed every correct shape and passed exactly two wrong ones, a form imported but never rendered and an aliased import. The first coder satisfied it the only other way, by **renaming `RecoveryEmailForm.*` to kebab-case**, which was outside its two-file budget and broke both this ticket's own first gate and merged `TASK-041713`'s verify paths. Told that refusing was an acceptable outcome, the same coder measured the alternatives on its next attempt and **declined to write the alias**, which is what got the ticket fixed rather than the code. `#1145` replaced it with two gates that cannot collide with the path — `from "./RecoveryEmailForm"` = 1 and `<RecoveryEmailForm` = 1 — and the planner's sibling sweep found the same class of defect in `TASK-041715`, where the tell was a hedge in the criterion (*"If your formatting produces two, say so on landing"*), prose that concedes nobody had run it. The fourth dispatch was prettier only: the coder reported `npm run check: 0` from a **piped** run, where `$?` is the pipe's status and not the check's; CI and the reviewer both measured it failing. **Recorded gap, not a defect:** wrapping the form in `display: none` leaves every test green, so nothing here asserts the form is usable rather than merely present |
| | [TASK-041715](tasks/TASK-041715-the-lobby-hands-the-account-screen-its-attach-call.md) The lobby hands the account screen its attach call | XS | done — landed 2026-08-28, first of the three tickets editing `Lobby.tsx`, which are ordered by that file alone. One dispatch, no findings. **Its gate had been corrected hours earlier and the correction is what let it pass first time**: `attachRecoveryEmail` was pinned at `= 1`, which the prop expression its own `## Scope` prescribes cannot produce — the JSX attribute name and the `account.attachRecoveryEmail` read are two occurrences, exactly as the merged `signUp={…}` line beside it scores 2 today. `#1145` corrected it to `= 2` after the planner's sweep of `TASK-041714`'s inverted gate, and the reviewer confirmed the two occurrences are those and not a contrivance. **A weak wait that turned out not to be weak**: the coder flagged that its first test waits on `/Recovery is/`, which settles profile state rather than form presence, so the form might have been present by timing. The reviewer measured it — `getByLabelText("Email address")` **throws** when the form is absent, so presence is carried by the throwing query and not by the wait, and the spy assertion catches broken wiring independently. Ticket `## Tests` is silent on the stronger wait; recorded as an observation, not a defect |
| | [TASK-041716](tasks/TASK-041716-the-screen-that-finishes-a-verification.md) The screen that finishes a verification, from a token it is handed once | S | done — landed 2026-08-28, asserting *once* under `React.StrictMode` with a **call count**, because `main.tsx` renders there and a second call spends a single-use token — and an outcome assertion cannot see the difference, since the second answer is simply discarded. **Two of six predictions failed, both reported rather than adjusted.** Step 1 (drop the once-guard) predicted two tests would redden and **one** did: `asks again for nothing…` is held by React's own `useEffect` dependency-array equality — the same `token`/`verify` references across a rerender — and not by the guard at all, so it does not test what its name suggests. Step 5 (append the outcome rather than replacing it) reddened **nothing at all**: `askedRef` gates before `verify` is touched, so at most one outcome can reach a mounted instance and append-vs-replace are indistinguishable on every reachable path. `never renders two sentences at once` therefore **cannot fail** as the component is currently shaped; it guards a path that does not exist yet, which is worth recording as that rather than as coverage. The ticket predicted this in its own Proof step 5. Also corrected in passing: the ticket names `HistoryScreen.tsx` as the model for avoiding a double effect, but that file has **no** such guard — its `ask` effect has no ref and no cleanup and would call `read` twice; it is harmless there only because reads are idempotent, which is precisely why it is the wrong model for a single-use token. `@testing-library/jest-dom` is absent from this repo, so `toBeInTheDocument()` throws `Invalid Chai property`; presence is asserted with throwing queries (`getByText`/`findByText`), where the throw is the real assertion |
| | [TASK-041717](tasks/TASK-041717-the-lobby-answers-a-verification-link.md) The lobby answers a verification link, and the token leaves the address | S | done — landed 2026-08-28 in one dispatch. Its fourth test **asserts a cost rather than a feature**: a link opened in a seated tab loses its token (`ADR-0076` §3, `ADR-0081` §Consequences), so the day somebody carves it out they must change a test that says why not. Three results the ticket did not anticipate. **Effect order is load-bearing and the ticket does not say so**: `## Scope` gives only the guard expression, and declaring the new effect *after* the existing `seatedByAFrame` effect is a real `ADR-0076` §3 violation — `clearToken()` runs second and overwrites `leave()`'s address correction, leaving `#/verify` in a tab a frame had already seated. The reviewer reproduced it: exactly one test reddens, the fourth, expecting hash `""` and getting `"#/verify"`. Declared *before*, `leave()` gets the last write. **A third count gate that forbids the correct shape**: `clearToken = 2` admits the import and the call but not the dependency array, so the shape `react-hooks/exhaustive-deps` requires scores 3 and fails. The reviewer confirmed **no shape satisfies both rules**, so a lint suppression shipped to satisfy a grep. Same defect as `TASK-041714` (inverted) and `TASK-041715` (off by one) — three in one story, and the sub-case differs each time, so *"an identifier that is also a module path"* was too narrow a rule to have swept for. **`ADR-0081` §5's *read exactly once* has no test anywhere that can detect its absence**: replacing the lazy `useState` initialiser with a per-render `tokenFromHash(window.location.hash)` leaves all 61 tests in the file **and the full 903-test `npm run check`** green, because `clearToken()` rewrites the hash to a value mapping to the same screen, so `useSyncExternalStore` never schedules the second render that would expose the difference. Three further Proof steps reddened a different set than predicted; every mismatch was reported as measured |
| | [TASK-041718](tasks/TASK-041718-the-screen-that-sets-a-new-password-and-says-what-it-costs.md) The screen that sets a new password, and says what it costs before it acts | S | done — landed 2026-08-28. `review: deep`. Carries `ADR-0080` §Consequences' constraint as a test: a refused password can be corrected on the same link. Its Proof step 7 **predicts the obvious stronger assertion is false**, since React reflects a controlled input's value into `innerHTML` — and that prediction held exactly. **The ticket's most valuable result is a test that was passing for the wrong reason.** Proof step 5 predicted that submitting with an empty token would redden a call count; it reddened **nothing**, and a trace showed `handleSubmit` was never invoked: a `disabled` submit button does not dispatch the click, so the test was asserting jsdom's platform behaviour rather than the component's `token === null` guard. Two mechanisms, only one of them tested — and the untested one is the one a JSX refactor would drop. The test was strengthened in place to submit the form directly, keeping its name and both assertions; the reviewer confirmed it now reddens when the guard is removed and reproduced the jsdom behaviour in isolation (0 handler calls disabled, 1 enabled). **Recorded gap, not a defect:** no test asserts `onDone`'s call count on the null-token path, so adding `onDone()` there leaves all eight green — a player arriving with a dead link would be sent onward as though the reset had succeeded. The shipped component is correct (its early return precedes any `onDone`), the `## Tests` table budgets exactly eight and has no row for it, so it is a follow-up. Four other Proof steps mismatched their predictions and all four were reported as measured rather than adjusted |
| | [TASK-041719](tasks/TASK-041719-the-lobby-answers-a-reset-link-and-sends-the-player-to-sign-in.md) The lobby answers a reset link, and sends the player to sign in | S | done — landed 2026-08-28 in one dispatch, last of the three tickets editing `Lobby.tsx`. It asserts the stored session token is **unchanged** after a reset, because `ADR-0083` §4 needs `#/sign-in` to work for a browser holding a dead string and nothing else here would catch a tidy-up that cleared it. **The `tokenFromHash = 2` gate was a constraint, not a miscount** — `TASK-041717` had already left the import plus one call, so the gate forced this ticket to *reuse* that single read instead of adding a second, which is `ADR-0081` §5's *read exactly once* enforced by arithmetic. The coder widened the existing effect's guard from `screen === "verify"` to `(screen === "verify" || screen === "reset") && mailedToken !== null` and **added no second effect and moved nothing**, preserving the ordering `TASK-041717` measured as load-bearing; the reviewer confirmed both. **The pipe defect was caught here before CI rather than after**: the coder's first `npm run check` was masked by a `tail` pipe, and re-running it bare surfaced a real Prettier failure it then fixed — the same mistake that shipped a false green on `TASK-041714`. **One Proof step was reasoned rather than run and was labelled as such**: Vitest stops a test at its first thrown assertion, so step 4 only ever showed a prefix; the reviewer isolated the second half and measured it reddening as the coder had inferred. `window.localStorage` reads `undefined` under this Vitest/jsdom setup — Node's own inert global shadows jsdom's, as `main.tsx` already notes against `DEC-032` — so a `withLocalStorage()` helper was added **inside the test file only**, mirroring the existing `withClipboard` pattern, and the reviewer confirmed it restores the global afterwards and that the *unchanged token* test really seeds a token first, so the assertion is not vacuous. **Recorded observation:** the `href` line asserts nothing the `hash` equality above it does not already imply, and a leak into a channel other than the address — a `console.log`, a second storage key — would leave every test green. The ticket's `## Tests` requires that line explicitly and asks for no other channel, so it stands as written |
| | [TASK-041720](tasks/TASK-041720-the-secret-sweep-drives-the-four-recovery-calls-too.md) The secret sweep drives the four recovery calls too | S | done — landed 2026-08-28, closing `STORY-0417`. `review: deep`. Eight calls, seven secrets, and it **deletes the `try`** that currently wraps three `expect()`s in `puts no secret in anything it logs`. **The premise was wrong, and the coder measured it against its own ticket.** That block is `try {` at line 251 and `} finally {` at 272 with **no `catch`** — and a `try`/`finally` runs its cleanup and then *re-raises*, so those three assertions were live the whole time. The reviewer established the boundary by experiment: breaking one on `develop` reddens the test; adding a bare `catch {}` around the same broken assertion turns the suite green at 5/5 while the leak stands. Only a `catch`, or an assertion helper that catches internally, can swallow — and neither existed here. Removing the `try` for `afterEach` + `vi.restoreAllMocks()` remains right as hygiene, foreclosing a later `catch`; it repaired nothing that was broken. **The driver had briefed the false version to roughly fifteen agents and written it onto this board as history; it is corrected here.** The review also **failed the PR on the documentation, not the test**: `no-secret-in-a-url.md` claimed the screens clear the fragment with `history.replaceState` *before ever calling* `verifyEmail` or `resetPassword`. Two independent instrumentations of the merged screens measured `["verifyEmail", "replaceState"]` — the reverse — because **React runs a child's mount effects before its parent's**, so `VerifyScreen`'s request precedes `Lobby`'s `clearToken`. True for `resetPassword`, which fires from a submit handler after every mount effect; false for `verifyEmail`, named in the same sentence. Corrected to state both separately, name the mechanism, and stop at the measured ordering without calling it a leak — both land in one commit with no paint between. **Recorded, not closed** (the ticket says so in as many words): no test here checks a **header** value against five of the seven secrets, so `headers: { "X-Debug": request.newPassword }` in `reset-password.ts` leaks with all five tests green — the header loop iterates `bearerSecrets`, not `SECRETS` |
| | [TASK-041721](tasks/TASK-041721-the-words-the-forgot-password-flow-says.md) The words the forgot-password flow says, and the second state it refuses to have | S | done — landed 2026-08-28, taking `recovery-text.ts` from **20 exports to 24**. The first of the three [`ADR-0087`](../docs/adr/ADR-0087-forgot-your-password-is-a-door-on-the-sign-in-screen.md) unblocked. Takes `recovery-text.ts` from **20 exports to 24** with §1's four constants, and adds one test saying there is no fifth: §6 gives an address the product does not hold exactly what everybody else gets, so a `FORGOT_PASSWORD_UNKNOWN` would rebuild in the copy the oracle `ADR-0031` §5 closed on the wire. **Measured before it was written**: appending the four constants and nothing else reddens exactly one test in one file — the key-set `toEqual` — which is why its `Files` table is two rows. Two gates are hand-counted rather than remembered: `FORGOT_PASSWORD_` at **4**, the four `export const` lines and nothing else, so the KDoc may name no constant; and the §6 needle list at **0**, its measured value on `develop` today. `TASK-041705`'s `address is` gate is deliberately **not** carried forward — it already scores 2 on `develop`, and `TASK-041706` dropped it for that same reason. Eleven `verify:` lines and **all seven Proof predictions matched**, the first ticket in this story where none mismatched. The coder **stalled** mid-run with both files edited and nothing committed; the worktree was inspected rather than re-dispatched over, the four gates measured green from outside, and the agent resumed to finish — a stall is infrastructure, not a verdict. **The forbidden-phrase guard is real but shallow, and both agents said so.** The reviewer confirmed it is *not* vacuous: planting `no such` into `FORGOT_PASSWORD_ACKNOWLEDGED` reddens on the loop's `not.toContain`. But it matches **exact substrings only**, so a rephrasing leaks the oracle while passing all six keywords — the coder's own example, *"If that address is verified on an account here, a link is on its way. We'll let you know if we can't find it"*, contains none of them. `forgotPassword` answers `202` for every address **by design** so nothing can tell a known one from an unknown one, and prose is the one place that guarantee can be undone in plain language. The ticket specifies exactly those six keywords and asks for nothing stronger, so this is **recorded as a follow-up, not a defect** |
| | [TASK-041722](tasks/TASK-041722-the-one-field-form-that-asks-for-a-reset-link.md) The one-field form that asks for a reset link, and answers everyone the same way | S | done — landed 2026-08-28. `ForgotPasswordForm`: one address, two outcomes, and `ADR-0087` §5's *form survives its own success*, so the sentence renders **with** the form and what was typed stays in the field. Its fixture is `" Zqx-Address-Zqx. "` — leading space, trailing dot, capitals — because `TASK-041713`'s already-trimmed, already-lowercase address left `.trim().toLowerCase()` invisible to all seven of its tests. The double-submit test drives `fireEvent.submit(form)` and not a click, since a disabled control never dispatches one (`TASK-041718` measured that). Gates: `<input` at **1** and `type="password"` at **0**, §5 allowing no password field anywhere in this flow; `fetch` at **0**; and a five-phrase sweep for copied sentences. `ForgotPasswordOutcome`'s count is deliberately **not** gated — two equally correct shapes score 2 and 4 on the same file. **The coder found its own test vacuous and repaired it, which is the best result in this story.** Its first-draft fixture for *says the same thing for every address* used three addresses that were **all of even length by accident**, so a mutation branching on `address.length % 2` reddened **nothing** — the fixture could not reach the mutated branch. It recognised the structurally-unreachable trap, measured the lengths with `node -e` rather than eyeballing them, and shipped a corrected fixture. The reviewer counted independently — 18 even, 11 and 15 odd — and confirmed the parity mutation now reddens on the equality. **A missed prediction with a sound explanation:** Proof step 2 predicted two tests moving and measured **four**, with *says the request did not go through…* staying green because it drives only the `failed` outcome, so an `accepted`-branch mutation is structurally unreachable from it. Reproduced and confirmed — correct scoping, not a weak test. **Recorded, not a defect:** nothing asserts that *Cancel* leaves the typed address untouched, so adding `setAddress("")` to it stays green; the ticket requires preservation only for the outcome states, so it is a follow-up |
| | [TASK-041723](tasks/TASK-041723-the-door-on-the-sign-in-screen-and-the-form-it-opens-in-place.md) The door on the sign-in screen, and the form it opens in place of the sign-in form | S | done — landed 2026-08-28, **closing `STORY-0417`**. The door below the sign-in form, conditional on nothing, opening the recovery form **in place of** it. `ADR-0087` §7 left *which component holds the form* to the planner, and the answer is a local `SignInScreenBody` in `Lobby.tsx` mounted only by the `sign-in` branch: leaving the screen unmounts the mode, with no reset handler and no effect to keep in step. That is the file's own idiom — `WaitingForRival` is written that way and `CopyLink` already holds state. **The whole change was applied to `develop` and measured, then reverted**: `App.test.tsx` stayed at 37, `Lobby.test.tsx` at 65, `SignInForm.test.tsx` at 7, with `tsc`, ESLint and Prettier clean — which is why `App.test.tsx` is not in its `Files` table. Every count gate was hand-counted on that shape: bare `ForgotPasswordForm` scores **3** in this file (named import, module path, JSX element), so the gates are `from "../account/ForgotPasswordForm"` at 1 and `<ForgotPasswordForm` at 1; `<SignInForm` stays at **1**, which is *never two forms in view* as a static count; `{FORGOT_PASSWORD_LABEL}` braced is **1** while bare is 2; `useEffect` stays at its merged **3**; and `forgot` in `screen.ts` stays at **0**, since §3 mints no slug. All eight Proof steps matched, the second ticket in this story with no mismatch — including step 6, which correctly predicted a mutation would stay **green** (`open("sign-in")` on the door press is a no-op because the address is already there). **The door is state, not an effect**: a local `SignInScreenBody` holds one `useState`, the three `useEffect` occurrences are unchanged in order and guard, and nothing touches the address — which is what keeps `TASK-041717`'s measured ordering intact. **`ADR-0087`'s named cost is asserted rather than engineered away**: *comes back to the sign-in form after a round trip through the lobby* pins that pressing *Back* leaves the whole sign-in screen instead of closing the form, because the door has no address to return from. **Recorded, not a defect:** `expect(signIn).not.toHaveBeenCalled()` is **redundant** — nothing in the recovery path could plausibly call `signIn`, so it stays green under the very *two forms in view* defect it appears to guard. The reviewer confirmed both halves: that line stays green, and the second test's `PASSWORD_LABEL`/`SIGN_IN_LABEL` absence checks are what actually catch it. The safety classifier was **unavailable** when this coder's work was checked, so its diff scope was verified independently before review and again at the PR head |

`STORY-0416`'s rows are in **id order**, and `depends_on` is the **sequence**; since `ADR-0077` and
`ADR-0078` landed the two no longer coincide. `TASK-041627` was re-cut into six — `ADR-0077`
§Consequences said it would be — and one edge moved: `TASK-041627` now ships `NoRecoveryMailer` and
runs **before** `TASK-041625`, because `TASK-041626`'s no-sender case binds that object and a seam
has to exist before its consumers. The chain is `…041608 → 041636 → 041609 → …041624 → 041627 →
041625 → 041637 → 041642 → 041643 → 041644 → 041626 → 041630 → 041631 → 041632 → 041633 → 041634 →
041628 → 041629`. **Three of those links are `ADR-0082`'s**, inserted on 2026-08-26 when
`TASK-041626` came back blocked because nothing in the codebase could produce the login handle its
mail needs. `041642` is the `Credentials` gate and runs **first**, ahead of the ticket that has to
produce a handle, since a gate landed after its temptation has passed was never tested against it;
`041643` lands the port member, the statement and the one test double; `041644` gates the
statement's behaviour. They are three tickets and not one because `ADR-0070`'s probe said so: the
minimum commit is three files, and no gate names either test file.
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

---

## EPIC-12 — Quality and defect repair

Opened 2026-08-29 on the human's instruction; licensed by
[`ADR-0089`](../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
(`DEC-082`) on three standing conditions — no dependency, no gate, no coverage claim. Rounds are
stories: the round number lives in the story body, not in the id.

**Round 1** ran `/qa-cycle smoke` on 2026-08-29 at commit `7f7b905f` and ended **`PASS`** with
`B(1) = 0` — six smoke cases, none failed, nothing to dedupe against and no severity to argue.
The fix set is empty, because `EPIC-12` §Termination rule 2 admits only `blocker` and `high` and
there were none. The one ticket it filed is a **harness** defect under `ADR-0089` §4: `SMK-03`
reads `pd.deviceId` from a profile the catalogue never tells anyone to open, so the case is red for
the harness rather than for the product. It is excluded from `B(1)`, it is repaired in
`docs/test-plan.md`, and no production code may change for it. `SMK-02`'s weak assertion was
assessed the same round and **deliberately not filed**, with reasons in the story. A `PASS` here is
a statement about one run, on one machine, at one commit — not coverage, and not citable as any
(`ADR-0089` §2c).

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| **[STORY-1201](stories/STORY-1201-the-qa-harness-two-agents-one-skill-one-catalogue.md)** The QA harness — two agents, one skill, one catalogue — *a retrospective record; no tickets* | | | done |
| **[STORY-1202](stories/STORY-1202-the-first-round-smoke-passed-and-one-case-did-not-run-as-written.md)** Round 1 — the smoke suite passed, and one case did not run as written — *schema 2* | | | done |
| | [TASK-120201](tasks/TASK-120201-smk-03-reads-a-device-id-from-a-profile-that-has-been-to-the-app.md) `SMK-03` reads a device id from a profile that has been to the app | XS | done |
| **[STORY-1203](stories/STORY-1203-the-qa-cases-skill-the-authoring-half.md)** The `qa-cases` skill — the authoring half, whose last act is a printed command — *not a round story; schema 2* | | | done |
| | [TASK-120301](tasks/TASK-120301-the-qa-cases-skill-file.md) Create the `qa-cases` skill, whose terminal act is a printed command | S | done |
| **[STORY-1204](stories/STORY-1204-the-epic-04-and-epic-05-catalogue-suites.md)** The `EPIC-04` and `EPIC-05` catalogue suites, authored from merged sources — *not a round story; schema 2* | | | done |
| | [TASK-120401](tasks/TASK-120401-the-epic-04-identity-suite.md) Write the `EPIC-04` identity suite into `docs/test-plan.md` | S | done |
| | [TASK-120402](tasks/TASK-120402-the-epic-05-ladder-suite.md) Write the `EPIC-05` ladder suite into `docs/test-plan.md` | S | done |
| **[STORY-1205](stories/STORY-1205-round-1-the-identity-write-path-and-the-presence-line.md)** Round 1 — no request declares its body, and the presence line never arrives — *schema 2* | | | **done** |
| | [TASK-120501](tasks/TASK-120501-every-request-with-a-body-declares-that-it-is-json.md) Every request with a body declares that it is JSON | S | done |
| | [TASK-120502](tasks/TASK-120502-the-rivals-presence-reaches-the-other-table.md) The rival's presence reaches the other table — *reclassified harness; superseded by `TASK-120506`* | S | dropped |
| | [TASK-120503](tasks/TASK-120503-no-case-assumes-a-device-with-no-finished-duel.md) No case assumes a device with no finished duel — *harness; excluded from `B(1)`* | S | done |
| | [TASK-120504](tasks/TASK-120504-a-round-allocates-the-third-profile-core-03-needs.md) A round allocates the third profile `CORE-03` needs — *harness; excluded from `B(1)`* | XS | done |
| | [TASK-120505](tasks/TASK-120505-the-driver-does-not-click-what-a-player-cannot-see.md) The driver does not click what a player cannot see — *harness; excluded from `B(1)`* | XS | done |
| | [TASK-120506](tasks/TASK-120506-a-case-can-end-a-browser-session-and-says-so.md) A case can end a browser session, and says so — *harness; supersedes `TASK-120502`* | XS | done |
| **[STORY-1206](stories/STORY-1206-round-2-the-account-screen-forgets-the-password-it-has.md)** Round 2 — the account screen forgets the password the profile has, and the cycle ends — *schema 2* | | | **done** |
| | [TASK-120601](tasks/TASK-120601-a-claimed-profile-is-never-offered-the-claim-form-again.md) A claimed profile is never offered the claim form again — *product; `medium`, never scheduled by this cycle* | S | done |
| | [TASK-120602](tasks/TASK-120602-the-catalogues-coin-query-reads-the-device-binding-table.md) The catalogue's coin query reads the table the device id actually lives in — *harness; excluded from `B(2)`* | XS | done |
| | [TASK-120603](tasks/TASK-120603-05-02s-standings-read-carries-the-identity-the-app-sends.md) `05-02`'s standings read carries the identity the app sends — *harness; excluded from `B(2)`* | XS | done |
| | [TASK-120604](tasks/TASK-120604-05-04-walks-the-pages-that-exist.md) `05-04` walks the pages that exist, and says what a hidden *Show more* proves — *harness; excluded from `B(2)`* | XS | done |
| **[STORY-1207](stories/STORY-1207-the-uat-focus-the-observer-and-what-it-may-file.md)** The UAT focus — the observer, the harness verb, the route map and what may be filed — *not a round story; schema 2* | | | **done** |
| | [TASK-120701](tasks/TASK-120701-state-clear-leaves-only-the-heartbeats-dedupe-stamp.md) `state --clear` leaves only the heartbeat's dedupe stamp | S | done |
| | [TASK-120702](tasks/TASK-120702-the-driver-captures-a-screen.md) The driver captures a screen — a `shot` verb over CDP | S | done |
| | [TASK-120703](tasks/TASK-120703-the-uat-screen-inventory.md) The UAT screen inventory — the route map a round walks | S | done |
| | [TASK-120704](tasks/TASK-120704-the-standing-questions-and-what-uat-does-not-cover.md) The standing questions, and what UAT does not cover | S | done |
| | [TASK-120705](tasks/TASK-120705-the-uat-agent-the-role-and-the-hands.md) The `uat` agent — the role, the refusals and the hands | S | done |
| | [TASK-120706](tasks/TASK-120706-what-uat-may-file-and-what-it-may-only-ask.md) What `uat` may file, and what it may only ask | S | done |
| | [TASK-120707](tasks/TASK-120707-the-uat-focus-of-the-qa-cycle-skill.md) The `uat` focus of the `qa-cycle` skill | S | done |
| | [TASK-120708](tasks/TASK-120708-the-merged-source-classifier-and-the-promotion-gate.md) `qa-manager` — the merged-source classifier and the promotion gate | S | done |
| | [TASK-120709](tasks/TASK-120709-two-more-exclusions-a-baseline-round-and-a-qualified-verdict.md) `qa-manager` — two more exclusions, a baseline round and a qualified verdict | S | done |
| **[STORY-1208](stories/STORY-1208-the-verdict-table-never-checks-for-a-baseline-round.md)** Step 6 stops a healthy cycle — the verdict table never checks for a baseline round — *not a round story; schema 2* | | | **done** |
| | [TASK-120801](tasks/TASK-120801-the-verdict-table-checks-for-a-baseline-round-first.md) Step 6's verdict table checks for a baseline round first | XS | done |
| | [TASK-120802](tasks/TASK-120802-termination-rule-4-carries-its-own-exemption.md) `EPIC-12` §Termination rule 4 carries its own exemption | XS | done |
| **[STORY-1209](stories/STORY-1209-round-1-uat-the-front-door-was-never-dressed.md)** Round 1 (UAT) — the front door was never dressed, and four screens have no card — *schema 2; `B(1) = 1`, verdict `PROCEED (conformance unjudged on 4 of 11 screens)`* | | | **done** |
| | [TASK-120901](tasks/TASK-120901-the-front-door-wears-the-clients-tokens.md) The front door and the waiting frame wear the client's tokens, and the room-code field can be seen — *product; `high` — **counts in `B(1)`*** | S | done |
| | [TASK-120902](tasks/TASK-120902-a-card-for-the-duels-screen.md) A card for the `duels` screen — *missing card; `high` — **excluded from `B(1)`*** | S | done |
| | [TASK-120903](tasks/TASK-120903-a-card-for-the-leaderboard-screen.md) A card for the `leaderboard` screen — *missing card; `high` — **excluded from `B(1)`*** | S | done |
| | [TASK-120904](tasks/TASK-120904-a-card-for-the-account-screen.md) A card for the `account` screen — *missing card; `high` — **excluded from `B(1)`*** | S | done |
| | [TASK-120905](tasks/TASK-120905-a-card-for-the-sign-in-screen.md) A card for the `sign-in` screen — *missing card; `high` — **excluded from `B(1)`*** | S | done |
| | [TASK-120906](tasks/TASK-120906-the-client-never-sends-on-a-socket-that-has-not-opened.md) The client never sends on a socket that has not opened — *product; `medium`, never scheduled by this cycle* | S | done |
| | [TASK-120907](tasks/TASK-120907-the-join-path-ships-neither-screen-its-cards-draw.md) The join path ships neither of the two screens its cards draw — *design; `medium`; **rewritten by the planner on 2026-08-31**, from a client ticket to a `module: design` one. `DEC-092` is answered: [`ADR-0094`](../docs/adr/ADR-0094-opening-the-invite-is-taking-the-seat.md) blessed the shipped join path, so `web-client/` is untouched and the two cards are what change — `join-duel.html` is **deleted** (§4a: no subject left, and `design/components/flow-actions.html` still cards its refusals, so no product state goes uncarded), `enter-code.html` keeps its path and becomes the first screen's join-by-code states (§4b), and `docs/test-plan.md`'s invite-link row moves to `duel-table.html` in the same diff (§4a). **Excluded from `B(1)`** as a decision-born ticket (`ADR-0092` §5)* | S | done |
| | [TASK-120908](tasks/TASK-120908-the-tables-sizing-control-is-the-cards-presets.md) The table's sizing control is the card's presets, not a range slider — *product; `medium`; **to be rewritten, not amended**, and rewritten on 2026-08-31 by the planner against [`ADR-0100`](../docs/adr/ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) and [`ADR-0101`](../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md); as first written it could not go green — its two-file budget excluded the driver `DEC-100` settles, and its three-button actions row broke the recorded `AllIn` frame independently of the amounts. **`DEC-101` is answered** — [`ADR-0101`](../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) fixes the preset arithmetic (`pot → callTo + base`, the fractions over the same base, out-of-range chips absent rather than clamped), so nothing gated the rewrite but the planner. **The file set was measured, not copied**: the change was stubbed and the whole of `.github/workflows/build.yml`'s pull-request gate set was run — Gradle green, and in `web-client/` typecheck, lint, format, test and build each on its own so no failing prefix could hide a later gate — and it named **five** paths, not six. `turn-fixture.ts` drops out on `ADR-0100` §7's own condition (*"only if the bar's new input needs a default"*): no gate names it, and `ADR-0101` §7 wants each test frame declared explicitly anyway, because a shared `committedThisStreet: 0` is the one value that hides the bug the second test exists to catch. Still `atomic:` on `whole-duel.test.tsx`. The rewrite carries `ADR-0100` §1's driver algorithm and §5's refusals, `ADR-0101` §§1–3's formulas (`pot → callTo + base`, the fractions over the same base, `floor` rounding, an out-of-range chip **absent** rather than clamped), and five worked frames — including the re-raise `ADR-0101` §7 demands and the preflop one where the button gets no `⅓` at all. Two things the probe found that no ADR could: `whole-duel.test.tsx` reddens unless the chips carry `disabled={sent}` (proved by mutation), and the driver's presses never flush while `driveScriptedDuel` wraps them in `act()`. `DEC-102`, the stepper's step, does **not** block it — the stepper is out of scope. Sequenced behind `TASK-120914` so the card it reads agrees with the ADR* | S | done |
| | [TASK-120909](tasks/TASK-120909-the-away-countdown-takes-the-shape-adr-0046-names.md) The away countdown takes the shape `ADR-0046` §3 names — *product; `medium`* | XS | done |
| | [TASK-120910](tasks/TASK-120910-the-profile-strip-shows-the-name-it-was-just-given.md) The profile strip shows the display name it was just given — *product; `medium`* | S | done |
| | [TASK-120911](tasks/TASK-120911-three-cards-carry-the-strings-their-adrs-settled.md) Three cards carry the strings the ADRs that superseded them settled — *design; card in arrears, `medium`, **not a product defect*** | S | done |
| | [TASK-120912](tasks/TASK-120912-not-now-is-dressed-like-the-control-beside-it.md) The result screen's *Not now* is dressed like the control beside it — *product; `low`* | XS | done |
| | [TASK-120913](tasks/TASK-120913-the-name-the-server-accepted-reaches-the-profile-the-provider-holds.md) The name the server accepted reaches the profile the provider holds — *product; `medium`, the second half of `TASK-120910`'s chain — **not filed by round 1**, split out at landing on 2026-08-31 because `TASK-120910`'s `Files` table named the provider and its test but not `NameSurface.tsx`, the one production file that calls the hook, so the capability merged with no caller; `TASK-120910`'s live-stack acceptance criterion moved here with the work* | XS | done |
| | [TASK-120914](tasks/TASK-120914-the-duel-tables-pot-chip-sizes-a-pot-sized-raise.md) The duel table card's selected `pot` chip reads the raise it sizes, not the pot — *design; card in arrears, `medium`, **not a product defect**; filed 2026-08-31 by the planner, owed to [`ADR-0101`](../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) §5. `design/screens/duel-table.html`'s hero frame prints `3,250` in two places — the stepper's `aria-label="raise to amount"` span and the actions row's `Raise to` — and both become **3,650**: 3,250 is the *base*, the pot as it will be after the call, printed where the raise that sizes to it belongs. Nothing else on the card moves, and `duel-table-states.html`'s own six `3,250`s are a different frame, pinned untouched by a gate. **Unblocks `TASK-120908`**, which reads this card for the sizing row's anatomy* | XS | done |
| **[STORY-1210](stories/STORY-1210-round-2-uat-the-four-new-cards-were-not-a-tautology.md)** Round 2 (UAT) — the four new cards were not a tautology, and the screens behind them are undressed — *schema 2; **baseline round**; `B(2) = 3`, verdict `PROCEED`* | | | **done** |
| | [TASK-121001](tasks/TASK-121001-the-duels-screen-wears-the-card-merged-for-it.md) The `duels` screen wears the card merged for it, and its search field can be seen — *product; `high` — **counts in `B(2)`*** | S | done |
| | [TASK-121002](tasks/TASK-121002-the-leaderboard-screen-wears-the-card-merged-for-it.md) The `leaderboard` screen wears the card merged for it, and a row reads as rank, name and coins — *product; `high` — **counts in `B(2)`*** | S | done |
| | [TASK-121003](tasks/TASK-121003-the-account-screens-controls-look-like-controls.md) The `account` screen's *Sign in* and *Sign out* are the card's buttons, not sentences — *product; `high` — **counts in `B(2)`*** | S | done |
| | [TASK-121004](tasks/TASK-121004-the-front-door-finishes-the-card-it-started.md) The front door finishes the card `TASK-120901` started — the fill, the code well, the wordmark — *product; `medium`, never scheduled by this cycle* — its third scope item, the wordmark, was **struck on 2026-08-31** and disposed by [`ADR-0098`](../docs/adr/ADR-0098-the-wordmark-belongs-to-the-front-door-alone.md) (`DEC-099`): the markup it quoted lives in `App.tsx` and renders on every screen, so the lockup is the front door's alone and the client half is its own ticket under this story | S | done |
| | [TASK-121005](tasks/TASK-121005-the-sign-in-submit-is-the-cards-fill-button.md) The `sign-in` form's submit is the card's fill button, not a smaller one — *product; `medium`; one half of the ticket round 2 filed, **split 2026-08-31*** | XS | done |
| | [TASK-121006](tasks/TASK-121006-the-account-forms-submits-are-the-cards-fill-button.md) The `account` screen's two form submits are the card's fill button — *product; `medium`* | XS | done |
| | [TASK-121007](tasks/TASK-121007-two-cards-and-a-margin-note-catch-up-with-the-client.md) Two cards and a margin note catch up with the client that overtook them — *design; cards in arrears, `medium`, **not a product defect*** | XS | done |
| | [TASK-121008](tasks/TASK-121008-the-driver-can-read-a-screen-that-auto-advances.md) The driver can read a screen that auto-advances between polls — *harness capability; `manual-verify` — **excluded from `B(2)`*** | S | done |
| | [TASK-121009](tasks/TASK-121009-the-catalogue-records-the-cards-it-has-and-the-question-it-closed.md) The catalogue records the cards it now has, and the question two rounds have closed — *harness — **excluded from `B(2)`*** | XS | done |
| | [TASK-121010](tasks/TASK-121010-the-sign-in-screens-route-out-is-the-cards-link.md) The `sign-in` screen's route out is the card's link, not body text — *product; `medium`; the other half of `TASK-121005`, **split 2026-08-31*** | XS | done |
| | [TASK-121011](tasks/TASK-121011-the-product-name-leaves-the-shell-that-draws-it-above-every-screen.md) The product's name leaves the shell that draws it above every screen — *product; `medium`; the first half of `TASK-121004`'s struck third scope item, disposed by [ADR-0098](../docs/adr/ADR-0098-the-wordmark-belongs-to-the-front-door-alone.md) (`DEC-099`); **split into two 2026-08-31** because that ADR's §4 names four files, `ADR-0068` caps a ticket at three, and a planner probe found no merged gate against splitting — this half alone runs `npm run check` green* | S | done |
| | [TASK-121012](tasks/TASK-121012-the-front-door-alone-wears-the-cards-wordmark.md) The front door alone wears the card's wordmark, and it says the product's name — *product; `medium`; the second half, and where [ADR-0098](../docs/adr/ADR-0098-the-wordmark-belongs-to-the-front-door-alone.md) §§1–2 land. Settles in the ticket, as that ADR directs, the one choice it left open — the lockup is an `h1` carrying `aria-label="Poker Duels"`, because the card's markup concatenates to `PokerDuels` and a name query cannot gate the difference* | S | done |
| **[STORY-1211](stories/STORY-1211-round-3-uat-the-count-fell-to-zero-and-the-cycle-ends.md)** Round 3 (UAT) — the count fell to zero, and what is still wrong is written down — *schema 2; **the last round rule 5 permits**; `B(3) = 0`, verdict `PASS`, fix set empty* | | | **done** |
| | [TASK-121101](tasks/TASK-121101-the-table-says-who-won-the-hand-it-just-finished.md) The table says who won the hand it just finished — *product; `medium`, rewritten from [ADR-0095](../docs/adr/ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md); the banner and its six gated strings* | S | done |
| | [TASK-121102](tasks/TASK-121102-accepting-a-rematch-draws-the-cards-dealing-frame.md) Accepting a rematch draws the card's dealing frame — *product; `low`, `manual-verify`* | S | done |
| | [TASK-121103](tasks/TASK-121103-the-duels-rows-and-filter-carry-the-cards-remaining-cues.md) The `duels` screen's checked filter, faint date and outcome weight are the card's — *product; `medium`* | S | done |
| | [TASK-121104](tasks/TASK-121104-a-leaderboard-rank-and-coin-figure-is-a-mono-figure.md) A leaderboard rank and coin figure is the card's mono figure — *product; `medium`* | XS | done |
| | [TASK-121105](tasks/TASK-121105-the-account-forms-labels-are-the-cards-labels.md) The `account` screen's field labels are the card's left-aligned muted labels — *product; `low`* | XS | done |
| | [TASK-121106](tasks/TASK-121106-the-sign-in-forms-labels-are-the-cards-labels.md) The `sign-in` screen's field labels are the card's left-aligned muted labels — *product; `low`* | XS | done |
| | [TASK-121107](tasks/TASK-121107-a-player-with-no-place-reads-the-cards-muted-line.md) A player with no place this season reads the card's muted line, not the accent box — *product; `low`, carried from round 3's `BLOCKED` state and confirmed statically* | XS | done |
| | [TASK-121108](tasks/TASK-121108-two-table-cards-name-the-street-their-pot-strip-prints.md) Two table cards name the street their pot strip prints — *design; card in arrears, `low`, **not a product defect*** | XS | done |
| | [TASK-121109](tasks/TASK-121109-the-lobby-hands-the-duel-table-the-hands-events.md) The lobby hands the duel table the hand's events — *product; `medium`, the second half of `TASK-121101`'s chain — **not filed by round 3**, split out by the planner on 2026-08-31 because `Lobby` → `DuelTable` → `PotStrip` plus a test is four files and no merged gate forbids splitting it (`ADR-0068` §4)* | XS | done |
| **[STORY-1212](stories/STORY-1212-the-audit-focus-the-observer-the-resize-and-what-a-criterion-costs.md)** The audit focus — the observer, the resize, and what an unmet criterion costs — *not a round story; schema 2* | | | **done** |
| | [TASK-121201](tasks/TASK-121201-the-driver-resizes-a-live-tab.md) The driver resizes a live tab — a `size` verb over CDP | S | done |
| | [TASK-121202](tasks/TASK-121202-the-audit-agent-the-walk-and-the-two-shapes.md) The `audit` agent — the walk, the hands and the two shapes | S | done |
| | [TASK-121203](tasks/TASK-121203-what-audit-answers-and-the-three-it-may-propose.md) What `audit` answers, and the three criteria it may propose | S | done |
| | [TASK-121204](tasks/TASK-121204-the-audit-focus-of-the-qa-cycle-skill.md) The `audit` focus of the `qa-cycle` skill | S | done |
| | [TASK-121205](tasks/TASK-121205-the-rubric-classifier-and-the-ticket-it-promotes.md) `qa-manager` — the rubric classifier and the ticket it promotes | S | done |
| | [TASK-121206](tasks/TASK-121206-the-audit-arithmetic-a-of-n-and-no-severity.md) `qa-manager` — the audit arithmetic, `A(N)` and no severity | S | done |
| | [TASK-121207](tasks/TASK-121207-termination-counts-criteria-under-the-audit-focus.md) `EPIC-12` §Termination counts criteria under the audit focus | XS | done |
| **[STORY-1213](stories/STORY-1213-round-1-audit-three-criteria-unmet-and-a-table-that-names-the-wrong-winner.md)** Round 1 (audit) — three criteria unmet, and a table that names the wrong winner — *schema 2; **the first round under the `audit` focus**; `A(1) = 3` of a five-criterion rubric, `A(0)` n/a, verdict `PROCEED`, fix set 3 in the rubric's own order* | | | **done** |
| | [TASK-121301](tasks/TASK-121301-the-runout-arrives-street-by-street-on-the-screen-too.md) The runout arrives street by street on the screen, not only in the log — *audit fix set 1 of 3, `R1` at beat 5; `manual-verify`; **unblocked and re-cut whole on 2026-09-01** against [`ADR-0102`](../docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md), which answered the `DEC-105` this ticket registered: the store paints a hand-ending `Snapshot` as one step per preceding `StreetDealt` then a final step, queues every frame arriving during them in arrival order, lags exactly the board prefix and the street label, and costs **600 ms** named once in `boot.ts` with **`0` meaning synchronous** so `drive-duel.tsx` boots at `0` and [ADR-0100](../docs/adr/ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §3's four e2e files stay unedited. `Files`, `Tests` and `verify:` all replaced; **`atomic:` at 10 files, measured by probing the whole pull-request gate set** — `duel-state.test.ts` reddens on the first commit that gives the reducer a field or an export, and all four recorded-frame suites redden on any commit where the default step is 600 without the driver at `0` (`Test Files 5 failed \| 112 passed (117)` against `1 failed` with the driver at `0`). `no-derivation.test.tsx` stays byte-unchanged, gated by its SHA-256* | S | done |
| | [TASK-121302](tasks/TASK-121302-the-decision-fits-a-390-by-664-screen.md) The decision fits a 390 by 664 screen, because the client draws the card's phone — *audit fix set 2 of 3, `R2` at beats 2/3, 4, 5 and 6; `manual-verify` — the failure is a measured geometry and [ADR-0089](../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md) §2b forbids a browser gate. **Rewritten whole on 2026-09-01** under [`ADR-0103`](../docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md) §5: the two sentences that were false are gone — the card is no longer `read` in its *Files* table and no longer described as conformance to a merged card, because the merged card measures **732 against 664** at 390 × 664 and describes no phone any conformance could reach. Now `depends_on: [TASK-121305]` and **backlog until that design ticket merges** — design precedes client. Its height-budget half (`min-height: 100dvh`, `flex: 1`, one column where there are two) survives as **necessary but not sufficient**; it carries `ADR-0103` §3's give order in order and exhaustively, and its file set is to be **measured, not copied** — the two rows are known starting points, and the client's 885 exceeds the card's 732 by 153 px that live somewhere the old two-file budget never accounted for* | S | done |
| | [TASK-121305](tasks/TASK-121305-the-duel-table-card-draws-the-phone-too.md) The duel-table card draws the phone too, and the cards give before the numbers — *design; **the predecessor `TASK-121302` gained on 2026-09-01**, filed by the planner under [`ADR-0103`](../docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md) §4 — **not a fourth criterion**, so the fix set stays 3 and `A(1)` stays 3. Amends `design/screens/duel-table.html`: the hardcoded `--w:96px` and `--w:40px` narrow with the column like `--bw` already does, and the file gains a **second frame at 390 × 664 whose markup is the same**, boxed in height as well as width — a separate phone card is refused and the line drawn is **markup identity, not file count**. One frame answers every beat, because the card already reserves every slot. Composing, not minting: a fit was probed using only names `tokens.css` already declares, reaching **664/664** at 390 × 664 with the laptop frame unchanged at every number it has today, so `review: light` and no interactive session. `manual-verify` — the fit is a browser measurement and `ADR-0089` §2b forbids a browser gate* | S | done |
| | [TASK-121303](tasks/TASK-121303-the-front-doors-three-doors-are-three-doors.md) The front door's three doors read as three doors, not as one word — *audit fix set 3 of 3, `R4` at beat 1; `manual-verify`; separates them and must **not** dress them — `DEC-094` stays open and unanswered* | XS | done |
| | [TASK-121304](tasks/TASK-121304-the-table-reads-this-duels-award-and-not-the-last-ones.md) The table reads this duel's award, not the last duel's — *product; **`high`**; the round's one functional defect — **outside the audit fix set and outside `A(1)`** ([ADR-0096](../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §5), filed to the one ledger and scheduled here because the eight-cap does not bind. A real gate: two new cases fail today* | XS | done |
| **[STORY-1214](stories/STORY-1214-a-duel-played-by-hand-deadlocked-on-presence.md)** A duel played by hand deadlocked on presence, and the catalogue could not have caught it — *schema 2; **not a round story** — found by the human playing by hand, outside every round, so `EPIC-12` §Termination rule 1 puts it in the ordinary backlog and no `A(N)` or `B(N)` moves; registers `DEC-107` (architect) and `DEC-108` (product owner)* | | | **done** |
| | [TASK-121401](tasks/TASK-121401-the-catalogue-sees-a-present-player-marked-away.md) The catalogue sees a present player marked away — *`CORE-06` gains presence, a false Reconnect preamble goes, and `CORE-21`–`CORE-23` are added **red on today's product*** | S | done |
| | [TASK-121402](tasks/TASK-121402-the-duel-table-column-fits-the-phone-it-is-nested-in.md) The duel table's column fits the phone it is nested in — *`manual-verify`; `TASK-121302` merged with its measurement undischarged and the client reads **712 / 664**; a **new** ticket, `STORY-1213` not reopened. **Merged at a measured 664.90625 / 664** (`scrollHeight` 665) under [`ADR-0106`](../docs/adr/ADR-0106-a-sub-pixel-residual-is-a-fit-and-one-pixel-is-the-fence.md), which answered `DEC-112`: a true excess strictly under one CSS pixel **is** a fit. The 48 px came off `main` and moved to the seven sections that relied on it — the six `max-w-[380px]` ones, the front door's bare `<section>` and `DuelResult`, the last found by review. 720 × 900 is an exact 900 / 900. The remaining 0.90625 px is owned by `DuelTable.tsx` and `ActionBar.tsx`, outside this ticket's `Files`, and is re-filed once per `ADR-0106` §4* | XS | done |
| | [TASK-121404](tasks/TASK-121404-a-connections-room-becomes-a-session-type-the-directory-can-read.md) A connection's room becomes a session type the directory can read — *`ADR-0104` §2's half of the presence repair, split out because the `ADR-0069` probe found it **green on its own**: `./gradlew check -PrequireDocker=true` exits 0 with the move applied and nothing else, and `ADR-0068` makes a change with a green intermediate state two tickets rather than one declared `atomic:`. Carries the ADR's **"single most important word"** — `RoomMembership.code` gains `@Volatile` — and gives it the only gate that can fail on it, a reflection check on the JVM modifier, since its absence "never [fails] on one thread". No behaviour change: delivery still crosses rooms after this merges* | S | done |
| | [TASK-121403](tasks/TASK-121403-presence-is-about-the-room-the-reader-is-in.md) Presence is about the room the reader is sitting in — *the blocker, **re-cut whole on 2026-09-02** against [`ADR-0104`](../docs/adr/ADR-0104-a-frame-reaches-the-connection-in-the-room-it-is-about.md), which answered `DEC-107`. `writerFor(player)` is **deleted** for `writerFor(player, room)`, `register` takes the connection's `RoomMembership`, and `deliver` edits one line — its signature and **all seven call sites untouched**. `Files`, `Tests`, acceptance criteria and `verify:` all replaced and **measured by probing the whole pull-request gate set** to exit 0 (2 453 tests, no suite skipped; three red runs named their own paths first). **`atomic:` at 6 files on the Kotlin compiler** — the deletion and the new arity redden `SeatDelivery.kt:40`, `DuelSocket.kt:195`, `SeatDeliveryTest.kt:59`, `ConnectionDirectoryTest.kt` ×12 and `DuelSocketWriterDirectoryTest.kt` ×4 in one step. `ADR-0104` §6's "not `atomic:`" is about the `PROTOCOL_VERSION` bump it does not carry, and the version does not move. Red-before/green-after is measured: mutate the room away and exactly **six** new tests fail. `depends_on: [TASK-121404]`* | S | done |
| | [TASK-121405](tasks/TASK-121405-the-measured-reproduction-is-a-server-test.md) The measured reproduction is a server test — *`ADR-0104` §7's first requirement at the socket: the host's **own device** opens a roomless second connection, the guest closes, and that lobby socket is told nothing. Closes a real blind spot — the merged `aThirdSocketInNoRoomIsToldNothing` **passes on the broken product**, because its third socket handshakes as a different device and is therefore a different player, the same shape `STORY-1214` found in `CORE-18`. No gate drags `DuelSocketDisconnectTest.kt` into the repair's radius, so it is its own ticket and says plainly that it is a **regression guard**, green when written, earning its place by a quoted mutation. `depends_on: [TASK-121403]`* | XS | done |
| | [TASK-121406](tasks/TASK-121406-the-store-is-scoped-to-the-room-the-server-last-named.md) The store is scoped to the room the server last named — *`ADR-0104` §4, and explicitly **not** what makes the system correct: it bounds §5's third window, the one no server check can close. Measured while probing: the reducer change alone leaves the whole client gate set green — **117 files, 985 tests** — so **no existing test observes the current unconditional `RoomJoined` case** and the ADR's warning that this branch "can rot with every test green" is confirmed, not assumed. Three new reducer tests, each killed by a different wrong implementation. Shares no file with the server tickets; `depends_on: [TASK-121403]` because landing it first would ship, alone, the option the ADR calls **"the trap in the option set"** — one line that turns the recorded trace green while the defect stands* | XS | done |
| **[STORY-1215](stories/STORY-1215-the-duel-tables-last-sub-pixel-and-the-headroom-it-buys.md)** The duel table's last sub-pixel, and the headroom it buys — *schema 2; **not a round story** and **no due date** — [`ADR-0106`](../docs/adr/ADR-0106-a-sub-pixel-residual-is-a-fit-and-one-pixel-is-the-fence.md) §1 rules the residual **is a fit**, so nothing here repairs a defect and `EPIC-12` §Termination rule 1 keeps it out of every `A(N)` and `B(N)`. It sits under its own story because `STORY-1214` is `done` and this is not its subject* | | | **done** |
| | [TASK-121501](tasks/TASK-121501-the-columns-whitespace-gives-one-token-step-at-the-phone.md) The column's whitespace gives one token step at the phone — *`manual-verify`; `ADR-0106` §4's single re-filed ticket, buying **23.09375 px** of headroom where the column stands 0.09375 px from the fence, and retiring §5's second read. **The ADR's named lever is inert and the probe says so**: `100cqi` resolves to **390px**, `(390 − 220) / 21.25` is exactly 8, and the computed padding stays **8px** with the clamp's floor at 8px, 4px, 2px or **0px** — so the **ramp** moves, not the floor, in the **two** files that transcribe it (`21.25` appears nowhere else in the repository). The card is in the budget because it owns the number, on `ADR-0103` §4's composing path — both endpoints are declared tokens, so composing and not minting. A five-child reproduction of the column at 390 × 664 reads a true **664.90625** / `scrollHeight` **665** today and a true **664** / **664** with the new clamp; the acceptance criterion is still the running stack's, never a gate (`ADR-0089` §2b). Whole pull-request gate set run with the change stubbed — Gradle with Docker (2 416 tests), `npm run check` (117 files, 988 tests), `npm run build`, the ticket linter, both `unittest` suites and `check-drift.sh` — all exit 0, naming no third path. If §3.1's whitespace does not yield the pixel, the ticket **stops and registers `DEC-113`**, which `ADR-0106` §4 makes a legitimate ending* | XS | done |

`STORY-1201` — the harness itself — **shipped before its story existed**: the two agents, the
skill, the driver and the catalogue merged in `#1159` and `#1161` with no story file and no
tickets. That gap in the trail is now closed by a **retrospective record** rather than by a
manufactured split, because tickets written against merged code can only be satisfied by an empty
diff. The record says plainly that it was written afterwards, and it gives the specific reason no
split was ever possible: `#1159` was opened as the **concrete proposal `DEC-082` would be decided
against** — *"so the architect answering `DEC-082` can read what is actually proposed rather than a
description of it"* — so until `ADR-0089` merged at 10:59 UTC no ticket built on it was startable,
and five minutes later the harness had merged. Its acceptance criteria are structural checks on
merged files, each a command, all run at `5848e529`; they are not a coverage claim and `ADR-0089`
§2c forbids citing them as one.

`STORY-1203` is the **authoring half**, and it is **not a round story** — the epic's Stories table
says so in its own row rather than leaving the reader to notice. It builds
`.claude/skills/qa-cases/SKILL.md`, the skill
[`ADR-0090`](../docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) §3
licensed the same day: `/qa-cases EPIC-04 EPIC-05` reads the epics, the ADRs, `docs/duel-rules.md`
and the client's literals, plans and lands the suites through `build-epic` as ordinary reviewed PRs,
and then **stops**, printing the `/qa-cycle` command the human types next. It runs no browser,
brings no stack up and dispatches neither QA agent. The story also completes `ADR-0090` §2's
allow-list: **exactly three** files under `.claude/` may name `qa-cycle`, the third is the file this
story creates, and its ticket carries that check as a `verify:` line so the list is enforced from
the moment it is complete. The gates were measured under both the shim `grep` an agent shell
resolves and `/usr/bin/grep`, and agreed — `0` with three files, `1` with a fourth.

`STORY-1204` is **the first pass that skill performs**, and it is **not a round story either** —
which moves the numbering a second time, so the round stories now resume at **`STORY-1205`** and the
epic's table says so in its own rows rather than leaving a reader to notice that `STORY-1203`'s
sentence about `STORY-1204` no longer holds. It writes the `EPIC-04` and `EPIC-05` suites into
`docs/test-plan.md` from merged sources — no stack, no browser, no cycle — and it lands as two
sequenced tickets rather than one, because both edit the same file and a batch that started them
together would conflict by construction. **Ten of the two epics' twenty-one Definition-of-done
promises produced a case; eleven are recorded as uncovered with a reason each**, in
§*What this catalogue does not cover*, and **none produced a `DEC`**: both epics closed behind long
ADR chains, so every reachable promise's expectation was already a merged literal or a merged
clause, and the refusal rule removed cases rather than expectations. Four findings came out of
reading the client rather than the epics — a fresh profile's strip races its own socket, so every
strip read follows a reload; three `wait` targets collide with the first screen's own door labels;
no case may assert an absolute rank on a database that persists between rounds; and *"the ladder
shows a month"* is an assertion that passes on the very defect `ADR-0061` §6 forbids. Both suites
carry `ADR-0090` §5's `Provisional` line, so the round that first runs them is expected to correct
cases, those corrections are **harness** tickets excluded from `B(N)`, and no production code may
change for one until it has reproduced by hand (`ADR-0089` §4).

`STORY-1207` is the **third non-round story**, and it is the one that makes the cycle carry a
second focus. [`ADR-0092`](../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md)
(`DEC-085`) merged on 2026-08-30 naming its own deliverables, and this story is that split: a
`shot` verb (CDP screenshot, Node built-ins, never committed, **no image-diff tooling ever**), a
`## UAT` section of `docs/test-plan.md` reusing the catalogue as a **route map**, a `uat` observer
with no `Write` and a `QUESTIONS` section capped at three per screen, the `uat` focus of
`qa-cycle`, and a `qa-manager` that triages both focuses on one ledger. The chain is linear —
harness, catalogue, observer, focus, manager — because §7 requires the catalogue section to merge
**before the first UAT round** and because each ticket names something the one before it created.
Three refusals are carried as refusals: the QA focus **never chains into** the UAT focus, neither
report prints the other's command, and a preceding QA cycle is practice and **never a checked
precondition** — a check would cite a round as a gate (`ADR-0089` §2c). `.claude/agents/qa.md`
stands byte-unchanged with its `sha256` gated in four tickets, and `qa-manager.md` still names
`qa-cycle` **nowhere**, which is the accident the split is most likely to produce and is gated
from both directions. The split also folded in a measured defect the ADR does not mention:
`notify.py state --clear` left `cron_armed: true` and a note from an earlier session behind, and
since `ADR-0092` makes UAT a second focus of the **same** cycle — one run state, one `Stop` hook —
a breadcrumb that lies after a QA teardown lies identically after a UAT one. It is the story's one
unit-testable ticket and it goes first. **No `DEC` was raised.** The one contradiction found while
splitting — `ADR-0091` §5 owes cards for `#/verify` and `#/reset`, which the catalogue puts
permanently out of the harness's reach because no mailed link ever arrives — is answered by
`ADR-0092`'s own text: §4 files a missing-card finding only for a screen **in scope**, §6 already
admits `out of scope` as a per-screen cell, and §4 leaves that debt to `ADR-0091` §5's `EPIC-06`
remainder. What is left is an asymmetry rather than a gap — those two cards will never have a
conformance check behind them — and `TASK-120703` writes it into the inventory rather than letting
it be discovered. `DEC-086`, the written bar for *"ready for real users"*, stays open for the
product owner and blocks nothing: no round may be cited as what made the product ready.

`STORY-1212` is the **fifth non-round story**, and it is the one that makes the cycle carry a
**third** focus.
[`ADR-0096`](../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md)
(`DEC-096`) and
[`ADR-0097`](../docs/adr/ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md)
(`DEC-097`) both merged on 2026-08-31 naming their own deliverables, and this story is that split:
a `size` verb (`Emulation.setDeviceMetricsOverride` with **four fields and no fifth**, a read-back
from the page, exit 1 on a mismatch), an `audit` observer with no `Write` — the **fifth** declared
file `ADR-0090` §2 now admits — the `audit` focus of `qa-cycle`, and a `qa-manager` that triages
three focuses on one ledger with `A(N)` beside `B(N)`. The chain is linear — harness, observer,
focus, manager, epic — because each ticket names something the one before it created, and because
`STORY-1208` is the standing evidence of what two drifting copies of one rule cost. Three refusals
are carried as refusals: **no focus ever chains into another**, no report prints another focus's
command, and a preceding round of any focus is practice and **never a checked precondition**.
`.claude/agents/qa.md` **and** `.claude/agents/uat.md` both stand byte-unchanged with their
`sha256`s gated in five tickets — `ADR-0096` §2 freezes `ADR-0092` §3 for both those focuses, and
that freeze is `ADR-0097` §4's whole case for a fifth file — and `qa-manager.md` still names
`qa-cycle` **nowhere**, gated from both directions. The story writes **no rubric document and
transcribes no criterion**: `ADR-0096` §2 supplies the rubric merged, closed and citable by id, so
the observer and the manager point at it exactly as the `uat` observer points at
`docs/test-plan.md` §*UAT*. **One `DEC` was raised** — `DEC-098`, the architect's, on whether the
rubric is that ADR section grown by amendment or a working document elsewhere — and it **blocks
nothing**: both readings have merged text behind them, the first audit round runs at either, and
the cost of the answer is one line in each of two files. Answered the same day by
[`ADR-0099`](../docs/adr/ADR-0099-the-rubric-is-the-adr-section-and-a-criterion-is-born-merged.md):
the rubric is `ADR-0096` §2 **itself**, grown only by an amending ADR, and no working
document ever exists — a criterion is *born* merged, so a document could only ever have
held a copy — and the cost fell to **zero lines**: both tickets already cite the decided
form, and the story's refusal to transcribe stopped being pending and became the rule.
## EPIC-13 — The living table

Opened 2026-09-02 on the human's raw feedback after they played a duel end to end, and **split into
eleven stories the same day**, once the `product-owner` agent had answered all six of its product
decisions. **`STORY-1301`–`STORY-1304` are cut into tickets** — three, seven, four and eight; the
first three are done and the fourth is startable. The other seven are not cut, because
`/plan-story <STORY-ID>` does that one story at a time, and
the first ticket of every story that puts a new surface in front of a player is its **design card**,
which merges before the ticket that implements it is startable (`ADR-0091` §2, `EPIC-13` *Design
first*).

**Eight items, eleven stories.** Item 4 becomes three — its card, its server half, its client half —
because the card is the only part of it `DEC-120` does not block, and keeping it whole would have
parked the epic's largest item behind an unanswered decision. Item 8 becomes two for the same
reason: `ADR-0112` §6's reproduction attempt needs no decision, and the guard it feeds waits on
`DEC-123`. Nothing merged: each item owns a card, a lifetime and a human's visual verdict of its own.

**One version step, once.** `STORY-1308` is the only story in this epic that moves the wire — the
five other answering ADRs each say so in as many words — so `ADR-0047`'s one-bumping-branch lock is
uncontended inside the epic and is checked against the rest of the board instead. Its bumping ticket
is `atomic:`, sized by `ADR-0070`'s probe run to green and never by another ticket's file list.

**Nothing here opens `poker-engine`.** `potTotal` already exists, a clock may not enter a pure
library, and chips and marks are drawings. A story that finds it needs the engine raises a `DEC`.

| Story | Task | Est | Status |
| --- | --- | --- | --- |
| **[STORY-1301](stories/STORY-1301-pot-names-every-chip-committed-to-the-hand.md)** `Pot` names every chip committed to the hand — *item 2; `ADR-0107`; **split into three tickets on 2026-09-02**. The split probed the change before writing the tickets and **corrected one of the story's own design notes**: the `Pot` pins in `DuelTable.test.tsx`, `Lobby.test.tsx` and `reconnect.test.tsx` **do not move** — every one of their fixtures holds `committedThisStreet: 0`, so the `5,675` beside commitments of 125 and 825 that `ADR-0107` attributes to `DuelTable.test.tsx` is `no-derivation.test.tsx`'s. Applying the sum reddens exactly one test in the repository, `expected [ 6625 ] to deeply equal []`, so no ticket opens those three files and `TASK-130103` pins them by measured per-file count instead* | | | **done — all three tickets merged on 2026-09-02.** `Pot` now names every chip committed to the hand: the card corrected (`TASK-130101`), the never-derives guard narrowed to exactly one named sum (`TASK-130102`), and the strip summing the view (`TASK-130103`). No hand opens at `Pot 0` again. The split's measured correction to `ADR-0107`'s Consequences held: not one pin in `DuelTable.test.tsx` (22), `Lobby.test.tsx` (80) or `reconnect.test.tsx` (8) moved, and all three counts were gated to prove it |
| | [TASK-130101](tasks/TASK-130101-the-duel-table-card-prints-the-pot-adr-0107-names.md) The duel-table card prints the pot `ADR-0107` names — *design; the card first (`ADR-0091` §2, and `EPIC-13`'s Design-first rule), and a **correction** rather than a new card, so no state enumeration is owed. Two `<span class="amount">` literals: `Pot&nbsp;2,450` → `Pot&nbsp;2,850` in both frames, measured at exactly two occurrences with no third. The card's own sizing row already agrees — `TASK-120914` merged `Raise to 3,650` = 400 + (2,850 + 400) on 2026-08-31, so the pot node is the last number on the card still quoting the collected pot. Three refusal gates pin `3,650` at four, the rival's `committed 400` at two and `duel-table-states.html`'s `Pot&nbsp;3,250` at one. `seat-and-pot.html`, `colors.html` and `type.html` also draw `2,450` and **checked not to move**: no bet-line beside any of them* | XS | done |
| | [TASK-130102](tasks/TASK-130102-the-never-derives-guard-admits-one-named-sum-and-no-other.md) The never-derives guard admits one named sum and no other — *`ADR-0107` §5's narrowing, given its own diff because the ADR calls it **"a real weakening of a test whose whole value was having no exceptions"**. `allowedNumbers(view)` = `numbersIn(view)` ∪ `{potTotal(view)}`, and the fixture-independence sweep runs over the **wider** set — so it now also proves no pair of the fixture's own numbers doubles, sums or differences onto the carve-out. One new test pins the carve-out at exactly one member wide, names it (`6625`), and shows a second sum (`pot + betToMatch` = 7,125) still rejected; it passes both before and after `TASK-130103`, so landing the narrowing first is bounded rather than blind. A gate refuses the string `PotStrip` anywhere in the file: a guard that borrowed the sum it guards could never catch a wrong one. Count of 7 measured on the finished file, not computed* | S | done |
| | [TASK-130103](tasks/TASK-130103-the-strip-prints-every-chip-committed-to-the-hand.md) The strip prints every chip committed to the hand — *the behaviour: `view.pot` + both `committedThisStreet`, **summed in the strip** rather than threaded as a prop (`ADR-0107` §5's shape, `ADR-0101` §7's precedent) — a prop would open `Lobby.tsx` and `DuelTable.tsx` for no gain. `PotStrip.tsx`'s docstring inverts and a gate refuses its old sentence; `PotStrip.test.tsx`'s `takes the pot from the view and not from what the seats put in` is renamed with its fixture unmoved, because the sum it guards against is wrong under both semantics. **Nothing merged moves, so the whole burden is on two new tests**, both measured red-before / green-after: `Pot 150` off a 0 + 50 + 100 fixture and `Pot 3,400` off a 2,450 + 125 + 825 one, with `2,575` and `3,275` refused so a one-seat sum cannot pass. Four count gates pin `no-derivation` at 7, `DuelTable` at 22, `Lobby` at 80 and `reconnect` at 8* | S | done |
| **[STORY-1302](stories/STORY-1302-the-host-waits-at-the-table.md)** The host waits at the table, and both promises move with them — *item 5; `ADR-0110`; **split into seven tickets on 2026-09-02**, one chain, `TASK-130201` startable. The split made two calls the story left to it. **The frames land in `design/screens/duel-table.html`**, not a new card file: `ADR-0110` §8.2 makes the arrival frame that file's own `Phone — 390 × 664` drawing, so the host-alone frames beside it turn the transition into an adjacency rather than a cross-file claim; it is the only card declaring the 390 × 664 box §8 puts the fit in; and a new file would copy ~90 lines of preamble, column and seat plate before drawing anything, plus a cloud `_ds_manifest.json` entry no ticket can make. **The null-view contract lands as a file** — `TASK-130206`'s `web-client/src/table/null-view.test.tsx`, rendering the real branch and sweeping text nodes *and* `aria-label`/`title`, so a clock, a chip pile or a last-act figure at the empty seat prints a digit and reddens it. The split also measured the blast radius rather than estimating it: exactly **three** assertions in `Lobby.test.tsx` go vacuous (`queryByRole("heading", { name: "Waiting for your rival" })` at lines 556, 888, 908) and exactly **one** test's expectation moves (`adds exactly two strings…`, which gains `You`), so the file's count is gated at **80** through all five client tickets. `ADR-0114` §6's one-render `waiting` window is **`STORY-1311`'s**, not this story's* | | | **done — all seven tickets merged on 2026-09-02.** The dedicated waiting screen is retired: the host waits at the duel table, the rival's empty seat says `Waiting for your rival`, and the invite is drawn beside them (`ADR-0110`). `ADR-0073` §3's two promises moved verbatim and are still made — 4x on the card and once in `WaitingTable.tsx` — so `ADR-0105` §2's reasoning still cites a sentence a surface renders. The story also landed the epic's **null-view contract** (`null-view.test.tsx`): every table surface a later story adds must now say what it shows when `view === null`, enforced rather than remembered. That file's `review: deep` earned its cost — it caught `spoken()` being read only through a digit filter, so a game fact spoken as a *name* escaped the guard its own docstring claimed to provide |
| | [TASK-130201](tasks/TASK-130201-the-card-seats-the-host-alone-at-the-phone.md) The card seats the host alone at the phone, at rest — *design first (`ADR-0091` §2, `EPIC-13`'s rule). One frame in `design/screens/duel-table.html`'s `.viewport.phone` box: the dashed empty seat saying `Waiting for your rival`, `ADR-0110` §5's three invite parts, `You`, and §4's way out with its promise. Adds `--pd-track-code: 0.14em` to a `:root` that declares no `--pd-track-*` today, and copies `.seat.seat-empty` from `create-duel.html` with its 1 px + 17 px metric intact. Ten refusal gates, all measured 2026-09-02: `class="pot"`, `class="bar"`, `class="board"`, `class="dealer"` and `Pot&nbsp;` stay at 2, `role="img"` at 16, and `10,000`, `Open seat`, `Link copied.` and `minutes` at 0 — because copying the retired frame wholesale would drag a starting stack and a duration onto a surface `ADR-0110` §3 and `ADR-0072` §6 forbid them on* | S | done |
| | [TASK-130202](tasks/TASK-130202-the-cards-three-remaining-variants-and-the-arrival.md) The card's three remaining host-alone variants, and the arrival — *`ADR-0110` §8.1's other three named frames — the copy succeeded, the copy was refused, no clipboard API — plus §8.2's arrival, discharged as a `.note` on the existing `Phone — 390 × 664` frame rather than a fifth drawing, because that frame **is** the arrival and a redraw would be a second maintenance site for one picture. `Copy the link` is gated at **3**, not 4: the client keeps the button in both feedback states and drops it only where there is no clipboard* | S | done |
| | [TASK-130203](tasks/TASK-130203-the-invite-is-a-component-of-its-own.md) The invite is a component of its own, and the DOM does not move — *`InvitePanel` in `web-client/src/table/`, because `ADR-0110` §5 puts the invite on the table. A pure extraction: `CopyLink` and the `roomLink` derivation move with the markup, `autoFocus` stays, no `<section>` is introduced (three `Lobby.test.tsx` tests scope with `.closest("section")`), and the gate is `Lobby.test.tsx` still passing **80** with no edit to the file. Its own five tests carry both clipboard fallbacks in one case, because either alone would pass against a component that had severed the other* | S | done |
| | [TASK-130204](tasks/TASK-130204-the-host-alone-table-is-a-component.md) The host-alone table is a component, drawn as the card draws it — *`WaitingTable`, built but **not wired** — a gate asserts `WaitingForRival` is still in `Lobby.tsx`, so the wiring cannot leak in and make this four files. Its root is the tree's only `<section>` and carries the duel column's class list verbatim, with the reason the literal repeats: `ADR-0103` §5 forbids a column **nested** in a column, not two branches that never render together. `SeatPlate` is unusable here — it takes a `SeatView` and always prints `formatChips(seat.stack)` — so a gate refuses both names, along with `PotStrip`, `BoardCards` and `ActionBar`* | S | done |
| | [TASK-130205](tasks/TASK-130205-creating-a-duel-lands-the-host-at-the-table.md) Creating a duel lands the host at the table, and the waiting screen is gone — *the branch swap, and the tests it invalidates, named rather than discovered. `Waiting for your rival` stops being a heading, so `queryByRole("heading", …)` goes vacuous on exactly three measured lines — 556, 888 and 908, across two tests — and each becomes `queryByText`. Measured this run so no coder has to find it: the live table renders `Waiting for your rival…` from `ActionBar.tsx:228`, and the ellipsis makes `queryByText("Waiting for your rival")` **null** there under the default `exact: true`. `adds exactly two strings…` becomes `…and no seventh`, asserting the **sorted** text-node set so nothing pins layout — the link never appears in it, being an `<input value>`. Count gated at 80: three edits, no test added or deleted* | S | done |
| | [TASK-130206](tasks/TASK-130206-what-the-table-shows-when-there-is-no-view.md) What the table shows when there is no view, written down as a gate — *`ADR-0110`'s standing tax on the rest of the epic, paid once and as its own reviewable diff: `null-view.test.tsx` renders the **screen**, not the component, and sweeps text nodes plus `aria-label`/`title`. Two inputs, deliberately — with `7Q4M9K2T` the only digit-bearing string on screen is the code, with `ABCDEFGH` there is none — so a literal compiled into a component cannot pass as an echo of the store. A third test fires all four probes at the live table and requires each to find something, because four refusals over probes that never work is the vacuous form of this guard; it asserts *"more than zero"* and never a count, so a later `EPIC-13` story changing the live table cannot redden it for an unrelated reason. `review: deep` — `ADR-0110` §3 is `ADR-0002` applied, and the failure it forbids is a client asserting a game fact* | S | done |
| | [TASK-130207](tasks/TASK-130207-the-retired-frame-leaves-the-card-and-the-inventory.md) The retired frame leaves the card, and the inventory names where the host waits — *last, not first: adding the new frames ahead of the client is `ADR-0091` §2, but removing the old one ahead of the client would leave a shipped screen with no frame and an inventory row pointing at a card that no longer draws its state — `ADR-0092` §4's `high`. Deletes `create-duel.html`'s second frame and only the CSS it alone used, keeping `.mark`/`.mark .coin` because `check-drift.sh` clause 6 compares that lockup against `graphics/wordmark.html`. Two `docs/test-plan.md` edits: the `SMK-04`/`CORE-20` inventory row's card, and `CORE-20`'s locative `do` cell — its `expect` and `fails if` untouched, so it is not a regrade (`ADR-0092` §7). `CORE-07` says "the waiting screen" too and means the live table's bar; its gates are scoped to the `CORE-20` line so it is not swept along* | XS | done |
| **[STORY-1303](stories/STORY-1303-the-acting-seat-is-marked-and-the-mark-moves.md)** The acting seat is marked, and the mark moves — *item 1; the human's eye on the drawing, `ADR-0115` on the stillness; **split into four tickets on 2026-09-02**, one chain, `TASK-130301` startable. `DEC-124` was answered and merged the same day, so **nothing here is blocked**. The split read `develop` and settled four things the story left open. **The still form already ships** — `.seat.on-turn`'s accent edge and `SeatPlate.tsx`'s `border-l-accent`, against a 2 px slot reserved off-turn — so `ADR-0115` §1 costs nothing to satisfy and everything to keep; `TASK-130303` gates `acting-mark`, `border-l-accent` and `border-l-transparent` in one assertion so the animation can never be traded for the edge. **Minting and the first card are one ticket**: nothing `--pd-motion-*` and no `prefers-reduced-motion` rule exists anywhere (re-measured), and a period minted before anything was drawn at it could not be corrected by a card ticket that does not hold the sheet — `ADR-0115` §4 hands the block's exact CSS to *"the minting ticket's"* judgment, and the vendored `web-client/src/styles/tokens.css` rides along because `tokens.test.ts` compares buffers. **The mark's home is `design/components/seat-and-pot.html`**, not a screen card: it is the canonical both screens copy, it already draws the seat *"in its states"* with **both** on-turn seats and an `.away` row, and three drawn states fit one `S` diff in 129 lines where a new table frame would cost sixty. So **no frame is added and `role="img"` does not move** — 16 and 24, `class="frame"` 6 and 3, `viewport phone` 5, all refusal gates in `TASK-130302`* | | | **done — all four tickets merged on 2026-09-02.** The acting seat is marked and the mark moves: `--pd-motion-turn-period`/`-ease` minted, `@keyframes pd-acting-seat` drawn on the component card and copied byte-identically to both screen cards, and `SeatPlate` renders `border-l-accent acting-mark` when `onTurn`. **`ADR-0115`'s still form is preserved by construction** — the fact lives in the accent edge that already shipped, and the animation only ever sits beside it; one gate pins `acting-mark`, `border-l-accent` and `border-l-transparent` together so it can never be traded away. Settled for later motion work: **Tailwind 4.3.3's `@utility` form does emit** into the production bundle, so the plain-rule fallback was not needed. The still-block in `tokens.css` now stills **both** `animation` and `transition`, product-wide |
| | [TASK-130301](tasks/TASK-130301-the-first-motion-tokens-and-the-marks-two-forms.md) The first motion tokens, the sheet's one still-block, and the seat's mark in both forms — *minting (`ADR-0091` §3), so the token and the drawing are judged by one eye in one pane; the human's verdict may trail the merge. `design/tokens/tokens.css` gains `--pd-motion-turn-period` and `--pd-motion-turn-ease` beside the product's **one** `@media (prefers-reduced-motion: reduce)` block, whose exact CSS `ADR-0115` §4 assigns here by name — zeroed tokens freeze a mid-flight frame, `animation: none` snaps to the base one, and the PR says which it took. `seat-and-pot.html` then draws all three of `ADR-0115` §6's states at both seats: the two existing on-turn rows gain the motion, a third row carries a `stilled` modifier for *acting — at rest*, and four verbatim captions make the naming a gate. Fifteen gates, every count measured 2026-09-02: `class="seat on-turn` 2 → **3**, `Your turn` 1 → **2**, `Their turn` and `aria-`/`role=` unmoved at 1 and 0* | S | done |
| | [TASK-130302](tasks/TASK-130302-the-two-screen-cards-carry-the-moving-mark.md) The two screen cards carry the moving mark, and nothing else on them moves — *`grep -rl on-turn design/` finds exactly three files; `TASK-130301` drew the canonical, and these two declare `.seat.on-turn` as a faithful copy of it, so leaving them behind would draw one class two ways. Copy only: the tokens into each `:root` (`check-drift.sh`'s value clause then compares them to the sheet), `@keyframes pd-acting-seat`, the rule, one media query. **Paint inside the reserved 2 px slot** — if the drawing needs geometry outside it, that is `ADR-0103` §3's give list running out, which its own words make a `DEC` and not a wider ticket. Twelve refusal gates hold every other number on both cards, and `stilled` is pinned at zero so the at-rest state stays drawn once* | S | done |
| | [TASK-130303](tasks/TASK-130303-the-acting-seats-mark-moves-and-the-still-mark-stays.md) The acting seat's mark moves on the table, and the still mark stays beside it — *`app.css` gains `@keyframes pd-acting-seat` and the `acting-mark` class, both **outside** `@theme static` because `theme.test.ts` requires every line inside that block to be a reset or a `var(--pd-*)` reference — a merged test a keyframes block would redden. `SeatPlate.tsx`'s on-turn branch keeps `border-l-accent` and gains `acting-mark`; `onTurn` itself is untouched, so `ADR-0046` §1's order holds by construction and an `AWAY`/`ABSENT` seat on turn gets no mark. The mark **speaks nothing** — `aria-label` pinned at exactly 1, `the button`. Three tests (5 → **8**), and the production bundle is grepped for `pd-acting-seat`, `acting-mark` and `prefers-reduced-motion`, because a Tailwind rule that compiles to nothing would pass every source-level gate* | S | done |
| | [TASK-130304](tasks/TASK-130304-the-mark-is-at-the-seat-the-server-named.md) The mark is at the seat the server named, and nowhere before it names one — *`review: deep`: the first story to amend `null-view.test.tsx`, `EPIC-13`'s standing contract, landed under a `deep` review because `ADR-0110` §3 is `ADR-0002` applied. **The mark renders nothing when `view === null`** — `Lobby.tsx` renders `WaitingTable` there and `WaitingTable` draws its own seat rows, never mounting `SeatPlate` — and it speaks nothing anywhere, so neither the digit sweep nor the `spoken()` closure changes shape; the new test asserts the absence with a positive control on the live table, because a selector matching nothing anywhere passes forever. In `DuelTable.test.tsx`, **both directions in one test**: `view-fixture.ts` defaults to `viewerSeat: 0, seatToAct: 0`, so a test that never writes `seatToAct: 1` cannot tell the view's field from a hard-coded seat — a gate greps for that literal. Counts: 22 → **24**, 4 → **5**, `no-derivation` pinned unmoved at 7, and `spoken(`/`digitBearing(` pinned at their measured 10 and 4 so the guard cannot be thinned while being added to* | S | done |
| **[STORY-1304](stories/STORY-1304-the-table-marks-the-last-act.md)** The table marks the last act, and the next deal clears it — *item 3; `ADR-0109`; **split into eight tickets on 2026-09-02**, one chain, `TASK-130401` startable. The split read `develop` and settled three things the story left open. **The mark is a reducer field** — `lastAct`, holding the whole act event, in `serverAction`'s register and `ADR-0109` §Consequences' own *"two reducer keys"* — which makes `ADR-0102` §1's queue do the hard clause for free: frames arriving while a hand's ending is painted are held until the last step has stood, so a `HandStarted` clears the mark **as the deal is painted** and never as its frame lands. The recorded script confirms the shape: the next hand's `HandStarted` rides its own `Events` frame, after the hand-completing `Snapshot`. **`view === null` shows nothing and a refresh loses the mark**, both written into tickets rather than left to a coder — `Lobby.tsx` renders `WaitingTable` there and it mounts no `SeatPlate`, and the mark speaks no `aria-label` and no `title` on any screen, so neither `null-view.test.tsx` closure changes shape; a resume gets a `Snapshot` with no `Events` in front of it, pinned as `a resume rebuilds no mark` rather than repaired (`ADR-0109` §Consequences, no wire bump). **`no-derivation.test.tsx` stays green because it never sees a mark** — all seven tests render `<DuelTable view={…} />` and pass no act, so the event's own `to`, which is not a `PlayerView` field, never reaches its sweeps; two tickets pin it at 7. Two traps measured and handed to `TASK-130403`: `duel-state.test.ts:43` enumerates every `DuelState` field with `toEqual`, and `:92` pins `Object.keys(duelState)` at three, so `isAct` stays module-private and `ActEvent` is a type export* | | | ready — **split**; `TASK-130401` startable |
| | [TASK-130401](tasks/TASK-130401-the-seat-card-draws-the-last-act-in-all-six-states.md) The seat card draws the last act, in all six of its states, at both seats — *design first (`ADR-0091` §2, `EPIC-13`'s rule). Six new plate rows on `design/components/seat-and-pot.html` — three at `ImKate`, three at `You` — `Fold` and `Check` bare, `Call 1,700`, `Bet 950`, `Raise to 2,300` and `All in 4,150` with a figure. Icon-versus-word, placement and colour stay the human's (`ADR-0024` §3, `ADR-0109` §5); three things do not: the mark rides in the plate's existing flex row so no box grows (`ADR-0103` §1), nothing about it moves (`@keyframes` pinned at 1, `animation` at 3 lines, `transition` at 0 — `ADR-0109` §4 and `ADR-0115`), and it mints no token. The four figures were chosen because all four read 0 today; `400` is not free, it sits inside `--pd-motion-turn-period: 2400ms`* | S | ready |
| | [TASK-130402](tasks/TASK-130402-the-two-table-cards-carry-the-mark-and-the-host-alone-frames-carry-none.md) The two table cards carry the last act in place, and the host-alone frames carry none — *`.last-act` copied character for character into both screen cards, as `.seat.on-turn` was. Two marks on `duel-table.html` (laptop and phone, `Bet 400` at `ImKate` — the only act consistent with her `committed 400` and the bar's `Call 400`), three on `duel-table-states.html` (`Check` at the hero on the turn frame, `Call 800` at the showdown — 3,250 + 800 + 800 = the 4,850 banner — and `Fold` beside the award line, which is `ADR-0109` §3's own worked example). **The four `Host alone` frames get none**, which is `ADR-0110` §3 drawn; a section-walking `awk` counts 0 there and a second identical walk finds `Waiting for your rival` 4 times, so the refusal cannot pass vacuously. No frame added: `class="frame"` 6/3, `role="img"` 16/24, `viewport phone` 5 all pinned* | S | backlog |
| | [TASK-130403](tasks/TASK-130403-the-reducer-remembers-the-act-just-made.md) The reducer remembers the act just made, and the deal that opens a hand takes it off — *`DuelState.lastAct: ActEvent | null`, set by any of the six acts, replaced by a later one, cleared by a `HandStarted` — one ordered walk over the frame's own events, so `Events[act, HandStarted]` clears and `Events[HandStarted, act]` does not, which is the third input the test pins and the one a *frame-contains-a-HandStarted* implementation fails. **Two merged tests are in the blast radius and this ticket owns them**: the initial-state literal gains `lastAct: null` (one line), and `Object.keys(duelState)` stays three, so `isAct` is module-private and `ActEvent` is a type export. 67 → **71*** | S | backlog |
| | [TASK-130404](tasks/TASK-130404-the-mark-stands-until-the-next-hand-is-painted.md) The mark stands until the next hand is painted, the duel's end retires it, and nothing else touches it — *one production line — `DuelFinished` clears it, `ADR-0075` §2's boundary guard applied to this field — and four tests. The first is the ADR's hardest clause in one flow: a fold's mark stands while `reveal` is non-null, **still stands after hand 2's `Events` and `Snapshot` have arrived and been queued**, and is gone after one `advanceReveal`. That middle assertion is what a clear-on-arrival implementation fails. Five labelled assertions cover the frames that leave it standing, gated by counting the verbatim string `leaves the mark standing` five times. `duelState.advanceReveal(` is called **zero** times in this file today — measured — so that gate is real. No timer, ever: a gate refuses `setTimeout`. 71 → **75*** | S | backlog |
| | [TASK-130405](tasks/TASK-130405-the-marks-words-are-the-buttons-words.md) The mark's words are the button's words, and its figure is the event's own total — *`lastActText(event)` in `action-text.ts` — the file `ADR-0109` §2 names — bridging the wire's `PlayerFolded`…`PlayerAllIn` spelling to `actionVerb`'s six words, with `event.to` on four of them and `null` on two. Six named tests, one per act, and **two figures on each of the four that carry one**, because a single figure cannot tell the event's field from a constant. Derivation is refused by shape, not by banning a character: `actionVerb(` pinned at exactly 11 (5 merged + 6 arms), `event.to` at exactly 4, `amount: null` at exactly 3, all measured. 5 → **11*** | S | backlog |
| | [TASK-130406](tasks/TASK-130406-the-seat-plate-draws-the-last-act-it-is-handed.md) The seat plate draws the last act it is handed, and speaks nothing — *the same three files `TASK-130303` spent on the acting mark: `@utility last-act` in `app.css` **outside** `@theme static` (`theme.test.ts` reddens otherwise), the prop on `SeatPlate.tsx`, and its own test file. The mark **speaks nothing** — `aria-label` pinned at exactly 1 and `title` at 0 — which is what keeps `null-view.test.tsx`'s `spoken()` closure closed two tickets later. Four tests: absent **and** `null` both draw nothing, fold and check are bare and carry no digit, and four acts with four different totals prove neither the verb nor the figure is hard-coded. No motion at all: `@keyframes` stays 1, `animation` stays 4 lines, `transition` stays 0. 8 → **12*** | S | backlog |
| | [TASK-130407](tasks/TASK-130407-one-mark-at-the-seat-the-act-names.md) One mark, at the seat the act names, and it moves when the other seat acts — *`DuelTable` hands the mark to the plate whose `seat.index` the event names and `null` to the other — `ADR-0109` §1's *exactly one mark, never one per seat*, which §Alternative 2 forecloses rather than defers. The replacement test re-renders with a second act at the **other** seat and asserts one `.last-act`, inside the right plate, with the first act's figure nowhere on the table: a per-seat implementation fails all three. `seat: 1` and `seat: 0` both read 0 in this file today, so both literal gates are real. 24 → **27**, with `no-derivation` pinned at 7 and `SeatPlate` at 12 to prove neither was reached* | S | backlog |
| | [TASK-130408](tasks/TASK-130408-the-screen-feeds-the-mark-and-there-is-none-before-the-first-snapshot.md) The screen feeds the mark, and there is none before the first snapshot — *`review: deep` — the second story to amend `EPIC-13`'s standing contract, which `TASK-130206` landed under a `deep` review because `ADR-0110` §3 is `ADR-0002` applied. One attribute in `Lobby.tsx` and one test in `null-view.test.tsx`, in **one** diff because that test is the wiring's only proof: its positive control applies an `Events` act and a `Snapshot` to a real store under the real `Lobby`, so deleting the attribute reddens it. Both open questions are written down here — nothing on the null view, printed or spoken, and no mark across a refresh until the next act. `spoken(` pinned at ≥ 12, `digitBearing(` at ≥ 4, `acting-mark` at ≥ 3, `Lobby.test.tsx` at 80. 5 → **6*** | XS | backlog |
| **[STORY-1305](stories/STORY-1305-a-bet-amount-can-be-typed.md)** A bet amount can be typed, and an illegal one is refused in the server's own numbers — *item 7; `ADR-0111`* | | | ready — waits on `STORY-1304` |
| **[STORY-1306](stories/STORY-1306-a-stack-is-chips-and-chips-move.md)** A stack is chips, and chips move — *item 6; the chip is **minted** interactively (`ADR-0091` §3); `DEC-124`* | | | ready — minting and card startable, client tickets blocked on `DEC-124` |
| **[STORY-1307](stories/STORY-1307-the-turn-clocks-card.md)** The turn clock's card — regular, running out, on timebank, expired — *item 4a; `ADR-0108` §5; the half no decision blocks* | | | ready — waits on `STORY-1306` |
| **[STORY-1308](stories/STORY-1308-the-server-states-a-deadline-and-plays-the-expired-seat.md)** The server states a deadline and plays the seat whose clock ran out — *item 4b; the only wire move in this epic* | | | **blocked on `DEC-120`** |
| **[STORY-1309](stories/STORY-1309-the-table-counts-down-and-the-pause-leaves-the-screen.md)** The table counts down, and the pause leaves the screen — *item 4c* | | | **blocked on `DEC-120`** |
| **[STORY-1310](stories/STORY-1310-the-refresh-paths-nobody-drove.md)** The refresh paths nobody drove, driven and written down — *item 8a; `ADR-0112` §6's six paths; no browser drive may be a `verify:` gate* | | | ready — decision-free, waits on `STORY-1301` **Settled 2026-09-02 while landing `TASK-130205`, so this story does not hunt a phantom:** a host who reloads while `WAITING` **does** return to the table. `boot.ts` re-sends `JoinRoom` on the next `Welcome`; `replyToJoinRoom` tries `resume()` — which is `null` for a `WAITING` room — and then **falls through to `join()`**, which succeeds and sends `RoomJoined`, so `roomCode` is set. A coder claimed the opposite by reasoning from the `resume()` path alone; the epic's driven measurement was right. |
| **[STORY-1311](stories/STORY-1311-only-a-running-duel-refuses-another-screen.md)** Only a running duel refuses another screen, and the refusal restores the address — *item 8b* | | | **blocked on `DEC-123`** |
