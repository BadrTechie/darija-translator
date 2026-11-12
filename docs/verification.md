# Verification record

Date: 2026-11-12 (Africa/Casablanca)

| Deliverable | Check | Result |
|---|---|---|
| Backend Java 17 source | `mvnw.cmd clean test` using Temurin 17 | Passed |
| Backend offline provider integration | IntelliJ integrated terminal: `mvnw.cmd verify` with local HTTP stub and Temurin 17 | Passed: BUILD SUCCESS; 9 tests, 0 failures/errors; 10.771 s |
| Python CLI normal entry point | Subprocess tests against authenticated local stub | Passed (2 tests) |
| PHP form | `php -l index.php` and browser form | Blocked: PHP executable is not installed on this machine |
| Chrome Manifest V3 | JSON parse and `node --check` on both scripts | Passed static checks; unpacked Chrome UI requires manual run |
| React Native API layer | `node --test test/api.test.js` | Passed (2 tests) |
| React Native Expo UI | `npm install` and emulator/device | Blocked: `npm install` produced no output/files after several minutes both sandboxed and with network permission; stopped. Run it on the laptop, then `npm run android` |
| Real Gemini translation | Authenticated backend request | Not run: no API key was read or available to this session |
| UML exports | SVG XML/viewability inspection | Passed: both SVG exports parsed as valid XML; sources reflect implemented classes/nodes |
| Submission archive | Open/list and exclusion audit | Passed after final packaging: required paths present and excluded patterns absent; test-only credentials are explicitly named `server.env.test` |

No real provider or Basic-auth secret is recorded in this file. See the root README for exact personal verification commands.
