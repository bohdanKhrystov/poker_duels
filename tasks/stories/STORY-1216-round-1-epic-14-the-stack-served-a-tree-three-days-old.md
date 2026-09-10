---
id: STORY-1216
title: Round 1 (EPIC-14) — the stack served a tree three days old, so nothing at ea4bd05c was measured
type: story
status: ready
parent: EPIC-12
labels: [process, qa, harness]
depends_on: []
---

## The round

**Round 1** of the `/qa-cycle epic EPIC-14` invocation. The round number lives here rather than in
the id, per `EPIC-12`'s Stories table. It is the **eighth** non-round-shaped entry problem this
ledger has had to name and the **first** round in which the harness measured a different commit
than the one it reported.

| | |
| --- | --- |
| Round | **1** |
| Scope | `epic EPIC-14` |
| Date | 2026-09-10 |
| Commit **reported** by the round | `ea4bd05c` |
| Commit **actually served** to the browser | `c3e1a830` (2026-09-07), on branch `worktree-agent-af32ec8cf22f8819c` |
| Stack | `up` — and up from the **wrong tree**, both halves |
| Cases | 12 run, 8 passed, 3 failed, 1 blocked (as reported) |
| `B(1)` | **0** — after dedupe, after `ADR-0089` §4's harness exclusion, and after the classifier below |
| `B(0)` | n/a — round 1 has no round 0 |
| Baseline round | **no** — no screen became conformance-judgeable here; the `qa` focus has no per-screen conformance table at all |
| Verdict | **`STOP_INFRA`** (`EPIC-12` §Termination, Exit states) — see §*The verdict, and why `PASS` is refused* |

## What this record is not

`ADR-0089` §2c, restated because this round ends with `B(1) = 0` and that is the number most
easily inflated:

> No coverage claim. The product of a run is a **dated round record**. Neither it nor
> `docs/test-plan.md` may be cited as coverage in an epic's `Metrics`, a Definition of done, or a
> ticket's `verify:`. A `PASS` is a statement about one run, on one machine, at one commit.

**`B(1) = 0` here does not mean the product is clean. It means the product was never looked at.**
Every finding in the report was observed against a build that predates the code it is about. The
right reading of this round is *no evidence either way about `ea4bd05c`*, and the round story says
so in place of a verdict it did not earn.

## The finding that swallowed the round

`qa` reported `COMMIT: ea4bd05c` and drove `http://localhost:5173/`. Those are two different
things, and this round is the first time anybody checked whether they agreed. They did not.

**Measured on 2026-09-10, on the machine the round ran on:**

| Fact | Evidence |
| --- | --- |
| The dev server on `:5173` is rooted in a stale agent worktree | `curl -s http://localhost:5173/src/profile/ProfileStrip.tsx` embeds `window.$RefreshReg$ = RefreshRuntime.getRefreshReg("/Users/bohdankhrystov/AndroidStudioProjects/poker_duels/.claude/worktrees/agent-af32ec8cf22f8819c/web-client/src/profile/ProfileStrip.tsx")` |
| The JVM server on `:8080` is rooted in the **same** stale worktree | its process classpath begins `/Users/…/.claude/worktrees/agent-af32ec8cf22f8819c/poker-server/build/classes/kotlin/main` |
| Both were started on **Monday 04:00**, days before the round | process start times |
| That worktree's branch is at `c3e1a830`, dated **2026-09-07 04:26** | `git log -1 refs/heads/worktree-agent-af32ec8cf22f8819c` |
| **92** commits touching `web-client/` or `poker-server/` landed between it and `ea4bd05c` | `git log --oneline c3e1a830..ea4bd05c -- web-client poker-server \| wc -l` |
| Three of the files every failing finding is about **do not exist** in the served tree | `web-client/src/result/RematchNotice.tsx`, `web-client/src/profile/NameAsk.tsx`, `web-client/src/account/device-standing.ts` — all absent on disk under that worktree |

The stories whose behaviour the round went looking for and did not find are exactly the stories
that merged after `c3e1a830`: `STORY-1405`, `1407`, `1408`, `1409`, `1410`, `1411`, `1412`,
`1413`, `1415`, `1416`.

### The mechanism, which is a harness defect and not bad luck

Nothing here required anybody to be careless. The skill brings the stack up like this
(`.claude/skills/qa-cycle/SKILL.md`, *Bringing it up*):

```bash
scripts/qa/stack.sh db-up
CP=$(scripts/qa/stack.sh cp)
java -cp "$CP" duels.poker.server.ApplicationKt     # background
cd web-client && npm run dev                        # background
scripts/qa/stack.sh wait-server
scripts/qa/stack.sh wait-web
```

