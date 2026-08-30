---
id: STORY-1207
title: The UAT focus — the observer, the harness verb, the route map and what may be filed
type: story
status: ready
parent: EPIC-12
labels: [process, qa, uat, meta]
depends_on: []
---

## Goal

`/qa-cycle uat <scope>` exists: a second focus of the same cycle, with its own observer
(`.claude/agents/uat.md`), its own hands (a `shot` verb), its own route map (`docs/test-plan.md`
§*UAT*), and a `qa-manager` that triages both focuses on one ledger under
[`ADR-0092`](../../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md)'s
classifier and arithmetic. Nothing here runs a round; this story builds the machinery a round
needs.

## This is not a round story

`EPIC-12`'s Stories table says *"one story per QA round; the round number lives in the story, not
the id."* **This story breaks that convention**, the way `STORY-1201` breaks it by being a
retrospective record and `STORY-1203`/`STORY-1204` break it by building and first running the
authoring skill. It brings no stack up, starts no browser and reports no `B(N)`. The epic's table
gains a row saying so, in the same PR, rather than leaving a reader to notice — this is the third
non-round story and the numbering has already moved twice.

## Why

`ADR-0092` merged on 2026-08-30 as `cfcc6a4e` and **names its own deliverables** in §8: the
`qa-cycle` skill's `uat` focus, `qa-manager`'s promotion gate and exclusions, a new `uat.md`, a
`shot` verb, and the test-plan section — *"land through the planner's tickets from this ADR,
through the ordinary gate."* This is that split. Nothing UAT exists in the repository today:
no `.claude/agents/uat.md`, no `uat` focus in the skill, no `shot` verb, no UAT section in the
catalogue, and `.claude/agents/qa-manager.md` mentions neither focus.

One deliverable is not from the ADR. **`notify.py state --clear` does not clear everything**,
measured on 2026-08-30 immediately after a `/qa-cycle regression` teardown ran it as its last
step: `cron_armed` stayed `true` while that teardown had just deleted the heartbeat cron, and a
`note` survived from an entirely earlier session. It belongs here rather than in a story of its
own because `ADR-0092` made UAT a **second focus of the same cycle**: one run state, one `Stop`
hook, one heartbeat cron serving both. A breadcrumb that lies after a QA teardown lies identically
after a UAT teardown, and this is the story that makes the shared apparatus carry a second focus.

## Design notes

Everything below is settled by a merged ADR. Nothing in this story is open, and the two places
where that was not obvious are argued rather than asserted.

**Ordering is a requirement, not a convenience.** `ADR-0092` §7: the catalogue's UAT section is
*"landed through reviewed PRs **before the first UAT round**."* The chain here is linear and runs
harness → catalogue → observer → focus → manager, so that no ticket ever merges naming something
that does not exist: the observer's brief names the `shot` verb and the §*UAT* section as its
budget, and the skill's focus dispatches the observer. Every ticket's dependency is the one before
it; exactly one is `ready`.

**The QA focus never chains into the UAT focus.** One turn that runs both is a skill running a
cycle as one of its steps — `ADR-0089` §2b failing, `ADR-0090`'s exact holding. Neither focus's
report prints the other's command. And **a preceding QA cycle is the human's practice, never a
checked precondition**: a skill that verified *"has a QA round passed at this commit?"* would cite
a round as a gate, which §2c forbids. `ADR-0092` §1 says all three out loud *"so no helpful
someone adds the check"*, and the tickets carry them as refusals rather than as features.

**`.claude/agents/qa.md` stands byte-unchanged.** `ADR-0092` §8 says so in as many words — its
refusal list (*"Do not report: wording you dislike, spacing, colour, anything `EPIC-06` owns"*) is
load-bearing for function rounds and is UAT's entire subject, and two files make the leak
structurally impossible in both directions. A ticket that edits `qa.md` is wrong; four tickets
carry its `sha256` as a gate.

**`.claude/agents/qa-manager.md` still may not name `qa-cycle`.** `ADR-0090` §2 declared three
files; `ADR-0092` §2 grows the set to **four**, adding `agents/uat.md` **mention-only**, and
changes nothing else. `qa-manager` names the cycle nowhere today and gains no licence to — which
is the most likely accident in `TASK-120708`/`TASK-120709`, since describing the `uat` focus
invites writing `/qa-cycle uat`. Both tickets gate on the absence, from both directions.

**The classifier is the fence around taste.** An observation may be filed as a **finding** only
when it contradicts something merged — a card, `design/tokens/tokens.css`, an owned literal, an
ADR section, a `docs/duel-rules.md` heading, a `docs/vision.md` sentence. An observation with no
merged source to contradict is a **question**, and `QUESTIONS` → `qa-manager` → a `DEC` for the
`product-owner` is its only route (`ADR-0092` §§3, 5). This is `ADR-0090` §4 transposed from
authoring to observing.

