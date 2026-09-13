# SUBZERO

**Know what you pay. Before it charges you.**

A premium, local-first Android app that makes recurring spending visible: normalized monthly and
yearly totals, upcoming charges, a payment calendar, and deterministic insights about what could be
cut. No accounts, no bank access. Every feature works offline; the one optional online feature
(enhanced assistant answers) is off by default.

English and Arabic, with right-to-left layout.

## Requirements

- Android Studio (current stable). The Gradle daemon runs on JDK 21 (`gradle/gradle-daemon-jvm.properties`); Gradle auto-provisions it if the Studio JBR is not found
- Android SDK: platform 37, build-tools 37

## Build

```
./gradlew build          # compiles, lints (warnings are errors), runs unit tests, builds release
./gradlew assembleDebug  # debug APK only
./gradlew test           # unit tests
```

The first build downloads Gradle 9.7.1 and all dependencies.

## Structure

Release process: [docs/release/runbook.md](docs/release/runbook.md). QA: [docs/qa/device-checklist.md](docs/qa/device-checklist.md).

See [docs/architecture/00-kickoff.md](docs/architecture/00-kickoff.md) for the architecture,
module layout, data model, decisions and roadmap.

```
build-logic/   Gradle convention plugins
app/           application shell (Navigation 3 + bottom navigation)
core/          common, domain (pure JVM), data, designsystem, navigation, notifications, ai, testing
feature/       onboarding, home, subscriptions, calendar, insights, settings, assistant
```

## Assistant

The assistant answers from the data on the device (`core:domain`, `LocalAssistant`), in English
and Arabic. Questions it cannot parse can optionally be sent to a hosted model — Settings →
Assistant → Enhanced answers, off by default, and the row only appears when a key is configured.

Configure the provider in `local.properties` (git-ignored, never committed) or through the
matching environment variables for CI:

```
subzero.ai.apiKey=...            # SUBZERO_AI_API_KEY
subzero.ai.baseUrl=https://...   # SUBZERO_AI_BASE_URL  (no trailing slash, https only)
subzero.ai.model=claude-opus-5   # SUBZERO_AI_MODEL
subzero.ai.provider=anthropic    # SUBZERO_AI_PROVIDER  (anthropic | openai)
```

`anthropic` posts to `{baseUrl}/v1/messages` with `x-api-key`; `openai` posts to
`{baseUrl}/chat/completions` with a bearer token. Settings → Assistant → **Test connection**
sends one request and shows the provider's error verbatim if it fails.

What leaves the device is a summary of the active subscriptions (name, amount, cycle, category,
declared usage, next date) and the question. Notes, the display name, ids and payment history
never are. A key embedded in an APK is extractable, so a public release should point `baseUrl`
at a proxy you control rather than shipping a provider key.

## Localization

Strings live in `values/` (English) and `values-ar/` (Arabic) per module; `app/src/main/res/xml/
locales_config.xml` drives the Android 13+ per-app language picker (Settings → Preferences →
Language). Arabic plurals carry all six CLDR categories. `LocalAssistant` normalizes Arabic input
(diacritics, alef/ya variants, Arabic-Indic digits) before matching keywords.
