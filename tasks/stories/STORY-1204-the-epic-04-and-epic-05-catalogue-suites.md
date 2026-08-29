---
id: STORY-1204
title: The EPIC-04 and EPIC-05 catalogue suites, authored from merged sources
type: story
status: ready
parent: EPIC-12
labels: [process, qa, meta]
depends_on: [STORY-1203]
---

## Goal

`docs/test-plan.md` carries a suite for `EPIC-04` and a suite for `EPIC-05`, each written from
merged sources with no stack and no browser, each carrying `ADR-0090` §5's `Provisional` line, and
each row citing the merged source of its expectation in a fifth `source` column. The two epics
leave §*Not yet written* and what neither suite reaches is written down in §*What this catalogue
does not cover* rather than quietly omitted.

## This is not a round story, and the numbering moves again

`EPIC-12`'s Stories table says *"one story per QA round; the round number lives in the story, not
the id"*. **This story breaks that convention**, the way `STORY-1201` breaks it by being a
retrospective record and `STORY-1203` breaks it by building a tool. It is the first authoring pass
`/qa-cases EPIC-04 EPIC-05` performs: it brings no stack up, starts no browser, dispatches neither
QA agent, invokes no cycle, and reports no `B(N)`.

`STORY-1203`'s PR wrote *"the round stories therefore resume at `STORY-1204`"*. **That sentence is
now wrong and this story's PR corrects it**: this story takes `1204`, so the round stories resume
at **`STORY-1205`**. The epic's Stories table gains a row saying so in the same shape `STORY-1203`'s
row uses, and its note says the convention has now moved twice. A convention silently shifted once
is unreliable; shifted twice without saying so it is worthless.

## Why

`docs/test-plan.md` §*Not yet written* has listed `EPIC-04` and `EPIC-05` with **no cases at all**
since the catalogue was written, and `ADR-0090` §Consequences names the pass that fills them as the
one *"with the most never-executed cases meeting step 4 at once"*. `STORY-1203` built the skill that
does it. Nothing else can: `qa` has no `Write`, `qa-manager` writes only bug tickets from a round it
was handed, and `qa-cycle` is the runtime half.

Both epics are **closed** — `EPIC-04` is `done`, `EPIC-05` is `ready` with `STORY-0506` its last
open thread — so their Definitions of done are settled promises rather than moving targets, and the
ADRs behind them are merged. That is exactly the condition `ADR-0090` §4 requires and the reason
this pass is worth running before a round rather than during one.

## Design notes

Everything below is settled by merged documents. Nothing here is open, and this pass raises no
`DEC`.

### The three constraints that shaped the split

**1. The driver writes nothing** (`ADR-0089` §3). It reads anything — the DOM, `localStorage`, the
database, the server's log — and the only storage write it may make is `forget-room`. So **no case
seeds a store, a socket frame or a database row to reach a screen**, and every precondition below
is built by playing. Where a promise's precondition cannot be built that way, no case is invented
for it; it is written down in §*What this catalogue does not cover*.

**2. A case with no merged source is not written** (`ADR-0090` §4). Every row carries a `source`
cell: the module holding the literal for player-facing text, otherwise an ADR section. **Nothing
in these two epics needed a `DEC`** — see *Why no decision was raised* below, which says how that
was checked rather than asserting it.

**3. Both suites live in one file**, so the two tickets are **sequenced, never parallel**:
`TASK-120402` declares `depends_on: [TASK-120401]` and carries a regression gate that reddens if the
`EPIC-04` suite loses a row or a column while the `EPIC-05` suite is added.

### The promises, and what each produced

`EPIC-04` promises **12** things and `EPIC-05` promises **9**. Ten produced a case; eleven are
recorded as uncovered, each for a reason stated in the catalogue.

