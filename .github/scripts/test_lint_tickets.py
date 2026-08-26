import os
import sys
import tempfile
import unittest
from pathlib import Path

import lint_tickets

EPIC = """---
id: EPIC-00
title: An epic
type: epic
status: ready
---

Body.
"""

STORY = """---
id: STORY-0001
title: A story
type: story
status: ready
parent: EPIC-00
---

Body.
"""

TASK_TEMPLATE = """---
id: {id}
title: {title}
type: task
status: {status}
schema: 2
estimate: S
tier: haiku
review: standard
files_touched: 1
parent: STORY-0001
verify:
  - python3 -c 'pass'
---

Body.
"""

BOARD_TEMPLATE = """|  | Task | Status |
| --- | --- | --- |
{rows}
"""


class BoardRegisterTest(unittest.TestCase):
    """Test board register validation in lint_tickets.py"""

    def setUp(self):
        """Create a temporary directory structure for each test."""
        self.tmp_dir = tempfile.TemporaryDirectory()
        self.old_cwd = os.getcwd()
        self.old_errors = lint_tickets.errors[:]
        lint_tickets.errors.clear()
        os.chdir(self.tmp_dir.name)

        # Create the basic directory structure
        Path("tasks").mkdir(parents=True)
        Path("tasks/epics").mkdir(parents=True)
        Path("tasks/stories").mkdir(parents=True)
        Path("tasks/tasks").mkdir(parents=True)
        Path("tasks/templates").mkdir(parents=True)

        # Create minimal files
        Path("tasks/epics/EPIC-00-epic.md").write_text(EPIC, encoding="utf-8")
        Path("tasks/stories/STORY-0001-story.md").write_text(STORY, encoding="utf-8")

    def tearDown(self):
        """Clean up after each test."""
        os.chdir(self.old_cwd)
        self.tmp_dir.cleanup()
        lint_tickets.errors.clear()
        lint_tickets.errors.extend(self.old_errors)

    def test_a_board_row_that_agrees_with_its_file_passes(self):
        """Row in-progress, file in-progress: exit 0. Control test."""
        # Create task file
        Path("tasks/tasks/TASK-000101-test.md").write_text(
            TASK_TEMPLATE.format(id="TASK-000101", title="Test task", status="in-progress"),
            encoding="utf-8"
        )

        # Create board with matching status
        board_content = BOARD_TEMPLATE.format(
            rows="| | [TASK-000101](tasks/TASK-000101-test.md) | **in-progress** |"
        )
        Path("tasks/BOARD.md").write_text(board_content, encoding="utf-8")

        # Run the linter
        result = lint_tickets.main()

        # Should pass
        self.assertEqual(result, 0, f"Expected exit 0, got {result}. Errors: {lint_tickets.errors}")

    def test_a_board_row_that_disagrees_with_its_file_fails(self):
        """Row done, file in-review — TASK-000101's exact shape: exit 1, message names both words."""
        # Create task file
        Path("tasks/tasks/TASK-000101-test.md").write_text(
            TASK_TEMPLATE.format(id="TASK-000101", title="Test task", status="in-review"),
            encoding="utf-8"
        )

        # Create board with different status
        board_content = BOARD_TEMPLATE.format(
            rows="| | [TASK-000101](tasks/TASK-000101-test.md) | **done** |"
        )
        Path("tasks/BOARD.md").write_text(board_content, encoding="utf-8")

        # Run the linter
        result = lint_tickets.main()

        # Should fail
        self.assertEqual(result, 1, f"Expected exit 1, got {result}")
        # Check that the error message contains both the board status and file status
        error_text = " ".join(lint_tickets.errors)
        self.assertIn("TASK-000101", error_text)
        self.assertIn("done", error_text.lower())
        self.assertIn("in-review", error_text.lower())

    def test_a_task_file_with_no_board_row_fails(self):
        """Task file with no row anywhere in the board: exit 1. The 29-files-no-rows failure."""
        # Create task file
        Path("tasks/tasks/TASK-000101-test.md").write_text(
            TASK_TEMPLATE.format(id="TASK-000101", title="Test task", status="in-progress"),
            encoding="utf-8"
        )

        # Create board with no matching row
        board_content = BOARD_TEMPLATE.format(rows="")
        Path("tasks/BOARD.md").write_text(board_content, encoding="utf-8")

        # Run the linter
        result = lint_tickets.main()

        # Should fail
        self.assertEqual(result, 1, f"Expected exit 1, got {result}")
        error_text = " ".join(lint_tickets.errors)
        self.assertIn("TASK-000101", error_text)

    def test_a_status_cell_with_prose_after_it_reads_only_the_status(self):
        """Cell reading **blocked** — waiting on a decision against file reading blocked: exit 0."""
        # Create task file
        Path("tasks/tasks/TASK-000101-test.md").write_text(
            TASK_TEMPLATE.format(id="TASK-000101", title="Test task", status="blocked"),
            encoding="utf-8"
        )

        # Create board with prose after the status (em dash)
        board_content = BOARD_TEMPLATE.format(
            rows="| | [TASK-000101](tasks/TASK-000101-test.md) | **blocked** — waiting on a decision |"
        )
        Path("tasks/BOARD.md").write_text(board_content, encoding="utf-8")

        # Run the linter
        result = lint_tickets.main()

        # Should pass
        self.assertEqual(result, 0, f"Expected exit 0, got {result}. Errors: {lint_tickets.errors}")

    def test_a_struck_through_dropped_cell_agrees_with_its_file(self):
        """Cell reading ~~dropped~~ against file reading dropped: exit 0. TASK-021113's live shape."""
        # Create task file
        Path("tasks/tasks/TASK-000101-test.md").write_text(
            TASK_TEMPLATE.format(id="TASK-000101", title="Test task", status="dropped"),
            encoding="utf-8"
        )

        # Create board with struck-through status
        board_content = BOARD_TEMPLATE.format(
            rows="| | [TASK-000101](tasks/TASK-000101-test.md) | ~~dropped~~ |"
        )
        Path("tasks/BOARD.md").write_text(board_content, encoding="utf-8")

        # Run the linter
        result = lint_tickets.main()

        # Should pass
        self.assertEqual(result, 0, f"Expected exit 0, got {result}. Errors: {lint_tickets.errors}")

    def test_an_id_named_only_in_prose_is_not_a_board_row(self):
        """A board whose only mention of the task is a prose sentence, no row: exit 1 for missing row."""
        # Create task file
        Path("tasks/tasks/TASK-000101-test.md").write_text(
            TASK_TEMPLATE.format(id="TASK-000101", title="Test task", status="in-progress"),
            encoding="utf-8"
        )

        # Create board with a prose line containing a markdown link to the task, but not in a table row.
        # This tests that the table-row guard (checking for "|" start) correctly excludes prose lines
        # with markdown links, which exist in the real board (e.g., "See [TASK-041223](...) for details").
        board_content = """See [TASK-000101](tasks/TASK-000101-test.md) for details on this feature.

| Epic | Story | Status |
| --- | --- | --- |
"""
        Path("tasks/BOARD.md").write_text(board_content, encoding="utf-8")

        # Run the linter
        result = lint_tickets.main()

        # Should fail because the task has no board row (prose mentions don't count)
        self.assertEqual(result, 1, f"Expected exit 1, got {result}")
        error_text = " ".join(lint_tickets.errors)
        self.assertIn("TASK-000101", error_text)
        # Verify the error is about a missing row, not a disagreement
        self.assertIn("no board row", error_text)


if __name__ == "__main__":
    unittest.main()
