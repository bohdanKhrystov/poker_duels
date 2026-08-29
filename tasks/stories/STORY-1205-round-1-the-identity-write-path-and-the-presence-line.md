---
id: STORY-1205
title: Round 1 — no request declares its body, and the presence line never arrives
type: story
status: ready
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
| `B(1)` | **2** |
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

## The two product defects

Both were **reproduced by hand by the manager**, not merely relayed. `ADR-0089` §4 makes that a
precondition of filing a `blocker` or a `high`, and the reproductions are written out below
because *"it looked real"* is not a reproduction.

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

### `CORE-18` → `TASK-120502`: nothing on the presence channel ever reaches the other player

`qa` graded this `medium`. **Upgraded to `high`, and the reason is below.**

**The hand reproduction**, a full timeline on the live stack:

```
A create room 98GEYD8S, B join by link, hand 1 dealt, both screens agreeing
node scripts/qa/drive.mjs 9232 eval "location.href='about:blank'"   # A leaves the app
poll B at +4s +8s +12s +16s +20s +24s  → byte-identical screen every time,
                                          no "Your rival is away", no countdown
A confirmed on about:blank throughout (`location.href` read back twice)
node scripts/qa/drive.mjs 9232 open "http://localhost:5173/?room=98GEYD8S"   # A returns
poll B at +3s                          → still nothing; no "Your rival is back."
```

**It reproduces.** A player navigating away from a tab is an ordinary player action. Product
defect, counted in `B(1)`.

**B's socket was alive the whole time**, which is what makes this a presence defect rather than a
frozen client: the moment A acted, B's screen updated in full — stacks, committed, the turn and
the action set. B receives everything except presence.

**Why `high` and not `medium`.** Three reasons, written down because an unexplained severity change
is how a real defect gets buried, and because this change *raises* `B(1)` rather than lowering it:

1. **The whole channel is dark, not the away half.** `qa` reported the away marking missing. The
   reproduction above shows the **return** notice missing in the same room, so `CORE-19`'s subject
   is broken by the same cause. `qa` passed `CORE-19` earlier in the round and that pass is not
   overturned here — but the defect is wider than the case that caught it.
2. **A player mid-duel cannot tell a thinking rival from a vanished one.** The vision's one success
   condition is *"we play a full heads-up match"*; a rival who leaves and a rival who is deciding
   render identically, for as long as the grace window lasts. There is no workaround, which is the
   `medium` row's own test.
3. **Two shipped stories are inert in a browser** — `STORY-0214` (the wire names an absent
   opponent) and `STORY-0313` (the table names an absent opponent) — while every gate under them
   is green. `poker-server`'s `DuelSocketDisconnectTest` asserts the other seat is told
   (`aClosingSocketTellsTheOpponentItIsAway`, `aClosingHostTellsTheGuestItIsAway`); the client's
   `PresenceNotice.test.tsx`, `presence-copy.test.tsx` and `duel-state.test.ts` assert the render
   and the reducer. Both halves pass and the whole is broken. That is `ADR-0088`'s gap exactly,
   and it is the reason `EPIC-12` exists.

## The three harness defects, and why none of them counts

`ADR-0089` §4 and `EPIC-12` §Termination rule 6: a failure that does not reproduce by hand is a
**harness** defect — filed against this epic, repaired in `scripts/qa/` or `docs/test-plan.md`,
**excluded from `B(N)`**, and **no production code may change to make it pass**. Excluding them is
the load-bearing half: counted, a stale catalogue would read as a product getting worse and would
trip the convergence rule on a healthy product.

**This ticks `EPIC-12`'s open Definition-of-done box.** `STORY-1202` explicitly declined to tick it
because `SMK-03` never actually failed. This round has three failing cases and one reported
observation that did not reproduce as product defects, filed against this epic and kept out of
`B(1)`. The rule is no longer untested prose.

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
- **New: 6** — two product defects, three harness defects, one observation resolved as designed
  behaviour.

