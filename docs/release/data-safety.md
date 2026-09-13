# Play Console — Data safety answers for SUBZERO 1.0.0

These answers describe the shipped 1.0 build. Re-check whenever the assistant's network use changes.

The app declares `INTERNET`, used by exactly one optional feature: **Enhanced answers**
(Settings → Assistant), which is **off by default**. With it off, no request is ever made.
With it on, each assistant question sends an anonymized summary of the active subscriptions
(service name, amount, billing cycle, category, declared usage, next date) plus the question
text to the configured AI provider over HTTPS. Notes, the display name, identifiers and
payment history are never sent, and nothing is sent by any other screen.

| Question | Answer | Why |
|---|---|---|
| Does your app collect or share any of the required user data types? | **Yes, optional** | Only if the user turns on Enhanced answers: the subscription summary and the question are sent to the AI provider to produce the answer. Off by default; the app is fully usable without it. |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | HTTPS only; the client refuses any non-`https://` endpoint. |
| Do you provide a way for users to request that their data is deleted? | **Yes** | Settings → Data & storage → Delete all data; uninstalling also removes everything. |
| Does your app use any SDKs that collect data? | **No** | Dependencies: AndroidX, Jetpack Compose, Hilt, Room, WorkManager, kotlinx. No analytics or ads SDKs; the AI call is plain HTTPS from our own code. |
| Is the data collection optional? | **Yes** | The Enhanced answers switch is off by default and can be turned off again at any time. |

Data stored locally only (on-device, in the app sandbox; included in Android Backup, encrypted by Google):
- Subscription names, prices, billing dates, categories, notes and the usage the user declares
- Preferences: home currency, theme, notification choices, an optional display name

Permissions declared: `POST_NOTIFICATIONS` (opt-in reminders), `WAKE_LOCK` and
`RECEIVE_BOOT_COMPLETED` (WorkManager's daily local job), and `INTERNET` (opt-in enhanced
assistant answers only). No location, contacts, camera or storage permissions.