**`B(N)` counts product defects alone** — not harness defects (`ADR-0089` §4), not missing cards
(registered `ADR-0091` §5 debt being collected), not decision-born improvements. A **baseline
round** — one in which a screen becomes conformance-judgeable for the first time — skips rule 4's
comparison, exactly as round 1 does; rule 5's three-round budget binds regardless. Every UAT
verdict line is qualified **inline**: `PASS (conformance unjudged on 6 of 7 screens)`.

**No image-comparison tooling enters this repository, ever** (`ADR-0092` §2a). A screenshot is
read by a reader, never diffed by a program; screenshots live under the round's `mktemp -d` and
are **never committed**; the durable evidence in a finding is text, quoted verbatim. The day pixel
tooling is wanted, §2a is failing and the question returns as a new `DEC`.

### Two screens this harness cannot reach, and why that is not a `DEC`

`ADR-0091` §5 owes cards for six shipped screens. `docs/test-plan.md` §*What this catalogue does
not cover* puts two of them — `#/verify` and `#/reset` — permanently out of the harness's reach,
and not for want of a case: a machine with no mail transport binds `NoRecoveryMailer` (`ADR-0031`
§7), the recovery tokens are stored only as `BYTEA` hashes (`V8__recovery_email.sql`), and **no
mailed link ever arrives for a driver to follow**. `ADR-0092` §7 makes the catalogue UAT's route
map, so UAT cannot walk to either — and would therefore file a missing-card finding for a card
that, once composed, no round could ever conformance-check.

**Three merged clauses settle it, so nothing here is invented and no `DEC` is registered:**

- `ADR-0092` §4 files a `high` for *"a screen **in scope** with no merged card"*. A screen no route
  reaches is not in a round's scope, so no missing-card finding is filed for these two.
- `ADR-0092` §6 already gives the per-screen table a cell value for exactly this — `out of scope`,
  beside `judged` and `BLOCKED — no card` — so a round records them as *not walked* rather than as
  passed or as a defect a harness could have caught.
- `ADR-0092` §4 narrows only the debt's **vehicle**: *"UAT rounds file the cards their scopes
  reach first, and the `EPIC-06` retrofit story, when split, covers only the slugs still
  cardless."* These two slugs are that remainder. Their cards are still owed under `ADR-0091` §5,
  and a card is a reference a human accepts at the pane, which needs no driver.

What is left is an **asymmetry**, not a gap: a card for an unwalkable screen will never have a
conformance check behind it. That is what every card was before this story existed — `ADR-0089`
§2c already forbids reading any round as the thing that validated one. `TASK-120703` writes the
asymmetry into the inventory in three sentences, so it is on the record rather than discovered by
whoever composes those two cards.

### What `--clear` keeps, decided here so no ticket has to

`state --clear` leaves exactly one field — `last_report_at`, the heartbeat's dedupe stamp — and
removes every other, `epics` and `started_at` included. `last_report_at` stays because it is
cross-run bookkeeping: clearing it would let the next run's first heartbeat fire immediately as
though none had ever been sent, a duplicate report about a run that has ended. `cron_armed`
becomes **unknown** rather than `false`, because `--clear` cannot know whether its caller has
deleted the cron yet — `qa-cycle`'s teardown runs `CronDelete` *after* it — and the run state's own
three-state design exists for exactly that (`run_state.py`: *"True, False, or None meaning
unknown — three states, never two"*). Asserting `not-armed` on a clear would be the optimism
`qa-cycle` forbids at the arming end.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-120701](../tasks/TASK-120701-state-clear-leaves-only-the-heartbeats-dedupe-stamp.md) | `state --clear` leaves only the heartbeat's dedupe stamp | ready |
| [TASK-120702](../tasks/TASK-120702-the-driver-captures-a-screen.md) | The driver captures a screen — a `shot` verb over CDP | backlog |
| [TASK-120703](../tasks/TASK-120703-the-uat-screen-inventory.md) | The UAT screen inventory — the route map a round walks | backlog |
| [TASK-120704](../tasks/TASK-120704-the-standing-questions-and-what-uat-does-not-cover.md) | The standing questions, and what UAT does not cover | backlog |
| [TASK-120705](../tasks/TASK-120705-the-uat-agent-the-role-and-the-hands.md) | The `uat` agent — the role, the refusals and the hands | backlog |
| [TASK-120706](../tasks/TASK-120706-what-uat-may-file-and-what-it-may-only-ask.md) | What `uat` may file, and what it may only ask | backlog |
| [TASK-120707](../tasks/TASK-120707-the-uat-focus-of-the-qa-cycle-skill.md) | The `uat` focus of the `qa-cycle` skill | backlog |
| [TASK-120708](../tasks/TASK-120708-the-merged-source-classifier-and-the-promotion-gate.md) | `qa-manager` — the merged-source classifier and the promotion gate | backlog |
| [TASK-120709](../tasks/TASK-120709-two-more-exclusions-a-baseline-round-and-a-qualified-verdict.md) | `qa-manager` — two more exclusions, a baseline round and a qualified verdict | backlog |

