# Darija Translator

Darija Translator is a Java 17/Jakarta EE application that translates submitted text into natural Moroccan Darija in Arabic script. One authenticated backend serves four clients: a Python CLI, PHP form, Chrome Manifest V3 side panel, and Expo/React Native app.

The Gemini API key exists only in the backend process. No client accepts or stores it.

## Architecture and provider choice

Open Liberty exposes public `GET /api/health` and Basic-authenticated `POST /api/translate`. `GeminiTranslationClient` uses Java 17's built-in `java.net.http.HttpClient`; no provider SDK was added. The prompt treats input as untrusted material to translate and asks for Arabic-script Darija only.

The default model is `gemini-3.5-flash-lite`, because Google's official pricing page currently describes it as optimized for translation/simple processing and lists free-tier input and output. Model access and quota depend on the Google AI Studio project, so `GEMINI_MODEL` is configurable. Confirm the model available to your own project in AI Studio before the live demo.

Official references:

- [Gemini REST text generation and `generateContent`](https://ai.google.dev/gemini-api/docs/generate-content/text-generation)
- [Gemini model names and stability](https://ai.google.dev/gemini-api/docs/models)
- [Gemini Developer API pricing/free tier](https://ai.google.dev/gemini-api/docs/pricing)
- [Gemini rate limits and project tiers](https://ai.google.dev/gemini-api/docs/rate-limits)
- [Gemini API errors](https://ai.google.dev/gemini-api/docs/api-errors)

## API contract

### Health

`GET /api/health` is public and returns HTTP 200:

```json
{"service":"darija-translator-backend","status":"UP"}
```

### Translate

`POST /api/translate` requires HTTP Basic authentication and `Content-Type: application/json`.

Request shape (preserved from the starter project):

```json
{"text":"Hello, how are you?","sourceLanguage":"English"}
```

`sourceLanguage` is optional and becomes `unspecified`. Missing or blank `text` returns HTTP 400. Every client must read the translated text from the same `translation` field in the HTTP 200 response:

```json
{
  "translation": "سلام، كيداير؟",
  "sourceLanguage": "English",
  "targetLanguage": "Moroccan Darija (Arabic script)",
  "model": "gemini-3.5-flash-lite"
}
```

Provider failures are JSON objects with `error` and `message`:

| Situation | HTTP | `error` |
|---|---:|---|
| Backend key/config missing | 503 | `PROVIDER_NOT_CONFIGURED` |
| Provider timeout | 504 | `PROVIDER_TIMEOUT` |
| Provider rejects request/quota/key | 502 | `PROVIDER_REJECTED_REQUEST` |
| Provider returns no usable text | 502 | `PROVIDER_EMPTY_RESPONSE` |
| Network/provider unavailable | 502 | `PROVIDER_UNAVAILABLE` |

Incorrect or absent Basic credentials return 401 with `WWW-Authenticate: Basic`. Basic auth is not encryption; use HTTPS for any non-local deployment.

## Windows prerequisites

- JDK 17 (the project compiles with `--release 17`)
- Internet on the first Maven/Liberty build
- Python 3.10+ for the CLI
- PHP 8+ with `allow_url_fopen=1` for the form
- Node.js 20+ plus an Android/iOS device or emulator for Expo
- Chrome for the unpacked Manifest V3 extension

Example Java setup in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Users\lcbad\.jdks\temurin-17.0.20.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

## Backend credentials and Gemini configuration

Set variables in the PowerShell window that starts Liberty. The prompts keep secrets out of shell history:

```powershell
$env:TRANSLATOR_DEMO_USERNAME = 'local-demo-user'
$demoSecret = Read-Host 'Basic-auth password' -AsSecureString
$env:TRANSLATOR_DEMO_PASSWORD = [System.Net.NetworkCredential]::new('', $demoSecret).Password
$keyStoreSecret = Read-Host 'Liberty keystore password' -AsSecureString
$env:keystore_password = [System.Net.NetworkCredential]::new('', $keyStoreSecret).Password
$geminiSecret = Read-Host 'Gemini API key' -AsSecureString
$env:GEMINI_API_KEY = [System.Net.NetworkCredential]::new('', $geminiSecret).Password
$env:GEMINI_MODEL = 'gemini-3.5-flash-lite'
```

Alternatively, copy `backend\src\main\liberty\config\server.env.example` to the ignored `server.env` and replace its placeholders. Never commit `server.env`, a real `.env`, keystores, screenshots containing keys, or terminal output that exposes a key.

Optional backend settings:

- `GEMINI_MODEL` (default `gemini-3.5-flash-lite`)
- `GEMINI_API_BASE_URL` (default official `v1beta` base; overridden only by offline tests)
- `GEMINI_TIMEOUT_SECONDS` (default 20; valid 1–300)

## Build, test, and run the backend

```powershell
Set-Location 'C:\Users\lcbad\Downloads\darija-translator\backend'
.\mvnw.cmd clean verify
.\mvnw.cmd liberty:dev
```

`clean verify` starts Open Liberty, runs the authenticated endpoint suite against a local Gemini-shaped HTTP stub, checks success, 400, 401, provider 429/rejection, empty response, and timeout, then stops Liberty. It needs no real API key and no provider network access after dependencies are cached.

Keep that first terminal open. In a second PowerShell window, verify health:

```powershell
Invoke-RestMethod 'http://localhost:9080/api/health'
```

### One real authenticated translation

With the backend running in the first terminal, configure the same Basic-auth demo credentials in the second terminal (the Gemini key stays only in the backend terminal), then call the endpoint:

```powershell
$env:TRANSLATOR_DEMO_USERNAME = 'local-demo-user'
$requestSecret = Read-Host 'Basic-auth password used by the backend' -AsSecureString
$env:TRANSLATOR_DEMO_PASSWORD = [System.Net.NetworkCredential]::new('', $requestSecret).Password
$pair = "$($env:TRANSLATOR_DEMO_USERNAME):$($env:TRANSLATOR_DEMO_PASSWORD)"
$basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
$body = @{ text = 'I left my phone at home. Can you call me after class?'; sourceLanguage = 'English' } | ConvertTo-Json -Compress
$result = Invoke-RestMethod 'http://localhost:9080/api/translate' -Method Post `
  -Headers @{ Authorization = "Basic $basic" } -ContentType 'application/json' -Body $body
$result.translation
```

This prints the translation, never the Gemini key. If it returns `PROVIDER_REJECTED_REQUEST`, check the model shown in Google AI Studio, quota, and API-key project, then set `GEMINI_MODEL` accordingly and restart Liberty.

## Client 1: Python CLI

Uses only Python's standard library.

```powershell
Set-Location 'C:\Users\lcbad\Downloads\darija-translator\clients\python'
$env:TRANSLATOR_BACKEND_URL = 'http://localhost:9080'
$env:TRANSLATOR_USERNAME = 'local-demo-user'
$clientSecret = Read-Host 'Basic-auth password used by the backend' -AsSecureString
$env:TRANSLATOR_PASSWORD = [System.Net.NetworkCredential]::new('', $clientSecret).Password
python .\darija_translate.py 'Hello, how are you?' --source-language English
python -m unittest -v test_cli.py
```

Text may also be piped on stdin. Options override the environment; avoid putting a password directly in command history.

## Client 2: PHP form

In the same configured PowerShell session:

```powershell
Set-Location 'C:\Users\lcbad\Downloads\darija-translator\clients\php'
$env:TRANSLATOR_BACKEND_URL = 'http://localhost:9080'
$env:TRANSLATOR_USERNAME = 'local-demo-user'
$clientSecret = Read-Host 'Basic-auth password used by the backend' -AsSecureString
$env:TRANSLATOR_PASSWORD = [System.Net.NetworkCredential]::new('', $clientSecret).Password
php -S 127.0.0.1:8081
```

Open `http://127.0.0.1:8081`. The PHP server calls the backend; the browser never receives credentials. Stop it with Ctrl+C.

## Client 3: Chrome Manifest V3 side panel

1. Open `chrome://extensions`, enable **Developer mode**, and choose **Load unpacked**.
2. Select `clients\chrome-extension`.
3. Open any ordinary webpage, highlight text, right-click, and choose **Translate selection to Darija**.
4. The side panel opens beside the page, transfers the selection, and translates automatically after settings exist.
5. On first use, expand **Backend settings**, enter `http://localhost:9080` plus the Basic-auth username/password, and save. Repeat the selection action.

The extension stores demo Basic credentials in Chrome extension local storage. Do not use valuable/reused credentials. The Gemini key is never present in the extension.

## Client 4: React Native (Expo)

```powershell
Set-Location 'C:\Users\lcbad\Downloads\darija-translator\clients\react-native'
npm install
$env:EXPO_PUBLIC_BACKEND_URL = 'http://10.0.2.2:9080'
$env:EXPO_PUBLIC_TRANSLATOR_USERNAME = 'local-demo-user'
$mobileSecret = Read-Host 'Basic-auth password used by the backend' -AsSecureString
$env:EXPO_PUBLIC_TRANSLATOR_PASSWORD = [System.Net.NetworkCredential]::new('', $mobileSecret).Password
npm test
npm run android
```

Backend URL by target:

- Android Studio emulator: `http://10.0.2.2:9080`
- iOS simulator on the same Mac: `http://127.0.0.1:9080`
- Physical phone: `http://<YOUR-PC-LAN-IP>:9080` (same Wi-Fi; allow inbound TCP 9080 in Windows Firewall)
- Expo Web on the same PC: `http://localhost:9080`

The app exposes editable backend URL and demo credential fields, with visible loading, result, and error states. Expo `EXPO_PUBLIC_*` values are bundled into the app, so use only disposable demo Basic credentials. They are unrelated to and must never contain `GEMINI_API_KEY`.

## Diagrams and demo

- [UML class diagram source](docs/uml/class-diagram.puml) and [viewable SVG](docs/uml/class-diagram.svg)
- [UML deployment diagram source](docs/uml/deployment-diagram.puml) and [viewable SVG](docs/uml/deployment-diagram.svg)
- [Timed five-minute demo script](docs/demo-script.md)
- [Verification record](docs/verification.md)
- [IntelliJ Maven output evidence](docs/intellij-maven-output.txt)

## Submission archive

The prepared archive is `submission\Badreddine_Lechhab_project2.zip`. It contains tracked source, both Maven wrappers, clients, UML source/exports, demo script, verification record, and README. It excludes `.git`, IDE state, real secrets, generated keystores, `target`, `node_modules`, caches, and nested ZIP files.

After making later changes, rebuild it from the repository root with the command documented in `submission\README.txt`, then inspect it with `Expand-Archive` or Windows Explorer before submission.
