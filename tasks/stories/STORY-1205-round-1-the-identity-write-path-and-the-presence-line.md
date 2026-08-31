---
id: STORY-1205
title: Round 1 — no request declares its body, and a navigation is not a disconnect
type: story
status: done
parent: EPIC-12
labels: [process, qa]
depends_on: []
---

## The round

**Round 1** of a `/qa-cycle regression` invocation. This is the round ledger `EPIC-12`
§Termination requires; the round number lives here rather than in the id, per the epic's Stories
table. It is the **first round story since `STORY-1202`** — `STORY-1203` and `STORY-1204` are not
rounds, which is why the numbering resumes here.

| | |
| --- | --- |
| Round | **1** |
| Scope | `regression` — SMOKE (6) + CORE (20) + EPIC-04 (5) + EPIC-05 (5) |
| Date | 2026-08-29 |
| Commit | `fe4bbf2a` |
| Stack | `up` — db, server, web |
| Cases | 36 catalogued; 32 run, 4 blocked. passed 27, failed 5 |
| `B(1)` | **1** — was 2 at first triage; `CORE-18` was reclassified, see below |
| `B(0)` | **n/a** — this is a new invocation, so the convergence rule cannot apply |
| Verdict | **`PROCEED`** |

## What this record is not

`ADR-0089` §2c, restated because a round record that omits it invites exactly the reading the
condition forbids:

> No coverage claim. The product of a run is a **dated round record**. Neither it nor
> `docs/test-plan.md` may be cited as coverage in an epic's `Metrics`, a Definition of done, or a
> ticket's `verify:`. A `PASS` is a statement about one run, on one machine, at one commit.

So: **this is a statement about one run, on one machine, at commit `fe4bbf2a`, on 2026-08-29.** It
is not a coverage claim, it may not be cited as one, and nothing here is permitted to appear in a
`verify:` block. Twenty-seven cases passed, five failed, four never ran. That is the whole of it.

`dist/` is still loaded by nothing — every case ran against `npm run dev`, so `ADR-0088` gap 3
survives this round exactly as it survived `STORY-1202`.

## The product defects — one, after re-triage

Two were filed at first triage. **One survived re-triage.** `ADR-0089` §4 makes a by-hand
reproduction a precondition of filing a `blocker` or a `high`, and both reproductions are
written out below — including the one that turned out to reproduce the harness rather than the
product, which is the more instructive of the two.

### `04-02` → `TASK-120501`: no request this client sends declares its body, so every write is a `400`

`qa` graded this `high` and reported four reproductions across two devices. **Severity unchanged.**

**The hand reproduction.** Ports 9232/9233 were still live, so the claim flow was walked again
with the driver, which is a player's hands (`ADR-0089` §3):

```
node scripts/qa/drive.mjs 9232 forget-room
node scripts/qa/drive.mjs 9232 open
node scripts/qa/drive.mjs 9232 click "Account"
node scripts/qa/drive.mjs 9232 wait "Give this profile a password"      # saw
node scripts/qa/drive.mjs 9232 type 0 mgrcheck0402                      # 12 chars, in the rules
node scripts/qa/drive.mjs 9232 type 1 managercheck-secret-1             # 21 chars, in the rules
node scripts/qa/drive.mjs 9232 click "Give this profile a password"
node scripts/qa/drive.mjs 9232 wait "This profile now has a password." 8000   # TIMED OUT
```

`SELECT count(*) FROM credential` was `0` before and `0` after. **It reproduces.** Product defect,
counted in `B(1)`.

**The mechanism, which `qa` could not see and which is why this ticket is larger than the finding.**
The client never sends a `Content-Type`. `web-client/src/account/sign-up.ts` builds
`headers: { "X-Device-Id": deviceId }` and a `JSON.stringify` body, so the browser labels it
`text/plain`; Ktor's `call.receive<SignUpRequest>()` refuses that and
`poker-server/.../http/AuthRoutes.kt` turns *every* decode failure into an empty-bodied `400`.
Two probes against the running server, chosen so neither could write a row, isolate it exactly:

