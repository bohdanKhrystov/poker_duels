---
schema: 2
id: TASK-000107
title: The split procedure carries the design-card rule ADR-0091 placed in it
type: task
status: ready
parent: STORY-0001
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, design, meta]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk '/^## /{s=0} /^## A new screen owes a card$/{s=1} s{l=tolower($0)} s && index(l,"web-client/src/routing/screen.ts"){a=1} s && index(l,"design notes"){b=1} s && index(l,"first ticket is the card"){c=1} s && index(l,"adr-0091"){d=1} END{exit (a&&b&&c&&d)?0:1}' .claude/agents/planner.md
  - awk '/^## /{s=0} /^## A new screen owes a card$/{s=1} s{l=tolower($0)} s && index(l,"name the file that will transcribe"){a=1} s && index(l,"scaffolding is composing, at any level of detail"){b=1} s && index(l,"one transcriber away"){c=1} s && index(l,"shipped code"){d=1} END{exit (a&&b&&c&&d)?0:1}' .claude/agents/planner.md
  - awk '/^## /{s=0} /^## A new screen owes a card$/{s=1} s{l=tolower($0)} s && index(l,"design/tokens/tokens.css"){a=1} s && index(l,"design/components/"){b=1} s && index(l,"design/graphics/"){c=1} s && index(l,"ls design/screens/"){d=1} s && index(l,"adr-0145"){e=1} END{exit (a&&b&&c&&d&&e)?0:1}' .claude/agents/planner.md
  - awk '/^## /{s=0} /^## A new screen owes a card$/{s=1} s{l=tolower($0)} s && index(l,"clause 3 is not"){a=1} s && index(l,"files") && index(l,"table"){b=1} END{exit (a&&b)?0:1}' .claude/agents/planner.md
  - awk '/^## /{a++} /^### /{b++} END{exit (a==6 && b==6)?0:1}' .claude/agents/planner.md
  - grep -rl "routing/screen.ts" .claude/agents .claude/skills | awk '{n++; f=$0} END{exit (n==1 && f==".claude/agents/planner.md")?0:1}'
  - grep -Fq "the failure mode of this project is tickets that grew" .claude/agents/planner.md
  - grep -Fq "A criterion a cheap model has to interpret is a criterion it will get wrong" .claude/agents/planner.md
  - awk 'END{exit (NR<=300)?0:1}' .claude/agents/planner.md
---

## Goal

`.claude/agents/planner.md` carries, in one section, the rule `ADR-0091` §2 says lives in that file
and the classification `ADR-0091` §3 assigns to the planner, as `ADR-0145` §§2–3 sharpened it. After
this merges, a planner splitting a story that puts a new surface in front of a player can answer
*does this story owe a card* and *is this card ticket minting or composing* from the file it already
reads, without opening either ADR.

## The defect, measured

`ADR-0091` §2 states the rule *"lives in **one** place: the planner's split procedure,
`.claude/agents/planner.md`"*. It never went there. **Measured on `develop` at `e5f865e8`**, that
file is **224 lines** with **5** `^## ` headings and **6** `^### ` headings, and it contains
**0** occurrences of `ADR-0091`, **0** of `screen.ts`, and **0** of *design card*; the only lines
matching *design* are *"is well designed"*, *"which of two designs"* and *"two designs to use"*, and
the only line matching *screen* is *"ordered by dependency, not by screen"*. Across
`.claude/agents/` and `.claude/skills/`, **0** files name `routing/screen.ts`. `ADR-0145` §6 states
the same measurement at `702408ed` and assigns this ticket to the planner. Every count in this
ticket and every `Today` cell below was re-measured at `e5f865e8`, not carried from the ADR.

Ten days of splits ran without it, and the epic it surfaced in is exactly where the cost showed:
`ADR-0145` exists because one card ticket's classification was argued at merge instead of decided at
split.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/planner.md` | modify |

You may **read** `docs/adr/ADR-0145-a-cards-scaffolding-is-composing-and-the-transcriber-names-the-mint.md`
§§2, 3, 6 and `docs/adr/ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md` §§2, 3. Those two
sections of `ADR-0091` and those two of `ADR-0145` are the specification; where this ticket and an
ADR disagree, the ADR wins and the disagreement is a finding.

## Scope

**One new top-level section, headed exactly `## A new screen owes a card`, inserted between
`### Ordering` and `## When you are given an epic`.** Roughly 50 lines. No `###` subheadings inside
it — gate 6 counts them. Nothing else in the file changes.

It opens by naming its sources — `ADR-0091` §§2–3 sharpened by `ADR-0145` §§2–3 — and says those
ADRs are the durable record while this file is the working copy, the one place the rule lives.

