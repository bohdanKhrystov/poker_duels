---
id: STORY-1208
title: Step 6 stops a healthy cycle — the verdict table never checks for a baseline round
type: story
status: done
parent: EPIC-12
labels: [process, qa, uat, meta, defect]
depends_on: [STORY-1207]
---

## Goal

A `qa-manager` that reads `## Step 6`'s verdict table top to bottom, and `EPIC-12` §Termination
rule 4 top to bottom, reaches the right verdict on a **baseline round** — the round in which
repaired cards first become measurable. Today both documents, read literally, fire
`STOP_DIVERGING` on exactly that round, and the exemption that saves it is prose one section away
in one document and absent from the other.

## This is not a round story

Like `STORY-1203`, `STORY-1204` and `STORY-1207`, it brings no stack up, starts no browser and
reports no `B(N)`. It is a **defect in the cycle's own machinery**, found by `TASK-120709`'s coder
and confirmed by its reviewer, both of whom judged the fix out of scope there — that ticket's
*Out of scope* reserves the row in as many words: *"Changing what `STOP_DIVERGING` does when it
fires. Only the `STOP_BLOCKED` row is scoped."* This is the fourth non-round story under
`EPIC-12`, and the epic's Stories table says so.

## Why

`TASK-120709` merged as `efa3b6fd` and put `ADR-0092` §6's baseline rule into
`.claude/agents/qa-manager.md` as prose under `## The UAT arithmetic`. It did not touch
`## Step 6`, whose table is what a triager actually applies:

| Verdict | When |
| --- | --- |
| `PROCEED` | `B(N) > 0`, `B(N) < B(N-1)` (or `N == 1`), and `N < 3` |
| `STOP_DIVERGING` | `B(N) >= B(N-1)` and `N > 1` |

`N == 1` is special-cased and nothing else is. The worked example, from that ticket's own coder:
round 1 excludes six missing cards, so `B(1) = 1`; the six cards are repaired; round 2 finds two
genuine `high` mismatches against the newly judgeable cards, so `B(2) = 2`. Read literally,
`2 >= 1` fires `STOP_DIVERGING` — **stopping the cycle on the exact round the repairs first became
measurable**, which is the outcome `ADR-0092` §6 exists to prevent.

The `PROCEED` row is the half nobody has noticed. Exempting only `STOP_DIVERGING` would leave a
baseline round with `B(N) >= B(N-1)` matching **no row at all**: `PASS` needs zero, `PROCEED`
needs a strict decrease, `STOP_BUDGET` needs `N == 3`, `STOP_BLOCKED` needs a human-only decision.
A table that must emit exactly one verdict would emit none. Both rows move together, or the fix is
worse than the defect.

## Design notes

Everything here is settled by `ADR-0092` §6 and `EPIC-12` §Termination. Nothing is open, and the
four calls that were not obvious are argued rather than asserted.

### The same defect lives in the epic, and that is the second ticket

`qa-manager.md`'s baseline paragraph says *"**rule 4** skips comparing"* and *"**Rule 5's**
three-round budget binds regardless."* Those numbers are `EPIC-12` §Termination's, and the epic is
in `qa-manager`'s *What you are given* as the round ledger. Rule 4 there reads *"If
`B(N) >= B(N-1)` the cycle **stops**"* with no exemption — so the cross-reference in the fixed
document points at the unfixed one. `ADR-0089` §4's amendment set the precedent: it landed as
§Termination **rule 6** *and* in the register, not the register alone. `ADR-0092` §6's amendment
got only the register row (`DEC-085`, point 5). `TASK-120802` closes that, and its gate is scoped
to rule 4's own numbered block rather than to the file.

### Whether a round is a baseline round is not a computed field, and that is a real hazard

**Gate it** — for a reason sharper than tidiness. The rule's input is *"a screen became
conformance-judgeable for the first time in round N — its card merged in round N-1's repairs."*
Round *N-1*'s story is written **at triage**, before its repairs run, so it records a card ticket
as *filed*, not as *merged*. Whether it merged is a second fact in a third place. The
determination is therefore not read off the ledger; it is reconstructed every round by the one
role with an incentive to skip it — and skipping it lands on the literal reading, which is this
story's defect one level up.

The fix is two lines and costs nothing: the determination becomes a **stated output**. `## The UAT
arithmetic` requires it in the round story with its reason, exactly as the three `B(N)` exclusions
already are; and the `## Report` block gains a `BASELINE:` field beside `B(N):`, so every triage
declares an answer whether or not the round was a baseline one. Nothing parses that report —
`grep -rn "B(N):"` across `.claude/`, `scripts/` and `docs/` returns the template line and nothing
else — so a new field breaks no consumer.

### The 230-line ceiling moves once, to 242, and this story says why

Every `STORY-1207` ticket carried `awk 'END{exit (NR<=N)?0:1}'` over
`.claude/agents/qa-manager.md`, each setting *N* to the size the file happened to reach;
`TASK-120709` landed at **exactly 230** against `NR<=230`. That gate is a **drift guard**, not a
budget, and a drift guard raised silently to whatever the current diff needs guards nothing.

