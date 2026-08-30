---
schema: 2
id: TASK-120706
title: What uat may file, and what it may only ask
type: task
status: backlog
parent: STORY-1207
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, uat, meta]
depends_on: [TASK-120705]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk '/^## /{s=0} /^## Three checks per screen-state$/{s=1} s{l=tolower($0)} s && index(l,"conformance"){a=1} s && index(l,"reachability"){b=1} s && index(l,"copy"){c=1} END{exit (a&&b&&c)?0:1}' .claude/agents/uat.md
  - awk 'index($0,"only when the observation contradicts something merged"){a=1} index($0,"BLOCKED — no card"){b=1} index($0,"observable by a human looking"){c=1} END{exit (a&&b&&c)?0:1}' .claude/agents/uat.md
  - awk '/^PER-SCREEN:/{p=1} /^FINDINGS:/{f=1} /^QUESTIONS:/{q=1} /^BLOCKED:/{b=1} END{exit (p&&f&&q&&b)?0:1}' .claude/agents/uat.md
  - awk 'index($0,"At most three questions per screen"){a=1} index($0,"never grade"){b=1} END{exit (a&&b)?0:1}' .claude/agents/uat.md
  - awk 'NR<=12 && NR==1 && $0 ~ /^-+$/{a=1} NR<=12 && $1=="name:" && $2=="uat"{b=1} NR<=12 && $1=="description:"{c=1} NR<=12 && $1=="tools:"{e=1; if (index($0,"Write") || index($0,"Edit")) d=1} END{exit (a&&b&&c&&e&&!d)?0:1}' .claude/agents/uat.md
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - grep -rl "qa-cycle" .claude/skills .claude/agents | awk '{n++; f[$0]=1} END{exit (n==4 && f[".claude/agents/qa.md"] && f[".claude/agents/uat.md"] && f[".claude/skills/qa-cases/SKILL.md"] && f[".claude/skills/qa-cycle/SKILL.md"])?0:1}'
  - awk 'END{exit (NR<=150)?0:1}' .claude/agents/uat.md
---

## Goal

`.claude/agents/uat.md` says what its three checks are, what separates a finding from a question,
and in exactly what shape it reports both — so `qa-manager` can parse it and so no observation
without a merged source behind it can be filed as a defect.

## The specification is `ADR-0092` §§2, 3, 4, 5, and it is binding

Read those four sections before writing a word. This ticket appends to the file
[`TASK-120705`](TASK-120705-the-uat-agent-the-role-and-the-hands.md) created; it changes nothing
already in it.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/uat.md` | modify |

You may **read** `docs/adr/ADR-0092-…` §§2, 3, 4, 5, `.claude/agents/qa.md` (the report shape this
extends — **not** modifiable), and `docs/test-plan.md` §*UAT*.

## Scope

**150 lines at most for the whole file**, gated — this ticket adds about 80 to the 61 the last one
left. A 141-line draft carrying every clause below was written and measured on 2026-08-30.

### 1. `## Three checks per screen-state` — the heading string gate 2 keys on

`ADR-0092` §3, one bullet each, the words **Conformance**, **Reachability** and **Copy** opening
them:

- **a. Conformance** — the shipped screen against its merged card. **Not pixel equality**: the
  client is responsive and a card is a fixed-width preview artefact, so pixel identity is false-red
  by construction. What is checked is the card's structure present, its vocabulary (tokens,
  components) used, its copy verbatim, its states rendered. Checking a transcription is
  conformance, not taste (`ADR-0091` §1).
- **b. Reachability** — every control the screen offers is visible and operable by some route a
  player has. `drive.mjs` already reports a control that exists but cannot be seen —
  *"found N match(es) …, all invisible"* — so the observation is mechanical.
- **c. Copy** — player-facing text contradicting the module that owns it, a merged ADR, or a
  `docs/vision.md` sentence.

### 2. The merged-source line, and what it fences off

- **File a finding only when the observation contradicts something merged** — a card,
  `design/tokens/tokens.css`, an owned literal, an ADR section, a `docs/duel-rules.md` heading, a
  `docs/vision.md` sentence. Write that clause containing the exact phrase
  `only when the observation contradicts something merged`, which gate 3 matches.
- **Cite the source in the finding.** A conformance finding names the card file it judged against;
  a copy finding names the owning module (`ADR-0089` §5, `ADR-0092` §2).
- **An observation with no merged source to contradict is a question**, not a finding — *this
  could be clearer*, *the emphasis feels wrong* — and `QUESTIONS` is its only route.