| Epic | Promise | Outcome |
| --- | --- | --- |
| 04 | every story `done` or `dropped` | uncovered — a board fact |
| 04 | play, win, name, claim, sign in from a second device | `04-05` |
| 04 | a claim leaves the balance byte-identical | `04-02` |
| 04 | no password in a body, a log or a `ServerMessage` | uncovered — the epic names its own method, *"asserted structurally"* |
| 04 | wrong password and unknown account are indistinguishable | `04-04` |
| 04 | nothing resolves a player from a display name | `04-03` |
| 04 | `/api/me` and `/api/me/duels` still answer anonymously | `04-01` |
| 04 | history paging is total and disjoint | uncovered — needs 11 finished duels and a concurrent insert |
| 04 | `V1` and `V2` byte-unchanged | uncovered — a repository fact |
| 04 | `poker-engine` inside the `ADR-0010` allowlist | uncovered — a repository fact |
| 04 | `verifyProtocolTypes` still passes | uncovered — a gate |
| 04 | checked by hand once, and recorded | uncovered — a receipt, and recovery by mail is unreachable |
| 05 | every story `done` or `dropped` | uncovered — a board fact |
| 05 | open the ladder from the first screen and leave it | `05-01` |
| 05 | a duel moves both standings by exactly `ADR-0014`'s arithmetic | `05-03` |
| 05 | a negative balance appears, in its correct position | `05-04` |
| 05 | the ladder names its season, from the response | `05-05` |
| 05 | the self line, and *no place* is not a zero | `05-02` (and `05-01`'s wait) |
| 05 | `ADR-0012`'s gate discharged in writing | uncovered — discharged by `ADR-0063`; a written record |
| 05 | `docs/protocol.md` contracts every endpoint | uncovered — a document fact |
| 05 | `poker-engine` untouched by every commit | uncovered — a repository fact |

**10 + 0 + 11 = 21.**

### Four findings the modules gave up, which a suite written from titles would have missed

These are why this pass read the client rather than the epics alone, and each is a `SMK-03`-class
defect caught before a round paid for it.

**1. A fresh profile's first load races, so every strip read follows a reload.** `pd.deviceId` is
written when the socket's `Welcome` lands (`web-client/src/store/boot.ts`), while `readProfile` runs
once at mount (`web-client/src/profile/profile-provider.tsx`) and `GET /api/me` refuses an unknown
device — *the socket mints, HTTP refuses* (`poker-server/.../auth/IdentityResolver.kt`). So a fresh
profile's first render shows `No profile yet.` and the strip is never re-read. Every case that reads
the strip opens twice.

**2. Three `wait` targets collide with the first screen's own buttons.** `Your duels`,
`Leaderboard` and `Account` are the labels of the first screen's three doors *and* the headings
behind them, so waiting on them proves nothing about the swap having happened. The suites wait on a
string that exists only past the door: `Opponent name` for the duels screen, the self line for the
ladder, `Give this profile a password` for the account screen, and `Forgot your password?` for the
sign-in screen.

**3. No case may assert an absolute rank.** The database persists between rounds, so ranks depend on
every duel ever played on that machine. What is deterministic is that a **fresh** profile's first
finished duel moves its standing by exactly one, and `ADR-0089` §3 already mandates a fresh Chrome
profile per round. A case pinning `rank 1` would be red for the machine's history.

**4. *The ladder shows a month* is an assertion that passes on the defect it is meant to catch.**
A client deriving the season from `new Date()` prints the same month a correct one does, on the day
a round runs. `05-05` therefore reads `GET /api/standings` — a read, licensed by `ADR-0089` §3 — and
compares the printed name against the season the response carried.

### The minus sign is U+2212 and the gates pin its bytes

`coinBalanceText` (`web-client/src/profile/profile-text.ts`) writes a negative balance with
`U+2212 MINUS SIGN`, not `U+002D HYPHEN-MINUS`: `−1`, bytes `e2 88 92 31`. A case written with an
ASCII hyphen would be red on a correct product forever. Both tickets gate the literal glyph, and
the mutation that swaps it for a hyphen was run and reddens the gate.

### Reading the database is legitimate, and only in `CORE-13`'s shape

`ADR-0089` §3 names the database in the list of things the harness may read, so `05-03`'s `psql` is
inside the licence. It is written the way `CORE-13` is written and for the same reason: **the
rendered number is the observation and the row is a second witness**, and the case is red when they
disagree. A case whose only observation is a `psql` row would be a test of the server, which
`poker-server/.../e2e/` already covers against real Postgres, and a round `PASS` over such a case
would read as the coverage claim `ADR-0089` §2c forbids. Every other `EPIC-05` case reads a screen.

### Why no decision was raised

`ADR-0090` §Consequences predicted this pass would produce `DEC`s, and it produced none. That is a
fact about these two epics, not a shortcut: both closed behind long ADR chains — `ADR-0027`,
`ADR-0029`, `ADR-0031`, `ADR-0037`, `ADR-0049`, `ADR-0056`, `ADR-0083`, `ADR-0087` for identity;
`ADR-0061`, `ADR-0063`, `ADR-0064`, `ADR-0065`, `ADR-0066` for the ladder — and every reachable
promise's expectation is already either a merged literal in a text module or a clause in one of
those ADRs. The refusal rule still bit, but it removed **cases**, not expectations: eleven promises
are uncovered, and each is uncovered because of a fact about the driver or about the promise's own
stated method, never because nobody has decided what the product should do.

**The one place a guess would have been easy** is `EPIC-04`'s *no password in a body, a log or a
`ServerMessage`*. A DOM assertion for it is available and would be vacuous — `drive.mjs` reads
`#root.innerText`, which never contains an input's value — and a storage assertion would invent a
promise nothing merged makes. The epic names its own method, *"asserted structurally rather than by
inspection"*, and this pass takes it at its word instead of dressing a green in a browser.

### What a provisional suite does not claim

`ADR-0090` §5, restated so the next reader does not have to fetch it: a merged decision proves what
was **decided**, not what **shipped**. Nothing here shows that a screen exists, that a control is
reachable or that a literal has not moved. The first round over these suites is expected to correct
cases, those corrections are **harness** tickets against `EPIC-12`, they are excluded from `B(N)`,
and **no production code may be changed to make one of these cases pass** until it has reproduced by
hand (`ADR-0089` §4).

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-120401](../tasks/TASK-120401-the-epic-04-identity-suite.md) | Write the `EPIC-04` identity suite into `docs/test-plan.md` | ready |
| [TASK-120402](../tasks/TASK-120402-the-epic-05-ladder-suite.md) | Write the `EPIC-05` ladder suite into `docs/test-plan.md` | backlog |

