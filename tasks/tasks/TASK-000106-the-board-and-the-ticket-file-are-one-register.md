---
schema: 2
id: TASK-000106
title: The board and the ticket file are one register, and the linter reads both
type: task
status: ready
parent: STORY-0001
module: process
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [process, tooling, tickets]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
  - cd .github/scripts && python3 -m unittest test_lint_tickets.BoardRegisterTest.test_a_board_row_that_agrees_with_its_file_passes
  - cd .github/scripts && python3 -m unittest test_lint_tickets.BoardRegisterTest.test_a_board_row_that_disagrees_with_its_file_fails
  - cd .github/scripts && python3 -m unittest test_lint_tickets.BoardRegisterTest.test_a_task_file_with_no_board_row_fails
  - cd .github/scripts && python3 -m unittest test_lint_tickets.BoardRegisterTest.test_a_status_cell_with_prose_after_it_reads_only_the_status
  - cd .github/scripts && python3 -m unittest test_lint_tickets.BoardRegisterTest.test_a_struck_through_dropped_cell_agrees_with_its_file
  - cd .github/scripts && python3 -m unittest test_lint_tickets.BoardRegisterTest.test_an_id_named_only_in_prose_is_not_a_board_row
  - cd .github/scripts && python3 -m unittest test_lint_tickets.BoardRegisterTest
---

## Goal

A ticket's `status:` and its `tasks/BOARD.md` cell cannot disagree without the `tickets` workflow
going red.

## Why

`lint_tickets.py` reads 869 ticket files and **never opens the board**. Every status in this
repository is therefore written down twice, in two files, with nothing keeping them in step — and the
drift is silent, which is the only kind that survives.

It has cost real work twice already:

- **A board row said `**ready**` while the file said `backlog`.** `--startable` derives startability
  from the *file*, so the ticket was invisible to the driver and simply did not get started. Nothing
  was red; the run just stalled.
- **A story split wrote 29 ticket files and no board rows at all.** Nothing went red then either.

And it is drifted **today**, measured on `develop` at `04f5b26a` with a throwaway script:

| Ticket | `tasks/BOARD.md` | the file |
| --- | --- | --- |
| `TASK-000101` | `**done**` (line 145) | `in-review` |
| `TASK-000103` | `**in-review**` (line 147) | `in-progress` |

Both have been wrong since `EPIC-00`. They are left in place deliberately so that **this ticket's own
check is what finds them**, rather than a planner quietly repairing two cells and leaving the gate
unwritten.

## What the same measurement found, so the check can land green

- **869 task files, 869 board task rows — a clean 1:1.** No task file is missing a row, no row points
  at a file that does not exist, and no id has two rows. So the coverage half of the check costs
  nothing to switch on.
- **Exactly two status disagreements**, the ones above.
- **One near-miss that a careless normaliser turns into a false third.** `TASK-021113`'s cell reads
  `~~dropped~~` and its file reads `dropped`. Strip `*` and backticks but not `~` and the check
  reports a phantom on a correct row. There is a test for it below.

## Files

| File | Action |
| --- | --- |
| `.github/scripts/lint_tickets.py` | modify |
| `.github/scripts/test_lint_tickets.py` | create |
| `.github/workflows/tickets.yml` | modify |

`tasks/BOARD.md` is **not** a Files row: it is part of every ticket's Definition of Done
([`tasks/README.md`](../README.md)), and no ticket in this repository budgets it. The two rows above
are reconciled there under that standard update — see `## Scope`, last bullet, which is not extra
scope but the new check's first finding. Read, and do not edit: `tasks/README.md` *Lifecycle*;
`scripts/notify/` (the `unittest` layout to copy).

## Scope

- One function in `lint_tickets.py` that reads `tasks/BOARD.md` and returns `{task id: status}`,
  and one that compares it against the collected tickets, called from `main()` beside
  `check_links`. Roughly forty lines.
- **A board row is a line whose stripped form starts with `|` and that contains a markdown link
  matching `](tasks/TASK-NNNNNN-….md)`.** The id comes from the *link target*, and the first link in
  the row wins. An id named in a prose paragraph is not a row — `TASK-000102` appears in prose at
  line 1599 and must be ignored.
