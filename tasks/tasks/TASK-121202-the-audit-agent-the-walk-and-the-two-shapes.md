---
schema: 2
id: TASK-121202
title: The audit agent — the walk, the hands and the two shapes
type: task
status: done
parent: STORY-1212
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, audit, meta]
depends_on: [TASK-121201]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk 'NR<=12 && NR==1 && $0 ~ /^-+$/{a=1} NR<=12 && $1=="name:" && $2=="audit"{b=1} NR<=12 && $1=="description:"{c=1} NR<=12 && $1=="tools:"{e=1; if (index($0,"Write") || index($0,"Edit")) d=1} END{exit (a&&b&&c&&e&&!d)?0:1}' .claude/agents/audit.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | awk '{n++; f[$0]=1} END{exit (n==5 && f[".claude/agents/qa.md"] && f[".claude/agents/uat.md"] && f[".claude/agents/audit.md"] && f[".claude/skills/qa-cases/SKILL.md"] && f[".claude/skills/qa-cycle/SKILL.md"])?0:1}'
  - grep -rl "qa-cycle" .claude/skills .claude/agents | grep -Ev '^\.claude/(agents/(qa|uat|audit)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' | awk 'END{exit (NR==0)?0:1}'
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - shasum -a 256 .claude/agents/uat.md | awk '{exit ($1=="d6c1cd3f619356ca3f0a9f4af4ad9441854818c1d67913efd271c5d378664cce")?0:1}'
  - awk 'index($0,"stack.sh status"){a=1} index($0,"ADR-0096"){b=1} index($0,"size 390 664"){c=1} index($0,"720 900"){d=1} index($0,"all-in"){e=1} END{exit (a&&b&&c&&d&&e)?0:1}' .claude/agents/audit.md
  - awk 'index($0,"blocker"){bad=1} index($0,"medium"){bad=1} END{exit bad?1:0}' .claude/agents/audit.md
  - awk 'END{exit (NR<=85)?0:1}' .claude/agents/audit.md
---

## Goal

`.claude/agents/audit.md` exists: a dispatchable `audit` agent with no `Write` and no `Edit`, which
knows that `ADR-0096` §1 is its walk, that the cycle owns the stack, which verbs are its hands, and
how one live tab is measured at two shapes.

## This ticket writes half a file, deliberately

`ADR-0096` and `ADR-0097` together give the audit observer everything `uat.md`'s 147 lines carry
plus a resize discipline, so the file lands in two tickets: **this one** through
`## Two shapes, one live tab`, and
[`TASK-121203`](TASK-121203-what-audit-answers-and-the-three-it-may-propose.md) — the rubric, what
a `not met` costs, the three it may propose and the report — immediately after.

**The intermediate state is inert**, which is what makes the seam safe: nothing dispatches `audit`
until `TASK-121204` adds the focus to `qa-cycle`'s `SKILL.md`, two tickets later. This is the same
seam `TASK-120705` used, and not `STORY-1203`'s case, where splitting would have merged a
human-invocable skill without the prohibitions licensing it.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/audit.md` | create |

You may **read** `.claude/agents/uat.md` — the sibling whose shape this follows, and which you may
**not** modify — `docs/adr/ADR-0096-…` §§1, 4, and `docs/adr/ADR-0097-…` §§1, 2, 3.

## Scope

**85 lines at most**, gated. A 76-line draft carrying every clause below was written and measured
on 2026-08-31, so the budget is real but not generous; if it feels tight, the file is transcribing
something `ADR-0096` already holds.

- **Frontmatter**: `name: audit` on its own line; a `description:` saying it walks a whole duel
  beat by beat, answers every criterion of the frozen rubric at every beat and at both shapes, and
  files nothing; `model: sonnet`; and `tools: Read, Bash, Grep` — **no `Write`, no `Edit`**. Gate 2
  reads all four and refuses either write tool by name.
- **The role, in three or four lines**: `qa` asks *does it work?*; `uat` asks *does it look like
  the thing that was decided?*; you ask whether the duel was any good to play — but only through a
  closed list and never in your own words. No `Write`, no `Edit`; you fix nothing and file nothing;
  `qa-manager` decides what it means.
- **`## Your walk, and your budget`** — **`ADR-0096` §1 is the walk and §2 is the list**, and those
  two sections are the entire context budget: read them and nothing else, and **never copy a beat
  or a criterion into another file**. Say that all eight beats are walked every round, that **both
  browsers are observed at every beat**, and that beat 5 is a hand that goes **all-in** and runs
  the board out — reachable with a player's hands alone, so nothing is seeded (`ADR-0089` §3). Say
  that the scope word goes in `SCOPE:` and **narrows nothing**, because a round ends when every
  criterion has been answered at every beat (`ADR-0096` §5).