**Two tickets, sequenced, because both edit one file.** A batch that started them together would
conflict on `docs/test-plan.md` by construction. Splitting by epic also gives each reviewer one
Definition of done to check the rows against, which is the whole of the review instruction.

## Acceptance criteria

- [ ] `docs/test-plan.md` carries a `## EPIC-04 —` suite of exactly five rows and a `## EPIC-05 —`
      suite of exactly five rows, every row five columns wide with a non-empty `source`.
- [ ] Each suite carries `ADR-0090` §5's `Provisional` line byte-for-byte, inside its own section.
- [ ] §*Not yet written*'s `EPIC-04` and `EPIC-05` rows no longer read *not written*.
- [ ] §*What this catalogue does not cover* names both epics and the eleven promises neither suite
      reaches, with a reason for each.
- [ ] `SMOKE` and `CORE` are byte-unchanged: 26 rows, four columns, not retrofitted
      (`ADR-0090` §4).
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0.

## Out of scope

- **Running a QA round.** `ADR-0090` §3 gives this pass no stack, no browser and no cycle by any
  route. Its terminal act is a report naming the command the human types next.
- **`EPIC-06`'s suite.** `docs/test-plan.md` already says it is *"mostly not testable this way"* and
  `qa` is instructed not to report styling. Its row stays in §*Not yet written*, and the pass that
  writes it — if one ever should — is its own command.
- **Retrofitting `SMOKE` and `CORE` with a `source` column.** `ADR-0090` §4 refuses that churn by
  name and prices the resulting mixed-width catalogue as a consequence.
- **Any `verify:` command that runs a QA case, brings a stack up, or cites a round.** `ADR-0089`
  §§2b and 2c. Every gate in this story is static and reads a committed file.
- **Ticking any `EPIC-12` Definition-of-done box.** None of them asks for a suite; the one about
  telling a harness defect from a product defect still needs a round to run.
- **Repairing anything either suite predicts will fail.** A provisional case is a claim about the
  catalogue until a round has run it (`ADR-0090` §5).
