---
schema: 2
id: TASK-120703
title: The UAT screen inventory — the route map a round walks
type: task
status: done
parent: STORY-1207
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, uat, meta]
depends_on: [TASK-120702]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk -F'|' '/^### /{s=0} /^### The screen inventory/{s=1} s && $2 ~ /`/ { n++; if (NF!=7) bad++; k=$2; gsub(/[ `]/,"",k); seen[k]=1; w=$5; gsub(/[ \t]/,"",w); if (w=="notwalked") nw++; else if (w!="walked") bad++ } END{ split("first duels leaderboard account sign-in verify reset", g, " "); for (i in g) if (!(g[i] in seen)) bad++; exit (n==13 && nw==2 && bad==0)?0:1 }' docs/test-plan.md
  - awk -F'|' '/^### /{s=0} /^### The screen inventory/{s=1} s && index($4,"design/screens/") { p=$4; gsub(/[ `]/,"",p); if ((getline junk < p) < 0) bad=1; close(p); n++ } END{ exit (n==7 && !bad)?0:1 }' docs/test-plan.md
  - awk -F'|' 'FNR==NR { if ($2 ~ /^ *`[A-Z0-9]+-[0-9]+` *$/) { k=$2; gsub(/[ `]/,"",k); ids[k]=1 } next } /^### /{s=0} /^### The screen inventory/{s=1} s && $2 ~ /`/ { n++; w=$5; gsub(/[ \t]/,"",w); if (w=="walked") { c=0; m=split($6, a, ","); for(i=1;i<=m;i++){ v=a[i]; gsub(/[ `]/,"",v); if (v ~ /^[A-Z0-9]+-[0-9]+$/) { c++; if (!(v in ids)) bad=1 } } ; if (c==0) bad=1 } else { if (index($6,"ADR-")==0) bad=1 } } END{ exit (n==13 && !bad)?0:1 }' docs/test-plan.md docs/test-plan.md
  - awk 'FNR==NR { if (match($0, /^ *case "[a-z-]+":/)) { v=$0; sub(/^ *case "/,"",v); sub(/":.*/,"",v); verbs[v]=1 } ; next } grab { list = list $0; grab=0 } index($0,"The driver is `node scripts/qa/drive.mjs") { list = list $0; grab=1 } END { for (v in verbs) if (!index(list, "`" v "`")) bad=1; exit bad?1:0 }' scripts/qa/drive.mjs docs/test-plan.md
  - awk -F'|' '/^## /{s=0} /^## EPIC-04 —/{s=1} s && $2 ~ /`04-0[0-9]`/ { n++; if (NF != 7) bad++ } $2 ~ /`(SMK|CORE)-[0-9]+`/ { c++; if (NF != 6) bad++ } END{ exit (n==5 && c==26 && bad==0)?0:1 }' docs/test-plan.md
  - awk -F'|' '/^## /{s=0} /^## EPIC-05 —/{s=1} s && $2 ~ /`05-0[0-9]`/ { n++; if (NF != 7) bad++ } END{ exit (n==5 && bad==0)?0:1 }' docs/test-plan.md
---

## Goal

`docs/test-plan.md` carries a `## UAT` section whose screen inventory names every screen-state a
UAT round walks, the merged card each is judged against, whether the harness can reach it at all,
and the case ids whose `do` columns are the route there.

## What this section is, and what it is not

