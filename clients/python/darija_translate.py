#!/usr/bin/env python3
"""Small dependency-free CLI for the Darija Translator backend."""

from __future__ import annotations

import argparse
import base64
import getpass
import json
import os
import socket
import sys
import urllib.error
import urllib.request


DEFAULT_BACKEND_URL = "http://localhost:9080"


class CliError(RuntimeError):
    """Expected error that can be shown directly to the CLI user."""


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
            try:
                data = json.load(response)
            except (UnicodeDecodeError, json.JSONDecodeError) as error:
                raise CliError("Backend returned unreadable JSON.") from error
    except urllib.error.HTTPError as error:
        message = _error_message(error)
        if error.code == 401:
            raise CliError(
                "Authentication rejected (HTTP 401). Check the backend username and password."
            ) from error
        if error.code == 400:
            raise CliError(f"Invalid input (HTTP 400): {message}") from error
        if error.code in {502, 503, 504}:
            raise CliError(f"Translation provider error (HTTP {error.code}): {message}") from error
        raise CliError(f"Backend error (HTTP {error.code}): {message}") from error
    except urllib.error.URLError as error:
        raise CliError(f"Cannot reach the backend: {error.reason}") from error
    except (TimeoutError, socket.timeout) as error:
        raise CliError("Cannot reach the backend: the request timed out.") from error

    translation = data.get("translation") if isinstance(data, dict) else None
    if not isinstance(translation, str) or not translation.strip():
        raise CliError("Backend response did not contain a non-empty 'translation' field.")
    return translation.strip()


def _error_message(error: urllib.error.HTTPError) -> str:
    try:
        body = json.loads(error.read().decode("utf-8"))
        if isinstance(body, dict) and isinstance(body.get("message"), str):
            return body["message"]
    except (UnicodeDecodeError, json.JSONDecodeError):
        pass
    return error.reason


def _read_text(explicit_text: str | None) -> str:
    if explicit_text is not None:
        return explicit_text
    if sys.stdin.isatty():
        return input("Text to translate: ")
    return sys.stdin.read()


def _read_source_language(explicit_language: str | None) -> str:
    if explicit_language is not None:
        return explicit_language.strip() or "unspecified"
    if sys.stdin.isatty():
        return input("Source language [unspecified]: ").strip() or "unspecified"
    return "unspecified"


def _read_username(explicit_username: str | None) -> str:
    configured = explicit_username or os.environ.get("TRANSLATOR_USERNAME")
    if configured:
        return configured
    if sys.stdin.isatty():
        username = input("Backend username: ").strip()
        if username:
            return username
    raise ValueError("Backend username is required; enter it or set TRANSLATOR_USERNAME.")


def _read_password() -> str:
    configured = os.environ.get("TRANSLATOR_PASSWORD")
    if configured:
        return configured
    if not sys.stdin.isatty():
        raise ValueError(
            "Set TRANSLATOR_PASSWORD for non-interactive use; interactive runs prompt securely."
        )
    password = getpass.getpass("Backend password: ")
    if not password:
        raise ValueError("Backend password must not be blank.")
    return password


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Translate text into Moroccan Darija in Arabic script."
    )
    parser.add_argument("text", nargs="?", help="Text to translate; prompts when omitted")
    parser.add_argument("--source-language", help="Prompts when omitted in an interactive terminal")
    parser.add_argument(
        "--backend-url",
        default=os.environ.get("TRANSLATOR_BACKEND_URL", DEFAULT_BACKEND_URL),
        help=f"Backend root URL (default: {DEFAULT_BACKEND_URL})",
    )
    parser.add_argument("--username", help="Defaults to TRANSLATOR_USERNAME")
    parser.add_argument("--timeout", type=float, default=30.0, help="Request timeout in seconds")
    return parser


def main(argv: list[str] | None = None) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    args = build_parser().parse_args(argv)
    text = _read_text(args.text)
    if not text.strip():
        print("Invalid input: text must not be blank.", file=sys.stderr)
        return 2
    if args.timeout <= 0:
        print("Invalid input: timeout must be greater than zero.", file=sys.stderr)
        return 2

    try:
        source_language = _read_source_language(args.source_language)
        username = _read_username(args.username)
        password = _read_password()
        result = translate(
            args.backend_url,
            username,
            password,
            text,
            source_language,
            args.timeout,
        )
    except (CliError, ValueError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1

    print(result)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
