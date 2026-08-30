import contextlib
import io
import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import credentials
import notify
import run_state
import telegram


class SendTest(unittest.TestCase):
    def test_send_exits_zero_with_no_credentials(self):
        # The contract of ADR-0042: a notifier never fails its caller.
        with mock.patch.object(notify.credentials_module, "resolve", return_value=None):
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(0, notify.main(["send", "anything"]))

    def test_send_reads_stdin_when_no_arguments(self):
        with mock.patch.object(notify, "deliver", return_value=(True, "sent")) as deliver:
            with mock.patch("sys.stdin", io.StringIO("piped report body")):
                with contextlib.redirect_stdout(io.StringIO()):
                    notify.main(["send"])
        self.assertEqual("piped report body", deliver.call_args[0][0])

    def test_send_joins_argument_words(self):
        with mock.patch.object(notify, "deliver", return_value=(True, "sent")) as deliver:
            with contextlib.redirect_stdout(io.StringIO()):
                notify.main(["send", "three", "separate", "words"])
        self.assertEqual("three separate words", deliver.call_args[0][0])


class DoctorTest(unittest.TestCase):
    def test_doctor_exits_non_zero_when_unconfigured(self):
        with mock.patch.dict(os.environ, {}, clear=True):
            with mock.patch.object(credentials, "DEFAULT_PATH", Path("/nowhere/at/all/telegram.env")):
                out = io.StringIO()
                with contextlib.redirect_stdout(out):
                    code = notify.main(["doctor"])
        self.assertEqual(1, code)
        self.assertIn("no credential file", out.getvalue())

    def test_doctor_names_the_missing_piece(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "telegram.env"
            path.write_text("TELEGRAM_BOT_TOKEN=only-the-token\n", encoding="utf-8")
            with mock.patch.dict(os.environ, {}, clear=True):
                with mock.patch.object(credentials, "DEFAULT_PATH", path):
                    out = io.StringIO()
                    with contextlib.redirect_stdout(out):
                        code = notify.main(["doctor"])
        self.assertEqual(1, code)
        self.assertIn("half a configuration", out.getvalue())
        self.assertIn(credentials.CHAT_KEY, out.getvalue())

    def test_doctor_exits_zero_when_the_send_succeeds(self):
        creds = credentials.Credentials(token="t:okon", chat_id="1")
        with mock.patch.object(notify.credentials_module, "resolve", return_value=creds):
            with mock.patch.object(notify.telegram_module, "send", return_value=telegram.Result(True, "sent")):
                out = io.StringIO()
                with contextlib.redirect_stdout(out):
                    code = notify.main(["doctor"])
        self.assertEqual(0, code)
        self.assertIn("ok", out.getvalue())


class UsageTest(unittest.TestCase):
    def test_unknown_subcommand_exits_two(self):
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit) as raised:
                notify.main(["nonsense"])
        self.assertEqual(2, raised.exception.code)

    def test_no_subcommand_exits_two(self):
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(2, notify.main([]))


class StateClearTest(unittest.TestCase):
    """TASK-120701: ``state --clear`` keeps only ``last_report_at``, the heartbeat's dedupe stamp."""

    def _clear(self, path):
        with mock.patch.object(notify, "STATE_PATH", path):
            with contextlib.redirect_stdout(io.StringIO()):
                notify.main(["state", "--clear"])

    def test_clear_still_removes_the_current_epic(self):
        # The half that already works: an absent current_epic is what silences the Stop hook.
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            run_state.save(path, run_state.RunState(current_epic="EPIC-12"))
            self._clear(path)
            self.assertIsNone(run_state.load(path).current_epic)

    def test_clear_leaves_the_cron_flag_unknown(self):
        # None, not False: --clear runs before CronDelete in qa-cycle's teardown, so it cannot
        # know the cron is gone yet.
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            run_state.save(path, run_state.RunState(cron_armed=True))
            self._clear(path)
            self.assertIsNone(run_state.load(path).cron_armed)

    def test_clear_drops_a_note_from_an_earlier_run(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            run_state.save(
                path,
                run_state.RunState(note="qa-cases built and running; EPIC-04 suite in review (PR#1169)"),
            )
            self._clear(path)
            self.assertIsNone(run_state.load(path).note)

    def test_clear_keeps_the_heartbeat_dedupe_stamp(self):
        stamp = "2026-08-29T21:11:36.245377+00:00"
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            run_state.save(path, run_state.RunState(last_report_at=stamp))
            self._clear(path)
            self.assertEqual(stamp, run_state.load(path).last_report_at)

    def test_clear_leaves_only_the_dedupe_stamp(self):
        # Every field set to a distinct non-default value first — a fixture that leaves a field
        # at its default cannot tell "the clear removed it" from "it was never there".
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            run_state.save(
                path,
                run_state.RunState(
                    epics=["EPIC-11", "EPIC-03"],
                    current_epic="EPIC-12",
                    current_story="STORY-1207",
                    last_report_at="2026-08-29T21:11:36.245377+00:00",
                    cron_armed=True,
                    started_at="2026-08-29T19:00:00+00:00",
                    note="qa-cases built and running; EPIC-04 suite in review (PR#1169)",
                ),
            )
            self._clear(path)
            raw = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual({"last_report_at"}, set(raw))


if __name__ == "__main__":
    unittest.main()
