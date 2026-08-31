---
id: STORY-1212
title: The audit focus — the observer, the resize, and what an unmet criterion costs
type: story
status: ready
parent: EPIC-12
labels: [process, qa, audit, meta]
depends_on: []
---

## Goal

`/qa-cycle audit <scope>` exists: a **third** focus of the same cycle, with its own observer
(`.claude/agents/audit.md` — the fifth declared file), its own hands (a `size` verb that walks one
live tab at two shapes), and a `qa-manager` that triages all three focuses on one ledger under
[`ADR-0096`](../../docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md)'s
rubric classifier and `A(N)` arithmetic. Nothing here runs a round; this story builds the machinery
a round needs.

## This is not a round story

`EPIC-12`'s Stories table says *"one story per QA round; the round number lives in the story, not
the id."* **This story breaks that convention**, exactly as
[`STORY-1207`](STORY-1207-the-uat-focus-the-observer-and-what-it-may-file.md) does — it is the same
shape one focus later. It brings no stack up, starts no browser and reports no `A(N)`. It is the
**fifth** non-round story, after `STORY-1203`, `STORY-1204`, `STORY-1207` and `STORY-1208` — with
`STORY-1201` counted separately as a **retrospective record**, the way the epic's own rows count
it. The epic's table gains a row saying so in the same PR, rather than leaving a reader to notice.

Round stories continue to take the next free id. The convention has now moved four times, and each
shift is written into the epic's own rows.

## Why

Two ADRs merged on 2026-08-31 and name their own deliverables.

`ADR-0096` (`DEC-096`) makes a **product audit** a third focus of this one cycle: its unit is the
**beat**, not the screen; it walks eight beats from the opened link to the rematch, both browsers
observed, at **two shapes**; a finding contradicts a **criterion** in a merged, closed, five-line
rubric and needs no other merged source; there is **no severity and no backlog** under this focus;
and termination is the rubric — `A(N)` counts criteria answered `not met`, so `A(N) ≤ |rubric|` is
a ceiling known before the round starts.

[`ADR-0097`](../../docs/adr/ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md)
(`DEC-097`) answers both halves of *what text changes in which files to make the focus legal*:
`ADR-0090` §2's declared-file set becomes **five**, `agents/audit.md` mention-only; and a viewport
resize is an **act** under `ADR-0089` §3, carried by one `size` verb that sets exactly `width`,
`height`, `deviceScaleFactor: 0` and `mobile: false`, reads the viewport back and **exits 1 if the
read-back is not the request**.

`EPIC-12`'s `## Open decisions` now reads **None**, and says in as many words: *"The first audit
round is unblocked, and the planner now has what it was missing — a fifth declared agent file for
the `Files` table, and a verb that walks one beat at two shapes."* Nothing audit exists in the
repository today: no `.claude/agents/audit.md`, no `size` verb, no `audit` focus in the skill, and
`.claude/agents/qa-manager.md` names neither the rubric nor `A(N)`.

## Design notes

Everything below is settled by a merged ADR, except the one question `## The decision this story
raises` registers. The places where *settled* was not obvious are argued below rather than
asserted.

**Ordering is a requirement, not a convenience.** The chain is linear and runs harness → observer
→ focus → manager → epic, so that no ticket ever merges naming something that does not exist: the
observer's brief names the `size` verb as its hands, the skill's focus dispatches the observer, and
the manager parses the report shape the observer fixes. Every ticket's dependency is the one before
it; exactly one is `ready`.

