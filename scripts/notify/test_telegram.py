import io
import unittest
import urllib.error
import urllib.parse

import credentials
import telegram

TOKEN = "8609897557:AAGsecretsecretsecret"
CREDS = credentials.Credentials(token=TOKEN, chat_id="591343919")


class FakeResponse:
    def __init__(self, payload):
        self.payload = payload

    def __enter__(self):
        return self

    def __exit__(self, *unused):
        return False

    def read(self):
        return self.payload.encode("utf-8")


def opener_returning(payload, captured):
    def opener(request, timeout=None):
        captured.append(request)
        return FakeResponse(payload)

    return opener


def opener_raising(error):
    def opener(request, timeout=None):
        raise error

    return opener


def body_of(request):
    return dict(urllib.parse.parse_qsl(request.data.decode("utf-8")))


class SendTest(unittest.TestCase):
    def test_posts_to_the_send_message_endpoint(self):
        captured = []
        telegram.send(CREDS, "hello", opener=opener_returning('{"ok":true}', captured))
        self.assertIn("/sendMessage", captured[0].full_url)
        self.assertIn(TOKEN, captured[0].full_url)

    def test_body_carries_chat_id_and_text(self):
        captured = []
        telegram.send(CREDS, "hello there", opener=opener_returning('{"ok":true}', captured))
        body = body_of(captured[0])
        self.assertEqual("591343919", body["chat_id"])
        self.assertEqual("hello there", body["text"])

    def test_no_parse_mode_is_sent(self):
        captured = []
        telegram.send(CREDS, "*not* markdown", opener=opener_returning('{"ok":true}', captured))
        self.assertNotIn("parse_mode", body_of(captured[0]))

    def test_long_text_is_truncated_to_the_limit(self):
        captured = []
        telegram.send(CREDS, "x" * 9000, opener=opener_returning('{"ok":true}', captured))
        self.assertLessEqual(len(body_of(captured[0])["text"]), telegram.LIMIT)

    def test_truncation_says_it_truncated(self):
        captured = []
        telegram.send(CREDS, "x" * 9000, opener=opener_returning('{"ok":true}', captured))
        self.assertTrue(body_of(captured[0])["text"].endswith(telegram.TRUNCATION_MARKER))

    def test_http_error_becomes_a_failed_result(self):
        error = urllib.error.HTTPError(
            f"https://api.telegram.org/bot{TOKEN}/sendMessage", 400, "Bad Request", {}, io.BytesIO(b"{}")
        )
        result = telegram.send(CREDS, "hi", opener=opener_raising(error))
        self.assertFalse(result.ok)

    def test_network_error_becomes_a_failed_result(self):
        result = telegram.send(CREDS, "hi", opener=opener_raising(urllib.error.URLError("offline")))
        self.assertFalse(result.ok)

    def test_error_detail_never_contains_the_token(self):
        # The token is in the URL, and HTTPError quotes the URL back at you.
        error = urllib.error.HTTPError(
            f"https://api.telegram.org/bot{TOKEN}/sendMessage",
            400,
            "Bad Request",
            {},
            io.BytesIO(f"unauthorized for bot{TOKEN}".encode("utf-8")),
        )
        result = telegram.send(CREDS, "hi", opener=opener_raising(error))
        self.assertNotIn(TOKEN, result.detail)
        self.assertNotIn("AAGsecretsecretsecret", result.detail)


if __name__ == "__main__":
    unittest.main()
