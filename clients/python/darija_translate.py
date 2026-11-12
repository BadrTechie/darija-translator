#!/usr/bin/env python3
"""Small dependency-free CLI for the Darija Translator backend."""

from __future__ import annotations

import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.request


def translate(base_url: str, username: str, password: str, text: str,
              source_language: str, timeout: float = 30.0) -> str:
    endpoint = base_url.rstrip("/") + "/api/translate"
    credentials = base64.b64encode(
        f"{username}:{password}".encode("utf-8")
    ).decode("ascii")
    payload = json.dumps(
        {"text": text, "sourceLanguage": source_language},
        ensure_ascii=False,
    ).encode("utf-8")
    request = urllib.request.Request(
        endpoint,
        data=payload,
        method="POST",
        headers={
            "Authorization": f"Basic {credentials}",
            "Content-Type": "application/json; charset=utf-8",
            "Accept": "application/json",
        },
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            data = json.load(response)
    except urllib.error.HTTPError as error:
        message = _error_message(error)
        raise RuntimeError(f"Backend returned HTTP {error.code}: {message}") from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"Cannot reach the backend: {error.reason}") from error

    translation = data.get("translation") if isinstance(data, dict) else None
    if not isinstance(translation, str) or not translation.strip():
        raise RuntimeError("Backend response did not contain a non-empty 'translation' field.")
    return translation.strip()


def _error_message(error: urllib.error.HTTPError) -> str:
    try:
        body = json.loads(error.read().decode("utf-8"))
        if isinstance(body, dict) and isinstance(body.get("message"), str):
            return body["message"]
    except (UnicodeDecodeError, json.JSONDecodeError):
        pass
    return error.reason


def _required_setting(name: str, explicit_value: str | None) -> str:
    value = explicit_value or os.environ.get(name)
    if value:
        return value
    raise ValueError(f"Set {name} or pass its matching command-line option.")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Translate text into Moroccan Darija in Arabic script."
    )
    parser.add_argument("text", nargs="?", help="Text to translate; stdin is used when omitted")
    parser.add_argument("--source-language", default="unspecified")
    parser.add_argument("--backend-url", help="Defaults to TRANSLATOR_BACKEND_URL")
    parser.add_argument("--username", help="Defaults to TRANSLATOR_USERNAME")
    parser.add_argument("--password", help="Defaults to TRANSLATOR_PASSWORD")
    return parser


def main(argv: list[str] | None = None) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    args = build_parser().parse_args(argv)
    text = args.text if args.text is not None else sys.stdin.read()
    if not text.strip():
        print("Text must not be blank.", file=sys.stderr)
        return 2

    try:
        result = translate(
            _required_setting("TRANSLATOR_BACKEND_URL", args.backend_url),
            _required_setting("TRANSLATOR_USERNAME", args.username),
            _required_setting("TRANSLATOR_PASSWORD", args.password),
            text,
            args.source_language,
        )
    except (RuntimeError, ValueError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1

    print(result)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
