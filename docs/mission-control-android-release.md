# MissionControl-Nebo (Android) release

`:mission-control:android` is MissionControl-Nebo, an Android monitor for a RadarNebo server: it reads the server's
public `/health` report every 5 seconds and shows its status, data source, situation, upstream and polling. English,
Ukrainian and Dutch (`src/main/res/values`, `values-uk`, `values-nl`). Application id
`com.gusak.lab.radarnebo.missioncontrol`.

## Signing key

The release key is kept apart from the RadarNebo app's key and outside the repo:

| File | Location | In git |
|---|---|---|
| Release keystore (PKCS12) | `~/Keys/MissionControl-Nebo/missioncontrol-nebo-release.jks` | No |
| Signing credentials | `~/Keys/MissionControl-Nebo/keystore.properties` | No |

`keystore.properties` in that folder has `storeFile` (relative to the folder), `storePassword`, `keyAlias`
(`missioncontrol-nebo`) and `keyPassword`. To keep the folder elsewhere, pass `-PmissionControl.keystoreDir=<dir>` or
set `MISSIONCONTROL_KEYSTORE_DIR`. Without it the release build is unsigned.

Back up the whole folder (with the password) in a password manager. With Play App Signing this is the **upload key**:
if it is lost, Google can reset it, but only after a support request.

On CI, store the `.jks` as a base64 secret, decode it into a directory, write `keystore.properties` next to it and
point `MISSIONCONTROL_KEYSTORE_DIR` at that directory.

## Building

```bash
./gradlew :mission-control:android:bundleRelease     # AAB for Google Play
./gradlew :mission-control:android:assembleRelease   # APK for sideloading
```

Outputs: `mission-control/android/build/outputs/bundle/release/android-release.aab` and
`mission-control/android/build/outputs/apk/release/android-release.apk`. The release build is minified with R8.

Before each new release, bump `versionCode` and `versionName` in `mission-control/android/build.gradle.kts`.

The module compiles against Android API 37.2 (Compose 1.13 needs 37.1 or later):
`sdkmanager "platforms;android-37.2"`.

## Store listing

`mission-control/android/play/` holds what the Play Console asks for:

- `icon-512.png` (app icon) and `feature-graphic.png` (1024 × 500), drawn by `tools/make-play-assets.swift`
- `listings/<en-US|uk|nl-NL>/`: title, short and full description, and the phone screenshots. `captures/<n>.png` are raw
  app captures (1080 × 2160, status and navigation bars cropped off); `make-play-assets.swift` frames them with the
  captions in `screenshot-captions.txt` into the 1080 × 1920 (9:16) `phone-screenshots/<n>.png` that Play requires
- `release-notes/<language>.txt`
- `privacy-policy.md`: published as a public Google Doc in the Gusak lab Drive (PuzzleScroll folder),
  https://docs.google.com/document/d/1S-4t0lohj6npOxOttlyyhss9AvBZOsHAUabfGgW5WKE/edit?usp=sharing

## First release on Google Play

The Play Console has no API for creating an app, so the first release is done by hand:

1. Play Console → **Create app**: name *MissionControl-Nebo*, default language English (United States), App, Free.
2. **Store presence → Main store listing**: paste the en-US texts and upload the icon, feature graphic and screenshots.
   Add translations for Ukrainian (uk) and Dutch (nl-NL) from the other listing folders.
3. **App content**: privacy policy URL; ads: none; app access: all functionality available without login;
   content rating questionnaire (utility, no user content); target audience 18+; data safety: no data collected or
   shared; not a news app; no government affiliation.
4. **Testing → Internal testing** (or Closed testing): create a release, accept Play App Signing, upload
   `android-release.aab`, paste the release notes, roll out.
5. Personal developer accounts created after November 2023 need a closed test with at least 12 testers for 14 days
   before **Production** opens. Organisation accounts can promote the release to Production directly.
