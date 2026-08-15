import unittest
from datetime import datetime, timezone

import board
import report
import run_state

NOW = datetime(2026, 8, 15, 22, 0, tzinfo=timezone.utc)


def backlog_with(blocked=(), counts=None):
    return board.Backlog(
        counts_by_status=dict(counts or {"done": 3, "in-progress": 1}),
        blocked=list(blocked),
    )


def runner_for(responses):
    """responses: {binary: (ok, output)}. Anything unlisted is a failure."""

    def runner(args, cwd=None):
        return responses.get(args[0], (False, "not found"))

    return runner


def everything_works(commits="abc1234 feat: a thing\ndef5678 fix: another"):
    return runner_for({"git": (True, commits), "gh": (True, "#12 a pull request")})


class SectionTest(unittest.TestCase):
    def test_all_four_sections_are_present(self):
        text = report.compose(".", run_state.RunState(), NOW, everything_works(), backlog=backlog_with())
        for heading in ("DONE", "IN PROGRESS", "BLOCKED", "BUDGET"):
            self.assertIn(heading, text)

    def test_commits_since_last_report_are_listed(self):
        seen = []

        def runner(args, cwd=None):
            seen.append(args)
            return (True, "abc1234 feat: landed this") if args[0] == "git" else (True, "")

        state = run_state.RunState(last_report_at="2026-08-15T20:00:00+00:00")
        text = report.compose(".", state, NOW, runner, backlog=backlog_with())
        self.assertIn("feat: landed this", text)
        self.assertTrue(any("--since=2026-08-15T20:00:00+00:00" in a for a in seen[0]))

    def test_missing_gh_degrades_only_its_section(self):
        runner = runner_for({"git": (True, "abc1234 feat: a thing")})
        text = report.compose(".", run_state.RunState(), NOW, runner, backlog=backlog_with())
        self.assertIn("gh could not list", text)
        self.assertIn("feat: a thing", text)  # the DONE section survived

    def test_git_failure_degrades_only_its_section(self):
        runner = runner_for({"gh": (True, "#12 a pull request")})
        text = report.compose(".", run_state.RunState(), NOW, runner, backlog=backlog_with())
        self.assertIn("git could not list commits", text)
        self.assertIn("#12 a pull request", text)  # the IN PROGRESS section survived

    def test_blocked_section_names_the_decision(self):
        blocked = [board.Blocked("TASK-110101", "A ticket", "DEC-036")]
        text = report.compose(".", run_state.RunState(), NOW, everything_works(), backlog=backlog_with(blocked))
        self.assertIn("DEC-036", text)


class CronLineTest(unittest.TestCase):
    def render(self, state):
        return report.compose(".", state, NOW, everything_works(), backlog=backlog_with())

    def test_cron_armed_true_renders_armed(self):
        self.assertIn("resume cron: armed", self.render(run_state.RunState(cron_armed=True)))

    def test_cron_armed_false_renders_not_armed(self):
        self.assertIn("NOT ARMED", self.render(run_state.RunState(cron_armed=False)))

    def test_cron_armed_absent_renders_unknown(self):
        # Absent and False mean different things; conflating them tells the human the run is
        # over when it may not be.
        text = self.render(run_state.RunState())
        self.assertIn("resume cron: unknown", text)
        self.assertNotIn("NOT ARMED", text)


class TruncationTest(unittest.TestCase):
    def oversized(self, limit):
        commits = "\n".join(f"abc{n:04d} a commit with a fairly long subject line {n}" for n in range(60))
        blocked = [board.Blocked(f"TASK-1101{n:02d}", "A blocked ticket", "DEC-036") for n in range(20)]
        return report.compose(
            ".", run_state.RunState(), NOW, everything_works(commits), limit=limit, backlog=backlog_with(blocked)
        )

    def test_oversized_report_drops_whole_sections(self):
        full = self.oversized(limit=1_000_000)
        limit = int(len(full) * 0.7)
        text = self.oversized(limit)

        self.assertLessEqual(len(text), limit)
        # The leading sections survive whole — truncation never splits one mid-list.
        self.assertIn("DONE", text)
        self.assertIn("a commit with a fairly long subject line 0", text)
        # …and the trailing one is gone. Assert on the section's *body*: its heading still
        # appears in the dropped-for-length note, which would make a heading check vacuous.
        self.assertNotIn("resume cron", text)

    def test_oversized_report_says_what_it_dropped(self):
        full = self.oversized(limit=1_000_000)
        text = self.oversized(int(len(full) * 0.7))
        self.assertIn("dropped for length", text)
        self.assertIn("BUDGET", text.split("dropped for length")[1])


class DegradedTest(unittest.TestCase):
    def test_composes_with_empty_state_and_no_tools(self):
        text = report.compose(".", run_state.RunState(), NOW, runner_for({}), backlog=board.Backlog())
        self.assertIn("Poker Duels", text)
        self.assertIn("BUDGET", text)


if __name__ == "__main__":
    unittest.main()