### 1. The trigger — `ADR-0091` §2

- At split time, a story whose tickets put a **new screen in front of a player** names, in its
  `## Design notes`, the card or cards under `design/screens/` that its screens implement.
- If no such card exists, **the split's first ticket is the card**, and it merges before the ticket
  implementing the screen is startable. Write the literal *first ticket is the card* — gate 2 reads
  it.
- The mechanical floor of *new screen* is **a new member of the `Screen` union in
  `web-client/src/routing/screen.ts`**. Write that path in full; gate 2 and gate 7 both read it.
- A new player-facing surface behind an **existing** slug — a panel, an offer, a profile strip — is
  the same rule applied by the planner's judgment. Say that the floor sees slugs only, which is
  `ADR-0091` §Consequences' own admission.

### 2. Who authors the card — `ADR-0091` §3

Minting (a new token, a new component, new visual language) is worked interactively with the human,
because taste does not survive a verify block. Composing (a screen card assembled from the settled
vocabulary) is an ordinary dispatched ticket: `module: design`, estimate `S` read as one card one
file, `review: light`. Either way the verdict that matters is the human's visual one (`ADR-0024` §3)
and **it may trail the merge** — `ADR-0145` §1 settles that the split governs authorship, never
approval.

### 3. The transcriber test — `ADR-0145` §2

- A card holds two kinds of drawing. **Subject** is what the product will render; a coder
  transcribes it into `web-client/`. **Scaffolding** is what the card draws so the subject reads —
  `.wrap`, `.eyebrow`, `.lede`, `.frame`, `.note`, and stand-ins such as `.stub` and `.screen`.
- The test, applied per drawing at split time: **name the file that will transcribe it.** A client
  file that exists, or one a ticket in the same split will write, means the ticket **mints** that
  drawing. *Nothing; it exists so the subject reads* means the drawing is scaffolding, and
  **scaffolding is composing, at any level of detail.** Write that clause verbatim — gate 3 reads
  `scaffolding is composing, at any level of detail`.
- Say why detail is not the axis: it could only be graded after the drawing exists, which is one
  dispatch too late.
- **Direction**: minting runs card → client, so a ticket copying *from* **shipped code** into a card
  mints nothing. Gate 3 reads `shipped code`.
- **Escalation**: scaffolding stops being scaffolding **one transcriber away** — the moment a client
  file is pointed at a stand-in, it is subject and the next ticket touching it is minting. Gate 3
  reads `one transcriber away`.

### 4. The three-clause floor — `ADR-0145` §3

A card ticket is **minting, without argument**, if any of these is true:

1. It writes `design/tokens/tokens.css`.
2. It creates a file under `design/components/` or `design/graphics/`.
3. Its scaffolding depicts a named product screen that has **no card** under `design/screens/`,
   checked with **`ls design/screens/`**.

Then: above the floor the planner judges by the transcriber test; the floor is **sufficient for
minting and not necessary**, and it misses a genuinely new treatment drawn inside an otherwise
ordinary screen card, which only the transcriber test catches (`ADR-0145` §Consequences).

### 5. What the floor cannot be read from, said out loud

`ADR-0145` §3 says all three clauses are answerable *"from the ticket's `## Files` table plus a
directory listing"*. **Clauses 1 and 2 are. Clause 3 is not** — no Files table says what a drawing
depicts, so clause 3 is read from the scope the planner is writing, and if the ticket does not say
what its stand-ins stand in for, clause 3 is unanswerable and the planner says so in the ticket
rather than guessing. Write the literal *Clause 3 is not* on a line that also carries *Files* and
*table* — gate 5 reads all three on one line. This is the one part of `ADR-0145` §§2–3 that is not
answerable from a Files table plus `ls`, and the section is honest about it rather than repeating
the ADR's sentence unqualified.

## Out of scope

- **Any other file.** No ADR is edited, no second copy of the rule is written into `qa-manager.md`,
  `uat.md`, a skill, `tasks/README.md`, `docs/workflow.md` or `CLAUDE.md`. `ADR-0091` §2 chose one
  place on the ground that a second copy drifts; gate 7 fails if a second agent or skill file names
  `routing/screen.ts`.
- **Rewriting or reflowing the rest of `planner.md`.** Gates 6, 8 and 9 exist because a whole-file
  rewrite that satisfies the new greps while dropping an existing subsection would otherwise pass.
- **Answering `DEC-161`** — what the minting side costs (an interactive session, or a dispatched
  `review: standard` ticket). `ADR-0145` §5 registered it as the architect's and this section must
  not settle it: state §3's *worked interactively with the human* as `ADR-0091` wrote it, and add
  nothing about tier or dispatch on the minting branch.
