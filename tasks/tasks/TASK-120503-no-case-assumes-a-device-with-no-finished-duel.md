---
schema: 2
id: TASK-120503
title: No case assumes a device with no finished duel
type: task
status: done
parent: STORY-1205
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, harness]
depends_on: []
verify:
  - test 0 -eq "$(grep -c Provisional docs/test-plan.md)"
  - awk -F'|' '/^\| `04-01` \|/ { if ($4 ~ /No duels yet/) bad=1 } END { exit bad }' docs/test-plan.md
  - awk -F'|' '/^\| `04-02` \|/ { if ($4 ~ /Duel coins/) bad=1 } END { exit bad }' docs/test-plan.md
  - awk -F'|' '/^\| `05-01` \|/ { if ($3 ~ /have no place/) bad=1 } END { exit bad }' docs/test-plan.md
  - awk -F'|' '/^\| `05-03` \|/ { if ($3 ~ /both no-place/) bad=1 } END { exit bad }' docs/test-plan.md
  - grep -qF '`04-01`' docs/test-plan.md && grep -qF '`04-02`' docs/test-plan.md && grep -qF '`05-01`' docs/test-plan.md && grep -qF '`05-02`' docs/test-plan.md && grep -qF '`05-03`' docs/test-plan.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The `EPIC-04` and `EPIC-05` suites are executable under every scope that runs them, instead of
only under a scope that never presents a device with finished duels — and the two
`> **Provisional**` notes are gone, because the first round has now happened.

## This is a harness defect, and what that means here

Filed under `ADR-0089` §4 and `EPIC-12` §Termination rule 6. It is **excluded from `B(1)`**, and
**no production code may be changed by this ticket** — the fix is one document. A `## Files` table
naming anything outside `docs/` is grounds to reject the diff on sight.

## The defect

Round 1 of `/qa-cycle regression`, 2026-08-29, commit `fe4bbf2a`. `04-01`, `05-01` and `05-02`
failed; `05-03` was passed only by a deviation from its written steps; `04-02` carries the same
rot behind a genuine product defect.

**Five cases assume a device that has finished no duel.** Under `regression` scope that device
cannot exist: the suites run in catalogue order, CORE necessarily plays duels on A and B before
EPIC-04 is reached, and a round allocates **two** profiles for the whole session.

| case | the assumption, quoted from its own row |
| --- | --- |
| `04-01` | `expect` — *"the duels screen renders with `No duels yet.`"* |
| `04-02` | `expect` — *"it is `−1 Duel coins` for a browser whose only duel was a loss"* |
| `05-01` | `do` — `A wait "You have no place on this season's leaderboard."` |
| `05-02` | `expect` — *"the self line is exactly `You have no place on this season's leaderboard.`"* |
| `05-03` | `do` — *"read both self lines (**both no-place**)"*, and `expect`'s absolute `on 1 duel coin.` |

**`04-02` is the urgent one.** `TASK-120501` repairs the product defect that case caught. The
moment it lands, a round-2 retest of `04-02` fails on `−1 Duel coins` anyway — inflating `B(2)` on
a catalogue artifact and risking a `STOP_DIVERGING` verdict on a product that just got better.
That is the exact failure `ADR-0089` §4 exists to prevent, arriving through the back door.

**`05-03` is the instructive one.** `qa` marked it **passed**, by checking the *delta* against
`duel_result` rather than the absolutes the case names. The judgement was right and the deviation
was silent — the same failure mode `STORY-1202` filed for `SMK-03`. A catalogue that describes
something other than what is executed rots exactly this way.

`EPIC-05`'s own preamble already forbids this: *"No case asserts an absolute rank. The database
persists between rounds…"* The cases assert one anyway. Both suites were authored from merged
sources and were never run, which is what `> **Provisional**` says and what `ADR-0090` §5
predicted.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |

## Scope

- **Delete both `> **Provisional**` notes.** `ADR-0090` §5 makes this the first round record's job:
  *"a provisional suite carries a line the first round record deletes."* Round 1 has run.
- **Rewrite the five rows so their observation is history-independent**, keeping each case's real
  subject:
  - `04-01` — the subject is *no refusal of an account-less device*, which is what its own
    `fails if` states. Assert the strip carries `<n> Duel coins` and the duels screen renders its
    own frame; do not assert how many duels there are.
  - `04-02` — assert the balance string is **unchanged across the claim**. That is what the case
    is for (`ADR-0014`: a claim must not rewrite or clamp a balance), and it needs no absolute.
  - `05-01` — the subject is *the ladder opens from the first screen and closes again*. Wait on a
    string the ladder always shows, not on the no-place sentence.
  - `05-02` — the subject is *a player who finished no duel is given no place*. Make the
    precondition explicit and derive it: read `GET /api/standings` for this device, the way `05-05`
    already reads it, and assert the self line agrees with the response either way.
  - `05-03` — assert the **delta** of one duel is exactly ±1 against the `duel_result` sum, which
    is what `qa` actually checked and what `ADR-0061` §4 actually promises.
- **Add one sentence to the `EPIC-04` and `EPIC-05` preambles** saying that a case may not assume a
  device with no finished duel, because scope order decides what has already been played and a
  round has two profiles for the whole session. One sentence, so the next authored suite does not
  rediscover it.
- Every case keeps its `source` column, and no `source` may be weakened to make a rewrite easier
  (`ADR-0090` §4).

## Out of scope

- **Any file outside `docs/test-plan.md`.** Named again because it is the rule this ticket exists
  under, not a preference.
- **Deleting any of the five cases.** The `verify:` block's last command fails if an id disappears.
  A case rewritten to be scope-independent is the deliverable; a case removed is the cheap fix.
- **`04-03`, `04-04` and `04-05`.** They were blocked by a product defect, not by this one, and
  `TASK-120501` unblocks them. Do not touch their rows.
- **`SMOKE` and `CORE`.** `ADR-0090` §4 says they are not retrofitted, and none of them assumes a
  duel-free device.
- **The `CORE-03` third-profile gap.** That is `TASK-120504`, which lands after this one on the
  same file.

## Tests

None — the deliverable is document text, so the gates are structural checks over that text, the
shape `TASK-120201` established. Each reads one pipe-delimited cell of one row.

| Gate | Proves | Today |
| --- | --- | --- |
| `! grep -qF 'Provisional'` | both notes are gone | **exits 1** — two notes are present |
| `04-01` `$4 !~ /No duels yet/` | the expect no longer pins an empty history | **exits 1** |
| `04-02` `$4 !~ /Duel coins/` | the expect no longer pins an absolute balance | **exits 1** |
| `05-01` `$3 !~ /have no place/` | the do no longer waits on the no-place sentence | **exits 1** |
| `05-03` `$3 !~ /both no-place/` | the do no longer requires a duel-free pair | **exits 1** |
| the five ids still present | nothing was deleted to satisfy the five above | exits 0 — it must keep doing so |

All six were run against `docs/test-plan.md` at commit `fe4bbf2a`; the first five exit `1` and the
sixth exits `0`.

## Acceptance criteria

- [ ] `docs/test-plan.md` contains no `Provisional` note.
- [ ] None of the five rows asserts an absolute balance, rank or duel count.
- [ ] All five case ids are still present, each with a `do`, an `expect`, a `fails if` and a
      `source`.
- [ ] The `EPIC-04` and `EPIC-05` preambles each gained the sentence about scope order.
- [ ] The diff touches exactly one file, and it is `docs/test-plan.md`.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
