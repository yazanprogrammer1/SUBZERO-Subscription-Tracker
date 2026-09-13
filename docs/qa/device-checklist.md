# SUBZERO — On-device QA checklist (Phase 12)

Everything below is what automated tests cannot prove: rendering, motion feel, real WorkManager
timing, system integrations. Run on at least one phone (API 26–36) and, if available, the API 37
emulator. Tick each line; anything failing becomes a tracked fix before Phase 13.

Build to test: `./gradlew :app:installDebug` (debug) and the release APK from Phase 13 for the
final pass. The debug build also carries the **SUBZERO Gallery** for component review. It has no
launcher icon on purpose (one app, one icon); start it with
`adb shell am start -n com.subzero.app.debug/com.subzero.app.gallery.GalleryActivity`.

## Visual (compare against the reference image)

- [ ] Onboarding: 5 pages, staggered reveal, glow drifts slowly, Skip jumps to last page.
- [ ] Home hero: number legible, glow subtle (not a blob), border visible but quiet.
- [ ] Cards: hairline borders visible in dark and light; no flat "Material default" look.
- [ ] Bottom bar pill slides between tabs; icons crossfade tint.
- [ ] Charts draw in on first appearance; current month highlighted.
- [ ] Row → detail shared-element expansion; back returns smoothly; predictive back mirrors it.
- [ ] Add form rises as a modal; success card shows ✓ then returns on its own.
- [ ] Light mode: everything readable, no dark-only assumptions.
- [ ] Font size at 200% (Settings → Display): no clipped text, hero number steps down, chips wrap.
- [ ] 320dp-wide device or split screen: bottom bar hides labels, nothing overflows.
- [ ] Tablet / landscape: content column capped at 640dp and centred.

## Accessibility

- [ ] TalkBack through Home: greeting, hero amount, next charge, trend, savings, insight in order.
- [ ] TalkBack on the list: each row read as one item (name, next date, price, cadence).
- [ ] Calendar days announce "September 16, 2026, 1 charge"; month arrows have labels.
- [ ] Form fields announce their labels ("Service name, edit box"); errors are announced.
- [ ] Chips, switches and the bottom bar report selected state.
- [ ] Every tap target ≥ 48dp (chips are 36dp visually with a 48dp touch area).
- [ ] "Remove animations" in system accessibility: app still works, no animation stalls.
- [ ] Colour is never the only signal: status badges have text; price increases have an icon.

## Data and edge cases (spec §43)

- [ ] Add a subscription anchored on Jan 31 → detail shows Feb 28 / Mar 31 history.
- [ ] Add a yearly subscription with first payment Feb 29 2024 → next charge Feb 28 2026, Feb 29 2028.
- [ ] Add a JPY subscription with a USD home currency → listed, excluded from totals with the note.
- [ ] Add a subscription priced 0 → allowed, shows $0.00.
- [ ] Add a second subscription with the same name → warning shown, save still allowed.
- [ ] Pause → totals drop, calendar hides it, Home "Upcoming" skips it; Resume → next date is in the future.
- [ ] Mark canceled → kept for records, excluded everywhere; Delete → gone, detail closes, insights update.
- [ ] Change the device date forward a month (or wait) → rollover records the payment on next launch.
- [ ] Kill the app mid-form (Don't keep activities) → form state survives; tab stack survives.
- [ ] Delete all data in Settings → onboarding shows again; export afterwards is empty.

## Notifications

- [ ] Enable "Upcoming charges" → permission prompt (API 33+) → allow → card disappears.
- [ ] Deny → warning card stays, "Open system settings" opens the app's notification page.
- [ ] Add a subscription due tomorrow, force the worker: `adb shell cmd jobscheduler run -f com.subzero.app.debug <jobId>` or wait for the 09:00 run → notification "X charges $Y tomorrow".
- [ ] Tap it → app opens on that subscription's detail (cold and warm start).
- [ ] Force the worker again the same day → no duplicate.
- [ ] Turn everything off → nothing arrives.

## Performance

- [ ] Cold start to Home under ~1.5s on a mid-range device (release build).
- [ ] Scrolling 50+ subscriptions is smooth (add via the form; 60 fps in the GPU profiler).
- [ ] Rotate on every screen: no jank, state kept.
- [ ] Battery: no wakelocks; the worker runs once a day (Developer options → WorkManager or `adb shell dumpsys jobscheduler`).
- [ ] Baseline profile generated before release (Phase 13) and startup re-measured.

## Export

- [ ] Export JSON → file opens, contains every subscription with price history and payments.
- [ ] Export CSV → opens in Sheets/Excel with correct columns; names with commas stay intact.