- **`## The stack is already up when you start`** — the `qa-cycle` skill owns the lifecycle; run
  `scripts/qa/stack.sh status`, require `up` on all three of `db`, `server` and `web`, and report
  `STACK: down` with that output otherwise. Name the four denied verbs (`kill`, `pkill`, `killall`,
  `rm`). **This is the sentence that names `qa-cycle`**, and it is the one mention `ADR-0097` §4
  licenses in this file: mention-only, never an invocation.
- **`## Driving the browsers`** — `scripts/qa/drive.mjs` on ports 9232 and 9233; you run it and do
  not rewrite it; a check the verbs cannot express is `BLOCKED` and a missing verb is a finding
  about the harness. Then:
  - **`record` then `frames`** — arm `record` **before** the action and read `frames` after it; a
    frame that lives less than one 250 ms poll is invisible to `wait` and `absent` at any interval.
    This is the evidence `R1` is answered with.
  - **`shot <path>`** into the round's temp directory. A screenshot is **read by a reader, never
    diffed by a program**; no image-comparison tool enters this repository; screenshots are
    **never committed**; the durable evidence is text, quoted verbatim.
  - **`ADR-0089` §3 in one paragraph** — act with a player's hands: click, type, navigate, reload,
    clear browser storage, **and resize a window**, which `ADR-0097` §1 adds as the sixth member of
    that list. Read anything. Write no application state; `forget-room` is the single licensed
    storage write.
- **`## Two shapes, one live tab`** — the five rules `ADR-0097` §§2 and 3 fix:
  - each browser's **first** act in a round is `size 390 664`, **before** `open`;
  - at a beat that re-answers `R2`/`R3`, `size 720 900` on **both** tabs, read both, then return
    **both** to `size 390 664` before the walk continues — resizing one seat confounds the shape
    with the seat;
  - a verb sequence crossing a `close` re-applies `size`, because a fresh tab inherits nothing;
  - `size` prints the viewport it achieved and exits 1 on a mismatch, but **nothing catches a
    resize you forgot**, so the record names where every `size` was issued;
  - **never claim a device** — `mobile: true`, a `deviceScaleFactor` above `0` or a fabricated
    `screen` produce a viewport no player can produce and turn an `R2` failure into a pass; a
    finding built on one is a **harness** defect (`ADR-0089` §4), never a product defect.
  - Add the one cost `ADR-0097` §Consequences names: a resize is a real DOM event, so it pushes
    frames into `window.__pdFrames` that no player action caused.

## Out of scope

- **Everything from the rubric onwards** — the five criteria, `met`/`not met` and its quoted
  observation, *one bar checked twice*, the three proposed criteria, the functional-defect route
  and the report block. That is `TASK-121203`, and a file that carries them here overruns gate 9.
- **Transcribing `ADR-0096` §1's beat table or §2's rubric table.** Cite the sections; a copy is a
  second register that rots, which is the refusal `TASK-120705` carried one focus earlier, and gate
  9's line cap is what makes it structural rather than advisory.
- **Any severity.** `ADR-0096` §5: there is none under this focus. Gate 8 refuses the strings
  `blocker` and `medium` anywhere in the file.
- **Any change to `.claude/agents/qa.md` or `.claude/agents/uat.md`.** `ADR-0096` §2 freezes
  `ADR-0092` §3 byte-unchanged for both focuses, and `ADR-0097` §4's whole case for a fifth file is
  that one file cannot hold two classifiers. Gates 5 and 6 pin both `sha256`s.
- **Any change to `.claude/agents/qa-manager.md`, `.claude/skills/qa-cycle/SKILL.md` or
  `.claude/skills/qa-cases/SKILL.md`.** Later tickets own two of them, and `qa-cases` is finished.
- **A sixth file naming `qa-cycle`.** `ADR-0090` §2 as amended by `ADR-0092` §2 and `ADR-0097` §4
  declares exactly five; gates 3 and 4 hold the set from both directions.
- **Invoking `/qa-cycle`, or telling the agent to.** The licence in this file is to *mention* the
  cycle in the stack sentence and nowhere else. A step that starts one is `ADR-0089` §2b failing.
- **Any `Write` or `Edit` tool, any file-writing instruction, any ticket-filing instruction.**
  `qa-manager` is the only role that writes a bug ticket, and that separation is what stops a
  tester grading its own findings.