With a stale stack already listening on `:5432`, `:8080` and `:5173`:

1. **`db-up` dies.** `docker-compose` derives its project name from the checkout directory's
   basename, so each worktree is a *different* project, and `docker-compose.yml` publishes a fixed
   host port `"5432:5432"`. A second project's postgres cannot bind. Three postgres containers
   exist on this machine right now — `poker_duels-postgres-1`,
   `agent-a3397725a81f02dca-postgres-1`, `agent-a4b7dd7713c66b740-postgres-1` — one per checkout
   that ever ran the verb. This is exactly the `compose up failed` the round reported as a note.
2. **The new JVM cannot bind `:8080`** and the background task dies into a log nobody reads.
3. **The new Vite picks another port.** `web-client/vite.config.ts` sets no `strictPort`, so Vite
   silently moves to `:5174` and prints it to a background log.
4. **`wait-server` and `wait-web` both pass** — they `curl` `:8080/health` and `:5173/` and ask
   only whether *something* answers. Something did: the three-day-old stack.
5. **`drive.mjs` hardcodes `const APP = "http://localhost:5173/"`**, and that dev server proxies
   `/api` and `/ws` to `localhost:8080`. So both halves of the round consistently addressed the
   stale tree, and every case was internally coherent.
6. **The `COMMIT:` line is `git rev-parse --short HEAD` in the agent's own checkout** — a fact
   about where the agent is standing, never about what the browser loaded.

Six merged mechanisms, each individually reasonable, composing into a round that reports a commit
it never tested. `ADR-0089` §4 exists for precisely this class and the rule is applied below.

## Dedupe

Searched: every story under `tasks/stories/` and every `TASK-12NNNN` under `tasks/tasks/` — the
fifteen `STORY-12NN` ledgers and their tickets — plus every `TASK-14NNNN` (124 files, all `done`).
Matching on **behaviour, not wording**, and across all three focuses, per `ADR-0092` §6's one
ledger.

- **Repeats: 0.** No open ticket names any of these behaviours.
- **Regressions: 0.** Nothing filed and marked `done` came back. This was checked rather than
  assumed, and the check is the same evidence as everything else: the behaviours the round could
  not find were never in the tree the round drove, so no closed ticket was exercised at all.
- **New: 3 harness defects**, filed below. **New product defects: 0.**

## The classifier, applied finding by finding

`ADR-0089` §4 and `EPIC-12` §Termination rule 6: before a `blocker` or `high` may be filed, the
failing case must **reproduce by hand**. It reproduces → a product defect, counted in `B(N)`. It
does not → a **harness** defect, filed against `EPIC-12`, excluded from `B(N)`, and **no production
code may be changed to make it pass**.

Every hand reproduction below was attempted against the stack as the round found it, with a
**varied mechanism** — a fresh headless Chrome profile driven by my own CDP script rather than by
`drive.mjs`'s verbs — because a hand check that reuses the harness's own step inherits the
harness's own fault.

