import json
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path

import run_state


class LoadTest(unittest.TestCase):
    def test_missing_file_loads_empty(self):
        state = run_state.load(Path("/nowhere/at/all/run-state.json"))
        self.assertIsNone(state.current_epic)
        self.assertEqual([], state.epics)

    def test_malformed_json_loads_empty(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            path.write_text("{not json at all", encoding="utf-8")
            self.assertIsNone(run_state.load(path).current_epic)

    def test_json_array_loads_empty(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            path.write_text('["valid json", "wrong shape"]', encoding="utf-8")
            self.assertIsNone(run_state.load(path).current_epic)


class SaveTest(unittest.TestCase):
    def test_round_trips_every_field(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            state = run_state.RunState(
                epics=["EPIC-11", "EPIC-03"],
                current_epic="EPIC-11",
                current_story="STORY-1101",
                last_report_at="2026-08-15T20:00:00+00:00",
                cron_armed=True,
                started_at="2026-08-15T19:00:00+00:00",
                note="a note",
            )
            run_state.save(path, state)
            back = run_state.load(path)
            for name in run_state.FIELDS:
                self.assertEqual(getattr(state, name), getattr(back, name), name)

    def test_cron_armed_false_is_not_lost_as_unknown(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            run_state.save(path, run_state.RunState(cron_armed=False))
            self.assertIs(False, run_state.load(path).cron_armed)

    def test_stamp_report_preserves_other_fields(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            run_state.save(path, run_state.RunState(current_epic="EPIC-11", cron_armed=True))
            run_state.stamp_report(path, datetime(2026, 8, 15, 21, 0, tzinfo=timezone.utc))
            back = run_state.load(path)
            self.assertEqual("EPIC-11", back.current_epic)
            self.assertIs(True, back.cron_armed)
            self.assertTrue(back.last_report_at.startswith("2026-08-15T21:00"))

    def test_save_creates_the_parent_directory(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "never" / "existed" / "run-state.json"
            run_state.save(path, run_state.RunState(current_epic="EPIC-11"))
            self.assertEqual("EPIC-11", json.loads(path.read_text())["current_epic"])


if __name__ == "__main__":
    unittest.main()
