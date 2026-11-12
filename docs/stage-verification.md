# Staged verification record

## 1. Authentication — PASSED

- Latest IntelliJ/Liberty report: 9 tests run, 0 failures, 0 errors, 0 skipped.
- All six authentication/endpoint checks passed: public health, missing credentials, incorrect credentials, valid credentials, blank authenticated text, and missing authenticated text.
- Valid credentials reached the translation provider stub and returned success; no HTTP 403 occurred.
- Liberty created/loaded its keystore, started the application, and stopped cleanly.
- IntelliJ's Maven runner reached the test phase and ended with `BUILD SUCCESS`; there was no separate dependency or network failure to record for this run.
- The old 403 branch therefore does not require further diagnosis.

## 2. Gemini backend integration — PASSED (automated)

- Official API check: `POST /v1beta/models/{model}:generateContent` with `x-goog-api-key`; default updated to configurable `gemini-3.5-flash-lite`, currently listed for free-tier use and translation/simple processing.
- Backend uses Java 17 `HttpClient`, Jakarta JSON-P, an environment-only key, and a 20-second default timeout.
- HTTP 200 success exposes the translated text in `translation`; placeholder status/messages are absent.
- IntelliJ Maven run: `BUILD SUCCESS`; 9 tests, 0 failures, 0 errors, 0 skipped (12.652 s).
- Stub assertions cover translation success, provider rejection/empty response/timeout, 401 authentication, and 400 validation.
- A provider-call counter proves unauthenticated, incorrectly authenticated, blank-text, and missing-text requests never reach the provider.

### Stage 2 live-call status — NOT RUN

- No usable `GEMINI_API_KEY` was present in the process environment or ignored Liberty `server.env`; demo Basic credentials were also absent.
- No secret was read or printed. The README contains exact private PowerShell steps and the required sentence for the personal live call.

## 3. Python CLI — PASSED

- Built on the existing standard-library `urllib` client; no pip package or download is required.
- Normal no-argument use prompts for text, source language, username when needed, and a hidden backend password via `getpass`.
- Backend URL defaults to `http://localhost:9080` and is configurable with `--backend-url` or `TRANSLATOR_BACKEND_URL`.
- Windows UTF-8 output displays the response `translation` as readable Arabic.
- Normal entry point exercised against a local authenticated HTTP stub because Liberty was not running.
- Subprocess coverage passed for Arabic success, HTTP 401 credentials, HTTP 400 input, HTTP 502 provider failure, blank local input, and unreachable backend; a focused test confirms the private `getpass` prompt.
- User-verified live on Windows: `Where can I buy a train ticket to Casablanca?` was translated through the running backend as `فين نقدر نشري تيكي ديال القطر ل كازا؟`.

## 4. PHP form — PASSED (USER-VERIFIED)

- The form accepts text, source language, backend username, and a password input that is never repopulated or rendered.
- PHP sends JSON to `POST /api/translate` with Basic authentication and renders only an escaped, non-empty `translation` value using `lang="ar"` and `dir="rtl"`.
- Dedicated messages cover rejected credentials, invalid input, provider failures, malformed/empty success responses, and an unreachable backend.
- PHP, Winget, Chocolatey, and Scoop were unavailable to the automated environment. Docker CLI existed but its engine was not running, so no agent-run lint or local PHP server was claimed.
- The README gives the minimal official PHP ZIP/Visual C++ runtime setup and exact lint/server commands.
- User-verified live on Windows: submitting `How much is a train ticket to Rabat?` through the local form returned readable Arabic, and the rendered password field was empty afterward.

## 5. Chrome Manifest V3 side panel — AUTOMATED CHECKS PASSED; LIVE UI PENDING

- Chrome 116+ MV3 manifest declares context-menu, side-panel, storage, and HTTP(S) host access.
- The context-menu action stores the exact selection in extension session storage and opens the side panel for the originating tab.
- A missing-credentials flow keeps the selection visible; saving the URL and Basic credentials automatically resumes translation.
- New selections replace the panel text, abort any older in-flight request, and render only the newest Darija result with `lang="ar"` and `dir="rtl"`.
- Backend URL/username persist locally; the password exists only in in-memory extension session storage. The Gemini key remains backend-only.
- `node --check` passed for both JavaScript entry points. `node --test test-extension.mjs` passed 3/3 interaction tests.
- Installed Chrome 157 exceeds the declared Chrome 116 minimum. Loading the unpacked extension, side-panel appearance, and one authenticated live translation remain user verification steps because they require changing the user's browser and entering the private backend password.
- Live-debug fix: the fetch now uses `credentials: "omit"` while retaining its explicit Basic header, preventing Chrome from owning the backend's 401 challenge. An isolated Chrome 157 probe sent one POST with the header present and returned one 401 to JavaScript; no header value was recorded.
- The panel test verifies one request per Save, URL `http://localhost:9080/api/translate`, POST method, explicit-header presence, 401 loading completion, retained selection, cleared session password, reopened settings, and no automatic retry. Correcting the password and saving starts exactly one new request.

## Dev/test Liberty configuration isolation — PASSED

- Root cause: dev mode and integration tests shared `darijaTranslatorServer`; the integration execution copied `server.env.test` into that persistent server directory, so later dev starts reused test credentials and the stub URL.
- The `integration-tests` Maven profile now owns the test lifecycle, test `server.env`, and `darijaTranslatorIntegrationServer`.
- Plain `liberty:dev` uses the separate `darijaTranslatorDevServer` and inherits normal credentials/provider settings from its PowerShell process.
- Test result: `BUILD SUCCESS`; 9 tests, 0 failures/errors/skips on the isolated integration server.
- Post-test dev verification: `darijaTranslatorDevServer` started, public health returned `UP`, and its generated env file contained no translator credentials, Gemini key/model, or stub URL.
- Stale legacy and temporary verification `server.env` files under `target` were removed. The verification dev server was stopped and port 9080 was confirmed free.
- A sandbox-only dev attempt failed before startup because that account could not access Maven Central; the cached host-context dev run succeeded and is the configuration evidence.
