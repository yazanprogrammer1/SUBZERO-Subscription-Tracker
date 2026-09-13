# Release runbook

## One-time
1. Generate the upload key (see keystore.properties.example) and store it outside the repo.
   Enroll in Play App Signing so Google holds the app signing key.
2. Create the Play Console listing from docs/release/play-listing.md and data-safety.md, and
   host privacy-policy.md at a public URL.

## Every release
1. Bump `versionCode` (+1) and `versionName` in app/build.gradle.kts; update release-notes.md.
2. `./gradlew build lintRelease` must be green (207+ tests, zero lint warnings).
3. Run docs/qa/device-checklist.md on a real device with the release build:
   `./gradlew :app:installRelease` (requires keystore.properties).
4. Baseline profile (recommended before the first public release): add a `:baselineprofile`
   `com.android.test` module with the `androidx.baselineprofile` plugin, run
   `./gradlew :app:generateReleaseBaselineProfile` on a device, and commit the generated
   `app/src/release/generated/baselineProfiles/`. Re-measure cold start.
5. `./gradlew :app:bundleRelease` → upload app/build/outputs/bundle/release/app-release.aab.
   Keep app/build/outputs/mapping/release/mapping.txt with the release and upload it to Play
   for readable crash reports (there is no crash SDK in the app).
6. Tag the commit: `git tag -a v1.0.0 -m "SUBZERO 1.0.0"`.

## Rollback
Play Console → Release → halt rollout; the previous AAB remains available. Local data stays
forward compatible within database schema version 1; any future migration must keep older
data readable and must never be destructive.
