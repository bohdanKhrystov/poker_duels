---
schema: 2
id: TASK-120801
title: Step 6's verdict table checks for a baseline round first
type: task
status: done
parent: STORY-1208
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, uat, meta, defect]
depends_on: [TASK-120709]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk '/^## /{s=0} /^## Step 6/{s=1} s && /^\| `STOP_DIVERGING` \|/ && index($0,"not a baseline round"){f=1} END{exit f?0:1}' .claude/agents/qa-manager.md
  - awk '/^## /{s=0} /^## Step 6/{s=1} s && /^\| `PROCEED` \|/ && index($0,"baseline round"){f=1} END{exit f?0:1}' .claude/agents/qa-manager.md
  - awk '/^## /{s=0} /^## Step 6/{s=1} s && index($0,"The UAT arithmetic") && !t{f=1} s && /^\| Verdict \| When \| Meaning \|$/{t=1} END{exit (f&&t)?0:1}' .claude/agents/qa-manager.md
  - awk '/^## /{s=0} /^## Report$/{s=1} s && /^BASELINE:/{a=1} s && /^VERDICT:/{b=1} s && /^B\(N\):/{c=1} END{exit (a&&b&&c)?0:1}' .claude/agents/qa-manager.md
  - awk '/^## /{s=0} /^## The UAT arithmetic$/{s=1} s{l=tolower($0)} s && index(l,"harness"){a=1} s && index(l,"missing card"){b=1} s && index(l,"decision-born"){c=1} s && index(l,"baseline round"){d=1} s && index(l,"pass (conformance unjudged on"){e=1} END{exit (a&&b&&c&&d&&e)?0:1}' .claude/agents/qa-manager.md
  - awk 'index($0,"qa-cycle"){f=1} END{exit f?1:0}' .claude/agents/qa-manager.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | grep -Ev '^\.claude/(agents/(qa|uat)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' | awk 'END{exit (NR==0)?0:1}'
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - shasum -a 256 .claude/skills/qa-cycle/SKILL.md | awk '{exit ($1=="2101bef0d0975ecb45ed410453a0886748a1b08c9298d7316c751915cac31c8c")?0:1}'
  - awk 'END{exit (NR<=242)?0:1}' .claude/agents/qa-manager.md
---

## Goal

A triager who reads `## Step 6` and applies its table cannot fire `STOP_DIVERGING` on a baseline
round, because the check comes before the table, both affected rows carry the exemption, and the
answer is written into the round story and the report.

## The defect, stated once

`## The UAT arithmetic` (line 90, merged by `TASK-120709`) says a baseline round skips rule 4's
comparison. `## Step 6` does not know. Its table special-cases `N == 1` and nothing else, so
`B(1) = 1` after six excluded missing cards, then `B(2) = 2` against the newly judgeable cards,
reads as `2 >= 1` and stops the cycle on the round the repairs first became measurable.

**Both rows move, not one.** `PROCEED` requires a strict decrease. Exempt only `STOP_DIVERGING`
and a baseline round with `B(N) >= B(N-1)` matches **no row at all** — a table that must emit
exactly one verdict emits none. That is the trap in this ticket and it is why the tier is
`sonnet`.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/qa-manager.md` | modify |

You may **read** `tasks/stories/STORY-1208-the-verdict-table-never-checks-for-a-baseline-round.md`
and `docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md` §6. Nothing
else. Do not open `.claude/skills/qa-cycle/SKILL.md` to copy its wording — gate 10 pins it
unchanged, and its list is out of scope for a stated reason.

## Scope

Four edits, about eight added lines. **The file is 230 lines and gate 11 admits 242.** That is
the whole budget: `230 + 12`, raised once, argued in the story. Do not reflow a paragraph you are
not editing to buy room — a reflow is invisible in a line count and defeats the guard.

### 1. `## Step 6` — one paragraph, above the table