**No focus chains into another, and that is three refusals now.** `ADR-0089` §2b as amended by
`ADR-0090` §1: a cycle is started by the human's own message and nothing else, and `/qa-cycle
audit <scope>` is the first act of the turn it starts. The QA focus never chains into UAT, UAT
never chains into audit, and no report prints another focus's command. A preceding round of any
focus is the human's practice, never a checked precondition — a check would cite a round as a gate
(`ADR-0089` §2c). The tickets carry these as refusals rather than as features.

**`.claude/agents/qa.md` and `.claude/agents/uat.md` both stand byte-unchanged.** `ADR-0096` §2
freezes `ADR-0092` §3's classifier **byte-unchanged for the `qa` and `uat` focuses**, and
`ADR-0097` §4's whole case for a fifth file is that one file cannot hold two mutually exclusive
classifiers. A ticket that edits either observer is wrong; five tickets carry both `sha256`s as
gates.

**`.claude/agents/qa-manager.md` still may not name `qa-cycle`.** `ADR-0090` §2 declared three
files, `ADR-0092` §2 made it four, `ADR-0097` §4 makes it **five** — and the manager is in none of
those sets. It names the cycle nowhere today and gains no licence to, which is the most likely
accident in `TASK-121205`/`TASK-121206`, since describing the `audit` focus invites writing
`/qa-cycle audit`. Both tickets gate on the absence.

**The declared-file-set amendment needs no ticket of its own.** `ADR-0097` §4 *is* the amendment,
and it is merged. Its working copy is the check itself, which lives in ticket `verify:` blocks —
exactly where `ADR-0092` §2's four-file version lives today (`TASK-120705` gates 3 and 4). The
five-file version, both directions, is carried by `TASK-121202` and re-run as a guard by three
later tickets. Verified against the tree as this story is written: the guard exits **0** at four
files, **0** at five, and **1** at six.

### The rubric is cited, never transcribed

`ADR-0096` §2 supplies the founding rubric in full — five criteria, `R1` to `R5`, in priority
order, each with its licensing source. It is merged, it is closed and it is general, which is the
whole of what §2 requires of it.

**No ticket here copies it into another file**, and that is deliberate rather than an omission.
`TASK-120705`'s own refusal list gives the reason: *"naming a case id, a card path or a screen
[in an agent file] makes a second register that rots"*, and the `uat` observer points at
`docs/test-plan.md` §*UAT* instead of transcribing it. The same discipline applies one focus later,
so `.claude/agents/audit.md` and `.claude/agents/qa-manager.md` both **name `ADR-0096` §§1 and 2 as
the observer's context budget** — the beat table is the walk, the rubric table is the list — and
neither restates a beat or a criterion. Two line caps enforce it structurally: a file that
transcribed both tables could not fit.

Whether a *working copy* of the rubric should exist somewhere, and by what mechanism a sixth
criterion enters it, is the one thing neither ADR settles. It is registered below and it blocks
nothing.

### The walk is eight beats, and a scope word narrows nothing

`ADR-0096` §5: *"A round ends when every criterion has been answered at **every beat**. The auditor
has no discretion to keep looking, because there is nothing else on the list to look at."* §1's
beat table has no scope column, and `A(N) ≤ |rubric|` is *"a ceiling known before the round
starts"* — a quantity independent of any scope.

So the walk is the eight beats, all of them, every round. The scope word `/qa-cycle audit <scope>`
takes is **recorded in the round record's `SCOPE:` line and narrows nothing**, because every report
shape in this cycle carries that field and dropping it would make three focuses parse differently
for no gain. This is a transcription of §5's own end condition, not a new rule: there is no smaller
audit than one duel, since `R1` and `R5` are properties of a sequence and beat 5 does not exist
until a hand goes all in.

### Beat 5 is reachable with a player's hands, and nothing is seeded

`ADR-0096` §1's own note on beat 5: *"reachable with a player's hands alone — 'A player may always
go all-in for their remaining stack' — so `ADR-0089` §3 is untouched and nothing is seeded."* The
observer clicks *All in* on a live table like a player would. No case seeds a stack, a deck or a
socket frame; `ADR-0002` and `ADR-0089` §3 stand.

### `docs/test-plan.md` gains nothing, and this is a reasoned refusal

`ADR-0092` §7 made the catalogue the UAT focus's **route map** and required its section to merge
*"before the first UAT round"*. Nothing analogous is said for the audit, and the reason is
structural: the audit's route map is `ADR-0096` §1's beat table, which is already merged. The
catalogue is a per-screen inventory, and `ADR-0096` §Context's third diagnosis is that *"coverage
is per-screen, and a duel is not a screen"* — adding a sequence to a screen inventory would put the
walk in the one document the ADR names as unable to hold it.

`docs/test-plan.md` containing **zero** cases naming an all-in is cited by `ADR-0096` §Context as
**evidence for the diagnosis**, not as a deliverable. Beat 5 is walked by the auditor, not by a
catalogue case, and no case is added, changed or regraded by this story.

### What ships without a gate, and who owns it

Three things in this story cannot be gated by a command, and each ticket says so rather than
inventing a `grep` that passes either way:

- **That `size` actually resizes.** A gate that drove a browser would be a `verify:` waiting on a
  QA case (`ADR-0089` §2b). `TASK-121201`'s gates prove the verb exists, reaches the CDP attach
  path, names only the four licensed fields, and mentions `innerWidth`/`innerHeight`; whether the
  read-back assert fires is the reviewer's, against `ADR-0097` §2.
- **That the observer obeys its brief.** No gate can see that, and `TASK-120301` priced this class
  of check honestly: a string gate puts a sentence into a file in words a later editor has to
  **delete** rather than merely fail to add. That is worth having and is not worth more.
- **That the field discipline holds tomorrow.** `ADR-0097` §Consequences names it: adding
  `mobile: true` is a two-word change that flips an `R2` `not met` to a **false pass**, measured,
  and no CI job may guard it because §2b forbids one. `TASK-121201` gate 8 refuses the string in
  one file, which is a convention with a gate on it rather than a proof.

## The decision this story raises

**`DEC-098` — the architect's.** Is the audit rubric `ADR-0096` §2 itself, grown by an amending
ADR the way `ADR-0092` §2 and `ADR-0097` §4 both grew `ADR-0090` §2's declared-file set, or is it a
working document elsewhere that §2 founds?

Both readings have merged text behind them, which is why no ticket picks one. `ADR-0096` §7 lists
three deletions — *"the rubric, the audit focus from `qa-cycle`'s `SKILL.md`, and whatever
`DEC-097` decides carries the observer"* — **plus** *"the ADR that says why"*, and a rubric that is
a separate deletion from its ADR is a file. Against that: §2 calls the rubric *"merged, closed and
general"* and supplies it in full; §3's *"a criterion **merged** mid-invocation applies to the
**next** invocation"* is satisfied exactly by an amending ADR; and `ADR-0097`, whose stated job was
*"what text changes in which files to make the focus legal"*, names four files and **no file for
the rubric**. `DEC-097` was framed as a question about `ADR-0089` §2's three conditions, so the
rubric's home fell between the two decisions rather than being refused by either.

**It blocks nothing, and the tickets are written so that it cannot.** `ADR-0096` §2 is merged,
closed and citable by criterion id today, so `TASK-121203` and `TASK-121205` cite it and the first
audit round can run at either answer. What the answer costs is **one line in each of those two
files** if a working copy is decided on. What deciding it in a ticket would cost is a transcribed
second register that rots, which is why no ticket creates one — the default here is refusal, the
same default `ADR-0090` §2 sets for a new mention of the cycle.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-121201](../tasks/TASK-121201-the-driver-resizes-a-live-tab.md) | The driver resizes a live tab — a `size` verb over CDP | ready |
| [TASK-121202](../tasks/TASK-121202-the-audit-agent-the-walk-and-the-two-shapes.md) | The `audit` agent — the walk, the hands and the two shapes | backlog |
| [TASK-121203](../tasks/TASK-121203-what-audit-answers-and-the-three-it-may-propose.md) | What `audit` answers, and the three criteria it may propose | backlog |
| [TASK-121204](../tasks/TASK-121204-the-audit-focus-of-the-qa-cycle-skill.md) | The `audit` focus of the `qa-cycle` skill | backlog |
| [TASK-121205](../tasks/TASK-121205-the-rubric-classifier-and-the-ticket-it-promotes.md) | `qa-manager` — the rubric classifier and the ticket it promotes | backlog |
| [TASK-121206](../tasks/TASK-121206-the-audit-arithmetic-a-of-n-and-no-severity.md) | `qa-manager` — the audit arithmetic, `A(N)` and no severity | backlog |
| [TASK-121207](../tasks/TASK-121207-termination-counts-criteria-under-the-audit-focus.md) | `EPIC-12` §Termination counts criteria under the audit focus | backlog |

**Seven tickets, and the seams are files.** One script, two documents split in half apiece, one
skill and one epic section. The two halves each land a merged state that is safe on its own,
because **nothing dispatches `audit` until `TASK-121204` merges** — every intermediate state of
`.claude/agents/audit.md` is inert, which is what made the same split safe in `STORY-1207` and
unsafe in `STORY-1203`. `qa-manager.md` is split on the seam `STORY-1207` already used for it: the
classifier before the arithmetic.

`TASK-121201` is first because the observer's brief names the verb, and because it is the story's
one ticket whose gates run a binary rather than read a file.

## Acceptance criteria

- [ ] `node scripts/qa/drive.mjs <port> size <w> <h>` reaches the CDP attach path — it is a verb,
      not the usage fallback — `scripts/qa/drive.mjs` imports nothing outside `node:`, and it is the
      only file under `scripts/qa/` naming the `Emulation.` domain.
- [ ] `scripts/qa/drive.mjs` contains `deviceScaleFactor: 0` and `mobile: false`, and contains
      neither `mobile: true` nor `screenOrientation` (`ADR-0097` §§2, 5).
- [ ] `.claude/agents/audit.md` exists with `name: audit`, no `Write` and no `Edit` in its
      `tools:`, and a report shape carrying `PER-CRITERION:`, `PROPOSED CRITERIA:`, `FUNCTIONAL:`
      and `BLOCKED:`.
- [ ] `.claude/agents/audit.md` names `ADR-0096` as its budget and contains neither `blocker` nor
      `medium` — there is no severity under this focus (`ADR-0096` §5).
- [ ] Exactly **five** files under `.claude/skills` and `.claude/agents` name `qa-cycle`, and they
      are `agents/qa.md`, `agents/uat.md`, `agents/audit.md`, `skills/qa-cases/SKILL.md` and
      `skills/qa-cycle/SKILL.md` (`ADR-0097` §4).
- [ ] `.claude/agents/qa.md` is byte-identical to its state at `f8383c4e` —
      `sha256 eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42`.
- [ ] `.claude/agents/uat.md` is byte-identical to its state at `f8383c4e` —
      `sha256 d6c1cd3f619356ca3f0a9f4af4ad9441854818c1d67913efd271c5d378664cce`.
- [ ] `.claude/agents/qa-manager.md` contains the string `qa-cycle` **nowhere**, and its
      `## The UAT focus` and `## The UAT arithmetic` sections survive with their three-question cap.
