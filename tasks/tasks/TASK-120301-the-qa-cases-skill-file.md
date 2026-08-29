---
schema: 2
id: TASK-120301
title: Create the qa-cases skill, whose terminal act is a printed command
type: task
status: ready
parent: STORY-1203
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, meta]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk 'NR==1 && $0 ~ /^-+$/{a=1} $1=="name:" && $2=="qa-cases"{b=1} $1=="description:"{c=1} END{exit (a&&b&&c)?0:1}' .claude/skills/qa-cases/SKILL.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | grep -Ev '^\.claude/(agents/qa\.md|skills/qa-(cycle|cases)/SKILL\.md)$' | awk 'END{exit (NR==0)?0:1}'
  - grep -rl "qa-cycle" .claude/skills .claude/agents | awk '{n++; f[$0]=1} END{exit (n==3 && f[".claude/agents/qa.md"] && f[".claude/skills/qa-cases/SKILL.md"] && f[".claude/skills/qa-cycle/SKILL.md"])?0:1}'
  - awk '/^## What it may not do$/{s=1;next} /^## /{s=0} s{l=tolower($0)} s&&index(l,"bring the stack up"){a=1} s&&index(l,"start a browser"){b=1} s&&index(l,"dispatch")&&index(l,"qa-manager"){c=1} s&&index(l,"invoke")&&index(l,"qa-cycle"){d=1} END{exit (a&&b&&c&&d)?0:1}' .claude/skills/qa-cases/SKILL.md
  - awk 'index($0,"`source`"){a=1} index($0,"no merged source is not written"){b=1} index($0,"product owner"){c=1} END{exit (a&&b&&c)?0:1}' .claude/skills/qa-cases/SKILL.md
  - awk 'index($0,"**Provisional** — authored") && index($0,"not yet run (`ADR-0090` §5)"){f=1} END{exit f?0:1}' .claude/skills/qa-cases/SKILL.md
  - awk 'index($0,"/qa-cycle epic "){a=1} index($0,"does not run it"){b=1} END{exit (a&&b)?0:1}' .claude/skills/qa-cases/SKILL.md
  - awk 'END{exit (NR<=120)?0:1}' .claude/skills/qa-cases/SKILL.md
---

## Goal

`/qa-cases EPIC-04 EPIC-05` is an invocable skill that plans and lands the catalogue suites for
those epics through ordinary reviewed PRs and then stops, printing the `/qa-cycle` command the
human types next.

## The specification is `ADR-0090`, and it is binding

