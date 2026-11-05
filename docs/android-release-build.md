# Android release build

How to set up signing for the Android app (`:app:android`) and build a release APK or App Bundle.

> **Google Play:** the app ships on the listing created for MissionControl-Nebo, so its application id is
> `com.gusak.lab.radarnebo.missioncontrol` and release builds are signed with that listing's upload key, read from
> `~/Keys/MissionControl-Nebo/keystore.properties` or the folder in `-PmissionControl.keystoreDir` /
> `MISSIONCONTROL_KEYSTORE_DIR`. The `keystore.properties` at the repo root described below is no longer used by the
> build. Store texts and graphics are in `app/android/play/`.

## Where the files go

| File | Location | In git |
|---|---|---|
| Release keystore | `app/android/keystore/radarnebo-release.jks` | No (`*.jks` is gitignored) |
| Signing credentials | `keystore.properties` in the repo root | No (`keystore.properties` is gitignored) |
| Firebase config | `app/android/google-services.json` | No (gitignored) |
| Server address and access key | `client.properties` in the repo root (copy `client.properties.example`) | No (gitignored) |

Never commit any of these. Store the keystore and its password together in a password manager or other
secure place. If you lose them, you can't sign updates with the same key.

## keystore.properties

`app/android/build.gradle.kts` reads `keystore.properties` from the repo root. It has four keys:

```properties
storeFile=app/android/keystore/radarnebo-release.jks
storePassword=<keystore password>
keyAlias=radarnebo
keyPassword=<key password>
```

- `storeFile` is relative to the repo root, but an absolute path works too, so the `.jks` can live outside the repo
  (for example `~/keys/radarnebo-release.jks`).
- The keystore is PKCS12, so `storePassword` and `keyPassword` are the same.

If `keystore.properties` is missing, the release build still runs but produces an **unsigned** APK/AAB, which can't
be installed or uploaded.

## Setting up a new machine

1. Get `radarnebo-release.jks` and its password from wherever the team keeps them.
2. Copy the keystore to `app/android/keystore/radarnebo-release.jks`.
3. Create `keystore.properties` in the repo root as shown above.
4. Optionally restrict permissions: `chmod 600 keystore.properties app/android/keystore/radarnebo-release.jks`.

## Creating a new keystore

Only do this if no key exists yet. A Play Store app must keep the same upload key unless you ask Google to reset it.

```bash
mkdir -p app/android/keystore
keytool -genkeypair -v \
  -keystore app/android/keystore/radarnebo-release.jks \
  -storetype PKCS12 -alias radarnebo \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=<name>, O=Gusak Lab, C=UA"
```

`keytool` asks for a password. Put that password in `keystore.properties` as both `storePassword` and `keyPassword`.

## Building

```bash
./gradlew :app:android:assembleRelease   # APK, for sideloading or direct distribution
./gradlew :app:android:bundleRelease     # AAB, for Google Play
```

Outputs:

- `app/android/build/outputs/apk/release/android-release.apk`
- `app/android/build/outputs/bundle/release/android-release.aab`

A local build is `3 (1.0.2)` unless you pass `-PradarNebo.versionCode=<n>` and `-PradarNebo.versionName=<x.y.z>`.

## Verifying the signature

```bash
$ANDROID_HOME/build-tools/<version>/apksigner verify --print-certs \
  app/android/build/outputs/apk/release/android-release.apk
```

To print the key fingerprints (SHA-1 / SHA-256), for example for Firebase or Google Play:

```bash
keytool -list -v -keystore app/android/keystore/radarnebo-release.jks -alias radarnebo
```

## Deploying to Google Play

`.github/workflows/deploy-android.yml` builds a signed bundle from `main` and uploads it to the **production** track
through the Google Play Developer API, with the notes in `app/android/play/release-notes/`. Google then reviews it as
usual. Update the release notes in a pull request before each release.

```bash
gh workflow run deploy-android.yml --ref main -f version_name=1.0.2
gh workflow run deploy-android.yml --ref main -f version_name=1.0.2 -f user_fraction=0.2   # staged
```

The versionCode is the workflow's run number + 2.

**Check the Play Console's Publishing overview first.** If a release is still in review, a deploy cancels that review
and resubmits everything together with the new release (the API's default, `CANCEL_IN_REVIEW_AND_SUBMIT`).

### Credentials

The secrets live in the repository's `google-play` environment, which only `main` can use:

| Secret | Value |
|---|---|
| `MISSIONCONTROL_KEYSTORE_BASE64` | `base64 -i missioncontrol-nebo-release.jks` |
| `MISSIONCONTROL_KEYSTORE_PASSWORD` | The keystore password (also the key password; the keystore is PKCS12) |
| `RADARNEBO_ANDROID_CLIENT_KEY` | The server key built into the public app; one of the server's `CLIENT_KEYS` |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | The service account's JSON key |

The service account:

1. Google Cloud Console, in the `elderpuzzlescroll` project: enable the **Google Play Android Developer API**, then
   under **IAM & Admin → Service Accounts** create `radarnebo-play-deploy`, without project roles, and add a JSON key.
2. Play Console → **Users and permissions → Invite new users**: enter the account's address. Under **App permissions**,
   add this app only, with **Release to production, exclude devices, and use Play App Signing**,
   **Release apps to testing tracks** and **Manage testing tracks and edit tester lists**. Leave account permissions empty.
3. `gh secret set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON --env google-play < key.json`, then delete `key.json`.

A new service account can take up to a day to start working with the API.
