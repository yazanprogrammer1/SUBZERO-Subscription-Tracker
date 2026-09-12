# SUBZERO — Technical Kickoff (Phase 0)

Status: **approved 2026-09-12**. This document records the architecture and the product decisions
taken before Phase 1. Later ADRs live next to it as `NN-<topic>.md`.

## Product in one paragraph

SUBZERO is a local-first recurring-spend intelligence app. It answers, in order: *what am I paying*
(normalized monthly/yearly totals), *what is about to charge me* (calendar + reminders), and *what can
I cut* (deterministic insights that end in an action). Every number is reproducible from data the user
entered, and nothing leaves the device.

## MVP boundary

**In:** manual subscriptions (add/edit/delete/pause/cancel), monthly + yearly normalization, correct
billing-date math, Home dashboard, list with search/filter/sort, detail with price + spending history,
calendar, deterministic insights engine, opt-in notifications, settings with export/delete, onboarding,
tests, R8 release build.

**Out of v1:** AI chat, automatic detection (notifications/email/bank/OCR/import), accounts and sync,
payments/premium, FX conversion, remote catalog and brand logos, app-usage-based insights, analytics
SDK, tablet two-pane layouts.

## Architecture

Clean Architecture + MVVM, unidirectional data flow, offline-only.

```
Compose UI (feature modules)  →  ViewModel (StateFlow<UiState>)
        ↓
Use cases + InsightRules (core:domain, pure JVM)
        ↓
Repository interfaces (core:domain)  ←  implementations (core:data: Room + DataStore)
```

Rules:
- `core:domain` is a pure JVM module. All financial and date logic lives there and is unit-tested.
- Time is injected (`java.time.Clock`); nothing calls `LocalDate.now()` directly.
- Money is `Long` minor units + ISO-4217 code. Fractional math goes through `BigDecimal`
  (`HALF_EVEN`) at one defined step.
- Billing dates are `LocalDate`, anchored on the first billing date; next dates are the *nth*
  occurrence from the anchor (31st stays 31st after February).
- Insights are a list of `InsightRule` objects evaluated by one use case.
- UI state is immutable; one-shot effects use a `Channel`.
- Navigation 3 with per-tab back stacks owned by the app module; features see only `Navigator`.
- Hilt (KSP) for DI; convention plugins in `build-logic/` for uniform module config.

## Modules

```
build-logic/convention   Gradle convention plugins
app                      Application, MainActivity, Nav3 shell, bottom nav
core/common              dispatchers, Clock, application scope
core/domain   [JVM]      models, Money, BillingCycle, repositories (interfaces), use cases, rules
core/data                Room, DataStore, repository implementations, migrations, schemas/
core/designsystem        tokens (colors, type, spacing, shapes, motion) + Subzero* components
core/navigation          NavKeys + Navigator contract
core/notifications       (Phase 9) WorkManager + notification builders
core/testing             fakes, rules, fixtures
feature/{onboarding,home,subscriptions,calendar,insights,settings}
```

Deviations from the original spec list: `core-model` merged into `core:domain`; `core-database`
is `core:data`; `feature-add-subscription` lives inside `feature:subscriptions`; `core-network` and
`core-security` are created only when they have a reason to exist.

## Decisions (approved)

| # | Decision | Choice |
|---|---|---|
| M1 | Navigation library | **Navigation 3** (1.1.7). Back stack owned by the app for custom transitions and shared elements. |
| M2 | minSdk | **26**. Native `java.time`, notification channels, no desugaring. |
| M3 | Module layout | As listed above. |
| M4 | Multi-currency | Money model supports any ISO currency. Dashboard totals aggregate the **home currency** only and show an explicit "N in other currencies not included" indicator. FX is post-MVP. |
| M5 | Usage data | **User-declared** (Daily / Weekly / Monthly / Rarely / Unknown). No usage-stats permission. Insight wording reflects the source. |
| M6 | Service icons | **Monogram avatars** with deterministic color; name-only autocomplete list. Real logos arrive with the remote catalog. |
| M7 | Spending history | **Hybrid**: pre-install charges estimated from anchor + cycle + price history (labelled), post-install charges recorded by the daily rollover. |
| M8 | Git | Local `main`; no remote configured yet. |

Additional decisions made during Phase 1 (routine, recorded for traceability):
- `applicationId = com.subzero.app`, root package `com.subzero`.
- `compileSdk 37`, `targetSdk 37` (lint enforces latest target; API 37 behavior changes are
  verified on an emulator in Phase 12), `minSdk 26`.
- JVM target 17 for all modules (D8 desugars language features for every minSdk).
- Kotlin compiler warnings and lint warnings are errors. Override locally with
  `-PwarningsAsErrors=false` only for exploration, never in commits.
- No `INTERNET` permission in the MVP. Backups include the database and DataStore (user data,
  Google-encrypted); nothing secret is stored.
- No SQLCipher: Android FBE + sandbox is the right threat model for names and prices.

## Verified toolchain (2026-09-11)

Kotlin 2.4.20 · AGP 9.2.1 (pinned to the Android Studio-supported version; built-in Kotlin, no `kotlin-android` plugin) · Gradle 9.7.1 · KSP 2.3.12 ·
Compose BOM 2026.09.00 (ui 1.12.1, material3 1.4.0) · Navigation 3 1.1.7 · Lifecycle 2.11.0 ·
Hilt 2.60.1 + androidx.hilt 1.4.0 · Room 2.8.5 · DataStore 1.2.1 · WorkManager 2.11.2 ·
Coroutines 1.11.0 · Serialization 1.11.0 · JUnit 4.13.2 · Truth 1.4.5 · Turbine 1.2.1.

## Roadmap

| Milestone | Phase | Deliverable |
|---|---|---|
| M1 | 1 | Foundation: Gradle, catalog, convention plugins, modules, Hilt, Nav3 shell, base theme |
| M2 | 2 | Domain models, Money, date engine, Room v1 + schema, repositories, use cases, edge-case tests |
| M3 | 3 | Design-system spec → tokens, motion, components with previews |
| M4 | 4 | Onboarding |
| M5 | 5 | Subscriptions: list, add/edit, detail, search/filter/sort |
| M6 | 6 | Home dashboard |
| M7 | 7 | Calendar |
| M8 | 8 | Insights engine |
| M9 | 9 | Notifications |
| M10 | 11 | Settings, export, light theme |
| M11 | 12 | QA, accessibility, performance, baseline profile |
| M12 | 13 | Release build, signing, icon, Play metadata |
| — | 10 | AI layer, only after M12 and only behind a backend proxy |

## Key risks

Date math (anchor-based, exhaustively tested) · mixed currencies without FX (explicit indicator) ·
no honest usage data (declared usage + careful wording) · brand logos (deferred) · notification
reliability on aggressive OEMs (flex windows, in-app note) · Navigation 3 maturity (isolated in
`app` + `core:navigation`) · Material3 expressive APIs are experimental (not used) · expensive
visual effects (no blur, depth via gradients/borders) · AI needs a key → backend proxy required ·
Room migrations (schema export from v1, never destructive) · large numbers / font scale / RTL
designed in from Phase 3.