- [ ] `.claude/skills/qa-cycle/SKILL.md` names `/qa-cycle audit`, `audit.md`, `A(N)`, `390 664` and
      `720 900`.
- [ ] `EPIC-12` §Termination names `A(N-1)` and the rubric's order inside the section itself.
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0.

## Out of scope

- **Running an audit round.** This story builds the machinery. The first round is the human typing
  `/qa-cycle audit <scope>`, and it produces its own round story under `EPIC-12` — `STORY-1213` or
  later, never this one.
- **Creating a rubric document, or transcribing `ADR-0096` §2's five criteria into any file.**
  `DEC-098` above. Until it is answered the rubric is cited where it is, and a copy would be the
  second register `TASK-120705` refused one focus earlier.
- **Adding a sixth criterion, or editing any of the five.** `ADR-0096` §3: the rubric is frozen for
  the invocation, no round may grow it, and a proposed criterion routes as `ADR-0092` §5 routes a
  question. A planner is not a round and has even less licence.
- **Any change to `docs/test-plan.md`.** The audit's route map is `ADR-0096` §1's beat table, and
  the catalogue is per-screen by construction — argued above rather than left as a silence. No case
  is added, changed or regraded, and no `expect` column moves.
- **Any change to `.claude/agents/qa.md` or `.claude/agents/uat.md`.** `ADR-0096` §2 freezes
  `ADR-0092` §3 byte-unchanged for both focuses; five tickets pin both `sha256`s.
