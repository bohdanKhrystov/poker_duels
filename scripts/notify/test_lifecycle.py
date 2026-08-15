import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path
from unittest import mock

import board
import lifecycle
import notify
import run_state

NOW = datetime(2026, 8, 15, 22, 0, tzinfo=timezone.utc)


def runner(args, cwd=None):
    return (True, "abc1234 feat: a thing") if args[0] == "git" else (True, "")


def backlog():
    return board.Backlog(
        counts_by_status={"done": 2},
        blocked=[board.Blocked("TASK-110101", "A blocked ticket", "DEC-036")],
    )


class StopReportTest(unittest.TestCase):
    def report(self, reason=None):
        return lifecycle.stop_report(".", run_state.RunState(), NOW, reason, runner, backlog())

    def test_stop_report_names_the_reason(self):
        self.assertIn("out of budget", self.report("out of budget"))

    def test_stop_report_without_a_reason_still_sends(self):
        text = self.report(None)
        self.assertIn("STOPPED", text)
        self.assertIn("not given", text)

    def test_stop_report_carries_done_and_blocked(self):
        text = self.report("done for the night")
        self.assertIn("feat: a thing", text)
        self.assertIn("DEC-036", text)


class BlockedReportTest(unittest.TestCase):
    def test_blocked_report_names_the_decision(self):
        text = lifecycle.blocked_report(
            ".", run_state.RunState(), "DEC-037", "Does a season reset coins?", NOW, runner, backlog()
        )
        self.assertIn("DEC-037", text)
        self.assertIn("Does a season reset coins?", text)


class BudgetReportTest(unittest.TestCase):
    def report(self, cron_armed):
        return lifecycle.budget_report(".", run_state.RunState(), cron_armed, None, NOW, runner, backlog())

    def test_budget_renders_armed(self):
        self.assertIn("resume cron: armed", self.report("armed"))

    def test_budget_renders_not_armed(self):
        self.assertIn("NOT ARMED", self.report("not-armed"))

    def test_budget_renders_unknown(self):
        text = self.report("unknown")
        self.assertIn("unknown", text)
        self.assertNotIn("NOT ARMED", text)

    def test_budget_rejects_an_unknown_cron_state(self):
        with self.assertRaises(ValueError):
            self.report("maybe")


class BudgetCommandTest(unittest.TestCase):
    def run_budget(self, argv, tmp):
        path = Path(tmp) / "run-state.json"
        with mock.patch.object(notify, "STATE_PATH", path):
            code = notify.main(argv)
        return code, run_state.load(path)

    def test_budget_requires_the_cron_flag(self):
        # Omitting it is made unmakeable rather than defaulted: a budget report that does not
        # say what happened to the resume cron cannot do the one job it exists for.
        with self.assertRaises(SystemExit) as raised:
            notify.main(["budget", "--dry-run"])
        self.assertEqual(2, raised.exception.code)

    def test_budget_writes_cron_armed_into_run_state(self):
        with tempfile.TemporaryDirectory() as tmp:
            code, state = self.run_budget(["budget", "--cron-armed", "not-armed", "--dry-run"], tmp)
            self.assertEqual(0, code)
            self.assertIs(False, state.cron_armed)

    def test_dry_run_sends_nothing(self):
        with tempfile.TemporaryDirectory() as tmp, mock.patch.object(notify.telegram_module, "send") as send:
            self.run_budget(["budget", "--cron-armed", "armed", "--dry-run"], tmp)
            send.assert_not_called()


if __name__ == "__main__":
    unittest.main()