`ADR-0092` §7: **the catalogue is reused as a route map.** *"The existing cases' `do` columns are
the routes — they already reach every screen-state the product has — and their `expect`/`fails if`
columns stay functional and are never graded on UX: a case graded on two rubrics is ambiguous on
both."* This section therefore **adds no case, changes no case, and touches no existing row**.
`SMOKE` and `CORE` are not retrofitted (`ADR-0090` §4's precedent, restated by §7).

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |

You may **read** `web-client/src/routing/screen.ts` (the `Screen` union only), `design/screens/`
(the file names — `ls`, not the contents), and `docs/adr/ADR-0092-…` §§3, 4, 6, 7.

## Scope

### 1. The verb line

The paragraph beginning *"The driver is `node scripts/qa/drive.mjs …"* lists ten verbs and is now
short two: `close` landed in `TASK-120506` and `shot` in `TASK-120702`. Gate 5 compares that list
against every `case "…":` in `scripts/qa/drive.mjs` and is **red today** for `close` alone, so
both go in — a list that is stale by one is what let it go stale by two.

### 2. A new `## UAT` section, at the end, before `## What this catalogue does not cover`

Preceded by a `---` rule, headed `## UAT — the screens a round walks, and the questions it asks`,
opening with two or three sentences saying what §7 says above: not a suite, adds no case, the
`do` columns are the routes, no `expect` is graded on UX.

### 3. `### The screen inventory` — exactly thirteen rows, five columns

The row set is **derived, not chosen**: one row per merged card under `design/screens/` (seven
files today), plus one row per member of the `Screen` union
(`web-client/src/routing/screen.ts`) that no card shows — the six `ADR-0091` §5 registered as
debt. 7 + 6 = 13.

| screen | state | card | walk | routes |
| --- | --- | --- | --- | --- |

- **screen** — the `Screen` union member, backticked. All seven members must appear; `first`
  appears seven times, once per card.
- **state** — which state of that screen the card draws, in plain words.
- **card** — the path under `design/screens/`, backticked, or the em dash `—` where none exists.
  Gate 3 opens every path it finds, so a typo or a renamed card is red.
- **walk** — exactly `walked` or exactly `not walked`. Nothing else; gate 2 rejects any third
  value.
- **routes** — for a `walked` row, the case ids whose `do` columns reach that state, comma
  separated and backticked; for a `not walked` row, the reason, naming the merged source it comes
  from. Gate 4 resolves every id against the case tables and refuses a `walked` row with no id at
  all.

### 4. `verify` and `reset` are `not walked`, and the reason is quoted

This document already says why, in §*What this catalogue does not cover*:

> **The whole of recovery — `#/verify`, `#/reset`, and *Forgot your password?* past its
> acknowledgement — is outside this catalogue for that reason.**

The reason is not laziness and no case can fix it: a machine with no mail transport binds
`NoRecoveryMailer` (`ADR-0031` §7), the verification and reset tokens are stored only as `BYTEA`
hashes (`V8__recovery_email.sql`), and **no mailed link ever arrives for a driver to follow**.
Both rows read `not walked` with `ADR-0031` §7 named, so the first UAT round records them as
*not walked* rather than as *passed* or as a defect a harness could have caught.

### 5. One paragraph under the table saying what that costs

Three sentences, all of them applications of merged text rather than new rules:

- **No missing-card finding is filed for them by a round.** `ADR-0092` §4 files a `high` for *"a
  screen **in scope** with no merged card"*, and a screen no route reaches is not in a round's
  scope. Its per-screen cells read `out of scope`, which is one of the three values §6 allows.
- **Their cards are still owed.** `ADR-0091` §5 registers all six cardless screens, and `ADR-0092`
  §4 narrows only the *vehicle*: *"UAT rounds file the cards their scopes reach first, and the
  `EPIC-06` retrofit story, when split, covers only the slugs still cardless."* These two slugs
  are that remainder. A card is a reference for a human to accept at the pane, which needs no
  driver.
- **And they will never have a conformance check behind them.** Say it plainly. A card for an
  unwalkable screen is accepted by the human alone — which is what every card was before this
  section existed, and `ADR-0089` §2c already forbids reading any round as the thing that
  validated one. Writing the asymmetry down is the point; nothing here repairs it.

## Out of scope

- **Every section of `docs/test-plan.md` except the verb line and the new `## UAT` section.** Gates
  6 and 7 re-run `TASK-120401` and `TASK-120402`'s own shape checks over the `EPIC-04` and
  `EPIC-05` suites, so a row or a column lost while adding this section is red.
- **The `### Standing questions` sub-section, §*What this catalogue does not cover*, and
  §*Not yet written*.** All three belong to `TASK-120704`, which runs next over the same file.
- **Writing, editing, renumbering or re-sourcing any case.** `ADR-0092` §7. A UAT observation is
  not a case, and this section is a map.
- **Composing any missing card, or filing a ticket for one.** A missing card is a round's `high`
  finding with the card path as its dedupe key (`ADR-0092` §4); filing one here would pre-empt
  both the round and the key.
- **Deciding what a `#/verify` address renders with no token.** The row says `not walked` and why;
  it makes no claim about what is on the screen.
- **Any change under `design/`, `web-client/` or `scripts/`.** This ticket reads them.
- **Any sentence that reads a round, a `PASS`, or this section as coverage.** `ADR-0089` §2c.

## Tests

No test class — the deliverable is one document section, so the gates are structural checks over
it. Every row was run on 2026-08-30 at commit `cfcc6a4e`, against the tree as it stands and
against a thirteen-row draft written to satisfy them: **four of the seven are red today, and
all seven are green with the section.** The other three are guards over suites this ticket must
not break.

| # | Gate | Proves | Today | With the section |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | the ticket, story and board rows agree | 0 | 0 |
| 2 | inventory shape | thirteen rows, five columns each (`NF==7`), all seven `Screen` members present, every `walk` cell exactly `walked` or `not walked`, exactly two `not walked` | **1** | 0 |
| 3 | card paths open | all seven card cells name a file that exists — `getline < path` returns −1 on a path that does not | **1** | 0 |
| 4 | routes resolve | every case id cited in the inventory is a case this document defines, **every `walked` row cites at least one**, and every `not walked` row names an `ADR-` | **1** | 0 |
| 5 | verb lists agree | every `case "…":` in `scripts/qa/drive.mjs` is named in the driver paragraph | **1** — `close` is missing today | 0 |
| 6 | `EPIC-04` suite intact | five `04-0N` rows at seven fields, twenty-six `SMK`/`CORE` rows at six — `TASK-120401`'s own gate | 0 | 0 |
| 7 | `EPIC-05` suite intact | five `05-0N` rows at seven fields — `TASK-120402`'s own gate | 0 | 0 |

**Gates 3 and 4 are the ones worth having, and both were mutation-tested on 2026-08-30.**
Renaming one card path in the draft to `design/screens/duel-finish.html` turns gate 3 **red**.
Replacing a `walked` row's `04-01` with prose turns gate 4 **red**; so does striking the `ADR-`
reference out of the `reset` row. A first attempt at gate 4 was written without the row count and
**passed vacuously on today's tree** — no section, no rows, no failures — which is why `n==13`
is inside it and not beside it.

**These gates are keyed on column position.** `-F'|'` means field five is the `walk` cell because
the table has five columns; a restructured row would break them silently if `NF` were not pinned,
which is why every one of gates 2, 6 and 7 pins a field count. Do not add a sixth column.

**What no gate here sees**: whether a `routes` cell names the case that actually reaches that
state. Gate 4 proves the id exists and that a walked row has one; it cannot read a `do` column.
That is the reviewer's, against the case tables in the same file.

## Acceptance criteria

- [ ] `docs/test-plan.md` has a `## UAT` section with a `### The screen inventory` table of
      thirteen five-column rows (gate 2).
- [ ] All seven `Screen` members appear in the `screen` column, and `first` appears once per card
      (gate 2).
- [ ] Every card cell either names a file that exists under `design/screens/` or is `—` (gate 3).
- [ ] Exactly two rows read `not walked` — `verify` and `reset` — and each names `ADR-0031` §7 as
      the reason (gates 2 and 4).
- [ ] Every `walked` row cites at least one case id, and every id cited exists in this document
      (gate 4).
- [ ] The driver paragraph lists every verb `scripts/qa/drive.mjs` implements, `close` and `shot`
      included (gate 5).
- [ ] The paragraph under the table states all three of §Scope 5's sentences: no missing-card
      finding is filed for an unwalkable screen, its card is still owed under `ADR-0091` §5 through
      the `EPIC-06` retrofit, and it will never have a conformance check behind it.
- [ ] The `EPIC-04` and `EPIC-05` suites are unchanged in shape (gates 6 and 7).
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
