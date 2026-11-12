# Five-minute demo script

Have the backend already running, Chrome open on a sample page, a PowerShell window in the Python client folder, and the mobile app open in the emulator. Keep secrets outside the recording frame.

## 0:00–0:35 — Goal and architecture

- State: “One authenticated Java 17 backend translates submitted material into Moroccan Darija in Arabic script for four clients.”
- Show `docs/uml/deployment-diagram.svg`.
- Point out that only the backend holds `GEMINI_API_KEY`; clients use Basic auth and all read `translation`.

## 0:35–1:35 — Backend source

- Open `TranslatorResource.translate()` and show unchanged blank-text HTTP 400 handling, then success/error response mapping.
- Open `GeminiTranslationClient`: show JDK `HttpClient`, `x-goog-api-key`, timeout, prompt-injection boundary, and `candidates[0].content.parts[].text` extraction.
- Open `ProviderConfiguration`: show configurable model, base URL, timeout, and environment-only key.
- Briefly show `web.xml` to confirm only `POST /api/translate` is protected and mention public health.

## 1:35–2:15 — Offline automated verification

- Run `backend\.\mvnw.cmd clean verify -Pintegration-tests` beforehand or show its final `BUILD SUCCESS`.
- Open `BackendEndpointIT`: show the local Gemini-shaped stub and tests for success, provider rejection, empty response, timeout, 400, and 401.
- Run `Invoke-RestMethod http://localhost:9080/api/health` to show the public endpoint.

## 2:15–3:00 — Live provider and Python/PHP clients

- Run the README’s real authenticated PowerShell request and show only `$result.translation`.
- Run `python .\darija_translate.py 'Welcome to Morocco' --source-language English`; point to Arabic output.
- Open the PHP page at `http://127.0.0.1:8081`, submit one sentence, and show the same response field rendered safely.

## 3:00–3:50 — Chrome side panel

- On a webpage, highlight a sentence and right-click **Translate selection to Darija**.
- Show that the side panel opens alongside the page, the selection appears automatically, and loading changes to the translation.
- Briefly show `manifest.json` (`manifest_version: 3`, context menu, side panel, storage) and `service-worker.js` selection transfer.

## 3:50–4:30 — React Native

- Show backend URL `10.0.2.2:9080` in the Android emulator.
- Enter text and tap **Translate to Darija**; point out loading indicator, Arabic result, and then intentionally blank input or stop backend briefly to demonstrate the error state.
- Mention physical phones use the PC’s LAN IP and same Wi-Fi.

## 4:30–5:00 — Design and submission close

- Show `docs/uml/class-diagram.svg` and name the resource, client, configuration, and exception responsibilities.
- Show the ZIP contents: source, `mvnw`/`mvnw.cmd`, clients, diagrams, README, and docs; point out absent secrets, `target`, `node_modules`, and keystores.
- Close with the GitHub repository link and state that the separate five-minute screen recording is the remaining submitted artifact.