Immediately after the section's opening *Compute B(N)* line and **before** `Emit exactly one:`.
Gate 4 requires the string `The UAT arithmetic` to appear inside `## Step 6` **above** the verdict
table's header row, and requires that header row to still exist; a pointer written below the table
is read too late, and the gate says so.

Four or five lines, in your own words, carrying all four of:

- the instruction to answer *"is `N` a baseline round?"* **before** reading the table;
- the cross-reference `§*The UAT arithmetic*` as where the answer is defined;
- the worked example, with its numbers: `B(1) = 1` after six excluded missing cards, `B(2) = 2`
  against the repaired cards, and the fact that `2 >= 1` read without the exemption stops the
  cycle on the round the repairs first became measurable;
- that the answer is written down — the round story and the `BASELINE:` line of `## Report`.

### 2. `## Step 6` — the two table rows, amended in place

Write these two literally. Both are the same width as what they replace, so this edit adds no
lines. Gates 2 and 3 read the rows themselves, not the section.

```
| `PROCEED` | `B(N) > 0`, `N < 3`, and `B(N) < B(N-1)` unless `N == 1` or *N* is a baseline round | repair this fix set, then retest |
| `STOP_DIVERGING` | `B(N) >= B(N-1)`, `N > 1`, and *N* is **not a baseline round** | the loop is not winning — end it |
```

Leave `PASS`, `STOP_BUDGET` and `STOP_BLOCKED` untouched. `STOP_BLOCKED`'s wording is
`TASK-120709`'s and gate 6's neighbour section still checks its own half.

### 3. `## The UAT arithmetic` — the determination is stated, not reconstructed

Two lines appended to the paragraph that ends **Rule 5's three-round budget binds regardless.**
The triager states in the round story whether *N* is a baseline round, which screens made it one,
and whose repairs merged their cards — for the same reason the three `B(N)` exclusions are stated
there. It is not derivable at a glance: round *N-1*'s story was written at **triage**, before its
repairs ran, so it records a card ticket as filed rather than as merged.

Do not disturb the existing sentences: gate 6 re-checks that `harness`, `missing card`,
`decision-born`, `baseline round` and `pass (conformance unjudged on` all still live in this
section.

### 4. `## Report` — one new field

Inside the fenced block, immediately under the `B(N):` line:

```
BASELINE: <no | yes — screens newly judgeable, cards merged in round N-1>
```

Gate 5 requires `BASELINE:`, `VERDICT:` and `B(N):` to all be present in `## Report`, so the field
is added rather than swapped in. Nothing parses this block —
`grep -rn "B(N):" .claude/ scripts/ docs/` returns the template line alone — so no consumer breaks.

## Out of scope

- **`.claude/skills/qa-cycle/SKILL.md`** (gate 10 pins its `sha256`) and **`.claude/agents/qa.md`**
  (gate 9). The skill's convergence bullet has the baseline bullet four bullets below it in the
  same list, and the skill computes no verdict — `STORY-1208` §*Out of scope* argues it.
- **`tasks/epics/EPIC-12-…`** §Termination rule 4. Same defect, other document, `TASK-120802`.
- **Writing `qa-cycle` anywhere in this file.** `ADR-0092` §2's four-file set; gates 7 and 8 both
  go red on it.
- **A sixth exit state, a fourth severity, a fourth `B(N)` exclusion**, or any change to what
  `PASS`, `STOP_BUDGET` or `STOP_BLOCKED` mean. `ADR-0092` §6: the machinery carries over.
- **Changing what `STOP_DIVERGING` does when it fires.** Only *when* it fires is in scope. The
  paragraph below the table — *"Not 'tries once more'. Stops."* — and the two forbidden cheats
  stay exactly as they are.
- **Making baseline-ness computable.** No script, no field in a machine-read file.
- **Raising gate 11 above 242.** If the fix will not fit, it is over-written; cut it.

## Tests

