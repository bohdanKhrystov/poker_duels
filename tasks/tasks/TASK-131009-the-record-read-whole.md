---
schema: 2
id: TASK-131009
title: The record read whole, and every finding given an owner
type: task
status: done
parent: STORY-1310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [qa, refresh]
depends_on: [TASK-131008]
verify:
  - awk '{ n += gsub(/NOT-YET-DRIVEN/, "&") } END { exit (n > 0) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^## What this found/ { n++ } END { exit (n != 1) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '{ n += gsub(/- \[ \]/, "&") } END { exit (n != 0) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '{ n += gsub(/- \[x\]/, "&") } END { exit (n < 6) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1310*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`STORY-1310` closes on its own evidence: the seven rows are read together, every finding is given a
named owner, and the story's six acceptance criteria are answered one by one rather than declared.

## Why the story does not end at the last row

`EPIC-13`'s Definition of done says the symptom is *"either reproduced and fixed, or recorded as not
reproducible with the paths that were tried written down"*, and `ADR-0112` §6 adds the sentence this
whole story exists for: ***"A dismissal without the attempt does not satisfy the DoD row."*** Seven
filled rows are the attempt. What the DoD asks for on top is the **reading** — what the attempt
found, and what happens to each thing it found. Left unwritten, the story merges as seven sentences
nobody joins up, and the epic closes on a table rather than on a conclusion.

## A reconciliation this ticket owes: why `P1`'s two drives disagreed

`P1` carries two honest readings that contradict each other — one found a reload on the result screen
landing on the lobby and staying, the other found the result screen surviving. `TASK-131004`'s
`delayed` leg then observed what neither `P1` drive did:

**On `delayed 300ms`, reloading the held result screen paints the lobby first and recovers to the
result within seconds** — reproduced on two separate reloads. On `bare` the same reload went straight
to the result, with no such flash.

That reframes the disagreement. The lobby may not be a *different outcome*; it may be the **first
paint of the same one**, visible when the network is slow and gone before a sampler catches it when
the network is fast. `P1`'s first drive armed `record`, read one frame — the lobby — and never waited
for a recovery, which is precisely what this would look like.

It also weakens the winner/account-offer hypothesis on `TASK-131003`: the variable may be **when the
observation happened relative to a recovery**, not what was on screen.

**Do not close this by asserting it.** Settle it by driving `P1`'s bare reload again and *waiting*
for the result screen, then say whether it recovers or genuinely stays on the lobby. If the record
cannot be reconciled, say so plainly — an unexplained disagreement honestly recorded is worth more
than a tidy story.

## A second finding this ticket owes an owner: the resume paints a stale room state

`P2`'s `delayed 300ms` leg found that `ADR-0112` §6's *"no lobby on the way"* **fails under latency**.
Reloading mid-runout, `open`'s own first paint is the **lobby**; then a lobby with the profile loaded;
then a **stale `Waiting for your rival` screen naming the room that has already finished**; and only
then the true `Victory`. On `bare` none of that is visible — the reload settles immediately.

Two things to separate when you give this an owner:

- **`ADR-0102` §5 arguably still holds.** Each screen is a state the server did state, arriving late
  rather than a fact the client invented. The driver made that distinction rather than collapsing it,
  and it is the right one to argue from.
- **`ADR-0112` §6 does not hold.** It says no lobby appears on the way. One does, twice, plus a
  waiting-room screen for a room that is over.

Whether a player briefly seeing their finished duel described as *waiting for your rival* is
acceptable-because-late or wrong-because-false is a **product** question, not a mechanism one. If the
record concludes it is wrong, that is a `DEC` for the product owner and this ticket should raise it
rather than describe it.

**And it bears on `P1`.** A client that visibly paints lobby-shaped screens before self-correcting is
a mechanism that produces either of `P1`'s contradictory readings depending on when a drive looked.
That strengthens the reconciliation already recorded above — but the driver was careful not to claim
it settles `P1`, and neither should this ticket without driving it.

**One measurement explicitly not claimed:** how long the recovery takes. The interval between arming
`record` and reading `frames` was dominated by agent-loop latency between shell invocations, not by
the app. The sequence and the endpoint are observed; the duration is not.

## Files

| File | Action |
| --- | --- |
| `tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md` | modify |

Read: `docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md` §6,
`docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §§5 and 6, and
`tasks/stories/STORY-1311-only-a-running-duel-refuses-another-screen.md`. Nothing else, and no
source is opened.

## Scope

- Add **`## What this found`** to the story, after the record table. Three or four paragraphs, no
  more: what the seven readings say together, what reproduced, what did not, and what could not be
  driven.
- **Give every finding an owner**, one row per finding:

  | column | what goes in it |
  | --- | --- |
  | finding | one sentence, naming the `P` row it came from |
  | owner | `STORY-1311`, a ticket to file, or a `DEC` to register with whose it is |
  | the gate it would need | a **non-browser** gate — the test file and the test name that would fail on the defect and pass on the repair — or, where only a browser can see it, `ADR-0089` §4's by-hand reproduction requirement written out |

  This is the story's fifth acceptance criterion in substance. **The ticket files nothing itself**:
  writing a brief complete enough to be filed verbatim is this ticket's work, and cutting the ticket
  file is the driver's next act, because a variable number of new files cannot be a fixed *Files*
  table. Say which in the row.
- **Tick the story's six acceptance criteria**, each with the sentence that answers it. Where one is
  met with a qualification — the mailed-link token half, which `TASK-131008` records as undrivable —
  the tick carries the qualification inline rather than standing bare.
- **Route `ADR-0114` §6's named residual explicitly**, whether or not it was seen. That ADR says a
  resume into a `PLAYING` room reads `waiting` for one render, that the client cannot close it
  without a wire change `ADR-0112` §7 forbids, and that *"if the owed drive observes it, that is a
  new `DEC` and not a guess made here."* The story must say, in one sentence, whether any of the
  seven readings observed it. **If it was observed, register the next free `DEC` in
  `docs/adr/README.md`'s `## Open decisions` — the architect's — and name it in the row.**
- **Say what the story does not claim.** `ADR-0089` §2c: seven statements about one run, on one
  machine, at one commit; no coverage; nothing here may be cited in a Definition of done as a check
  that passed.

## Out of scope

- **Any repair, and any client source.** `STORY-1311` owns the guard, `ADR-0114` owns its mechanism.
- **Filing the finding tickets.** Briefs, not files — for the reason given above.
- **Re-driving anything.** If a row looks wrong, say so in `## What this found` and let the driver
  decide; a re-drive is a new ticket, not this one widening.
- **Adding an `EPIC-13` suite to `docs/test-plan.md`.** Tempting, and wrong here on two merged
  grounds. `ADR-0090` §5 gives a suite two lawful origins and neither is *a story's findings*; and
  `STORY-1311` and `STORY-1302`'s successors are about to change the behaviour four of these rows
  describe, so cases written now would be stale before the next round runs them — the rot
  `ADR-0089` §4 exists to keep out of `B(N)`. If a case is worth having afterwards, it is
  `qa-cases`' to author from the merged ADRs, not this ticket's to leave behind.
- **`/qa-cycle`, `A(N)`/`B(N)`, and any verdict table** (`ADR-0089` §2b).

## Tests

No test can be written, for `TASK-131003`'s merged reason. Seven gates, and what each is worth:

| Gate | Proves | Today |
| --- | --- | --- |
| zero placeholders remain | every path has a result — re-checked here, because this is the ticket that ticks the criterion claiming it | green if `TASK-131008` merged; **red** if a row regressed |
| seven `P` rows stand | none was deleted on the way | green — a regression guard |
| `## What this found` exists exactly once | the reading was written | **red** |
| no unticked criterion remains in the story | all six were answered rather than left | **red** |
| at least six ticked criteria | the list was answered rather than deleted, which is the cheapest way to pass the gate above | **red** |
| no `verify:` block under `tasks/tasks/TASK-1310*.md` names `drive.mjs` or `stack.sh` | `ADR-0089` §2b held across all nine tickets — the story's fourth criterion, and the one gate in this story that checks a claim rather than a shape | green — the claim, checked |
| `lint_tickets.py` | the story's sixth criterion | green |

**Four of the seven check a document's shape and the ticket says so rather than dressing it up.** A
`## What this found` heading with an empty section passes gate three; six ticks next to sentences
that answer nothing pass gates four and five. What those gates buy is that the *shape* of an honest
close cannot be skipped — the judgement that the content is honest is the reviewer's, reading the
seven rows against the seven PR bodies that produced them.

## Acceptance criteria

- [ ] `## What this found` reads the seven rows together and says what reproduced, what did not, and
      what could not be driven
- [ ] Every finding has a row naming its owner and a **non-browser** gate — a test file and a test
      name — or `ADR-0089` §4's by-hand reproduction requirement where only a browser can see it
- [ ] Each of the story's six acceptance criteria is ticked with the sentence that answers it, and
      the mailed-link one carries its undrivable half inline rather than bare
- [ ] `ADR-0114` §6's one-render `waiting` residual is answered in one sentence — observed or not —
      and a `DEC` is registered if it was
- [ ] The story states, in its own words, that these are seven statements about one run on one
      machine at one commit, and are cited as coverage nowhere (`ADR-0089` §2c)
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
