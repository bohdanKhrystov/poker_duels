import tempfile
import unittest
from pathlib import Path

import board

TASK = """---
schema: 2
id: {id}
title: {title}
type: task
status: {status}
parent: STORY-1101
verify:
  - python3 -c 'pass'
---

{body}
"""

EPIC = """---
id: {id}
title: An epic
type: epic
status: {status}
---

Body.
"""


def tree(tmp, tasks=(), epics=(), extra=None):
    root = Path(tmp)
    (root / "tasks").mkdir(parents=True)
    (root / "epics").mkdir(parents=True)
    (root / "templates").mkdir(parents=True)
    (root / "BOARD.md").write_text("not a ticket", encoding="utf-8")
    (root / "templates" / "task.md").write_text("---\nid: TASK-EESSTT\n---\n", encoding="utf-8")

    for ticket_id, status, body in tasks:
        (root / "tasks" / f"{ticket_id}-x.md").write_text(
            TASK.format(id=ticket_id, title="A task", status=status, body=body), encoding="utf-8"
        )
    for epic_id, status in epics:
        (root / "epics" / f"{epic_id}-x.md").write_text(
            EPIC.format(id=epic_id, status=status), encoding="utf-8"
        )
    if extra is not None:
        (root / "tasks" / "TASK-999999-broken.md").write_text(extra, encoding="utf-8")
    return root


class ScanTest(unittest.TestCase):
    def test_counts_tickets_by_status(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = tree(tmp, tasks=[
                ("TASK-110101", "done", "x"),
                ("TASK-110102", "done", "x"),
                ("TASK-110103", "ready", "x"),
            ])
            counts = board.scan(root).counts_by_status
            self.assertEqual(2, counts["done"])
            self.assertEqual(1, counts["ready"])

    def test_blocked_tickets_carry_their_decision_id(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = tree(tmp, tasks=[("TASK-110101", "blocked", "Waiting on DEC-036 to land.")])
            self.assertEqual("DEC-036", board.scan(root).blocked[0].decision)

    def test_blocked_ticket_without_a_decision_is_still_listed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = tree(tmp, tasks=[("TASK-110101", "blocked", "Waiting on a rebase.")])
            blocked = board.scan(root).blocked
            self.assertEqual(1, len(blocked))
            self.assertIsNone(blocked[0].decision)

    def test_malformed_frontmatter_is_skipped_not_fatal(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = tree(tmp, tasks=[("TASK-110101", "done", "x")], extra="no frontmatter here")
            self.assertEqual(1, board.scan(root).counts_by_status["done"])

    def test_malformed_frontmatter_is_counted(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = tree(tmp, tasks=[("TASK-110101", "done", "x")], extra="no frontmatter here")
            self.assertEqual(1, board.scan(root).skipped)

    def test_missing_directory_returns_empty(self):
        result = board.scan("/nowhere/at/all/tasks")
        self.assertEqual({}, result.counts_by_status)
        self.assertEqual([], result.blocked)

    def test_epic_status_reads_the_epic_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = tree(tmp, epics=[("EPIC-11", "ready")])
            self.assertEqual("ready", board.scan(root).epic_status("EPIC-11"))


if __name__ == "__main__":
    unittest.main()