| request | status |
| --- | --- |
| `POST /api/auth/sign-up`, real device id, **no** `Content-Type`, 5-char password | **`400`** |
| `POST /api/auth/sign-up`, real device id, **`application/json`**, same 5-char password | `422` — the body decoded, the password was judged |
| `POST /api/auth/sign-in`, **no** `Content-Type`, unknown handle | **`400`** |
| `POST /api/auth/sign-in`, **`application/json`**, unknown handle | `401` — the body decoded |

The `422` and the `401` are the proof: with the header the server reads the body and answers on
the merits. **The server is correct and the client is the defect.**

**The blast radius is the whole identity surface, not sign-up.** All seven body-carrying calls
omit the header — `sign-up.ts`, `sign-in.ts`, `set-name.ts`, `verify-email.ts`,
`attach-recovery-email.ts`, `forgot-password.ts`, `reset-password.ts`. Confirmed on the display
name, which no case in this round covers and which nobody had noticed was dead:
`type 1 "Manager Check"`, `click "Set my name"` left the lobby reading `No name`, and
`SELECT count(*) FROM player WHERE display_name IS NOT NULL` was **`0`** for all thirteen devices
in the database after a full regression run. `PUT /api/me/name` with the header answered `200`.

That is why `TASK-120501` fixes the transport rather than the one call site the case touched:
repairing `sign-up.ts` alone would leave six identical bugs behind and would not repair the defect
that was reported, only its first symptom.

**`04-03`, `04-04` and `04-05` are blocked by this and are not separate findings.** Each needs a
successfully claimed profile as its precondition. Four of the five-case EPIC-04 suite are
unreachable behind one defect, which is what makes this the round's first-priority ticket.

**`qa` was wrong on one detail, and it is worth correcting.** The report says *"the UI shows no
error text at all"*. It shows one: `SignUpForm.tsx` maps `400` to `handle-refused` and renders
`A handle is 3 to 32 of a–z, 0–9, dot, dash or underscore, and starts with a letter or a number.`
The screen capture in the hand reproduction above carries it. The refusal is worse than silence,
not better — it blames the player's handle for a request the client malformed — but the finding
should be true in its particulars.

### `CORE-18` → **reclassified as a harness defect** → `TASK-120506`

**This is the round's one reversal, and it is written out in full because a triage that quietly
changes its mind is worth less than one that shows its work.**

`qa` graded this `medium`. At first triage I raised it to `high` and filed `TASK-120502` against
three files under `web-client/src/`. **That was wrong.** The coder dispatched to it returned
`blocked` having changed nothing, reporting that none of the three files is the cause and proposing
`web-client/vite.config.ts`'s dev-server WebSocket proxy instead. I tested that hypothesis rather
than adopting it, and it is also wrong. `TASK-120502` is `dropped`; `TASK-120506` replaces it.

**What the product actually does**, when the player performs an action that ends their session —
closing the tab — **through the Vite dev proxy, which was the accused component**:

```
close A's app tab over CDP (/json/close/<targetId>)
  +4s   B: "Your rival is away. The duel is paused." 56   (seat plate reads Away)
  +8s   ... 52     +12s ... 48     +16s ... 44            (the grace window ticking)
reopen the room on A
  +4s   B: "Your rival is back."
```

Vite's upstream connections to Ktor fell from 3 to 1 at the instant of the close. **The teardown
crossed the proxy and the `OpponentPresence` push came back through it**, in under four seconds,
in both directions. There is no product defect here, and the proxy is exonerated by the same run
that would have convicted it.

**Why my own hand-reproduction was not a reproduction.** It produced A's "absence" with
`location.href='about:blank'` — the method `qa` used, which I repeated. On this headless Chrome
**that does not close the page's WebSocket at all.** A paired experiment, one tab, one socket
type, one variable changed, on a socket that **never touches the proxy**:

| done to a `ws://localhost:8080/ws` socket, proxy not in the path | after 30s |
| --- | --- |
| explicit `.close()` | **gone within 3s** — 2 sockets → 1 |
| `location.href='about:blank'` | **still ESTABLISHED** — 2 sockets → 2 |