So it is raised **once, deliberately, to 242** — `230 + 12` — with the reasoning on the record
rather than in a commit message. The specified fix measured **238 lines** in a probe run while
this story was written: eight added lines, six of them Step 6 prose and two the arithmetic
section's, since the two table rows are amended in place and cost nothing and `BASELINE:` is one.
The four lines of slack are deliberate — a coder one line over would otherwise reflow a
neighbouring paragraph, which is invisible in a line count and would blow a silent hole in the
guard. `TASK-120802` carries `NR<=242` **unchanged** as a green guard, so the number is asserted
twice in this story and raised once. The next ticket that wants a line trims one.

### Three documents restate the stopping rules, and only two of them can misfire

`ADR-0092` §8 refused a `uat-manager` because *"two copies of a rule drift"*, and named the price
it was accepting: `qa-cycle`'s `SKILL.md`, `qa-manager.md` and `EPIC-12` each hold a prose copy.
Two are fixed here. The third is left alone for a stated reason, in *Out of scope* below, and
pinned by `sha256` in both tickets so that the refusal is an exit code rather than a hope.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-120801](../tasks/TASK-120801-the-verdict-table-checks-for-a-baseline-round-first.md) | Step 6's verdict table checks for a baseline round first | ready |
| [TASK-120802](../tasks/TASK-120802-termination-rule-4-carries-its-own-exemption.md) | `EPIC-12` §Termination rule 4 carries its own exemption | backlog |

Two tickets, one file each, chained. They are not one ticket because they are two documents with
two audiences and two independently checkable states: `qa-manager.md` is the brief the triager
runs on, and `EPIC-12` §Termination is the ledger it is handed. `TASK-120801` is first because
`TASK-120802`'s prose cites the exemption `TASK-120801` writes, and because a reader who arrives
mid-story finds the operative document already correct rather than the reverse.

## Acceptance criteria

- [ ] `## Step 6`'s `STOP_DIVERGING` row contains the literal `not a baseline round`, and its
      `PROCEED` row contains `baseline round` — in the rows themselves, not in a nearby paragraph.
- [ ] `## Step 6` names `The UAT arithmetic` **above** the verdict table's header row, so the
      check comes before the table a reader is about to apply.
- [ ] `## Report` carries `BASELINE:` alongside the surviving `VERDICT:` and `B(N):` lines.
- [ ] `EPIC-12` §Termination rule 4 names a `baseline` round and cites `ADR-0092`, inside rule 4's
      own numbered block; rule 6 still names the harness exclusion.
- [ ] `EPIC-12` §Termination's `STOP_BLOCKED` exit-state row is **byte-unchanged**.
- [ ] `.claude/agents/qa-manager.md` names `qa-cycle` nowhere and is 242 lines or fewer;
      `.claude/agents/qa.md` and `.claude/skills/qa-cycle/SKILL.md` are byte-unchanged.
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0.

## Out of scope

- **`.claude/skills/qa-cycle/SKILL.md`.** Its `## The stopping rules` list carries the same
  literal — *"if `B(N) >= B(N-1)`, stop with `STOP_DIVERGING`"* — but its **Baseline round** bullet
  sits four bullets below it in the same list under the same heading, so a reader meets both; and
  the skill says outright that *"`qa-manager` computes and enforces them; this skill obeys without
  arguing."* It routes on a verdict it is handed and computes none, so it cannot produce this
  defect. Both tickets pin its `sha256`
  (`2101bef0d0975ecb45ed410453a0886748a1b08c9298d7316c751915cac31c8c`).
- **`EPIC-12` §Termination's `STOP_BLOCKED` exit-state row.** `ADR-0092` §5 assigned that scoping
  to *"the epic's register"* by name, and `DEC-085` point 6 carries it. Editing the row would go
  past the ADR; `TASK-120802` gates it byte-unchanged.
- **A fourth `B(N)` exclusion, a sixth exit state, a fourth severity, or any loosening of rules 2,
  3 or 5.** `ADR-0092` §6: the machinery *"carries over unchanged"*. A fourth is a new `DEC`.
- **Making baseline-ness computable by a program.** A script that decided the verdict would be a
  second source of truth beside the prose the agent actually reads, and which of the two binds is
  an architectural question nothing has answered. If it is ever wanted it is a `DEC`, not a ticket.
- **Any change to `.claude/agents/qa.md`** (`ADR-0092` §8: byte-unchanged; both tickets pin its
  `sha256`), to `.claude/agents/uat.md`, or to `docs/test-plan.md`.
- **Writing `qa-cycle` into `.claude/agents/qa-manager.md`.** `ADR-0092` §2's four-file set; the
  manager *"gains no licence to"* name it. Gated from both directions in both tickets.
- **Running a UAT or QA round.** The first UAT round is the human typing `/qa-cycle uat <scope>`
  and produces its own round story — `STORY-1209` or later, never this one.
- **Ticking any `EPIC-12` Definition-of-done box.** None of them asks for this repair.
