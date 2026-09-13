# Play Console — Data safety answers for SUBZERO 1.0.0

These answers describe the shipped 1.0 build. Re-check before any release that adds networking.

| Question | Answer | Why |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | Nothing leaves the device. The app has no INTERNET permission. |
| Is all of the user data collected by your app encrypted in transit? | N/A | No data is transmitted. |
| Do you provide a way for users to request that their data is deleted? | **Yes** | Settings → Data & storage → Delete all data; uninstalling also removes everything. |
| Does your app use any SDKs that collect data? | **No** | Dependencies: AndroidX, Jetpack Compose, Hilt, Room, WorkManager, kotlinx. No analytics or ads SDKs. |

Data stored locally only (on-device, in the app sandbox; included in Android Backup, encrypted by Google):
- Subscription names, prices, billing dates, categories, notes and the usage the user declares
- Preferences: home currency, theme, notification choices, an optional display name

Permissions declared: `POST_NOTIFICATIONS` (opt-in reminders), `WAKE_LOCK` and
`RECEIVE_BOOT_COMPLETED` (WorkManager's daily local job). No location, contacts, camera,
storage or network permissions.