- **An `audit-cycle` skill, an `audit-manager` agent, or a `Write` tool on the observer.**
  `ADR-0092` §8 applies unamended (`ADR-0097` §4): one manager, one ledger, and two filers over one
  product break dedupe across three focuses.
- **A sixth declared file.** `ADR-0097` §4 declares exactly five; the check is carried from both
  directions.
- **Device emulation, touch emulation, a user-agent override, `screenOrientation`, or any
  `deviceScaleFactor` other than `0`.** `ADR-0097` §§2, 5 and §Consequences: measured,
  `mobile: true` widens the layout viewport 390 → 520 and turns an `R2` failure into a **false
  pass**. The day the rubric wants a criterion those fields would answer, `ADR-0089` §§2a and 3
  return as a new `DEC`.
- **`Browser.setWindowBounds`, or any window-sizing route.** `ADR-0097` §Alternatives 1: Chrome
  clamps a window to 500 px wide and adds 87 px of chrome, so neither shape is expressible.
- **Any npm package, any `package.json`, any lockfile, and any image-comparison tooling.**
  `ADR-0089` §2a, `ADR-0092` §2a, `ADR-0088` §1.
- **A landscape shape, a tablet shape, or a third viewport.** `ADR-0096` §4's table is complete and
  `ADR-0097` §5 records the human's *"we are ok to support only one orientation for mobile form
  factor."*
- **Any `verify:` block that waits on a QA, UAT or audit case, and any citation of a round as
  coverage.** `ADR-0089` §§2b, 2c. Every gate in this story reads a committed file or runs the
  driver against a port with no browser on it.
- **A baseline-round exemption under this focus.** `ADR-0096` §5 lists the audit's termination rules
  and includes none, and `ADR-0092` §6's rule is defined over a screen becoming
  conformance-judgeable through a merged card — which no audit round measures. `DEC-093`, which
  asks whether that rule extends to other instruments, is untouched here.
- **Answering `DEC-088`, `DEC-093`, `DEC-087` or any open `DEC`.** None blocks this story;
  `DEC-088`'s promotion-slot ordering is the UAT gate and `ADR-0096` §5 says so in as many words.
- **Ticking any `EPIC-12` Definition-of-done box.** None of them asks for this machinery, and the
  one about telling a harness defect from a product defect still needs a round.