No test class — one prose document. Every row below was run at commit `efa3b6fd`, which is this
ticket's base. The **After** column was measured too: a probe copy carrying the four edits above
was built in `/tmp`, every gate was run against it, and it came out at **238 lines**. The probe
was discarded; nothing in this repository changed.

| # | Gate | Proves | Today | After |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` over the `STOP_DIVERGING` **row** in `## Step 6` | the exemption is in the defective cell, not near it | **1** | 0 |
| 3 | `awk` over the `PROCEED` **row** in `## Step 6` | the other half of the partition moved too | **1** | 0 |
| 4 | `awk`: `The UAT arithmetic` inside `## Step 6` **and above** the verdict table's header row, which must still be present | the check is reachable *before* the table, not after it | **1** | 0 |
| 5 | `awk` over `## Report`: `BASELINE:` **and** `VERDICT:` **and** `B(N):` | the determination is a declared output, and the field was added rather than swapped in | **1** | 0 |
| 6 | `awk` scoped to `## The UAT arithmetic` (`TASK-120709` gate 2, verbatim) | edit 3 did not disturb the three exclusions, the baseline rule or the qualified-verdict example | 0 — a **guard** | 0 |
| 7 | `awk` over `qa-cycle` in this file | the manager still names the cycle nowhere | 0 — a guard | 0 |
| 8 | `ADR-0092` §2's own command | no undeclared file names the cycle | 0 — a guard | 0 |
| 9 | `sha256` of `qa.md` | byte-unchanged | 0 — a guard | 0 |
| 10 | `sha256` of `qa-cycle/SKILL.md` | the third prose copy was left alone, deliberately | 0 — a guard | 0 |
| 11 | `awk 'END{exit (NR<=242)?0:1}'` | the file did not run away — 230 today, 238 in the probe | 0 | 0 |

**Gates 2 and 3 are row-scoped, and that scoping is what makes them non-vacuous.** The word
`baseline` written into a paragraph beside the table would leave the table saying the old thing
while a section-scoped gate went green; keying each match to a regex anchored on that row's own
first cell is what stops it. Both row patterns were confirmed to match today's rows — the same
`awk` with the `index(...)` clause removed exits 0 — so their red is a missing exemption, not a
pattern that matches nothing.

**Gate 4 is the one that encodes the defect itself**, which is an *ordering* defect: the rule
exists, and Step 6 never tells the reader to consult it first. It is also the weakest gate here,
and the ticket says so rather than pretending otherwise: a coder can satisfy it with any sentence
containing `The UAT arithmetic` placed above the table. It proves position, not content.

**What no gate here can see** is whether the four sentences of edit 1 and the two of edit 3 are
*right*. **So the review is to read `## Step 6` and `## The UAT arithmetic` against `ADR-0092`
§6** and reject: a worked example whose stated verdict is not `PROCEED`; a paragraph that exempts
a baseline round from rule 5's three-round budget, which binds regardless; any sentence implying
the exemption applies to a QA-focus round, where no screen becomes conformance-judgeable; and any
wording that lets a triager declare a round baseline without naming the screens and the round
whose repairs merged their cards.

## Acceptance criteria

- [ ] `## Step 6`'s `STOP_DIVERGING` row contains the literal `not a baseline round` (gate 2).
- [ ] `## Step 6`'s `PROCEED` row contains `baseline round` (gate 3).
- [ ] `## Step 6` names `The UAT arithmetic` above the `| Verdict | When | Meaning |` header, and
      that header still exists (gate 4).
- [ ] `## Report` carries `BASELINE:`, `VERDICT:` and `B(N):` (gate 5).
- [ ] `## The UAT arithmetic` still carries all five strings `TASK-120709` put there (gate 6).
- [ ] The file names `qa-cycle` nowhere (gates 7 and 8).
- [ ] `.claude/agents/qa.md` and `.claude/skills/qa-cycle/SKILL.md` are byte-unchanged (gates 9
      and 10), and `qa-manager.md` is 242 lines or fewer (gate 11).
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