[`ADR-0090`](../../docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) §3
fixes this skill's shape, §4 fixes how a case is written, §5 fixes what an authored suite carries.
Read those three sections before writing a word. **Do not invent a step that is not in them**, and
in particular do not add a helpful last step that runs the cycle — that is the single failure this
ticket exists to prevent, it is `ADR-0089` §2b failing, and `ADR-0090` §Consequences forecloses even
the conditional forms by name (*"a skill that runs the cycle conditionally, or only when the human
is watching, or only for smoke, is condition **b** failing, not a refinement of it"*).

## Files

| File | Action |
| --- | --- |
| `.claude/skills/qa-cases/SKILL.md` | create |
| `docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md` | read — §§2, 3, 4, 5 |
| `docs/test-plan.md` | read — §*How a case is written*, §*Per-epic suites* |
| `.claude/skills/qa-cycle/SKILL.md` | read — the sibling skill and the format precedent |
| `.claude/skills/plan-story/SKILL.md` | read — a short skill's shape |

## Scope

- **Create `.claude/skills/qa-cases/SKILL.md`**, with skill frontmatter: `name: qa-cases` on its
  own line, and a `description:` that says it authors catalogue cases from merged sources, runs no
  browser, and never starts a cycle.
- **A section headed exactly `## What it may do`**, carrying `ADR-0090` §3's four permissions:
  read the epics, the ADRs, `docs/duel-rules.md`, `docs/vision.md` and the client's own literals;
  plan a story and its tickets; run them through `build-epic`; update `docs/test-plan.md` including
  its §*Not yet written* table.
- **A section headed exactly `## What it may not do`** — the heading string is what gate 5 keys on —
  carrying §3's four prohibitions, one per bullet. Each bullet must contain one of these phrases,
  which are what gate 5 matches, case-insensitively, **inside that section only**: *bring the stack
  up*; *start a browser*; *dispatch* together with *qa-manager*; *invoke* together with *qa-cycle*.
- **§4's sourcing rule**: every case carries a fifth `source` column — the module holding the
  literal for player-facing text, otherwise an ADR section or a `docs/duel-rules.md` heading — and
  the sentence that a case whose expectation **has no merged source is not written**, becoming a
  `DEC-NNN` in `docs/adr/README.md` for the **product owner** while the case waits.
- **§5's marker, verbatim**, as the line an authored suite carries and the first round record
  deletes — copy it byte-for-byte from `ADR-0090` §5 or `docs/test-plan.md`, em dash and all:

  > **Provisional** — authored YYYY-MM-DD from merged sources, not yet run (`ADR-0090` §5).

- **A terminal report** that ends with the command the human types next, with the scope filled in —
  `/qa-cycle epic EPIC-04` — and the sentence **`It prints that line; it does not run it.`**
- **120 lines at most**, gated. 64 was enough for a draft carrying every clause above, so the
  budget is not tight; if it feels tight, the file has grown a procedure `ADR-0090` did not ask for.

## Out of scope

- **Every file except `.claude/skills/qa-cases/SKILL.md`.** `docs/test-plan.md` and
  `.claude/skills/qa-cycle/SKILL.md` were amended by `ADR-0090`'s own PR (`8dfebbc1`) and are
  already correct; re-editing them is churn against a merged file. A `## Files` table row naming
  anything else is grounds to reject the diff on sight.
- **`.claude/agents/qa-manager.md`.** `ADR-0090` §2: it names the cycle **nowhere today and gains
  no licence to**. A mention added here fails this ticket's own third gate.
- **Writing a catalogue case, or touching `docs/test-plan.md`'s tables.** This ticket builds the
  tool. The first authoring pass is the human typing `/qa-cases` afterwards.
- **Any notification, cron or heartbeat wiring.** `qa-cycle` reports itself because a cycle is long
  and silent; `qa-cases` delegates its long half to `build-epic`, which already reports. `ADR-0090`
  §3 does not license it and a cron near this skill is the shape §1 forbids.
- **A `verify:` command that runs a QA case, or cites a round.** `ADR-0089` §2b and §2c. Every gate
  here reads a committed file.

## Tests

No test class — the deliverable is one prose document, so the gates are structural checks over that
text, the shape [`TASK-120201`](TASK-120201-smk-03-reads-a-device-id-from-a-profile-that-has-been-to-the-app.md)
uses. Every row was run on 2026-08-29 at commit `8dfebbc1`, against the tree as it stands and
against a 64-line draft written to satisfy them: **nine red, then nine green.** The draft is what
makes the 120-line budget in *Scope* a measurement rather than a guess.

| # | Gate | Proves | Today | With the file |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | the ticket, story, epic row and board rows agree | exits 0 — and must keep doing so once this ticket's status and board cell move | 0 |
| 2 | `awk` over the frontmatter | the file is **invocable as `/qa-cases`** — a hyphen rule on line 1, a `name:` field whose value is `qa-cases`, and a `description:` field. Nothing else here would catch a `SKILL.md` that merged without frontmatter and is therefore not a command | **exits 2** — no such file | 0 |
| 3 | `ADR-0090` §2's command, verbatim on one line | no **fourth** file under `.claude/skills` or `.claude/agents` names `qa-cycle` | exits 0 with the two files there are — a guard, not a progress gate | 0 |
| 4 | `grep -rl … \| awk` set check | the set naming the cycle is **exactly** the three the ADR declares, `qa-cases` among them | **exits 1** — there are two | 0 |
| 5 | `awk` scoped to `## What it may not do` | all four §3 prohibitions are written **inside that section** | **exits 2** | 0 |
| 6 | `awk` over `` `source` ``, *no merged source is not written*, *product owner* | §4's rule and its routing are in the file | **exits 2** | 0 |
| 7 | `awk` over the `Provisional` literal | §5's marker is present **byte-for-byte**, em dash, backticks and `§5` included | **exits 2** | 0 |
| 8 | `awk` over `/qa-cycle epic ` and *does not run it* | the terminal report names the next command with a scope, and says it is not run | **exits 2** | 0 |
| 9 | `awk 'END{exit (NR<=120)?0:1}'` | the file is an `S`, not a second `qa-cycle` | **exits 2** | 0 |

**Gate 2 compares awk fields rather than matching `/^name: qa-cases$/`**, and the first draft of it
did the latter. Two reasons it does not: an anchored regex is the shape the shim `grep` and BSD grep
are known to disagree about, and — measured here — a `"---"` string literal inside a `verify:`
command puts a `---` **inside this ticket's own frontmatter**, which broke a naive frontmatter split
on the first try. The repository's linter survives it (it looks for `\n---\n`), but a gate that
booby-traps the file carrying it is not worth three characters. Run against
`.claude/skills/qa-cycle/SKILL.md` — a real skill file with the wrong `name:` — gate 2 exits **1**.

**Gate 3 and gate 4 were measured under both greps.** The bare `grep` in an agent shell is a
function shimming `ugrep`; `/usr/bin/grep` is BSD grep. Both pipelines were run under each on
2026-08-29 and agreed exactly: **0** with the three declared files present, **1** with a fourth
(`.claude/skills/epic-and-qa/SKILL.md`, the evasion `ADR-0090` §2 names). Gate 4's `awk` set test
was written rather than a second `grep -v`, because the shim and BSD grep disagree about `-v -q`
over a piped multi-line stream and about an anchor inside a mid-pattern alternation.

### What gate 5 can and cannot see

It can see that the four prohibitions are **written down, in a section of their own**, in words a
later editor would have to *delete* rather than merely fail to add — which is the whole point, since
a skill that omits *"does not bring the stack up"* is a skill whose next reader adds it. Three
mutations were run to check it is not vacuous:

- delete the *bring the stack up* bullet → **exits 1**;
- keep the sentence but move it into `## What it may do` → **exits 1**, so the gate really is
  scoped to the section and not to the file;
- capitalisation: the bullets open with `**Bring the stack up.**`, so the gate lowercases each line
  before matching. A case-sensitive version of gate 5 returned **1** on a correct file, and a naive
  `grep -c "bring the stack up"` over the whole file returns **0** on it. That is the near miss.

It **cannot** see obedience. A file that keeps all four bullets and adds a step running the cycle
three sections later passes gate 5, and passes gate 8 too — mutating *"It prints that line; it does
not run it."* into *"It prints that line, then runs it."* turns gate 8 red, but **adding** a run
step alongside the sentence does not. `ADR-0090` §2 says this in its own words: *"what no grep can
catch is whether one of the three declared files runs the cycle rather than naming it, because
print this command and run this command are the same string."* That half is the reviewer's, and §2
says §3 is a verb list *"so that one reviewer can check it against one file"* — so **the review is
to read the finished file against `ADR-0090` §3's four verbs and four prohibitions, and reject any
step not in them.**

### What gate 7 can and cannot see

It can see that the `Provisional` literal is in the skill file, unparaphrased — mutating the em dash
into a hyphen, and the trailing ADR reference from *§5* down to the bare ADR id, turns it red. Both
were changed in one mutation and the gate exits **1**. That matters because the string is
already merged verbatim in two other files, and a marker whose job is to be **found and deleted** by
a later document is exactly the kind of string that rots into three spellings.

It **cannot** see that any suite's marker is true. It reads a template in a skill file, not a suite.
Whether a given suite is provisional is a per-suite fact, and no standing gate should assert it:
`EPIC-03`'s CORE suite was written while testing and correctly carries no line, and a corrected
suite correctly loses one. Gate the marker per authoring ticket and per round record, never
repository-wide.

## Acceptance criteria

- [ ] `.claude/skills/qa-cases/SKILL.md` exists, with `name: qa-cases` frontmatter and a
      `description:` (gate 2).
- [ ] Exactly three files under `.claude/skills` and `.claude/agents` name `qa-cycle`, and they are
      `agents/qa.md`, `skills/qa-cycle/SKILL.md` and `skills/qa-cases/SKILL.md` (gates 3 and 4).
- [ ] A `## What it may not do` section contains all four of `ADR-0090` §3's prohibitions —
      the stack, a browser, dispatching `qa`/`qa-manager`, invoking `/qa-cycle` (gate 5).
- [ ] The file states §4's rule — a `source` column, and a case with no merged source is not
      written but becomes a `DEC` for the product owner (gate 6).
- [ ] The file carries §5's `Provisional` line verbatim (gate 7).
- [ ] The report ends by naming `/qa-cycle epic <SCOPE>` and saying it does not run it (gate 8).
- [ ] The file is 120 lines or fewer (gate 9).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
