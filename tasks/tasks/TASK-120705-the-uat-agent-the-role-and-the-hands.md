---
schema: 2
id: TASK-120705
title: The uat agent — the role, the refusals and the hands
type: task
status: done
parent: STORY-1207
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, uat, meta]
depends_on: [TASK-120704]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk 'NR<=12 && NR==1 && $0 ~ /^-+$/{a=1} NR<=12 && $1=="name:" && $2=="uat"{b=1} NR<=12 && $1=="description:"{c=1} NR<=12 && $1=="tools:"{e=1; if (index($0,"Write") || index($0,"Edit")) d=1} END{exit (a&&b&&c&&e&&!d)?0:1}' .claude/agents/uat.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | awk '{n++; f[$0]=1} END{exit (n==4 && f[".claude/agents/qa.md"] && f[".claude/agents/uat.md"] && f[".claude/skills/qa-cases/SKILL.md"] && f[".claude/skills/qa-cycle/SKILL.md"])?0:1}'
  - grep -rl "qa-cycle" .claude/skills .claude/agents | grep -Ev '^\.claude/(agents/(qa|uat)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' | awk 'END{exit (NR==0)?0:1}'
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - awk 'index($0,"stack.sh status"){b=1} index($0,"`shot"){c=1} index($0,"forget-room"){d=1} index($0,"test-plan.md"){e=1} END{exit (b&&c&&d&&e)?0:1}' .claude/agents/uat.md
  - awk 'index($0,"never diffed by a program"){a=1} index($0,"never committed"){b=1} END{exit (a&&b)?0:1}' .claude/agents/uat.md
  - awk 'END{exit (NR<=70)?0:1}' .claude/agents/uat.md
---

## Goal

`.claude/agents/uat.md` exists: a dispatchable `uat` agent with no `Write` and no `Edit`, which
knows its scope, that the cycle owns the stack, and which verbs are its hands.

## This ticket writes half a file, deliberately

`ADR-0092` §8 gives `uat` *"`qa`'s report shape plus the `QUESTIONS` section"*, and `qa.md` is 148
lines. That is over an `S`, so the file lands in two tickets: **this one** through
`## Driving the browsers`, and [`TASK-120706`](TASK-120706-what-uat-may-file-and-what-it-may-only-ask.md)
— the three checks, the merged-source classifier, severity and the report — immediately after.

**The intermediate state is inert**, which is what makes the seam safe: nothing dispatches `uat`
until `TASK-120707` adds the focus to `qa-cycle`'s `SKILL.md`, two tickets later. This is not
`STORY-1203`'s case, where splitting would have merged a *human-invocable* skill without the
prohibitions licensing it.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/uat.md` | create |

You may **read** `.claude/agents/qa.md` — the sibling whose shape this follows, and which you may
**not** modify — `docs/adr/ADR-0092-…` §§2, 3, 8, and `docs/test-plan.md` §*UAT*.

## Scope

**70 lines at most**, gated. A 61-line draft carrying every clause below was written and measured
on 2026-08-30, so the budget is not tight; if it feels tight, the file is repeating something
`ADR-0092` or `qa.md` already holds.

- **Frontmatter**: `name: uat` on its own line; a `description:` saying it walks the screens under
  the UAT focus, reports what contradicts a merged source, asks the rest, and files nothing;
  `model: sonnet`; and `tools: Read, Bash, Grep` — **no `Write`, no `Edit`**. Gate 2 reads all
  four and refuses either write tool by name.
- **The role, in three or four lines**: `qa` asks *does it work?*; you ask *does it look like the
  thing that was decided?* You have no `Write` and no `Edit`, you fix nothing and file nothing,
  `qa-manager` decides what it means and the `product-owner` answers what you ask.
- **`## Your scope`** — the same three scopes `qa` takes (`epic <ID>`, `smoke`, `regression`),
  read as *which screens the inventory says that scope reaches*. **`docs/test-plan.md` §*UAT* is
  the context budget**: its inventory names the screen-states, the card each is judged against and
  the case ids whose `do` columns are the routes. Read that section and the case rows it names and
  nothing else. Say that the `expect` and `fails if` columns stay functional and are never graded
  on UX (`ADR-0092` §7).
- **`## The stack is already up when you start`** — the `qa-cycle` skill owns the lifecycle; run
  `scripts/qa/stack.sh status`, require `up` on all three of `db`, `server` and `web`, and report
  `STACK: down` with that output otherwise. Name the four denied verbs (`kill`, `pkill`, `killall`,
  `rm`). **This is the sentence that names `qa-cycle`**, and it is the one mention `ADR-0092` §2
  licenses in this file: mention-only, never an invocation.
- **`## Driving the browsers`** — `scripts/qa/drive.mjs` on ports 9232 and 9233; you run it and do
  not rewrite it; a check the verbs cannot express is `BLOCKED` and a missing verb is a finding
  about the harness. Then:
  - **`shot <path>`** writes the screen as a PNG into the round's temp directory. **A screenshot
    is read by a reader, never diffed by a program** — no image-comparison tool enters this
    repository — and screenshots are **never committed**. The durable evidence in a finding is
    text: rendered copy, computed styles and geometry read through `eval`, quoted verbatim
    (`ADR-0092` §§2a, 3). Both quoted phrases are what gate 7 matches.
  - **Rendering a card is a read**: opening `file:///…/design/screens/<card>.html` in a harness
    tab reads a repository file with a renderer; the card is not the application.
  - **`ADR-0089` §3 in one paragraph**: act with a player's hands — click, type, navigate, reload,
    clear browser storage; read anything — DOM, `localStorage`, the database, the log; **write no
    application state**; `forget-room` is the single licensed storage write.