- **A third viewport, a landscape shape or a tablet shape.** `ADR-0096` §4's table is complete and
  `ADR-0097` §5 records the human's *"we are ok to support only one orientation for mobile form
  factor."*
- **Naming a case id, a card path or a screen.** The audit's unit is the beat; a screen inventory
  belongs to the `uat` focus and copying one here would make this file rot with `docs/test-plan.md`.

## Tests

No test class — the deliverable is one prose document, so the gates are structural checks over that
text, the shape `TASK-120301` and `TASK-120705` both use. Every row below was run on 2026-08-31 at
commit `f8383c4e`, against the tree as it stands and against the 76-line draft: **five of the nine
are red today — 2, 3, 7, 8 and 9 — and all nine are green with the file.** Gates 4, 5 and 6 are
guards and were 0 both times.

| # | Gate | Proves | Today | With the file |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` over the frontmatter | the file is **dispatchable as `audit`** — a hyphen rule on line 1, `name: audit`, a `description:`, a `tools:` line — and that `tools:` names **neither `Write` nor `Edit`** | **2** — no such file | 0 |
| 3 | `grep -rl` set equality | exactly **five** files under `.claude/` name `qa-cycle`, and they are the five `ADR-0097` §4 declares | **1** — there are four | 0 |
| 4 | `ADR-0097` §4's own command, verbatim | no **sixth** file names the cycle | 0 — a guard, not a progress gate | 0 |
| 5 | `sha256` of `qa.md` | `.claude/agents/qa.md` is byte-identical to `f8383c4e` | 0 — a guard | 0 |
| 6 | `sha256` of `uat.md` | `.claude/agents/uat.md` is byte-identical to `f8383c4e` — `ADR-0096` §2 freezes the UAT classifier | 0 — a guard | 0 |
| 7 | `awk` over five literals | the file names the stack check, `ADR-0096` as its budget, both shapes, and the all-in beat | **2** | 0 |
| 8 | `awk` over two forbidden literals | **no severity** — `blocker` and `medium` appear nowhere (`ADR-0096` §5) | **2** — no such file | 0 |
| 9 | `awk 'END{exit (NR<=85)?0:1}'` | the file is this ticket's half, and is too short to have transcribed either merged table | **2** | 0 |

**Gate 2 is the one that cannot be satisfied by prose.** A `tools:` line containing `Write` fails
it, and that is the structural half of *"you file nothing"*. Run against
`.claude/agents/qa-manager.md` — a real agent file with `Write` in its tools — the same gate exits
**1**; run against a stub whose `tools:` reads `Read, Write, Bash, Grep` it also exits **1**. Both
measured on 2026-08-31.

**Gate 3 was measured in all three worlds**, by feeding the awk the file list rather than by
creating files: it exits **1** at today's four, **0** at five with `agents/audit.md` added, and
**1** at six. Gate 4 exits **0** at four and five and **1** at six, under both the shim `grep` an
agent shell resolves and `/usr/bin/grep`.

**Gate 8 is the only gate here that a later editor has to *delete* rather than merely fail to
add**, and it is the one worth the most. Every other string gate is worth exactly what
`TASK-120301` said its kind is worth: it puts a sentence into the file, and it cannot see that the
agent obeys any of it. Whether a round actually resizes both tabs and restores them is the
reviewer's, then the round record's — `ADR-0097` §Consequences says plainly that a *forgotten*
restore has no catch at all.

**Gates 5 and 6 are golden values and must stay literal.** If either goes red, the answer is to
revert that observer, never to update the hash.

## Acceptance criteria

- [ ] `.claude/agents/audit.md` exists with `name: audit`, a `description:`, and a `tools:` line
      containing neither `Write` nor `Edit` (gate 2).
- [ ] Exactly five files under `.claude/skills` and `.claude/agents` name `qa-cycle`, and they are
      `agents/qa.md`, `agents/uat.md`, `agents/audit.md`, `skills/qa-cases/SKILL.md`,
      `skills/qa-cycle/SKILL.md` (gates 3 and 4).
- [ ] `.claude/agents/qa.md` has `sha256 eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42`
      and `.claude/agents/uat.md` has
      `sha256 d6c1cd3f619356ca3f0a9f4af4ad9441854818c1d67913efd271c5d378664cce` (gates 5 and 6).
- [ ] The file names `scripts/qa/stack.sh status`, `ADR-0096`, `size 390 664`, `720 900` and
      `all-in` (gate 7).
- [ ] The file contains neither `blocker` nor `medium` (gate 8).
- [ ] The file is **85 lines or fewer** (gate 9).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