- **A new gate anywhere.** `ADR-0145` §Alternatives rejected an allowlist in `check-drift.sh` and a
  `lint_tickets.py` check on the `minting` label; this ticket writes prose in an agent file and adds
  no CI step.
- **Changing the `Screen` union, any card under `design/`, or any client file.**
- **The story file's own task table.** `STORY-0001` records in prose that its table is deliberately
  out of step and left as found; this is not the pull request that gets to make a claim about it.

## Tests

No test class — one prose document, gated by section-scoped `awk`. **Every `Today` cell was run on
`develop` at `e5f865e8`, and every `After` cell was run against a draft of the section built from
this ticket's Scope**, so each of gates 2–7 is known to be both red now and achievable.

| # | Gate | Proves | Today | After |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket file and board row agree | 0 | 0 |
| 2 | `awk` scoped to `## A new screen owes a card` | the trigger: `screen.ts`, `Design notes`, *first ticket is the card*, `ADR-0091` — **inside that section** | **1** | 0 |
| 3 | same scope | the transcriber test, *at any level of detail*, *one transcriber away*, the direction rule | **1** | 0 |
| 4 | same scope | the three clauses: `tokens.css`, `components/`, `graphics/`, `ls design/screens/`, `ADR-0145` | **1** | 0 |
| 5 | same scope | clause 3's limit is stated, on one line with *Files* and *table* | **1** | 0 |
| 6 | `^## ` count is 6 and `^### ` count is 6 | exactly one new top-level section arrives, at `##` and not `###`, and all six existing subsections survive | **1** — 5 and 6 today | 0 |
| 7 | `grep -rl routing/screen.ts` over `.claude/agents` and `.claude/skills` | the rule is in **exactly one** file and it is `planner.md` — `ADR-0091` §2's one-place clause | **1** — 0 files match today | 0 |
| 8 | `grep -Fq` on `### Size`'s sentence | the size rules were not rewritten | 0 — a **guard** | 0 |
| 9 | `grep -Fq` on `### The verify block`'s sentence | that subsection was not rewritten | 0 — a **guard** | 0 |
| 10 | `awk 'END{exit (NR<=300)?0:1}'` | the file did not run away — 224 lines today, about 276 after | 0 | 0 |

**Scoping is what makes gates 2–5 non-vacuous.** The same sentences written into `## Read` or into
the epic section would leave the split procedure saying nothing while every grep went green; keying
them to `## A new screen owes a card` is what stops that. **Gate 6 is the mutation guard for the
scope itself**: an implementer who satisfies gates 2–5 by deleting `### Tier` and writing over it
goes red on the `^### ` count, and one who files the section as `### A new screen owes a card` goes
red on the `^## ` count. Both were checked by hand against the draft.

**What no gate here can see** is whether the section is *coherent* — a grep cannot tell the
transcriber test from four pasted sentences that mention it. So the review is to read the finished
section against `ADR-0091` §§2–3 and `ADR-0145` §§2–3 and reject any sentence that (a) makes detail
or specificity the trigger, (b) makes the split about what the human has not yet seen rather than
what the card creates, (c) states the three-clause floor as **necessary** for minting, or (d) claims
clause 3 is readable from a Files table.

## Acceptance criteria

- [ ] `.claude/agents/planner.md` has a section headed exactly `## A new screen owes a card`, placed
      between `### Ordering` and `## When you are given an epic`.
- [ ] That section states `ADR-0091` §2's trigger: the `## Design notes` naming, *first ticket is
      the card*, and the `Screen` union in `web-client/src/routing/screen.ts` as the mechanical
      floor (gate 2).
- [ ] It states `ADR-0145` §2's transcriber test, including *scaffolding is composing, at any level
      of detail*, the card → client direction over shipped code, and the one-transcriber-away
      escalation (gate 3).
- [ ] It states `ADR-0145` §3's three clauses, naming `design/tokens/tokens.css`,
      `design/components/`, `design/graphics/` and `ls design/screens/` (gate 4).
- [ ] It says clause 3 is **not** readable from a Files table, on one line carrying *Clause 3 is
      not*, *Files* and *table* (gate 5).
- [ ] The file has exactly 6 `^## ` and 6 `^### ` headings (gate 6), and no other file under
      `.claude/agents/` or `.claude/skills/` names `routing/screen.ts` (gate 7).
- [ ] `### Size` and `### The verify block is the whole point` keep their opening sentences
      (gates 8, 9), and the file is 300 lines or fewer (gate 10).
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