The instrument was validated in the same run: opening the socket took the count 1 → 2 and closing
it took it 2 → 1, so it detects a real close. **A player whose socket is still open is present,
and the server is right to say so.** `CORE-18`'s precondition was never established, so the case
was never run — it reported on a disconnect that had not happened.

**Why the dev-proxy hypothesis is rejected, having been tested rather than argued.** It predicts a
teardown or a push lost in transit. Both cross that proxy above. And the paired table shows the
missing teardown on a path with no proxy in it, so the proxy cannot be what swallows it — on this
browser there was nothing to swallow. The coder's experiments 1, 2 and 5 (`.close()` direct,
abrupt exit, `.close()` through the proxy) all agree with my results; its experiment 3 — a real
tab, a real navigation, direct to the backend, `AWAY` arriving promptly — I could not reproduce,
and the paired control above is the reason I do not accept it.

**The coordinator's second question answers itself, and in the harness's favour twice over.** It
asked whether production puts a WebSocket proxy between client and server, since that decides
whether a player could ever hit this. `ADR-0026` §*What it forecloses* leaves both topologies open
— *"Ktor serving the built assets, or a reverse proxy — a later deployment story's choice"* — so
"production has no proxy" is not available as an argument. It does not matter: `server.proxy` is a
**dev-server** key, `vite build` emits static assets that contain none of it, and the accused
component therefore ships in no topology at all. And the proxy is not the cause anyway.

**The real defect is that the harness has no way to make a player leave.** `drive.mjs`'s ten verbs
are `open`, `text`, `click`, `wait`, `absent`, `type`, `link`, `device`, `forget-room`, `eval` —
none of them ends a session — and `CORE-18`'s `do` cell is the whole of *"during A's absence"*,
which does not say how absence is produced. So a tester improvises, and the improvisation was
silently wrong. That is the same shape as `SMK-03` in `STORY-1202` and it is a **harness** defect:
`TASK-120506`, repaired in `scripts/qa/drive.mjs` and `docs/test-plan.md`, excluded from `B(1)`,
**no production code changed**.

**`CORE-19` is not broken either.** My first triage recorded the return notice as missing and used
it to justify the upgrade to `high`. It was missing for the same reason — A had never left — and
it renders correctly above. That upgrade is withdrawn along with the classification.

**What this cost, and the lesson worth keeping.** `ADR-0089` §4 asks whether a failure reproduces
by hand. Round 1 answered yes for `CORE-18` — and every hand-check in this round reached the client
through `localhost:5173` **and through the same improvised absence**, so the reproduction inherited
the harness's own defect and satisfied §4's letter while inverting its purpose. §4 needs the
by-hand path to differ from the harness's path in the specific respect under suspicion, not merely
to be performed by a human. That sentence belongs in `ADR-0089` §4 and is not added here, because
a round story may not amend an ADR; it is left as the finding it is.

## The four harness defects, and why none of them counts

`ADR-0089` §4 and `EPIC-12` §Termination rule 6: a failure that does not reproduce by hand is a
**harness** defect — filed against this epic, repaired in `scripts/qa/` or `docs/test-plan.md`,
**excluded from `B(N)`**, and **no production code may change to make it pass**. Excluding them is
the load-bearing half: counted, a stale catalogue would read as a product getting worse and would
trip the convergence rule on a healthy product.

**This ticks `EPIC-12`'s open Definition-of-done box.** `STORY-1202` explicitly declined to tick it
because `SMK-03` never actually failed. This round has three failing cases, one reported
observation and — after re-triage — **one defect that had already been filed as a product `high`
and dispatched to a coder** that did not reproduce as product defects. All are filed against this
epic and kept out of `B(1)`. The rule is no longer untested prose; `TASK-120502` is the proof that
it bites late as well as early.

### `04-01`, `05-01`, `05-02` — and, unreported, `05-03` and `04-02` → `TASK-120503`

`qa` claimed all three are round-composition artifacts. **The claim holds, and it is bigger than
`qa` stated.** Judged, not adopted:

- `04-01` expects `No duels yet.`; `05-01` waits on `You have no place on this season's
  leaderboard.`; `05-02` asserts `You are rank ` stays absent. All three assume a device with **no
  finished duel**.
