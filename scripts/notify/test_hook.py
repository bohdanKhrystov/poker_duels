import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

REAL_HOOK = Path(__file__).resolve().parent / "hooks" / "stop_hook.sh"

# A stand-in for notify.py that records the arguments it was called with, so the test proves the
# wiring without sending anything.
FAKE_NOTIFY = """#!/usr/bin/env python3
import sys, pathlib
marker = pathlib.Path(__file__).resolve().parents[2] / "called.txt"
marker.write_text(" ".join(sys.argv[1:]), encoding="utf-8")
"""


def fake_repo(tmp, run_state=None):
    """Build scripts/notify/hooks/ under `tmp` so the hook's own path resolution is exercised."""
    root = Path(tmp)
    hooks = root / "scripts" / "notify" / "hooks"
    hooks.mkdir(parents=True)
    shutil.copy(REAL_HOOK, hooks / "stop_hook.sh")
    (hooks / "stop_hook.sh").chmod(0o755)

    notify_py = root / "scripts" / "notify" / "notify.py"
    notify_py.write_text(FAKE_NOTIFY, encoding="utf-8")

    if run_state is not None:
        (root / ".claude").mkdir(parents=True)
        (root / ".claude" / "run-state.json").write_text(json.dumps(run_state), encoding="utf-8")

    return root, hooks / "stop_hook.sh"


def run(script, stdin="", cwd=None):
    return subprocess.run(
        ["bash", str(script)], input=stdin, capture_output=True, text=True, cwd=cwd, timeout=30
    )


class ExitCodeTest(unittest.TestCase):
    def test_hook_exits_zero_on_empty_stdin(self):
        self.assertEqual(0, run(REAL_HOOK, "").returncode)

    def test_hook_exits_zero_on_malformed_json(self):
        # A payload shape change must never be able to break the session it reports on.
        self.assertEqual(0, run(REAL_HOOK, "{not json at all").returncode)


class SilenceTest(unittest.TestCase):
    def test_hook_is_silent_without_run_state(self):
        with tempfile.TemporaryDirectory() as tmp:
            root, script = fake_repo(tmp, run_state=None)
            run(script, "{}")
            self.assertFalse((root / "called.txt").exists())

    def test_hook_is_silent_when_run_state_has_no_epic(self):
        with tempfile.TemporaryDirectory() as tmp:
            root, script = fake_repo(tmp, run_state={"last_report_at": "2026-08-15T22:00:00+00:00"})
            run(script, "{}")
            self.assertFalse((root / "called.txt").exists())


class WiringTest(unittest.TestCase):
    def test_hook_invokes_stop_when_a_run_is_in_flight(self):
        with tempfile.TemporaryDirectory() as tmp:
            root, script = fake_repo(tmp, run_state={"current_epic": "EPIC-11"})
            run(script, '{"hook_event_name":"Stop"}')
            self.assertIn("stop", (root / "called.txt").read_text())

    def test_hook_resolves_the_repo_from_its_own_path(self):
        # A hook's working directory is not guaranteed, so $PWD may never be trusted.
        with tempfile.TemporaryDirectory() as tmp:
            root, script = fake_repo(tmp, run_state={"current_epic": "EPIC-11"})
            run(script, "{}", cwd="/")
            self.assertTrue((root / "called.txt").exists())


if __name__ == "__main__":
    unittest.main()
