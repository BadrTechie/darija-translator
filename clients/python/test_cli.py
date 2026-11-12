import base64
import json
import os
import subprocess
import sys
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from unittest.mock import patch

import darija_translate


class StubBackend(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers["Content-Length"])
        request = json.loads(self.rfile.read(length))
        expected = "Basic " + base64.b64encode(b"demo:secret").decode("ascii")
        if self.path != "/api/translate" or self.headers.get("Authorization") != expected:
            self.send_response(401)
            response = {"error": "UNAUTHORIZED", "message": "Invalid credentials"}
        elif request.get("text") == "provider-fail":
            self.send_response(502)
            response = {"error": "PROVIDER_UNAVAILABLE", "message": "Provider unavailable"}
        elif request.get("text") == "invalid":
            self.send_response(400)
            response = {"error": "VALIDATION_ERROR", "message": "Text is invalid"}
        else:
            self.send_response(200)
            response = {"translation": "سلام من الاختبار"}
        body = json.dumps(response, ensure_ascii=False).encode("utf-8")
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, _format, *_args):
        pass


class CliTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), StubBackend)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.url = f"http://127.0.0.1:{cls.server.server_port}"
        cls.script = Path(__file__).with_name("darija_translate.py")

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()

    def run_cli(self, text, *, password="secret", backend_url=None):
        environment = os.environ.copy()
        environment["TRANSLATOR_PASSWORD"] = password
        return subprocess.run(
            [sys.executable, str(self.script), text,
             "--source-language", "English",
             "--backend-url", backend_url or self.url,
             "--username", "demo", "--timeout", "2"],
            check=False,
            capture_output=True,
            text=True,
            encoding="utf-8",
            env=environment,
        )

    def test_normal_entry_point_reads_translation_field(self):
        result = self.run_cli("Hello")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("سلام من الاختبار", result.stdout.strip())

    def test_normal_entry_point_reports_provider_error(self):
        result = self.run_cli("provider-fail")
        self.assertEqual(1, result.returncode)
        self.assertIn("Translation provider error (HTTP 502)", result.stderr)
        self.assertIn("Provider unavailable", result.stderr)

    def test_normal_entry_point_reports_rejected_credentials(self):
        result = self.run_cli("Hello", password="incorrect")
        self.assertEqual(1, result.returncode)
        self.assertIn("Authentication rejected (HTTP 401)", result.stderr)

    def test_normal_entry_point_reports_backend_validation_error(self):
        result = self.run_cli("invalid")
        self.assertEqual(1, result.returncode)
        self.assertIn("Invalid input (HTTP 400)", result.stderr)

    def test_normal_entry_point_rejects_blank_text_locally(self):
        result = self.run_cli("   ")
        self.assertEqual(2, result.returncode)
        self.assertIn("text must not be blank", result.stderr)

    def test_normal_entry_point_reports_unreachable_backend(self):
        result = self.run_cli("Hello", backend_url="http://127.0.0.1:1")
        self.assertEqual(1, result.returncode)
        self.assertIn("Cannot reach the backend", result.stderr)

    def test_interactive_password_uses_private_getpass_prompt(self):
        with patch.dict(os.environ, {"TRANSLATOR_PASSWORD": ""}), \
                patch.object(darija_translate.sys, "stdin") as stdin, \
                patch.object(darija_translate.getpass, "getpass",
                             return_value="hidden-secret") as password_prompt:
            stdin.isatty.return_value = True
            self.assertEqual("hidden-secret", darija_translate._read_password())
            password_prompt.assert_called_once_with("Backend password: ")


if __name__ == "__main__":
    unittest.main()