| Report finding | `qa` said | Hand check | Verdict | In `B(1)`? |
| --- | --- | --- | --- | --- |
| name dialog missing | `high` | Reproduced on the stale stack: fresh profile, first-ever `Play duel` → straight to `Waiting for your rival`, room `ZZP9M10C`. But `NameAsk.tsx` and `name-ask.ts` **do not exist** in the served tree | **harness** — untested at `ea4bd05c` | no |
| account never says "anonymous" | `high` | `ANONYMOUS_STATE` occurs **0** times in the served tree's `account-text.ts`; it is present at `ea4bd05c` | **harness** | no |
| permanence line still shown | `high` | Reproduced verbatim on the lobby of the stale stack. The string occurs **nowhere** under `web-client/src/` at `ea4bd05c` — `ADR-0130` is *correct*, the browser was three days old | **harness** | no |
| no rename control anywhere | `high` | Same cause. Two notes: the served tree puts `Set my name` on the **lobby strip** (the pre-`STORY-1409` surface), and the probe `innerText.includes('rename')` is unsound at any commit — the product never uses the word *rename*, the control is `Set my name` | **harness** | no |
| stale password state | `medium` | `hasPassword` reaches `AccountScreen` only after `STORY-1408`, which is not in the served tree | **harness** | no (`medium` never enters `B(N)`'s fix set either) |
| sign-out does not hand a new profile | `high` | `device-standing.ts` — the module `ADR-0135` §2 puts the whole answer in — **does not exist** in the served tree | **harness** | no |
| the rematch offer never reaches the rival | `high` | `RematchNotice.tsx` **does not exist** in the served tree. There was no panel to fail | **harness** | no |
| play again opens a second room | `high` | `heldRoom` occurs nowhere in the served tree's `poker-server/src/main` | **harness** | no |
| date locale | `low` | **Not stale.** `web-client/src/profile/profile-text.ts` is byte-identical between `c3e1a830` and `ea4bd05c` (`git diff --stat` empty), both call sites pass no `locales`, so `Intl.DateTimeFormat(undefined, …)` uses the runtime's locale. But this is **decided, merged behaviour**: `ADR-0061` §Consequences says in as many words that *"`finishedAtText` renders instants 'in the reader's locale'"*. `CLAUDE.md`'s *English everywhere* governs code, tickets, docs and commits — not a reader's rendered timestamp | **not a defect** — contradicts no merged source and matches one | no |

**`B(1)` = 0 blocker + 0 high = 0.** The three exclusions `ADR-0092` §5 and `ADR-0089` §4 name are
stated here as one list rather than as a rule and two footnotes, because forgetting any one of them
flips a verdict:

1. **Harness defects** — eight of the nine findings, for the reason above. Excluded because a
   stack pointed at the wrong tree reads as a product decaying, and step 5 of the loop would then
   merge a diff to satisfy a build that is not the product.
2. **Missing cards** — none this round; `ADR-0091` §5 debt is not what this focus collects.
3. **Decision-born tickets** — none this round; no `product-owner` answer produced a ticket here.

### Was `EPIC14-2` one defect or several?

**Neither.** The six findings under that id are six symptoms of **one** cause, and that cause is
not a product defect at all: the browser was running a build that predates `STORY-1407`,
`STORY-1408`, `STORY-1409` and `STORY-1410`. At `ea4bd05c` the code behind all six is present.
So the answer to *one or several* is **zero product defects and one harness defect** — and the
useful lesson is that a single id stretched over six symptoms was, this time, the correct
intuition for the wrong reason.

### Absent, or built and unreached?

The question the triage was asked to settle, per finding: **neither.** The code is present at
`ea4bd05c` and was **not loaded**. That is a third category, and it is worth naming because the
first two both point at a diff and this one points at the harness. Three of the files are not
merely unreached — they are **not on disk** in the tree the browser was served from.

## The verdict, and why `PASS` is refused

`B(1) = 0`, so Step 6's table reads `PASS` on its face. **`PASS` is refused, and the refusal is the
judgement this round exists to make.**

`EPIC-12` defines `PASS` as *"a round's report has zero `blocker` and zero `high`"*, and
`STORY-1206` spells out what that is a statement about: *one run, on one machine, at one commit*.
This round has no commit to make a statement about. Emitting `PASS` would convert *nothing was
measured* into *nothing is wrong*, which is precisely the coverage inflation `ADR-0089` §2c
forbids, and it would license closing a cycle over an untested product.

The verdict is **`STOP_INFRA`**, which `EPIC-12` §Termination's Exit states table already carries:
*the stack could not be brought up*. It was brought up — it was not the product's. Step 6's
five-row table in `.claude/agents/qa-manager.md` omits this state; that omission is itself worth a
line here, because a manager reading only the agent file has no true row to emit. It is recorded as
an observation, not filed: `TASK-121602` moves the report shape, and whether Step 6's table gains
the sixth row is a question for whoever next opens that file.

**Every stop is a successful run.** This one stops early, names the exact mechanism, files the
repair, and asks the human to open the next cycle once the harness can prove what it is driving —
which is the only legal way a cycle starts (`ADR-0089` §2b).

## The fix set

Three tickets, **all harness**, all against `EPIC-12` per `ADR-0089` §4, all excluded from `B(1)`,
and **none of them may touch production code**. The eight-ticket cap does not bind.

| Ticket | What it is for |
| --- | --- |
| `TASK-121601` | `stack.sh` gains `served` — the harness can name the checkout behind `:5173` and `:8080`, and the commit each is at |
| `TASK-121602` | a round records the tree it drove, and `qa` stops reporting its own `HEAD` as the commit under test |
| `TASK-121603` | `db-up` names one database wherever it is run from, so a worktree's copy stops opening a second project against a bound port |

**Deferred to the backlog: `TASK-121604`** — `chrome-up` occasionally failing to bind when three
are launched back to back, succeeding on isolated retry. `qa` itself called it local contention and
filed it as a note. It is real, it is small, and it is not what cost this round; scheduling it would
be work manufactured out of a symptom.

### What is deliberately out of scope, and why

**`web-client/vite.config.ts` does not gain `strictPort`.** It would stop step 3 of the mechanism
dead, and it is the tempting one-line fix. `ADR-0089` §4 forbids changing production code to make a
harness case pass, and the `server:` block of a Vite config is close enough to that line that
crossing it here would set the precedent for crossing it somewhere that mattered. `TASK-121601`
detects the mismatch instead, and `TASK-121602` refuses the round. If a later decision wants
`strictPort`, it is a ticket of its own with an ADR behind it.

## Found outside the round, and not filed

`EPIC-12` §Termination rule 1 freezes a round's bug set at triage: anything found during triage
that is not in the report cannot extend the round. Both of the following go to the **ordinary
backlog**, move no `B(N)`, and reopen nothing — the route `STORY-1214` and `STORY-1215` established.

**1. The standing rematch panel is reachable by no player action.** Read from source at
`ea4bd05c`, not driven, because no stack serving `ea4bd05c` exists to drive:

- `RematchNotice` renders only when `shown !== "first"` — that is, only while a *chosen* screen
  (`duels`, `leaderboard`, `account`, `sign-in`, `verify`, `reset`) is showing. `ADR-0138` §2 says
  so, and `RematchNotice.screens.test.tsx` proves it on all six.
- The three controls that open a chosen screen — `open("duels")`, `open("leaderboard")`,
  `open("account")` — are rendered **only in `Lobby.tsx`'s front-door branch**, after every store
  branch. The result screen's branch (`Lobby.tsx:340`) renders `DuelResult` and nothing else.
- `grep -rn 'href=' web-client/src --include='*.tsx'` finds exactly **two** links in the whole
  client, both `href="/"`: `DuelResult.tsx:62` and `WaitingTable.tsx:50`. There is no hash link
  anywhere.
- So a player on the result screen has one control, `Back to the lobby`, which is a full document
  navigation. `ADR-0123` §1 rules that case out by name — *"A player who has left the room is not
  followed, and cannot be"* — and `ADR-0138` §2 repeats it: *"the document is replaced and the
  store dies with it."*

The set of screens a player can actually reach while holding a finished room is therefore
**empty**, and `STORY-1415`'s thirteen tickets ship a surface no press can produce. `ADR-0123`
§Context justifies the whole decision on the premise that *"the product now invites a player to
leave the only screen on which a rematch can be seen"* — that invitation does not exist in the
markup. This is why the round's `EPIC14-4` finding, once retested against a correct stack, may well
come back: not because `RematchNotice` is broken, but because nothing reaches it.

It is not filed as a defect because the repair is a choice between two merged sources and filing
one would presume the answer. **`DEC-162` is registered for the human**, on `ADR-0123`'s own
routing: that ADR records the *whether* as *"the human's, stated verbatim"*, and the human's quoted
sentence — *"it shoud appear on any screen, like if opp come back to lobby it or any other page in
still shoud be shown"* — names the lobby, which §1 then carved out. Only the person whose sentence
it was may say which reading stands.

**2. `.claude/agents/qa-manager.md` Step 6 has no row for a round that measured the wrong tree.**
Recorded above; not filed.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-121601](../tasks/TASK-121601-the-harness-names-the-tree-it-is-serving.md) | The harness names the tree it is serving | ready |
| [TASK-121602](../tasks/TASK-121602-a-round-records-the-tree-it-drove.md) | A round records the tree it drove, not its own HEAD | backlog — serialised behind `TASK-121601`, whose output it reads |
| [TASK-121603](../tasks/TASK-121603-one-database-wherever-stack-sh-is-run-from.md) | One database, wherever `stack.sh` is run from | backlog — serialised behind `TASK-121601`, which touches the same two files |
| [TASK-121604](../tasks/TASK-121604-three-browsers-launched-back-to-back-all-bind.md) | Three browsers launched back to back all bind | backlog |

## Acceptance criteria

- [ ] `scripts/qa/stack.sh served` names the filesystem root and commit behind `:5173` and `:8080`,
      and its answer is proved red/green by a hermetic self-test case with no stack up.
- [ ] `.claude/agents/qa.md`'s report shape no longer carries
      `COMMIT: <git rev-parse --short HEAD>`, and the `qa-cycle` skill's *Bringing it up* refuses a
      round whose served commit is not the commit under test.
- [ ] `scripts/qa/stack.sh db-up` names the same compose project from any checkout in this
      repository, proved by the self-test's recorded call log.
- [ ] `DEC-162` has a row in `tasks/BOARD.md`'s decision register.

## Out of scope

- **Every product defect in the report.** All eight are withdrawn as unverified, not as absent.
  They are re-tested in round 2, against a stack that can name itself, and whichever of them
  survives is round 2's `B(2)` — round 1's frozen set does not follow them (`EPIC-12` §Termination
  rule 1).
- **`strictPort` on the Vite dev server** — §*What is deliberately out of scope* above.
- **The rematch panel's reachability** — `DEC-162`, the human's, ordinary backlog.
