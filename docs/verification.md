# Verification record

Date: 2026-11-13 (Africa/Casablanca)

| Deliverable | Check | Result |
|---|---|---|
| Backend Java 17 source | `mvnw.cmd clean test` using Temurin 17 | Passed |
| Backend offline provider integration | IntelliJ integrated terminal: `mvnw.cmd verify -Pintegration-tests` with local HTTP stub and Temurin 17 | Passed: BUILD SUCCESS; 9 tests, 0 failures/errors; isolated integration server |
| Python CLI normal entry point | Seven automated client tests plus user-run authenticated backend translation | Passed; live result: `فين نقدر نشري تيكي ديال القطر ل كازا؟` |
| PHP form | Source/security review plus user-run authenticated browser form | Passed live; readable Arabic returned and password field cleared |
| Chrome Manifest V3 | Syntax checks, 3 interaction tests, isolated challenged-fetch probe, and Chrome 157 live UI | Passed with extension 1.1.1: two selections produced two different results; `Example Domain` → `نطاق كمثال` |
| React Native API layer | `node --test test/*.test.js` | Passed (3 tests): exact authenticated POST and JSON body, source normalization, error handling |
| React Native Expo UI | SDK 58 dependency alignment, Expo Doctor, API tests, and cache-cleared `expo export --platform ios` | Passed automated checks: Expo 58.0.25 / React Native 0.88.1 / React 19.3.0, 20/20 Doctor checks, 3/3 API tests, 597-module iOS bundle; physical iPhone Expo Go run remains user verification |
| Real Gemini translation | Authenticated backend through normal clients | User-verified live through both the Python CLI and PHP form; no key was exposed |
| UML exports | SVG XML/viewability inspection | Passed: both SVG exports parsed as valid XML; sources reflect implemented classes/nodes |
| Submission archive | Open/list and exclusion audit | Passed after final packaging: required paths present and excluded patterns absent; test-only credentials are explicitly named `server.env.test` |

No real provider or Basic-auth secret is recorded in this file. See the root README for exact personal verification commands.