**Nine tickets, and the seams are files.** Four documents, one script and one Python change, with
the two largest documents split in half because each half is a merged state that is safe on its
own: the catalogue's inventory before its questions, and the observer's hands before its judgment.
Nothing dispatches `uat` until `TASK-120707` merges, so every intermediate state of
`.claude/agents/uat.md` is inert — which is what makes that split safe where `STORY-1203`'s was
not. `TASK-120701` is first because it is independent of the UAT chain and is the story's one
genuinely unit-testable ticket; it blocks nothing and nothing blocks it.

## Acceptance criteria

- [ ] `node scripts/qa/drive.mjs <port> shot <path>` reaches the CDP attach path — it is a verb,
      not the usage fallback — and `scripts/qa/drive.mjs` imports nothing outside `node:`.
- [ ] `docs/test-plan.md` carries a `## UAT` section whose inventory has 13 five-column rows, names
      every member of the `Screen` union, cites only card paths that exist on disk and only case
      ids the catalogue defines, marks exactly two rows `not walked` with `ADR-0031` §7 named, and
      whose four standing questions each cite `ADR-0092`.
- [ ] No `EPIC-04`, `EPIC-05` or `EPIC-06` row in §*Not yet written* still says *"until its first
      round"*.
- [ ] `.claude/agents/uat.md` exists with `name: uat`, no `Write` and no `Edit` in its `tools:`,
      and a report shape carrying `PER-SCREEN:`, `FINDINGS:`, `QUESTIONS:` and `BLOCKED:`.
- [ ] Exactly **four** files under `.claude/skills` and `.claude/agents` name `qa-cycle`, and they
      are `agents/qa.md`, `agents/uat.md`, `skills/qa-cases/SKILL.md` and `skills/qa-cycle/SKILL.md`
      (`ADR-0092` §2).
- [ ] `.claude/agents/qa.md` is byte-identical to its state at `cfcc6a4e` —
      `sha256 eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42`.
- [ ] `.claude/agents/qa-manager.md` contains the string `qa-cycle` **nowhere**.
- [ ] After `notify.py state --clear`, the run state holds `last_report_at` and nothing else, and
      `test_cli.py` runs 13 tests.
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0.

## Out of scope

- **Running a UAT round.** This story builds the machinery. The first round is the human typing
  `/qa-cycle uat <scope>`, and it produces its own round story under `EPIC-12` — `STORY-1208` or
  later, never this one.
- **Composing any of the six missing design cards.** A missing card is a `high` finding whose
  repair *is* the card, filed by `qa-manager` at a round's triage and dispatched per `ADR-0091`
  §3. Filing one here would pre-empt the round and the card-path dedupe key (`ADR-0092` §4).
- **`ADR-0091` §5's `EPIC-06` retrofit story.** The human's run-now call narrows it to the slugs
  still cardless when it is split — `verify` and `reset` among them; §5's register and every other
  sentence of it are unchanged, and nothing here reopens `EPIC-06`.
- **Deleting either authored suite's `Provisional` line.** `ADR-0090` §5 gives that to the round
  record that first ran it, which must also name the cases that round corrected — and three of
  those corrections have not merged. It belongs to `STORY-1205`/`STORY-1206`.
- **Any change to `.claude/agents/qa.md`.** `ADR-0092` §8: byte-unchanged.
- **A `uat-cycle` skill or a `uat-manager` agent.** `ADR-0092` §8 refused both: a second copy of
  the stopping rules drifts, and two filers over one product break dedupe across focuses.
- **Any image-comparison, screenshot-diff or pixel-threshold tool, and any new dependency in any
  module.** `ADR-0089` §2a, restated by `ADR-0092` §2a. A diff threshold is an opinion in a verify
  block.
- **Committing a screenshot.** They are working artefacts under the round's temp directory.
- **Any `verify:` block that waits on a QA or UAT case, and any citation of a round as coverage.**
  `ADR-0089` §§2b, 2c. Every gate in this story reads a committed file, runs a unit test, or runs
  the driver against a port with no browser on it.
- **Retrofitting `SMOKE` or `CORE` rows with a `source` column, or regrading any `expect` on UX.**
  `ADR-0092` §7 and `ADR-0090` §4's no-retrofit precedent. A case graded on two rubrics is
  ambiguous on both.
- **Answering `DEC-086`** — the written bar for *"ready for real users"*. Registered open for the
  product owner by `ADR-0092`'s own PR; it blocks nothing here and UAT runs without it.
- **Ticking any `EPIC-12` Definition-of-done box.** None of them asks for this machinery, and the
  one about telling a harness defect from a product defect still needs a round.
