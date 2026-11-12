import base64
import json
import subprocess
import sys
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


class StubBackend(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers["Content-Length"])
        request = json.loads(self.rfile.read(length))
        expected = "Basic " + base64.b64encode(b"demo:secret").decode("ascii")
        if self.path != "/api/translate" or self.headers.get("Authorization") != expected:
            self.send_response(401)
            response = {"error": "UNAUTHORIZED", "message": "Invalid credentials"}
        elif request.get("text") == "fail":
            self.send_response(502)
            response = {"error": "PROVIDER_UNAVAILABLE", "message": "Provider unavailable"}
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

    def run_cli(self, text):
        return subprocess.run(
            [sys.executable, str(self.script), text, "--backend-url", self.url,
             "--username", "demo", "--password", "secret"],
            check=False,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )

    def test_normal_entry_point_reads_translation_field(self):
        result = self.run_cli("Hello")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("سلام من الاختبار", result.stdout.strip())

    def test_normal_entry_point_displays_backend_error(self):
        result = self.run_cli("fail")
        self.assertEqual(1, result.returncode)
        self.assertIn("Provider unavailable", result.stderr)


if __name__ == "__main__":
    unittest.main()