- Under `regression` scope that device cannot exist. The suites run in catalogue order, CORE
  necessarily plays duels on A and B before EPIC-04 is reached, and the round allocates **two**
  profiles for the whole session. Verified on the live stack after the round: A's ladder reads
  `You are rank 1 this season, on 1 duel coin.`
- Neither concrete `fails if` fired. `04-01`'s two failure conditions are `No profile yet.` and
  `Your duels did not load…`; neither occurred, so the behaviour the case actually guards — that
  an account-less device is not refused — **held**.

Two more cases carry the same rot and `qa` did not report them, which is the more valuable half:

- **`05-03`'s `do` cell reads *"read both self lines (both no-place)"*** and its `expect` pins the
  absolutes `on 1 duel coin.` / `on −1 duel coins.` `qa` marked it **passed** — by checking the
  **delta** against `duel_result` instead. That is a correct judgement and a **silent deviation**
  from the case as written, which is the `SMK-03` failure mode `STORY-1202` filed. Recorded so the
  catalogue stops describing something other than what is executed.
- **`04-02`'s own `expect` pins `−1 Duel coins`.** Once `TASK-120501` lands, a round-2 retest of
  `04-02` would fail on that absolute even though the claim now works — inflating `B(2)` on a
  harness artifact and risking a `STOP_DIVERGING` on a product that just improved. Repairing it is
  the single most time-critical line of `TASK-120503`.

`EPIC-05`'s own preamble already forbids this — *"No case asserts an absolute rank"* — and its
cases assert one anyway. That is what a provisional suite is for. `ADR-0090` §5 predicted this
round's shape and it is what happened: the suites were authored from merged decisions, which prove
what was *decided*, not what *shipped*. The same ticket deletes both `> **Provisional**` notes,
which `ADR-0090` §5 makes the first round record's job.

### `CORE-03` → `TASK-120504`

Blocked, not failed. The case needs a *"fresh profile C"*; `.claude/skills/qa-cycle/SKILL.md`
starts exactly two, on 9232 and 9233, and the `qa` agent may not start Chrome itself. A second tab
on an existing port shares that port's `pd.deviceId` and is the same player (`ADR-0018`), so there
is no way to run it. The fix allocates a third profile rather than weakening the case: `CORE-03`
guards *"Two players. Never three."*, which the vision states outright.

### The leaderboard's `Show more` → `TASK-120505`

`qa` reported, outside the 36 cases, that the control *"stays enabled with no effect once every
season entry is already shown"*. **It does not reproduce as a product defect, and the product is
right.** Read from the live ladder screen:

```
[{"text":"Show more","innerText":"Show more","hidden":true,"disabled":false,"offsetParent":"null"},
 {"text":"Back","innerText":"Back","hidden":false,"disabled":false,"offsetParent":"shown"}]
```

`LadderScreen.tsx` renders it `hidden={!canAskMore}` — deliberately hidden rather than unmounted,
so a second press lands on a control its guard can refuse. **A player cannot see or click it.**
The driver can: `drive.mjs`'s `clickExpr` filters on `!e.disabled` and nothing else, and Chrome's
`innerText` falls back to `textContent` for an unrendered node, so the driver finds a control no
player can reach and calls `.click()` on it directly. Harness defect, in `scripts/qa/drive.mjs`,
excluded from `B(1)`, and **no production code may change for it**.

It is worth more than the observation that produced it: the same blindness can make a future case
believe it paged when it did not — `05-04` walks the ladder with exactly this control.

## The observation that is not a defect at all: dates in Ukrainian

`qa` reported that duel-history dates render as `29 серп. 2026 р., 20:36` on an otherwise English
screen, and noted `CLAUDE.md`'s *"English everywhere"*. Reproduced immediately on the lobby.

**Filed as nothing, and the reason is a merged source, not a judgement.**
`web-client/src/profile/profile-text.ts`'s `finishedAtText` is documented as rendering *"in the
reader's locale"*, and `ADR-0061` §Costs names that behaviour and accepts it:

> **A UTC boundary meets locale-rendered times.** `finishedAtText` renders instants *"in the
> reader's locale"*, so a player far enough east or west can read a duel as finishing on
> 1 September and find …

`CLAUDE.md`'s rule names *code, tickets, docs, commits* — not a reader's date format. So this is
designed behaviour with a merged decision behind it. Filing it as a bug would have `build-epic`
change production code to satisfy a rule that does not cover it, which is the same error §4 exists
to prevent, arriving from the other direction. **No `DEC` is raised**: `CLAUDE.md` rule 5 asks for
one only when no ADR covers the question, and one does. If English-only dates are wanted, that is
a product decision for the `product-owner` agent, and it starts as a request, not as a defect.

**No catalogue case is added for it either.** The only merged source says *"the reader's locale"*,
which is machine-dependent; a case asserting it would be red on a differently configured machine —
precisely the rot `ADR-0090` §4 forbids writing into the catalogue.

## Dedupe

Searched: every story under `tasks/stories/` and every `TASK-12NNNN` under `tasks/tasks/`. Four
exist — `TASK-120201`, `TASK-120301`, `TASK-120401`, `TASK-120402` — and one round story,
`STORY-1202`, whose single finding was `SMK-03`'s device-id ordering.

- **Repeats: 0.** No finding this round matches a defect already filed and open.
- **Regressions: 0.** `TASK-120201` is `done` and `SMK-03` **passed** this round, so the one closed
  defect stayed closed. Nothing filed-and-done came back.
- **New: 6** — **one** product defect, **four** harness defects (one of them reclassified from
  a product defect after the fix set was dispatched), and one observation resolved as designed
  behaviour.

## `B(1)` = 1

`blocker` 0 + `high` 1, after dedupe and after the **four** harness defects are excluded.

It was **2** at first triage. `CORE-18` was filed as a `high` product defect and reclassified as a
harness defect once `ADR-0089` §4's test was re-run on a path that did not share the harness's own
fault. The recount is recorded here rather than quietly applied, because a `B(N)` that moves after
a fix set is dispatched is exactly the number a reader is entitled to distrust.

**The correction makes the cycle stricter, not laxer, and that is the right direction.** With
`B(1) = 1`, round 2 must reach `B(2) = 0` or the run ends `STOP_DIVERGING`. At `B(1) = 2` a round 2
reporting one `high` would have counted as convergence. A bar built on a misclassification is worse
than a hard one.

**Nothing was deferred**, so no deferral hides inside that number: the fix set is one ticket
against a budget of eight, and no severity was lowered to make a count fall. The one severity that
moved at first triage went **up** (`CORE-18`, `medium` → `high`); it is now withdrawn entirely,
with the evidence, not downgraded to `medium` to tidy the arithmetic.

**No `medium` and no `low` was filed to the backlog**, because after triage there were none.

**Verdict: `PROCEED`.** `B(1) = 1 > 0`, there is no `B(0)` to diverge from, and this is round 1 of
a budget of 3. `TASK-120501` has landed; retest.

## State this triage changed, disclosed

Two mutations, both on QA-owned rows, both disclosed because a round record that hides its own
side effects is worth less than none:

1. **A display name `x` was written** to the player behind device `Yv8Bj2yL3yFzE1KXqqyGQQ`, by the
   `PUT /api/me/name` probe with the header. The probe was chosen to be non-writing and a one-char
   name was expected to be refused; it was accepted. A name cannot be unset by the product
   (`ADR-0029`), so the row stands. It is a QA browser profile from this round, and round 2 gets a
   fresh profile and therefore a fresh player.
2. **Room `98GEYD8S` holds an unfinished duel** between the two round-1 profiles, from the
   `CORE-18` reproduction.

