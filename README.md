# SUBZERO

**Know what you pay. Before it charges you.**

A premium, local-first Android app that makes recurring spending visible: normalized monthly and
yearly totals, upcoming charges, a payment calendar, and deterministic insights about what could be
cut. No accounts, no network, no bank access.

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
core/          common, domain (pure JVM), data, designsystem, navigation, testing
feature/       onboarding, home, subscriptions, calendar, insights, settings
```
