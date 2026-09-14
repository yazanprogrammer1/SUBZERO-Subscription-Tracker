# SUBZERO Privacy Policy

Effective: 2026-09-14

The published version of this policy is [`docs/privacy-policy.html`](../privacy-policy.html)
(English and Arabic). This file is the plain-text source of the same statements; keep the two in
step whenever what the app stores or sends changes.

SUBZERO is a subscription tracker that runs on your device.

**What we collect:** nothing. SUBZERO has no user accounts, uses no analytics, advertising or
crash-reporting services, and the developer operates no server that receives data from the app.

**What stays on your device:** the subscriptions you enter (names, prices, billing cycles and
dates, categories, notes and how often you say you use them), the payment records and price
changes derived from them, your preferences (currency, theme, notification choices, language,
optional display name), and — if you set one up — the AI provider endpoint, model and API key,
the key encrypted with an Android Keystore key before it is stored. This data lives in the app's
private storage and may be included in your Android backup, which Google encrypts.

**Enhanced assistant answers (optional, off by default):** the assistant answers from the data
on the device and needs no internet. You may connect it to an AI provider of your choice under
Settings → Assistant with a key you obtain from that provider yourself; the developer is not that
provider and never receives the data. While the switch is on, each question sends over HTTPS to
the endpoint you configured: the question and the earlier turns of the conversation; a summary
of your subscriptions (names, amounts, cycles, categories, declared usage, next dates, and
active/paused/canceled status); your monthly and yearly totals; recorded price changes; and
recorded payments summed by month. Never sent: notes, display name, internal identifiers. Data
sent to the provider is governed by that provider's policy. Turning the switch off, or removing
the key, stops all network use immediately.

**Notifications:** reminders are off by default. If you turn them on, SUBZERO asks for the
notification permission and schedules reminders locally. No data is sent anywhere.

**Permissions:** `POST_NOTIFICATIONS` (reminders, only if enabled), `INTERNET` (enhanced answers
only), `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED` (WorkManager's daily local job). No location,
contacts, camera, microphone, files or other apps.

**Your control:** you can export all your data as JSON or CSV, and delete everything, from
Settings → Data & storage. Uninstalling the app removes all its data.

**Children:** SUBZERO is not directed at children under 13 and collects no data from anyone.

**Languages:** SUBZERO is available in English and Arabic and follows your phone's language;
on Android 13 and later you can pick a different language for this app alone.

**Changes:** if a future version changes what is stored or what enhanced answers send, this
policy and the Play Store data safety section will be updated before that version is released.

**Contact:** open an issue at
https://github.com/yazanprogrammer1/SUBZERO-Subscription-Tracker/issues