- **A finding must be `observable by a human looking`** (gate 3's third literal): at the screen,
  and where a card is cited, at the screen and the rendered card side by side, by eye and never by
  pixel count. Name the three harness defects `ADR-0092` §2 names — a clipped headless capture
  (widths under ~500 px clip rather than overflow), a stale card path, a geometry read taken
  mid-transition — and say they are **harness** defects, filed against `EPIC-12`, never repaired
  in production code.

### 3. A screen with no card

One finding, severity `high`, naming the card path that does not exist. Then **walk the screen
anyway**: checks **b** and **c** have sources independent of any card, so their findings are
reported normally, and only check **a** reads `BLOCKED — no card` — the literal gate 3 matches,
em dash included.

### 4. `## Severity` — a first opinion `qa-manager` may overrule

Four rows, in `qa.md`'s shape but judged on this focus's subject: `blocker` a screen that cannot
be used for its purpose; `high` a merged card's structure or copy is not what shipped, **and a
screen with no card**; `medium` a divergence with a way round it; `low` one a player is unlikely
to notice.

### 5. `## Report` — the shape `qa-manager` parses

One fenced block. It is `qa`'s shape plus two additions, and the four field names below must each
start a line inside it, because gate 4 anchors on them:

- `SCOPE:` / `FOCUS: uat`, `STACK:`, `COMMIT:`, `SCREENS: <walked>/<in scope>`
- **`PER-SCREEN:`** — one entry per screen-state, carrying checks `A`, `B` and `C`, each of which
  is `judged`, `BLOCKED — no card`, or `out of scope`. Those three values are `ADR-0092` §6's, and
  they are what the skill's qualified verdict line counts.
- **`FINDINGS:`** — `SCREEN`, `SEVERITY`, `CHECK` (a/b/c), `SOURCE` (the merged thing it
  contradicts), `WHAT`, `STEPS`, `EVIDENCE` quoted verbatim.
- **`QUESTIONS:`** — `SCREEN` and `QUESTION`.
- **`BLOCKED:`** — a screen-state that could not be walked, and why.

Then, under the block: **`At most three questions per screen`** — the literal gate 5 matches — the
sharpest you have, each a concrete choice answerable in one sentence (*"should the pot be the most
prominent number on the table screen?"*, never *"does this feel right?"*), and the sentence that
you ask, you never answer, and you **never grade** your own question as a finding.

## Out of scope

- **Anything the agent might do with an answer.** `qa-manager` is the only promoter and the
  `product-owner` is the only answerer (`ADR-0092` §5). This file's last word on a question is
  asking it.
- **A fifth report field, a severity of its own invention, or a fourth check.** `ADR-0092` §3 says
  three checks; §6 says three per-screen cell values.
- **Any change to `.claude/agents/qa.md`** — gate 7 pins its `sha256` — or to `qa-manager.md`,
  `qa-cycle/SKILL.md` or `qa-cases/SKILL.md`.
- **Anything already in `.claude/agents/uat.md`**: the frontmatter, the role, the scope table, the
  stack section and the driving section are `TASK-120705`'s and are correct. Gate 6 re-runs its
  frontmatter check, so a `tools:` line that gains `Write` while this half is written is red.
- **Any image-comparison, diff or pixel-threshold instruction.** `ADR-0092` §2a. Conformance is
  *not* pixel equality and saying so is in scope; a tool that measures it is not.
- **Filing, writing, or ticketing anything.** The agent has no `Write`.
- **A `B(N)` rule, an exclusion, a verdict or a stopping rule.** Those are `qa-manager`'s, in
  `TASK-120708` and `TASK-120709`. An observer that computes `B(N)` is an observer grading itself.

## Tests

No test class — one prose document. Every row was run on 2026-08-30 at commit `cfcc6a4e`, against
the tree as it stands and against a 141-line draft.

| # | Gate | Proves | Today | After |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` scoped to `## Three checks per screen-state` | all three checks are written **inside that section**, lowercased before matching so `**a. Conformance**` still hits | **2** — no such file today | 0 |
| 3 | `awk` over three literals | the merged-source line, the `BLOCKED — no card` cell value and *observable by a human looking* are in the file | **2** | 0 |
| 4 | `awk` anchored on four report fields | `PER-SCREEN:`, `FINDINGS:`, `QUESTIONS:` and `BLOCKED:` each start a line — the contract `qa-manager` parses | **2** | 0 |
| 5 | `awk` over the questions cap | *At most three questions per screen* and *never grade* are written | **2** | 0 |
| 6 | frontmatter, `TASK-120705`'s gate | `name: uat` and no `Write`/`Edit` survived this half | **2** | 0 |
| 7 | `sha256` of `qa.md` | `ADR-0092` §8's byte-unchanged clause held | 0 — a guard | 0 |
| 8 | four-file set | no fifth file started naming the cycle | **1** until `TASK-120705` merges | 0 |
| 9 | `awk 'END{exit (NR<=150)?0:1}'` | the whole file is still two `S` tickets' worth | **2** | 0 |

**Gate 4 is the strongest here and it is still only a shape.** It proves the four field names
start lines, which is exactly what `qa-manager` splits on — a report missing `QUESTIONS:` is
unparseable and the gate catches it. It cannot prove that a run fills them honestly, and no gate
can: the merged-source line is judgment, which `ADR-0092` §Consequences prices out loud as *"the
fourth judgment-not-exit-code rule in this structure"*.

**Gates 2, 3 and 5 are string checks, and every literal in them was chosen to be a phrase a later
editor must delete rather than merely fail to add.** `BLOCKED — no card` carries an em dash on
purpose — the same lesson `TASK-120301`'s gate 7 recorded, where mutating an em dash into a hyphen
turned the gate red and was the point of pinning the literal.

**What the review must do, because no gate does it**: read the finished file against `ADR-0092`
§3's three checks and §5's four bullets, and reject any instruction that lets the agent answer a
question, file a ticket, or resolve a disagreement between the catalogue and a merged decision.

## Acceptance criteria

- [ ] `## Three checks per screen-state` names Conformance, Reachability and Copy (gate 2).
- [ ] The file contains `only when the observation contradicts something merged`,
      `BLOCKED — no card` and `observable by a human looking` (gate 3).
- [ ] The report block starts lines with `PER-SCREEN:`, `FINDINGS:`, `QUESTIONS:` and `BLOCKED:`
      (gate 4).
- [ ] The file states `At most three questions per screen` and that the agent never grades its own
      question as a finding (gate 5).
- [ ] The frontmatter still reads `name: uat` with no `Write` and no `Edit` (gate 6).
- [ ] `.claude/agents/qa.md` is still `sha256 eca3f411…4b1b42` (gate 7).
- [ ] Exactly four files under `.claude/` name `qa-cycle` (gate 8).
- [ ] `.claude/agents/uat.md` is **150 lines or fewer** (gate 9).
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