- **The status is the last cell of the row.** It may be bold (`**done**`), struck through
  (`~~dropped~~`), or carry an explanation after an em dash (`**blocked** — \`DEC-077\` answered…`).
  Normalise by taking the text before the first ` — `, then stripping `*`, `~`, backticks and
  whitespace, then lowercasing.
- **Two failures, each naming both sides**: a task file with no board row, and a row whose status
  differs from the file's. The message quotes the id, the board's word and the file's word, so a
  reader does not have to open either.
- **The check is scoped to task rows.** Epic and story rows are excluded on a mechanical ground, not
  a stylistic one: the epic table has a trailing release column, so its status is not the last cell,
  and it uses words like `in progress` that are not in `STATUSES` at all. Story rows carry whole
  paragraphs in the status cell. A test pins that an id named only in prose is not read; the
  epic/story exclusion is enforced by the `tasks/` link pattern itself.
- **`lint_tickets.py`'s shape does not change.** `TASKS` stays a relative `Path("tasks")` and
  `errors` stays a module-level list, so the tests `os.chdir` into a temporary tree, call
  `lint_tickets.main()`, and `lint_tickets.errors.clear()` between cases. Refactoring the script to
  take a root argument is a bigger diff than the check and is **not** in this ticket.
- **The two drifted rows are reconciled in this PR, in `tasks/BOARD.md`, to match their files** —
  `TASK-000101` to `in-review`, `TASK-000103` to `in-progress`. The direction is not a judgement:
  `tasks/README.md`'s lifecycle puts the status in the ticket and updates the board *from* it, so the
  file is the register and the board is the index. Without this the new check exits 1 the moment it
  lands and the PR cannot merge.

## Out of scope

- **Deciding whether `TASK-000101` and `TASK-000103` are actually finished.** Reconciling a cell to
  its file is mechanical; changing a `status:` is a claim about which pull request merged what, and
  it belongs in a ticket that can say. This ticket makes the disagreement impossible to *keep* — it
  does not settle it, and after this PR the board will say two `EPIC-00` tickets are unfinished,
  loudly, which is the improvement.
- **Story files' own `## Tasks` tables**, which are a **third** register nothing reads.
  `STORY-0001`'s table lists two of its five tickets and calls `TASK-000102` `blocked` while both the
  board and the file say `done`. **A refusal, and it is not gated by a test** — asserting that a
  thirty-line function does not read a file it never opens tests an absence, and the absence is
  visible in the diff. Recorded here so the next reader finds a decision rather than an oversight,
  and so whoever wants that check writes their own ticket.
- **Epic and story rows.** Refused above with the mechanical reason.
- **Repairing anything the check finds beyond the two rows named.** The measurement says there is
  nothing else. If there is, it is a new ticket, because a repair made to turn a gate green is a
  change nobody reviewed against its own criteria.
- Any change to `scripts/notify/`, to the design-drift step, or to the `build` workflow.

## Tests

`.github/scripts/test_lint_tickets.py`, class `BoardRegisterTest`, one `unittest.TestCase`. Each test
builds a minimal `tasks/` tree in a `TemporaryDirectory` — one epic, one story, one task, and a
`BOARD.md` — then runs `lint_tickets.main()` and asserts the exit code.

| Test | Proves |
| --- | --- |
| `test_a_board_row_that_agrees_with_its_file_passes` | Row `in-progress`, file `in-progress`: exit `0`. The control, without which every other test passes against a checker that always fails |
| `test_a_board_row_that_disagrees_with_its_file_fails` | Row `done`, file `in-review` — `TASK-000101`'s exact shape: exit `1`, and the message names both words. Two **different** statuses that are both valid, so a checker comparing a status against a constant cannot pass |
| `test_a_task_file_with_no_board_row_fails` | A task file with no row anywhere in the board: exit `1`. The 29-files-no-rows failure, gated |
| `test_a_status_cell_with_prose_after_it_reads_only_the_status` | A cell reading `**blocked** — waiting on a decision` against a file reading `blocked`: exit `0`. `TASK-041226`'s live shape; a checker comparing the raw cell fails this |
| `test_a_struck_through_dropped_cell_agrees_with_its_file` | A cell reading `~~dropped~~` against a file reading `dropped`: exit `0`. `TASK-021113`'s live shape, and the false positive measured above |
| `test_an_id_named_only_in_prose_is_not_a_board_row` | A board whose only mention of the task is a prose sentence outside any table, and no row: exit `1` **for the missing row**, not `0` for a row it thought it found. Distinguishes a row from a mention in the direction that matters |