Neither affects `B(1)`, and both are wiped from the round's point of view by
`ADR-0089` §3's fresh-profile-per-round rule.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-120501](../tasks/TASK-120501-every-request-with-a-body-declares-that-it-is-json.md) | Every request with a body declares that it is JSON | done |
| [TASK-120502](../tasks/TASK-120502-the-rivals-presence-reaches-the-other-table.md) | The rival's presence reaches the other table | **dropped** — reclassified as a harness defect; superseded by `TASK-120506` |
| [TASK-120503](../tasks/TASK-120503-no-case-assumes-a-device-with-no-finished-duel.md) | No case assumes a device with no finished duel | ready |
| [TASK-120504](../tasks/TASK-120504-a-round-allocates-the-third-profile-core-03-needs.md) | A round allocates the third profile `CORE-03` needs | backlog — goes `ready` when `TASK-120503` merges; both edit `docs/test-plan.md` |
| [TASK-120505](../tasks/TASK-120505-the-driver-does-not-click-what-a-player-cannot-see.md) | The driver does not click what a player cannot see | done |
| [TASK-120506](../tasks/TASK-120506-a-case-can-end-a-browser-session-and-says-so.md) | A case can end a browser session, and says so | backlog — supersedes `TASK-120502`; goes `ready` when `TASK-120503` merges |

**The fix set is `TASK-120501` alone** — one `high`, against a budget of eight, and it has landed.
`TASK-120503`, `TASK-120504`, `TASK-120505` and `TASK-120506` are **harness** tickets against
`EPIC-12`: not in the fix set, not counted in `B(1)`, and no production file appears in any of
their `## Files` tables.

**Two of them must land before the round-2 retest**, or round 2 reports failures the product did
not cause:

- `TASK-120503`, or `04-02` and `05-03` are red whatever the product does.
- `TASK-120506`, or `CORE-18` is red again for the same reason it was red this round.

## Acceptance criteria

- [ ] Every finding in round 1's report was deduped against the existing round stories and tickets
      before triage; the search and its result are recorded.
- [ ] Every `high` still filed reproduces by hand **on a path that does not share the harness's
      own fault**, and the reproduction is written out per finding (`ADR-0089` §4).
- [ ] `B(1)` is computed and stated: **1**, and the recount from 2 is recorded with its cause.
- [ ] The verdict is exactly one of the five named exit states: **`PROCEED`**.
- [ ] Every severity change is written down with its reason — `CORE-18` `medium` → `high` at
      first triage, then **withdrawn entirely** when the finding was reclassified as a harness
      defect. Both moves, and the evidence for each, are recorded.
- [ ] Every harness defect is filed against `EPIC-12`, repaired in `scripts/qa/`,
      `docs/test-plan.md` or the skill, and excluded from `B(1)`; no production file appears in
      any of their `## Files` tables.
- [ ] The record states, in its own words, that it is one run on one machine at one commit and not
      a coverage claim (`ADR-0089` §2c).
- [x] `TASK-120501` is merged.
- [ ] `TASK-120502` is `dropped` with its reasoning kept in the file, not deleted.

## Out of scope

- **The four blocked cases as findings.** `CORE-03` is a harness gap and `04-03`/`04-04`/`04-05`
  are downstream of `04-02`. A blocked case is not a failure and none is counted in `B(1)`.
- **Anything found while repairing this fix set.** `EPIC-12` §Termination rule 1 freezes the
  round's bug set at triage; a defect found during repair or retest belongs to round 2's report.
- **English-only dates.** Designed behaviour with a merged source; see above. A product request,
  not a defect, and not this cycle's.
- **`web-client/vite.config.ts`.** Accused of dropping the socket teardown, tested, and
  exonerated: the teardown and the `OpponentPresence` push both cross it in under four seconds.
  Nothing in this round changes it.
- **Serving `dist/` instead of `npm run dev` in a round.** Raised as a candidate repair for
  `CORE-18`. Declined *as that repair*, because the dev server is not the cause and fixing a
  phantom teaches the loop the wrong lesson. It remains a good idea on its own merits —
  `ADR-0088` gap 3, that the built bundle is proven by nothing — and belongs in its own ticket
  argued on that gap, not smuggled in as a defect repair.
- **The triplicated comment block in `poker-server/.../http/AuthRoutes.kt`.** Noticed while reading
  the sign-up route, is in no case's `expect`, changes no behaviour, and is not a QA finding. If it
  is worth fixing it is its own ticket.