## Out of scope

- **Everything from `## Three checks per screen-state` onwards** — the three checks, the
  merged-source classifier, the missing-card rule, severity and the report block. That is
  `TASK-120706`, and a file that carries them here overruns the 70-line gate.
- **Any change to `.claude/agents/qa.md`.** `ADR-0092` §8: byte-unchanged, and gate 5 pins its
  `sha256`. Its refusal list (*"Do not report: wording you dislike, spacing, colour, anything
  `EPIC-06` owns"*) is load-bearing for function rounds and is UAT's entire subject; two files are
  what make the leak impossible in both directions.
- **Any change to `.claude/agents/qa-manager.md`, `.claude/skills/qa-cycle/SKILL.md` or
  `.claude/skills/qa-cases/SKILL.md`.** Later tickets own two of them, and `qa-cases` is finished.
- **A fifth file naming `qa-cycle`.** `ADR-0090` §2 as amended by `ADR-0092` §2 declares exactly
  four; gates 3 and 4 hold the set from both directions.
- **Invoking `/qa-cycle`, or telling the agent to.** The licence in this file is to *mention* the
  cycle in the stack sentence and nowhere else. A step that starts one is `ADR-0089` §2b failing.
- **Any `Write` or `Edit` tool, any file-writing instruction, any ticket-filing instruction.**
  `qa-manager` is the only role that writes a bug ticket, and that separation is what stops a
  tester grading its own findings.
- **Naming a case id, a card path or a screen.** They live in `docs/test-plan.md` §*UAT*; copying
  them into an agent file makes a second register that rots.

## Tests

No test class — the deliverable is one prose document, so the gates are structural checks over
that text, the shape [`TASK-120301`](TASK-120301-the-qa-cases-skill-file.md) uses. Every row was
run on 2026-08-30 at commit `cfcc6a4e`, against the tree as it stands and against a 61-line draft
written to satisfy them: **five of the eight are red today — 2, 3, 6, 7 and 8 — and all eight are
green with the file.** Gates 4 and 5 are guards and were 0 both times.

| # | Gate | Proves | Today | With the file |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` over the frontmatter | the file is **dispatchable as `uat`** — a hyphen rule on line 1, `name: uat`, a `description:`, a `tools:` line — and that `tools:` names **neither `Write` nor `Edit`** | **2** — no such file | 0 |
| 3 | `grep -rl` set equality | exactly **four** files under `.claude/` name `qa-cycle`, and they are the four `ADR-0092` §2 declares | **1** — there are three | 0 |
| 4 | `ADR-0092` §2's own command, verbatim | no **fifth** file names the cycle | 0 — a guard, not a progress gate | 0 |
| 5 | `sha256` of `qa.md` | `.claude/agents/qa.md` is byte-identical to `cfcc6a4e` | 0 — a guard | 0 |
| 6 | `awk` over four literals | the file names the stack check, the `shot` verb, `forget-room` and its catalogue budget | **2** | 0 |
| 7 | `awk` over two literals | *never diffed by a program* and *never committed* are written down | **2** | 0 |
| 8 | `awk 'END{exit (NR<=70)?0:1}'` | the file is this ticket's half, not the whole brief | **2** | 0 |

**Gate 2 is the one that cannot be satisfied by prose.** A `tools:` line containing `Write` fails
it, and that is the structural half of *"you file nothing"*. Run against
`.claude/agents/qa-manager.md` — a real agent file with `Write` in its tools — the same gate
exits **1**; run against `.claude/agents/qa.md` — right tools, wrong name — it also exits **1**.
Both measured.

**Gates 6 and 7 are string checks and are worth exactly what `TASK-120301` said they are worth**:
they put a sentence into the file in words a later editor has to **delete** rather than merely
fail to add. They cannot see that the agent obeys any of it, and no gate can — the `shot` verb's
existence is `TASK-120702`'s gate 3, and whether a run ever diffs an image is the reviewer's, then
the round record's.

**Gate 5 is a golden value and must stay literal.** `eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42`
is `sha256(.claude/agents/qa.md)` at `cfcc6a4e`, measured 2026-08-30. If it goes red, the answer
is to revert `qa.md`, never to update the hash.

## Acceptance criteria

- [ ] `.claude/agents/uat.md` exists with `name: uat`, a `description:`, and a `tools:` line
      containing neither `Write` nor `Edit` (gate 2).
- [ ] Exactly four files under `.claude/skills` and `.claude/agents` name `qa-cycle`, and they are
      `agents/qa.md`, `agents/uat.md`, `skills/qa-cases/SKILL.md`, `skills/qa-cycle/SKILL.md`
      (gates 3 and 4).
- [ ] `.claude/agents/qa.md` has `sha256 eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42`
      (gate 5).
- [ ] The file names `scripts/qa/stack.sh status`, the `shot` verb, `forget-room`, and
      `docs/test-plan.md` as its budget (gate 6).
- [ ] The file says a screenshot is *never diffed by a program* and *never committed* (gate 7).
- [ ] The file is **70 lines or fewer** (gate 8).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