## `B(1)` = 2

`blocker` 0 + `high` 2, after dedupe and after the three harness defects are excluded.

**Nothing was deferred**, so no deferral is hiding inside that number: the fix set is two tickets
against a budget of eight, every qualifying defect is in it, and no severity was lowered. The one
severity that moved went **up** — `CORE-18`, `medium` → `high` — which raises `B(1)` and makes
round 2's convergence bar harder, the conservative direction.

**No `medium` and no `low` was filed to the backlog**, because after triage there were none: the
three `low`s `qa` reported are harness defects, and the one remaining observation is designed
behaviour. That is stated rather than padded with tickets nobody would schedule.

**Verdict: `PROCEED`.** `B(1) = 2 > 0`, there is no `B(0)` to diverge from, and this is round 1 of
a budget of 3. Repair the fix set, then retest.

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
| [TASK-120501](../tasks/TASK-120501-every-request-with-a-body-declares-that-it-is-json.md) | Every request with a body declares that it is JSON | ready |
| [TASK-120502](../tasks/TASK-120502-the-rivals-presence-reaches-the-other-table.md) | The rival's presence reaches the other table | ready |
| [TASK-120503](../tasks/TASK-120503-no-case-assumes-a-device-with-no-finished-duel.md) | No case assumes a device with no finished duel | ready |
| [TASK-120504](../tasks/TASK-120504-a-round-allocates-the-third-profile-core-03-needs.md) | A round allocates the third profile `CORE-03` needs | backlog — goes `ready` when `TASK-120503` merges; both edit `docs/test-plan.md` |
| [TASK-120505](../tasks/TASK-120505-the-driver-does-not-click-what-a-player-cannot-see.md) | The driver does not click what a player cannot see | ready |

**The fix set is `TASK-120501` and `TASK-120502`** — the two `high`s, two tickets against a budget
of eight. `TASK-120503`, `TASK-120504` and `TASK-120505` are **harness** tickets against `EPIC-12`:
they are not in the fix set, they are not counted in `B(1)`, and no production file appears in any
of their `## Files` tables.

`TASK-120503` should nevertheless land **before** the round-2 retest, for the reason given above:
without it, `04-02` and `05-03` are red in round 2 whatever the product does.

## Acceptance criteria

- [ ] Every finding in round 1's report was deduped against the existing round stories and tickets
      before triage; the search and its result are recorded.
- [ ] Every `high` filed reproduces by hand, and the reproduction is written out per finding
      (`ADR-0089` §4).
- [ ] `B(1)` is computed and stated: **2**.
- [ ] The verdict is exactly one of the five named exit states: **`PROCEED`**.
- [ ] Every severity change is written down with its reason — one, `CORE-18` `medium` → `high`.
- [ ] Every harness defect is filed against `EPIC-12`, repaired in `scripts/qa/`,
      `docs/test-plan.md` or the skill, and excluded from `B(1)`; no production file appears in
      any of their `## Files` tables.
- [ ] The record states, in its own words, that it is one run on one machine at one commit and not
      a coverage claim (`ADR-0089` §2c).
- [ ] `TASK-120501` and `TASK-120502` are merged.

## Out of scope

- **The four blocked cases as findings.** `CORE-03` is a harness gap and `04-03`/`04-04`/`04-05`
  are downstream of `04-02`. A blocked case is not a failure and none is counted in `B(1)`.
- **Anything found while repairing this fix set.** `EPIC-12` §Termination rule 1 freezes the
  round's bug set at triage; a defect found during repair or retest belongs to round 2's report.
- **English-only dates.** Designed behaviour with a merged source; see above. A product request,
  not a defect, and not this cycle's.
- **The triplicated comment block in `poker-server/.../http/AuthRoutes.kt`.** Noticed while reading
  the sign-up route, is in no case's `expect`, changes no behaviour, and is not a QA finding. If it
  is worth fixing it is its own ticket.
