---
schema: 2
id: TASK-110202
title: Read ticket and epic status out of the board
type: task
status: done
parent: STORY-1102
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [process, notifications]
depends_on: [TASK-110201]
verify:
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p test_board.py
  - grep -c "    def test_" scripts/notify/test_board.py
---

## Goal

`scripts/notify/board.py` turns the backlog into counts and lists a report can print, reading the
ticket files rather than parsing `BOARD.md` prose.

## Files

| File | Action |
| --- | --- |
| `scripts/notify/board.py` | create |
| `scripts/notify/test_board.py` | create |

## Scope

- `scan(tasks_dir) -> Backlog` walking `tasks/**/*.md`, reading the same flat frontmatter
  `.github/scripts/lint_tickets.py` reads. Do not import the linter — it exits the process on
  malformed input, which is correct for CI and fatal for a status message.
- `Backlog` exposes `counts_by_status`, `blocked` (id, title, and the `DEC-NNN` named in the
  ticket body, or `None`), and `epic_status(epic_id)`.
- A ticket whose frontmatter is missing or malformed is **skipped, counted as skipped, and
  reported** — never fatal, and never silently dropped, because a silent drop makes the numbers
  wrong in a message nobody can check.
- The `DEC-NNN` for a blocked ticket is the first `DEC-\d{3}` appearing in its body. One regex,
  no interpretation.
- `scan` on a missing directory returns an empty `Backlog`.

## Out of scope

- Validating anything. The linter owns correctness; this reads for a message and must never
  disagree with the linter loudly enough to matter.
- `git` or `gh` — `TASK-110203` gathers those.

## Tests

`test_board.py`

| Test | Proves |
| --- | --- |
| `test_counts_tickets_by_status` | the arithmetic over a fixture tree |
| `test_blocked_tickets_carry_their_decision_id` | `DEC-036` is extracted from the body |
| `test_blocked_ticket_without_a_decision_is_still_listed` | `None` rather than omission |
| `test_malformed_frontmatter_is_skipped_not_fatal` | a broken file does not stop the scan |
| `test_malformed_frontmatter_is_counted` | the skip is visible in the result |
| `test_missing_directory_returns_empty` | a report is still possible outside a repository |
| `test_epic_status_reads_the_epic_file` | epic-level status comes from the epic, not inference |

## Acceptance criteria

- [ ] `test_counts_tickets_by_status` passes
- [ ] `test_blocked_tickets_carry_their_decision_id` passes
- [ ] `test_blocked_ticket_without_a_decision_is_still_listed` passes
- [ ] `test_malformed_frontmatter_is_skipped_not_fatal` passes
- [ ] `test_malformed_frontmatter_is_counted` passes
- [ ] `test_missing_directory_returns_empty` passes
- [ ] `test_epic_status_reads_the_epic_file` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
