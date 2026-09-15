# SUBZERO

**Know what you pay. Before it charges you.**

A premium, local-first Android app that makes recurring spending visible: normalized monthly and
yearly totals, upcoming charges, a payment calendar, and deterministic insights about what could be
cut. No accounts, no bank access. Every feature works offline; the one optional online feature
(enhanced assistant answers) is off by default.

English and Arabic, with right-to-left layout.


<img src="screenshots/app.png"/>


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

The debug build carries a design-system gallery with no launcher icon of its own, so a debug
install adds one app icon, not two. Open it with:

```
adb shell am start -n com.yazanprogrammer.subzero.debug/com.subzero.app.gallery.GalleryActivity
```

## Structure

Release process: [docs/release/runbook.md](docs/release/runbook.md). QA: [docs/qa/device-checklist.md](docs/qa/device-checklist.md).
Privacy policy (English and Arabic, the page Google Play links to): [docs/privacy-policy.html](docs/privacy-policy.html),
served by GitHub Pages from the `docs/` folder.

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
and Arabic. It understands: monthly and yearly totals, spend in one category, a full category
breakdown, most and least expensive subscriptions, one subscription by name, what you rarely use,
price increases, the next charge, what is due this month or in the next N days, what you have
actually paid (this month, last month, this year, in total), what is paused or canceled, how many
you track, plus greetings and "what can you do". Anything else is answered honestly as not
understood, with suggestions — it never guesses.

Two things make it feel like a conversation: naming a service answers about that service, and a
short follow-up with a pronoun ("when is it charged?", "كم سعره؟") keeps the previous subject,
carried in `AssistantContext.focus`. Every answer offers follow-up chips, whose own localized text
becomes the next question.

Questions it cannot parse can optionally be sent to a hosted model — Settings →
Assistant → Enhanced answers, off by default, and the row only appears when a key is configured.
The model gets the earlier turns of the conversation, the subscription summary, the totals, price
changes and payments summarized by month.

**In the app** (no rebuild): Settings → Assistant → Set up provider. Pick Anthropic or an
OpenAI-compatible provider, paste the endpoint, model and your own key, then Test connection. The
key is encrypted with a key held in the Android Keystore and stored on the device only; it is sent
as a header to the endpoint you entered and nowhere else. "Forget key" clears it.

**At build time** (optional default for your own builds), in `local.properties` (git-ignored,
never committed) or the matching environment variables for CI:

```
subzero.ai.apiKey=...            # SUBZERO_AI_API_KEY
subzero.ai.baseUrl=https://...   # SUBZERO_AI_BASE_URL  (no trailing slash, https only)
subzero.ai.model=claude-opus-5   # SUBZERO_AI_MODEL
subzero.ai.provider=anthropic    # SUBZERO_AI_PROVIDER  (anthropic | openai)
```

Anything set in the app wins field by field over the build default, so a build with no key at all
is fine: the user brings their own.

`anthropic` posts to `{baseUrl}/v1/messages` with `x-api-key`; `openai` posts to
`{baseUrl}/chat/completions` with a bearer token. SUBZERO identifies itself honestly in the
`User-Agent`; it never claims to be another vendor's client, so a relay that only serves one
particular client will refuse it — use a provider that accepts API clients. **Test connection**
sends one request and maps the reply to a reason: key refused (401/403), no credit (402), no such
endpoint or model (404), rate limited (429), provider failing (5xx), or unreachable — always with
the provider's own sentence underneath.

A key is never read back into the UI: the field shows whether one is stored, and leaving it empty
keeps it. If a device cannot encrypt, the save fails loudly instead of dropping the key.

What leaves the device is a summary of the subscriptions (name, amount, cycle, category, declared
usage, next date; paused ones by name and status), the totals, price changes, monthly payment
totals, the earlier turns and the question. Notes, the display name and record ids never are. A key embedded in an APK is extractable, so a public release should point `baseUrl`
at a proxy you control rather than shipping a provider key.

## Localization

Strings live in `values/` (English) and `values-ar/` (Arabic) per module; `app/src/main/res/xml/
locales_config.xml` drives the Android 13+ per-app language picker (Settings → Preferences →
Language). Arabic plurals carry all six CLDR categories.

The type scale is script-aware (`SubzeroTypography`, `TypeScript`): Inter carries no Arabic glyphs,
so Arabic is drawn by the system Arabic face, whose descenders (the final م of "التقويم") were
clipped by Inter's tighter line box. Under Arabic, headings step down one step, every line gets at
least 1.5x leading, and negative tracking is dropped. `LocalAssistant` normalizes Arabic input
(diacritics, alef/ya variants, Arabic-Indic digits) before matching keywords.
