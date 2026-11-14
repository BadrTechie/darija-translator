# Five-minute demo script

Have the backend already running, Chrome open on a sample page, a PowerShell window in the Python client folder, and the physical iPhone ready with Expo Go SDK 58. The iPhone and laptop must be connected to the same Wi-Fi. Keep API keys, passwords, secure prompts, authentication headers, and all other secrets outside the recording frame.

## Pre-recording checklist

- Liberty already running
- Gemini key and passwords entered before recording
- Health response ready
- UML SVG tabs ready
- Source files ready in the IDE
- Python and PHP clients ready
- Chrome extension loaded
- iPhone Expo app ready
- GitHub page ready
- No secrets visible

## 0:00–0:35 — Goal and architecture

- State: “One authenticated Java 17 backend translates submitted material into Moroccan Darija in Arabic script for four clients.”
- Show `docs/uml/deployment-diagram.svg`, then briefly trace the normal request in `docs/uml/translation-sequence.svg`.
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

- On a webpage, highlight a sentence and right-click **Translate to Darija**.
- Show that the side panel opens alongside the page, the selection appears automatically, and loading changes to the translation.
- Briefly show `manifest.json` (`manifest_version: 3`, context menu, side panel, storage) and `service-worker.js` selection transfer.

## 3:50–4:30 — React Native on physical iPhone

- Show the app on the physical iPhone in Expo Go SDK 58, with backend URL `http://<CURRENT-PC-WIFI-IP>:9080` and the password masked.
- Enter English text, tap **Translate to Darija**, and show the readable right-to-left Darija result while the backend remains running.
- Mention that the password is runtime-only and `GEMINI_API_KEY` stays in the Java backend; do not expose either credential or any authentication header.
- If live iPhone mirroring is unavailable, insert a short iPhone screen recording into the desktop recording for this segment.

## 4:30–5:00 — Design and submission close

- Show `docs/uml/class-diagram.svg` and name the resource, client, configuration, and exception responsibilities.
- Briefly show the public repository contents: source, `mvnw`/`mvnw.cmd`, clients, diagrams, README, and docs; point out absent secrets, `target`, `node_modules`, and keystores.
- Close by stating that the GitHub repository is already publicly available at <https://github.com/BadrTechie/darija-translator>.