## Acceptance criteria

- [ ] `BoardRegisterTest.test_a_board_row_that_agrees_with_its_file_passes` passes
- [ ] `BoardRegisterTest.test_a_board_row_that_disagrees_with_its_file_fails` passes, and asserts the
      message contains **both** the board's word and the file's word
- [ ] `BoardRegisterTest.test_a_task_file_with_no_board_row_fails` passes
- [ ] `BoardRegisterTest.test_a_status_cell_with_prose_after_it_reads_only_the_status` passes
- [ ] `BoardRegisterTest.test_a_struck_through_dropped_cell_agrees_with_its_file` passes
- [ ] `BoardRegisterTest.test_an_id_named_only_in_prose_is_not_a_board_row` passes
- [ ] `python3 .github/scripts/lint_tickets.py`, run from the repository root, exits `0` on the real
      backlog — which requires the two `EPIC-00` cells to have been reconciled in this PR
- [ ] `.github/workflows/tickets.yml` runs the new tests, by `discover` over `.github/scripts`, so a
      seventh test added later needs no workflow edit
- [ ] `grep -c 'TASK-000101' .github/scripts/lint_tickets.py` returns `0` — no exemption list, no
      known-drift allowance
- [ ] No file outside the three listed differs, apart from the two reconciled `tasks/BOARD.md` cells
- [ ] Every command in `verify:` exits 0

## Proof

1. Revert one of the two reconciled cells — put `**done**` back on `TASK-000101`'s board row.
   **`python3 .github/scripts/lint_tickets.py` exits 1**, naming `TASK-000101`, `'done'` and
   `'in-review'`. This is the check catching the drift it was written for, on live data, and it is
   the run to paste into the PR. Restore the cell.
2. Delete `TASK-000104`'s board row entirely.
   **The linter exits 1 on the missing row**, and the six unit tests still pass — because they run
   against temporary trees, not this one. Worth seeing once: the unit tests gate the *logic* and the
   `lint_tickets.py` line in `verify:` gates the *backlog*, and neither substitutes for the other.
   Restore the row.
3. Normalise the cell by stripping only `` "*` " `` and not `~`.
   **`test_a_struck_through_dropped_cell_agrees_with_its_file` reddens alone**, and the real backlog
   grows a third failure on `TASK-021113` that is not a drift at all. The measurement above found
   this before the code existed; the test is what keeps it found.
4. Compare the whole last cell rather than the text before the em dash.
   **`test_a_status_cell_with_prose_after_it_reads_only_the_status` reddens alone.** Every other unit
   test uses a bare cell and cannot see it — which is why one fixture in the six carries prose.
5. Match the id with `re.search(r"TASK-\d{6}", line)` on any line rather than requiring a table row
   with a `tasks/…md` link.
   **`test_an_id_named_only_in_prose_is_not_a_board_row` reddens**, because a prose sentence is read
   as a row whose last "cell" is the whole sentence. Against the real board this is worse than a red
   run: `TASK-000102` is discussed in prose at line 1599 and would be compared against a paragraph.
6. Delete the `lint_tickets.errors.clear()` from `setUp`, then run the **whole class** —
   `cd .github/scripts && python3 -m unittest test_lint_tickets.BoardRegisterTest`.
   **`test_a_status_cell_with_prose_after_it_reads_only_the_status` and
   `test_a_struck_through_dropped_cell_agrees_with_its_file` redden**, and the three that expect exit
   `1` do not. `errors` is a module-level list in `lint_tickets.py`, `unittest` runs a class's methods
   in alphabetical order, and both of those sort *after*
   `test_a_board_row_that_disagrees_with_its_file_fails` — so they inherit its errors and see exit `1`
   for a tree that is clean. **Every one of the six per-method `verify:` lines still exits 0**,
   because each runs a single method in a fresh process — only the seventh line, which runs the whole
   class, catches it. That is why the block carries both forms: the six prove each named method
   *exists* (a class-level run exits 0 on a method that was never written), and the seventh proves
   they pass *together*. Say so in the PR.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
